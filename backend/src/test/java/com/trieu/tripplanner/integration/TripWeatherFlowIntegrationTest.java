package com.trieu.tripplanner.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.trieu.tripplanner.TestcontainersConfiguration;
import com.trieu.tripplanner.model.User;
import com.trieu.tripplanner.provider.weather.MockWeatherProvider;
import com.trieu.tripplanner.provider.weather.WeatherProvider;
import com.trieu.tripplanner.provider.weather.dto.DailyForecast;
import com.trieu.tripplanner.repository.UserRepository;
import com.trieu.tripplanner.security.JwtTokenProvider;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * The weather of a trip through every layer against real MySQL (design.md 10.2 "Weather", rules 14.20 and 14.22).
 * What only this level can prove:
 * <ul>
 *   <li>the mock source is the bean the application really uses when nothing else is configured;</li>
 *   <li>"today" comes from the time zone stored in the account, read from the database;</li>
 *   <li>the trip, its days and the forecast fit together: one element per day, the right days carry a forecast;</li>
 *   <li>the number of SQL statements does not grow with the length of the trip (CLAUDE.md section 8).</li>
 * </ul>
 * The clock of the application stands still at 05/10/2026 03:00 UTC: 10:00 on the 5th in Việt Nam, 20:00 on the
 * 4th in Los Angeles. Hibernate statistics are switched on for this class only.
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class TripWeatherFlowIntegrationTest {

    private static final String TRIPS_URL = "/api/v1/trips";

    private static final Instant NOW = Instant.parse("2026-10-05T03:00:00Z");
    /** "Today" for an account in Việt Nam at {@link #NOW}. */
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 5);

    private static final BigDecimal DA_NANG_LAT = new BigDecimal("16.0678");
    private static final BigDecimal DA_NANG_LNG = new BigDecimal("108.2208");

    // Replaces the clock bean of ClockConfig for this class
    @TestBean(methodName = "clockStandingStill")
    private Clock clock;

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private WeatherProvider weatherProvider;

    private String ownerBearer;
    private String otherBearer;

    static Clock clockStandingStill() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }

    @BeforeEach
    void createUsers() {
        ownerBearer = bearerFor(userRepository.save(user("owner@example.com", User.DEFAULT_TIMEZONE)));
        otherBearer = bearerFor(userRepository.save(user("other@example.com", User.DEFAULT_TIMEZONE)));
    }

    @AfterEach
    void cleanUp() {
        // Days go with their trips (ON DELETE CASCADE); soft-deleted trips are rows too
        jdbcTemplate.update("DELETE FROM trips");
        jdbcTemplate.update("DELETE FROM users");
    }

    // ---- the forecast of a trip ----------------------------------------------------------------------------------

    @Test
    void travellerSeesAForecastForTheDaysOfTheTripInsideTheNextSixteenDays() {
        // 04/10 .. 22/10: yesterday, the sixteen days of the window (05/10 .. 20/10), and two days beyond it
        long tripId = createTrip(ownerBearer, TODAY.minusDays(1), TODAY.plusDays(17), true);

        String answer = body(get(weatherUrl(tripId), ownerBearer));

        assertThat(JsonPath.<String>read(answer, "$.data.status")).isEqualTo("OK");
        List<Map<String, Object>> days = JsonPath.read(answer, "$.data.days");
        assertThat(days).hasSize(19);
        // One element per day of the trip, the same days in the same order as the itinerary shows them
        List<Map<String, Object>> tripDays = JsonPath.read(body(get(TRIPS_URL + "/" + tripId + "/days", ownerBearer)),
                "$.data");
        assertThat(days).extracting(day -> day.get("dayId")).containsExactlyElementsOf(
                tripDays.stream().map(day -> day.get("id")).toList());
        assertThat(days).extracting(day -> day.get("date")).containsExactlyElementsOf(
                tripDays.stream().map(day -> day.get("date")).toList());

        assertThat(days.get(0)).containsEntry("date", "2026-10-04").containsEntry("forecast", null);
        assertThat(days.subList(1, 17)).allSatisfy(day -> assertThat(day.get("forecast")).isNotNull());
        assertThat(days.get(16)).containsEntry("date", "2026-10-20");   // day 16, counting today
        assertThat(days.get(17)).containsEntry("date", "2026-10-21").containsEntry("forecast", null);
        assertThat(days.get(18)).containsEntry("date", "2026-10-22").containsEntry("forecast", null);

        // The numbers are those of the mock source at the destination of the trip, and nothing else
        assertThat(weatherProvider).isInstanceOf(MockWeatherProvider.class);
        DailyForecast expected = weatherProvider.forecast(DA_NANG_LAT, DA_NANG_LNG, TODAY, TODAY).getFirst();
        assertThat(JsonPath.<Map<String, Object>>read(answer, "$.data.days[1].forecast"))
                .containsEntry("condition", expected.condition().name())
                .containsEntry("tempMin", expected.tempMin())
                .containsEntry("tempMax", expected.tempMax())
                .containsEntry("precipitationProbability", expected.precipitationProbability())
                .hasSize(4);

        // Reloading the page shows the same weather
        String again = body(get(weatherUrl(tripId), ownerBearer));
        assertThat(JsonPath.<Object>read(again, "$.data")).isEqualTo(JsonPath.<Object>read(answer, "$.data"));
    }

    @Test
    void tripGetsItsForecastOnceItsDestinationIsChosen() {
        long tripId = createTrip(ownerBearer, TODAY, TODAY.plusDays(2), false);

        // No destination yet: an answer, not an error
        MvcTestResult before = get(weatherUrl(tripId), ownerBearer);
        assertThat(before).hasStatusOk();
        assertThat(JsonPath.<String>read(body(before), "$.data.status")).isEqualTo("NO_DESTINATION");
        List<Map<String, Object>> daysBefore = JsonPath.read(body(before), "$.data.days");
        assertThat(daysBefore).hasSize(3).allSatisfy(day -> assertThat(day.get("forecast")).isNull());

        assertThat(send("PATCH", TRIPS_URL + "/" + tripId, ownerBearer, """
                { "destinationName": "Đà Nẵng", "destinationLat": 16.0678, "destinationLng": 108.2208 }
                """)).hasStatusOk();

        String after = body(get(weatherUrl(tripId), ownerBearer));
        assertThat(JsonPath.<String>read(after, "$.data.status")).isEqualTo("OK");
        List<Map<String, Object>> daysAfter = JsonPath.read(after, "$.data.days");
        assertThat(daysAfter).hasSize(3).allSatisfy(day -> assertThat(day.get("forecast")).isNotNull());
    }

    @Test
    void todayIsTheDayOfTheAccountReadFromTheDatabase() {
        // The same moment: the 5th in Việt Nam, still the 4th in Los Angeles
        String losAngelesBearer = bearerFor(userRepository.save(user("la@example.com", "America/Los_Angeles")));
        long vietnamTrip = createTrip(ownerBearer, TODAY.minusDays(1), TODAY.plusDays(1), true);
        long losAngelesTrip = createTrip(losAngelesBearer, TODAY.minusDays(1), TODAY.plusDays(1), true);

        List<Map<String, Object>> seenFromVietnam = JsonPath.read(body(get(weatherUrl(vietnamTrip), ownerBearer)),
                "$.data.days");
        List<Map<String, Object>> seenFromLosAngeles = JsonPath.read(
                body(get(weatherUrl(losAngelesTrip), losAngelesBearer)), "$.data.days");

        // 04/10 is yesterday for one traveller and today for the other
        assertThat(seenFromVietnam.get(0)).containsEntry("date", "2026-10-04").containsEntry("forecast", null);
        assertThat(seenFromLosAngeles.get(0)).containsEntry("date", "2026-10-04");
        assertThat(seenFromLosAngeles.get(0).get("forecast")).isNotNull();
        // The days both can still see carry the same forecast: same place, same day
        assertThat(seenFromLosAngeles.get(1).get("forecast")).isEqualTo(seenFromVietnam.get(1).get("forecast"));
    }

    // ---- who may see it ------------------------------------------------------------------------------------------

    @Test
    void weatherOfATripIsOnlyForThoseWhoMaySeeTheTrip() {
        long tripId = createTrip(ownerBearer, TODAY, TODAY.plusDays(2), true);

        assertThat(mvc.get().uri(weatherUrl(tripId)).exchange())
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("UNAUTHORIZED");
        assertThat(get(weatherUrl(tripId), otherBearer))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("FORBIDDEN");
        assertThat(get(weatherUrl(999_999L), ownerBearer))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("RESOURCE_NOT_FOUND");

        // A deleted trip is gone for its owner and for everybody else alike
        assertThat(send("DELETE", TRIPS_URL + "/" + tripId, ownerBearer, null)).hasStatusOk();
        assertThat(get(weatherUrl(tripId), ownerBearer)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(get(weatherUrl(tripId), otherBearer)).hasStatus(HttpStatus.NOT_FOUND);
    }

    // ---- no query per day ----------------------------------------------------------------------------------------

    @Test
    void numberOfQueriesDoesNotGrowWithTheLengthOfTheTrip() {
        long threeDays = createTrip(ownerBearer, TODAY, TODAY.plusDays(2), true);
        long thirtyDays = createTrip(ownerBearer, TODAY, TODAY.plusDays(29), true);
        long withoutDestination = createTrip(ownerBearer, TODAY, TODAY.plusDays(29), false);

        // permission + trip + days + time zone of the caller
        assertThat(statementsFor(weatherUrl(threeDays))).isEqualTo(4);
        assertThat(statementsFor(weatherUrl(thirtyDays))).isEqualTo(4);
        // no destination: "today" is never needed
        assertThat(statementsFor(weatherUrl(withoutDestination))).isEqualTo(3);
    }

    // ---- helpers -------------------------------------------------------------------------------------------------

    private long statementsFor(String url) {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        assertThat(get(url, ownerBearer)).hasStatusOk();
        return statistics.getPrepareStatementCount();
    }

    /** POST /trips; with a destination the trip points at the centre of Đà Nẵng. */
    private long createTrip(String bearer, LocalDate start, LocalDate end, boolean withDestination) {
        String destination = withDestination
                ? ", \"destinationName\": \"Đà Nẵng\", \"destinationLat\": %s, \"destinationLng\": %s"
                        .formatted(DA_NANG_LAT, DA_NANG_LNG)
                : "";
        MvcTestResult result = send("POST", TRIPS_URL, bearer, """
                { "title": "Đà Nẵng", "startDate": "%s", "endDate": "%s", "currency": "VND"%s }
                """.formatted(start, end, destination));
        assertThat(result).hasStatus(HttpStatus.CREATED);
        return ((Number) JsonPath.read(body(result), "$.data.id")).longValue();
    }

    private static String weatherUrl(long tripId) {
        return "/api/v1/weather/trips/" + tripId;
    }

    private MvcTestResult get(String url, String bearer) {
        return send("GET", url, bearer, null);
    }

    private MvcTestResult send(String method, String url, String bearer, String json) {
        var request = switch (method) {
            case "POST" -> mvc.post();
            case "PATCH" -> mvc.patch();
            case "DELETE" -> mvc.delete();
            default -> mvc.get();
        };
        var prepared = request.uri(url).header(HttpHeaders.AUTHORIZATION, bearer);
        if (json != null) {
            prepared = prepared.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return prepared.exchange();
    }

    // Explicit UTF-8: MockHttpServletResponse otherwise decodes as ISO-8859-1 and mangles Vietnamese
    private static String body(MvcTestResult result) {
        return new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
    }

    private String bearerFor(User user) {
        return "Bearer " + jwtTokenProvider.generateAccessToken(user).token();
    }

    private static User user(String email, String timezone) {
        return User.builder()
                .email(email)
                .passwordHash("$2a$12$placeholder-bcrypt-hash-not-real")
                .fullName("Test " + email)
                .emailVerified(true)
                .timezone(timezone)
                .build();
    }

}

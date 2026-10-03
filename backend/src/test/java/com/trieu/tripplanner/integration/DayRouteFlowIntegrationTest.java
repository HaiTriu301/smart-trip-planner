package com.trieu.tripplanner.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.trieu.tripplanner.TestcontainersConfiguration;
import com.trieu.tripplanner.model.User;
import com.trieu.tripplanner.repository.UserRepository;
import com.trieu.tripplanner.security.JwtTokenProvider;
import jakarta.persistence.EntityManagerFactory;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * The travel of a day through every layer against real MySQL and the map source the application picks by
 * itself (design.md 10.2 "Quy ước Route"). What only this level can prove:
 * <ul>
 *   <li>the legs follow the order of the day as the database holds it, also after a drag and drop;</li>
 *   <li>the place of an activity, attached or removed through the activity API, is what the route is made of;</li>
 *   <li>the real permission bean and the real "day of this trip" query refuse what they must;</li>
 *   <li>the number of SQL statements does not grow with the number of activities (CLAUDE.md section 8).</li>
 * </ul>
 * The numbers come from the bundled places of Đà Nẵng and the estimate of the mock source (straight line x 1.3,
 * 30 km/h). Hibernate statistics are switched on like in ActivityPlaceFlowIntegrationTest, so both share one
 * application context.
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class DayRouteFlowIntegrationTest {

    private static final String TRIPS_URL = "/api/v1/trips";
    private static final String PLACES_URL = "/api/v1/places";

    private static final String CHO_HAN = "da-nang-cho-han";
    private static final String BUN_CHA_CA = "da-nang-bun-cha-ca-109";
    private static final String LINH_UNG = "da-nang-chua-linh-ung";

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

    private String ownerBearer;
    private String otherBearer;

    /** A trip 05/10 → 07/10 of the owner, created before every test. */
    private long tripId;
    private List<Long> dayIds;

    @BeforeEach
    void createUsersAndTrip() {
        ownerBearer = bearerFor(userRepository.save(user("owner@example.com")));
        otherBearer = bearerFor(userRepository.save(user("other@example.com")));
        tripId = createTrip(ownerBearer, "Đà Nẵng");
        dayIds = dayIdsOf(tripId, ownerBearer);
    }

    @AfterEach
    void cleanUp() {
        // Activities go with their trips (ON DELETE CASCADE); places only once no activity points to them
        jdbcTemplate.update("DELETE FROM trips");
        jdbcTemplate.update("DELETE FROM places");
        jdbcTemplate.update("DELETE FROM users");
    }

    // ---- the route follows the day -------------------------------------------------------------------------------

    @Test
    void routeOfADayFollowsItsActivitiesAlsoAfterADragAndDrop() {
        long market = addActivity("Chợ Hàn", CHO_HAN);
        long lunch = addActivity("Bún chả cá", BUN_CHA_CA);
        long pagoda = addActivity("Chùa Linh Ứng", LINH_UNG);

        assertThat(send("GET", routeUrl(0), ownerBearer, null))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        { "success": true,
                          "data": { "legs": [ { "fromActivityId": %d, "toActivityId": %d,
                                                "distanceMeters": 998, "durationSeconds": 120 },
                                              { "fromActivityId": %d, "toActivityId": %d,
                                                "distanceMeters": 8827, "durationSeconds": 1059 } ],
                                    "totalDistanceMeters": 9825, "totalDurationSeconds": 1179 } }
                        """.formatted(market, lunch, lunch, pagoda));

        // The pagoda is dragged to the top of the day: pagoda, market, lunch
        assertThat(send("PUT", TRIPS_URL + "/" + tripId + "/activities/reorder", ownerBearer, """
                { "items": [ { "activityId": %d, "dayId": %d, "orderIndex": 500 } ] }
                """.formatted(pagoda, dayIds.get(0)))).hasStatusOk();

        assertThat(send("GET", routeUrl(0), ownerBearer, null))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        { "data": { "legs": [ { "fromActivityId": %d, "toActivityId": %d,
                                                "distanceMeters": 8812, "durationSeconds": 1057 },
                                              { "fromActivityId": %d, "toActivityId": %d,
                                                "distanceMeters": 998, "durationSeconds": 120 } ],
                                    "totalDistanceMeters": 9810, "totalDurationSeconds": 1177 } }
                        """.formatted(pagoda, market, market, lunch));
    }

    @Test
    void movingAnActivityToAnotherDayMovesItOutOfTheRoute() {
        long market = addActivity("Chợ Hàn", CHO_HAN);
        long lunch = addActivity("Bún chả cá", BUN_CHA_CA);
        long pagoda = addActivity("Chùa Linh Ứng", LINH_UNG);

        // Lunch goes to day 2: day 1 travels market → pagoda directly, day 2 has one place and nothing to travel
        assertThat(send("PUT", TRIPS_URL + "/" + tripId + "/activities/reorder", ownerBearer, """
                { "items": [ { "activityId": %d, "dayId": %d, "orderIndex": 1000 } ] }
                """.formatted(lunch, dayIds.get(1)))).hasStatusOk();

        assertThat(send("GET", routeUrl(0), ownerBearer, null))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        { "data": { "legs": [ { "fromActivityId": %d, "toActivityId": %d,
                                                "distanceMeters": 8812, "durationSeconds": 1057 } ],
                                    "totalDistanceMeters": 8812, "totalDurationSeconds": 1057 } }
                        """.formatted(market, pagoda));
        assertThat(send("GET", routeUrl(1), ownerBearer, null)).hasStatusOk().bodyJson().isLenientlyEqualTo(EMPTY_ROUTE);
    }

    // ---- activities without a place ------------------------------------------------------------------------------

    @Test
    void activityWithoutAPlaceIsSkippedAndRemovingAPlaceShortensTheRoute() {
        long market = addActivity("Chợ Hàn", CHO_HAN);
        long rest = id(send("POST", activitiesUrl(0), ownerBearer, """
                { "title": "Nghỉ trưa" }
                """));
        long lunch = addActivity("Bún chả cá", BUN_CHA_CA);
        long pagoda = addActivity("Chùa Linh Ứng", LINH_UNG);

        // "Nghỉ trưa" stands between the market and lunch and is in no leg: no 500, the way is not cut there
        String route = body(send("GET", routeUrl(0), ownerBearer, null));
        assertThat(JsonPath.<List<Number>>read(route, "$.data.legs[*].fromActivityId"))
                .extracting(Number::longValue).containsExactly(market, lunch);
        assertThat(JsonPath.<List<Number>>read(route, "$.data.legs[*].toActivityId"))
                .extracting(Number::longValue).containsExactly(lunch, pagoda).doesNotContain(rest);
        assertThat(JsonPath.<Integer>read(route, "$.data.totalDistanceMeters")).isEqualTo(9825);

        // Lunch loses its place: one leg, from the market straight to the pagoda
        assertThat(send("PATCH", activityUrl(lunch), ownerBearer, """
                { "clearPlace": true }
                """)).hasStatusOk();
        assertThat(send("GET", routeUrl(0), ownerBearer, null))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        { "data": { "legs": [ { "fromActivityId": %d, "toActivityId": %d,
                                                "distanceMeters": 8812, "durationSeconds": 1057 } ],
                                    "totalDistanceMeters": 8812, "totalDurationSeconds": 1057 } }
                        """.formatted(market, pagoda));

        // The pagoda loses its place too: one place left, nothing to travel, still an answer and not an error
        assertThat(send("PATCH", activityUrl(pagoda), ownerBearer, """
                { "clearPlace": true }
                """)).hasStatusOk();
        assertThat(send("GET", routeUrl(0), ownerBearer, null)).hasStatusOk().bodyJson().isLenientlyEqualTo(EMPTY_ROUTE);
    }

    @Test
    void dayWithoutActivitiesHasAnEmptyRoute() {
        assertThat(send("GET", routeUrl(2), ownerBearer, null)).hasStatusOk().bodyJson().isLenientlyEqualTo(EMPTY_ROUTE);
    }

    // ---- who may read it -----------------------------------------------------------------------------------------

    @Test
    void routeIsForThoseWhoMayViewTheTripAndOnlyForDaysOfThatTrip() {
        addActivity("Chợ Hàn", CHO_HAN);
        addActivity("Chùa Linh Ứng", LINH_UNG);

        // no token; a signed-in user who is not the owner
        assertThat(send("GET", routeUrl(0), "", null)).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(send("GET", routeUrl(0), otherBearer, null))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("FORBIDDEN");

        // The other user owns a trip, so the permission check on THAT trip passes. Asking through it for a day of
        // the owner's trip must look like a day that does not exist, and show none of its legs
        long othersTrip = createTrip(otherBearer, "Huế");
        MvcTestResult throughOwnTrip = send("GET", TRIPS_URL + "/" + othersTrip + "/days/" + dayIds.get(0) + "/route",
                otherBearer, null);
        assertThat(throughOwnTrip)
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(body(throughOwnTrip)).doesNotContain("legs");

        // a day that never existed; a trip that never existed
        assertThat(send("GET", TRIPS_URL + "/" + tripId + "/days/999999/route", ownerBearer, null))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send("GET", TRIPS_URL + "/999999/days/" + dayIds.get(0) + "/route", ownerBearer, null))
                .hasStatus(HttpStatus.NOT_FOUND);
        // not a number
        assertThat(send("GET", TRIPS_URL + "/" + tripId + "/days/abc/route", ownerBearer, null))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("VALIDATION_ERROR");
    }

    // ---- no N+1 --------------------------------------------------------------------------------------------------

    @Test
    void routeCostsFourQueriesHoweverManyActivitiesTheDayHas() {
        // permission + trip exists + day of trip + activities with their places
        assertThat(statementsFor(routeUrl(0))).isEqualTo(4);

        addActivity("Chợ Hàn", CHO_HAN);
        addActivity("Chùa Linh Ứng", LINH_UNG);
        assertThat(statementsFor(routeUrl(0))).isEqualTo(4);

        for (String externalId : List.of(BUN_CHA_CA, "da-nang-cau-rong", "da-nang-cho-con", "da-nang-ga-da-nang")) {
            addActivity("Ghé " + externalId, externalId);
        }
        send("POST", activitiesUrl(0), ownerBearer, """
                { "title": "Nghỉ trưa" }
                """);
        assertThat(statementsFor(routeUrl(0))).isEqualTo(4);

        // and the legs are really there, not left out to save the queries: 6 places, 5 legs
        List<Object> legs = JsonPath.read(body(send("GET", routeUrl(0), ownerBearer, null)), "$.data.legs");
        assertThat(legs).hasSize(5);
    }

    // ---- helpers -------------------------------------------------------------------------------------------------

    private static final String EMPTY_ROUTE = """
            { "success": true, "data": { "legs": [], "totalDistanceMeters": 0, "totalDurationSeconds": 0 } }
            """;

    private long statementsFor(String url) {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        assertThat(send("GET", url, ownerBearer, null)).hasStatusOk();
        return statistics.getPrepareStatementCount();
    }

    /** Picks a result of the bundled data and adds an activity at that place to day 1; returns the activity id. */
    private long addActivity(String title, String externalId) {
        MvcTestResult place = send("POST", PLACES_URL, ownerBearer, """
                { "provider": "MOCK", "externalId": "%s" }
                """.formatted(externalId));
        assertThat(place).hasStatusOk();
        MvcTestResult activity = send("POST", activitiesUrl(0), ownerBearer, """
                { "title": "%s", "placeId": %d }
                """.formatted(title, id(place)));
        assertThat(activity).hasStatus(HttpStatus.CREATED);
        return id(activity);
    }

    private long createTrip(String bearer, String title) {
        MvcTestResult trip = send("POST", TRIPS_URL, bearer, """
                { "title": "%s", "startDate": "2026-10-05", "endDate": "2026-10-07", "currency": "VND" }
                """.formatted(title));
        assertThat(trip).hasStatus(HttpStatus.CREATED);
        return id(trip);
    }

    private List<Long> dayIdsOf(long trip, String bearer) {
        List<Number> ids = JsonPath.read(body(send("GET", TRIPS_URL + "/" + trip + "/days", bearer, null)),
                "$.data[*].id");
        return ids.stream().map(Number::longValue).toList();
    }

    private String routeUrl(int dayPosition) {
        return TRIPS_URL + "/" + tripId + "/days/" + dayIds.get(dayPosition) + "/route";
    }

    private String activitiesUrl(int dayPosition) {
        return TRIPS_URL + "/" + tripId + "/days/" + dayIds.get(dayPosition) + "/activities";
    }

    private String activityUrl(long activityId) {
        return TRIPS_URL + "/" + tripId + "/activities/" + activityId;
    }

    private MvcTestResult send(String method, String url, String bearer, String json) {
        var request = switch (method) {
            case "POST" -> mvc.post();
            case "PUT" -> mvc.put();
            case "PATCH" -> mvc.patch();
            default -> mvc.get();
        };
        var prepared = request.uri(url).header(HttpHeaders.AUTHORIZATION, bearer);
        if (json != null) {
            prepared = prepared.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return prepared.exchange();
    }

    private static long id(MvcTestResult result) {
        return ((Number) JsonPath.read(body(result), "$.data.id")).longValue();
    }

    // Explicit UTF-8: MockHttpServletResponse otherwise decodes as ISO-8859-1 and mangles Vietnamese
    private static String body(MvcTestResult result) {
        return new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
    }

    private String bearerFor(User user) {
        return "Bearer " + jwtTokenProvider.generateAccessToken(user).token();
    }

    private static User user(String email) {
        return User.builder()
                .email(email)
                .passwordHash("$2a$12$placeholder-bcrypt-hash-not-real")
                .fullName("Test " + email)
                .emailVerified(true)
                .build();
    }

}

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
import java.util.stream.IntStream;
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
 * Trip days through every layer against real MySQL: JWT → @tripPermission → TripService / TripDayService →
 * Flyway schema. Hibernate statistics are switched on for this class only, to count the SQL statements behind
 * GET /trips/{id} and prove the days do not cause N+1 queries (CLAUDE.md section 8).
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class TripDayFlowIntegrationTest {

    private static final String TRIPS_URL = "/api/v1/trips";

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
    private String strangerBearer;

    @BeforeEach
    void createUsers() {
        ownerBearer = bearerFor(userRepository.save(user("owner@example.com")));
        strangerBearer = bearerFor(userRepository.save(user("stranger@example.com")));
    }

    @AfterEach
    void cleanUp() {
        // trip_days go with their trips (ON DELETE CASCADE); trips must go before users (no cascade on owner)
        jdbcTemplate.update("DELETE FROM trips");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void daysFollowTheTripThroughCreateTitleShiftExtendAndCut() {
        long tripId = createTrip("2026-10-01", "2026-10-03");

        // Created with the trip: Ngày 1..3
        List<Long> dayIds = dayIds(tripId);
        assertThat(dayIndexesAndDates(tripId)).containsExactly("1:2026-10-01", "2:2026-10-02", "3:2026-10-03");

        patchDay(ownerBearer, tripId, dayIds.get(0), "A");
        patchDay(ownerBearer, tripId, dayIds.get(1), "B");
        patchDay(ownerBearer, tripId, dayIds.get(2), "C");

        // Same length, one week later → the whole block moves, titles and ids kept
        assertThat(patchTrip(tripId, """
                { "startDate": "2026-10-08", "endDate": "2026-10-10" }
                """)).hasStatusOk();
        assertThat(titlesByDate(tripId)).containsExactly("2026-10-08=A", "2026-10-09=B", "2026-10-10=C");
        assertThat(dayIds(tripId)).containsExactlyElementsOf(dayIds);

        // Longer → kept by calendar date, two empty days appended
        assertThat(patchTrip(tripId, """
                { "endDate": "2026-10-12" }
                """)).hasStatusOk();
        assertThat(titlesByDate(tripId)).containsExactly(
                "2026-10-08=A", "2026-10-09=B", "2026-10-10=C", "2026-10-11=null", "2026-10-12=null");

        // Cut the start → C stays on its date and becomes Ngày 1
        assertThat(patchTrip(tripId, """
                { "startDate": "2026-10-10" }
                """)).hasStatusOk();
        assertThat(dayIndexesAndDates(tripId)).containsExactly("1:2026-10-10", "2:2026-10-11", "3:2026-10-12");
        assertThat(titlesByDate(tripId)).startsWith("2026-10-10=C");

        // The detail endpoint shows the same days
        assertThat(mvc.get().uri(TRIPS_URL + "/" + tripId).header(HttpHeaders.AUTHORIZATION, ownerBearer))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        { "data": { "startDate": "2026-10-10",
                                    "days": [ { "dayIndex": 1, "date": "2026-10-10", "title": "C" },
                                              { "dayIndex": 2, "date": "2026-10-11" },
                                              { "dayIndex": 3, "date": "2026-10-12" } ] } }
                        """);
    }

    @Test
    void tripDetailCostsTheSameNumberOfQueriesForTwoDaysAndForSixtyDays() {
        long shortTrip = createTrip("2026-10-01", "2026-10-02");
        long longTrip = createTrip("2026-10-01", "2026-11-29");

        long shortQueries = statementsFor(shortTrip);
        long longQueries = statementsFor(longTrip);

        // permission check + trip + days: independent of the number of days, i.e. no N+1
        assertThat(shortQueries).isEqualTo(3);
        assertThat(longQueries).isEqualTo(shortQueries);
    }

    @Test
    void rejectedTripCreatesNoDays() {
        assertThat(mvc.post().uri(TRIPS_URL).header(HttpHeaders.AUTHORIZATION, ownerBearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "title": "Quá dài", "startDate": "2026-10-01", "endDate": "2026-11-30" }
                        """))
                .hasStatus(HttpStatus.BAD_REQUEST);

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM trip_days", Integer.class)).isZero();
    }

    @Test
    void strangerCannotReadOrEditTheDays() {
        long tripId = createTrip("2026-10-01", "2026-10-02");
        long dayId = firstDayId(tripId);

        assertThat(listDays(strangerBearer, tripId)).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(patchDay(strangerBearer, tripId, dayId, "Sửa trộm")).hasStatus(HttpStatus.FORBIDDEN);

        String title = jdbcTemplate.queryForObject("SELECT title FROM trip_days WHERE id = ?", String.class, dayId);
        assertThat(title).isNull();
    }

    @Test
    void dayOfAnotherTripCannotBeEditedThroughTheWrongUrl() {
        long tripA = createTrip("2026-10-01", "2026-10-01");
        long tripB = createTrip("2026-11-01", "2026-11-01");
        long dayOfB = firstDayId(tripB);

        // Owner of both, permission on A passes, but the day belongs to B
        assertThat(patchDay(ownerBearer, tripA, dayOfB, "Nhầm chuyến"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    void daysOfADeletedTripAreGone() {
        long tripId = createTrip("2026-10-01", "2026-10-02");
        long dayId = firstDayId(tripId);
        assertThat(mvc.delete().uri(TRIPS_URL + "/" + tripId).header(HttpHeaders.AUTHORIZATION, ownerBearer))
                .hasStatusOk();

        assertThat(listDays(ownerBearer, tripId)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(patchDay(ownerBearer, tripId, dayId, "Sau khi xoá")).hasStatus(HttpStatus.NOT_FOUND);
        // Soft delete: the rows are still there, ready if the trip is ever restored
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM trip_days WHERE trip_id = ?", Integer.class, tripId)).isEqualTo(2);
    }

    private long statementsFor(long tripId) {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        assertThat(mvc.get().uri(TRIPS_URL + "/" + tripId).header(HttpHeaders.AUTHORIZATION, ownerBearer))
                .hasStatusOk();
        return statistics.getPrepareStatementCount();
    }

    private long createTrip(String start, String end) {
        MvcTestResult result = mvc.post().uri(TRIPS_URL).header(HttpHeaders.AUTHORIZATION, ownerBearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "title": "Chuyến đi", "startDate": "%s", "endDate": "%s" }
                        """.formatted(start, end))
                .exchange();
        assertThat(result).hasStatus(HttpStatus.CREATED);
        return ((Number) JsonPath.read(body(result), "$.data.id")).longValue();
    }

    private MvcTestResult patchTrip(long tripId, String json) {
        return mvc.patch().uri(TRIPS_URL + "/" + tripId).header(HttpHeaders.AUTHORIZATION, ownerBearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .exchange();
    }

    private MvcTestResult listDays(String bearer, long tripId) {
        return mvc.get().uri(TRIPS_URL + "/" + tripId + "/days").header(HttpHeaders.AUTHORIZATION, bearer).exchange();
    }

    private MvcTestResult patchDay(String bearer, long tripId, long dayId, String title) {
        return mvc.patch().uri(TRIPS_URL + "/" + tripId + "/days/" + dayId).header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "title": "%s" }
                        """.formatted(title))
                .exchange();
    }

    private long firstDayId(long tripId) {
        return dayIds(tripId).getFirst();
    }

    // JsonPath returns Integer for small numbers, so go through Number instead of declaring List<Long>
    private List<Long> dayIds(long tripId) {
        List<Number> ids = JsonPath.read(body(listDays(ownerBearer, tripId)), "$.data[*].id");
        return ids.stream().map(Number::longValue).toList();
    }

    /** "dayIndex:date" per day, in API order. */
    private List<String> dayIndexesAndDates(long tripId) {
        String json = body(listDays(ownerBearer, tripId));
        List<Integer> indexes = JsonPath.read(json, "$.data[*].dayIndex");
        List<String> dates = JsonPath.read(json, "$.data[*].date");
        return IntStream.range(0, dates.size())
                .mapToObj(i -> indexes.get(i) + ":" + dates.get(i))
                .toList();
    }

    /** "date=title" per day, in API order. */
    private List<String> titlesByDate(long tripId) {
        String json = body(listDays(ownerBearer, tripId));
        List<String> dates = JsonPath.read(json, "$.data[*].date");
        List<String> titles = JsonPath.read(json, "$.data[*].title");
        return IntStream.range(0, dates.size())
                .mapToObj(i -> dates.get(i) + "=" + titles.get(i))
                .toList();
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

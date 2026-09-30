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
 * Reorder (drag and drop) through every layer against real MySQL: JWT → @tripPermission → ActivityService →
 * Flyway schema. What only this level can prove:
 * <ul>
 *   <li>a rejected batch leaves every position exactly as it was (transaction rollback);</li>
 *   <li>the renumbering of a crowded day reaches the database, not just the response;</li>
 *   <li>moving an activity never increments its version (@OptimisticLock exclusion on a real UPDATE);</li>
 *   <li>time conflicts on a move are compared as typed, whatever the time zone of the JVM;</li>
 *   <li>the number of SQL statements does not grow with the number of activities in the day.</li>
 * </ul>
 * Hibernate statistics are switched on for this class only.
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ActivityReorderFlowIntegrationTest {

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

    /** A trip 01/10 → 03/10 of the owner, created before every test. */
    private long tripId;
    private List<Long> dayIds;
    private String reorderUrl;

    @BeforeEach
    void createUsersAndTrip() {
        ownerBearer = bearerFor(userRepository.save(user("owner@example.com")));
        strangerBearer = bearerFor(userRepository.save(user("stranger@example.com")));
        tripId = createTrip("Đà Lạt", "2026-10-01", "2026-10-03");
        dayIds = dayIds(tripId);
        reorderUrl = TRIPS_URL + "/" + tripId + "/activities/reorder";
    }

    @AfterEach
    void cleanUp() {
        // trip_days and activities go with their trips (ON DELETE CASCADE); trips must go before users (no cascade)
        jdbcTemplate.update("DELETE FROM trips");
        jdbcTemplate.update("DELETE FROM users");
    }

    // ---- moves inside a day and across days -----------------------------------------------------------------------

    @Test
    void ownerReordersInsideADayAndAcrossDaysAndTheDetailFollows() {
        long a = add(0, "A");
        long b = add(0, "B");
        long c = add(0, "C");
        long x = add(1, "X");

        // C is dropped between A and B: only C changes
        MvcTestResult inside = reorder(ownerBearer, "", item(c, day(0), 1500));
        assertThat(inside).hasStatusOk();
        assertThat(JsonPath.<List<String>>read(body(inside), "$.data[*].id")).hasSize(1);
        assertThat(JsonPath.<List<String>>read(body(inside), "$.data[0].activities[*].title"))
                .containsExactly("A", "C", "B");
        assertThat(placement(day(0))).containsExactly("A=1000", "C=1500", "B=2000");

        // A goes to the top of day 2, B moves to the top of day 1, in one request
        MvcTestResult across = reorder(ownerBearer, "", item(a, day(1), 500), item(b, day(0), 1000));
        assertThat(across).hasStatusOk();
        assertThat(JsonPath.<List<Number>>read(body(across), "$.data[*].id"))
                .extracting(Number::longValue).containsExactly(day(0), day(1));
        assertThat(JsonPath.<List<String>>read(body(across), "$.data[0].activities[*].title")).containsExactly("B", "C");
        assertThat(JsonPath.<List<String>>read(body(across), "$.data[1].activities[*].title")).containsExactly("A", "X");
        assertThat(placement(day(0))).containsExactly("B=1000", "C=1500");
        assertThat(placement(day(1))).containsExactly("A=500", "X=1000");

        // the day that loses its last activity is still reported, empty
        MvcTestResult emptied = reorder(ownerBearer, "", item(b, day(2), 1000), item(c, day(2), 2000));
        assertThat(emptied).hasStatusOk();
        assertThat(JsonPath.<List<Object>>read(body(emptied), "$.data[0].activities")).isEmpty();
        assertThat(placement(day(0))).isEmpty();

        // the trip detail shows the same picture
        String detail = body(send("GET", TRIPS_URL + "/" + tripId, ownerBearer, null));
        assertThat(JsonPath.<List<String>>read(detail, "$.data.days[0].activities[*].title")).isEmpty();
        assertThat(JsonPath.<List<String>>read(detail, "$.data.days[1].activities[*].title")).containsExactly("A", "X");
        assertThat(JsonPath.<List<String>>read(detail, "$.data.days[2].activities[*].title")).containsExactly("B", "C");

        // moving is not editing: no version was bumped, content is untouched
        assertThat(jdbcTemplate.queryForObject("SELECT MAX(version) FROM activities", Long.class)).isZero();
        assertThat(jdbcTemplate.queryForList("SELECT title FROM activities ORDER BY id", String.class))
                .containsExactly("A", "B", "C", "X");
        assertThat(x).isPositive();
    }

    // ---- all or nothing ------------------------------------------------------------------------------------------

    @Test
    void aRejectedBatchLeavesEveryPositionAsItWas() {
        long a = add(0, "A");
        long b = add(0, "B");
        long otherTripId = createTrip("Huế", "2026-11-01", "2026-11-01");
        long otherDay = dayIds(otherTripId).getFirst();
        List<String> before = placement(day(0));

        // valid first item, then: unknown activity / activity of another trip / day of another trip / duplicate
        assertThat(reorder(ownerBearer, "", item(a, day(0), 9000), item(999_999L, day(0), 100)))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(reorder(ownerBearer, "", item(a, day(0), 9000), item(b, otherDay, 100)))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(reorder(ownerBearer, "", item(a, day(0), 9000), item(a, day(1), 100)))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.details[0].field").isEqualTo("items");
        assertThat(reorder(ownerBearer, "", item(a, day(0), 9000), item(b, day(0), 0)))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.details[0].field").isEqualTo("items[1].orderIndex");

        // through the other trip's URL the owner has permission, but the activity is not there
        assertThat(mvc.put().uri(TRIPS_URL + "/" + otherTripId + "/activities/reorder")
                .header(HttpHeaders.AUTHORIZATION, ownerBearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(items(item(a, otherDay, 100))))
                .hasStatus(HttpStatus.NOT_FOUND);

        assertThat(reorder(strangerBearer, "", item(a, day(0), 9000))).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.put().uri(reorderUrl).contentType(MediaType.APPLICATION_JSON).content(items(item(a, day(0), 9000))))
                .hasStatus(HttpStatus.UNAUTHORIZED);

        assertThat(placement(day(0))).isEqualTo(before);
        assertThat(jdbcTemplate.queryForObject("SELECT MAX(version) FROM activities", Long.class)).isZero();
    }

    @Test
    void activitiesOfADeletedTripCannotBeReordered() {
        long a = add(0, "A");
        assertThat(send("DELETE", TRIPS_URL + "/" + tripId, ownerBearer, null)).hasStatusOk();

        assertThat(reorder(ownerBearer, "", item(a, day(0), 500))).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(placement(day(0))).containsExactly("A=1000");
    }

    // ---- rule 14.4 on a move -------------------------------------------------------------------------------------

    @Test
    void movingIntoAnotherDayIsCheckedForOverlapAsTypedNotShiftedByTheTimeZone() {
        long dawn = addTimed(0, "Săn mây", "03:00", "04:00");
        long lunch = addTimed(0, "Ăn trưa", "11:30", "13:00");
        long sunrise = addTimed(1, "Ngắm bình minh", "03:30", "05:00");
        long coffee = addTimed(1, "Cà phê", "13:00", "13:30");
        long dinner = addTimed(1, "Ăn tối", "20:00", "21:00");
        List<String> before = placement(day(0), day(1));

        // 03:30–05:00 arriving next to 03:00–04:00: blocked, and the valid first item is not applied either
        MvcTestResult blocked = reorder(ownerBearer, "", item(coffee, day(0), 500), item(sunrise, day(0), 700));
        assertThat(blocked).hasStatus(HttpStatus.CONFLICT)
                .bodyJson().isLenientlyEqualTo("""
                        { "errorCode": "ACTIVITY_TIME_CONFLICT",
                          "details": [ { "field": "items[1].dayId" } ] }
                        """);
        assertThat(blocked).bodyJson().extractingPath("$.details[0].message").isEqualTo(
                "Hoạt động \"Ngắm bình minh\" trùng giờ với hoạt động \"Săn mây\" (03:00 - 04:00) trong ngày mới");
        assertThat(placement(day(0), day(1))).isEqualTo(before);

        // 20:00–21:00 is what a +07:00 shift would turn 03:00–04:00 into: it must NOT be seen as a conflict
        assertThat(reorder(ownerBearer, "", item(dinner, day(0), 5000))).hasStatusOk();
        // touching 11:30–13:00 is fine
        assertThat(reorder(ownerBearer, "", item(coffee, day(0), 4000))).hasStatusOk();
        // confirmed by the user
        assertThat(reorder(ownerBearer, "?allowOverlap=true", item(sunrise, day(0), 700))).hasStatusOk();
        // inside the day nothing is checked, although dawn and sunrise overlap now
        assertThat(reorder(ownerBearer, "", item(sunrise, day(0), 100), item(dawn, day(0), 200))).hasStatusOk();

        assertThat(placement(day(0))).containsExactly(
                "Ngắm bình minh=100", "Săn mây=200", "Ăn trưa=2000", "Cà phê=4000", "Ăn tối=5000");
        assertThat(lunch).isPositive();
    }

    @Test
    void twoOverlappingActivitiesCanSwapDaysInOneRequest() {
        long lunch = addTimed(0, "Ăn trưa", "11:30", "13:00");
        long coffee = addTimed(1, "Cà phê", "12:00", "12:30");

        assertThat(reorder(ownerBearer, "", item(lunch, day(1), 1000), item(coffee, day(0), 1000))).hasStatusOk();

        assertThat(placement(day(0))).containsExactly("Cà phê=1000");
        assertThat(placement(day(1))).containsExactly("Ăn trưa=1000");
    }

    // ---- rule 14.5 -----------------------------------------------------------------------------------------------

    @Test
    void aCrowdedDayIsRenumberedInTheDatabaseNotOnlyInTheResponse() {
        long a = add(0, "A");
        long b = add(0, "B");
        long c = add(0, "C");

        // C keeps landing between A and B: the gap halves each time until it is below 10
        for (int index : new int[] {1500, 1250, 1125, 1062, 1031, 1015}) {
            assertThat(reorder(ownerBearer, "", item(c, day(0), index))).hasStatusOk();
        }
        assertThat(placement(day(0))).containsExactly("A=1000", "C=1015", "B=2000");

        MvcTestResult crowded = reorder(ownerBearer, "", item(c, day(0), 1007));
        assertThat(crowded).hasStatusOk();
        assertThat(JsonPath.<List<Integer>>read(body(crowded), "$.data[0].activities[*].orderIndex"))
                .containsExactly(1000, 2000, 3000);
        assertThat(placement(day(0))).containsExactly("A=1000", "C=2000", "B=3000");
        // renumbering is a move too: still no version bump on any row
        assertThat(jdbcTemplate.queryForObject("SELECT MAX(version) FROM activities", Long.class)).isZero();
        assertThat(a + b).isPositive();
    }

    // ---- no N+1 --------------------------------------------------------------------------------------------------

    @Test
    void reorderCostsTheSameNumberOfQueriesWhateverTheSizeOfTheDay() {
        long first = add(0, "Đầu");
        long second = add(0, "Cuối");
        long smallDay = statementsFor(item(second, day(0), 500));

        for (int n = 1; n <= 20; n++) {
            add(0, "Thêm " + n);
        }
        long bigDay = statementsFor(item(second, day(0), 30_000));

        // permission + trip exists + activities of the batch + days + UPDATE of the moved row + activities of the day
        assertThat(smallDay).isEqualTo(6);
        assertThat(bigDay).isEqualTo(smallDay);
        assertThat(first).isPositive();
    }

    // ---- helpers -------------------------------------------------------------------------------------------------

    private long statementsFor(String item) {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        assertThat(reorder(ownerBearer, "", item)).hasStatusOk();
        return statistics.getPrepareStatementCount();
    }

    private MvcTestResult reorder(String bearer, String query, String... items) {
        return mvc.put().uri(reorderUrl + query).header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(items(items))
                .exchange();
    }

    private long day(int position) {
        return dayIds.get(position);
    }

    private long add(int dayPosition, String title) {
        return id(send("POST", TRIPS_URL + "/" + tripId + "/days/" + day(dayPosition) + "/activities", ownerBearer,
                "{ \"title\": \"" + title + "\" }"));
    }

    private long addTimed(int dayPosition, String title, String start, String end) {
        return id(send("POST", TRIPS_URL + "/" + tripId + "/days/" + day(dayPosition) + "/activities", ownerBearer,
                "{ \"title\": \"%s\", \"startTime\": \"%s\", \"endTime\": \"%s\" }".formatted(title, start, end)));
    }

    /** "title=order_index" per activity of the given days, as MySQL holds them, in display order. */
    private List<String> placement(long... ofDays) {
        String placeholders = String.join(",", java.util.Collections.nCopies(ofDays.length, "?"));
        Object[] args = java.util.Arrays.stream(ofDays).boxed().toArray();
        return jdbcTemplate.query("""
                SELECT title, order_index FROM activities WHERE trip_day_id IN (%s)
                ORDER BY trip_day_id, order_index, id""".formatted(placeholders),
                (rs, row) -> rs.getString("title") + "=" + rs.getInt("order_index"), args);
    }

    private long createTrip(String title, String start, String end) {
        MvcTestResult result = send("POST", TRIPS_URL, ownerBearer, """
                { "title": "%s", "startDate": "%s", "endDate": "%s" }
                """.formatted(title, start, end));
        assertThat(result).hasStatus(HttpStatus.CREATED);
        return id(result);
    }

    // JsonPath returns Integer for small numbers, so go through Number instead of declaring List<Long>
    private List<Long> dayIds(long ofTripId) {
        List<Number> ids = JsonPath.read(
                body(send("GET", TRIPS_URL + "/" + ofTripId + "/days", ownerBearer, null)), "$.data[*].id");
        return ids.stream().map(Number::longValue).toList();
    }

    private MvcTestResult send(String method, String url, String bearer, String json) {
        var request = switch (method) {
            case "POST" -> mvc.post();
            case "DELETE" -> mvc.delete();
            default -> mvc.get();
        };
        var prepared = request.uri(url).header(HttpHeaders.AUTHORIZATION, bearer);
        if (json != null) {
            prepared = prepared.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        MvcTestResult result = prepared.exchange();
        if ("POST".equals(method)) {
            assertThat(result).hasStatus(HttpStatus.CREATED);
        }
        return result;
    }

    private static String items(String... items) {
        return "{ \"items\": [" + String.join(",", items) + "] }";
    }

    private static String item(long activityId, long dayId, int orderIndex) {
        return "{ \"activityId\": %d, \"dayId\": %d, \"orderIndex\": %d }".formatted(activityId, dayId, orderIndex);
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

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
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Activities through every layer against real MySQL: JWT → @tripPermission → ActivityService / TripService →
 * Flyway schema. What only this level can prove:
 * <ul>
 *   <li>times reach the database as typed, whatever the time zone of the JVM (BUG-ACT-001, BUG-ACT-002);</li>
 *   <li>a rejected request really leaves nothing behind (transaction rollback);</li>
 *   <li>cutting a day removes its activities through the foreign key, and only when the user forced it;</li>
 *   <li>the number of SQL statements does not grow with the number of activities (CLAUDE.md section 8).</li>
 * </ul>
 * Hibernate statistics are switched on for this class only.
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ActivityFlowIntegrationTest {

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

    private Long ownerId;
    private String ownerBearer;
    private String strangerBearer;

    /** A trip 01/10 → 03/10 of the owner, created before every test. */
    private long tripId;
    private List<Long> dayIds;

    @BeforeEach
    void createUsersAndTrip() {
        User owner = userRepository.save(user("owner@example.com"));
        ownerId = owner.getId();
        ownerBearer = bearerFor(owner);
        strangerBearer = bearerFor(userRepository.save(user("stranger@example.com")));

        tripId = createTrip("Đà Lạt", "2026-10-01", "2026-10-03", "USD");
        dayIds = dayIds(tripId);
    }

    @AfterEach
    void cleanUp() {
        // trip_days and activities go with their trips (ON DELETE CASCADE); trips must go before users (no cascade)
        jdbcTemplate.update("DELETE FROM trips");
        jdbcTemplate.update("DELETE FROM users");
    }

    // ---- the whole life of an activity ---------------------------------------------------------------------------

    @Test
    void ownerAddsListsEditsAndDeletesActivities() {
        MvcTestResult created = send("POST", dayUrl(0), ownerBearer, """
                { "title": "  Săn mây  ", "type": "SIGHTSEEING", "startTime": "03:00", "endTime": "08:15",
                  "note": "Mang áo ấm", "costAmount": 12.5, "bookingUrl": "https://example.com/tour/1" }
                """);
        assertThat(created).hasStatus(HttpStatus.CREATED)
                .bodyJson().isLenientlyEqualTo("""
                        { "success": true,
                          "data": { "title": "Săn mây", "type": "SIGHTSEEING", "startTime": "03:00",
                                    "endTime": "08:15", "orderIndex": 1000, "note": "Mang áo ấm", "costAmount": 12.5,
                                    "currency": "USD", "bookingUrl": "https://example.com/tour/1", "version": 0 } }
                        """);
        long first = id(created);
        assertThat(created).bodyJson().extractingPath("$.data.dayId").isEqualTo(dayIds.get(0).intValue());
        assertThat(created).bodyJson().extractingPath("$.data.createdById").isEqualTo(ownerId.intValue());
        // 03:00 on a +07:00 machine is the case a time zone shift would turn into 20:00 of the day before
        assertThat(row(first)).containsEntry("s", "03:00:00").containsEntry("e", "08:15:00");

        long second = id(send("POST", dayUrl(0), ownerBearer, """
                { "title": "Ăn trưa" }
                """));
        long onDayTwo = id(send("POST", dayUrl(1), ownerBearer, """
                { "title": "Chợ đêm" }
                """));

        // list of one day: display order, other days left out
        assertThat(titles(send("GET", dayUrl(0), ownerBearer, null))).containsExactly("Săn mây", "Ăn trưa");
        assertThat(row(second)).containsEntry("order_index", 2000).containsEntry("type", "OTHER");

        // partial update: only what is sent changes, "" clears the note, the version goes up
        MvcTestResult updated = send("PATCH", activityUrl(first), ownerBearer, """
                { "title": "Săn mây Cầu Đất", "note": "", "endTime": "07:30" }
                """);
        assertThat(updated).hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        { "data": { "title": "Săn mây Cầu Đất", "note": null, "startTime": "03:00", "endTime": "07:30",
                                    "type": "SIGHTSEEING", "costAmount": 12.5, "currency": "USD", "orderIndex": 1000,
                                    "version": 1 } }
                        """);
        assertThat(row(first)).containsEntry("s", "03:00:00").containsEntry("e", "07:30:00");

        // the trip detail shows every activity under its own day
        String detail = body(send("GET", TRIPS_URL + "/" + tripId, ownerBearer, null));
        assertThat(JsonPath.<List<String>>read(detail, "$.data.days[0].activities[*].title"))
                .containsExactly("Săn mây Cầu Đất", "Ăn trưa");
        assertThat(JsonPath.<List<String>>read(detail, "$.data.days[1].activities[*].title"))
                .containsExactly("Chợ đêm");
        assertThat(JsonPath.<List<Object>>read(detail, "$.data.days[2].activities")).isEmpty();

        // hard delete: the row is gone, the others keep their index, the next one goes to the end
        assertThat(send("DELETE", activityUrl(first), ownerBearer, null)).hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        { "success": true, "data": null }
                        """);
        assertThat(countActivities("id = " + first)).isZero();
        assertThat(send("DELETE", activityUrl(first), ownerBearer, null)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(row(second)).containsEntry("order_index", 2000);
        long third = id(send("POST", dayUrl(0), ownerBearer, """
                { "title": "Cà phê" }
                """));
        assertThat(row(third)).containsEntry("order_index", 3000);
        assertThat(row(onDayTwo)).containsEntry("title", "Chợ đêm");
    }

    // ---- rule 14.5, placement by start time (Task 2.6) --------------------------------------------------------

    @Test
    void startTimePlacesTheActivityAndOtherEditsLeaveItWhereItIs() {
        long morning = id(send("POST", dayUrl(0), ownerBearer, """
                { "title": "Ăn sáng", "startTime": "08:00" }
                """));
        send("POST", dayUrl(0), ownerBearer, """
                { "title": "Dạo phố" }
                """);
        // nothing starts later than 12:00: to the end of the day, the untimed one stays after "Ăn sáng"
        long noon = id(send("POST", dayUrl(0), ownerBearer, """
                { "title": "Ăn trưa", "startTime": "12:00" }
                """));
        assertThat(titles(send("GET", dayUrl(0), ownerBearer, null)))
                .containsExactly("Ăn sáng", "Dạo phố", "Ăn trưa");

        // created last, shown before the first activity that starts later; the untimed one is skipped over
        assertThat(send("POST", dayUrl(0), ownerBearer, """
                { "title": "Bảo tàng", "startTime": "10:00", "endTime": "11:00" }
                """)).hasStatus(HttpStatus.CREATED);
        assertThat(titles(send("GET", dayUrl(0), ownerBearer, null)))
                .containsExactly("Ăn sáng", "Dạo phố", "Bảo tàng", "Ăn trưa");

        // a new start time moves it; the untimed activity keeps its place after "Ăn sáng"
        assertThat(send("PATCH", activityUrl(noon), ownerBearer, """
                { "startTime": "07:00" }
                """)).hasStatusOk();
        assertThat(titles(send("GET", dayUrl(0), ownerBearer, null)))
                .containsExactly("Ăn trưa", "Ăn sáng", "Dạo phố", "Bảo tàng");

        // after a drag, 08:00 above 07:00 is out of time order; a new name keeps it there
        assertThat(send("PUT", TRIPS_URL + "/" + tripId + "/activities/reorder", ownerBearer, """
                { "items": [ { "activityId": %d, "dayId": %d, "orderIndex": 1 } ] }
                """.formatted(morning, dayIds.get(0)))).hasStatusOk();
        assertThat(send("PATCH", activityUrl(morning), ownerBearer, """
                { "title": "Ăn sáng sớm", "startTime": "08:00" }
                """)).hasStatusOk();
        assertThat(titles(send("GET", dayUrl(0), ownerBearer, null)))
                .containsExactly("Ăn sáng sớm", "Ăn trưa", "Dạo phố", "Bảo tàng");
    }

    // ---- rule 14.4 -----------------------------------------------------------------------------------------------

    @Test
    void overlappingTimesAreRejectedUnlessTheUserAllowsThem() {
        long breakfast = id(send("POST", dayUrl(0), ownerBearer, timed("Ăn sáng", "09:00", "10:00")));

        assertThat(send("POST", dayUrl(0), ownerBearer, timed("Cà phê", "09:30", "10:30")))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().isLenientlyEqualTo("""
                        { "success": false, "errorCode": "ACTIVITY_TIME_CONFLICT",
                          "message": "Hoạt động bị trùng giờ với một hoạt động khác trong ngày",
                          "details": [ { "field": "startTime" } ] }
                        """);
        assertThat(countActivities("title = 'Cà phê'")).isZero();

        // touching ranges, another day, and an activity without an end time are all fine
        long market = id(send("POST", dayUrl(0), ownerBearer, timed("Đi chợ", "10:00", "11:00")));
        assertThat(send("POST", dayUrl(1), ownerBearer, timed("Ngày khác", "09:30", "10:30")))
                .hasStatus(HttpStatus.CREATED);
        assertThat(send("POST", dayUrl(0), ownerBearer, """
                { "title": "Chỉ có giờ bắt đầu", "startTime": "09:15" }
                """)).hasStatus(HttpStatus.CREATED);

        // confirmed by the user
        assertThat(send("POST", dayUrl(0) + "?allowOverlap=true", ownerBearer, timed("Cà phê", "09:30", "10:30")))
                .hasStatus(HttpStatus.CREATED);

        // early morning: compared as typed, not shifted by the time zone (BUG-ACT-002)
        assertThat(send("POST", dayUrl(0), ownerBearer, timed("Săn mây", "03:00", "04:00")))
                .hasStatus(HttpStatus.CREATED);
        MvcTestResult early = send("POST", dayUrl(0), ownerBearer, timed("Ngắm bình minh", "03:30", "05:00"));
        assertThat(early).hasStatus(HttpStatus.CONFLICT);
        assertThat(early).bodyJson().extractingPath("$.details[0].message")
                .isEqualTo("Trùng giờ với hoạt động \"Săn mây\" (03:00 - 04:00)");
        MvcTestResult dinnerCreated = send("POST", dayUrl(0), ownerBearer, timed("Ăn tối", "20:00", "21:00"));
        assertThat(dinnerCreated).hasStatus(HttpStatus.CREATED);
        long dinner = id(dinnerCreated);

        // editing: moving into another activity is refused and leaves the row exactly as it was
        Map<String, Object> before = row(market);
        assertThat(send("PATCH", activityUrl(market), ownerBearer, """
                { "title": "Không được lưu", "startTime": "09:45" }
                """)).hasStatus(HttpStatus.CONFLICT);
        assertThat(row(market)).isEqualTo(before);

        // ...a changed range is checked again, even against a neighbour it already overlapped: "Cà phê"
        // (09:30–10:30) was added over "Đi chợ" (10:00–11:00) with allowOverlap, that gives no free pass later
        assertThat(send("PATCH", activityUrl(market), ownerBearer, """
                { "endTime": "11:30" }
                """)).hasStatus(HttpStatus.CONFLICT);
        assertThat(row(market)).isEqualTo(before);

        // ...an activity is never its own conflict: 20:00–21:00 may grow to 20:00–21:30
        assertThat(send("PATCH", activityUrl(dinner), ownerBearer, """
                { "endTime": "21:30" }
                """)).hasStatusOk();
        assertThat(row(dinner)).containsEntry("s", "20:00:00").containsEntry("e", "21:30:00");

        // ...and an unchanged range is not checked at all, so an overlapping activity can still be renamed
        assertThat(send("PATCH", activityUrl(breakfast), ownerBearer, """
                { "title": "Ăn sáng muộn" }
                """)).hasStatusOk();

        assertThat(send("PATCH", activityUrl(market) + "?allowOverlap=true", ownerBearer, """
                { "startTime": "09:45" }
                """)).hasStatusOk();
        assertThat(row(market)).containsEntry("s", "09:45:00").containsEntry("e", "11:00:00");
    }

    @Test
    void timeRulesAnswer400WithVietnameseFieldDetailsAndSaveNothing() {
        assertThat(send("POST", dayUrl(0), ownerBearer, timed("Sai giờ", "10:00", "09:00")))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        { "errorCode": "VALIDATION_ERROR",
                          "details": [ { "field": "endTime", "message": "Giờ kết thúc phải sau giờ bắt đầu" } ] }
                        """);
        assertThat(send("POST", dayUrl(0), ownerBearer, """
                { "title": "Thiếu giờ bắt đầu", "endTime": "09:00" }
                """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        { "details": [ { "field": "startTime",
                                         "message": "Cần nhập giờ bắt đầu khi đã có giờ kết thúc" } ] }
                        """);
        assertThat(send("POST", dayUrl(0), ownerBearer, """
                { "title": "   ", "costAmount": -1 }
                """)).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(countActivities("1 = 1")).isZero();

        // on update the rule is checked against the stored value
        long activity = id(send("POST", dayUrl(0), ownerBearer, timed("Ăn sáng", "09:00", "10:00")));
        assertThat(send("PATCH", activityUrl(activity), ownerBearer, """
                { "endTime": "08:00" }
                """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.details[0].field").isEqualTo("endTime");
        assertThat(row(activity)).containsEntry("e", "10:00:00").containsEntry("version", 0L);
    }

    // ---- permission and wrong URLs -------------------------------------------------------------------------------

    @Test
    void strangerAndAnonymousCannotReadAddEditOrDelete() {
        long activity = id(send("POST", dayUrl(0), ownerBearer, timed("Ăn sáng", "09:00", "10:00")));
        Map<String, Object> before = row(activity);

        assertThat(send("GET", dayUrl(0), strangerBearer, null)).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(send("POST", dayUrl(0), strangerBearer, """
                { "title": "Thêm trộm" }
                """)).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(send("PATCH", activityUrl(activity), strangerBearer, """
                { "title": "Sửa trộm" }
                """)).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(send("DELETE", activityUrl(activity), strangerBearer, null)).hasStatus(HttpStatus.FORBIDDEN);

        assertThat(mvc.get().uri(dayUrl(0))).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.delete().uri(activityUrl(activity))).hasStatus(HttpStatus.UNAUTHORIZED);

        assertThat(row(activity)).isEqualTo(before);
        assertThat(countActivities("1 = 1")).isEqualTo(1);
    }

    @Test
    void activityAndDayCannotBeReachedThroughAnotherTrip() {
        long otherTripId = createTrip("Huế", "2026-11-01", "2026-11-01", "VND");
        long otherDay = dayIds(otherTripId).getFirst();
        long activity = id(send("POST", dayUrl(0), ownerBearer, timed("Ăn sáng", "09:00", "10:00")));
        Map<String, Object> before = row(activity);

        // The owner has permission on both trips, so only the "belongs to this trip" check can stop these
        String wrongDayUrl = TRIPS_URL + "/" + tripId + "/days/" + otherDay + "/activities";
        String wrongActivityUrl = TRIPS_URL + "/" + otherTripId + "/activities/" + activity;
        assertThat(send("POST", wrongDayUrl, ownerBearer, """
                { "title": "Nhầm ngày" }
                """)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send("GET", wrongDayUrl, ownerBearer, null)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send("PATCH", wrongActivityUrl, ownerBearer, """
                { "title": "Nhầm chuyến" }
                """))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(send("DELETE", wrongActivityUrl, ownerBearer, null)).hasStatus(HttpStatus.NOT_FOUND);

        assertThat(row(activity)).isEqualTo(before);
        assertThat(countActivities("1 = 1")).isEqualTo(1);
    }

    @Test
    void activitiesOfADeletedTripAreUnreachableButStillStored() {
        long activity = id(send("POST", dayUrl(0), ownerBearer, timed("Ăn sáng", "09:00", "10:00")));
        assertThat(send("DELETE", TRIPS_URL + "/" + tripId, ownerBearer, null)).hasStatusOk();

        assertThat(send("GET", dayUrl(0), ownerBearer, null)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send("POST", dayUrl(0), ownerBearer, """
                { "title": "Sau khi xoá" }
                """)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send("PATCH", activityUrl(activity), ownerBearer, """
                { "title": "Sau khi xoá" }
                """)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send("DELETE", activityUrl(activity), ownerBearer, null)).hasStatus(HttpStatus.NOT_FOUND);

        // Soft delete of the trip: the activity is still there, ready if the trip is ever restored
        assertThat(row(activity)).containsEntry("title", "Ăn sáng");
    }

    // ---- rule 14.3 -----------------------------------------------------------------------------------------------

    @Test
    void shorteningATripIsBlockedUntilForcedAndARejectedRequestChangesNothing() {
        send("POST", dayUrl(0), ownerBearer, timed("Ăn sáng", "09:00", "10:00"));
        send("POST", dayUrl(2), ownerBearer, timed("Chợ đêm", "19:00", "21:00"));
        send("POST", dayUrl(2), ownerBearer, timed("Ăn tối", "21:00", "22:00"));
        String tripUrl = TRIPS_URL + "/" + tripId;
        Map<String, Object> before = tripRow();

        // blocked: nothing at all may change, not even the title sent in the same request
        assertThat(send("PATCH", tripUrl, ownerBearer, """
                { "title": "Tên không được lưu", "endDate": "2026-10-02" }
                """))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().isLenientlyEqualTo("""
                        { "success": false, "errorCode": "TRIP_DAY_HAS_ACTIVITIES",
                          "message": "Đổi ngày sẽ xoá những ngày đang có hoạt động",
                          "details": [ { "field": "force",
                                         "message": "2 hoạt động trong 1 ngày sẽ bị xoá nếu đổi ngày" } ] }
                        """);
        assertThat(tripRow()).isEqualTo(before);
        assertThat(dayIds(tripId)).containsExactlyElementsOf(dayIds);
        assertThat(activitiesByDate()).containsExactly(
                "2026-10-01=Ăn sáng", "2026-10-03=Chợ đêm", "2026-10-03=Ăn tối");

        // a day without activities can be added and cut again without force
        assertThat(send("PATCH", tripUrl, ownerBearer, """
                { "endDate": "2026-10-04" }
                """)).hasStatusOk();
        assertThat(send("PATCH", tripUrl, ownerBearer, """
                { "endDate": "2026-10-03" }
                """)).hasStatusOk();

        // shifting the whole trip cuts nothing: not blocked, the activities follow their day
        assertThat(send("PATCH", tripUrl, ownerBearer, """
                { "startDate": "2026-10-08", "endDate": "2026-10-10" }
                """)).hasStatusOk();
        assertThat(activitiesByDate()).containsExactly(
                "2026-10-08=Ăn sáng", "2026-10-10=Chợ đêm", "2026-10-10=Ăn tối");

        // confirmed by the user: the day goes, and its activities with it
        assertThat(send("PATCH", tripUrl + "?force=true", ownerBearer, """
                { "title": "Đà Lạt 2 ngày", "endDate": "2026-10-09" }
                """))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        { "data": { "title": "Đà Lạt 2 ngày", "startDate": "2026-10-08", "endDate": "2026-10-09" } }
                        """);
        assertThat(activitiesByDate()).containsExactly("2026-10-08=Ăn sáng");
        assertThat(dayIds(tripId)).containsExactly(dayIds.get(0), dayIds.get(1));
    }

    // ---- no N+1 --------------------------------------------------------------------------------------------------

    @Test
    void listOfADayCostsTheSameNumberOfQueriesForZeroAndForManyActivities() {
        long emptyDay = statementsForList(dayUrl(1));

        for (int n = 1; n <= 20; n++) {
            assertThat(send("POST", dayUrl(0), ownerBearer, """
                    { "title": "Hoạt động %d" }
                    """.formatted(n))).hasStatus(HttpStatus.CREATED);
        }
        long busyDay = statementsForList(dayUrl(0));

        // permission check + trip exists + day of trip + activities
        assertThat(emptyDay).isEqualTo(4);
        assertThat(busyDay).isEqualTo(emptyDay);
        assertThat(titles(send("GET", dayUrl(0), ownerBearer, null))).hasSize(20).startsWith("Hoạt động 1", "Hoạt động 2");
    }

    // ---- helpers -------------------------------------------------------------------------------------------------

    private long statementsForList(String url) {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        assertThat(send("GET", url, ownerBearer, null)).hasStatusOk();
        return statistics.getPrepareStatementCount();
    }

    private String dayUrl(int dayPosition) {
        return TRIPS_URL + "/" + tripId + "/days/" + dayIds.get(dayPosition) + "/activities";
    }

    private String activityUrl(long activityId) {
        return TRIPS_URL + "/" + tripId + "/activities/" + activityId;
    }

    private long createTrip(String title, String start, String end, String currency) {
        MvcTestResult result = send("POST", TRIPS_URL, ownerBearer, """
                { "title": "%s", "startDate": "%s", "endDate": "%s", "currency": "%s" }
                """.formatted(title, start, end, currency));
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
            case "PUT" -> mvc.put();
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

    /** The row as MySQL holds it; times are read as text so that no driver or JVM time zone is involved. */
    private Map<String, Object> row(long activityId) {
        return jdbcTemplate.queryForMap("""
                SELECT title, type, CAST(start_time AS CHAR) AS s, CAST(end_time AS CHAR) AS e, order_index, version
                FROM activities WHERE id = ?""", activityId);
    }

    private Map<String, Object> tripRow() {
        return jdbcTemplate.queryForMap("""
                SELECT title, CAST(start_date AS CHAR) AS s, CAST(end_date AS CHAR) AS e, version
                FROM trips WHERE id = ?""", tripId);
    }

    /** "date=title" per activity of the trip, in date then insertion order. */
    private List<String> activitiesByDate() {
        return jdbcTemplate.query("""
                SELECT CAST(d.date AS CHAR) AS day, a.title FROM activities a
                JOIN trip_days d ON d.id = a.trip_day_id
                WHERE d.trip_id = ? ORDER BY d.date, a.id""",
                (rs, rowNumber) -> rs.getString("day") + "=" + rs.getString("title"), tripId);
    }

    private Integer countActivities(String condition) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM activities WHERE " + condition, Integer.class);
    }

    private static String timed(String title, String start, String end) {
        return """
                { "title": "%s", "startTime": "%s", "endTime": "%s" }
                """.formatted(title, start, end);
    }

    private static List<String> titles(MvcTestResult result) {
        return JsonPath.read(body(result), "$.data[*].title");
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

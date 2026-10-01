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
 * The place of an activity through every layer against real MySQL: search → pick → attach → read → change →
 * remove (design.md 10.2 "Địa điểm của activity", rules 14.18 and 14.19). What only this level can prove:
 * <ul>
 *   <li>the version of the activity goes up when its place changes, and only then (Hibernate, not the service);</li>
 *   <li>a refused place really leaves nothing behind;</li>
 *   <li>a place another user added by hand is refused exactly like a place that does not exist;</li>
 *   <li>activities with a place cost no more SQL statements than activities without (CLAUDE.md section 8).</li>
 * </ul>
 * Hibernate statistics are switched on for this class only.
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ActivityPlaceFlowIntegrationTest {

    private static final String TRIPS_URL = "/api/v1/trips";
    private static final String PLACES_URL = "/api/v1/places";

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

    /** A trip 01/10 → 03/10 of the owner, created before every test. */
    private long tripId;
    private List<Long> dayIds;

    @BeforeEach
    void createUsersAndTrip() {
        ownerBearer = bearerFor(userRepository.save(user("owner@example.com")));
        otherBearer = bearerFor(userRepository.save(user("other@example.com")));

        MvcTestResult trip = send("POST", TRIPS_URL, ownerBearer, """
                { "title": "Đà Lạt", "startDate": "2026-10-01", "endDate": "2026-10-03", "currency": "VND" }
                """);
        assertThat(trip).hasStatus(HttpStatus.CREATED);
        tripId = id(trip);
        List<Number> ids = JsonPath.read(body(send("GET", TRIPS_URL + "/" + tripId + "/days", ownerBearer, null)),
                "$.data[*].id");
        dayIds = ids.stream().map(Number::longValue).toList();
    }

    @AfterEach
    void cleanUp() {
        // Activities go with their trips (ON DELETE CASCADE); places only once no activity points to them, and
        // before the users who added them by hand
        jdbcTemplate.update("DELETE FROM trips");
        jdbcTemplate.update("DELETE FROM places");
        jdbcTemplate.update("DELETE FROM users");
    }

    // ---- the whole life of the place of an activity --------------------------------------------------------------

    @Test
    void travellerSearchesPicksAttachesChangesAndRemovesAPlace() {
        // search: what the screen shows, with the two values that name the result
        String found = body(mvc.get().uri(PLACES_URL + "/search?q={q}", "cho da lat")
                .header(HttpHeaders.AUTHORIZATION, ownerBearer).exchange());
        assertThat(JsonPath.<String>read(found, "$.data[0].name")).isEqualTo("Chợ Đà Lạt");
        String externalId = JsonPath.read(found, "$.data[0].externalId");

        // pick: the result becomes a place with an id of ours
        long market = pick(externalId);

        // attach while creating
        MvcTestResult created = send("POST", dayUrl(0), ownerBearer, """
                { "title": "Mua đặc sản", "placeId": %d }
                """.formatted(market));
        assertThat(created).hasStatus(HttpStatus.CREATED)
                .bodyJson().isLenientlyEqualTo("""
                        { "data": { "title": "Mua đặc sản", "version": 0,
                                    "place": { "provider": "MOCK", "name": "Chợ Đà Lạt", "lat": 11.9434358,
                                               "lng": 108.4371779, "category": "SHOPPING" } } }
                        """);
        long activity = id(created);
        assertThat(created).bodyJson().extractingPath("$.data.place.id").isEqualTo((int) market);
        assertThat(row(activity)).containsEntry("place_id", market);

        // read: the list of the day and the trip detail both carry the place
        assertThat(JsonPath.<String>read(body(send("GET", dayUrl(0), ownerBearer, null)), "$.data[0].place.name"))
                .isEqualTo("Chợ Đà Lạt");
        assertThat(JsonPath.<String>read(body(send("GET", TRIPS_URL + "/" + tripId, ownerBearer, null)),
                "$.data.days[0].activities[0].place.name")).isEqualTo("Chợ Đà Lạt");

        // change: another place, the content version goes up
        long lake = pick("da-lat-ho-xuan-huong");
        assertThat(send("PATCH", activityUrl(activity), ownerBearer, """
                { "placeId": %d }
                """.formatted(lake)))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        { "data": { "title": "Mua đặc sản", "version": 1, "place": { "name": "Hồ Xuân Hương" } } }
                        """);
        assertThat(row(activity)).containsEntry("place_id", lake).containsEntry("version", 1L);

        // an edit that does not mention the place keeps it
        assertThat(send("PATCH", activityUrl(activity), ownerBearer, """
                { "title": "Dạo hồ" }
                """))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        { "data": { "title": "Dạo hồ", "version": 2, "place": { "name": "Hồ Xuân Hương" } } }
                        """);

        // remove: the activity has no place any more, both places are still stored for other activities
        assertThat(send("PATCH", activityUrl(activity), ownerBearer, """
                { "clearPlace": true }
                """))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        { "data": { "title": "Dạo hồ", "version": 3, "place": null } }
                        """);
        assertThat(row(activity)).containsEntry("place_id", null);
        assertThat(count("places")).isEqualTo(2);

        // removing again changes nothing: no error, and the version does not move
        assertThat(send("PATCH", activityUrl(activity), ownerBearer, """
                { "clearPlace": true }
                """))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        { "data": { "version": 3, "place": null } }
                        """);
    }

    @Test
    void deletingAnActivityKeepsItsPlaceForTheNextOne() {
        long market = pick("da-lat-cho-da-lat");
        long first = id(send("POST", dayUrl(0), ownerBearer, """
                { "title": "Mua đặc sản", "placeId": %d }
                """.formatted(market)));

        assertThat(send("DELETE", activityUrl(first), ownerBearer, null)).hasStatusOk();

        assertThat(count("activities")).isZero();
        assertThat(count("places")).isEqualTo(1);
        // Picking the same result again gives the same place, and it can be attached again
        assertThat(pick("da-lat-cho-da-lat")).isEqualTo(market);
        assertThat(send("POST", dayUrl(1), ownerBearer, """
                { "title": "Ghé chợ lần nữa", "placeId": %d }
                """.formatted(market))).hasStatus(HttpStatus.CREATED);
    }

    // ---- places that may not be used -----------------------------------------------------------------------------

    @Test
    void placeAddedByHandIsForItsCreatorAndLooksMissingToEverybodyElse() {
        long ownHome = addByHand(ownerBearer, "Nhà bà ngoại");
        long othersHome = addByHand(otherBearer, "Nhà của người khác");

        // the creator uses the place like any other
        MvcTestResult own = send("POST", dayUrl(0), ownerBearer, """
                { "title": "Thăm bà", "placeId": %d }
                """.formatted(ownHome));
        assertThat(own).hasStatus(HttpStatus.CREATED)
                .bodyJson().isLenientlyEqualTo("""
                        { "data": { "place": { "provider": "MANUAL", "name": "Nhà bà ngoại" } } }
                        """);
        long activity = id(own);

        // somebody else's place, and a place that never existed: the two answers are the same, word for word
        MvcTestResult someoneElses = send("POST", dayUrl(0), ownerBearer, """
                { "title": "Lẻn vào", "placeId": %d }
                """.formatted(othersHome));
        MvcTestResult unknown = send("POST", dayUrl(0), ownerBearer, """
                { "title": "Lẻn vào", "placeId": 999999 }
                """);
        assertThat(someoneElses).hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        { "errorCode": "VALIDATION_ERROR",
                          "details": [ { "field": "placeId", "message": "Địa điểm không tồn tại" } ] }
                        """);
        assertThat(unknown).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(JsonPath.<Object>read(body(someoneElses), "$.details"))
                .isEqualTo(JsonPath.<Object>read(body(unknown), "$.details"));
        assertThat(JsonPath.<String>read(body(someoneElses), "$.message"))
                .isEqualTo(JsonPath.<String>read(body(unknown), "$.message"));
        assertThat(count("activities")).isEqualTo(1);

        // the same refusal on an edit, and the other fields of that request are not applied either
        assertThat(send("PATCH", activityUrl(activity), ownerBearer, """
                { "title": "Tên mới", "placeId": %d }
                """.formatted(othersHome)))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.details[0].field").isEqualTo("placeId");
        assertThat(row(activity))
                .containsEntry("title", "Thăm bà")
                .containsEntry("place_id", ownHome)
                .containsEntry("version", 0L);
    }

    @Test
    void changingAndRemovingThePlaceInOneRequestIsRefusedAndNothingChanges() {
        long market = pick("da-lat-cho-da-lat");
        long lake = pick("da-lat-ho-xuan-huong");
        long activity = id(send("POST", dayUrl(0), ownerBearer, """
                { "title": "Mua đặc sản", "placeId": %d }
                """.formatted(market)));

        assertThat(send("PATCH", activityUrl(activity), ownerBearer, """
                { "placeId": %d, "clearPlace": true }
                """.formatted(lake)))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        { "errorCode": "VALIDATION_ERROR",
                          "details": [ { "field": "clearPlace",
                                         "message": "Không thể vừa đổi vừa bỏ địa điểm trong cùng một lần sửa" } ] }
                        """);
        assertThat(row(activity)).containsEntry("place_id", market).containsEntry("version", 0L);
    }

    @Test
    void wrongPlaceIsReportedBeforeTheOverlapSoConfirmingTheOverlapCannotFailOnIt() {
        assertThat(send("POST", dayUrl(0), ownerBearer, """
                { "title": "Ăn sáng", "startTime": "08:00", "endTime": "09:00" }
                """)).hasStatus(HttpStatus.CREATED);

        // Overlaps "Ăn sáng" AND names a place that does not exist: the field error wins over the 409
        assertThat(send("POST", dayUrl(0), ownerBearer, """
                { "title": "Cà phê", "startTime": "08:30", "endTime": "09:30", "placeId": 999999 }
                """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.details[0].field").isEqualTo("placeId");
        assertThat(count("activities")).isEqualTo(1);
    }

    // ---- no N+1 --------------------------------------------------------------------------------------------------

    @Test
    void activitiesWithAPlaceCostNoMoreQueriesThanActivitiesWithout() {
        List<String> picked = List.of("da-lat-cho-da-lat", "da-lat-ho-xuan-huong", "da-lat-ga-da-lat",
                "da-lat-crazy-house", "da-lat-dinh-bao-dai");
        long last = 0;
        for (String externalId : picked) {
            last = id(send("POST", dayUrl(0), ownerBearer, """
                    { "title": "Ghé %s", "placeId": %d }
                    """.formatted(externalId, pick(externalId))));
        }
        long withoutPlace = id(send("POST", dayUrl(0), ownerBearer, """
                { "title": "Nghỉ trưa" }
                """));

        // The numbers of the tests written before places existed (ActivityFlow, TripDayFlow, ActivityReorderFlow)
        // list of a day: permission + trip exists + day of trip + activities
        assertThat(statementsFor("GET", dayUrl(0), null)).isEqualTo(4);
        // trip detail: permission + trip + days + activities
        assertThat(statementsFor("GET", TRIPS_URL + "/" + tripId, null)).isEqualTo(4);
        // reorder: permission + trip exists + activities of the batch + days + UPDATE + activities of the day
        assertThat(statementsFor("PUT", TRIPS_URL + "/" + tripId + "/activities/reorder", """
                { "items": [ { "activityId": %d, "dayId": %d, "orderIndex": 500 } ] }
                """.formatted(last, dayIds.get(0)))).isEqualTo(6);
        // edit: the place comes with the activity, so renaming one with a place costs what renaming one without does
        assertThat(statementsFor("PATCH", activityUrl(last), """
                { "title": "Tên mới A" }
                """))
                .isEqualTo(statementsFor("PATCH", activityUrl(withoutPlace), """
                        { "title": "Tên mới B" }
                        """));

        // and the places are really there, not left out to save the queries
        List<String> names = JsonPath.read(body(send("GET", TRIPS_URL + "/" + tripId, ownerBearer, null)),
                "$.data.days[0].activities[*].place.name");
        assertThat(names).hasSize(picked.size()).contains("Chợ Đà Lạt", "Hồ Xuân Hương");
    }

    // ---- helpers -------------------------------------------------------------------------------------------------

    private long statementsFor(String method, String url, String json) {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        assertThat(send(method, url, ownerBearer, json)).hasStatusOk();
        return statistics.getPrepareStatementCount();
    }

    /** POST /places for a result of the bundled data; returns the id of the stored place. */
    private long pick(String externalId) {
        MvcTestResult result = send("POST", PLACES_URL, ownerBearer, """
                { "provider": "MOCK", "externalId": "%s" }
                """.formatted(externalId));
        assertThat(result).hasStatusOk();
        return id(result);
    }

    private long addByHand(String bearer, String name) {
        MvcTestResult result = send("POST", PLACES_URL + "/manual", bearer, """
                { "name": "%s", "lat": 11.9404, "lng": 108.4583 }
                """.formatted(name));
        assertThat(result).hasStatus(HttpStatus.CREATED);
        return id(result);
    }

    private String dayUrl(int dayPosition) {
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
            case "DELETE" -> mvc.delete();
            default -> mvc.get();
        };
        var prepared = request.uri(url).header(HttpHeaders.AUTHORIZATION, bearer);
        if (json != null) {
            prepared = prepared.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return prepared.exchange();
    }

    /** The activity as MySQL holds it. */
    private Map<String, Object> row(long activityId) {
        return jdbcTemplate.queryForMap("SELECT title, place_id, version FROM activities WHERE id = ?", activityId);
    }

    private int count(String table) {
        Integer rows = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
        return rows == null ? 0 : rows;
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

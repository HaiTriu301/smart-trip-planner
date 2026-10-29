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
 * Share links through every layer against real MySQL (design.md 10.2 "Liên kết", "Trang công khai", rule 14.26):
 * the owner creates a link, a guest without an account opens the trip with it, the views are counted, the owner
 * revokes it and the guest is shut out. What only this level can prove:
 * <ul>
 *   <li>the token really finds its link, its trip and the itinerary on MySQL, and nothing private leaks;</li>
 *   <li>a page costs four statements however big the trip is (CLAUDE.md section 8);</li>
 *   <li>a revoked or expired link, or a deleted trip, ends in the same 404 over HTTP;</li>
 *   <li>the visibility label of the trip follows its links.</li>
 * </ul>
 * Hibernate statistics are on, with the same properties as the other flow tests so the application context is
 * shared with them.
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ShareLinkFlowIntegrationTest {

    private static final String TRIPS_URL = "/api/v1/trips";
    private static final String PUBLIC_URL = "/api/v1/public/trips/";
    private static final String OWNER_EMAIL = "owner@example.com";
    private static final String BOOKING_URL = "https://booking.example.com/private-reservation";
    private static final String DAY_NOTE = "Ghi chú riêng của ngày";

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

    /**
     * A trip 01/10 → 03/10 of the owner. Day 1 has a private note and two activities: one at a place with a note,
     * a cost and a booking link, one without a place. Days 2 and 3 are empty.
     */
    private long tripId;

    @BeforeEach
    void createTripWithAnItinerary() {
        ownerBearer = bearerFor(userRepository.save(user(OWNER_EMAIL)));
        strangerBearer = bearerFor(userRepository.save(user("stranger@example.com")));

        MvcTestResult trip = send("POST", TRIPS_URL, ownerBearer, """
                { "title": "Đà Lạt", "description": "Đi cùng nhóm bạn", "startDate": "2026-10-01",
                  "endDate": "2026-10-03", "budgetAmount": 5000000, "currency": "VND" }
                """);
        assertThat(trip).hasStatus(HttpStatus.CREATED);
        tripId = id(trip);
        long firstDay = ((Number) JsonPath.read(body(send("GET", TRIPS_URL + "/" + tripId + "/days", ownerBearer,
                null)), "$.data[0].id")).longValue();
        jdbcTemplate.update("UPDATE trip_days SET note = ? WHERE id = ?", DAY_NOTE, firstDay);

        MvcTestResult place = send("POST", "/api/v1/places", ownerBearer, """
                { "provider": "MOCK", "externalId": "da-lat-cho-da-lat" }
                """);
        assertThat(place).hasStatusOk();
        String dayUrl = TRIPS_URL + "/" + tripId + "/days/" + firstDay + "/activities";
        assertThat(send("POST", dayUrl, ownerBearer, """
                { "title": "Ăn sáng", "type": "FOOD", "startTime": "07:30", "endTime": "08:15", "note": "Bánh căn",
                  "costAmount": 150000, "currency": "VND", "bookingUrl": "%s", "placeId": %d }
                """.formatted(BOOKING_URL, id(place)))).hasStatus(HttpStatus.CREATED);
        assertThat(send("POST", dayUrl, ownerBearer, """
                { "title": "Dạo hồ", "type": "SIGHTSEEING" }
                """)).hasStatus(HttpStatus.CREATED);
    }

    @AfterEach
    void cleanUp() {
        // trip_days, activities and share_links go with their trips (ON DELETE CASCADE); places once no activity
        // points to them; users last
        jdbcTemplate.update("DELETE FROM trips");
        jdbcTemplate.update("DELETE FROM places");
        jdbcTemplate.update("DELETE FROM users");
    }

    // ---- the whole story over HTTP -------------------------------------------------------------------------------

    @Test
    void ownerSharesAGuestOpensTheTripTheViewsAreCountedAndRevokingShutsTheGuestOut() {
        // 1. Only the owner makes links; a stranger is refused and nothing is stored
        assertThat(send("POST", linksUrl(), strangerBearer, null)).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(count("share_links")).isZero();
        assertThat(visibility()).isEqualTo("PRIVATE");

        // 2. The owner creates a link without a body: never expires, no views yet, the trip is labelled LINK
        MvcTestResult created = send("POST", linksUrl(), ownerBearer, null);
        assertThat(created).hasStatus(HttpStatus.CREATED).bodyJson().isLenientlyEqualTo("""
                { "data": { "permission": "VIEW", "expiresAt": null, "viewCount": 0, "expired": false } }
                """);
        String token = JsonPath.read(body(created), "$.data.token");
        long linkId = id(created);
        assertThat(token).matches("[a-z0-9]{32}");
        assertThat(visibility()).isEqualTo("LINK");

        // 3. A guest without an account opens the trip: the itinerary, and nothing of what is private
        MvcTestResult page = send("GET", PUBLIC_URL + token, null, null);
        assertThat(page).hasStatusOk().bodyJson().isLenientlyEqualTo("""
                { "data": {
                    "title": "Đà Lạt", "description": "Đi cùng nhóm bạn",
                    "startDate": "2026-10-01", "endDate": "2026-10-03",
                    "days": [
                      { "dayIndex": 1, "date": "2026-10-01", "activities": [
                          { "title": "Ăn sáng", "type": "FOOD", "startTime": "07:30", "endTime": "08:15",
                            "note": "Bánh căn", "place": { "name": "Chợ Đà Lạt" } },
                          { "title": "Dạo hồ", "type": "SIGHTSEEING", "place": null } ] },
                      { "dayIndex": 2, "date": "2026-10-02", "activities": [] },
                      { "dayIndex": 3, "date": "2026-10-03", "activities": [] } ] } }
                """);
        // The lenient comparison above ignores the order inside arrays; the order is part of the itinerary
        assertThat(JsonPath.<List<String>>read(body(page), "$.data.days[*].date"))
                .containsExactly("2026-10-01", "2026-10-02", "2026-10-03");
        assertThat(JsonPath.<List<String>>read(body(page), "$.data.days[0].activities[*].title"))
                .containsExactly("Ăn sáng", "Dạo hồ");
        assertThat(body(page))
                .doesNotContain(OWNER_EMAIL, "Test owner", BOOKING_URL, DAY_NOTE, "150000", "5000000")
                .doesNotContain("\"id\"", "ownerId", "visibility", "costAmount", "budget");

        // 4. Every page served counts; four statements whatever the size of the trip
        assertThat(statementsForThePageOf(token)).isEqualTo(4);
        assertThat(viewCountInTheOwnersList()).isEqualTo(2);

        // 5. The same token in upper case is not the link (the column compares without case): 404, not counted
        assertThat(send("GET", PUBLIC_URL + token.toUpperCase(), null, null)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(viewCountInTheOwnersList()).isEqualTo(2);

        // 6. A stranger cannot revoke it; the owner can, once
        assertThat(send("DELETE", linksUrl() + "/" + linkId, strangerBearer, null)).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(send("DELETE", linksUrl() + "/" + linkId, ownerBearer, null)).hasStatusOk();
        assertThat(send("DELETE", linksUrl() + "/" + linkId, ownerBearer, null)).hasStatus(HttpStatus.NOT_FOUND);

        // 7. The guest is shut out with exactly the answer of a token that never existed
        MvcTestResult revoked = send("GET", PUBLIC_URL + token, null, null);
        MvcTestResult unknown = send("GET", PUBLIC_URL + "z".repeat(32), null, null);
        assertSameRefusal(revoked, unknown);

        // 8. The trip is private again, the list is empty, the row and its count are kept
        assertThat(visibility()).isEqualTo("PRIVATE");
        assertThat(send("GET", linksUrl(), ownerBearer, null)).hasStatusOk()
                .bodyJson().extractingPath("$.data").asArray().isEmpty();
        assertThat(jdbcTemplate.queryForObject("SELECT view_count FROM share_links WHERE id = ?", Integer.class,
                linkId)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("SELECT revoked_at IS NOT NULL FROM share_links WHERE id = ?",
                Boolean.class, linkId)).isTrue();
    }

    // ---- the other ways a link stops working ----------------------------------------------------------------------

    @Test
    void aLiveLinkOfADeletedTripIs404() {
        String token = createLink(null);
        assertThat(send("GET", PUBLIC_URL + token, null, null)).hasStatusOk();

        assertThat(send("DELETE", TRIPS_URL + "/" + tripId, ownerBearer, null)).hasStatusOk();

        assertSameRefusal(send("GET", PUBLIC_URL + token, null, null),
                send("GET", PUBLIC_URL + "z".repeat(32), null, null));
        // Never revoked: the link is switched off by its trip, and the refused opening was not counted
        assertThat(jdbcTemplate.queryForMap("SELECT revoked_at, view_count FROM share_links WHERE token = ?", token))
                .containsEntry("revoked_at", null).containsEntry("view_count", 1);
    }

    @Test
    void aLinkIs404FromTheMomentItExpiresAndStaysInTheOwnersListMarkedExpired() {
        String token = createLink("2099-01-01T00:00:00Z");
        assertThat(send("GET", PUBLIC_URL + token, null, null)).hasStatusOk();

        // Time passes: the expiry is moved into the past by MySQL, in UTC (CLAUDE.md section 8, BUG-AUTH-008)
        jdbcTemplate.update("UPDATE share_links SET expires_at = UTC_TIMESTAMP(6) - INTERVAL 1 SECOND WHERE token = ?",
                token);

        assertSameRefusal(send("GET", PUBLIC_URL + token, null, null),
                send("GET", PUBLIC_URL + "z".repeat(32), null, null));
        assertThat(send("GET", linksUrl(), ownerBearer, null)).hasStatusOk().bodyJson().isLenientlyEqualTo("""
                { "data": [ { "viewCount": 1, "expired": true } ] }
                """);
        // Expired but not revoked: the trip keeps its label until the owner revokes the link (design.md 10.2)
        assertThat(visibility()).isEqualTo("LINK");
    }

    @Test
    void revokingOneOfTwoLinksLeavesTheOtherOpenAndTheTripShared() {
        String kept = createLink(null);
        String revoked = createLink(null);
        Long revokedId = jdbcTemplate.queryForObject("SELECT id FROM share_links WHERE token = ?", Long.class,
                revoked);

        assertThat(send("DELETE", linksUrl() + "/" + revokedId, ownerBearer, null)).hasStatusOk();

        assertThat(send("GET", PUBLIC_URL + revoked, null, null)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send("GET", PUBLIC_URL + kept, null, null)).hasStatusOk();
        assertThat(visibility()).isEqualTo("LINK");
        assertThat(JsonPath.<List<String>>read(body(send("GET", linksUrl(), ownerBearer, null)), "$.data[*].token"))
                .containsExactly(kept);
    }

    @Test
    void sharingAndUnsharingLeaveTheTripVersionAlone() {
        String token = createLink(null);
        assertThat(visibility()).isEqualTo("LINK");
        assertThat(tripVersion()).isZero();

        Long linkId = jdbcTemplate.queryForObject("SELECT id FROM share_links WHERE token = ?", Long.class, token);
        assertThat(send("DELETE", linksUrl() + "/" + linkId, ownerBearer, null)).hasStatusOk();

        // The last link is gone, so the trip is private again; an open "edit trip" form is not made stale
        assertThat(visibility()).isEqualTo("PRIVATE");
        assertThat(tripVersion()).isZero();
    }

    // ---- helpers -------------------------------------------------------------------------------------------------

    /** Every refusal of the public page is the same 404: a guest cannot tell why. */
    private static void assertSameRefusal(MvcTestResult refused, MvcTestResult unknown) {
        assertThat(refused).hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(unknown).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(JsonPath.<String>read(body(refused), "$.message"))
                .isEqualTo(JsonPath.<String>read(body(unknown), "$.message"));
    }

    /** Opens the public page as a guest; returns the number of statements it cost. */
    private long statementsForThePageOf(String token) {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        assertThat(send("GET", PUBLIC_URL + token, null, null)).hasStatusOk();
        return statistics.getPrepareStatementCount();
    }

    /** The count as the owner reads it, from the one link of the trip. */
    private int viewCountInTheOwnersList() {
        return JsonPath.read(body(send("GET", linksUrl(), ownerBearer, null)), "$.data[0].viewCount");
    }

    /** POST /share-links as the owner; returns the token. */
    private String createLink(String expiresAt) {
        MvcTestResult created = send("POST", linksUrl(), ownerBearer,
                expiresAt == null ? null : "{ \"expiresAt\": \"" + expiresAt + "\" }");
        assertThat(created).hasStatus(HttpStatus.CREATED);
        return JsonPath.read(body(created), "$.data.token");
    }

    private String visibility() {
        return jdbcTemplate.queryForObject("SELECT visibility FROM trips WHERE id = ?", String.class, tripId);
    }

    private long tripVersion() {
        return jdbcTemplate.queryForObject("SELECT version FROM trips WHERE id = ?", Long.class, tripId);
    }

    private String linksUrl() {
        return TRIPS_URL + "/" + tripId + "/share-links";
    }

    private MvcTestResult send(String method, String url, String bearer, String json) {
        var request = switch (method) {
            case "POST" -> mvc.post();
            case "DELETE" -> mvc.delete();
            default -> mvc.get();
        };
        var prepared = request.uri(url);
        if (bearer != null) {
            prepared = prepared.header(HttpHeaders.AUTHORIZATION, bearer);
        }
        if (json != null) {
            prepared = prepared.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return prepared.exchange();
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

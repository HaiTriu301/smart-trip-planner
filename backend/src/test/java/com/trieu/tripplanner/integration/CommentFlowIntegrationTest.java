package com.trieu.tripplanner.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.trieu.tripplanner.TestcontainersConfiguration;
import com.trieu.tripplanner.model.TripMember;
import com.trieu.tripplanner.model.User;
import com.trieu.tripplanner.model.enums.MemberRole;
import com.trieu.tripplanner.model.enums.MemberStatus;
import com.trieu.tripplanner.repository.TripMemberRepository;
import com.trieu.tripplanner.repository.TripRepository;
import com.trieu.tripplanner.repository.UserRepository;
import com.trieu.tripplanner.security.JwtTokenProvider;
import jakarta.persistence.EntityManagerFactory;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
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
 * Comments through every layer against real MySQL and Redis (design.md 10.2 "Quy ước Comment API", rule 27).
 * What only this level can prove:
 * <ul>
 *   <li>a thread posted by two people reads back in order, replies sitting on the activity of their parent;</li>
 *   <li>deleting a root really hides its replies, and the rows stay in the table (soft delete);</li>
 *   <li>deleting an activity through its own endpoint takes its comments with it (ON DELETE CASCADE);</li>
 *   <li>the list costs the same number of SQL statements whatever the number of comments (CLAUDE.md section 8).</li>
 * </ul>
 * The per-cell permissions are TripPermissionMatrixIntegrationTest's job. Hibernate statistics are switched on for
 * this class only.
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class CommentFlowIntegrationTest {

    private static final String TRIPS_URL = "/api/v1/trips";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private TripMemberRepository tripMemberRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private String ownerBearer;
    private String viewerBearer;

    /** A trip 01/10 → 03/10 of the owner with one activity on day 1, and an accepted VIEWER. */
    private long tripId;
    private long activityId;

    @BeforeEach
    void createTripWithOneActivityAndAViewer() {
        User owner = userRepository.save(user("owner@example.com", "Chủ chuyến đi"));
        User viewer = userRepository.save(user("viewer@example.com", "Người xem"));
        ownerBearer = bearerFor(owner);
        viewerBearer = bearerFor(viewer);

        tripId = id(send("POST", TRIPS_URL, ownerBearer, """
                { "title": "Đà Lạt", "startDate": "2026-10-01", "endDate": "2026-10-03" }
                """));
        long dayId = ((Number) JsonPath.read(body(send("GET", TRIPS_URL + "/" + tripId + "/days", ownerBearer,
                null)), "$.data[0].id")).longValue();
        activityId = id(send("POST", TRIPS_URL + "/" + tripId + "/days/" + dayId + "/activities", ownerBearer, """
                { "title": "Chợ Đà Lạt" }
                """));

        // Written straight to the table: SharingFlowIntegrationTest covers the invitation itself
        Instant invited = Instant.parse("2026-09-01T00:00:00Z");
        tripMemberRepository.save(TripMember.builder()
                .trip(tripRepository.findById(tripId).orElseThrow()).user(viewer).invitedEmail(viewer.getEmail())
                .role(MemberRole.VIEWER).status(MemberStatus.ACCEPTED)
                .invitedBy(owner).invitedAt(invited).acceptedAt(invited.plusSeconds(60))
                .build());
    }

    @AfterEach
    void cleanUp() {
        // Days, activities, members and comments go with their trips (ON DELETE CASCADE); trips before users
        jdbcTemplate.update("DELETE FROM trips");
        jdbcTemplate.update("DELETE FROM users");
    }

    // ---- post, reply, read -----------------------------------------------------------------------------------------

    @Test
    void theOwnerPostsTheViewerRepliesAndBothReadTheThreadInOrder() {
        long onTrip = post(ownerBearer, """
                { "content": "Cả nhà nhớ mang áo ấm" }
                """);
        long question = post(ownerBearer, """
                { "content": "Mấy giờ đi chợ?", "activityId": %d }
                """.formatted(activityId));
        // The viewer sends no activityId: the reply sits on the activity of the question anyway
        long answer = post(viewerBearer, """
                { "content": "  7 giờ nhé  ", "parentId": %d }
                """.formatted(question));

        String all = body(send("GET", commentsUrl(), viewerBearer, null));
        assertThat(JsonPath.<List<Number>>read(all, "$.data[*].id"))
                .extracting(Number::longValue).containsExactly(onTrip, question, answer);
        assertThat(JsonPath.<String>read(all, "$.data[2].content")).isEqualTo("7 giờ nhé");
        assertThat(JsonPath.<Number>read(all, "$.data[2].parentId").longValue()).isEqualTo(question);
        assertThat(JsonPath.<Number>read(all, "$.data[2].activityId").longValue()).isEqualTo(activityId);
        assertThat(JsonPath.<String>read(all, "$.data[2].author.fullName")).isEqualTo("Người xem");
        assertThat(JsonPath.<String>read(all, "$.data[0].author.fullName")).isEqualTo("Chủ chuyến đi");

        // The card of the activity: the question and its answer, not the comment on the whole trip
        String ofActivity = body(send("GET", commentsUrl() + "?activityId=" + activityId, ownerBearer, null));
        assertThat(JsonPath.<List<Number>>read(ofActivity, "$.data[*].id"))
                .extracting(Number::longValue).containsExactly(question, answer);

        // One level only: answering the answer is refused on the parentId field, and nothing is stored
        assertThat(send("POST", commentsUrl(), ownerBearer, """
                { "content": "Ok", "parentId": %d }
                """.formatted(answer)))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.details[0].field").isEqualTo("parentId");
        assertThat(rows()).isEqualTo(3);
    }

    // ---- delete --------------------------------------------------------------------------------------------------

    @Test
    void deletingARootHidesItsRepliesAndKeepsTheRowsOnlyTheAuthorOrTheOwnerMayDelete() {
        long question = post(ownerBearer, """
                { "content": "Mấy giờ đi chợ?" }
                """);
        long answer = post(viewerBearer, """
                { "content": "7 giờ nhé", "parentId": %d }
                """.formatted(question));
        long viewerOwn = post(viewerBearer, """
                { "content": "Ai mang ô?" }
                """);

        // The viewer may view the trip, but the question is not theirs
        assertThat(send("DELETE", commentsUrl() + "/" + question, viewerBearer, null))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("FORBIDDEN");

        assertThat(send("DELETE", commentsUrl() + "/" + question, ownerBearer, null)).hasStatusOk()
                .bodyJson().extractingPath("$.data").isNull();

        // The answer went with the question; the viewer's own comment stays
        String left = body(send("GET", commentsUrl(), viewerBearer, null));
        assertThat(JsonPath.<List<Number>>read(left, "$.data[*].id"))
                .extracting(Number::longValue).containsExactly(viewerOwn);
        // Soft delete: the three rows are still in the table, two of them stamped with the same instant
        assertThat(rows()).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(DISTINCT deleted_at) FROM comments WHERE id IN (?, ?)",
                Integer.class, question, answer)).isEqualTo(1);
        // A second delete finds nothing
        assertThat(send("DELETE", commentsUrl() + "/" + question, ownerBearer, null))
                .hasStatus(HttpStatus.NOT_FOUND);

        // The owner removes any comment of the trip
        assertThat(send("DELETE", commentsUrl() + "/" + viewerOwn, ownerBearer, null)).hasStatusOk();
        assertThat(JsonPath.<List<Object>>read(body(send("GET", commentsUrl(), ownerBearer, null)), "$.data"))
                .isEmpty();
    }

    @Test
    void deletingAnActivityThroughItsEndpointTakesItsCommentsAway() {
        long question = post(ownerBearer, """
                { "content": "Mấy giờ đi chợ?", "activityId": %d }
                """.formatted(activityId));
        post(viewerBearer, """
                { "content": "7 giờ nhé", "parentId": %d }
                """.formatted(question));
        long onTrip = post(viewerBearer, """
                { "content": "Cả nhà nhớ mang áo ấm" }
                """);

        assertThat(send("DELETE", TRIPS_URL + "/" + tripId + "/activities/" + activityId, ownerBearer, null))
                .hasStatusOk();

        assertThat(JsonPath.<List<Number>>read(body(send("GET", commentsUrl(), ownerBearer, null)), "$.data[*].id"))
                .extracting(Number::longValue).containsExactly(onTrip);
        // Gone from the table, not only hidden: the activity was deleted for good
        assertThat(rows()).isEqualTo(1);
        // And the activity cannot be asked about any more
        assertThat(send("GET", commentsUrl() + "?activityId=" + activityId, ownerBearer, null))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    // ---- SQL statements --------------------------------------------------------------------------------------------

    @Test
    void theListCostsTheSameNumberOfStatementsWhateverTheNumberOfComments() {
        long question = post(ownerBearer, """
                { "content": "Mấy giờ đi chợ?", "activityId": %d }
                """.formatted(activityId));

        // The owner's permission comes from Redis, stored by the call above
        // all: trip exists + comments with their authors
        assertThat(statementsFor(commentsUrl())).isEqualTo(2);
        // one activity: trip exists + activity of the trip + comments with their authors
        assertThat(statementsFor(commentsUrl() + "?activityId=" + activityId)).isEqualTo(3);

        for (int i = 1; i <= 5; i++) {
            post(i % 2 == 0 ? ownerBearer : viewerBearer, """
                    { "content": "Trả lời %d", "parentId": %d }
                    """.formatted(i, question));
        }

        assertThat(statementsFor(commentsUrl())).isEqualTo(2);
        assertThat(statementsFor(commentsUrl() + "?activityId=" + activityId)).isEqualTo(3);
        // and the six comments are really there, with both authors
        assertThat(JsonPath.<List<String>>read(body(send("GET", commentsUrl(), ownerBearer, null)),
                "$.data[*].author.fullName"))
                .hasSize(6).contains("Chủ chuyến đi", "Người xem");
    }

    // ---- helpers -------------------------------------------------------------------------------------------------

    private String commentsUrl() {
        return TRIPS_URL + "/" + tripId + "/comments";
    }

    /** POST .../comments; returns the id of the new comment. */
    private long post(String bearer, String json) {
        MvcTestResult result = send("POST", commentsUrl(), bearer, json);
        assertThat(result).hasStatus(HttpStatus.CREATED);
        return id(result);
    }

    private long statementsFor(String url) {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        assertThat(send("GET", url, ownerBearer, null)).hasStatusOk();
        return statistics.getPrepareStatementCount();
    }

    /** Every row of the table, deleted ones included. */
    private int rows() {
        Integer rows = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM comments", Integer.class);
        return rows == null ? 0 : rows;
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

    private static User user(String email, String fullName) {
        return User.builder()
                .email(email)
                .passwordHash("$2a$12$placeholder-bcrypt-hash-not-real")
                .fullName(fullName)
                .emailVerified(true)
                .build();
    }

}

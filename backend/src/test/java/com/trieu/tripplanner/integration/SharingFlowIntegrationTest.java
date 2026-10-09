package com.trieu.tripplanner.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.trieu.tripplanner.TestcontainersConfiguration;
import com.trieu.tripplanner.model.Trip;
import com.trieu.tripplanner.model.TripMember;
import com.trieu.tripplanner.model.User;
import com.trieu.tripplanner.model.enums.MemberRole;
import com.trieu.tripplanner.model.enums.MemberStatus;
import com.trieu.tripplanner.repository.TripMemberRepository;
import com.trieu.tripplanner.repository.TripRepository;
import com.trieu.tripplanner.repository.UserRepository;
import com.trieu.tripplanner.security.JwtTokenProvider;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
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
 * Sharing through every layer against real MySQL (design.md 6.2, 10.2 "Sharing"). Grown milestone by milestone
 * of Task 4.1: first what each role may do on the existing trip endpoints once the evaluator knows members,
 * later the whole invitation story over HTTP.
 * The member rows of this class are written through the repository: the invitation endpoints have their own
 * tests, and this one asks what a membership is worth once it exists.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class SharingFlowIntegrationTest {

    private static final String TRIPS_URL = "/api/v1/trips";
    private static final Instant SEPT_1 = Instant.parse("2026-09-01T00:00:00Z");

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

    private User owner;
    private String ownerBearer;
    private String editorBearer;
    private String viewerBearer;
    private String pendingBearer;
    private String removedBearer;
    private String strangerBearer;

    /** A trip 01/10 → 03/10 of the owner with one member of every kind. */
    private long tripId;
    private long dayId;
    private long viewerMemberId;
    private long removedMemberId;

    @BeforeEach
    void createTripAndMembers() {
        owner = userRepository.save(user("owner@example.com"));
        User editor = userRepository.save(user("editor@example.com"));
        User viewer = userRepository.save(user("viewer@example.com"));
        User pending = userRepository.save(user("pending@example.com"));
        User removed = userRepository.save(user("removed@example.com"));
        User stranger = userRepository.save(user("stranger@example.com"));
        ownerBearer = bearerFor(owner);
        editorBearer = bearerFor(editor);
        viewerBearer = bearerFor(viewer);
        pendingBearer = bearerFor(pending);
        removedBearer = bearerFor(removed);
        strangerBearer = bearerFor(stranger);

        MvcTestResult created = send("POST", TRIPS_URL, ownerBearer, """
                { "title": "Đà Lạt", "startDate": "2026-10-01", "endDate": "2026-10-03" }
                """);
        assertThat(created).hasStatus(HttpStatus.CREATED);
        tripId = id(created);
        dayId = ((Number) JsonPath.read(body(send("GET", TRIPS_URL + "/" + tripId + "/days", ownerBearer, null)),
                "$.data[0].id")).longValue();

        Trip trip = tripRepository.findById(tripId).orElseThrow();
        tripMemberRepository.save(member(trip, editor, MemberRole.EDITOR, MemberStatus.ACCEPTED));
        viewerMemberId = tripMemberRepository.save(member(trip, viewer, MemberRole.VIEWER, MemberStatus.ACCEPTED)).getId();
        tripMemberRepository.save(member(trip, pending, MemberRole.EDITOR, MemberStatus.PENDING));
        removedMemberId = tripMemberRepository.save(member(trip, removed, MemberRole.EDITOR, MemberStatus.REMOVED)).getId();
    }

    @AfterEach
    void cleanUp() {
        // trip_days, activities and trip_members go with their trips (ON DELETE CASCADE); trips before users
        jdbcTemplate.update("DELETE FROM trips");
        jdbcTemplate.update("DELETE FROM users");
    }

    // ---- what each role may do (design.md 6.2, test bắt buộc design 16 "Viewer sửa activity → 403") -------------

    @Test
    void viewerReadsEverythingOfTheTripButChangesNothing() {
        assertThat(send("GET", TRIPS_URL + "/" + tripId, viewerBearer, null)).hasStatusOk()
                .bodyJson().extractingPath("$.data.title").isEqualTo("Đà Lạt");
        assertThat(send("GET", TRIPS_URL + "/" + tripId + "/days", viewerBearer, null)).hasStatusOk();
        assertThat(send("GET", TRIPS_URL + "/" + tripId + "/days/" + dayId + "/activities", viewerBearer, null))
                .hasStatusOk();
        assertThat(send("GET", "/api/v1/weather/trips/" + tripId, viewerBearer, null)).hasStatusOk();

        assertThat(send("PATCH", TRIPS_URL + "/" + tripId, viewerBearer, """
                { "title": "Đổi tên" }
                """)).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(send("PATCH", TRIPS_URL + "/" + tripId + "/status", viewerBearer, """
                { "status": "PLANNED" }
                """)).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(send("POST", activitiesUrl(), viewerBearer, """
                { "title": "Ăn sáng" }
                """)).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(send("DELETE", TRIPS_URL + "/" + tripId, viewerBearer, null)).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(send("POST", TRIPS_URL + "/" + tripId + "/members", viewerBearer, """
                { "email": "x@example.com", "role": "VIEWER" }
                """)).hasStatus(HttpStatus.FORBIDDEN);

        // Nothing of the above got through
        assertThat(JsonPath.<String>read(body(send("GET", TRIPS_URL + "/" + tripId, ownerBearer, null)),
                "$.data.title")).isEqualTo("Đà Lạt");
        assertThat(count("activities")).isZero();
    }

    @Test
    void editorEditsTheTripAndItsActivitiesButNeitherDeletesItNorManagesMembers() {
        assertThat(send("PATCH", TRIPS_URL + "/" + tripId, editorBearer, """
                { "title": "Đà Lạt mùa hoa" }
                """)).hasStatusOk();
        MvcTestResult activity = send("POST", activitiesUrl(), editorBearer, """
                { "title": "Ăn sáng" }
                """);
        assertThat(activity).hasStatus(HttpStatus.CREATED);
        assertThat(send("PATCH", TRIPS_URL + "/" + tripId + "/activities/" + id(activity), editorBearer, """
                { "title": "Ăn sáng ở chợ" }
                """)).hasStatusOk();

        assertThat(send("DELETE", TRIPS_URL + "/" + tripId, editorBearer, null)).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(send("POST", TRIPS_URL + "/" + tripId + "/members", editorBearer, """
                { "email": "x@example.com", "role": "VIEWER" }
                """)).hasStatus(HttpStatus.FORBIDDEN);

        assertThat(jdbcTemplate.queryForObject("SELECT title FROM trips WHERE id = ?", String.class, tripId))
                .isEqualTo("Đà Lạt mùa hoa");
        assertThat(jdbcTemplate.queryForObject("SELECT deleted_at FROM trips WHERE id = ?", Object.class, tripId))
                .isNull();
        assertThat(count("trip_members")).isEqualTo(4);
    }

    @Test
    void pendingRemovedAndStrangerAreAllTreatedAlike() {
        for (String bearer : new String[] {pendingBearer, removedBearer, strangerBearer}) {
            assertThat(send("GET", TRIPS_URL + "/" + tripId, bearer, null)).hasStatus(HttpStatus.FORBIDDEN);
            assertThat(send("GET", TRIPS_URL + "/" + tripId + "/days", bearer, null)).hasStatus(HttpStatus.FORBIDDEN);
            assertThat(send("PATCH", TRIPS_URL + "/" + tripId, bearer, """
                    { "title": "Đổi tên" }
                    """)).hasStatus(HttpStatus.FORBIDDEN);
        }
    }

    @Test
    void membersOfOneTripHaveNoAccessToAnotherTripOfTheSameOwner() {
        MvcTestResult other = send("POST", TRIPS_URL, ownerBearer, """
                { "title": "Huế", "startDate": "2026-11-01", "endDate": "2026-11-02" }
                """);
        long otherId = id(other);

        assertThat(send("GET", TRIPS_URL + "/" + otherId, editorBearer, null)).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(send("GET", TRIPS_URL + "/" + otherId, viewerBearer, null)).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void aMemberOfADeletedTripGets404LikeEverybodyElse() {
        assertThat(send("DELETE", TRIPS_URL + "/" + tripId, ownerBearer, null)).hasStatusOk();

        assertThat(send("GET", TRIPS_URL + "/" + tripId, editorBearer, null)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send("GET", TRIPS_URL + "/" + tripId, strangerBearer, null)).hasStatus(HttpStatus.NOT_FOUND);
    }

    // ---- changing a role takes effect at once (design.md 10.2 "đổi vai trò") ------------------------------------

    @Test
    void promotingAViewerToEditorLetsThemEditOnTheVeryNextRequest() {
        assertThat(send("PATCH", TRIPS_URL + "/" + tripId, viewerBearer, """
                { "title": "Chưa được" }
                """)).hasStatus(HttpStatus.FORBIDDEN);

        assertThat(send("PATCH", memberUrl(viewerMemberId), ownerBearer, """
                { "role": "EDITOR" }
                """)).hasStatusOk().bodyJson().extractingPath("$.data.role").isEqualTo("EDITOR");

        assertThat(send("PATCH", TRIPS_URL + "/" + tripId, viewerBearer, """
                { "title": "Giờ thì được" }
                """)).hasStatusOk();
        assertThat(send("POST", activitiesUrl(), viewerBearer, """
                { "title": "Ăn sáng" }
                """)).hasStatus(HttpStatus.CREATED);

        // and back: demoting cuts the write access just as fast
        assertThat(send("PATCH", memberUrl(viewerMemberId), ownerBearer, """
                { "role": "VIEWER" }
                """)).hasStatusOk();
        assertThat(send("PATCH", TRIPS_URL + "/" + tripId, viewerBearer, """
                { "title": "Hết được" }
                """)).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void onlyTheOwnerChangesRolesAndOnlyOfCurrentMembersOfThisTrip() {
        assertThat(send("PATCH", memberUrl(viewerMemberId), editorBearer, """
                { "role": "EDITOR" }
                """)).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(send("PATCH", memberUrl(removedMemberId), ownerBearer, """
                { "role": "VIEWER" }
                """)).hasStatus(HttpStatus.NOT_FOUND);

        MvcTestResult other = send("POST", TRIPS_URL, ownerBearer, """
                { "title": "Huế", "startDate": "2026-11-01", "endDate": "2026-11-02" }
                """);
        assertThat(send("PATCH", TRIPS_URL + "/" + id(other) + "/members/" + viewerMemberId, ownerBearer, """
                { "role": "EDITOR" }
                """)).hasStatus(HttpStatus.NOT_FOUND);

        assertThat(jdbcTemplate.queryForObject("SELECT role FROM trip_members WHERE id = ?", String.class, viewerMemberId))
                .isEqualTo("VIEWER");
    }

    // ---- helpers -------------------------------------------------------------------------------------------------

    private String memberUrl(long memberId) {
        return TRIPS_URL + "/" + tripId + "/members/" + memberId;
    }

    private String activitiesUrl() {
        return TRIPS_URL + "/" + tripId + "/days/" + dayId + "/activities";
    }

    private TripMember member(Trip trip, User user, MemberRole role, MemberStatus status) {
        return TripMember.builder()
                .trip(trip)
                .user(user)
                .invitedEmail(user.getEmail())
                .role(role)
                .status(status)
                .inviteTokenHash(status == MemberStatus.PENDING ? "hash-for-" + user.getEmail() : null)
                .inviteExpiresAt(status == MemberStatus.PENDING ? Instant.now().plusSeconds(3600) : null)
                .invitedBy(owner)
                .invitedAt(SEPT_1)
                .acceptedAt(status == MemberStatus.PENDING ? null : SEPT_1.plusSeconds(60))
                .build();
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

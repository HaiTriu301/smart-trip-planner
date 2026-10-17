package com.trieu.tripplanner.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.trieu.tripplanner.TestcontainersConfiguration;
import com.trieu.tripplanner.model.Trip;
import com.trieu.tripplanner.model.TripMember;
import com.trieu.tripplanner.model.User;
import com.trieu.tripplanner.model.enums.MemberRole;
import com.trieu.tripplanner.model.enums.MemberStatus;
import com.trieu.tripplanner.provider.mail.MockMailProvider;
import com.trieu.tripplanner.repository.TripMemberRepository;
import com.trieu.tripplanner.repository.TripRepository;
import com.trieu.tripplanner.repository.UserRepository;
import com.trieu.tripplanner.security.JwtTokenProvider;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * The permission matrix of design.md 6.2, checked cell by cell over HTTP against real MySQL and Redis.
 * <p>
 * {@link #ACTIONS} is the table of the design written as code: one row per trip endpoint, with the level it
 * needs (view, edit, owner) and the status it answers on success. The expected answer of every cell follows
 * from one rule ({@link #expectedStatus}): no token → 401; a caller the level allows → the success status;
 * anyone else → 403. PENDING and REMOVED members are strangers (design.md 6.2).
 * <p>
 * Every cell starts from fresh data, so an allowed "delete the trip" cannot change what the next cell sees and
 * the order the cells run in does not matter (CLAUDE.md rule 24).
 * <p>
 * {@link #everyTripEndpointIsGuardedByTheEvaluatorAndListedInTheMatrix} reads the endpoints the application
 * really serves: a new trip endpoint without {@code @tripPermission}, or missing from the table, fails the build.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class TripPermissionMatrixIntegrationTest {

    private static final Instant SEPT_1 = Instant.parse("2026-09-01T00:00:00Z");

    /** Who calls: the owner, an accepted member of each role, the two kinds of former or future member, and two outsiders. */
    enum Caller { OWNER, EDITOR, VIEWER, PENDING, REMOVED, STRANGER, GUEST }

    /** The three levels of design.md 6.2 and the evaluator method that guards each one. */
    enum Level {
        VIEW("canView", EnumSet.of(Caller.OWNER, Caller.EDITOR, Caller.VIEWER)),
        EDIT("canEdit", EnumSet.of(Caller.OWNER, Caller.EDITOR)),
        OWNER("isOwner", EnumSet.of(Caller.OWNER));

        final String evaluatorMethod;
        final Set<Caller> allowed;

        Level(String evaluatorMethod, Set<Caller> allowed) {
            this.evaluatorMethod = evaluatorMethod;
            this.allowed = allowed;
        }
    }

    /**
     * One row of the matrix. {@code pattern} is the path exactly as the controller maps it, so the scan can match
     * it; its {placeholders} and those of {@code body} are filled with the ids of the fixture of the cell.
     */
    record Action(String name, HttpMethod method, String pattern, Level level, HttpStatus success, String body) {

        String endpoint() {
            return method.name() + " " + pattern;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final String TRIP = "/api/v1/trips/{id}";
    private static final String TRIP_OF = "/api/v1/trips/{tripId}";

    static final List<Action> ACTIONS = List.of(
            // View: owner and every accepted member
            new Action("Xem chuyến đi", HttpMethod.GET, TRIP, Level.VIEW, HttpStatus.OK, null),
            new Action("Xem các ngày", HttpMethod.GET, TRIP_OF + "/days", Level.VIEW, HttpStatus.OK, null),
            new Action("Xem hoạt động của ngày", HttpMethod.GET, TRIP_OF + "/days/{dayId}/activities", Level.VIEW,
                    HttpStatus.OK, null),
            new Action("Xem quãng đường của ngày", HttpMethod.GET, TRIP_OF + "/days/{dayId}/route", Level.VIEW,
                    HttpStatus.OK, null),
            new Action("Xem thời tiết", HttpMethod.GET, "/api/v1/weather/trips/{tripId}", Level.VIEW,
                    HttpStatus.OK, null),
            new Action("Xem thành viên", HttpMethod.GET, TRIP_OF + "/members", Level.VIEW, HttpStatus.OK, null),
            new Action("Đăng bình luận", HttpMethod.POST, TRIP_OF + "/comments", Level.VIEW, HttpStatus.CREATED, """
                    { "content": "Nhớ mang áo ấm" }"""),
            // Edit: owner and accepted editors
            new Action("Sửa chuyến đi", HttpMethod.PATCH, TRIP, Level.EDIT, HttpStatus.OK, """
                    { "title": "Đà Lạt mùa hoa" }"""),
            new Action("Đổi trạng thái", HttpMethod.PATCH, TRIP + "/status", Level.EDIT, HttpStatus.OK, """
                    { "status": "PLANNED" }"""),
            new Action("Sửa ngày", HttpMethod.PATCH, TRIP_OF + "/days/{dayId}", Level.EDIT, HttpStatus.OK, """
                    { "title": "Ngày đầu" }"""),
            new Action("Thêm hoạt động", HttpMethod.POST, TRIP_OF + "/days/{dayId}/activities", Level.EDIT,
                    HttpStatus.CREATED, """
                    { "title": "Ăn trưa" }"""),
            new Action("Sửa hoạt động", HttpMethod.PATCH, TRIP_OF + "/activities/{activityId}", Level.EDIT,
                    HttpStatus.OK, """
                    { "title": "Ăn sáng ở chợ" }"""),
            new Action("Sắp xếp lại hoạt động", HttpMethod.PUT, TRIP_OF + "/activities/reorder", Level.EDIT,
                    HttpStatus.OK, """
                    { "items": [ { "activityId": {activityId}, "dayId": {dayId}, "orderIndex": 500 } ] }"""),
            new Action("Xoá hoạt động", HttpMethod.DELETE, TRIP_OF + "/activities/{activityId}", Level.EDIT,
                    HttpStatus.OK, null),
            // Owner only
            new Action("Xoá chuyến đi", HttpMethod.DELETE, TRIP, Level.OWNER, HttpStatus.OK, null),
            new Action("Mời thành viên", HttpMethod.POST, TRIP_OF + "/members", Level.OWNER, HttpStatus.CREATED, """
                    { "email": "new@example.com", "role": "VIEWER" }"""),
            new Action("Đổi vai trò thành viên", HttpMethod.PATCH, TRIP_OF + "/members/{memberId}", Level.OWNER,
                    HttpStatus.OK, """
                    { "role": "EDITOR" }"""),
            new Action("Gỡ thành viên", HttpMethod.DELETE, TRIP_OF + "/members/{memberId}", Level.OWNER,
                    HttpStatus.OK, null),
            new Action("Tạo liên kết chia sẻ", HttpMethod.POST, TRIP_OF + "/share-links", Level.OWNER,
                    HttpStatus.CREATED, null),
            new Action("Xem liên kết chia sẻ", HttpMethod.GET, TRIP_OF + "/share-links", Level.OWNER,
                    HttpStatus.OK, null),
            new Action("Thu hồi liên kết chia sẻ", HttpMethod.DELETE, TRIP_OF + "/share-links/{linkId}", Level.OWNER,
                    HttpStatus.OK, null));

    /**
     * Trip endpoints left out of the evaluator on purpose. Accepting an invitation: the caller has no right on the
     * trip yet, that is what the call is for (the token is the check). The public page: a share link, no account.
     */
    private static final Set<String> NOT_GUARDED_ON_PURPOSE = Set.of(
            "POST /api/v1/trips/{tripId}/members/accept",
            "GET /api/v1/public/trips/{token}");

    /** {@code @tripPermission.canView(#tripId, principal)} → method name and the argument it reads the trip id from. */
    private static final Pattern GUARD = Pattern.compile("@tripPermission\\.(\\w+)\\(#(\\w+), principal\\)");

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

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
    private MockMailProvider mailProvider;

    private final Map<Caller, String> bearers = new EnumMap<>(Caller.class);

    /** The ids the placeholders of {@link #ACTIONS} stand for. */
    private final Map<String, Long> ids = new TreeMap<>();

    /**
     * A trip 01/10 → 03/10 of the owner, with one activity on day 1, one share link, and one member row for each
     * of EDITOR, VIEWER (ACCEPTED), PENDING and REMOVED; the stranger has an account and no row.
     */
    @BeforeEach
    void createTripWithOneMemberOfEveryKind() {
        mailProvider.clear();
        Map<Caller, User> users = new EnumMap<>(Caller.class);
        for (Caller caller : Caller.values()) {
            if (caller != Caller.GUEST) {
                User user = userRepository.save(user(caller.name().toLowerCase() + "@example.com"));
                users.put(caller, user);
                bearers.put(caller, "Bearer " + jwtTokenProvider.generateAccessToken(user).token());
            }
        }

        long tripId = idOf(send(HttpMethod.POST, "/api/v1/trips", bearers.get(Caller.OWNER), """
                { "title": "Đà Lạt", "startDate": "2026-10-01", "endDate": "2026-10-03" }"""));
        ids.put("id", tripId);
        ids.put("tripId", tripId);
        long dayId = ((Number) JsonPath.read(body(send(HttpMethod.GET, "/api/v1/trips/" + tripId + "/days",
                bearers.get(Caller.OWNER), null)), "$.data[0].id")).longValue();
        ids.put("dayId", dayId);
        ids.put("activityId", idOf(send(HttpMethod.POST, "/api/v1/trips/" + tripId + "/days/" + dayId + "/activities",
                bearers.get(Caller.OWNER), """
                { "title": "Ăn sáng" }""")));
        ids.put("linkId", idOf(send(HttpMethod.POST, "/api/v1/trips/" + tripId + "/share-links",
                bearers.get(Caller.OWNER), null)));

        Trip trip = tripRepository.findById(tripId).orElseThrow();
        User owner = users.get(Caller.OWNER);
        ids.put("memberId", tripMemberRepository.save(
                member(trip, owner, users.get(Caller.VIEWER), MemberRole.VIEWER, MemberStatus.ACCEPTED)).getId());
        tripMemberRepository.save(member(trip, owner, users.get(Caller.EDITOR), MemberRole.EDITOR, MemberStatus.ACCEPTED));
        tripMemberRepository.save(member(trip, owner, users.get(Caller.PENDING), MemberRole.EDITOR, MemberStatus.PENDING));
        tripMemberRepository.save(member(trip, owner, users.get(Caller.REMOVED), MemberRole.EDITOR, MemberStatus.REMOVED));
    }

    @AfterEach
    void cleanUp() {
        // Days, activities, members and share links go with their trips (ON DELETE CASCADE); trips before users
        jdbcTemplate.update("DELETE FROM trips");
        jdbcTemplate.update("DELETE FROM users");
        mailProvider.clear();
    }

    // ---- design.md 6.2, every cell ----------------------------------------------------------------------------

    static Stream<Arguments> everyCell() {
        return ACTIONS.stream().flatMap(action -> Arrays.stream(Caller.values()).map(caller -> Arguments.of(action, caller)));
    }

    @ParameterizedTest(name = "{1} × {0}")
    @MethodSource("everyCell")
    void everyCallerGetsWhatTheMatrixPromises(Action action, Caller caller) {
        HttpStatus expected = expectedStatus(action, caller);

        MvcTestResult result = call(action, caller);

        assertThat(result).as("%s × %s", caller, action).hasStatus(expected);
        if (expected == HttpStatus.UNAUTHORIZED || expected == HttpStatus.FORBIDDEN) {
            assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo(expected.name());
        }
    }

    /** The whole of design.md 6.2 in one rule. */
    private static HttpStatus expectedStatus(Action action, Caller caller) {
        if (caller == Caller.GUEST) {
            return HttpStatus.UNAUTHORIZED;
        }
        return action.level().allowed.contains(caller) ? action.success() : HttpStatus.FORBIDDEN;
    }

    // ---- a deleted trip is not found, whoever asks (design.md 6.2, CLAUDE.md rule 15) ---------------------------

    static Stream<Arguments> everyActionOnADeletedTrip() {
        return ACTIONS.stream().flatMap(action -> Stream.of(Caller.OWNER, Caller.EDITOR, Caller.STRANGER)
                .map(caller -> Arguments.of(action, caller)));
    }

    @ParameterizedTest(name = "{1} × {0}")
    @MethodSource("everyActionOnADeletedTrip")
    void aDeletedTripIsNotFoundForTheOwnerAMemberAndAStranger(Action action, Caller caller) {
        assertThat(send(HttpMethod.DELETE, "/api/v1/trips/" + ids.get("tripId"), bearers.get(Caller.OWNER), null))
                .hasStatusOk();

        assertThat(call(action, caller)).as("%s × %s on a deleted trip", caller, action)
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("RESOURCE_NOT_FOUND");
    }

    // ---- no trip endpoint escapes the matrix ---------------------------------------------------------------------

    @Test
    void everyTripEndpointIsGuardedByTheEvaluatorAndListedInTheMatrix() {
        Map<String, String> served = new TreeMap<>();
        handlerMapping.getHandlerMethods().forEach((info, handler) -> {
            for (String pattern : info.getPatternValues()) {
                if (!pattern.contains("/trips/{")) {
                    continue;
                }
                PreAuthorize guard = handler.getMethodAnnotation(PreAuthorize.class);
                for (RequestMethod method : info.getMethodsCondition().getMethods()) {
                    served.put(method.name() + " " + pattern, guard == null ? "none" : describe(guard.value()));
                }
            }
        });

        // The exceptions exist (a renamed path would otherwise slip out of the check unnoticed) and are unguarded
        assertThat(served).containsKeys(NOT_GUARDED_ON_PURPOSE.toArray(String[]::new));
        NOT_GUARDED_ON_PURPOSE.forEach(served::remove);

        // Every other trip endpoint: in the table, guarded by the evaluator method of its level, reading the trip id
        // from the path variable that holds it. And every row of the table is an endpoint that exists
        Map<String, String> expected = ACTIONS.stream().collect(Collectors.toMap(Action::endpoint,
                action -> action.level().evaluatorMethod + "(#" + tripVariable(action.pattern()) + ")",
                (first, second) -> first, TreeMap::new));
        assertThat(served).isEqualTo(expected);
    }

    /** {@code @tripPermission.canEdit(#tripId, principal)} → {@code canEdit(#tripId)}; anything else as written. */
    private static String describe(String expression) {
        Matcher matcher = GUARD.matcher(expression);
        return matcher.matches() ? matcher.group(1) + "(#" + matcher.group(2) + ")" : expression;
    }

    private static String tripVariable(String pattern) {
        return pattern.contains("{tripId}") ? "tripId" : "id";
    }

    // ---- helpers -------------------------------------------------------------------------------------------------

    private MvcTestResult call(Action action, Caller caller) {
        return send(action.method(), fill(action.pattern()), bearers.get(caller),
                action.body() == null ? null : fill(action.body()));
    }

    /** Replaces every {name} of the text by the id of the fixture with that name. */
    private String fill(String text) {
        String filled = text;
        for (Map.Entry<String, Long> id : ids.entrySet()) {
            filled = filled.replace("{" + id.getKey() + "}", id.getValue().toString());
        }
        return filled;
    }

    private MvcTestResult send(HttpMethod method, String url, String bearer, String json) {
        var request = mvc.method(method).uri(url);
        if (bearer != null) {
            request = request.header(HttpHeaders.AUTHORIZATION, bearer);
        }
        if (json != null) {
            request = request.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return request.exchange();
    }

    private static TripMember member(Trip trip, User owner, User user, MemberRole role, MemberStatus status) {
        return TripMember.builder()
                .trip(trip)
                .user(user)
                .invitedEmail(user.getEmail())
                .role(role)
                .status(status)
                .inviteTokenHash(status == MemberStatus.PENDING ? "0".repeat(64) : null)
                .inviteExpiresAt(status == MemberStatus.PENDING ? Instant.now().plusSeconds(3600) : null)
                .invitedBy(owner)
                .invitedAt(SEPT_1)
                .acceptedAt(status == MemberStatus.PENDING ? null : SEPT_1.plusSeconds(60))
                .build();
    }

    private static long idOf(MvcTestResult result) {
        assertThat(result.getResponse().getStatus()).isBetween(200, 201);
        return ((Number) JsonPath.read(body(result), "$.data.id")).longValue();
    }

    // Explicit UTF-8: MockHttpServletResponse otherwise decodes as ISO-8859-1 and mangles Vietnamese
    private static String body(MvcTestResult result) {
        return new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
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

package com.trieu.tripplanner.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.trieu.tripplanner.TestcontainersConfiguration;
import com.trieu.tripplanner.common.constant.CacheNames;
import com.trieu.tripplanner.dto.internal.TripAccess;
import com.trieu.tripplanner.dto.request.ChangeMemberRoleRequest;
import com.trieu.tripplanner.model.TripMember;
import com.trieu.tripplanner.model.User;
import com.trieu.tripplanner.model.enums.MemberRole;
import com.trieu.tripplanner.model.enums.MemberStatus;
import com.trieu.tripplanner.repository.TripMemberRepository;
import com.trieu.tripplanner.repository.TripRepository;
import com.trieu.tripplanner.repository.UserRepository;
import com.trieu.tripplanner.security.JwtTokenProvider;
import com.trieu.tripplanner.security.permission.TripAccessCache;
import com.trieu.tripplanner.service.SharingService;
import jakarta.persistence.EntityManagerFactory;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The permission cache against a real Redis and a real MySQL (design.md 6.2, 8.1 "trip:permission"). What the
 * permission matrix cannot see: where the answer is stored and for how long, that a second check asks the database
 * nothing, that "no such trip" is never stored, that a removal touches one account only, that a stored refusal
 * is never the last word (BUG-SHARE-002), and that a removal waits for the change to be committed.
 * <p>
 * That a change of role, a removal or an acceptance takes effect on the very next request is proven over HTTP by
 * SharingFlowIntegrationTest, which now runs with this cache in front of every check.
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class TripAccessCacheIntegrationTest {

    private static final Instant SEPT_1 = Instant.parse("2026-09-01T00:00:00Z");

    @Autowired
    private TripAccessCache tripAccessCache;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

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
    private SharingService sharingService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private User owner;
    private User editor;
    private String ownerBearer;
    private long tripId;
    private long editorMemberId;

    /** A trip of the owner with one accepted EDITOR. */
    @BeforeEach
    void createTripWithAnEditor() {
        owner = userRepository.save(user("owner@example.com"));
        editor = userRepository.save(user("editor@example.com"));
        ownerBearer = bearerFor(owner);
        MvcTestResult created = mvc.post().uri("/api/v1/trips").header(HttpHeaders.AUTHORIZATION, ownerBearer)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        { "title": "Đà Lạt", "startDate": "2026-10-01", "endDate": "2026-10-03" }""").exchange();
        assertThat(created).hasStatus(HttpStatus.CREATED);
        tripId = ((Number) JsonPath.read(new String(created.getResponse().getContentAsByteArray(),
                StandardCharsets.UTF_8), "$.data.id")).longValue();
        editorMemberId = acceptedMember(editor, MemberRole.EDITOR);
        // Creating the trip asked no permission; start from an empty cache anyway
        cacheManager.getCache(CacheNames.TRIP_PERMISSION).clear();
    }

    @AfterEach
    void cleanUp() {
        cacheManager.getCache(CacheNames.TRIP_PERMISSION).clear();
        jdbcTemplate.update("DELETE FROM trips");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void firstCheckStoresTheAccessInRedisForFiveMinutesAsPlainJson() {
        String key = redisKey(editor.getId(), tripId);
        assertThat(redis.hasKey(key)).isFalse();

        Optional<TripAccess> access = tripAccessCache.find(tripId, editor.getId());

        assertThat(access).contains(new TripAccess(owner.getId(), MemberRole.EDITOR));
        assertThat(redis.hasKey(key)).isTrue();
        long secondsLeft = redis.getExpire(key, TimeUnit.SECONDS);
        assertThat(secondsLeft).isBetween(TimeUnit.MINUTES.toSeconds(5) - 60, TimeUnit.MINUTES.toSeconds(5));
        // The two facts and nothing else: no Java class name a reader of Redis could make the application build
        assertThat(redis.opsForValue().get(key))
                .isEqualTo("{\"ownerId\":" + owner.getId() + ",\"memberRole\":\"EDITOR\"}");
    }

    @Test
    void secondCheckIsAnsweredFromRedisWithoutAnySql() {
        Statistics statistics = statistics();
        statistics.clear();
        Optional<TripAccess> first = tripAccessCache.find(tripId, owner.getId());
        assertThat(statistics.getPrepareStatementCount()).as("first check reads the database").isEqualTo(1);

        statistics.clear();
        Optional<TripAccess> second = tripAccessCache.find(tripId, owner.getId());

        assertThat(statistics.getPrepareStatementCount()).as("second check").isZero();
        // Read back as the same record, the owner's null role included
        assertThat(second).isEqualTo(first).contains(new TripAccess(owner.getId(), null));
    }

    @Test
    void aTripThatDoesNotExistIsNeverStored() {
        long missingTripId = tripId + 1000;

        assertThat(tripAccessCache.find(missingTripId, owner.getId())).isEmpty();

        // Stored, "missing" would keep letting this account through once a trip gets that id (design.md 6.2)
        assertThat(redis.hasKey(redisKey(owner.getId(), missingTripId))).isFalse();
        Statistics statistics = statistics();
        statistics.clear();
        assertThat(tripAccessCache.find(missingTripId, owner.getId())).isEmpty();
        assertThat(statistics.getPrepareStatementCount()).as("asked to the database again").isEqualTo(1);
    }

    @Test
    void evictForgetsOneAccountOnOneTripOnly() {
        tripAccessCache.find(tripId, owner.getId());
        tripAccessCache.find(tripId, editor.getId());

        tripAccessCache.evict(tripId, editor.getId());

        assertThat(redis.hasKey(redisKey(editor.getId(), tripId))).isFalse();
        assertThat(redis.hasKey(redisKey(owner.getId(), tripId))).as("the owner's entry stays").isTrue();
    }

    @Test
    void aTripDeletedAfterItsAccessWasStoredIsStillNotFound() {
        String tripUrl = "/api/v1/trips/" + tripId;
        assertThat(mvc.get().uri(tripUrl).header(HttpHeaders.AUTHORIZATION, ownerBearer)).hasStatusOk();
        assertThat(redis.hasKey(redisKey(owner.getId(), tripId))).isTrue();

        assertThat(mvc.delete().uri(tripUrl).header(HttpHeaders.AUTHORIZATION, ownerBearer)).hasStatusOk();

        // The stored "owner" answer lets the request past the check; the service knows the trip is gone
        assertThat(redis.hasKey(redisKey(owner.getId(), tripId))).isTrue();
        assertThat(mvc.get().uri(tripUrl).header(HttpHeaders.AUTHORIZATION, ownerBearer))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("RESOURCE_NOT_FOUND");
    }

    // ---- a stored refusal is read again (BUG-SHARE-002) ---------------------------------------------------------

    @Test
    void aStrangerWhoseRefusalWasStoredGets404OnceTheTripIsDeleted() {
        String strangerBearer = bearerFor(userRepository.save(user("stranger@example.com")));
        String tripUrl = "/api/v1/trips/" + tripId;
        assertThat(mvc.get().uri(tripUrl).header(HttpHeaders.AUTHORIZATION, strangerBearer))
                .hasStatus(HttpStatus.FORBIDDEN);

        assertThat(mvc.delete().uri(tripUrl).header(HttpHeaders.AUTHORIZATION, ownerBearer)).hasStatusOk();

        // A deleted trip answers 404 to everybody (CLAUDE.md rule 15), whatever was stored before
        assertThat(mvc.get().uri(tripUrl).header(HttpHeaders.AUTHORIZATION, strangerBearer))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    void aViewerWhoseAccessWasStoredGets404WhenEditingTheDeletedTrip() {
        User viewer = userRepository.save(user("viewer@example.com"));
        acceptedMember(viewer, MemberRole.VIEWER);
        String viewerBearer = bearerFor(viewer);
        String tripUrl = "/api/v1/trips/" + tripId;
        assertThat(mvc.get().uri(tripUrl).header(HttpHeaders.AUTHORIZATION, viewerBearer)).hasStatusOk();

        assertThat(mvc.delete().uri(tripUrl).header(HttpHeaders.AUTHORIZATION, ownerBearer)).hasStatusOk();

        // "Viewer" is stored: allowed to view, refused to edit. The refusal is read again and the trip is gone
        assertThat(mvc.patch().uri(tripUrl).header(HttpHeaders.AUTHORIZATION, viewerBearer)
                .contentType(MediaType.APPLICATION_JSON).content("{ \"title\": \"Sau khi xoá\" }"))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void aStoredRefusalIsReadAgainAndReplacedByTheCurrentAccess() {
        User newcomer = userRepository.save(user("newcomer@example.com"));
        String newcomerBearer = bearerFor(newcomer);
        String tripUrl = "/api/v1/trips/" + tripId;
        assertThat(mvc.get().uri(tripUrl).header(HttpHeaders.AUTHORIZATION, newcomerBearer))
                .hasStatus(HttpStatus.FORBIDDEN);
        assertThat(redis.opsForValue().get(redisKey(newcomer.getId(), tripId))).contains("\"memberRole\":null");

        // Written straight to the table, so nothing removed the stored "stranger"
        acceptedMember(newcomer, MemberRole.VIEWER);

        assertThat(mvc.get().uri(tripUrl).header(HttpHeaders.AUTHORIZATION, newcomerBearer)).hasStatusOk();
        assertThat(redis.opsForValue().get(redisKey(newcomer.getId(), tripId))).contains("\"memberRole\":\"VIEWER\"");
    }

    // ---- the entry goes once the change is committed (design.md 8.1 "Xoá sau commit") ----------------------------

    @Test
    void aChangeOfRoleRemovesTheStoredAccessOnlyOnceItIsCommitted() {
        String key = redisKey(editor.getId(), tripId);
        tripAccessCache.find(tripId, editor.getId());

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            sharingService.changeRole(tripId, editorMemberId, new ChangeMemberRoleRequest(MemberRole.VIEWER));
            // Removed now, a check running alongside could read the old role (not committed yet) and store it again
            // for 5 minutes: the editor would keep editing after the demotion
            assertThat(redis.hasKey(key)).as("before the commit").isTrue();
        });

        assertThat(redis.hasKey(key)).as("after the commit").isFalse();
        assertThat(tripAccessCache.find(tripId, editor.getId())).contains(new TripAccess(owner.getId(), MemberRole.VIEWER));
    }

    @Test
    void aChangeThatIsRolledBackKeepsTheStoredAccess() {
        String key = redisKey(editor.getId(), tripId);
        tripAccessCache.find(tripId, editor.getId());

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            sharingService.remove(tripId, editorMemberId);
            status.setRollbackOnly();
        });

        // Nothing changed in the database, so the stored answer is still the right one
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM trip_members WHERE id = ?", String.class,
                editorMemberId)).isEqualTo("ACCEPTED");
        assertThat(redis.hasKey(key)).isTrue();
    }

    // ---- helpers -------------------------------------------------------------------------------------------------

    private long acceptedMember(User user, MemberRole role) {
        return tripMemberRepository.save(TripMember.builder()
                .trip(tripRepository.findById(tripId).orElseThrow()).user(user).invitedEmail(user.getEmail())
                .role(role).status(MemberStatus.ACCEPTED)
                .invitedBy(owner).invitedAt(SEPT_1).acceptedAt(SEPT_1.plusSeconds(60))
                .build()).getId();
    }

    private String bearerFor(User user) {
        return "Bearer " + jwtTokenProvider.generateAccessToken(user).token();
    }

    /** How Spring names the entry in Redis: the cache name, two colons, then our key. */
    private static String redisKey(long userId, long tripId) {
        return CacheNames.TRIP_PERMISSION + "::" + userId + ":" + tripId;
    }

    private Statistics statistics() {
        return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
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

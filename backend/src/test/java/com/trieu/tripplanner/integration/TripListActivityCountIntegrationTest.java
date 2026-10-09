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
import jakarta.persistence.EntityManagerFactory;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
 * The trip cards (GET /trips) against real MySQL: the activity count of each trip, the owner of a shared trip,
 * and a query count that grows neither with the number of trips on the page nor with the number of shared ones
 * (one grouped activity count, owners fetched with the trips, no N+1).
 * Same Spring context as ActivityReorderFlowIntegrationTest (Hibernate statistics on), so it is reused.
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class TripListActivityCountIntegrationTest {

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

    private User owner;
    private User stranger;
    private String ownerBearer;
    private String strangerBearer;

    @BeforeEach
    void createUsers() {
        owner = userRepository.save(user("owner@example.com"));
        stranger = userRepository.save(user("stranger@example.com"));
        ownerBearer = bearerFor(owner);
        strangerBearer = bearerFor(stranger);
    }

    @AfterEach
    void cleanUp() {
        // trip_days, activities and trip_members go with their trips (ON DELETE CASCADE); trips before users
        jdbcTemplate.update("DELETE FROM trips");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void eachCardCountsAllActivitiesOfItsTripOverEveryDay() {
        long daLat = createTrip(ownerBearer, "Đà Lạt", "2026-10-01", "2026-10-02");
        long hue = createTrip(ownerBearer, "Huế", "2026-11-01", "2026-11-01");
        createTrip(ownerBearer, "Chưa lên lịch", "2026-12-01", "2026-12-03");
        long strangers = createTrip(strangerBearer, "Của người khác", "2026-10-01", "2026-10-01");
        List<Long> daLatDays = dayIds(daLat);
        addActivity(ownerBearer, daLat, daLatDays.get(0), "Ăn sáng");
        addActivity(ownerBearer, daLat, daLatDays.get(0), "Chợ Đà Lạt");
        addActivity(ownerBearer, daLat, daLatDays.get(1), "Hồ Xuân Hương");
        addActivity(ownerBearer, hue, dayIds(hue).get(0), "Đại Nội");
        addActivity(strangerBearer, strangers, dayIds(strangers).get(0), "Không được đếm cho chủ");

        MvcTestResult list = list(ownerBearer);

        assertThat(list).hasStatusOk();
        assertThat(countsByTitle(list)).containsExactlyInAnyOrderEntriesOf(Map.of(
                "Đà Lạt", 3, "Huế", 1, "Chưa lên lịch", 0));
    }

    @Test
    void queryCountDoesNotGrowWithTheNumberOfTripsOnThePage() {
        long first = createTrip(ownerBearer, "Chuyến 1", "2026-10-01", "2026-10-01");
        addActivity(ownerBearer, first, dayIds(first).get(0), "Hoạt động 1");
        long oneTrip = statementsForList(ownerBearer);

        for (int i = 2; i <= 6; i++) {
            long trip = createTrip(ownerBearer, "Chuyến " + i, "2026-10-01", "2026-10-01");
            addActivity(ownerBearer, trip, dayIds(trip).get(0), "Hoạt động " + i);
        }
        long sixTrips = statementsForList(ownerBearer);

        // trips of the page + one grouped activity count; no count per card
        assertThat(sixTrips).isEqualTo(oneTrip);
        assertThat(oneTrip).isEqualTo(2);
    }

    // ---- shared trips on the cards (design.md 10.2 "Chuyến đi được chia sẻ trong danh sách") --------------------

    @Test
    void sharedTripsAreListedWithTheirOwnerAndCountedWithoutAnExtraQueryPerCard() {
        long shared = createTrip(ownerBearer, "Của chủ, chia sẻ cho tôi", "2026-10-01", "2026-10-01");
        long onlyInvited = createTrip(ownerBearer, "Mới mời, chưa nhận", "2026-10-02", "2026-10-02");
        createTrip(strangerBearer, "Của tôi", "2026-10-03", "2026-10-03");
        share(shared, stranger, MemberStatus.ACCEPTED);
        share(onlyInvited, stranger, MemberStatus.PENDING);
        addActivity(ownerBearer, shared, dayIds(shared).get(0), "Ăn sáng");

        MvcTestResult list = list(strangerBearer);

        assertThat(list).hasStatusOk();
        assertThat(JsonPath.<List<String>>read(body(list), "$.data.items[*].title"))
                .containsExactlyInAnyOrder("Của chủ, chia sẻ cho tôi", "Của tôi");
        // The shared card names its owner, so the UI can show "Được chia sẻ · của {ownerName}"
        assertThat(JsonPath.<List<Integer>>read(body(list), "$.data.items[?(@.title == 'Của chủ, chia sẻ cho tôi')].ownerId"))
                .containsExactly(owner.getId().intValue());
        assertThat(JsonPath.<List<String>>read(body(list), "$.data.items[?(@.title == 'Của chủ, chia sẻ cho tôi')].ownerName"))
                .containsExactly("Test owner@example.com");
        assertThat(JsonPath.<List<Integer>>read(body(list), "$.data.items[?(@.title == 'Của tôi')].ownerId"))
                .containsExactly(stranger.getId().intValue());
        assertThat(countsByTitle(list)).containsEntry("Của chủ, chia sẻ cho tôi", 1);

        // The chips agree with the list: the shared trip counts, the pending one does not
        assertThat(mvc.get().uri(TRIPS_URL + "/status-counts").header(HttpHeaders.AUTHORIZATION, strangerBearer))
                .hasStatusOk().bodyJson().extractingPath("$.data.total").isEqualTo(2);

        // Owners come with the trips: still trips of the page + one grouped activity count
        assertThat(statementsForList(strangerBearer)).isEqualTo(2);
    }

    // ---- helpers -------------------------------------------------------------------------------------------------

    private long statementsForList(String bearer) {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        assertThat(list(bearer)).hasStatusOk();
        return statistics.getPrepareStatementCount();
    }

    /** A membership row written directly: inviting and accepting have their own tests (SharingFlowIntegrationTest). */
    private void share(long tripId, User user, MemberStatus status) {
        Trip trip = tripRepository.findById(tripId).orElseThrow();
        tripMemberRepository.save(TripMember.builder()
                .trip(trip)
                .user(user)
                .invitedEmail(user.getEmail())
                .role(MemberRole.VIEWER)
                .status(status)
                .inviteTokenHash(status == MemberStatus.PENDING ? "hash-for-" + user.getEmail() : null)
                .inviteExpiresAt(status == MemberStatus.PENDING ? Instant.now().plusSeconds(3600) : null)
                .invitedBy(trip.getOwner())
                .invitedAt(Instant.parse("2026-09-01T00:00:00Z"))
                .acceptedAt(status == MemberStatus.ACCEPTED ? Instant.parse("2026-09-01T00:01:00Z") : null)
                .build());
    }

    private MvcTestResult list(String bearer) {
        return mvc.get().uri(TRIPS_URL).header(HttpHeaders.AUTHORIZATION, bearer).exchange();
    }

    private static Map<String, Integer> countsByTitle(MvcTestResult result) {
        List<String> titles = JsonPath.read(body(result), "$.data.items[*].title");
        List<Integer> counts = JsonPath.read(body(result), "$.data.items[*].activityCount");
        assertThat(titles).hasSameSizeAs(counts);
        return IntStream.range(0, titles.size()).boxed().collect(Collectors.toMap(titles::get, counts::get));
    }

    private long createTrip(String bearer, String title, String start, String end) {
        MvcTestResult created = mvc.post().uri(TRIPS_URL).header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "title": "%s", "startDate": "%s", "endDate": "%s" }
                        """.formatted(title, start, end))
                .exchange();
        assertThat(created).hasStatus(HttpStatus.CREATED);
        return ((Number) JsonPath.read(body(created), "$.data.id")).longValue();
    }

    private List<Long> dayIds(long tripId) {
        return jdbcTemplate.queryForList("SELECT id FROM trip_days WHERE trip_id = ? ORDER BY day_index", Long.class, tripId);
    }

    private void addActivity(String bearer, long tripId, long dayId, String title) {
        assertThat(mvc.post().uri(TRIPS_URL + "/" + tripId + "/days/" + dayId + "/activities")
                .header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "title": "%s" }
                        """.formatted(title)))
                .hasStatus(HttpStatus.CREATED);
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

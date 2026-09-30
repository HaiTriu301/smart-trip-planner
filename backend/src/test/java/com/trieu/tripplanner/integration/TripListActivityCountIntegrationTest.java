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
 * Activity count on the trip cards (GET /trips) against real MySQL: the number of each trip, and a query count
 * that does not grow with the number of trips on the page (one grouped count, no N+1).
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
        // trip_days and activities go with their trips (ON DELETE CASCADE); trips must go before users (no cascade)
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
        long oneTrip = statementsForList();

        for (int i = 2; i <= 6; i++) {
            long trip = createTrip(ownerBearer, "Chuyến " + i, "2026-10-01", "2026-10-01");
            addActivity(ownerBearer, trip, dayIds(trip).get(0), "Hoạt động " + i);
        }
        long sixTrips = statementsForList();

        // trips of the page + one grouped activity count; no count per card
        assertThat(sixTrips).isEqualTo(oneTrip);
        assertThat(oneTrip).isEqualTo(2);
    }

    // ---- helpers -------------------------------------------------------------------------------------------------

    private long statementsForList() {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        assertThat(list(ownerBearer)).hasStatusOk();
        return statistics.getPrepareStatementCount();
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

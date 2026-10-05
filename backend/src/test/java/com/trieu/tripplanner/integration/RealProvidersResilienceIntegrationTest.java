package com.trieu.tripplanner.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.trieu.tripplanner.TestcontainersConfiguration;
import com.trieu.tripplanner.exception.ProviderUnavailableException;
import com.trieu.tripplanner.model.User;
import com.trieu.tripplanner.model.enums.PlaceProvider;
import com.trieu.tripplanner.provider.map.MapProvider;
import com.trieu.tripplanner.provider.map.dto.Coordinate;
import com.trieu.tripplanner.provider.map.dto.PlaceResult;
import com.trieu.tripplanner.provider.weather.WeatherProvider;
import com.trieu.tripplanner.repository.UserRepository;
import com.trieu.tripplanner.security.JwtTokenProvider;
import com.trieu.tripplanner.support.StubHttpServer;
import com.trieu.tripplanner.support.StubHttpServer.Answer;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * The whole application with the real providers switched on (design.md 7.3). Retry, circuit breaker and rate
 * limiter are annotations that only work through the proxies of a running application, so nothing less than
 * the real context can show them. The four outside services are one stand-in server on this machine, told apart
 * by the path each provider calls; no test leaves the machine (CLAUDE.md rule 24).
 * <p>
 * Two values differ from application.yml, to keep the tests short: a call gives up after 0.3 s instead of 3 s,
 * and a retry waits 20 ms instead of 500 ms. The limits that are being proven (three attempts, ten calls, one
 * lookup a second) are the real ones.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class RealProvidersResilienceIntegrationTest {

    private static final StubHttpServer SERVICES = StubHttpServer.scripted();

    private static final String PHOTON = "/api/";
    private static final String NOMINATIM = "/lookup";
    private static final String OSRM = "/route/";
    private static final String OPEN_METEO = "/v1/forecast";

    private static final String PLACES = sample("provider/osm/photon-search-cho-han.json");
    private static final String PLACE = sample("provider/osm/nominatim-lookup-cho-han.json");
    private static final String ROUTE = sample("provider/osm/osrm-route-three-stops.json");
    private static final String FORECAST = sample("provider/open-meteo/forecast-16-days.json");

    private static final Coordinate CHO_HAN = point("16.0683525", "108.2242830");
    private static final Coordinate CAU_RONG = point("16.0611000", "108.2272000");
    private static final Coordinate MY_KHE = point("16.0600000", "108.2470000");
    private static final List<Coordinate> THREE_STOPS = List.of(CHO_HAN, CAU_RONG, MY_KHE);

    /** minimum-number-of-calls of the circuit breakers in application.yml. */
    private static final int CALLS_BEFORE_A_CIRCUIT_CAN_OPEN = 10;

    @DynamicPropertySource
    static void realProvidersAtTheStandInServer(DynamicPropertyRegistry registry) {
        registry.add("app.providers.map", () -> "osm");
        registry.add("app.providers.weather", () -> "open-meteo");
        registry.add("app.providers.osm.photon-base-url", SERVICES::baseUrl);
        registry.add("app.providers.osm.nominatim-base-url", SERVICES::baseUrl);
        registry.add("app.providers.osm.osrm-base-url", SERVICES::baseUrl);
        registry.add("app.providers.open-meteo.base-url", SERVICES::baseUrl);
        registry.add("spring.http.clients.read-timeout", () -> "300ms");
        registry.add("resilience4j.retry.configs.default.wait-duration", () -> "20ms");
    }

    @AfterAll
    static void stopTheStandInServer() {
        SERVICES.close();
    }

    @Autowired
    private MapProvider mapProvider;

    @Autowired
    private WeatherProvider weatherProvider;

    @Autowired
    private CircuitBreakerRegistry circuitBreakers;

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void everyServiceAnswersAndEveryCircuitIsClosed() {
        SERVICES.reset();
        SERVICES.script(PHOTON, Answer.ok(PLACES));
        SERVICES.script(NOMINATIM, Answer.ok(PLACE));
        SERVICES.script(OSRM, Answer.ok(ROUTE));
        SERVICES.script(OPEN_METEO, Answer.ok(FORECAST));
        circuitBreakers.getAllCircuitBreakers().forEach(CircuitBreaker::reset);
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM users");
    }

    // ---- retry ---------------------------------------------------------------------------------------------------

    @Test
    void searchThatFailsTwiceWithA5xxSucceedsOnTheThirdAttempt() {
        SERVICES.script(PHOTON, Answer.status(503), Answer.status(500), Answer.ok(PLACES));

        List<PlaceResult> places = mapProvider.search("chợ hàn", 8, null);

        assertThat(places).extracting(PlaceResult::name).contains("Chợ Hàn");
        assertThat(SERVICES.calls(PHOTON)).isEqualTo(3);
    }

    @Test
    void serviceThatKeepsFailingIsCalledThreeTimesThenTheFailureIsReported() {
        SERVICES.script(OSRM, Answer.status(502));

        assertUnavailable("osrm", () -> mapProvider.route(THREE_STOPS));

        assertThat(SERVICES.calls(OSRM)).isEqualTo(3);
    }

    @Test
    void serviceThatDoesNotAnswerInTimeIsTriedAgainToo() {
        SERVICES.script(OPEN_METEO, Answer.silence(), Answer.silence(), Answer.ok(FORECAST));

        assertThat(weatherProvider.forecast(CHO_HAN.lat(), CHO_HAN.lng(), LocalDate.of(2026, 10, 5),
                LocalDate.of(2026, 10, 7))).isNotNull();

        assertThat(SERVICES.calls(OPEN_METEO)).isEqualTo(3);
    }

    @Test
    void requestTheServiceRefusesIsNotTriedAgain() {
        // 400 is how OSRM says "no way between these points": asking again changes nothing
        SERVICES.script(OSRM, Answer.status(400));

        assertUnavailable("osrm", () -> mapProvider.route(THREE_STOPS));

        assertThat(SERVICES.calls(OSRM)).isEqualTo(1);
    }

    @Test
    void answerThatCannotBeReadIsNotTriedAgain() {
        SERVICES.script(PHOTON, Answer.ok("not json at all"));

        assertUnavailable("photon", () -> mapProvider.search("chợ hàn", 8, null));

        assertThat(SERVICES.calls(PHOTON)).isEqualTo(1);
    }

    // ---- circuit breaker -----------------------------------------------------------------------------------------

    @Test
    void serviceThatIsDownStopsBeingCalledAndTheOthersGoOn() {
        SERVICES.script(PHOTON, Answer.status(500));

        // Every search fails, each after up to three calls, until the circuit has seen enough
        for (int search = 1; search <= 6; search++) {
            assertUnavailable("photon", () -> mapProvider.search("chợ hàn", 8, null));
        }

        // Ten calls, all failed: from the eleventh on Photon is left alone, and the failure comes at once
        assertThat(SERVICES.calls(PHOTON)).isEqualTo(CALLS_BEFORE_A_CIRCUIT_CAN_OPEN);
        assertThat(circuitBreakers.circuitBreaker("photon").getState()).isEqualTo(CircuitBreaker.State.OPEN);
        Instant start = Instant.now();
        assertUnavailable("photon", () -> mapProvider.search("chợ hàn", 8, null));
        assertThat(Duration.between(start, Instant.now())).isLessThan(Duration.ofMillis(200));

        // One circuit per service: routes and forecasts are not touched by Photon being down
        assertThat(mapProvider.route(THREE_STOPS)).hasSize(2);
        assertThat(weatherProvider.forecast(CHO_HAN.lat(), CHO_HAN.lng(), LocalDate.of(2026, 10, 5),
                LocalDate.of(2026, 10, 7))).isNotNull();
        assertThat(circuitBreakers.circuitBreaker("osrm").getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void manyCallsAtTheSameMomentAllGetTheSameFailureWhileTheCircuitIsOpen() throws Exception {
        // Each of the four circuits in turn: every provider method has a fallback of its own (BUG-PLAT-004:
        // with private fallbacks, some of these calls ended in an IllegalAccessException)
        List<Callable<Object>> calls = List.of(
                () -> mapProvider.search("chợ hàn", 8, null),
                () -> mapProvider.route(THREE_STOPS),
                () -> weatherProvider.forecast(CHO_HAN.lat(), CHO_HAN.lng(), LocalDate.of(2026, 10, 5),
                        LocalDate.of(2026, 10, 7)));
        circuitBreakers.getAllCircuitBreakers().forEach(CircuitBreaker::transitionToOpenState);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Callable<Object>> crowd = IntStream.range(0, 300).mapToObj(i -> calls.get(i % calls.size())).toList();
            for (Future<Object> call : executor.invokeAll(crowd)) {
                assertThatThrownBy(call::get).cause().isInstanceOf(ProviderUnavailableException.class)
                        .hasMessageContaining("suspended");
            }
        }

        assertThat(SERVICES.calls(PHOTON) + SERVICES.calls(OSRM) + SERVICES.calls(OPEN_METEO)).isZero();
    }

    @Test
    void refusedRequestsDoNotOpenTheCircuit() {
        // Days whose places have no road between them must not cut the routes of everybody else
        SERVICES.script(OSRM, Answer.status(400));

        for (int day = 1; day <= 25; day++) {
            assertUnavailable("osrm", () -> mapProvider.route(THREE_STOPS));
        }

        assertThat(SERVICES.calls(OSRM)).isEqualTo(25);
        assertThat(circuitBreakers.circuitBreaker("osrm").getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void openCircuitIsAnsweredWith503ThroughTheApi() {
        User user = userRepository.save(User.builder().email("owner@example.com")
                .passwordHash("$2a$12$placeholder-bcrypt-hash-not-real").fullName("Test").emailVerified(true).build());
        String bearer = "Bearer " + jwtTokenProvider.generateAccessToken(user).token();

        // The application as a user meets it: a search answered by the real source through every layer
        assertThat(mvc.get().uri("/api/v1/places/search?q={q}", "resilience while photon is up")
                .header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatusOk()
                .bodyJson().extractingPath("$.data[0].provider").isEqualTo("OSM");

        circuitBreakers.circuitBreaker("photon").transitionToOpenState();
        int callsSoFar = SERVICES.calls(PHOTON);

        assertThat(mvc.get().uri("/api/v1/places/search?q={q}", "resilience while photon is cut off")
                .header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatus(503)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("PROVIDER_UNAVAILABLE");
        assertThat(SERVICES.calls(PHOTON)).isEqualTo(callsSoFar);
    }

    // ---- rate limiter --------------------------------------------------------------------------------------------

    @Test
    void lookupsAreSpacedOneASecondAndAQueueThatIsTooLongFailsAtOnce() throws Exception {
        // One after the other: the second and the third wait for their turn
        Instant start = Instant.now();
        for (String id : List.of("W1", "W2", "W3")) {
            assertThat(mapProvider.lookup(id)).map(PlaceResult::provider).contains(PlaceProvider.OSM);
        }
        assertThat(Duration.between(start, Instant.now())).isGreaterThanOrEqualTo(Duration.ofSeconds(1));
        assertThat(SERVICES.calls(NOMINATIM)).isEqualTo(3);

        // Forty at the same moment: a turn comes every second and nobody waits longer than two
        List<Callable<Boolean>> lookups = IntStream.rangeClosed(1, 40)
                .<Callable<Boolean>>mapToObj(i -> () -> {
                    try {
                        return mapProvider.lookup("N" + i).isPresent();
                    }
                    catch (ProviderUnavailableException ex) {
                        assertThat(ex.getMessage()).contains("nominatim");
                        return false;
                    }
                })
                .toList();
        long served = 0;
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (Future<Boolean> lookup : executor.invokeAll(lookups)) {
                served += lookup.get() ? 1 : 0;
            }
        }

        assertThat(served).isBetween(1L, 4L);
        // The ones that were turned away never reached Nominatim
        assertThat(SERVICES.calls(NOMINATIM)).isEqualTo(3 + (int) served);
    }

    // ---- helpers -------------------------------------------------------------------------------------------------

    private static void assertUnavailable(String source, ThrowingCallable call) {
        assertThatThrownBy(call).isInstanceOf(ProviderUnavailableException.class).hasMessageContaining(source);
    }

    private static Coordinate point(String lat, String lng) {
        return new Coordinate(new BigDecimal(lat), new BigDecimal(lng));
    }

    private static String sample(String path) {
        try {
            return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

}

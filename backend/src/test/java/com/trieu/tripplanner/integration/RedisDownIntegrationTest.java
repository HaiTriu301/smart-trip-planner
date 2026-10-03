package com.trieu.tripplanner.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.trieu.tripplanner.TestcontainersConfiguration;
import com.trieu.tripplanner.model.User;
import com.trieu.tripplanner.repository.UserRepository;
import com.trieu.tripplanner.security.JwtTokenProvider;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.testcontainers.containers.GenericContainer;

/**
 * The application while Redis is switched off (design.md 8.1: Redis is not a source of data, so losing it must
 * not take the application down). The Redis container of the test is really stopped; MySQL keeps running.
 * <p>
 * The application context is thrown away after this class: its Redis is gone, and the tests that come next must
 * get a working one.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
class RedisDownIntegrationTest {

    /** Far below the 60 seconds the Redis client would wait by default, with room for a slow machine. */
    private static final Duration ACCEPTABLE_DELAY = Duration.ofSeconds(10);

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private GenericContainer<?> redisContainer;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String bearer;

    @BeforeEach
    void switchRedisOffAndSignIn() {
        // Stopping a stopped container does nothing, so every test starts from "Redis is off"
        redisContainer.stop();
        assertThat(redisContainer.isRunning()).isFalse();
        User user = userRepository.save(User.builder().email("owner@example.com")
                .passwordHash("$2a$12$placeholder-bcrypt-hash-not-real").fullName("Test").emailVerified(true).build());
        bearer = "Bearer " + jwtTokenProvider.generateAccessToken(user).token();
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void healthStaysUpWhileRedisIsOff() {
        // What a load balancer or a hosting platform asks before it sends traffic to the application
        assertThat(mvc.get().uri("/actuator/health"))
                .hasStatusOk()
                .bodyJson().extractingPath("$.status").isEqualTo("UP");
    }

    @Test
    void placeSearchStillAnswersWhileRedisIsOff(CapturedOutput output) {
        Instant start = Instant.now();

        // Two searches: the first one is also the first time the application talks to this dead Redis
        for (int attempt = 1; attempt <= 2; attempt++) {
            assertThat(mvc.get().uri("/api/v1/places/search?q={q}", "chợ hàn").header(HttpHeaders.AUTHORIZATION, bearer))
                    .hasStatusOk()
                    .bodyJson().extractingPath("$.data[0].name").isEqualTo("Chợ Hàn");
        }

        // Slower than with Redis, but the user is not left waiting for the Redis client to give up
        assertThat(Duration.between(start, Instant.now())).isLessThan(ACCEPTABLE_DELAY);
        // The failure is not swallowed in silence: the log says which cache failed and why (CLAUDE.md rule 12)
        assertThat(output.getOut()).contains("WARN").contains("Cache 'place:search' failed to get entry");
    }

}

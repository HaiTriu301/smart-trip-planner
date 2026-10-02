package com.trieu.tripplanner.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.trieu.tripplanner.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
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
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
class RedisDownIntegrationTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private GenericContainer<?> redisContainer;

    @BeforeEach
    void switchRedisOff() {
        // Stopping a stopped container does nothing, so every test starts from "Redis is off"
        redisContainer.stop();
        assertThat(redisContainer.isRunning()).isFalse();
    }

    @Test
    void healthStaysUpWhileRedisIsOff() {
        // What a load balancer or a hosting platform asks before it sends traffic to the application
        assertThat(mvc.get().uri("/actuator/health"))
                .hasStatusOk()
                .bodyJson().extractingPath("$.status").isEqualTo("UP");
    }

}

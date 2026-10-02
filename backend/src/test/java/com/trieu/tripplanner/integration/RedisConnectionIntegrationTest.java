package com.trieu.tripplanner.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.trieu.tripplanner.TestcontainersConfiguration;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;

/**
 * The application can talk to Redis (design.md 8.1). Nothing is cached yet: this only proves the connection the
 * caches of the next commits stand on, against a real Redis in Docker.
 * <p>
 * Same annotations as the other flow tests on purpose (MockMvc included, although unused here): Spring then
 * reuses their application context instead of starting a second application and a second pair of containers.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class RedisConnectionIntegrationTest {

    private static final String KEY = "test:redis-connection";
    private static final int REDIS_PORT = 6379;

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private LettuceConnectionFactory connectionFactory;

    @Autowired
    private GenericContainer<?> redisContainer;

    @AfterEach
    void removeTheKey() {
        redis.delete(KEY);
    }

    @Test
    void valueWrittenWithATimeLimitIsReadBackAndKeepsThatLimit() {
        redis.opsForValue().set(KEY, "Chợ Hàn", Duration.ofMinutes(5));

        assertThat(redis.opsForValue().get(KEY)).isEqualTo("Chợ Hàn");
        // Seconds left before Redis drops the key by itself
        assertThat(redis.getExpire(KEY, TimeUnit.SECONDS)).isBetween(1L, 300L);
    }

    @Test
    void valueIsGoneOnceItsTimeIsUp() {
        redis.opsForValue().set(KEY, "sắp hết hạn", Duration.ofMillis(200));

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(redis.hasKey(KEY)).isFalse());
        assertThat(redis.opsForValue().get(KEY)).isNull();
    }

    @Test
    void unknownKeyReadsAsNothing() {
        assertThat(redis.opsForValue().get("test:never-written")).isNull();
    }

    @Test
    void applicationIsConnectedToTheRedisOfTheTestNotToOneOnTheDevelopersMachine() {
        // The container publishes Redis on a random port; a test that reached localhost:6379 would be using
        // whatever docker compose left running, and would pass or fail depending on the machine
        assertThat(connectionFactory.getHostName()).isEqualTo(redisContainer.getHost());
        assertThat(connectionFactory.getPort()).isEqualTo(redisContainer.getMappedPort(REDIS_PORT));
    }

}

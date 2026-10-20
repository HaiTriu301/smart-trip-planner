package com.trieu.tripplanner.security.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.trieu.tripplanner.security.ratelimit.RateLimiter.Decision;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.ClientOptions.DisconnectedBehavior;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * The bucket store on a real Redis, without the application around it: what Bucket4j writes, when a token comes
 * back, and what happens when Redis goes away. Bucket4j 8.21 is built against Lettuce 6 while Spring Boot 4.1 ships
 * Lettuce 7, so these tests are also the proof that the two work together (WORKFLOW.md Task 4.7, decision 10).
 * <p>
 * Every test counts under its own random key, so the shared Redis never carries state from one test to another.
 */
@ExtendWith(OutputCaptureExtension.class)
class RateLimiterTest {

    private static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:7-alpine");
    private static final int REDIS_PORT = 6379;
    private static final Duration REDIS_TIMEOUT = Duration.ofSeconds(1);

    /** 3 calls an hour: one comes back every 20 minutes. */
    private static final RateLimit THREE_AN_HOUR = new RateLimit(3, Duration.ofHours(1));
    private static final long TWENTY_MINUTES = Duration.ofMinutes(20).toSeconds();

    private static GenericContainer<?> redis;
    private static RedisClient client;
    private static StatefulRedisConnection<String, byte[]> connection;
    private static StatefulRedisConnection<String, String> inspector;

    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-20T08:00:00Z"));
    private final String key = "test:" + UUID.randomUUID();

    @BeforeAll
    static void startRedis() {
        redis = new GenericContainer<>(REDIS_IMAGE).withExposedPorts(REDIS_PORT);
        redis.start();
        client = clientFor(redis);
        connection = client.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
        inspector = client.connect();
    }

    @AfterAll
    static void stopRedis() {
        inspector.close();
        connection.close();
        client.shutdown();
        redis.stop();
    }

    @Test
    void allowsUpToTheCapacityThenRejectsWithTheWaitForTheNextCall() {
        RateLimiter limiter = new RateLimiter(connection, clock, REDIS_TIMEOUT);

        for (int call = 1; call <= 3; call++) {
            assertThat(limiter.tryConsume(key, THREE_AN_HOUR)).as("call %d", call).isEqualTo(new Decision(true, 0));
        }

        assertThat(limiter.tryConsume(key, THREE_AN_HOUR)).isEqualTo(new Decision(false, TWENTY_MINUTES));
    }

    @Test
    void givesOneCallBackAfterTheAnnouncedWait() {
        RateLimiter limiter = new RateLimiter(connection, clock, REDIS_TIMEOUT);
        for (int call = 1; call <= 3; call++) {
            limiter.tryConsume(key, THREE_AN_HOUR);
        }

        clock.advance(Duration.ofSeconds(TWENTY_MINUTES - 1));
        assertThat(limiter.tryConsume(key, THREE_AN_HOUR)).as("one second early").isEqualTo(new Decision(false, 1));

        clock.advance(Duration.ofSeconds(1));
        assertThat(limiter.tryConsume(key, THREE_AN_HOUR).allowed()).as("on time").isTrue();
        assertThat(limiter.tryConsume(key, THREE_AN_HOUR)).as("only one came back")
                .isEqualTo(new Decision(false, TWENTY_MINUTES));
    }

    @Test
    void keepsASeparateBucketPerKey() {
        RateLimiter limiter = new RateLimiter(connection, clock, REDIS_TIMEOUT);
        RateLimit once = new RateLimit(1, Duration.ofHours(1));

        assertThat(limiter.tryConsume(key + ":a", once).allowed()).isTrue();
        assertThat(limiter.tryConsume(key + ":a", once).allowed()).isFalse();

        assertThat(limiter.tryConsume(key + ":b", once).allowed()).isTrue();
    }

    @Test
    void storesTheBucketUnderTheRatePrefixWithAnExpiry() {
        RateLimiter limiter = new RateLimiter(connection, clock, REDIS_TIMEOUT);

        limiter.tryConsume(key, THREE_AN_HOUR);

        String redisKey = RateLimiter.KEY_PREFIX + key;
        assertThat(inspector.sync().exists(redisKey)).isEqualTo(1L);
        assertThat(inspector.sync().exists(key)).as("never without the prefix").isZero();
        // One token is missing: full again after 20 minutes, then kept 10 seconds more
        assertThat(inspector.sync().pttl(redisKey))
                .isPositive()
                .isLessThanOrEqualTo(Duration.ofMinutes(20).plusSeconds(10).toMillis());
    }

    @Test
    void letsCallsThroughWhenRedisGoesDown(CapturedOutput output) {
        // A Redis of its own: stopping the shared one would break the other tests
        GenericContainer<?> doomed = new GenericContainer<>(REDIS_IMAGE).withExposedPorts(REDIS_PORT);
        doomed.start();
        RedisClient doomedClient = clientFor(doomed);
        try (StatefulRedisConnection<String, byte[]> doomedConnection =
                     doomedClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE))) {
            RateLimiter limiter = new RateLimiter(doomedConnection, clock, REDIS_TIMEOUT);
            RateLimit once = new RateLimit(1, Duration.ofHours(1));
            // Spend the only call while Redis is up, so a working limiter would now say no
            assertThat(limiter.tryConsume(key, once).allowed()).isTrue();

            doomed.stop();
            long started = System.nanoTime();
            Decision decision = limiter.tryConsume(key, once);
            Duration took = Duration.ofNanos(System.nanoTime() - started);

            assertThat(decision).isEqualTo(new Decision(true, 0));
            assertThat(took).as("waits at most about one Redis timeout").isLessThan(Duration.ofSeconds(5));
            assertThat(output).contains("Rate limit not checked for " + key + ", letting the call through");
        }
        finally {
            doomedClient.shutdown();
        }
    }

    @Test
    void rejectsALimitWithoutCallsOrWithoutAPeriod() {
        assertThatThrownBy(() -> new RateLimit(0, Duration.ofHours(1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RateLimit(5, Duration.ZERO)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RateLimit(5, null)).isInstanceOf(IllegalArgumentException.class);
    }

    /** The same client settings the application uses for Redis: 1 second timeout, refuse while disconnected. */
    private static RedisClient clientFor(GenericContainer<?> container) {
        RedisClient redisClient = RedisClient.create(RedisURI.builder()
                .withHost(container.getHost())
                .withPort(container.getMappedPort(REDIS_PORT))
                .withTimeout(REDIS_TIMEOUT)
                .build());
        redisClient.setOptions(ClientOptions.builder()
                .disconnectedBehavior(DisconnectedBehavior.REJECT_COMMANDS)
                .build());
        return redisClient;
    }

    /** A clock the test moves by hand, so "20 minutes later" takes no time. */
    private static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            throw new UnsupportedOperationException("not needed by the rate limiter");
        }

    }

}

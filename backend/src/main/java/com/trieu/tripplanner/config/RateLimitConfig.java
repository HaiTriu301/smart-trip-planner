package com.trieu.tripplanner.config;

import com.trieu.tripplanner.security.ratelimit.RateLimiter;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.ClientOptions.DisconnectedBehavior;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.data.redis.autoconfigure.DataRedisConnectionDetails;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The Redis side of the rate limits (design.md 8.2), only when {@code app.rate-limit.enabled} is true.
 * SecurityConfig puts RateLimitFilter in the chain when the {@link RateLimiter} bean exists, so with the limits
 * off nothing here is created and no extra Redis connection is opened.
 * <p>
 * Bucket4j talks to Lettuce directly, not through Spring Data Redis, so it gets a client of its own. It points at
 * the same Redis (the connection details also come from Testcontainers in tests) and behaves like the cache
 * client of RedisClientConfig: the same timeout, and commands refused at once while Redis is gone, so a lost Redis
 * costs no waiting and the call goes through (fail open).
 * <p>
 * The connection is opened at startup: with the limits on, the application needs Redis to start. Once running it
 * survives Redis going away and reconnects in the background.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBooleanProperty("app.rate-limit.enabled")
public class RateLimitConfig {

    /** Used when spring.data.redis does not set a timeout: the same 1 second as the caches (design.md 8.1). */
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(1);

    @Bean
    RedisClient rateLimitRedisClient(DataRedisConnectionDetails connectionDetails, DataRedisProperties redis) {
        // One Redis in Docker next to the application (design.md 17): standalone, no TLS
        DataRedisConnectionDetails.Standalone standalone = connectionDetails.getStandalone();
        RedisURI.Builder uri = RedisURI.builder()
                .withHost(standalone.getHost())
                .withPort(standalone.getPort())
                .withDatabase(standalone.getDatabase())
                .withTimeout(timeoutOf(redis));
        String password = connectionDetails.getPassword();
        if (password != null && connectionDetails.getUsername() != null) {
            uri.withAuthentication(connectionDetails.getUsername(), password);
        }
        else if (password != null) {
            uri.withPassword(password);
        }

        RedisClient client = RedisClient.create(uri.build());
        client.setOptions(ClientOptions.builder()
                .disconnectedBehavior(DisconnectedBehavior.REJECT_COMMANDS)
                .socketOptions(SocketOptions.builder()
                        .connectTimeout(Objects.requireNonNullElse(redis.getConnectTimeout(), DEFAULT_TIMEOUT))
                        .build())
                .build());
        return client;
    }

    /** Bucket keys are text, bucket states are the bytes Bucket4j writes. */
    @Bean
    StatefulRedisConnection<String, byte[]> rateLimitRedisConnection(RedisClient rateLimitRedisClient) {
        return rateLimitRedisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
    }

    @Bean
    RateLimiter rateLimiter(StatefulRedisConnection<String, byte[]> rateLimitRedisConnection, Clock clock,
                            DataRedisProperties redis) {
        return new RateLimiter(rateLimitRedisConnection, clock, timeoutOf(redis));
    }

    private static Duration timeoutOf(DataRedisProperties redis) {
        return Objects.requireNonNullElse(redis.getTimeout(), DEFAULT_TIMEOUT);
    }

}

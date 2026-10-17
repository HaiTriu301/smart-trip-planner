package com.trieu.tripplanner.config;

import io.lettuce.core.ClientOptions.DisconnectedBehavior;
import org.springframework.boot.data.redis.autoconfigure.LettuceClientOptionsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * How the Redis client behaves while it has lost Redis (design.md 8.1: Redis is a cache, never a reason to make a
 * request wait).
 * <p>
 * By default Lettuce keeps every command of a lost connection in a queue until it has connected again, so each
 * read and each write of a cache waited the full {@code spring.data.redis.timeout} (1 s) before the cache step was
 * skipped: with Redis gone, every cached request was seconds slower (BUG-PLAT-005). Rejected at once instead, the
 * command fails straight to CacheConfig's error handler and the request goes on without the cache. Lettuce still
 * reconnects in the background, and the cache is used again as soon as Redis is back.
 * <p>
 * A Redis that is connected but does not answer still costs the timeout: nothing on this side can tell it apart
 * from a slow answer.
 */
@Configuration(proxyBeanMethods = false)
public class RedisClientConfig {

    @Bean
    LettuceClientOptionsBuilderCustomizer rejectCommandsWhileDisconnected() {
        return options -> options.disconnectedBehavior(DisconnectedBehavior.REJECT_COMMANDS);
    }

}

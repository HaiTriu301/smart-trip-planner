package com.trieu.tripplanner.security.ratelimit;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.lettuce.core.api.StatefulRedisConnection;
import java.time.Clock;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;

/**
 * Counts calls in Redis, one bucket of tokens per key (design.md 8.2). Each call takes a token; a bucket without
 * tokens means the caller went over its {@link RateLimit}. Bucket4j keeps the bucket under {@value #KEY_PREFIX}
 * plus the key and updates it in one compare-and-swap, so two requests at the same moment never both get the last
 * token.
 * <p>
 * Redis only holds counters here, never data: when Redis cannot answer, the call is let through and a warning is
 * logged (fail open, same as the caches in design.md 8.1). A down Redis must not lock every user out.
 */
@Slf4j
public class RateLimiter {

    /** Prefix of every bucket in Redis; no cache name in CacheNames starts with it. */
    static final String KEY_PREFIX = "rate:";

    /**
     * How long a full bucket stays in Redis after its last change. A bucket that has refilled to the top is the
     * same as no bucket, so it is dropped shortly after: idle callers do not pile up in Redis.
     */
    private static final Duration KEEP_FULL_BUCKET = Duration.ofSeconds(10);

    private static final long NANOS_PER_SECOND = Duration.ofSeconds(1).toNanos();

    private final ProxyManager<String> buckets;

    /**
     * @param connection     a Redis connection with String keys and byte[] values, owned by the caller
     * @param clock          the application clock; the time a token comes back is measured on it
     * @param requestTimeout how long a check waits for Redis before letting the call through
     */
    public RateLimiter(StatefulRedisConnection<String, byte[]> connection, Clock clock, Duration requestTimeout) {
        this.buckets = Bucket4jLettuce.casBasedBuilder(connection)
                .clientClock(timeMeterOf(clock))
                .requestTimeout(requestTimeout)
                .expirationAfterWrite(ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(KEEP_FULL_BUCKET))
                .build();
    }

    /**
     * Takes one token from the bucket of {@code key}, creating a full bucket on first use.
     *
     * @param key   who is counted, without the prefix, for example {@code register:203.0.113.7}
     * @param limit the size of the bucket and how fast it refills
     * @return whether the call may go on and, if not, how long until a token is back
     */
    public Decision tryConsume(String key, RateLimit limit) {
        try {
            ConsumptionProbe probe = buckets.builder()
                    .build(KEY_PREFIX + key, () -> configurationOf(limit))
                    .tryConsumeAndReturnRemaining(1);
            return probe.isConsumed()
                    ? Decision.ALLOWED
                    : Decision.rejected(secondsRoundedUp(probe.getNanosToWaitForRefill()));
        }
        catch (RuntimeException ex) {
            // Any failure of Redis or of the client lands here: refused while disconnected, timed out, reset
            log.warn("Rate limit not checked for {}, letting the call through: {}: {}",
                    key, ex.getClass().getSimpleName(), ex.getMessage());
            return Decision.ALLOWED;
        }
    }

    private static BucketConfiguration configurationOf(RateLimit limit) {
        return BucketConfiguration.builder()
                .addLimit(bandwidth -> bandwidth.capacity(limit.capacity()).refillGreedy(limit.capacity(), limit.period()))
                .build();
    }

    /** At least one second: a rejected call is never told to come back "in 0 seconds". */
    private static long secondsRoundedUp(long nanos) {
        return Math.max(1, (nanos + NANOS_PER_SECOND - 1) / NANOS_PER_SECOND);
    }

    /** Bucket4j measures time with its own interface; this one reads the application clock. */
    private static TimeMeter timeMeterOf(Clock clock) {
        return new TimeMeter() {
            @Override
            public long currentTimeNanos() {
                return clock.millis() * 1_000_000L;
            }

            @Override
            public boolean isWallClockBased() {
                return true;
            }
        };
    }

    /**
     * The answer for one call.
     *
     * @param allowed           whether the call may go on
     * @param retryAfterSeconds when not allowed, seconds until the bucket has a token again; 0 when allowed
     */
    public record Decision(boolean allowed, long retryAfterSeconds) {

        static final Decision ALLOWED = new Decision(true, 0);

        static Decision rejected(long retryAfterSeconds) {
            return new Decision(false, retryAfterSeconds);
        }

    }

}

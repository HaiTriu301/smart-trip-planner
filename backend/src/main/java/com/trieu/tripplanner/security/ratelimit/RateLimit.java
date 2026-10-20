package com.trieu.tripplanner.security.ratelimit;

import java.time.Duration;

/**
 * How many calls a caller may make in a period (design.md 8.2), for example 5 calls in 15 minutes.
 * <p>
 * The calls come back one at a time, evenly spread over the period: with 5 in 15 minutes a caller who used them
 * all gets one back every 3 minutes, never more than 5 at once.
 *
 * @param capacity how many calls fit in the period, at least 1
 * @param period   how long it takes for all of them to come back
 */
public record RateLimit(long capacity, Duration period) {

    public RateLimit {
        if (capacity < 1) {
            throw new IllegalArgumentException("A rate limit allows at least one call, got " + capacity);
        }
        if (period == null || period.isZero() || period.isNegative()) {
            throw new IllegalArgumentException("A rate limit needs a positive period, got " + period);
        }
    }

}

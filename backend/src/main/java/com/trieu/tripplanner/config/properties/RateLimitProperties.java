package com.trieu.tripplanner.config.properties;

import com.trieu.tripplanner.security.ratelimit.RateLimit;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * How often one caller may call the endpoints that have a limit of their own (design.md 8.2), bound from
 * {@code app.rate-limit.*}. The numbers live in application.yml; which endpoint a rule covers and who it counts
 * (address, email, user) is fixed in RateLimitFilter.
 * <p>
 * A {@link RateLimit} checks its own numbers, so a limit of 0 calls or without a period stops the application at
 * startup instead of locking everyone out.
 *
 * @param enabled  whether the limits apply at all; off in the test profile, where older tests call the same
 *                 endpoints hundreds of times from one address. RateLimitConfig reads the same property
 * @param register {@code POST /auth/register}, counted per address
 */
@Validated
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        boolean enabled,
        @NotNull RateLimit register) {
}

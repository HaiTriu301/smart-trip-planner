package com.trieu.tripplanner.config.properties;

import com.trieu.tripplanner.security.ratelimit.RateLimit;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * How often one caller may call the API (design.md 8.2), bound from {@code app.rate-limit.*}: a limit for some
 * endpoints of their own, and a global one for every call. The numbers live in application.yml; which endpoint a
 * rule covers and who it counts (address, email, user) is fixed in RateLimitFilter.
 * <p>
 * A {@link RateLimit} checks its own numbers, so a limit of 0 calls or without a period stops the application at
 * startup instead of locking everyone out.
 *
 * @param enabled            whether the limits apply at all; off in the test profile, where older tests call the
 *                           same endpoints hundreds of times from one address. RateLimitConfig reads the same
 *                           property
 * @param login              {@code POST /auth/login}, counted per address and email
 * @param register           {@code POST /auth/register}, counted per address
 * @param resendVerification {@code POST /auth/resend-verification}, counted per address
 * @param forgotPassword     {@code POST /auth/forgot-password}, counted per address
 * @param publicTrip         {@code GET /public/trips/{token}}, the page of a share link, counted per address
 * @param placeSearch        {@code GET /places/search}, counted per signed-in user
 * @param globalUser         every call under {@code /api/} of a signed-in user, wherever it comes from
 * @param globalGuest        every call under {@code /api/} without a sign-in, counted per address
 */
@Validated
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        boolean enabled,
        @NotNull RateLimit login,
        @NotNull RateLimit register,
        @NotNull RateLimit resendVerification,
        @NotNull RateLimit forgotPassword,
        @NotNull RateLimit publicTrip,
        @NotNull RateLimit placeSearch,
        @NotNull RateLimit globalUser,
        @NotNull RateLimit globalGuest) {
}

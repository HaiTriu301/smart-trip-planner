package com.trieu.tripplanner.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Token settings bound from {@code app.jwt.*} (design.md 6.1).
 * The secret comes from the JWT_SECRET environment variable; startup fails if it is shorter than 64 characters.
 *
 * @param secret       HMAC key material for HS256 (>= 64 chars so the key is at least 512 bits)
 * @param accessTtl    lifetime of the access token (default 15 minutes)
 * @param refreshTtl   lifetime of the refresh token and its cookie (default 7 days)
 * @param issuer       {@code iss} claim, also required when parsing
 * @param cookieSecure add the Secure flag to the refresh cookie (true in prod, HTTPS only)
 */
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        @NotBlank @Size(min = 64) String secret,
        @DefaultValue("15m") @NotNull Duration accessTtl,
        @DefaultValue("7d") @NotNull Duration refreshTtl,
        @DefaultValue("smart-trip-planner") @NotBlank String issuer,
        @DefaultValue("false") boolean cookieSecure) {
}

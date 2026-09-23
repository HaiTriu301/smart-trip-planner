package com.trieu.tripplanner.security;

import com.trieu.tripplanner.config.properties.JwtProperties;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;

/**
 * Builds the {@code refresh_token} cookie exactly as design.md 6.1 specifies:
 * HttpOnly (JS cannot read it), SameSite=Lax (other sites cannot POST it), Path limited to the auth endpoints
 * (never sent with ordinary API calls), Secure in prod.
 */
@RequiredArgsConstructor
public class RefreshTokenCookies {

    public static final String NAME = "refresh_token";
    public static final String PATH = "/api/v1/auth";
    private static final String SAME_SITE = "Lax";

    private final JwtProperties properties;

    public ResponseCookie create(String rawToken) {
        return base(rawToken).maxAge(properties.refreshTtl()).build();
    }

    /** Max-Age=0 tells the browser to drop the cookie immediately (logout, or after a theft was detected). */
    public ResponseCookie clear() {
        return base("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .sameSite(SAME_SITE)
                .path(PATH);
    }

}

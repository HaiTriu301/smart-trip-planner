package com.trieu.tripplanner.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.trieu.tripplanner.config.properties.JwtProperties;
import com.trieu.tripplanner.model.User;
import com.trieu.tripplanner.model.enums.Plan;
import com.trieu.tripplanner.model.enums.Role;
import com.trieu.tripplanner.support.TestUsers;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * Pure unit test: no Spring. Proves the token we mint is the token we accept, and nothing else is.
 */
class JwtTokenProviderTest {

    private static final String SECRET = "unit-test-secret-0123456789abcdef0123456789abcdef0123456789abcdef";
    private static final String OTHER_SECRET = "other-secret-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";

    private final JwtJsonCodec codec = new JwtJsonCodec(JsonMapper.builder().build());
    private final JwtTokenProvider provider = new JwtTokenProvider(properties(SECRET, "smart-trip-planner"), codec);
    private final User admin = TestUsers.admin(7L, "admin@example.com");

    @Test
    void roundTripKeepsEveryClaim() {
        Instant before = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        JwtTokenProvider.AccessToken issued = provider.generateAccessToken(admin);
        JwtTokenProvider.JwtClaims claims = provider.parse(issued.token());

        assertThat(claims.userId()).isEqualTo(7L);
        assertThat(claims.email()).isEqualTo("admin@example.com");
        assertThat(claims.role()).isEqualTo(Role.ADMIN);
        assertThat(claims.plan()).isEqualTo(Plan.PREMIUM);
        assertThat(claims.jti()).isNotBlank();
        // JWT stores seconds, so compare at second precision
        assertThat(claims.expiresAt()).isEqualTo(issued.expiresAt().truncatedTo(ChronoUnit.SECONDS));
        assertThat(issued.expiresAt()).isBetween(before.plus(Duration.ofMinutes(15)), Instant.now().plus(Duration.ofMinutes(15)));
    }

    @Test
    void tokenIsSignedWithHs256AndHasThreeSegments() {
        String token = provider.generateAccessToken(admin).token();

        String[] segments = token.split("\\.");
        assertThat(segments).hasSize(3);
        String header = new String(Base64.getUrlDecoder().decode(segments[0]), StandardCharsets.UTF_8);
        assertThat(header).contains("\"alg\":\"HS256\"");
    }

    @Test
    void everyTokenGetsAUniqueId() {
        String jti1 = provider.parse(provider.generateAccessToken(admin).token()).jti();
        String jti2 = provider.parse(provider.generateAccessToken(admin).token()).jti();

        assertThat(jti1).isNotEqualTo(jti2);
    }

    @Test
    void expiredTokenIsRejectedWithExpiredJwtException() {
        String expired = provider.generateAccessToken(admin, Instant.now().minus(Duration.ofMinutes(20))).token();

        assertThatThrownBy(() -> provider.parse(expired)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        JwtTokenProvider other = new JwtTokenProvider(properties(OTHER_SECRET, "smart-trip-planner"), codec);
        String foreign = other.generateAccessToken(admin).token();

        assertThatThrownBy(() -> provider.parse(foreign))
                .isInstanceOf(JwtException.class)
                .isNotInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void tokenFromAnotherIssuerIsRejected() {
        JwtTokenProvider other = new JwtTokenProvider(properties(SECRET, "someone-else"), codec);
        String foreign = other.generateAccessToken(admin).token();

        assertThatThrownBy(() -> provider.parse(foreign)).isInstanceOf(JwtException.class);
    }

    @Test
    void tamperedPayloadIsRejected() {
        String token = provider.generateAccessToken(admin).token();
        String[] parts = token.split("\\.");
        // Flip a character in the payload: the signature no longer matches
        String tamperedPayload = parts[1].charAt(0) == 'a' ? 'b' + parts[1].substring(1) : 'a' + parts[1].substring(1);
        String tampered = parts[0] + "." + tamperedPayload + "." + parts[2];

        assertThatThrownBy(() -> provider.parse(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    void garbageIsRejectedAsMalformed() {
        assertThatThrownBy(() -> provider.parse("khong-phai-jwt")).isInstanceOf(MalformedJwtException.class);
    }

    private static JwtProperties properties(String secret, String issuer) {
        return new JwtProperties(secret, Duration.ofMinutes(15), Duration.ofDays(7), issuer, false);
    }

}

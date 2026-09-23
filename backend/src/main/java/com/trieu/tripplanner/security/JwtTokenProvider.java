package com.trieu.tripplanner.security;

import com.trieu.tripplanner.config.properties.JwtProperties;
import com.trieu.tripplanner.model.User;
import com.trieu.tripplanner.model.enums.Plan;
import com.trieu.tripplanner.model.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;

/**
 * Creates and verifies access tokens (design.md 6.1): JWT signed with HS256, 15-minute TTL,
 * claims {@code sub} (user id), {@code email}, {@code role}, {@code plan}, {@code jti}, {@code iss}, {@code iat}, {@code exp}.
 * Nothing is stored server-side; the signature alone proves the token was issued by us.
 */
public class JwtTokenProvider {

    static final String CLAIM_EMAIL = "email";
    static final String CLAIM_ROLE = "role";
    static final String CLAIM_PLAN = "plan";

    private final JwtProperties properties;
    private final JwtJsonCodec codec;
    private final SecretKey key;

    public JwtTokenProvider(JwtProperties properties, JwtJsonCodec codec) {
        this.properties = properties;
        this.codec = codec;
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public AccessToken generateAccessToken(User user) {
        return generateAccessToken(user, Instant.now());
    }

    /**
     * Same as {@link #generateAccessToken(User)} with an explicit issue time; tests use it to mint expired tokens.
     */
    public AccessToken generateAccessToken(User user, Instant issuedAt) {
        Instant expiresAt = issuedAt.plus(properties.accessTtl());
        String token = Jwts.builder()
                .json(codec)
                .issuer(properties.issuer())
                .subject(String.valueOf(user.getId()))
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .claim(CLAIM_EMAIL, user.getEmail())
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim(CLAIM_PLAN, user.getPlan().name())
                .signWith(key, Jwts.SIG.HS256)
                .compact();
        return new AccessToken(token, issuedAt, expiresAt);
    }

    /**
     * Verifies signature, issuer and expiry, then extracts the claims.
     *
     * @throws ExpiredJwtException when the token is genuine but past {@code exp}
     * @throws JwtException        for every other problem (bad signature, malformed, wrong issuer, ...)
     */
    public JwtClaims parse(String token) {
        Claims claims = Jwts.parser()
                .json(codec)
                .verifyWith(key)
                .requireIssuer(properties.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return new JwtClaims(
                Long.parseLong(claims.getSubject()),
                claims.get(CLAIM_EMAIL, String.class),
                Role.valueOf(claims.get(CLAIM_ROLE, String.class)),
                Plan.valueOf(claims.get(CLAIM_PLAN, String.class)),
                claims.getId(),
                claims.getExpiration().toInstant());
    }

    /** A freshly signed access token with its validity window. */
    public record AccessToken(String token, Instant issuedAt, Instant expiresAt) {

        /** Whole TTL in seconds (design.md 6.1 {@code expiresIn}); independent of how long issuing took. */
        public long expiresInSeconds() {
            return java.time.Duration.between(issuedAt, expiresAt).toSeconds();
        }

    }

    /** The verified content of an access token. */
    public record JwtClaims(Long userId, String email, Role role, Plan plan, String jti, Instant expiresAt) {
    }

}

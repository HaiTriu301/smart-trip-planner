package com.trieu.tripplanner.service;

import com.trieu.tripplanner.common.util.SecureTokens;
import com.trieu.tripplanner.config.properties.JwtProperties;
import com.trieu.tripplanner.dto.internal.ClientInfo;
import com.trieu.tripplanner.model.RefreshToken;
import com.trieu.tripplanner.model.User;
import com.trieu.tripplanner.repository.RefreshTokenRepository;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns the refresh token lifecycle (design.md 6.1): random 64-char token to the client, SHA-256 hex in the DB.
 * Simple enough to be a class without interface (CLAUDE.md rule 5).
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final int USER_AGENT_MAX = 255;
    private static final int IP_MAX = 45;

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    /**
     * Creates a new session for the user. Returns the raw token exactly once; it cannot be recovered later.
     */
    @Transactional
    public IssuedRefreshToken issue(User user, ClientInfo client) {
        String rawToken = SecureTokens.generate();
        Instant expiresAt = Instant.now().plus(jwtProperties.refreshTtl());

        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(hash(rawToken))
                .expiresAt(expiresAt)
                .userAgent(truncate(client.userAgent(), USER_AGENT_MAX))
                .ipAddress(truncate(client.ipAddress(), IP_MAX))
                .build());

        return new IssuedRefreshToken(rawToken, expiresAt);
    }

    @Transactional(readOnly = true)
    public Optional<RefreshToken> findByRawToken(String rawToken) {
        return refreshTokenRepository.findByTokenHash(hash(rawToken));
    }

    /** Marks one session dead. The entity is managed, so the UPDATE runs when the caller's transaction commits. */
    public void revoke(RefreshToken token) {
        token.revoke(Instant.now());
    }

    /** Theft response: every live session of the user is revoked at once. */
    @Transactional
    public int revokeAll(Long userId) {
        return refreshTokenRepository.revokeAllActiveByUserId(userId, Instant.now());
    }

    /** SHA-256 hex of the raw token; kept here so existing callers and tests keep working. */
    public static String hash(String rawToken) {
        return SecureTokens.sha256Hex(rawToken);
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    /** The raw token to put in the cookie and when it stops being valid. */
    public record IssuedRefreshToken(String rawToken, Instant expiresAt) {
    }

}

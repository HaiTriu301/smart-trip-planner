package com.trieu.tripplanner.service;

import com.trieu.tripplanner.common.util.SecureTokens;
import com.trieu.tripplanner.exception.InvalidTokenException;
import com.trieu.tripplanner.model.User;
import com.trieu.tripplanner.model.VerificationToken;
import com.trieu.tripplanner.model.enums.VerificationTokenType;
import com.trieu.tripplanner.repository.VerificationTokenRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues and consumes the one-time tokens that travel in email links (design.md 5.2, 14.16).
 * Same storage rule as refresh tokens: the raw value leaves the server exactly once, the DB keeps its SHA-256.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationTokenService {

    private final VerificationTokenRepository verificationTokenRepository;

    /**
     * Creates a fresh token for the user and kills any older live token of the same type,
     * so a re-sent email makes the previous link stop working.
     *
     * @return the raw token to embed in the link
     */
    @Transactional
    public String issue(User user, VerificationTokenType type) {
        Instant now = Instant.now();
        int invalidated = verificationTokenRepository.invalidateActive(user.getId(), type, now);
        if (invalidated > 0) {
            log.debug("Invalidated {} older {} token(s) for user id={}", invalidated, type, user.getId());
        }

        String rawToken = SecureTokens.generate();
        verificationTokenRepository.save(VerificationToken.builder()
                .user(user)
                .tokenHash(SecureTokens.sha256Hex(rawToken))
                .type(type)
                .expiresAt(now.plus(type.ttl()))
                .build());
        return rawToken;
    }

    /**
     * Validates the raw token from a link and marks it used in one step. Every failure maps to the same
     * INVALID_TOKEN so a caller cannot tell "never existed" from "expired" (design.md 14.16).
     *
     * @throws InvalidTokenException unknown, wrong type, already used, or expired
     */
    @Transactional
    public VerificationToken consume(String rawToken, VerificationTokenType expectedType) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidTokenException("blank token");
        }
        VerificationToken token = verificationTokenRepository.findByTokenHash(SecureTokens.sha256Hex(rawToken))
                .orElseThrow(() -> new InvalidTokenException("unknown token"));

        if (token.getType() != expectedType) {
            throw new InvalidTokenException("type " + token.getType() + " used as " + expectedType);
        }
        if (token.isUsed()) {
            throw new InvalidTokenException("token id=" + token.getId() + " already used");
        }
        Instant now = Instant.now();
        if (token.isExpired(now)) {
            throw new InvalidTokenException("token id=" + token.getId() + " expired");
        }

        token.markUsed(now);
        return token;
    }

}

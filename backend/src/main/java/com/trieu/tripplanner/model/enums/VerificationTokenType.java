package com.trieu.tripplanner.model.enums;

import java.time.Duration;

/**
 * What a one-time token in {@code verification_tokens} is for, with its lifetime (design.md 5.2).
 * Stored as MySQL ENUM: adding a constant requires an ALTER TABLE migration.
 */
public enum VerificationTokenType {

    EMAIL_VERIFY(Duration.ofHours(24)),
    PASSWORD_RESET(Duration.ofHours(1));

    private final Duration ttl;

    VerificationTokenType(Duration ttl) {
        this.ttl = ttl;
    }

    public Duration ttl() {
        return ttl;
    }

}

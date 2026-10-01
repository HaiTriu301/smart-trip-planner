package com.trieu.tripplanner.model;

import com.trieu.tripplanner.model.enums.RevokedReason;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * One login session (design.md 5.2 "refresh_tokens"). Only the SHA-256 of the token is stored; the raw value
 * lives in the client's httpOnly cookie. A row is "alive" while {@code revoked_at} is null and {@code expires_at}
 * is in the future. No setters: the only state change is {@link #revoke(Instant, RevokedReason)}.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RefreshToken extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // CHAR(64) in MySQL: tell Hibernate so ddl-auto=validate does not expect VARCHAR
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    /** Null while alive, and on rows revoked before V8 (those count as rotated). */
    @Enumerated(EnumType.STRING)
    @Column(name = "revoked_reason")
    private RevokedReason revokedReason;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public boolean isActive(Instant now) {
        return !isRevoked() && !isExpired(now);
    }

    /**
     * True when the token was exchanged for a newer one. Presenting it again is the theft signal of design.md 6.1;
     * a token revoked for any other reason is only a stale cookie.
     */
    public boolean wasRotated() {
        return isRevoked() && (revokedReason == null || revokedReason == RevokedReason.ROTATED);
    }

    /** Idempotent: keeps the first revocation time and reason. */
    public void revoke(Instant now, RevokedReason reason) {
        if (revokedAt == null) {
            revokedAt = now;
            revokedReason = reason;
        }
    }

}

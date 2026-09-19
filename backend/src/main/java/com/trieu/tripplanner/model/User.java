package com.trieu.tripplanner.model;

import com.trieu.tripplanner.model.enums.Plan;
import com.trieu.tripplanner.model.enums.Role;
import com.trieu.tripplanner.model.enums.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * An account (design.md 5.2 "users"). Column types must match V2__create_users_table.sql exactly:
 * ddl-auto=validate refuses to start otherwise.
 * <p>
 * Soft delete (CLAUDE.md rule 10): {@code repository.delete()} only stamps {@code deleted_at}, and every query
 * on this entity silently ignores rows where it is set.
 */
@Entity
@Table(name = "users")
@SQLDelete(sql = "UPDATE users SET deleted_at = NOW(6) WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class User extends BaseEntity {

    public static final String DEFAULT_TIMEZONE = "Asia/Ho_Chi_Minh";
    public static final String DEFAULT_LOCALE = "vi";

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    /** BCrypt (strength 12) output, never the raw password. Named *Hash so nobody mistakes it for plaintext. */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    @Builder.Default
    @Column(name = "timezone", nullable = false, length = 64)
    private String timezone = DEFAULT_TIMEZONE;

    @Builder.Default
    @Column(name = "locale", nullable = false, length = 10)
    private String locale = DEFAULT_LOCALE;

    // Hibernate 7 maps @Enumerated(STRING) to the native MySQL ENUM type, matching the migration
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role = Role.USER;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "plan", nullable = false)
    private Plan plan = Plan.FREE;

    @Column(name = "plan_expires_at")
    private Instant planExpiresAt;

    @Builder.Default
    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    /** Set by the @SQLDelete statement only; null means the account is live. */
    @Setter(AccessLevel.NONE)
    @Column(name = "deleted_at")
    private Instant deletedAt;

    /**
     * Email is the login identifier and has a UNIQUE index, so it is stored lowercase (design.md 5.2)
     * regardless of how the caller typed it. Kept in the entity so no code path can bypass it.
     */
    @PrePersist
    @PreUpdate
    void normalizeEmail() {
        if (email != null) {
            email = email.trim().toLowerCase(Locale.ROOT);
        }
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isPremium() {
        return plan == Plan.PREMIUM && (planExpiresAt == null || planExpiresAt.isAfter(Instant.now()));
    }

}

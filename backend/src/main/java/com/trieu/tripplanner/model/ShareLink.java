package com.trieu.tripplanner.model;

import com.trieu.tripplanner.model.enums.SharePermission;
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
 * A link that lets anybody holding its token view a trip without an account (design.md 5.2 "share_links",
 * rule 14.26). Column types must match V12__create_share_links.sql exactly: ddl-auto=validate refuses to start
 * otherwise.
 * <p>
 * The token is stored raw: the link is made to be handed around and the owner lists and copies it again. It is
 * still a secret of the trip, so it never goes to the log (CLAUDE.md rule 17).
 * <p>
 * Everything that makes the link what it is ({@code trip}, {@code token}, {@code permission}, {@code expiresAt},
 * {@code createdBy}) is fixed when it is created and not updatable. {@code viewCount} is not updatable either:
 * it is only ever raised by a single UPDATE statement, so saving the entity for another reason (revoking the
 * link) can never write back an older count. The row is never deleted by the application and has no
 * {@code deleted_at}: a link ends by being revoked.
 */
@Entity
@Table(name = "share_links")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ShareLink extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false, updatable = false)
    private Trip trip;

    /** 32 characters of [a-z0-9]; CHAR(32) in MySQL, hence the explicit JDBC type for ddl-auto=validate. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "token", nullable = false, unique = true, updatable = false, length = 32)
    private String token;

    // Hibernate 7 maps @Enumerated(STRING) to the native MySQL ENUM type, matching the migration
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "permission", nullable = false, updatable = false)
    private SharePermission permission = SharePermission.VIEW;

    /** Null = never expires. */
    @Column(name = "expires_at", updatable = false)
    private Instant expiresAt;

    /** Null = live. */
    @Column(name = "revoked_at")
    private Instant revokedAt;

    /** Successful openings of the public page. Read-only here: see the class comment. */
    @Builder.Default
    @Column(name = "view_count", nullable = false, updatable = false)
    private int viewCount = 0;

    /** The owner who created the link; kept for audit. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private User createdBy;

}

package com.trieu.tripplanner.model;

import com.trieu.tripplanner.model.enums.MemberRole;
import com.trieu.tripplanner.model.enums.MemberStatus;
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
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A person invited to a trip (design.md 5.2 "trip_members", 6.2). Column types must match
 * V11__create_trip_members.sql exactly: ddl-auto=validate refuses to start otherwise.
 * <p>
 * The owner of a trip never has a row here; {@code Trip.owner} is the only source of truth for ownership.
 * A row lives through the whole story of one email on one trip: invited (PENDING, token set), accepted
 * (ACCEPTED, user set, token cleared), removed (REMOVED), invited again (back to PENDING with a new token).
 * It is never deleted by the application and has no {@code deleted_at}.
 * <p>
 * {@code trip}, {@code invitedEmail} and {@code invitedBy} have no setters: they identify the row and are fixed
 * when it is created. Everything the invitation lifecycle changes has one.
 */
@Entity
@Table(name = "trip_members")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TripMember extends BaseEntity {

    @Setter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false, updatable = false)
    private Trip trip;

    /** Null until the invited email belongs to an account; set when inviting an existing user or on accept. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /** Lowercase, normalised like {@code User.email}; the UNIQUE key (trip_id, invited_email) relies on it. */
    @Setter(AccessLevel.NONE)
    @Column(name = "invited_email", nullable = false, updatable = false, length = 255)
    private String invitedEmail;

    // Hibernate 7 maps @Enumerated(STRING) to the native MySQL ENUM type, matching the migration
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private MemberRole role;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MemberStatus status = MemberStatus.PENDING;

    /** SHA-256 hex of the raw invitation token (CHAR(64)); null once accepted. The raw token is never stored. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "invite_token_hash", unique = true, length = 64)
    private String inviteTokenHash;

    @Column(name = "invite_expires_at")
    private Instant inviteExpiresAt;

    @Setter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invited_by", nullable = false, updatable = false)
    private User invitedBy;

    /** When the (latest) invitation mail was sent; re-inviting moves it forward. */
    @Column(name = "invited_at", nullable = false)
    private Instant invitedAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

}

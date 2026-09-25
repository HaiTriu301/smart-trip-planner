package com.trieu.tripplanner.model;

import com.trieu.tripplanner.model.enums.TripStatus;
import com.trieu.tripplanner.model.enums.TripVisibility;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

/**
 * A planned journey (design.md 5.2 "trips"). Column types must match V5__create_trips_table.sql exactly:
 * ddl-auto=validate refuses to start otherwise.
 * <p>
 * Soft delete (CLAUDE.md rule 10, design.md 14.8): {@code repository.delete()} only stamps {@code deleted_at}.
 * Because the entity is versioned, Hibernate binds both id and version to the custom DELETE statement,
 * so the SQL must contain two placeholders; the version is bumped too, like any other write.
 * <p>
 * {@code owner}, {@code slug} and {@code version} have no setters: the owner is fixed at creation (ownership
 * transfer is a separate action, design.md 14.7), the slug never changes so shared links keep working,
 * and the version belongs to Hibernate.
 */
@Entity
@Table(name = "trips")
@SQLDelete(sql = "UPDATE trips SET deleted_at = NOW(6), version = version + 1 WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Trip extends BaseEntity {

    public static final String DEFAULT_CURRENCY = "VND";

    @Setter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "title", nullable = false, length = 160)
    private String title;

    @Setter(AccessLevel.NONE)
    @Column(name = "slug", nullable = false, unique = true, length = 200)
    private String slug;

    // TEXT in MySQL: without columnDefinition, validate expects VARCHAR(255)
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "cover_image_url", length = 512)
    private String coverImageUrl;

    @Column(name = "destination_name", length = 200)
    private String destinationName;

    @Column(name = "destination_lat", precision = 10, scale = 7)
    private BigDecimal destinationLat;

    @Column(name = "destination_lng", precision = 10, scale = 7)
    private BigDecimal destinationLng;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /** Money is always BigDecimal / DECIMAL(15,2) (CLAUDE.md rule 9). */
    @Column(name = "budget_amount", precision = 15, scale = 2)
    private BigDecimal budgetAmount;

    // CHAR(3) in MySQL: tell Hibernate so ddl-auto=validate does not expect VARCHAR
    @Builder.Default
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false, length = 3)
    private String currency = DEFAULT_CURRENCY;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TripStatus status = TripStatus.DRAFT;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false)
    private TripVisibility visibility = TripVisibility.PRIVATE;

    /** Optimistic lock (CLAUDE.md rule 22, design.md 11.3): 0 on insert, +1 on every update. */
    @Setter(AccessLevel.NONE)
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /** Set by the @SQLDelete statement only; null means the trip is live. */
    @Setter(AccessLevel.NONE)
    @Column(name = "deleted_at")
    private Instant deletedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }

}

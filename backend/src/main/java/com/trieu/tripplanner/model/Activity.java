package com.trieu.tripplanner.model;

import com.trieu.tripplanner.model.enums.ActivityType;
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
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.LocalTimeJdbcType;

/**
 * One thing to do inside a trip day (design.md 5.2 "activities"). Column types must match
 * V7__create_activities.sql exactly (ddl-auto=validate).
 * <p>
 * Hard delete, no soft delete: an activity is a child of its day and goes with it (ON DELETE CASCADE).
 * <p>
 * The two TIME columns use {@link LocalTimeJdbcType}: the LocalTime is handed to the driver as it is. The default
 * mapping goes through java.sql.Time and hibernate.jdbc.time_zone=UTC, which shifts the value by the JVM's offset
 * (03:00 in a +07:00 JVM is stored as 20:00) and breaks every comparison done in SQL.
 * <p>
 * Only the fields a user edits have setters. {@code tripDay} and {@code orderIndex} change together through
 * {@link #moveTo}, used by the reorder endpoint and by the placement by start time; {@code createdBy} is fixed at
 * creation and {@code version} belongs to Hibernate.
 * <p>
 * The version protects the content of the activity, not its position (design.md 11.3): {@code tripDay} and
 * {@code orderIndex} are excluded from the optimistic lock, so a drag and drop by one user never makes another
 * user's open edit form stale.
 */
@Entity
@Table(name = "activities")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Activity extends BaseEntity {

    @OptimisticLock(excluded = true)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_day_id", nullable = false)
    private TripDay tripDay;

    @Setter
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Setter
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ActivityType type = ActivityType.OTHER;

    /** Wall-clock time inside the day; the calendar date is the day's. Null = not scheduled yet. */
    @Setter
    @JdbcType(LocalTimeJdbcType.class)
    @Column(name = "start_time")
    private LocalTime startTime;

    /** Needs a start time and must be after it (rule 14.4, chk_activities_time_range). */
    @Setter
    @JdbcType(LocalTimeJdbcType.class)
    @Column(name = "end_time")
    private LocalTime endTime;

    /** Position inside the day, spaced by 1000 so an insert in between renumbers nothing (rule 14.5). */
    @OptimisticLock(excluded = true)
    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    // TEXT in MySQL: without columnDefinition, validate expects VARCHAR(255)
    @Setter
    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    /** Money is always BigDecimal / DECIMAL(15,2) (CLAUDE.md rule 9). */
    @Setter
    @Column(name = "cost_amount", precision = 15, scale = 2)
    private BigDecimal costAmount;

    // CHAR(3) in MySQL: tell Hibernate so ddl-auto=validate does not expect VARCHAR
    @Setter
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", length = 3)
    private String currency;

    @Setter
    @Column(name = "booking_url", length = 512)
    private String bookingUrl;

    /** Who added the activity; shown to collaborators (design.md 11). Never taken from the request body. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private User createdBy;

    /** Optimistic lock (CLAUDE.md rule 22, design.md 11.3): 0 on insert, +1 on every update. */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /**
     * Puts the activity at a position of a day: the same day for a reorder, another one for a move. Day and
     * position always change together, a position means nothing without its day.
     */
    public void moveTo(TripDay day, int newOrderIndex) {
        this.tripDay = day;
        this.orderIndex = newOrderIndex;
    }

}

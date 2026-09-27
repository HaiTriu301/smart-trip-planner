package com.trieu.tripplanner.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One calendar day of a trip (design.md 5.2 "trip_days"): the slot activities are attached to.
 * Column types must match V6__create_trip_days.sql exactly (ddl-auto=validate).
 * <p>
 * Days are generated and kept in sync with the trip's date range by the service (rules 14.2, 14.3); they are
 * consecutive, one per date, with {@code dayIndex} 1..n. The association is one-way on purpose: Trip has no
 * {@code days} collection, so days are always loaded through the repository with a known number of queries.
 * <p>
 * No soft delete and no @Version (design.md 5.2): a day disappears only when the user shortens the trip.
 */
@Entity
@Table(name = "trip_days")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TripDay extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    /** 1-based position inside the trip ("Ngày 1"); renumbered by TripDayService when the date range changes. */
    @Setter
    @Column(name = "day_index", nullable = false)
    private int dayIndex;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Setter
    @Column(name = "title", length = 160)
    private String title;

    // TEXT in MySQL: without columnDefinition, validate expects VARCHAR(255)
    @Setter
    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

}

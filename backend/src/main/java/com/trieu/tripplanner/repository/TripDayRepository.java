package com.trieu.tripplanner.repository;

import com.trieu.tripplanner.model.TripDay;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Days of a trip (design.md 5.2 "trip_days"). Rows are created and kept in sync with the trip's date range
 * by TripDayService only; nothing else inserts or deletes them.
 */
public interface TripDayRepository extends JpaRepository<TripDay, Long> {
}

package com.trieu.tripplanner.repository;

import com.trieu.tripplanner.model.TripDay;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Days of a trip (design.md 5.2 "trip_days"). Rows are created and kept in sync with the trip's date range
 * by TripDayService only; nothing else inserts or deletes them.
 */
public interface TripDayRepository extends JpaRepository<TripDay, Long> {

    /** One query, served by uk_trip_days_trip_date (trip_id, date); date order equals day_index order. */
    List<TripDay> findByTripIdOrderByDate(Long tripId);

    /** Empty when the day belongs to another trip, so /trips/1/days/{day of trip 2} answers 404. */
    Optional<TripDay> findByIdAndTripId(Long id, Long tripId);

}

package com.trieu.tripplanner.repository;

import com.trieu.tripplanner.model.TripDay;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Days of a trip (design.md 5.2 "trip_days"). Rows are created and kept in sync with the trip's date range
 * by TripDayService only; nothing else inserts or deletes them.
 * <p>
 * The bulk statements below bypass the persistence context. They run before any day of the trip is loaded
 * in that transaction, and flushAutomatically writes pending changes (e.g. the trip's new dates) first.
 */
public interface TripDayRepository extends JpaRepository<TripDay, Long> {

    /** One query, served by uk_trip_days_trip_date (trip_id, date); date order equals day_index order. */
    List<TripDay> findByTripIdOrderByDate(Long tripId);

    /** Empty when the day belongs to another trip, so /trips/1/days/{day of trip 2} answers 404. */
    Optional<TripDay> findByIdAndTripId(Long id, Long tripId);

    /**
     * Several days of one trip in one query, in calendar order. A day of another trip is simply missing from
     * the result; the reorder service turns that into a 404 for the whole batch.
     */
    @Query("select d from TripDay d where d.id in :ids and d.trip.id = :tripId order by d.date")
    List<TripDay> findAllByIdInAndTripId(@Param("ids") Collection<Long> ids, @Param("tripId") Long tripId);

    /*
     * Shifting every date of a trip by N days (design.md rule 14.3). MySQL checks UNIQUE (trip_id, date) after
     * EACH row, not at the end of the statement: moving day 1 from 01/10 to 02/10 while day 2 still holds 02/10
     * fails. Updating in the right order avoids it — latest date first when moving later, earliest first when
     * moving earlier. ORDER BY cannot take a direction parameter, hence two statements. Native because JPQL has
     * neither DATE_ADD nor UPDATE ... ORDER BY.
     */

    /** @param days positive: move the whole trip later */
    @Modifying(flushAutomatically = true)
    @Query(value = """
            UPDATE trip_days SET date = DATE_ADD(date, INTERVAL :days DAY), updated_at = NOW(6)
            WHERE trip_id = :tripId ORDER BY date DESC""", nativeQuery = true)
    int shiftDatesLatestFirst(@Param("tripId") Long tripId, @Param("days") long days);

    /** @param days negative: move the whole trip earlier */
    @Modifying(flushAutomatically = true)
    @Query(value = """
            UPDATE trip_days SET date = DATE_ADD(date, INTERVAL :days DAY), updated_at = NOW(6)
            WHERE trip_id = :tripId ORDER BY date ASC""", nativeQuery = true)
    int shiftDatesEarliestFirst(@Param("tripId") Long tripId, @Param("days") long days);

    /** Hard delete of the days cut off by a shorter range (no soft delete, design.md 5.2). */
    @Modifying(flushAutomatically = true)
    @Query("delete from TripDay d where d.trip.id = :tripId and (d.date < :start or d.date > :end)")
    int deleteOutsideRange(@Param("tripId") Long tripId, @Param("start") LocalDate start,
                           @Param("end") LocalDate end);

}

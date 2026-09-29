package com.trieu.tripplanner.repository;

import com.trieu.tripplanner.dto.internal.DroppedActivities;
import com.trieu.tripplanner.model.Activity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Activities of a trip day (design.md 5.2 "activities"). Hard delete: there is no soft-delete filter here.
 */
public interface ActivityRepository extends JpaRepository<Activity, Long> {

    /**
     * Highest order_index used in the day, 0 when the day is empty. Served by idx_activities_day_order
     * (trip_day_id, order_index), so MySQL reads one index entry instead of the day's rows.
     */
    @Query("select coalesce(max(a.orderIndex), 0) from Activity a where a.tripDay.id = :dayId")
    int findMaxOrderIndexByTripDayId(@Param("dayId") Long dayId);

    /**
     * Activities of the day that have both a start and an end time, earliest first: the candidates for the
     * overlap check of rule 14.4, which ActivityService then does in Java.
     * <p>
     * The times are deliberately NOT compared in SQL. A LocalTime passed as a query parameter is bound with
     * Hibernate's default TIME type, not with the LocalTimeJdbcType of the entity attributes, so
     * hibernate.jdbc.time_zone=UTC shifts it by the JVM offset and "start_time < :end" compares a wall-clock
     * column with a shifted value. Reading the column is safe; binding a time parameter is not.
     */
    @Query("""
            select a from Activity a
            where a.tripDay.id = :dayId and a.startTime is not null and a.endTime is not null
            order by a.startTime, a.id""")
    List<Activity> findTimedByTripDayId(@Param("dayId") Long dayId);

    /**
     * Activities of the day in display order. Served by idx_activities_day_order (trip_day_id, order_index).
     * The id breaks the tie when two activities share an index (two simultaneous inserts): the older one first.
     */
    List<Activity> findByTripDayIdOrderByOrderIndexAscIdAsc(Long dayId);

    /**
     * Empty when the activity belongs to another trip, so /trips/1/activities/{activity of trip 2} answers 404.
     * The trip id is read from trip_days; the trips table is not joined.
     */
    @Query("select a from Activity a where a.id = :id and a.tripDay.trip.id = :tripId")
    Optional<Activity> findByIdAndTripId(@Param("id") Long id, @Param("tripId") Long tripId);

    /**
     * What would be lost if the trip kept only the days from start to end (design.md rule 14.3): the activities
     * of the days outside that range. Asked before the days are deleted, because the delete cascades to the
     * activities without a trace. An aggregate without GROUP BY always returns one row; both counts are 0 when
     * the cut days are empty.
     */
    @Query("""
            select new com.trieu.tripplanner.dto.internal.DroppedActivities(count(distinct d.id), count(a))
            from Activity a join a.tripDay d
            where d.trip.id = :tripId and (d.date < :start or d.date > :end)""")
    DroppedActivities countInDaysOutsideRange(@Param("tripId") Long tripId, @Param("start") LocalDate start,
                                              @Param("end") LocalDate end);

}

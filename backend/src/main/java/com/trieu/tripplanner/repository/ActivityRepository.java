package com.trieu.tripplanner.repository;

import com.trieu.tripplanner.model.Activity;
import java.util.List;
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

}

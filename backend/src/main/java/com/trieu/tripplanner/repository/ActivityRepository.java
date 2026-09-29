package com.trieu.tripplanner.repository;

import com.trieu.tripplanner.model.Activity;
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

}

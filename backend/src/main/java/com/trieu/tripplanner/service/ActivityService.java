package com.trieu.tripplanner.service;

import com.trieu.tripplanner.dto.request.CreateActivityRequest;
import com.trieu.tripplanner.dto.request.ReorderActivitiesRequest;
import com.trieu.tripplanner.dto.request.UpdateActivityRequest;
import com.trieu.tripplanner.dto.response.ActivityResponse;
import com.trieu.tripplanner.dto.response.TripDayDetailResponse;
import java.util.List;

/**
 * Activities of a trip day (design.md 10.2 "Itinerary", rules 14.4 and 14.5). Permission checks are NOT done
 * here: controllers guard every call with {@code @tripPermission} (CLAUDE.md rule 15). Methods therefore only
 * answer 404 when the trip, the day or the activity cannot be reached through the ids of the URL.
 */
public interface ActivityService {

    /**
     * Activities of one day in display order (orderIndex, then id).
     *
     * @throws com.trieu.tripplanner.exception.ResourceNotFoundException trip missing or deleted, or the day
     *                                                                   belongs to another trip (404)
     */
    List<ActivityResponse> list(Long tripId, Long dayId);

    /**
     * Adds an activity at the end of the day: orderIndex = highest index of the day + 1000.
     *
     * @param tripId       from the URL; the day must belong to it
     * @param userId       from the authenticated principal (CLAUDE.md rule 16), stored as the creator
     * @param allowOverlap true: the client confirmed that the activity may overlap another one (rule 14.4)
     * @throws com.trieu.tripplanner.exception.ResourceNotFoundException trip missing or deleted, or the day
     *                                                                   belongs to another trip (404)
     * @throws com.trieu.tripplanner.exception.BusinessRuleException     endTime without startTime, or endTime
     *                                                                   not after startTime (400); the time
     *                                                                   range overlaps another activity of the
     *                                                                   day and allowOverlap is false (409)
     */
    ActivityResponse create(Long tripId, Long dayId, Long userId, CreateActivityRequest request,
                            boolean allowOverlap);

    /**
     * Partial update: null keeps the current value, a blank note or bookingUrl clears it. The day, the position
     * and the creator never change here. Rules are checked on the merged result.
     * <p>
     * The overlap rule is applied only when the time range actually changes, and never against the activity
     * itself: renaming an activity that was saved with allowOverlap must not be refused.
     *
     * @param allowOverlap true: the client confirmed that the new time range may overlap another activity
     * @throws com.trieu.tripplanner.exception.ResourceNotFoundException trip missing or deleted, or the activity
     *                                                                   belongs to another trip (404)
     * @throws com.trieu.tripplanner.exception.BusinessRuleException     invalid merged times (400); the new
     *                                                                   range overlaps another activity (409)
     */
    ActivityResponse update(Long tripId, Long activityId, UpdateActivityRequest request, boolean allowOverlap);

    /**
     * Hard delete (design.md 5.2 "activities"): the row is gone, there is nothing to restore. The order indexes
     * of the other activities of the day are left as they are; the gap is harmless (rule 14.5).
     *
     * @throws com.trieu.tripplanner.exception.ResourceNotFoundException trip missing or deleted, or the activity
     *                                                                   belongs to another trip (404)
     */
    void delete(Long tripId, Long activityId);

    /**
     * Applies every move of the request in one transaction (design.md 10.2 "Quy ước Reorder"): all of them or
     * none. An activity keeps its content and its version; only its day and position change.
     *
     * @return the days that lost or received an activity, in calendar order, each with its activities in the
     *         new display order
     * @throws com.trieu.tripplanner.exception.ResourceNotFoundException trip missing or deleted, or an activity
     *                                                                   or a day of the request belongs to
     *                                                                   another trip (404); nothing is moved
     * @throws com.trieu.tripplanner.exception.BusinessRuleException     the same activity twice in the request
     *                                                                   (400); nothing is moved
     */
    List<TripDayDetailResponse> reorder(Long tripId, ReorderActivitiesRequest request);

}

package com.trieu.tripplanner.service;

import com.trieu.tripplanner.dto.request.CreateActivityRequest;
import com.trieu.tripplanner.dto.response.ActivityResponse;
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

}

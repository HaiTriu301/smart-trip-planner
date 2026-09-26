package com.trieu.tripplanner.service;

import com.trieu.tripplanner.common.PageResponse;
import com.trieu.tripplanner.dto.internal.TripFilter;
import com.trieu.tripplanner.dto.request.CreateTripRequest;
import com.trieu.tripplanner.dto.request.UpdateTripRequest;
import com.trieu.tripplanner.dto.response.TripResponse;
import com.trieu.tripplanner.dto.response.TripSummaryResponse;
import org.springframework.data.domain.Pageable;

/**
 * Trip CRUD (design.md 10.2, rules 14.1 and 14.8). Permission checks are NOT done here: controllers guard
 * every trip-scoped call with {@code @tripPermission} (CLAUDE.md rule 15). Methods taking a tripId therefore
 * only need to answer 404 when the trip does not exist.
 */
public interface TripService {

    /**
     * Trips owned by the user, newest first by default.
     *
     * @param userId from the authenticated principal (CLAUDE.md rule 16)
     * @throws com.trieu.tripplanner.exception.BusinessRuleException sort on an unsupported property (400)
     */
    PageResponse<TripSummaryResponse> list(Long userId, TripFilter filter, Pageable pageable);

    /**
     * Creates a DRAFT trip owned by the user, with a fresh unique slug.
     *
     * @throws com.trieu.tripplanner.exception.BusinessRuleException invalid date range or half coordinates (400)
     */
    TripResponse create(Long userId, CreateTripRequest request);

    /**
     * @throws com.trieu.tripplanner.exception.ResourceNotFoundException missing or deleted trip (404)
     */
    TripResponse get(Long tripId);

    /**
     * Partial update; rules are checked on the merged result. The slug and status never change here.
     *
     * @throws com.trieu.tripplanner.exception.ResourceNotFoundException missing or deleted trip (404)
     * @throws com.trieu.tripplanner.exception.BusinessRuleException     invalid merged state (400)
     */
    TripResponse update(Long tripId, UpdateTripRequest request);

    /**
     * Soft delete (design.md 14.8): the row keeps existing with deleted_at set.
     *
     * @throws com.trieu.tripplanner.exception.ResourceNotFoundException missing or already deleted trip (404)
     */
    void delete(Long tripId);

}

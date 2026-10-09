package com.trieu.tripplanner.dto.response;

import com.trieu.tripplanner.model.enums.TripStatus;
import com.trieu.tripplanner.model.enums.TripVisibility;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * GET /api/v1/trips/{id} only (design.md 10.2 "Quy ước Trip API"): the fields of {@link TripResponse} plus the
 * days in calendar order, each with its activities in display order. Flat on purpose so the client reads the
 * same field names as TripResponse.
 * POST / PATCH keep returning the lighter TripResponse, so a write never has to load the days.
 * Since Task 4.1 also the members (same list as GET /members, owner first) and the caller's own role, so the UI
 * can hide every write control from a viewer without a second request.
 */
public record TripDetailResponse(
        Long id,
        Long ownerId,
        String title,
        String slug,
        String description,
        String coverImageUrl,
        String destinationName,
        BigDecimal destinationLat,
        BigDecimal destinationLng,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal budgetAmount,
        String currency,
        TripStatus status,
        TripVisibility visibility,
        Long version,
        Instant createdAt,
        Instant updatedAt,
        List<TripDayDetailResponse> days,
        List<MemberResponse> members,
        // What the caller is to this trip: OWNER, EDITOR or VIEWER (the evaluator let nobody else in)
        TripRole myRole) {
}

package com.trieu.tripplanner.dto.response;

import java.time.LocalDate;
import java.util.List;

/**
 * One day inside GET /api/v1/trips/{id} (design.md 10.2 "Quy ước Trip API"): the fields of
 * {@link TripDayResponse} plus the activities of the day in display order. Flat on purpose so the client reads
 * the same field names as TripDayResponse.
 * <p>
 * Used by the trip detail only. GET /days and PATCH /days/{dayId} keep returning the lighter TripDayResponse.
 *
 * @param activities never null; empty when nothing is planned for the day yet
 */
public record TripDayDetailResponse(
        Long id,
        int dayIndex,
        LocalDate date,
        String title,
        String note,
        List<ActivityResponse> activities) {
}

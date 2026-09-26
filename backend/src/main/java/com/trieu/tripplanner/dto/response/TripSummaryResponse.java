package com.trieu.tripplanner.dto.response;

import com.trieu.tripplanner.model.enums.TripStatus;
import com.trieu.tripplanner.model.enums.TripVisibility;
import java.time.Instant;
import java.time.LocalDate;

/**
 * One row of the trip list (a card in the grid). Not related to GET /trips/{id}/summary
 * (design.md 10.2 "Quy ước Trip API"). Holds nothing that needs a join, so the list stays one query.
 */
public record TripSummaryResponse(
        Long id,
        String title,
        String slug,
        String coverImageUrl,
        String destinationName,
        LocalDate startDate,
        LocalDate endDate,
        TripStatus status,
        TripVisibility visibility,
        Instant createdAt) {
}

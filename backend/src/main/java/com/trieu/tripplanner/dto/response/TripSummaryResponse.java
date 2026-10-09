package com.trieu.tripplanner.dto.response;

import com.trieu.tripplanner.model.enums.TripStatus;
import com.trieu.tripplanner.model.enums.TripVisibility;
import java.time.Instant;
import java.time.LocalDate;

/**
 * One row of the trip list (a card in the grid). Not related to GET /trips/{id}/summary
 * (design.md 10.2 "Quy ước Trip API"). The owner is the only thing from another table: the list query fetches
 * it along with the trips (TripSpecifications.withOwner), so the card can say "Được chia sẻ · của {ownerName}"
 * when ownerId is not the caller.
 */
public record TripSummaryResponse(
        Long id,
        Long ownerId,
        String ownerName,
        String title,
        String slug,
        String coverImageUrl,
        String destinationName,
        LocalDate startDate,
        LocalDate endDate,
        TripStatus status,
        TripVisibility visibility,
        Instant createdAt,
        // All activities of the trip, every day together (shown on the trip card)
        long activityCount) {
}

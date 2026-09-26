package com.trieu.tripplanner.dto.response;

import com.trieu.tripplanner.model.enums.TripStatus;
import com.trieu.tripplanner.model.enums.TripVisibility;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Full view of one trip (GET/POST/PATCH). Days, activities and members are added by later tasks
 * (design.md 10.2 "Phạm vi theo phase").
 *
 * @param version optimistic-lock version; clients will have to send it back from Task 5.3
 */
public record TripResponse(
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
        Instant updatedAt) {
}

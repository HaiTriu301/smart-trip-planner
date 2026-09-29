package com.trieu.tripplanner.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.trieu.tripplanner.model.enums.ActivityType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;

/**
 * One activity as the client sees it (design.md 10.2 "Quy ước Activity API").
 *
 * @param dayId       the trip day the activity belongs to
 * @param startTime   written as "HH:mm" (design.md 10.1); null = not scheduled
 * @param orderIndex  position inside the day, spaced by 1000 (rule 14.5)
 * @param createdById id of the user who added it; the client resolves the name from the member list
 * @param version     optimistic lock counter; the client sends it back from Task 5.3 on
 */
public record ActivityResponse(
        Long id,
        Long dayId,
        String title,
        ActivityType type,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(pattern = "HH:mm") LocalTime endTime,
        int orderIndex,
        String note,
        BigDecimal costAmount,
        String currency,
        String bookingUrl,
        Long createdById,
        Long version,
        Instant createdAt,
        Instant updatedAt) {
}

package com.trieu.tripplanner.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Body of PATCH /api/v1/trips/{tripId}/days/{dayId} (design.md 10.2 "Quy ước sửa ngày").
 * For each field: absent or null → keep the current value; "" or only spaces → clear it (stored as NULL);
 * anything else → trimmed and stored. Unlike the trip PATCH, clearing is supported because setting and
 * removing a day title is an everyday action. {@code @Size} ignores null, so omitted fields are never rejected.
 */
public record UpdateTripDayRequest(
        @Size(max = 160, message = "{validation.trip-day.title.too-long}")
        String title,

        @Size(max = 5000, message = "{validation.trip-day.note.too-long}")
        String note) {
}

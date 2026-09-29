package com.trieu.tripplanner.dto.request;

import com.trieu.tripplanner.model.enums.ActivityType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * Body of PATCH /api/v1/trips/{tripId}/activities/{activityId}: partial update (design.md 10.2 "Quy ước
 * Activity API"). For every field: absent or null → keep the current value.
 * <p>
 * The two optional texts, {@code note} and {@code bookingUrl}, can also be cleared: "" or only spaces → stored
 * as NULL. Times, cost and currency cannot be cleared once set (same limit as the trip PATCH).
 * <p>
 * Not in the body on purpose: the day and orderIndex change only through the reorder endpoint (Task 2.4).
 * Constraints mirror {@link CreateActivityRequest}; every annotation used here ignores null, so omitted fields
 * are never rejected. The rules between startTime and endTime are checked in ActivityService on the merged state.
 */
public record UpdateActivityRequest(
        // @NotBlank would reject null; this only rejects a present-but-blank title
        @Pattern(regexp = "(?s).*\\S.*", message = "{validation.activity.title.required}")
        @Size(max = 200, message = "{validation.activity.title.too-long}")
        String title,

        ActivityType type,

        LocalTime startTime,

        LocalTime endTime,

        @Size(max = 5000, message = "{validation.activity.note.too-long}")
        String note,

        @DecimalMin(value = "0", message = "{validation.activity.cost.negative}")
        @Digits(integer = 13, fraction = 2, message = "{validation.activity.cost.digits}")
        BigDecimal costAmount,

        @Pattern(regexp = "^[A-Z]{3}$", message = "{validation.activity.currency.invalid}")
        String currency,

        // Blank is allowed: it means "remove the link"
        @Size(max = 512, message = "{validation.activity.booking-url.too-long}")
        @Pattern(regexp = "^\\s*$|^https?://\\S+$", message = "{validation.activity.booking-url.invalid}")
        String bookingUrl) {
}

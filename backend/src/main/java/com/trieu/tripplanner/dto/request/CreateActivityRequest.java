package com.trieu.tripplanner.dto.request;

import com.trieu.tripplanner.model.enums.ActivityType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * Body of POST /api/v1/trips/{tripId}/days/{dayId}/activities (design.md 10.2 "Quy ước Activity API").
 * Only shape rules live here (CLAUDE.md rule 13); the rules between startTime and endTime are checked in
 * ActivityService because PATCH must apply them to the merged state too.
 * <p>
 * Not in the body on purpose: the creator is the signed-in user (CLAUDE.md rule 16), the day comes from the URL,
 * and orderIndex is assigned by the server (rule 14.5).
 *
 * @param type      null → OTHER
 * @param startTime "HH:mm" or "HH:mm:ss"; seconds are dropped
 * @param endTime   needs a startTime and must be after it
 * @param currency  ISO 4217 code; null with a cost → the currency of the trip
 * @param placeId   id of a place returned by POST /places or POST /places/manual; null → no place. Whether the
 *                  place exists and may be used by this user is checked in the service
 */
public record CreateActivityRequest(
        @NotBlank(message = "{validation.activity.title.required}")
        @Size(max = 200, message = "{validation.activity.title.too-long}")
        String title,

        ActivityType type,

        LocalTime startTime,

        LocalTime endTime,

        @Size(max = 255, message = "{validation.activity.note.too-long}")
        String note,

        @DecimalMin(value = "0", message = "{validation.activity.cost.negative}")
        @Digits(integer = 13, fraction = 2, message = "{validation.activity.cost.digits}")
        BigDecimal costAmount,

        @Pattern(regexp = "^[A-Z]{3}$", message = "{validation.activity.currency.invalid}")
        String currency,

        @Size(max = 512, message = "{validation.activity.booking-url.too-long}")
        @Pattern(regexp = "^https?://\\S+$", message = "{validation.activity.booking-url.invalid}")
        String bookingUrl,

        Long placeId) {
}

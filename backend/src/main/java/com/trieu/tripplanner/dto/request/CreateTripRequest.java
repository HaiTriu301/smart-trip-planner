package com.trieu.tripplanner.dto.request;

import com.trieu.tripplanner.model.enums.TripVisibility;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Body of POST /api/v1/trips. Only shape rules live here (CLAUDE.md rule 13); rules spanning several fields
 * (end >= start, max 60 days, lat/lng given together) are checked in TripService because PATCH must apply
 * them to the merged state too (design.md 14.1, 10.2 "Quy ước Trip API").
 * The owner is the signed-in user, never a body field (CLAUDE.md rule 16); status always starts as DRAFT.
 *
 * @param currency   ISO 4217 code; null → VND
 * @param visibility null → PRIVATE
 */
public record CreateTripRequest(
        @NotBlank(message = "{validation.trip.title.required}")
        @Size(max = 160, message = "{validation.trip.title.too-long}")
        String title,

        @Size(max = 5000, message = "{validation.trip.description.too-long}")
        String description,

        @Size(max = 512, message = "{validation.trip.cover-image-url.too-long}")
        @Pattern(regexp = "^https?://\\S+$", message = "{validation.trip.cover-image-url.invalid}")
        String coverImageUrl,

        @Size(max = 200, message = "{validation.trip.destination-name.too-long}")
        String destinationName,

        @DecimalMin(value = "-90", message = "{validation.trip.latitude.range}")
        @DecimalMax(value = "90", message = "{validation.trip.latitude.range}")
        @Digits(integer = 3, fraction = 7, message = "{validation.trip.coordinate.precision}")
        BigDecimal destinationLat,

        @DecimalMin(value = "-180", message = "{validation.trip.longitude.range}")
        @DecimalMax(value = "180", message = "{validation.trip.longitude.range}")
        @Digits(integer = 3, fraction = 7, message = "{validation.trip.coordinate.precision}")
        BigDecimal destinationLng,

        @NotNull(message = "{validation.trip.start-date.required}")
        LocalDate startDate,

        @NotNull(message = "{validation.trip.end-date.required}")
        LocalDate endDate,

        @DecimalMin(value = "0", message = "{validation.trip.budget.negative}")
        @Digits(integer = 13, fraction = 2, message = "{validation.trip.budget.digits}")
        BigDecimal budgetAmount,

        @Pattern(regexp = "^[A-Z]{3}$", message = "{validation.trip.currency.invalid}")
        String currency,

        TripVisibility visibility) {
}

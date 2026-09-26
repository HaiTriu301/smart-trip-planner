package com.trieu.tripplanner.dto.request;

import com.trieu.tripplanner.model.enums.TripVisibility;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Body of PATCH /api/v1/trips/{id}: partial update, a null field keeps its current value
 * (design.md 10.2 "Quy ước Trip API"). Clearing an optional field is not supported yet.
 * No {@code status}: it changes only through PATCH /{id}/status. Constraints mirror {@link CreateTripRequest};
 * every annotation used here ignores null, so omitted fields are never rejected.
 */
public record UpdateTripRequest(
        // @NotBlank would reject null; this only rejects a present-but-blank title
        @Pattern(regexp = "(?s).*\\S.*", message = "{validation.trip.title.required}")
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

        LocalDate startDate,

        LocalDate endDate,

        @DecimalMin(value = "0", message = "{validation.trip.budget.negative}")
        @Digits(integer = 13, fraction = 2, message = "{validation.trip.budget.digits}")
        BigDecimal budgetAmount,

        @Pattern(regexp = "^[A-Z]{3}$", message = "{validation.trip.currency.invalid}")
        String currency,

        TripVisibility visibility) {
}

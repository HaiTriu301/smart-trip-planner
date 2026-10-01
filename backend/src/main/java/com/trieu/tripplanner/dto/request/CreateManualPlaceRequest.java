package com.trieu.tripplanner.dto.request;

import com.trieu.tripplanner.model.enums.ActivityType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Body of POST /api/v1/places/manual: a place the user describes because no search result fits (design.md 10.2
 * "Quy ước Place API"). Unlike a picked search result, every fact comes from the client here: the place is
 * private to its creator (rule 14.19), so nobody else depends on it being right.
 * <p>
 * Not in the body on purpose: the creator is the signed-in user (CLAUDE.md rule 16).
 *
 * @param address  optional; blank is stored as no address
 * @param category optional; one of the activity types, so the form can preselect the type of the activity
 */
public record CreateManualPlaceRequest(
        @NotBlank(message = "{validation.place.name.required}")
        @Size(max = 200, message = "{validation.place.name.too-long}")
        String name,

        @Size(max = 500, message = "{validation.place.address.too-long}")
        String address,

        @NotNull(message = "{validation.place.latitude.required}")
        @DecimalMin(value = "-90", message = "{validation.place.latitude.range}")
        @DecimalMax(value = "90", message = "{validation.place.latitude.range}")
        @Digits(integer = 3, fraction = 7, message = "{validation.place.coordinate.precision}")
        BigDecimal lat,

        @NotNull(message = "{validation.place.longitude.required}")
        @DecimalMin(value = "-180", message = "{validation.place.longitude.range}")
        @DecimalMax(value = "180", message = "{validation.place.longitude.range}")
        @Digits(integer = 3, fraction = 7, message = "{validation.place.coordinate.precision}")
        BigDecimal lng,

        ActivityType category) {
}

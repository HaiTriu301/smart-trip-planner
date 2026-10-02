package com.trieu.tripplanner.dto.request;

import com.trieu.tripplanner.model.enums.PlaceProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/v1/places: "I pick this search result" (design.md 10.2 "Quy ước Place API").
 * Only the two values that name the place are accepted. Name, address and coordinates are NOT in the body on
 * purpose: the server reads them from the source itself, because the stored copy is shared by every user
 * (rule 14.18) and must not depend on what one client sends.
 *
 * @param provider   as returned by the search
 * @param externalId as returned by the search
 */
public record SavePlaceRequest(
        @NotNull(message = "{validation.place.provider.required}")
        PlaceProvider provider,

        @NotBlank(message = "{validation.place.external-id.required}")
        @Size(max = 128, message = "{validation.place.external-id.too-long}")
        String externalId) {
}

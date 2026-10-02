package com.trieu.tripplanner.dto.response;

import com.trieu.tripplanner.model.enums.PlaceProvider;
import java.math.BigDecimal;

/**
 * A place stored in the app (design.md 10.2 "Quy ước Place API"). Unlike a search result it has an {@code id},
 * the value an activity refers to.
 *
 * @param category one of the ActivityType names for the mock data, so the form can preselect the activity type;
 *                 may be null
 */
public record PlaceResponse(
        Long id,
        PlaceProvider provider,
        String name,
        String address,
        BigDecimal lat,
        BigDecimal lng,
        String category) {
}

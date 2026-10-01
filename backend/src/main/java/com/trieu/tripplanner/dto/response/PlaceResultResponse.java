package com.trieu.tripplanner.dto.response;

import com.trieu.tripplanner.model.enums.PlaceProvider;
import java.math.BigDecimal;

/**
 * One row of GET /api/v1/places/search (design.md 10.2 "Quy ước Place API"). There is no {@code id}: a search
 * result is not in our database yet. The client sends {@code provider} + {@code externalId} back when the user
 * picks it (POST /places, Task 3.2).
 *
 * @param category one of the ActivityType names for the mock data, so the form can preselect the activity type;
 *                 may be null
 */
public record PlaceResultResponse(
        PlaceProvider provider,
        String externalId,
        String name,
        String address,
        BigDecimal lat,
        BigDecimal lng,
        String category) {
}

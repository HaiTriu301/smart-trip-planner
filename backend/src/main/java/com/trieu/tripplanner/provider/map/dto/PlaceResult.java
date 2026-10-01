package com.trieu.tripplanner.provider.map.dto;

import com.trieu.tripplanner.model.enums.PlaceProvider;
import java.math.BigDecimal;

/**
 * One place as a map source describes it. Not stored yet: it has no id of ours, only the source's own.
 *
 * @param provider   the source that returned it
 * @param externalId the id of the place inside that source; with {@code provider} it names the place for good
 * @param address    may be null when the source has none
 * @param category   one of the ActivityType names for the mock data; real sources may send anything, or null
 */
public record PlaceResult(
        PlaceProvider provider,
        String externalId,
        String name,
        String address,
        BigDecimal lat,
        BigDecimal lng,
        String category) {
}

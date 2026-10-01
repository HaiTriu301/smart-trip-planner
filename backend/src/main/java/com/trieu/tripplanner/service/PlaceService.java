package com.trieu.tripplanner.service;

import com.trieu.tripplanner.dto.response.PlaceResultResponse;
import com.trieu.tripplanner.exception.BusinessRuleException;
import com.trieu.tripplanner.mapper.PlaceMapper;
import com.trieu.tripplanner.provider.map.MapProvider;
import com.trieu.tripplanner.provider.map.dto.Coordinate;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Places (design.md 10.2 "Place"). Talks to the map source only through {@link MapProvider}: switching from the
 * bundled data to a real service changes configuration, not this class (CLAUDE.md rule 19).
 * Simple enough to be a class without interface (CLAUDE.md rule 5).
 */
@Service
@RequiredArgsConstructor
public class PlaceService {

    private final MapProvider mapProvider;
    private final PlaceMapper placeMapper;

    /**
     * Places matching a keyword. No database access: results come straight from the map source and are not
     * stored until the user picks one.
     *
     * @param query keyword as typed; spaces around it are dropped before it reaches the source
     * @param lat   with {@code lng}, the point around which matches are listed first (the destination of the
     *              trip being planned); both null: no preference
     * @throws BusinessRuleException 400 VALIDATION_ERROR on the missing one when only one of lat / lng is sent
     */
    public List<PlaceResultResponse> search(String query, int limit, BigDecimal lat, BigDecimal lng) {
        return placeMapper.toResultResponses(mapProvider.search(query.trim(), limit, toCoordinate(lat, lng)));
    }

    /** A point needs both numbers; half a coordinate is a client mistake, not "no preference". */
    private static Coordinate toCoordinate(BigDecimal lat, BigDecimal lng) {
        if ((lat == null) != (lng == null)) {
            String missing = (lat == null) ? "lat" : "lng";
            throw BusinessRuleException.invalidField(missing, "error.place.coordinates-incomplete",
                    "Only one of lat/lng was sent to the place search");
        }
        return (lat == null) ? null : new Coordinate(lat, lng);
    }

}

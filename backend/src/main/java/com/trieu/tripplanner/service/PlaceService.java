package com.trieu.tripplanner.service;

import com.trieu.tripplanner.dto.request.SavePlaceRequest;
import com.trieu.tripplanner.dto.response.PlaceResponse;
import com.trieu.tripplanner.dto.response.PlaceResultResponse;
import com.trieu.tripplanner.exception.BusinessRuleException;
import com.trieu.tripplanner.exception.ResourceNotFoundException;
import com.trieu.tripplanner.mapper.PlaceMapper;
import com.trieu.tripplanner.model.Place;
import com.trieu.tripplanner.model.enums.PlaceProvider;
import com.trieu.tripplanner.provider.map.MapProvider;
import com.trieu.tripplanner.provider.map.dto.Coordinate;
import com.trieu.tripplanner.provider.map.dto.PlaceResult;
import com.trieu.tripplanner.repository.PlaceRepository;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Places (design.md 10.2 "Place"). Talks to the map source only through {@link MapProvider}: switching from the
 * bundled data to a real service changes configuration, not this class (CLAUDE.md rule 19).
 * Simple enough to be a class without interface (CLAUDE.md rule 5).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceService {

    private static final String PLACE = "Place";

    private final MapProvider mapProvider;
    private final PlaceRepository placeRepository;
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

    /**
     * "I pick this search result": returns the stored copy of the place, creating it on first use (design.md
     * rule 14.18). Asking twice for the same place ends with one row and one id.
     *
     * @throws ResourceNotFoundException the source knows no place with this externalId (404)
     */
    public PlaceResponse getOrCreate(SavePlaceRequest request) {
        PlaceProvider provider = request.provider();
        String externalId = request.externalId().trim();
        Place place = placeRepository.findByProviderAndExternalId(provider, externalId)
                .orElseGet(() -> copyFromSource(provider, externalId));
        return placeMapper.toResponse(place);
    }

    /** Reads the place from the source itself, never from the request: the copy is shared by every user. */
    private Place copyFromSource(PlaceProvider provider, String externalId) {
        PlaceResult found = mapProvider.lookup(externalId)
                .orElseThrow(() -> new ResourceNotFoundException(PLACE, provider + "/" + externalId));
        Place saved = placeRepository.saveAndFlush(Place.builder()
                .provider(found.provider())
                .externalId(found.externalId())
                .name(found.name())
                .address(found.address())
                .lat(found.lat())
                .lng(found.lng())
                .category(found.category())
                .build());
        log.info("Place {} stored from {}/{}", saved.getId(), provider, externalId);
        return saved;
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

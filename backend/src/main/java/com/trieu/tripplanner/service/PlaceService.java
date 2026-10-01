package com.trieu.tripplanner.service;

import com.trieu.tripplanner.dto.response.PlaceResultResponse;
import com.trieu.tripplanner.mapper.PlaceMapper;
import com.trieu.tripplanner.provider.map.MapProvider;
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
     */
    public List<PlaceResultResponse> search(String query, int limit) {
        return placeMapper.toResultResponses(mapProvider.search(query.trim(), limit));
    }

}

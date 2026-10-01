package com.trieu.tripplanner.provider.map;

import com.trieu.tripplanner.provider.map.dto.Coordinate;
import com.trieu.tripplanner.provider.map.dto.PlaceResult;
import java.util.List;

/**
 * Port for everything a map service does for the app (design.md 7.2). The implementation is chosen by
 * {@code app.providers.map}: {@code mock} reads a bundled file, {@code osm} (Task 3.8) calls OpenStreetMap services.
 * Services depend on this interface only (CLAUDE.md rule 19).
 */
public interface MapProvider {

    /**
     * Places whose name or address matches the keyword, best match first.
     *
     * @param query what the user typed, already trimmed; never blank
     * @param limit maximum number of results, at least 1
     * @param near  where the user is planning (the destination of the trip): matching places around it come
     *              before matching places elsewhere. Null: no preference
     * @return at most {@code limit} places; empty when nothing matches. The same input gives the same list in
     *         the same order
     */
    List<PlaceResult> search(String query, int limit, Coordinate near);

}

package com.trieu.tripplanner.provider.map;

import com.trieu.tripplanner.model.enums.PlaceProvider;
import com.trieu.tripplanner.provider.map.dto.Coordinate;
import com.trieu.tripplanner.provider.map.dto.PlaceResult;
import com.trieu.tripplanner.provider.map.dto.RouteLeg;
import java.util.List;
import java.util.Optional;

/**
 * Port for everything a map service does for the app (design.md 7.2). The implementation is chosen by
 * {@code app.providers.map}: {@code mock} reads a bundled file, {@code osm} (Task 3.8) calls OpenStreetMap services.
 * Services depend on this interface only (CLAUDE.md rule 19).
 */
public interface MapProvider {

    /**
     * The source this implementation reads. Every result it returns carries this value, and it is the only
     * provider a client may name when it picks a result: an id of one source means nothing to another.
     */
    PlaceProvider provider();

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

    /**
     * One place by the id the source itself gave it in a search result. This is how the server reads the facts
     * of a place a client says it picked, instead of trusting the name and coordinates the client would send.
     *
     * @param externalId {@link PlaceResult#externalId()} of an earlier result
     * @return empty when the source knows no such place
     */
    Optional<PlaceResult> lookup(String externalId);

    /**
     * Travel along the points in the order given: one leg between every two consecutive points. The points are
     * visited as they come; the source never reorders them to find a shorter way.
     *
     * @param points the stops of the route, first to last
     * @return one leg fewer than there are points, leg {@code i} going from point {@code i} to point
     *         {@code i + 1}; empty when there are fewer than two points. One means of travel for every leg
     */
    List<RouteLeg> route(List<Coordinate> points);

}

package com.trieu.tripplanner.service;

import com.trieu.tripplanner.common.constant.CacheNames;
import com.trieu.tripplanner.model.enums.PlaceProvider;
import com.trieu.tripplanner.provider.map.MapProvider;
import com.trieu.tripplanner.provider.map.dto.Coordinate;
import com.trieu.tripplanner.provider.map.dto.RouteLeg;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * Temporary copies of travel legs (design.md 8.1 "route:legs"). Stands between RouteService and the map source:
 * the first time the travel through a list of points is asked for, the source answers and the answer is stored
 * in Redis; for the next 24 hours the same list, from any user, is answered from Redis. With a real routing
 * service behind the source this is what keeps opening a day from being a call to a public server every time.
 * <p>
 * A bean of its own for the same reasons as {@link PlaceSearchCache}: the cache works through a proxy and only
 * sees calls from another bean. A failure of the source is an exception and is never stored.
 */
@Component
@RequiredArgsConstructor
public class RouteCache {

    /** Two points closer than about 1 m are the same stop. */
    static final int COORDINATE_SCALE = 5;

    private final MapProvider mapProvider;

    /**
     * What the map source answers for the travel along these points, from Redis when the same points were asked
     * for in the same order in the last 24 hours.
     *
     * @param points the stops of the route, first to last; at least two
     */
    @Cacheable(cacheNames = CacheNames.ROUTE_LEGS, key = "#root.target.keyOf(#points)")
    public List<RouteLeg> legs(List<Coordinate> points) {
        return mapProvider.route(points);
    }

    /** The key of these points for the map source in use. */
    public String keyOf(List<Coordinate> points) {
        return keyOf(mapProvider.provider(), points);
    }

    /**
     * Which questions share one stored answer: the same points in the same order, asked to the same source.
     * The order is part of the question, so moving an activity or giving it another place asks the source again
     * by itself; nothing has to be removed from the cache. The source is part of the question because the
     * straight-line estimate and a routing service give different numbers for the same points: after a switch
     * of {@code app.providers.map}, the answers of the other source must not be served for a day.
     *
     * @return for example {@code OSM|16.06835,108.22428;16.06110,108.22720}
     */
    public static String keyOf(PlaceProvider source, List<Coordinate> points) {
        return points.stream()
                .map(point -> rounded(point.lat()) + "," + rounded(point.lng()))
                .collect(Collectors.joining(";", source + "|", ""));
    }

    private static String rounded(BigDecimal degrees) {
        return degrees.setScale(COORDINATE_SCALE, RoundingMode.HALF_UP).toPlainString();
    }

}

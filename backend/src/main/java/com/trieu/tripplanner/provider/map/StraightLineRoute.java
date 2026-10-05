package com.trieu.tripplanner.provider.map;

import com.trieu.tripplanner.provider.map.dto.Coordinate;
import com.trieu.tripplanner.provider.map.dto.RouteLeg;
import java.util.ArrayList;
import java.util.List;

/**
 * Travel estimated without a road network (design.md 7.2): each leg is the straight line between its two points
 * made {@link #ROAD_FACTOR} times longer, travelled at {@link #SPEED_KM_PER_HOUR}. Used by every map source that
 * has no routing service behind it.
 */
final class StraightLineRoute {

    /** Roads are never straight: a way is taken to be this much longer than the straight line. */
    static final double ROAD_FACTOR = 1.3;

    /** One made-up means of travel, about a motorbike or a car in town. */
    static final double SPEED_KM_PER_HOUR = 30;

    private StraightLineRoute() {
    }

    /**
     * One leg between every two consecutive points, in the order given. The time is computed from the rounded
     * distance, so the two numbers of a leg always agree when checked by hand.
     */
    static List<RouteLeg> legs(List<Coordinate> points) {
        List<RouteLeg> legs = new ArrayList<>();
        for (int i = 1; i < points.size(); i++) {
            long distanceMeters = Math.round(points.get(i - 1).distanceMetersTo(points.get(i)) * ROAD_FACTOR);
            long durationSeconds = Math.round(distanceMeters * 3600 / (SPEED_KM_PER_HOUR * 1000));
            legs.add(new RouteLeg(distanceMeters, durationSeconds));
        }
        return List.copyOf(legs);
    }

}

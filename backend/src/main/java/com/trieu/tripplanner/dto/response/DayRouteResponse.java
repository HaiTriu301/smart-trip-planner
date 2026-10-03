package com.trieu.tripplanner.dto.response;

import java.util.List;

/**
 * The travel of one day of a trip (design.md 10.2 "Quy ước Route").
 *
 * @param legs                 one leg between every two consecutive activities, in the order the day shows them
 * @param totalDistanceMeters  sum of the legs as they are returned, so adding them up by hand gives this number
 * @param totalDurationSeconds sum of the legs; travel only, the time spent at each activity is not counted
 */
public record DayRouteResponse(
        List<RouteLegResponse> legs,
        long totalDistanceMeters,
        long totalDurationSeconds) {
}

package com.trieu.tripplanner.provider.map.dto;

/**
 * Travel between two consecutive points of a route, as a map source estimates it.
 *
 * @param distanceMeters  length of the way, rounded to the metre; 0 when both points are the same
 * @param durationSeconds time to travel it, rounded to the second
 */
public record RouteLeg(long distanceMeters, long durationSeconds) {
}

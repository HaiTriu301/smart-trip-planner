package com.trieu.tripplanner.dto.internal;

/**
 * Number of activities of one trip, all days together. One row per trip of the grouped count query; a trip
 * without activities has no row.
 */
public record TripActivityCount(Long tripId, long count) {
}

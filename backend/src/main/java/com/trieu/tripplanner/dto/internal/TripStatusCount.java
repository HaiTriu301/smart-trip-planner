package com.trieu.tripplanner.dto.internal;

import com.trieu.tripplanner.model.enums.TripStatus;

/** One row of the grouped count: how many trips have this status. Statuses without trips have no row. */
public record TripStatusCount(TripStatus status, long count) {
}

package com.trieu.tripplanner.dto.response;

import com.trieu.tripplanner.model.enums.TripStatus;
import java.util.Map;

/**
 * Trips of the user per status, for the counters on the status chips of the trip list.
 *
 * @param total  all trips matching the keyword, whatever their status (the "Tất cả" chip)
 * @param counts every status in declaration order, 0 when no trip has it
 */
public record TripStatusCountsResponse(long total, Map<TripStatus, Long> counts) {
}

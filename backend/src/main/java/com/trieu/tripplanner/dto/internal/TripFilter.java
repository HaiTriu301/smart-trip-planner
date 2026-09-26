package com.trieu.tripplanner.dto.internal;

import com.trieu.tripplanner.model.enums.TripStatus;
import java.time.LocalDate;

/**
 * Optional filters of GET /api/v1/trips (design.md 10.2 "Quy ước Trip API"). Every field may be null.
 *
 * @param status exact status
 * @param q      case-insensitive substring of title or destination name
 * @param from   keep trips whose date range overlaps [from, to]; either bound may be absent
 * @param to     see {@code from}
 */
public record TripFilter(TripStatus status, String q, LocalDate from, LocalDate to) {
}

package com.trieu.tripplanner.dto.request;

import com.trieu.tripplanner.model.enums.TripStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Body of PATCH /api/v1/trips/{id}/status (design.md 10.2). Any status may follow any other: the user
 * sets it by hand, nothing changes it automatically yet.
 */
public record UpdateTripStatusRequest(
        @NotNull(message = "{validation.trip.status.required}")
        TripStatus status) {
}

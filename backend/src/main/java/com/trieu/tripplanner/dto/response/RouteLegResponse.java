package com.trieu.tripplanner.dto.response;

/**
 * Travel from one activity of a day to the next one (design.md 10.2 "Quy ước Route").
 *
 * @param fromActivityId  the activity the leg leaves from
 * @param toActivityId    the activity the leg arrives at
 * @param distanceMeters  whole metres; the UI turns it into "8,4 km"
 * @param durationSeconds whole seconds; the UI turns it into "25 phút". An estimate, not a measured time
 */
public record RouteLegResponse(
        Long fromActivityId,
        Long toActivityId,
        long distanceMeters,
        long durationSeconds) {
}

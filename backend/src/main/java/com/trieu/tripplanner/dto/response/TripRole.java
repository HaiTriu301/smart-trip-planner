package com.trieu.tripplanner.dto.response;

/**
 * What the caller, or a listed person, is to a trip, as the API reports it (design.md 6.2).
 * OWNER is computed from {@code trips.owner_id}; EDITOR and VIEWER come from {@code trip_members.role}
 * ({@link com.trieu.tripplanner.model.enums.MemberRole}), which has no OWNER value on purpose.
 */
public enum TripRole {
    OWNER,
    EDITOR,
    VIEWER
}

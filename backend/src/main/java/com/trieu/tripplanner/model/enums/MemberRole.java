package com.trieu.tripplanner.model.enums;

/**
 * What a member of a trip may do (design.md 6.2), as stored in {@code trip_members.role}.
 * There is no OWNER here on purpose: the owner has no member row, {@code trips.owner_id} names them.
 * The API speaks in {@code TripRole}, which adds OWNER on top of these two.
 */
public enum MemberRole {
    /** Views and edits the trip, its days and activities; cannot delete the trip nor manage members. */
    EDITOR,
    /** Views only. */
    VIEWER
}

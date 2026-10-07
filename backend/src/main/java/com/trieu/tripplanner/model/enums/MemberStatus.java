package com.trieu.tripplanner.model.enums;

/**
 * Where an invitation stands (design.md 5.2 "trip_members"). Only ACCEPTED grants anything: PENDING and
 * REMOVED members are strangers to the permission check (design.md 6.2).
 */
public enum MemberStatus {
    /** Invitation mail sent, link not used yet. */
    PENDING,
    /** The invitee signed in with the invited email and used the link. */
    ACCEPTED,
    /** The owner removed the member; the row stays so the same email can be invited again (decision 11). */
    REMOVED
}

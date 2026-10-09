package com.trieu.tripplanner.dto.response;

import com.trieu.tripplanner.model.enums.MemberStatus;
import java.time.Instant;

/**
 * One person on a trip: the owner or a member row (design.md 10.2 "Sharing"). Nothing from the account but
 * what a fellow traveller may see; no password hash can get here because there is no field for it.
 *
 * @param memberId   id of the trip_members row; null for the owner, who has no row
 * @param userId     the account, null while an invited email has no account yet
 * @param fullName   from the account, null while there is none
 * @param email      the invited address (lowercase); for the owner, the account email
 * @param role       OWNER, EDITOR or VIEWER
 * @param status     PENDING or ACCEPTED (REMOVED rows are never listed); ACCEPTED for the owner
 * @param invitedAt  when the latest invitation mail was sent; null for the owner
 * @param acceptedAt when the invitation was accepted; null while pending and for the owner
 */
public record MemberResponse(
        Long memberId,
        Long userId,
        String fullName,
        String email,
        String avatarUrl,
        TripRole role,
        MemberStatus status,
        Instant invitedAt,
        Instant acceptedAt) {
}

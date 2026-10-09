package com.trieu.tripplanner.dto.internal;

import com.trieu.tripplanner.model.enums.MemberRole;

/**
 * What one account is to one live trip, read in a single query by {@code TripRepository.findAccess} for the
 * permission evaluator (design.md 6.2 "Triển khai theo giai đoạn", Task 4.1).
 *
 * @param ownerId    owner of the trip, always present: the row exists only when the trip does
 * @param memberRole role of the account's ACCEPTED membership, null when it has none (owner, stranger,
 *                   PENDING or REMOVED member)
 */
public record TripAccess(Long ownerId, MemberRole memberRole) {

    public boolean isOwner(Long userId) {
        return ownerId.equals(userId);
    }

    /** Owner or ACCEPTED member of any role (design.md 6.2 "Xem trip"). */
    public boolean canView(Long userId) {
        return isOwner(userId) || memberRole != null;
    }

    /** Owner or ACCEPTED EDITOR (design.md 6.2 "Sửa thông tin trip", "CRUD activity"). */
    public boolean canEdit(Long userId) {
        return isOwner(userId) || memberRole == MemberRole.EDITOR;
    }

}

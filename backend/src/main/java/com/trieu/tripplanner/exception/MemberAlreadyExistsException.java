package com.trieu.tripplanner.exception;

import com.trieu.tripplanner.common.constant.ErrorCode;

/**
 * Inviting an email that has already accepted an invitation to the trip → 409 MEMBER_ALREADY_EXISTS
 * (design.md rule 14.23). Pending and removed rows are not a conflict: they are re-invited instead.
 * The email itself is not put in the message: it is personal data and the log does not need it.
 */
public class MemberAlreadyExistsException extends AppException {

    public MemberAlreadyExistsException(Long tripId, Long memberId) {
        super(ErrorCode.MEMBER_ALREADY_EXISTS,
                "Trip %d: member %d has already accepted, cannot invite again".formatted(tripId, memberId));
    }

}

package com.trieu.tripplanner.exception;

import com.trieu.tripplanner.common.constant.ErrorCode;

/**
 * A verify-email / reset-password token is unknown, expired, already used or of the wrong type → 400 INVALID_TOKEN.
 * One code for every case (design.md 14.16): the client should simply request a new link, and a detailed answer
 * would let an attacker probe which tokens exist. The precise reason only goes to the log.
 */
public class InvalidTokenException extends AppException {

    public InvalidTokenException(String reason) {
        super(ErrorCode.INVALID_TOKEN, "Verification token rejected: " + reason);
    }

}

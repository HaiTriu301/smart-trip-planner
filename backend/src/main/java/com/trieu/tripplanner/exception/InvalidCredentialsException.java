package com.trieu.tripplanner.exception;

import com.trieu.tripplanner.common.constant.ErrorCode;

/**
 * Login with an unknown email, a soft-deleted account, or a wrong password → 401 INVALID_CREDENTIALS.
 * One exception for all three so the response never reveals which part was wrong.
 */
public class InvalidCredentialsException extends AppException {

    public InvalidCredentialsException() {
        super(ErrorCode.INVALID_CREDENTIALS, "Email or password did not match");
    }

}

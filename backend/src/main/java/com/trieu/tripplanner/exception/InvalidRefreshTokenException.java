package com.trieu.tripplanner.exception;

import com.trieu.tripplanner.common.constant.ErrorCode;

/**
 * The refresh cookie is missing, unknown, expired or already used → 401 UNAUTHORIZED.
 * The client's only sensible move is to log in again, so one code covers all cases; the reason goes to the log.
 */
public class InvalidRefreshTokenException extends AppException {

    public InvalidRefreshTokenException(String reason) {
        super(ErrorCode.UNAUTHORIZED, "Refresh token rejected: " + reason);
    }

}

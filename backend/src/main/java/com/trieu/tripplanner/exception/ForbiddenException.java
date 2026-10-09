package com.trieu.tripplanner.exception;

import com.trieu.tripplanner.common.constant.ErrorCode;

/**
 * The caller is signed in and the resource exists, but this account may not do this → 403 FORBIDDEN, same
 * envelope as a {@code @PreAuthorize} denial. For rules the permission evaluator cannot express because they
 * are about one row rather than the whole trip (e.g. an invitation addressed to another email).
 * The client gets the generic FORBIDDEN text; the reason goes to the log only.
 */
public class ForbiddenException extends AppException {

    public ForbiddenException(String reason) {
        super(ErrorCode.FORBIDDEN, reason);
    }

}

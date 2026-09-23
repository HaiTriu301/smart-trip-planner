package com.trieu.tripplanner.exception;

import com.trieu.tripplanner.common.constant.ErrorCode;

/**
 * The credentials were right but the email has not been verified yet → 403 EMAIL_NOT_VERIFIED.
 */
public class EmailNotVerifiedException extends AppException {

    public EmailNotVerifiedException(Long userId) {
        super(ErrorCode.EMAIL_NOT_VERIFIED, "Account %d has not verified its email".formatted(userId));
    }

}

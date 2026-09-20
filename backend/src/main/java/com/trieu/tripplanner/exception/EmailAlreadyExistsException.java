package com.trieu.tripplanner.exception;

import com.trieu.tripplanner.common.constant.ErrorCode;

/**
 * Registration with an email that already belongs to an account → 409 EMAIL_ALREADY_EXISTS.
 * The email itself is not put in the message: it is personal data and the log does not need it.
 */
public class EmailAlreadyExistsException extends AppException {

    public EmailAlreadyExistsException() {
        super(ErrorCode.EMAIL_ALREADY_EXISTS, "Email is already registered");
    }

    /**
     * Raised when the UNIQUE index rejects the insert because another request registered the same
     * email between our existence check and our insert.
     */
    public EmailAlreadyExistsException(Throwable cause) {
        this();
        initCause(cause);
    }

}

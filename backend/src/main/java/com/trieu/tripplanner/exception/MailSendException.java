package com.trieu.tripplanner.exception;

import com.trieu.tripplanner.common.constant.ErrorCode;

/**
 * The SMTP provider could not deliver a message. Mail is sent asynchronously, so this normally surfaces in
 * AsyncConfig's uncaught-exception log rather than in an HTTP response (design.md 14.17).
 */
public class MailSendException extends AppException {

    public MailSendException(String recipientHint, Throwable cause) {
        super(ErrorCode.PROVIDER_UNAVAILABLE, "Failed to send mail to " + recipientHint);
        initCause(cause);
    }

}

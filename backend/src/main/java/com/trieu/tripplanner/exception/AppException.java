package com.trieu.tripplanner.exception;

import com.trieu.tripplanner.common.constant.ErrorCode;
import java.util.List;
import lombok.Getter;

/**
 * Base class of every exception the application throws on purpose.
 * The exception message is an English detail for logs only; the client receives the
 * localized text of {@link ErrorCode#getMessageKey()} from messages.properties,
 * plus one entry per {@link FieldViolation} in {@code details} when the error concerns specific fields.
 */

@Getter
public abstract class AppException extends  RuntimeException{

    private final ErrorCode errorCode;
    private final List<FieldViolation> details;

    protected  AppException(ErrorCode errorCode, String logMessage){
        this(errorCode, logMessage, List.of());
    }

    protected AppException(ErrorCode errorCode, String logMessage, List<FieldViolation> details) {
        super(logMessage);
        this.errorCode = errorCode;
        this.details = List.copyOf(details);
    }
}

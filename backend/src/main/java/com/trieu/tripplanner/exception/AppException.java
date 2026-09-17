package com.trieu.tripplanner.exception;

import com.trieu.tripplanner.common.constant.ErrorCode;
import lombok.Getter;

/**
 * Base class of every exception the application throws on purpose.
 * The exception message is an English detail for logs only; the client receives the
 * localized text of {@link ErrorCode#getMessageKey()} from messages.properties.
 */

@Getter
public abstract class AppException extends  RuntimeException{

    private final ErrorCode errorCode;

    protected  AppException(ErrorCode errorCode, String logMessage){
        super(logMessage);
        this.errorCode = errorCode;
    }
}

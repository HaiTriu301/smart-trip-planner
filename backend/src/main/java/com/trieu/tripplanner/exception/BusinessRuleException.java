package com.trieu.tripplanner.exception;

import com.trieu.tripplanner.common.constant.ErrorCode;

/**
 * A business rule from design.md 14 was violated, e.g. overlapping activities.
 */
public class BusinessRuleException extends AppException {

    public BusinessRuleException(ErrorCode errorCode, String logMessage){
        super(errorCode, logMessage);
    }
}

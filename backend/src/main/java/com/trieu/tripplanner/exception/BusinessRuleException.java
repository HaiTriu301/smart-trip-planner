package com.trieu.tripplanner.exception;

import com.trieu.tripplanner.common.constant.ErrorCode;
import java.util.List;

/**
 * A business rule from design.md 14 was violated, e.g. overlapping activities.
 */
public class BusinessRuleException extends AppException {

    public BusinessRuleException(ErrorCode errorCode, String logMessage){
        super(errorCode, logMessage);
    }

    public BusinessRuleException(ErrorCode errorCode, String logMessage, List<FieldViolation> details) {
        super(errorCode, logMessage, details);
    }

    /**
     * 400 VALIDATION_ERROR reported on one field, for rules that need the service (other fields, stored state)
     * and therefore cannot be Bean Validation annotations on the DTO (CLAUDE.md rule 13).
     */
    public static BusinessRuleException invalidField(String field, String messageKey, String logMessage,
                                                     Object... args) {
        return new BusinessRuleException(ErrorCode.VALIDATION_ERROR, logMessage,
                List.of(FieldViolation.of(field, messageKey, args)));
    }
}

package com.trieu.tripplanner.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.trieu.tripplanner.common.constant.ErrorCode;

import java.time.Instant;
import java.util.List;

/**
 * Error envelope (design.md 10.1). Only GlobalExceptionHandler creates it.
 * {@code details} is omitted from the JSON when there is no field-level error.
 */
public record ErrorResponse(
        boolean success,
        ErrorCode errorCode,
        String message,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) List<FieldError> details,
        Instant timestamp,
        String path) {

    public static ErrorResponse of(ErrorCode errorCode, String message, String path) {
        return of(errorCode, message, List.of(), path);
    }

    public static ErrorResponse of(ErrorCode errorCode, String message, List<FieldError> details, String path) {
        return new ErrorResponse(false, errorCode, message, List.copyOf(details), Instant.now(), path);
    }

    /**
     * One invalid input: the field (or parameter) name and why it was rejected.
     */
    public record FieldError(String field, String message) {
    }

}

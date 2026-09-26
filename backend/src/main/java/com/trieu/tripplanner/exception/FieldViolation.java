package com.trieu.tripplanner.exception;

import java.util.List;

/**
 * A business-rule failure tied to one request field, e.g. {@code endDate} too far after {@code startDate}.
 * Holds a messages.properties key, not text (CLAUDE.md rule 14): GlobalExceptionHandler resolves it into
 * {@code ErrorResponse.details}, the same place Bean Validation errors go.
 *
 * @param field      request field name as the client sent it
 * @param messageKey key in messages.properties, may use {0}, {1}... placeholders
 * @param args       values for those placeholders
 */
public record FieldViolation(String field, String messageKey, List<Object> args) {

    public FieldViolation {
        args = List.copyOf(args);
    }

    public static FieldViolation of(String field, String messageKey, Object... args) {
        return new FieldViolation(field, messageKey, List.of(args));
    }

}

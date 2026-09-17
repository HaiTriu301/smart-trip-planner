package com.trieu.tripplanner.common;

import java.time.Instant;

/**
 * Success envelope for every REST response (design.md 10.1).
 * Errors never use this type, see {@link ErrorResponse}.
 */
public record ApiResponse<T>(boolean success, T data, String message, Instant timestamp) {

    private static final String DEFAULT_MESSAGE = "OK";

    public static <T> ApiResponse<T> ok(T data){
        return ok(data, DEFAULT_MESSAGE);
    }

    public static <T> ApiResponse<T> ok(T data, String message){
        return new ApiResponse<>(true, data, message, Instant.now());
    }
}

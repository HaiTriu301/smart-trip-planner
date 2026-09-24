package com.trieu.tripplanner.common.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "error.validation"),
    INVALID_TOKEN(HttpStatus.BAD_REQUEST, "error.invalid-token"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "error.unauthorized"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "error.token-expired"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "error.invalid-credentials"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "error.forbidden"),
    EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN, "error.email-not-verified"),
    ACCOUNT_BLOCKED(HttpStatus.FORBIDDEN, "error.account-blocked"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "error.resource-not-found"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "error.method-not-allowed"),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "error.email-already-exists"),
    ACTIVITY_TIME_CONFLICT(HttpStatus.CONFLICT, "error.activity-time-conflict"),
    STALE_VERSION(HttpStatus.CONFLICT, "error.stale-version"),
    QUOTA_EXCEEDED(HttpStatus.PAYMENT_REQUIRED, "error.quota-exceeded"),
    PREMIUM_REQUIRED(HttpStatus.PAYMENT_REQUIRED, "error.premium-required"),
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "error.rate-limit-exceeded"),
    PROVIDER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "error.provider-unavailable"),
    PAYMENT_FAILED(HttpStatus.BAD_GATEWAY, "error.payment-failed"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "error.internal");

    private final HttpStatus httpStatus;
    private final String messageKey;
}

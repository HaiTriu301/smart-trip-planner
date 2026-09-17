package com.trieu.tripplanner.exception;


import com.trieu.tripplanner.common.ErrorResponse;
import com.trieu.tripplanner.common.constant.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Turns every exception into an {@link ErrorResponse} (design.md 10.3).
 * Client mistakes are mapped explicitly; anything else becomes a generic 500.
 */

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private static final String MALFORMED_BODY_KEY = "error.validation.malformed-body";
    private static final String TYPE_MISMATCH_KEY = "error.validation.type-mismatch";

    private static final Comparator<ErrorResponse.FieldError> DETAILS_ORDER = Comparator
            .comparing(ErrorResponse.FieldError::field)
            .thenComparing(ErrorResponse.FieldError::message, Comparator.nullsFirst(Comparator.naturalOrder()));

    private final MessageSource messageSource;

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException ex, HttpServletRequest request) {
        ErrorCode errorCode = ex.getErrorCode();
        if (errorCode.getHttpStatus().is5xxServerError()) {
            log.error("Application error {} on {}: {}", errorCode, request.getRequestURI(), ex.getMessage(), ex);
        }
        else {
            log.warn("Application error {} on {}: {}", errorCode, request.getRequestURI(), ex.getMessage());
        }
        return build(errorCode, message(errorCode.getMessageKey()), List.of(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                      HttpServletRequest request) {
        String objectName = ex.getBindingResult().getObjectName();
        List<ErrorResponse.FieldError> details = ex.getBindingResult().getAllErrors().stream()
                .map(error -> toFieldError(error, objectName))
                .toList();
        return validationError(details, request);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleHandlerMethodValidation(HandlerMethodValidationException ex,
                                                                       HttpServletRequest request) {
        List<ErrorResponse.FieldError> details = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> toFieldError(error, result.getMethodParameter().getParameterName())))
                .toList();
        return validationError(details, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
                                                                   HttpServletRequest request) {
        List<ErrorResponse.FieldError> details = ex.getConstraintViolations().stream()
                .map(violation -> new ErrorResponse.FieldError(
                        leafName(violation.getPropertyPath()), violation.getMessage()))
                .toList();
        return validationError(details, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                      HttpServletRequest request) {
        // Log only the cause type: parser messages can echo raw body content such as passwords
        log.warn("Unreadable request body on {}: {}", request.getRequestURI(),
                ex.getMostSpecificCause().getClass().getSimpleName());
        return build(ErrorCode.VALIDATION_ERROR, message(MALFORMED_BODY_KEY), List.of(), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                                          HttpServletRequest request) {
        return build(ErrorCode.VALIDATION_ERROR, message(TYPE_MISMATCH_KEY, ex.getName()), List.of(), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex,
                                                               HttpServletRequest request) {
        ErrorCode errorCode = ErrorCode.RESOURCE_NOT_FOUND;
        return build(errorCode, message(errorCode.getMessageKey()), List.of(), request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                  HttpServletRequest request) {
        ErrorCode errorCode = ErrorCode.METHOD_NOT_ALLOWED;
        // ex.getHeaders() carries the Allow header that a 405 response must include
        return ResponseEntity.status(errorCode.getHttpStatus())
                .headers(ex.getHeaders())
                .body(ErrorResponse.of(errorCode, message(errorCode.getMessageKey()), request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        ErrorCode errorCode = ErrorCode.INTERNAL_ERROR;
        return build(errorCode, message(errorCode.getMessageKey()), List.of(), request);
    }

    private ResponseEntity<ErrorResponse> validationError(List<ErrorResponse.FieldError> details,
                                                          HttpServletRequest request) {
        // Validators report errors in no particular order; sort so clients always get a stable list
        List<ErrorResponse.FieldError> sorted = details.stream().sorted(DETAILS_ORDER).toList();
        ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;
        return build(errorCode, message(errorCode.getMessageKey()), sorted, request);
    }

    private ResponseEntity<ErrorResponse> build(ErrorCode errorCode, String message,
                                                List<ErrorResponse.FieldError> details, HttpServletRequest request) {
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ErrorResponse.of(errorCode, message, details, request.getRequestURI()));
    }

    private String message(String key, Object... args) {
        return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
    }

    private static ErrorResponse.FieldError toFieldError(MessageSourceResolvable error, String fallbackField) {
        // Spring's FieldError is written fully qualified to avoid clashing with ErrorResponse.FieldError
        String field = (error instanceof org.springframework.validation.FieldError fieldError)
                ? fieldError.getField()
                : fallbackField;
        return new ErrorResponse.FieldError(field, error.getDefaultMessage());
    }

    /**
     * "createTrip.request.title" -> "title"; falls back to the full path when the leaf has no name.
     */
    private static String leafName(Path path) {
        String name = null;
        for (Path.Node node : path) {
            name = node.getName();
        }
        return (name != null) ? name : path.toString();
    }
}

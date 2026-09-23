package com.trieu.tripplanner.security;

import com.trieu.tripplanner.common.constant.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import tools.jackson.databind.ObjectMapper;

/**
 * Called when an unauthenticated request hits a protected URL. Replaces Spring Security's default
 * (empty body + WWW-Authenticate header) with a 401 {@code ErrorResponse}.
 * If {@link JwtAuthenticationFilter} rejected a bearer token, its reason wins: an expired token answers
 * TOKEN_EXPIRED so the frontend knows to call /auth/refresh instead of sending the user to the login page.
 * Registered by SecurityConfig via @Import, not component scanning.
 */
@Slf4j
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;
    private final MessageSource messageSource;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        ErrorCode errorCode = request.getAttribute(JwtAuthenticationFilter.ERROR_ATTRIBUTE) instanceof ErrorCode marker
                ? marker
                : ErrorCode.UNAUTHORIZED;
        // Only the exception type: its message may echo credentials
        log.debug("Unauthenticated request {} {} → {}: {}", request.getMethod(), request.getRequestURI(),
                errorCode, exception.getClass().getSimpleName());
        SecurityErrorResponses.write(request, response, errorCode, objectMapper, messageSource);
    }

}

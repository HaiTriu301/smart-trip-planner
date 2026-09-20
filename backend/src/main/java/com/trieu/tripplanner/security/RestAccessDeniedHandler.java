package com.trieu.tripplanner.security;

import com.trieu.tripplanner.common.constant.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.ObjectMapper;

/**
 * Called when an authenticated user is denied at the filter level (URL rules in SecurityConfig).
 * Denials from @PreAuthorize inside controllers go to GlobalExceptionHandler instead; both produce the same 403 body.
 */
@Slf4j
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final MessageSource messageSource;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        log.warn("Access denied at filter level on {} {}", request.getMethod(), request.getRequestURI());
        SecurityErrorResponses.write(request, response, ErrorCode.FORBIDDEN, objectMapper, messageSource);
    }

}

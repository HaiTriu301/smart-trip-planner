package com.trieu.tripplanner.security;

import com.trieu.tripplanner.common.ErrorResponse;
import com.trieu.tripplanner.common.constant.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;

/**
 * Writes an {@link ErrorResponse} from inside the security filter chain, where GlobalExceptionHandler
 * cannot help because the request never reached a controller. Keeps 401/403 in the same envelope as every other error.
 */
final class SecurityErrorResponses {

    private SecurityErrorResponses() {
    }

    static void write(HttpServletRequest request, HttpServletResponse response, ErrorCode errorCode,
                      ObjectMapper objectMapper, MessageSource messageSource) throws IOException {
        String message = messageSource.getMessage(errorCode.getMessageKey(), null, LocaleContextHolder.getLocale());
        ErrorResponse body = ErrorResponse.of(errorCode, message, request.getRequestURI());

        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(body));
        response.getWriter().flush();
    }

}

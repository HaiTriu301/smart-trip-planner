package com.trieu.tripplanner.security.ratelimit;

import com.trieu.tripplanner.common.constant.ErrorCode;
import com.trieu.tripplanner.config.properties.RateLimitProperties;
import com.trieu.tripplanner.security.SecurityErrorResponses;
import com.trieu.tripplanner.security.ratelimit.RateLimiter.Decision;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * Stops a caller who went over the limit of an endpoint before the request reaches a controller (design.md 8.2):
 * a rejected call never touches the database and never sends a mail.
 * <p>
 * Sits in the Spring Security chain right after JwtAuthenticationFilter. Created by SecurityConfig, not a
 * {@code @Component}: a Filter bean would also be registered once more in the plain servlet chain, and every
 * {@code @WebMvcTest} would need Redis.
 * <p>
 * The caller's address is {@link HttpServletRequest#getRemoteAddr()}, never a header the caller can write
 * (WORKFLOW.md Task 4.7, decision 4).
 */
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    static final String REMAINING_HEADER = "X-RateLimit-Remaining";
    static final String RESET_HEADER = "X-RateLimit-Reset";

    private final RateLimiter rateLimiter;
    private final ObjectMapper objectMapper;
    private final MessageSource messageSource;
    private final List<Rule> rules;

    public RateLimitFilter(RateLimiter rateLimiter, RateLimitProperties properties, ObjectMapper objectMapper,
                           MessageSource messageSource) {
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
        this.messageSource = messageSource;
        this.rules = List.of(
                new Rule("register", PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/v1/auth/register"),
                        properties.register()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Optional<Rule> rule = rules.stream().filter(candidate -> candidate.matcher().matches(request)).findFirst();
        if (rule.isPresent()) {
            String address = request.getRemoteAddr();
            Decision decision = rateLimiter.tryConsume(rule.get().name() + ":" + address, rule.get().limit());
            if (!decision.allowed()) {
                log.info("Rate limit {} reached from {}, next call in {}s", rule.get().name(), address,
                        decision.retryAfterSeconds());
                reject(request, response, decision);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    /** 429 in the usual ErrorResponse envelope, with the wait in seconds in both headers of design.md 8.2. */
    private void reject(HttpServletRequest request, HttpServletResponse response, Decision decision)
            throws IOException {
        String wait = String.valueOf(decision.retryAfterSeconds());
        response.setHeader(REMAINING_HEADER, "0");
        response.setHeader(RESET_HEADER, wait);
        response.setHeader(HttpHeaders.RETRY_AFTER, wait);
        SecurityErrorResponses.write(request, response, ErrorCode.RATE_LIMIT_EXCEEDED, objectMapper, messageSource);
    }

    /**
     * One limit on one endpoint.
     *
     * @param name    first part of the bucket key, also used in the log
     * @param matcher which requests it counts
     * @param limit   how many calls in how long
     */
    private record Rule(String name, RequestMatcher matcher, RateLimit limit) {
    }

}

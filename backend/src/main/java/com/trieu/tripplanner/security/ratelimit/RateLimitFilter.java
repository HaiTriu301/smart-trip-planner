package com.trieu.tripplanner.security.ratelimit;

import com.trieu.tripplanner.common.constant.ErrorCode;
import com.trieu.tripplanner.common.util.SecureTokens;
import com.trieu.tripplanner.config.properties.RateLimitProperties;
import com.trieu.tripplanner.security.SecurityErrorResponses;
import com.trieu.tripplanner.security.ratelimit.RateLimiter.Decision;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
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

    /**
     * The most of a login body the filter reads. A real one (email up to 255 characters, password up to 72) is far
     * smaller; a longer body is passed on to the controller without being read here.
     */
    static final int MAX_LOGIN_BODY_BYTES = 8 * 1024;

    /** Stands for "no email found in the body" in a key; a SHA-256 in hex can never be this. */
    private static final String NO_EMAIL = "-";

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
                new Rule("login", post("/api/v1/auth/login"), properties.login(), CountedBy.ADDRESS_AND_EMAIL),
                new Rule("register", post("/api/v1/auth/register"), properties.register(), CountedBy.ADDRESS),
                // The two endpoints that send mail: each mail costs a share of the daily quota of the mail service
                new Rule("resend-verification", post("/api/v1/auth/resend-verification"),
                        properties.resendVerification(), CountedBy.ADDRESS),
                new Rule("forgot-password", post("/api/v1/auth/forgot-password"), properties.forgotPassword(),
                        CountedBy.ADDRESS));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Optional<Rule> match = rules.stream().filter(rule -> rule.matcher().matches(request)).findFirst();
        if (match.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        Rule rule = match.get();
        String address = request.getRemoteAddr();
        // The body can be read only once: from here on the chain gets the wrapper, which replays it
        HttpServletRequest passedOn = request;
        String caller = address;
        if (rule.countedBy() == CountedBy.ADDRESS_AND_EMAIL) {
            CachedBodyRequest cached = new CachedBodyRequest(request, MAX_LOGIN_BODY_BYTES);
            passedOn = cached;
            caller = address + ":" + emailPartOf(cached);
        }

        Decision decision = rateLimiter.tryConsume(rule.name() + ":" + caller, rule.limit());
        if (!decision.allowed()) {
            // The address only: the key may hold the hash of an email
            log.info("Rate limit {} reached from {}, next call in {}s", rule.name(), address,
                    decision.retryAfterSeconds());
            reject(request, response, decision);
            return;
        }
        chain.doFilter(passedOn, response);
    }

    /**
     * The email of a login body, in the form AuthService looks it up (trimmed, lower case), as a SHA-256: the
     * email never sits in Redis as plain text. A body without a readable email (not JSON, no email, too long)
     * gives {@value #NO_EMAIL}, so all such calls from one address share one bucket: a strange body is still
     * counted, the controller answers it 400 as before.
     */
    private String emailPartOf(CachedBodyRequest request) {
        Optional<byte[]> body = request.body();
        if (body.isEmpty()) {
            return NO_EMAIL;
        }
        try {
            JsonNode email = objectMapper.readTree(body.get()).path("email");
            if (!email.isString() || email.stringValue().isBlank()) {
                return NO_EMAIL;
            }
            return SecureTokens.sha256Hex(email.stringValue().trim().toLowerCase(Locale.ROOT));
        }
        catch (JacksonException ex) {
            return NO_EMAIL;
        }
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

    private static RequestMatcher post(String path) {
        return PathPatternRequestMatcher.pathPattern(HttpMethod.POST, path);
    }

    /** Who one bucket belongs to. */
    private enum CountedBy {
        /** One bucket per address. */
        ADDRESS,
        /** One bucket per address and email of the body, so guessing at one account does not block the others. */
        ADDRESS_AND_EMAIL
    }

    /**
     * One limit on one endpoint.
     *
     * @param name      first part of the bucket key, also used in the log
     * @param matcher   which requests it counts
     * @param limit     how many calls in how long
     * @param countedBy who one bucket belongs to
     */
    private record Rule(String name, RequestMatcher matcher, RateLimit limit, CountedBy countedBy) {
    }

}

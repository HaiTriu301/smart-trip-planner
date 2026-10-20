package com.trieu.tripplanner.security.ratelimit;

import com.trieu.tripplanner.common.constant.ErrorCode;
import com.trieu.tripplanner.common.util.SecureTokens;
import com.trieu.tripplanner.config.properties.RateLimitProperties;
import com.trieu.tripplanner.security.CustomUserDetails;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Stops a caller who went over a limit before the request reaches a controller (design.md 8.2): a rejected call
 * never touches the database and never sends a mail. Some endpoints have a limit of their own ({@link Rule}); every
 * call under {@code /api/} also counts against a global limit, per user once signed in, per address before.
 * <p>
 * Sits in the Spring Security chain right after JwtAuthenticationFilter, so the signed-in user is known when a
 * rule counts per user. Created by SecurityConfig, not a
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

    /** What the global limits count (WORKFLOW.md Task 4.7, decision 12). */
    private static final RequestMatcher API = PathPatternRequestMatcher.pathPattern("/api/**");

    private final RateLimiter rateLimiter;
    private final ObjectMapper objectMapper;
    private final MessageSource messageSource;
    private final List<Rule> rules;
    private final RateLimit globalUser;
    private final RateLimit globalGuest;
    private final SecurityContextHolderStrategy contextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();

    public RateLimitFilter(RateLimiter rateLimiter, RateLimitProperties properties, ObjectMapper objectMapper,
                           MessageSource messageSource) {
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
        this.messageSource = messageSource;
        this.globalUser = properties.globalUser();
        this.globalGuest = properties.globalGuest();
        this.rules = List.of(
                new Rule("login", post("/api/v1/auth/login"), properties.login(), CountedBy.ADDRESS_AND_EMAIL),
                new Rule("register", post("/api/v1/auth/register"), properties.register(), CountedBy.ADDRESS),
                // The two endpoints that send mail: each mail costs a share of the daily quota of the mail service
                new Rule("resend-verification", post("/api/v1/auth/resend-verification"),
                        properties.resendVerification(), CountedBy.ADDRESS),
                new Rule("forgot-password", post("/api/v1/auth/forgot-password"), properties.forgotPassword(),
                        CountedBy.ADDRESS),
                // Every token counts, unknown ones too: guessing tokens costs the same allowance as viewing pages
                new Rule("public-trip", PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/api/v1/public/trips/*"),
                        properties.publicTrip(), CountedBy.ADDRESS),
                // Counted before the cache: a search answered from Redis takes a call all the same (design.md 8.2)
                new Rule("place-search", PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/api/v1/places/search"),
                        properties.placeSearch(), CountedBy.USER));
    }

    /**
     * The limit of the endpoint first, then the global one (design.md 8.2). A call let through takes from both; a
     * call the endpoint limit turns away never reaches the global count. One the global limit turns away has already
     * spent its call of the endpoint limit: Bucket4j cannot take from two buckets in one step.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        HttpServletRequest passedOn = request;
        Optional<Rule> match = rules.stream().filter(rule -> rule.matcher().matches(request)).findFirst();
        if (match.isPresent()) {
            Rule rule = match.get();
            if (rule.countedBy() == CountedBy.ADDRESS_AND_EMAIL) {
                // The body can be read only once: from here on the chain gets the wrapper, which replays it
                passedOn = new CachedBodyRequest(request, MAX_LOGIN_BODY_BYTES);
            }
            // No caller: a rule per user on a call without sign-in. The authorization step answers it 401
            Optional<String> caller = callerOf(rule.countedBy(), passedOn);
            if (caller.isPresent() && !allowed(rule.name(), caller.get(), rule.limit(), request, response)) {
                return;
            }
        }

        // Only the API: the health check (called all the time by Docker and the proxy) and Swagger are not counted
        if (API.matches(request) && !allowedByTheGlobalLimit(request, response)) {
            return;
        }
        chain.doFilter(passedOn, response);
    }

    private Optional<String> callerOf(CountedBy countedBy, HttpServletRequest request) {
        String address = request.getRemoteAddr();
        return switch (countedBy) {
            case ADDRESS -> Optional.of(address);
            case ADDRESS_AND_EMAIL -> Optional.of(address + ":" + emailPartOf((CachedBodyRequest) request));
            case USER -> signedInUserId().map(id -> "u" + id);
        };
    }

    /** A signed-in user is counted wherever they call from, a guest per address. */
    private boolean allowedByTheGlobalLimit(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        Optional<Long> userId = signedInUserId();
        return userId.isPresent()
                ? allowed("global-user", "u" + userId.get(), globalUser, request, response)
                : allowed("global-guest", request.getRemoteAddr(), globalGuest, request, response);
    }

    /** Takes one call from the bucket {@code name:caller}; when it is empty, answers 429 and returns false. */
    private boolean allowed(String name, String caller, RateLimit limit, HttpServletRequest request,
                            HttpServletResponse response) throws IOException {
        Decision decision = rateLimiter.tryConsume(name + ":" + caller, limit);
        if (decision.allowed()) {
            return true;
        }
        // The address only, never the key: the key may hold the hash of an email
        log.info("Rate limit {} reached from {}, next call in {}s", name, request.getRemoteAddr(),
                decision.retryAfterSeconds());
        reject(request, response, decision);
        return false;
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

    /** The user JwtAuthenticationFilter signed in for this request, if any. */
    private Optional<Long> signedInUserId() {
        Authentication authentication = contextHolderStrategy.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof CustomUserDetails user
                ? Optional.of(user.getId())
                : Optional.empty();
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
        ADDRESS_AND_EMAIL,
        /** One bucket per signed-in user, wherever they call from: people behind one address do not share it. */
        USER
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

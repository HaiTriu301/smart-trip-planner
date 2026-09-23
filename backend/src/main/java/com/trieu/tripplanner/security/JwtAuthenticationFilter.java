package com.trieu.tripplanner.security;

import com.trieu.tripplanner.common.constant.ErrorCode;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Runs once per request before authorization. If a {@code Authorization: Bearer <jwt>} header is present and the
 * token verifies, the user becomes the authenticated principal for this request.
 * <p>
 * On a bad token the filter does <em>not</em> fail the request itself: it records why in a request attribute and
 * lets the chain continue. The authorization filter then rejects protected URLs and
 * {@link RestAuthenticationEntryPoint} reads the attribute to answer TOKEN_EXPIRED vs UNAUTHORIZED.
 * Public URLs keep working even with a stale token in the header.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** Request attribute holding the {@link ErrorCode} explaining why the bearer token was rejected. */
    public static final String ERROR_ATTRIBUTE = JwtAuthenticationFilter.class.getName() + ".error";

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final SecurityContextHolderStrategy contextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        try {
            JwtTokenProvider.JwtClaims claims = jwtTokenProvider.parse(token);
            CustomUserDetails principal = CustomUserDetails.fromClaims(claims);

            UsernamePasswordAuthenticationToken authentication =
                    UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContext context = contextHolderStrategy.createEmptyContext();
            context.setAuthentication(authentication);
            contextHolderStrategy.setContext(context);
        }
        catch (ExpiredJwtException ex) {
            request.setAttribute(ERROR_ATTRIBUTE, ErrorCode.TOKEN_EXPIRED);
        }
        catch (JwtException | IllegalArgumentException ex) {
            // Never log the token itself; the class name is enough to tell signature from format problems
            log.debug("Rejected bearer token on {} {}: {}", request.getMethod(), request.getRequestURI(),
                    ex.getClass().getSimpleName());
            request.setAttribute(ERROR_ATTRIBUTE, ErrorCode.UNAUTHORIZED);
        }

        chain.doFilter(request, response);
    }

}

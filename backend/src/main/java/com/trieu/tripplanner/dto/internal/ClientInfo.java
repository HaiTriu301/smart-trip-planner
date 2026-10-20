package com.trieu.tripplanner.dto.internal;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

/**
 * Where a login came from, stored with the refresh token for the "signed-in devices" screen.
 * Built in the controller so services never touch HttpServletRequest (design.md 4.1).
 *
 * @param userAgent raw User-Agent header, may be null
 * @param ipAddress the caller's address as the servlet container sees it ({@code getRemoteAddr()})
 */
public record ClientInfo(String userAgent, String ipAddress) {

    /**
     * Never reads X-Forwarded-For itself: its first entry is whatever the caller typed, so trusting it would let
     * anyone pick the address stored with a session or used as a rate limit key (design.md 6.3). Behind the
     * production proxy, nginx overwrites the header with the real address and
     * {@code server.forward-headers-strategy: native} makes the container put it in {@code getRemoteAddr()}.
     */
    public static ClientInfo from(HttpServletRequest request) {
        return new ClientInfo(request.getHeader(HttpHeaders.USER_AGENT), request.getRemoteAddr());
    }

    public static ClientInfo unknown() {
        return new ClientInfo(null, null);
    }

}

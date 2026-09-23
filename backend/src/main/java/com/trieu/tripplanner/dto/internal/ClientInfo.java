package com.trieu.tripplanner.dto.internal;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

/**
 * Where a login came from, stored with the refresh token for the "signed-in devices" screen.
 * Built in the controller so services never touch HttpServletRequest (design.md 4.1).
 *
 * @param userAgent raw User-Agent header, may be null
 * @param ipAddress first X-Forwarded-For entry when behind nginx, else the socket address
 */
public record ClientInfo(String userAgent, String ipAddress) {

    private static final String FORWARDED_FOR = "X-Forwarded-For";

    public static ClientInfo from(HttpServletRequest request) {
        String forwarded = request.getHeader(FORWARDED_FOR);
        String ip = (forwarded != null && !forwarded.isBlank())
                ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
        return new ClientInfo(request.getHeader(HttpHeaders.USER_AGENT), ip);
    }

    public static ClientInfo unknown() {
        return new ClientInfo(null, null);
    }

}

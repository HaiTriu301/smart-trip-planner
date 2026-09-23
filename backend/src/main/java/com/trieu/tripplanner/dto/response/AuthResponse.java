package com.trieu.tripplanner.dto.response;

/**
 * Body of login and refresh (design.md 6.1). The refresh token is NOT here: it goes in the httpOnly cookie.
 *
 * @param accessToken JWT for the Authorization header
 * @param tokenType   always "Bearer"
 * @param expiresIn   seconds until the access token expires; the frontend schedules refresh from this
 * @param user        the signed-in account
 */
public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {

    public static final String BEARER = "Bearer";

}

package com.trieu.tripplanner.dto.internal;

import com.trieu.tripplanner.dto.response.AuthResponse;

/**
 * What AuthService hands back after login/refresh: the JSON body plus the raw refresh token the controller
 * must place in the cookie. Never serialized as a whole.
 */
public record AuthTokens(AuthResponse response, String refreshToken) {
}

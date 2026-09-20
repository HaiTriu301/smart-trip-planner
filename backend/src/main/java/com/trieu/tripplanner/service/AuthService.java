package com.trieu.tripplanner.service;

import com.trieu.tripplanner.dto.request.RegisterRequest;
import com.trieu.tripplanner.dto.response.UserResponse;

/**
 * Account lifecycle: registration now, login/refresh/verify/reset in Tasks 1.3–1.4.
 */
public interface AuthService {

    /**
     * Creates a FREE, unverified account.
     *
     * @throws com.trieu.tripplanner.exception.EmailAlreadyExistsException when the email is taken (409)
     */
    UserResponse register(RegisterRequest request);

}

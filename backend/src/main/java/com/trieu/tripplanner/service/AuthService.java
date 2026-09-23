package com.trieu.tripplanner.service;

import com.trieu.tripplanner.dto.internal.AuthTokens;
import com.trieu.tripplanner.dto.internal.ClientInfo;
import com.trieu.tripplanner.dto.request.LoginRequest;
import com.trieu.tripplanner.dto.request.RegisterRequest;
import com.trieu.tripplanner.dto.response.UserResponse;

/**
 * Account lifecycle: registration and login now; refresh/logout in the next step, verify/reset in Task 1.4.
 */
public interface AuthService {

    /**
     * Creates a FREE, unverified account.
     *
     * @throws com.trieu.tripplanner.exception.EmailAlreadyExistsException when the email is taken (409)
     */
    UserResponse register(RegisterRequest request);

    /**
     * Password login. Checks run in this order (design.md 6.1): credentials → BLOCKED → email verified.
     *
     * @throws com.trieu.tripplanner.exception.InvalidCredentialsException unknown email or wrong password (401)
     * @throws com.trieu.tripplanner.exception.AccountBlockedException     status BLOCKED (403)
     * @throws com.trieu.tripplanner.exception.EmailNotVerifiedException   email not verified yet (403)
     */
    AuthTokens login(LoginRequest request, ClientInfo client);

}

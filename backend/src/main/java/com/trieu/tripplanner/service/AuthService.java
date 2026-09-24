package com.trieu.tripplanner.service;

import com.trieu.tripplanner.dto.internal.AuthTokens;
import com.trieu.tripplanner.dto.internal.ClientInfo;
import com.trieu.tripplanner.dto.request.LoginRequest;
import com.trieu.tripplanner.dto.request.RegisterRequest;
import com.trieu.tripplanner.dto.request.ResetPasswordRequest;
import com.trieu.tripplanner.dto.response.UserResponse;

/**
 * Account lifecycle: registration, login, token refresh, logout. Verify/reset flows arrive in Task 1.4.
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

    /**
     * Rotation: the presented refresh token is revoked and a new pair is issued. Presenting an already
     * revoked token is treated as theft and kills every session of that user.
     *
     * @param rawRefreshToken cookie value, may be null when the cookie is absent
     * @throws com.trieu.tripplanner.exception.InvalidRefreshTokenException missing, unknown, expired or reused (401)
     * @throws com.trieu.tripplanner.exception.AccountBlockedException      user was blocked meanwhile (403)
     */
    AuthTokens refresh(String rawRefreshToken, ClientInfo client);

    /**
     * Revokes the presented refresh token. Idempotent: an unknown or absent token is silently ignored.
     */
    void logout(String rawRefreshToken);

    /**
     * Consumes an EMAIL_VERIFY token from the mailed link and marks the account verified.
     *
     * @throws com.trieu.tripplanner.exception.InvalidTokenException unknown, expired, used or wrong-type token (400)
     */
    void verifyEmail(String rawToken);

    /**
     * Sends a fresh verification mail when the email belongs to an ACTIVE, still unverified account.
     * Never throws for unknown / verified / blocked emails (design.md 14.15): the caller always answers 200.
     */
    void resendVerification(String email);

    /**
     * Mails a PASSWORD_RESET link when the email belongs to a verified, ACTIVE account (design.md 14.12).
     * Never throws for other emails (14.15): the caller always answers 200.
     */
    void forgotPassword(String email);

    /**
     * Consumes a PASSWORD_RESET token, stores the new BCrypt hash and revokes every refresh token of the user
     * so other devices must sign in again (design.md 14.16).
     *
     * @throws com.trieu.tripplanner.exception.InvalidTokenException unknown, expired, used or wrong-type token (400)
     */
    void resetPassword(ResetPasswordRequest request);

}

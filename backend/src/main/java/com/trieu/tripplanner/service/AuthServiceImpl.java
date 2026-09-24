package com.trieu.tripplanner.service;

import com.trieu.tripplanner.dto.internal.AuthTokens;
import com.trieu.tripplanner.dto.internal.ClientInfo;
import com.trieu.tripplanner.dto.request.LoginRequest;
import com.trieu.tripplanner.dto.request.RegisterRequest;
import com.trieu.tripplanner.dto.request.ResetPasswordRequest;
import com.trieu.tripplanner.dto.response.AuthResponse;
import com.trieu.tripplanner.dto.response.UserResponse;
import com.trieu.tripplanner.exception.AccountBlockedException;
import com.trieu.tripplanner.exception.EmailAlreadyExistsException;
import com.trieu.tripplanner.exception.EmailNotVerifiedException;
import com.trieu.tripplanner.exception.InvalidCredentialsException;
import com.trieu.tripplanner.exception.InvalidRefreshTokenException;
import com.trieu.tripplanner.mapper.UserMapper;
import com.trieu.tripplanner.model.RefreshToken;
import com.trieu.tripplanner.model.User;
import com.trieu.tripplanner.model.VerificationToken;
import com.trieu.tripplanner.model.enums.UserStatus;
import com.trieu.tripplanner.model.enums.VerificationTokenType;
import com.trieu.tripplanner.repository.UserRepository;
import com.trieu.tripplanner.security.CustomUserDetails;
import com.trieu.tripplanner.security.JwtTokenProvider;
import java.time.Instant;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final VerificationTokenService verificationTokenService;
    private final MailService mailService;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        // Same normalization the entity applies on persist, so the duplicate check sees the stored form
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException();
        }

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName().trim())
                .build();               // role USER, plan FREE, status ACTIVE, emailVerified false via @Builder.Default

        try {
            user = userRepository.saveAndFlush(user);
        }
        catch (DataIntegrityViolationException ex) {
            // Two requests registered the same email at once: the UNIQUE index caught the second one
            throw new EmailAlreadyExistsException(ex);
        }

        sendVerificationMail(user);
        log.info("Registered new user id={}", user.getId());
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public AuthTokens login(LoginRequest request, ClientInfo client) {
        Authentication authentication;
        try {
            // DaoAuthenticationProvider: load by email, compare BCrypt. Unknown email is also BadCredentials.
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(normalizeEmail(request.email()), request.password()));
        }
        catch (BadCredentialsException ex) {
            throw new InvalidCredentialsException();
        }

        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        User user = userRepository.findById(principal.getId()).orElseThrow(InvalidCredentialsException::new);

        // Only after the password matched, so a wrong password never reveals the account state
        assertCanUseAccount(user);
        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException(user.getId());
        }

        log.info("User id={} logged in", user.getId());
        return issueTokens(user, client);
    }

    /**
     * noRollbackFor: when we reject the request AFTER revoking sessions (theft, expiry, blocked account),
     * those revocations must still be committed. Without it the exception would roll the UPDATE back and
     * the stolen token's siblings would stay alive.
     */
    @Override
    @Transactional(noRollbackFor = {InvalidRefreshTokenException.class, AccountBlockedException.class})
    public AuthTokens refresh(String rawRefreshToken, ClientInfo client) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new InvalidRefreshTokenException("cookie missing");
        }

        RefreshToken stored = refreshTokenService.findByRawToken(rawRefreshToken)
                .orElseThrow(() -> new InvalidRefreshTokenException("unknown token"));
        User user = stored.getUser();
        Instant now = Instant.now();

        if (stored.isRevoked()) {
            // A token we already rotated away is being presented again: someone else holds a copy.
            // We cannot tell which party is the attacker, so every session of this user is terminated.
            int revoked = refreshTokenService.revokeAll(user.getId());
            log.warn("Reuse of revoked refresh token id={} for user id={}; revoked {} live session(s)",
                    stored.getId(), user.getId(), revoked);
            throw new InvalidRefreshTokenException("token already used");
        }
        if (stored.isExpired(now)) {
            refreshTokenService.revoke(stored);
            throw new InvalidRefreshTokenException("token expired");
        }
        if (user.getStatus() == UserStatus.BLOCKED) {
            refreshTokenService.revokeAll(user.getId());
            throw new AccountBlockedException(user.getId());
        }

        // Rotation: the old session ends, a brand-new one starts
        refreshTokenService.revoke(stored);
        return issueTokens(user, client);
    }

    @Override
    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokenService.findByRawToken(rawRefreshToken).ifPresent(token -> {
            refreshTokenService.revoke(token);
            log.info("User id={} logged out session id={}", token.getUser().getId(), token.getId());
        });
    }

    @Override
    @Transactional
    public void verifyEmail(String rawToken) {
        VerificationToken token = verificationTokenService.consume(rawToken, VerificationTokenType.EMAIL_VERIFY);
        User user = token.getUser();
        // Managed entity inside the transaction: the UPDATE runs at commit
        user.setEmailVerified(true);
        log.info("User id={} verified email", user.getId());
    }

    @Override
    @Transactional
    public void resendVerification(String email) {
        userRepository.findByEmail(normalizeEmail(email)).ifPresentOrElse(user -> {
            if (user.isEmailVerified() || user.getStatus() == UserStatus.BLOCKED) {
                // Nothing to do, but the caller still answers 200 (design.md 14.15)
                log.debug("Resend verification skipped for user id={}: verified={} status={}",
                        user.getId(), user.isEmailVerified(), user.getStatus());
                return;
            }
            sendVerificationMail(user);
            log.info("Re-sent verification mail to user id={}", user.getId());
        }, () -> log.debug("Resend verification requested for an unknown email"));
    }

    @Override
    @Transactional
    public void forgotPassword(String email) {
        userRepository.findByEmail(normalizeEmail(email)).ifPresentOrElse(user -> {
            // Rule 14.12: only verified accounts receive mail (other than the verification mail itself)
            if (!user.isEmailVerified() || user.getStatus() == UserStatus.BLOCKED) {
                log.debug("Password reset skipped for user id={}: verified={} status={}",
                        user.getId(), user.isEmailVerified(), user.getStatus());
                return;
            }
            String rawToken = verificationTokenService.issue(user, VerificationTokenType.PASSWORD_RESET);
            mailService.sendPasswordResetMail(user.getEmail(), user.getFullName(), rawToken);
            log.info("Sent password reset mail to user id={}", user.getId());
        }, () -> log.debug("Password reset requested for an unknown email"));
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        VerificationToken token = verificationTokenService.consume(request.token(), VerificationTokenType.PASSWORD_RESET);
        User user = token.getUser();

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        // Whoever held the old password (or a stolen session) is signed out everywhere (design.md 14.16)
        int revoked = refreshTokenService.revokeAll(user.getId());
        log.info("User id={} reset password; revoked {} live session(s)", user.getId(), revoked);
    }

    private void sendVerificationMail(User user) {
        // Issue inside the transaction (hash persisted), send outside it (@Async on the MailService bean)
        String rawToken = verificationTokenService.issue(user, VerificationTokenType.EMAIL_VERIFY);
        mailService.sendVerificationMail(user.getEmail(), user.getFullName(), rawToken);
    }

    private AuthTokens issueTokens(User user, ClientInfo client) {
        JwtTokenProvider.AccessToken access = jwtTokenProvider.generateAccessToken(user);
        RefreshTokenService.IssuedRefreshToken refresh = refreshTokenService.issue(user, client);

        AuthResponse response = new AuthResponse(access.token(), AuthResponse.BEARER, access.expiresInSeconds(),
                userMapper.toResponse(user));
        return new AuthTokens(response, refresh.rawToken());
    }

    private static void assertCanUseAccount(User user) {
        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new AccountBlockedException(user.getId());
        }
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

}

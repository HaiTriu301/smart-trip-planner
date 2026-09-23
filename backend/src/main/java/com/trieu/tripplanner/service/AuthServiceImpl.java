package com.trieu.tripplanner.service;

import com.trieu.tripplanner.dto.internal.AuthTokens;
import com.trieu.tripplanner.dto.internal.ClientInfo;
import com.trieu.tripplanner.dto.request.LoginRequest;
import com.trieu.tripplanner.dto.request.RegisterRequest;
import com.trieu.tripplanner.dto.response.AuthResponse;
import com.trieu.tripplanner.dto.response.UserResponse;
import com.trieu.tripplanner.exception.AccountBlockedException;
import com.trieu.tripplanner.exception.EmailAlreadyExistsException;
import com.trieu.tripplanner.exception.EmailNotVerifiedException;
import com.trieu.tripplanner.exception.InvalidCredentialsException;
import com.trieu.tripplanner.mapper.UserMapper;
import com.trieu.tripplanner.model.User;
import com.trieu.tripplanner.model.enums.UserStatus;
import com.trieu.tripplanner.repository.UserRepository;
import com.trieu.tripplanner.security.CustomUserDetails;
import com.trieu.tripplanner.security.JwtTokenProvider;
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

        // Task 1.4: issue verification token + send mail here
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
        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new AccountBlockedException(user.getId());
        }
        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException(user.getId());
        }

        log.info("User id={} logged in", user.getId());
        return issueTokens(user, client);
    }

    private AuthTokens issueTokens(User user, ClientInfo client) {
        JwtTokenProvider.AccessToken access = jwtTokenProvider.generateAccessToken(user);
        RefreshTokenService.IssuedRefreshToken refresh = refreshTokenService.issue(user, client);

        AuthResponse response = new AuthResponse(access.token(), AuthResponse.BEARER, access.expiresInSeconds(),
                userMapper.toResponse(user));
        return new AuthTokens(response, refresh.rawToken());
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

}

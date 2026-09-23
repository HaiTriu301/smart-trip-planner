package com.trieu.tripplanner.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.trieu.tripplanner.common.constant.ErrorCode;
import com.trieu.tripplanner.config.SecurityConfig;
import com.trieu.tripplanner.dto.internal.AuthTokens;
import com.trieu.tripplanner.dto.internal.ClientInfo;
import com.trieu.tripplanner.dto.request.LoginRequest;
import com.trieu.tripplanner.dto.request.RegisterRequest;
import com.trieu.tripplanner.dto.response.UserResponse;
import com.trieu.tripplanner.exception.AccountBlockedException;
import com.trieu.tripplanner.exception.EmailAlreadyExistsException;
import com.trieu.tripplanner.exception.EmailNotVerifiedException;
import com.trieu.tripplanner.exception.InvalidCredentialsException;
import com.trieu.tripplanner.mapper.UserMapper;
import com.trieu.tripplanner.model.User;
import com.trieu.tripplanner.model.enums.Plan;
import com.trieu.tripplanner.model.enums.Role;
import com.trieu.tripplanner.model.enums.UserStatus;
import com.trieu.tripplanner.repository.UserRepository;
import com.trieu.tripplanner.security.CustomUserDetails;
import com.trieu.tripplanner.security.JwtTokenProvider;
import com.trieu.tripplanner.support.TestUsers;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Pure unit test: no Spring context, no database. Repository, AuthenticationManager, token provider and
 * RefreshTokenService are mocked; the encoder and the MapStruct mapper are real.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String RAW_PASSWORD = "MatKhau123";
    private static final ClientInfo CLIENT = new ClientInfo("JUnit", "127.0.0.1");

    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private RefreshTokenService refreshTokenService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(SecurityConfig.BCRYPT_STRENGTH);
    private final UserMapper userMapper = Mappers.getMapper(UserMapper.class);

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userRepository, passwordEncoder, userMapper,
                authenticationManager, jwtTokenProvider, refreshTokenService);
    }

    @Nested
    class Register {

        @Test
        void hashesPasswordWithBcrypt12AndAppliesDefaults() {
            when(userRepository.existsByEmail("an@example.com")).thenReturn(false);
            when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            UserResponse response = authService.register(registerRequest("an@example.com", "Nguyễn An"));

            ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
            verify(userRepository).saveAndFlush(saved.capture());
            User user = saved.getValue();

            assertThat(user.getPasswordHash()).startsWith("$2a$12$").isNotEqualTo(RAW_PASSWORD);
            assertThat(passwordEncoder.matches(RAW_PASSWORD, user.getPasswordHash())).isTrue();
            assertThat(user.getEmail()).isEqualTo("an@example.com");
            assertThat(user.getRole()).isEqualTo(Role.USER);
            assertThat(user.getPlan()).isEqualTo(Plan.FREE);
            assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(user.isEmailVerified()).isFalse();
            assertThat(response.email()).isEqualTo("an@example.com");
            assertThat(response.fullName()).isEqualTo("Nguyễn An");
        }

        @Test
        void normalizesEmailAndTrimsName() {
            when(userRepository.existsByEmail("binh@example.com")).thenReturn(false);
            when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            UserResponse response = authService.register(registerRequest("  Binh@Example.COM ", "  Trần Bình  "));

            verify(userRepository).existsByEmail("binh@example.com");
            assertThat(response.email()).isEqualTo("binh@example.com");
            assertThat(response.fullName()).isEqualTo("Trần Bình");
        }

        @Test
        void rejectsExistingEmail() {
            when(userRepository.existsByEmail("dup@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(registerRequest("dup@example.com", "Dup")))
                    .isInstanceOf(EmailAlreadyExistsException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);
            verify(userRepository, never()).saveAndFlush(any());
        }

        @Test
        void translatesUniqueIndexRaceIntoEmailAlreadyExists() {
            when(userRepository.existsByEmail("race@example.com")).thenReturn(false);
            DataIntegrityViolationException dbError = new DataIntegrityViolationException("Duplicate entry");
            when(userRepository.saveAndFlush(any(User.class))).thenThrow(dbError);

            assertThatThrownBy(() -> authService.register(registerRequest("race@example.com", "Race")))
                    .isInstanceOf(EmailAlreadyExistsException.class)
                    .hasCause(dbError);
        }

    }

    @Nested
    class Login {

        private final User verified = TestUsers.verified(7L, "an@example.com");

        @Test
        void issuesAccessTokenAndRefreshTokenForVerifiedActiveUser() {
            stubSuccessfulAuthentication(verified);
            stubTokenIssuing(verified, "jwt-access", "raw-refresh-64");

            AuthTokens tokens = authService.login(new LoginRequest("An@Example.com", RAW_PASSWORD), CLIENT);

            assertThat(tokens.refreshToken()).isEqualTo("raw-refresh-64");
            assertThat(tokens.response().accessToken()).isEqualTo("jwt-access");
            assertThat(tokens.response().tokenType()).isEqualTo("Bearer");
            assertThat(tokens.response().expiresIn()).isEqualTo(900L);
            assertThat(tokens.response().user().id()).isEqualTo(7L);
            assertThat(tokens.response().user().email()).isEqualTo("an@example.com");

            // Email normalized before it reaches the AuthenticationManager
            ArgumentCaptor<Authentication> attempt = ArgumentCaptor.forClass(Authentication.class);
            verify(authenticationManager).authenticate(attempt.capture());
            assertThat(attempt.getValue().getPrincipal()).isEqualTo("an@example.com");
            assertThat(attempt.getValue().getCredentials()).isEqualTo(RAW_PASSWORD);
            verify(refreshTokenService).issue(verified, CLIENT);
        }

        @Test
        void wrongPasswordBecomesInvalidCredentialsAndIssuesNothing() {
            when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authService.login(new LoginRequest("an@example.com", "sai"), CLIENT))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_CREDENTIALS);
            verifyNoInteractions(jwtTokenProvider, refreshTokenService);
        }

        @Test
        void blockedAccountIsRejectedAfterPasswordCheck() {
            User blocked = TestUsers.blocked(8L, "blocked@example.com");
            stubSuccessfulAuthentication(blocked);

            assertThatThrownBy(() -> authService.login(new LoginRequest("blocked@example.com", RAW_PASSWORD), CLIENT))
                    .isInstanceOf(AccountBlockedException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.ACCOUNT_BLOCKED);
            verifyNoInteractions(jwtTokenProvider, refreshTokenService);
        }

        @Test
        void unverifiedEmailIsRejectedAfterPasswordCheck() {
            User unverified = TestUsers.unverified(9L, "new@example.com");
            stubSuccessfulAuthentication(unverified);

            assertThatThrownBy(() -> authService.login(new LoginRequest("new@example.com", RAW_PASSWORD), CLIENT))
                    .isInstanceOf(EmailNotVerifiedException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.EMAIL_NOT_VERIFIED);
            verifyNoInteractions(jwtTokenProvider, refreshTokenService);
        }

    }

    private void stubSuccessfulAuthentication(User user) {
        CustomUserDetails principal = CustomUserDetails.from(user);
        Authentication authenticated = UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(authenticated);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
    }

    private void stubTokenIssuing(User user, String accessToken, String rawRefreshToken) {
        Instant issuedAt = Instant.now();
        when(jwtTokenProvider.generateAccessToken(user))
                .thenReturn(new JwtTokenProvider.AccessToken(accessToken, issuedAt, issuedAt.plus(Duration.ofMinutes(15))));
        when(refreshTokenService.issue(user, CLIENT))
                .thenReturn(new RefreshTokenService.IssuedRefreshToken(rawRefreshToken, Instant.now().plus(Duration.ofDays(7))));
    }

    private static RegisterRequest registerRequest(String email, String fullName) {
        return new RegisterRequest(email, RAW_PASSWORD, RAW_PASSWORD, fullName);
    }

}

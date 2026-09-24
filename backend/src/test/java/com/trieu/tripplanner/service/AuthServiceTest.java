package com.trieu.tripplanner.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.trieu.tripplanner.dto.request.ResetPasswordRequest;
import com.trieu.tripplanner.dto.response.UserResponse;
import com.trieu.tripplanner.exception.AccountBlockedException;
import com.trieu.tripplanner.exception.EmailAlreadyExistsException;
import com.trieu.tripplanner.exception.EmailNotVerifiedException;
import com.trieu.tripplanner.exception.InvalidCredentialsException;
import com.trieu.tripplanner.exception.InvalidRefreshTokenException;
import com.trieu.tripplanner.exception.InvalidTokenException;
import com.trieu.tripplanner.mapper.UserMapper;
import com.trieu.tripplanner.model.RefreshToken;
import com.trieu.tripplanner.model.User;
import com.trieu.tripplanner.model.VerificationToken;
import com.trieu.tripplanner.model.enums.Plan;
import com.trieu.tripplanner.model.enums.Role;
import com.trieu.tripplanner.model.enums.UserStatus;
import com.trieu.tripplanner.model.enums.VerificationTokenType;
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
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Pure unit test: no Spring context, no database. Repository, AuthenticationManager, token providers, mail and
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
    @Mock
    private VerificationTokenService verificationTokenService;
    @Mock
    private MailService mailService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(SecurityConfig.BCRYPT_STRENGTH);
    private final UserMapper userMapper = Mappers.getMapper(UserMapper.class);

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userRepository, passwordEncoder, userMapper,
                authenticationManager, jwtTokenProvider, refreshTokenService, verificationTokenService, mailService);
    }

    @Nested
    class Register {

        @Test
        void hashesPasswordAppliesDefaultsAndSendsVerificationMail() {
            when(userRepository.existsByEmail("an@example.com")).thenReturn(false);
            when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(verificationTokenService.issue(any(User.class), eq(VerificationTokenType.EMAIL_VERIFY))).thenReturn("raw-verify");

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

            // Token issued for the saved user, mail sent with the raw token (Task 1.4)
            verify(verificationTokenService).issue(user, VerificationTokenType.EMAIL_VERIFY);
            verify(mailService).sendVerificationMail("an@example.com", "Nguyễn An", "raw-verify");
        }

        @Test
        void normalizesEmailAndTrimsName() {
            when(userRepository.existsByEmail("binh@example.com")).thenReturn(false);
            when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(verificationTokenService.issue(any(), any())).thenReturn("raw");

            UserResponse response = authService.register(registerRequest("  Binh@Example.COM ", "  Trần Bình  "));

            verify(userRepository).existsByEmail("binh@example.com");
            assertThat(response.email()).isEqualTo("binh@example.com");
            assertThat(response.fullName()).isEqualTo("Trần Bình");
        }

        @Test
        void rejectsExistingEmailWithoutSendingAnything() {
            when(userRepository.existsByEmail("dup@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(registerRequest("dup@example.com", "Dup")))
                    .isInstanceOf(EmailAlreadyExistsException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);
            verify(userRepository, never()).saveAndFlush(any());
            verifyNoInteractions(verificationTokenService, mailService);
        }

        @Test
        void translatesUniqueIndexRaceIntoEmailAlreadyExists() {
            when(userRepository.existsByEmail("race@example.com")).thenReturn(false);
            DataIntegrityViolationException dbError = new DataIntegrityViolationException("Duplicate entry");
            when(userRepository.saveAndFlush(any(User.class))).thenThrow(dbError);

            assertThatThrownBy(() -> authService.register(registerRequest("race@example.com", "Race")))
                    .isInstanceOf(EmailAlreadyExistsException.class)
                    .hasCause(dbError);
            verifyNoInteractions(mailService);
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

            ArgumentCaptor<Authentication> attempt = ArgumentCaptor.forClass(Authentication.class);
            verify(authenticationManager).authenticate(attempt.capture());
            assertThat(attempt.getValue().getPrincipal()).isEqualTo("an@example.com");
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
            stubSuccessfulAuthentication(TestUsers.blocked(8L, "blocked@example.com"));

            assertThatThrownBy(() -> authService.login(new LoginRequest("blocked@example.com", RAW_PASSWORD), CLIENT))
                    .isInstanceOf(AccountBlockedException.class);
            verifyNoInteractions(jwtTokenProvider, refreshTokenService);
        }

        @Test
        void unverifiedEmailIsRejectedAfterPasswordCheck() {
            stubSuccessfulAuthentication(TestUsers.unverified(9L, "new@example.com"));

            assertThatThrownBy(() -> authService.login(new LoginRequest("new@example.com", RAW_PASSWORD), CLIENT))
                    .isInstanceOf(EmailNotVerifiedException.class);
            verifyNoInteractions(jwtTokenProvider, refreshTokenService);
        }

    }

    @Nested
    class Refresh {

        private final User user = TestUsers.verified(7L, "an@example.com");

        @Test
        void missingCookieIsRejected() {
            assertThatThrownBy(() -> authService.refresh(null, CLIENT)).isInstanceOf(InvalidRefreshTokenException.class);
            assertThatThrownBy(() -> authService.refresh("  ", CLIENT)).isInstanceOf(InvalidRefreshTokenException.class);
            verifyNoInteractions(refreshTokenService);
        }

        @Test
        void unknownTokenIsRejected() {
            when(refreshTokenService.findByRawToken("ghost")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refresh("ghost", CLIENT))
                    .isInstanceOf(InvalidRefreshTokenException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.UNAUTHORIZED);
        }

        @Test
        void reusedRevokedTokenIsTreatedAsTheftAndKillsAllSessions() {
            RefreshToken revoked = liveToken(user);
            revoked.revoke(Instant.now().minusSeconds(30));
            when(refreshTokenService.findByRawToken("old")).thenReturn(Optional.of(revoked));

            assertThatThrownBy(() -> authService.refresh("old", CLIENT)).isInstanceOf(InvalidRefreshTokenException.class);

            verify(refreshTokenService).revokeAll(7L);
            verify(refreshTokenService, never()).issue(any(), any());
            verifyNoInteractions(jwtTokenProvider);
        }

        @Test
        void expiredTokenIsRevokedAndRejected() {
            RefreshToken expired = RefreshToken.builder().user(user).tokenHash("h")
                    .expiresAt(Instant.now().minusSeconds(1)).build();
            when(refreshTokenService.findByRawToken("stale")).thenReturn(Optional.of(expired));

            assertThatThrownBy(() -> authService.refresh("stale", CLIENT)).isInstanceOf(InvalidRefreshTokenException.class);

            verify(refreshTokenService).revoke(expired);
            verify(refreshTokenService, never()).issue(any(), any());
        }

        @Test
        void blockedUserCannotRefreshAndLosesAllSessions() {
            User blocked = TestUsers.blocked(8L, "blocked@example.com");
            when(refreshTokenService.findByRawToken("ok")).thenReturn(Optional.of(liveToken(blocked)));

            assertThatThrownBy(() -> authService.refresh("ok", CLIENT)).isInstanceOf(AccountBlockedException.class);

            verify(refreshTokenService).revokeAll(8L);
            verify(refreshTokenService, never()).issue(any(), any());
        }

        @Test
        void validTokenIsRotatedIntoANewPair() {
            RefreshToken current = liveToken(user);
            when(refreshTokenService.findByRawToken("current")).thenReturn(Optional.of(current));
            stubTokenIssuing(user, "jwt-2", "raw-refresh-2");

            AuthTokens tokens = authService.refresh("current", CLIENT);

            verify(refreshTokenService).revoke(current);
            verify(refreshTokenService).issue(user, CLIENT);
            verify(refreshTokenService, never()).revokeAll(any());
            assertThat(tokens.refreshToken()).isEqualTo("raw-refresh-2");
            assertThat(tokens.response().accessToken()).isEqualTo("jwt-2");
        }

    }

    @Nested
    class Logout {

        @Test
        void missingCookieIsANoOp() {
            authService.logout(null);
            authService.logout("");

            verifyNoInteractions(refreshTokenService);
        }

        @Test
        void unknownTokenIsSilentlyIgnored() {
            when(refreshTokenService.findByRawToken("ghost")).thenReturn(Optional.empty());

            authService.logout("ghost");

            verify(refreshTokenService, never()).revoke(any());
        }

        @Test
        void knownTokenIsRevoked() {
            RefreshToken token = liveToken(TestUsers.verified(7L, "an@example.com"));
            when(refreshTokenService.findByRawToken("current")).thenReturn(Optional.of(token));

            authService.logout("current");

            verify(refreshTokenService).revoke(token);
        }

    }

    @Nested
    class VerifyEmail {

        @Test
        void consumesTokenAndMarksUserVerified() {
            User unverified = TestUsers.unverified(9L, "new@example.com");
            VerificationToken token = VerificationToken.builder().user(unverified).tokenHash("h")
                    .type(VerificationTokenType.EMAIL_VERIFY).expiresAt(Instant.now().plus(Duration.ofHours(1))).build();
            when(verificationTokenService.consume("raw", VerificationTokenType.EMAIL_VERIFY)).thenReturn(token);

            authService.verifyEmail("raw");

            assertThat(unverified.isEmailVerified()).isTrue();
        }

        @Test
        void invalidTokenPropagatesAndChangesNothing() {
            when(verificationTokenService.consume("bad", VerificationTokenType.EMAIL_VERIFY))
                    .thenThrow(new InvalidTokenException("unknown token"));

            assertThatThrownBy(() -> authService.verifyEmail("bad"))
                    .isInstanceOf(InvalidTokenException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_TOKEN);
            verifyNoInteractions(userRepository, mailService);
        }

    }

    @Nested
    class ResendVerification {

        @Test
        void unknownEmailSendsNothingAndDoesNotThrow() {
            when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

            authService.resendVerification("Ghost@Example.com");

            verifyNoInteractions(verificationTokenService, mailService);
        }

        @Test
        void alreadyVerifiedOrBlockedAccountsSendNothing() {
            when(userRepository.findByEmail("done@example.com")).thenReturn(Optional.of(TestUsers.verified(1L, "done@example.com")));
            when(userRepository.findByEmail("blocked@example.com")).thenReturn(Optional.of(
                    TestUsers.user(2L, "blocked@example.com", Role.USER, Plan.FREE, UserStatus.BLOCKED, false)));

            authService.resendVerification("done@example.com");
            authService.resendVerification("blocked@example.com");

            verifyNoInteractions(verificationTokenService, mailService);
        }

        @Test
        void unverifiedActiveAccountGetsAFreshTokenAndMail() {
            User unverified = TestUsers.unverified(9L, "new@example.com");
            when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.of(unverified));
            when(verificationTokenService.issue(unverified, VerificationTokenType.EMAIL_VERIFY)).thenReturn("raw-2");

            authService.resendVerification("new@example.com");

            verify(mailService).sendVerificationMail("new@example.com", unverified.getFullName(), "raw-2");
        }

    }

    @Nested
    class ForgotPassword {

        @Test
        void unknownEmailSendsNothingAndDoesNotThrow() {
            when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

            authService.forgotPassword("Ghost@Example.com");

            verifyNoInteractions(verificationTokenService, mailService);
        }

        @Test
        void unverifiedOrBlockedAccountsGetNoResetMail() {
            // Rule 14.12: only verified accounts receive mail other than the verification mail itself
            when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.of(TestUsers.unverified(9L, "new@example.com")));
            when(userRepository.findByEmail("blocked@example.com")).thenReturn(Optional.of(TestUsers.blocked(8L, "blocked@example.com")));

            authService.forgotPassword("new@example.com");
            authService.forgotPassword("blocked@example.com");

            verifyNoInteractions(verificationTokenService, mailService);
        }

        @Test
        void verifiedActiveAccountGetsAResetTokenAndMail() {
            User verified = TestUsers.verified(7L, "an@example.com");
            when(userRepository.findByEmail("an@example.com")).thenReturn(Optional.of(verified));
            when(verificationTokenService.issue(verified, VerificationTokenType.PASSWORD_RESET)).thenReturn("raw-reset");

            authService.forgotPassword("an@example.com");

            verify(mailService).sendPasswordResetMail("an@example.com", verified.getFullName(), "raw-reset");
            verify(mailService, never()).sendVerificationMail(any(), any(), any());
        }

    }

    @Nested
    class ResetPassword {

        @Test
        void storesNewBcryptHashAndRevokesEverySession() {
            User user = TestUsers.verified(7L, "an@example.com");
            String oldHash = user.getPasswordHash();
            VerificationToken token = VerificationToken.builder().user(user).tokenHash("h")
                    .type(VerificationTokenType.PASSWORD_RESET).expiresAt(Instant.now().plus(Duration.ofMinutes(30))).build();
            when(verificationTokenService.consume("raw", VerificationTokenType.PASSWORD_RESET)).thenReturn(token);
            when(refreshTokenService.revokeAll(7L)).thenReturn(2);

            authService.resetPassword(new ResetPasswordRequest("raw", "MatKhauMoi456", "MatKhauMoi456"));

            assertThat(user.getPasswordHash()).isNotEqualTo(oldHash).startsWith("$2a$12$");
            assertThat(passwordEncoder.matches("MatKhauMoi456", user.getPasswordHash())).isTrue();
            assertThat(passwordEncoder.matches(RAW_PASSWORD, user.getPasswordHash())).isFalse();
            verify(refreshTokenService).revokeAll(7L);
        }

        @Test
        void invalidTokenChangesNothing() {
            when(verificationTokenService.consume("bad", VerificationTokenType.PASSWORD_RESET))
                    .thenThrow(new InvalidTokenException("expired"));

            assertThatThrownBy(() -> authService.resetPassword(new ResetPasswordRequest("bad", "MatKhauMoi456", "MatKhauMoi456")))
                    .isInstanceOf(InvalidTokenException.class);
            verifyNoInteractions(refreshTokenService, mailService, userRepository);
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

    private static RefreshToken liveToken(User user) {
        RefreshToken token = RefreshToken.builder().user(user).tokenHash("h").expiresAt(Instant.now().plus(Duration.ofDays(1))).build();
        ReflectionTestUtils.setField(token, "id", 100L);
        return token;
    }

    private static RegisterRequest registerRequest(String email, String fullName) {
        return new RegisterRequest(email, RAW_PASSWORD, RAW_PASSWORD, fullName);
    }

}

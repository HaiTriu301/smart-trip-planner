package com.trieu.tripplanner.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trieu.tripplanner.common.constant.ErrorCode;
import com.trieu.tripplanner.config.SecurityConfig;
import com.trieu.tripplanner.dto.request.RegisterRequest;
import com.trieu.tripplanner.dto.response.UserResponse;
import com.trieu.tripplanner.exception.EmailAlreadyExistsException;
import com.trieu.tripplanner.mapper.UserMapper;
import com.trieu.tripplanner.model.User;
import com.trieu.tripplanner.model.enums.Plan;
import com.trieu.tripplanner.model.enums.Role;
import com.trieu.tripplanner.model.enums.UserStatus;
import com.trieu.tripplanner.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Pure unit test: no Spring context, no database. The repository is mocked; the encoder and the
 * MapStruct-generated mapper are real so the test proves the actual hashing and mapping.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String RAW_PASSWORD = "MatKhau123";

    @Mock
    private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(SecurityConfig.BCRYPT_STRENGTH);
    private final UserMapper userMapper = Mappers.getMapper(UserMapper.class);

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userRepository, passwordEncoder, userMapper);
    }

    @Test
    void registerHashesPasswordWithBcrypt12AndAppliesDefaults() {
        when(userRepository.existsByEmail("an@example.com")).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = authService.register(request("an@example.com", "Nguyễn An"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(saved.capture());
        User user = saved.getValue();

        assertThat(user.getPasswordHash()).startsWith("$2a$12$").isNotEqualTo(RAW_PASSWORD);
        assertThat(passwordEncoder.matches(RAW_PASSWORD, user.getPasswordHash())).isTrue();
        assertThat(user.getEmail()).isEqualTo("an@example.com");
        assertThat(user.getFullName()).isEqualTo("Nguyễn An");
        assertThat(user.getRole()).isEqualTo(Role.USER);
        assertThat(user.getPlan()).isEqualTo(Plan.FREE);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.isEmailVerified()).isFalse();

        assertThat(response.email()).isEqualTo("an@example.com");
        assertThat(response.fullName()).isEqualTo("Nguyễn An");
        assertThat(response.role()).isEqualTo(Role.USER);
        assertThat(response.plan()).isEqualTo(Plan.FREE);
        assertThat(response.emailVerified()).isFalse();
    }

    @Test
    void registerNormalizesEmailAndTrimsNameBeforeUse() {
        when(userRepository.existsByEmail("binh@example.com")).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = authService.register(request("  Binh@Example.COM ", "  Trần Bình  "));

        verify(userRepository).existsByEmail("binh@example.com");
        assertThat(response.email()).isEqualTo("binh@example.com");
        assertThat(response.fullName()).isEqualTo("Trần Bình");
    }

    @Test
    void registerRejectsEmailThatAlreadyExists() {
        when(userRepository.existsByEmail("dup@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request("dup@example.com", "Dup")))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void registerTranslatesUniqueIndexRaceIntoEmailAlreadyExists() {
        when(userRepository.existsByEmail("race@example.com")).thenReturn(false);
        DataIntegrityViolationException dbError = new DataIntegrityViolationException("Duplicate entry 'race@example.com'");
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(dbError);

        assertThatThrownBy(() -> authService.register(request("race@example.com", "Race")))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasCause(dbError);
    }

    private static RegisterRequest request(String email, String fullName) {
        return new RegisterRequest(email, RAW_PASSWORD, RAW_PASSWORD, fullName);
    }

}

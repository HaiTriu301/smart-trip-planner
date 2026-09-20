package com.trieu.tripplanner.service;

import com.trieu.tripplanner.dto.request.RegisterRequest;
import com.trieu.tripplanner.dto.response.UserResponse;
import com.trieu.tripplanner.exception.EmailAlreadyExistsException;
import com.trieu.tripplanner.mapper.UserMapper;
import com.trieu.tripplanner.model.User;
import com.trieu.tripplanner.repository.UserRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
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

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        // Same normalization the entity applies on persist, so the duplicate check sees the stored form
        String email = request.email().trim().toLowerCase(Locale.ROOT);

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

}

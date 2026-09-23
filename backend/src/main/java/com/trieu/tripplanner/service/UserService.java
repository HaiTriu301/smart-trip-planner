package com.trieu.tripplanner.service;

import com.trieu.tripplanner.dto.response.UserResponse;
import com.trieu.tripplanner.exception.ResourceNotFoundException;
import com.trieu.tripplanner.mapper.UserMapper;
import com.trieu.tripplanner.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Profile operations. Plain class for now (CLAUDE.md rule 5); grows an interface when update/avatar arrive.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    /**
     * @param userId taken from the authenticated principal, never from the request (CLAUDE.md rule 16)
     */
    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        return userRepository.findById(userId)
                .map(userMapper::toResponse)
                // Token valid but row gone (deleted while the token was alive): behave like any missing resource
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

}

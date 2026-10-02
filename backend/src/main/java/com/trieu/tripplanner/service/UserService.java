package com.trieu.tripplanner.service;

import com.trieu.tripplanner.dto.response.UserResponse;
import com.trieu.tripplanner.exception.ResourceNotFoundException;
import com.trieu.tripplanner.mapper.UserMapper;
import com.trieu.tripplanner.model.User;
import com.trieu.tripplanner.repository.UserRepository;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Profile operations. Plain class for now (CLAUDE.md rule 5); grows an interface when update/avatar arrive.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private static final String USER = "User";
    private static final ZoneId DEFAULT_ZONE = ZoneId.of(User.DEFAULT_TIMEZONE);

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final Clock clock;

    /**
     * @param userId taken from the authenticated principal, never from the request (CLAUDE.md rule 16)
     */
    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        return userRepository.findById(userId)
                .map(userMapper::toResponse)
                // Token valid but row gone (deleted while the token was alive): behave like any missing resource
                .orElseThrow(() -> new ResourceNotFoundException(USER, userId));
    }

    /**
     * "Today" for this account: the calendar day in the time zone of the account (design.md rule 14.22). At
     * 23:30 UTC it is already tomorrow for an account in Việt Nam. The time zone is read from the database, not
     * from the access token, so a changed time zone counts from the next request on.
     * <p>
     * A stored time zone that Java cannot read does not fail the request: the default zone is used and the bad
     * value is logged.
     *
     * @param userId taken from the authenticated principal, never from the request (CLAUDE.md rule 16)
     * @throws ResourceNotFoundException the account does not exist or is deleted (404)
     */
    public LocalDate today(Long userId) {
        String timezone = userRepository.findTimezoneById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER, userId));
        return LocalDate.now(clock.withZone(zoneOf(timezone, userId)));
    }

    private static ZoneId zoneOf(String timezone, Long userId) {
        try {
            return ZoneId.of(timezone);
        }
        catch (DateTimeException ex) {
            log.warn("User {} has an unusable time zone '{}', using {} instead", userId, timezone, DEFAULT_ZONE);
            return DEFAULT_ZONE;
        }
    }

}

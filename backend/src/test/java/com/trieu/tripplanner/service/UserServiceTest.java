package com.trieu.tripplanner.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.trieu.tripplanner.exception.ResourceNotFoundException;
import com.trieu.tripplanner.mapper.UserMapper;
import com.trieu.tripplanner.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * "Today" for an account, with a clock that stands still at a chosen moment.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final long USER_ID = 7L;
    private static final LocalDate OCT_5 = LocalDate.of(2026, 10, 5);
    private static final LocalDate OCT_6 = LocalDate.of(2026, 10, 6);

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Test
    void todayIsTheCalendarDayInTheTimeZoneOfTheAccount() {
        // One moment, three calendars: 23:30 UTC on the 5th
        UserService userService = serviceAt("2026-10-05T23:30:00Z");

        when(userRepository.findTimezoneById(USER_ID)).thenReturn(Optional.of("Asia/Ho_Chi_Minh"));
        assertThat(userService.today(USER_ID)).isEqualTo(OCT_6);   // 06:30 on the 6th

        when(userRepository.findTimezoneById(USER_ID)).thenReturn(Optional.of("UTC"));
        assertThat(userService.today(USER_ID)).isEqualTo(OCT_5);

        when(userRepository.findTimezoneById(USER_ID)).thenReturn(Optional.of("America/Los_Angeles"));
        assertThat(userService.today(USER_ID)).isEqualTo(OCT_5);   // 16:30 on the 5th
    }

    @Test
    void dayChangesExactlyAtMidnightOfTheAccount() {
        when(userRepository.findTimezoneById(USER_ID)).thenReturn(Optional.of("Asia/Ho_Chi_Minh"));

        // Việt Nam is UTC+7: midnight there is 17:00 UTC
        assertThat(serviceAt("2026-10-05T16:59:59Z").today(USER_ID)).isEqualTo(OCT_5);
        assertThat(serviceAt("2026-10-05T17:00:00Z").today(USER_ID)).isEqualTo(OCT_6);
    }

    @Test
    void unusableTimeZoneFallsBackToVietnamTimeInsteadOfFailing() {
        UserService userService = serviceAt("2026-10-05T23:30:00Z");

        when(userRepository.findTimezoneById(USER_ID)).thenReturn(Optional.of("Mars/Olympus"));
        assertThat(userService.today(USER_ID)).isEqualTo(OCT_6);

        when(userRepository.findTimezoneById(USER_ID)).thenReturn(Optional.of(""));
        assertThat(userService.today(USER_ID)).isEqualTo(OCT_6);
    }

    @Test
    void todayOfAMissingOrDeletedAccountIsNotFound() {
        when(userRepository.findTimezoneById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> serviceAt("2026-10-05T23:30:00Z").today(USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private UserService serviceAt(String instant) {
        return new UserService(userRepository, userMapper, Clock.fixed(Instant.parse(instant), ZoneOffset.UTC));
    }

}

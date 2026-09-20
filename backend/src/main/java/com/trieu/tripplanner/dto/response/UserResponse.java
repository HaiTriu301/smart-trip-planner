package com.trieu.tripplanner.dto.response;

import com.trieu.tripplanner.model.enums.Plan;
import com.trieu.tripplanner.model.enums.Role;
import com.trieu.tripplanner.model.enums.UserStatus;
import java.time.Instant;

/**
 * Public view of a {@link com.trieu.tripplanner.model.User}. Deliberately has no password field,
 * so the hash cannot leak even if someone maps the whole entity by mistake.
 */
public record UserResponse(
        Long id,
        String email,
        String fullName,
        String avatarUrl,
        String timezone,
        String locale,
        Role role,
        Plan plan,
        Instant planExpiresAt,
        boolean emailVerified,
        UserStatus status,
        Instant createdAt) {
}

package com.trieu.tripplanner.support;

import com.trieu.tripplanner.model.User;
import com.trieu.tripplanner.model.enums.Plan;
import com.trieu.tripplanner.model.enums.Role;
import com.trieu.tripplanner.model.enums.UserStatus;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Builds User instances that look persisted (non-null id) without a database.
 * The id is set reflectively because BaseEntity deliberately has no setter for it.
 */
public final class TestUsers {

    public static final String PASSWORD_HASH = "$2a$12$placeholder-bcrypt-hash-not-real";

    private TestUsers() {
    }

    public static User verified(Long id, String email) {
        return user(id, email, Role.USER, Plan.FREE, UserStatus.ACTIVE, true);
    }

    public static User unverified(Long id, String email) {
        return user(id, email, Role.USER, Plan.FREE, UserStatus.ACTIVE, false);
    }

    public static User blocked(Long id, String email) {
        return user(id, email, Role.USER, Plan.FREE, UserStatus.BLOCKED, true);
    }

    public static User admin(Long id, String email) {
        return user(id, email, Role.ADMIN, Plan.PREMIUM, UserStatus.ACTIVE, true);
    }

    public static User user(Long id, String email, Role role, Plan plan, UserStatus status, boolean emailVerified) {
        User user = User.builder()
                .email(email)
                .passwordHash(PASSWORD_HASH)
                .fullName("Test " + email)
                .role(role)
                .plan(plan)
                .status(status)
                .emailVerified(emailVerified)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

}

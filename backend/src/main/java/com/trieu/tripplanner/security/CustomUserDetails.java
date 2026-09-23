package com.trieu.tripplanner.security;

import com.trieu.tripplanner.model.User;
import com.trieu.tripplanner.model.enums.Plan;
import com.trieu.tripplanner.model.enums.Role;
import com.trieu.tripplanner.model.enums.UserStatus;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * The authenticated principal Spring Security carries around. Built either from the database (login, via
 * CustomUserDetailsService) or from the verified JWT claims (every later request, no DB hit).
 * <p>
 * {@code isEnabled()/isAccountNonLocked()} deliberately stay {@code true}: if they reflected BLOCKED,
 * DaoAuthenticationProvider would reject blocked accounts <em>before</em> checking the password and leak
 * the account state to anyone. AuthService performs the status/verified checks after the password matched
 * (order fixed in design.md 6.1).
 */
@Getter
public final class CustomUserDetails implements UserDetails {

    private static final String ROLE_PREFIX = "ROLE_";

    private final Long id;
    private final String email;
    private final String passwordHash;
    private final Role role;
    private final Plan plan;
    private final UserStatus status;
    private final boolean emailVerified;

    private CustomUserDetails(Long id, String email, String passwordHash, Role role, Plan plan,
                              UserStatus status, boolean emailVerified) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.plan = plan;
        this.status = status;
        this.emailVerified = emailVerified;
    }

    public static CustomUserDetails from(User user) {
        return new CustomUserDetails(user.getId(), user.getEmail(), user.getPasswordHash(), user.getRole(),
                user.getPlan(), user.getStatus(), user.isEmailVerified());
    }

    /**
     * A token is only issued to an ACTIVE, verified account, so those two facts are implied by a valid token.
     */
    public static CustomUserDetails fromClaims(JwtTokenProvider.JwtClaims claims) {
        return new CustomUserDetails(claims.userId(), claims.email(), null, claims.role(), claims.plan(),
                UserStatus.ACTIVE, true);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // hasRole('ADMIN') in @PreAuthorize expects the ROLE_ prefix
        return List.of(new SimpleGrantedAuthority(ROLE_PREFIX + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

}

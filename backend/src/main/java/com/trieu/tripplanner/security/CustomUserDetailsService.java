package com.trieu.tripplanner.security;

import com.trieu.tripplanner.repository.UserRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bridge between Spring Security's login flow and our users table. DaoAuthenticationProvider calls this
 * with the email typed at login, then compares the BCrypt hash itself.
 * Soft-deleted users are invisible to the repository, so they end up as "not found" → bad credentials.
 * Being a bean also makes Spring Boot stop generating the in-memory default user.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        return userRepository.findByEmail(normalized)
                .map(CustomUserDetails::from)
                // DaoAuthenticationProvider turns this into BadCredentialsException, hiding whether the email exists
                .orElseThrow(() -> new UsernameNotFoundException("No account for the given email"));
    }

}

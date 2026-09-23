package com.trieu.tripplanner.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Password login machinery (design.md 6.1). Kept apart from SecurityConfig because it needs the
 * database-backed UserDetailsService, which web slice tests do not have.
 * DaoAuthenticationProvider: load user by email → compare BCrypt hash → BadCredentialsException on any mismatch
 * (including unknown email, so callers cannot tell which one was wrong).
 */
@Configuration(proxyBeanMethods = false)
public class AuthenticationConfig {

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
                                                       PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

}

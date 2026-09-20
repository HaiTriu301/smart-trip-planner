package com.trieu.tripplanner.config;

import com.trieu.tripplanner.security.RestAccessDeniedHandler;
import com.trieu.tripplanner.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Stateless REST security (design.md 6). Everything is locked unless listed in {@link #PUBLIC_PATHS};
 * Task 1.3 adds the JWT filter that actually authenticates requests.
 * The entry point / denied handler are @Import-ed here (not component-scanned) so slice tests get them
 * by importing this single class.
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableMethodSecurity
@Import({RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
@RequiredArgsConstructor
public class SecurityConfig {

    /** design.md 6.3: BCrypt strength 12 → hashes start with $2a$12$ */
    public static final int BCRYPT_STRENGTH = 12;

    private static final String[] PUBLIC_PATHS = {
            "/api/v1/auth/**",
            "/api/v1/ping",
            "/actuator/health", "/actuator/health/**", "/actuator/info",
            "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**"
    };

    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Tokens travel in the Authorization header, not in a session cookie, so CSRF does not apply.
                // Revisit in Task 1.3 when the refresh token cookie is introduced (SameSite=Strict + path-scoped).
                .csrf(AbstractHttpConfigurer::disable)
                // Picks up the corsFilter bean from CorsConfig so preflight is answered before authorization
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    }

}

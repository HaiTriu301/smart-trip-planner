package com.trieu.tripplanner.config;

import com.trieu.tripplanner.config.properties.JwtProperties;
import com.trieu.tripplanner.security.JwtAuthenticationFilter;
import com.trieu.tripplanner.security.JwtJsonCodec;
import com.trieu.tripplanner.security.JwtTokenProvider;
import com.trieu.tripplanner.security.RefreshTokenCookies;
import com.trieu.tripplanner.security.RestAccessDeniedHandler;
import com.trieu.tripplanner.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Stateless REST security (design.md 6). Everything is locked unless listed in {@link #PUBLIC_PATHS}.
 * {@link JwtAuthenticationFilter} authenticates requests that carry a valid bearer token.
 * The security helpers are @Import-ed here (not component-scanned) so slice tests get the whole
 * chain by importing this single class. Beans that need the database (UserDetailsService,
 * AuthenticationManager) live in AuthenticationConfig instead.
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
@Import({RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, JwtJsonCodec.class, JwtTokenProvider.class})
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

    /** Under /api/v1/auth but still requires a valid access token (design.md 10.2). */
    private static final String LOGOUT_PATH = "/api/v1/auth/logout";

    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final JwtTokenProvider jwtTokenProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Access token travels in the Authorization header; the refresh cookie is protected by
                // SameSite=Lax + Path=/api/v1/auth (design.md 6.1), so Spring's CSRF token is not needed.
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
                        .requestMatchers(LOGOUT_PATH).authenticated()   // more specific rule first
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated())
                // Created here, not as a bean: a Filter bean would also be registered in the plain servlet chain
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    }

    @Bean
    public RefreshTokenCookies refreshTokenCookies(JwtProperties jwtProperties) {
        return new RefreshTokenCookies(jwtProperties);
    }

}

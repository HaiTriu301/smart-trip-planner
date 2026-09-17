package com.trieu.tripplanner.config;

import com.trieu.tripplanner.config.properties.AppProperties;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Lets only the configured frontend origin call the API from a browser (design.md 6.3).
 * Declared as a filter so preflight requests are answered before reaching any controller.
 */
@Configuration(proxyBeanMethods = false)
public class CorsConfig {

    private static final long PREFLIGHT_CACHE_SECONDS = 3600;

    @Bean
    public CorsFilter corsFilter(AppProperties appProperties) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(appProperties.frontendUrl()));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        // The refresh token travels in an httpOnly cookie (design.md 6.1), so credentials must be allowed
        config.setAllowCredentials(true);
        config.setMaxAge(PREFLIGHT_CACHE_SECONDS);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return new CorsFilter(source);
    }

}

package com.trieu.tripplanner.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.trieu.tripplanner.config.properties.AppProperties;
import com.trieu.tripplanner.controller.HealthController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(HealthController.class)
@Import(CorsConfig.class)
// Slice tests skip @ConfigurationPropertiesScan, so register the properties CorsConfig needs
@EnableConfigurationProperties(AppProperties.class)
@ActiveProfiles("test")
class CorsConfigTest {

    // Same value as app.frontend-url in application-test.yml
    private static final String FRONTEND_ORIGIN = "http://localhost:5173";

    @Autowired
    private MockMvcTester mvc;

    @Test
    void preflightFromFrontendOriginIsAllowedWithCredentials() {
        assertThat(mvc.options().uri("/api/v1/ping")
                .header(HttpHeaders.ORIGIN, FRONTEND_ORIGIN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .hasStatusOk()
                .hasHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONTEND_ORIGIN)
                .hasHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
    }

    @Test
    void preflightFromUnknownOriginIsRejected() {
        assertThat(mvc.options().uri("/api/v1/ping")
                .header(HttpHeaders.ORIGIN, "http://evil.example.com")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .hasStatus(HttpStatus.FORBIDDEN)
                .doesNotContainHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
    }

}

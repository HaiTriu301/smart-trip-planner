package com.trieu.tripplanner.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.trieu.tripplanner.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

// Without the real SecurityConfig, @WebMvcTest falls back to Spring Security defaults and /ping would be 401.
// The test profile supplies app.jwt.secret that SecurityConfig's JwtProperties requires.
@WebMvcTest(HealthController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class HealthControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void pingReturnsSuccessEnvelope() {
        MvcTestResult result = mvc.get().uri("/api/v1/ping").exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "success": true,
                          "data": "pong",
                          "message": "OK"
                        }
                        """);
        assertThat(result).bodyJson().hasPathSatisfying("$.timestamp", timestamp -> assertThat(timestamp).isNotNull());
    }

}

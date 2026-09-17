package com.trieu.tripplanner.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@WebMvcTest(HealthController.class)
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

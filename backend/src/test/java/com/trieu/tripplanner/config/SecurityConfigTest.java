package com.trieu.tripplanner.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.trieu.tripplanner.common.ApiResponse;
import com.trieu.tripplanner.controller.HealthController;
import com.trieu.tripplanner.security.RestAccessDeniedHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Verifies the filter chain rules and that every security failure uses the ErrorResponse envelope.
 */
@WebMvcTest(controllers = {HealthController.class, SecurityConfigTest.ProtectedController.class})
@Import({SecurityConfig.class, SecurityConfigTest.ProtectedController.class})
class SecurityConfigTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RestAccessDeniedHandler accessDeniedHandler;

    @Test
    void publicPingNeedsNoAuthentication() {
        assertThat(mvc.get().uri("/api/v1/ping")).hasStatusOk();
    }

    @Test
    void protectedUrlWithoutLoginReturns401Envelope() {
        assertThat(mvc.get().uri("/test/secure"))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "success": false,
                          "errorCode": "UNAUTHORIZED",
                          "message": "Bạn cần đăng nhập để tiếp tục",
                          "path": "/test/secure"
                        }
                        """);
    }

    @Test
    void unknownUrlWithoutLoginIsAlso401NotFound404() {
        // Security runs before routing, so anonymous callers cannot probe which URLs exist
        assertThat(mvc.get().uri("/api/v1/khong-ton-tai"))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("UNAUTHORIZED");
    }

    @Test
    @WithMockUser
    void protectedUrlWithLoginSucceeds() {
        assertThat(mvc.get().uri("/test/secure"))
                .hasStatusOk()
                .bodyJson().extractingPath("$.data").isEqualTo("secure");
    }

    @Test
    @WithMockUser
    void postWithoutCsrfTokenIsAcceptedBecauseApiIsStateless() {
        assertThat(mvc.post().uri("/test/secure")).hasStatusOk();
    }

    @Test
    @WithMockUser(roles = "USER")
    void preAuthorizeDenialReturns403EnvelopeViaGlobalExceptionHandler() {
        assertThat(mvc.get().uri("/test/admin"))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "success": false,
                          "errorCode": "FORBIDDEN",
                          "message": "Bạn không có quyền thực hiện thao tác này",
                          "path": "/test/admin"
                        }
                        """);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void preAuthorizeAllowsMatchingRole() {
        assertThat(mvc.get().uri("/test/admin")).hasStatusOk();
    }

    @Test
    void filterLevelAccessDeniedHandlerWritesSame403Envelope() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/users");
        MockHttpServletResponse response = new MockHttpServletResponse();

        accessDeniedHandler.handle(request, response, new AccessDeniedException("denied"));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString())
                .contains("\"errorCode\":\"FORBIDDEN\"")
                .contains("\"path\":\"/api/v1/admin/users\"")
                .contains("Bạn không có quyền thực hiện thao tác này");
    }

    @Test
    void passwordEncoderIsBcryptStrength12() {
        String hash = passwordEncoder.encode("MatKhau123");

        assertThat(hash).startsWith("$2a$12$");
        assertThat(passwordEncoder.matches("MatKhau123", hash)).isTrue();
        assertThat(passwordEncoder.matches("sai-mat-khau", hash)).isFalse();
    }

    /**
     * Test-only endpoints: one behind authentication, one behind a role check.
     */
    @RestController
    @RequestMapping("/test")
    static class ProtectedController {

        @GetMapping("/secure")
        ApiResponse<String> secure() {
            return ApiResponse.ok("secure");
        }

        @PostMapping("/secure")
        ApiResponse<String> securePost() {
            return ApiResponse.ok("posted");
        }

        @GetMapping("/admin")
        @PreAuthorize("hasRole('ADMIN')")
        ApiResponse<String> adminOnly() {
            return ApiResponse.ok("admin");
        }

    }

}

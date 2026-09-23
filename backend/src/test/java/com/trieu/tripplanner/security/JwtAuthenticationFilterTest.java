package com.trieu.tripplanner.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.trieu.tripplanner.common.ApiResponse;
import com.trieu.tripplanner.config.SecurityConfig;
import com.trieu.tripplanner.controller.HealthController;
import com.trieu.tripplanner.model.User;
import com.trieu.tripplanner.support.TestUsers;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The real filter chain with real tokens: what a browser experiences with a good, expired or forged bearer token.
 */
@WebMvcTest(controllers = {HealthController.class, JwtAuthenticationFilterTest.WhoAmIController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilterTest.WhoAmIController.class})
@ActiveProfiles("test")
class JwtAuthenticationFilterTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private final User user = TestUsers.verified(42L, "an@example.com");
    private final User admin = TestUsers.admin(1L, "admin@example.com");

    @Test
    void validTokenAuthenticatesAndExposesPrincipal() {
        String token = jwtTokenProvider.generateAccessToken(user).token();

        assertThat(mvc.get().uri("/test/whoami").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .hasStatusOk()
                .bodyJson().extractingPath("$.data").isEqualTo("42:an@example.com:ROLE_USER");
    }

    @Test
    void adminTokenPassesRoleCheck() {
        String token = jwtTokenProvider.generateAccessToken(admin).token();

        assertThat(mvc.get().uri("/test/admin").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .hasStatusOk();
    }

    @Test
    void userTokenFailsRoleCheckWith403() {
        String token = jwtTokenProvider.generateAccessToken(user).token();

        assertThat(mvc.get().uri("/test/admin").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("FORBIDDEN");
    }

    @Test
    void expiredTokenReturnsTokenExpiredSoClientKnowsToRefresh() {
        String expired = jwtTokenProvider.generateAccessToken(user, Instant.now().minus(Duration.ofHours(1))).token();

        assertThat(mvc.get().uri("/test/whoami").header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "success": false,
                          "errorCode": "TOKEN_EXPIRED",
                          "message": "Phiên đăng nhập đã hết hạn",
                          "path": "/test/whoami"
                        }
                        """);
    }

    @Test
    void forgedTokenReturnsPlainUnauthorized() {
        assertThat(mvc.get().uri("/test/whoami").header(HttpHeaders.AUTHORIZATION, "Bearer not.a.jwt"))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("UNAUTHORIZED");
    }

    @Test
    void missingHeaderReturnsPlainUnauthorized() {
        assertThat(mvc.get().uri("/test/whoami"))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("UNAUTHORIZED");
    }

    @Test
    void nonBearerSchemeIsIgnoredAndRequestStaysAnonymous() {
        assertThat(mvc.get().uri("/test/whoami").header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz"))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void staleTokenDoesNotBreakPublicEndpoints() {
        String expired = jwtTokenProvider.generateAccessToken(user, Instant.now().minus(Duration.ofHours(1))).token();

        assertThat(mvc.get().uri("/api/v1/ping").header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
                .hasStatusOk();
    }

    @RestController
    @RequestMapping("/test")
    static class WhoAmIController {

        @GetMapping("/whoami")
        ApiResponse<String> whoAmI(@AuthenticationPrincipal CustomUserDetails principal) {
            return ApiResponse.ok(principal.getId() + ":" + principal.getEmail() + ":"
                    + principal.getAuthorities().iterator().next().getAuthority());
        }

        @GetMapping("/admin")
        @PreAuthorize("hasRole('ADMIN')")
        ApiResponse<String> adminOnly() {
            return ApiResponse.ok("admin");
        }

    }

}

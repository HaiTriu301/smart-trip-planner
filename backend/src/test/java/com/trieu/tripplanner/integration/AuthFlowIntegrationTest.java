package com.trieu.tripplanner.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.trieu.tripplanner.TestcontainersConfiguration;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * The whole Phase 1.3 story against real MySQL: register → verify (by SQL until Task 1.4) → login → /users/me
 * → refresh → replay the old cookie → every session dies → login again.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class AuthFlowIntegrationTest {

    private static final String EMAIL = "flow@example.com";
    private static final String PASSWORD = "MatKhau123";
    private static final String LOGIN_BODY = """
            {"email": "%s", "password": "%s"}
            """.formatted(EMAIL, PASSWORD);

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void registerAndVerifyUser() {
        assertThat(postJson("/api/v1/auth/register", """
                {"email": "%s", "password": "%s", "confirmPassword": "%s", "fullName": "Flow User"}
                """.formatted(EMAIL, PASSWORD, PASSWORD))).hasStatus(HttpStatus.CREATED);
        // Task 1.4 introduces the real verification flow; until then flip the flag directly
        jdbcTemplate.update("UPDATE users SET email_verified = 1 WHERE email = ?", EMAIL);
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM refresh_tokens");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void loginThenCallProtectedEndpointWithAccessToken() {
        MvcTestResult login = postJson("/api/v1/auth/login", LOGIN_BODY);
        assertThat(login).hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        { "success": true, "data": { "tokenType": "Bearer", "expiresIn": 900, "user": { "email": "flow@example.com" } } }
                        """);
        String accessToken = accessToken(login);
        Cookie refreshCookie = login.getResponse().getCookie("refresh_token");
        assertThat(refreshCookie).isNotNull();
        assertThat(refreshCookie.getValue()).hasSize(64);
        assertThat(refreshCookie.isHttpOnly()).isTrue();
        assertThat(refreshCookie.getPath()).isEqualTo("/api/v1/auth");

        assertThat(mvc.get().uri("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .hasStatusOk()
                .bodyJson().extractingPath("$.data.email").isEqualTo("flow@example.com");
        assertThat(mvc.get().uri("/api/v1/users/me"))
                .hasStatus(HttpStatus.UNAUTHORIZED);

        // DB holds only the hash, never the cookie value
        String storedHash = jdbcTemplate.queryForObject("SELECT token_hash FROM refresh_tokens", String.class);
        assertThat(storedHash).hasSize(64).matches("[0-9a-f]+").isNotEqualTo(refreshCookie.getValue());
    }

    @Test
    void refreshRotatesAndReplayingTheOldCookieKillsEverySession() {
        Cookie first = login().getResponse().getCookie("refresh_token");

        // 1. legitimate rotation
        MvcTestResult refreshed = mvc.post().uri("/api/v1/auth/refresh").cookie(first).exchange();
        assertThat(refreshed).hasStatusOk();
        Cookie second = refreshed.getResponse().getCookie("refresh_token");
        assertThat(second.getValue()).isNotEqualTo(first.getValue());
        assertThat(liveSessions()).isEqualTo(1);

        // 2. attacker (or stale tab) replays the first cookie → theft detected
        assertThat(mvc.post().uri("/api/v1/auth/refresh").cookie(first))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("UNAUTHORIZED");
        assertThat(liveSessions()).isZero();

        // 3. the legitimate second cookie is dead too
        assertThat(mvc.post().uri("/api/v1/auth/refresh").cookie(second)).hasStatus(HttpStatus.UNAUTHORIZED);

        // 4. but the user can simply log in again
        assertThat(postJson("/api/v1/auth/login", LOGIN_BODY)).hasStatusOk();
        assertThat(liveSessions()).isEqualTo(1);
    }

    @Test
    void logoutRevokesSessionAndClearsCookie() {
        MvcTestResult login = login();
        Cookie cookie = login.getResponse().getCookie("refresh_token");

        MvcTestResult logout = mvc.post().uri("/api/v1/auth/logout")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(login))
                .cookie(cookie)
                .exchange();

        assertThat(logout).hasStatusOk();
        assertThat(logout.getResponse().getCookie("refresh_token").getMaxAge()).isZero();
        assertThat(liveSessions()).isZero();
        assertThat(mvc.post().uri("/api/v1/auth/refresh").cookie(cookie)).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void wrongPasswordUnverifiedAndBlockedAccountsAreRejectedInDesignOrder() {
        assertThat(postJson("/api/v1/auth/login", """
                {"email": "flow@example.com", "password": "SaiMatKhau1"}
                """))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("INVALID_CREDENTIALS");

        jdbcTemplate.update("UPDATE users SET email_verified = 0 WHERE email = ?", EMAIL);
        assertThat(postJson("/api/v1/auth/login", LOGIN_BODY))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("EMAIL_NOT_VERIFIED");

        jdbcTemplate.update("UPDATE users SET email_verified = 1, status = 'BLOCKED' WHERE email = ?", EMAIL);
        assertThat(postJson("/api/v1/auth/login", LOGIN_BODY))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("ACCOUNT_BLOCKED");

        // Blocked + wrong password must still look like plain bad credentials (no state leak)
        assertThat(postJson("/api/v1/auth/login", """
                {"email": "flow@example.com", "password": "SaiMatKhau1"}
                """))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("INVALID_CREDENTIALS");

        assertThat(liveSessions()).isZero();
    }

    private MvcTestResult login() {
        MvcTestResult result = postJson("/api/v1/auth/login", LOGIN_BODY);
        assertThat(result).hasStatusOk();
        return result;
    }

    private MvcTestResult postJson(String url, String body) {
        return mvc.post().uri(url).contentType(MediaType.APPLICATION_JSON).content(body).exchange();
    }

    private static String accessToken(MvcTestResult login) {
        try {
            String body = login.getResponse().getContentAsString();
            int start = body.indexOf("\"accessToken\":\"") + "\"accessToken\":\"".length();
            return body.substring(start, body.indexOf('"', start));
        }
        catch (java.io.UnsupportedEncodingException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private int liveSessions() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refresh_tokens WHERE revoked_at IS NULL", Integer.class);
        return count == null ? 0 : count;
    }

}

package com.trieu.tripplanner.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.trieu.tripplanner.TestcontainersConfiguration;
import com.trieu.tripplanner.provider.mail.MailMessage;
import com.trieu.tripplanner.provider.mail.MockMailProvider;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
 * The whole Phase 1 story against real MySQL: register → verification mail (MockMailProvider) → verify → login
 * → /users/me → refresh → replay the old cookie → every session dies → login again.
 * Mail is sent @Async, so the test waits for it with Awaitility instead of sleeping.
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
    private static final Pattern VERIFY_LINK = Pattern.compile("verify-email\\?token=([A-Za-z0-9_-]{64})");

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMailProvider mailProvider;

    @BeforeEach
    void registerAndVerifyThroughTheMailedLink() {
        mailProvider.clear();
        register(EMAIL);
        String token = verificationTokenFromMail(1, EMAIL);

        assertThat(postJson("/api/v1/auth/verify-email", tokenBody(token))).hasStatusOk();
        Boolean verified = jdbcTemplate.queryForObject("SELECT email_verified FROM users WHERE email = ?", Boolean.class, EMAIL);
        assertThat(verified).isTrue();
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM verification_tokens");
        jdbcTemplate.update("DELETE FROM refresh_tokens");
        jdbcTemplate.update("DELETE FROM users");
        mailProvider.clear();
    }

    @Test
    void verificationLinkWorksOnlyOnce() {
        String token = verificationTokenFromMail(1, EMAIL);   // the link already used in @BeforeEach

        assertThat(postJson("/api/v1/auth/verify-email", tokenBody(token)))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("INVALID_TOKEN");
        Integer used = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM verification_tokens WHERE used_at IS NOT NULL", Integer.class);
        assertThat(used).isEqualTo(1);
    }

    @Test
    void resendInvalidatesTheOldLinkAndTheNewOneVerifies() {
        register("second@example.com");
        String first = verificationTokenFromMail(2, "second@example.com");

        assertThat(postJson("/api/v1/auth/resend-verification", """
                {"email": "Second@Example.com"}
                """)).hasStatusOk();
        String second = verificationTokenFromMail(3, "second@example.com");
        assertThat(second).isNotEqualTo(first);

        assertThat(postJson("/api/v1/auth/verify-email", tokenBody(first)))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("INVALID_TOKEN");
        assertThat(postJson("/api/v1/auth/verify-email", tokenBody(second))).hasStatusOk();
    }

    @Test
    void resendForUnknownOrVerifiedEmailAnswers200ButSendsNothing() {
        int mailsBefore = mailProvider.sent().size();

        assertThat(postJson("/api/v1/auth/resend-verification", """
                {"email": "nobody@example.com"}
                """)).hasStatusOk();
        assertThat(postJson("/api/v1/auth/resend-verification", """
                {"email": "%s"}
                """.formatted(EMAIL))).hasStatusOk();   // already verified in @BeforeEach

        assertThat(mailProvider.sent()).hasSize(mailsBefore);
    }

    @Test
    void loginThenCallProtectedEndpointWithAccessToken() {
        MvcTestResult login = postJson("/api/v1/auth/login", LOGIN_BODY);
        assertThat(login).hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        { "success": true, "data": { "tokenType": "Bearer", "expiresIn": 900,
                          "user": { "email": "flow@example.com", "emailVerified": true } } }
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

        String storedHash = jdbcTemplate.queryForObject("SELECT token_hash FROM refresh_tokens", String.class);
        assertThat(storedHash).hasSize(64).matches("[0-9a-f]+").isNotEqualTo(refreshCookie.getValue());
    }

    @Test
    void refreshRotatesAndReplayingTheOldCookieKillsEverySession() {
        Cookie first = login().getResponse().getCookie("refresh_token");

        MvcTestResult refreshed = mvc.post().uri("/api/v1/auth/refresh").cookie(first).exchange();
        assertThat(refreshed).hasStatusOk();
        Cookie second = refreshed.getResponse().getCookie("refresh_token");
        assertThat(second.getValue()).isNotEqualTo(first.getValue());
        assertThat(liveSessions()).isEqualTo(1);

        assertThat(mvc.post().uri("/api/v1/auth/refresh").cookie(first))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("UNAUTHORIZED");
        assertThat(liveSessions()).isZero();

        assertThat(mvc.post().uri("/api/v1/auth/refresh").cookie(second)).hasStatus(HttpStatus.UNAUTHORIZED);

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

        assertThat(postJson("/api/v1/auth/login", """
                {"email": "flow@example.com", "password": "SaiMatKhau1"}
                """))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("INVALID_CREDENTIALS");

        assertThat(liveSessions()).isZero();
    }

    // ---------- helpers ----------

    private void register(String email) {
        assertThat(postJson("/api/v1/auth/register", """
                {"email": "%s", "password": "%s", "confirmPassword": "%s", "fullName": "Flow User"}
                """.formatted(email, PASSWORD, PASSWORD))).hasStatus(HttpStatus.CREATED);
    }

    /** Waits for the n-th mail (1-based) to arrive on the async thread and pulls the token out of its link. */
    private String verificationTokenFromMail(int expectedCount, String expectedRecipient) {
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(mailProvider.sent()).hasSize(expectedCount));
        MailMessage mail = mailProvider.sent().get(expectedCount - 1);
        assertThat(mail.to()).isEqualTo(expectedRecipient);
        Matcher matcher = VERIFY_LINK.matcher(mail.htmlBody());
        assertThat(matcher.find()).as("verification link in mail body").isTrue();
        return matcher.group(1);
    }

    private static String tokenBody(String token) {
        return """
                {"token": "%s"}
                """.formatted(token);
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

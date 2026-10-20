package com.trieu.tripplanner.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.trieu.tripplanner.TestcontainersConfiguration;
import com.trieu.tripplanner.provider.mail.MockMailProvider;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
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
 * The rate limits of design.md 8.2 through the whole application: RateLimitFilter in the security chain, buckets
 * in a real Redis, numbers from application.yml. The test profile turns the limits off for every other test; this
 * class turns them back on.
 * <p>
 * Each test calls from an address of its own, so no test spends the calls of another and the order of the tests
 * does not matter. New limit scenarios go here, not in a new class: each class
 * with its own properties is one more application startup.
 */
@SpringBootTest(properties = "app.rate-limit.enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class RateLimitIntegrationTest {

    private static final String REGISTER_URL = "/api/v1/auth/register";
    /** application.yml: 3 registrations an hour per address, so one comes back every 20 minutes. */
    private static final int REGISTRATIONS_AN_HOUR = 3;
    private static final long TWENTY_MINUTES = Duration.ofMinutes(20).toSeconds();

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMailProvider mailProvider;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM verification_tokens");
        jdbcTemplate.update("DELETE FROM users");
        mailProvider.clear();
    }

    @Test
    void theFourthRegistrationFromOneAddressIsTurnedAwayBeforeReachingTheService() {
        String address = "203.0.113.31";
        for (int call = 1; call <= REGISTRATIONS_AN_HOUR; call++) {
            assertThat(register(address, "limit" + call + "@example.com")).hasStatus(HttpStatus.CREATED);
        }
        await().atMost(Duration.ofSeconds(5)).until(() -> mailProvider.sent().size() == REGISTRATIONS_AN_HOUR);

        MvcTestResult rejected = register(address, "limit4@example.com");

        assertThat(rejected)
                .hasStatus(HttpStatus.TOO_MANY_REQUESTS)
                .hasHeader("X-RateLimit-Remaining", "0")
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "success": false,
                          "errorCode": "RATE_LIMIT_EXCEEDED",
                          "message": "Bạn gửi quá nhiều yêu cầu, vui lòng thử lại sau",
                          "path": "/api/v1/auth/register"
                        }
                        """);
        // Seconds until one registration comes back: 20 minutes, minus the moments the first calls took
        String retryAfter = rejected.getResponse().getHeader(HttpHeaders.RETRY_AFTER);
        assertThat(Long.parseLong(retryAfter)).isBetween(TWENTY_MINUTES - 60, TWENTY_MINUTES);
        assertThat(rejected.getResponse().getHeader("X-RateLimit-Reset")).isEqualTo(retryAfter);

        // Stopped in the filter: no account and no verification mail for the fourth email
        Integer accounts = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email = 'limit4@example.com'", Integer.class);
        assertThat(accounts).isZero();
        await().during(Duration.ofMillis(700)).atMost(Duration.ofSeconds(2))
                .until(() -> mailProvider.sent().size() == REGISTRATIONS_AN_HOUR);
    }

    @Test
    void anotherAddressStillRegistersWhileOneIsBlocked() {
        String blocked = "203.0.113.32";
        for (int call = 1; call <= REGISTRATIONS_AN_HOUR; call++) {
            register(blocked, "busy" + call + "@example.com");
        }
        assertThat(register(blocked, "busy4@example.com")).hasStatus(HttpStatus.TOO_MANY_REQUESTS);

        assertThat(register("203.0.113.33", "neighbour@example.com")).hasStatus(HttpStatus.CREATED);
    }

    @Test
    void aForwardedForHeaderDoesNotBuyANewAllowance() {
        String address = "203.0.113.34";
        for (int call = 1; call <= REGISTRATIONS_AN_HOUR; call++) {
            register(address, "spoof" + call + "@example.com");
        }

        // The caller writes this header itself: the count stays on the address the connection came from
        assertThat(mvc.post().uri(REGISTER_URL)
                .with(request -> {
                    request.setRemoteAddr(address);
                    return request;
                })
                .header("X-Forwarded-For", "198.51.100.99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody("spoof4@example.com")))
                .hasStatus(HttpStatus.TOO_MANY_REQUESTS);
    }

    private MvcTestResult register(String address, String email) {
        return mvc.post().uri(REGISTER_URL)
                .with(request -> {
                    request.setRemoteAddr(address);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody(email))
                .exchange();
    }

    private static String registerBody(String email) {
        return """
                {"email": "%s", "password": "MatKhau123", "confirmPassword": "MatKhau123", "fullName": "Người thử"}
                """.formatted(email);
    }

}

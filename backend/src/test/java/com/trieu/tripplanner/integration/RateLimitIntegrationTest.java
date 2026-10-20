package com.trieu.tripplanner.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.trieu.tripplanner.TestcontainersConfiguration;
import com.trieu.tripplanner.model.User;
import com.trieu.tripplanner.provider.mail.MockMailProvider;
import com.trieu.tripplanner.repository.UserRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String PASSWORD = "MatKhau123";
    /** application.yml: 5 logins in 15 minutes per address and email, so one comes back every 3 minutes. */
    private static final int LOGINS_IN_FIFTEEN_MINUTES = 5;
    private static final long THREE_MINUTES = Duration.ofMinutes(3).toSeconds();
    /** application.yml: 3 registrations an hour per address, so one comes back every 20 minutes. */
    private static final int REGISTRATIONS_AN_HOUR = 3;
    private static final long TWENTY_MINUTES = Duration.ofMinutes(20).toSeconds();

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMailProvider mailProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM refresh_tokens");
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

    @Test
    void theSixthLoginForOneEmailFromOneAddressIsTurnedAway() {
        String address = "203.0.113.41";
        for (int call = 1; call <= LOGINS_IN_FIFTEEN_MINUTES; call++) {
            assertThat(login(address, loginBody("nobody41@example.com")))
                    .as("call %d", call)
                    .hasStatus(HttpStatus.UNAUTHORIZED);
        }

        MvcTestResult rejected = login(address, loginBody("nobody41@example.com"));

        assertThat(rejected)
                .hasStatus(HttpStatus.TOO_MANY_REQUESTS)
                .hasHeader("X-RateLimit-Remaining", "0")
                .bodyJson().isLenientlyEqualTo("""
                        { "success": false, "errorCode": "RATE_LIMIT_EXCEEDED", "path": "/api/v1/auth/login" }
                        """);
        // One login comes back every 3 minutes
        String retryAfter = rejected.getResponse().getHeader(HttpHeaders.RETRY_AFTER);
        assertThat(Long.parseLong(retryAfter)).isBetween(THREE_MINUTES - 60, THREE_MINUTES);
        assertThat(rejected.getResponse().getHeader("X-RateLimit-Reset")).isEqualTo(retryAfter);
    }

    @Test
    void anotherEmailOrAnotherAddressStillGetsThrough() {
        String address = "203.0.113.42";
        for (int call = 1; call <= LOGINS_IN_FIFTEEN_MINUTES; call++) {
            login(address, loginBody("target42@example.com"));
        }
        assertThat(login(address, loginBody("target42@example.com"))).hasStatus(HttpStatus.TOO_MANY_REQUESTS);

        // Someone else behind the same address (an office, a cafe) can still sign in to their own account
        assertThat(login(address, loginBody("colleague42@example.com"))).hasStatus(HttpStatus.UNAUTHORIZED);
        // The owner of the account, from another address, is not locked out by the one guessing
        assertThat(login("203.0.113.43", loginBody("target42@example.com"))).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void anEmailWrittenDifferentlyStillCountsAsTheSameEmail() {
        String address = "203.0.113.44";
        // The service trims and lowers an email before it looks for the account: the count does the same
        String[] spellings = {"Case44@Example.com", "CASE44@EXAMPLE.COM", "  case44@example.com", "case44@example.com  ",
                "cAsE44@eXaMpLe.CoM"};
        for (String spelling : spellings) {
            assertThat(login(address, loginBody(spelling)).getResponse().getStatus())
                    .as(spelling)
                    .isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        }

        assertThat(login(address, loginBody("case44@example.com"))).hasStatus(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void everyLoginCountsEvenWithTheRightPassword() {
        String address = "203.0.113.45";
        saveVerifiedUser("member45@example.com");

        // The filter read the body to find the email, and the controller still reads all of it
        for (int call = 1; call <= LOGINS_IN_FIFTEEN_MINUTES; call++) {
            assertThat(login(address, loginBody("member45@example.com"))).as("call %d", call).hasStatusOk();
        }

        // The limit is checked before the password: a right one gets the same 429 as a wrong one
        assertThat(login(address, loginBody("member45@example.com")))
                .hasStatus(HttpStatus.TOO_MANY_REQUESTS)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("RATE_LIMIT_EXCEEDED");
    }

    @Test
    void aBodyWithoutAReadableEmailIsStillCountedForItsAddress() {
        String address = "203.0.113.46";
        for (int call = 1; call <= 4; call++) {
            // No email at all: the controller answers 400, the call is counted all the same
            assertThat(login(address, """
                    {"password": "MatKhau123"}
                    """)).hasStatus(HttpStatus.BAD_REQUEST);
        }
        assertThat(login(address, "this is not json")).hasStatus(HttpStatus.BAD_REQUEST);

        // Too long for the filter to read: counted in the same bucket, even though it holds an email
        assertThat(login(address, longLoginBody("someone46@example.com"))).hasStatus(HttpStatus.TOO_MANY_REQUESTS);

        // A body the filter can read has a bucket of its own
        assertThat(login(address, loginBody("someone46@example.com"))).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void aBodyTooLongForTheFilterStillReachesTheControllerInFull() {
        saveVerifiedUser("member47@example.com");

        assertThat(login("203.0.113.47", longLoginBody("member47@example.com"))).hasStatusOk();
    }

    private void saveVerifiedUser(String email) {
        userRepository.save(User.builder().email(email).passwordHash(passwordEncoder.encode(PASSWORD))
                .fullName("Người thử").emailVerified(true).build());
    }

    private MvcTestResult login(String address, String body) {
        return mvc.post().uri(LOGIN_URL)
                .with(request -> {
                    request.setRemoteAddr(address);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .exchange();
    }

    private static String loginBody(String email) {
        return """
                {"email": "%s", "password": "%s"}
                """.formatted(email, PASSWORD);
    }

    /** Valid JSON, longer than the filter reads (8 KB): spaces before the closing brace make it so. */
    private static String longLoginBody(String email) {
        String body = loginBody(email).strip();
        return body.substring(0, body.length() - 1) + " ".repeat(9 * 1024) + "}";
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

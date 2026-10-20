package com.trieu.tripplanner.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.jayway.jsonpath.JsonPath;
import com.trieu.tripplanner.TestcontainersConfiguration;
import com.trieu.tripplanner.model.User;
import com.trieu.tripplanner.provider.mail.MockMailProvider;
import com.trieu.tripplanner.repository.UserRepository;
import com.trieu.tripplanner.security.JwtTokenProvider;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * The rate limits of design.md 8.2 through the whole application: RateLimitFilter in the security chain, buckets
 * in a real Redis, numbers from application.yml. The test profile turns the limits off for every other test; this
 * class turns them back on.
 * <p>
 * Each test calls from an address of its own, so no test spends the calls of another and the order of the tests
 * does not matter. New limit scenarios go here, not in a new class: each class
 * with its own properties is one more application startup.
 */
@SpringBootTest(properties = {
        "app.rate-limit.enabled=true",
        // The same 1000 calls, refilled over a day instead of an hour: at 1000 an hour one call comes back every
        // 3.6 seconds, and on a slow machine the 1000 calls of a test take longer than that
        "app.rate-limit.global-user.period=24h"
})
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
    private static final String RESEND_URL = "/api/v1/auth/resend-verification";
    private static final String FORGOT_URL = "/api/v1/auth/forgot-password";
    /** application.yml: 5 calls an hour per address on each endpoint that sends mail. */
    private static final int MAILS_AN_HOUR = 5;
    private static final long TWELVE_MINUTES = Duration.ofMinutes(12).toSeconds();
    /** application.yml: 120 visits of public trip pages an hour per address. */
    private static final int PUBLIC_VIEWS_AN_HOUR = 120;
    /** application.yml: 60 place searches an hour per signed-in user. */
    private static final int SEARCHES_AN_HOUR = 60;
    /** Every call under /api/: 1000 per signed-in user (refilled over a day here, see above), 200 per address. */
    private static final int USER_CALLS_AN_HOUR = 1000;
    private static final int GUEST_CALLS_AN_HOUR = 200;
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

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private StringRedisTemplate redis;

    @AfterEach
    void cleanUp() {
        // Share links and trip days go with their trips (ON DELETE CASCADE)
        jdbcTemplate.update("DELETE FROM trips");
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
        saveUser("member45@example.com", true);

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
        saveUser("member47@example.com", true);

        assertThat(login("203.0.113.47", longLoginBody("member47@example.com"))).hasStatusOk();
    }

    @Test
    void theSixthVerificationMailRequestFromOneAddressIsTurnedAwayAndSendsNothing() {
        String address = "203.0.113.51";
        saveUser("waiting51@example.com", false);
        for (int call = 1; call <= MAILS_AN_HOUR; call++) {
            assertThat(postFrom(address, RESEND_URL, emailBody("waiting51@example.com"))).as("call %d", call)
                    .hasStatusOk();
        }
        await().atMost(Duration.ofSeconds(5)).until(() -> mailProvider.sent().size() == MAILS_AN_HOUR);

        MvcTestResult rejected = postFrom(address, RESEND_URL, emailBody("waiting51@example.com"));

        assertThat(rejected)
                .hasStatus(HttpStatus.TOO_MANY_REQUESTS)
                .hasHeader("X-RateLimit-Remaining", "0")
                .bodyJson().extractingPath("$.errorCode").isEqualTo("RATE_LIMIT_EXCEEDED");
        // 5 an hour: one comes back every 12 minutes
        assertThat(Long.parseLong(rejected.getResponse().getHeader(HttpHeaders.RETRY_AFTER)))
                .isBetween(TWELVE_MINUTES - 60, TWELVE_MINUTES);
        await().during(Duration.ofMillis(700)).atMost(Duration.ofSeconds(2))
                .until(() -> mailProvider.sent().size() == MAILS_AN_HOUR);
    }

    @Test
    void theSixthPasswordResetRequestFromOneAddressIsTurnedAwayAndSendsNothing() {
        String address = "203.0.113.52";
        saveUser("forgetful52@example.com", true);
        for (int call = 1; call <= MAILS_AN_HOUR; call++) {
            assertThat(postFrom(address, FORGOT_URL, emailBody("forgetful52@example.com"))).as("call %d", call)
                    .hasStatusOk();
        }
        await().atMost(Duration.ofSeconds(5)).until(() -> mailProvider.sent().size() == MAILS_AN_HOUR);

        assertThat(postFrom(address, FORGOT_URL, emailBody("forgetful52@example.com")))
                .hasStatus(HttpStatus.TOO_MANY_REQUESTS)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("RATE_LIMIT_EXCEEDED");
        await().during(Duration.ofMillis(700)).atMost(Duration.ofSeconds(2))
                .until(() -> mailProvider.sent().size() == MAILS_AN_HOUR);
    }

    @Test
    void aBlockedAddressGetsTheSameAnswerWhetherTheEmailHasAnAccountOrNot() {
        String address = "203.0.113.53";
        saveUser("known53@example.com", true);
        for (int call = 1; call <= MAILS_AN_HOUR; call++) {
            postFrom(address, FORGOT_URL, emailBody("unknown53@example.com"));
        }

        MvcTestResult unknown = postFrom(address, FORGOT_URL, emailBody("unknown53@example.com"));
        MvcTestResult known = postFrom(address, FORGOT_URL, emailBody("known53@example.com"));

        // Nothing in the answer tells the two apart (design.md rule 14.15): same status, same body but the time
        assertThat(unknown).hasStatus(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(known).hasStatus(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(withoutTimestamp(known)).isEqualTo(withoutTimestamp(unknown));
    }

    @Test
    void eachMailEndpointHasItsOwnCount() {
        String address = "203.0.113.54";
        for (int call = 1; call <= MAILS_AN_HOUR; call++) {
            postFrom(address, RESEND_URL, emailBody("nobody54@example.com"));
        }
        assertThat(postFrom(address, RESEND_URL, emailBody("nobody54@example.com")))
                .hasStatus(HttpStatus.TOO_MANY_REQUESTS);

        assertThat(postFrom(address, FORGOT_URL, emailBody("nobody54@example.com"))).hasStatusOk();
    }

    @Test
    void thePublicPageIsLimitedPerAddressAndARejectedVisitIsNotCountedAsAView() {
        String token = shareLinkOfANewTrip("owner61@example.com");
        String address = "203.0.113.61";
        for (int call = 1; call <= PUBLIC_VIEWS_AN_HOUR; call++) {
            assertThat(openPublicPage(address, token).getResponse().getStatus()).as("call %d", call).isEqualTo(200);
        }

        MvcTestResult rejected = openPublicPage(address, token);

        assertThat(rejected)
                .hasStatus(HttpStatus.TOO_MANY_REQUESTS)
                .hasHeader("X-RateLimit-Remaining", "0")
                .bodyJson().extractingPath("$.errorCode").isEqualTo("RATE_LIMIT_EXCEEDED");
        // 120 an hour: one comes back every 30 seconds
        assertThat(Long.parseLong(rejected.getResponse().getHeader(HttpHeaders.RETRY_AFTER))).isBetween(1L, 30L);
        // Stopped before the service: the owner's count of views did not move for the rejected visit
        assertThat(viewCountOf(token)).isEqualTo(PUBLIC_VIEWS_AN_HOUR);

        // Somebody else, from another address, still sees the trip, and that view is counted
        assertThat(openPublicPage("203.0.113.62", token)).hasStatusOk();
        assertThat(viewCountOf(token)).isEqualTo(PUBLIC_VIEWS_AN_HOUR + 1);
    }

    @Test
    void guessingTokensSpendsTheSameAllowanceAsOpeningPages() {
        String token = shareLinkOfANewTrip("owner63@example.com");
        String address = "203.0.113.63";
        for (int guess = 1; guess < PUBLIC_VIEWS_AN_HOUR; guess++) {
            assertThat(openPublicPage(address, "guess" + guess).getResponse().getStatus()).as("guess %d", guess)
                    .isEqualTo(404);
        }

        // One call left for this address, whatever the token
        assertThat(openPublicPage(address, token)).hasStatusOk();
        assertThat(openPublicPage(address, token)).hasStatus(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void placeSearchIsCountedPerUserEvenFromOneAddressAndEvenFromTheCache() {
        String address = "203.0.113.71";
        String first = bearerOf("searcher71@example.com");
        String second = bearerOf("neighbour71@example.com");
        // The same words every time: after the first call the answer comes from the Redis cache, and still counts
        for (int call = 1; call <= SEARCHES_AN_HOUR; call++) {
            assertThat(search(address, first).getResponse().getStatus()).as("call %d", call).isEqualTo(200);
        }
        assertThat(redis.keys("place:search::*")).as("the answer sits in the cache").isNotEmpty();

        MvcTestResult rejected = search(address, first);

        assertThat(rejected)
                .hasStatus(HttpStatus.TOO_MANY_REQUESTS)
                .hasHeader("X-RateLimit-Remaining", "0")
                .bodyJson().extractingPath("$.errorCode").isEqualTo("RATE_LIMIT_EXCEEDED");
        // 60 an hour: one comes back every minute
        assertThat(Long.parseLong(rejected.getResponse().getHeader(HttpHeaders.RETRY_AFTER))).isBetween(1L, 60L);

        // Another user behind the same address (a family, an office) has a count of their own
        assertThat(search(address, second)).hasStatusOk();
    }

    @Test
    void aSearchWithoutSignInIsNotCountedAndStillAnswers401() {
        assertThat(search("203.0.113.72", null))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("UNAUTHORIZED");
    }

    @Test
    void aGuestOverTheGlobalLimitIsTurnedAwayOnTheWholeApiButNotOnTheHealthCheck() {
        String address = "203.0.113.81";
        for (int call = 1; call <= GUEST_CALLS_AN_HOUR; call++) {
            assertThat(ping(address, null).getResponse().getStatus()).as("call %d", call).isEqualTo(200);
        }

        MvcTestResult rejected = ping(address, null);

        assertThat(rejected)
                .hasStatus(HttpStatus.TOO_MANY_REQUESTS)
                .hasHeader("X-RateLimit-Remaining", "0")
                .bodyJson().extractingPath("$.errorCode").isEqualTo("RATE_LIMIT_EXCEEDED");
        // 200 an hour: one comes back every 18 seconds
        assertThat(Long.parseLong(rejected.getResponse().getHeader(HttpHeaders.RETRY_AFTER))).isBetween(1L, 18L);
        // Any other API path from that address, here one with a limit of its own that is still full
        assertThat(register(address, "global81@example.com")).hasStatus(HttpStatus.TOO_MANY_REQUESTS);

        // Outside /api/: what Docker and the proxy call all the time, and the API documentation
        assertThat(mvc.get().uri("/actuator/health").with(from(address))).hasStatusOk();
        assertThat(mvc.get().uri("/v3/api-docs").with(from(address))).hasStatusOk();
    }

    @Test
    void aSignedInUserHasAGlobalLimitOfTheirOwn() {
        String address = "203.0.113.82";
        String bearer = bearerOf("busy82@example.com");
        for (int call = 1; call <= USER_CALLS_AN_HOUR; call++) {
            assertThat(ping(address, bearer).getResponse().getStatus()).as("call %d", call).isEqualTo(200);
        }

        assertThat(ping(address, bearer))
                .hasStatus(HttpStatus.TOO_MANY_REQUESTS)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("RATE_LIMIT_EXCEEDED");

        // Counted per user: another user and a guest from the same address still have all their calls
        assertThat(ping(address, bearerOf("calm82@example.com"))).hasStatusOk();
        assertThat(ping(address, null)).hasStatusOk();
    }

    @Test
    void aCallWithALimitOfItsOwnTakesFromBothButATurnedAwayOneTakesOnlyFromItsOwn() {
        String address = "203.0.113.83";
        for (int call = 1; call <= REGISTRATIONS_AN_HOUR; call++) {
            assertThat(register(address, "both" + call + "@example.com")).hasStatus(HttpStatus.CREATED);
        }
        // Turned away by the limit of registration: does not reach the global count
        assertThat(register(address, "both4@example.com")).hasStatus(HttpStatus.TOO_MANY_REQUESTS);

        // The 3 registrations let through took 3 calls of the global 200: 197 are left, not 196
        int left = GUEST_CALLS_AN_HOUR - REGISTRATIONS_AN_HOUR;
        for (int call = 1; call <= left; call++) {
            assertThat(ping(address, null).getResponse().getStatus()).as("call %d", call).isEqualTo(200);
        }
        assertThat(ping(address, null)).hasStatus(HttpStatus.TOO_MANY_REQUESTS);
    }

    private MvcTestResult ping(String address, String bearer) {
        var request = mvc.get().uri("/api/v1/ping").with(from(address));
        if (bearer != null) {
            request = request.header(HttpHeaders.AUTHORIZATION, bearer);
        }
        return request.exchange();
    }

    private static RequestPostProcessor from(String address) {
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }

    private String bearerOf(String email) {
        User user = userRepository.save(User.builder().email(email).passwordHash(passwordEncoder.encode(PASSWORD))
                .fullName("Người tìm").emailVerified(true).build());
        return "Bearer " + jwtTokenProvider.generateAccessToken(user).token();
    }

    private MvcTestResult search(String address, String bearer) {
        var request = mvc.get().uri("/api/v1/places/search?q={q}", "chợ hàn")
                .with(servletRequest -> {
                    servletRequest.setRemoteAddr(address);
                    return servletRequest;
                });
        if (bearer != null) {
            request = request.header(HttpHeaders.AUTHORIZATION, bearer);
        }
        return request.exchange();
    }

    /** A trip of a new owner and a share link to it; returns the token of the link. */
    private String shareLinkOfANewTrip(String ownerEmail) {
        User owner = userRepository.save(User.builder().email(ownerEmail).passwordHash(passwordEncoder.encode(PASSWORD))
                .fullName("Chủ chuyến đi").emailVerified(true).build());
        String bearer = "Bearer " + jwtTokenProvider.generateAccessToken(owner).token();
        MvcTestResult trip = mvc.post().uri("/api/v1/trips").header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        { "title": "Đà Lạt", "startDate": "2026-10-01", "endDate": "2026-10-03" }
                        """).exchange();
        assertThat(trip).hasStatus(HttpStatus.CREATED);
        Number tripId = JsonPath.read(bodyOf(trip), "$.data.id");

        MvcTestResult link = mvc.post().uri("/api/v1/trips/" + tripId + "/share-links")
                .header(HttpHeaders.AUTHORIZATION, bearer).exchange();
        assertThat(link).hasStatus(HttpStatus.CREATED);
        return JsonPath.read(bodyOf(link), "$.data.token");
    }

    private MvcTestResult openPublicPage(String address, String token) {
        return mvc.get().uri("/api/v1/public/trips/" + token)
                .with(request -> {
                    request.setRemoteAddr(address);
                    return request;
                })
                .exchange();
    }

    private int viewCountOf(String token) {
        Integer views = jdbcTemplate.queryForObject("SELECT view_count FROM share_links WHERE token = ?",
                Integer.class, token);
        return views == null ? 0 : views;
    }

    // Explicit UTF-8: MockHttpServletResponse otherwise decodes as ISO-8859-1
    private static String bodyOf(MvcTestResult result) {
        return new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
    }

    private void saveUser(String email, boolean verified) {
        userRepository.save(User.builder().email(email).passwordHash(passwordEncoder.encode(PASSWORD))
                .fullName("Người thử").emailVerified(verified).build());
    }

    private MvcTestResult postFrom(String address, String url, String body) {
        return mvc.post().uri(url)
                .with(request -> {
                    request.setRemoteAddr(address);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .exchange();
    }

    private static String emailBody(String email) {
        return """
                {"email": "%s"}
                """.formatted(email);
    }

    private static String withoutTimestamp(MvcTestResult result) {
        String body = new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        return body.replaceAll("\"timestamp\":\"[^\"]*\"", "");
    }

    private MvcTestResult login(String address, String body) {
        return postFrom(address, LOGIN_URL, body);
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

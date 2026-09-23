package com.trieu.tripplanner.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.trieu.tripplanner.config.SecurityConfig;
import com.trieu.tripplanner.dto.internal.AuthTokens;
import com.trieu.tripplanner.dto.internal.ClientInfo;
import com.trieu.tripplanner.dto.request.LoginRequest;
import com.trieu.tripplanner.dto.request.RegisterRequest;
import com.trieu.tripplanner.dto.response.AuthResponse;
import com.trieu.tripplanner.dto.response.UserResponse;
import com.trieu.tripplanner.exception.EmailAlreadyExistsException;
import com.trieu.tripplanner.exception.InvalidCredentialsException;
import com.trieu.tripplanner.model.enums.Plan;
import com.trieu.tripplanner.model.enums.Role;
import com.trieu.tripplanner.model.enums.UserStatus;
import com.trieu.tripplanner.service.AuthService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Web slice: real controller, validation, exception handler, security rules and cookie building; the service is a mock.
 */
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class AuthControllerTest {

    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String LOGIN_URL = "/api/v1/auth/login";

    private static final UserResponse USER = new UserResponse(1L, "an@example.com", "Nguyễn An", null,
            "Asia/Ho_Chi_Minh", "vi", Role.USER, Plan.FREE, null, true, UserStatus.ACTIVE,
            Instant.parse("2026-09-20T10:00:00Z"));
    private static final AuthTokens TOKENS = new AuthTokens(new AuthResponse("jwt-access", "Bearer", 900, USER), "raw-refresh");

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private AuthService authService;

    // ---------- register (Task 1.2) ----------

    @Test
    void registerReturns201WithUserInsideEnvelopeAndNeverEchoesPassword() {
        UserResponse created = new UserResponse(1L, "an@example.com", "Nguyễn An", null,
                "Asia/Ho_Chi_Minh", "vi", Role.USER, Plan.FREE, null, false, UserStatus.ACTIVE,
                Instant.parse("2026-09-20T10:00:00Z"));
        when(authService.register(any(RegisterRequest.class))).thenReturn(created);

        MvcTestResult result = postJson(REGISTER_URL, """
                {"email": "an@example.com", "password": "MatKhau123", "confirmPassword": "MatKhau123", "fullName": "Nguyễn An"}
                """);

        assertThat(result)
                .hasStatus(HttpStatus.CREATED)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "success": true,
                          "message": "OK",
                          "data": { "id": 1, "email": "an@example.com", "role": "USER", "plan": "FREE",
                                    "emailVerified": false, "status": "ACTIVE", "createdAt": "2026-09-20T10:00:00Z" }
                        }
                        """);
        assertThat(result).body().asString().doesNotContain("MatKhau123").doesNotContain("passwordHash");
    }

    @Test
    void invalidRegisterBodyReturns400WithOneDetailPerViolationAndSkipsService() {
        MvcTestResult result = postJson(REGISTER_URL, """
                {"email": "khong-phai-email", "password": "yeu", "confirmPassword": "yeu", "fullName": ""}
                """);

        assertThat(result)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        { "success": false, "errorCode": "VALIDATION_ERROR", "message": "Dữ liệu không hợp lệ",
                          "path": "/api/v1/auth/register" }
                        """);
        assertThat(result).bodyJson().extractingPath("$.details[*].field").asArray()
                .containsExactly("email", "fullName", "password", "password");
        verifyNoInteractions(authService);
    }

    @Test
    void mismatchedConfirmPasswordReturns400OnConfirmPasswordField() {
        MvcTestResult result = postJson(REGISTER_URL, """
                {"email": "an@example.com", "password": "MatKhau123", "confirmPassword": "MatKhau124", "fullName": "An"}
                """);

        assertThat(result)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        { "errorCode": "VALIDATION_ERROR",
                          "details": [ { "field": "confirmPassword", "message": "Mật khẩu xác nhận không khớp" } ] }
                        """);
        verifyNoInteractions(authService);
    }

    @Test
    void missingConfirmPasswordReturns400WithoutMismatchNoise() {
        assertThat(postJson(REGISTER_URL, """
                {"email": "an@example.com", "password": "MatKhau123", "fullName": "An"}
                """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        { "details": [ { "field": "confirmPassword", "message": "Vui lòng nhập lại mật khẩu" } ] }
                        """);
    }

    @Test
    void duplicateEmailReturns409WithErrorCode() {
        when(authService.register(any(RegisterRequest.class))).thenThrow(new EmailAlreadyExistsException());

        assertThat(postJson(REGISTER_URL, """
                {"email": "dup@example.com", "password": "MatKhau123", "confirmPassword": "MatKhau123", "fullName": "Dup"}
                """))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().isLenientlyEqualTo("""
                        { "success": false, "errorCode": "EMAIL_ALREADY_EXISTS", "message": "Email đã được sử dụng" }
                        """);
    }

    // ---------- login ----------

    @Test
    void loginReturnsAccessTokenInBodyAndRefreshTokenInHttpOnlyCookie() {
        when(authService.login(any(LoginRequest.class), any(ClientInfo.class))).thenReturn(TOKENS);

        MvcTestResult result = postJson(LOGIN_URL, """
                {"email": "an@example.com", "password": "MatKhau123"}
                """);

        assertThat(result)
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        { "success": true,
                          "data": { "accessToken": "jwt-access", "tokenType": "Bearer", "expiresIn": 900,
                                    "user": { "id": 1, "email": "an@example.com" } } }
                        """);
        // The refresh token is in the cookie only, never in the body
        assertThat(result).body().asString().doesNotContain("raw-refresh");
        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie)
                .startsWith("refresh_token=raw-refresh;")
                .contains("Path=/api/v1/auth")
                .contains("Max-Age=604800")
                .contains("HttpOnly")
                .contains("SameSite=Lax")
                .doesNotContain("Secure");   // cookie-secure=false outside prod
    }

    @Test
    void loginPassesUserAgentAndClientIpToService() {
        when(authService.login(any(LoginRequest.class), any(ClientInfo.class))).thenReturn(TOKENS);

        mvc.post().uri(LOGIN_URL).contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.USER_AGENT, "JUnit/5")
                .header("X-Forwarded-For", "203.0.113.7, 10.0.0.1")
                .content("""
                        {"email": "an@example.com", "password": "MatKhau123"}
                        """)
                .exchange();

        verify(authService).login(any(LoginRequest.class), eq(new ClientInfo("JUnit/5", "203.0.113.7")));
    }

    @Test
    void loginWithWrongPasswordReturns401InvalidCredentials() {
        when(authService.login(any(LoginRequest.class), any(ClientInfo.class))).thenThrow(new InvalidCredentialsException());

        MvcTestResult result = postJson(LOGIN_URL, """
                {"email": "an@example.com", "password": "sai-roi"}
                """);

        assertThat(result)
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().isLenientlyEqualTo("""
                        { "success": false, "errorCode": "INVALID_CREDENTIALS", "message": "Email hoặc mật khẩu không đúng" }
                        """);
        assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE)).isNull();
    }

    @Test
    void loginWithEmptyBodyFieldsReturns400() {
        assertThat(postJson(LOGIN_URL, """
                {"email": "", "password": ""}
                """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.details[*].field").asArray().containsExactly("email", "password");
        verifyNoInteractions(authService);
    }

    private MvcTestResult postJson(String url, String body) {
        return mvc.post().uri(url).contentType(MediaType.APPLICATION_JSON).content(body).exchange();
    }

}

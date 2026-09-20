package com.trieu.tripplanner.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.trieu.tripplanner.config.SecurityConfig;
import com.trieu.tripplanner.dto.request.RegisterRequest;
import com.trieu.tripplanner.dto.response.UserResponse;
import com.trieu.tripplanner.exception.EmailAlreadyExistsException;
import com.trieu.tripplanner.model.enums.Plan;
import com.trieu.tripplanner.model.enums.Role;
import com.trieu.tripplanner.model.enums.UserStatus;
import com.trieu.tripplanner.service.AuthService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Web slice: real controller, validation, exception handler and security rules; the service is a mock.
 * No @WithMockUser anywhere: /api/v1/auth/** must work without a token.
 */
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    private static final String REGISTER_URL = "/api/v1/auth/register";

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void registerReturns201WithUserInsideEnvelopeAndNeverEchoesPassword() {
        UserResponse created = new UserResponse(1L, "an@example.com", "Nguyễn An", null,
                "Asia/Ho_Chi_Minh", "vi", Role.USER, Plan.FREE, null, false, UserStatus.ACTIVE,
                Instant.parse("2026-09-20T10:00:00Z"));
        when(authService.register(any(RegisterRequest.class))).thenReturn(created);

        MvcTestResult result = mvc.post().uri(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "an@example.com", "password": "MatKhau123", "confirmPassword": "MatKhau123", "fullName": "Nguyễn An"}
                        """)
                .exchange();

        assertThat(result)
                .hasStatus(HttpStatus.CREATED)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "success": true,
                          "message": "OK",
                          "data": {
                            "id": 1,
                            "email": "an@example.com",
                            "fullName": "Nguyễn An",
                            "role": "USER",
                            "plan": "FREE",
                            "emailVerified": false,
                            "status": "ACTIVE",
                            "createdAt": "2026-09-20T10:00:00Z"
                          }
                        }
                        """);
        assertThat(result).body().asString()
                .doesNotContain("MatKhau123")
                .doesNotContain("passwordHash");
    }

    @Test
    void invalidBodyReturns400WithOneDetailPerViolationAndSkipsService() {
        MvcTestResult result = mvc.post().uri(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "khong-phai-email", "password": "yeu", "confirmPassword": "yeu", "fullName": ""}
                        """)
                .exchange();

        assertThat(result)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "success": false,
                          "errorCode": "VALIDATION_ERROR",
                          "message": "Dữ liệu không hợp lệ",
                          "path": "/api/v1/auth/register"
                        }
                        """);
        // "yeu" breaks both @Size and @Pattern, so password appears twice; messages come from messages.properties
        assertThat(result).bodyJson().extractingPath("$.details[*].field").asArray()
                .containsExactly("email", "fullName", "password", "password");
        assertThat(result).bodyJson().extractingPath("$.details[*].message").asArray()
                .contains("Email không đúng định dạng", "Họ tên không được để trống",
                        "Mật khẩu phải dài từ 8 đến 72 ký tự");

        verifyNoInteractions(authService);
    }

    @Test
    void mismatchedConfirmPasswordReturns400OnConfirmPasswordField() {
        MvcTestResult result = mvc.post().uri(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "an@example.com", "password": "MatKhau123", "confirmPassword": "MatKhau124", "fullName": "An"}
                        """)
                .exchange();

        assertThat(result)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "errorCode": "VALIDATION_ERROR",
                          "details": [ { "field": "confirmPassword", "message": "Mật khẩu xác nhận không khớp" } ]
                        }
                        """);
        verifyNoInteractions(authService);
    }

    @Test
    void missingConfirmPasswordReturns400WithoutMismatchNoise() {
        MvcTestResult result = mvc.post().uri(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "an@example.com", "password": "MatKhau123", "fullName": "An"}
                        """)
                .exchange();

        // Only @NotBlank fires; the class-level check stays quiet when a side is null
        assertThat(result)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "details": [ { "field": "confirmPassword", "message": "Vui lòng nhập lại mật khẩu" } ]
                        }
                        """);
    }

    @Test
    void duplicateEmailReturns409WithErrorCode() {
        when(authService.register(any(RegisterRequest.class))).thenThrow(new EmailAlreadyExistsException());

        assertThat(mvc.post().uri(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "dup@example.com", "password": "MatKhau123", "confirmPassword": "MatKhau123", "fullName": "Dup"}
                        """))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "success": false,
                          "errorCode": "EMAIL_ALREADY_EXISTS",
                          "message": "Email đã được sử dụng"
                        }
                        """);
    }

}

package com.trieu.tripplanner.controller;

import com.trieu.tripplanner.common.ApiResponse;
import com.trieu.tripplanner.dto.internal.AuthTokens;
import com.trieu.tripplanner.dto.internal.ClientInfo;
import com.trieu.tripplanner.dto.request.LoginRequest;
import com.trieu.tripplanner.dto.request.RegisterRequest;
import com.trieu.tripplanner.dto.request.ResendVerificationRequest;
import com.trieu.tripplanner.dto.request.VerifyEmailRequest;
import com.trieu.tripplanner.dto.response.AuthResponse;
import com.trieu.tripplanner.dto.response.UserResponse;
import com.trieu.tripplanner.security.RefreshTokenCookies;
import com.trieu.tripplanner.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Auth endpoints (design.md 10.2). Whitelisted in SecurityConfig except /logout, which needs a valid access token.
 * The controller's only extra job is moving the refresh token between the service and the cookie.
 */
@Tag(name = "Auth", description = "Đăng ký, đăng nhập, quản lý phiên")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookies refreshTokenCookies;

    @Operation(summary = "Đăng ký tài khoản",
               description = "Tạo tài khoản FREE chưa xác thực email. Trả 409 EMAIL_ALREADY_EXISTS nếu email đã dùng.")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }

    @Operation(summary = "Đăng nhập",
               description = "Trả access token (15 phút) trong body và đặt cookie httpOnly refresh_token (7 ngày). "
                       + "401 INVALID_CREDENTIALS, 403 ACCOUNT_BLOCKED / EMAIL_NOT_VERIFIED.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request,
                                                           HttpServletRequest httpRequest) {
        AuthTokens tokens = authService.login(request, ClientInfo.from(httpRequest));
        return withCookie(refreshTokenCookies.create(tokens.refreshToken()), tokens.response());
    }

    @Operation(summary = "Xoay token",
               description = "Đọc cookie refresh_token, thu hồi nó và phát cặp token mới. Dùng lại token cũ → 401 và mọi phiên bị thu hồi.")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @CookieValue(name = RefreshTokenCookies.NAME, required = false) String refreshToken,
            HttpServletRequest httpRequest) {
        AuthTokens tokens = authService.refresh(refreshToken, ClientInfo.from(httpRequest));
        return withCookie(refreshTokenCookies.create(tokens.refreshToken()), tokens.response());
    }

    @Operation(summary = "Đăng xuất", description = "Thu hồi refresh token trong cookie và xoá cookie. Cần access token.")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = RefreshTokenCookies.NAME, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return withCookie(refreshTokenCookies.clear(), null);
    }

    @Operation(summary = "Xác thực email",
               description = "Nhận token từ link trong mail. 400 INVALID_TOKEN nếu token sai, hết hạn hoặc đã dùng.")
    @PostMapping("/verify-email")
    public ApiResponse<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request.token());
        return ApiResponse.ok(null);
    }

    @Operation(summary = "Gửi lại mail xác thực",
               description = "Luôn trả 200 dù email có tồn tại hay không. Chỉ gửi khi tài khoản tồn tại và chưa xác thực.")
    @PostMapping("/resend-verification")
    public ApiResponse<Void> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        authService.resendVerification(request.email());
        return ApiResponse.ok(null);
    }

    private static <T> ResponseEntity<ApiResponse<T>> withCookie(ResponseCookie cookie, T body) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.ok(body));
    }

}

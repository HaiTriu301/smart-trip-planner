package com.trieu.tripplanner.controller;

import com.trieu.tripplanner.common.ApiResponse;
import com.trieu.tripplanner.dto.response.UserResponse;
import com.trieu.tripplanner.security.CustomUserDetails;
import com.trieu.tripplanner.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Current-user endpoints (design.md 10.2 "User"). Not in PUBLIC_PATHS, so a valid access token is required
 * and {@code @AuthenticationPrincipal} is always populated by JwtAuthenticationFilter.
 */
@Tag(name = "User", description = "Hồ sơ người dùng hiện tại")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "Thông tin tài khoản đang đăng nhập")
    @GetMapping("/me")
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal CustomUserDetails principal) {
        // The id comes from the verified token, never from the request (CLAUDE.md rule 16)
        return ApiResponse.ok(userService.getProfile(principal.getId()));
    }

}

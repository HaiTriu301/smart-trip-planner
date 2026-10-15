package com.trieu.tripplanner.controller;

import com.trieu.tripplanner.common.ApiResponse;
import com.trieu.tripplanner.dto.response.PublicTripResponse;
import com.trieu.tripplanner.service.PublicTripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Pages anybody can open without an account (design.md 10.2, rule 14.26). Listed in SecurityConfig.PUBLIC_PATHS
 * and not guarded by {@code @tripPermission}: the token of the share link is the only key.
 */
@Tag(name = "Public", description = "Trang không cần đăng nhập")
@RestController
@RequestMapping("/api/v1/public/trips")
@RequiredArgsConstructor
public class PublicTripController {

    private final PublicTripService publicTripService;

    @Operation(summary = "Xem chuyến đi qua liên kết chia sẻ",
               description = "Không cần đăng nhập. Trả tên, mô tả, điểm đến, ngày đi / về và lịch trình từng ngày "
                       + "(hoạt động, giờ, ghi chú của hoạt động, địa điểm). Không có thông tin của chủ hay thành "
                       + "viên, chi phí, link đặt chỗ, ghi chú của ngày. Token sai dạng, không tồn tại, liên kết đã "
                       + "thu hồi hoặc hết hạn, chuyến đi đã xoá: cùng một 404 RESOURCE_NOT_FOUND.")
    // No lock icon in Swagger: the global bearer requirement does not apply here
    @SecurityRequirements
    @GetMapping("/{token}")
    public ApiResponse<PublicTripResponse> get(@PathVariable String token) {
        return ApiResponse.ok(publicTripService.open(token));
    }

}

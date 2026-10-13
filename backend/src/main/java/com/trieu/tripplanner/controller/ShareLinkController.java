package com.trieu.tripplanner.controller;

import com.trieu.tripplanner.common.ApiResponse;
import com.trieu.tripplanner.dto.request.CreateShareLinkRequest;
import com.trieu.tripplanner.dto.response.ShareLinkResponse;
import com.trieu.tripplanner.service.SharingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Share links of a trip (design.md 10.2 "Sharing", rule 14.26). Everything here is owner-only, guarded by
 * {@code tripPermission.isOwner} (CLAUDE.md rule 15): 403 when the trip exists but the caller does not own it,
 * 404 from the service when it does not exist. Opening a link is not here: that is the public page, which needs
 * no account.
 */
// Same Swagger group as TripMemberController, which carries the description of the tag
@Tag(name = "Sharing")
@RestController
@RequestMapping("/api/v1/trips/{tripId}/share-links")
@RequiredArgsConstructor
public class ShareLinkController {

    private final SharingService sharingService;

    @Operation(summary = "Tạo liên kết chia sẻ",
               description = "Chỉ chủ chuyến đi. Tạo một liên kết chỉ xem: ai cầm liên kết cũng xem được lịch trình "
                       + "mà không cần tài khoản. Giao diện ghép địa chỉ {origin}/share/{token} từ token trả về. Một "
                       + "chuyến đi có thể có nhiều liên kết. Chuyến đi chuyển sang visibility LINK. Body không bắt "
                       + "buộc: không gửi body hoặc không có expiresAt thì liên kết không hết hạn; expiresAt (giờ "
                       + "UTC, ví dụ 2026-11-01T16:59:59Z) phải ở tương lai, nếu không 400 ở field expiresAt. 403 "
                       + "nếu không phải chủ, 404 nếu chuyến đi không tồn tại.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@tripPermission.isOwner(#tripId, principal)")
    public ApiResponse<ShareLinkResponse> create(@PathVariable Long tripId,
                                                 @Valid @RequestBody(required = false) CreateShareLinkRequest request) {
        // No body → null: the service treats it like a body without expiresAt
        return ApiResponse.ok(sharingService.createLink(tripId, request));
    }

    @Operation(summary = "Danh sách liên kết chia sẻ",
               description = "Chỉ chủ chuyến đi. Các liên kết chưa thu hồi, mới nhất đứng đầu. Liên kết đã hết hạn "
                       + "vẫn hiện với expired = true để chủ biết vì sao nó không mở được nữa. Chuyến đi chưa có "
                       + "liên kết thì trả mảng rỗng. 403 nếu không phải chủ, 404 nếu chuyến đi không tồn tại.")
    @GetMapping
    @PreAuthorize("@tripPermission.isOwner(#tripId, principal)")
    public ApiResponse<List<ShareLinkResponse>> list(@PathVariable Long tripId) {
        return ApiResponse.ok(sharingService.listLinks(tripId));
    }

}

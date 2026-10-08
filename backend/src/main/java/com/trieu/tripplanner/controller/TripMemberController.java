package com.trieu.tripplanner.controller;

import com.trieu.tripplanner.common.ApiResponse;
import com.trieu.tripplanner.dto.request.InviteMemberRequest;
import com.trieu.tripplanner.dto.response.MemberResponse;
import com.trieu.tripplanner.service.SharingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Members and invitations of a trip (design.md 10.2 "Sharing"). Managing members is owner-only, guarded by
 * {@code tripPermission.isOwner} (CLAUDE.md rule 15): 403 when the trip exists but the caller does not own it,
 * 404 from the service when it does not exist.
 */
@Tag(name = "Sharing", description = "Thành viên và lời mời của chuyến đi")
@RestController
@RequestMapping("/api/v1/trips/{tripId}/members")
@RequiredArgsConstructor
public class TripMemberController {

    private final SharingService sharingService;

    @Operation(summary = "Mời thành viên theo email",
               description = "Chỉ chủ chuyến đi. Gửi mail có link nhận lời, hiệu lực 7 ngày; người được mời có thể "
                       + "chưa có tài khoản. Vai trò EDITOR (cùng sửa) hoặc VIEWER (chỉ xem). 400 nếu email là "
                       + "của chính chủ chuyến đi. 403 nếu không phải chủ, 404 nếu chuyến đi không tồn tại.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@tripPermission.isOwner(#tripId, principal)")
    public ApiResponse<MemberResponse> invite(@PathVariable Long tripId,
                                              @Valid @RequestBody InviteMemberRequest request) {
        return ApiResponse.ok(sharingService.invite(tripId, request));
    }

}

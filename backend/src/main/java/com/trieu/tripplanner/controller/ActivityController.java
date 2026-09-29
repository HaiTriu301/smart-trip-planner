package com.trieu.tripplanner.controller;

import com.trieu.tripplanner.common.ApiResponse;
import com.trieu.tripplanner.dto.request.CreateActivityRequest;
import com.trieu.tripplanner.dto.request.UpdateActivityRequest;
import com.trieu.tripplanner.dto.response.ActivityResponse;
import com.trieu.tripplanner.security.CustomUserDetails;
import com.trieu.tripplanner.service.ActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Activities of a trip (design.md 10.2 "Itinerary"). Guarded by {@code tripPermission} on the trip of the URL
 * (CLAUDE.md rule 15); the service answers 404 when the day does not belong to that trip.
 */
@Tag(name = "Activity", description = "Hoạt động trong từng ngày của chuyến đi")
@RestController
@RequestMapping("/api/v1/trips/{tripId}")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @Operation(summary = "Danh sách hoạt động của một ngày",
               description = "Sắp theo thứ tự hiển thị (orderIndex). 403 nếu không có quyền xem, 404 nếu chuyến đi "
                       + "không tồn tại hoặc ngày không thuộc chuyến đi.")
    @GetMapping("/days/{dayId}/activities")
    @PreAuthorize("@tripPermission.canView(#tripId, principal)")
    public ApiResponse<List<ActivityResponse>> list(@PathVariable Long tripId, @PathVariable Long dayId) {
        return ApiResponse.ok(activityService.list(tripId, dayId));
    }

    @Operation(summary = "Thêm hoạt động vào một ngày",
               description = "Hoạt động mới nằm cuối ngày. Giờ dạng HH:mm, được để trống. Có giờ kết thúc thì phải có "
                       + "giờ bắt đầu và kết thúc phải sau bắt đầu. Có chi phí mà không gửi currency thì lấy tiền tệ "
                       + "của chuyến đi. 404 nếu ngày không thuộc chuyến đi. 409 ACTIVITY_TIME_CONFLICT nếu trùng giờ "
                       + "với hoạt động khác trong ngày; gửi lại kèm allowOverlap=true để vẫn thêm.")
    @PostMapping("/days/{dayId}/activities")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@tripPermission.canEdit(#tripId, principal)")
    public ApiResponse<ActivityResponse> create(@PathVariable Long tripId, @PathVariable Long dayId,
                                                @AuthenticationPrincipal CustomUserDetails principal,
                                                @RequestParam(defaultValue = "false") boolean allowOverlap,
                                                @Valid @RequestBody CreateActivityRequest request) {
        return ApiResponse.ok(activityService.create(tripId, dayId, principal.getId(), request, allowOverlap));
    }

    @Operation(summary = "Sửa hoạt động",
               description = "Chỉ gửi field cần đổi (field null giữ nguyên). note và bookingUrl gửi chuỗi rỗng để xoá. "
                       + "Không đổi ngày và thứ tự ở đây. Đổi giờ làm trùng hoạt động khác: 409 "
                       + "ACTIVITY_TIME_CONFLICT, gửi lại kèm allowOverlap=true để vẫn lưu. 404 nếu hoạt động không "
                       + "thuộc chuyến đi.")
    @PatchMapping("/activities/{activityId}")
    @PreAuthorize("@tripPermission.canEdit(#tripId, principal)")
    public ApiResponse<ActivityResponse> update(@PathVariable Long tripId, @PathVariable Long activityId,
                                                @RequestParam(defaultValue = "false") boolean allowOverlap,
                                                @Valid @RequestBody UpdateActivityRequest request) {
        return ApiResponse.ok(activityService.update(tripId, activityId, request, allowOverlap));
    }

}

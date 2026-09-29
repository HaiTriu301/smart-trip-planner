package com.trieu.tripplanner.controller;

import com.trieu.tripplanner.common.ApiResponse;
import com.trieu.tripplanner.dto.request.CreateActivityRequest;
import com.trieu.tripplanner.dto.response.ActivityResponse;
import com.trieu.tripplanner.security.CustomUserDetails;
import com.trieu.tripplanner.service.ActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @Operation(summary = "Thêm hoạt động vào một ngày",
               description = "Hoạt động mới nằm cuối ngày. Giờ dạng HH:mm, được để trống. Có giờ kết thúc thì phải có "
                       + "giờ bắt đầu và kết thúc phải sau bắt đầu. Có chi phí mà không gửi currency thì lấy tiền tệ "
                       + "của chuyến đi. 404 nếu ngày không thuộc chuyến đi.")
    @PostMapping("/days/{dayId}/activities")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@tripPermission.canEdit(#tripId, principal)")
    public ApiResponse<ActivityResponse> create(@PathVariable Long tripId, @PathVariable Long dayId,
                                                @AuthenticationPrincipal CustomUserDetails principal,
                                                @Valid @RequestBody CreateActivityRequest request) {
        return ApiResponse.ok(activityService.create(tripId, dayId, principal.getId(), request));
    }

}

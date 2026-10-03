package com.trieu.tripplanner.controller;

import com.trieu.tripplanner.common.ApiResponse;
import com.trieu.tripplanner.dto.request.UpdateTripDayRequest;
import com.trieu.tripplanner.dto.response.DayRouteResponse;
import com.trieu.tripplanner.dto.response.TripDayResponse;
import com.trieu.tripplanner.service.RouteService;
import com.trieu.tripplanner.service.TripDayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Days of a trip (design.md 10.2 "Itinerary"). Days are created and removed only through the trip's dates;
 * this controller reads them, edits their title / note and gives the travel of a day. Guarded by {@code tripPermission} (CLAUDE.md rule 15).
 */
@Tag(name = "Trip day", description = "Các ngày trong lịch trình của chuyến đi")
@RestController
@RequestMapping("/api/v1/trips/{tripId}/days")
@RequiredArgsConstructor
public class TripDayController {

    private final TripDayService tripDayService;
    private final RouteService routeService;

    @Operation(summary = "Danh sách ngày của chuyến đi",
               description = "Sắp theo ngày (Ngày 1, 2, 3...). 403 nếu không có quyền xem, 404 nếu chuyến đi không tồn tại.")
    @GetMapping
    @PreAuthorize("@tripPermission.canView(#tripId, principal)")
    public ApiResponse<List<TripDayResponse>> list(@PathVariable Long tripId) {
        return ApiResponse.ok(tripDayService.list(tripId));
    }

    @Operation(summary = "Sửa tiêu đề / ghi chú của một ngày",
               description = "Field không gửi hoặc null: giữ nguyên. Chuỗi rỗng: xoá. Ngày và số thứ tự không sửa ở đây "
                       + "(đi theo ngày của chuyến đi). 404 nếu ngày không thuộc chuyến đi.")
    @PatchMapping("/{dayId}")
    @PreAuthorize("@tripPermission.canEdit(#tripId, principal)")
    public ApiResponse<TripDayResponse> update(@PathVariable Long tripId, @PathVariable Long dayId,
                                               @Valid @RequestBody UpdateTripDayRequest request) {
        return ApiResponse.ok(tripDayService.update(tripId, dayId, request));
    }

    @Operation(summary = "Quãng đường di chuyển trong một ngày",
               description = "Mỗi chặng nối hai hoạt động liền nhau theo đúng thứ tự của ngày: quãng đường (mét) và "
                       + "thời gian (giây), kèm tổng cả ngày. Con số là ước lượng cho một phương tiện. "
                       + "403 nếu không có quyền xem, 404 nếu chuyến đi không tồn tại hoặc ngày không thuộc chuyến đi.")
    @GetMapping("/{dayId}/route")
    @PreAuthorize("@tripPermission.canView(#tripId, principal)")
    public ApiResponse<DayRouteResponse> route(@PathVariable Long tripId, @PathVariable Long dayId) {
        return ApiResponse.ok(routeService.forDay(tripId, dayId));
    }

}

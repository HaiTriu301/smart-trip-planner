package com.trieu.tripplanner.controller;

import com.trieu.tripplanner.common.ApiResponse;
import com.trieu.tripplanner.dto.response.TripDayResponse;
import com.trieu.tripplanner.service.TripDayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Days of a trip (design.md 10.2 "Itinerary"). Days are created and removed only through the trip's dates;
 * this controller reads them and edits their title / note. Guarded by {@code tripPermission} (CLAUDE.md rule 15).
 */
@Tag(name = "Trip day", description = "Các ngày trong lịch trình của chuyến đi")
@RestController
@RequestMapping("/api/v1/trips/{tripId}/days")
@RequiredArgsConstructor
public class TripDayController {

    private final TripDayService tripDayService;

    @Operation(summary = "Danh sách ngày của chuyến đi",
               description = "Sắp theo ngày (Ngày 1, 2, 3...). 403 nếu không có quyền xem, 404 nếu chuyến đi không tồn tại.")
    @GetMapping
    @PreAuthorize("@tripPermission.canView(#tripId, principal)")
    public ApiResponse<List<TripDayResponse>> list(@PathVariable Long tripId) {
        return ApiResponse.ok(tripDayService.list(tripId));
    }

}

package com.trieu.tripplanner.controller;

import com.trieu.tripplanner.common.ApiResponse;
import com.trieu.tripplanner.dto.response.TripWeatherResponse;
import com.trieu.tripplanner.service.WeatherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Weather (design.md 10.2 "Weather"). The forecast belongs to a trip, so the endpoint is guarded by
 * {@code tripPermission} like everything else inside a trip (CLAUDE.md rule 15).
 */
@Tag(name = "Weather", description = "Dự báo thời tiết của chuyến đi")
@RestController
@RequestMapping("/api/v1/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;

    @Operation(summary = "Dự báo thời tiết từng ngày của chuyến đi",
               description = "Mỗi ngày của chuyến đi một phần tử, sắp theo ngày. forecast gồm condition (CLEAR, "
                       + "PARTLY_CLOUDY, CLOUDY, FOG, RAIN, THUNDERSTORM, SNOW), tempMin và tempMax (độ C) và "
                       + "precipitationProbability (0 đến 100). Dự báo lấy theo toạ độ điểm đến của chuyến đi. "
                       + "status là OK, hoặc NO_DESTINATION khi chuyến đi chưa có toạ độ điểm đến: vẫn 200, forecast của "
                       + "mọi ngày là null. 403 nếu không có quyền xem, 404 nếu chuyến đi không tồn tại.")
    @GetMapping("/trips/{tripId}")
    @PreAuthorize("@tripPermission.canView(#tripId, principal)")
    public ApiResponse<TripWeatherResponse> forTrip(@PathVariable Long tripId) {
        return ApiResponse.ok(weatherService.forTrip(tripId));
    }

}

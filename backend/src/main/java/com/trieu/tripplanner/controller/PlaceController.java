package com.trieu.tripplanner.controller;

import com.trieu.tripplanner.common.ApiResponse;
import com.trieu.tripplanner.dto.response.PlaceResultResponse;
import com.trieu.tripplanner.service.PlaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Places (design.md 10.2 "Place"). Any signed-in user may search; nothing here belongs to a trip, so there is
 * no {@code tripPermission} check.
 */
@Tag(name = "Place", description = "Tìm địa điểm để gắn vào hoạt động")
@RestController
@RequestMapping("/api/v1/places")
@RequiredArgsConstructor
public class PlaceController {

    static final int MAX_QUERY_LENGTH = 100;
    static final int MAX_LIMIT = 20;

    private final PlaceService placeService;

    @Operation(summary = "Tìm địa điểm theo tên hoặc địa chỉ",
               description = "Không phân biệt hoa thường và dấu tiếng Việt: \"linh ung\" tìm ra \"Chùa Linh Ứng\". "
                       + "q từ 2 đến 100 ký tự; limit từ 1 đến 20, mặc định 8. Gửi lat và lng (đủ cả hai) để địa điểm "
                       + "quanh toạ độ đó, trong bán kính 50 km, đứng trước. Không có kết quả: 200 với mảng rỗng. "
                       + "Kết quả chưa có id: địa điểm chỉ được lưu khi người dùng chọn nó.")
    @GetMapping("/search")
    public ApiResponse<List<PlaceResultResponse>> search(
            // At least two characters once the spaces around the keyword are dropped
            @RequestParam
            @Pattern(regexp = "(?s)\\s*\\S.*\\S\\s*", message = "{validation.place.query.too-short}")
            @Size(max = MAX_QUERY_LENGTH, message = "{validation.place.query.too-long}")
            String q,
            @RequestParam(defaultValue = "8")
            @Min(value = 1, message = "{validation.place.limit.range}")
            @Max(value = MAX_LIMIT, message = "{validation.place.limit.range}")
            int limit,
            @RequestParam(required = false)
            @DecimalMin(value = "-90", message = "{validation.place.latitude.range}")
            @DecimalMax(value = "90", message = "{validation.place.latitude.range}")
            BigDecimal lat,
            @RequestParam(required = false)
            @DecimalMin(value = "-180", message = "{validation.place.longitude.range}")
            @DecimalMax(value = "180", message = "{validation.place.longitude.range}")
            BigDecimal lng) {
        return ApiResponse.ok(placeService.search(q, limit, lat, lng));
    }

}

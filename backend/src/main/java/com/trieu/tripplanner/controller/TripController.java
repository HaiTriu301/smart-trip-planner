package com.trieu.tripplanner.controller;

import com.trieu.tripplanner.common.ApiResponse;
import com.trieu.tripplanner.common.PageResponse;
import com.trieu.tripplanner.dto.internal.TripFilter;
import com.trieu.tripplanner.dto.request.CreateTripRequest;
import com.trieu.tripplanner.dto.request.UpdateTripRequest;
import com.trieu.tripplanner.dto.request.UpdateTripStatusRequest;
import com.trieu.tripplanner.dto.response.TripDetailResponse;
import com.trieu.tripplanner.dto.response.TripResponse;
import com.trieu.tripplanner.dto.response.TripStatusCountsResponse;
import com.trieu.tripplanner.dto.response.TripSummaryResponse;
import com.trieu.tripplanner.model.enums.TripStatus;
import com.trieu.tripplanner.security.CustomUserDetails;
import com.trieu.tripplanner.service.TripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * Trip CRUD (design.md 10.2). Every endpoint on one trip is guarded by the {@code tripPermission} bean
 * (CLAUDE.md rule 15): 403 when the trip exists but is not yours, 404 from the service when it does not exist.
 */
@Tag(name = "Trip", description = "Tạo, xem, sửa, xoá chuyến đi")
@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    @Operation(summary = "Danh sách chuyến đi của tôi và được chia sẻ với tôi",
               description = "Chuyến đi tôi tạo và chuyến đi tôi là thành viên đã nhận lời, chung một danh sách; "
                       + "ownerId và ownerName cho biết chủ (khác tôi thì là chuyến được chia sẻ). "
                       + "Lọc theo status, q (tên hoặc điểm đến), from/to (khoảng ngày giao nhau). "
                       + "Mặc định size=20 (tối đa 100), sort=createdAt,desc; sort được: createdAt, updatedAt, startDate, title.")
    @GetMapping
    public ApiResponse<PageResponse<TripSummaryResponse>> list(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(required = false) TripStatus status,
            @RequestParam(required = false) @Size(max = 200, message = "{validation.trip.query.too-long}") String q,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ApiResponse.ok(tripService.list(principal.getId(), new TripFilter(status, q, from, to), pageable));
    }

    @Operation(summary = "Số chuyến đi theo trạng thái",
               description = "Đếm chuyến đi của tôi và được chia sẻ với tôi theo từng trạng thái cho các chip lọc, "
                       + "có tính từ khoá q giống GET /trips. Đủ 5 trạng thái, trạng thái không có chuyến nào trả 0.")
    @GetMapping("/status-counts")
    public ApiResponse<TripStatusCountsResponse> statusCounts(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(required = false) @Size(max = 200, message = "{validation.trip.query.too-long}") String q) {
        return ApiResponse.ok(tripService.countByStatus(principal.getId(), q));
    }

    @Operation(summary = "Tạo chuyến đi",
               description = "Trạng thái ban đầu DRAFT. 400 nếu ngày kết thúc trước ngày bắt đầu hoặc dài hơn 60 ngày.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TripResponse> create(@AuthenticationPrincipal CustomUserDetails principal,
                                            @Valid @RequestBody CreateTripRequest request) {
        return ApiResponse.ok(tripService.create(principal.getId(), request));
    }

    @Operation(summary = "Chi tiết chuyến đi",
               description = "Kèm danh sách ngày theo thứ tự. 403 nếu không có quyền xem, 404 nếu không tồn tại.")
    @GetMapping("/{id}")
    @PreAuthorize("@tripPermission.canView(#id, principal)")
    public ApiResponse<TripDetailResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(tripService.get(id));
    }

    @Operation(summary = "Sửa chuyến đi",
               description = "Chỉ gửi field cần đổi (field null giữ nguyên). Không đổi status ở đây. Đổi ngày làm cắt "
                       + "ngày đang có hoạt động: 409 TRIP_DAY_HAS_ACTIVITIES, không lưu gì; gửi lại kèm force=true "
                       + "để cắt ngày và xoá hoạt động của ngày đó.")
    @PatchMapping("/{id}")
    @PreAuthorize("@tripPermission.canEdit(#id, principal)")
    public ApiResponse<TripResponse> update(@PathVariable Long id,
                                            @RequestParam(defaultValue = "false") boolean force,
                                            @Valid @RequestBody UpdateTripRequest request) {
        return ApiResponse.ok(tripService.update(id, request, force));
    }

    @Operation(summary = "Đổi trạng thái chuyến đi",
               description = "Chuyển tự do giữa DRAFT, PLANNED, ONGOING, COMPLETED, ARCHIVED. Gửi lại trạng thái "
                       + "đang có vẫn trả 200. Cách duy nhất để đổi trạng thái.")
    @PatchMapping("/{id}/status")
    @PreAuthorize("@tripPermission.canEdit(#id, principal)")
    public ApiResponse<TripResponse> updateStatus(@PathVariable Long id,
                                                  @Valid @RequestBody UpdateTripStatusRequest request) {
        return ApiResponse.ok(tripService.updateStatus(id, request.status()));
    }

    @Operation(summary = "Xoá chuyến đi", description = "Xoá mềm. Chỉ chủ sở hữu.")
    @DeleteMapping("/{id}")
    @PreAuthorize("@tripPermission.isOwner(#id, principal)")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        tripService.delete(id);
        return ApiResponse.ok(null);
    }

}

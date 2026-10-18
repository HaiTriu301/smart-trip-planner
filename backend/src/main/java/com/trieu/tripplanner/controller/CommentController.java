package com.trieu.tripplanner.controller;

import com.trieu.tripplanner.common.ApiResponse;
import com.trieu.tripplanner.dto.request.CreateCommentRequest;
import com.trieu.tripplanner.dto.response.CommentResponse;
import com.trieu.tripplanner.security.CustomUserDetails;
import com.trieu.tripplanner.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Comments of a trip (design.md 10.2 "Comment", rule 27). Whoever may view the trip may read and post, guarded by
 * {@code tripPermission.canView} (CLAUDE.md rule 15): 403 when the trip exists but the caller may not see it, 404
 * from the service when it does not exist.
 */
@Tag(name = "Comment", description = "Bình luận trên chuyến đi và hoạt động")
@RestController
@RequestMapping("/api/v1/trips/{tripId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "Danh sách bình luận",
               description = "Mọi bình luận còn tồn tại của chuyến đi (cho cả chuyến, về từng hoạt động, và các câu "
                       + "trả lời) trong một danh sách phẳng, cũ trước; cùng thời điểm thì theo mã. Câu trả lời có "
                       + "parentId để giao diện lồng một cấp. Không phân trang. Ai xem được chuyến đi thì xem được. "
                       + "403 nếu không xem được chuyến đi, 404 nếu chuyến đi không tồn tại.")
    @GetMapping
    @PreAuthorize("@tripPermission.canView(#tripId, principal)")
    public ApiResponse<List<CommentResponse>> list(@PathVariable Long tripId) {
        return ApiResponse.ok(commentService.list(tripId));
    }

    @Operation(summary = "Đăng bình luận",
               description = "Ai xem được chuyến đi thì đăng được (kể cả người chỉ xem). content: 1 đến 2000 ký tự, "
                       + "chữ thuần (không HTML), giữ xuống dòng, khoảng trắng hai đầu bị bỏ; trống hoặc dài hơn: "
                       + "400 ở field content. activityId (không bắt buộc): bình luận về một hoạt động của chuyến "
                       + "đi này; bỏ trống là bình luận cho cả chuyến. parentId (không bắt buộc): trả lời một bình "
                       + "luận gốc còn tồn tại của chuyến đi này, chỉ một cấp; cha không hợp lệ: 400 ở field "
                       + "parentId. Trả lời nằm ở hoạt động của cha, activityId gửi kèm bị bỏ qua. Tác giả là người "
                       + "đang đăng nhập. 403 nếu không xem được chuyến đi, 404 nếu chuyến đi không tồn tại hoặc "
                       + "hoạt động không thuộc chuyến đi.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@tripPermission.canView(#tripId, principal)")
    public ApiResponse<CommentResponse> create(@PathVariable Long tripId,
                                               @AuthenticationPrincipal CustomUserDetails principal,
                                               @Valid @RequestBody CreateCommentRequest request) {
        return ApiResponse.ok(commentService.create(tripId, principal.getId(), request));
    }

}

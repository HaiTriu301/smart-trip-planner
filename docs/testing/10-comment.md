# 10 · Bình luận

> Cập nhật: 2026-10-18 · **Task 4.4 xong**: 9 commit từ `4297fde` đến `2b35eb5` trên nhánh `feat/T4.4-comments`, gộp vào `main` qua pull request, build xanh 1485 lượt test, không có lỗi, kiểm tra thủ công `MT-COMMENT-01` đến `MT-COMMENT-07` chưa chạy · Mốc 8 (test toàn luồng của bình luận) tại `2b35eb5` (1485 lượt test) · Mốc 7 (xoá bình luận) tại `a52585e` (1481 lượt test) · Mốc 6 (lọc bình luận theo hoạt động) tại `a333a36` (1455 lượt test) · Mốc 5 (xem danh sách bình luận) tại `e9b1f1b` (1448 lượt test) · Mốc 4 (trả lời một bình luận) tại `c1f5e3a` (1425 lượt test) · Mốc 3 (bình luận về một hoạt động) tại `f76e6bd` (1415 lượt test) · commit 3a (gom chỗ dựng yêu cầu bình luận trong test về một hàm, không đổi hành vi) tại `c553200` (1410 lượt test) · Mốc 2 (đăng bình luận vào chuyến đi) tại `c961d35` (1410 lượt test) · Mốc 1 (bảng lưu bình luận) tại `4297fde` (1385 lượt test) · [Về trang chính](README.md)

Người xem được một chuyến đi thì đọc và viết được bình luận: cho cả chuyến, hoặc cho riêng một hoạt động. Có thể trả lời một bình luận, nhưng chỉ một cấp (không trả lời một câu trả lời). Bình luận không sửa được. Tác giả xoá được bình luận của mình, chủ chuyến xoá được mọi bình luận; xoá một bình luận gốc thì các câu trả lời của nó mất theo. Phần máy chủ làm ở Task 4.4, giao diện ở Task 4.6.

File này được ghi dần theo từng mốc của Task 4.4:

| Mốc | Nội dung | Phần trong file | Mã commit |
|---|---|---|---|
| 1 | Bảng lưu bình luận; bình luận mất theo khi hoạt động, ngày hoặc chuyến đi bị xoá hẳn | A | `4297fde` |
| 2 | Người xem được chuyến đi đăng một bình luận cho cả chuyến (`POST /api/v1/trips/{tripId}/comments`) | B | `c961d35` |
| 3a | Gom các chỗ test tự dựng yêu cầu đăng bình luận về một hàm dùng chung, để thêm trường mới (hoạt động, bình luận cha) không làm vỡ test cũ. Không đổi hành vi, không có kịch bản mới | — | `c553200` |
| 3 | Bình luận về một hoạt động của chuyến đi (trường `activityId` khi đăng) | C | `f76e6bd` |
| 4 | Trả lời một bình luận gốc, chỉ một cấp (trường `parentId` khi đăng) | D | `c1f5e3a` |
| 5 | Xem mọi bình luận của chuyến đi (`GET /api/v1/trips/{tripId}/comments`) | E | `e9b1f1b` |
| 6 | Chỉ xem bình luận của một hoạt động (`?activityId=`) | F | `a333a36` |
| 7 | Xoá bình luận (`DELETE /api/v1/trips/{tripId}/comments/{commentId}`): tác giả hoặc chủ chuyến; xoá gốc thì mất cả câu trả lời | G | `a52585e` |
| 8 | Test toàn luồng: đăng, trả lời, xem, xoá qua mọi tầng với MySQL và Redis thật; đếm số câu SQL của danh sách | H | `2b35eb5` |

## A. Bảng lưu bình luận

> **Yêu cầu:** design.md 5.2 bảng `comments`, rule 27 · **Kiểm bởi:** `CommentMappingTest`

Mốc 1 chỉ tạo chỗ lưu, chưa có API. Các kịch bản kiểm rằng chỗ lưu giữ đúng nội dung, bình luận đã xoá bị ẩn mà không mất dòng, bình luận mất theo khi thứ nó gắn vào bị xoá hẳn, và một bình luận đã viết thì không đổi được.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-COMMENT-001 | Lưu một câu trả lời gắn vào một hoạt động rồi đọc lại | Đọc lại đúng chuyến đi, hoạt động, tác giả, bình luận được trả lời và nội dung; có thời điểm tạo; chưa bị xoá | Đúng | Đạt |
| TC-COMMENT-002 | Lưu một bình luận cho cả chuyến đi | Không gắn hoạt động nào, không trả lời bình luận nào | Đúng | Đạt |
| TC-COMMENT-003 | Lưu một bình luận dài đúng 2000 ký tự, có chữ có dấu, biểu tượng cảm xúc (🌲) và xuống dòng | Đọc lại y nguyên từng ký tự | Biên | Đạt |
| TC-COMMENT-004 | Xoá một bình luận theo cách thông thường của ứng dụng | Bình luận không còn đọc ra được, nhưng dòng vẫn nằm trong cơ sở dữ liệu kèm thời điểm xoá (xoá mềm) | Đúng | Đạt |
| TC-COMMENT-005 | Xoá hẳn một hoạt động có một bình luận và một câu trả lời; chuyến đi có thêm một bình luận chung | Bình luận và câu trả lời của hoạt động mất theo; bình luận chung của chuyến đi còn nguyên | Đúng | Đạt |
| TC-COMMENT-006 | Xoá hẳn một ngày (việc xảy ra khi rút ngắn chuyến đi và chấp nhận mất hoạt động) mà hoạt động trong ngày có bình luận và câu trả lời | Bình luận và câu trả lời mất theo hoạt động | Đúng | Đạt |
| TC-COMMENT-007 | Xoá hẳn một chuyến đi có bình luận chung, bình luận trên hoạt động, và câu trả lời cho cả hai (việc bộ dọn dẹp sẽ làm với chuyến đã xoá mềm quá 30 ngày) | Mọi bình luận mất theo, không lỗi. Đã thử bỏ quy tắc "xoá bình luận gốc thì xoá câu trả lời" khỏi bảng: `TC-COMMENT-005` đến `007` đỏ vì cơ sở dữ liệu từ chối xoá, nên quy tắc này là cần thiết | Đúng | Đạt |
| TC-COMMENT-008 | Xoá hẳn tài khoản của một người còn bình luận | Bị từ chối (nêu tên khoá `fk_comments_user`): tài khoản chỉ được xoá mềm, xoá hẳn không được âm thầm xoá bình luận của họ | Bảo mật | Đạt |
| TC-COMMENT-009 | Sau khi lưu, cố đổi chuyến đi, hoạt động, tác giả, bình luận được trả lời và nội dung của một câu trả lời, rồi lưu lại | Cả năm giá trị không đổi trong cơ sở dữ liệu: bình luận không sửa được | Bảo mật | Đạt |

## B. Đăng bình luận vào chuyến đi

> **Yêu cầu:** design.md 6.2 (dòng "Comment"), 10.2 "Quy ước Comment API", rule 27 · **Kiểm bởi:** `CommentServiceTest`, `CommentControllerTest`, `TripPermissionMatrixIntegrationTest` (dòng "Đăng bình luận")

Mốc 2 làm dạng đơn giản nhất của việc đăng: một bình luận cho cả chuyến đi, chỉ có nội dung. Gắn vào hoạt động và trả lời một bình luận là hai mốc sau.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-COMMENT-010 | Chủ chuyến đi, người cùng sửa và người chỉ xem (đã nhận lời) lần lượt đăng một bình luận | Cả ba đều được (201): ai xem được chuyến đi thì bình luận được | Đúng | Đạt |
| TC-COMMENT-011 | Người lạ đã đăng nhập, người được mời mà chưa nhận lời, người đã bị gỡ khỏi chuyến đăng bình luận; khách chưa đăng nhập cũng thử | Ba người đầu bị từ chối (403 `FORBIDDEN`), khách bị từ chối (401); không bình luận nào được lưu | Bảo mật | Đạt |
| TC-COMMENT-012 | Chủ xoá chuyến đi, rồi chủ, người cùng sửa và người lạ đăng bình luận vào chuyến đó | Cả ba nhận 404 `RESOURCE_NOT_FOUND`: người lạ cũng không biết được chuyến từng tồn tại | Bảo mật | Đạt |
| TC-COMMENT-013 | Đăng bình luận vào một mã chuyến đi không tồn tại | 404 `RESOURCE_NOT_FOUND`, không lưu gì | Sai | Đạt |
| TC-COMMENT-014 | Người chỉ xem đăng "  Nhớ mang áo ấm  " (có khoảng trắng hai đầu) | Lưu "Nhớ mang áo ấm"; tác giả là người đang đăng nhập; bình luận không gắn hoạt động nào và không trả lời bình luận nào | Đúng | Đạt |
| TC-COMMENT-015 | Nội dung có nhiều dòng, có dòng trống ở đầu và cuối | Bỏ khoảng trắng và dòng trống ở hai đầu; các dòng ở giữa giữ nguyên | Đúng | Đạt |
| TC-COMMENT-016 | Gửi kèm `userId` / `authorId` của người khác trong nội dung yêu cầu | Hai trường đó bị bỏ qua: tác giả vẫn là người đang đăng nhập, không mạo danh được | Bảo mật | Đạt |
| TC-COMMENT-017 | Nội dung dài đúng 2000 ký tự | Được (201) | Biên | Đạt |
| TC-COMMENT-018 | Nội dung dài 2001 ký tự | 400 `VALIDATION_ERROR`, ô `content`: "Bình luận không được vượt quá 2000 ký tự" | Biên | Đạt |
| TC-COMMENT-019 | Nội dung là chuỗi rỗng | 400, ô `content`: "Nội dung bình luận không được để trống" | Sai | Đạt |
| TC-COMMENT-020 | Nội dung chỉ có khoảng trắng và xuống dòng | 400, cùng câu báo như chuỗi rỗng | Sai | Đạt |
| TC-COMMENT-021 | Không có trường `content`, hoặc không gửi nội dung yêu cầu nào | 400 `VALIDATION_ERROR` | Sai | Đạt |
| TC-COMMENT-022 | Mã chuyến đi trên đường dẫn không phải số (`/trips/abc/comments`) | 400 `VALIDATION_ERROR`, không tới phần xử lý | Sai | Đạt |
| TC-COMMENT-023 | Đọc câu trả lời sau khi đăng | Có mã bình luận, `activityId` và `parentId` rỗng, tác giả gồm mã, họ tên, ảnh đại diện (không có email), nội dung và thời điểm tạo | Bảo mật | Đạt |

## C. Bình luận về một hoạt động

> **Yêu cầu:** design.md 5.2 bảng `comments` (cột `activity_id`), 10.2 "Quy ước Comment API" · **Kiểm bởi:** `CommentServiceTest`, `CommentControllerTest`

Mốc 3 cho phép gửi kèm mã một hoạt động khi đăng, để bình luận nói về riêng hoạt động đó thay vì cả chuyến. Quyền vẫn như phần B (ai xem được chuyến đi thì được). Quyền chỉ được kiểm trên chuyến đi ghi ở đường dẫn, nên phải chặn việc gắn bình luận vào hoạt động của một chuyến khác.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-COMMENT-024 | Người chỉ xem đăng "Chợ mở lúc mấy giờ?" kèm mã một hoạt động của chuyến đi | Được (201): bình luận gắn đúng hoạt động đó, không trả lời bình luận nào; câu trả lời có `activityId` là mã hoạt động | Đúng | Đạt |
| TC-COMMENT-025 | Người xem được chuyến đi A đăng bình luận vào chuyến A nhưng kèm mã một hoạt động của chuyến B (chuyến họ không xem được) | 404 `RESOURCE_NOT_FOUND`, không lưu gì: không gắn được bình luận vào chuyến khác, và không biết được hoạt động đó có tồn tại hay không | Bảo mật | Đạt |
| TC-COMMENT-026 | Kèm mã một hoạt động không tồn tại | 404 `RESOURCE_NOT_FOUND`, không lưu gì | Sai | Đạt |
| TC-COMMENT-027 | Chuyến đi không tồn tại, có kèm mã hoạt động | 404 về chuyến đi; không tra tới hoạt động, không lưu gì | Sai | Đạt |
| TC-COMMENT-028 | Mã hoạt động không phải số (`"activityId": "abc"`) | 400 `VALIDATION_ERROR`, không tới phần xử lý | Sai | Đạt |

## D. Trả lời một bình luận

> **Yêu cầu:** design.md 5.2 bảng `comments` (cột `parent_id`, "Trả lời chỉ một cấp"), 10.2 "Quy ước Comment API" · **Kiểm bởi:** `CommentServiceTest`, `CommentControllerTest`, `CommentRepositoryTest`

Mốc 4 cho phép gửi kèm mã một bình luận để trả lời nó. Chỉ trả lời được bình luận gốc (bình luận không trả lời ai), còn tồn tại, của cùng chuyến đi; mọi trường hợp khác bị từ chối với cùng một câu báo ở ô `parentId`. Câu trả lời luôn nằm cùng chỗ với bình luận được trả lời (cả chuyến, hoặc cùng hoạt động).

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-COMMENT-029 | Người chỉ xem gửi "7 giờ nhé" kèm `parentId` là mã một bình luận gốc của cả chuyến | Được (201): câu trả lời trỏ tới bình luận gốc, tác giả là người đang đăng nhập, không gắn hoạt động nào; câu trả lời có `parentId` là mã bình luận gốc | Đúng | Đạt |
| TC-COMMENT-030 | Trả lời một bình luận gốc nằm trên hoạt động "Chợ Đà Lạt", gửi kèm mã một hoạt động khác | Câu trả lời nằm ở "Chợ Đà Lạt" như bình luận gốc; mã hoạt động gửi kèm bị bỏ qua, không được tra tới | Đúng | Đạt |
| TC-COMMENT-031 | Trả lời một câu trả lời (thử tạo cấp thứ hai) | 400 `VALIDATION_ERROR`, ô `parentId`: "Chỉ trả lời được một bình luận gốc còn tồn tại của chuyến đi này"; không lưu gì | Sai | Đạt |
| TC-COMMENT-032 | Trả lời một bình luận gốc đã bị xoá | 400, cùng câu báo ở ô `parentId` | Sai | Đạt |
| TC-COMMENT-033 | Đăng vào chuyến đi A, trả lời một bình luận gốc của chuyến đi B | 400, cùng câu báo: không trả lời được sang chuyến khác, và không biết được bình luận đó có tồn tại hay không. Cùng bình luận đó, hỏi qua chuyến B thì tìm thấy | Bảo mật | Đạt |
| TC-COMMENT-034 | `parentId` là mã một bình luận không tồn tại | 400, cùng câu báo ở ô `parentId` | Sai | Đạt |
| TC-COMMENT-035 | Chuyến đi không tồn tại, có kèm `parentId` | 404 về chuyến đi; không tra tới bình luận được trả lời, không lưu gì | Sai | Đạt |

## E. Xem danh sách bình luận

> **Yêu cầu:** design.md 10.2 "Quy ước Comment API" (dòng GET, "Xem") · **Kiểm bởi:** `CommentServiceTest`, `CommentControllerTest`, `CommentRepositoryTest`, `TripPermissionMatrixIntegrationTest` (dòng "Xem bình luận")

Mốc 5 trả mọi bình luận của chuyến đi trong một danh sách phẳng: bình luận cho cả chuyến, bình luận về hoạt động và câu trả lời nằm chung, cũ trước. Câu trả lời mang mã bình luận được trả lời để giao diện (Task 4.6) lồng vào dưới. Lọc theo hoạt động là mốc sau.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-COMMENT-036 | Chủ chuyến đi, người cùng sửa và người chỉ xem (đã nhận lời) lần lượt xem danh sách | Cả ba đều được (200) | Đúng | Đạt |
| TC-COMMENT-037 | Người lạ đã đăng nhập, người được mời mà chưa nhận lời, người đã bị gỡ khỏi chuyến xem danh sách; khách chưa đăng nhập cũng thử | Ba người đầu bị từ chối (403 `FORBIDDEN`), khách bị từ chối (401) | Bảo mật | Đạt |
| TC-COMMENT-038 | Chủ xoá chuyến đi, rồi chủ, người cùng sửa và người lạ xem danh sách bình luận của chuyến đó | Cả ba nhận 404 `RESOURCE_NOT_FOUND`; bình luận không được đọc tới | Bảo mật | Đạt |
| TC-COMMENT-039 | Chuyến có một bình luận về hoạt động lúc 3 giờ, một bình luận cho cả chuyến lúc 2 giờ, và một câu trả lời cũng lúc 3 giờ (đăng sau) | Thứ tự: bình luận lúc 2 giờ, bình luận lúc 3 giờ, rồi câu trả lời lúc 3 giờ: cũ trước, cùng thời điểm thì cái đăng trước đứng trước. Câu trả lời có `parentId` trỏ tới bình luận gốc; mỗi bình luận có tác giả (mã, họ tên, ảnh đại diện) | Đúng | Đạt |
| TC-COMMENT-040 | Chuyến có một bình luận còn, một bình luận đã xoá; một chuyến khác cũng có bình luận | Chỉ hiện bình luận còn của đúng chuyến này | Bảo mật | Đạt |
| TC-COMMENT-041 | Chuyến đi chưa có bình luận nào | 200, danh sách rỗng (`[]`) | Biên | Đạt |
| TC-COMMENT-042 | Đọc danh sách có bình luận của hai người khác nhau | Thông tin tác giả được lấy cùng một lần đọc với danh sách, nên số lần đọc cơ sở dữ liệu không tăng theo số bình luận. Đã thử bỏ cách lấy kèm này: test đỏ, nên test bắt được lỗi | Đúng | Đạt |
| TC-COMMENT-043 | Mã chuyến đi trên đường dẫn không phải số (`/trips/abc/comments`) | 400 `VALIDATION_ERROR`, không tới phần xử lý | Sai | Đạt |

## F. Lọc bình luận theo hoạt động

> **Yêu cầu:** design.md 10.2 "Quy ước Comment API" (dòng GET `?activityId=`, "Xem") · **Kiểm bởi:** `CommentServiceTest`, `CommentControllerTest`, `CommentRepositoryTest`

Mốc 6 cho phép xem riêng bình luận của một hoạt động, như khi mở thẻ hoạt động ở giao diện (Task 4.6, số bình luận trên thẻ cũng đếm từ danh sách này). Câu trả lời luôn đi cùng bình luận gốc của nó nên cuộc trao đổi không bị cắt đôi. Quyền giống phần E.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-COMMENT-044 | Hoạt động "Chợ Đà Lạt" có hai bình luận (lúc 2 giờ và 3 giờ), một câu trả lời lúc 4 giờ và một bình luận đã xoá; chuyến còn một bình luận chung và một bình luận về hoạt động "Hồ Xuân Hương". Xem với `activityId` của "Chợ Đà Lạt" | Chỉ ba bình luận của "Chợ Đà Lạt" theo thứ tự 2 giờ, 3 giờ, câu trả lời 4 giờ; không có bình luận chung, bình luận của hoạt động khác và bình luận đã xoá. Tác giả được lấy cùng lần đọc | Đúng | Đạt |
| TC-COMMENT-045 | Xem với mã một hoạt động của chuyến khác, hoặc mã không tồn tại | 404 `RESOURCE_NOT_FOUND` (không phải danh sách rỗng); bình luận không được đọc tới | Bảo mật | Đạt |
| TC-COMMENT-046 | Chuyến đi không tồn tại, có kèm `activityId` | 404 về chuyến đi; không tra tới hoạt động | Sai | Đạt |
| TC-COMMENT-047 | `activityId` không phải số (`?activityId=abc`) | 400 `VALIDATION_ERROR`, không tới phần xử lý | Sai | Đạt |
| TC-COMMENT-048 | Không gửi `activityId` | Danh sách của cả chuyến như phần E; không tra tới hoạt động nào | Đúng | Đạt |

## G. Xoá bình luận

> **Yêu cầu:** design.md 5.2 bảng `comments` (xoá mềm, một câu cho gốc và trả lời), 10.2 "Quy ước Comment API" (dòng DELETE, "Xoá"), rule 27 · **Kiểm bởi:** `CommentServiceTest`, `CommentControllerTest`, `CommentRepositoryTest`, `TripPermissionMatrixIntegrationTest` (dòng "Xoá bình luận")

Mốc 7 cho tác giả xoá bình luận của mình, và chủ chuyến đi xoá bất kỳ bình luận nào trên chuyến của họ. Xoá là ẩn đi (dòng vẫn nằm trong cơ sở dữ liệu kèm thời điểm xoá). Xoá một bình luận gốc thì các câu trả lời của nó mất cùng lúc, nên không bao giờ còn câu trả lời mồ côi trong danh sách. Thứ tự kiểm: chuyến đi (404), bình luận (404), rồi người xoá (403).

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-COMMENT-049 | Tác giả xoá bình luận gốc của mình, bình luận này có một câu trả lời; chuyến còn một bình luận khác kèm câu trả lời | 200, `data` rỗng. Bình luận và câu trả lời của nó bị xoá trong cùng một lần, cùng một thời điểm (lấy từ đồng hồ của ứng dụng, lưu theo giờ UTC); bình luận kia và câu trả lời của nó còn nguyên trong danh sách | Đúng | Đạt |
| TC-COMMENT-050 | Chủ chuyến đi xoá bình luận của người chỉ xem | Được | Đúng | Đạt |
| TC-COMMENT-051 | Người chỉ xem xoá bình luận của chủ chuyến | 403 `FORBIDDEN`, không xoá gì | Bảo mật | Đạt |
| TC-COMMENT-052 | Người cùng sửa xoá bình luận của người chỉ xem | 403 `FORBIDDEN`: được sửa chuyến đi không có nghĩa là được xoá bình luận của người khác | Bảo mật | Đạt |
| TC-COMMENT-053 | Xoá một trong hai câu trả lời của một bình luận gốc | Chỉ câu trả lời đó bị xoá; bình luận gốc và câu trả lời còn lại vẫn còn | Đúng | Đạt |
| TC-COMMENT-054 | Một câu trả lời đã bị xoá lúc 2 giờ, rồi bình luận gốc của nó bị xoá lúc 3 giờ | Câu trả lời giữ thời điểm xoá 2 giờ của nó, chỉ bình luận gốc nhận 3 giờ | Biên | Đạt |
| TC-COMMENT-055 | Xoá lại một bình luận đã xoá, hoặc xoá bằng mã một bình luận của chuyến khác | 404 `RESOURCE_NOT_FOUND`, không xoá gì; không biết được bình luận của chuyến khác có tồn tại hay không | Bảo mật | Đạt |
| TC-COMMENT-056 | Chủ xoá chuyến đi, rồi chủ, người cùng sửa và người lạ xoá bình luận của chính mình trên chuyến đó | Cả ba nhận 404 `RESOURCE_NOT_FOUND`, trước khi bình luận được tra tới | Bảo mật | Đạt |
| TC-COMMENT-057 | Mỗi người gọi xoá bình luận của chính mình: chủ, người cùng sửa, người chỉ xem; người lạ, người được mời chưa nhận lời, người đã bị gỡ (bình luận của họ được ghi sẵn); khách chưa đăng nhập | Ba người đầu được (200). Ba người sau bị chặn (403 `FORBIDDEN`) ngay cả với bình luận của chính họ: người đã bị gỡ không xoá được bình luận cũ của mình, chủ chuyến vẫn xoá được. Khách: 401 | Bảo mật | Đạt |
| TC-COMMENT-058 | Mã bình luận trên đường dẫn không phải số (`/comments/abc`) | 400 `VALIDATION_ERROR`, không tới phần xử lý | Sai | Đạt |

Đã thử bỏ phần "và các câu trả lời của nó" khỏi câu lệnh xoá: `TC-COMMENT-049` đỏ, nên test bắt được lỗi này.

## H. Toàn luồng

> **Yêu cầu:** design.md 10.2 "Quy ước Comment API", rule 27, CLAUDE.md mục 8 (số câu SQL) · **Kiểm bởi:** `CommentFlowIntegrationTest`

Mốc 8 chạy các bước như người dùng thật, qua mọi tầng của ứng dụng với MySQL và Redis thật, để kiểm những điều chỉ thấy được khi các phần ghép lại. Quyền của từng người cho từng thao tác đã nằm ở ma trận quyền (phần B, E, G), không lặp lại ở đây.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-COMMENT-059 | Chủ đăng "Cả nhà nhớ mang áo ấm" cho cả chuyến, rồi hỏi "Mấy giờ đi chợ?" trên hoạt động "Chợ Đà Lạt". Người chỉ xem trả lời "  7 giờ nhé  " mà không gửi mã hoạt động. Cả hai xem danh sách, rồi xem riêng hoạt động. Chủ thử trả lời câu trả lời | Danh sách theo đúng thứ tự đăng; câu trả lời đã bỏ khoảng trắng hai đầu, trỏ tới câu hỏi, nằm ở "Chợ Đà Lạt", mang tên "Người xem"; bình luận đầu mang tên "Chủ chuyến đi". Xem riêng hoạt động: chỉ câu hỏi và câu trả lời. Trả lời câu trả lời: 400 ở ô `parentId`, trong bảng vẫn chỉ 3 bình luận | Đúng | Đạt |
| TC-COMMENT-060 | Chủ hỏi, người chỉ xem trả lời và đăng thêm một bình luận riêng. Người chỉ xem thử xoá câu hỏi của chủ; chủ xoá câu hỏi; chủ xoá lại lần nữa; chủ xoá bình luận riêng của người chỉ xem | Người chỉ xem: 403. Chủ xoá câu hỏi: 200, câu trả lời mất theo, bình luận riêng của người chỉ xem còn; ba dòng vẫn nằm trong bảng, câu hỏi và câu trả lời mang cùng một thời điểm xoá. Xoá lại: 404. Chủ xoá bình luận của người chỉ xem: 200, danh sách rỗng | Đúng | Đạt |
| TC-COMMENT-061 | Hoạt động "Chợ Đà Lạt" có câu hỏi và câu trả lời, chuyến có thêm một bình luận chung. Chủ xoá hoạt động qua chức năng xoá hoạt động | Câu hỏi và câu trả lời mất hẳn khỏi bảng (không chỉ bị ẩn), bình luận chung còn; xem bình luận của hoạt động đã xoá: 404 | Đúng | Đạt |
| TC-COMMENT-062 | Đếm số câu lệnh gửi tới cơ sở dữ liệu khi chủ xem danh sách (quyền đã nằm trong bộ nhớ tạm), lúc có 1 bình luận và lúc có 6 bình luận của hai người | Cả chuyến: 2 câu (kiểm chuyến đi, lấy bình luận kèm tác giả). Một hoạt động: 3 câu (thêm kiểm hoạt động). Không đổi khi số bình luận tăng; đủ 6 bình luận với cả hai tên tác giả | Đúng | Đạt |

## Kiểm tra thủ công

| Mã | Các bước | Kết quả mong đợi | Trạng thái |
|---|---|---|---|
| MT-COMMENT-01 | Chạy `docker compose up -d mysql redis mailhog` rồi backend profile `local`. Xem log lúc khởi động. Trong MySQL chạy `SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1;` và `SHOW CREATE TABLE comments;` | Log có dòng áp dụng migration 13 (hoặc báo đã cập nhật nếu đã chạy trước đó), ứng dụng lên bình thường. Dòng cuối của lịch sử: `13`, `create comments`, `1`. Bảng có 4 khoá ngoại: `fk_comments_trip`, `fk_comments_activity`, `fk_comments_parent` đều `ON DELETE CASCADE`; `fk_comments_user` không có `ON DELETE` | Chưa chạy |
| MT-COMMENT-02 | Chạy backend profile `local`. Tài khoản A (chủ) mời B làm `VIEWER`, B nhận lời (như `MT-SHARE-01` đến `MT-SHARE-03`); C là tài khoản chưa được mời. Trong Swagger, nhóm "Comment": bằng B gọi `POST /api/v1/trips/{id}/comments` với nội dung có dấu (gõ trong Swagger, không dùng curl) `{"content": "  Nhớ mang áo ấm  "}`. Bằng C gọi cùng lệnh. Bằng B gửi `{"content": "   "}`. Xem log backend | B: 201, `content` = "Nhớ mang áo ấm" (không còn khoảng trắng hai đầu), `author.id` là mã của B, `author` không có email. C: 403 `FORBIDDEN`. Nội dung toàn khoảng trắng: 400, ô `content` với câu "Nội dung bình luận không được để trống". Log có dòng `comment ... posted by user ...` chỉ ghi mã, **không** in nội dung | Chưa chạy |
| MT-COMMENT-03 | Tiếp theo `MT-COMMENT-02`. Trong Swagger, bằng B gọi `GET /api/v1/trips/{id}` để lấy mã một hoạt động của chuyến A, rồi gọi `POST /api/v1/trips/{id}/comments` với `{"content": "Chợ mở lúc mấy giờ?", "activityId": <mã đó>}`. Bằng A tạo một chuyến đi thứ hai có một hoạt động; bằng B gửi lại lệnh trên vào chuyến thứ nhất nhưng với mã hoạt động của chuyến thứ hai. Gửi thêm `"activityId": 999999` | Lần đầu: 201, `activityId` là mã hoạt động đã gửi. Hoạt động của chuyến thứ hai và mã 999999: cả hai 404 `RESOURCE_NOT_FOUND` với cùng câu báo, không bình luận nào được lưu | Chưa chạy |
| MT-COMMENT-04 | Tiếp theo `MT-COMMENT-03`. Trong Swagger, bằng A gọi `POST /api/v1/trips/{id}/comments` với `{"content": "Trả lời nhé", "parentId": <mã bình luận "Chợ mở lúc mấy giờ?" của B>, "activityId": 999999}`. Bằng B trả lời chính câu trả lời vừa tạo (`parentId` = mã câu trả lời của A) | Lần đầu: 201, `parentId` là mã bình luận của B, `activityId` là mã hoạt động của bình luận đó (không phải 999999, không báo 404). Lần hai: 400 `VALIDATION_ERROR`, ô `parentId` với câu "Chỉ trả lời được một bình luận gốc còn tồn tại của chuyến đi này" | Chưa chạy |
| MT-COMMENT-05 | Tiếp theo `MT-COMMENT-04`. Trong Swagger, bằng B gọi `GET /api/v1/trips/{id}/comments` của chuyến thứ nhất. Bằng C gọi cùng lệnh. Xem log backend có bật in câu SQL (`logging.level.org.hibernate.SQL=debug`) lúc B gọi | B: 200, mọi bình luận đã đăng ở `MT-COMMENT-02` đến `04` theo thứ tự đăng (bình luận toàn khoảng trắng không có vì đã bị từ chối); câu trả lời của A có `parentId` là mã bình luận của B. C: 403 `FORBIDDEN`. Log: chỉ một câu `select` vào bảng `comments` (có `join users`), không có câu riêng cho từng tác giả | Chưa chạy |
| MT-COMMENT-06 | Tiếp theo `MT-COMMENT-05`. Trong Swagger, bằng B gọi `GET /api/v1/trips/{id}/comments` với `activityId` = mã hoạt động đã dùng ở `MT-COMMENT-03`. Gọi lại với `activityId` = 999999 | Lần đầu: 200, chỉ bình luận "Chợ mở lúc mấy giờ?" và câu trả lời của A; không có "Nhớ mang áo ấm" (bình luận cho cả chuyến). `activityId` = 999999: 404 `RESOURCE_NOT_FOUND` | Chưa chạy |
| MT-COMMENT-07 | Tiếp theo `MT-COMMENT-06`. Trong Swagger, bằng B gọi `DELETE /api/v1/trips/{id}/comments/{commentId}` với mã câu trả lời của A (từ `MT-COMMENT-04`). Bằng B xoá bình luận "Chợ mở lúc mấy giờ?" của chính mình, rồi gọi `GET /api/v1/trips/{id}/comments`. Bằng A xoá bình luận "Nhớ mang áo ấm" của B. Bằng A gọi lại đúng lệnh vừa rồi. Xem log backend | Câu trả lời của A: 403 `FORBIDDEN`. Bình luận của B: 200, `data` rỗng; danh sách không còn bình luận đó, cũng không còn câu trả lời của A dưới nó. A xoá bình luận của B: 200. Gọi lại: 404 `RESOURCE_NOT_FOUND`. Log có dòng `comment ... deleted by user ... (2 rows with its replies)` cho lần xoá bình luận có câu trả lời, chỉ ghi mã, không in nội dung | Chưa chạy |

## Lỗi đã phát hiện

| Mã | Phát hiện ở | Hiện tượng | Nguyên nhân | Cách sửa | Trạng thái |
|---|---|---|---|---|---|
| _(chưa có)_ | | | | | |

# 09 · Chia sẻ chuyến đi

> Cập nhật: 2026-10-09 · **Task 4.1 xong**: PR #26, merge commit `5004457`, 12 commit từ `3c66957` đến `f7d317f`, build xanh 1059 lượt test · Mốc 10 tại `f7d317f` (1059 lượt test) · Mốc 9 tại `0a41839` (1058 lượt test; `BUG-SHARE-001` đã sửa) · Mốc 8 tại `c0dfcfd` (1054 lượt test) · Mốc 7 tại `a18ce26` (1051 lượt test) · Mốc 6 tại `99d46a7` (1039 lượt test) · Mốc 5 tại `6617fce` (1026 lượt test) · Mốc 4 tại `6a1ec45` (1016 lượt test) · Mốc 3 tại `e2158db` (1007 lượt test) · Mốc 2c tại `5b92632` (994 lượt test) · Mốc 2b tại `ac83f4e` (988 lượt test) · Mốc 2a tại `eb009c8` (974 lượt test) · Mốc 1 tại `3c66957` (971 lượt test) · kiểm tra thủ công `MT-SHARE-01` đến `MT-SHARE-09` chưa chạy · [Về trang chính](README.md)

Chủ chuyến đi mời người khác vào chuyến của mình theo email và quyết định họ chỉ được xem hay được cùng sửa. Người được mời bấm link trong mail để nhận lời. Chủ có thể đổi vai trò hoặc gỡ một thành viên bất cứ lúc nào. Phần thành viên làm ở Task 4.1; liên kết chia sẻ cho người không có tài khoản ở Task 4.2; bảng kiểm quyền đầy đủ ở Task 4.3; giao diện ở Task 4.5.

File này được ghi dần theo từng mốc của Task 4.1:

| Mốc | Nội dung | Phần trong file | Mã commit |
|---|---|---|---|
| 1 | Bảng lưu thành viên và lời mời của một chuyến đi | A | `3c66957` |
| 2a | Mail mời tham gia chuyến đi (nội dung và link); chưa có ai gửi, API mời ở mốc 2b | B | `eb009c8` |
| 2b | Chủ chuyến đi mời một email mới vào chuyến đi (`POST /api/v1/trips/{tripId}/members`) | C | `ac83f4e` |
| 2c | Mời lại một email đã có dòng: đang chờ thì gửi lại, đã bị gỡ thì mở lại, đã nhận lời thì từ chối (409) | D | `5b92632` |
| 3 | Người được mời nhận lời bằng token trong mail (`POST /api/v1/trips/{tripId}/members/accept`) | E | `e2158db` |
| 4 | Bộ kiểm quyền nhận thành viên đã nhận lời: người chỉ xem xem được, người cùng sửa sửa được, mọi API cũ của chuyến đi đổi theo mà không sửa dòng nào | F | `6a1ec45` |
| 5 | Xem danh sách thành viên của chuyến đi (`GET /api/v1/trips/{tripId}/members`), chủ đứng đầu | G | `6617fce` |
| 6 | Chủ đổi vai trò của một thành viên (`PATCH /api/v1/trips/{tripId}/members/{memberId}`), có hiệu lực ngay | H | `99d46a7` |
| 7 | Chủ gỡ thành viên hoặc rút lời mời (`DELETE /api/v1/trips/{tripId}/members/{memberId}`), giữ dòng, mất quyền ngay, mời lại dùng lại dòng | I | `a18ce26` |
| 8 | Danh sách và chip trạng thái (`GET /api/v1/trips`, `GET /api/v1/trips/status-counts`) gồm cả chuyến được chia sẻ; mỗi dòng có `ownerId`, `ownerName` | J | `c0dfcfd` |
| 9 | Chi tiết chuyến đi (`GET /api/v1/trips/{id}`) trả thêm `members` và `myRole`; 5 câu SQL thay vì 4 | K | `0a41839` |
| 10 | Test toàn luồng mời qua HTTP: mời → mail → đăng ký → nhận lời → xem → nâng vai trò → sửa → gỡ → mời lại | L | `f7d317f` |

Vài từ dùng trong file:

| Từ | Nghĩa |
|---|---|
| Chủ chuyến đi | Người tạo chuyến đi. Không nằm trong bảng thành viên; hệ thống biết chủ qua chính chuyến đi |
| Thành viên | Người được chủ mời vào chuyến đi. Mỗi người một dòng trong bảng `trip_members`, dù đã nhận lời hay chưa |
| Vai trò | `EDITOR` được xem và sửa nội dung chuyến đi; `VIEWER` chỉ được xem. Trên API còn có vai trò `OWNER` dành cho chủ, nhưng giá trị này không bao giờ được ghi vào bảng |
| Trạng thái lời mời | `PENDING` đã gửi mail, chưa nhận lời · `ACCEPTED` đã nhận lời · `REMOVED` đã bị chủ gỡ. Chỉ `ACCEPTED` mới có quyền |
| Token mời | Chuỗi bí mật nằm trong link của mail mời, dùng một lần, sống 7 ngày. Hệ thống chỉ lưu bản băm (SHA-256) của nó, giống token đặt lại mật khẩu |

---

## A. Bảng lưu thành viên và lời mời

> **Yêu cầu:** design.md 5.2 bảng `trip_members`, 6.2 (chủ không có dòng), quyết định 2 và 11 của bản rà Phase 4 · **Kiểm bởi:** `TripMemberMappingTest`

Mốc này chỉ tạo chỗ lưu, chưa có API. Các kịch bản kiểm rằng chỗ lưu giữ đúng dữ liệu và từ chối những tổ hợp không được phép.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-SHARE-001 | Lưu một lời mời đang chờ (email, vai trò, bản băm token, hạn, người mời, lúc mời) rồi đọc lại | Mọi giá trị đọc lại y nguyên, kể cả phần triệu giây của thời điểm; trạng thái mặc định là `PENDING`; chưa có tài khoản và chưa có lúc nhận lời | Đúng | Đạt |
| TC-SHARE-002 | Lưu một thành viên đã nhận lời: có tài khoản, có lúc nhận lời, không còn token | Đọc lại đúng tài khoản, trạng thái `ACCEPTED`, lúc nhận lời; token và hạn token trống | Đúng | Đạt |
| TC-SHARE-003 | Cùng một tài khoản được ghi hai dòng trong cùng chuyến đi (dù email ghi khác nhau) | Bị từ chối, nêu tên khoá `uk_trip_members_trip_user` | Sai | Đạt |
| TC-SHARE-004 | Cùng một email được mời hai lần vào cùng chuyến đi | Bị từ chối, nêu tên khoá `uk_trip_members_trip_email` | Sai | Đạt |
| TC-SHARE-005 | Cùng một email được mời vào hai chuyến đi khác nhau | Cả hai dòng đều lưu được | Biên | Đạt |
| TC-SHARE-006 | Hai lời mời đang chờ tới hai email chưa có tài khoản trong cùng chuyến đi | Cả hai lưu được: ô tài khoản trống không bị coi là trùng nhau | Biên | Đạt |
| TC-SHARE-007 | Hai dòng (ở hai chuyến đi khác nhau) có cùng bản băm token | Bị từ chối, nêu tên khoá `uk_trip_members_invite_token_hash`: một link mời chỉ thuộc về một lời mời | Bảo mật | Đạt |
| TC-SHARE-008 | Hai thành viên đã nhận lời, cả hai không còn token | Cả hai lưu được: ô token trống không bị coi là trùng nhau | Biên | Đạt |
| TC-SHARE-009 | Lưu lần lượt từng vai trò (`EDITOR`, `VIEWER`) và từng trạng thái (`PENDING`, `ACCEPTED`, `REMOVED`) | Mọi giá trị của Java đều được cột ENUM của MySQL nhận và đọc lại đúng | Đúng | Đạt |
| TC-SHARE-010 | Ghi thẳng vào bảng một dòng có vai trò `OWNER` | MySQL từ chối (`Data truncated for column 'role'`): chủ chuyến không bao giờ là một dòng thành viên | Bảo mật | Đạt |
| TC-SHARE-011 | Xoá hẳn một chuyến đi khỏi cơ sở dữ liệu (việc bộ dọn dẹp sẽ làm với chuyến đã xoá mềm quá 30 ngày) | Mọi dòng thành viên của chuyến đó mất theo, không còn dòng mồ côi | Đúng | Đạt |
| TC-SHARE-012 | Sau khi lưu, cố đổi chuyến đi, email được mời và người mời của một dòng rồi lưu lại | Ba giá trị này không đổi trong cơ sở dữ liệu; chỉ phần được phép (vai trò) đổi | Bảo mật | Đạt |

## B. Mail mời tham gia chuyến đi

> **Yêu cầu:** design.md rule 14.23, 10.2 "Quy ước Sharing API" (link `/invite?trip={id}&token=...`), 7.1 (gửi mail qua `MailProvider`) · **Kiểm bởi:** `MailServiceTest`

Mail được dựng từ mẫu `trip-invitation.html` và gửi qua cổng mail của ứng dụng; test dùng cổng giả nên không cần máy chủ mail. Mốc này chỉ có hàm gửi, chưa có API gọi nó.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-SHARE-013 | Gửi mail mời với vai trò cùng chỉnh sửa tới một địa chỉ, kèm tên người mời, tên chuyến đi, mã chuyến đi và token | Mail tới đúng địa chỉ; tiêu đề nêu tên người mời và tên chuyến đi; thân mail có tên người mời, tên chuyến đi, chữ "cùng chỉnh sửa", hạn 7 ngày, và link `{địa chỉ giao diện}/invite?trip={mã}&token={token}`; không còn biểu thức mẫu chưa thay | Đúng | Đạt |
| TC-SHARE-014 | Gửi mail mời với vai trò chỉ xem | Thân mail ghi "chỉ xem", không ghi "cùng chỉnh sửa" | Đúng | Đạt |
| TC-SHARE-015 | Tên chuyến đi chứa mã HTML (`<script>`) | Trong mail mã đó hiện thành chữ thường, không thành thẻ HTML chạy được | Bảo mật | Đạt |

## C. Mời một email mới vào chuyến đi

> **Yêu cầu:** design.md 10.2 "Quy ước Sharing API" (mời), rule 14.23, 6.2 (chỉ chủ mời), rule 14.6 (không mời chính mình) · **Kiểm bởi:** `SharingServiceTest`, `TripMemberControllerTest`

Phần này là trường hợp email **chưa có dòng nào** trong chuyến đi. Email đã có dòng xử lý ở phần D (mốc 2c).

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-SHARE-016 | Chủ mời một email chưa có tài khoản với vai trò cùng chỉnh sửa | 201. Một dòng mới: đúng chuyến đi, email, vai trò `EDITOR`, trạng thái `PENDING`, chưa có tài khoản, người mời là chủ, lúc mời là "bây giờ", hạn token đúng 7 ngày sau. Mail gửi tới email đó với token thô; dòng chỉ giữ bản băm SHA-256 của token, hai giá trị khác nhau và khớp nhau. Câu trả lời có mã dòng, email, vai trò, trạng thái, lúc mời | Đúng | Đạt |
| TC-SHARE-017 | Email gõ có khoảng trắng và chữ hoa (`  Friend@Example.COM `) | Lưu và gửi mail tới `friend@example.com` | Biên | Đạt |
| TC-SHARE-018 | Email được mời đã có tài khoản | Dòng mới trỏ sẵn tới tài khoản đó nhưng vẫn `PENDING` (vẫn phải nhận lời); câu trả lời có mã và tên tài khoản | Đúng | Đạt |
| TC-SHARE-019 | Mời với vai trò chỉ xem | Mail ghi "chỉ xem" | Đúng | Đạt |
| TC-SHARE-020 | Chủ mời chính email của mình (gõ hoa thường khác đi) | 400 `VALIDATION_ERROR` ở trường `email`: "Đây là email của chính bạn, chủ chuyến đi không cần được mời". Không lưu gì, không gửi mail | Sai | Đạt |
| TC-SHARE-021 | Mời vào chuyến đi không tồn tại hoặc đã xoá | 404 `RESOURCE_NOT_FOUND`; không lưu, không gửi mail | Sai | Đạt |
| TC-SHARE-022 | Chủ gọi API mời với body hợp lệ | 201, body JSON có `memberId`, `email`, `role`, `status`, `invitedAt` | Đúng | Đạt |
| TC-SHARE-023 | Người không phải chủ (kể cả người cùng chỉnh sửa) gọi API mời | 403 `FORBIDDEN`, không tới tầng xử lý | Bảo mật | Đạt |
| TC-SHARE-024 | Gọi API mời khi chưa đăng nhập | 401 `UNAUTHORIZED` | Bảo mật | Đạt |
| TC-SHARE-025 | Email sai định dạng | 400 ở trường `email`: "Email không đúng định dạng" | Sai | Đạt |
| TC-SHARE-026 | Thiếu vai trò | 400 ở trường `role`: "Thiếu vai trò của thành viên (EDITOR hoặc VIEWER)" | Sai | Đạt |
| TC-SHARE-027 | Gửi vai trò `OWNER` | 400 `VALIDATION_ERROR`: giá trị không tồn tại, không ai tạo được chủ thứ hai | Bảo mật | Đạt |
| TC-SHARE-028 | Tầng xử lý báo "email của chủ" | API trả 400 với đúng câu thông báo ở trường `email` | Sai | Đạt |
| TC-SHARE-029 | Tầng xử lý báo chuyến đi không tồn tại | API trả 404 `RESOURCE_NOT_FOUND` | Sai | Đạt |

## D. Mời lại một email đã có dòng trong chuyến đi

> **Yêu cầu:** design.md 10.2 "Quy ước Sharing API" (mời), rule 14.23, 10.3 (`MEMBER_ALREADY_EXISTS`), quyết định 11 của bản rà Phase 4 (gỡ giữ dòng, mời lại dùng lại dòng) · **Kiểm bởi:** `SharingServiceTest`, `TripMemberControllerTest`, `ErrorCodeTest`

Mỗi email chỉ có một dòng trong một chuyến đi. Mời lại không tạo dòng mới mà làm việc trên dòng cũ, tuỳ dòng đó đang ở trạng thái nào.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-SHARE-030 | Mời lại một email đang chờ (`PENDING`), lần này với vai trò khác | 201 trên **cùng dòng cũ**: token mới (bản băm khác bản cũ, link cũ hết tác dụng), hạn 7 ngày tính từ bây giờ, lúc mời là bây giờ, vai trò đổi theo lần gửi mới; gửi lại mail với token mới | Đúng | Đạt |
| TC-SHARE-031 | Mời lại email đang chờ mà từ lần mời đầu người đó đã đăng ký tài khoản | Dòng cũ được gắn với tài khoản mới đăng ký | Biên | Đạt |
| TC-SHARE-032 | Mời lại một người đã bị gỡ (`REMOVED`, từng nhận lời, có tài khoản) | Dòng cũ về `PENDING`, xoá lúc nhận lời (phải nhận lại), giữ liên kết tài khoản, token mới hạn 7 ngày, gửi mail; không tra lại tài khoản vì đã biết | Đúng | Đạt |
| TC-SHARE-033 | Mời lại một email đã nhận lời (`ACCEPTED`) | 409 `MEMBER_ALREADY_EXISTS`; dòng cũ không đổi gì (trạng thái, vai trò, không có token), không lưu, không gửi mail | Sai | Đạt |
| TC-SHARE-034 | API nhận 409 từ tầng xử lý | Body JSON `errorCode` = `MEMBER_ALREADY_EXISTS`, thông báo "Người này đã là thành viên của chuyến đi" | Sai | Đạt |
| TC-SHARE-035 | Mã lỗi `MEMBER_ALREADY_EXISTS` có câu thông báo tiếng Việt trong `messages.properties` | Có (test chung của mọi mã lỗi) | Đúng | Đạt |

Chưa xử lý: hai lần mời **cùng lúc** một email mới (hai request chạy song song). Lần thứ hai bị khoá UNIQUE từ chối và trả 500 thay vì 201 hoặc 409. Giao diện gọi tuần tự nên chưa gặp; ghi lại để xét khi làm hộp thoại chia sẻ (Task 4.5).

## E. Nhận lời mời

> **Yêu cầu:** design.md 10.2 "Quy ước Sharing API" (nhận lời), rule 14.23 (token một lần, 7 ngày), quyết định thêm khi rà Phase 4 (phải đăng nhập đúng email được mời) · **Kiểm bởi:** `SharingServiceTest`, `TripMemberControllerTest`

Người được mời đăng nhập (hoặc đăng ký rồi đăng nhập) bằng chính email được mời, rồi gửi token lấy từ link trong mail. Endpoint này **không** kiểm quyền trên chuyến đi vì người gọi chưa phải thành viên; nó chỉ cần đã đăng nhập. Sau khi nhận lời, các API khác của chuyến đi bắt đầu cho người này vào (từ mốc 4).

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-SHARE-036 | Người có đúng email được mời gửi token còn hạn của chuyến đi đúng | 200. Dòng chuyển `ACCEPTED`, gắn tài khoản, ghi lúc nhận lời, xoá token và hạn token; vai trò giữ nguyên như lúc mời. Hệ thống tìm dòng bằng bản băm của token, không bao giờ đưa token thô vào cơ sở dữ liệu. Không gửi mail | Đúng | Đạt |
| TC-SHARE-037 | Email tài khoản viết hoa thường khác email được mời | Vẫn nhận lời được (so không phân biệt hoa thường) | Biên | Đạt |
| TC-SHARE-038 | Token không tồn tại | 400 `INVALID_TOKEN`; không lưu gì, không tra tài khoản | Sai | Đạt |
| TC-SHARE-039 | Token hợp lệ nhưng của chuyến đi khác với chuyến đi trên đường dẫn | 400 `INVALID_TOKEN`; không lưu gì | Bảo mật | Đạt |
| TC-SHARE-040 | Token đã hết hạn (hạn đúng bằng "bây giờ" cũng coi là hết) | 400 `INVALID_TOKEN`; dòng vẫn `PENDING`, không lưu gì | Biên | Đạt |
| TC-SHARE-041 | Dòng không còn ở trạng thái chờ (ví dụ đã bị gỡ) nhưng token vẫn khớp | 400 `INVALID_TOKEN`; không lưu gì | Sai | Đạt |
| TC-SHARE-042 | Tài khoản đang đăng nhập có email khác email được mời (link bị chuyển tiếp) | 403 `FORBIDDEN`. Lời mời vẫn `PENDING`, chưa gắn tài khoản, token còn nguyên: người đúng vẫn dùng được link | Bảo mật | Đạt |
| TC-SHARE-043 | Dùng cùng một link hai lần | Lần hai 400 `INVALID_TOKEN` vì bản băm đã bị xoá sau lần một | Bảo mật | Đạt |
| TC-SHARE-044 | Gọi API nhận lời với token hợp lệ | 200, body có `memberId`, `userId`, `role`, `status` = `ACCEPTED`, `acceptedAt`; mã tài khoản lấy từ token đăng nhập; **không** hỏi bộ kiểm quyền của chuyến đi | Đúng | Đạt |
| TC-SHARE-045 | Gọi API nhận lời khi chưa đăng nhập | 401 | Bảo mật | Đạt |
| TC-SHARE-046 | Token để trống | 400 ở trường `token`: "Thiếu mã xác thực" | Sai | Đạt |
| TC-SHARE-047 | Tầng xử lý từ chối token | 400 `INVALID_TOKEN` với câu "Liên kết không hợp lệ hoặc đã hết hạn, vui lòng yêu cầu lại" (một câu chung, không nói rõ vì sao, giống token xác thực email) | Sai | Đạt |
| TC-SHARE-048 | Tầng xử lý báo sai email | 403 `FORBIDDEN` với câu chung "Bạn không có quyền thực hiện thao tác này" | Bảo mật | Đạt |

## F. Thành viên được vào chuyến đi theo đúng vai trò

> **Yêu cầu:** design.md 6.2 (ma trận quyền, "Triển khai theo giai đoạn"), 16 (test bắt buộc "Viewer sửa activity → 403"), CLAUDE.md rule 15 · **Kiểm bởi:** `TripPermissionEvaluatorTest`, `TripRepositoryTest`, `SharingFlowIntegrationTest`

Mọi API của chuyến đi (chuyến, ngày, hoạt động, thời tiết, quãng đường, thành viên) hỏi cùng một bộ kiểm quyền. Mốc này đổi bộ kiểm quyền từ "có phải chủ không" thành "chủ, hay thành viên đã nhận lời với vai trò gì", bằng **một câu truy vấn** cho mỗi request. Không API nào phải sửa.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-SHARE-049 | Câu truy vấn quyền: hỏi cho chủ và cho người lạ | Trả mã chủ, không có vai trò thành viên, cho cả hai | Đúng | Đạt |
| TC-SHARE-050 | Câu truy vấn quyền: chuyến có người cùng sửa, người chỉ xem (đã nhận lời), người đang chờ, người đã gỡ | Hai người đã nhận lời có đúng vai trò; người đang chờ và đã gỡ **không có vai trò**, y như người lạ | Bảo mật | Đạt |
| TC-SHARE-051 | Thành viên của chuyến A hỏi quyền trên chuyến B của cùng chủ | Không có vai trò | Bảo mật | Đạt |
| TC-SHARE-052 | Hỏi quyền trên chuyến đã xoá mềm (vẫn còn dòng thành viên) hoặc không tồn tại | Không có kết quả, cho cả chủ lẫn thành viên | Đúng | Đạt |
| TC-SHARE-053 | Bộ kiểm quyền: chủ | Xem, sửa, và thao tác chỉ-chủ đều được | Đúng | Đạt |
| TC-SHARE-054 | Bộ kiểm quyền: người cùng sửa đã nhận lời | Xem được, sửa được, thao tác chỉ-chủ bị từ chối | Đúng | Đạt |
| TC-SHARE-055 | Bộ kiểm quyền: người chỉ xem đã nhận lời | Xem được; sửa và thao tác chỉ-chủ bị từ chối | Đúng | Đạt |
| TC-SHARE-056 | Bộ kiểm quyền: người lạ, người đang chờ, người đã gỡ | Bị từ chối cả ba | Bảo mật | Đạt |
| TC-SHARE-057 | Bộ kiểm quyền: chuyến không tồn tại | Cho qua để tầng xử lý trả 404 (không lộ chuyến nào có thật) | Đúng | Đạt |
| TC-SHARE-058 | Bộ kiểm quyền: chưa đăng nhập hoặc thiếu mã chuyến | Từ chối ngay, không hỏi cơ sở dữ liệu | Bảo mật | Đạt |
| TC-SHARE-059 | Toàn luồng: người chỉ xem gọi xem chuyến, xem ngày, xem hoạt động, xem thời tiết; rồi sửa chuyến, đổi trạng thái, thêm hoạt động, xoá chuyến, mời thành viên | Bốn lần xem 200. Năm lần ghi 403, dữ liệu không đổi (tên chuyến giữ nguyên, không có hoạt động nào) | Bảo mật | Đạt |
| TC-SHARE-060 | Toàn luồng: người cùng sửa sửa tên chuyến, thêm rồi sửa hoạt động; rồi xoá chuyến, mời thành viên | Ba thao tác sửa thành công (tên mới nằm trong cơ sở dữ liệu). Xoá và mời bị 403: chuyến chưa bị xoá, số thành viên không đổi | Đúng | Đạt |
| TC-SHARE-061 | Toàn luồng: người đang chờ, người đã gỡ, người lạ xem chuyến, xem ngày, sửa chuyến | Cả ba người, cả ba thao tác đều 403 | Bảo mật | Đạt |
| TC-SHARE-062 | Toàn luồng: thành viên của chuyến A mở chuyến B của cùng chủ | 403 | Bảo mật | Đạt |
| TC-SHARE-063 | Toàn luồng: chủ xoá chuyến, rồi thành viên và người lạ mở chuyến đó | Cả hai 404, giống nhau | Đúng | Đạt |

## G. Danh sách thành viên

> **Yêu cầu:** design.md 10.2 "Sharing" (`GET /members`), 6.2 (ai xem được chuyến thì xem được danh sách; chủ không có dòng) · **Kiểm bởi:** `TripMemberRepositoryTest`, `SharingServiceTest`, `TripMemberControllerTest`

Danh sách gồm chủ chuyến đi ở dòng đầu (vai trò `OWNER`, không có mã dòng thành viên), rồi người đã nhận lời, rồi lời mời đang chờ; trong mỗi nhóm người được mời trước đứng trước. Người đã bị gỡ không hiện. Mỗi dòng có tên và ảnh đại diện của tài khoản (nếu đã có tài khoản), không bao giờ có mật khẩu.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-SHARE-064 | Truy vấn danh sách của chuyến có: hai người đã nhận lời (mời lúc 0:50 và 3:20), hai lời mời đang chờ (mời lúc 1:40 và 5:00), một người đã gỡ | Thứ tự: hai người đã nhận lời theo giờ mời, rồi hai lời mời đang chờ theo giờ mời; người đã gỡ không có | Đúng | Đạt |
| TC-SHARE-065 | Truy vấn danh sách rồi đọc tên tài khoản của từng dòng | Tài khoản đã được tải cùng câu truy vấn, không tốn thêm câu nào; dòng chưa có tài khoản thì tài khoản trống | Đúng | Đạt |
| TC-SHARE-066 | Chuyến đi khác của cùng chủ cũng có thành viên | Chỉ trả thành viên của chuyến được hỏi; chuyến không tồn tại trả rỗng | Bảo mật | Đạt |
| TC-SHARE-067 | Tầng xử lý ghép danh sách: chuyến có một người đã nhận lời và một lời mời đang chờ | Ba dòng: chủ (`OWNER`, `ACCEPTED`, không mã dòng, không ngày mời), người đã nhận lời (đủ mã tài khoản, tên, ngày mời, ngày nhận), lời mời đang chờ (chưa có tài khoản, chưa có ngày nhận) | Đúng | Đạt |
| TC-SHARE-068 | Chuyến đi chưa mời ai | Danh sách chỉ có chủ | Biên | Đạt |
| TC-SHARE-069 | Chuyến đi không tồn tại | 404; không truy vấn thành viên | Sai | Đạt |
| TC-SHARE-070 | Gọi API danh sách với quyền xem | 200; JSON đúng thứ tự, dòng chủ có `memberId` null và `role` `OWNER`; toàn bộ body không chứa chữ "password" | Bảo mật | Đạt |
| TC-SHARE-071 | Gọi API danh sách khi không có quyền xem | 403, không tới tầng xử lý | Bảo mật | Đạt |
| TC-SHARE-072 | Gọi API danh sách của chuyến không tồn tại | 404 `RESOURCE_NOT_FOUND` | Sai | Đạt |
| TC-SHARE-073 | Gọi API danh sách khi chưa đăng nhập | 401 | Bảo mật | Đạt |

## H. Đổi vai trò thành viên

> **Yêu cầu:** design.md 10.2 "Quy ước Sharing API" (đổi vai trò / gỡ), 6.2 (chỉ chủ), rule 14.7 (không chuyển quyền chủ ở đây) · **Kiểm bởi:** `SharingServiceTest`, `TripMemberControllerTest`, `SharingFlowIntegrationTest`

Chủ nâng một người chỉ xem lên cùng chỉnh sửa hoặc ngược lại. Áp dụng cho cả lời mời đang chờ. Có hiệu lực ngay ở request kế tiếp của người đó vì bộ kiểm quyền đọc cơ sở dữ liệu mỗi lần (Task 4.3 thêm bộ nhớ đệm và xoá nó ở đây).

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-SHARE-074 | Chủ nâng người chỉ xem (đã nhận lời) lên cùng chỉnh sửa | Dòng đổi vai trò, trạng thái giữ nguyên; câu trả lời có vai trò mới | Đúng | Đạt |
| TC-SHARE-075 | Đổi vai trò của một lời mời đang chờ | Được; vẫn đang chờ | Đúng | Đạt |
| TC-SHARE-076 | Gửi lại đúng vai trò đang có | 200, không ghi gì xuống cơ sở dữ liệu | Biên | Đạt |
| TC-SHARE-077 | Đổi vai trò của người đã bị gỡ | 404; dòng không đổi | Sai | Đạt |
| TC-SHARE-078 | `memberId` không thuộc chuyến đi này | 404 | Bảo mật | Đạt |
| TC-SHARE-079 | Chuyến đi không tồn tại | 404 trước khi đọc bảng thành viên | Sai | Đạt |
| TC-SHARE-080 | Chủ gọi API đổi vai trò | 200, body có `memberId`, `role` mới | Đúng | Đạt |
| TC-SHARE-081 | Người không phải chủ gọi API đổi vai trò | 403, không tới tầng xử lý | Bảo mật | Đạt |
| TC-SHARE-082 | Thiếu vai trò trong body | 400 ở trường `role` | Sai | Đạt |
| TC-SHARE-083 | Gửi vai trò `OWNER` | 400: không chuyển được quyền chủ bằng API này | Bảo mật | Đạt |
| TC-SHARE-084 | Tầng xử lý báo không tìm thấy thành viên | 404 `RESOURCE_NOT_FOUND` | Sai | Đạt |
| TC-SHARE-085 | Toàn luồng: người chỉ xem sửa chuyến (403) → chủ nâng lên cùng sửa → người đó sửa chuyến và thêm hoạt động → chủ hạ về chỉ xem → người đó sửa chuyến | 403, rồi 200 và 201 **ngay request kế tiếp**, rồi lại 403 ngay | Đúng | Đạt |
| TC-SHARE-086 | Toàn luồng: người cùng sửa đổi vai trò người khác; chủ đổi vai trò người đã gỡ; chủ dùng `memberId` của chuyến A trên chuyến B | 403, 404, 404; vai trò trong cơ sở dữ liệu không đổi | Bảo mật | Đạt |

## I. Gỡ thành viên

> **Yêu cầu:** design.md rule 14.25 (giữ dòng `REMOVED`, mất quyền ngay, mời lại dùng lại dòng, không gỡ được chủ), 10.2 "Quy ước Sharing API" (đổi vai trò / gỡ), 6.2 (chỉ chủ) · **Kiểm bởi:** `SharingServiceTest`, `TripMemberControllerTest`, `SharingFlowIntegrationTest`

Chủ gỡ một thành viên đã nhận lời hoặc rút lại một lời mời đang chờ. Dòng không bị xoá mà chuyển sang trạng thái "đã gỡ": tài khoản, vai trò và ngày nhận lời cũ vẫn đọc được, và mời lại cùng email sẽ dùng lại đúng dòng đó. Mã token của lời mời bị xoá nên link trong mail chết ngay. Chủ chuyến đi không có dòng thành viên nên không gỡ được.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-SHARE-087 | Gỡ một người đã nhận lời | Dòng sang `REMOVED`; tài khoản, vai trò và ngày nhận lời giữ nguyên; không gửi mail | Đúng | Đạt |
| TC-SHARE-088 | Rút lại một lời mời đang chờ | Dòng sang `REMOVED`, mã token và hạn bị xoá: link trong mail không còn tìm thấy dòng nào | Bảo mật | Đạt |
| TC-SHARE-089 | Gỡ người đã bị gỡ | 404, không ghi gì | Sai | Đạt |
| TC-SHARE-090 | `memberId` không thuộc chuyến đi này | 404, không ghi gì | Bảo mật | Đạt |
| TC-SHARE-091 | Chuyến đi không tồn tại | 404 trước khi đọc bảng thành viên | Sai | Đạt |
| TC-SHARE-092 | Chủ gọi API gỡ | 200, `data` null, tầng xử lý được gọi đúng mã chuyến và mã dòng | Đúng | Đạt |
| TC-SHARE-093 | Người không phải chủ gọi API gỡ | 403, không tới tầng xử lý | Bảo mật | Đạt |
| TC-SHARE-094 | Gọi API gỡ khi chưa đăng nhập | 401 | Bảo mật | Đạt |
| TC-SHARE-095 | Tầng xử lý báo không tìm thấy thành viên | 404 `RESOURCE_NOT_FOUND` | Sai | Đạt |
| TC-SHARE-096 | Toàn luồng: người cùng sửa đang sửa được → chủ gỡ → người đó xem và sửa → chủ xem danh sách → chủ mời lại đúng email đó | 200 rồi 403 cả xem lẫn sửa **ngay request kế tiếp**; danh sách không còn email đó; mời lại 201 với **cùng `memberId`** và trạng thái đang chờ, số dòng trong bảng không tăng; chưa nhận lời thì vẫn 403 | Đúng | Đạt |
| TC-SHARE-097 | Toàn luồng: chủ rút lời mời đang chờ rồi người được mời dùng link cũ nhận lời | Dòng `REMOVED` và cột mã token trống trong cơ sở dữ liệu; nhận lời 400 `INVALID_TOKEN` | Bảo mật | Đạt |
| TC-SHARE-098 | Toàn luồng: người cùng sửa và người chỉ xem gỡ người khác; chủ gỡ người đã gỡ; chủ dùng `memberId` của chuyến A trên chuyến B | 403, 403, 404, 404; người chỉ xem vẫn `ACCEPTED` và vẫn xem được chuyến | Bảo mật | Đạt |

## J. Chuyến đi được chia sẻ trong danh sách

> **Yêu cầu:** design.md 10.2 "Quy ước Trip API" (chuyến đi được chia sẻ trong danh sách), 6.2 · **Kiểm bởi:** `TripRepositoryTest`, `TripServiceTest`, `TripControllerTest`, `TripListActivityCountIntegrationTest`, `SharingFlowIntegrationTest`

Trang danh sách và các chip trạng thái gồm cả chuyến đi mà người dùng là thành viên đã nhận lời, chung một danh sách với chuyến của họ, cùng bộ lọc. Mỗi dòng có mã và tên chủ chuyến để thẻ hiện "Được chia sẻ · của {tên chủ}" (giao diện làm ở Task 4.5). Lời mời đang chờ và người đã bị gỡ không thấy chuyến. Chủ chuyến được nạp cùng câu truy vấn lấy trang, nên số câu SQL của danh sách không đổi (2 câu) dù có bao nhiêu chuyến được chia sẻ. Các kịch bản của Phase 2 về danh sách và chip ([03-trip.md](03-trip.md)) vẫn đúng cho chuyến của người khác **không** chia sẻ cho mình.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-SHARE-099 | Truy vấn danh sách của một người có: một chuyến của mình, một chuyến được chia sẻ (đã nhận lời), một chuyến mới được mời, một chuyến đã bị gỡ, một chuyến của người khác không mời | Chỉ chuyến của mình và chuyến đã nhận lời | Bảo mật | Đạt |
| TC-SHARE-100 | Từ khoá tìm kiếm trên danh sách có chuyến được chia sẻ; danh sách của người đã chia sẻ | Từ khoá áp cho cả chuyến được chia sẻ; danh sách của người chia sẻ không có thêm chuyến nào của người kia | Đúng | Đạt |
| TC-SHARE-101 | Lấy một trang đầy (kích thước 1) với điều kiện nạp kèm chủ chuyến, rồi đếm theo trạng thái bằng cùng điều kiện | Tổng số dòng đúng (câu `COUNT` của phân trang không bị hỏng vì nạp kèm); chủ chuyến đã có sẵn trong bộ nhớ, không tốn thêm câu truy vấn; câu đếm theo trạng thái vẫn chạy | Đúng | Đạt |
| TC-SHARE-102 | Đếm theo trạng thái khi có chuyến được chia sẻ "đã lên kế hoạch" (mở rộng `TC-TRIP-059`) | Chuyến được chia sẻ được đếm như chuyến của mình; từ khoá vẫn lọc đúng | Đúng | Đạt |
| TC-SHARE-103 | Tầng xử lý ghép dòng danh sách | Mỗi dòng có `ownerId` và `ownerName` của chủ chuyến | Đúng | Đạt |
| TC-SHARE-104 | Gọi API danh sách | JSON mỗi dòng có `ownerId`, `ownerName` | Đúng | Đạt |
| TC-SHARE-105 | Toàn luồng: người dùng có một chuyến của mình, một chuyến được chia sẻ có một hoạt động, một chuyến mới được mời chưa nhận | Danh sách có hai chuyến; chuyến được chia sẻ mang `ownerId`, `ownerName` của chủ và `activityCount` 1; chuyến của mình mang `ownerId` của chính mình; chip "Tất cả" là 2; danh sách vẫn tốn đúng 2 câu SQL | Đúng | Đạt |
| TC-SHARE-106 | Toàn luồng: người cùng sửa, người chỉ xem, lời mời đang chờ, người đã gỡ, người lạ cùng gọi danh sách và chip của chuyến "Đà Lạt" | Hai người đầu thấy đúng một chuyến với `ownerId`, `ownerName` của chủ và chip "Tất cả" 1; ba người sau thấy 0 chuyến, chip 0; chủ thấy `ownerId` là chính mình | Bảo mật | Đạt |

## K. Chi tiết chuyến đi có thành viên và vai trò của tôi

> **Yêu cầu:** design.md 10.2 "Quy ước Trip API" (`GET /{id}` trả thêm `members` và `myRole`, số câu SQL 4 → 5), 6.2 · **Kiểm bởi:** `TripRepositoryTest`, `TripServiceTest`, `TripControllerTest`, `SharingFlowIntegrationTest`, `ActivityPlaceFlowIntegrationTest`, `TripDayFlowIntegrationTest`

Trang chi tiết nhận luôn danh sách thành viên (giống `GET /members`, chủ đứng đầu) và vai trò của chính người gọi (`OWNER` / `EDITOR` / `VIEWER`) để giao diện ẩn các nút ghi với người chỉ xem mà không phải gọi thêm. Chủ chuyến được nạp cùng câu lấy chuyến đi, thành viên kèm tài khoản trong một câu, nên chi tiết tốn 5 câu SQL bất kể số ngày, hoạt động hay thành viên. Vai trò của tôi được suy từ chính danh sách thành viên, không tốn thêm câu nào.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-SHARE-107 | Truy vấn chuyến đi kèm chủ; chuyến đã xoá; mã không tồn tại | Chủ đã có sẵn trong bộ nhớ (không tốn câu truy vấn thứ hai); hai trường hợp sau trả rỗng | Đúng | Đạt |
| TC-SHARE-108 | Tầng xử lý: chủ xem chi tiết chuyến có một người cùng sửa và một lời mời đang chờ | Có đủ ngày, hoạt động; `members` đúng như tầng chia sẻ trả về (chủ đứng đầu); `myRole` = `OWNER` | Đúng | Đạt |
| TC-SHARE-109 | Tầng xử lý: người cùng sửa xem chi tiết | `myRole` = `EDITOR` | Đúng | Đạt |
| TC-SHARE-110 | Tầng xử lý: người chỉ có lời mời đang chờ, hoặc người không có dòng nào, xem chi tiết (lưới an toàn nếu quyền mất giữa lúc kiểm và lúc đọc) | 403 trước khi tải ngày và hoạt động | Bảo mật | Đạt |
| TC-SHARE-111 | Tầng xử lý: chuyến đi không tồn tại | 404; không hỏi thành viên, không tải ngày | Sai | Đạt |
| TC-SHARE-112 | Gọi API chi tiết với quyền xem | 200; JSON có `myRole` = `OWNER` và `members` (chủ `memberId` null, người cùng sửa đủ tên); mã tài khoản lấy từ token; body không chứa chữ "password" | Đúng | Đạt |
| TC-SHARE-113 | Toàn luồng: chủ, người cùng sửa, người chỉ xem cùng xem chi tiết chuyến "Đà Lạt" | `myRole` lần lượt `OWNER`, `EDITOR`, `VIEWER`; `members` theo thứ tự chủ, hai người đã nhận lời, lời mời đang chờ; người đã gỡ không có; không lộ mật khẩu | Đúng | Đạt |
| TC-SHARE-114 | Đếm câu SQL của chi tiết chuyến đi (mở rộng `TC-ACT-098` và `TC-ACT-202`) | Đúng **5 câu**: quyền, chuyến đi kèm chủ, thành viên kèm tài khoản, ngày, hoạt động; không tăng theo số ngày, hoạt động hay địa điểm | Biên | Đạt · từng lỗi BUG-SHARE-001 |

## L. Toàn luồng mời qua HTTP

> **Yêu cầu:** design.md 10.2 "Sharing", rule 14.23–14.25 · **Kiểm bởi:** `SharingFlowIntegrationTest` (một phương thức, các bước kiểm nối tiếp nhau với dữ liệu thật trong MySQL và mail từ `MockMailProvider`)

Câu chuyện đầy đủ như người dùng trải qua, đi qua mọi tầng bằng HTTP, không ghi thẳng vào cơ sở dữ liệu: chủ mời một email **chưa có tài khoản**, mail được gửi ở luồng nền (test chờ bằng Awaitility), người được mời đăng ký đúng email, xác thực, đăng nhập, nhận lời, dùng chuyến đi theo từng vai trò, bị gỡ rồi được mời lại. Các bước dưới đây là các đoạn của cùng một bài test; một đoạn đỏ là cả bài đỏ.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-SHARE-115 | Chủ mời `Friend@Example.com` (chưa có tài khoản) với vai trò chỉ xem | 201, email đã chuẩn hoá, `userId` null, `PENDING`. Mail đầu tiên gửi tới đúng email, tiêu đề có tên chủ và tên chuyến, link có đúng mã chuyến và token 64 ký tự; cơ sở dữ liệu giữ mã băm khác token | Đúng | Đạt |
| TC-SHARE-116 | Người được mời đăng ký đúng email đó, xác thực bằng link trong mail thứ hai, đăng nhập, rồi xem chuyến và danh sách | Đăng ký 201, xác thực 200, đăng nhập 200. Có tài khoản nhưng chưa nhận lời: xem chuyến 403, danh sách 0 chuyến | Bảo mật | Đạt |
| TC-SHARE-117 | Nhận lời bằng token trên chuyến khác; người khác dùng token; đúng người nhận lời; dùng lại link | 400 `INVALID_TOKEN`; 403; 200 với đúng `memberId`, `ACCEPTED`; lần hai 400 | Bảo mật | Đạt |
| TC-SHARE-118 | Người chỉ xem vừa nhận lời xem danh sách, chi tiết và sửa chuyến | Danh sách có đúng 1 chuyến với `ownerId`, `ownerName` của chủ; chi tiết `myRole` = `VIEWER`; sửa 403 | Đúng | Đạt |
| TC-SHARE-119 | Chủ nâng lên cùng chỉnh sửa, người đó sửa chuyến và thêm hoạt động, xem lại chi tiết | 200; sửa 200, thêm 201 ngay request kế tiếp; `myRole` = `EDITOR` | Đúng | Đạt |
| TC-SHARE-120 | Chủ gỡ, người đó xem, sửa, xem danh sách | 200; xem 403, sửa 403, danh sách 0 chuyến; dòng trong cơ sở dữ liệu `REMOVED` | Bảo mật | Đạt |
| TC-SHARE-121 | Chủ mời lại cùng email với vai trò cùng sửa, người đó nhận lời bằng link mới | 201 với **cùng `memberId`**, `PENDING`, đã có tên tài khoản; mail thứ ba có token **khác** token cũ; nhận lời 200; `myRole` = `EDITOR`; bảng thành viên không thêm dòng; cả câu chuyện gửi đúng 3 mail | Đúng | Đạt |

## Kiểm tra thủ công

| Mã | Các bước | Kết quả mong đợi | Trạng thái |
|---|---|---|---|
| MT-SHARE-01 | Chạy backend profile `local` (mail qua MailHog). Đăng nhập bằng tài khoản A, tạo một chuyến đi. Trong Swagger gọi `POST /api/v1/trips/{id}/members` với `{"email": "b@example.com", "role": "VIEWER"}`. Mở MailHog `http://localhost:8025` | 201; MailHog có một mail tới `b@example.com`, tiêu đề "{tên A} mời bạn cùng lên kế hoạch cho chuyến đi "{tên chuyến}"", thân mail ghi "chỉ xem" và link dạng `http://localhost:5173/invite?trip={id}&token=...`. Log backend không in token | Chưa chạy |
| MT-SHARE-02 | Tiếp `MT-SHARE-01`: gọi lại cùng body nhưng `"role": "EDITOR"` | 201 với cùng `memberId` lần trước, `role` = `EDITOR`; MailHog có mail thứ hai với link khác link thứ nhất | Chưa chạy |
| MT-SHARE-03 | Tiếp `MT-SHARE-02`: đăng ký tài khoản `b@example.com`, xác thực qua MailHog, đăng nhập bằng B. Lấy `token` từ link của mail thứ hai, gọi `POST /api/v1/trips/{id}/members/accept` với `{"token": "..."}`. Gọi lại lần nữa với cùng token. Rồi lấy token của mail thứ nhất gọi thử | Lần một 200, `status` = `ACCEPTED`, `userId` là của B. Lần hai 400 `INVALID_TOKEN`. Token của mail thứ nhất cũng 400 (đã bị thay khi gửi lại) | Chưa chạy |
| MT-SHARE-04 | Tiếp `MT-SHARE-03` (B là EDITOR): bằng token đăng nhập của B gọi `GET /api/v1/trips/{id}`, `PATCH /api/v1/trips/{id}` với `{"title": "B sửa"}`, rồi `DELETE /api/v1/trips/{id}`. Đăng nhập một tài khoản C chưa được mời, gọi `GET /api/v1/trips/{id}` | B: xem 200, sửa 200 (tên đổi), xoá 403. C: 403 | Chưa chạy |
| MT-SHARE-05 | Tiếp `MT-SHARE-04`: A mời thêm `d@example.com` (chưa có tài khoản). Bằng token của A rồi của B gọi `GET /api/v1/trips/{id}/members`. Bằng token của C gọi cùng URL | A và B cùng nhận 200 với ba dòng theo thứ tự: A (`OWNER`, `memberId` null), B (`EDITOR`, `ACCEPTED`, có `fullName`), D (`VIEWER`, `PENDING`, `userId` và `fullName` null). C: 403 | Chưa chạy |
| MT-SHARE-06 | Tiếp `MT-SHARE-05`: A gọi `PATCH /api/v1/trips/{id}/members/{memberId của B}` với `{"role": "VIEWER"}`. B gọi `PATCH /api/v1/trips/{id}` với `{"title": "B sửa lần hai"}` rồi `GET /api/v1/trips/{id}` | A: 200, `role` = `VIEWER`. B: sửa 403 ngay lần đầu, xem vẫn 200 | Chưa chạy |
| MT-SHARE-07 | Tiếp `MT-SHARE-06`: A gọi `DELETE /api/v1/trips/{id}/members/{memberId của B}`. B gọi `GET /api/v1/trips/{id}`. A gọi `GET /api/v1/trips/{id}/members`. A mời lại `b@example.com` với `{"role": "EDITOR"}` rồi mở MailHog | A gỡ: 200, `data` null. B: 403 ngay lần đầu. Danh sách: không còn B. Mời lại: 201 với `memberId` **giống hệt** mã cũ, `status` = `PENDING`; MailHog có mail mời mới cho B | Chưa chạy |
| MT-SHARE-08 | Tiếp `MT-SHARE-04` (B đã nhận lời làm `EDITOR`): bằng token của B gọi `GET /api/v1/trips` và `GET /api/v1/trips/status-counts`. Bằng token của C gọi hai URL đó | B: danh sách có chuyến của A với `ownerId` = mã của A, `ownerName` = tên A; `total` của chip tính chuyến đó. C: danh sách không có chuyến của A, `total` không tính | Chưa chạy |
| MT-SHARE-09 | Tiếp `MT-SHARE-06` (B là `VIEWER`): bằng token của A rồi của B gọi `GET /api/v1/trips/{id}` | A: `myRole` = `OWNER`. B: `myRole` = `VIEWER`. Cả hai: `members` có A (`OWNER`, `memberId` null) đứng đầu, rồi B, rồi D (`PENDING`); không có trường mật khẩu | Chưa chạy |

## Lỗi đã phát hiện

| Mã | Phát hiện ở | Hiện tượng | Nguyên nhân | Cách sửa | Trạng thái |
|---|---|---|---|---|---|
| BUG-SHARE-001 | Mốc 9, `./gradlew build` sau khi `GET /trips/{id}` trả thêm thành viên | `TripDayFlowIntegrationTest.tripDetailCostsTheSameNumberOfQueriesWhateverTheNumberOfDaysAndActivities` đỏ: `expected: 4L but was: 5L` | Không phải lỗi của chức năng: câu thứ 5 là câu lấy thành viên đúng như design.md 10.2 chốt (4 → 5). design.md chỉ nhắc cập nhật `ActivityPlaceFlowIntegrationTest`, còn bài đếm câu SQL của Task 2.2 trong `TripDayFlowIntegrationTest` bị bỏ sót khi lên kế hoạch | Sửa con số mong đợi trong bài test cũ thành 5 kèm chú thích; ghi lại `TC-ACT-098` | Đã sửa |

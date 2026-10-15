# 09 · Chia sẻ chuyến đi

> Cập nhật: 2026-10-15 · **Task 4.2 xong**: 9 commit từ `7068e23` đến `1fdf57d` trên nhánh `feat/T4.2-share-links`, gộp vào `main` qua pull request, build xanh 1155 lượt test · Mốc 9 (test toàn luồng của liên kết chia sẻ) tại `1fdf57d` (1155 lượt test) · Mốc 8 (đếm lượt xem của liên kết) tại `e811cc2` (1151 lượt test) · Mốc 7 (trang công khai của chuyến đi) tại `226742a` (1148 lượt test) · Mốc 6 (thu hồi liên kết chia sẻ) tại `937349b` (1126 lượt test) · Mốc 5 (danh sách liên kết chia sẻ) tại `bac6e6a` (1111 lượt test) · Mốc 4 (liên kết có hạn dùng) tại `2c9397d` (1093 lượt test) · Mốc 3 (tạo liên kết chia sẻ) tại `055cc33` (1081 lượt test) · Mốc 2 (tạo / sửa chuyến đi không còn nhận chế độ hiển thị) tại `2a1faec` (1072 lượt test) · Mốc 1 (bảng lưu liên kết chia sẻ) tại `7068e23` (1069 lượt test) · kiểm tra thủ công `MT-SHARE-10` đến `MT-SHARE-16` chưa chạy · 2026-10-09 · **Task 4.1 xong**: PR #26, merge commit `5004457`, 12 commit từ `3c66957` đến `f7d317f`, build xanh 1059 lượt test · Mốc 10 tại `f7d317f` (1059 lượt test) · Mốc 9 tại `0a41839` (1058 lượt test; `BUG-SHARE-001` đã sửa) · Mốc 8 tại `c0dfcfd` (1054 lượt test) · Mốc 7 tại `a18ce26` (1051 lượt test) · Mốc 6 tại `99d46a7` (1039 lượt test) · Mốc 5 tại `6617fce` (1026 lượt test) · Mốc 4 tại `6a1ec45` (1016 lượt test) · Mốc 3 tại `e2158db` (1007 lượt test) · Mốc 2c tại `5b92632` (994 lượt test) · Mốc 2b tại `ac83f4e` (988 lượt test) · Mốc 2a tại `eb009c8` (974 lượt test) · Mốc 1 tại `3c66957` (971 lượt test) · kiểm tra thủ công `MT-SHARE-01` đến `MT-SHARE-09` chưa chạy · [Về trang chính](README.md)

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

Task 4.2 (liên kết chia sẻ và trang công khai) ghi tiếp vào file này, cũng theo từng mốc:

| Mốc | Nội dung | Phần trong file | Mã commit |
|---|---|---|---|
| 1 | Bảng lưu liên kết chia sẻ của một chuyến đi | M | `7068e23` |
| 2 | Tạo và sửa chuyến đi (`POST`, `PATCH /api/v1/trips`) không còn nhận chế độ hiển thị từ người gọi: nhãn này từ nay chỉ do liên kết chia sẻ quyết định | [03-trip.md](03-trip.md) phần L | `2a1faec` |
| 3 | Chủ chuyến đi tạo một liên kết chia sẻ (`POST /api/v1/trips/{tripId}/share-links`); chuyến đi được gắn nhãn "chia sẻ qua liên kết" | N | `055cc33` |
| 4 | Liên kết có thể có hạn dùng: chủ gửi kèm thời điểm hết hạn khi tạo, thời điểm đó phải ở tương lai | O | `2c9397d` |
| 5 | Chủ xem lại các liên kết chưa thu hồi của chuyến đi (`GET /api/v1/trips/{tripId}/share-links`), mới nhất trước; liên kết hết hạn vẫn hiện kèm dấu `expired` | P | `bac6e6a` |
| 6 | Chủ thu hồi một liên kết (`DELETE /api/v1/trips/{tripId}/share-links/{linkId}`); thu hồi liên kết cuối cùng thì chuyến đi về nhãn riêng tư | Q | `937349b` |
| 7 | Người không có tài khoản mở trang công khai của chuyến đi bằng liên kết (`GET /api/v1/public/trips/{token}`); mọi trường hợp bị từ chối là cùng một 404 | R | `226742a` |
| 8 | Mỗi lần trang công khai mở thành công, lượt xem của liên kết tăng 1; lần bị từ chối không tính | S | `e811cc2` |
| 9 | Test toàn luồng trên MySQL thật: chủ tạo liên kết, khách mở, lượt xem tăng, 4 câu SQL, chủ thu hồi, khách nhận 404; chuyến đi đã xoá và liên kết hết hạn cũng 404 | T | `1fdf57d` |

Vài từ dùng trong file:

| Từ | Nghĩa |
|---|---|
| Chủ chuyến đi | Người tạo chuyến đi. Không nằm trong bảng thành viên; hệ thống biết chủ qua chính chuyến đi |
| Thành viên | Người được chủ mời vào chuyến đi. Mỗi người một dòng trong bảng `trip_members`, dù đã nhận lời hay chưa |
| Vai trò | `EDITOR` được xem và sửa nội dung chuyến đi; `VIEWER` chỉ được xem. Trên API còn có vai trò `OWNER` dành cho chủ, nhưng giá trị này không bao giờ được ghi vào bảng |
| Trạng thái lời mời | `PENDING` đã gửi mail, chưa nhận lời · `ACCEPTED` đã nhận lời · `REMOVED` đã bị chủ gỡ. Chỉ `ACCEPTED` mới có quyền |
| Token mời | Chuỗi bí mật nằm trong link của mail mời, dùng một lần, sống 7 ngày. Hệ thống chỉ lưu bản băm (SHA-256) của nó, giống token đặt lại mật khẩu |
| Liên kết chia sẻ | Đường link chủ chuyến đi tạo ra để người **không có tài khoản** xem được lịch trình. Chỉ cho xem, không cho sửa. Mỗi liên kết một dòng trong bảng `share_links` |
| Token liên kết | Chuỗi 32 ký tự ngẫu nhiên nằm trong đường link. Khác token mời, hệ thống lưu nguyên chuỗi này (không băm) vì chủ cần xem lại và sao chép lại liên kết; nó chỉ mở được quyền xem |
| Thu hồi | Chủ tắt một liên kết. Dòng không bị xoá mà được ghi thời điểm thu hồi; không bật lại được |

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

## M. Bảng lưu liên kết chia sẻ

> **Yêu cầu:** design.md 5.2 bảng `share_links`, rule 14.26, quyết định 3 của bản rà Phase 4 (liên kết chỉ có quyền xem) · **Kiểm bởi:** `ShareLinkMappingTest`

Mốc 1 của Task 4.2 chỉ tạo chỗ lưu, chưa có API. Các kịch bản kiểm rằng chỗ lưu giữ đúng dữ liệu, mỗi token chỉ thuộc về một liên kết, và những gì làm nên một liên kết không bị đổi sau khi tạo.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-SHARE-122 | Lưu một liên kết có đủ mọi giá trị (token, hạn dùng, lúc thu hồi, 12 lượt xem, người tạo) rồi đọc lại | Mọi giá trị đọc lại y nguyên, kể cả phần triệu giây của hai thời điểm; quyền là `VIEW` | Đúng | Đạt |
| TC-SHARE-123 | Lưu một liên kết chỉ có chuyến đi, token và người tạo | Quyền mặc định là `VIEW`; không có hạn dùng; chưa thu hồi; 0 lượt xem | Đúng | Đạt |
| TC-SHARE-124 | Hai liên kết của hai chuyến đi khác nhau dùng cùng một token | Bị từ chối, nêu tên khoá `uk_share_links_token`: trang công khai tìm chuyến đi chỉ bằng token, nên một token không được thuộc về hai liên kết | Bảo mật | Đạt |
| TC-SHARE-125 | Một chuyến đi có hai liên kết với hai token khác nhau | Cả hai lưu được: không giới hạn số liên kết của một chuyến | Đúng | Đạt |
| TC-SHARE-126 | Lưu một liên kết có token dài 33 ký tự | Bị từ chối (`Data too long for column 'token'`): token luôn đúng 32 ký tự | Biên | Đạt |
| TC-SHARE-127 | Lưu lần lượt từng quyền có trong code (hiện chỉ có `VIEW`) | Cột ENUM của MySQL nhận và đọc lại đúng. Nếu sau này code thêm quyền mới mà quên sửa cột, test này đỏ | Đúng | Đạt |
| TC-SHARE-128 | Ghi thẳng vào bảng một liên kết có quyền `EDIT` | MySQL từ chối (`Data truncated for column 'permission'`): liên kết chỉ cho xem | Bảo mật | Đạt |
| TC-SHARE-129 | Xoá hẳn khỏi cơ sở dữ liệu một chuyến đi có hai liên kết, một còn sống và một đã thu hồi (việc bộ dọn dẹp sẽ làm với chuyến đã xoá mềm quá 30 ngày) | Cả hai liên kết mất theo, không còn dòng mồ côi | Đúng | Đạt |
| TC-SHARE-130 | Sau khi lưu, cố đổi chuyến đi, token, hạn dùng và người tạo của một liên kết, rồi lưu lại cùng lúc với việc ghi thời điểm thu hồi | Bốn giá trị kia không đổi trong cơ sở dữ liệu; chỉ thời điểm thu hồi được ghi | Bảo mật | Đạt |
| TC-SHARE-131 | Chủ đang giữ một liên kết đọc ra lúc có 0 lượt xem; trong lúc đó có 7 lượt mở trang công khai; rồi chủ lưu lại liên kết (thu hồi) | Lượt xem vẫn là 7: việc lưu liên kết không bao giờ ghi đè số lượt xem bằng con số cũ | Biên | Đạt |

## N. Tạo liên kết chia sẻ

> **Yêu cầu:** design.md 10.2 "Quy ước Sharing API" (liên kết), 6.2 (chỉ chủ tạo liên kết), rule 14.26 · **Kiểm bởi:** `SharingServiceTest`, `ShareLinkControllerTest`

Chủ chuyến đi bấm tạo là có ngay một liên kết chỉ xem, không phải nhập gì. Hệ thống tự sinh token ngẫu nhiên và gắn nhãn "chia sẻ qua liên kết" cho chuyến đi. Ở mốc này liên kết chưa có hạn dùng (Mốc 4), chưa xem lại được danh sách (Mốc 5), chưa thu hồi được (Mốc 6) và chưa mở được trang công khai (Mốc 7). Việc nhãn và liên kết thật sự nằm trong cơ sở dữ liệu được kiểm ở test toàn luồng của Mốc 9 và bài thủ công `MT-SHARE-11`.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-SHARE-132 | Chủ tạo liên kết cho một chuyến đi đang riêng tư | Một liên kết được lưu: thuộc đúng chuyến đi, người tạo là chủ, quyền `VIEW`, token gồm đúng 32 ký tự chữ thường và số, không có hạn dùng, chưa thu hồi, 0 lượt xem. Nhãn của chuyến đi đổi từ `PRIVATE` sang `LINK`. Không đụng tới thành viên, không gửi mail | Đúng | Đạt |
| TC-SHARE-133 | Chủ tạo hai liên kết cho cùng một chuyến đi | Cả hai được lưu, mỗi liên kết một token khác nhau; nhãn chuyến đi vẫn là `LINK` | Đúng | Đạt |
| TC-SHARE-134 | Token vừa sinh ra trùng với token của một liên kết đã có | Hệ thống sinh token khác và lưu liên kết với token chưa ai dùng, thay vì để cơ sở dữ liệu báo lỗi | Biên | Đạt |
| TC-SHARE-135 | Tạo liên kết cho chuyến đi không tồn tại hoặc đã xoá | 404 `RESOURCE_NOT_FOUND`, không lưu liên kết nào | Sai | Đạt |
| TC-SHARE-136 | Chủ gọi API tạo liên kết, không gửi nội dung gì | 201; câu trả lời có mã liên kết, token, quyền `VIEW`, 0 lượt xem và lúc tạo | Đúng | Đạt |
| TC-SHARE-137 | Người không phải chủ (kể cả thành viên cùng sửa hoặc chỉ xem) gọi API tạo liên kết | 403 `FORBIDDEN`, yêu cầu không đi tới phần xử lý | Bảo mật | Đạt |
| TC-SHARE-138 | Gọi API tạo liên kết khi chưa đăng nhập | 401, không kiểm quyền, không xử lý gì | Bảo mật | Đạt |
| TC-SHARE-139 | Mã chuyến đi trên đường dẫn là chữ (`/trips/abc/share-links`) | 400 `VALIDATION_ERROR`, không xử lý gì | Sai | Đạt |

## O. Liên kết có hạn dùng

> **Yêu cầu:** design.md 10.2 "Quy ước Sharing API" (`expiresAt` tuỳ chọn, phải ở tương lai, body không bắt buộc), rule 14.26 · **Kiểm bởi:** `SharingServiceTest`, `ShareLinkControllerTest`

Khi tạo liên kết, chủ có thể gửi kèm thời điểm hết hạn. Không gửi thì liên kết không bao giờ hết hạn. Thời điểm hết hạn được chốt lúc tạo, không sửa được về sau. Hệ thống so với "bây giờ" bằng đồng hồ của ứng dụng, nên test đứng yên ở một thời điểm cố định và không phụ thuộc ngày chạy. Mốc này mới lưu và trả lại hạn dùng; việc liên kết hết hạn thật sự không mở được nữa thuộc trang công khai (Mốc 7).

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-SHARE-140 | Chủ tạo liên kết kèm hạn dùng 7 ngày sau | 201. Liên kết lưu đúng thời điểm đó và câu trả lời có `expiresAt` bằng đúng giá trị gửi lên; chuyến đi được gắn nhãn `LINK` | Đúng | Đạt |
| TC-SHARE-141 | Chủ gửi nội dung rỗng `{}` (không có hạn dùng) | 201. Liên kết không có hạn dùng, `expiresAt` trong câu trả lời là `null`, giống hệt khi không gửi nội dung gì (`TC-SHARE-136`) | Đúng | Đạt |
| TC-SHARE-142 | Hạn dùng chỉ sau "bây giờ" đúng một phần triệu giây | Được chấp nhận và lưu nguyên giá trị | Biên | Đạt |
| TC-SHARE-143 | Hạn dùng có phần lẻ mịn hơn một phần triệu giây (0,123456789 giây) | Phần mịn hơn bị cắt bỏ (còn 0,123456): giá trị lưu và giá trị trả lời bằng nhau, đúng độ chính xác mà cơ sở dữ liệu giữ được, nên lần đọc sau cho cùng con số | Biên | Đạt |
| TC-SHARE-144 | Hạn dùng ở quá khứ (1 giây trước), đúng bằng "bây giờ", hoặc chỉ hơn "bây giờ" 999 phần tỉ giây | 400 `VALIDATION_ERROR`, lỗi ở ô `expiresAt`: "Thời điểm hết hạn của liên kết phải ở tương lai". Không lưu liên kết nào; nhãn của chuyến đi vẫn là `PRIVATE` | Sai | Đạt |
| TC-SHARE-145 | Hạn dùng chỉ có ngày, không có giờ (`2026-11-01`) | 400 `VALIDATION_ERROR` ngay khi đọc nội dung, không đi tới phần xử lý | Sai | Đạt |
| TC-SHARE-146 | Gửi kèm quyền `"permission": "EDIT"` khi tạo liên kết | 201. Giá trị gửi kèm bị bỏ qua; liên kết vẫn chỉ có quyền `VIEW` | Bảo mật | Đạt |

## P. Danh sách liên kết chia sẻ

> **Yêu cầu:** design.md 10.2 "Sharing" (`GET /share-links`) và "Quy ước Sharing API" (`expired`, không có `revokedAt`), 6.2 (chỉ chủ) · **Kiểm bởi:** `ShareLinkRepositoryTest`, `SharingServiceTest`, `ShareLinkControllerTest`

Chủ chuyến đi xem lại các liên kết mình đã tạo để sao chép lại hoặc chọn cái cần thu hồi. Danh sách chỉ gồm liên kết chưa thu hồi, cái mới tạo đứng đầu. Liên kết đã quá hạn vẫn nằm trong danh sách với `expired = true`, để chủ hiểu vì sao người nhận không mở được nữa. Dấu `expired` do máy chủ tính theo đồng hồ của ứng dụng tại lúc gọi; nó không được lưu trong cơ sở dữ liệu. Thứ tự và việc lọc được kiểm trên MySQL thật; phần còn lại kiểm ở tầng xử lý và tầng web.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-SHARE-147 | Một chuyến đi có ba liên kết tạo cách nhau 10 phút, lưu vào cơ sở dữ liệu không theo thứ tự thời gian | Danh sách trả về theo lúc tạo, mới nhất đứng đầu | Đúng | Đạt |
| TC-SHARE-148 | Ba liên kết có cùng một thời điểm tạo | Thứ tự vẫn xác định: liên kết có mã lớn hơn (tạo sau) đứng trước, nên hai lần gọi liên tiếp không đổi chỗ các dòng | Biên | Đạt |
| TC-SHARE-149 | Chuyến đi có bốn liên kết: một còn dùng được, một đã thu hồi, một quá hạn nhưng chưa thu hồi, một quá hạn rồi bị thu hồi | Danh sách có đúng hai liên kết chưa thu hồi (cái còn dùng được và cái quá hạn). Hai liên kết đã thu hồi không hiện | Đúng | Đạt |
| TC-SHARE-150 | Hai chuyến đi, mỗi chuyến một liên kết | Danh sách của chuyến nào chỉ có liên kết của chuyến đó; mã chuyến đi không có liên kết nào cho danh sách rỗng | Bảo mật | Đạt |
| TC-SHARE-151 | Chủ xem danh sách của chuyến đi có hai liên kết (một có hạn 7 ngày sau và 0 lượt xem, một không hạn và 12 lượt xem) | Mỗi dòng có mã, token, quyền `VIEW`, hạn dùng, số lượt xem, lúc tạo và `expired`; thứ tự giữ nguyên như cơ sở dữ liệu trả về. Việc xem không ghi gì và không đổi nhãn của chuyến đi | Đúng | Đạt |
| TC-SHARE-152 | Liên kết không có hạn dùng, hoặc có hạn còn sau "bây giờ" đúng một phần triệu giây | `expired` = `false` | Biên | Đạt |
| TC-SHARE-153 | Liên kết có hạn đúng bằng "bây giờ", trước "bây giờ" một phần triệu giây, hoặc trước một ngày | Liên kết vẫn nằm trong danh sách, `expired` = `true`, `expiresAt` giữ nguyên. Ranh giới trùng với lúc tạo (`TC-SHARE-144`): thời điểm bị từ chối khi tạo cũng là thời điểm bị coi là hết hạn | Biên | Đạt |
| TC-SHARE-154 | Chủ vừa tạo một liên kết, có hoặc không có hạn dùng | Câu trả lời của `POST` có thêm `expired` = `false` | Đúng | Đạt |
| TC-SHARE-155 | Chuyến đi chưa có liên kết nào | 200 với mảng rỗng `[]`, không phải `null` | Biên | Đạt |
| TC-SHARE-156 | Xem danh sách liên kết của chuyến đi không tồn tại hoặc đã xoá | 404 `RESOURCE_NOT_FOUND`, không đọc bảng liên kết | Sai | Đạt |
| TC-SHARE-157 | Chủ gọi `GET /api/v1/trips/{tripId}/share-links` | 200; hai liên kết đúng thứ tự. Mỗi dòng có đúng bảy trường `id`, `token`, `permission`, `expiresAt`, `viewCount`, `createdAt`, `expired`; không có `revokedAt`, không có thông tin chuyến đi hay người tạo | Đúng | Đạt |
| TC-SHARE-158 | Người không phải chủ (kể cả thành viên cùng sửa hoặc chỉ xem) gọi API danh sách | 403 `FORBIDDEN`, yêu cầu không đi tới phần xử lý: token của liên kết chỉ chủ được thấy | Bảo mật | Đạt |
| TC-SHARE-159 | Gọi API danh sách khi chưa đăng nhập | 401, không kiểm quyền, không xử lý gì | Bảo mật | Đạt |
| TC-SHARE-160 | Mã chuyến đi trên đường dẫn là chữ (`GET /trips/abc/share-links`) | 400 `VALIDATION_ERROR`, không xử lý gì | Sai | Đạt |

## Q. Thu hồi liên kết chia sẻ

> **Yêu cầu:** design.md 10.2 "Sharing" (`DELETE /share-links/{id}`) và "Quy ước Sharing API" (thu hồi liên kết cuối → `PRIVATE`, thu hồi lần hai → 404), rule 14.26 · **Kiểm bởi:** `ShareLinkRepositoryTest`, `SharingServiceTest`, `ShareLinkControllerTest`

Chủ chuyến đi tắt một liên kết đã phát ra. Dòng của liên kết không bị xoá mà được ghi thời điểm thu hồi, và không bật lại được: muốn chia sẻ tiếp thì tạo liên kết mới. Liên kết đã thu hồi rời khỏi danh sách (phần P). Nhãn của chuyến đi đi theo các liên kết: còn một liên kết chưa thu hồi thì vẫn là `LINK`, hết thì về `PRIVATE`. Liên kết quá hạn nhưng chưa thu hồi vẫn được tính là "còn". Mốc này mới ghi việc thu hồi; việc người cầm liên kết đã thu hồi nhận 404 thuộc trang công khai (Mốc 7) và test toàn luồng (Mốc 9).

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-SHARE-161 | Chủ thu hồi một liên kết trong khi chuyến đi còn liên kết khác chưa thu hồi | Liên kết được ghi thời điểm thu hồi bằng đúng "bây giờ"; token và số lượt xem giữ nguyên. Nhãn của chuyến đi vẫn là `LINK`. Không đụng tới thành viên, không gửi mail | Đúng | Đạt |
| TC-SHARE-162 | Chủ thu hồi liên kết cuối cùng chưa thu hồi của chuyến đi | Liên kết được ghi thời điểm thu hồi; nhãn của chuyến đi đổi từ `LINK` về `PRIVATE` | Đúng | Đạt |
| TC-SHARE-163 | Chủ thu hồi một liên kết đã quá hạn từ hôm qua, cũng là liên kết cuối cùng | Thu hồi được như mọi liên kết khác; hạn dùng cũ giữ nguyên; lúc này nhãn của chuyến đi mới về `PRIVATE` | Biên | Đạt |
| TC-SHARE-164 | Thu hồi lần thứ hai một liên kết đã thu hồi | 404 `RESOURCE_NOT_FOUND`. Thời điểm thu hồi lần đầu giữ nguyên, không ghi gì, nhãn của chuyến đi không đổi | Sai | Đạt |
| TC-SHARE-165 | Thu hồi với mã của một liên kết thuộc chuyến đi khác | 404 `RESOURCE_NOT_FOUND`, giống hệt mã không tồn tại. Không ghi gì, nhãn của chuyến đi không đổi | Bảo mật | Đạt |
| TC-SHARE-166 | Thu hồi liên kết của chuyến đi không tồn tại hoặc đã xoá | 404 `RESOURCE_NOT_FOUND`, không đọc bảng liên kết | Sai | Đạt |
| TC-SHARE-167 | Tìm một liên kết theo mã của nó kèm mã chuyến đi (trên MySQL thật) | Đúng chuyến đi: tìm thấy, kể cả khi liên kết đã thu hồi. Đúng mã nhưng sai chuyến đi, hoặc mã không có: không tìm thấy | Bảo mật | Đạt |
| TC-SHARE-168 | Chuyến đi có một liên kết đang dùng và một liên kết đã thu hồi; một chuyến đi khác có liên kết đang dùng | Hỏi "ngoài liên kết đang dùng này, chuyến đi còn liên kết nào chưa thu hồi không": không. Liên kết của chuyến đi khác không được tính | Đúng | Đạt |
| TC-SHARE-169 | Chuyến đi có hai liên kết chưa thu hồi, một trong hai đã quá hạn | Với mỗi liên kết, câu hỏi "còn liên kết nào khác chưa thu hồi không" đều trả lời có: liên kết quá hạn vẫn giữ nhãn `LINK` cho chuyến đi | Biên | Đạt |
| TC-SHARE-170 | Thu hồi một trong hai liên kết rồi đọc lại từ cơ sở dữ liệu | Thời điểm thu hồi được lưu đúng tới phần triệu giây; danh sách chỉ còn liên kết kia; bảng vẫn có đủ hai dòng | Đúng | Đạt |
| TC-SHARE-171 | Chủ gọi `DELETE /api/v1/trips/{tripId}/share-links/{linkId}` | 200, `data` là `null` | Đúng | Đạt |
| TC-SHARE-172 | Người không phải chủ (kể cả thành viên cùng sửa hoặc chỉ xem) gọi API thu hồi | 403 `FORBIDDEN`, yêu cầu không đi tới phần xử lý | Bảo mật | Đạt |
| TC-SHARE-173 | Gọi API thu hồi khi chưa đăng nhập | 401, không kiểm quyền, không xử lý gì | Bảo mật | Đạt |
| TC-SHARE-174 | Chủ gọi API thu hồi với mã liên kết không có hoặc đã thu hồi | 404 `RESOURCE_NOT_FOUND` | Sai | Đạt |
| TC-SHARE-175 | Đặt token của liên kết vào chỗ mã liên kết trên đường dẫn | 400 `VALIDATION_ERROR`, không xử lý gì: liên kết chỉ được thu hồi theo mã số | Sai | Đạt |

## R. Trang công khai của chuyến đi

> **Yêu cầu:** design.md 10.2 "Trang công khai" (không cần đăng nhập, cùng một 404, token sai dạng không hỏi cơ sở dữ liệu, nội dung `PublicTripResponse`), rule 14.26; CLAUDE.md rule 17 (không ghi token vào log) · **Kiểm bởi:** `PublicTripServiceTest`, `PublicTripControllerTest`

Ai cầm liên kết cũng mở được lịch trình mà không cần tài khoản. Trang chỉ cho thấy những gì người xem cần để theo lịch trình: tên, mô tả, điểm đến, ngày đi và về, từng ngày với các hoạt động (giờ, ghi chú của hoạt động, địa điểm). Thông tin của chủ và thành viên, chi phí, link đặt chỗ, ghi chú của ngày không bao giờ ra ngoài. Token không đúng dạng, không tồn tại, liên kết đã thu hồi hoặc đã hết hạn, chuyến đi đã xoá: người xem đều nhận cùng một câu trả lời 404, nên không đoán được token nào từng có thật. Lý do thật chỉ ghi vào log, kèm mã liên kết chứ không kèm token. Mốc này kiểm ở tầng xử lý và tầng web; việc tìm đúng dòng trên MySQL thật, số câu SQL của mỗi lần mở và lượt xem thuộc Mốc 8 và test toàn luồng của Mốc 9.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-SHARE-176 | Khách (không đăng nhập) mở trang bằng liên kết còn dùng của chuyến đi hai ngày: ngày 1 có hai hoạt động (một có địa điểm, một không), ngày 2 chưa có hoạt động | 200. Có tên, mô tả, điểm đến và toạ độ, ngày đi và về. Hai ngày theo thứ tự lịch; hoạt động giữ thứ tự hiển thị; giờ dạng `07:30`; hoạt động có ghi chú và địa điểm (tên, địa chỉ, toạ độ); hoạt động không có địa điểm thì `place` = `null`; ngày 2 có danh sách hoạt động rỗng | Đúng | Đạt |
| TC-SHARE-177 | Đọc kỹ nội dung trang của một chuyến đi có ngân sách, ghi chú của ngày, chi phí và link đặt chỗ của hoạt động | Mỗi tầng chỉ có đúng các trường đã chốt. Không có mã của bất kỳ dòng nào, không có chủ, email, thành viên, ngân sách, chi phí, link đặt chỗ, ghi chú của ngày, trạng thái hay chế độ hiển thị | Bảo mật | Đạt |
| TC-SHARE-178 | Liên kết có hạn còn sau "bây giờ" đúng một phần triệu giây | Mở được | Biên | Đạt |
| TC-SHARE-179 | Token không đúng dạng: rỗng, 31 ký tự, 33 ký tự, chữ hoa, có dấu gạch, có khoảng trắng, có chữ có dấu, chuỗi kiểu tấn công SQL | 404 `RESOURCE_NOT_FOUND`, không hỏi cơ sở dữ liệu. Token chữ hoa phải bị từ chối trước khi hỏi, vì cơ sở dữ liệu so chữ không phân biệt hoa thường và sẽ tìm ra liên kết thật | Bảo mật | Đạt |
| TC-SHARE-180 | Token đúng dạng nhưng không thuộc liên kết nào | 404 `RESOURCE_NOT_FOUND`, không đọc ngày hay hoạt động | Sai | Đạt |
| TC-SHARE-181 | Mở bằng liên kết đã thu hồi | 404 `RESOURCE_NOT_FOUND`, không đọc gì của chuyến đi | Bảo mật | Đạt |
| TC-SHARE-182 | Mở bằng liên kết có hạn đúng bằng "bây giờ", trước "bây giờ" một phần triệu giây, hoặc trước một tuần | 404 `RESOURCE_NOT_FOUND`. Ranh giới trùng với dấu `expired` trong danh sách của chủ (phần P) | Biên | Đạt |
| TC-SHARE-183 | Liên kết chưa thu hồi, còn hạn, nhưng chuyến đi đã bị xoá | 404 `RESOURCE_NOT_FOUND`, không đọc ngày hay hoạt động | Sai | Đạt |
| TC-SHARE-184 | Lý do từ chối mà hệ thống ghi lại (token lạ, liên kết đã thu hồi, hết hạn, chuyến đi đã xoá) | Nêu mã liên kết và lý do, không bao giờ chứa token | Bảo mật | Đạt |
| TC-SHARE-185 | Người mở trang gửi kèm một token đăng nhập hỏng hoặc đã hết hạn | Vẫn 200: trang công khai không đòi đăng nhập, nên chủ có phiên đăng nhập cũ vẫn mở được liên kết của mình | Đúng | Đạt |
| TC-SHARE-186 | Trang bị từ chối, bất kể lý do | 404 với cùng một nội dung: `RESOURCE_NOT_FOUND`, câu "Không tìm thấy dữ liệu yêu cầu" | Bảo mật | Đạt |
| TC-SHARE-187 | Dòng log của một lần trả 404 trên trang công khai | Đường dẫn được ghi với token thay bằng `***` (`/api/v1/public/trips/***`); token không xuất hiện ở đâu trong log | Bảo mật | Đạt |
| TC-SHARE-188 | Gửi `POST` hoặc `DELETE` tới địa chỉ trang công khai | 405, không xử lý gì: phần không cần đăng nhập chỉ cho đọc | Bảo mật | Đạt |

## S. Lượt xem của liên kết

> **Yêu cầu:** design.md 10.2 "Trang công khai" (mỗi lần gọi thành công tăng `view_count` bằng một câu `UPDATE ... SET view_count = view_count + 1`), design.md 5.2 "share_links" (lượt xem không làm đổi `updated_at`) · **Kiểm bởi:** `PublicTripServiceTest`, `ShareLinkRepositoryTest`

Mỗi lần ai đó mở được trang công khai, liên kết họ dùng được cộng thêm một lượt xem; chủ thấy con số này trong danh sách liên kết (phần P). Lần bị từ chối (404) không được tính. Mọi lần mở thành công đều tính, kể cả chủ tự mở và mỗi lần tải lại. Cơ sở dữ liệu tự cộng vào con số nó đang giữ, nên hai người mở cùng lúc đều được tính. Lượt xem không phải là một thay đổi của liên kết: thời điểm sửa gần nhất của liên kết giữ nguyên. Việc đếm trên toàn luồng HTTP và số câu SQL của mỗi lần mở (4) kiểm ở Mốc 9.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-SHARE-189 | Khách mở được trang công khai | Lượt xem của đúng liên kết đó tăng 1, một lần, và là việc cuối cùng sau khi đã đọc xong chuyến đi, các ngày và hoạt động | Đúng | Đạt |
| TC-SHARE-190 | Trang bị từ chối: token lạ, liên kết đã thu hồi, đã hết hạn, chuyến đi đã xoá, token sai dạng | Không liên kết nào được tăng lượt xem | Sai | Đạt |
| TC-SHARE-191 | Trên MySQL thật, tăng lượt xem của một liên kết hai lần liên tiếp | Liên kết đó có 2 lượt; liên kết khác của cùng chuyến đi vẫn 0; tăng cho một mã không tồn tại không đổi gì và không báo lỗi | Đúng | Đạt |
| TC-SHARE-192 | Thời điểm sửa gần nhất (`updated_at`) của liên kết sau khi tăng lượt xem | Không đổi | Biên | Đạt |
| TC-SHARE-193 | Liên kết được đọc lên lúc còn 0 lượt, sau đó một khách mở trang, rồi chủ thu hồi chính liên kết đã đọc lên đó | Lượt xem vẫn là 1: lưu việc thu hồi không ghi đè con số cũ | Biên | Đạt |

## T. Toàn luồng liên kết chia sẻ qua HTTP

> **Yêu cầu:** design.md 10.2 "Liên kết", "Trang công khai", rule 14.26; CLAUDE.md mục 8 (đếm số câu SQL) · **Kiểm bởi:** `ShareLinkFlowIntegrationTest`

Đi qua mọi tầng như người dùng thật, trên MySQL thật: phân quyền thật, bảo mật thật, cơ sở dữ liệu thật. Đây là nơi duy nhất chứng minh token tìm đúng liên kết và lịch trình trong cơ sở dữ liệu, mỗi lần mở trang tốn đúng 4 câu SQL, và các kiểu từ chối thật sự ra cùng một 404 qua HTTP. Lần chạy đầu cho thấy với chuyến đi đã xoá, câu tìm liên kết không trả dòng nào (Hibernate áp điều kiện "chưa xoá" của chuyến đi vào câu tìm), nên log ghi lý do là "token lạ"; bước kiểm "chuyến đi đã xoá" trong phần xử lý là lớp chặn thứ hai. Người xem vẫn nhận cùng một 404.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-SHARE-194 | Toàn luồng: chủ tạo liên kết không gửi body → khách (không đăng nhập) mở trang → mở lại → chủ xem danh sách → chủ thu hồi → khách mở lại | Tạo: 201, quyền xem, không hạn, 0 lượt xem, chuyến đi gắn nhãn "chia sẻ qua liên kết". Khách: 200 với tên, mô tả, ngày đi và về, ba ngày theo thứ tự, hai hoạt động của ngày 1 theo thứ tự với giờ, ghi chú, địa điểm. Danh sách: 2 lượt xem. Thu hồi: 200; thu hồi lần hai 404. Sau thu hồi: khách nhận 404 giống hệt token chưa từng có; chuyến đi về riêng tư; danh sách rỗng; dòng liên kết vẫn còn, giữ 2 lượt xem | Đúng | Đạt |
| TC-SHARE-195 | Trang công khai của một chuyến đi có ngân sách, ghi chú của ngày, chi phí và link đặt chỗ | Không có email hay tên chủ, link đặt chỗ, ghi chú của ngày, số tiền chi phí hay ngân sách, mã của dòng nào, chế độ hiển thị | Bảo mật | Đạt |
| TC-SHARE-196 | Người lạ (đã đăng nhập, không phải chủ) tạo hoặc thu hồi liên kết của chuyến đi | 403; không liên kết nào được tạo, liên kết của chủ vẫn còn | Phân quyền | Đạt |
| TC-SHARE-197 | Số câu SQL của một lần mở trang công khai | Đúng 4: liên kết kèm chuyến đi, các hoạt động kèm địa điểm, các ngày, câu tăng lượt xem | Hiệu năng | Đạt |
| TC-SHARE-198 | Mở bằng token đúng nhưng viết chữ hoa, trên MySQL thật (cơ sở dữ liệu so chữ không phân biệt hoa thường) | 404, lượt xem không tăng | Bảo mật | Đạt |
| TC-SHARE-199 | Chủ xoá chuyến đi trong khi liên kết còn dùng được | Khách nhận 404 giống hệt token chưa từng có; liên kết không bị đánh dấu thu hồi; lần bị từ chối không tính lượt xem | Sai | Đạt |
| TC-SHARE-200 | Liên kết có hạn, khách mở được, rồi tới lúc hết hạn | Khách nhận 404 giống hệt token chưa từng có; danh sách của chủ vẫn có liên kết, đánh dấu hết hạn, giữ lượt xem; chuyến đi vẫn mang nhãn "chia sẻ qua liên kết" tới khi chủ thu hồi | Biên | Đạt |
| TC-SHARE-201 | Chuyến đi có hai liên kết, chủ thu hồi một | Liên kết bị thu hồi: 404; liên kết còn lại vẫn mở được; chuyến đi giữ nhãn "chia sẻ qua liên kết"; danh sách chỉ còn liên kết đó | Đúng | Đạt |

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
| MT-SHARE-10 | Chạy backend profile `local`, đăng nhập trong Swagger. Gọi `POST /api/v1/trips` với `{"title": "Thu visibility", "startDate": "2026-11-01", "endDate": "2026-11-02", "visibility": "PUBLIC"}`. Rồi gọi `PATCH /api/v1/trips/{id}` với `{"title": "Doi ten", "visibility": "LINK"}`, và `GET /api/v1/trips/{id}` | Tạo: 201, `visibility` trong câu trả lời là `PRIVATE`. Sửa: 200, tên đổi thành "Doi ten", `visibility` vẫn `PRIVATE`. Xem lại: `PRIVATE`. Khung body mẫu của hai API trong Swagger không còn trường `visibility` | Chưa chạy |
| MT-SHARE-11 | Chạy backend profile `local`. Đăng nhập bằng A (chủ một chuyến đi) trong Swagger, gọi `POST /api/v1/trips/{id}/share-links` không có body, gọi thêm lần nữa, rồi `GET /api/v1/trips/{id}`. Đăng nhập bằng B (thành viên `EDITOR` hoặc `VIEWER` của chuyến đó) gọi cùng `POST`. Xem log backend và chạy `SELECT id, token, permission, view_count FROM share_links;` trong MySQL | A: hai lần đều 201, `permission` = `VIEW`, `viewCount` = 0, hai `token` khác nhau, mỗi token đúng 32 ký tự chữ thường và số; chi tiết chuyến đi có `visibility` = `LINK`. B: 403. Log có hai dòng `share link ... created` chỉ ghi mã liên kết, **không** in token. Bảng có đúng hai dòng với hai token đó | Chưa chạy |
| MT-SHARE-12 | Tiếp `MT-SHARE-11` (A là chủ). Trong Swagger gọi `POST /api/v1/trips/{id}/share-links` ba lần, với body lần lượt: `{"expiresAt": "2030-01-01T00:00:00Z"}`, `{"expiresAt": "2020-01-01T00:00:00Z"}`, `{}` | Lần một: 201, `expiresAt` = `2030-01-01T00:00:00Z`. Lần hai: 400 `VALIDATION_ERROR`, `details` có ô `expiresAt` với câu "Thời điểm hết hạn của liên kết phải ở tương lai"; bảng `share_links` không thêm dòng. Lần ba: 201, `expiresAt` = `null` | Chưa chạy |
| MT-SHARE-13 | Tiếp `MT-SHARE-12` (chuyến đi của A đã có bốn liên kết từ hai bài trước). Bằng tài khoản A gọi `GET /api/v1/trips/{id}/share-links`. Trong MySQL chạy `UPDATE share_links SET expires_at = '2020-01-01' WHERE id = {mã một liên kết};` rồi gọi lại. Bằng tài khoản B (thành viên của chuyến đó) gọi cùng URL. Tạo một chuyến đi mới bằng A và gọi danh sách liên kết của nó | Lần một: 200, bốn liên kết, cái tạo sau cùng đứng đầu, tất cả `expired` = `false`; token trùng với token đã nhận lúc tạo. Lần hai: liên kết vừa sửa vẫn có mặt, với `expired` = `true`. B: 403. Chuyến đi mới: 200 với `data` = `[]` | Chưa chạy |
| MT-SHARE-14 | Tiếp `MT-SHARE-13` (chuyến đi của A có bốn liên kết). Bằng tài khoản B (thành viên) gọi `DELETE /api/v1/trips/{id}/share-links/{mã một liên kết}`. Bằng A gọi cùng lệnh đó hai lần liên tiếp, rồi `GET /api/v1/trips/{id}/share-links` và `GET /api/v1/trips/{id}`. Sau đó A thu hồi lần lượt ba liên kết còn lại và gọi lại `GET /api/v1/trips/{id}`. Trong MySQL chạy `SELECT id, revoked_at FROM share_links;` | B: 403. A lần một: 200, `data` = `null`; lần hai: 404 `RESOURCE_NOT_FOUND`. Danh sách còn ba liên kết, không có cái vừa thu hồi; chuyến đi vẫn `visibility` = `LINK`. Sau khi thu hồi hết: danh sách rỗng, `visibility` = `PRIVATE`. Bảng vẫn đủ bốn dòng, cả bốn có `revoked_at` | Chưa chạy |
| MT-SHARE-15 | Chạy backend profile `local`. Bằng tài khoản A (Swagger): trong một chuyến đi, ghi chú cho ngày 1, thêm một hoạt động có ghi chú, chi phí, link đặt chỗ và địa điểm; tạo một liên kết, chép `token`. Không đăng nhập, chạy `curl.exe -s -i http://localhost:8080/api/v1/public/trips/{token}`. Chạy lại với một chữ của token đổi thành chữ hoa. A thu hồi liên kết rồi chạy lại lệnh đầu. Xem log backend. Mở Swagger, tìm nhóm "Public" | Lần đầu: 200, có lịch trình, giờ dạng `HH:mm`, ghi chú và địa điểm của hoạt động; **không** có chi phí, link đặt chỗ, ghi chú của ngày, email, tên chủ, mã nào. Token chữ hoa: 404. Sau khi thu hồi: 404 với nội dung giống hệt (trừ `timestamp`, `path`). Log có dòng `RESOURCE_NOT_FOUND on /api/v1/public/trips/***`, không in token. Swagger: endpoint không có biểu tượng khoá | Chưa chạy |
| MT-SHARE-16 | Chạy backend profile `local`. Bằng tài khoản A (Swagger): tạo một liên kết mới, chép `token`, xem danh sách liên kết (`GET /api/v1/trips/{tripId}/share-links`). Không đăng nhập, chạy hai lần `curl.exe -s -o NUL -w "%{http_code}" http://localhost:8080/api/v1/public/trips/{token}`, rồi một lần với token đổi chữ cuối. Xem lại danh sách của A | Lúc tạo: `viewCount` = 0. Hai lần đầu in `200`, lần sau in `404`. Danh sách sau đó: `viewCount` = 2 (lần 404 không tính) | Chưa chạy |

## Lỗi đã phát hiện

| Mã | Phát hiện ở | Hiện tượng | Nguyên nhân | Cách sửa | Trạng thái |
|---|---|---|---|---|---|
| BUG-SHARE-001 | Mốc 9, `./gradlew build` sau khi `GET /trips/{id}` trả thêm thành viên | `TripDayFlowIntegrationTest.tripDetailCostsTheSameNumberOfQueriesWhateverTheNumberOfDaysAndActivities` đỏ: `expected: 4L but was: 5L` | Không phải lỗi của chức năng: câu thứ 5 là câu lấy thành viên đúng như design.md 10.2 chốt (4 → 5). design.md chỉ nhắc cập nhật `ActivityPlaceFlowIntegrationTest`, còn bài đếm câu SQL của Task 2.2 trong `TripDayFlowIntegrationTest` bị bỏ sót khi lên kế hoạch | Sửa con số mong đợi trong bài test cũ thành 5 kèm chú thích; ghi lại `TC-ACT-098` | Đã sửa |

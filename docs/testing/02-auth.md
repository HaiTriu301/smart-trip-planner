# 02 · Xác thực người dùng

> Cập nhật: 2026-10-01 · build xanh tại commit `025799a` (Task 2.7, 532 lượt test) · BUG-AUTH-007 đã sửa, chờ chạy MT-AUTH-08 · [Về trang chính](README.md)

Tính năng này cho người dùng đăng ký, xác thực email, đăng nhập, giữ phiên đăng nhập, và lấy lại mật khẩu. Làm ở Task 1.1 đến 1.5.

Vài từ dùng trong file:

| Từ | Nghĩa |
|---|---|
| Access token | Thẻ ra vào ngắn hạn, sống 15 phút, gửi kèm mỗi lần gọi API |
| Refresh token | Thẻ dài hạn, sống 7 ngày, nằm trong cookie, dùng để xin access token mới |
| Phiên | Một lần đăng nhập trên một thiết bị, ứng với một refresh token |
| Thu hồi | Đánh dấu một phiên là hết hiệu lực trước hạn |

---

## A. Đăng ký

> **Yêu cầu:** design.md 10.2, rule 14.13, 14.14 · **Kiểm bởi:** `AuthControllerTest`, `AuthServiceTest`, `UserRepositoryTest`, `AuthRegistrationIntegrationTest`

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-AUTH-001 | Đăng ký với email, mật khẩu, tên hợp lệ | 201. Tài khoản có vai trò người dùng thường, gói miễn phí, chưa xác thực email. Một mail xác thực được gửi đi | Đúng | Đạt |
| TC-AUTH-002 | Đăng ký với email `Demo@Example.com` và tên có khoảng trắng thừa | Email lưu thành chữ thường `demo@example.com`, tên được cắt khoảng trắng | Đúng | Đạt |
| TC-AUTH-003 | Xem phản hồi và database sau khi đăng ký | Phản hồi **không chứa** mật khẩu. Database chỉ lưu mã băm BCrypt, không lưu mật khẩu gốc | Bảo mật | Đạt |
| TC-AUTH-004 | Gửi form có email sai định dạng, mật khẩu yếu, tên trống | 400 `VALIDATION_ERROR`, mỗi lỗi một dòng chi tiết. Không tạo tài khoản | Sai | Đạt |
| TC-AUTH-005 | Mật khẩu nhập lại khác mật khẩu | 400, báo lỗi ở ô nhập lại mật khẩu | Sai | Đạt |
| TC-AUTH-006 | Bỏ trống ô nhập lại mật khẩu | 400, chỉ báo "thiếu", không báo thêm "không khớp" | Sai | Đạt |
| TC-AUTH-007 | Đăng ký bằng email đã có người dùng | 409 `EMAIL_ALREADY_EXISTS`. Database vẫn chỉ có một tài khoản, không gửi mail | Sai | Đạt |
| TC-AUTH-008 | Hai người đăng ký cùng một email đúng cùng lúc | Một người thành công, người kia nhận 409. Không bao giờ có hai tài khoản trùng email | Biên | Đạt |

## B. Đăng nhập

> **Yêu cầu:** design.md 6.1 · **Kiểm bởi:** `AuthControllerTest`, `AuthServiceTest`, `AuthFlowIntegrationTest`

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-AUTH-009 | Tài khoản đã xác thực đăng nhập đúng mật khẩu | 200. Access token nằm trong phản hồi với thời hạn 900 giây. Refresh token nằm trong cookie mà JavaScript không đọc được | Đúng | Đạt · từng lỗi BUG-AUTH-002 |
| TC-AUTH-010 | Đăng nhập sai mật khẩu, hoặc email không tồn tại | 401 `INVALID_CREDENTIALS` với **một thông báo chung**, không cho biết email có tồn tại hay không | Bảo mật | Đạt |
| TC-AUTH-011 | Tài khoản chưa xác thực email, mật khẩu đúng | 403 `EMAIL_NOT_VERIFIED` | Sai | Đạt |
| TC-AUTH-012 | Tài khoản bị khoá, mật khẩu đúng | 403 `ACCOUNT_BLOCKED` | Sai | Đạt |
| TC-AUTH-013 | Tài khoản bị khoá, mật khẩu **sai** | 401 `INVALID_CREDENTIALS`. Người không biết mật khẩu không dò được tài khoản đang bị khoá. Không phiên nào được tạo | Bảo mật | Đạt |
| TC-AUTH-014 | Bỏ trống email hoặc mật khẩu | 400 `VALIDATION_ERROR` | Sai | Đạt |
| TC-AUTH-015 | Đăng nhập từ một trình duyệt | Phiên được lưu kèm tên trình duyệt và địa chỉ IP. Giá trị quá dài được cắt cho vừa | Đúng | Đạt |

## C. Dùng access token

> **Yêu cầu:** design.md 6.1, 10.3 · **Kiểm bởi:** `JwtTokenProviderTest`, `JwtAuthenticationFilterTest`, `UserControllerTest`

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-AUTH-016 | Gọi "thông tin của tôi" với token hợp lệ | 200, trả hồ sơ của **đúng người** ghi trong token | Đúng | Đạt |
| TC-AUTH-017 | Gọi mà không gửi token | 401 `UNAUTHORIZED` | Bảo mật | Đạt |
| TC-AUTH-018 | Gọi với token đã hết hạn | 401 `TOKEN_EXPIRED`. Mã riêng này báo cho trang web biết cần xin token mới | Bảo mật | Đạt |
| TC-AUTH-019 | Gọi với token giả, token bị sửa nội dung, token ký bằng khoá khác, hoặc token của hệ thống khác | 401 `UNAUTHORIZED` trong cả bốn trường hợp | Bảo mật | Đạt |
| TC-AUTH-020 | Tạo token cho một người dùng rồi đọc lại | Đọc lại đủ mã người dùng, email, vai trò, gói. Mỗi token có một mã riêng không trùng | Đúng | Đạt |
| TC-AUTH-021 | Người dùng thường dùng token gọi chức năng của quản trị | 403. Quản trị viên gọi thì được | Bảo mật | Đạt |
| TC-AUTH-022 | Gửi token hỏng vào một endpoint công khai | Endpoint công khai vẫn trả 200, token hỏng không làm hỏng trang công khai | Đúng | Đạt |
| TC-AUTH-023 | Token còn hạn nhưng tài khoản đã bị xoá | 404 | Sai | Đạt |

## D. Làm mới phiên và đăng xuất

> **Yêu cầu:** design.md 6.1, mục 16 · **Kiểm bởi:** `AuthServiceTest`, `RefreshTokenServiceTest`, `RefreshTokenRepositoryTest`, `AuthControllerTest`, `AuthFlowIntegrationTest`

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-AUTH-024 | Xin token mới bằng refresh token hợp lệ | 200. Refresh token cũ bị thu hồi, một cặp token mới được phát | Đúng | Đạt |
| TC-AUTH-025 | Xin token mới mà không có cookie, hoặc cookie chứa giá trị lạ | 401 | Sai | Đạt |
| TC-AUTH-026 | **Dùng lại** một refresh token đã được đổi lấy token mới (đã xoay vòng) | 401, và **mọi phiên** của người dùng đó bị thu hồi. Hệ thống coi đây là dấu hiệu token bị đánh cắp, vì token đã xoay chỉ còn ở bản sao bị lấy trộm. Kịch bản được thu hẹp ở Task 2.7: trước đó áp dụng cho **mọi** token đã bị thu hồi | Bảo mật | Đạt · từng lỗi BUG-AUTH-001, BUG-AUTH-006 |
| TC-AUTH-027 | Xin token mới bằng refresh token đã hết hạn | 401, token đó bị thu hồi. Phiên khác không bị đụng tới | Sai | Đạt |
| TC-AUTH-028 | Tài khoản bị khoá xin token mới | Bị từ chối, mọi phiên bị thu hồi | Bảo mật | Đạt |
| TC-AUTH-029 | Xem refresh token trong database | Chỉ có mã băm SHA-256. Người đọc được database cũng không dùng được token | Bảo mật | Đạt |
| TC-AUTH-030 | Thu hồi mọi phiên của một người | Phiên của người khác không bị ảnh hưởng | Đúng | Đạt |
| TC-AUTH-031 | Đăng xuất khi đang đăng nhập | 200, phiên bị thu hồi, cookie bị xoá | Đúng | Đạt |
| TC-AUTH-032 | Đăng xuất mà không có access token | 401 | Bảo mật | Đạt |
| TC-AUTH-033 | Đăng xuất khi cookie đã mất hoặc không còn hợp lệ | Vẫn 200, không báo lỗi | Biên | Đạt |
| TC-AUTH-055 | Đăng nhập trên điện thoại. Đặt lại mật khẩu rồi đăng nhập lại trên laptop. Điện thoại gửi lại refresh token cũ | Điện thoại nhận 401. Phiên mới của laptop **vẫn sống** và xin được token mới. Đây là cookie cũ, không phải token bị trộm | Bảo mật | Đạt · từng lỗi BUG-AUTH-006 |
| TC-AUTH-056 | Đăng nhập trên hai thiết bị, đăng xuất ở thiết bị thứ nhất, rồi refresh token của thiết bị đó bị gửi lại | 401. Phiên của thiết bị thứ hai vẫn sống | Bảo mật | Đạt · từng lỗi BUG-AUTH-006 |
| TC-AUTH-057 | Một token đã xoay vòng **và** đã quá hạn 7 ngày bị gửi lại | 401, không thu hồi phiên nào khác. Hết hạn được kiểm trước, nên token cũ không thể dùng làm "nút tắt mọi phiên" mãi mãi | Biên | Đạt · từng lỗi BUG-AUTH-008 |
| TC-AUTH-058 | Refresh token bị từ chối vì bất kỳ lý do nào ở trên | Phản hồi 401 kèm lệnh xoá cookie, để trình duyệt thôi gửi lại token chết mỗi lần tải trang. Phản hồi **không nói lý do**, lý do chỉ nằm trong log máy chủ | Bảo mật | Đạt · từng lỗi BUG-AUTH-006 |
| TC-AUTH-059 | Xem một phiên đã bị thu hồi trong database | Có ghi **lý do**: xoay vòng, đăng xuất, đặt lại mật khẩu, tài khoản bị khoá, hết hạn, hoặc phát hiện dùng lại. Cả 6 lý do đều lưu được. Phiên đã bị thu hồi từ trước giữ nguyên lý do đầu tiên | Đúng | Đạt |
| TC-AUTH-060 | Một phiên bị thu hồi **trước** khi hệ thống biết ghi lý do bị gửi lại | Xử lý như token đã xoay vòng: mọi phiên bị thu hồi. Giữ nguyên mức an toàn cũ cho dữ liệu cũ | Biên | Đạt |

## E. Xác thực email

> **Yêu cầu:** design.md 10.2, rule 14.15, 14.16, 14.17 · **Kiểm bởi:** `AuthServiceTest`, `VerificationTokenServiceTest`, `VerificationTokenRepositoryTest`, `MailServiceTest`, `AuthControllerTest`, `AuthFlowIntegrationTest`

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-AUTH-034 | Đăng ký rồi bấm link trong mail | Email được xác thực, sau đó đăng nhập được | Đúng | Đạt |
| TC-AUTH-035 | Xem nội dung mail xác thực | Có lời chào theo tên, có link tới trang web kèm mã xác thực | Đúng | Đạt |
| TC-AUTH-036 | Bấm link xác thực lần thứ hai | 400 `INVALID_TOKEN`. Link chỉ dùng được một lần | Sai | Đạt |
| TC-AUTH-037 | Bấm link đã quá 24 giờ | 400 `INVALID_TOKEN` | Biên | Đạt |
| TC-AUTH-038 | Gửi mã xác thực sai hoặc không tồn tại | 400 `INVALID_TOKEN`, tài khoản không đổi. Cùng một mã lỗi cho mọi lý do | Bảo mật | Đạt |
| TC-AUTH-039 | Gửi mã xác thực để trống | 400 `VALIDATION_ERROR` | Sai | Đạt |
| TC-AUTH-040 | Tài khoản chưa xác thực yêu cầu gửi lại mail | Link cũ mất hiệu lực, link mới dùng được | Đúng | Đạt |
| TC-AUTH-041 | Yêu cầu gửi lại cho email không tồn tại, đã xác thực, hoặc bị khoá | 200 với **cùng một thông báo**, không có mail nào được gửi | Bảo mật | Đạt |
| TC-AUTH-042 | Yêu cầu gửi lại với email sai định dạng | 400 `VALIDATION_ERROR` | Sai | Đạt |
| TC-AUTH-043 | Dùng link xác thực email để đặt lại mật khẩu, hoặc ngược lại | 400 `INVALID_TOKEN`. Mỗi loại link chỉ dùng cho đúng việc của nó | Bảo mật | Đạt |

## F. Quên và đặt lại mật khẩu

> **Yêu cầu:** design.md 10.2, rule 14.12, 14.13, 14.15, 14.16 · **Kiểm bởi:** `AuthServiceTest`, `VerificationTokenServiceTest`, `MailServiceTest`, `AuthControllerTest`, `AuthFlowIntegrationTest`

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-AUTH-044 | Tài khoản đã xác thực báo quên mật khẩu | 200, một mail chứa link đặt lại được gửi đi | Đúng | Đạt |
| TC-AUTH-045 | Báo quên mật khẩu với email không tồn tại, chưa xác thực, hoặc bị khoá | 200 với **cùng một thông báo**, không có mail nào được gửi | Bảo mật | Đạt |
| TC-AUTH-046 | Báo quên mật khẩu với email sai định dạng | 400 `VALIDATION_ERROR` | Sai | Đạt |
| TC-AUTH-047 | Đặt mật khẩu mới bằng link hợp lệ | 200. Mật khẩu mới đăng nhập được, mật khẩu cũ bị từ chối, **mọi phiên cũ** bị thu hồi | Đúng | Đạt |
| TC-AUTH-048 | Đặt mật khẩu mới quá yếu, hoặc nhập lại không khớp | 400 `VALIDATION_ERROR`. Link chưa bị dùng, người dùng thử lại được | Sai | Đạt |
| TC-AUTH-049 | Đặt mật khẩu mới bằng link sai hoặc đã dùng | 400 `INVALID_TOKEN`, mật khẩu không đổi | Sai | Đạt |
| TC-AUTH-050 | Xem thời hạn của link đặt lại mật khẩu | Link sống đúng 1 giờ | Biên | Đạt |

## G. Lưu trữ tài khoản

> **Yêu cầu:** design.md 5.2 bảng `users` · **Kiểm bởi:** `UserRepositoryTest`, `RefreshTokenRepositoryTest`, `VerificationTokenRepositoryTest`

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-AUTH-051 | Lưu một tài khoản rồi đọc lại | Mọi thông tin đọc lại đúng, kể cả vai trò, gói và thời điểm | Đúng | Đạt · từng lỗi BUG-AUTH-003 |
| TC-AUTH-052 | Xoá một tài khoản | Dòng dữ liệu vẫn còn trong database và được đánh dấu đã xoá. Hệ thống không còn tìm thấy tài khoản | Đúng | Đạt |
| TC-AUTH-053 | Sửa một tài khoản | Thông tin mới được lưu, ngày tạo giữ nguyên | Đúng | Đạt |
| TC-AUTH-054 | Tài khoản gói Premium đã quá ngày hết hạn | Hệ thống coi như gói miễn phí | Biên | Đạt |

---

## Kiểm tra thủ công

Chạy bằng PowerShell. Cần có: hạ tầng và backend đang chạy theo `MT-PLAT-01` và `MT-PLAT-02`.

Hai điều cần biết về PowerShell trước khi chạy:

- **Nội dung JSON phải ghi ra file rồi gửi bằng `-d "@file"`.** Viết JSON thẳng trong dòng lệnh thì PowerShell làm hỏng dấu ngoặc kép trước khi curl nhận được, và server trả 400 "không đúng định dạng JSON". Đây là `BUG-AUTH-005`.
- **Chỉ dùng chữ không dấu trong file gửi đi.** Muốn thử tiếng Việt thì dùng trang Swagger.

Nên chạy các lệnh trong một thư mục tạm, ví dụ `cd $env:TEMP`, để file JSON và file cookie không lọt vào thư mục dự án.

### MT-AUTH-01 · Đăng ký qua API

- [x] Chạy hai lệnh bên dưới: lệnh đầu ghi nội dung ra file, lệnh sau gửi đi. Trả 201, email trong phản hồi là `demo@example.com` chữ thường, không có mật khẩu.
- [x] Chạy lại lệnh gửi. Trả 409 `EMAIL_ALREADY_EXISTS`, thông báo "Email đã được sử dụng".
- [x] Sửa file: mật khẩu thành `matkhau123` ở cả hai ô và đổi sang email khác. Gửi lại. Trả 400, chi tiết lỗi ở ô `password`.
- [x] Sửa file: ô nhập lại khác mật khẩu. Gửi lại. Trả 400, chi tiết lỗi ở ô `confirmPassword`.

```powershell
'{"email":"Demo@Example.com","password":"MatKhau123","confirmPassword":"MatKhau123","fullName":"Demo"}' | Set-Content -Encoding ascii register.json
curl.exe -s -i -X POST localhost:8080/api/v1/auth/register -H "Content-Type: application/json" -d "@register.json"
```

**Kết quả:** Đạt · **Ngày:** 2026-09-30 · **Ghi chú:** chủ dự án tự chạy sau khi hướng dẫn được sửa; từng lỗi BUG-AUTH-005 cùng ngày

### MT-AUTH-02 · Xác thực email qua hộp thư

Cần có: vừa đăng ký một tài khoản mới.

- [x] Mở `http://localhost:8025`. Có mail "Xác thực email", link bắt đầu bằng `http://localhost:5173/verify-email?token=`.
- [x] Bấm link khi trang web đang chạy. Trang báo xác thực thành công.
- [x] Tải lại trang đó. Trang báo link không hợp lệ, vì link chỉ dùng một lần.
- [x] Đăng nhập bằng tài khoản vừa xác thực. Đăng nhập được.

**Kết quả:** Đạt · **Ngày:** 2026-09-30 · **Ghi chú:** chủ dự án tự chạy từng lỗi BUG-AUTH-004 ngày 2026-09-25

### MT-AUTH-03 · Đăng nhập, làm mới phiên, phát hiện dùng lại token

Cần có: tài khoản `demo@example.com` đã xác thực.

- [x] Chạy lệnh 0 để ghi nội dung đăng nhập ra file, rồi lệnh 1. Trả 200, có `accessToken`, và dòng `Set-Cookie: refresh_token=...; HttpOnly; SameSite=Lax`.
- [x] Chạy lệnh 2 với access token vừa nhận. Trả 200 kèm hồ sơ.
- [x] Chạy lệnh 2 nhưng bỏ phần `-H`. Trả 401 `UNAUTHORIZED`.
- [x] Chạy lệnh 3. Trả 200, có access token mới và cookie mới.
- [x] Chạy lệnh 4, tức là dùng lại cookie cũ. Trả 401.
- [x] Chạy lệnh 5, tức là dùng cookie mới. Cũng trả 401, vì mọi phiên đã bị thu hồi.

```powershell
# 0
'{"email":"demo@example.com","password":"MatKhau123"}' | Set-Content -Encoding ascii login.json
# 1
curl.exe -s -i -c cookies.txt -X POST localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d "@login.json"
# 2
curl.exe -s -i localhost:8080/api/v1/users/me -H "Authorization: Bearer <accessToken>"
# 3
curl.exe -s -i -b cookies.txt -c cookies2.txt -X POST localhost:8080/api/v1/auth/refresh
# 4
curl.exe -s -i -b cookies.txt -X POST localhost:8080/api/v1/auth/refresh
# 5
curl.exe -s -i -b cookies2.txt -X POST localhost:8080/api/v1/auth/refresh
```

**Kết quả:** Đạt · **Ngày:** 2026-09-30 · **Ghi chú:** chủ dự án tự chạy

### MT-AUTH-04 · Quên và đặt lại mật khẩu

Cần có: tài khoản đã xác thực, trang web đang chạy.

- [x] Ở trang quên mật khẩu, nhập email của tài khoản. Trang báo đã gửi hướng dẫn.
- [x] Mở `http://localhost:8025`. Có mail đặt lại mật khẩu.
- [x] Bấm link, nhập mật khẩu mới hai lần. Trang báo thành công.
- [x] Đăng nhập bằng mật khẩu mới. Đăng nhập được.
- [x] Đăng nhập bằng mật khẩu cũ. Bị từ chối.
- [x] Ở trang quên mật khẩu, nhập một email không tồn tại. Trang báo **y hệt** bước đầu, hộp thư không có mail mới.

**Kết quả:** Đạt · **Ngày:** 2026-09-30 · **Ghi chú:** chủ dự án tự chạy

### MT-AUTH-05 · Giữ đăng nhập trên trang web

Cần có: trang web đang chạy ở `http://localhost:5173`.

- [x] Đăng nhập. Trang chuyển tới danh sách chuyến đi.
- [x] Nhấn F5. Vẫn ở trang danh sách, không bị đưa về trang đăng nhập.
- [x] Mở thẳng `http://localhost:5173/login` khi đang đăng nhập. Trang tự chuyển về danh sách chuyến đi.
- [x] Đăng xuất. Trang về màn hình đăng nhập.
- [x] Mở thẳng `http://localhost:5173/trips` sau khi đăng xuất. Trang đưa về màn hình đăng nhập.

**Kết quả:** Đạt · **Ngày:** 2026-09-30 · **Ghi chú:** chủ dự án tự chạy

### MT-AUTH-06 · Nhiều tab dùng chung một phiên

Cần có: đang đăng nhập, mở trang web ở hai tab.

- [x] Đăng xuất ở tab thứ nhất, rồi tải lại tab thứ hai. Tab thứ hai về màn hình đăng nhập.
- [x] Đăng nhập lại ở tab thứ nhất, rồi tải lại tab thứ hai. Tab thứ hai vào thẳng danh sách chuyến đi.

**Kết quả:** Đạt · **Ngày:** 2026-09-30 · **Ghi chú:** chủ dự án tự chạy

### MT-AUTH-07 · Cookie cũ của phiên đã đăng xuất không làm mất phiên khác

Thêm ở Task 2.7 Mốc 2 để kiểm lại lỗi `BUG-AUTH-006` trên máy của bạn. Cần có: backend chạy bản mới (lúc khởi động log có dòng Flyway áp dụng phiên bản 8), tài khoản `demo@example.com` đã xác thực, file `login.json` của `MT-AUTH-03`.

- [ ] Chạy lệnh 1 (thiết bị A) và lệnh 2 (thiết bị B). Cả hai trả 200. Ghi lại `accessToken` của lệnh 1.
- [ ] Chạy lệnh 3: đăng xuất thiết bị A. Trả 200.
- [ ] Chạy lệnh 4: thiết bị A gửi lại cookie cũ. Trả 401, và có dòng `Set-Cookie: refresh_token=; ... Max-Age=0`.
- [ ] Chạy lệnh 5: thiết bị B xin token mới. Trả **200**. Trước khi sửa, bước này trả 401 vì lệnh 4 đã làm mất luôn phiên của B.

```powershell
# 1
curl.exe -s -i -c cookiesA.txt -X POST localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d "@login.json"
# 2
curl.exe -s -i -c cookiesB.txt -X POST localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d "@login.json"
# 3
curl.exe -s -i -b cookiesA.txt -X POST localhost:8080/api/v1/auth/logout -H "Authorization: Bearer <accessToken của lệnh 1>"
# 4
curl.exe -s -i -b cookiesA.txt -X POST localhost:8080/api/v1/auth/refresh
# 5
curl.exe -s -i -b cookiesB.txt -X POST localhost:8080/api/v1/auth/refresh
```

**Kết quả:** Chưa chạy

### MT-AUTH-08 · Phiên bị rớt thì dữ liệu của người trước không còn hiện (Task 2.7)

> Trạng thái: Chưa chạy sau khi sửa · kiểm lại lỗi BUG-AUTH-007

Cần có: hai tài khoản đã xác thực (A và B), A có ít nhất một chuyến đi. Chạy backend với access token sống 1 phút để không phải chờ 15 phút: `./gradlew bootRun --args='--spring.profiles.active=local --app.jwt.access-ttl=1m'`.

- [ ] Đăng nhập bằng A, mở trang chi tiết một chuyến đi của A.
- [ ] Làm cho phiên của A hết hiệu lực từ phía máy chủ bằng lệnh bên dưới (nhập mật khẩu database khi được hỏi).
- [ ] Chờ hơn 1 phút, bấm sang cửa sổ khác rồi quay lại trình duyệt. Trang đưa về màn hình đăng nhập.
- [ ] Đăng nhập **ngay** bằng B. Trang đưa tới đúng địa chỉ chuyến đi của A và báo "Bạn không có quyền xem chuyến đi này." Không lúc nào thấy tên hay hoạt động của chuyến đi A. Trước khi sửa, chuyến đi của A hiện ra vài giây rồi mới tới câu báo.
- [ ] Bấm "Chuyến đi của bạn": danh sách chỉ có chuyến đi của B.
- [ ] Đăng xuất bằng nút trên thanh điều hướng, đăng nhập lại bằng A: thấy lại chuyến đi của A (đăng xuất thường vẫn hoạt động như cũ).
- [ ] Chạy lại backend không có `--app.jwt.access-ttl=1m`.

```powershell
docker exec -it tripplanner-mysql mysql -u tripuser -p tripplanner -e "UPDATE refresh_tokens SET revoked_at = UTC_TIMESTAMP(6), revoked_reason = 'LOGOUT' WHERE revoked_at IS NULL;"
```

**Kết quả:** Chưa chạy

---

## Lỗi đã phát hiện

Nguồn: mục "Bẫy đã gặp" của Task 1.3 và 1.5 trong `WORKFLOW.md`. Các lỗi này được sửa trước khi code được gộp vào nhánh chính.

| Mã lỗi | Test case | Ngày | Hiện tượng | Nguyên nhân | Cách sửa | Trạng thái |
|---|---|---|---|---|---|---|
| BUG-AUTH-008 | TC-AUTH-057 | 2026-10-01 | `AuthFlowIntegrationTest > expiredTokenIsRejectedWithoutKillingOtherSessionsEvenIfItWasRotated() FAILED`: sau khi gửi lại token đã hết hạn, test mong đợi còn 1 phiên sống, thực tế 0 (`expected: 1 but was: 0`). Hai test mới còn lại của mốc và 531 lượt khác đều đạt | **Test sai**, hệ thống đúng. Test tự sửa thời điểm hết hạn trong database thành "một phút trước" theo giờ của máy chạy test (giờ Việt Nam), còn ứng dụng đọc cột đó theo giờ quốc tế. Lệch 7 tiếng nên với ứng dụng, token vẫn chưa hết hạn và bị xử lý đúng như token đã xoay bị dùng lại | Test để chính database tính "một phút trước" theo giờ quốc tế. Code không đổi | Đã sửa, commit `d219c3a` |
| BUG-AUTH-007 | MT-AUTH-08 | 2026-10-01 | Phát hiện khi rà soát code Phase 1–2, **chưa chạy thử**. Phiên của người A hết hiệu lực giữa chừng, trang đưa về màn hình đăng nhập. Người B đăng nhập trên cùng tab trong vòng 5 phút thì được đưa tới đúng trang A đang xem và thấy dữ liệu chuyến đi của A trong vài giây, trước khi hệ thống báo không có quyền | Dữ liệu đã tải được giữ trong bộ nhớ của trang để hiện lại cho nhanh. Bộ nhớ này chỉ được xoá khi người dùng **tự** bấm đăng xuất, không được xoá khi phiên bị rớt | Một quy tắc duy nhất thay cho việc nhớ xoá ở từng chỗ: hễ trạng thái chuyển từ "đã đăng nhập" sang "chưa đăng nhập", vì bất kỳ lý do gì, dữ liệu đã tải bị xoá hết | Đã sửa trong commit `025799a`, chờ chạy MT-AUTH-08 |
| BUG-AUTH-006 | TC-AUTH-026, TC-AUTH-055, TC-AUTH-056, TC-AUTH-058 | 2026-10-01 | Phát hiện khi rà soát code Phase 1–2; **đã tái hiện** bằng ba test mới ở Mốc 2 trước khi sửa (mong đợi còn 1 phiên sống, thực tế 0; cookie không bị xoá). Một người đăng nhập trên điện thoại và laptop, đặt lại mật khẩu trên laptop rồi đăng nhập lại. Khi điện thoại mở lại trang, nó gửi refresh token cũ (đã bị thu hồi lúc đặt lại mật khẩu). Hệ thống coi đó là token bị đánh cắp và thu hồi **mọi** phiên, kể cả phiên mới của laptop. Điện thoại không được bảo xoá cookie nên mỗi lần tải trang lại lặp lại | **Yêu cầu sai**, code làm đúng yêu cầu cũ. Hệ thống chỉ ghi "token đã bị thu hồi", không ghi **vì sao**, nên token bị thu hồi do đặt lại mật khẩu hay đăng xuất bị xử lý giống token đã xoay vòng bị dùng lại. `design.md` 6.1 (dùng lại token đã thu hồi là trộm) mâu thuẫn với rule 14.16 (đặt lại mật khẩu thu hồi mọi token). `design.md` 6.1 đã sửa ngày 2026-10-01 | Mỗi phiên bị thu hồi được ghi kèm lý do. Chỉ token bị thu hồi do xoay vòng mới bị coi là dấu hiệu trộm khi gửi lại; token bị thu hồi vì lý do khác chỉ bị từ chối. Token hết hạn được kiểm trước. Mọi lần từ chối đều kèm lệnh xoá cookie | Đã sửa, commit `d219c3a` |
| BUG-AUTH-005 | MT-AUTH-01 | 2026-09-30 | Bước 1 trả 400 `VALIDATION_ERROR` "Nội dung yêu cầu không đúng định dạng JSON hoặc sai kiểu dữ liệu" thay vì 201 | **Hướng dẫn test sai**, hệ thống đúng. PowerShell 5.1 làm hỏng dấu ngoặc kép trong JSON viết thẳng ở tham số `-d`, nên server nhận được nội dung không phải JSON. Tái hiện được trên chính máy phát triển | Hướng dẫn đổi sang ghi JSON ra file rồi gửi bằng `-d "@file"`. Đã thử: server đọc được JSON. Sửa cả `MT-AUTH-03`. Code không đổi | Đã sửa trong tài liệu |
| BUG-AUTH-001 | TC-AUTH-026 | 2026-09-23 | Token bị đánh cắp vẫn dùng tiếp được. Lệnh thu hồi mọi phiên không có tác dụng | Hệ thống thu hồi xong rồi báo lỗi 401. Việc báo lỗi làm database huỷ luôn lệnh thu hồi vừa ghi | Khai báo rằng lỗi 401 này không được huỷ những gì đã ghi | Đã sửa, commit `4ef93a0` |
| BUG-AUTH-002 | TC-AUTH-009 | 2026-09-23 | Thời hạn access token trả về là 899 giây thay vì 900 | Thời hạn được tính từ "bây giờ", sau khi vài phần nghìn giây đã trôi qua | Tính từ thời điểm phát token | Đã sửa, commit `bf6f998` |
| BUG-AUTH-003 | TC-AUTH-051 | 2026-09-23 | Test so sánh thời điểm lúc đạt lúc lỗi | **Test sai.** Java giữ thời gian chi tiết hơn database, database làm tròn khi lưu | Test làm tròn thời điểm trước khi so sánh | Đã sửa, commit `bf6f998` |
| BUG-AUTH-004 | MT-AUTH-02 | 2026-09-25 | Trang xác thực email báo link không hợp lệ dù xác thực đã thành công | Ở chế độ phát triển, trang gửi mã xác thực hai lần. Lần hai bị từ chối vì mã chỉ dùng một lần | Đổi cách gọi để hai lần gửi trùng nhau được gộp thành một | Đã sửa, commit `9f0d093` |

BUG-AUTH-006 là lỗi của yêu cầu, không phải của code: `TC-AUTH-026` vẫn đạt vì nó kiểm đúng điều yêu cầu cũ ghi. Test chỉ chứng minh code khớp yêu cầu, không chứng minh yêu cầu đúng. Lỗi lộ ra khi ghép hai quy tắc đúng riêng lẻ (thu hồi khi đặt lại mật khẩu, coi token đã thu hồi là trộm) vào một tình huống hai thiết bị.

BUG-AUTH-008 là bẫy múi giờ quen thuộc ở một chỗ mới: lần này nằm trong chính test, khi test tự ghi thời gian vào database bằng đường khác với ứng dụng. Nó được ghi vào tài liệu trước khi tìm nguyên nhân, đúng quy tắc "ghi trước, sửa sau".

BUG-AUTH-001 là lỗi đáng nhớ nhất. Test đơn vị đều đạt, chỉ test tích hợp chạy trên database thật mới phát hiện được.

BUG-AUTH-005 là lỗi đầu tiên do **test thủ công** phát hiện, và là lỗi của chính hướng dẫn test. Hệ thống từ chối đúng vì nội dung nhận được không phải JSON. Nó cho thấy vì sao test thủ công phải được chạy thật thay vì chỉ viết ra: một hướng dẫn chưa ai chạy có thể sai ngay từ bước đầu.

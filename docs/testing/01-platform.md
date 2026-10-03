# 01 · Nền tảng

> Cập nhật: 2026-10-03 · build xanh tại commit `957550e` (Task 3.4, 742 lượt test) · [Về trang chính](README.md)

Nền tảng là phần mọi tính năng khác dựa vào: khung của phản hồi, cách báo lỗi, ai được gọi gì, và cấu hình.

---

## A. Kiểm tra sống và tài liệu API

> **Yêu cầu:** design.md 10.1, 6.3 · **Kiểm bởi:** `HealthControllerTest`, `TripPlannerApplicationTests`

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-PLAT-001 | Bất kỳ ai gọi endpoint ping | 200, đúng khung phản hồi thành công, dữ liệu là "pong" | Đúng | Đạt |
| TC-PLAT-002 | Công cụ giám sát hỏi tình trạng hệ thống | 200, trạng thái `UP` | Đúng | Đạt |
| TC-PLAT-003 | Người đã đăng nhập gọi trang giám sát hiển thị biến môi trường | 404, trang này không được mở nên không đọc được secret | Bảo mật | Đạt |
| TC-PLAT-004 | Dev mở bản mô tả API sinh tự động | 200, có liệt kê endpoint | Đúng | Đạt |
| TC-PLAT-005 | Ứng dụng khởi động với database trống | Các script tạo bảng chạy đúng thứ tự, ứng dụng lên được | Đúng | Đạt |

## B. Báo lỗi

> **Yêu cầu:** design.md 10.1, 10.3 · **Kiểm bởi:** `GlobalExceptionHandlerTest`, `ErrorCodeTest`

Mọi lỗi dùng chung một khung: mã lỗi, thông báo tiếng Việt, đường dẫn, thời điểm.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-PLAT-006 | Yêu cầu một bản ghi không tồn tại | 404 `RESOURCE_NOT_FOUND`, thông báo tiếng Việt | Sai | Đạt |
| TC-PLAT-007 | Hành động vi phạm một quy tắc nghiệp vụ | Trả đúng mã của quy tắc đó, ví dụ 409 `ACTIVITY_TIME_CONFLICT` | Sai | Đạt |
| TC-PLAT-008 | Gửi form có hai ô nhập sai | 400 `VALIDATION_ERROR`, liệt kê **cả hai** ô sai | Sai | Đạt |
| TC-PLAT-009 | Gửi nội dung không đúng định dạng JSON | 400, thông báo nội dung sai định dạng | Sai | Đạt |
| TC-PLAT-010 | Gửi tham số ngoài khoảng cho phép, ví dụ kích thước trang bằng 0 | 400, nêu tên tham số sai | Biên | Đạt |
| TC-PLAT-011 | Đường dẫn cần số nhưng nhận chữ | 400, không phải lỗi 500 | Sai | Đạt |
| TC-PLAT-012 | Người đã đăng nhập gọi đường dẫn không tồn tại | 404 đúng khung lỗi, không phải trang lỗi mặc định | Sai | Đạt |
| TC-PLAT-013 | Gọi thao tác xoá vào endpoint chỉ cho đọc | 405, cho biết thao tác nào được phép | Sai | Đạt |
| TC-PLAT-014 | Máy chủ gặp lỗi không lường trước | 500 với thông báo chung, **không lộ** chi tiết nội bộ | Bảo mật | Đạt |
| TC-PLAT-015 | Tra từng mã lỗi trong file thông báo | Mã nào cũng có thông báo, dấu tiếng Việt đọc đúng | Đúng | Đạt |
| TC-PLAT-029 | Gửi nội dung kiểu form (không phải JSON) vào một endpoint nhận JSON | 415 `UNSUPPORTED_MEDIA_TYPE`, thông báo nêu API chỉ nhận JSON, phản hồi cho biết kiểu nội dung được nhận. **Không phải** lỗi 500 | Sai | Đạt · từng lỗi BUG-PLAT-003 |
| TC-PLAT-030 | Gọi một endpoint nhưng đòi dữ liệu trả về kiểu XML | 406 `NOT_ACCEPTABLE`, thông báo lỗi vẫn ở dạng JSON đúng khung | Sai | Đạt · từng lỗi BUG-PLAT-003 |
| TC-PLAT-031 | Yêu cầu một bản ghi không tồn tại, đồng thời đòi dữ liệu trả về kiểu XML | Vẫn 404 `RESOURCE_NOT_FOUND` ở dạng JSON: lỗi thật không bị che mất vì kiểu dữ liệu người gọi đòi | Biên | Đạt · từng lỗi BUG-PLAT-003 |
| TC-PLAT-032 | Gọi endpoint mà thiếu một tham số bắt buộc | 400 `VALIDATION_ERROR`, nêu tên tham số thiếu và câu "Thiếu tham số bắt buộc" | Sai | Đạt · từng lỗi BUG-PLAT-003 |

## C. Khoá truy cập mặc định

> **Yêu cầu:** design.md 6, 6.3 · **Kiểm bởi:** `SecurityConfigTest`, `TripPlannerApplicationTests`

Mặc định mọi endpoint đều bị khoá, trừ những endpoint được chủ động mở công khai.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-PLAT-016 | Khách chưa đăng nhập gọi endpoint công khai | 200 | Đúng | Đạt |
| TC-PLAT-017 | Khách chưa đăng nhập gọi endpoint được bảo vệ | 401 `UNAUTHORIZED`, yêu cầu đăng nhập | Bảo mật | Đạt |
| TC-PLAT-018 | Khách chưa đăng nhập gọi đường dẫn không tồn tại | 401, **không phải** 404, để khách không dò được đường dẫn nào có thật | Bảo mật | Đạt |
| TC-PLAT-019 | Người đã đăng nhập gọi endpoint được bảo vệ | 200 kèm dữ liệu | Đúng | Đạt |
| TC-PLAT-020 | Người đã đăng nhập gửi yêu cầu ghi không kèm mã CSRF | 200, vì API không giữ phiên nên không cần mã này | Đúng | Đạt |
| TC-PLAT-021 | Người dùng thường gọi endpoint của quản trị | 403 `FORBIDDEN` | Bảo mật | Đạt |
| TC-PLAT-022 | Quản trị viên gọi endpoint của quản trị | 200 | Đúng | Đạt |
| TC-PLAT-023 | Mật khẩu được chuẩn bị để lưu | Lưu dưới dạng băm BCrypt độ mạnh 12, không lưu mật khẩu gốc | Bảo mật | Đạt |

## D. Gọi API từ trang web khác

> **Yêu cầu:** design.md 6.3 · **Kiểm bởi:** `CorsConfigTest`

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-PLAT-024 | Trang web của dự án xin phép gọi API | Được phép, kể cả gửi cookie đăng nhập | Đúng | Đạt |
| TC-PLAT-025 | Một trang web lạ xin phép gọi API | 403, bị từ chối | Bảo mật | Đạt |

## E. Cấu hình

> **Yêu cầu:** design.md 7.1, 17.3 · **Kiểm bởi:** `AppPropertiesTest`

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-PLAT-026 | Khởi động khi chưa khai báo cấu hình gửi mail | Dùng bộ gửi mail giả, ứng dụng chạy được khi chưa có key nào | Đúng | Đạt |
| TC-PLAT-027 | Khởi động khi bỏ trống địa chỉ trang web | Ứng dụng **không khởi động**, nêu tên cấu hình thiếu | Sai | Đạt |
| TC-PLAT-028 | Khởi động khi tên dịch vụ ngoài bị gõ sai | Ứng dụng **không khởi động**, nêu tên cấu hình sai | Sai | Đạt |

## F. Kết nối Redis

> **Yêu cầu:** design.md 8.1 (Redis là nơi giữ tạm, không phải nguồn dữ liệu) · **Kiểm bởi:** `RedisConnectionIntegrationTest`

Redis là một kho chạy ở máy chủ, cạnh backend, dùng để giữ tạm câu trả lời của các nguồn bên ngoài (tìm địa điểm, dự báo thời tiết) trong một thời hạn. Task 3.4 Commit 1 mới chỉ nối ứng dụng với Redis; chưa có gì được giữ tạm. Test chạy với một Redis thật trong Docker, tự khởi động cùng test.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-PLAT-033 | Ghi một giá trị tiếng Việt vào Redis với thời hạn 5 phút rồi đọc lại | Đọc ra đúng giá trị đã ghi, kể cả dấu tiếng Việt; Redis cho biết giá trị còn hạn, không quá 5 phút | Đúng | Đạt |
| TC-PLAT-034 | Ghi một giá trị với thời hạn 0,2 giây rồi chờ | Hết hạn thì Redis tự xoá: không còn đọc được nữa. Đây là cơ chế làm cho bản giữ tạm tự hết hạn | Biên | Đạt |
| TC-PLAT-035 | Đọc một khoá chưa từng được ghi | Không ra gì, không báo lỗi | Biên | Đạt |
| TC-PLAT-036 | Xem ứng dụng trong test đang nối tới Redis nào | Nối tới Redis của chính lần chạy test (cổng ngẫu nhiên), không nối tới Redis đang chạy sẵn trên máy lập trình viên. Nếu nối nhầm, test sẽ đạt hay lỗi tuỳ máy | Bảo mật | Đạt |

Kiểm chứng ngược (2026-10-03): tạm bỏ phần nối Redis của test vào ứng dụng thì `TC-PLAT-036` đỏ, trong khi ba kịch bản còn lại **vẫn đạt** vì ứng dụng lặng lẽ dùng Redis đang chạy sẵn trên máy. Đó chính là tình huống kịch bản này được viết ra để bắt.

## G. Ứng dụng khi Redis tắt

> **Yêu cầu:** design.md 8.1 (Redis lỗi hoặc tắt thì ứng dụng vẫn chạy), 6.3 (Actuator chỉ mở `health`, `info`) · **Kiểm bởi:** `RedisDownIntegrationTest`

`/actuator/health` là địa chỉ mà nền tảng triển khai gọi để hỏi "ứng dụng còn sống không"; nhận câu trả lời DOWN thì nó ngừng gửi người dùng tới ứng dụng. Redis chỉ giữ bản tạm, mất Redis thì ứng dụng vẫn làm được việc, nên Redis tắt **không được** làm health báo DOWN. Test dừng hẳn container Redis rồi mới gọi.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-PLAT-037 | Dừng Redis, rồi gọi `/actuator/health` | 200, `status` = `UP` | Biên | Đạt |
| TC-PLAT-038 | Dừng Redis, rồi tìm địa điểm hai lần (lần đầu cũng là lần đầu ứng dụng chạm tới Redis đã chết) | Cả hai lần 200 với đủ kết quả, tổng thời gian dưới 10 giây (thực đo khoảng 1 giây). Log có dòng WARN nêu tên cache, khoá và lý do ("Unable to connect to Redis"), không in cả chồng lỗi | Biên | Đạt |
| TC-PLAT-039 | Dừng Redis, tạo một chuyến đi có điểm đến bắt đầu từ hôm nay, rồi xem thời tiết | 200, ngày đầu có dự báo, trả lời trong vòng vài giây; log có WARN của cache `weather:forecast` | Biên | Đạt |

Trước khi sửa (2026-10-03, viết test trước rồi mới sửa cấu hình): với Redis dừng, health trả `DOWN`, test đỏ. Nguyên nhân: từ Commit 1, thư viện Redis tự thêm một chỉ báo Redis vào health. Commit 2 tắt chỉ báo đó; test xanh. Đây là lỗi đã biết trước và là lý do của commit, nên không ghi thành dòng `BUG-`.

`TC-PLAT-038` cũng được viết trước khi sửa (Commit 5): với Redis dừng, tìm địa điểm trả **500** sau khoảng 1,5 giây, đúng điểm hở đã ghi ở Commit 4. Commit 5 thêm bộ xử lý lỗi cache (ghi WARN rồi bỏ qua cache) và đặt thời gian chờ Redis 1 giây; test xanh.

Giới hạn của test này: Redis dừng trên cùng máy thì bị từ chối kết nối **ngay lập tức**, nên thời gian chờ 1 giây chưa được test tự động chứng minh. Thời gian chờ chỉ phát huy khi máy chủ Redis không trả lời (mạng rớt): khi đó mặc định 60 giây của thư viện mới gây treo. Giá trị 1 giây được chốt theo thiết kế (design.md 8.1), chưa đo được bằng test.

---

## Kiểm tra thủ công

Chạy trên máy của bạn bằng PowerShell. Làm xong bước nào thì đánh dấu `[x]` bước đó, rồi điền dòng kết quả.

### MT-PLAT-01 · Hạ tầng khởi động

Cần có: Docker Desktop đang chạy, file `.env` ở thư mục gốc.

- [x] Chạy `docker compose up -d mysql redis mailhog`. Lệnh kết thúc không lỗi.
- [x] Chạy `docker compose ps`. Ba dịch vụ đều `Up` và `healthy`.
- [x] Mở `http://localhost:8025`. Thấy hộp thư MailHog.

**Kết quả:** Đạt · **Ngày:** 2026-09-30 · **Ghi chú:** chủ dự án tự chạy

### MT-PLAT-02 · Backend khởi động

Cần có: MT-PLAT-01 đã đạt, cổng 8080 trống.

- [x] Trong `backend/`, chạy `./gradlew bootRun --args='--spring.profiles.active=local'`. Log có dòng `Started TripPlannerApplication`.
- [x] Đọc các dòng Flyway trong log. Không có lỗi `checksum mismatch`.
- [x] Chạy `curl.exe -s -i http://localhost:8080/actuator/health`. Trả 200 và `{"status":"UP"}`.

**Kết quả:** Đạt · **Ngày:** 2026-09-30 · **Ghi chú:** chủ dự án tự chạy

### MT-PLAT-03 · Trang tài liệu API

Cần có: backend đang chạy.

- [x] Mở `http://localhost:8080/swagger-ui.html`. Trang mở được mà không cần đăng nhập.
- [x] Xem danh sách nhóm. Có nhóm Auth, Trip, Trip day.
- [x] Mở endpoint ping, bấm **Try it out** rồi **Execute**. Trả 200 và `"data": "pong"`.

**Kết quả:** Đạt · **Ngày:** 2026-09-30 · **Ghi chú:** chủ dự án tự chạy

### MT-PLAT-04 · Trang web gọi được backend

Cần có: backend đang chạy, trong `frontend/` có file `.env` và `npm run dev` đang chạy.

- [x] Chạy `curl.exe -s -i http://localhost:5173/api/v1/ping`. Trả 200 và `"data":"pong"`.
- [x] Chạy `curl.exe -s -i http://localhost:5173/api/v1/khong-ton-tai`. Trả 401 và `"errorCode":"UNAUTHORIZED"`.

**Kết quả:** Đạt · **Ngày:** 2026-09-30 · **Ghi chú:** chủ dự án tự chạy

### MT-PLAT-05 · Frontend build được

Cần có: đã chạy `npm install` trong `frontend/`.

- [x] Chạy `npm run lint`. Không có lỗi.
- [x] Chạy `npm run build`. Build thành công, có thư mục `dist/`.

**Kết quả:** Đạt · **Ngày:** 2026-09-30 · **Ghi chú:** chủ dự án tự chạy

### MT-PLAT-06 · Tắt Redis giữa chừng

Thêm ở Task 3.4 Commit 5. Backend đang chạy, đã đăng nhập trên Swagger.

- [ ] Gọi `GET /api/v1/places/search?q=chợ hàn`: 200.
- [ ] Chạy `docker stop tripplanner-redis`.
- [ ] Gọi lại cùng địa chỉ: vẫn **200** với cùng kết quả, trả lời trong vòng vài giây. Trong log của backend có dòng `WARN ... Cache 'place:search' failed to get entry ...: Unable to connect to Redis`, không có chồng lỗi dài.
- [ ] Gọi `GET /actuator/health`: `{"status":"UP"}`.
- [ ] Gọi `GET /api/v1/weather/trips/{id}` với một chuyến đi có điểm đến: vẫn 200, có dự báo; log có thêm dòng WARN của cache `weather:forecast`.
- [ ] Chạy `docker start tripplanner-redis`, chờ vài giây, gọi lại tìm kiếm: 200, không còn dòng WARN mới.

**Kết quả:** Chưa chạy

---

## Lỗi đã phát hiện

Mã `BUG-PLAT-001` và `BUG-PLAT-002` là ví dụ giả định trong [hướng dẫn](00-failed-test-guide.md), nên lỗi thật đầu tiên của tính năng này mang số 003.

| Mã lỗi | Test case | Ngày | Hiện tượng | Nguyên nhân | Cách sửa | Trạng thái |
|---|---|---|---|---|---|---|
| BUG-PLAT-003 | TC-PLAT-029 đến TC-PLAT-032 | 2026-10-01 | Phát hiện khi rà soát code Phase 1–2, **đã chạy thử** trên backend local. (1) Gửi đăng nhập với nội dung kiểu form thay vì JSON (`curl.exe -X POST localhost:8080/api/v1/auth/login -d "x=1"`): nhận 500 `INTERNAL_ERROR` "Đã có lỗi xảy ra, vui lòng thử lại sau", trong khi lỗi là của người gọi. (2) Gọi endpoint công khai `GET /api/v1/ping` kèm `Accept: text/xml`: nhận 401 `UNAUTHORIZED` "Bạn cần đăng nhập để tiếp tục" với `path` là `/error`. (3) Chưa chạy thử được vì chưa có endpoint nào như vậy: thiếu một tham số bắt buộc trên đường dẫn cũng sẽ ra 500 | Ba loại lỗi do người gọi gây ra (sai kiểu nội dung gửi lên, đòi kiểu dữ liệu trả về mà hệ thống không có, thiếu tham số) chưa được xử lý riêng nên rơi vào nhóm "lỗi không lường trước". Ở trường hợp (2), chính việc ghi thông báo lỗi cũng thất bại, máy chủ chuyển sang trang lỗi mặc định, và trang đó lại yêu cầu đăng nhập | Mỗi loại lỗi có mã riêng: 415 cho sai kiểu nội dung gửi lên, 406 cho kiểu dữ liệu trả về không có, 400 kèm tên tham số cho tham số thiếu. Mọi thông báo lỗi luôn được ghi ở dạng JSON, không phụ thuộc kiểu dữ liệu người gọi đòi. Đã chạy lại hai lệnh ở cột hiện tượng trên bản mới (cổng 8081): nhận 415 và 406 | Đã sửa, commit `622cba1` |

BUG-PLAT-003 không do test nào bắt được: 507 lượt test đều xanh vì mọi test đều gửi JSON đúng kiểu. Nó lộ ra khi đọc lại code theo câu hỏi "người gọi làm sai thì hệ thống trả gì".

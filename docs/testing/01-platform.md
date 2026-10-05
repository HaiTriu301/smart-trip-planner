# 01 · Nền tảng

> Cập nhật: 2026-10-05 · Task 3.8 Commit 8 (phần H: thử lại, ngắt mạch, giới hạn tần suất quanh các dịch vụ thật; `BUG-PLAT-004` đã sửa): tại `3f49f60`, build xanh, 955 lượt test; `MT-PLAT-07` chưa chạy · Commit 7 (thêm `TC-PLAT-040`: quãng đường khi Redis tắt) tại `9e20586`, 930 lượt test · trước đó build xanh tại commit `957550e` (Task 3.4, 742 lượt test) · [Về trang chính](README.md)

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
| TC-PLAT-040 | (Task 3.8) Dừng Redis, tạo một chuyến đi có hai hoạt động ở hai địa điểm, rồi xem quãng đường của ngày | 200 với một chặng 998 m, trả lời trong vòng vài giây; log có WARN của cache `route:legs` | Biên | Đạt |

Trước khi sửa (2026-10-03, viết test trước rồi mới sửa cấu hình): với Redis dừng, health trả `DOWN`, test đỏ. Nguyên nhân: từ Commit 1, thư viện Redis tự thêm một chỉ báo Redis vào health. Commit 2 tắt chỉ báo đó; test xanh. Đây là lỗi đã biết trước và là lý do của commit, nên không ghi thành dòng `BUG-`.

`TC-PLAT-038` cũng được viết trước khi sửa (Commit 5): với Redis dừng, tìm địa điểm trả **500** sau khoảng 1,5 giây, đúng điểm hở đã ghi ở Commit 4. Commit 5 thêm bộ xử lý lỗi cache (ghi WARN rồi bỏ qua cache) và đặt thời gian chờ Redis 1 giây; test xanh.

Giới hạn của test này: Redis dừng trên cùng máy thì bị từ chối kết nối **ngay lập tức**, nên thời gian chờ 1 giây chưa được test tự động chứng minh. Thời gian chờ chỉ phát huy khi máy chủ Redis không trả lời (mạng rớt): khi đó mặc định 60 giây của thư viện mới gây treo. Giá trị 1 giây được chốt theo thiết kế (design.md 8.1), chưa đo được bằng test.

---

## H. Khi dịch vụ bên ngoài lỗi: thử lại, ngắt mạch, giới hạn tần suất

> **Yêu cầu:** design.md 7.3 · **Kiểm bởi:** `FailurePredicatesTest`, `RealProvidersResilienceIntegrationTest`

Thêm ở Task 3.8 Commit 8. Áp dụng cho bốn dịch vụ thật (Photon, Nominatim, OSRM, Open-Meteo), mỗi dịch vụ một bộ riêng:

- **Thử lại:** lần gọi lỗi thoáng qua được gọi lại, tối đa 3 lần gọi cho một yêu cầu, cách nhau 0,5 giây.
- **Ngắt mạch:** dịch vụ đang sập thì ứng dụng thôi gọi nó và báo lỗi ngay. Mạch mở khi từ 10 lần gọi trở lên, trong 20 lần gần nhất có một nửa lỗi. Sau 30 giây ứng dụng cho 2 lần gọi đi thử; được thì đóng mạch lại.
- **Giới hạn tần suất:** Nominatim chỉ được gọi 1 lần mỗi giây cho cả ứng dụng (điều khoản của máy chủ công cộng). Lần tra phải chờ lượt tối đa 2 giây; hàng chờ dài hơn thì báo lỗi ngay.

Người dùng luôn nhận cùng một lỗi "dịch vụ bên ngoài không sẵn sàng" (`PROVIDER_UNAVAILABLE`), dù là lỗi thật, mạch đang mở hay hết lượt.

Lỗi nào được thử lại, lỗi nào được tính vào việc mở mạch (`FailurePredicatesTest`, không cần khởi động ứng dụng):

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-PLAT-041 | Chín loại lỗi của một dịch vụ: mã 500, 503, không trả lời kịp; mã 400, 403, 404; mã 429 (dịch vụ báo bị gọi quá nhiều); trả lời không đọc được; trả lời đọc được nhưng thiếu nội dung | **Thử lại:** chỉ 500, 503 và không trả lời kịp. **Tính vào mở mạch:** ba loại đó, thêm 429 và trả lời không đọc được. Mã 400 / 403 / 404 không tính: dịch vụ vẫn khoẻ, câu hỏi sai | Đúng | Đạt |
| TC-PLAT-042 | Một lỗi không phải của dịch vụ bên ngoài (lỗi trong code của ứng dụng) | Không thử lại, không tính vào mở mạch: lỗi của ta không được giấu sau ba lần gọi | Biên | Đạt |

Cả ứng dụng chạy thật với hai nguồn thật được bật; bốn dịch vụ là một máy chủ giả trên máy chạy test, đếm được số lần bị gọi (`RealProvidersResilienceIntegrationTest`). Để test ngắn, hai con số khác cấu hình thật: một lần gọi bỏ cuộc sau 0,3 giây (thật: 3 giây), hai lần thử cách nhau 0,02 giây (thật: 0,5 giây). Các giới hạn được chứng minh (3 lần gọi, 10 lần gọi, 1 lần tra mỗi giây) là giá trị thật.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-PLAT-043 | Tìm địa điểm; Photon trả 503, rồi 500, rồi trả lời đúng | Người dùng nhận kết quả như bình thường. Photon bị gọi đúng 3 lần | Đúng | Đạt |
| TC-PLAT-044 | Hỏi quãng đường; OSRM trả 502 mãi | Báo `PROVIDER_UNAVAILABLE` sau đúng 3 lần gọi, không gọi mãi | Sai | Đạt |
| TC-PLAT-045 | Hỏi dự báo; Open-Meteo im lặng hai lần rồi trả lời | Có dự báo. Bị gọi 3 lần: không trả lời kịp cũng được thử lại | Biên | Đạt |
| TC-PLAT-046 | Hỏi quãng đường; OSRM trả 400 (cách nó nói "không có đường giữa các điểm này") | Báo lỗi sau **1** lần gọi: hỏi lại không đổi kết quả | Sai | Đạt |
| TC-PLAT-047 | Tìm địa điểm; Photon trả 200 nhưng nội dung không phải JSON | Báo lỗi sau **1** lần gọi | Sai | Đạt |
| TC-PLAT-048 | Photon trả 500 mãi; tìm địa điểm 6 lần liên tiếp, rồi lần thứ 7 | Photon chỉ bị gọi **10** lần (không phải 18): mạch mở, các lần sau báo lỗi ngay (dưới 0,2 giây) mà không gọi nữa. Quãng đường và dự báo vẫn chạy bình thường: mỗi dịch vụ một mạch | Đúng | Đạt |
| TC-PLAT-049 | Tra 3 địa điểm liên tiếp qua Nominatim; rồi 40 lần tra cùng một lúc | Ba lần đầu mất ít nhất 1 giây (mỗi giây một lượt), cả ba thành công. Trong 40 lần cùng lúc chỉ 1 đến 4 lần được phục vụ, số còn lại nhận `PROVIDER_UNAVAILABLE` có ghi `nominatim` và **không tới Nominatim** (số lần máy chủ giả bị gọi bằng đúng số lần được phục vụ) | Biên | Đạt · từng lỗi BUG-PLAT-004 |
| TC-PLAT-050 | OSRM trả 400 cho 25 lần hỏi quãng đường liên tiếp | Cả 25 lần đều tới OSRM, mạch vẫn đóng: vài ngày có địa điểm không nối được bằng đường bộ không được cắt quãng đường của mọi người khác | Biên | Đạt |
| TC-PLAT-051 | Qua API, có đăng nhập: `GET /places/search` khi Photon chạy; rồi mở mạch của Photon và tìm một từ khoá khác | Lần đầu 200, kết quả mang nguồn `OSM` (lần đầu tiên một test chạy cả ứng dụng với nguồn bản đồ thật qua mọi tầng). Lần sau **503** `PROVIDER_UNAVAILABLE`, Photon không bị gọi thêm | Sai | Đạt |
| TC-PLAT-052 | Mở cả bốn mạch, rồi 300 lần gọi cùng một lúc (tìm địa điểm, quãng đường, dự báo) | Cả 300 lần nhận đúng `PROVIDER_UNAVAILABLE`, không dịch vụ nào bị gọi. Thêm khi sửa BUG-PLAT-004 để ép lỗi "hai lần gọi cùng lúc" lộ ra chắc chắn | Biên | Đạt · từng lỗi BUG-PLAT-004 |

Kiểm chứng ngược (2026-10-05): đổi hai hàm dự phòng trở lại `private` thì `TC-PLAT-052` đỏ; trả lại thì xanh. Sau khi sửa, chạy riêng class này 3 lần liên tiếp và một lần cả bộ đều xanh.

Điều test tự động **chưa chứng minh**:
- **Mạch tự đóng lại** sau 30 giây khi dịch vụ sống lại: không test nào chờ 30 giây. Xem bằng tay ở `MT-PLAT-07`.
- Hai con số thật 3 giây và 0,5 giây (test dùng số nhỏ hơn). Tệ nhất một yêu cầu chờ khoảng 3 × 3 + 2 × 0,5 = 10 giây trước khi báo lỗi, khi dịch vụ nhận kết nối rồi im lặng.
- `TC-PLAT-049` phụ thuộc đồng hồ: "1 đến 4 lần được phục vụ" là khoảng cho phép, không phải con số chính xác.
- Giới hạn 1 lần mỗi giây là của **một** bản ứng dụng đang chạy. Chạy hai bản cùng lúc thì mỗi bản có lượt riêng (hiện dự án chỉ chạy một bản).

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

### MT-PLAT-07 · Dịch vụ bản đồ thật mất kết nối rồi có lại

Thêm ở Task 3.8 Commit 8. Cần có mạng lúc đầu. Chạy backend với nguồn thật: `./gradlew bootRun --args='--spring.profiles.active=local --app.providers.map=osm'`, đăng nhập trên Swagger.

- [ ] Gọi `GET /api/v1/places/search?q=bảo tàng chăm`: 200, có kết quả.
- [ ] Ngắt mạng của máy (tắt Wi-Fi). Gọi tìm kiếm với một từ khoá **mới** (từ khoá cũ được trả từ bản giữ tạm): **503** `PROVIDER_UNAVAILABLE`, sau vài giây chờ.
- [ ] Gọi thêm 3 lần nữa, mỗi lần một từ khoá mới: vẫn 503. Từ lần thứ tư trở đi câu trả lời 503 về **ngay lập tức** (mạch đã mở, ứng dụng không còn chờ dịch vụ).
- [ ] Mở trang một chuyến đi trên giao diện: danh sách hoạt động vẫn hiện, chỉ ô tìm địa điểm báo lỗi.
- [ ] Nối mạng lại. Chờ hơn 30 giây. Gọi tìm kiếm với một từ khoá mới: 200 trở lại, không cần khởi động lại backend.

**Kết quả:** Chưa chạy

---

## Lỗi đã phát hiện

Mã `BUG-PLAT-001` và `BUG-PLAT-002` là ví dụ giả định trong [hướng dẫn](00-failed-test-guide.md), nên lỗi thật đầu tiên của tính năng này mang số 003.

| Mã lỗi | Test case | Ngày | Hiện tượng | Nguyên nhân | Cách sửa | Trạng thái |
|---|---|---|---|---|---|---|
| BUG-PLAT-004 | `TC-PLAT-049` (Task 3.8 Commit 8, test mới của commit) | 2026-10-05 | `RealProvidersResilienceIntegrationTest > lookupsAreSpacedOneASecondAndAQueueThatIsTooLongFailsAtOnce` **xanh khi chạy riêng, đỏ khi chạy cả bộ** (`./gradlew build`, 954 lượt, 1 đỏ). Tám lần tra địa điểm cùng lúc; một lần trong số đó không nhận lỗi "dịch vụ không sẵn sàng" như mong đợi mà nhận `UndeclaredThrowableException`, bên trong là `IllegalAccessException: class io.github.resilience4j.spring6.fallback.FallbackMethod cannot access a member of class ...OsmMapProvider with modifiers "private"` | **Code sai**, test đúng. Hàm dự phòng (hàm thư viện Resilience4j gọi thay khi mạch mở hoặc hết lượt) được khai báo `private`. Thư viện mở quyền truy cập hàm đó, gọi, rồi **đóng lại** sau mỗi lần; hai lần gọi cùng lúc thì lần này đóng đúng lúc lần kia đang gọi (đã đọc mã của `FallbackMethod.invoke` trong thư viện bản 2.4.0 để xác nhận). Chỉ lộ khi có nhiều lần gọi đồng thời, nên lúc có lúc không. Trên thực tế: lúc dịch vụ sập và nhiều người cùng tìm kiếm, một số người sẽ nhận lỗi 500 thay vì 503 | Đổi cả 5 hàm dự phòng của hai provider thành `public` (thư viện không cần mở / đóng quyền với hàm `public`), kèm ghi chú trong code. Thêm `TC-PLAT-052` (300 lần gọi cùng lúc khi mạch mở) và tăng `TC-PLAT-049` từ 8 lên 40 lần tra cùng lúc để lỗi này không thể lọt lại. Kiểm chứng ngược: trả lại `private` thì `TC-PLAT-052` đỏ | Đã sửa |
| BUG-PLAT-003 | TC-PLAT-029 đến TC-PLAT-032 | 2026-10-01 | Phát hiện khi rà soát code Phase 1–2, **đã chạy thử** trên backend local. (1) Gửi đăng nhập với nội dung kiểu form thay vì JSON (`curl.exe -X POST localhost:8080/api/v1/auth/login -d "x=1"`): nhận 500 `INTERNAL_ERROR` "Đã có lỗi xảy ra, vui lòng thử lại sau", trong khi lỗi là của người gọi. (2) Gọi endpoint công khai `GET /api/v1/ping` kèm `Accept: text/xml`: nhận 401 `UNAUTHORIZED` "Bạn cần đăng nhập để tiếp tục" với `path` là `/error`. (3) Chưa chạy thử được vì chưa có endpoint nào như vậy: thiếu một tham số bắt buộc trên đường dẫn cũng sẽ ra 500 | Ba loại lỗi do người gọi gây ra (sai kiểu nội dung gửi lên, đòi kiểu dữ liệu trả về mà hệ thống không có, thiếu tham số) chưa được xử lý riêng nên rơi vào nhóm "lỗi không lường trước". Ở trường hợp (2), chính việc ghi thông báo lỗi cũng thất bại, máy chủ chuyển sang trang lỗi mặc định, và trang đó lại yêu cầu đăng nhập | Mỗi loại lỗi có mã riêng: 415 cho sai kiểu nội dung gửi lên, 406 cho kiểu dữ liệu trả về không có, 400 kèm tên tham số cho tham số thiếu. Mọi thông báo lỗi luôn được ghi ở dạng JSON, không phụ thuộc kiểu dữ liệu người gọi đòi. Đã chạy lại hai lệnh ở cột hiện tượng trên bản mới (cổng 8081): nhận 415 và 406 | Đã sửa, commit `622cba1` |

BUG-PLAT-003 không do test nào bắt được: 507 lượt test đều xanh vì mọi test đều gửi JSON đúng kiểu. Nó lộ ra khi đọc lại code theo câu hỏi "người gọi làm sai thì hệ thống trả gì".

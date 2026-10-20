# 13 · Giới hạn số lần gọi

> Cập nhật: 2026-10-21 · **Task 4.7 xong** (8 commit `c249e92` đến `adb29c5` trên nhánh `feat/T4.7-core-rate-limit`, gộp vào `main` qua pull request; backend 1513 lượt test xanh; không có lỗi mới; `MT-RATE-01` đạt, `MT-RATE-02` đến `05` chưa chạy) · Mốc 8 (hạn mức toàn cục: 1000 lần / giờ mỗi người dùng, 200 lần / giờ mỗi địa chỉ với khách, chỉ tính `/api/...`) tại `adb29c5`, build xanh 1513 lượt test · Mốc 7 (tìm địa điểm: 60 lần / giờ mỗi người dùng, kể cả lượt trúng bộ nhớ tạm) tại `005f00f`, build xanh 1510 lượt test · 2026-10-20 · Mốc 6 (trang công khai của chuyến đi: 120 lần / giờ mỗi địa chỉ) tại `cf75ac3`, build xanh 1508 lượt test · Mốc 5 (gửi lại mail xác thực, quên mật khẩu: mỗi endpoint 5 lần / giờ mỗi địa chỉ) tại `d8b3ef9`, build xanh 1506 lượt test · Mốc 4 (đăng nhập tối đa 5 lần / 15 phút cho mỗi cặp địa chỉ + email) tại `45e5856`, build xanh 1502 lượt test · Mốc 3 (đăng ký tối đa 3 lần / giờ mỗi địa chỉ) tại `d348a38`, build xanh 1495 lượt test · Mốc 2 (bộ đếm lượt gọi trên Redis) tại `1d8c8fd`, build xanh 1492 lượt test · Mốc 1 (máy chủ không còn tin địa chỉ IP do người gọi tự khai) tại `c249e92`, build xanh 1486 lượt test · nhánh `feat/T4.7-core-rate-limit` · [Về trang chính](README.md)

Mỗi người gọi chỉ được gọi một số lần nhất định trong một khoảng thời gian: đăng nhập, đăng ký, gửi mail, trang công khai của chuyến đi, tìm địa điểm, và một hạn mức chung cho mọi lời gọi. Vượt hạn mức thì máy chủ trả 429 "Bạn gửi quá nhiều yêu cầu, vui lòng thử lại sau" và cho biết bao lâu nữa được gọi lại. Mục đích: chặn dò mật khẩu, chặn spam mail, và giữ cho máy chủ cùng các dịch vụ bản đồ công cộng không bị gọi quá nhiều trước khi đưa bản chạy thật lên mạng (Task 4.8). Phần còn lại (mức của gói trả phí, AI, thời tiết) làm ở Task 8.1.

Các mốc của Task 4.7:

| Mốc | Nội dung | Phần trong file | Mã commit |
|---|---|---|---|
| 1 | Địa chỉ IP của người gọi lấy từ kết nối thật, không tin header `X-Forwarded-For` do người gọi tự điền | A | `c249e92` |
| 2 | Bộ đếm lượt gọi trên Redis (thư viện Bucket4j): mỗi người gọi một "xô" lượt, hết lượt thì báo phải chờ bao lâu; Redis hỏng thì cho qua. Chưa gắn vào endpoint nào | B | `1d8c8fd` |
| 3 | Đăng ký tài khoản: mỗi địa chỉ IP tối đa 3 lần / giờ. Cũng là mốc gắn bộ đếm vào máy chủ: cấu hình bật / tắt, bộ lọc chạy trước mọi endpoint, câu trả lời 429 | C | `d348a38` |
| 4 | Đăng nhập: mỗi cặp địa chỉ IP + email tối đa 5 lần / 15 phút; bộ lọc đọc email trong body; Redis tắt thì vẫn đăng nhập được | D | `45e5856` |
| 5 | Hai endpoint gửi mail ("gửi lại mail xác thực", "quên mật khẩu"): mỗi endpoint 5 lần / giờ mỗi địa chỉ IP, chống spam mail | E | `d8b3ef9` |
| 6 | Trang công khai của chuyến đi: 120 lần / giờ mỗi địa chỉ IP, kể cả lượt đoán mã sai; lượt bị chặn không làm tăng số lượt xem | F | `cf75ac3` |
| 7 | Tìm địa điểm: 60 lần / giờ mỗi **người dùng** (không theo IP), kể cả lượt được trả lời từ bộ nhớ tạm | G | `005f00f` |
| 8 | Hạn mức toàn cục cho mọi lời gọi API: 1000 lần / giờ mỗi người dùng, 200 lần / giờ mỗi địa chỉ IP với khách; kiểm tra sống và Swagger không bị đếm | H | `adb29c5` |

## A. Địa chỉ IP của người gọi

> **Yêu cầu:** design.md 6.3 (địa chỉ IP lấy từ `request.getRemoteAddr()`) · **Kiểm bởi:** `AuthControllerTest`

Hạn mức theo IP chỉ có nghĩa khi người gọi không tự chọn được IP của mình. Trước mốc này máy chủ đọc phần tử đầu của header `X-Forwarded-For`, mà header này do người gọi tự điền: đổi một dòng trong header là thành "người khác", được hạn mức mới, và địa chỉ lưu kèm phiên đăng nhập (`TC-AUTH-015`) cũng là địa chỉ giả. Nay máy chủ chỉ dùng địa chỉ của kết nối. Trên bản chạy thật, nginx ghi đè header bằng IP thật và máy chủ ứng dụng tự điền địa chỉ đó (Task 4.8).

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-RATE-001 | Đăng nhập từ địa chỉ `198.51.100.20` | Phiên được tạo với tên trình duyệt và đúng địa chỉ `198.51.100.20` | Đúng | Đạt |
| TC-RATE-002 | Đăng nhập từ địa chỉ `198.51.100.20` nhưng tự gửi header `X-Forwarded-For: 203.0.113.7, 10.0.0.1` | Phiên vẫn được tạo với địa chỉ thật `198.51.100.20`, không phải `203.0.113.7`. Trước mốc này kịch bản ngược lại (tin header) là hành vi được test khẳng định | Bảo mật | Đạt |

## B. Bộ đếm lượt gọi

> **Yêu cầu:** design.md 8.2 (khoá `rate:`, Redis lỗi thì cho qua, thư viện Bucket4j) · **Kiểm bởi:** `RateLimiterTest` (Redis thật trong Docker, không khởi động cả ứng dụng)

Mốc 2 làm bộ đếm, chưa gắn vào endpoint nào: chưa có gì người dùng thấy được. Mỗi người gọi (mỗi "khoá", ví dụ một địa chỉ IP) có một xô lượt gọi trong Redis. Mỗi lần gọi lấy một lượt; xô hết lượt thì lần gọi bị từ chối kèm số giây phải chờ. Lượt quay lại **đều đặn từng lượt một**: hạn mức 3 lượt / giờ thì cứ 20 phút có lại một lượt, không bao giờ quá 3. Các test dùng đồng hồ giả để "20 phút sau" không tốn thời gian chờ.

Thư viện Bucket4j 8.21.0 được viết cho Lettuce 6 (thư viện kết nối Redis), còn dự án dùng Lettuce 7.5.2 của Spring Boot 4.1; tài liệu không ghi hai bản có hợp nhau không. Các test dưới đây chạy trên Redis thật nên cũng là bằng chứng hai bản chạy được cùng nhau.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-RATE-003 | Hạn mức 3 lượt / giờ, gọi 4 lần liền | 3 lần đầu được qua, lần 4 bị từ chối kèm "chờ 1200 giây" (20 phút: thời gian để có lại một lượt) | Biên | Đạt |
| TC-RATE-004 | Dùng hết 3 lượt, chờ 19 phút 59 giây rồi gọi; chờ thêm 1 giây rồi gọi hai lần | Lúc 19 phút 59 giây: bị từ chối, "chờ 1 giây". Đủ 20 phút: lần đầu được qua, lần sau bị từ chối "chờ 1200 giây" vì chỉ có lại đúng một lượt | Biên | Đạt |
| TC-RATE-005 | Hạn mức 1 lượt / giờ: người A dùng hết lượt, người B gọi | B vẫn được qua: mỗi người một xô riêng | Đúng | Đạt |
| TC-RATE-006 | Gọi một lần rồi xem trong Redis | Xô nằm dưới tên bắt đầu bằng `rate:` (không trùng tên bộ nhớ tạm nào), có hạn tự xoá: không quá 20 phút 10 giây (thời gian để xô đầy lại cộng 10 giây). Người không gọi nữa thì xô tự biến mất, Redis không đầy dần | Đúng | Đạt |
| TC-RATE-007 | Hạn mức 1 lượt / giờ, dùng hết lượt, rồi tắt hẳn Redis và gọi tiếp | Lần gọi được **cho qua** (không báo lỗi, không bị chặn), mất chưa tới 5 giây; log có cảnh báo `Rate limit not checked for ..., letting the call through`. Lỗi thật lúc chạy: Redis từ chối ngay vì đang mất kết nối, không phải chờ | Đúng | Đạt |
| TC-RATE-008 | Khai báo hạn mức 0 lượt, hoặc khoảng thời gian bằng 0, hoặc không có khoảng thời gian | Bị từ chối ngay lúc tạo: cấu hình sai thì ứng dụng không lên, thay vì chặn hết mọi người hoặc không chặn ai | Sai | Đạt |

## C. Giới hạn đăng ký

> **Yêu cầu:** design.md 8.2 (`POST /auth/register`: 3 lần / giờ theo IP; câu trả lời 429 kèm `X-RateLimit-Remaining`, `X-RateLimit-Reset`, `Retry-After`) · **Kiểm bởi:** `RateLimitIntegrationTest` (cả ứng dụng, MySQL và Redis thật trong Docker)

Từ mốc này bộ đếm chạy thật trong máy chủ: một bộ lọc đứng trước mọi endpoint, nhận ra lời gọi đăng ký và trừ một lượt trong xô của địa chỉ IP đó. Hết lượt thì máy chủ trả lời ngay, lời gọi không tới phần xử lý đăng ký: không tạo tài khoản, không gửi mail, không chạm database. Hạn mức đọc từ `application.yml` (`app.rate-limit.register`: 3 lượt / 1 giờ). Khi chạy test, giới hạn bị tắt cho mọi class test khác (các test cũ đăng ký hàng trăm lần từ cùng một địa chỉ); `RateLimitIntegrationTest` bật lại và mỗi test dùng một địa chỉ riêng.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-RATE-009 | Đăng ký 4 tài khoản khác nhau liền nhau từ cùng địa chỉ `203.0.113.31` | 3 lần đầu thành công (201). Lần 4 trả **429** với body lỗi chung của hệ thống: mã `RATE_LIMIT_EXCEEDED`, câu "Bạn gửi quá nhiều yêu cầu, vui lòng thử lại sau", đường dẫn `/api/v1/auth/register`; header `X-RateLimit-Remaining: 0`, `Retry-After` và `X-RateLimit-Reset` bằng nhau, khoảng 1200 giây (20 phút nữa có lại một lượt). Lần chạy thật: 1200 | Biên | Đạt |
| TC-RATE-010 | Lần đăng ký thứ 4 ở `TC-RATE-009` bị chặn | Không có tài khoản nào được tạo cho email thứ 4, và chỉ có đúng 3 mail xác thực được gửi (chờ thêm 0,7 giây vẫn là 3): lời gọi bị chặn không tới phần xử lý | Bảo mật | Đạt |
| TC-RATE-011 | Địa chỉ `203.0.113.32` đã bị chặn; địa chỉ `203.0.113.33` đăng ký | Địa chỉ thứ hai vẫn đăng ký được (201): mỗi địa chỉ một hạn mức riêng | Đúng | Đạt |
| TC-RATE-012 | Địa chỉ `203.0.113.34` đã dùng hết 3 lượt, gọi lần 4 kèm header tự điền `X-Forwarded-For: 198.51.100.99` | Vẫn **429**: đổi header không được hạn mức mới, máy chủ đếm theo địa chỉ của kết nối (phần A) | Bảo mật | Đạt |

## D. Giới hạn đăng nhập

> **Yêu cầu:** design.md 6.3, 8.2 (`POST /auth/login`: 5 lần / 15 phút theo IP + email; email đọc từ body, băm SHA-256 trong khoá; body không cho ra email vẫn bị đếm theo IP) · **Kiểm bởi:** `RateLimitIntegrationTest`, `RedisDownIntegrationTest` (`TC-PLAT-055` ở [01-platform.md](01-platform.md))

Đăng nhập được đếm theo **cặp địa chỉ IP + email**: người dò mật khẩu của một tài khoản bị chặn sau 5 lần, nhưng người khác ngồi cùng mạng (văn phòng, quán cà phê) vẫn đăng nhập được vào tài khoản của mình, và chủ tài khoản ở nơi khác cũng không bị khoá theo. Email nằm trong body của lời gọi, nên bộ lọc đọc trước body (tối đa 8 KB) rồi chuyển nguyên vẹn cho phần xử lý đăng nhập. Email được chuẩn hoá như lúc tìm tài khoản (bỏ khoảng trắng hai đầu, chữ thường) rồi băm SHA-256: Redis và log không bao giờ chứa email thô. Mọi lần đăng nhập đều bị đếm, kể cả lần đúng mật khẩu, vì bộ lọc chạy trước khi kiểm mật khẩu; nhờ vậy câu trả lời 429 giống hệt nhau dù email có tồn tại hay không (design rule 14.15).

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-RATE-013 | Đăng nhập 6 lần liền bằng cùng một email (không tồn tại) từ cùng địa chỉ | 5 lần đầu 401 `INVALID_CREDENTIALS`. Lần 6: **429** `RATE_LIMIT_EXCEEDED`, `X-RateLimit-Remaining: 0`, `Retry-After` = `X-RateLimit-Reset` khoảng 180 giây (3 phút nữa có lại một lượt). Lần chạy thật: 179–180 | Biên | Đạt |
| TC-RATE-014 | Email A đã bị chặn ở địa chỉ X; đăng nhập email B từ địa chỉ X, và email A từ địa chỉ Y | Cả hai vẫn được xử lý (401 vì sai mật khẩu, không phải 429) | Đúng | Đạt |
| TC-RATE-015 | Đăng nhập 5 lần với cùng một email viết khác nhau (`Case44@Example.com`, chữ hoa toàn bộ, có khoảng trắng ở đầu / cuối...), lần 6 viết thường | Lần 6 bị **429**: cả 6 cách viết là một email, không lách được hạn mức bằng cách đổi chữ hoa / thêm khoảng trắng | Bảo mật | Đạt |
| TC-RATE-016 | Tài khoản thật đăng nhập đúng mật khẩu 6 lần liền | 5 lần đầu 200 (bộ lọc đọc body rồi phần đăng nhập vẫn đọc được đủ). Lần 6 **429** dù đúng mật khẩu: hạn mức được kiểm trước mật khẩu, nên câu trả lời không cho biết tài khoản có tồn tại hay không | Bảo mật | Đạt |
| TC-RATE-017 | Từ một địa chỉ: 4 lần gửi body không có email, 1 lần gửi chữ không phải JSON, rồi một body dài hơn 8 KB có email | 5 lần đầu 400 (phần xử lý vẫn nhận đúng body hỏng như trước). Body dài bị **429**: cả 6 lần chung một xô "không đọc được email" của địa chỉ đó, nên body lạ không lách được luật. Sau đó một body bình thường có email vẫn được xử lý (401): xô riêng | Bảo mật | Đạt |
| TC-RATE-018 | Tài khoản thật đăng nhập với body hợp lệ nhưng dài 9 KB (thêm khoảng trắng) | 200: body dài hơn phần bộ lọc đọc vẫn tới phần đăng nhập đầy đủ, không bị cắt | Biên | Đạt |

## E. Giới hạn hai endpoint gửi mail

> **Yêu cầu:** design.md 8.2 (`POST /auth/resend-verification`, `POST /auth/forgot-password`: mỗi endpoint 5 lần / giờ theo IP), rule 14.15 (câu trả lời không cho biết email có tài khoản hay không) · **Kiểm bởi:** `RateLimitIntegrationTest`

"Gửi lại mail xác thực" và "Quên mật khẩu" là hai chỗ người ngoài có thể bắt máy chủ gửi mail. Không giới hạn thì một người có thể gửi hàng nghìn mail tới hộp thư của người khác, và làm hết hạn mức gửi mail mỗi ngày của dịch vụ gửi mail (Brevo, gói miễn phí) khi chạy thật. Mỗi endpoint đếm riêng, theo địa chỉ IP: 5 lần / giờ, cứ 12 phút có lại một lượt.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-RATE-019 | Tài khoản chưa xác thực; từ một địa chỉ bấm "gửi lại mail xác thực" 6 lần | 5 lần đầu 200, có đúng 5 mail. Lần 6: **429** `RATE_LIMIT_EXCEEDED`, `X-RateLimit-Remaining: 0`, `Retry-After` khoảng 720 giây (12 phút); chờ thêm 0,7 giây vẫn chỉ 5 mail. Lần chạy thật: 720 | Biên | Đạt |
| TC-RATE-020 | Tài khoản đã xác thực; từ một địa chỉ bấm "quên mật khẩu" 6 lần | 5 lần đầu 200, 5 mail đặt lại mật khẩu. Lần 6: **429**, không có mail thứ 6 | Biên | Đạt |
| TC-RATE-021 | Một địa chỉ đã dùng hết 5 lượt "quên mật khẩu" (với email không có tài khoản); gọi thêm một lần với email đó và một lần với email **có** tài khoản | Cả hai 429 với body giống hệt nhau (chỉ khác thời điểm): người gọi không dò được email nào có tài khoản qua giới hạn này | Bảo mật | Đạt |
| TC-RATE-022 | Một địa chỉ đã dùng hết lượt "gửi lại mail xác thực", rồi gọi "quên mật khẩu" | "Quên mật khẩu" vẫn 200: mỗi endpoint một hạn mức riêng | Đúng | Đạt |

## F. Giới hạn trang công khai của chuyến đi

> **Yêu cầu:** design.md 8.2, 10.2 (`GET /public/trips/{token}`: 120 lần / giờ theo IP), ghi nợ ở Task 4.2 · **Kiểm bởi:** `RateLimitIntegrationTest`

Trang công khai là trang ai có liên kết chia sẻ cũng mở được mà không cần tài khoản. Không giới hạn thì một người có thể làm hai việc: gọi liên tục làm máy chủ quá tải, hoặc **đoán mã** của liên kết. Mỗi địa chỉ IP được 120 lượt / giờ (cứ 30 giây có lại một lượt), tính cả lượt dùng mã sai, nên đoán mã chậm như xem trang. Lượt bị chặn dừng trước khi tới phần xử lý, nên số lượt xem mà chủ chuyến đi thấy (`view_count`) không tăng. Mã trong đường dẫn không lọt vào log: bộ lọc chỉ ghi tên luật và địa chỉ IP.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-RATE-023 | Từ một địa chỉ mở trang công khai của một liên kết 121 lần liền | 120 lần đầu 200. Lần 121: **429** `RATE_LIMIT_EXCEEDED`, `X-RateLimit-Remaining: 0`, `Retry-After` không quá 30 giây (lần chạy thật: 28). Số lượt xem của liên kết là **120**: lượt bị chặn không được đếm | Biên | Đạt |
| TC-RATE-024 | Ngay sau `TC-RATE-023`, mở cùng trang từ một địa chỉ khác | 200, số lượt xem thành 121: người khác vẫn xem được, và lượt đó được đếm | Đúng | Đạt |
| TC-RATE-025 | Từ một địa chỉ thử 119 mã không tồn tại, rồi mở mã thật 2 lần | 119 lần 404, lần mở mã thật đầu tiên 200 (lượt cuối cùng), lần sau **429**: đoán mã tốn chung hạn mức với xem trang. Log chỉ có đường dẫn đã che (`/api/v1/public/trips/***`), không có mã nào | Bảo mật | Đạt |

## G. Giới hạn tìm địa điểm

> **Yêu cầu:** design.md 8.2 (`GET /places/search`: 60 lần / giờ mỗi người dùng, mức FREE; đếm trước cache) · **Kiểm bởi:** `RateLimitIntegrationTest`

Tìm địa điểm là endpoint duy nhất ở đây gọi ra dịch vụ bên ngoài (Photon của OpenStreetMap khi chạy thật), nên được giới hạn **theo người dùng đã đăng nhập** chứ không theo địa chỉ IP: cả nhà hay cả văn phòng dùng chung một IP thì mỗi người vẫn có 60 lượt riêng. Mọi lượt đều bị đếm, kể cả lượt được trả lời từ bộ nhớ tạm Redis (câu hỏi để ngỏ ở Task 3.4, chốt ở Task 4.7): đơn giản, và chặn được cả việc làm máy chủ quá tải. Gọi khi chưa đăng nhập thì không có ai để đếm: bộ lọc để bước kiểm đăng nhập trả 401 như cũ.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-RATE-026 | Một người dùng tìm cùng từ khoá "chợ hàn" 61 lần liền | 60 lần đầu 200; trong Redis có câu trả lời đã cất (`place:search::...`), tức từ lần 2 câu trả lời đến từ bộ nhớ tạm. Lần 61: **429** `RATE_LIMIT_EXCEEDED`, `X-RateLimit-Remaining: 0`, `Retry-After` không quá 60 giây (lần chạy thật: 60): lượt trúng bộ nhớ tạm vẫn bị đếm | Biên | Đạt |
| TC-RATE-027 | Ngay sau `TC-RATE-026`, một người dùng khác tìm từ **cùng địa chỉ IP** | 200: mỗi người dùng một hạn mức riêng | Đúng | Đạt |
| TC-RATE-028 | Tìm địa điểm khi chưa đăng nhập | 401 `UNAUTHORIZED` như trước, không phải 429: không có người dùng để đếm | Sai | Đạt |

## H. Hạn mức toàn cục

> **Yêu cầu:** design.md 8.2 (toàn cục: 1000 lần / giờ mỗi người dùng đã đăng nhập, 200 lần / giờ mỗi địa chỉ IP với khách; chỉ tính `/api/**`; luật riêng kiểm trước, lượt đi qua trừ cả hai) · **Kiểm bởi:** `RateLimitIntegrationTest`

Ngoài các luật riêng ở phần C đến G, **mọi** lời gọi vào API (`/api/...`) còn bị đếm vào một hạn mức chung: người đã đăng nhập theo tài khoản (1000 lần / giờ), khách theo địa chỉ IP (200 lần / giờ). Đây là lưới an toàn cho mọi endpoint chưa có luật riêng (thời tiết, quãng đường, chuyến đi...), và chặn cả khách đổi email liên tục để lách giới hạn đăng nhập. Địa chỉ kiểm tra sống `/actuator/health` (Docker và proxy gọi liên tục) và tài liệu API (Swagger) không bị đếm.

Thứ tự: luật riêng kiểm trước. Lượt bị luật riêng chặn thì không bị trừ ở hạn mức chung; lượt được qua thì bị trừ ở cả hai.

Một điểm về cách test: với 1000 lượt / giờ, cứ 3,6 giây lại có thêm một lượt, trong khi 1000 lần gọi trong test tốn khoảng 2,3 giây trên máy phát triển. Máy chậm hơn (máy chạy CI ở Task 4.8) có thể kịp có lượt mới và làm test lúc xanh lúc đỏ. Vì vậy class test giữ con số 1000 nhưng cho lượt quay lại trong 24 giờ (`app.rate-limit.global-user.period=24h`); cấu hình thật vẫn là 1 giờ.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-RATE-029 | Khách (không đăng nhập) gọi `GET /api/v1/ping` 201 lần từ một địa chỉ | 200 lần đầu 200. Lần 201: **429** `RATE_LIMIT_EXCEEDED`, `X-RateLimit-Remaining: 0`, `Retry-After` không quá 18 giây (lần chạy thật: 18). Đăng ký từ địa chỉ đó (luật riêng còn lượt) cũng **429**: hạn mức chung áp cho mọi đường dẫn API | Biên | Đạt |
| TC-RATE-030 | Ngay sau `TC-RATE-029`, cùng địa chỉ gọi `/actuator/health` và `/v3/api-docs` | Cả hai 200: kiểm tra sống và tài liệu API không bị đếm, không bị chặn | Đúng | Đạt |
| TC-RATE-031 | Người dùng đã đăng nhập gọi `GET /api/v1/ping` 1001 lần từ một địa chỉ | 1000 lần đầu 200, lần 1001 **429**. Cùng địa chỉ đó, một người dùng khác và một khách vẫn 200: hạn mức của người dùng tính theo tài khoản, không theo IP | Biên | Đạt |
| TC-RATE-032 | Khách đăng ký 3 lần (được) rồi lần 4 (bị luật đăng ký chặn), sau đó gọi `/api/v1/ping` 197 lần rồi thêm 1 lần | 3 lần đăng ký 201, lần 4 **429**. 197 lần ping đều 200, lần 198 **429**: 3 lượt đăng ký được qua đã trừ vào hạn mức chung (còn 197, không phải 200), còn lượt bị chặn thì không trừ (nếu trừ thì lần ping thứ 197 đã bị chặn) | Đúng | Đạt |

## Kiểm tra thủ công

| Mã | Các bước | Kết quả mong đợi | Trạng thái |
|---|---|---|---|
| MT-RATE-01 | Chạy `docker compose up -d mysql redis mailhog` rồi backend profile `local`. Dùng một tài khoản đã xác thực. Trong PowerShell ghi body ra file: `'{"email":"<email>","password":"<mật khẩu>"}' \| Set-Content -Encoding ascii body.json`, rồi gọi thẳng backend: `curl.exe -s -i -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -H "X-Forwarded-For: 203.0.113.7" -d "@body.json"`. Trong MySQL chạy `SELECT ip_address, user_agent, created_at FROM refresh_tokens ORDER BY id DESC LIMIT 1;` | 200 kèm access token. Dòng mới nhất có `ip_address` là địa chỉ của máy mình (`127.0.0.1` hoặc `0:0:0:0:0:0:0:1`), **không** phải `203.0.113.7`; `user_agent` bắt đầu bằng `curl/` | Đạt · 2026-10-20, chủ dự án chạy: `ip_address` = `0:0:0:0:0:0:0:1`, `user_agent` = `curl/...` |
| MT-RATE-02 | Chạy `docker compose up -d mysql redis mailhog` rồi backend profile `local` (giới hạn bật sẵn). Trong PowerShell ghi body ra file: `'{"email":"rate-test@example.com","password":"MatKhau123","confirmPassword":"MatKhau123","fullName":"Rate Test"}' \| Set-Content -Encoding ascii reg.json`, rồi gọi 4 lần liền: `curl.exe -s -i -X POST http://localhost:8080/api/v1/auth/register -H "Content-Type: application/json" -d "@reg.json"`. Sau đó xem Redis: `docker exec tripplanner-redis redis-cli --scan --pattern "rate:*"` và `docker exec tripplanner-redis redis-cli ttl "<khoá vừa thấy>"`. Muốn thử lại ngay: `docker exec tripplanner-redis redis-cli del "<khoá>"` | Lần 1: 201 (MailHog có mail xác thực). Lần 2 và 3: 409 "Email đã được sử dụng" (vẫn bị đếm: bộ lọc không biết kết quả). Lần 4: **429** "Bạn gửi quá nhiều yêu cầu, vui lòng thử lại sau", có header `X-RateLimit-Remaining: 0`, `X-RateLimit-Reset` và `Retry-After` khoảng `1200`; MailHog không có thêm mail. Redis có một khoá dạng `rate:register:0:0:0:0:0:0:0:1` (hoặc `...:127.0.0.1`), TTL dương, không quá 3610 giây. Log backend có dòng `Rate limit register reached from ...` | Chưa chạy |
| MT-RATE-03 | Chạy backend profile `local` và `npm run dev`. Mở trang đăng nhập, nhập một email có tài khoản, gõ **sai** mật khẩu và bấm "Đăng nhập" 6 lần liền. Rồi đăng nhập **đúng** bằng một email khác (cùng máy) | Lần 1–5: form báo sai email hoặc mật khẩu. Lần 6: form hiện "Bạn gửi quá nhiều yêu cầu, vui lòng thử lại sau". Email khác đăng nhập được bình thường. Muốn thử lại ngay: `docker exec tripplanner-redis redis-cli --scan --pattern "rate:login:*"` rồi `redis-cli del` khoá đó. Trong Redis không có khoá nào chứa email thô | Chưa chạy |
| MT-RATE-04 | Chạy backend profile `local`. Ghi body `'{"email":"<email đã xác thực>"}' \| Set-Content -Encoding ascii forgot.json`, rồi gọi 6 lần: `curl.exe -s -i -X POST http://localhost:8080/api/v1/auth/forgot-password -H "Content-Type: application/json" -d "@forgot.json"`. Mở MailHog (`http://localhost:8025`) | Lần 1–5: 200, MailHog có 5 mail "đặt lại mật khẩu". Lần 6: **429** "Bạn gửi quá nhiều yêu cầu, vui lòng thử lại sau", `Retry-After` khoảng `720`; MailHog vẫn chỉ 5 mail. Muốn thử lại ngay: `docker exec tripplanner-redis redis-cli --scan --pattern "rate:forgot-password:*"` rồi `redis-cli del` khoá đó | Chưa chạy |
| MT-RATE-05 | Chạy backend profile `local` và `npm run dev`. Đăng nhập, mở một chuyến đi, mở ô thêm hoạt động và gõ tìm địa điểm như dùng thật: khoảng 10 lần tìm, mỗi lần gõ một tên địa điểm. Sau đó xem số lượt còn lại: `docker exec tripplanner-redis redis-cli --scan --pattern "rate:place-search:*"` (khoá dạng `rate:place-search:u<mã người dùng>`) | Không lần nào bị báo "Bạn gửi quá nhiều yêu cầu". Ghi lại cảm nhận: 10 lần tìm có làm gần hết 60 lượt không (mỗi lần tìm dự kiến tốn 2–4 lượt). Nếu thấy chật thì nâng con số `app.rate-limit.place-search.capacity`, không đổi cách đếm (design.md 8.2) | Chưa chạy |

## Lỗi đã phát hiện

| Mã | Phát hiện ở | Hiện tượng | Nguyên nhân | Cách sửa | Trạng thái |
|---|---|---|---|---|---|
| _(chưa có)_ | | | | | |

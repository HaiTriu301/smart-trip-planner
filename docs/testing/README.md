# Tài liệu kiểm thử — Smart Trip Planner

> Cập nhật: 2026-10-18 · **Task 4.5 code xong** (19 commit `6760a26` đến `3ff64fd` trên nhánh `feat/T4.5-sharing-ui`, gộp vào `main` qua pull request; lint, build và 176 test giao diện xanh; chủ dự án đã chạy 18 bài thủ công `MT-SHAREUI-01` đến `18`, đều đạt). Diễn biến: Mốc 17 (bản đồ trên trang công khai, mốc cuối) tại `3ff64fd`, Mốc 16 tại `d3374d0`, Mốc 15 tại `154ba33`, Mốc 14 tại `154cd99`, Mốc 13 tại `f2ced15`, sau Mốc 12 (tiêu đề hộp thoại đứng yên) tại `da20805`, Mốc 12 tại `9271a54`; sửa `BUG-UI-011` (Esc trên hộp hỏi lại đóng luôn hộp bên ngoài; ở [06-itinerary-ui.md](06-itinerary-ui.md)) tại `540c624`, chủ dự án đã kiểm lại ngày 2026-10-18; Mốc 11 tại `80cd003`, Mốc 10 tại `0f9775b`, Mốc 9 tại `47e4b19`, Mốc 8 tại `4d805f9`, Mốc 7 tại `0ef4c10`, Mốc 6 tại `1f537f5`, Mốc 5 tại `7575498`, Mốc 4 tại `b7aef61`, Mốc 3 tại `401e1c8`, Mốc 2 tại `91053c4`, Mốc 1 tại `6760a26`; kịch bản ở [11-sharing-ui.md](11-sharing-ui.md) · **Task 4.4 xong** (9 commit `4297fde` đến `2b35eb5` trên nhánh `feat/T4.4-comments`, gộp vào `main` qua pull request; backend 1485 lượt test xanh; không có lỗi mới; 7 bài thủ công `MT-COMMENT-01` đến `MT-COMMENT-07` chưa chạy). Diễn biến: Mốc 8 (test toàn luồng của bình luận) tại `2b35eb5`, kịch bản ở [10-comment.md](10-comment.md) phần H; Mốc 7 (xoá bình luận) tại `a52585e`, kịch bản ở phần G; Mốc 6 (lọc bình luận theo hoạt động) tại `a333a36`, kịch bản ở phần F; Mốc 5 (xem danh sách bình luận) tại `e9b1f1b`, kịch bản ở phần E; Mốc 4 (trả lời một bình luận) tại `c1f5e3a`, kịch bản ở phần D; Mốc 3 (bình luận về một hoạt động) tại `f76e6bd`, kịch bản ở phần C; commit 3a (gom chỗ dựng yêu cầu bình luận trong test, không đổi hành vi) tại `c553200`; Mốc 2 (đăng bình luận vào chuyến đi) tại `c961d35`, kịch bản ở [10-comment.md](10-comment.md) phần B; Mốc 1 (bảng lưu bình luận) tại `4297fde`, kịch bản ở phần A · 2026-10-17 · **Task 4.3 xong** (7 commit `9330e88` đến `744da8d` trên nhánh `feat/T4.3-permission-cache`, gộp vào `main` qua pull request; backend 1376 lượt test xanh; `BUG-PLAT-005` và `BUG-SHARE-002` đã sửa; bài thủ công `MT-SHARE-17` chưa chạy). Diễn biến: Mốc 1 (ma trận quyền đầy đủ) tại `9330e88`, kịch bản ở [09-sharing.md](09-sharing.md) phần U; Mốc 2 (bộ kiểm quyền đọc qua một bean riêng, không đổi hành vi) tại `8737124`; commit sửa `BUG-PLAT-005` (lệnh Redis không còn chờ khi mất kết nối) tại `1687773`, kịch bản ở [01-platform.md](01-platform.md) phần G; Mốc 3 (bộ nhớ tạm của quyền, `BUG-SHARE-002` đã sửa) tại `8e5cdb0`, kịch bản ở [09-sharing.md](09-sharing.md) phần V; Mốc 4 (xoá bản nhớ của quyền sau khi lưu xong) tại `e86aa4f`; Mốc 5 (người cùng sửa giữ được địa điểm tự thêm của chủ đang gắn trên hoạt động) tại `58c4160`, kịch bản ở [05-activity.md](05-activity.md) phần R, S, T; Mốc 6 (nhận lời mời của chuyến đã xoá trả 404) tại `744da8d`, kịch bản ở [09-sharing.md](09-sharing.md) phần E · 2026-10-15 · **Task 4.2 xong** (9 commit `7068e23` đến `1fdf57d` trên nhánh `feat/T4.2-share-links`, gộp vào `main` qua pull request; backend 1155 lượt test xanh; 7 bài thủ công `MT-SHARE-10` đến `MT-SHARE-16` chưa chạy). Diễn biến: Mốc 1 (bảng lưu liên kết chia sẻ) tại `7068e23`, kịch bản ở [09-sharing.md](09-sharing.md) phần M; Mốc 2 (tạo / sửa chuyến đi không còn nhận chế độ hiển thị từ người gọi) tại `2a1faec`, kịch bản ở [03-trip.md](03-trip.md) phần L; Mốc 3 (tạo liên kết chia sẻ) tại `055cc33`, kịch bản ở [09-sharing.md](09-sharing.md) phần N; Mốc 4 (liên kết có hạn dùng) tại `2c9397d`, kịch bản ở phần O; Mốc 5 (danh sách liên kết chia sẻ) tại `bac6e6a`, kịch bản ở phần P; Mốc 6 (thu hồi liên kết chia sẻ) tại `937349b`, kịch bản ở phần Q; Mốc 7 (trang công khai của chuyến đi) tại `226742a`, kịch bản ở phần R; Mốc 8 (đếm lượt xem của liên kết) tại `e811cc2`, kịch bản ở phần S; Mốc 9 (test toàn luồng của liên kết chia sẻ) tại `1fdf57d`, kịch bản ở phần T · 2026-10-09 · **Task 4.1 xong** (PR #26, merge commit `5004457`; 12 commit `3c66957` đến `f7d317f`; backend 1059 lượt test xanh; `BUG-SHARE-001` đã sửa; 9 bài thủ công `MT-SHARE-01` đến `MT-SHARE-09` chưa chạy; kịch bản ở [09-sharing.md](09-sharing.md) phần A đến L) · 2026-10-05 · **Task 3.8 xong** (11 commit, `90f0eb5` đến `da6a6a0`; backend 955 lượt test xanh, giao diện 115 test xanh; 8 bài thủ công của task chưa chạy: `MT-PLACE-06`, `MT-PLACE-07`, `MT-PLAT-07`, `MT-WEATHER-02`, `MT-WEATHER-03`, `MT-UI-90`, `MT-UI-91`, `MT-UI-92`). Diễn biến: Commit 1 (nguồn dự báo thật Open-Meteo) tại `90f0eb5`, Commit 2 (nguồn dự báo lỗi thì vẫn trả lời) tại `8b7a4c3`, build backend xanh, 821 lượt test, Commit 3 (thẻ thời tiết báo "Tạm thời không có dự báo") lint, build và 109 test giao diện xanh trên nhánh `feat/T3.8-real-providers`; Commit 4 (bản giữ tạm kết quả tìm kiếm phân biệt từ khoá có dấu) tại `6602aee`, Commit 5 (tìm và chọn địa điểm từ OpenStreetMap) tại `3f78612`, Commit 6 (quãng đường theo đường thật từ OSRM) tại `a3eeea7`, Commit 7 (giữ tạm quãng đường trong Redis) tại `9e20586`, commit sửa `BUG-PLACE-004` (bản giữ tạm của hai nguồn khác nhau không còn dùng chung ô) tại `08bd0ad`, Commit 8 (thử lại, ngắt mạch, giới hạn tần suất quanh các dịch vụ thật; `BUG-PLAT-004` đã sửa) build backend xanh, 955 lượt test; `MT-PLAT-07` chưa chạy; Commit 8 tại `3f49f60`; Commit 9 (giao diện không tự gọi lại request bị trả `PROVIDER_UNAVAILABLE`) tại `8a2ae0d`, Commit 10 (chân trang ghi nguồn dữ liệu) tại `da6a6a0`, lint, build và 115 test giao diện xanh, `MT-UI-91`, `MT-UI-92` chưa chạy; `MT-WEATHER-02`, `MT-WEATHER-03`, `MT-UI-90`, `MT-PLACE-06`, `MT-PLACE-07` chưa chạy · Task 3.9 xong, 12 commit tới `26dc267` (bảng màu mới, thanh trên cùng nền trắng, trang chuyến đi kiểu mới trên màn rộng và màn hẹp, đã chỉnh kích thước sau lần xem đầu của chủ dự án; font Inter; `MT-UI-78` đến `MT-UI-89` chưa chạy đủ) · build backend xanh tại commit `e75152c` (Task 3.5, 769 lượt test) · frontend lint, build và test tự động (`npm run test`) xanh: 108 test tại `6955238` (Task 3.7), 109 test tại `26dc267` (Task 3.9); 33 bài thủ công của Task 3.6 và 3.7 (`MT-UI-45` đến `MT-UI-77`) chưa chạy đủ; `BUG-UI-010` đã sửa, chờ chủ dự án kiểm lại · không còn lỗi đang mở, 6 lỗi đã sửa chờ chủ dự án kiểm lại sau rà soát Phase 1–2, sửa ở Task 2.7

Thư mục này ghi lại **hệ thống phải làm gì, đã kiểm tra thế nào, kết quả ra sao**.
Mỗi tính năng là một file. Người đọc không cần biết code.

## Danh sách file

| File | Tính năng | Tình trạng |
|---|---|---|
| [00-failed-test-guide.md](00-failed-test-guide.md) | Hướng dẫn: làm gì khi một test lỗi, kèm ví dụ | Đã ghi |
| [01-platform.md](01-platform.md) | Nền tảng: phản hồi, báo lỗi, khoá truy cập, cấu hình, kết nối Redis (Task 3.4) | Đã ghi |
| [02-auth.md](02-auth.md) | Đăng ký, đăng nhập, xác thực email, đặt lại mật khẩu | Đã ghi |
| [03-trip.md](03-trip.md) | Chuyến đi: tạo, xem, sửa, xoá | Đã ghi |
| [04-trip-day.md](04-trip-day.md) | Các ngày của chuyến đi | Đã ghi |
| [05-activity.md](05-activity.md) | Hoạt động trong ngày, kể cả sắp xếp lại; gắn địa điểm (Task 3.2) | Đã ghi, chờ chạy thủ công `MT-ACT-13`, `MT-ACT-14` |
| [06-itinerary-ui.md](06-itinerary-ui.md) | Giao diện lịch trình: danh sách, tạo, sửa chuyến đi và hoạt động; địa điểm và bản đồ (Task 3.6); thời tiết, quãng đường, ngày đã qua, hỏi hoàn thành chuyến đi, và test tự động của các hàm tính toán (Task 3.7); bảng màu, font và trang chuyến đi dạng thẻ (Task 3.9) | Đã ghi, chờ chạy thủ công |
| [07-place.md](07-place.md) | Địa điểm: tìm theo tên (Task 3.1); chọn kết quả, tự thêm địa điểm (Task 3.2); giữ tạm kết quả tìm trong Redis (Task 3.4); quãng đường trong ngày (Task 3.5); nguồn bản đồ thật OpenStreetMap (Task 3.8) | Đã ghi, chờ chạy thủ công |
| [08-weather.md](08-weather.md) | Thời tiết của chuyến đi: dự báo từng ngày, giới hạn 16 ngày (Task 3.3); giữ tạm trong Redis (Task 3.4); nguồn dự báo thật Open-Meteo, nguồn lỗi thì vẫn trả lời (Task 3.8) | Đã ghi, chờ chạy thủ công `MT-WEATHER-01` đến `MT-WEATHER-03` |
| [09-sharing.md](09-sharing.md) | Chia sẻ chuyến đi: thành viên và lời mời (Task 4.1); liên kết chia sẻ và trang công khai (Task 4.2); ma trận quyền đầy đủ và bộ nhớ tạm của quyền (Task 4.3) | Đã ghi, chờ chạy thủ công `MT-SHARE-01` đến `MT-SHARE-17` |
| [10-comment.md](10-comment.md) | Bình luận trên chuyến đi và hoạt động, trả lời một cấp, xoá (Task 4.4) | Đã ghi, chờ chạy thủ công `MT-COMMENT-01` đến `MT-COMMENT-07` |
| [11-sharing-ui.md](11-sharing-ui.md) | Giao diện chia sẻ: chuyến được chia sẻ trong danh sách, chế độ chỉ xem, hộp chia sẻ, nhận lời mời, trang công khai (Task 4.5); bình luận (Task 4.6) | Đang ghi theo mốc, chờ chạy thủ công `MT-SHAREUI-01` đến `18` |

## Cách đọc một test case

| Cột | Ý nghĩa |
|---|---|
| **Mã** | Mã cố định của test case, ví dụ `TC-PLAT-008` |
| **Kịch bản** | Ai làm gì, trong hoàn cảnh nào |
| **Kết quả mong đợi** | Hệ thống phải phản hồi thế nào |
| **Loại** | `Đúng`: dữ liệu hợp lệ, phải chấp nhận · `Sai`: dữ liệu không hợp lệ, phải từ chối · `Biên`: dữ liệu nằm đúng ngưỡng giới hạn · `Bảo mật`: kiểm quyền và chống lộ thông tin |
| **Trạng thái** | `Đạt` · `Lỗi` · `Chưa chạy` |

Đầu mỗi nhóm test case có một dòng ghi **yêu cầu** nằm ở đâu trong `design.md` và **class test** nào kiểm nó.

Mã `TC-` là test tự động, máy chạy mỗi lần build. Mã `MT-` là test thủ công, người chạy theo từng bước.

## Quy tắc ghi trạng thái

1. Test tự động ghi `Đạt` khi `./gradlew build` xanh tại commit ghi ở đầu file.
2. Test thủ công chỉ ghi `Đạt` sau khi có người thật sự chạy, kèm ngày chạy.
3. Test thủ công chưa ai chạy lại thì ghi `Chưa chạy`.
4. Mỗi `Lỗi` phải có một dòng trong mục "Lỗi đã phát hiện" của file đó.
5. Test lỗi ngoài dự kiến thì **ghi ngay trước khi sửa**, với trạng thái `Đang mở`. Xem [hướng dẫn](00-failed-test-guide.md).
6. Lỗi đã sửa thì dòng lỗi **vẫn giữ lại**. Test case ghi `Đạt · từng lỗi BUG-...` để người đọc tra được lịch sử.

## Tổng hợp

| Tính năng | Tự động | Thủ công | Đạt | Lỗi | Chưa chạy | Lỗi đã sửa |
|---|:--:|:--:|:--:|:--:|:--:|:--:|
| Nền tảng | 54 | 7 | 59 | 0 | 2 | 3 |
| Xác thực người dùng | 60 | 8 | 66 | 0 | 2 | 7 |
| Chuyến đi | 68 | 5 | 72 | 0 | 1 | 3 |
| Các ngày của chuyến đi | 35 | 3 | 38 | 0 | 0 | 3 |
| Hoạt động trong ngày | 205 | 14 | 217 | 0 | 2 | 4 |
| Giao diện lịch trình | 76 | 92 | 76 | 0 | 92 | 5 |
| Địa điểm | 171 | 7 | 171 | 0 | 7 | 4 |
| Thời tiết | 56 | 3 | 56 | 0 | 3 | 0 |
| Chia sẻ chuyến đi | 233 | 17 | 233 | 0 | 17 | 2 |
| Bình luận | 62 | 7 | 62 | 0 | 7 | 0 |
| Giao diện chia sẻ | 29 | 18 | 47 | 0 | 0 | 0 |
| **Tổng** | **1049** | **181** | **1097** | **0** | **133** | **31** |

**Lỗi đang mở: 0.** Task 4.5 bắt được và sửa `BUG-UI-011` (nhấn Esc trên một hộp hỏi lại nằm trong hộp thoại khác thì đóng luôn hộp bên ngoài; có từ Task 2.5), chủ dự án đã kiểm lại ngày 2026-10-18, ghi ở [06-itinerary-ui.md](06-itinerary-ui.md). 18 bài thủ công của Task 4.5 đều đạt ở lần chạy đầu. Task 4.4 không có test đỏ ngoài dự kiến, không có lỗi mới. Task 4.3 bắt được và sửa hai lỗi: `BUG-PLAT-005` (Redis mất kết nối làm mỗi lệnh chờ 1 giây, [01-platform.md](01-platform.md)) và `BUG-SHARE-002` (bản nhớ quyền cũ làm chuyến đã xoá trả 403 thay vì 404, [09-sharing.md](09-sharing.md)). Rà soát code Phase 1–2 ngày 2026-10-01 tìm ra 9 lỗi mà test không bắt được; cả 9 đã sửa ở Task 2.7 (một lỗi một commit). Ba lỗi đã được xác nhận: `BUG-PLAT-003` và `BUG-AUTH-006` bằng test tự động và chạy thử, `BUG-UI-003` do chủ dự án thử lại. Sáu lỗi giao diện còn lại **đã sửa trong code nhưng chưa ai chạy lại trên trình duyệt**: `BUG-UI-004` (`MT-UI-43`), `BUG-UI-005` (`MT-UI-44`), `BUG-UI-006` (`MT-UI-12`), `BUG-UI-007` (`MT-UI-02`), `BUG-UI-008` (`MT-UI-26`), `BUG-AUTH-007` (`MT-AUTH-08`). Trong lúc sửa phát sinh thêm `BUG-AUTH-008` (test sai), đã sửa. Task 3.6: chủ dự án thử trên trình duyệt và tìm ra `BUG-UI-009` (bản đồ phóng to không hiện), đã sửa và được chủ dự án xác nhận ngày 2026-10-04. Task 3.7: chủ dự án xem trên trình duyệt và tìm ra `BUG-UI-010` (tên ngày ở cột giữa bị xuống dòng), đã sửa ngày 2026-10-05, **chưa được kiểm lại** (`MT-UI-77`). Task 3.8: đọc lại code lúc làm Commit 7 tìm ra `BUG-PLACE-004` (bản giữ tạm kết quả tìm địa điểm và dự báo của hai nguồn dùng chung ô), đã sửa ngày 2026-10-05 kèm test tự động; kiểm lại bằng tay ở bước đầu của `MT-PLACE-06`. Commit 8 của Task 3.8: test mới bắt được `BUG-PLAT-004` (nhiều lần gọi cùng lúc khi mạch mở nhận lỗi 500 thay vì 503), đã sửa cùng ngày kèm test ép lỗi lộ ra.

Dự án có 1072 method test trong 88 file test, chạy thành 1485 lượt vì một số test lặp lại với nhiều bộ dữ liệu. Số kịch bản ít hơn vì một kịch bản thường được nhiều method ở các tầng khác nhau cùng kiểm. Một số method thuần kỹ thuật không được ghi thành kịch bản riêng. Giao diện có thêm 176 test trong 16 file (Vitest, từ Task 3.7), không tính vào các con số trên.

## Chưa kiểm thử

| Phần | Lý do |
|---|---|
| Test tự động cho phần nhìn thấy của giao diện (nút, hộp thoại, bố cục) | Từ Task 3.7 chỉ có test cho các hàm tính toán; test từng thành phần giao diện để Task 8.3. Hiện kiểm bằng bài thủ công `MT-UI` |
| Hạn mức gói miễn phí | Tính năng thuộc Phase 6 |
| Giới hạn số lần gọi | Tính năng thuộc Phase 8 |

## Chạy test tự động

```bash
cd backend
./gradlew build        # cần bật Docker Desktop
```

Báo cáo chi tiết từng test: `backend/build/reports/tests/test/index.html`

Giao diện (từ Task 3.7), không cần Docker:

```bash
cd frontend
npm run test
```

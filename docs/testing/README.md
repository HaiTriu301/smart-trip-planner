# Tài liệu kiểm thử — Smart Trip Planner

> Cập nhật: 2026-10-09 · **Task 4.1 xong** (PR #26, merge commit `5004457`; 12 commit `3c66957` đến `f7d317f`; backend 1059 lượt test xanh; `BUG-SHARE-001` đã sửa; 9 bài thủ công `MT-SHARE-01` đến `MT-SHARE-09` chưa chạy; kịch bản ở [09-sharing.md](09-sharing.md) phần A đến L) · 2026-10-05 · **Task 3.8 xong** (11 commit, `90f0eb5` đến `da6a6a0`; backend 955 lượt test xanh, giao diện 115 test xanh; 8 bài thủ công của task chưa chạy: `MT-PLACE-06`, `MT-PLACE-07`, `MT-PLAT-07`, `MT-WEATHER-02`, `MT-WEATHER-03`, `MT-UI-90`, `MT-UI-91`, `MT-UI-92`). Diễn biến: Commit 1 (nguồn dự báo thật Open-Meteo) tại `90f0eb5`, Commit 2 (nguồn dự báo lỗi thì vẫn trả lời) tại `8b7a4c3`, build backend xanh, 821 lượt test, Commit 3 (thẻ thời tiết báo "Tạm thời không có dự báo") lint, build và 109 test giao diện xanh trên nhánh `feat/T3.8-real-providers`; Commit 4 (bản giữ tạm kết quả tìm kiếm phân biệt từ khoá có dấu) tại `6602aee`, Commit 5 (tìm và chọn địa điểm từ OpenStreetMap) tại `3f78612`, Commit 6 (quãng đường theo đường thật từ OSRM) tại `a3eeea7`, Commit 7 (giữ tạm quãng đường trong Redis) tại `9e20586`, commit sửa `BUG-PLACE-004` (bản giữ tạm của hai nguồn khác nhau không còn dùng chung ô) tại `08bd0ad`, Commit 8 (thử lại, ngắt mạch, giới hạn tần suất quanh các dịch vụ thật; `BUG-PLAT-004` đã sửa) build backend xanh, 955 lượt test; `MT-PLAT-07` chưa chạy; Commit 8 tại `3f49f60`; Commit 9 (giao diện không tự gọi lại request bị trả `PROVIDER_UNAVAILABLE`) tại `8a2ae0d`, Commit 10 (chân trang ghi nguồn dữ liệu) tại `da6a6a0`, lint, build và 115 test giao diện xanh, `MT-UI-91`, `MT-UI-92` chưa chạy; `MT-WEATHER-02`, `MT-WEATHER-03`, `MT-UI-90`, `MT-PLACE-06`, `MT-PLACE-07` chưa chạy · Task 3.9 xong, 12 commit tới `26dc267` (bảng màu mới, thanh trên cùng nền trắng, trang chuyến đi kiểu mới trên màn rộng và màn hẹp, đã chỉnh kích thước sau lần xem đầu của chủ dự án; font Inter; `MT-UI-78` đến `MT-UI-89` chưa chạy đủ) · build backend xanh tại commit `e75152c` (Task 3.5, 769 lượt test) · frontend lint, build và test tự động (`npm run test`) xanh: 108 test tại `6955238` (Task 3.7), 109 test tại `26dc267` (Task 3.9); 33 bài thủ công của Task 3.6 và 3.7 (`MT-UI-45` đến `MT-UI-77`) chưa chạy đủ; `BUG-UI-010` đã sửa, chờ chủ dự án kiểm lại · không còn lỗi đang mở, 6 lỗi đã sửa chờ chủ dự án kiểm lại sau rà soát Phase 1–2, sửa ở Task 2.7

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
| [09-sharing.md](09-sharing.md) | Chia sẻ chuyến đi: thành viên và lời mời (Task 4.1) | Đã ghi, chờ chạy thủ công `MT-SHARE-01` đến `MT-SHARE-09` |

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
| Nền tảng | 52 | 7 | 57 | 0 | 2 | 2 |
| Xác thực người dùng | 60 | 8 | 66 | 0 | 2 | 7 |
| Chuyến đi | 65 | 5 | 69 | 0 | 1 | 3 |
| Các ngày của chuyến đi | 35 | 3 | 38 | 0 | 0 | 3 |
| Hoạt động trong ngày | 202 | 14 | 214 | 0 | 2 | 4 |
| Giao diện lịch trình | 76 | 92 | 76 | 0 | 92 | 4 |
| Địa điểm | 171 | 7 | 171 | 0 | 7 | 4 |
| Thời tiết | 56 | 3 | 56 | 0 | 3 | 0 |
| Chia sẻ chuyến đi | 121 | 9 | 121 | 0 | 9 | 1 |
| **Tổng** | **838** | **148** | **868** | **0** | **118** | **28** |

**Lỗi đang mở: 0.** Rà soát code Phase 1–2 ngày 2026-10-01 tìm ra 9 lỗi mà test không bắt được; cả 9 đã sửa ở Task 2.7 (một lỗi một commit). Ba lỗi đã được xác nhận: `BUG-PLAT-003` và `BUG-AUTH-006` bằng test tự động và chạy thử, `BUG-UI-003` do chủ dự án thử lại. Sáu lỗi giao diện còn lại **đã sửa trong code nhưng chưa ai chạy lại trên trình duyệt**: `BUG-UI-004` (`MT-UI-43`), `BUG-UI-005` (`MT-UI-44`), `BUG-UI-006` (`MT-UI-12`), `BUG-UI-007` (`MT-UI-02`), `BUG-UI-008` (`MT-UI-26`), `BUG-AUTH-007` (`MT-AUTH-08`). Trong lúc sửa phát sinh thêm `BUG-AUTH-008` (test sai), đã sửa. Task 3.6: chủ dự án thử trên trình duyệt và tìm ra `BUG-UI-009` (bản đồ phóng to không hiện), đã sửa và được chủ dự án xác nhận ngày 2026-10-04. Task 3.7: chủ dự án xem trên trình duyệt và tìm ra `BUG-UI-010` (tên ngày ở cột giữa bị xuống dòng), đã sửa ngày 2026-10-05, **chưa được kiểm lại** (`MT-UI-77`). Task 3.8: đọc lại code lúc làm Commit 7 tìm ra `BUG-PLACE-004` (bản giữ tạm kết quả tìm địa điểm và dự báo của hai nguồn dùng chung ô), đã sửa ngày 2026-10-05 kèm test tự động; kiểm lại bằng tay ở bước đầu của `MT-PLACE-06`. Commit 8 của Task 3.8: test mới bắt được `BUG-PLAT-004` (nhiều lần gọi cùng lúc khi mạch mở nhận lỗi 500 thay vì 503), đã sửa cùng ngày kèm test ép lỗi lộ ra.

Dự án có 889 method test trong 75 file test, chạy thành 1059 lượt vì một số test lặp lại với nhiều bộ dữ liệu. Số kịch bản ít hơn vì một kịch bản thường được nhiều method ở các tầng khác nhau cùng kiểm. Một số method thuần kỹ thuật không được ghi thành kịch bản riêng. Giao diện có thêm 115 test trong 9 file (Vitest, từ Task 3.7), không tính vào các con số trên.

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

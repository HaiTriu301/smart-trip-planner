# Tài liệu kiểm thử — Smart Trip Planner

> Cập nhật: 2026-10-01 · build backend xanh tại commit `025799a` (Task 2.7, 532 lượt test) · không còn lỗi đang mở, 6 lỗi đã sửa chờ chủ dự án kiểm lại sau rà soát Phase 1–2, sửa ở Task 2.7

Thư mục này ghi lại **hệ thống phải làm gì, đã kiểm tra thế nào, kết quả ra sao**.
Mỗi tính năng là một file. Người đọc không cần biết code.

## Danh sách file

| File | Tính năng | Tình trạng |
|---|---|---|
| [00-failed-test-guide.md](00-failed-test-guide.md) | Hướng dẫn: làm gì khi một test lỗi, kèm ví dụ | Đã ghi |
| [01-platform.md](01-platform.md) | Nền tảng: phản hồi, báo lỗi, khoá truy cập, cấu hình | Đã ghi |
| [02-auth.md](02-auth.md) | Đăng ký, đăng nhập, xác thực email, đặt lại mật khẩu | Đã ghi |
| [03-trip.md](03-trip.md) | Chuyến đi: tạo, xem, sửa, xoá | Đã ghi |
| [04-trip-day.md](04-trip-day.md) | Các ngày của chuyến đi | Đã ghi |
| [05-activity.md](05-activity.md) | Hoạt động trong ngày, kể cả sắp xếp lại | Đã ghi |
| [06-itinerary-ui.md](06-itinerary-ui.md) | Giao diện lịch trình: danh sách, tạo, sửa chuyến đi và hoạt động | Đã ghi, chờ chạy thủ công |

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
| Nền tảng | 32 | 5 | 37 | 0 | 0 | 1 |
| Xác thực người dùng | 60 | 8 | 66 | 0 | 2 | 7 |
| Chuyến đi | 65 | 5 | 69 | 0 | 1 | 3 |
| Các ngày của chuyến đi | 35 | 3 | 38 | 0 | 0 | 3 |
| Hoạt động trong ngày | 174 | 12 | 186 | 0 | 0 | 4 |
| Giao diện lịch trình | 0 | 44 | 0 | 0 | 44 | 3 |
| **Tổng** | **366** | **77** | **396** | **0** | **47** | **21** |

**Lỗi đang mở: 0.** Rà soát code Phase 1–2 ngày 2026-10-01 tìm ra 9 lỗi mà test không bắt được; cả 9 đã sửa ở Task 2.7 (một lỗi một commit). Ba lỗi đã được xác nhận: `BUG-PLAT-003` và `BUG-AUTH-006` bằng test tự động và chạy thử, `BUG-UI-003` do chủ dự án thử lại. Sáu lỗi giao diện còn lại **đã sửa trong code nhưng chưa ai chạy lại trên trình duyệt**: `BUG-UI-004` (`MT-UI-43`), `BUG-UI-005` (`MT-UI-44`), `BUG-UI-006` (`MT-UI-12`), `BUG-UI-007` (`MT-UI-02`), `BUG-UI-008` (`MT-UI-26`), `BUG-AUTH-007` (`MT-AUTH-08`). Trong lúc sửa phát sinh thêm `BUG-AUTH-008` (test sai), đã sửa.

Dự án có 492 method test trong 40 file test, chạy thành 532 lượt vì một số test lặp lại với nhiều bộ dữ liệu. Số kịch bản ít hơn vì một kịch bản thường được nhiều method ở các tầng khác nhau cùng kiểm. Một số method thuần kỹ thuật không được ghi thành kịch bản riêng.

## Chưa kiểm thử

| Phần | Lý do |
|---|---|
| Test tự động cho giao diện | Chưa có công cụ, sẽ thêm ở task frontend sau |
| Hạn mức gói miễn phí | Tính năng thuộc Phase 6 |
| Giới hạn số lần gọi | Tính năng thuộc Phase 8 |

## Chạy test tự động

```bash
cd backend
./gradlew build        # cần bật Docker Desktop
```

Báo cáo chi tiết từng test: `backend/build/reports/tests/test/index.html`

# Tài liệu kiểm thử — Smart Trip Planner

> Cập nhật: 2026-09-29 · build xanh tại commit `50bb13c`

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
| [05-activity.md](05-activity.md) | Hoạt động trong ngày | Đã ghi |

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
| Nền tảng | 28 | 5 | 28 | 0 | 5 | 0 |
| Xác thực người dùng | 54 | 6 | 54 | 0 | 6 | 4 |
| Chuyến đi | 42 | 4 | 42 | 0 | 4 | 3 |
| Các ngày của chuyến đi | 33 | 3 | 33 | 0 | 3 | 3 |
| Hoạt động trong ngày | 111 | 9 | 111 | 0 | 9 | 3 |
| **Tổng** | **268** | **27** | **268** | **0** | **27** | **13** |

Dự án có 383 method test trong 39 class, chạy thành 411 lượt vì một số test lặp lại với nhiều bộ dữ liệu. Số kịch bản ít hơn vì một kịch bản thường được nhiều method ở các tầng khác nhau cùng kiểm. Một số method thuần kỹ thuật không được ghi thành kịch bản riêng.

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

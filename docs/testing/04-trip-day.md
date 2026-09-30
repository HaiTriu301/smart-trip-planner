# 04 · Các ngày của chuyến đi

> Cập nhật: 2026-09-30 · build xanh tại commit `1b30b1f` · [Về trang chính](README.md)

Mỗi chuyến đi có sẵn một "ngày" cho mỗi ngày lịch từ ngày bắt đầu tới ngày kết thúc. Người dùng không tự thêm hay xoá ngày. Hệ thống tự làm việc đó khi chuyến đi được tạo hoặc đổi ngày. Làm ở Task 2.2.

Vài từ dùng trong file:

| Từ | Nghĩa |
|---|---|
| Ngày lịch | Ngày thật trên lịch, ví dụ 01/10/2026 |
| Số thứ tự ngày | Vị trí của ngày trong chuyến đi: Ngày 1, Ngày 2, Ngày 3 |
| Dời nguyên khối | Cả chuyến đi chuyển sang ngày khác, số ngày không đổi |

---

## A. Tự sinh ngày khi tạo chuyến đi

> **Yêu cầu:** design.md rule 14.2, 5.2 bảng `trip_days` · **Kiểm bởi:** `TripDayServiceTest`, `TripDayFlowIntegrationTest`

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-DAY-001 | Tạo chuyến đi 3 ngày, từ 01/10 đến 03/10 | Có đúng 3 ngày, đánh số 1, 2, 3, khớp với 01/10, 02/10, 03/10 | Đúng | Đạt |
| TC-DAY-002 | Tạo chuyến đi chỉ 1 ngày | Có đúng 1 ngày | Biên | Đạt |
| TC-DAY-003 | Tạo chuyến đi vắt qua cuối tháng và cuối năm | Các ngày liên tiếp nhau, không sót và không trùng ngày nào | Biên | Đạt |
| TC-DAY-004 | Tạo chuyến đi dài nhất cho phép, 60 ngày | Có đúng 60 ngày | Biên | Đạt |
| TC-DAY-005 | Tạo chuyến đi bị từ chối vì quá dài | Không có ngày nào được tạo | Sai | Đạt |

## B. Xem danh sách ngày

> **Yêu cầu:** design.md 10.2 Itinerary, 6.2 · **Kiểm bởi:** `TripDayControllerTest`, `TripDayServiceTest`, `TripDayRepositoryTest`, `TripDayFlowIntegrationTest`

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-DAY-006 | Chủ sở hữu xem các ngày của chuyến đi | 200, các ngày xếp theo thứ tự thời gian. Không lẫn ngày của chuyến đi khác | Đúng | Đạt |
| TC-DAY-007 | Xem khi chưa đăng nhập | 401 | Bảo mật | Đạt |
| TC-DAY-008 | Người lạ xem các ngày của chuyến đi người khác | 403 | Bảo mật | Đạt |
| TC-DAY-009 | Xem các ngày của chuyến đi không tồn tại hoặc đã xoá | 404 | Sai | Đạt |
| TC-DAY-010 | Mã chuyến đi trên đường dẫn là chữ | 400 `VALIDATION_ERROR` | Sai | Đạt |

## C. Sửa tiêu đề và ghi chú của ngày

> **Yêu cầu:** design.md 10.2 "Quy ước sửa ngày" · **Kiểm bởi:** `TripDayControllerTest`, `TripDayServiceTest`, `TripDayFlowIntegrationTest`

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-DAY-011 | Gửi tiêu đề và ghi chú mới | 200, giá trị được lưu, khoảng trắng thừa ở hai đầu được cắt | Đúng | Đạt |
| TC-DAY-012 | Chỉ gửi tiêu đề, không gửi ghi chú | Tiêu đề đổi, ghi chú **giữ nguyên** | Đúng | Đạt |
| TC-DAY-013 | Gửi tiêu đề là chuỗi rỗng hoặc chỉ có khoảng trắng | Tiêu đề bị **xoá**. Trang web sẽ hiện lại "Ngày 1" | Biên | Đạt |
| TC-DAY-014 | Gửi tiêu đề dài hơn 160 ký tự | 400, lỗi ở ô tiêu đề | Biên | Đạt |
| TC-DAY-015 | Sửa ngày của chuyến B qua đường dẫn của chuyến A | 404. Người dùng có quyền trên chuyến A cũng không sửa được ngày của chuyến B bằng cách này | Bảo mật | Đạt |
| TC-DAY-016 | Người lạ sửa ngày của chuyến đi người khác | 403 | Bảo mật | Đạt |
| TC-DAY-017 | Sửa ngày của chuyến đi đã xoá | 404 | Sai | Đạt |

## D. Đổi ngày của chuyến đi

> **Yêu cầu:** design.md rule 14.3 · **Kiểm bởi:** `TripDayReconcileTest`, `TripServiceTest`, `TripDayFlowIntegrationTest`

Đây là quy tắc phức tạp nhất của tính năng. Hệ thống chọn cách xử lý theo bảng quyết định sau:

| Số ngày sau khi đổi | Ngày bắt đầu | Cách xử lý | Tiêu đề và ghi chú |
|---|---|---|---|
| Không đổi | Đổi | Dời nguyên khối | Giữ nguyên toàn bộ |
| Tăng hoặc giảm | Bất kỳ | Giữ theo ngày lịch | Ngày lịch nào còn nằm trong khoảng mới thì giữ, ngày bị cắt thì mất |
| Không đổi | Không đổi | Không làm gì | Giữ nguyên toàn bộ |

Mọi kịch bản bên dưới bắt đầu từ một chuyến đi 01/10 đến 03/10, ba ngày có tiêu đề A, B, C.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-DAY-018 | Dời muộn 1 ngày: 02/10 đến 04/10 | A, B, C chuyển sang 02, 03, 04. Tiêu đề và mã của từng ngày giữ nguyên | Biên | Đạt · từng lỗi BUG-DAY-001 |
| TC-DAY-019 | Dời sớm 1 ngày: 30/09 đến 02/10 | A, B, C chuyển sang 30/09, 01/10, 02/10 | Biên | Đạt · từng lỗi BUG-DAY-001 |
| TC-DAY-020 | Dời muộn 1 tuần: 08/10 đến 10/10 | A, B, C chuyển sang 08, 09, 10 | Đúng | Đạt |
| TC-DAY-021 | Kéo dài phía cuối: 01/10 đến 05/10 | A, B, C giữ nguyên. Thêm hai ngày trống là Ngày 4 và Ngày 5 | Đúng | Đạt |
| TC-DAY-022 | Kéo dài phía đầu: 29/09 đến 03/10 | Thêm hai ngày trống ở đầu. A, B, C trở thành Ngày 3, 4, 5 | Đúng | Đạt |
| TC-DAY-023 | Cắt phía cuối: chỉ còn 01/10 | Ngày B và C bị xoá. A giữ nguyên là Ngày 1 | Đúng | Đạt |
| TC-DAY-024 | Cắt phía đầu: chỉ còn 03/10 | Ngày A và B bị xoá. C ở lại đúng 03/10 và trở thành Ngày 1 | Đúng | Đạt |
| TC-DAY-025 | Vừa dời vừa đổi số ngày: 08/10 đến 12/10 | Có 5 ngày trống mới. **A, B, C bị mất** vì không ngày lịch nào trùng với khoảng mới | Biên | Đạt |
| TC-DAY-026 | Gửi lại đúng ngày bắt đầu và kết thúc đang lưu | Không có gì thay đổi | Biên | Đạt |

`TC-DAY-025` là hành vi đúng theo thiết kế, nhưng dễ làm người dùng mất dữ liệu. Trang web sẽ hướng dẫn làm hai bước ở Task 2.5: dời chuyến đi trước, đổi số ngày sau.

Các kịch bản cắt ngày ở trên dùng ngày **không có hoạt động**. Từ Task 2.3, ngày bị cắt mà đang có hoạt động thì thay đổi bị chặn. Xem [05-activity.md](05-activity.md) phần G.

## E. Chi tiết chuyến đi kèm danh sách ngày

> **Yêu cầu:** design.md 10.2 "Quy ước Trip API", CLAUDE.md mục 8 · **Kiểm bởi:** `TripServiceTest`, `TripDayFlowIntegrationTest`

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-DAY-027 | Xem chi tiết một chuyến đi | Phản hồi có thông tin chuyến đi và các ngày theo thứ tự thời gian | Đúng | Đạt · từng lỗi BUG-DAY-003 |
| TC-DAY-028 | Xem chi tiết chuyến đi 2 ngày, rồi chuyến đi 60 ngày | Số câu truy vấn database bằng nhau. Chuyến đi dài hơn không làm hệ thống chậm đi. Con số là 3 ở Task 2.2, và là 4 từ Task 2.3 khi chi tiết kèm cả hoạt động, xem `TC-ACT-098` | Biên | Đạt |
| TC-DAY-029 | Xem chi tiết chuyến đi không tồn tại | 404, hệ thống không tải danh sách ngày | Sai | Đạt |

## F. Chốt chặn ở database

> **Yêu cầu:** design.md 5.2 bảng `trip_days` · **Kiểm bởi:** `TripDayMappingTest`

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-DAY-030 | Ghi thẳng vào database hai ngày trùng ngày lịch trong một chuyến đi | Database từ chối | Sai | Đạt |
| TC-DAY-031 | Ghi thẳng vào database một ngày có số thứ tự bằng 0 | Database từ chối | Biên | Đạt |
| TC-DAY-032 | Xoá hẳn một chuyến đi khỏi database | Các ngày của nó bị xoá theo | Đúng | Đạt |
| TC-DAY-033 | Xoá mềm một chuyến đi | Các ngày của nó **vẫn còn**, để khôi phục chuyến đi thì ngày còn nguyên | Đúng | Đạt · từng lỗi BUG-DAY-002 |

---

## Kiểm tra thủ công

Làm trên trang Swagger. Cần có: backend đang chạy, đã đăng nhập bằng một tài khoản đã xác thực email.

### MT-DAY-01 · Ngày được sinh cùng chuyến đi

- [x] Tạo chuyến đi từ `2026-10-01` đến `2026-10-03`.
- [x] Gọi danh sách ngày của chuyến đi đó. Trả 3 ngày, số thứ tự 1, 2, 3, ngày lịch 01, 02, 03 tháng 10.
- [x] Gọi chi tiết chuyến đi. Phần `days` có đúng 3 ngày đó.

**Kết quả:** Đạt · **Ngày:** 2026-09-30 · **Ghi chú:** chủ dự án tự chạy trên Swagger

### MT-DAY-02 · Đặt và xoá tiêu đề của ngày

Cần có: chuyến đi từ `MT-DAY-01`.

- [x] Sửa Ngày 1, gửi tiêu đề "Khám phá trung tâm" và ghi chú "Nhận phòng lúc 14 giờ". Trả 200, dấu tiếng Việt đúng.
- [x] Sửa Ngày 1, chỉ gửi ghi chú mới. Tiêu đề vẫn là "Khám phá trung tâm".
- [x] Sửa Ngày 1, gửi tiêu đề là chuỗi rỗng `""`. Tiêu đề trở về `null`, ghi chú giữ nguyên.

**Kết quả:** Đạt · **Ngày:** 2026-09-30 · **Ghi chú:** chủ dự án tự chạy trên Swagger

### MT-DAY-03 · Đổi ngày của chuyến đi

Cần có: một chuyến đi mới từ `2026-10-01` đến `2026-10-03`, ba ngày được đặt tiêu đề A, B, C.

- [x] Sửa chuyến đi thành `2026-10-08` đến `2026-10-10`. Danh sách ngày là 08, 09, 10 với tiêu đề A, B, C.
- [x] Sửa ngày kết thúc thành `2026-10-12`. Có 5 ngày, hai ngày cuối không có tiêu đề.
- [x] Sửa ngày bắt đầu thành `2026-10-10`. Còn 3 ngày. Ngày 1 là 10/10 với tiêu đề C.

**Kết quả:** Đạt · **Ngày:** 2026-09-30 · **Ghi chú:** chủ dự án tự chạy trên Swagger

---

## Lỗi đã phát hiện

Nguồn: mục "Bẫy đã gặp khi làm 2.2" trong `WORKFLOW.md`. Các lỗi này được sửa trước khi code được gộp vào nhánh chính.

| Mã lỗi | Test case | Ngày | Hiện tượng | Nguyên nhân | Cách sửa | Trạng thái |
|---|---|---|---|---|---|---|
| BUG-DAY-001 | TC-DAY-018, TC-DAY-019 | 2026-09-28 | Dời chuyến đi 1 ngày thì database báo trùng ngày | Database kiểm tra trùng sau **từng dòng**. Khi Ngày 1 chuyển sang 02/10 thì Ngày 2 vẫn đang giữ 02/10 | Dời muộn thì cập nhật ngày cuối trước. Dời sớm thì cập nhật ngày đầu trước | Đã sửa, commit `a571f03` |
| BUG-DAY-002 | TC-DAY-033 | 2026-09-27 | Test xoá mềm chuyến đi báo lỗi | **Test sai.** Test giữ các ngày trong bộ nhớ khi xoá chuyến đi, điều mà ứng dụng thật không làm | Test xoá bộ nhớ đệm rồi tải lại chuyến đi trước khi xoá | Đã sửa, commit `6cdb7f6` |
| BUG-DAY-003 | TC-DAY-027 | 2026-09-28 | Test đơn vị của phần chuyển đổi dữ liệu dừng đột ngột | Bộ chuyển đổi của chuyến đi cần bộ chuyển đổi của ngày, nhưng không nhận được | Đổi cách hai bộ chuyển đổi được nối với nhau | Đã sửa, commit `ad51a9c` |

BUG-DAY-001 đã được kiểm chứng ngược: khi đảo thứ tự cập nhật, test đỏ trở lại. Điều đó chứng minh test thật sự bảo vệ được quy tắc này.

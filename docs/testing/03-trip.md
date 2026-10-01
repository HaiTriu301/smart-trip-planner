# 03 · Chuyến đi

> Cập nhật: 2026-10-01 · build xanh tại commit `8f06d72` (merge Task 2.6) · [Về trang chính](README.md)

Tính năng này cho người dùng tạo, xem danh sách, xem chi tiết, sửa và xoá chuyến đi. Làm ở Task 2.1.
Đổi trạng thái chuyến đi (phần H) làm ở Task 2.5 Mốc 5. Giới hạn mô tả 1000 ký tự (phần I) chốt ở Task 2.5, trước đó là 5000. Số hoạt động trên thẻ chuyến đi (phần J) và số chuyến đi theo trạng thái (phần K) làm trên Task 2.6 (`feat/T2.6-ui-guide`).
Ở giai đoạn này chỉ **chủ sở hữu** mới có quyền trên chuyến đi. Chia sẻ cho người khác thuộc Phase 4.

Vài từ dùng trong file:

| Từ | Nghĩa |
|---|---|
| Chủ sở hữu | Người tạo ra chuyến đi |
| Người lạ | Người đã đăng nhập nhưng không phải chủ sở hữu |
| Slug | Tên rút gọn không dấu dùng trong đường dẫn, ví dụ `da-lat-3-ngay-x7k2qp` |
| Xoá mềm | Đánh dấu là đã xoá, dữ liệu vẫn còn trong database |

---

## A. Tạo chuyến đi

> **Yêu cầu:** design.md 10.2 "Quy ước Trip API", rule 14.1 · **Kiểm bởi:** `TripControllerTest`, `TripServiceTest`, `TripFlowIntegrationTest`

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-TRIP-001 | Tạo chuyến đi với tên và ngày hợp lệ | 201. Trạng thái là bản nháp, chế độ riêng tư, tiền tệ VND. Chủ sở hữu là **người đang đăng nhập** | Đúng | Đạt |
| TC-TRIP-002 | Tạo chuyến đi và tự chọn tiền tệ, chế độ hiển thị | Giữ đúng giá trị người dùng chọn | Đúng | Đạt |
| TC-TRIP-003 | Tạo chuyến đi dài **đúng 60 ngày** | Tạo thành công | Biên | Đạt |
| TC-TRIP-004 | Tạo chuyến đi dài **61 ngày** | 400, lỗi ở ô ngày kết thúc: "Chuyến đi dài tối đa 60 ngày" | Biên | Đạt |
| TC-TRIP-005 | Ngày kết thúc trước ngày bắt đầu | 400, lỗi ở ô ngày kết thúc | Sai | Đạt |
| TC-TRIP-006 | Nhập vĩ độ mà không nhập kinh độ | 400, lỗi ở ô còn thiếu | Sai | Đạt |
| TC-TRIP-007 | Gửi form sai nhiều ô: tên trống, thiếu ngày, tiền tệ viết thường, vĩ độ 91, ngân sách âm | 400, liệt kê từng ô sai. Không tạo chuyến đi | Sai | Đạt |
| TC-TRIP-008 | Tạo chuyến đi khi chưa đăng nhập | 401 | Bảo mật | Đạt |

## B. Tên rút gọn trong đường dẫn

> **Yêu cầu:** design.md 5.2 bảng `trips` · **Kiểm bởi:** `SlugGeneratorTest`, `TripServiceTest`, `TripRepositoryTest`

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-TRIP-009 | Tên tiếng Việt có dấu, kể cả chữ "đ" | Bỏ dấu, viết thường, nối bằng gạch ngang, thêm 6 ký tự ngẫu nhiên ở cuối | Đúng | Đạt |
| TC-TRIP-010 | Tên có nhiều ký hiệu và khoảng trắng liền nhau | Gộp thành một gạch ngang, không có gạch ở đầu và cuối | Đúng | Đạt |
| TC-TRIP-011 | Tên chỉ toàn ký hiệu, không còn chữ nào dùng được | Dùng một tên dự phòng | Biên | Đạt |
| TC-TRIP-012 | Tên rất dài | Được cắt ngắn, không kết thúc bằng gạch ngang | Biên | Đạt |
| TC-TRIP-013 | Tên rút gọn vừa sinh ra đã có chuyến đi khác dùng | Sinh lại với 6 ký tự khác. Sau 5 lần vẫn trùng thì báo lỗi | Biên | Đạt |
| TC-TRIP-014 | Tên rút gọn trùng với một chuyến đi **đã xoá** | Vẫn tính là trùng và sinh lại | Biên | Đạt · từng lỗi BUG-TRIP-002 |

## C. Danh sách chuyến đi

> **Yêu cầu:** design.md 10.2 "Quy ước Trip API" · **Kiểm bởi:** `TripControllerTest`, `TripServiceTest`, `TripRepositoryTest`, `TripFlowIntegrationTest`

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-TRIP-015 | Xem danh sách khi hệ thống có chuyến đi của nhiều người | Chỉ thấy chuyến đi của **chính mình** | Bảo mật | Đạt |
| TC-TRIP-016 | Lọc theo trạng thái và theo từ khoá | Từ khoá tìm trong cả tên và điểm đến | Đúng | Đạt |
| TC-TRIP-017 | Lọc tháng 10, có một chuyến từ 28/9 đến 3/10 | Chuyến đó **có** trong kết quả, vì khoảng ngày giao với tháng 10 | Biên | Đạt |
| TC-TRIP-018 | Tìm từ khoá `50%` | Chỉ ra "Giảm 50% vé", không ra "Giảm 500 vé". Dấu `%` được hiểu đúng nghĩa đen | Biên | Đạt |
| TC-TRIP-019 | Xem danh sách mà không chọn gì | Mỗi trang 20 chuyến, chuyến mới tạo lên trước | Đúng | Đạt |
| TC-TRIP-020 | Yêu cầu mỗi trang nhiều hơn 100 chuyến | Hệ thống hạ xuống 100 | Biên | Đạt |
| TC-TRIP-021 | Lọc theo một trạng thái không tồn tại | 400 `VALIDATION_ERROR` | Sai | Đạt |
| TC-TRIP-022 | Sắp xếp theo một cột không được hỗ trợ | 400, lỗi ở tham số sắp xếp, kèm danh sách cột được phép | Sai | Đạt |
| TC-TRIP-023 | Xem danh sách sau khi xoá một chuyến | Chuyến đã xoá không còn hiện | Đúng | Đạt |
| TC-TRIP-024 | Xem danh sách khi chưa đăng nhập | 401 | Bảo mật | Đạt |

## D. Xem chi tiết

> **Yêu cầu:** design.md 6.2, 10.2 · **Kiểm bởi:** `TripControllerTest`, `TripServiceTest`, `TripPermissionEvaluatorTest`, `TripFlowIntegrationTest`

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-TRIP-025 | Chủ sở hữu xem chuyến đi của mình | 200 kèm đầy đủ thông tin | Đúng | Đạt |
| TC-TRIP-026 | Người lạ xem chuyến đi của người khác | 403 `FORBIDDEN` | Bảo mật | Đạt |
| TC-TRIP-027 | Xem chuyến đi không tồn tại hoặc đã xoá | 404, kể cả với người lạ | Sai | Đạt |
| TC-TRIP-028 | Mã chuyến đi trên đường dẫn là chữ | 400 `VALIDATION_ERROR` | Sai | Đạt |
| TC-TRIP-029 | Khách chưa đăng nhập bị kiểm quyền | Bị từ chối ngay, không cần tra database | Bảo mật | Đạt |

## E. Sửa chuyến đi

> **Yêu cầu:** design.md 10.2 "Quy ước Trip API", rule 14.1 · **Kiểm bởi:** `TripControllerTest`, `TripServiceTest`, `TripMappingTest`, `TripFlowIntegrationTest`

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-TRIP-030 | Chỉ gửi những ô cần đổi | Ô được gửi thì đổi, ô không gửi giữ nguyên. Tên rút gọn và trạng thái **không đổi** | Đúng | Đạt |
| TC-TRIP-031 | Chỉ gửi ngày kết thúc, và ngày này trước ngày bắt đầu đang lưu | 400. Quy tắc được kiểm trên dữ liệu sau khi đã ghép ô mới với ô cũ | Sai | Đạt |
| TC-TRIP-032 | Chỉ gửi ngày bắt đầu sớm hơn 70 ngày, làm chuyến đi dài thành 73 ngày | 400: "Chuyến đi dài tối đa 60 ngày". Ngày mới được so với ngày kết thúc đang lưu | Biên | Đạt |
| TC-TRIP-033 | Chỉ gửi vĩ độ cho chuyến đi chưa có toạ độ | 400, lỗi ở ô kinh độ còn thiếu | Sai | Đạt |
| TC-TRIP-034 | Sửa tên thành chuỗi chỉ có khoảng trắng | 400 | Sai | Đạt |
| TC-TRIP-035 | Người lạ sửa chuyến đi của người khác | 403, dữ liệu không đổi | Bảo mật | Đạt |
| TC-TRIP-036 | Sửa một chuyến đi | Số phiên bản tăng thêm 1 sau mỗi lần sửa | Đúng | Đạt |

## F. Xoá chuyến đi

> **Yêu cầu:** design.md rule 14.8, 6.2 · **Kiểm bởi:** `TripControllerTest`, `TripServiceTest`, `TripMappingTest`, `TripFlowIntegrationTest`

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-TRIP-037 | Chủ sở hữu xoá chuyến đi | 200. Dòng dữ liệu vẫn còn và được đánh dấu đã xoá. Hệ thống không còn tìm thấy chuyến đi | Đúng | Đạt · từng lỗi BUG-TRIP-001 |
| TC-TRIP-038 | Người lạ xoá chuyến đi của người khác | 403, chuyến đi vẫn còn nguyên | Bảo mật | Đạt |
| TC-TRIP-039 | Xoá chuyến đi không tồn tại | 404 | Sai | Đạt |

## G. Chốt chặn ở database

> **Yêu cầu:** design.md 5.2 bảng `trips` · **Kiểm bởi:** `TripMappingTest`

Các kiểm tra này là lớp bảo vệ cuối cùng. Chúng chặn dữ liệu sai kể cả khi phần kiểm tra ở ứng dụng có lỗi.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-TRIP-040 | Lưu một chuyến đi rồi đọc lại | Mọi thông tin đọc lại đúng, kể cả mô tả dài, toạ độ và số tiền | Đúng | Đạt · từng lỗi BUG-TRIP-003 |
| TC-TRIP-041 | Ghi thẳng vào database một chuyến có ngày kết thúc trước ngày bắt đầu | Database từ chối | Sai | Đạt |
| TC-TRIP-042 | Ghi thẳng vào database hai chuyến trùng tên rút gọn | Database từ chối | Sai | Đạt |

## H. Đổi trạng thái chuyến đi

> **Yêu cầu:** design.md 10.2 bảng Trip (`PATCH /{id}/status`) · **Kiểm bởi:** `TripControllerTest`, `TripServiceTest`, `TripFlowIntegrationTest`

Trạng thái gồm: nháp, đã lên kế hoạch, đang diễn ra, đã hoàn thành, đã lưu trữ. Người dùng tự chọn, chuyển qua lại tự do. Đây là cách duy nhất để đổi trạng thái; form sửa chuyến đi không đổi được trạng thái.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-TRIP-043 | Chủ sở hữu đổi chuyến đi từ nháp sang "đã lên kế hoạch" | 200, trạng thái mới, số phiên bản tăng 1. Lọc danh sách theo "đã lên kế hoạch" thấy chuyến đi, lọc theo "nháp" thì không | Đúng | Đạt |
| TC-TRIP-044 | Lần lượt đổi sang đang diễn ra, đã hoàn thành, đã lưu trữ, rồi quay về nháp | Lần nào cũng 200, database lưu đúng từng trạng thái | Đúng | Đạt |
| TC-TRIP-045 | Gửi lại đúng trạng thái đang có | 200, không ghi gì vào database, số phiên bản không đổi | Biên | Đạt |
| TC-TRIP-046 | Đổi trạng thái | Chỉ trạng thái thay đổi. Tên, ngày giữ nguyên, các ngày của chuyến đi không bị động tới | Đúng | Đạt |
| TC-TRIP-047 | Gửi yêu cầu không có trạng thái | 400, lỗi ở ô `status`: "Trạng thái không được để trống" | Sai | Đạt |
| TC-TRIP-048 | Gửi trạng thái không tồn tại, ví dụ `CANCELLED` | 400 `VALIDATION_ERROR` | Sai | Đạt |
| TC-TRIP-049 | Người lạ đổi trạng thái chuyến đi của người khác | 403, database vẫn giữ trạng thái nháp | Bảo mật | Đạt |
| TC-TRIP-050 | Đổi trạng thái chuyến đi không tồn tại | 404, không ghi gì | Sai | Đạt |

## I. Độ dài mô tả

> **Yêu cầu:** design.md 10.2 "Quy ước Trip API" (mô tả tối đa 1000 ký tự, chốt ở Task 2.5) · **Kiểm bởi:** `TripControllerTest`

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-TRIP-051 | Tạo chuyến đi có mô tả đúng 1000 chữ có dấu tiếng Việt | 201. Giới hạn tính theo số ký tự, không theo số byte | Biên | Đạt |
| TC-TRIP-052 | Tạo chuyến đi có mô tả 1001 ký tự | 400, lỗi ở ô mô tả: "Mô tả không được vượt quá 1000 ký tự". Không tạo gì | Biên | Đạt |
| TC-TRIP-053 | Sửa mô tả thành 1001 ký tự | 400, lỗi ở ô mô tả. Không sửa gì | Biên | Đạt |

## J. Số hoạt động trên danh sách chuyến đi

> **Yêu cầu:** Task 2.6 (`feat/T2.6-ui-guide`), mockup trang danh sách ("5 ngày · 12 hoạt động") · **Kiểm bởi:** `ActivityRepositoryTest`, `TripServiceTest`, `TripControllerTest`, `TripListActivityCountIntegrationTest`

Mỗi dòng của danh sách chuyến đi có thêm tổng số hoạt động của chuyến đó, tính gộp mọi ngày.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-TRIP-054 | Chuyến đi có 2 hoạt động ở ngày 1 và 1 hoạt động ở ngày 2 | Thẻ ghi 3 hoạt động, gộp mọi ngày | Đúng | Đạt |
| TC-TRIP-055 | Chuyến đi chưa có hoạt động nào | Ghi 0, không bị thiếu hay báo lỗi | Biên | Đạt |
| TC-TRIP-056 | Người khác có hoạt động trong chuyến đi của họ | Không được cộng vào số của chủ tài khoản đang xem | Bảo mật | Đạt |
| TC-TRIP-057 | Trang danh sách không có chuyến đi nào | Hệ thống không chạy câu đếm hoạt động | Biên | Đạt |
| TC-TRIP-058 | Số chuyến đi trên trang tăng từ 1 lên 6 | Số câu truy vấn database **không đổi** (2 câu): đếm gộp một lần cho cả trang, không đếm riêng từng thẻ | Đúng | Đạt |

TC-TRIP-058 được kiểm chứng ngược: sửa tạm code cho đếm từng thẻ một thì test này đỏ, trả code về thì xanh.

## K. Số chuyến đi theo trạng thái

> **Yêu cầu:** Task 2.6 (`feat/T2.6-ui-guide`), mockup trang danh sách (chip "Tất cả 6", "Nháp 1"...), endpoint `GET /api/v1/trips/status-counts?q=` · **Kiểm bởi:** `TripRepositoryTest`, `TripServiceTest`, `TripControllerTest`, `TripFlowIntegrationTest`

Con số trên từng chip trạng thái của trang danh sách. Dùng đúng bộ lọc của danh sách (chỉ chuyến của mình, từ khoá tìm kiếm, bỏ chuyến đã xoá), nên số trên chip luôn khớp với kết quả khi bấm chip đó.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-TRIP-059 | Có 2 chuyến nháp và 1 chuyến đã lên kế hoạch | Trả số từng trạng thái, "Tất cả" là 3 | Đúng | Đạt |
| TC-TRIP-060 | Trạng thái không có chuyến nào | Vẫn có mặt trong kết quả với số 0, đủ 5 trạng thái theo đúng thứ tự | Biên | Đạt |
| TC-TRIP-061 | Tài khoản chưa có chuyến đi nào | Tất cả là 0 | Biên | Đạt |
| TC-TRIP-062 | Đang tìm "hội an" (viết hoa, có dấu hay không đều được) | Chỉ đếm các chuyến có tên hoặc điểm đến khớp từ khoá, giống danh sách | Đúng | Đạt |
| TC-TRIP-063 | Chuyến đi của người khác, và chuyến đi đã xoá | Không được đếm | Bảo mật | Đạt |
| TC-TRIP-064 | Gọi khi chưa đăng nhập | 401 | Bảo mật | Đạt |
| TC-TRIP-065 | Từ khoá dài hơn 200 ký tự | 400 `VALIDATION_ERROR`, không đếm gì | Sai | Đạt |

---

## Kiểm tra thủ công

Làm trên trang Swagger `http://localhost:8080/swagger-ui.html` để nhập được tiếng Việt.
Cần có: backend đang chạy, và hai tài khoản đã xác thực email, gọi là A và B.

**Cách đăng nhập trên Swagger:** gọi endpoint đăng nhập, chép `accessToken` trong phản hồi, bấm nút **Authorize** ở đầu trang rồi dán vào.

### MT-TRIP-01 · Tạo và xem chuyến đi

- [x] Đăng nhập bằng tài khoản A.
- [x] Tạo chuyến đi tên "Đà Lạt 3 ngày", từ `2026-10-01` đến `2026-10-03`. Trả 201, trạng thái `DRAFT`, tên rút gọn bắt đầu bằng `da-lat-3-ngay-`.
- [x] Xem danh sách chuyến đi. Có chuyến vừa tạo.
- [x] Xem chi tiết bằng mã vừa nhận. Trả 200, tên hiển thị đúng dấu tiếng Việt.

**Kết quả:** Đạt · **Ngày:** 2026-09-30 · **Ghi chú:** chủ dự án tự chạy trên Swagger

### MT-TRIP-02 · Giới hạn 60 ngày

- [x] Tạo chuyến đi từ `2026-10-01` đến `2026-11-29`. Trả 201, vì đây là đúng 60 ngày.
- [x] Tạo chuyến đi từ `2026-10-01` đến `2026-11-30`. Trả 400, thông báo "Chuyến đi dài tối đa 60 ngày".
- [x] Tạo chuyến đi từ `2026-10-05` đến `2026-10-01`. Trả 400, thông báo ngày kết thúc phải bằng hoặc sau ngày bắt đầu.

**Kết quả:** Đạt · **Ngày:** 2026-09-30 · **Ghi chú:** chủ dự án tự chạy trên Swagger

### MT-TRIP-03 · Người lạ không đụng được chuyến đi

Cần có: mã chuyến đi của tài khoản A từ `MT-TRIP-01`.

- [x] Đăng nhập bằng tài khoản B.
- [x] Xem danh sách. Không có chuyến đi của A.
- [x] Xem chi tiết chuyến đi của A. Trả 403.
- [x] Sửa tên chuyến đi của A. Trả 403.
- [x] Xoá chuyến đi của A. Trả 403.
- [x] Đăng nhập lại bằng A và xem chi tiết. Tên chuyến đi không đổi.

**Kết quả:** Đạt · **Ngày:** 2026-09-30 · **Ghi chú:** chủ dự án tự chạy trên Swagger

### MT-TRIP-04 · Sửa và xoá

- [x] Đăng nhập bằng A. Sửa chuyến đi, chỉ gửi tên mới. Trả 200, ngày và tên rút gọn giữ nguyên, số phiên bản tăng 1.
- [x] Xoá chuyến đi. Trả 200.
- [x] Xem chi tiết chuyến đi vừa xoá. Trả 404.
- [x] Chạy câu lệnh bên dưới trong MySQL. Dòng dữ liệu vẫn còn, cột `deleted_at` có giá trị.

```sql
SELECT id, title, deleted_at FROM trips ORDER BY id DESC LIMIT 5;
```

**Kết quả:** Đạt · **Ngày:** 2026-09-30 · **Ghi chú:** chủ dự án tự chạy trên Swagger

### MT-TRIP-05 · Đổi trạng thái

Cần có: một chuyến đi của tài khoản A đang ở trạng thái `DRAFT`.

- [ ] Đăng nhập bằng A. Gọi `PATCH /api/v1/trips/{id}/status` với body `{ "status": "PLANNED" }`. Trả 200, `status` là `PLANNED`, số phiên bản tăng 1.
- [ ] Gọi lại đúng body đó. Trả 200, số phiên bản **không** tăng.
- [ ] Gọi danh sách với `status=PLANNED`. Có chuyến đi này.
- [ ] Gửi body `{}`. Trả 400, lỗi ở ô `status`: "Trạng thái không được để trống".
- [ ] Gửi body `{ "status": "CANCELLED" }`. Trả 400.
- [ ] Đăng nhập bằng B, gọi đổi trạng thái chuyến đi của A. Trả 403.

**Kết quả:** Chưa chạy

---

## Lỗi đã phát hiện

Nguồn: mục "Bẫy đã gặp khi làm 2.1" trong `WORKFLOW.md`. Các lỗi này được sửa trước khi code được gộp vào nhánh chính.

| Mã lỗi | Test case | Ngày | Hiện tượng | Nguyên nhân | Cách sửa | Trạng thái |
|---|---|---|---|---|---|---|
| BUG-TRIP-001 | TC-TRIP-037 | 2026-09-26 | Xoá chuyến đi báo lỗi | Chuyến đi có số phiên bản, nên lệnh xoá nhận hai giá trị là mã và phiên bản. Câu lệnh xoá mềm chỉ chừa chỗ cho một | Câu lệnh xoá mềm so cả mã lẫn phiên bản | Đã sửa, commit `1c4c3f5` |
| BUG-TRIP-002 | TC-TRIP-014 | 2026-09-26 | Kiểm tra trùng tên rút gọn không thấy chuyến đi đã xoá, trong khi database vẫn giữ tên đó | Mọi câu truy vấn thông thường đều tự ẩn dữ liệu đã xoá mềm | Dùng câu truy vấn trực tiếp, đếm cả chuyến đã xoá | Đã sửa, commit `7abf0df` |
| BUG-TRIP-003 | TC-TRIP-040 | 2026-09-26 | Ứng dụng không khởi động, báo kiểu cột không khớp | Cột mô tả dài và cột mã tiền tệ 3 ký tự được khai báo khác nhau giữa code và database | Khai báo rõ kiểu cột trong code | Đã sửa, commit `1c4c3f5` |

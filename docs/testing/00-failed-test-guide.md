# 00 · Hướng dẫn khi test lỗi

> [Về trang chính](README.md)

File này chỉ cách ghi lại và xử lý một test lỗi. Ví dụ 1 và 2 là **giả định** để minh hoạ định dạng. Ví dụ 3 là lỗi **có thật** của dự án.

---

## Nguyên tắc: hiện tại và lịch sử

| Nơi ghi | Cho biết điều gì | Khi lỗi được sửa |
|---|---|---|
| Dòng test case | Tình trạng **hiện tại** | Trạng thái đổi về `Đạt`, kèm mã lỗi từng gặp |
| Mục "Lỗi đã phát hiện" | **Lịch sử** các lỗi | Dòng lỗi được giữ nguyên mãi mãi, chỉ điền thêm cách sửa |

Nhờ mã lỗi đi kèm, người đọc nhìn dòng test case là biết nó từng lỗi và tra được chuyện gì đã xảy ra.

| Thời điểm | Cột trạng thái của test case | Trạng thái của dòng lỗi |
|---|---|---|
| Chưa từng lỗi | `Đạt` | Không có dòng lỗi |
| Đang lỗi | `Lỗi · BUG-PLAT-001` | `Đang mở` |
| Đã sửa | `Đạt · từng lỗi BUG-PLAT-001` | `Đã sửa` kèm mã commit |

---

## Quy tắc số một: ghi trước, sửa sau

Khi một test lỗi ngoài dự kiến, việc **đầu tiên** là ghi lỗi vào tài liệu với trạng thái `Đang mở`. Sau đó mới sửa.

Lý do: lỗi sửa xong trong vài phút rất dễ bị quên ghi lại. Khi đó tài liệu chỉ còn toàn dòng `Đạt` và mất đi phần giá trị nhất, là lịch sử những gì từng sai.

Lúc ghi chỉ cần hai thứ đã biết chắc: **hiện tượng** và **thông báo lỗi thật**. Nguyên nhân và cách sửa để trống, điền sau khi sửa xong.

Không cần ghi khi test lỗi do gõ nhầm trong chính test đang viết dở, hoặc do code chưa biên dịch được. Nếu phân vân thì vẫn ghi.

---

## Các bước xử lý

| Bước | Việc cần làm | Ghi vào đâu |
|---|---|---|
| 1 | Xác định test nào lỗi | Đọc kết quả build hoặc báo cáo `build/reports/tests/test/index.html` |
| 2 | Tìm test case tương ứng | Tra tên class test ở dòng "Kiểm bởi" trong file của tính năng |
| 3 | Xác định **ai sai**: code, yêu cầu, hay test | Xem bảng bên dưới |
| 4 | **Ghi nhận lỗi ngay, trước khi sửa** | Đổi trạng thái test case thành `Lỗi`, thêm một dòng `Đang mở` vào mục "Lỗi đã phát hiện" |
| 5 | Sửa rồi chạy lại toàn bộ test | `./gradlew build` |
| 6 | Đóng lỗi | Trạng thái test case về `Đạt`, trạng thái lỗi thành `Đã sửa` kèm mã commit |

### Bước 3: ai sai?

Đây là bước quan trọng nhất. Test lỗi không có nghĩa là code sai.

| Tình huống | Dấu hiệu | Cách xử lý |
|---|---|---|
| **Code sai** | Kết quả thật khác với `design.md` | Sửa code. Không sửa test |
| **Yêu cầu đã đổi** | Đã thống nhất đổi quy tắc, nhưng test còn theo quy tắc cũ | Sửa `design.md` trước, rồi sửa test case và test |
| **Test sai** | Test kiểm một điều mà `design.md` không yêu cầu | Sửa test, ghi lý do vào cột ghi chú của lỗi |

### Ba điều không được làm

1. Không xoá test đang lỗi để build xanh.
2. Không sửa "kết quả mong đợi" cho khớp với kết quả thật, trừ khi yêu cầu thật sự đã đổi.
3. Không commit khi test đang đỏ.

### Trạng thái của một lỗi

| Trạng thái | Ý nghĩa |
|---|---|
| `Đang mở` | Đã ghi nhận, chưa sửa |
| `Đã sửa` | Đã sửa và test đã chạy lại đạt |
| `Không sửa` | Quyết định giữ nguyên, phải ghi lý do |

---

## Ví dụ 1 · Test tự động lỗi (giả định)

**Tình huống giả định:** khi thêm endpoint mới, dev đưa nhầm cả nhóm đường dẫn `/api/v1/**` vào danh sách công khai.

### Bước 1. Build báo đỏ

```
SecurityConfigTest > unknownUrlWithoutLoginIsAlso401NotFound404() FAILED
    Expected status: 401 UNAUTHORIZED
    Actual status:   404 NOT_FOUND

270 tests completed, 1 failed
BUILD FAILED
```

### Bước 2 và 3. Tìm test case và xác định ai sai

`SecurityConfigTest` nằm ở nhóm C của `01-platform.md`. Kịch bản khớp là `TC-PLAT-018`.
`design.md` 6.3 yêu cầu khách không dò được đường dẫn. Kết quả thật trả 404 là trái yêu cầu. Kết luận: **code sai**.

### Bước 4. Ghi nhận

Dòng test case trong `01-platform.md` đổi thành:

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-PLAT-018 | Khách chưa đăng nhập gọi đường dẫn không tồn tại | 401, **không phải** 404, để khách không dò được đường dẫn nào có thật | Bảo mật | **Lỗi · BUG-PLAT-001** |

Mục "Lỗi đã phát hiện" ở cuối `01-platform.md` có thêm:

| Mã lỗi | Test case | Ngày | Hiện tượng | Nguyên nhân | Cách sửa | Trạng thái |
|---|---|---|---|---|---|---|
| BUG-PLAT-001 | TC-PLAT-018 | 2026-10-02 | Khách chưa đăng nhập nhận 404 thay vì 401 | Cả nhóm `/api/v1/**` bị đưa vào danh sách công khai | | Đang mở |

### Bước 5 và 6. Sửa, chạy lại, đóng lỗi

Sau khi sửa và build xanh, hai dòng trên trở thành:

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-PLAT-018 | Khách chưa đăng nhập gọi đường dẫn không tồn tại | 401, **không phải** 404, để khách không dò được đường dẫn nào có thật | Bảo mật | Đạt · từng lỗi BUG-PLAT-001 |

| Mã lỗi | Test case | Ngày | Hiện tượng | Nguyên nhân | Cách sửa | Trạng thái |
|---|---|---|---|---|---|---|
| BUG-PLAT-001 | TC-PLAT-018 | 2026-10-02 | Khách chưa đăng nhập nhận 404 thay vì 401 | Cả nhóm `/api/v1/**` bị đưa vào danh sách công khai | Chỉ mở công khai đúng endpoint mới | Đã sửa, commit `abc1234` |

Dòng lỗi **được giữ lại** sau khi sửa. Dòng test case cũng giữ mã lỗi để người đọc biết nó từng lỗi.

---

## Ví dụ 2 · Test thủ công lỗi (giả định)

**Tình huống giả định:** bạn chạy `MT-PLAT-02`, backend không khởi động được.

Bước nào đạt thì đánh dấu `[x]`. Bước lỗi để trống ô và ghi kết quả thật ngay bên dưới.

### MT-PLAT-02 · Backend khởi động

- [x] Trong `backend/`, chạy `./gradlew bootRun --args='--spring.profiles.active=local'`. Log có dòng `Started TripPlannerApplication`.
- [ ] Đọc các dòng Flyway trong log. Không có lỗi `checksum mismatch`.
  - **Kết quả thật:** log báo `Migration checksum mismatch for migration version 6`, ứng dụng dừng.
- [ ] Chạy `curl.exe -s -i http://localhost:8080/actuator/health`. Trả 200 và `{"status":"UP"}`.
  - **Không chạy được** vì bước trước lỗi.

**Kết quả:** Lỗi · BUG-PLAT-002 · **Ngày:** 2026-10-02 · **Ghi chú:** dừng ở bước 2

Dòng lỗi tương ứng:

| Mã lỗi | Test case | Ngày | Hiện tượng | Nguyên nhân | Cách sửa | Trạng thái |
|---|---|---|---|---|---|---|
| BUG-PLAT-002 | MT-PLAT-02 | 2026-10-02 | Backend dừng lúc khởi động, báo `checksum mismatch` ở phiên bản 6 | File tạo bảng số 6 bị sửa sau khi đã chạy | Trả file về nội dung cũ, viết thay đổi vào file mới số 8 | Đã sửa, commit `def5678` |

Sau khi sửa, chạy lại **cả ba bước** từ đầu. Phần test thủ công luôn cho thấy **lần chạy mới nhất**, nên nó trở thành:

- [x] Trong `backend/`, chạy `./gradlew bootRun --args='--spring.profiles.active=local'`. Log có dòng `Started TripPlannerApplication`.
- [x] Đọc các dòng Flyway trong log. Không có lỗi `checksum mismatch`.
- [x] Chạy `curl.exe -s -i http://localhost:8080/actuator/health`. Trả 200 và `{"status":"UP"}`.

**Kết quả:** Đạt · **Ngày:** 2026-10-03 · **Ghi chú:** từng lỗi BUG-PLAT-002 ngày 2026-10-02

Các dòng "Kết quả thật" của lần chạy lỗi được xoá khỏi danh sách bước, vì nội dung đó đã nằm trong cột hiện tượng của BUG-PLAT-002.

---

## Ví dụ 3 · Lỗi có thật: giờ bị lưu lệch múi giờ

Lỗi này xảy ra ngày 2026-09-29 khi làm Mốc 1 của Task 2.3. Nó được ghi trong [05-activity.md](05-activity.md).

Test báo đỏ:

```
ActivityMappingTest > timesAreStoredAsWrittenWithoutTimeZoneShift() FAILED
    org.hibernate.exception.ConstraintViolationException

11 tests completed, 1 failed
```

Ai sai: `design.md` 5.2 quy định giờ kết thúc phải sau giờ bắt đầu, và dữ liệu nhập vào hợp lệ. Kết luận: **code sai**.

| Mã lỗi | Test case | Ngày | Hiện tượng | Nguyên nhân | Cách sửa | Trạng thái |
|---|---|---|---|---|---|---|
| BUG-ACT-001 | TC-ACT-003 | 2026-09-29 | Hoạt động 03:00 đến 08:15 bị lưu thành 20:00 đến 01:15, database từ chối | Ứng dụng đổi mọi giá trị thời gian sang UTC, kể cả giờ trong ngày không kèm ngày | Ghi nguyên văn hai cột giờ, không chuyển đổi | Đã sửa, commit `3c4c294` |

Lỗi này được phát hiện và sửa trong cùng một buổi làm việc, trước khi commit. Vì vậy test case của nó ghi thẳng `Đạt`, không có giai đoạn ghi `Lỗi`.

Trạng thái `Lỗi` chỉ xuất hiện trong tài liệu khi lỗi **chưa sửa được ngay** và phải để lại sang lần sau.

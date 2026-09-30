# 06 · Giao diện lịch trình

> Cập nhật: 2026-09-30 · Task 2.5 đã merge (`eb1ad5e`), lint + build xanh · kiểm tra thủ công chưa chạy · [Về trang chính](README.md)

Giao diện web để người dùng xem danh sách chuyến đi, tạo chuyến đi, xem và sửa lịch trình từng ngày, thêm hoạt động và kéo thả để sắp xếp lại. Làm ở Task 2.5.

Phần giao diện chưa có test tự động (công cụ sẽ thêm ở task frontend sau). Mỗi mốc chỉ được commit khi hai lệnh kiểm tra mã nguồn chạy xanh:

| Lệnh | Kiểm tra gì |
|---|---|
| `npm run lint` | Mã nguồn viết đúng quy tắc, ví dụ không có biến thừa, không gọi hook React sai chỗ |
| `npm run build` | Đúng kiểu dữ liệu trên toàn bộ mã nguồn, và đóng gói được thành bản chạy thật |

Hai lệnh này không kiểm tra giao diện có làm đúng nghiệp vụ hay không. Việc đó do các bài kiểm tra thủ công bên dưới đảm nhận.

File này được ghi dần theo từng mốc của task. Mỗi mốc là một commit:

| Mốc | Nội dung | Kiểm tra thủ công | Commit |
|---|---|---|---|
| 1 | Trang danh sách chuyến đi | MT-UI-01 đến MT-UI-05 | `9fd9635` |
| 2 | Tạo chuyến đi qua 3 bước | MT-UI-06 đến MT-UI-08 | `db7be12` |
| 3 | Trang chi tiết chuyến đi, sửa tiêu đề và ghi chú của ngày | MT-UI-09 đến MT-UI-12 | `e81b0c3` |
| 4 | Sửa và xoá chuyến đi, hỏi lại khi đổi ngày làm mất hoạt động; nút sửa ngày gọn hơn, ngày chưa có tiêu đề ghi "Chưa có tiêu đề" | MT-UI-13 đến MT-UI-18 | `b017310` |
| 5 | Đổi trạng thái chuyến đi (backend + ô chọn trên trang chi tiết). Test tự động ở [03-trip.md](03-trip.md) phần H | MT-UI-19 | `98d0d6f` |
| 6 | Thêm, sửa, xoá hoạt động; hỏi lại khi trùng giờ | MT-UI-20 đến MT-UI-24 | `e5d877b` (tách dùng chung), `f589842` |
| 7 | Kéo thả để sắp xếp hoạt động, trong ngày và sang ngày khác | MT-UI-25 đến MT-UI-30 | `b14516d` |
| Sau Mốc 7 | Rút giới hạn: ghi chú của ngày và hoạt động 255 ký tự, mô tả chuyến đi 1000 ký tự | MT-UI-31 | `007adb7` |
| Sau Mốc 7 | Ghi chú dài hơn 2 dòng được thu gọn; tên chuyến đi dài không đẩy nút xuống | MT-UI-32, MT-UI-33 | `35b95a1` |

---

## Kiểm tra thủ công

Cần có:

1. Docker đang chạy MySQL, Redis, MailHog (`docker compose up -d mysql redis mailhog`).
2. Backend đang chạy bản code mới nhất, profile `local`.
3. Frontend đang chạy: trong thư mục `frontend`, chạy `npm run dev`, mở `http://localhost:5173`.
4. Một tài khoản đã xác thực email.

Từ Mốc 2 có thể tạo chuyến đi ngay trên giao diện (`MT-UI-06`). Với các bài của Mốc 1, cách nhanh nhất vẫn là tạo dữ liệu mẫu bằng Swagger `http://localhost:8080/swagger-ui.html` với `POST /api/v1/trips`, ví dụ ba chuyến đi:

```json
{ "title": "Đà Lạt mùa hoa", "destinationName": "Đà Lạt", "startDate": "2026-10-01", "endDate": "2026-10-03" }
{ "title": "Hội An cuối tuần", "destinationName": "Hội An", "startDate": "2026-11-07", "endDate": "2026-11-08" }
{ "title": "Họp lớp", "startDate": "2026-12-20", "endDate": "2026-12-20" }
```

### MT-UI-01 · Xem danh sách chuyến đi

- [ ] Đăng nhập. Trang chuyển tới `/trips`, tiêu đề "Chuyến đi của tôi", đầu trang có tên người dùng và nút "Đăng xuất".
- [ ] Ba chuyến đi hiện thành ba thẻ, chuyến tạo sau cùng đứng đầu.
- [ ] Thẻ "Đà Lạt mùa hoa" ghi điểm đến "Đà Lạt", ngày "01/10/2026 – 03/10/2026 · 3 ngày", nhãn trạng thái "Nháp".
- [ ] Thẻ "Họp lớp" không có dòng điểm đến, ngày chỉ ghi "20/12/2026 · 1 ngày".
- [ ] Thu hẹp cửa sổ trình duyệt: lưới đổi từ 3 cột xuống 2 cột rồi 1 cột, không bị tràn ngang.
- [ ] Bấm "Đăng xuất". Quay về trang đăng nhập.
- [ ] Đăng nhập bằng một tài khoản khác chưa có chuyến đi nào. Trang ghi "Bạn chưa có chuyến đi nào.", không thấy chuyến đi của tài khoản đầu.

**Kết quả:** Chưa chạy

### MT-UI-02 · Tìm chuyến đi

- [ ] Gõ `hội an` vào ô tìm kiếm rồi dừng tay. Khoảng nửa giây sau chỉ còn thẻ "Hội An cuối tuần". Thanh địa chỉ có `?q=hội+an` (hoặc dạng mã hoá của nó).
- [ ] Nhấn F5. Ô tìm kiếm vẫn ghi `hội an`, danh sách vẫn chỉ có một thẻ.
- [ ] Gõ `đà` (tìm theo tên chuyến đi và điểm đến, không phân biệt hoa thường). Còn thẻ "Đà Lạt mùa hoa".
- [ ] Gõ `không có chuyến này`. Trang ghi "Không tìm thấy chuyến đi nào phù hợp."
- [ ] Bấm nút Back của trình duyệt. Ô tìm kiếm và danh sách quay về đúng kết quả của lần tìm trước.
- [ ] Xoá hết chữ trong ô tìm kiếm. Hiện lại cả ba thẻ, thanh địa chỉ không còn `?q=`.

**Kết quả:** Chưa chạy

### MT-UI-03 · Lọc theo trạng thái

Chuyến đi mới tạo luôn ở trạng thái "Nháp". Đổi trạng thái trên giao diện có ở mốc sau.

- [ ] Chọn "Nháp". Hiện cả ba thẻ, thanh địa chỉ có `?status=DRAFT`.
- [ ] Chọn "Đã lên kế hoạch". Trang ghi "Không tìm thấy chuyến đi nào phù hợp."
- [ ] Chọn "Tất cả trạng thái". Hiện lại cả ba thẻ.
- [ ] Sửa thanh địa chỉ thành `http://localhost:5173/trips?status=ABC` rồi Enter. Trang không báo lỗi, hiện cả ba thẻ như "Tất cả trạng thái".

**Kết quả:** Chưa chạy

### MT-UI-04 · Phân trang

Mỗi trang có 12 chuyến đi. Cần ít nhất 13 chuyến đi: tạo thêm 10 chuyến bằng Swagger (tên tuỳ ý).

- [ ] Trang đầu có 12 thẻ, cuối trang ghi "Trang 1 / 2". Nút "Trước" bị mờ, không bấm được.
- [ ] Bấm "Sau". Trang 2 có phần còn lại, thanh địa chỉ có `?page=2`, nút "Sau" bị mờ.
- [ ] Bấm nút Back của trình duyệt. Quay về trang 1.
- [ ] Đang ở trang 2, gõ một từ vào ô tìm kiếm. Danh sách quay về trang 1 của kết quả tìm.
- [ ] Sửa thanh địa chỉ thành `?page=0` hoặc `?page=abc`. Trang hiện trang 1, không báo lỗi.

**Kết quả:** Chưa chạy

### MT-UI-05 · Mất kết nối máy chủ

- [ ] Đang ở trang danh sách, tắt backend.
- [ ] Đổi bộ lọc trạng thái. Trang báo "Không kết nối được máy chủ, vui lòng thử lại" và có nút "Thử lại".
- [ ] Bật lại backend, bấm "Thử lại". Danh sách hiện lại bình thường.

**Kết quả:** Chưa chạy

### MT-UI-06 · Tạo chuyến đi đủ ba bước

- [ ] Ở trang danh sách, bấm "Tạo chuyến đi". Trang chuyển tới `/trips/new`, thanh bước ghi "1. Thông tin" đang được chọn.
- [ ] Bước 1: nhập tên `Sapa săn mây`, mô tả `Đi cùng gia đình`, bỏ trống ảnh bìa. Bấm "Tiếp".
- [ ] Bước 2: nhập điểm đến `Sa Pa`. Bấm "Tiếp".
- [ ] Bước 3: chọn ngày `05/12/2026` đến `07/12/2026`. Dưới ô ngày hiện "Chuyến đi dài 3 ngày. Tối đa 60 ngày."
- [ ] Nhập ngân sách `5000000`, giữ tiền tệ `VND`. Bấm "Tạo chuyến đi".
- [ ] Trang chuyển sang trang chi tiết của chuyến đi vừa tạo (từ Mốc 3; ở Mốc 2 trang quay về danh sách). Bấm "← Chuyến đi của tôi". Thẻ "Sapa săn mây" đứng đầu, ghi "Sa Pa", "05/12/2026 – 07/12/2026 · 3 ngày", nhãn "Nháp".
- [ ] Trên Swagger, gọi `GET /api/v1/trips/{id}` với mã của chuyến đi vừa tạo. Có đúng 3 ngày, mô tả và tên có dấu tiếng Việt đúng, `budgetAmount` là `5000000`, `currency` là `VND`.

**Kết quả:** Chưa chạy

### MT-UI-07 · Báo lỗi ở từng bước

- [ ] Bước 1 để trống tên, bấm "Tiếp". Ô tên báo "Tên chuyến đi không được để trống", vẫn ở bước 1.
- [ ] Nhập tên, nhập ảnh bìa `abc`, bấm "Tiếp". Ô ảnh bìa báo "Đường dẫn ảnh bìa phải bắt đầu bằng http:// hoặc https://".
- [ ] Xoá ảnh bìa, sang bước 2 rồi bấm "Quay lại". Tên vừa nhập vẫn còn.
- [ ] Sang bước 3, bấm "Tạo chuyến đi" khi chưa chọn ngày. Hai ô ngày báo "Ngày bắt đầu không được để trống" và "Ngày kết thúc không được để trống".
- [ ] Chọn ngày bắt đầu `10/12/2026`, ngày kết thúc `09/12/2026` (gõ tay nếu lịch không cho chọn). Ô ngày kết thúc báo "Ngày kết thúc phải bằng hoặc sau ngày bắt đầu".
- [ ] Chọn `01/01/2027` đến `02/03/2027` (61 ngày). Ô ngày kết thúc báo "Chuyến đi dài tối đa 60 ngày".
- [ ] Đổi ngày kết thúc thành `01/03/2027` (đúng 60 ngày). Dòng dưới ghi "Chuyến đi dài 60 ngày." và không còn báo lỗi.
- [ ] Nhập ngân sách lần lượt `-5`, `abc`, `1.234`. Ô ngân sách báo lần lượt "Ngân sách không được âm", "Ngân sách phải là một số, ví dụ 5000000", "Ngân sách có tối đa 13 chữ số phần nguyên và 2 chữ số thập phân".
- [ ] Xoá ngân sách, bấm "Tạo chuyến đi". Chuyến đi 60 ngày được tạo, danh sách ghi "60 ngày".

**Kết quả:** Chưa chạy

### MT-UI-08 · Chỉ nhập phần bắt buộc, huỷ giữa chừng

- [ ] Mở "Tạo chuyến đi", nhập tên `Đi chơi một ngày` rồi nhấn phím Enter. Wizard sang bước 2 (Enter có tác dụng như "Tiếp").
- [ ] Bỏ trống điểm đến, nhấn Enter. Sang bước 3.
- [ ] Chọn cùng một ngày cho cả hai ô, bấm "Tạo chuyến đi", rồi quay về danh sách. Thẻ mới không có dòng điểm đến, ngày ghi "· 1 ngày".
- [ ] Mở "Tạo chuyến đi" lần nữa, nhập tên rồi bấm "Huỷ". Quay về danh sách, không có chuyến đi mới nào được tạo.

**Kết quả:** Chưa chạy

### MT-UI-09 · Xem chi tiết chuyến đi

> Trạng thái: Chưa chạy lại sau khi sửa · từng lỗi BUG-UI-001

Dùng chuyến đi "Sapa săn mây" (05/12/2026 đến 07/12/2026) tạo ở `MT-UI-06`.

- [ ] Ở danh sách, bấm vào thẻ "Sapa săn mây". Trang chuyển tới `/trips/{mã}`.
- [ ] Đầu trang có tên chuyến đi, nhãn "Nháp", dòng "Sa Pa · 05/12/2026 – 07/12/2026 · 3 ngày · Ngân sách 5.000.000 ₫", và mô tả "Đi cùng gia đình".
- [ ] Có ba khung ngày, lần lượt ghi "Ngày 1 · Thứ bảy, 05/12/2026", "Ngày 2 · Chủ nhật, 06/12/2026", "Ngày 3 · Thứ hai, 07/12/2026". Dưới dòng ngày của mỗi khung có chữ nghiêng màu xám "Chưa có tiêu đề" (từ Mốc 4), và dòng "Chưa có hoạt động nào."
- [ ] Màn hình rộng: bên trái có danh sách "Ngày 1 · 05/12", "Ngày 2 · 06/12", "Ngày 3 · 07/12", mỗi dòng có số 0 và dòng nhỏ nghiêng "Chưa có tiêu đề" (từ Mốc 4). Bấm "Ngày 3", trang cuộn tới khung ngày 3.
- [ ] Thu hẹp cửa sổ: danh sách bên trái ẩn đi, các khung ngày chiếm hết chiều ngang.
- [ ] Nhấn F5. Trang chi tiết tải lại đúng chuyến đi, vẫn đăng nhập.
- [ ] Mở chuyến đi 60 ngày tạo ở `MT-UI-07`. Đủ 60 khung ngày, danh sách bên trái cuộn được.

**Kết quả:** Chưa chạy

### MT-UI-10 · Hoạt động hiện trong ngày

Từ Mốc 6 có thể thêm hoạt động ngay trên giao diện (`MT-UI-20`). Ở Mốc 3, tạo hoạt động bằng Swagger: gọi `POST /api/v1/trips/{tripId}/days/{dayId}/activities` cho Ngày 1 của "Sapa săn mây" (mã ngày lấy từ `GET /api/v1/trips/{tripId}/days`), lần lượt với hai nội dung:

```json
{ "title": "Ăn trưa lẩu cá hồi", "type": "FOOD", "startTime": "11:30", "endTime": "13:00", "note": "Đặt bàn trước", "costAmount": 350000, "bookingUrl": "https://example.com/dat-ban" }
{ "title": "Dạo chợ đêm" }
```

- [ ] Nhấn F5 trang chi tiết. Ngày 1 có hai hoạt động, "Ăn trưa lẩu cá hồi" đứng trước.
- [ ] Hoạt động đầu ghi giờ "11:30 – 13:00", nhãn "Ăn uống", ghi chú "Đặt bàn trước", chi phí "350.000 ₫" và "Link đặt chỗ".
- [ ] Bấm "Link đặt chỗ". Trang `example.com` mở ở tab mới, tab chuyến đi vẫn còn.
- [ ] Hoạt động thứ hai ghi "Chưa đặt giờ" ở cột giờ, nhãn "Khác", không có chi phí.
- [ ] Danh sách ngày bên trái: dòng "Ngày 1" có số 2.

**Kết quả:** Chưa chạy

### MT-UI-11 · Sửa tiêu đề và ghi chú của ngày

- [ ] Góc phải mỗi khung ngày có nút nhỏ "✎ Sửa" màu xám, không viền. Rê chuột lên thì nút có nền xám nhạt và chữ đậm hơn (từ Mốc 4).
- [ ] Ở Ngày 2, bấm "✎ Sửa". Khung ngày đổi thành form có ô "Tiêu đề của ngày" và "Ghi chú".
- [ ] Nhập tiêu đề `Chinh phục Fansipan`, ghi chú hai dòng `Đi cáp treo` và `Mang áo ấm`. Bấm "Lưu".
- [ ] Khung ngày hiện tiêu đề và ghi chú đúng hai dòng. Danh sách ngày bên trái có tiêu đề thay cho "Chưa có tiêu đề" dưới "Ngày 2".
- [ ] Bấm "✎ Sửa" lần nữa, sửa tiêu đề rồi bấm "Huỷ". Tiêu đề cũ vẫn giữ nguyên.
- [ ] Bấm "✎ Sửa", xoá hết tiêu đề, bấm "Lưu". Tiêu đề đổi lại thành "Chưa có tiêu đề" ở cả khung ngày lẫn danh sách bên trái, ghi chú vẫn còn.
- [ ] Trên Swagger, gọi `GET /api/v1/trips/{tripId}/days`. Ngày 2 có `title` là `null`, `note` còn nguyên.
- [ ] Dán một tiêu đề dài 161 ký tự, bấm "Lưu". Ô tiêu đề báo "Tiêu đề của ngày không được vượt quá 160 ký tự".

**Kết quả:** Chưa chạy

### MT-UI-12 · Không mở được chuyến đi

- [ ] Sửa thanh địa chỉ thành `http://localhost:5173/trips/999999`. Trang báo "Không tìm thấy chuyến đi. Có thể chuyến đi đã bị xoá." kèm link về danh sách.
- [ ] Sửa thành `http://localhost:5173/trips/abc`. Trang báo "Không tìm thấy chuyến đi.".
- [ ] Đăng nhập bằng tài khoản thứ hai, mở địa chỉ chi tiết của "Sapa săn mây" (của tài khoản đầu). Trang báo "Bạn không có quyền xem chuyến đi này."
- [ ] Quay lại tài khoản đầu, xoá một chuyến đi bằng Swagger (`DELETE /api/v1/trips/{id}`), rồi mở địa chỉ chi tiết của nó. Trang báo "Không tìm thấy chuyến đi. Có thể chuyến đi đã bị xoá."

**Kết quả:** Chưa chạy

### MT-UI-13 · Sửa thông tin chuyến đi

Dùng chuyến đi "Sapa săn mây" ở `MT-UI-09`, Ngày 1 có hai hoạt động từ `MT-UI-10`.

- [ ] Ở trang chi tiết, bấm "Sửa". Hộp "Sửa chuyến đi" mở ra, các ô đã điền sẵn thông tin hiện tại, kể cả ngân sách `5000000` và tiền tệ `VND`.
- [ ] Bấm "Huỷ". Hộp đóng lại, không có gì thay đổi. Mở lại rồi nhấn phím Esc, hộp cũng đóng.
- [ ] Mở lại, không sửa gì, bấm "Lưu". Hộp đóng, không có gì thay đổi.
- [ ] Mở lại, sửa tên thành `Sapa săn mây mùa đông`, điểm đến thành `Lào Cai`, ngân sách thành `6500000`. Bấm "Lưu".
- [ ] Hộp đóng. Đầu trang hiện tên mới, "Lào Cai" và "Ngân sách 6.500.000 ₫". Các ngày và hoạt động giữ nguyên.
- [ ] Bấm "← Chuyến đi của tôi". Thẻ hiện tên và điểm đến mới.
- [ ] Mở lại "Sửa", xoá hết tên, bấm "Lưu". Ô tên báo "Tên chuyến đi không được để trống", hộp vẫn mở.

**Kết quả:** Chưa chạy

### MT-UI-14 · Không xoá trắng được thông tin đã nhập

Hệ thống chưa hỗ trợ bỏ trống một thông tin tuỳ chọn đã nhập (design.md 10.2). Giao diện phải báo rõ thay vì âm thầm giữ giá trị cũ.

- [ ] Mở "Sửa", xoá hết ô điểm đến, bấm "Lưu". Ô điểm đến báo "Chưa hỗ trợ xoá thông tin này, hãy nhập giá trị mới", hộp vẫn mở.
- [ ] Nhập lại điểm đến, xoá hết ô ngân sách, bấm "Lưu". Ô ngân sách báo cùng câu đó.
- [ ] Bấm "Huỷ". Thông tin chuyến đi không đổi.

**Kết quả:** Chưa chạy

### MT-UI-15 · Dời cả chuyến đi

Chuyến đi đang từ 05/12/2026 đến 07/12/2026.

- [ ] Mở "Sửa", đổi ngày thành `12/12/2026` đến `14/12/2026`. Dưới ô ngày hiện khung xanh "Dời cả chuyến đi: mọi ngày và hoạt động dời theo, không mất gì."
- [ ] Bấm "Lưu". Không có câu hỏi nào, hộp đóng.
- [ ] Ngày 1 giờ là "Thứ bảy, 12/12/2026" và vẫn có hai hoạt động. Tiêu đề và ghi chú của Ngày 2 (nếu có từ `MT-UI-11`) vẫn nằm ở Ngày 2.

**Kết quả:** Chưa chạy

### MT-UI-16 · Kéo dài và rút ngắn ở cuối chuyến

- [ ] Mở "Sửa", đổi ngày kết thúc thành `15/12/2026`. Dưới ô ngày hiện khung đỏ bắt đầu bằng "Đổi độ dài".
- [ ] Bấm "Lưu". Chuyến đi có 4 ngày, Ngày 4 trống.
- [ ] Mở "Sửa", đổi ngày kết thúc về `14/12/2026`, bấm "Lưu". Ngày 4 trống nên không có câu hỏi nào, chuyến đi còn 3 ngày.

**Kết quả:** Chưa chạy

### MT-UI-17 · Bỏ ngày đầu đang có hoạt động

Chuyến đi đang từ 12/12/2026 đến 14/12/2026, Ngày 1 có hai hoạt động.

- [ ] Mở "Sửa", đổi ngày bắt đầu thành `13/12/2026`, giữ ngày kết thúc `14/12/2026`. Khung đỏ bắt đầu bằng "Vừa dời vừa đổi độ dài" và hướng dẫn làm hai lần.
- [ ] Bấm "Lưu". Hộp hỏi "Vừa dời vừa đổi độ dài chuyến đi?". Bấm "Huỷ": hộp hỏi đóng, form sửa vẫn mở với ngày vừa nhập.
- [ ] Bấm "Lưu" rồi "Vẫn lưu". Hộp hỏi thứ hai hiện "Xoá hoạt động khi đổi ngày?" với câu "2 hoạt động trong 1 ngày sẽ bị xoá nếu đổi ngày." và nút đỏ "Vẫn đổi ngày".
- [ ] Bấm "Huỷ". Đóng hộp sửa, nhấn F5: chuyến đi vẫn từ 12/12 đến 14/12, hai hoạt động còn nguyên.
- [ ] Làm lại từ đầu, lần này bấm "Vẫn đổi ngày". Chuyến đi còn 2 ngày: Ngày 1 là 13/12/2026, Ngày 2 là 14/12/2026. Hai hoạt động của ngày 12/12 đã bị xoá.

**Kết quả:** Chưa chạy

### MT-UI-18 · Xoá chuyến đi

- [ ] Ở trang chi tiết một chuyến đi, bấm nút đỏ "Xoá". Hộp "Xoá chuyến đi?" hiện tên chuyến đi.
- [ ] Bấm "Huỷ". Hộp đóng, chuyến đi còn nguyên.
- [ ] Bấm "Xoá" rồi "Xoá chuyến đi". Trang chuyển về danh sách, thẻ của chuyến đi đó biến mất.
- [ ] Bấm nút Back của trình duyệt. Không quay lại trang chi tiết của chuyến đi đã xoá.
- [ ] Dán địa chỉ chi tiết cũ vào thanh địa chỉ. Trang báo "Không tìm thấy chuyến đi. Có thể chuyến đi đã bị xoá."

**Kết quả:** Chưa chạy

### MT-UI-19 · Đổi trạng thái trên trang chi tiết

- [ ] Ở trang chi tiết, nhãn trạng thái cạnh tên chuyến đi là một ô chọn, đang ghi "Nháp".
- [ ] Chọn "Đã lên kế hoạch". Nhãn đổi màu xanh, không cần bấm "Lưu". Nhấn F5, nhãn vẫn là "Đã lên kế hoạch".
- [ ] Về danh sách. Thẻ của chuyến đi có nhãn "Đã lên kế hoạch". Lọc "Đã lên kế hoạch" thấy chuyến đi này, lọc "Nháp" thì không.
- [ ] Quay lại trang chi tiết, lần lượt chọn "Đang diễn ra", "Đã hoàn thành", "Đã lưu trữ", rồi "Nháp". Lần nào nhãn cũng đổi đúng.
- [ ] Mở "Sửa", sửa tên rồi lưu. Trạng thái vẫn giữ nguyên.
- [ ] Tắt backend, chọn một trạng thái khác. Cạnh ô chọn hiện chữ đỏ "Không kết nối được máy chủ, vui lòng thử lại", ô chọn quay về trạng thái cũ. Bật lại backend.

**Kết quả:** Chưa chạy

### MT-UI-20 · Thêm hoạt động

Dùng một chuyến đi có tiền tệ `VND`, Ngày 1 chưa có hoạt động nào.

- [ ] Cuối khung Ngày 1 có nút nhỏ "+ Thêm hoạt động". Bấm vào, hộp "Thêm hoạt động" mở ra. Loại mặc định là "Khác", tiền tệ mặc định là `VND` (tiền tệ của chuyến đi).
- [ ] Nhập tên `Ăn trưa lẩu gà lá é`, loại "Ăn uống", giờ `11:30` đến `13:00`, chi phí `350000`, link `https://example.com/dat-ban`, ghi chú `Đặt bàn trước`. Bấm "Thêm".
- [ ] Hộp đóng. Ngày 1 có hoạt động mới với đầy đủ thông tin, chi phí ghi "350.000 ₫". Số bên cạnh "Ngày 1" ở danh sách bên trái là 1.
- [ ] Thêm hoạt động thứ hai chỉ với tên `Dạo hồ Xuân Hương`. Hoạt động này nằm **sau** hoạt động đầu, cột giờ ghi "Chưa đặt giờ", nhãn "Khác", không có chi phí.
- [ ] Mỗi hoạt động có hai nút nhỏ màu xám "✎ Sửa" và "Xoá". Rê chuột lên "Xoá" thì nút chuyển màu đỏ.

**Kết quả:** Chưa chạy

### MT-UI-21 · Báo lỗi khi nhập sai

Mở "+ Thêm hoạt động" ở một ngày bất kỳ.

- [ ] Để trống tên, bấm "Thêm". Ô tên báo "Tên hoạt động không được để trống".
- [ ] Nhập tên, chỉ nhập giờ kết thúc `09:00`. Ô giờ bắt đầu báo "Cần nhập giờ bắt đầu khi đã có giờ kết thúc".
- [ ] Nhập giờ `10:00` đến `09:00`. Ô giờ kết thúc báo "Giờ kết thúc phải sau giờ bắt đầu". Đổi thành `10:00` đến `10:00`: vẫn báo lỗi đó.
- [ ] Sửa giờ cho đúng, nhập chi phí `-5`. Ô chi phí báo "Chi phí không được âm".
- [ ] Xoá chi phí, nhập link `abc`. Ô link báo "Đường dẫn đặt chỗ phải bắt đầu bằng http:// hoặc https://".
- [ ] Bấm "Huỷ". Không có hoạt động nào được thêm.

**Kết quả:** Chưa chạy

### MT-UI-22 · Trùng giờ

Ngày 1 đang có "Ăn trưa lẩu gà lá é" từ 11:30 đến 13:00 (`MT-UI-20`).

- [ ] Thêm `Cà phê` từ `12:00` đến `12:30`. Hộp "Trùng giờ với hoạt động khác" hiện dòng `Trùng giờ với hoạt động "Ăn trưa lẩu gà lá é" (11:30 - 13:00)` và câu "Bạn vẫn muốn lưu hoạt động này?".
- [ ] Bấm "Huỷ". Hộp hỏi đóng, form thêm vẫn mở với dữ liệu vừa nhập. Chưa có hoạt động nào được thêm.
- [ ] Bấm "Thêm" rồi "Vẫn lưu". "Cà phê" được thêm vào Ngày 1.
- [ ] Thêm `Nghỉ trưa` từ `13:00` đến `14:00`. Không có câu hỏi nào (chạm đầu nhau không tính là trùng).
- [ ] Sửa "Cà phê", chỉ đổi tên thành `Cà phê trứng`, bấm "Lưu". Không có câu hỏi nào, vì giờ không đổi.
- [ ] Sửa "Cà phê trứng", đổi giờ thành `12:15` đến `12:45`. Hộp hỏi trùng giờ hiện lại.

**Kết quả:** Chưa chạy

### MT-UI-23 · Sửa hoạt động

- [ ] Bấm "✎ Sửa" ở "Ăn trưa lẩu gà lá é". Hộp "Sửa hoạt động" điền sẵn mọi thông tin: giờ `11:30` và `13:00`, chi phí `350000`, loại "Ăn uống".
- [ ] Không sửa gì, bấm "Lưu". Hộp đóng.
- [ ] Mở lại, đổi ghi chú thành `Gọi trước 30 phút`, xoá hết link đặt chỗ. Bấm "Lưu". Hoạt động hiện ghi chú mới, không còn "Link đặt chỗ".
- [ ] Mở lại, xoá hết ghi chú, bấm "Lưu". Ghi chú biến mất.
- [ ] Mở lại, xoá giờ kết thúc, bấm "Lưu". Ô giờ kết thúc báo "Chưa hỗ trợ xoá thông tin này, hãy nhập giá trị mới". (Xoá giờ bắt đầu thì ô đó báo "Cần nhập giờ bắt đầu khi đã có giờ kết thúc".)
- [ ] Nhập lại giờ, xoá chi phí, bấm "Lưu". Ô chi phí báo cùng câu đó. Bấm "Huỷ".
- [ ] Mở lại, đổi chi phí thành `400000` và tiền tệ `USD`. Bấm "Lưu". Chi phí hiện theo USD.

**Kết quả:** Chưa chạy

### MT-UI-24 · Xoá hoạt động

- [ ] Bấm "Xoá" ở "Nghỉ trưa". Hộp "Xoá hoạt động?" hiện tên hoạt động.
- [ ] Bấm "Huỷ". Hoạt động vẫn còn.
- [ ] Bấm "Xoá" rồi "Xoá hoạt động". Hoạt động biến mất, số bên cạnh "Ngày 1" ở danh sách bên trái giảm 1.
- [ ] Nhấn F5. Hoạt động không xuất hiện lại.

**Kết quả:** Chưa chạy

### MT-UI-25 · Kéo thả trong cùng một ngày

Dùng một ngày có ba hoạt động không trùng giờ, tạm gọi A, B, C theo thứ tự hiện tại (thêm lần lượt nên số thứ tự là 1000, 2000, 3000).

- [ ] Bên trái mỗi hoạt động có tay nắm "⠿". Rê chuột lên, con trỏ đổi thành bàn tay.
- [ ] Bấm vào tay nắm rồi thả ra, không kéo. Không có gì xảy ra. Nút "✎ Sửa", "Xoá" và "Link đặt chỗ" vẫn bấm được như cũ.
- [ ] Kéo C lên trên A rồi thả. Trong lúc kéo, các hoạt động khác dịch chỗ nhường chỗ. Thả xong, thứ tự là C, A, B ngay lập tức.
- [ ] Nhấn F5. Thứ tự vẫn là C, A, B.
- [ ] Chạy câu lệnh SQL bên dưới. C có số thứ tự 500 (nửa của 1000), A và B giữ 1000 và 2000.
- [ ] Kéo C xuống giữa A và B. Số thứ tự của C là 1500.

```sql
SELECT a.title, a.order_index, d.day_index
FROM activities a JOIN trip_days d ON d.id = a.trip_day_id
WHERE d.trip_id = <mã chuyến đi>
ORDER BY d.day_index, a.order_index;
```

**Kết quả:** Chưa chạy

### MT-UI-26 · Kéo sang ngày khác

- [ ] Kéo một hoạt động **không có giờ** từ Ngày 1 sang giữa hai hoạt động của Ngày 2. Khi rê qua Ngày 2, hoạt động hiện sẵn ở vị trí sẽ thả. Thả xong, hoạt động nằm ở Ngày 2 đúng chỗ đó.
- [ ] Số bên cạnh "Ngày 1" ở danh sách bên trái giảm 1, "Ngày 2" tăng 1.
- [ ] Ngày 3 chưa có hoạt động nào ghi "Chưa có hoạt động nào. Có thể kéo hoạt động từ ngày khác vào đây." Kéo một hoạt động vào vùng đó: vùng sáng viền xanh. Thả xong, Ngày 3 có hoạt động này.
- [ ] Nhấn F5. Mọi hoạt động vẫn ở ngày mới. Câu lệnh SQL ở `MT-UI-25` cho thấy `day_index` đã đổi.

**Kết quả:** Chưa chạy

### MT-UI-27 · Trùng giờ khi chuyển ngày

Ngày 2 có `Tham quan` từ 09:00 đến 11:00. Ngày 1 có `Cà phê sáng` từ 10:00 đến 10:30.

- [ ] Kéo "Cà phê sáng" sang Ngày 2. Hoạt động hiện ở Ngày 2, rồi hộp "Trùng giờ ở ngày mới" hiện dòng trùng giờ với "Tham quan" (09:00 - 11:00).
- [ ] Trong lúc hộp đang mở, tay nắm kéo của các hoạt động bị làm mờ, không kéo được.
- [ ] Bấm "Huỷ". "Cà phê sáng" về lại Ngày 1 đúng chỗ cũ. Nhấn F5: vẫn ở Ngày 1.
- [ ] Kéo lại lần nữa, bấm "Vẫn chuyển". "Cà phê sáng" nằm ở Ngày 2. Nhấn F5: vẫn ở Ngày 2.
- [ ] Đổi thứ tự hai hoạt động đang trùng giờ **trong cùng Ngày 2**. Không có câu hỏi nào (giờ không đổi nên không kiểm).

**Kết quả:** Chưa chạy

### MT-UI-28 · Đánh lại số thứ tự khi khoảng cách quá nhỏ

Dùng một ngày có ba hoạt động với số thứ tự 1000, 2000, 3000.

- [ ] Kéo hoạt động cuối lên đầu, lặp lại 7 lần (mỗi lần là hoạt động đang nằm cuối). Sau mỗi lần, thứ tự trên màn hình đúng như vừa kéo.
- [ ] Sau lần thứ 7, chạy câu lệnh SQL ở `MT-UI-25`. Số thứ tự của ngày đó là 1000, 2000, 3000 theo đúng thứ tự trên màn hình (hệ thống đã tự đánh lại vì khoảng cách từ 0 tới hoạt động đầu nhỏ hơn 10).
- [ ] Nhấn F5. Thứ tự không đổi.

**Kết quả:** Chưa chạy

### MT-UI-29 · Mất kết nối khi đang kéo thả

- [ ] Tắt backend. Kéo một hoạt động sang vị trí khác.
- [ ] Hoạt động nhảy tới chỗ mới rồi quay về chỗ cũ. Trên danh sách ngày hiện khung đỏ "Không sắp xếp được: Không kết nối được máy chủ, vui lòng thử lại. Các hoạt động đã về chỗ cũ."
- [ ] Bật lại backend, kéo lại. Khung đỏ biến mất, hoạt động được sắp xếp bình thường.

**Kết quả:** Chưa chạy

### MT-UI-30 · Kéo thả bằng bàn phím

- [ ] Nhấn Tab cho tới khi tay nắm "⠿" của một hoạt động được chọn (có viền).
- [ ] Nhấn Space để nhấc, nhấn mũi tên xuống một lần, nhấn Space để thả. Hoạt động đổi chỗ với hoạt động bên dưới.
- [ ] Nhấc bằng Space rồi nhấn Esc. Hoạt động về chỗ cũ, không có gì được lưu.

**Kết quả:** Chưa chạy

### MT-UI-31 · Giới hạn độ dài ghi chú và mô tả

Chuẩn bị một đoạn văn dài hơn 1000 ký tự để dán (ví dụ chép một đoạn tin tức).

- [ ] Mở "✎ Sửa" của một ngày. Nhãn ghi "Ghi chú (tối đa 255 ký tự)". Dán đoạn văn dài: ô chỉ nhận 255 ký tự đầu. Bấm "Lưu": lưu thành công.
- [ ] Mở "+ Thêm hoạt động". Nhãn ghi "Ghi chú (không bắt buộc, tối đa 255 ký tự)". Dán đoạn văn dài: ô chỉ nhận 255 ký tự đầu.
- [ ] Mở "Sửa" chuyến đi. Nhãn ghi "Mô tả (không bắt buộc, tối đa 1000 ký tự)". Dán đoạn văn dài: ô chỉ nhận 1000 ký tự đầu. Wizard "Tạo chuyến đi" cũng vậy.
- [ ] Trên Swagger, gửi `PATCH /api/v1/trips/{tripId}/days/{dayId}` với ghi chú dài 256 ký tự (chỉ dùng chữ không dấu, xem CLAUDE.md mục 2). Trả 400 "Ghi chú của ngày không được vượt quá 255 ký tự".

**Kết quả:** Chưa chạy

### MT-UI-32 · Thu gọn ghi chú dài

- [ ] Sửa một ngày, nhập ghi chú ngắn một dòng `Mang áo mưa`. Khung ngày hiện đủ ghi chú, **không** có nút "Đọc thêm".
- [ ] Sửa lại ghi chú thành 5 dòng (nhấn Enter giữa các dòng). Khung ngày chỉ hiện 2 dòng đầu, cuối dòng thứ hai có "…", bên dưới có nút "Đọc thêm".
- [ ] Bấm "Đọc thêm". Hiện đủ 5 dòng, đúng chỗ xuống dòng đã gõ, nút đổi thành "Thu gọn". Bấm "Thu gọn": về lại 2 dòng.
- [ ] Nhập ghi chú là một dòng rất dài không xuống dòng (khoảng 250 ký tự). Trên màn hình rộng, nếu vừa 2 dòng thì không có nút. Thu hẹp cửa sổ cho tới khi chữ cần hơn 2 dòng: nút "Đọc thêm" tự xuất hiện.
- [ ] Làm tương tự với ghi chú của một hoạt động (sửa hoạt động) và mô tả của chuyến đi (nút "Sửa" ở đầu trang). Cả ba nơi đều thu gọn giống nhau.

**Kết quả:** Chưa chạy

### MT-UI-33 · Tên chuyến đi dài

- [ ] Sửa tên chuyến đi thành một câu dài 160 ký tự (giới hạn tối đa). Ở đầu trang chi tiết, tên chỉ hiện 2 dòng, cuối dòng thứ hai có "…".
- [ ] Rê chuột lên tên. Hiện khung gợi ý với tên đầy đủ.
- [ ] Trên màn hình rộng: ô trạng thái, nút "Sửa" và "Xoá" nằm cùng một hàng ở góc phải, ngang dòng đầu của tên, không bị đẩy xuống dưới.
- [ ] Thu hẹp cửa sổ xuống cỡ điện thoại: tên chiếm hết chiều ngang, cụm trạng thái + nút nằm ở dòng riêng ngay dưới tên, căn phải.
- [ ] Sửa tên thành một chuỗi liền 160 chữ `a` không có khoảng trắng. Tên được bẻ dòng, trang không có thanh cuộn ngang, thẻ ở trang danh sách cũng không bị tràn.
- [ ] Đặt lại tên ngắn. Cụm trạng thái + nút vẫn ở góc phải.

**Kết quả:** Chưa chạy

---

## Lỗi đã phát hiện

| Mã lỗi | Test case | Ngày | Hiện tượng | Nguyên nhân | Cách sửa | Trạng thái |
|---|---|---|---|---|---|---|
| BUG-UI-001 | MT-UI-09, MT-UI-11 | 2026-09-30 | Trên màn hình rộng, dòng tiêu đề của mỗi ngày bị bẻ mỗi chữ một dòng ("Ngày / 1 / · / Thứ / bảy, / 03/10/2026"), nút sửa ngày nằm lơ lửng giữa khung. Chủ dự án phát hiện khi xem trang chi tiết, có ảnh chụp màn hình | Mọi nút của hệ thống mặc định rộng hết khung chứa. Chỗ cần nút vừa chữ thì ghi thêm một lớp "rộng vừa nội dung" để ghi đè, nhưng khi một phần tử có hai lớp cùng quy định độ rộng, công cụ tạo giao diện (Tailwind) không đảm bảo lớp nào thắng. Ở đây lớp "rộng hết khung" thắng. Nút sửa ngày lại được đặt là không được co, nên chiếm gần hết bề ngang và ép phần chữ bên trái co về hẹp nhất | Độ rộng của nút thành một tuỳ chọn riêng (`fullWidth`), không ghi đè bằng lớp nữa. Sửa mọi chỗ đang ghi đè (16 nút trong 7 file) | Đã sửa trong commit Mốc 4, chờ chạy lại MT-UI-09 và MT-UI-11 |

BUG-UI-001 không bị hai lệnh kiểm tra mã nguồn bắt được: mã nguồn đúng cú pháp và đúng kiểu dữ liệu, chỉ có kết quả hiển thị sai. Đây là loại lỗi mà chỉ người nhìn giao diện thật mới thấy, và là lý do mỗi mốc giao diện đều cần kiểm tra thủ công.

Bài học: không ghi đè một lớp giao diện bằng một lớp khác cùng thuộc tính. Thuộc tính nào cần thay đổi theo từng chỗ dùng thì đưa thành tuỳ chọn của component.

# 06 · Giao diện lịch trình

> Cập nhật: 2026-10-01 · Task 2.5 đã merge (`eb1ad5e`) · Task 2.6 (làm lại giao diện theo `UI_GUIDE.md`) đã merge (`8f06d72`) · kiểm tra thủ công chưa chạy · Task 2.7: BUG-UI-003 đã sửa và được chủ dự án xác nhận; BUG-UI-004 đến BUG-UI-008 đã sửa chờ kiểm lại · [Về trang chính](README.md)

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

Task 2.6 (`feat/T2.6-ui-guide`): áp dụng hệ thống giao diện trong `UI_GUIDE.md` (bảng màu, font, bo góc, ray thời gian). Phần lớn là đổi giao diện, hành vi giữ nguyên, nên các bài ở trên vẫn dùng được; bài nào có nhãn hoặc vị trí nút đổi thì được sửa ngay trong bài. Hai commit thêm dữ liệu mới từ backend theo góp ý khi xem mockup: số hoạt động trên thẻ (commit 7) và số chuyến đi theo trạng thái (commit 8); test tự động của chúng ở [03-trip.md](03-trip.md) phần J, K.

| Commit | Nội dung | Kiểm tra thủ công | Commit |
|---|---|---|---|
| 1 | Token màu, font Be Vietnam Pro, nút, hộp thoại, thông báo, thanh trên và chân trang mới | MT-UI-34 | `c3c8e4e` |
| 2 | Quy ước form: dấu `*` cho ô bắt buộc, gợi ý dưới ô, lỗi hiện khi rời ô, vòng focus | MT-UI-35 | `6923e05` |
| 3 | Trang danh sách theo mockup: chip trạng thái, ô sắp xếp, thẻ ảnh 16:9, khung xương, trạng thái rỗng | MT-UI-01 đến MT-UI-04 (đã sửa theo giao diện mới), MT-UI-36 | `042b8e9` |
| 4 | Trang chi tiết kiểu bảng giờ tàu: ray thời gian, menu "⋮", cột ngày tự theo vị trí cuộn, thông báo (toast), nút ghi rõ việc ("Lưu thay đổi", "Thêm hoạt động") | MT-UI-09 đến MT-UI-33 (đã sửa theo giao diện mới), MT-UI-37 | `332ad85` |
| 5 | Điện thoại: dải chip ngày dính trên cùng, nút ↑ / ↓ thay cho kéo thả trên màn hình cảm ứng | MT-UI-38 | `521d566` |
| 6 | Trang đăng nhập / đăng ký / đặt lại mật khẩu theo mockup (nút mắt, "Quên mật khẩu?" cạnh nhãn), thanh bước của wizard dạng tuyến | MT-UI-06 (đã sửa), MT-UI-39 | `efa9e73` |
| 7 | Thẻ chuyến đi ghi tổng số hoạt động ("5 ngày · 12 hoạt động"). Có thay đổi backend, test tự động ở [03-trip.md](03-trip.md) phần J | MT-UI-01, MT-UI-10 (đã sửa) | `9501e8e` |
| 8 | Chip trạng thái có số chuyến đi và chấm xanh cho "Đang diễn ra" như mockup. Có thay đổi backend, test tự động ở [03-trip.md](03-trip.md) phần K | MT-UI-03 (đã sửa) | `a838d5b` |
| 9 | Ô tìm kiếm chuyển lên thanh điều hướng; ở trang khác nhấn Enter thì mở danh sách với kết quả tìm | MT-UI-02 (viết lại), MT-UI-36 (đã sửa) | `7811124` |
| 10 | Một ngày một trang (`/trips/:id/days/:dayIndex`), nút chính "+ Thêm hoạt động" ở tiêu đề ngày, nút ngày trước / sau; nút ↑ / ↓ chỉ trong ngày | MT-UI-09, MT-UI-20, MT-UI-37, MT-UI-38 (đã sửa); MT-UI-26, MT-UI-27 chờ commit 11 | `3bd7fcf` |
| 11 | Chuyển hoạt động sang ngày khác: thả lên tên ngày ở cột trái, hoặc menu "⋮" → "Chuyển sang ngày…"; thông báo có link mở ngày mới | MT-UI-26, MT-UI-27 (viết lại) | `c74a21f` |
| 12 | Hoạt động tự vào đúng chỗ theo giờ bắt đầu khi thêm hoặc đổi giờ. Chỉ đổi backend, test tự động ở [05-activity.md](05-activity.md) phần N | MT-UI-40 | `db74c66` |
| 13 | Ngày dài: khối mô tả ngày dính ở trên, ngang hàng cột ngày bên trái (màn hình rộng); nút "Đầu ngày" trong khối mô tả và nút tròn nhỏ "Lên đầu trang" ở góc dưới phải khi cuộn sâu | MT-UI-41 | `b73fc79` |

Task 2.7 (`fix/T2.7-review-fixes`): sửa các lỗi tìm ra khi rà soát code Phase 1–2. Mỗi mốc một lỗi, một commit.

| Mốc | Nội dung | Kiểm tra thủ công | Commit |
|---|---|---|---|
| 3 | Mở hộp thoại thì con trỏ nằm ở ô đầu tiên của nội dung, không nằm ở nút "×" (`BUG-UI-003`) | MT-UI-42 | `2cc814d` |
| 4 | Esc và nút "×" không đóng hộp sửa / thêm trong lúc đang lưu (`BUG-UI-004`) | MT-UI-43 | `009e696` |
| 5 | Tải lại ngầm bị lỗi thì trang chi tiết vẫn giữ nguyên, có khung báo và nút "Thử lại" (`BUG-UI-005`) | MT-UI-44 | `752f383` |
| 6 | Yêu cầu bị máy chủ từ chối (404, 403...) không còn bị tự gửi lại 3 lần: thông báo hiện ngay (`BUG-UI-006`) | MT-UI-12 (đã thêm yêu cầu về thời gian) | `ae2af3b` |
| 7 | Ô tìm kiếm giữ dấu cách đang gõ khi ngừng tay giữa hai từ (`BUG-UI-007`) | MT-UI-02 (đã thêm một bước) | `e4f4520` |
| 8 | Thả một hoạt động lên chính ngày đang xem ở cột trái thì không có gì thay đổi (`BUG-UI-008`) | MT-UI-26, bước 3 | `dcfcfbe` |
| 9 | Phiên bị rớt thì dữ liệu đã tải bị xoá, người đăng nhập sau không thấy dữ liệu của người trước (`BUG-AUTH-007`, ghi ở [02-auth.md](02-auth.md)) | MT-AUTH-08 | `025799a` |

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

- [ ] Đăng nhập. Trang chuyển tới `/trips`, tiêu đề "Chuyến đi của bạn" kèm dòng phụ, thanh trên cùng có tên người dùng và nút "Đăng xuất".
- [ ] Ba chuyến đi hiện thành ba thẻ, chuyến tạo sau cùng đứng đầu.
- [ ] Thẻ "Đà Lạt mùa hoa": nhãn "Nháp" ở góc trên bên phải ảnh bìa, "Đà Lạt" có icon ghim ở góc dưới bên trái ảnh; dưới ảnh là tên, ngày "01/10/2026 – 03/10/2026" có icon lịch, và dòng cuối "3 ngày · 0 hoạt động".
- [ ] Thẻ "Họp lớp" không có nhãn điểm đến trên ảnh, ngày chỉ ghi "20/12/2026", dòng cuối "1 ngày · 0 hoạt động".
- [ ] Thu hẹp cửa sổ trình duyệt: lưới đổi từ 3 cột xuống 2 cột rồi 1 cột, không bị tràn ngang.
- [ ] Bấm "Đăng xuất". Quay về trang đăng nhập.
- [ ] Đăng nhập bằng một tài khoản khác chưa có chuyến đi nào. Trang có hình bản đồ gấp, câu "Chưa có chuyến đi nào. Tạo chuyến đầu tiên để bắt đầu lên lịch trình." và nút "Tạo chuyến đi"; không thấy chuyến đi của tài khoản đầu.

**Kết quả:** Chưa chạy

### MT-UI-02 · Tìm chuyến đi

> Trạng thái: Chưa chạy sau khi sửa · kiểm lại lỗi BUG-UI-007

Ô tìm kiếm nằm giữa thanh điều hướng màu tối (từ Task 2.6, commit 9; trước đó nằm trong hàng lọc của trang danh sách).

- [ ] Ở trang danh sách, gõ `hội an` vào ô tìm kiếm trên thanh điều hướng rồi dừng tay. Khoảng nửa giây sau chỉ còn thẻ "Hội An cuối tuần". Thanh địa chỉ có `?q=hội+an` (hoặc dạng mã hoá của nó).
- [ ] Nhấn F5. Ô tìm kiếm vẫn ghi `hội an`, danh sách vẫn chỉ có một thẻ.
- [ ] Gõ `đà` (tìm theo tên chuyến đi và điểm đến, không phân biệt hoa thường). Còn thẻ "Đà Lạt mùa hoa".
- [ ] Gõ tiếp **một dấu cách**, ngừng tay 1 giây, rồi gõ `lạt`. Ô tìm kiếm ghi `đà lạt` (có dấu cách), vẫn còn thẻ "Đà Lạt mùa hoa". Trước khi sửa, dấu cách biến mất trong lúc ngừng tay, ô thành `đàlạt` và không tìm ra gì.
- [ ] Gõ `không có chuyến này`. Trang ghi "Không tìm thấy chuyến đi nào phù hợp. Thử từ khoá khác hoặc bỏ bớt bộ lọc." kèm nút "Xoá bộ lọc". Bấm nút đó: ô tìm kiếm trên thanh điều hướng cũng trống theo.
- [ ] Chọn chip "Nháp" rồi gõ từ khoá: chip "Nháp" vẫn được giữ, chỉ lọc thêm theo từ khoá.
- [ ] Xoá hết chữ trong ô tìm kiếm. Hiện lại cả ba thẻ, thanh địa chỉ không còn `?q=`.
- [ ] Mở trang chi tiết một chuyến đi: ô tìm kiếm trống. Gõ `hội an` rồi nhấn Enter: trang chuyển về danh sách với kết quả tìm. Bấm Back của trình duyệt: quay lại trang chi tiết vừa xem.
- [ ] Gõ từng chữ ở trang danh sách không tạo thêm bước trong lịch sử trình duyệt: bấm Back là rời trang danh sách, không lùi qua từng từ khoá đã gõ.
- [ ] Thu hẹp cửa sổ cỡ điện thoại: ô tìm kiếm xuống thành một hàng riêng dưới logo, rộng hết chiều ngang, vẫn trong thanh màu tối.

**Kết quả:** Chưa chạy

### MT-UI-03 · Lọc theo trạng thái

Chuyến đi mới tạo luôn ở trạng thái "Nháp". Đổi trạng thái trên giao diện có ở mốc sau.

- [ ] Trạng thái giờ là hàng chip "Tất cả", "Nháp", "Đã lên kế hoạch"... Bấm chip "Nháp": chip được tô đậm, hiện cả ba thẻ, thanh địa chỉ có `?status=DRAFT`.
- [ ] Bấm chip "Đã lên kế hoạch". Trang báo không tìm thấy chuyến đi nào phù hợp. Bấm "Xoá bộ lọc": về chip "Tất cả".
- [ ] Bấm chip "Tất cả". Hiện lại cả ba thẻ.
- [ ] Sửa thanh địa chỉ thành `http://localhost:5173/trips?status=ABC` rồi Enter. Trang không báo lỗi, hiện cả ba thẻ, chip "Tất cả" được tô đậm.
- [ ] Mỗi chip có số chuyến đi trong một ô tròn nhỏ bên phải: "Tất cả 3", "Nháp 3", các chip còn lại 0. Chip đang chọn nền đen, ô số trong suốt.
- [ ] Chip "Đang diễn ra" có một chấm xanh ngọc đứng yên (không nhấp nháy).
- [ ] Đổi một chuyến sang "Đã lên kế hoạch" ở trang chi tiết rồi quay lại danh sách: "Nháp" còn 2, "Đã lên kế hoạch" thành 1, "Tất cả" vẫn 3.
- [ ] Gõ `hội an` vào ô tìm kiếm: các số đếm lại theo kết quả tìm ("Tất cả 1"...). Bấm chip nào thì số thẻ hiện ra đúng bằng số trên chip đó.
- [ ] Tạo thêm một chuyến đi hoặc xoá một chuyến: khi quay về danh sách, số trên chip đã đổi theo, không cần F5.

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

- [ ] Ở trang danh sách, bấm "Tạo chuyến đi". Trang chuyển tới `/trips/new`, thanh bước là ba "ga" nối bằng đường ray: ga 1 "Thông tin" viền xanh ngọc (đang ở bước này), ga 2 và 3 màu xám.
- [ ] Bước 1: nhập tên `Sapa săn mây`, mô tả `Đi cùng gia đình`, bỏ trống ảnh bìa. Bấm "Tiếp".
- [ ] Bước 2: nhập điểm đến `Sa Pa`. Bấm "Tiếp".
- [ ] Bước 3: chọn ngày `05/12/2026` đến `07/12/2026`. Dưới ô ngày hiện "Chuyến đi dài 3 ngày. Tối đa 60 ngày."
- [ ] Nhập ngân sách `5000000`, giữ tiền tệ `VND`. Bấm "Tạo chuyến đi".
- [ ] Trang chuyển sang trang chi tiết của chuyến đi vừa tạo (từ Mốc 3; ở Mốc 2 trang quay về danh sách). Bấm link "Chuyến đi của bạn" (có mũi tên trái) ở đầu trang. Thẻ "Sapa săn mây" đứng đầu, ảnh bìa có nhãn "Sa Pa" và "Nháp", ngày "05/12/2026 – 07/12/2026", dòng cuối "3 ngày · 0 hoạt động".
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
- [ ] Xoá ngân sách, bấm "Tạo chuyến đi". Chuyến đi 60 ngày được tạo, danh sách ghi "60 ngày · 0 hoạt động".

**Kết quả:** Chưa chạy

### MT-UI-08 · Chỉ nhập phần bắt buộc, huỷ giữa chừng

- [ ] Mở "Tạo chuyến đi", nhập tên `Đi chơi một ngày` rồi nhấn phím Enter. Wizard sang bước 2 (Enter có tác dụng như "Tiếp").
- [ ] Bỏ trống điểm đến, nhấn Enter. Sang bước 3.
- [ ] Chọn cùng một ngày cho cả hai ô, bấm "Tạo chuyến đi", rồi quay về danh sách. Thẻ mới không có nhãn điểm đến trên ảnh, dòng cuối ghi "1 ngày · 0 hoạt động".
- [ ] Mở "Tạo chuyến đi" lần nữa, nhập tên rồi bấm "Huỷ". Quay về danh sách, không có chuyến đi mới nào được tạo.

**Kết quả:** Chưa chạy

### MT-UI-09 · Xem chi tiết chuyến đi

> Trạng thái: Chưa chạy lại sau khi sửa · từng lỗi BUG-UI-001

Dùng chuyến đi "Sapa săn mây" (05/12/2026 đến 07/12/2026) tạo ở `MT-UI-06`.

- [ ] Ở danh sách, bấm vào thẻ "Sapa săn mây". Trang chuyển tới `/trips/{mã}/days/1` (một ngày một trang, từ Task 2.6).
- [ ] Đầu trang có tên chuyến đi, ô trạng thái "Nháp", một hàng thông tin gồm "Sa Pa", "05/12/2026 – 07/12/2026", "3 ngày", "Ngân sách 5.000.000 ₫" (mỗi mục có icon riêng), và mô tả "Đi cùng gia đình".
- [ ] Chỉ có Ngày 1: "Ngày 1 · Thứ bảy, 05/12/2026", dưới đó chữ nghiêng màu xám "Chưa có tiêu đề", bên phải là nút "Sửa" và nút xanh "+ Thêm hoạt động". Phần ray là khung nét đứt "Ngày này còn trống. Thêm địa điểm bạn muốn ghé." kèm nút "Thêm hoạt động".
- [ ] Dưới cùng có nút "Ngày 2 ›". Bấm vào: trang hiện "Ngày 2 · Chủ nhật, 06/12/2026", thanh địa chỉ đổi thành `/days/2`, có thêm nút "‹ Ngày 1". Ngày 3 là "Thứ hai, 07/12/2026" và chỉ có nút "‹ Ngày 2".
- [ ] Màn hình rộng: bên trái có danh sách "Ngày 1 · 05/12", "Ngày 2 · 06/12", "Ngày 3 · 07/12", mỗi dòng có số 0 và dòng nhỏ nghiêng "Chưa có tiêu đề". Bấm "Ngày 3": trang chuyển sang Ngày 3, mục "Ngày 3" ở danh sách được tô nền xanh nhạt.
- [ ] Thu hẹp cửa sổ: danh sách bên trái đổi thành dải chip ngày trên cùng, ngày đang xem chiếm hết chiều ngang.
- [ ] Đang ở Ngày 3, nhấn F5: vẫn ở Ngày 3. Bấm Back: về Ngày 2.
- [ ] Sửa thanh địa chỉ thành `/days/9` (không tồn tại) hoặc bỏ phần `/days/…`: trang tự chuyển về Ngày 1.
- [ ] Mở chuyến đi 60 ngày tạo ở `MT-UI-07`. Danh sách bên trái có đủ 60 ngày và cuộn được; bấm "Ngày 60" mở đúng ngày cuối.

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
- [ ] Về trang danh sách: thẻ "Sapa săn mây" ghi "3 ngày · 2 hoạt động". Thêm một hoạt động vào Ngày 2 rồi quay lại danh sách: thẻ ghi "3 ngày · 3 hoạt động" (số gộp mọi ngày).

**Kết quả:** Chưa chạy

### MT-UI-11 · Sửa tiêu đề và ghi chú của ngày

- [ ] Góc phải mỗi ngày có nút nhỏ "Sửa" có icon bút chì, màu xám, không viền. Rê chuột lên thì nút có nền xám nhạt và chữ đậm hơn.
- [ ] Ở Ngày 2, bấm "Sửa". Khung ngày đổi thành form có ô "Tiêu đề của ngày" và "Ghi chú".
- [ ] Nhập tiêu đề `Chinh phục Fansipan`, ghi chú hai dòng `Đi cáp treo` và `Mang áo ấm`. Bấm "Lưu thay đổi".
- [ ] Khung ngày hiện tiêu đề và ghi chú đúng hai dòng. Danh sách ngày bên trái có tiêu đề thay cho "Chưa có tiêu đề" dưới "Ngày 2".
- [ ] Bấm "Sửa" lần nữa, sửa tiêu đề rồi bấm "Huỷ". Tiêu đề cũ vẫn giữ nguyên.
- [ ] Bấm "Sửa", xoá hết tiêu đề, bấm "Lưu thay đổi". Tiêu đề đổi lại thành "Chưa có tiêu đề" ở cả khung ngày lẫn danh sách bên trái, ghi chú vẫn còn.
- [ ] Trên Swagger, gọi `GET /api/v1/trips/{tripId}/days`. Ngày 2 có `title` là `null`, `note` còn nguyên.
- [ ] Dán một tiêu đề dài 161 ký tự, bấm "Lưu thay đổi". Ô tiêu đề báo "Tiêu đề của ngày không được vượt quá 160 ký tự".

**Kết quả:** Chưa chạy

### MT-UI-12 · Không mở được chuyến đi

> Trạng thái: Chưa chạy sau khi sửa · kiểm lại lỗi BUG-UI-006

- [ ] Sửa thanh địa chỉ thành `http://localhost:5173/trips/999999`. Trang báo "Không tìm thấy chuyến đi. Có thể chuyến đi đã bị xoá." kèm link về danh sách. Thông báo hiện **gần như ngay** (dưới 1 giây); trước khi sửa phải nhìn khung chờ khoảng 7 giây. Trong DevTools, thẻ Network chỉ có **một** request tới `/trips/999999`, không phải bốn.
- [ ] Sửa thành `http://localhost:5173/trips/abc`. Trang báo "Không tìm thấy chuyến đi.".
- [ ] Đăng nhập bằng tài khoản thứ hai, mở địa chỉ chi tiết của "Sapa săn mây" (của tài khoản đầu). Trang báo "Bạn không có quyền xem chuyến đi này."
- [ ] Quay lại tài khoản đầu, xoá một chuyến đi bằng Swagger (`DELETE /api/v1/trips/{id}`), rồi mở địa chỉ chi tiết của nó. Trang báo "Không tìm thấy chuyến đi. Có thể chuyến đi đã bị xoá."

**Kết quả:** Chưa chạy

### MT-UI-13 · Sửa thông tin chuyến đi

Dùng chuyến đi "Sapa săn mây" ở `MT-UI-09`, Ngày 1 có hai hoạt động từ `MT-UI-10`.

- [ ] Ở trang chi tiết, bấm "Sửa". Hộp "Sửa chuyến đi" mở ra, các ô đã điền sẵn thông tin hiện tại, kể cả ngân sách `5000000` và tiền tệ `VND`.
- [ ] Bấm "Huỷ". Hộp đóng lại, không có gì thay đổi. Mở lại rồi nhấn phím Esc, hộp cũng đóng.
- [ ] Mở lại, không sửa gì, bấm "Lưu thay đổi". Hộp đóng, không có gì thay đổi.
- [ ] Mở lại, sửa tên thành `Sapa săn mây mùa đông`, điểm đến thành `Lào Cai`, ngân sách thành `6500000`. Bấm "Lưu thay đổi".
- [ ] Hộp đóng, góc dưới bên phải hiện thông báo "Đã lưu thay đổi". Đầu trang hiện tên mới, "Lào Cai" và "Ngân sách 6.500.000 ₫". Các ngày và hoạt động giữ nguyên.
- [ ] Bấm link "Chuyến đi của bạn" (có mũi tên trái) ở đầu trang. Thẻ hiện tên và điểm đến mới.
- [ ] Mở lại "Sửa", xoá hết tên, bấm "Lưu thay đổi". Ô tên báo "Tên chuyến đi không được để trống", hộp vẫn mở.

**Kết quả:** Chưa chạy

### MT-UI-14 · Không xoá trắng được thông tin đã nhập

Hệ thống chưa hỗ trợ bỏ trống một thông tin tuỳ chọn đã nhập (design.md 10.2). Giao diện phải báo rõ thay vì âm thầm giữ giá trị cũ.

- [ ] Mở "Sửa", xoá hết ô điểm đến, bấm "Lưu thay đổi". Ô điểm đến báo "Chưa hỗ trợ xoá thông tin này, hãy nhập giá trị mới", hộp vẫn mở.
- [ ] Nhập lại điểm đến, xoá hết ô ngân sách, bấm "Lưu thay đổi". Ô ngân sách báo cùng câu đó.
- [ ] Bấm "Huỷ". Thông tin chuyến đi không đổi.

**Kết quả:** Chưa chạy

### MT-UI-15 · Dời cả chuyến đi

Chuyến đi đang từ 05/12/2026 đến 07/12/2026.

- [ ] Mở "Sửa", đổi ngày thành `12/12/2026` đến `14/12/2026`. Dưới ô ngày hiện khung xanh "Dời cả chuyến đi: mọi ngày và hoạt động dời theo, không mất gì."
- [ ] Bấm "Lưu thay đổi". Không có câu hỏi nào, hộp đóng.
- [ ] Ngày 1 giờ là "Thứ bảy, 12/12/2026" và vẫn có hai hoạt động. Tiêu đề và ghi chú của Ngày 2 (nếu có từ `MT-UI-11`) vẫn nằm ở Ngày 2.

**Kết quả:** Chưa chạy

### MT-UI-16 · Kéo dài và rút ngắn ở cuối chuyến

- [ ] Mở "Sửa", đổi ngày kết thúc thành `15/12/2026`. Dưới ô ngày hiện khung đỏ bắt đầu bằng "Đổi độ dài".
- [ ] Bấm "Lưu thay đổi". Chuyến đi có 4 ngày, Ngày 4 trống.
- [ ] Mở "Sửa", đổi ngày kết thúc về `14/12/2026`, bấm "Lưu thay đổi". Ngày 4 trống nên không có câu hỏi nào, chuyến đi còn 3 ngày.

**Kết quả:** Chưa chạy

### MT-UI-17 · Bỏ ngày đầu đang có hoạt động

Chuyến đi đang từ 12/12/2026 đến 14/12/2026, Ngày 1 có hai hoạt động.

- [ ] Mở "Sửa", đổi ngày bắt đầu thành `13/12/2026`, giữ ngày kết thúc `14/12/2026`. Khung đỏ bắt đầu bằng "Vừa dời vừa đổi độ dài" và hướng dẫn làm hai lần.
- [ ] Bấm "Lưu thay đổi". Hộp hỏi "Vừa dời vừa đổi độ dài chuyến đi?". Bấm "Huỷ": hộp hỏi đóng, form sửa vẫn mở với ngày vừa nhập.
- [ ] Bấm "Lưu thay đổi" rồi "Vẫn lưu". Hộp hỏi thứ hai hiện "Xoá hoạt động khi đổi ngày?" với câu "2 hoạt động trong 1 ngày sẽ bị xoá nếu đổi ngày." và nút nền đỏ "Vẫn đổi ngày".
- [ ] Bấm "Huỷ". Đóng hộp sửa, nhấn F5: chuyến đi vẫn từ 12/12 đến 14/12, hai hoạt động còn nguyên.
- [ ] Làm lại từ đầu, lần này bấm "Vẫn đổi ngày". Chuyến đi còn 2 ngày: Ngày 1 là 13/12/2026, Ngày 2 là 14/12/2026. Hai hoạt động của ngày 12/12 đã bị xoá.

**Kết quả:** Chưa chạy

### MT-UI-18 · Xoá chuyến đi

- [ ] Ở trang chi tiết một chuyến đi, bấm nút "Xoá" (chữ và viền đỏ, nền trắng). Hộp "Xoá chuyến đi?" hiện tên chuyến đi, nút xác nhận "Xoá chuyến đi" có nền đỏ.
- [ ] Bấm "Huỷ". Hộp đóng, chuyến đi còn nguyên.
- [ ] Bấm "Xoá" rồi "Xoá chuyến đi". Trang chuyển về danh sách, thẻ của chuyến đi đó biến mất.
- [ ] Bấm nút Back của trình duyệt. Không quay lại trang chi tiết của chuyến đi đã xoá.
- [ ] Dán địa chỉ chi tiết cũ vào thanh địa chỉ. Trang báo "Không tìm thấy chuyến đi. Có thể chuyến đi đã bị xoá."

**Kết quả:** Chưa chạy

### MT-UI-19 · Đổi trạng thái trên trang chi tiết

- [ ] Ở trang chi tiết, nhãn trạng thái cạnh tên chuyến đi là một ô chọn, đang ghi "Nháp".
- [ ] Chọn "Đã lên kế hoạch". Nhãn đổi màu xanh, không cần bấm "Lưu thay đổi". Nhấn F5, nhãn vẫn là "Đã lên kế hoạch".
- [ ] Về danh sách. Thẻ của chuyến đi có nhãn "Đã lên kế hoạch". Lọc "Đã lên kế hoạch" thấy chuyến đi này, lọc "Nháp" thì không.
- [ ] Quay lại trang chi tiết, lần lượt chọn "Đang diễn ra", "Đã hoàn thành", "Đã lưu trữ", rồi "Nháp". Lần nào nhãn cũng đổi đúng.
- [ ] Mở "Sửa", sửa tên rồi lưu. Trạng thái vẫn giữ nguyên.
- [ ] Tắt backend, chọn một trạng thái khác. Góc dưới bên phải hiện thông báo đỏ "Không đổi được trạng thái: Không kết nối được máy chủ, vui lòng thử lại", ô chọn quay về trạng thái cũ. Bật lại backend.

**Kết quả:** Chưa chạy

### MT-UI-20 · Thêm hoạt động

Dùng một chuyến đi có tiền tệ `VND`, Ngày 1 chưa có hoạt động nào.

- [ ] Ngày 1 đang trống: khung nét đứt "Ngày này còn trống…" có nút "Thêm hoạt động". Bấm vào, hộp "Thêm hoạt động" mở ra. Loại mặc định là "Khác", tiền tệ mặc định là `VND` (tiền tệ của chuyến đi).
- [ ] Nhập tên `Ăn trưa lẩu gà lá é`, loại "Ăn uống", giờ `11:30` đến `13:00`, chi phí `350000`, link `https://example.com/dat-ban`, ghi chú `Đặt bàn trước`. Bấm "Thêm hoạt động".
- [ ] Hộp đóng. Ngày 1 có hoạt động mới với đầy đủ thông tin, chi phí ghi "350.000 ₫". Số bên cạnh "Ngày 1" ở danh sách bên trái là 1.
- [ ] Thêm hoạt động thứ hai chỉ với tên `Dạo hồ Xuân Hương`. Hoạt động này nằm **sau** hoạt động đầu, cột giờ ghi "Chưa đặt giờ", nhãn "Khác", không có chi phí.
- [ ] Rê chuột lên một hoạt động: mép trái hiện tay nắm kéo, mép phải hiện nút "⋮". Bấm "⋮": menu có "Sửa", "Chuyển sang ngày…" và "Xoá" (chữ đỏ). Nút thêm hoạt động là nút xanh "+ Thêm hoạt động" ở góc phải tiêu đề ngày (không có nút ở cuối ray).

**Kết quả:** Chưa chạy

### MT-UI-21 · Báo lỗi khi nhập sai

Mở "Thêm hoạt động" ở một ngày bất kỳ.

- [ ] Để trống tên, bấm "Thêm hoạt động". Ô tên báo "Tên hoạt động không được để trống".
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
- [ ] Bấm "Thêm hoạt động" rồi "Vẫn lưu". "Cà phê" được thêm vào Ngày 1.
- [ ] Thêm `Nghỉ trưa` từ `13:00` đến `14:00`. Không có câu hỏi nào (chạm đầu nhau không tính là trùng).
- [ ] Sửa "Cà phê", chỉ đổi tên thành `Cà phê trứng`, bấm "Lưu thay đổi". Không có câu hỏi nào, vì giờ không đổi.
- [ ] Sửa "Cà phê trứng", đổi giờ thành `12:15` đến `12:45`. Hộp hỏi trùng giờ hiện lại.

**Kết quả:** Chưa chạy

### MT-UI-23 · Sửa hoạt động

- [ ] Mở menu "⋮" của "Ăn trưa lẩu gà lá é", chọn "Sửa". Hộp "Sửa hoạt động" điền sẵn mọi thông tin: giờ `11:30` và `13:00`, chi phí `350000`, loại "Ăn uống".
- [ ] Không sửa gì, bấm "Lưu thay đổi". Hộp đóng.
- [ ] Mở lại, đổi ghi chú thành `Gọi trước 30 phút`, xoá hết link đặt chỗ. Bấm "Lưu thay đổi". Hoạt động hiện ghi chú mới, không còn "Link đặt chỗ".
- [ ] Mở lại, xoá hết ghi chú, bấm "Lưu thay đổi". Ghi chú biến mất.
- [ ] Mở lại, xoá giờ kết thúc, bấm "Lưu thay đổi". Ô giờ kết thúc báo "Chưa hỗ trợ xoá thông tin này, hãy nhập giá trị mới". (Xoá giờ bắt đầu thì ô đó báo "Cần nhập giờ bắt đầu khi đã có giờ kết thúc".)
- [ ] Nhập lại giờ, xoá chi phí, bấm "Lưu thay đổi". Ô chi phí báo cùng câu đó. Bấm "Huỷ".
- [ ] Mở lại, đổi chi phí thành `400000` và tiền tệ `USD`. Bấm "Lưu thay đổi". Chi phí hiện theo USD.

**Kết quả:** Chưa chạy

### MT-UI-24 · Xoá hoạt động

- [ ] Mở menu "⋮" của "Nghỉ trưa", chọn "Xoá". Hộp "Xoá hoạt động?" hiện tên hoạt động.
- [ ] Bấm "Huỷ". Hoạt động vẫn còn.
- [ ] Mở lại menu, chọn "Xoá" rồi "Xoá hoạt động". Hoạt động biến mất, hiện thông báo "Đã xoá hoạt động", số bên cạnh "Ngày 1" ở danh sách bên trái giảm 1.
- [ ] Nhấn F5. Hoạt động không xuất hiện lại.

**Kết quả:** Chưa chạy

### MT-UI-25 · Kéo thả trong cùng một ngày

Dùng một ngày có ba hoạt động không trùng giờ, tạm gọi A, B, C theo thứ tự hiện tại (thêm lần lượt nên số thứ tự là 1000, 2000, 3000).

- [ ] Rê chuột lên một hoạt động: tay nắm kéo (icon sáu chấm dọc) hiện ở mép trái thẻ. Rê lên tay nắm, con trỏ đổi thành bàn tay.
- [ ] Bấm vào tay nắm rồi thả ra, không kéo. Không có gì xảy ra. Nút "⋮" và "Link đặt chỗ" vẫn bấm được như cũ.
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

### MT-UI-26 · Chuyển hoạt động sang ngày khác

> Trạng thái: Chưa chạy sau khi sửa · kiểm lại lỗi BUG-UI-008 (bước 3)

Từ Task 2.6 mỗi trang chỉ hiện một ngày, nên có hai cách chuyển (commit 11). Dùng chuyến đi 3 ngày: Ngày 1 có ít nhất 3 hoạt động, Ngày 3 trống.

- [ ] Màn hình rộng, đang ở Ngày 1: kéo một hoạt động **không có giờ** bằng tay nắm, rê lên mục "Ngày 3" ở cột trái. Mục đó có viền xanh ngọc. Thả ra: hoạt động biến khỏi Ngày 1; góc dưới bên phải hiện thông báo `Đã chuyển "…" sang Ngày 3` kèm link "Mở Ngày 3".
- [ ] Số bên cạnh "Ngày 1" ở cột trái giảm 1, "Ngày 3" tăng 1. Bấm link "Mở Ngày 3" trong thông báo: trang chuyển sang Ngày 3, hoạt động nằm ở **cuối** ngày.
- [ ] Kéo hoạt động **đầu tiên** của ngày nhưng thả lên chính "Ngày 1" (ngày đang xem) ở cột trái: không có gì thay đổi, hoạt động vẫn ở đầu ngày. Trước khi sửa, nó nhảy xuống cuối ngày.
- [ ] Kéo một hoạt động qua lại trong ngày, đi ngang cột trái mà không dừng trên mục nào: thẻ không bị hút sang ngày khác, thả vào giữa ngày thì chỉ đổi thứ tự như bình thường.
- [ ] Mở menu "⋮" của một hoạt động ở Ngày 1: có mục "Chuyển sang ngày…". Chọn: hộp "Chuyển sang ngày khác" có ô "Ngày" liệt kê Ngày 2 và Ngày 3 (không có Ngày 1). Chọn "Ngày 2", bấm "Chuyển sang ngày": hoạt động chuyển xuống cuối Ngày 2, hiện thông báo có link "Mở Ngày 2".
- [ ] Bấm "Huỷ" trong hộp chuyển ngày: không có gì thay đổi.
- [ ] Giả lập điện thoại (`MT-UI-38`): menu "⋮" vẫn có "Chuyển sang ngày…" và dùng được như trên.
- [ ] Mở một chuyến đi chỉ có 1 ngày: menu "⋮" không có mục "Chuyển sang ngày…".
- [ ] Nhấn F5 ở ngày mới: hoạt động vẫn ở đó. Câu lệnh SQL ở `MT-UI-25` cho thấy `day_index` đã đổi.

**Kết quả:** Chưa chạy

### MT-UI-27 · Trùng giờ khi chuyển ngày

Ngày 2 có `Tham quan` từ 09:00 đến 11:00. Ngày 1 có `Cà phê sáng` từ 10:00 đến 10:30.

- [ ] Ở Ngày 1, kéo "Cà phê sáng" thả lên "Ngày 2" ở cột trái. Hoạt động biến khỏi Ngày 1, rồi hộp "Trùng giờ ở ngày mới" hiện dòng trùng giờ với "Tham quan" (09:00 - 11:00).
- [ ] Trong lúc hộp đang mở, tay nắm kéo của các hoạt động bị làm mờ, không kéo được.
- [ ] Bấm "Huỷ". "Cà phê sáng" hiện lại ở Ngày 1 đúng chỗ cũ, không có thông báo "Đã chuyển". Nhấn F5: vẫn ở Ngày 1.
- [ ] Làm lại bằng menu "⋮" → "Chuyển sang ngày…" → "Ngày 2": hộp trùng giờ hiện giống hệt. Bấm "Vẫn chuyển": hiện thông báo `Đã chuyển "Cà phê sáng" sang Ngày 2`. Mở Ngày 2: "Cà phê sáng" nằm cuối ngày, có dải vàng nhạt báo trùng giờ.
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
- [ ] Hoạt động nhảy tới chỗ mới rồi quay về chỗ cũ. Góc dưới bên phải hiện thông báo đỏ "Không sắp xếp được: Không kết nối được máy chủ, vui lòng thử lại. Các hoạt động đã về chỗ cũ."
- [ ] Bật lại backend, kéo lại. Hoạt động được sắp xếp bình thường.

**Kết quả:** Chưa chạy

### MT-UI-30 · Kéo thả bằng bàn phím

- [ ] Nhấn Tab cho tới khi tay nắm kéo của một hoạt động được chọn: tay nắm hiện ra và có vòng sáng xanh ngọc.
- [ ] Nhấn Space để nhấc, nhấn mũi tên xuống một lần, nhấn Space để thả. Hoạt động đổi chỗ với hoạt động bên dưới.
- [ ] Nhấc bằng Space rồi nhấn Esc. Hoạt động về chỗ cũ, không có gì được lưu.

**Kết quả:** Chưa chạy

### MT-UI-31 · Giới hạn độ dài ghi chú và mô tả

Chuẩn bị một đoạn văn dài hơn 1000 ký tự để dán (ví dụ chép một đoạn tin tức).

- [ ] Mở "Sửa" của một ngày. Dưới ô "Ghi chú" có dòng gợi ý "Tối đa 255 ký tự" (từ Task 2.6; trước đó giới hạn ghi trong nhãn). Dán đoạn văn dài: ô chỉ nhận 255 ký tự đầu. Bấm "Lưu thay đổi": lưu thành công.
- [ ] Mở "Thêm hoạt động". Dưới ô "Ghi chú" có dòng gợi ý "Tối đa 255 ký tự". Dán đoạn văn dài: ô chỉ nhận 255 ký tự đầu.
- [ ] Mở "Sửa" chuyến đi. Dưới ô "Mô tả" có dòng gợi ý "Tối đa 1000 ký tự". Dán đoạn văn dài: ô chỉ nhận 1000 ký tự đầu. Wizard "Tạo chuyến đi" cũng vậy.
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

### MT-UI-34 · Nền tảng giao diện mới (Task 2.6)

Nhấn `Ctrl+F5` sau khi chuyển sang nhánh `feat/T2.6-ui-guide` để trình duyệt tải lại CSS và font.

- [ ] Chữ ở mọi trang dùng font Be Vietnam Pro: dấu của "Ế", "Ợ", "Ỡ" không chạm chữ hoa (thử gõ tên chuyến đi "ĐỢT NGHỈ Ở HỘI AN").
- [ ] Nền trang xám lạnh nhạt (không phải trắng tinh, không phải màu kem). Thanh trên cùng cao, màu xanh đen, có ô vuông xanh ngọc chứa icon ghim và chữ "Smart Trip Planner" màu trắng. Bấm vào đó về danh sách chuyến đi.
- [ ] Góc phải thanh trên có tên người dùng và nút "Đăng xuất" có icon. Rê chuột lên nút: nền sáng nhẹ.
- [ ] Cuối trang có dải màu xanh đen ghi "© 2026 Smart Trip Planner".
- [ ] Nút chính (ví dụ "Tạo chuyến đi", "Lưu thay đổi") màu xanh ngọc, bo góc nhỏ. Bấm "Lưu thay đổi" trong một form: nút hiện vòng quay nhỏ bên trái và **vẫn giữ chữ "Lưu thay đổi"**, không đổi thành "Đang xử lý...".
- [ ] Nhấn Tab qua các nút: nút đang được chọn có vòng sáng xanh ngọc nhìn thấy rõ.
- [ ] Mở một hộp thoại (sửa chuyến đi): góc bo lớn hơn nút, nền phía sau tối mờ, góc trên bên phải có nút "×". Bấm "×" hoặc Esc thì hộp đóng.
- [ ] Thu hẹp cửa sổ xuống cỡ điện thoại rồi mở lại hộp thoại: hộp nằm sát mép dưới màn hình, rộng hết chiều ngang, cao tối đa 90% màn hình.
- [ ] Thẻ chuyến đi, khung ngày và thẻ hoạt động chỉ có viền mảnh, không có bóng đổ. Thẻ chuyến đi chưa có ảnh bìa hiện khối màu phẳng, không có dải màu chuyển.
- [ ] Trang đăng nhập có logo ghim ở trên khung đăng nhập; khung có viền mảnh, không có bóng.
- [ ] Khung báo lỗi (ví dụ tắt backend rồi đổi bộ lọc) có icon tròn chấm than bên trái, chữ và viền màu đỏ gạch.

**Kết quả:** Chưa chạy

### MT-UI-35 · Quy ước form mới (Task 2.6)

- [ ] Trang đăng nhập: nhãn "Email" và "Mật khẩu" có dấu `*` màu đỏ phía sau. Không còn chữ "(không bắt buộc)" ở bất kỳ form nào.
- [ ] Bấm vào ô Email rồi bấm ra ngoài khi ô còn trống. Lỗi hiện **ngay lúc rời ô**, có icon tròn chấm than, chữ đỏ nhỏ dưới ô, viền ô chuyển đỏ.
- [ ] Gõ lại email hợp lệ: lỗi tự mất ngay khi gõ, không cần bấm "Đăng nhập".
- [ ] Mở một form rồi gõ vào một ô, **chưa rời ô**: chưa có lỗi nào hiện ra trong lúc đang gõ lần đầu.
- [ ] Ô đang được chọn có viền xanh ngọc và vòng sáng nhạt quanh ô; rê chuột lên ô chưa chọn thì viền đậm hơn một chút.
- [ ] Wizard "Tạo chuyến đi": "Tên chuyến đi", "Ngày bắt đầu", "Ngày kết thúc" có `*`; "Mô tả", "Đường dẫn ảnh bìa", "Điểm đến", "Ngân sách" không có. Dưới "Mô tả" có gợi ý "Tối đa 1000 ký tự", dưới "Điểm đến" có gợi ý về bản đồ.
- [ ] Bấm "Tiếp" khi chưa nhập tên (chưa hề chạm vào ô): lỗi vẫn hiện, vì bấm "Tiếp" là kiểm tra cả bước.
- [ ] Form hoạt động: chỉ "Tên hoạt động" có `*`; dưới "Ghi chú" có gợi ý "Tối đa 255 ký tự". Khi ô ghi chú báo lỗi thì dòng lỗi thay chỗ dòng gợi ý.
- [ ] Mọi ô nhập một dòng và ô chọn cao bằng nhau (40px), bo góc nhỏ giống nút.

**Kết quả:** Chưa chạy

### MT-UI-36 · Trang danh sách mới (Task 2.6)

Cần ít nhất 3 chuyến đi có ngày đi khác nhau, một chuyến có ảnh bìa (sửa chuyến đi, nhập một đường dẫn ảnh `https://...jpg`).

- [ ] Nút "Tạo chuyến đi" có dấu `+`, nằm bên phải tiêu đề. Bấm chuột giữa (hoặc Ctrl + bấm): mở được trong tab mới, vì đây là link thật.
- [ ] Hàng lọc nằm giữa hai đường kẻ mảnh: hàng chip trạng thái và ô "Sắp xếp" ở cuối hàng. Ô tìm kiếm (icon kính lúp) nằm trên thanh điều hướng, không nằm trong hàng lọc.
- [ ] Chọn "Ngày đi sớm nhất": chuyến có ngày đi sớm nhất đứng đầu, thanh địa chỉ có `sort=start-asc`. Nhấn F5: vẫn giữ cách sắp xếp. Chọn "Ngày đi muộn nhất", "Tên A → Z": thứ tự đổi đúng. Chọn lại "Tạo gần đây nhất": `sort` biến mất khỏi thanh địa chỉ.
- [ ] Thẻ có ảnh bìa: ảnh tỉ lệ 16:9, không có lớp màu phủ lên ảnh, nhãn trạng thái nền trắng đọc rõ. Thẻ không có ảnh: khối xám nhạt có icon ghim.
- [ ] Rê chuột lên thẻ: viền và tên chuyển sang màu xanh ngọc. Tên dài hơn một dòng bị cắt bằng "…", rê chuột lên tên thấy tên đầy đủ.
- [ ] Nhấn F5 (hoặc bật chế độ mạng chậm trong DevTools): trong lúc tải hiện 3 khung xương cùng hình dạng thẻ, có dải sáng chạy ngang, không hiện chữ "Đang tải...".
- [ ] Thu hẹp cửa sổ: lưới đổi 3 → 2 → 1 cột; hàng chip cuộn ngang được, không làm tràn trang.
- [ ] Trong hệ điều hành bật "giảm chuyển động" (Windows: Settings → Accessibility → Visual effects → Animation effects: Off), tải lại trang: khung xương đứng yên, không có dải sáng chạy.

**Kết quả:** Chưa chạy

### MT-UI-37 · Trang chi tiết kiểu bảng giờ tàu (Task 2.6)

Dùng một chuyến đi 3 ngày: Ngày 1 có 4 hoạt động đủ các loại, trong đó hai hoạt động trùng giờ (đã lưu bằng "Vẫn lưu") và một hoạt động không có giờ; Ngày 2 có 1 hoạt động; Ngày 3 trống.

- [ ] Mỗi ngày là một ray dọc: bên trái là giờ bắt đầu (chữ số thẳng cột, căn phải), giữa là một đường kẻ mảnh có chấm tròn cho từng hoạt động, bên phải là thẻ. Hoạt động không có giờ hiện "—" trên ray.
- [ ] Chấm trên ray và viền trái (3px) của thẻ cùng màu theo loại: Tham quan xanh dương, Ăn uống cam, Di chuyển xám, Lưu trú tím, Mua sắm hồng, Khác xanh lá. Thẻ luôn có icon + chữ tên loại, nền thẻ vẫn trắng (không tô màu loại).
- [ ] Hai hoạt động trùng giờ có nền vàng rất nhạt và icon tam giác cảnh báo cạnh giờ. Sửa một trong hai sang giờ không trùng: dải vàng biến mất ở cả hai.
- [ ] Tay nắm và nút "⋮" chỉ hiện khi rê chuột lên thẻ. Nhấn Tab tới thẻ: hai nút đó cũng hiện ra.
- [ ] Mở "⋮" bằng bàn phím (Tab tới rồi nhấn Enter): dùng mũi tên lên/xuống chọn "Sửa" / "Chuyển sang ngày…" / "Xoá", Enter để chọn, Esc để đóng menu.
- [ ] Kéo một hoạt động: thẻ theo con trỏ hơi nghiêng, nổi bóng; ở chỗ sẽ thả hiện một đường ngang màu xanh ngọc.
- [ ] Màn hình rộng: cột trái "Ngày 1 / Ngày 2 / Ngày 3" có số hoạt động; ngày đang xem có nền xanh nhạt và vạch xanh ngọc ở mép trái. Bấm "Ngày 3": trang chuyển sang Ngày 3 và mục đó được tô.
- [ ] Ngày có nhiều hoạt động: cuộn xuống cuối, bấm "Ngày 2 ›": trang tự cuộn lên để thấy tiêu đề "Ngày 2 · …".
- [ ] Sửa ngày, sửa chuyến đi, thêm / sửa / xoá hoạt động, đổi trạng thái: sau mỗi thao tác, góc dưới bên phải hiện thông báo ngắn ("Đã lưu thay đổi", "Đã thêm hoạt động", "Đã xoá hoạt động", "Đã đổi trạng thái"), tự tắt sau khoảng 4 giây, có nút "×" để đóng sớm. Xoá chuyến đi: thông báo "Đã xoá chuyến đi" hiện ở trang danh sách.
- [ ] Thu hẹp cửa sổ cỡ điện thoại: thông báo hiện ở trên cùng màn hình, rộng gần hết chiều ngang.
- [ ] Nhấn F5 (hoặc mạng chậm): trong lúc tải hiện khung xương của đầu trang và 4 thẻ, không hiện chữ "Đang tải chuyến đi...".

**Kết quả:** Chưa chạy

### MT-UI-38 · Trang chi tiết trên điện thoại (Task 2.6)

Nút ↑ / ↓ chỉ hiện trên màn hình **cảm ứng**, không phụ thuộc độ rộng cửa sổ. Trên máy tính, giả lập bằng Chrome: F12 → bấm biểu tượng điện thoại (Toggle device toolbar, `Ctrl+Shift+M`) → chọn một máy như "iPhone 12 Pro" → nhấn F5. Dùng chuyến đi của `MT-UI-37`.

- [ ] Cột ngày bên trái biến mất; thay vào đó là một dải chip "Ngày 1 · 05/12", "Ngày 2 · 06/12"... dưới đầu trang, cuộn ngang được.
- [ ] Cuộn xuống: dải chip dính ở mép trên màn hình. Chip của ngày đang xem được tô đen. Bấm chip của một ngày ở cuối dải: trang chuyển sang ngày đó và dải chip tự cuộn ngang để chip tô đen luôn nhìn thấy.
- [ ] Tiêu đề ngày có nút "Sửa" và nút xanh "+ Thêm" (chữ rút gọn trên điện thoại).
- [ ] Mỗi hoạt động có hai nút mũi tên ↑ ↓ ở mép trái (không có tay nắm kéo), và nút "⋮" luôn hiện (không cần rê chuột).
- [ ] Bấm ↓ ở hoạt động đầu Ngày 1: hoạt động đổi chỗ với hoạt động bên dưới ngay lập tức. Nhấn F5: thứ tự mới vẫn giữ.
- [ ] Nút ↑ của hoạt động đầu ngày và nút ↓ của hoạt động cuối ngày bị mờ, không bấm được (nút ↑ / ↓ chỉ đổi thứ tự trong ngày, từ Task 2.6 commit 10). Chuyển sang ngày khác làm bằng menu "⋮" → "Chuyển sang ngày…" (`MT-UI-26`).
- [ ] Tắt giả lập điện thoại (máy tính có chuột) nhưng thu hẹp cửa sổ cho hẹp như điện thoại: dải chip vẫn hiện, nhưng mỗi hoạt động có tay nắm kéo thay vì nút mũi tên, và kéo thả vẫn dùng được.

**Kết quả:** Chưa chạy

### MT-UI-39 · Trang đăng nhập và wizard mới (Task 2.6)

Đăng xuất trước khi làm các bước về đăng nhập.

- [ ] Trang đăng nhập: trên khung có logo ghim trong ô vuông xanh ngọc, chữ "Smart Trip Planner" và dòng phụ "Kế hoạch hành trình theo dòng thời gian". Khung rộng khoảng 400px, viền mảnh, không bóng. Dưới khung là "Chưa có tài khoản? Đăng ký".
- [ ] "Quên mật khẩu?" nằm cùng hàng với nhãn "Mật khẩu", căn phải. Bấm vào: tới trang quên mật khẩu.
- [ ] Gõ mật khẩu: hiện dấu chấm. Bấm biểu tượng con mắt ở cuối ô: mật khẩu hiện thành chữ, biểu tượng đổi thành mắt gạch chéo. Bấm lại: ẩn đi.
- [ ] Nhấn Tab từ ô mật khẩu: nút con mắt nhận được focus (có vòng sáng). Nhấn Enter trên nút đó: mật khẩu hiện / ẩn, form không bị gửi.
- [ ] Nút "Đăng nhập" rộng hết khung, cao hơn các nút thường một chút (44px).
- [ ] Trang đăng ký: hai ô mật khẩu đều có nút con mắt, bấm riêng từng ô. Dưới ô "Mật khẩu" có dòng gợi ý quy tắc mật khẩu; khi ô báo lỗi thì dòng lỗi thay chỗ dòng gợi ý.
- [ ] Trang đặt lại mật khẩu (mở link trong mail ở MailHog): hai ô mật khẩu có nút con mắt và dòng gợi ý như trang đăng ký.
- [ ] Đăng nhập, mở "Tạo chuyến đi": tiêu đề 32px kèm dòng phụ "Ba bước: thông tin chung, điểm đến và ngày đi."
- [ ] Bấm "Tiếp": ga 1 thành vòng tròn xanh ngọc có dấu ✓, đoạn ray từ ga 1 sang ga 2 chuyển xanh, ga 2 "Điểm đến" viền xanh và chữ đậm. Bấm "Quay lại": ga 1 bỏ dấu ✓, đoạn ray trở lại xám.

**Kết quả:** Chưa chạy

### MT-UI-40 · Hoạt động tự vào đúng chỗ theo giờ (Task 2.6)

Dùng một ngày có "Ăn sáng" 08:00, "Dạo phố" không có giờ, "Ăn trưa" 12:00, theo đúng thứ tự đó.

- [ ] Thêm "Bảo tàng" bắt đầu 10:00. Sau khi lưu, thẻ hiện giữa "Dạo phố" và "Ăn trưa", không nằm cuối ngày.
- [ ] Thêm "Ăn sáng sớm" bắt đầu 06:00: lên đầu ngày.
- [ ] Thêm "Chợ đêm" bắt đầu 20:00: xuống cuối ngày.
- [ ] Thêm "Dạo đêm" không có giờ (xuống cuối, sau "Chợ đêm"), rồi thêm "Ăn khuya" bắt đầu 22:00: "Ăn khuya" xuống cuối ngày, sau "Dạo đêm".
- [ ] Thêm "Mua quà" không có giờ: xuống cuối ngày như trước.
- [ ] Sửa "Ăn trưa", đổi giờ bắt đầu thành 07:00: thẻ nhảy lên giữa "Ăn sáng sớm" và "Ăn sáng". "Dạo phố" vẫn đứng ngay sau "Ăn sáng".
- [ ] Kéo "Ăn sáng" (08:00) lên đầu ngày. Sửa tên của nó thành "Ăn phở", giữ giờ: thẻ vẫn ở đầu ngày, không bị xếp lại. Chỉ sửa giờ kết thúc: cũng không di chuyển.
- [ ] Nhấn F5: thứ tự y như trên màn hình.

**Kết quả:** Chưa chạy

### MT-UI-41 · Ngày có nhiều hoạt động (Task 2.6)

Dùng một ngày có khoảng 20 hoạt động, đủ để phải cuộn nhiều màn hình. Màn hình rộng (từ 1024px) trước.

- [ ] Mở ngày đó, chưa cuộn: trang trông như trước, khối mô tả ngày không bị dịch lên xuống, chỉ có thêm một đường kẻ mảnh dưới khối.
- [ ] Cuộn xuống: khối mô tả ngày (tên ngày, tiêu đề, ghi chú, "Sửa", "+ Thêm hoạt động") đứng yên ở trên, **mép trên ngang với cột ngày bên trái**. Các thẻ hoạt động **và chấm mốc giờ trên ray** trượt khuất dưới khối, không lộ ra phía trên khối (`BUG-UI-002`).
- [ ] Đang ở giữa danh sách, bấm "+ Thêm hoạt động" trong khối: mở hộp thêm hoạt động. Bấm "Sửa": form sửa ngày mở ngay tại chỗ.
- [ ] Cuộn quá một màn hình: trong khối mô tả ngày, cạnh "Sửa", hiện nút "↑ Đầu ngày"; ở góc dưới phải màn hình hiện một nút tròn nhỏ có mũi tên lên (rê chuột thấy chữ "Lên đầu trang"). Dưới danh sách ngày ở cột trái **không** có nút nào. Cuộn ngược lên gần đầu: cả hai nút biến mất.
- [ ] Bấm "Đầu ngày": trang cuộn mượt tới chỗ hoạt động đầu tiên nằm ngay dưới khối mô tả, giống lúc mới mở ngày. Tên chuyến đi vẫn khuất phía trên. Nhấn Tab: focus đi tiếp từ phần ngày (tới nút "Sửa"), không nhảy về đầu trang.
- [ ] Chuyển một hoạt động sang ngày khác để hiện thông báo ở góc dưới phải: thông báo nằm **dưới** nút tròn nhỏ, không che nút. Cuộn xuống tận cuối trang: nút nằm trên dải footer màu tối, không chạm footer.
- [ ] Bấm nút tròn nhỏ ở góc dưới phải: trang cuộn mượt về đầu, thấy tên chuyến đi. Nhấn Tab: focus đi tiếp từ tên chuyến đi (tới ô trạng thái), không nhảy về thanh trên cùng.
- [ ] Kéo một thẻ lên gần mép trên màn hình: trang tự cuộn lên, khối mô tả không chặn việc kéo. Thả lên tên một ngày ở cột trái vẫn chuyển được ngày (`MT-UI-26`).
- [ ] Mở menu "⋮" của thẻ ngay dưới khối mô tả: menu hiện đè lên trên, không bị khối che.
- [ ] Giả lập điện thoại (`MT-UI-38`): khối mô tả **không** dính, cuộn đi như bình thường. Cuộn sâu: nút tròn mũi tên hiện ở góc dưới phải; cuộn tới cuối trang thì nút vẫn nằm trên footer, không chạm; bấm thì về **đầu ngày**: tiêu đề ngày nằm ngay dưới hàng chip, không bị chip che. Không có thanh cuộn ngang.
- [ ] Bề rộng khoảng 800px (máy tính bảng): nút tròn hiện ở góc dưới **trái**, không chạm footer khi cuộn tới cuối. Chuyển một hoạt động sang ngày khác để hiện thông báo: thông báo ở góc dưới phải, không đè lên nút.
- [ ] Màn hình rộng, cuộn xuống cuối một ngày dài rồi bấm "Ngày sau ›": trang chuyển sang ngày mới và tự cuộn về đầu ngày mới. Còn khi đang ở gần đầu trang mà bấm một ngày ở cột trái: trang đứng yên.
- [ ] Ngày chỉ có 1–2 hoạt động (không cần cuộn quá một màn hình): không thấy nút "Đầu ngày" hay "Lên đầu trang".

**Kết quả:** Chưa chạy

### MT-UI-42 · Con trỏ khi mở hộp thoại (Task 2.7)

> Trạng thái: Chạy một phần sau khi sửa · kiểm lại lỗi BUG-UI-003

Dùng một chuyến đi có ít nhất 2 ngày và 1 hoạt động. Không dùng chuột sau khi hộp đã mở, chỉ dùng bàn phím.

- [ ] Bấm "+ Thêm hoạt động" rồi gõ ngay `Ăn sáng`: chữ vào ô "Tên hoạt động". Nhấn Esc để đóng.
- [ ] Mở menu "⋮" của một hoạt động, chọn "Sửa": con trỏ nằm trong ô "Tên hoạt động" đang có sẵn tên. Gõ thêm một chữ thì chữ đó vào ô tên. Nhấn Esc.
- [ ] Bấm "Sửa" của chuyến đi: con trỏ nằm trong ô "Tên chuyến đi". Nhấn Esc.
- [ ] Mở menu "⋮", chọn "Chuyển sang ngày…": con trỏ nằm ở ô "Ngày", nhấn mũi tên xuống thì ngày được chọn đổi. Nhấn Esc.
- [ ] Mở menu "⋮", chọn "Xoá": nút "Huỷ" đang có vòng focus. Nhấn Enter: hộp đóng, hoạt động **vẫn còn**.
- [ ] Bấm "Xoá" của chuyến đi: nút "Huỷ" đang có vòng focus. Nhấn Enter: hộp đóng, chuyến đi vẫn còn.
- [ ] Trong hộp "Thêm hoạt động", nhấn Tab nhiều lần: con trỏ đi hết các ô, tới nút "×", rồi quay lại ô đầu. Không lọt ra trang phía sau.

**Kết quả:** Chạy một phần · **Ngày:** 2026-10-01 · **Ghi chú:** chủ dự án thử sau khi sửa và báo "con trỏ tự vào ô đầu tiên" khi mở hộp. Chưa có kết quả cho các bước về hộp xác nhận (nút "Huỷ", phím Enter) và phím Tab, nên các ô trên chưa được đánh dấu

### MT-UI-43 · Không đóng được hộp thoại lúc đang lưu (Task 2.7)

> Trạng thái: Chưa chạy sau khi sửa · kiểm lại lỗi BUG-UI-004

Cần làm chậm mạng để kịp bấm: mở DevTools (F12), thẻ Network, đổi "No throttling" thành "Slow 4G". Dùng một ngày đã có hoạt động 09:00 đến 10:00.

- [ ] Bấm "+ Thêm hoạt động", nhập tên, giờ `09:30` đến `10:30` (trùng giờ), bấm "Thêm hoạt động" rồi **nhấn Esc ngay** khi nút đang quay. Hộp **không đóng**.
- [ ] Vài giây sau hộp hỏi "Trùng giờ với hoạt động khác" hiện ra. Trước khi sửa, hộp đã đóng ở bước trên và câu hỏi này không bao giờ hiện.
- [ ] Bấm "Huỷ" ở hộp hỏi. Bấm lại "Thêm hoạt động", lần này bấm nút "×" khi nút đang quay. Hộp không đóng.
- [ ] Khi hộp hỏi hiện lại, bấm "Huỷ", rồi nhấn Esc: lúc này không còn đang lưu nên hộp "Thêm hoạt động" đóng bình thường.
- [ ] Bấm "Sửa" của chuyến đi, rút ngắn để bỏ một ngày đang có hoạt động, bấm "Lưu thay đổi" rồi nhấn Esc ngay. Hộp không đóng, vài giây sau hộp hỏi "Xoá hoạt động khi đổi ngày?" hiện ra. Bấm "Huỷ".
- [ ] Đổi lại "No throttling".

**Kết quả:** Chưa chạy

### MT-UI-44 · Trang chi tiết khi máy chủ không trả lời (Task 2.7)

> Trạng thái: Chưa chạy sau khi sửa · kiểm lại lỗi BUG-UI-005

Cần hai cửa sổ: trình duyệt và cửa sổ đang chạy backend.

- [ ] Mở trang chi tiết một chuyến đi. Bấm "+ Thêm hoạt động", gõ tên `Đang gõ dở` nhưng **chưa lưu**.
- [ ] Tắt backend (Ctrl+C ở cửa sổ backend). Bấm sang một cửa sổ khác rồi bấm quay lại trình duyệt, chờ khoảng 10 giây.
- [ ] Trang chi tiết **vẫn còn nguyên**, hộp "Thêm hoạt động" vẫn mở và vẫn có chữ `Đang gõ dở`. Trước khi sửa, cả trang bị thay bằng một khung báo lỗi và chữ đang gõ mất.
- [ ] Nhấn Esc đóng hộp. Phía trên tên chuyến đi có khung đỏ "Không kết nối được máy chủ, vui lòng thử lại. Đang hiển thị dữ liệu đã tải trước đó." và nút "Thử lại".
- [ ] Bật lại backend, chờ nó khởi động xong, bấm "Thử lại". Nút quay một lúc rồi khung đỏ biến mất.
- [ ] Về trang danh sách. Tắt backend, bấm vào một chuyến đi **chưa mở lần nào** trong phiên này. Sau khoảng 10 giây trang chỉ có khung báo lỗi, nút "Thử lại" và link "Chuyến đi của bạn".
- [ ] Bật lại backend, bấm "Thử lại". Trang chi tiết hiện ra.
- [ ] Mở địa chỉ `/trips/999999`: vẫn là câu "Không tìm thấy chuyến đi. Có thể chuyến đi đã bị xoá." và **không có** nút "Thử lại" (thử lại cũng không tìm thấy).

**Kết quả:** Chưa chạy

---

## Lỗi đã phát hiện

| Mã lỗi | Test case | Ngày | Hiện tượng | Nguyên nhân | Cách sửa | Trạng thái |
|---|---|---|---|---|---|---|
| BUG-UI-001 | MT-UI-09, MT-UI-11 | 2026-09-30 | Trên màn hình rộng, dòng tiêu đề của mỗi ngày bị bẻ mỗi chữ một dòng ("Ngày / 1 / · / Thứ / bảy, / 03/10/2026"), nút sửa ngày nằm lơ lửng giữa khung. Chủ dự án phát hiện khi xem trang chi tiết, có ảnh chụp màn hình | Mọi nút của hệ thống mặc định rộng hết khung chứa. Chỗ cần nút vừa chữ thì ghi thêm một lớp "rộng vừa nội dung" để ghi đè, nhưng khi một phần tử có hai lớp cùng quy định độ rộng, công cụ tạo giao diện (Tailwind) không đảm bảo lớp nào thắng. Ở đây lớp "rộng hết khung" thắng. Nút sửa ngày lại được đặt là không được co, nên chiếm gần hết bề ngang và ép phần chữ bên trái co về hẹp nhất | Độ rộng của nút thành một tuỳ chọn riêng (`fullWidth`), không ghi đè bằng lớp nữa. Sửa mọi chỗ đang ghi đè (16 nút trong 7 file) | Đã sửa trong commit Mốc 4, chờ chạy lại MT-UI-09 và MT-UI-11 |
| BUG-UI-002 | MT-UI-41 | 2026-10-01 | Task 2.6 commit 13, trước khi commit: khi cuộn một ngày dài trên màn hình rộng, các thẻ hoạt động trượt khuất dưới khối mô tả ngày, nhưng chấm tròn đánh dấu mốc giờ trên ray không bị che mà vẽ đè lên khối. Chủ dự án phát hiện khi thử trên trình duyệt | Chấm mốc được đặt "nổi" một bậc (`z-10`) để nằm trên đường ray. Khối mô tả ngày cũng nổi đúng một bậc đó. Hai thứ ngang bậc thì cái nằm sau trong trang được vẽ sau, tức là chấm đè lên khối | Danh sách hoạt động thành một lớp riêng (`isolate`): bậc nổi của chấm chỉ có tác dụng bên trong danh sách, cả danh sách nằm dưới khối mô tả | Đã sửa trong commit 13 `b73fc79`, chờ chạy lại MT-UI-41 |
| BUG-UI-003 | MT-UI-42 | 2026-10-01 | Phát hiện khi rà soát code Phase 1–2. **Chủ dự án đã thử trên trình duyệt và xác nhận** ngày 2026-10-01: con trỏ nằm ở nút "×" ở hộp thêm hoạt động, hộp sửa hoạt động và hộp sửa chuyến đi. Mở hộp "Thêm hoạt động" rồi gõ ngay: không có chữ nào vào ô tên, vì con trỏ nằm ở nút "×" đóng hộp. Nhấn Space hoặc Enter lúc đó làm hộp đóng lại. Tương tự ở hộp sửa chuyến đi và hộp chuyển ngày | Ô đầu tiên được đánh dấu "tự nhận con trỏ", nhưng việc đó xảy ra lúc hộp thoại còn đang ẩn nên không có tác dụng. Khi hộp hiện ra, trình duyệt đặt con trỏ vào phần tử bấm được đầu tiên, là nút "×" | Sửa ở thành phần hộp thoại dùng chung, nên mọi hộp đều được sửa cùng lúc: ngay sau khi hộp hiện ra, con trỏ được chuyển tới phần tử đầu tiên của nội dung (ô nhập đầu tiên của form; nút "Huỷ" của hộp xác nhận, để nhấn Enter không bao giờ xoá nhầm). Bỏ dấu "tự nhận con trỏ" không có tác dụng ở ba form | Đã sửa, commit `2cc814d`. Chủ dự án thử lại ngày 2026-10-01: con trỏ vào ô đầu tiên |
| BUG-UI-004 | MT-UI-43 | 2026-10-01 | Rà soát code, chưa chạy trên trình duyệt. Bấm "Thêm hoạt động" rồi nhấn Esc (hoặc "×") trước khi máy chủ trả lời: hộp đóng. Nếu máy chủ từ chối vì trùng giờ, câu hỏi "Vẫn lưu?" không hiện ra, cũng không có thông báo nào: hoạt động không được lưu mà người dùng không biết. Tương tự ở hộp sửa chuyến đi (câu hỏi xoá ngày có hoạt động) | Nút "Huỷ" bị khoá trong lúc đang lưu, nhưng phím Esc và nút "×" thì không. Hộp xác nhận (`ConfirmDialog`) đã chặn đúng, hai hộp có form thì chưa | Vỏ hộp thoại hỏi "form bên trong có đang lưu không" và bỏ qua Esc cùng nút "×" trong lúc đó. Lưu xong (thành công hay bị từ chối) thì đóng được như thường | Đã sửa trong commit `009e696`, chờ chạy MT-UI-43 |
| BUG-UI-005 | MT-UI-44 | 2026-10-01 | Rà soát code, chưa chạy trên trình duyệt. Đang ở trang chi tiết chuyến đi, có thể đang gõ dở form hoạt động. Chuyển sang cửa sổ khác rồi quay lại, đúng lúc backend đang khởi động lại hoặc mạng chập chờn: cả trang bị thay bằng ô báo lỗi, form đang gõ mất, không có nút "Thử lại" | Trang tự tải lại dữ liệu khi người dùng quay lại cửa sổ. Lần tải lại lỗi thì trang hiện lỗi **thay cho** dữ liệu đang có, dù dữ liệu cũ vẫn còn trong bộ nhớ. Trang danh sách xử lý đúng (báo lỗi phía trên, giữ danh sách cũ) | Còn dữ liệu đã tải thì giữ nguyên trang, thêm khung báo và nút "Thử lại" phía trên tên chuyến đi. Chỉ thay cả trang bằng thông báo khi chưa tải được gì, hoặc khi máy chủ trả lời rõ là chuyến đi không còn (404) hay không có quyền (403) | Đã sửa trong commit `752f383`, chờ chạy MT-UI-44 |
| BUG-UI-006 | MT-UI-12 | 2026-10-01 | Rà soát code, chưa chạy trên trình duyệt. Mở địa chỉ của một chuyến đi không tồn tại hoặc không có quyền xem: khung chờ hiện khoảng 7 giây rồi mới tới câu "Không tìm thấy chuyến đi". `MT-UI-12` kiểm đúng câu thông báo nhưng chưa nêu thời gian chờ | Yêu cầu bị máy chủ **từ chối** (404, 403) được tự gửi lại 3 lần, cách nhau 1, 2 và 4 giây, như thể đó là lỗi mạng tạm thời | Quy định chung cho mọi lần tải dữ liệu: máy chủ đã trả lời từ chối (mã 4xx) thì không gửi lại. Mất mạng, quá thời gian chờ và lỗi máy chủ (5xx) vẫn được thử lại 3 lần như cũ | Đã sửa trong commit `ae2af3b`, chờ chạy MT-UI-12 |
| BUG-UI-007 | MT-UI-02 | 2026-10-01 | Rà soát code, chưa chạy trên trình duyệt. Ở ô tìm kiếm của trang danh sách, gõ `đà` kèm một dấu cách rồi ngừng tay khoảng 0,3 giây: dấu cách biến mất. Gõ tiếp `nẵng` thì ô thành `đànẵng` và không tìm ra gì. `MT-UI-02` chỉ gõ liền một mạch nên không gặp | Khi ngừng gõ, từ khoá được cắt khoảng trắng thừa rồi ghi lên thanh địa chỉ. Ô tìm kiếm thấy thanh địa chỉ đổi liền chép ngược giá trị đã cắt vào chính nó | Ô tìm kiếm chỉ chép từ thanh địa chỉ khi thanh địa chỉ nói **khác** với ô (nút Back, "Xoá bộ lọc", rời trang danh sách). Khi thanh địa chỉ đã khớp với chữ trong ô thì giữ nguyên chữ đang gõ | Đã sửa trong commit `e4f4520`, chờ chạy MT-UI-02 |
| BUG-UI-008 | MT-UI-26, bước 3 | 2026-10-01 | Rà soát code, chưa chạy trên trình duyệt. Kéo một hoạt động rồi thả lên chính ngày đang xem ở cột trái: hoạt động nhảy xuống **cuối ngày**. `MT-UI-26` bước 3 mong đợi "không có gì thay đổi"; bài này chưa được chạy nên lỗi chưa lộ | Đường thả lên tên ngày luôn chuyển hoạt động xuống cuối ngày đích mà không kiểm ngày đích có phải ngày hiện tại hay không. Đường menu "⋮" có kiểm này | Thả lên đúng ngày mà hoạt động đang ở thì coi như người dùng đổi ý: không làm gì, không gọi máy chủ | Đã sửa trong commit `dcfcfbe`, chờ chạy MT-UI-26 |

BUG-UI-003 đến BUG-UI-008 được tìm ra bằng cách đọc lại code ngày 2026-10-01, trước khi vào Phase 3, vì 41 bài kiểm tra thủ công của file này chưa được chạy. Ít nhất hai lỗi (006, 008) nằm đúng ở bước mà một bài có sẵn sẽ kiểm. Cột "Kết quả" của các bài `MT-UI` vẫn là "Chưa chạy": chỉ người thật chạy mới được ghi kết quả.

BUG-UI-002 cũng là loại lỗi chỉ thấy bằng mắt: mỗi phần (khối dính, chấm trên ray) đều đúng khi đứng riêng, lỗi chỉ xuất hiện khi hai phần chồng lên nhau lúc cuộn.

BUG-UI-001 không bị hai lệnh kiểm tra mã nguồn bắt được: mã nguồn đúng cú pháp và đúng kiểu dữ liệu, chỉ có kết quả hiển thị sai. Đây là loại lỗi mà chỉ người nhìn giao diện thật mới thấy, và là lý do mỗi mốc giao diện đều cần kiểm tra thủ công.

Bài học: không ghi đè một lớp giao diện bằng một lớp khác cùng thuộc tính. Thuộc tính nào cần thay đổi theo từng chỗ dùng thì đưa thành tuỳ chọn của component.

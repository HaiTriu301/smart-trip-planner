# 12 · Ô nhập kiểu mới

> Cập nhật: 2026-10-18 · **Task 4.9 code xong** (11 commit `0f59cfd` đến `26b61bc` trên nhánh `feat/T4.9-form-controls`; lint, build và 204 test tự động của giao diện xanh). Chủ dự án đã chạy cả 11 bài thủ công `MT-FORM-01` đến `11`, **đều đạt**, kèm kiểm lại `BUG-UI-012`, ngày 2026-10-18 · Sửa `BUG-UI-012` (form mời chỉ báo lỗi khi bấm "Mời"; lỗi ghi ở [11-sharing-ui.md](11-sharing-ui.md)) tại `26b61bc` · Mốc 10 (chọn nhanh hạn của liên kết chia sẻ, mốc cuối) tại `6f3ff96` · Mốc 9 (lịch chọn ngày đi – ngày về) tại `423207a` · Mốc 8 (ô giờ gõ nhanh, danh sách mỗi 15 phút) tại `92efda5` · Mốc 7 (ô vai trò cỡ nhỏ trong danh sách thành viên) tại `b2d2c69` · Mốc 6 (chọn ngày đích có ngày tháng) tại `5791acb` · Mốc 5 (ô chọn kiểu mới cho tiền tệ) tại `5fa90f5` · Mốc 4 (menu sắp xếp danh sách chuyến đi) tại `29b3f94` · Mốc 3 (menu đổi trạng thái chuyến đi) tại `40d2462` · Mốc 2 (nút gạt chọn vai trò ở form mời) tại `b7fd53d` · Mốc 1 (ô ghi chú tự giãn, bộ đếm ký tự) tại `0f59cfd` · [Về trang chính](README.md)

Các ô nhập của form được làm lại cho dễ dùng hơn (Task 4.9): ô ghi chú cao theo nội dung và báo khi sắp hết chỗ, ô chọn ít lựa chọn thành nút bấm thẳng, ô sổ xuống có dòng phụ, ô giờ gõ nhanh, lịch chọn ngày đi – ngày về. Dữ liệu gửi lên máy chủ không đổi, nên không có thay đổi backend.

File này được ghi dần theo từng mốc của Task 4.9:

| Mốc | Nội dung | Kiểm tra | Mã commit |
|---|---|---|---|
| 1 | Ô ghi chú (mô tả chuyến đi, ghi chú hoạt động, ghi chú ngày) cao theo nội dung từ 3 tới 8 dòng rồi mới cuộn; bộ đếm "230 / 255" hiện khi đã dùng từ 80% giới hạn | `TC-FORM-001` đến `004`, `MT-FORM-01` | `0f59cfd` |
| 2 | Form mời thành viên chọn vai trò bằng nút gạt "Chỉnh sửa" / "Chỉ xem" thay cho ô sổ xuống. Không có hàm tính toán mới nên không có test tự động | `MT-FORM-02` | `b7fd53d` |
| 3 | Trạng thái chuyến đi là huy hiệu có mũi tên, bấm mở menu 5 trạng thái có chấm màu và dấu ✓, chọn là lưu. Menu "⋮" của thẻ hoạt động mở / đóng cùng hiệu ứng. Không có hàm tính toán mới nên không có test tự động | `MT-FORM-03` | `40d2462` |
| 4 | Danh sách chuyến đi sắp xếp bằng nút "Sắp xếp: …" mở menu có dấu ✓, thay cho ô sổ xuống. Không có hàm tính toán mới nên không có test tự động | `MT-FORM-04` | `29b3f94` |
| 5 | Ô "Tiền tệ" (form chuyến đi và form hoạt động) là ô chọn kiểu mới: danh sách nổi có dấu ✓, tên tiếng Việt của từng loại tiền, gõ chữ để nhảy dòng; danh sách hiện đúng trên hộp thoại và Esc chỉ đóng danh sách. Tên loại tiền là dữ liệu, không có hàm tính toán mới nên không có test tự động | `MT-FORM-05` | `5fa90f5` |
| 6 | Hộp "Chuyển sang ngày khác" chọn ngày bằng ô chọn kiểu mới: mỗi dòng "Ngày 3" và dòng phụ thứ, ngày tháng, tiêu đề của ngày. Ô `<select>` cũ của trình duyệt không còn chỗ nào dùng. Không có hàm tính toán mới nên không có test tự động | `MT-FORM-06` | `5791acb` |
| 7 | Chủ chuyến đổi vai trò của từng thành viên bằng ô chọn kiểu mới cỡ nhỏ trong hộp chia sẻ; vẫn lưu ngay khi chọn. Không có hàm tính toán mới nên không có test tự động | `MT-FORM-07` | `b2d2c69` |
| 8 | Ô giờ của form hoạt động gõ được nhanh ("930" thành 09:30), có danh sách mỗi 15 phút, luôn 24 giờ; ô giờ kết thúc ghi thời lượng | `TC-FORM-005` đến `017`, `MT-FORM-08` | `92efda5` |
| 9 | Ngày đi và ngày về chọn trên một lịch tiếng Việt (2 tháng trên máy tính, 1 tháng trên điện thoại) trong một ô; dưới ô ghi "5 ngày 4 đêm" | `TC-FORM-018` đến `024`, `MT-FORM-09` | `423207a` |
| 10 | Hạn của liên kết chia sẻ chọn nhanh bằng chip "Không hết hạn" / "7 ngày" / "30 ngày" / "Chọn ngày…" (chip cuối mở lịch); dòng dưới chip ghi liên kết dùng được tới ngày nào | `TC-FORM-025` đến `028`, `MT-FORM-10` | `6f3ff96` |
| Sửa lỗi | Hộp "Chia sẻ" không còn báo "Email không được để trống" khi chủ chuyến chỉ làm việc khác; lỗi của ô email chỉ hiện khi bấm "Mời" (`BUG-UI-012`) | `MT-FORM-11` | `26b61bc` |

## A. Test tự động: khi nào hiện bộ đếm ký tự

> **Yêu cầu:** WORKFLOW.md Task 4.9 quyết định 5, UI_GUIDE 7.2 · **Kiểm bởi:** `lib/charCounter.test.ts` (4 lượt)

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-FORM-001 | Ghi chú giới hạn 255 ký tự, mới gõ 0 hoặc 203 ký tự | Không hiện bộ đếm | Đúng | Đạt |
| TC-FORM-002 | Gõ tới 204 ký tự (80% của 255, làm tròn lên); mô tả 800 / 1000 ký tự | Hiện "204 / 255", "800 / 1000" | Biên | Đạt |
| TC-FORM-003 | Gõ đủ 255 / 255 ký tự | Hiện "255 / 255", đánh dấu đã đầy (số đậm hơn) | Biên | Đạt |
| TC-FORM-004 | Ô không có giới hạn độ dài | Không bao giờ hiện bộ đếm | Biên | Đạt |

## B. Test tự động: đọc giờ gõ tay và danh sách giờ

> **Yêu cầu:** WORKFLOW.md Task 4.9 quyết định 4, UI_GUIDE 7.17 · **Kiểm bởi:** `lib/timeInput.test.ts` (9 lượt)

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-FORM-005 | Gõ 1 hoặc 2 chữ số: "9", "09", "21", "0" | Hiểu là giờ chẵn: 09:00, 09:00, 21:00, 00:00 | Đúng | Đạt |
| TC-FORM-006 | Gõ 3 hoặc 4 chữ số: "930", "0930", "2145" | 09:30, 09:30, 21:45 | Đúng | Đạt |
| TC-FORM-007 | Gõ giờ có dấu phân cách: "9:30", "09:30", "9.30", "9h30", "21h", " 7 H 05 " | 09:30 (bốn cách đầu), 21:00, 07:05 | Đúng | Đạt |
| TC-FORM-008 | Để trống hoặc chỉ gõ khoảng trắng | Ô trống (không có giờ), không báo lỗi | Biên | Đạt |
| TC-FORM-009 | Gõ thứ không phải giờ trong ngày: "24", "24:00", "960", "12:60", "12345", "abc", "9 sáng" | Không nhận (form báo giờ không hợp lệ) | Sai | Đạt |
| TC-FORM-010 | Gõ "9:5" (một chữ số phút sau dấu hai chấm) | Không nhận: không rõ là 09:05 hay 09:50 | Sai | Đạt |
| TC-FORM-011 | Danh sách giờ trong ngày | 96 dòng mỗi 15 phút, từ 00:00 tới 23:45 | Đúng | Đạt |
| TC-FORM-012 | Tìm dòng của 09:30 và của 09:20 | Đều là dòng 09:30 (09:20 lấy dòng kế tiếp) | Đúng | Đạt |
| TC-FORM-013 | Tìm dòng của 23:50 | Không còn dòng nào trong ngày | Biên | Đạt |

## C. Test tự động: thời lượng của hoạt động

> **Yêu cầu:** WORKFLOW.md Task 4.9 quyết định 4, UI_GUIDE 7.17 · **Kiểm bởi:** `lib/duration.test.ts` (4 lượt)

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-FORM-014 | Từ 09:00 tới 10:30; từ 23:30 tới 23:45 | 90 phút; 15 phút | Đúng | Đạt |
| TC-FORM-015 | Giờ kết thúc bằng hoặc trước giờ bắt đầu | Không có thời lượng | Sai | Đạt |
| TC-FORM-016 | Một trong hai ô trống hoặc chưa phải giờ ("930" chưa đổi) | Không có thời lượng | Biên | Đạt |
| TC-FORM-017 | Viết thời lượng 15, 60, 90, 150 phút | "15 phút", "1 giờ", "1 giờ 30 phút", "2 giờ 30 phút" | Đúng | Đạt |

## D. Test tự động: ngày trên lịch và độ dài chuyến đi

> **Yêu cầu:** WORKFLOW.md Task 4.9 quyết định 3 và bẫy 4 (ngày lệch một ngày), UI_GUIDE 7.18 · **Kiểm bởi:** `lib/calendarDate.test.ts` (7 lượt)

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-FORM-018 | Đổi "2026-03-12" thành ngày trên lịch rồi đổi lại | Đúng ngày 12 tháng 3 năm 2026 theo giờ máy, đổi lại vẫn "2026-03-12" (không lệch sang ngày 11 như khi đọc theo UTC) | Đúng | Đạt |
| TC-FORM-019 | Một ô lịch lúc 23:59 và lúc 00:00 cùng ngày | Cả hai đều là "2026-01-05" | Biên | Đạt |
| TC-FORM-020 | Giá trị trống, "2026-02-30", "2026-13-01", "12/03/2026"; và "2028-02-29" | Bốn giá trị đầu không phải ngày; 29/02/2028 (năm nhuận) là ngày | Sai | Đạt |
| TC-FORM-021 | Cộng ngày qua cuối tháng, cuối năm, ngày nhuận | 2026-03-30 + 3 = 2026-04-02; 2026-12-30 + 3 = 2027-01-02; 2028-02-28 + 1 = 2028-02-29 | Biên | Đạt |
| TC-FORM-022 | Chuyến 12/03 – 16/03 và 12/03 – 13/03 | "5 ngày 4 đêm", "2 ngày 1 đêm" | Đúng | Đạt |
| TC-FORM-023 | Chuyến đi trong một ngày | "1 ngày" (không ghi "0 đêm") | Biên | Đạt |
| TC-FORM-024 | Thiếu một ngày, hoặc ngày về trước ngày đi | Không ghi độ dài | Sai | Đạt |

## E. Test tự động: ngày cuối của liên kết chia sẻ khi chọn nhanh

> **Yêu cầu:** WORKFLOW.md Task 4.9 quyết định 3, UI_GUIDE 8.1 "Tạo liên kết" · **Kiểm bởi:** `lib/linkExpiry.test.ts` (4 lượt mới; 10 lượt của `expiryInstant` có từ Task 4.5 ở [11-sharing-ui.md](11-sharing-ui.md))

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-FORM-025 | Chọn "Không hết hạn" | Không có ngày cuối: liên kết không hết hạn | Đúng | Đạt |
| TC-FORM-026 | Hôm nay 18/10/2026 chọn "7 ngày", "30 ngày"; hôm nay 28/12/2026 chọn "7 ngày" | Hết ngày 25/10/2026, 17/11/2026; 04/01/2027 (qua cuối tháng, cuối năm) | Đúng | Đạt |
| TC-FORM-027 | "Chọn ngày…" đã chọn 24/12/2026; chưa chọn ngày nào | Ngày cuối là 24/12/2026; chưa có ngày cuối (form báo "Chọn ngày hết hạn trên lịch") | Biên | Đạt |
| TC-FORM-028 | Đã chọn một ngày trên lịch rồi quay lại "7 ngày" | Ngày cuối theo "7 ngày", bỏ qua ngày trên lịch | Biên | Đạt |

## Chuẩn bị chung cho các bài thủ công

Docker chạy `mysql`, `redis`, `mailhog`; backend profile `local`; frontend `npm run dev`. Một tài khoản có một chuyến đi vài ngày, có hoạt động. Để có sẵn đoạn chữ dài: gõ một câu rồi chép dán nhiều lần.

### MT-FORM-01 · Ô ghi chú tự giãn và bộ đếm ký tự (Mốc 1)

- [x] Mở "Thêm hoạt động": ô "Ghi chú" cao khoảng 3 dòng, không có thanh cuộn, góc dưới phải **không** có góc kéo đổi cỡ.
- [x] Gõ nhiều dòng (Enter xuống dòng): ô cao dần theo từng dòng; tới dòng thứ 8 thì dừng cao và có thanh cuộn trong ô. Hộp thoại không nhảy, tiêu đề hộp vẫn đứng yên.
- [x] Xoá bớt chữ: ô thấp lại, không thấp hơn 3 dòng.
- [x] Gõ hoặc dán chữ tới khoảng 200 ký tự: chưa có bộ đếm. Từ 204 ký tự: bên phải dòng "Tối đa 255 ký tự" hiện "204 / 255", số tăng theo từng phím. Tới 255: số đậm hơn và không gõ thêm được.
- [x] Lưu hoạt động có ghi chú dài đó, rồi mở "Sửa" lại: ô cao vừa nội dung và bộ đếm hiện ngay khi mở, đúng số ký tự.
- [x] Ghi chú của ngày ("Sửa" ở đầu một ngày): giống ghi chú hoạt động (giới hạn 255).
- [x] Mô tả chuyến đi (bước 1 của "Tạo chuyến đi" và hộp "Sửa chuyến đi"): bộ đếm chỉ hiện từ 800 ký tự ("800 / 1000").
- [x] 375px (DevTools, iPhone SE): bộ đếm và dòng gợi ý cùng một hàng, không đè lên nhau; ô rộng hết khung.
- [x] Nếu có Firefox: ô cũng tự giãn và dừng ở 8 dòng như trên Chrome.

**Kết quả:** Đạt · **Ngày:** 2026-10-18 · **Ghi chú:** chủ dự án tự chạy

### MT-FORM-02 · Nút gạt chọn vai trò khi mời (Mốc 2)

Tài khoản A là chủ một chuyến; MailHog mở ở `http://localhost:8025`.

- [x] A mở "Chia sẻ": dưới nhãn "Vai trò" là một rãnh xám có hai phần "✎ Chỉnh sửa" và "👁 Chỉ xem"; "Chỉnh sửa" đang được chọn (nền trắng, chữ đậm). Ô email, nút gạt và nút "Mời" nằm trên một hàng, nút gạt cao bằng ô email.
- [x] Con trỏ vẫn nằm ở ô email khi mở hộp (không nhảy vào nút gạt).
- [x] Bấm "Chỉ xem": phần trắng chuyển sang "Chỉ xem" mượt, không giật. Rê chuột lên phần chưa chọn: chữ đậm màu hơn.
- [x] Bàn phím: từ ô email nhấn Tab vào nút gạt (vòng xanh quanh phần đang chọn), phím ← / → đổi lựa chọn, Tab tiếp tới nút "Mời".
- [x] Mời một email với "Chỉ xem": danh sách có người mới mang vai trò "Chỉ xem"; ô email trống lại, nút gạt **vẫn** ở "Chỉ xem". Mời tiếp một email khác với "Chỉnh sửa": người đó mang vai trò "Chỉnh sửa".
- [x] 375px: ô email, nút gạt (rộng hết khung, hai phần bằng nhau) và nút "Mời" xếp dọc, không tràn.
- [x] Điện thoại thật hoặc DevTools có cảm ứng: nút gạt cao hơn (44px), chạm là chọn.

**Kết quả:** Đạt · **Ngày:** 2026-10-18 · **Ghi chú:** chủ dự án tự chạy

### MT-FORM-03 · Menu đổi trạng thái chuyến đi (Mốc 3)

Tài khoản A là chủ một chuyến đi; B là người chỉ xem của chuyến đó (nếu có từ các bài trước).

- [x] A mở trang chuyến đi: cạnh nút "Chia sẻ" là huy hiệu trạng thái cùng màu như trước, thêm mũi tên nhỏ bên phải.
- [x] Bấm huy hiệu: menu mờ dần mở ra ngay dưới, mũi tên xoay ngược lên; 5 dòng "Nháp", "Đã lên kế hoạch", "Đang diễn ra", "Đã hoàn thành", "Đã lưu trữ", mỗi dòng có chấm màu giống màu huy hiệu của nó; dòng hiện tại có dấu ✓ xanh ở bên phải.
- [x] Rê chuột qua các dòng: dòng dưới con trỏ có nền xám nhạt.
- [x] Chọn "Đang diễn ra": menu đóng, huy hiệu đổi ngay sang "Đang diễn ra" (màu xanh ngọc) với vòng quay thay mũi tên, rồi toast "Đã đổi trạng thái". Về `/trips`: thẻ chuyến đi cũng mang trạng thái mới.
- [x] Mở lại menu và chọn đúng trạng thái đang có: menu đóng, **không** có toast, DevTools → Network không có request `PATCH .../status`.
- [x] Bàn phím: Tab tới huy hiệu (vòng xanh), Enter mở menu, ↓ / ↑ di chuyển, Enter chọn; mở lại rồi Esc: menu đóng, focus quay về huy hiệu.
- [x] Bấm ra ngoài khi menu đang mở: menu đóng, không đổi gì.
- [x] Tắt backend rồi đổi trạng thái: toast đỏ "Không đổi được trạng thái: …", huy hiệu quay về trạng thái cũ.
- [x] Menu "⋮" của một thẻ hoạt động: mở / đóng cùng kiểu mờ dần; rê chuột lên "Xoá" nền xám nhạt, chữ vẫn đỏ; các mục chạy như cũ.
- [x] B (chỉ xem): vẫn là huy hiệu trạng thái **không** có mũi tên, bấm không mở gì.
- [x] Màn cảm ứng (DevTools có cảm ứng): các dòng của menu cao hơn (44px), chạm là chọn.
- [x] Hệ điều hành bật "giảm chuyển động" (Windows: Settings → Accessibility → Visual effects → Animation effects tắt): menu hiện ngay, không mờ dần.

**Kết quả:** Đạt · **Ngày:** 2026-10-18 · **Ghi chú:** chủ dự án tự chạy

### MT-FORM-04 · Menu sắp xếp danh sách chuyến đi (Mốc 4)

Tài khoản có từ 3 chuyến đi trở lên, khác tên và khác ngày đi.

- [x] Mở `/trips`: cuối hàng chip trạng thái (màn rộng) là nút viền mảnh "⇅ Sắp xếp: **Tạo gần đây nhất** ⌄", cao bằng trước.
- [x] Bấm nút: viền nút thành xanh ngọc, mũi tên xoay lên, menu mờ dần mở ra canh phải với nút; 4 dòng, ✓ ở "Tạo gần đây nhất".
- [x] Chọn "Tên A → Z": menu đóng, danh sách sắp lại theo tên, nút ghi "Sắp xếp: Tên A → Z", địa chỉ trang có `sort=title`. F5: vẫn sắp theo tên, ✓ ở đúng dòng.
- [x] Đang ở trang 2 (cần hơn 12 chuyến, bỏ qua nếu không có) rồi đổi thứ tự: về trang 1.
- [x] Chọn lại thứ tự đang dùng: không tải lại danh sách (DevTools → Network không có request `trips` mới).
- [x] Chọn "Tạo gần đây nhất": `sort` biến khỏi địa chỉ trang. Nút Back của trình duyệt quay lại thứ tự trước.
- [x] Bàn phím: Tab tới nút (vòng xanh), Enter mở, ↓ / ↑, Enter chọn; Esc đóng và trả focus về nút.
- [x] 375px: nút nằm dưới hàng chip, chữ không tràn; menu không ra ngoài màn hình. Màn cảm ứng: nút và các dòng cao 44px.

**Kết quả:** Đạt · **Ngày:** 2026-10-18 · **Ghi chú:** chủ dự án tự chạy

### MT-FORM-05 · Ô chọn tiền tệ kiểu mới (Mốc 5)

Tài khoản có một chuyến đi có hoạt động.

- [x] **Trong trang** (bước 2 "Ngày đi" của "Tạo chuyến đi"): ô "Tiền tệ" trông như ô chữ, ghi "VND", có mũi tên. Bấm: danh sách mờ dần mở ngay dưới, rộng hơn ô; 7 dòng, mỗi dòng mã tiền và tên tiếng Việt bên dưới ("VND" / "Đồng Việt Nam", "USD" / "Đô la Mỹ"...); ✓ xanh ở "VND".
- [x] Rê chuột qua các dòng: nền xanh ngọc nhạt. Chọn "USD": danh sách đóng, ô ghi "USD" (không kèm tên). Tạo chuyến đi xong: ngân sách hiện bằng USD ở trang chuyến đi.
- [x] **Trong hộp thoại** ("Sửa chuyến đi"): bấm ô "Tiền tệ": danh sách hiện **trên** hộp thoại (không bị hộp che, không bị cắt ở mép hộp), bấm chọn được. Lưu: tiền tệ đổi đúng.
- [x] Trong "Sửa chuyến đi", mở danh sách rồi nhấn **Esc**: chỉ danh sách đóng, hộp thoại **vẫn mở**, chữ đã gõ trong các ô khác còn nguyên. Nhấn Esc lần nữa: hộp thoại đóng.
- [x] Mở danh sách rồi bấm ra ngoài (vào chỗ trống trong hộp): danh sách đóng, hộp vẫn mở, tiền tệ không đổi.
- [x] Hộp "Thêm hoạt động" (ô "Tiền tệ" hẹp cạnh "Chi phí"): danh sách vẫn đủ rộng để đọc hết "Đô la Singapore"; hoạt động mới mặc định theo tiền tệ của chuyến đi như trước; chọn "EUR" rồi lưu: thẻ hoạt động ghi chi phí bằng EUR.
- [x] Ô "Tiền tệ" nằm gần đáy hộp (thu cửa sổ cho thấp): danh sách tự mở lên trên hoặc ngắn lại và cuộn được, không tràn ra ngoài màn hình.
- [x] Bàn phím: Tab tới ô (vòng xanh), Enter hoặc ↓ mở, ↑ / ↓ di chuyển, Enter chọn; khi danh sách đang mở gõ "s": nhảy tới "SGD"; Tab ra khỏi ô sang ô kế tiếp như bình thường.
- [x] Một chuyến đi cũ có mã tiền tệ ngoài danh sách (tạo qua Swagger, ví dụ "GBP"): ô vẫn ghi "GBP", danh sách có thêm dòng "GBP" (không có tên tiếng Việt) ở cuối.
- [x] Màn cảm ứng (DevTools có cảm ứng) và 375px: ô cao 44px, các dòng cao hơn, chạm là chọn; danh sách không ra ngoài màn hình.

**Kết quả:** Đạt · **Ngày:** 2026-10-18 · **Ghi chú:** chủ dự án tự chạy

### MT-FORM-06 · Chọn ngày đích khi chuyển hoạt động (Mốc 6)

Một chuyến đi từ 3 ngày trở lên; một ngày có tiêu đề dài (ví dụ "Khám phá bán đảo Sơn Trà và các bãi biển phía đông thành phố"); Ngày 1 có ít nhất một hoạt động.

- [x] Menu "⋮" của một hoạt động ở Ngày 1 → "Chuyển sang ngày…": hộp mở, con trỏ nằm ở ô "Ngày" (vòng xanh quanh ô), ô ghi "Ngày 2".
- [x] Bấm ô: danh sách hiện trên hộp, **không có Ngày 1** (ngày đang chứa hoạt động); mỗi dòng "Ngày N" và bên dưới "Thứ …, dd/mm/yyyy" kèm " · tiêu đề" nếu ngày có tiêu đề; ✓ ở "Ngày 2".
- [x] Dòng có tiêu đề dài: chữ xuống dòng trong danh sách, danh sách không rộng quá hộp thoại.
- [x] Chọn "Ngày 3": ô ghi "Ngày 3". Bấm "Chuyển sang ngày": hoạt động xuống cuối Ngày 3, toast có link "Mở Ngày 3" như trước.
- [x] Mở lại hộp, mở danh sách rồi Esc: chỉ danh sách đóng; Esc lần nữa: hộp đóng, hoạt động không đổi chỗ.
- [x] Bàn phím: khi hộp vừa mở, Enter mở danh sách (con trỏ đã ở ô), ↓ chọn ngày, Enter; Tab tới "Chuyển sang ngày", Enter.
- [x] 375px: hộp nằm sát đáy, danh sách vừa màn hình, các dòng cao hơn trên màn cảm ứng.

**Kết quả:** Đạt · **Ngày:** 2026-10-18 · **Ghi chú:** chủ dự án tự chạy

### MT-FORM-07 · Ô vai trò cỡ nhỏ trong danh sách thành viên (Mốc 7)

A là chủ một chuyến có B đã nhận lời ("Chỉ xem") và một lời mời đang chờ ("Chỉnh sửa").

- [x] A mở "Chia sẻ": dòng B và dòng đang chờ có ô chọn nhỏ (thấp hơn ô email, chữ nhỏ) có mũi tên, ghi đúng vai trò; ô ở hai dòng rộng bằng nhau. Dòng của A vẫn là huy hiệu "Chủ sở hữu".
- [x] Bấm ô của B: danh sách hiện trên hộp thoại, 2 dòng "Chỉnh sửa" / "Chỉ xem", ✓ ở "Chỉ xem". Chọn "Chỉnh sửa": danh sách đóng, ô bị khoá (con trỏ chờ) trong lúc lưu, rồi toast "Đã đổi vai trò". Ô không đổi bề rộng.
- [x] Chọn lại đúng vai trò đang có: không có toast, DevTools → Network không có request `PATCH .../members/...`.
- [x] Đổi vai trò của lời mời đang chờ: lưu được như trên.
- [x] Mở danh sách rồi Esc: chỉ danh sách đóng, hộp chia sẻ vẫn mở.
- [x] Bàn phím: Tab tới ô (vòng xanh), Enter mở, ↑ / ↓, Enter chọn.
- [x] Tab 2: gỡ B; quay lại tab 1 (không tải lại) đổi vai trò của B: toast đỏ "Không đổi được vai trò: …", danh sách tải lại và B biến mất.
- [x] B (chỉ xem) mở "Thành viên": các dòng vẫn là huy hiệu vai trò, không có ô chọn.
- [x] 375px và màn cảm ứng: ô cao 44px, không đẩy nút "Gỡ" ra khỏi dòng.

**Kết quả:** Đạt · **Ngày:** 2026-10-18 · **Ghi chú:** chủ dự án tự chạy

### MT-FORM-08 · Ô giờ gõ nhanh và danh sách giờ (Mốc 8)

Một chuyến đi có ngày với ít nhất một hoạt động có giờ (ví dụ 09:00 – 10:00).

- [x] "Thêm hoạt động": hai ô "Giờ bắt đầu" / "Giờ kết thúc" có icon đồng hồ, chữ mờ "--:--".
- [x] Gõ "930" vào giờ bắt đầu rồi Tab: ô thành "09:30". Thử lần lượt "9", "9h30", "21h", "7.45": thành 09:00, 09:30, 21:00, 07:45.
- [x] Bấm vào ô giờ bắt đầu: danh sách mờ dần mở ngay dưới ô, cuộn sẵn tới dòng 09:30 (nền xanh ngọc nhạt). Ô trống thì danh sách cuộn tới khoảng 08:00, không dòng nào sáng.
- [x] Đang mở danh sách, gõ "14": dòng 14:00 sáng lên và nằm giữa danh sách. Bấm dòng 14:15: ô thành "14:15", danh sách đóng, con trỏ vẫn ở ô.
- [x] Giờ kết thúc: bấm vào: danh sách bắt đầu từ 14:30, mỗi dòng ghi thời lượng ("14:30   15 phút", "15:15   1 giờ"...). Chọn 15:45: trong ô hiện "15:45" và bên phải mờ "(1 giờ 30 phút)".
- [x] Đổi giờ bắt đầu thành 15:00: thời lượng trong ô kết thúc đổi thành "(45 phút)". Đổi giờ bắt đầu thành 16:00: thời lượng biến mất, bấm "Thêm hoạt động" thì báo "Giờ kết thúc phải sau giờ bắt đầu" như trước.
- [x] Bàn phím: trong ô, ↓ mở danh sách, ↓ / ↑ di chuyển, Enter chọn dòng (form **không** bị gửi); Esc đóng danh sách, hộp thoại **vẫn mở**; Esc lần nữa đóng hộp.
- [x] Gõ "25" rồi Tab: ô vẫn ghi "25", dưới ô báo "Giờ không hợp lệ, hãy nhập dạng 09:30 (hoặc gõ 930)". Sửa thành "10": lỗi mất khi rời ô.
- [x] Gõ "930" vào ô rồi nhấn Enter ngay (danh sách đóng): form được gửi với giờ 09:30 (thẻ hoạt động ghi 09:30).
- [x] Lưu một hoạt động có giờ, mở "Sửa": hai ô ghi đúng giờ đã lưu, thời lượng hiện đúng. Xoá trắng giờ đã lưu rồi lưu: báo "Chưa hỗ trợ xoá thông tin này…" như trước.
- [x] Trùng giờ với hoạt động khác (09:30 – 10:30 khi đã có 09:00 – 10:00): vẫn có hộp hỏi "Trùng giờ…" như trước.
- [x] Máy đặt định dạng giờ 12 tiếng (Windows: Settings → Time & language → Region, hoặc trình duyệt tiếng Anh): ô vẫn hiện 24 giờ.
- [x] 375px và điện thoại: chạm vào ô hiện bàn phím số; danh sách vừa bề ngang ô, dòng cao hơn; hai ô xếp dọc.

**Kết quả:** Đạt · **Ngày:** 2026-10-18 · **Ghi chú:** chủ dự án tự chạy

### MT-FORM-09 · Lịch chọn ngày đi – ngày về (Mốc 9)

Một tài khoản đã đăng nhập; một chuyến đi có sẵn vài ngày và hoạt động (để thử "Sửa chuyến đi").

- [x] "Tạo chuyến đi", qua bước 1 và 2 tới bước "Ngày đi": **một** ô "Ngày đi – ngày về *" có icon lịch, chữ mờ "Chọn ngày đi và ngày về", con trỏ đã ở ô; dưới ô "Tối đa 60 ngày.".
- [x] Bấm ô: lịch mờ dần mở ngay dưới ô (lần đầu có thể thoáng khung xám khi đang tải). Màn rộng: **2 tháng** cạnh nhau, tiêu đề "Tháng 10 2026" / "Tháng 11 2026", hàng thứ "T2 T3 T4 T5 T6 T7 CN"; hôm nay có viền tròn xanh. Dòng trên lịch: "Chọn ngày đi".
- [x] Bấm một ngày: ngày đó thành tròn xanh đặc, dòng trên lịch thành "Từ dd/mm/yyyy: chọn ngày về". Các ngày làm chuyến đi dài quá 60 ngày bị mờ, bấm không được.
- [x] Bấm ngày về 4 ngày sau: lịch đóng; ô ghi "dd/mm/yyyy – dd/mm/yyyy", dưới ô "5 ngày 4 đêm. Tối đa 60 ngày.". Mở lại: khoảng đã chọn có hai đầu tròn xanh và dải xanh nhạt ở giữa, lịch mở đúng tháng của ngày đi.
- [x] Đang mở lại, bấm một ngày mới: khoảng cũ mất, bắt đầu khoảng mới từ ngày đó. Bấm một ngày **trước** ngày vừa bấm: khoảng bắt đầu lại từ ngày sớm hơn.
- [x] Bấm cùng một ngày hai lần: ô ghi một ngày, dưới ô "1 ngày".
- [x] Mở lịch, bấm ngày đi rồi nhấn Esc (hoặc bấm ra ngoài): lịch đóng, ô vẫn ghi khoảng cũ.
- [x] Nút ‹ › đổi tháng; chọn được ngày đã qua (ví dụ tháng trước).
- [x] Chưa chọn gì, bấm "Tạo chuyến đi": dưới ô báo "Ngày bắt đầu không được để trống", con trỏ về ô. Chỉ mở lịch rồi Esc (không chọn): lỗi cũng hiện khi lịch đóng, **không** hiện lúc lịch vừa mở.
- [x] Tạo chuyến 12/03 – 16/03 (năm sau): trang chuyến đi có đúng 5 ngày, từ 12/03 tới 16/03 (không lệch ngày).
- [x] **Trong hộp "Sửa chuyến đi":** lịch hiện trên hộp, chọn được; Esc khi lịch mở chỉ đóng lịch, hộp vẫn mở. Rút ngắn chuyến có hoạt động ở ngày bị cắt: vẫn có khung đỏ giải thích và hộp hỏi lại khi lưu như trước; dời cả chuyến: khung xanh "dời" như trước.
- [x] Bàn phím: Tab tới ô, Enter mở lịch (con trỏ ở ngày đã chọn hoặc hôm nay), mũi tên đi giữa các ngày, Enter chọn ngày đi rồi ngày về.
- [x] 375px (DevTools, iPhone SE): lịch **1 tháng**, vừa màn hình, ô ngày cao 44px trên cảm ứng, chạm là chọn.

**Kết quả:** Đạt · **Ngày:** 2026-10-18 · **Ghi chú:** chủ dự án tự chạy

### MT-FORM-10 · Chọn nhanh hạn của liên kết chia sẻ (Mốc 10)

A là chủ một chuyến đi; Swagger mở sẵn để xem `expiresAt` của liên kết (`GET /trips/{id}/share-links`).

- [x] A mở "Chia sẻ", mục "Liên kết chia sẻ": nhóm "Hết hạn" có 4 chip, "Không hết hạn" đang chọn (viền và nền xanh ngọc); dưới chip "Ai có liên kết đều xem được cho tới khi bạn thu hồi"; nút "Tạo liên kết" ở hàng dưới.
- [x] Bấm "Tạo liên kết": liên kết mới "Không hết hạn" ở đầu danh sách, toast "Đã tạo liên kết".
- [x] Bấm "7 ngày": dòng dưới chip thành "Dùng được đến hết ngày {hôm nay + 7}". Tạo liên kết: dòng của liên kết ghi "Dùng được đến hết ngày …" đúng ngày đó; Swagger: `expiresAt` là cuối ngày đó theo giờ Việt Nam (16:59:59.999Z). Nhóm chip về lại "Không hết hạn".
- [x] "30 ngày": dòng dưới chip ghi đúng hôm nay + 30, tạo được.
- [x] Bấm "Chọn ngày…": hiện ô "Dùng được đến hết ngày *" và lịch **mở luôn** (1 tháng, nổi trên hộp thoại); các ngày trước hôm nay bị mờ, bấm không được. Bấm một ngày: lịch đóng, ô ghi ngày đó. Tạo liên kết: hạn đúng ngày đó.
- [x] "Chọn ngày…" rồi Esc ngay (chưa chọn ngày): lịch đóng, hộp vẫn mở; bấm "Tạo liên kết": dưới ô báo "Chọn ngày hết hạn trên lịch", không tạo gì.
- [x] Chọn một ngày rồi quay về "7 ngày": ô ngày ẩn, dòng dưới chip theo "7 ngày"; tạo: hạn theo 7 ngày. Bấm lại "Chọn ngày…": ô hiện ngày đã chọn lúc nãy.
- [x] Bàn phím: Tab vào nhóm chip (vòng xanh quanh chip đang chọn), ← / → đổi chip; tới "Chọn ngày…" bằng phím: ô ngày hiện ra nhưng lịch **không** tự mở; Tab tới ô, Enter mở lịch.
- [x] Trong lúc đang tạo, Esc / "×" không đóng hộp (như Task 4.5).
- [x] 375px: 4 chip xuống hai hàng, không tràn; ô ngày rộng hết khung; chip cao 44px trên màn cảm ứng.

**Kết quả:** Đạt · **Ngày:** 2026-10-18 · **Ghi chú:** chủ dự án tự chạy

### MT-FORM-11 · Form mời chỉ báo lỗi khi bấm "Mời" (kiểm lại BUG-UI-012)

A là chủ một chuyến có ít nhất một thành viên và một liên kết.

- [x] A mở "Chia sẻ" (con trỏ ở ô email), không gõ gì, bấm vào ô vai trò của một thành viên, đổi vai trò: **không** có chữ đỏ dưới ô email.
- [x] Mở lại hộp, bấm Tab qua ô email sang nút gạt, chip hạn liên kết, bấm "Tạo liên kết": tạo được, ô email vẫn không báo lỗi.
- [x] Bấm "Mời" khi ô email trống: dưới ô "Email không được để trống". Gõ "a": lỗi đổi ngay thành "Email không đúng định dạng"; gõ tiếp cho đủ một email: lỗi biến mất ngay khi gõ, không cần rời ô.
- [x] Gõ "abc" rồi bấm sang chỗ khác (không bấm "Mời"): không báo lỗi. Bấm "Mời": báo "Email không đúng định dạng", không gửi gì.
- [x] Mời một email đúng: toast "Đã gửi lời mời", ô email trống lại và **không** báo "Email không được để trống".
- [x] Lỗi của máy chủ (mời email của chính A, hoặc người đã là thành viên) vẫn hiện dưới ô email như trước.

**Kết quả:** Đạt · **Ngày:** 2026-10-18 · **Ghi chú:** chủ dự án tự chạy; kiểm lại `BUG-UI-012`

## Lỗi đã phát hiện

Chưa có lỗi của riêng file này: 11 bài thủ công đều đạt ở lần chạy đầu. `BUG-UI-012` (chủ dự án báo trước khi chạy bài thủ công: hộp "Chia sẻ" báo "Email không được để trống" khi không định mời ai) nằm ở form mời của Task 4.5 nên ghi ở [11-sharing-ui.md](11-sharing-ui.md); đã sửa và kiểm lại bằng `MT-FORM-11`.

# 11 · Giao diện chia sẻ

> Cập nhật: 2026-10-18 · **Task 4.5 code xong** (19 commit `6760a26` đến `3ff64fd` trên nhánh `feat/T4.5-sharing-ui`; lint, build và 176 test tự động của giao diện xanh). Chủ dự án đã chạy cả 18 bài thủ công `MT-SHAREUI-01` đến `18`, **đều đạt**, và kiểm lại `BUG-UI-011` ngày 2026-10-18 · Mốc 17 (bản đồ trên trang công khai) tại `3ff64fd` · Mốc 16 (trang chuyến đi công khai) tại `d3374d0` · Mốc 15 (tách phần thân thẻ hoạt động) tại `154ba33` · Mốc 14 (thu hồi liên kết) tại `154cd99` · Mốc 13 (tạo liên kết) tại `f2ced15` · Sau Mốc 12 (tiêu đề hộp thoại đứng yên) tại `da20805` · Mốc 12 (xem và sao chép liên kết chia sẻ) tại `9271a54` · Mốc 11 (gỡ thành viên, huỷ lời mời) tại `80cd003` · **Sửa `BUG-UI-011`** (Esc trên hộp hỏi lại đóng luôn hộp bên ngoài; chủ dự án đã xác nhận trên trình duyệt) tại `540c624`, chủ dự án đã kiểm lại; ghi ở [06-itinerary-ui.md](06-itinerary-ui.md) · Mốc 10 (đổi vai trò) tại `0f9775b` · Mốc 9 (gửi lại lời mời) tại `47e4b19` · Mốc 8 (đăng nhập / đăng ký khi mở link mời) tại `4d805f9` · Mốc 7 (trang nhận lời mời) tại `0ef4c10` · Mốc 6 (mời thành viên) tại `1f537f5` · Mốc 5 (hộp thoại thành viên) tại `7575498` · Mốc 4 (lịch trình chỉ đọc) tại `b7aef61` · Mốc 3 (ẩn nút sửa ở đầu trang) tại `401e1c8` · Mốc 2 (huy hiệu vai trò cạnh tên chuyến đi) tại `91053c4` · Mốc 1 (thẻ chuyến đi được chia sẻ) tại `6760a26` · [Về trang chính](README.md)

Giao diện cho phần chia sẻ của Phase 4: người được mời thấy chuyến đi trong danh sách của mình, người chỉ xem không thấy nút sửa, chủ chuyến mời và quản lý thành viên, tạo liên kết chia sẻ, người được mời nhận lời từ link trong mail, và khách cầm liên kết xem được lịch trình mà không cần tài khoản (Task 4.5). Bình luận (Task 4.6) ghi tiếp vào file này.

Backend của các tính năng này đã có từ Task 4.1 đến 4.4 và đã được kiểm ở [09-sharing.md](09-sharing.md). File này chỉ kiểm phần nhìn thấy trên trình duyệt. Phần tính toán (vai trò được làm gì, chữ cái đầu của tên, địa chỉ liên kết, ngày hết hạn) có test tự động từ các mốc sau.

File này được ghi dần theo từng mốc của Task 4.5:

| Mốc | Nội dung | Kiểm tra | Mã commit |
|---|---|---|---|
| 1 | Thẻ của chuyến đi người khác chia sẻ cho mình có huy hiệu "Được chia sẻ" và tên chủ chuyến. Chỉ là hiển thị, không có hàm tính toán mới nên không có test tự động | `MT-SHAREUI-01` | `6760a26` |
| 2 | Trang chi tiết có huy hiệu vai trò của người đang xem cạnh tên chuyến đi (Chủ sở hữu / Chỉnh sửa / Chỉ xem); chuyến riêng của chủ không có huy hiệu | `TC-SHAREUI-001` đến `003`, `MT-SHAREUI-02` | `91053c4` |
| 3 | Người chỉ xem không thấy ô đổi trạng thái (chỉ thấy huy hiệu), nút "Sửa", "Xoá" và không bị hỏi "Hoàn thành chuyến đi?"; người chỉnh sửa không thấy "Xoá" | `TC-SHAREUI-004`, `005`, `MT-SHAREUI-03` | `401e1c8` |
| 4 | Lịch trình của người chỉ xem không có nút sửa, thêm, kéo thả, menu "⋮"; câu chữ ở ngày trống, bản đồ và thời tiết không bảo họ đi sửa. Dùng lại hàm của Mốc 3 nên không có test tự động mới | `MT-SHAREUI-04` | `b7aef61` |
| 5 | Nút "Chia sẻ" (chủ) hoặc "Thành viên" (người khác) ở đầu trang mở hộp thoại có danh sách thành viên: ảnh hoặc chữ cái đầu, tên, email, vai trò, "Đang chờ" | `TC-SHAREUI-006` đến `009`, `MT-SHAREUI-05` | `7575498` |
| 6 | Chủ chuyến mời một email với vai trò "Chỉnh sửa" hoặc "Chỉ xem" ngay trong hộp chia sẻ; lỗi của máy chủ hiện dưới ô email. Quy tắc kiểm email giống trang đăng ký, máy chủ đã kiểm ở [09-sharing.md](09-sharing.md); không có hàm tính toán mới nên không có test tự động | `MT-SHAREUI-06` | `1f537f5` |
| 7 | Trang `/invite` mở từ link trong mail: nhận lời ngay rồi vào chuyến đi; báo "Lời mời không còn hiệu lực", "Lời mời này dành cho email khác" (kèm nút đăng xuất) hoặc lỗi kết nối kèm "Thử lại" | `TC-SHAREUI-010` đến `012`, `MT-SHAREUI-07` | `0ef4c10` |
| 8 | Người mở link mời khi chưa đăng nhập được hướng dẫn trên trang đăng nhập, đăng ký và "Kiểm tra hộp thư"; đường quay lại lời mời giữ được khi chuyển giữa đăng nhập và đăng ký | `TC-SHAREUI-013` đến `015`, `MT-SHAREUI-08` | `4d805f9` |
| 9 | Dòng lời mời đang chờ ghi ngày mời ("Đã mời 12/10", theo múi giờ tài khoản); chủ chuyến có nút "Gửi lại" | `TC-SHAREUI-016`, `017`, `MT-SHAREUI-09` | `47e4b19` |
| 10 | Chủ chuyến đổi vai trò "Chỉnh sửa" / "Chỉ xem" của từng thành viên (kể cả lời mời đang chờ) ngay trong danh sách, chọn là lưu. Không có hàm tính toán mới nên không có test tự động; máy chủ đã kiểm ở [09-sharing.md](09-sharing.md) | `MT-SHAREUI-10` | `0f9775b` |
| 11 | Chủ chuyến gỡ một thành viên hoặc huỷ một lời mời đang chờ, có hộp hỏi lại. Không có hàm tính toán mới nên không có test tự động; máy chủ đã kiểm ở [09-sharing.md](09-sharing.md) | `MT-SHAREUI-11` | `80cd003` |
| Sửa lỗi | Esc trên một hộp hỏi lại chỉ đóng hộp đó, hộp bên dưới (form hoạt động, sửa chuyến đi, hộp chia sẻ) giữ nguyên (`BUG-UI-011`) | `MT-SHAREUI-11` bước Esc, `MT-UI-20` | `540c624` |
| 12 | Chủ chuyến thấy các liên kết chia sẻ trong hộp "Chia sẻ": địa chỉ đầy đủ, nút "Sao chép", số lượt xem, hạn dùng, nhãn "Hết hạn" | `TC-SHAREUI-018` đến `020`, `MT-SHAREUI-12` | `9271a54` |
| Sau 12 | Mọi hộp thoại: nội dung dài thì chỉ phần nội dung cuộn, tiêu đề và nút "×" đứng yên (điểm 1 khi duyệt mockup hộp chia sẻ). Chỉ là bố cục, không có test tự động | `MT-SHAREUI-13` | `da20805` |
| 13 | Chủ chuyến tạo liên kết chia sẻ, có thể chọn ngày hết hạn; ngày được đổi thành thời điểm cuối ngày theo múi giờ tài khoản | `TC-SHAREUI-021` đến `024`, `MT-SHAREUI-14` | `f2ced15` |
| 14 | Chủ chuyến thu hồi một liên kết chia sẻ (có hộp hỏi lại); người cầm liên kết không mở được nữa. Không có hàm tính toán mới nên không có test tự động; máy chủ đã kiểm ở [09-sharing.md](09-sharing.md) | `MT-SHAREUI-15` | `154cd99` |
| 15 | Tách phần thân của thẻ hoạt động (giờ, loại, tên, địa điểm, ghi chú, chi phí, link đặt chỗ) thành một phần dùng chung, để trang công khai (Mốc 16) vẽ cùng một kiểu thẻ. Không đổi gì trên màn hình | `MT-SHAREUI-16` | `154ba33` |
| 16 | Trang công khai `/share/:token`: ai có liên kết đều xem được tóm tắt và mọi ngày của chuyến đi, không cần tài khoản; liên kết hỏng hoặc không còn dùng được báo "Liên kết không còn hiệu lực" | `TC-SHAREUI-025`, `026`, `MT-SHAREUI-17` | `d3374d0` |
| 17 | Trang công khai có một bản đồ với mọi địa điểm của chuyến đi, không số, không đường nối; bản đồ trong ứng dụng không đổi | `TC-SHAREUI-027` đến `029`, `MT-SHAREUI-18` | `3ff64fd` |

## A. Test tự động: khi nào hiện huy hiệu vai trò

> **Yêu cầu:** WORKFLOW.md Task 4.5 quyết định 4, UI_GUIDE 8.1 · **Kiểm bởi:** `features/sharing/tripRole.test.ts` (4 lượt)

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-SHAREUI-001 | Chủ chuyến mở chuyến đi chỉ có mình trong danh sách thành viên | Không hiện huy hiệu "Chủ sở hữu" | Đúng | Đạt |
| TC-SHAREUI-002 | Chủ chuyến mở chuyến đi đã mời thêm một người (đang chờ hoặc đã nhận lời) | Hiện huy hiệu "Chủ sở hữu" | Đúng | Đạt |
| TC-SHAREUI-003 | Người chỉnh sửa, rồi người chỉ xem, mở chuyến đi được chia sẻ | Luôn hiện huy hiệu vai trò của mình (2 lượt) | Đúng | Đạt |

Điều test tự động **chưa chứng minh**: huy hiệu nằm đúng chỗ, đúng màu trên trang. Xem trên trình duyệt: `MT-SHAREUI-02`.

## B. Test tự động: ai được sửa chuyến đi

> **Yêu cầu:** design.md 6.2 (ma trận quyền: sửa thông tin, đổi trạng thái, sửa lịch trình) · **Kiểm bởi:** `features/sharing/tripRole.test.ts` (3 lượt)

Giao diện dùng cùng một hàm để quyết định có hiện các nút sửa hay không. Máy chủ vẫn tự kiểm quyền (đã kiểm ở [09-sharing.md](09-sharing.md)); hàm này chỉ để người chỉ xem không gặp nút bấm vào là bị từ chối.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-SHAREUI-004 | Chủ chuyến, rồi người chỉnh sửa | Được sửa (2 lượt) | Đúng | Đạt |
| TC-SHAREUI-005 | Người chỉ xem | Không được sửa | Bảo mật | Đạt |

## C. Test tự động: chữ cái đầu trên ảnh đại diện

> **Yêu cầu:** UI_GUIDE 7.14 · **Kiểm bởi:** `lib/initials.test.ts` (9 lượt)

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-SHAREUI-006 | Tên hai hoặc ba từ, có dấu, viết thường, hoặc có dấu cách thừa ("Mai Anh", "Trần Nhật Triều", "đặng văn ân", "  Lê   Hoàng  ") | Chữ đầu của từ đầu và từ cuối, viết hoa, giữ dấu: "MA", "TT", "ĐÂ", "LH" (4 lượt) | Đúng | Đạt |
| TC-SHAREUI-007 | Tên chỉ một từ ("Trieu") | Một chữ: "T" | Biên | Đạt |
| TC-SHAREUI-008 | Tên gõ bằng bàn phím gửi chữ và dấu thành hai ký tự riêng ("Ê" = "E" + dấu mũ) | Vẫn ra một chữ có dấu: "ÊÚ", không tách dấu ra | Biên | Đạt |
| TC-SHAREUI-009 | Người được mời chưa có tài khoản: tên trống, rỗng, hoặc chỉ có dấu cách | Chữ đầu của email, viết hoa: "Q" (3 lượt) | Biên | Đạt |

## D. Test tự động: đọc link trong mail mời

> **Yêu cầu:** design.md 10.2 "Quy ước Sharing API" (link `/invite?trip={tripId}&token={token}`), UI_GUIDE 8.8 · **Kiểm bởi:** `lib/inviteParams.test.ts` (12 lượt)

Link thiếu hoặc hỏng bị nhận ra ngay trên trình duyệt, trang báo "Lời mời không còn hiệu lực" mà không gửi gì lên máy chủ.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-SHAREUI-010 | Link đúng như máy chủ gửi: mã chuyến là số, token 43 ký tự | Đọc ra đúng mã chuyến và token | Đúng | Đạt |
| TC-SHAREUI-011 | Link thiếu mã chuyến, thiếu token, token rỗng; mã chuyến là chữ, bằng 0, âm, có phần thập phân, hoặc quá lớn | Coi là link không hợp lệ (8 lượt) | Sai | Đạt |
| TC-SHAREUI-012 | Token bị cắt ngắn, bị chèn dấu cách (dấu "+" trong link), hoặc có ký tự máy chủ không bao giờ dùng ("/", "=", ".") | Coi là link không hợp lệ (3 lượt) | Biên | Đạt |

## E. Test tự động: nhận ra người đến từ link mời

> **Yêu cầu:** design.md 15 (`/invite`), WORKFLOW.md Task 4.5 quyết định 1, UI_GUIDE 8.3 · **Kiểm bởi:** `features/auth/returnTo.test.ts` (7 lượt)

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-SHAREUI-013 | Trang đăng nhập được mở vì người chưa đăng nhập vừa mở link mời | Nhận ra là đến từ lời mời (hiện hướng dẫn) | Đúng | Đạt |
| TC-SHAREUI-014 | Trang đăng nhập được mở vì người chưa đăng nhập mở một trang chuyến đi | Không phải lời mời (không hiện hướng dẫn) | Đúng | Đạt |
| TC-SHAREUI-015 | Trang đăng nhập mở thẳng, hoặc thông tin đi kèm trống, thiếu, sai kiểu | Không phải lời mời, không lỗi (5 lượt) | Biên | Đạt |

## F. Test tự động: ngày mời theo múi giờ tài khoản

> **Yêu cầu:** design.md rule 14.22 (ngày tính theo múi giờ tài khoản), UI_GUIDE 8.1 "Lời mời đang chờ" · **Kiểm bởi:** `lib/format.test.ts` → `formatDayMonthIn` (3 lượt)

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-SHAREUI-016 | Máy chủ ghi thời điểm mời 03:00 giờ quốc tế ngày 12/10; thời điểm có cả phần triệu giây như máy chủ gửi | "12/10" theo giờ Việt Nam; phần lẻ giây không làm lỗi ("05/01") | Đúng | Đạt |
| TC-SHAREUI-017 | Mời lúc 17:30 giờ quốc tế ngày 12/10, tức 00:30 ngày 13/10 ở Việt Nam | Tài khoản giờ Việt Nam thấy "13/10"; tài khoản giờ quốc tế thấy "12/10": ngày theo múi giờ tài khoản, không theo máy | Biên | Đạt |

## G. Test tự động: địa chỉ của liên kết chia sẻ

> **Yêu cầu:** design.md 10.2 "Quy ước Sharing API" (máy chủ chỉ gửi token, giao diện ghép `{origin}/share/{token}`) · **Kiểm bởi:** `lib/shareUrl.test.ts` (4 lượt)

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-SHAREUI-018 | Ứng dụng chạy ở máy (`http://localhost:5173`) và trên tên miền thật | Địa chỉ là gốc của trang + `/share/` + token (2 lượt) | Đúng | Đạt |
| TC-SHAREUI-019 | Gốc của trang có sẵn dấu "/" ở cuối | Không ra hai dấu "/" liền nhau | Biên | Đạt |
| TC-SHAREUI-020 | Token có ký tự đặc biệt ("/", "?"; máy chủ không bao giờ gửi, kiểm phòng xa) | Ký tự được mã hoá, địa chỉ không bị lệch sang trang khác | Bảo mật | Đạt |

## H. Test tự động: ngày hết hạn của liên kết

> **Yêu cầu:** WORKFLOW.md Task 4.5 quyết định 5 (liên kết dùng được hết ngày đã chọn, theo múi giờ tài khoản), design.md rule 14.22, 10.2 (`expiresAt` là một thời điểm) · **Kiểm bởi:** `lib/linkExpiry.test.ts` (10 lượt)

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-SHAREUI-021 | Chọn ngày 20/10/2026 với tài khoản giờ Việt Nam, giờ quốc tế, giờ Ấn Độ (lệch nửa tiếng), giờ California | Thời điểm hết hạn là 23:59:59,999 ngày 20/10 theo giờ của tài khoản đó, ví dụ giờ Việt Nam là 16:59:59,999 giờ quốc tế (4 lượt) | Đúng | Đạt |
| TC-SHAREUI-022 | Ngày cuối tháng, ngày cuối năm, ngày 29/02 của năm nhuận | Không nhảy sai sang tháng / năm khác (3 lượt) | Biên | Đạt |
| TC-SHAREUI-023 | Ngày bắt đầu giờ mùa hè (Berlin, 29/03/2026) và ngày kết thúc giờ mùa hè (New York, 01/11/2026) | Dùng độ lệch giờ của cuối ngày đó, không phải đầu ngày (2 lượt) | Biên | Đạt |
| TC-SHAREUI-024 | Tài khoản có múi giờ trình duyệt không biết | Dùng giờ Việt Nam, không làm hỏng trang | Biên | Đạt |

## I. Test tự động: nhận ra địa chỉ liên kết hỏng

> **Yêu cầu:** design.md 10.2 "Trang công khai" (token 32 ký tự `[a-z0-9]`), UI_GUIDE 8.5 · **Kiểm bởi:** `lib/shareUrl.test.ts` → `isShareToken` (6 lượt)

Địa chỉ `/share/...` không có dạng mà máy chủ cấp thì trang báo "Liên kết không còn hiệu lực" ngay, không gửi gì lên máy chủ.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-SHAREUI-025 | Token đúng dạng máy chủ cấp (32 chữ thường và số) | Được coi là liên kết, trang hỏi máy chủ | Đúng | Đạt |
| TC-SHAREUI-026 | Không có token, token thiếu một ký tự, thừa một ký tự, viết hoa, hoặc có ký tự khác | Không phải liên kết, trang báo ngay mà không hỏi máy chủ (5 lượt) | Sai | Đạt |

## J. Test tự động: địa điểm trên bản đồ của trang công khai

> **Yêu cầu:** WORKFLOW.md Task 4.5 quyết định 2 (một bản đồ cho cả chuyến), UI_GUIDE 8.5 · **Kiểm bởi:** `features/public/publicStops.test.ts` (3 lượt)

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-SHAREUI-027 | Chuyến hai ngày, mỗi ngày có hoạt động gắn địa điểm | Mọi địa điểm lên bản đồ theo thứ tự ngày rồi thứ tự trong ngày; tên ghi kèm ngày ("Ngày 1 · Chùa Linh Ứng"); đúng loại và toạ độ | Đúng | Đạt |
| TC-SHAREUI-028 | Có hoạt động không gắn địa điểm và có ngày trống | Chúng không lên bản đồ; mã của từng điểm không trùng nhau dù khác ngày | Biên | Đạt |
| TC-SHAREUI-029 | Chuyến không có hoạt động nào gắn địa điểm | Danh sách rỗng, trang không vẽ bản đồ | Biên | Đạt |

## Chuẩn bị chung cho các bài thủ công

Cần hai tài khoản đã xác thực email: **A** (chủ chuyến) và **B** (người được mời). Chạy backend với profile `local`, frontend bằng `npm run dev`, MailHog mở ở `http://localhost:8025`.

Trước khi có giao diện mời (Mốc 6) và trang nhận lời (Mốc 7), tạo thành viên bằng Swagger (`http://localhost:8080/swagger-ui.html`):

1. Đăng nhập A (`POST /api/v1/auth/login`), bấm **Authorize** và dán access token.
2. `POST /api/v1/trips/{tripId}/members` với `{"email": "<email của B>", "role": "VIEWER"}` → 201.
3. Mở mail mời trong MailHog, chép giá trị `token` trong link `/invite?trip=...&token=...`.
4. Đăng nhập B trên Swagger, `POST /api/v1/trips/{tripId}/members/accept` với `{"token": "<token vừa chép>"}` → 200.

## Kiểm tra thủ công

### MT-SHAREUI-01 · Thẻ chuyến đi được chia sẻ (Mốc 1)

A có ít nhất hai chuyến đi; một chuyến đã chia sẻ cho B (VIEWER, đã nhận lời) theo phần "Chuẩn bị chung". B có ít nhất một chuyến của riêng mình.

- [x] Đăng nhập B, mở `/trips`: chuyến của A nằm chung danh sách. Góc trên **trái** ảnh bìa có huy hiệu viền xanh dương "Được chia sẻ"; góc trên phải vẫn là huy hiệu trạng thái.
- [x] Dưới dòng ngày đi của thẻ đó có icon người và dòng "Của {tên đầy đủ của A}".
- [x] Chuyến của chính B không có huy hiệu "Được chia sẻ" và không có dòng "Của …".
- [x] Làm lại với một chủ chuyến có tên thật dài (đăng ký tài khoản mới với tên dài, chưa có màn đổi tên): dòng "Của …" bị cắt bằng "…" trên một dòng, rê chuột thấy tên đầy đủ; thẻ không bị vỡ.
- [x] Màn hình rộng (3 cột) và 375px (1 cột): hai huy hiệu trên ảnh không đè lên nhau; chân thẻ các thẻ trong cùng hàng vẫn thẳng hàng.
- [x] Đăng xuất B, đăng nhập A: hai chuyến của A **không** có huy hiệu "Được chia sẻ" (A là chủ).
- [x] Bấm thẻ được chia sẻ ở tài khoản B: mở được trang chi tiết chuyến đi (chưa ẩn nút sửa: Mốc 3, 4).

**Kết quả:** Đạt · **Ngày:** 2026-10-18 · **Ghi chú:** chủ dự án tự chạy

### MT-SHAREUI-02 · Huy hiệu vai trò cạnh tên chuyến đi (Mốc 2)

Ba tài khoản: A (chủ), B (VIEWER đã nhận lời), C (EDITOR đã nhận lời) trên cùng một chuyến của A; A có thêm một chuyến riêng không mời ai.

- [x] A mở chuyến riêng: cạnh tên **không** có huy hiệu nào.
- [x] A mở chuyến đã chia sẻ: ngay sau tên có huy hiệu xanh ngọc "Chủ sở hữu", ngang dòng đầu của tên.
- [x] B mở chuyến đó: huy hiệu xám "Chỉ xem". C mở: huy hiệu xanh dương "Chỉnh sửa".
- [x] A mời thêm một email chưa nhận lời trên chuyến riêng (Swagger), tải lại trang: huy hiệu "Chủ sở hữu" xuất hiện.
- [x] Chuyến có tên rất dài (2 dòng): tên vẫn tối đa 2 dòng có "…", huy hiệu nằm cuối dòng đầu, không đẩy ô trạng thái và nút "Sửa" / "Xoá" xuống; rê chuột lên tên thấy tên đầy đủ.
- [x] 375px: tên, huy hiệu, rồi hàng ô trạng thái + nút bên dưới; không có thanh cuộn ngang.

**Kết quả:** Đạt · **Ngày:** 2026-10-18 · **Ghi chú:** chủ dự án tự chạy

### MT-SHAREUI-03 · Đầu trang của người chỉ xem và người chỉnh sửa (Mốc 3)

Như `MT-SHAREUI-02`: A chủ, B VIEWER, C EDITOR trên một chuyến của A. Để thử hộp "Hoàn thành chuyến đi?", chuyến đó có ngày cuối đã qua và trạng thái còn "Đang diễn ra" (hoặc Nháp / Đã lên kế hoạch).

- [x] A mở chuyến: thấy ô trạng thái bấm được, nút "Sửa" và "Xoá"; hộp "Hoàn thành chuyến đi?" hiện như trước.
- [x] C (chỉnh sửa) mở: thấy ô trạng thái và nút "Sửa", **không** có "Xoá"; hộp "Hoàn thành chuyến đi?" có hiện; bấm "Hoàn thành chuyến đi" thì thành công.
- [x] B (chỉ xem) mở: chỗ ô trạng thái là một huy hiệu cùng màu, bấm vào không có gì; **không** có "Sửa", "Xoá"; **không** có hộp "Hoàn thành chuyến đi?".
- [x] Ở tài khoản B, huy hiệu trạng thái nằm ngang dòng đầu của tên chuyến (màn rộng) và nằm hàng riêng dưới tên ở 375px.
- [x] A đổi B thành "Chỉnh sửa" (Swagger `PATCH /members/{memberId}`), B tải lại trang: ô trạng thái và nút "Sửa" xuất hiện.
- [x] Lịch trình của B vẫn còn nút "Thêm hoạt động", tay kéo, menu "⋮": đúng ở mốc này, ẩn ở Mốc 4.

**Kết quả:** Đạt · **Ngày:** 2026-10-18 · **Ghi chú:** chủ dự án tự chạy

### MT-SHAREUI-04 · Lịch trình chỉ đọc của người chỉ xem (Mốc 4)

A chủ, B VIEWER, C EDITOR trên một chuyến của A. Chuyến có ít nhất 2 ngày: một ngày có vài hoạt động gắn địa điểm (để có bản đồ và quãng đường), một ngày trống. Thử thêm một lần với một chuyến **chưa đặt vị trí điểm đến**.

- [x] B mở ngày có hoạt động: khối tiêu đề ngày **không** có "Sửa" và "+ Thêm hoạt động"; rê chuột lên thẻ không hiện tay kéo bên trái và menu "⋮" bên phải.
- [x] B trên màn hình cảm ứng (hoặc DevTools giả lập điện thoại có cảm ứng): không có nút ↑ / ↓ trên thẻ.
- [x] B dùng bàn phím: Tab qua các thẻ không dừng ở tay kéo hay menu nào.
- [x] B rê chuột lên thẻ: marker tương ứng trên bản đồ vẫn phóng to. Bấm một marker: trang vẫn cuộn tới đúng thẻ và thẻ có viền sáng.
- [x] B mở ngày trống: khung nét đứt chỉ ghi "Ngày này chưa có hoạt động.", không có nút. Thẻ bản đồ chỉ ghi "Ngày này chưa có địa điểm nào." (không có câu "Thêm địa điểm cho hoạt động…").
- [x] B mở chuyến chưa đặt vị trí điểm đến: thẻ thời tiết ghi "Chuyến đi chưa đặt vị trí điểm đến nên chưa có dự báo thời tiết."; thẻ bản đồ của ngày trống không nhắc tới "Sửa" chuyến đi.
- [x] B vẫn chuyển ngày được bằng cột ngày, chip ngày, nút ngày trước / sau; bản đồ, thời tiết, quãng đường giữa các hoạt động hiện như với chủ.
- [x] C (chỉnh sửa) và A (chủ) mở cùng chuyến: mọi nút sửa, thêm, kéo thả, menu "⋮" vẫn đủ như trước; kéo thả một hoạt động vẫn lưu được; ngày trống vẫn có nút "Thêm hoạt động".

**Kết quả:** Đạt · **Ngày:** 2026-10-18 · **Ghi chú:** chủ dự án tự chạy

### MT-SHAREUI-05 · Hộp thoại thành viên (Mốc 5)

A chủ, B VIEWER (đã nhận lời), C EDITOR (đã nhận lời) trên một chuyến của A; A mời thêm một email **chưa có tài khoản** (Swagger, không cần nhận lời). A có thêm một chuyến riêng không mời ai.

- [x] A mở chuyến: ở đầu trang có nút viền trắng "Chia sẻ" (icon chia sẻ) giữa ô trạng thái và "Sửa"; nút chính của trang vẫn chỉ là "+ Thêm hoạt động".
- [x] Bấm "Chia sẻ": hộp thoại rộng 640px tiêu đề "Chia sẻ chuyến đi", danh sách "Thành viên (4)": A đầu tiên với huy hiệu xanh ngọc "Chủ sở hữu", rồi B và C theo thứ tự được mời, rồi email chưa có tài khoản.
- [x] Mỗi dòng có vòng tròn chữ cái đầu (A, B, C: hai chữ từ đầu và cuối tên, nền xanh nhạt), tên đậm, email xám bên dưới, huy hiệu vai trò ("Chỉnh sửa" xanh dương, "Chỉ xem" xám) bên phải.
- [x] Dòng email chưa có tài khoản: chữ cái đầu là chữ đầu của email trên nền vàng nhạt, chỉ một dòng email, có huy hiệu vàng "Đang chờ" cạnh huy hiệu vai trò.
- [x] Hộp đóng bằng "×" và bằng phím Esc; bấm ra vùng tối bên ngoài thì hộp **không** đóng (như mọi hộp thoại khác của ứng dụng). Đóng xong trang giữ nguyên.
- [x] B (chỉ xem) mở chuyến: nút ghi "Thành viên" (icon nhóm người) nằm sau huy hiệu trạng thái; hộp thoại tiêu đề "Thành viên chuyến đi", cùng danh sách, không có nút nào khác ngoài "×".
- [x] C (chỉnh sửa) mở: nút "Thành viên" nằm giữa ô trạng thái và "Sửa".
- [x] A mở chuyến riêng: nút "Chia sẻ" vẫn có; hộp có "Thành viên (1)" chỉ một dòng của A.
- [x] Email hoặc tên rất dài: bị cắt bằng "…", huy hiệu không bị đẩy ra ngoài hộp.
- [x] 375px: hộp nằm sát mép dưới, rộng hết màn hình; cụm ô trạng thái + nút ở đầu trang xuống dòng nếu thiếu chỗ, không có thanh cuộn ngang.

**Kết quả:** Đạt · **Ngày:** 2026-10-18 · **Ghi chú:** chủ dự án tự chạy

### MT-SHAREUI-06 · Mời thành viên (Mốc 6)

A chủ một chuyến chưa mời ai; B có tài khoản đã xác thực; C đã là thành viên (đã nhận lời) của một chuyến khác của A. MailHog mở ở `http://localhost:8025`.

- [x] A mở chuyến chưa mời ai, bấm "Chia sẻ": trên cùng là "Mời thành viên" với ô "Email *", ô "Vai trò" (mặc định "Chỉnh sửa"), nút xanh "Mời"; con trỏ đã nằm trong ô email. Dưới form có câu "Chỉ mình bạn thấy chuyến đi này. Mời bạn bè để cùng chỉnh sửa." (không có nút); danh sách "Thành viên (1)".
- [x] Bấm "Mời" khi ô trống: lỗi "Email không được để trống" dưới ô, không gửi gì (DevTools → Network không có request). Gõ "abc": rời ô thì báo "Email không đúng định dạng".
- [x] Gõ email của B có dấu cách ở đầu và cuối, chọn "Chỉ xem", bấm "Mời": nút quay vòng rồi toast "Đã gửi lời mời tới {email của B}"; ô email trống lại, ô vai trò vẫn "Chỉ xem"; danh sách thành "Thành viên (2)", dòng B có "Đang chờ" và "Chỉ xem"; câu "Chỉ mình bạn…" biến mất. MailHog có mail mời gửi tới B.
- [x] Mời lại đúng email của B: vẫn thành công, MailHog có mail thứ hai (gửi lại lời mời), danh sách vẫn 2 người.
- [x] Gõ email của chính A, bấm "Mời": dưới ô email hiện "Đây là email của chính bạn, chủ chuyến đi không cần được mời", không có khung đỏ nào khác.
- [x] Trên chuyến mà C đã nhận lời, mời email của C: dưới ô email hiện "Người này đã là thành viên của chuyến đi".
- [x] Tắt backend rồi bấm "Mời" với một email mới: khung đỏ "Không kết nối được máy chủ, vui lòng thử lại" trên các ô, hộp không đóng.
- [x] Bật lại backend, mời một email mới và nhấn Esc ngay khi nút đang quay: hộp **không** đóng; khi xong mới đóng được.
- [x] B (chỉ xem) và một EDITOR mở hộp "Thành viên": **không** có mục "Mời thành viên".
- [x] 375px: ô email, ô vai trò, nút "Mời" xếp dọc, không tràn mép.

**Kết quả:** Đạt · **Ngày:** 2026-10-18 · **Ghi chú:** chủ dự án tự chạy

### MT-SHAREUI-07 · Nhận lời mời từ link trong mail (Mốc 7)

A chủ một chuyến; B và D có tài khoản đã xác thực. A mời B ("Chỉ xem") trong hộp "Chia sẻ". Mở mail trong MailHog, chép link của nút trong mail (`http://localhost:5173/invite?trip=...&token=...`).

- [x] Đăng xuất hết, dán link vào trình duyệt: sang trang đăng nhập. Đăng nhập B: quay lại `/invite`, thoáng thấy logo và thẻ "Đang nhận lời mời…", rồi vào thẳng trang chuyến đi của A, toast "Đã nhận lời mời tham gia chuyến đi". Đầu trang có huy hiệu "Chỉ xem".
- [x] Vào `/trips` của B: chuyến của A có "Được chia sẻ". Ở tài khoản A, hộp "Chia sẻ": dòng B không còn "Đang chờ" (tải lại trang).
- [x] B mở lại đúng link đó: vào thẳng chuyến đi, toast "Bạn đã là thành viên của chuyến đi này".
- [x] A mời D, nhưng mở link của D khi đang đăng nhập B: thẻ "Lời mời này dành cho email khác", dòng "Bạn đang đăng nhập bằng {email của B}.". Bấm "Đăng xuất để đổi tài khoản": sang trang đăng nhập; đăng nhập D thì quay lại và nhận lời thành công.
- [x] A mời lại một email đang chờ (mail thứ hai), rồi mở link của mail **thứ nhất**: thẻ "Lời mời không còn hiệu lực" với nút "Về danh sách chuyến đi" (bấm thì về `/trips`).
- [x] Sửa tay link: xoá vài ký tự cuối của token, hoặc đổi `trip=` thành chữ: thẻ "Lời mời không còn hiệu lực" hiện ngay; DevTools → Network không có request `members/accept`.
- [x] A xoá chuyến đi rồi người được mời mở link: "Lời mời không còn hiệu lực".
- [x] Tắt backend, mở một link còn hiệu lực: thẻ "Chưa nhận được lời mời" với câu lỗi kết nối và nút "Thử lại". Bật lại backend, bấm "Thử lại": nhận lời thành công.
- [x] DevTools → Network khi mở một link: chỉ **một** request `POST .../members/accept` (không gửi hai lần).
- [x] 375px: thẻ vừa màn hình, không tràn.

**Kết quả:** Đạt · **Ngày:** 2026-10-18 · **Ghi chú:** chủ dự án tự chạy

### MT-SHAREUI-08 · Đăng nhập, đăng ký khi mở link mời (Mốc 8)

A mời một email **chưa có tài khoản** (E) và một email có tài khoản (B). Chép link mời của cả hai từ MailHog. Đăng xuất trước mỗi bước.

- [x] Mở link của B: trang đăng nhập có khung xanh dương "Đăng nhập bằng đúng email đã nhận lời mời để tham gia chuyến đi. Chưa có tài khoản thì đăng ký bằng email đó." trên form. Đăng nhập B: về `/invite` và nhận lời thành công.
- [x] Mở link của B, bấm "Đăng ký" ở dưới khung rồi bấm "Đăng nhập" để quay lại: khung hướng dẫn vẫn còn; đăng nhập B vẫn về `/invite`.
- [x] Mở link của E, bấm "Đăng ký": trang đăng ký có khung "Đăng ký bằng đúng email đã nhận lời mời. Xác thực email xong, mở lại link trong mail mời để tham gia chuyến đi.". Đăng ký E: trang "Kiểm tra hộp thư" có thêm khung "Xác thực xong, mở lại link trong mail mời để tham gia chuyến đi.".
- [x] Xác thực E bằng mail trong MailHog (mở ở tab khác), đăng nhập ở tab đó: vào `/trips` (chưa có chuyến của A). Mở lại link mời của E: nhận lời thành công, vào chuyến của A.
- [x] Làm lại bước trên nhưng sau khi xác thực, quay về **tab cũ** đang ở "Kiểm tra hộp thư" và bấm "Đăng nhập" ở dưới khung: đăng nhập E xong về thẳng `/invite` và nhận lời.
- [x] Mở `/login` trực tiếp (không qua link mời) hoặc bị đưa về đăng nhập từ một trang chuyến đi: **không** có khung hướng dẫn; đăng nhập như trước.

**Kết quả:** Đạt · **Ngày:** 2026-10-18 · **Ghi chú:** chủ dự án tự chạy

### MT-SHAREUI-09 · Gửi lại lời mời (Mốc 9)

A chủ một chuyến; A mời email E (chưa nhận lời) trong hộp "Chia sẻ". MailHog mở ở `http://localhost:8025`.

- [x] A mở "Chia sẻ": dòng E có "Đang chờ", huy hiệu vai trò, nút chữ "Gửi lại"; dưới email có dòng xám "Đã mời {ngày hôm nay}" (ví dụ "Đã mời 18/10").
- [x] Bấm "Gửi lại": nút quay vòng rồi toast "Đã gửi lại lời mời tới {E}"; MailHog có mail mời mới. Mở link của mail **cũ**: "Lời mời không còn hiệu lực". Link của mail mới vẫn nhận lời được.
- [x] Trong lúc nút "Gửi lại" đang quay, nhấn Esc: hộp không đóng.
- [x] Để hai tab: tab 1 A mở hộp "Chia sẻ"; tab 2 E nhận lời bằng link mới. Quay lại tab 1 (không tải lại), bấm "Gửi lại" trên dòng E: toast đỏ "Không gửi lại được lời mời: Người này đã là thành viên của chuyến đi"; danh sách tải lại, dòng E hết "Đang chờ" và không còn nút "Gửi lại".
- [x] B (chỉ xem) mở hộp "Thành viên": dòng đang chờ vẫn có "Đã mời …" nhưng **không** có nút "Gửi lại". Dòng đã nhận lời không có dòng "Đã mời …".

**Kết quả:** Đạt · **Ngày:** 2026-10-18 · **Ghi chú:** chủ dự án tự chạy

### MT-SHAREUI-10 · Đổi vai trò thành viên (Mốc 10)

A chủ một chuyến; B đã nhận lời với vai trò "Chỉ xem"; email E đang chờ với vai trò "Chỉnh sửa".

- [x] A mở "Chia sẻ": dòng của A vẫn là huy hiệu "Chủ sở hữu"; dòng B và dòng E có ô chọn nhỏ (B: "Chỉ xem", E: "Chỉnh sửa") thay cho huy hiệu vai trò.
- [x] Đổi B sang "Chỉnh sửa": ô bị khoá thoáng qua, toast "Đã đổi vai trò", ô giữ "Chỉnh sửa". Đóng rồi mở lại hộp: vẫn "Chỉnh sửa".
- [x] Ở tài khoản B, tải lại trang chuyến đi: huy hiệu "Chỉnh sửa", có nút "Sửa", "Thêm hoạt động", kéo thả được.
- [x] Đổi E sang "Chỉ xem", rồi E nhận lời bằng link trong mail: E vào chuyến với huy hiệu "Chỉ xem".
- [x] Dùng bàn phím: Tab tới ô chọn của một dòng, có vòng focus xanh; trình đọc màn hình đọc "Vai trò của {tên}".
- [x] Hai tab: tab 1 A mở hộp "Chia sẻ"; tab 2 A gỡ B qua Swagger (`DELETE /members/{memberId}`). Ở tab 1 đổi vai trò B: toast đỏ "Không đổi được vai trò: …", danh sách tải lại và B biến mất.
- [x] B hoặc E (không phải chủ) mở hộp "Thành viên": chỉ có huy hiệu, không có ô chọn nào.
- [x] 375px và màn hình cảm ứng: ô chọn cao vừa ngón tay, dòng không tràn mép.

**Kết quả:** Đạt · **Ngày:** 2026-10-18 · **Ghi chú:** chủ dự án tự chạy

### MT-SHAREUI-11 · Gỡ thành viên, huỷ lời mời (Mốc 11)

A chủ một chuyến; B đã nhận lời; email E đang chờ.

- [x] A mở "Chia sẻ": dòng B có nút "Gỡ", dòng E có nút "Huỷ lời mời" (cạnh "Gửi lại"); dòng của A không có nút nào.
- [x] Bấm "Gỡ" trên dòng B: hộp "Gỡ thành viên?" mở **chồng lên** hộp chia sẻ, câu "{tên B} sẽ không còn xem được chuyến đi này. Bạn có thể mời lại sau.", con trỏ ở nút "Huỷ". Bấm "Huỷ": chỉ hộp hỏi đóng, B còn nguyên.
- [x] Mở lại, nhấn **Esc**: chỉ hộp hỏi đóng, hộp chia sẻ **vẫn mở** (`BUG-UI-011`). Làm thêm hai chỗ cũ: hộp "Trùng giờ với hoạt động khác" trong form thêm hoạt động (thêm 09:30–10:30 khi đã có 09:00–10:00), và hộp hỏi lại khi rút ngắn chuyến đi có hoạt động ở ngày bị cắt trong "Sửa chuyến đi": Esc chỉ đóng hộp hỏi, form bên dưới giữ nguyên chữ đã gõ.
- [x] Mở lại, bấm nút đỏ "Gỡ thành viên": toast "Đã gỡ {tên B}", B biến khỏi danh sách. Ở tài khoản B, tải lại trang chuyến đi: "Bạn không có quyền xem chuyến đi này."; ở `/trips` của B không còn chuyến đó.
- [x] Bấm "Huỷ lời mời" trên dòng E: hộp "Huỷ lời mời?" với nút "Giữ lời mời" / "Huỷ lời mời". Xác nhận: toast "Đã huỷ lời mời gửi tới {E}". Mở link trong mail của E: "Lời mời không còn hiệu lực".
- [x] Mời lại email của B: thành công, B quay lại danh sách với "Đang chờ" (dòng cũ được mở lại, không lỗi trùng).
- [x] Hai tab: tab 1 A mở hộp hỏi gỡ B; tab 2 A gỡ B trước. Ở tab 1 bấm "Gỡ thành viên": lỗi hiện trong hộp hỏi (không tìm thấy), hộp chưa đóng.
- [x] B (không phải chủ) mở hộp "Thành viên": không có nút "Gỡ" / "Huỷ lời mời".

**Kết quả:** Đạt · **Ngày:** 2026-10-18 · **Ghi chú:** chủ dự án tự chạy; kiểm lại luôn `BUG-UI-011`

### MT-SHAREUI-12 · Xem và sao chép liên kết chia sẻ (Mốc 12)

Chưa có nút "Tạo liên kết" (Mốc 13): tạo bằng Swagger, đăng nhập A (chủ chuyến). `POST /api/v1/trips/{tripId}/share-links` ba lần: không có body (không hết hạn); body `{"expiresAt": "<ngày mai, ví dụ 2026-10-19T16:59:59Z>"}`; body với thời điểm **2 phút sau** bây giờ (giờ quốc tế, đuôi `Z`), rồi chờ 2 phút cho liên kết này hết hạn.

- [x] A mở "Chia sẻ": dưới danh sách thành viên có vạch ngăn, tiêu đề "Liên kết chia sẻ" và câu "Ai có liên kết đều xem được chuyến đi, không cần tài khoản."; ba khung liên kết, mới tạo nhất ở trên.
- [x] Mỗi khung có ô địa chỉ `http://localhost:5173/share/` + 32 ký tự; bấm vào ô thì cả địa chỉ được chọn; không gõ sửa được.
- [x] Liên kết không hạn: "0 lượt xem · Không hết hạn". Liên kết hạn ngày mai: "· Dùng được đến hết ngày {ngày mai}" (ví dụ `2026-10-19T16:59:59Z` là 23:59 ngày 19/10 giờ Việt Nam → "19/10/2026").
- [x] Liên kết đã hết hạn: huy hiệu xám "Hết hạn", "· Hết hạn ngày {hôm nay}", chữ địa chỉ nhạt, nút "Sao chép" bị khoá.
- [x] Bấm "Sao chép" ở liên kết không hạn: toast "Đã sao chép liên kết"; dán vào thanh địa chỉ của một tab khác được đúng địa chỉ (trang công khai làm ở Mốc 16, hiện tại địa chỉ này đưa về trang chính).
- [x] Gọi `GET /api/v1/public/trips/{token}` của liên kết đó hai lần bằng Swagger (không cần đăng nhập), đóng rồi mở lại hộp "Chia sẻ": "2 lượt xem".
- [x] Thu hồi một liên kết bằng Swagger (`DELETE /share-links/{linkId}`), mở lại hộp: liên kết đó biến mất.
- [x] Chuyến chưa có liên kết nào: "Chưa có liên kết nào.".
- [x] Tắt backend, mở hộp ở một chuyến chưa mở lần nào: mục liên kết báo lỗi kèm "Thử lại"; bật lại backend, bấm "Thử lại": danh sách hiện.
- [x] B hoặc C (không phải chủ) mở hộp "Thành viên": **không** có mục "Liên kết chia sẻ".
- [x] 375px: ô địa chỉ co lại, nút "Sao chép" không tràn mép; địa chỉ dài cuộn ngang được trong ô.

**Kết quả:** Đạt · **Ngày:** 2026-10-18 · **Ghi chú:** chủ dự án tự chạy

### MT-SHAREUI-13 · Tiêu đề hộp thoại đứng yên khi nội dung cuộn (sau Mốc 12)

Thu cửa sổ trình duyệt cho thấp lại (khoảng 600px chiều cao) để nội dung dài hơn hộp.

- [x] Chủ chuyến mở "Chia sẻ" ở chuyến có vài thành viên và vài liên kết: cuộn trong hộp thì chỉ phần dưới tiêu đề cuộn; "Chia sẻ chuyến đi" và nút "×" luôn ở trên cùng; không có thanh cuộn của cả trang phía sau.
- [x] Mở hộp "Thêm hoạt động" (form dài có ô tìm địa điểm): cuộn xuống tới nút "Thêm hoạt động", tiêu đề vẫn ở trên; danh sách gợi ý của ô tìm địa điểm vẫn hiện đầy đủ khi gõ.
- [x] Mở "Sửa chuyến đi" (có bản đồ chọn vị trí): cuộn được tới nút "Lưu thay đổi"; bản đồ hiện đầy đủ, không có vùng xám.
- [x] Hộp ngắn (hộp hỏi "Xoá hoạt động?"): trông như trước, không có thanh cuộn thừa; khoảng cách tiêu đề → nội dung không đổi.
- [x] Vòng focus của ô nhập sát mép trái / phải (ô email trong "Chia sẻ") không bị cắt.
- [x] 375px: hộp nằm sát mép dưới như trước, cao tối đa 90% màn hình, tiêu đề đứng yên khi cuộn.
- [x] Đóng hộp rồi mở lại: hộp không còn hiện khi đã đóng (không có khung trắng sót lại trên trang).

**Kết quả:** Đạt · **Ngày:** 2026-10-18 · **Ghi chú:** chủ dự án tự chạy

### MT-SHAREUI-14 · Tạo liên kết chia sẻ (Mốc 13)

A chủ một chuyến chưa có liên kết nào. Swagger mở sẵn để xem kết quả.

- [x] A mở "Chia sẻ": dưới câu "Ai có liên kết…" có ô "Hết hạn sau ngày" (gợi ý "Bỏ trống: liên kết không hết hạn") và nút viền trắng "Tạo liên kết"; nút xanh duy nhất của hộp vẫn là "Mời".
- [x] Để trống ngày, bấm "Tạo liên kết": toast "Đã tạo liên kết", "Chưa có liên kết nào." biến mất, liên kết mới ghi "0 lượt xem · Không hết hạn".
- [x] Chọn ngày mai, bấm "Tạo liên kết": liên kết mới ở **đầu** danh sách, ghi "· Dùng được đến hết ngày {ngày mai}"; ô ngày trống lại. Swagger `GET /share-links`: `expiresAt` của nó là `{ngày mai}T16:59:59.999Z` (23:59:59 giờ Việt Nam).
- [x] Chọn **hôm nay**: tạo được, ghi "Dùng được đến hết ngày {hôm nay}", chưa có nhãn "Hết hạn".
- [x] Ô ngày không cho chọn ngày trước hôm nay trên lịch; gõ tay một ngày đã qua rồi bấm "Tạo liên kết": lỗi "Ngày hết hạn phải từ hôm nay trở đi" dưới ô, không gửi gì.
- [x] Tắt backend, bấm "Tạo liên kết": khung đỏ "Không kết nối được máy chủ…" trên form; hộp không đóng bằng Esc trong lúc nút đang quay.
- [x] Bấm "Sao chép" ở liên kết vừa tạo: chép được đúng địa chỉ.
- [x] 375px: ô ngày và nút xếp dọc, không tràn mép.

**Kết quả:** Đạt · **Ngày:** 2026-10-18 · **Ghi chú:** chủ dự án tự chạy

### MT-SHAREUI-15 · Thu hồi liên kết chia sẻ (Mốc 14)

A chủ một chuyến có hai liên kết còn dùng được và một liên kết đã hết hạn (tạo như `MT-SHAREUI-12`).

- [x] A mở "Chia sẻ": mỗi liên kết, kể cả liên kết hết hạn, có nút viền đỏ "Thu hồi" ở cuối dòng lượt xem.
- [x] Bấm "Thu hồi" ở một liên kết: hộp "Thu hồi liên kết?" mở chồng lên hộp chia sẻ, con trỏ ở nút "Huỷ". Nhấn Esc: chỉ hộp hỏi đóng, hộp chia sẻ còn nguyên.
- [x] Bấm lại "Thu hồi", rồi nút đỏ "Thu hồi liên kết": toast "Đã thu hồi liên kết", liên kết đó biến khỏi danh sách. Gọi `GET /api/v1/public/trips/{token}` của nó bằng Swagger: 404.
- [x] Thu hồi nốt các liên kết còn lại (cả liên kết hết hạn): danh sách ghi "Chưa có liên kết nào."; Swagger `GET /trips/{id}`: `visibility` là `PRIVATE`.
- [x] Hai tab: tab 1 A mở hộp hỏi thu hồi một liên kết; tab 2 A thu hồi đúng liên kết đó bằng Swagger. Ở tab 1 bấm "Thu hồi liên kết": lỗi "không tìm thấy" hiện trong hộp hỏi, hộp chưa đóng; bấm "Huỷ", mở lại hộp chia sẻ thì liên kết đó đã biến mất.
- [x] 375px: nút "Thu hồi" xuống dòng dưới chữ lượt xem nếu thiếu chỗ, không tràn mép.

**Kết quả:** Đạt · **Ngày:** 2026-10-18 · **Ghi chú:** chủ dự án tự chạy

### MT-SHAREUI-16 · Thẻ hoạt động không đổi sau khi tách (Mốc 15)

So với trước Mốc 15, trang chuyến đi của chủ phải **giống hệt**. Dùng một ngày có đủ kiểu hoạt động.

- [x] Thẻ có giờ đầy đủ, chỉ giờ bắt đầu, và không giờ ("Chưa đặt giờ"); chip loại có màu và icon như cũ.
- [x] Thẻ có địa điểm (tên khác tên hoạt động: "Tên · địa chỉ"; tên trùng: chỉ địa chỉ) và thẻ không có địa điểm (không có hàng địa điểm).
- [x] Thẻ có chi phí, có link đặt chỗ, có cả hai, và không có cả hai (không có vạch chân thẻ).
- [x] Ghi chú dài có "Đọc thêm"; thẻ trùng giờ có nền vàng nhạt và tam giác cảnh báo.
- [x] Lề trái của chữ trong thẻ khi có tay kéo (chủ) và khi không có (người chỉ xem, Mốc 4) như trước; menu "⋮", rê chuột làm marker phóng to, bấm marker cuộn tới thẻ vẫn chạy.

**Kết quả:** Đạt · **Ngày:** 2026-10-18 · **Ghi chú:** chủ dự án tự chạy

### MT-SHAREUI-17 · Trang chuyến đi công khai (Mốc 16)

A chủ một chuyến 3–4 ngày: có mô tả, một ngày có tiêu đề, vài hoạt động có giờ / không giờ / có địa điểm / có ghi chú / có chi phí và link đặt chỗ, một ngày trống. A tạo một liên kết trong hộp "Chia sẻ" và sao chép.

- [x] Mở liên kết trong cửa sổ **ẩn danh** (chưa đăng nhập): không có thanh điều hướng; logo nhỏ góc trên trái; tên chuyến, dòng điểm đến / ngày đi / số ngày, mô tả có "Đọc thêm". Không có ngân sách, trạng thái, tên A, thành viên.
- [x] Mọi ngày nằm trên một trang theo thứ tự: "Ngày 1 · {thứ}, dd/mm", tiêu đề của ngày (nếu có) màu xanh có icon cờ, rồi ray thời gian với chấm màu theo loại và thẻ trắng: giờ, chip loại, tên, địa điểm, ghi chú. **Không** có chi phí, link đặt chỗ, tay kéo, menu "⋮", nút thêm.
- [x] Ngày trống: khung nét đứt "Ngày này chưa có hoạt động.". Ghi chú của ngày (A có viết) **không** hiện.
- [x] Cuối trang: nút viền trắng "Tạo chuyến đi của bạn", dòng "Được chia sẻ qua Smart Trip Planner", dòng "Bản đồ và địa điểm © OpenStreetMap" (bấm mở tab mới trang bản quyền OSM). Bấm "Tạo chuyến đi của bạn" khi chưa đăng nhập: sang trang đăng nhập.
- [x] Lượt xem: sau khi mở trang, ở tài khoản A mở hộp "Chia sẻ" (đóng rồi mở lại để tải mới): "1 lượt xem". Trong tab ẩn danh, chuyển sang tab khác rồi quay lại vài lần: lượt xem **không** tăng thêm. Tải lại trang (F5) mới tăng thêm 1.
- [x] DevTools → Network khi mở trang: đúng **một** request `GET /api/v1/public/trips/...`.
- [x] Mở cùng liên kết khi đang đăng nhập (A hoặc B): trang giống hệt, vẫn không có thanh điều hướng.
- [x] A thu hồi liên kết, tải lại trang công khai: "Liên kết không còn hiệu lực" với nút "Tạo chuyến đi của bạn". Làm tương tự với liên kết hết hạn và với chuyến A đã xoá.
- [x] Sửa tay địa chỉ: bỏ một ký tự cuối, hoặc viết hoa một chữ: "Liên kết không còn hiệu lực" ngay, Network **không** có request tới `/public/trips`.
- [x] Tắt backend, mở một liên kết còn dùng được: "Chưa tải được chuyến đi" với câu lỗi và "Thử lại"; bật lại backend, bấm "Thử lại": trang hiện.
- [x] 375px: một cột vừa màn hình, thẻ không tràn, tên chuyến dài xuống dòng; không có thanh cuộn ngang.

**Kết quả:** Đạt · **Ngày:** 2026-10-18 · **Ghi chú:** chủ dự án tự chạy

### MT-SHAREUI-18 · Bản đồ trên trang công khai (Mốc 17)

Dùng chuyến của `MT-SHAREUI-17` (vài ngày, có hoạt động gắn địa điểm ở hơn một ngày) và một chuyến khác không có địa điểm nào; mỗi chuyến một liên kết.

- [x] Mở liên kết của chuyến có địa điểm: ngay dưới phần tóm tắt có thẻ "Bản đồ chuyến đi" với chip "N địa điểm" (N = số hoạt động có địa điểm của cả chuyến); bản đồ cao vừa phải, đóng khung vừa mọi marker.
- [x] Marker giọt nước màu theo loại, **không có số**; **không có** đường nét đứt nối các điểm.
- [x] Rê chuột lên một marker: hiện "Ngày 2 · {tên hoạt động}". Bấm marker: ô nhỏ hiện cùng chữ, **không có** nút "Xem trong lịch trình"; bấm "×" của ô hoặc ra ngoài thì ô đóng.
- [x] Lăn chuột khi con trỏ nằm trên bản đồ: **trang cuộn**, bản đồ không phóng to / thu nhỏ. Nút + / − vẫn phóng được; kéo bản đồ được.
- [x] Góc dưới phải bản đồ có dòng ghi nguồn "Leaflet | © OpenStreetMap contributors".
- [x] Cuộn trang qua bản đồ: bản đồ không đè lên chữ hay thẻ nào, không dính lại khi cuộn.
- [x] Mở liên kết của chuyến không có địa điểm nào: **không có** thẻ bản đồ, phần tóm tắt nối thẳng xuống Ngày 1.
- [x] Trong ứng dụng (tài khoản A), bản đồ của ngày **vẫn như cũ**: có số, có đường nối, bấm marker cuộn tới thẻ, lăn chuột phóng bản đồ.
- [x] 375px trên điện thoại (hoặc giả lập có cảm ứng): marker to hơn (36px), chạm marker mở ô tên; bản đồ rộng hết cột.

**Kết quả:** Đạt · **Ngày:** 2026-10-18 · **Ghi chú:** chủ dự án tự chạy

## Lỗi đã phát hiện

Chưa có lỗi của riêng file này: 18 bài thủ công đều đạt ở lần chạy đầu. `BUG-UI-011` (Esc trên hộp hỏi lại đóng luôn hộp bên ngoài; đã sửa, chủ dự án kiểm lại ngày 2026-10-18 trong `MT-SHAREUI-11`) thuộc thành phần hộp thoại dùng chung nên ghi ở [06-itinerary-ui.md](06-itinerary-ui.md).

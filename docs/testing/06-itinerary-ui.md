# 06 · Giao diện lịch trình

> Cập nhật: 2026-10-05 · **Task 3.7 xong: 16 commit (`8ba6710` đến `6955238`), lint, build và 108 test tự động xanh; 15 bài `MT-UI-63` đến `MT-UI-77` chưa chạy đủ; `BUG-UI-010` đã sửa, chờ kiểm lại.** Diễn biến: Commit 1 (`8ba6710`) thêm công cụ test tự động cho giao diện (Vitest), 7 test xanh; Commit 2 (`847cda9`) thêm dải thời tiết dưới bản đồ, Commit 3 (`d32e248`) thêm câu mời đặt vị trí điểm đến, Commit 4 (`a7f818c`) thêm báo lỗi khi dự báo không tải được, Commit 5 (`f1e9f04`) thêm dòng thời tiết ở tiêu đề ngày trên màn hẹp, Commit 6 (`f9ff894`) thêm thời gian và quãng đường giữa các hoạt động (33 test tự động xanh), Commit 7 (`8408ac0`) tính lại quãng đường sau khi ngày thay đổi, Commit 8 (`e8511be`) hiện chặng đi qua hoạt động không có địa điểm, Commit 9 (`51abea7`) thêm nhãn "Đã qua" / "Hôm nay", Commit 10 (`e767e62`) mở chuyến đi đang diễn ra ở ngày hôm nay, Commit 11 (`584df0f`) thêm thời tiết trên thẻ ở trang danh sách, Commit 12 (`d221461`) hỏi hoàn thành chuyến đi đã qua ngày cuối, Commit 13 (`d8ba1ee`) nhớ câu trả lời "Để sau". Chủ dự án xem dải thời tiết trên trình duyệt ngày 2026-10-05 và yêu cầu hai thay đổi (Commit 14, 15); Commit 14 (`4a736be`) bỏ khỏi dải các ngày không có dự báo, Commit 15 (`40bdd35`) thay cuộn ngang bằng nút chuyển trang (108 test tự động xanh). Chủ dự án tìm ra `BUG-UI-010` (tên ngày ở cột giữa bị xuống dòng), sửa ở Commit 16, chờ kiểm lại; các bài `MT-UI-63` đến `MT-UI-77` chưa có kết quả đầy đủ · Task 3.6 xong: 21 commit (`2810b64` đến `e4bc2d0`), lint và build xanh; BUG-UI-009 đã sửa và được chủ dự án xác nhận; 18 bài `MT-UI-45` đến `MT-UI-62` chưa chạy đủ · Task 2.5 đã merge (`eb1ad5e`) · Task 2.6 (làm lại giao diện theo `UI_GUIDE.md`) đã merge (`8f06d72`) · kiểm tra thủ công chưa chạy · Task 2.7: BUG-UI-003 đã sửa và được chủ dự án xác nhận; BUG-UI-004 đến BUG-UI-008 đã sửa chờ kiểm lại · [Về trang chính](README.md)

Giao diện web để người dùng xem danh sách chuyến đi, tạo chuyến đi, xem và sửa lịch trình từng ngày, thêm hoạt động và kéo thả để sắp xếp lại. Làm ở Task 2.5.

Tới hết Task 3.6, phần giao diện chưa có test tự động. Từ Task 3.7 có `npm run test` cho các **hàm tính toán** (không vẽ gì lên màn hình); phần nhìn thấy được vẫn do các bài kiểm tra thủ công đảm nhận. Mỗi mốc chỉ được commit khi các lệnh sau chạy xanh:

| Lệnh | Kiểm tra gì |
|---|---|
| `npm run lint` | Mã nguồn viết đúng quy tắc, ví dụ không có biến thừa, không gọi hook React sai chỗ |
| `npm run build` | Đúng kiểu dữ liệu trên toàn bộ mã nguồn, và đóng gói được thành bản chạy thật |
| `npm run test` (từ Task 3.7) | Các hàm tính toán của giao diện cho đúng kết quả: xem mục "Test tự động của giao diện" |

Hai lệnh đầu không kiểm tra giao diện có làm đúng nghiệp vụ hay không; lệnh thứ ba chỉ kiểm phần tính toán. Phần còn lại do các bài kiểm tra thủ công bên dưới đảm nhận.

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

Task 3.6 (`feat/T3.6-place-map-ui`): địa điểm của hoạt động và bản đồ. Mỗi commit một việc.

| Commit | Nội dung | Kiểm tra thủ công | Mã commit |
|---|---|---|---|
| 1 | Thẻ hoạt động hiện địa điểm ngay dưới tên, có icon ghim | MT-UI-45 | `2810b64` |
| 2 | Tách phần ô nhập của hộp thoại hoạt động ra file riêng, chuẩn bị thêm ô địa điểm. Không đổi hành vi, không có bài mới | dùng lại MT-UI-20, MT-UI-21, MT-UI-22 | `4d026f3` |
| 3 | Tìm và chọn địa điểm trong hộp thoại hoạt động: gợi ý khi đang gõ, khung địa điểm đã chọn, tự điền tên hoạt động còn trống | MT-UI-46 | `c7e52e1` |
| 4 | Ô tìm địa điểm hiện 5 gợi ý đầu, "Xem tất cả" mở đủ kết quả (tối đa 20) trong khung có thanh cuộn | MT-UI-47 | `23d4b60` |
| 5 | Bỏ địa điểm của hoạt động: bấm "×" rồi lưu. Đóng điểm hở tạm thời của Commit 3 | MT-UI-48 | `311edac` |
| 6 | Ô "Loại" của form hoạt động thành 6 nút có icon và màu của từng loại | MT-UI-49 | `72fec35` |
| 7 | Thêm hoạt động: chọn địa điểm thì loại tự đổi theo nhóm của địa điểm, khi người dùng chưa tự chọn loại | MT-UI-50 | `f7acd99` |
| 8 | Điểm đến của chuyến đi có vị trí: chọn bằng ô tìm địa điểm ở wizard và hộp sửa chuyến đi | MT-UI-51 | `b9448dd` |
| 9 | Trong form hoạt động, gợi ý địa điểm quanh điểm đến của chuyến đi đứng trước | MT-UI-52 | `5c9033b` |
| 10 | Cột bản đồ trên trang chi tiết: nền OpenStreetMap làm nhạt, marker giọt nước theo màu loại hoạt động kèm số thứ tự | MT-UI-53 | `11cc83d` |
| 11 | Đường nét đứt nối các địa điểm của ngày theo thứ tự | MT-UI-54 | `967f606` |
| 12 | Ngày chưa có địa điểm: bản đồ mở ở điểm đến của chuyến đi, kèm thẻ hướng dẫn | MT-UI-55 | `3e3404e` |
| 13 | Nút phóng bản đồ của ngày ra cả cửa sổ; Esc hoặc "×" để thu lại | MT-UI-56 | `1bfd189` |
| 14 | Rê chuột hoặc Tab vào một thẻ hoạt động thì marker của nó trên bản đồ to lên và có vòng sáng | MT-UI-57 | `c04a0f9` |
| 15 | Bấm marker để tới thẻ hoạt động: cuộn thẳng tới thẻ khi dùng chuột; ô tên có nút "Xem trong lịch trình" trên màn cảm ứng và bản đồ phóng to | MT-UI-58 | `487d06f` |
| 16 | Màn hình hẹp: hai nút "Lịch trình" / "Bản đồ" dưới dải chip ngày, mỗi lúc hiện một trong hai | MT-UI-59 | `0d3e494` |
| 17 | Vùng chạm đủ 44px trên màn hình cảm ứng: nút "⋮", chip ngày, các nút "×"; marker 36px (nợ ghi từ Task 2.7) | MT-UI-60 | `2977345` |
| 18 | "Không tìm thấy? Tự thêm địa điểm": nhập tên và bấm lên bản đồ nhỏ để đặt vị trí | MT-UI-61 | `67ab468` |
| 19 | Bản đồ nhỏ ở ô điểm đến (wizard và hộp sửa chuyến đi): hiện vị trí, bấm để đặt hoặc dời | MT-UI-62 | `72e97d5` |
| 20 | Bản đồ phóng to hiện đúng (`BUG-UI-009`) | MT-UI-56, bước 2 | `c712093` |
| 21 | Nền bản đồ giữ màu gốc của OpenStreetMap, không còn bị làm xám | MT-UI-53 (bước về màu nền) | `e4bc2d0` |

Task 3.7 (`feat/T3.7-weather-route-ui`): thời tiết, quãng đường di chuyển, ngày đã qua. Mỗi commit một việc.

| Commit | Nội dung | Kiểm tra | Mã commit |
|---|---|---|---|
| 1 | Thêm công cụ test tự động cho giao diện (Vitest, lệnh `npm run test`). Người dùng chưa thấy gì mới. Bài test đầu tiên kiểm cách tính vị trí của một hoạt động vừa được kéo thả, phần đã có từ Task 2.5 | TC-UI-001 đến TC-UI-007 | `8ba6710` |
| 2 | Dải thời tiết dưới bản đồ của trang chi tiết: mỗi ngày một ô (icon có màu, nhiệt độ cao / thấp, khả năng mưa), bấm một ô để sang ngày đó; ngày không có dự báo ghi "Chưa có" | MT-UI-63 | `847cda9` |
| 3 | Chuyến đi chưa đặt vị trí điểm đến: khung thời tiết mời đặt vị trí. Sửa chuyến đi (điểm đến, ngày đi) xong thì dự báo tự cập nhật | MT-UI-64 | `d32e248` |
| 4 | Dự báo không tải được: khung thời tiết ghi "Tạm thời không có dự báo." kèm "Thử lại"; phần còn lại của trang vẫn dùng được; dự báo đã tải trước đó được giữ lại | MT-UI-65 | `a7f818c` |
| 5 | Dưới 1024px: dưới tiêu đề ngày có một dòng thời tiết của ngày đang xem (tình trạng, nhiệt độ, khả năng mưa); ngày không có dự báo thì không có dòng này | MT-UI-66 | `f1e9f04` |
| 6 | Giữa hai thẻ hoạt động liền nhau cùng có địa điểm có dòng "25 phút · 8,4 km"; ẩn khi đang kéo | TC-UI-008 đến TC-UI-019, MT-UI-67 | `f9ff894` |
| 7 | Sau khi kéo thả, chuyển ngày, thêm, sửa, xoá hoạt động hoặc đổi địa điểm, quãng đường được tính lại cho cả ngày đi lẫn ngày đến. Đóng điểm hở tạm thời của Commit 6 | MT-UI-68 | `8408ac0` |
| 8 | Chặng đi qua một hoạt động không có địa điểm hiện dưới thẻ xuất phát, ghi kèm tên đích: "12 phút · 3,2 km tới Cầu Rồng" | TC-UI-020 đến TC-UI-022, MT-UI-69 | `e8511be` |
| 9 | Ngày đã qua và ngày hôm nay được đánh dấu ở cột ngày, chip ngày và tiêu đề ngày; "hôm nay" tính theo múi giờ của tài khoản | TC-UI-023 đến TC-UI-029, MT-UI-70 | `51abea7` |
| 10 | Mở một chuyến đi đang diễn ra (từ thẻ ở trang danh sách) thì vào thẳng ngày hôm nay thay vì Ngày 1 | TC-UI-030 đến TC-UI-033, MT-UI-71 | `e767e62` |
| 11 | Thẻ ở trang danh sách có thời tiết ở chân thẻ: "Hôm nay" cho chuyến đi đang diễn ra, "Ngày đi" cho chuyến đi sắp bắt đầu trong 16 ngày tới | TC-UI-034 đến TC-UI-040, MT-UI-72 | `584df0f` |
| 12 | Mở một chuyến đi đã qua ngày cuối mà trạng thái chưa đóng: hộp hỏi "Hoàn thành chuyến đi?"; đồng ý thì trạng thái thành "Đã hoàn thành", lịch trình vẫn sửa được | TC-UI-041 đến TC-UI-045, MT-UI-73 | `d221461` |
| 13 | Bấm "Để sau" thì không bị hỏi lại về chuyến đi đó cho tới lần đăng nhập sau, kể cả khi tải lại trang. Đóng điểm hở tạm thời của Commit 12 | TC-UI-046 đến TC-UI-055, MT-UI-74 | `d8ba1ee` |
| 14 | Dải thời tiết chỉ còn các ngày có dự báo: ngày đã qua và ngày xa hơn 16 ngày không có ô; không còn ngày nào thì khung ghi một câu giải thích (yêu cầu của chủ dự án sau khi xem trên trình duyệt) | TC-UI-056 đến TC-UI-062, MT-UI-75; MT-UI-63 sửa theo | `4a736be` |
| 15 | Dải thời tiết không còn cuộn ngang: nhiều ngày hơn chỗ chứa thì chia trang, hai nút ở hai đầu lật mỗi lần một trang; mở một ngày thì dải tự lật tới trang có ngày đó (yêu cầu của chủ dự án) | TC-UI-063 đến TC-UI-069, MT-UI-76; MT-UI-63 sửa theo | `40bdd35` |
| 16 | Tên ngày ở cột giữa đứng riêng một hàng, không còn bị các nút ép xuống dòng (`BUG-UI-010`) | MT-UI-77; MT-UI-70 chạy lại | `6955238` |

---

## Test tự động của giao diện

Chạy bằng `npm run test` trong thư mục `frontend`. Chỉ kiểm các hàm tính toán: đưa dữ liệu vào, so kết quả trả về. Không mở trình duyệt, không gọi máy chủ.

### A. Vị trí của hoạt động sau khi kéo thả

> **Yêu cầu:** design.md rule 14.5, 10.2 "Quy ước Reorder" · **Kiểm bởi:** `lib/orderIndex.test.ts`

Mỗi hoạt động trong ngày có một số thứ tự, các số cách nhau 1000. Khi thả một hoạt động vào giữa hai hoạt động khác, giao diện chọn số nằm giữa và chỉ gửi đúng hoạt động vừa thả. Hết số trống thì đánh số lại cả ngày.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-UI-001 | Thả một hoạt động vào giữa hai hoạt động có số 1000 và 2000 | Chỉ hoạt động vừa thả được gửi đi, với số 1500 | Đúng | Đạt |
| TC-UI-002 | Thả một hoạt động xuống cuối ngày, sau hoạt động có số 2000 | Số mới là 3000 (cách hoạt động cuối một bước) | Đúng | Đạt |
| TC-UI-003 | Thả một hoạt động lên đầu ngày, trước hoạt động có số 1000 | Số mới là 500 | Đúng | Đạt |
| TC-UI-004 | Thả một hoạt động vào một ngày chưa có hoạt động nào | Số mới là 1000 | Biên | Đạt |
| TC-UI-005 | Thả vào giữa hai hoạt động có số liền nhau (5 và 6), không còn số trống | Cả ngày được đánh số lại: 1000, 2000, 3000 theo thứ tự mới | Biên | Đạt |
| TC-UI-006 | Thả lên đầu ngày khi hoạt động đầu tiên đã mang số nhỏ nhất (1) | Cả ngày được đánh số lại | Biên | Đạt |
| TC-UI-007 | Thả xuống cuối ngày khi hoạt động cuối đã mang số lớn nhất cho phép (1 tỉ) | Cả ngày được đánh số lại | Biên | Đạt |

### B. Cách ghi quãng đường và thời gian di chuyển

> **Yêu cầu:** UI_GUIDE 8.1 "Đoạn di chuyển"; design.md 10.2 "Quy ước Route" (máy chủ trả mét và giây, số nguyên) · **Kiểm bởi:** `lib/format.test.ts`

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-UI-008 | Quãng đường dưới 1 km: 1 m, 998 m, 999 m | Ghi bằng mét: "1 m", "998 m", "999 m" | Đúng | Đạt |
| TC-UI-009 | Quãng đường từ 1 km: 1000 m, 3200 m, 125.300 m | Ghi bằng km, một chữ số thập phân, dấu phẩy: "1,0 km", "3,2 km", "125,3 km" | Biên | Đạt |
| TC-UI-010 | Quãng đường cần làm tròn: 8440 m và 8460 m | "8,4 km" và "8,5 km" | Đúng | Đạt |
| TC-UI-011 | Thời gian dưới một giờ: 1, 59, 60, 61, 120, 1500 giây | Làm tròn **lên** phút: "1 phút", "1 phút", "1 phút", "2 phút", "2 phút", "25 phút" | Biên | Đạt |
| TC-UI-012 | Thời gian từ một giờ: 3540, 3541, 3600, 3900, 7260 giây | "59 phút", "1 giờ", "1 giờ", "1 giờ 5 phút", "2 giờ 1 phút" | Biên | Đạt |
| TC-UI-013 | Một chặng có quãng đường nhưng máy chủ trả 0 giây | Ghi "1 phút", không bao giờ ghi "0 phút" | Biên | Đạt |

### C. Chặng di chuyển nào được hiện, và hiện dưới thẻ nào

> **Yêu cầu:** design.md 10.2 "Quy ước Route", UI_GUIDE 8.1 "Đoạn di chuyển" · **Kiểm bởi:** `lib/travelLegs.test.ts`

Máy chủ trả các chặng của một ngày theo thứ tự lúc được hỏi. Màn hình có thể đã đi trước: người dùng vừa kéo một thẻ, hoặc vừa bỏ địa điểm của một hoạt động. Giao diện chỉ vẽ một chặng khi nó còn đúng với những gì đang hiện, để không có con số cũ nằm sai chỗ.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-UI-014 | Ngày có 3 hoạt động đều có địa điểm, máy chủ trả 2 chặng | Mỗi chặng nằm dưới thẻ của hoạt động xuất phát: dưới thẻ 1 và dưới thẻ 2 | Đúng | Đạt |
| TC-UI-015 | Ngày không có chặng nào (một địa điểm) | Không vẽ gì | Biên | Đạt |
| TC-UI-016 | Hai hoạt động liền nhau ở cùng một địa điểm, chặng dài 0 m | Chặng không được vẽ | Biên | Đạt |
| TC-UI-017 | Các chặng được tải cho thứ tự 1 → 2 → 3, rồi thẻ 3 bị kéo lên đầu (3 → 1 → 2) | Chặng 1 → 2 vẫn hiện (vẫn đúng); chặng 2 → 3 biến mất (giờ nó chỉ ngược lên trên); chưa có chặng 3 → 1 | Đúng | Đạt |
| TC-UI-018 | Một hoạt động vừa bị xoá hoặc chuyển sang ngày khác | Các chặng nối với hoạt động đó biến mất | Đúng | Đạt |
| TC-UI-019 | Một hoạt động vừa bị bỏ địa điểm | Chặng nối với nó biến mất | Đúng | Đạt |
| TC-UI-020 | Ngày có 4 hoạt động, hoạt động thứ hai không có địa điểm; máy chủ trả chặng 1 → 3 và 3 → 4 | Chặng 1 → 3 nằm dưới thẻ 1 và được đánh dấu "đích không phải thẻ kế tiếp" (giao diện ghi thêm tên đích); chặng 3 → 4 hiện bình thường | Đúng | Đạt |
| TC-UI-021 | Giữa hai đầu của một chặng có hai hoạt động không có địa điểm | Chặng vẫn hiện dưới thẻ xuất phát, có ghi tên đích | Biên | Đạt |
| TC-UI-022 | Chặng được tải là 1 → 3, rồi hoạt động 2 ở giữa được gắn địa điểm (hoặc một hoạt động có địa điểm được kéo vào giữa) | Chặng 1 → 3 biến mất: nó không còn đúng với ngày đang hiện | Đúng | Đạt |

### D. "Hôm nay" là ngày nào, và một ngày đã qua hay chưa

> **Yêu cầu:** design.md rule 14.22 ("hôm nay" tính theo múi giờ của tài khoản; một ngày đã qua khi hết ngày đó) · **Kiểm bởi:** `lib/today.test.ts`

"Hôm nay" không lấy theo giờ của máy đang mở trình duyệt mà theo múi giờ ghi trong tài khoản (hiện mọi tài khoản là giờ Việt Nam), để giao diện và máy chủ luôn nói cùng một ngày.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-UI-023 | Lúc 18:30 ngày 12/10 theo giờ quốc tế (01:30 ngày 13/10 ở Việt Nam), tài khoản giờ Việt Nam | Hôm nay là 13/10; cùng lúc đó tài khoản giờ quốc tế vẫn là 12/10 | Đúng | Đạt |
| TC-UI-024 | Một giây trước và đúng nửa đêm giờ Việt Nam | 23:59:59 còn là 12/10; 00:00:00 đã là 13/10 | Biên | Đạt |
| TC-UI-025 | Tài khoản ở múi giờ chậm hơn giờ quốc tế (Los Angeles), lúc 03:00 ngày 01/01 giờ quốc tế | Hôm nay còn là 31/12 của năm trước | Biên | Đạt |
| TC-UI-026 | Ngày và tháng có một chữ số (05/03) | Viết đủ hai chữ số: `2026-03-05` | Đúng | Đạt |
| TC-UI-027 | Tài khoản mang tên múi giờ mà trình duyệt không biết | Dùng giờ Việt Nam, trang không lỗi | Sai | Đạt |
| TC-UI-028 | So một ngày với hôm nay (13/10): 12/10, 13/10, 14/10 | Lần lượt: đã qua, hôm nay, sắp tới | Biên | Đạt |
| TC-UI-029 | So qua ranh giới tháng và năm: 30/09, 31/12 năm trước, 01/11, 01/01 năm sau | Hai ngày đầu đã qua, hai ngày sau sắp tới | Biên | Đạt |

### E. Mở một chuyến đi thì vào ngày nào

> **Yêu cầu:** design.md rule 14.22 (mở chuyến đi đang diễn ra thì vào ngày hôm nay) · **Kiểm bởi:** `lib/tripDates.test.ts`

Áp dụng khi mở chuyến đi mà không nói rõ ngày nào (bấm một thẻ ở trang danh sách). "Đang diễn ra" tính theo ngày: hôm nay nằm trong khoảng ngày đi, bất kể trạng thái đang ghi là gì.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-UI-030 | Chuyến đi 12–14/10, hôm nay là 13/10 | Mở Ngày 2 | Đúng | Đạt |
| TC-UI-031 | Hôm nay là ngày đầu (12/10) hoặc ngày cuối (14/10) của chuyến đi | Mở Ngày 1 hoặc Ngày 3: ngày đầu và ngày cuối đều tính là đang diễn ra | Biên | Đạt |
| TC-UI-032 | Hôm nay là 11/10 (một ngày trước khi đi) hoặc 15/10 (một ngày sau khi về) | Mở Ngày 1 | Biên | Đạt |
| TC-UI-033 | Chuyến đi còn rất xa hoặc đã qua rất lâu | Mở Ngày 1 | Đúng | Đạt |

### F. Thẻ chuyến đi hiện thời tiết của ngày nào

> **Yêu cầu:** design.md rule 14.20 (thẻ ở trang danh sách; dự báo chỉ có cho 16 ngày tính từ hôm nay) · **Kiểm bởi:** `features/weather/cardForecast.test.ts`

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-UI-034 | Chuyến đi 12–15/10, hôm nay 13/10 | Thẻ hiện thời tiết của **hôm nay** (13/10) | Đúng | Đạt |
| TC-UI-035 | Chuyến đi bắt đầu đúng hôm nay; kết thúc đúng hôm nay; chỉ dài một ngày là hôm nay | Cả ba đều tính là đang diễn ra: hiện thời tiết hôm nay | Biên | Đạt |
| TC-UI-036 | Chuyến đi bắt đầu ngày mai | Thẻ hiện thời tiết của **ngày khởi hành** | Đúng | Đạt |
| TC-UI-037 | Chuyến đi bắt đầu đúng ngày thứ 16 tính từ hôm nay (28/10 khi hôm nay là 13/10) | Vẫn hiện thời tiết ngày khởi hành: đó là ngày cuối cùng còn có dự báo | Biên | Đạt |
| TC-UI-038 | Chuyến đi bắt đầu ngày thứ 17 (29/10) | Thẻ không hiện thời tiết, không gọi máy chủ | Biên | Đạt |
| TC-UI-039 | Chuyến đi kết thúc hôm qua | Thẻ không hiện thời tiết, không gọi máy chủ | Biên | Đạt |
| TC-UI-040 | Khoảng 16 ngày bắc qua năm mới: hôm nay 20/12, chuyến đi bắt đầu 04/01 và 05/01 | 04/01 còn trong khoảng (hiện ngày khởi hành); 05/01 thì không | Biên | Đạt |

### G. Khi nào hỏi "Hoàn thành chuyến đi?"

> **Yêu cầu:** design.md rule 14.22 (không tự đổi trạng thái; hỏi khi chuyến đi đã qua ngày cuối mà trạng thái còn Nháp, Đã lên kế hoạch hoặc Đang diễn ra) · **Kiểm bởi:** `lib/tripDates.test.ts`

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-UI-041 | Chuyến đi kết thúc hôm qua, trạng thái Nháp / Đã lên kế hoạch / Đang diễn ra | Hỏi, với cả ba trạng thái | Đúng | Đạt |
| TC-UI-042 | Chuyến đi kết thúc hôm qua, trạng thái Đã hoàn thành hoặc Đã lưu trữ | Không hỏi | Đúng | Đạt |
| TC-UI-043 | Hôm nay là ngày cuối của chuyến đi | Không hỏi: ngày cuối chưa hết | Biên | Đạt |
| TC-UI-044 | Chuyến đi còn ở phía trước | Không hỏi | Đúng | Đạt |
| TC-UI-045 | Chuyến đi kết thúc từ năm trước, vẫn là Nháp | Hỏi | Biên | Đạt |

### H. Nhớ câu trả lời "Để sau"

> **Yêu cầu:** design.md rule 14.22 ("Để sau" → không hỏi lại về chuyến đi đó cho tới lần đăng nhập sau) · **Kiểm bởi:** `stores/completePromptStore.test.ts`

Danh sách chuyến đi đã trả lời "Để sau" được giữ trong bộ nhớ của trình duyệt (chỉ có mã số chuyến đi), nên tải lại trang không bị hỏi lại. Danh sách bị xoá khi phiên đăng nhập kết thúc.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-UI-046 | Đọc lại danh sách đã lưu; chưa lưu gì | Đúng các mã đã lưu; danh sách rỗng khi chưa có gì | Đúng | Đạt |
| TC-UI-047 | Dữ liệu trong trình duyệt bị hỏng: không phải JSON, không phải danh sách, chuỗi rỗng | Coi như danh sách rỗng, không lỗi | Sai | Đạt |
| TC-UI-048 | Danh sách bị sửa tay, lẫn chữ, số lẻ và giá trị trống | Chỉ giữ các mã là số nguyên | Sai | Đạt |
| TC-UI-049 | Trình duyệt không cho đọc hoặc không có bộ nhớ (chế độ riêng tư, chặn dữ liệu trang) | Danh sách rỗng, không lỗi | Sai | Đạt |
| TC-UI-050 | Lưu danh sách rồi đọc lại; lưu danh sách rỗng | Đọc lại đúng; danh sách rỗng thì mục lưu bị xoá hẳn | Đúng | Đạt |
| TC-UI-051 | Trình duyệt từ chối ghi (hết chỗ, bị chặn) | Không lỗi; trang vẫn dùng được | Sai | Đạt |
| TC-UI-052 | Bấm "Để sau" cho chuyến đi 3, chuyến đi 12, rồi lại chuyến đi 3 | Danh sách có 3 và 12, mỗi mã một lần | Đúng | Đạt |
| TC-UI-053 | Đang đăng nhập, đã "Để sau" một chuyến đi, rồi đăng xuất | Danh sách bị xoá | Đúng | Đạt |
| TC-UI-054 | Mở lại trang sau nhiều ngày, phiên cũ đã hết hạn (chưa kịp đăng nhập lại) | Danh sách bị xoá, nên sau khi đăng nhập sẽ được hỏi lại | Biên | Đạt |
| TC-UI-055 | Tải lại trang khi phiên còn hiệu lực | Danh sách **được giữ**: đây vẫn là lần đăng nhập đó | Biên | Đạt |

### I. Dải thời tiết hiện những ngày nào

> **Yêu cầu:** UI_GUIDE 9 "Thời tiết" (chốt 2026-10-05 sau khi chủ dự án xem trên trình duyệt: chỉ hiện ngày có dự báo); design.md rule 14.20 · **Kiểm bởi:** `features/weather/stripDays.test.ts`

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-UI-056 | Chuyến đi 12–14/10, hôm nay 13/10 (máy chủ không trả dự báo cho ngày 12) | Dải chỉ có Ngày 2 và Ngày 3, đúng thứ tự; Ngày 1 đã qua không có ô | Đúng | Đạt |
| TC-UI-057 | Chuyến đi có ngày cuối nằm ngoài 16 ngày tới | Ngày đó không có ô; các ngày trước nó vẫn hiện | Biên | Đạt |
| TC-UI-058 | Mọi ngày của chuyến đi đều có dự báo | Hiện đủ mọi ngày | Đúng | Đạt |
| TC-UI-059 | Không ngày nào có dự báo | Dải không có ô nào (thay bằng một câu giải thích) | Biên | Đạt |
| TC-UI-060 | Không có dự báo, ngày cuối của chuyến đi là hôm qua | Lý do: chuyến đi đã qua | Biên | Đạt |
| TC-UI-061 | Không có dự báo, chuyến đi bắt đầu tháng sau | Lý do: chuyến đi còn ở ngoài khoảng có dự báo | Đúng | Đạt |
| TC-UI-062 | Không có dự báo dù hôm nay còn là một ngày của chuyến đi (nguồn dự báo trả thiếu) | Không coi là "đã qua" | Biên | Đạt |

### J. Dải thời tiết chia trang thế nào

> **Yêu cầu:** UI_GUIDE 9 "Thời tiết" (chốt 2026-10-05: không cuộn ngang, chuyển trang bằng nút, mỗi lần một trang) · **Kiểm bởi:** `features/weather/stripDays.test.ts`

Mỗi ô cần ít nhất 88px; hai nút chuyển trang chiếm 64px. Cột bản đồ rộng 420px (418px bên trong viền) hoặc 360px (358px).

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-UI-063 | Cột 420px, chuyến đi có 4 ngày (hoặc 2 ngày) có dự báo | Hiện đủ, không có nút; các ô chia đều bề ngang | Đúng | Đạt |
| TC-UI-064 | Cột 420px, có 5 ngày hoặc 16 ngày có dự báo | Chia trang, **4 ngày một trang**, có hai nút | Biên | Đạt |
| TC-UI-065 | Cột 360px: 4 ngày; 7 ngày | 4 ngày: hiện đủ, không nút. 7 ngày: chia trang, 3 ngày một trang | Biên | Đạt |
| TC-UI-066 | Dải rộng 900px (tab "Bản đồ" trên máy tính bảng), 16 ngày | 9 ngày một trang | Đúng | Đạt |
| TC-UI-067 | Dải rất hẹp (120px), hoặc chưa đo được bề rộng | Vẫn có 1 ô mỗi trang, không bao giờ 0 | Biên | Đạt |
| TC-UI-068 | Số trang: 7 ô chia 3; 6 ô chia 3; 16 ô chia 4; 1 ô; 0 ô | 3, 2, 4, 1, 1 trang | Biên | Đạt |
| TC-UI-069 | Ô thứ mấy nằm ở trang nào (3 ô một trang): ô đầu, ô thứ 3, ô thứ 4, ô thứ 7 | Trang 1, trang 1, trang 2, trang 3 | Biên | Đạt |

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

### MT-UI-45 · Địa điểm trên thẻ hoạt động (Task 3.6 Commit 1)

Ở commit này giao diện chưa có ô chọn địa điểm (tới Commit 3), nên địa điểm được gắn qua Swagger như bài `MT-PLACE-05` ở [07-place.md](07-place.md). Cần backend và frontend đang chạy.

- [ ] Trên Swagger: đăng nhập, "Authorize". Gọi `POST /api/v1/places` với `{"provider": "MOCK", "externalId": "da-nang-cho-han"}`, ghi lại `id` của địa điểm.
- [ ] Gọi `POST /api/v1/trips/{tripId}/days/{dayId}/activities` ba lần cho cùng một ngày: `{"title": "Mua đặc sản", "placeId": <id>}`, `{"title": "Chợ Hàn", "placeId": <id>}`, và `{"title": "Nghỉ trưa"}` (không có địa điểm).
- [ ] Mở trang chi tiết chuyến đi trên giao diện, vào đúng ngày đó.
- [ ] Thẻ "Mua đặc sản": dưới tên có icon ghim và dòng "Chợ Hàn · " kèm địa chỉ của chợ, chữ nhỏ màu xám.
- [ ] Thẻ "Chợ Hàn": dòng địa điểm **chỉ có địa chỉ**, không lặp lại chữ "Chợ Hàn".
- [ ] Thẻ "Nghỉ trưa": **không có** dòng địa điểm, không có khoảng trống thừa.
- [ ] Thu hẹp cửa sổ trình duyệt xuống cỡ điện thoại: địa chỉ dài tự xuống dòng, không tràn ra ngoài thẻ, icon ghim vẫn ở đầu dòng thứ nhất.
- [ ] Kéo thả thẻ "Mua đặc sản" sang vị trí khác: thẻ bay theo con trỏ vẫn có dòng địa điểm; thả xong dòng địa điểm vẫn còn.

**Kết quả:** Chưa chạy

### MT-UI-46 · Chọn địa điểm trong hộp thoại hoạt động (Task 3.6 Commit 3)

Cần backend và frontend đang chạy bản code mới nhất, một chuyến đi có ít nhất một ngày.

- [ ] Mở một ngày, bấm "+ Thêm hoạt động". Ô đầu tiên là "Địa điểm" và con trỏ đang nằm trong ô đó.
- [ ] Gõ `c`: chưa có danh sách nào. Gõ thêm `h` rồi `ợ` (thành `chợ`): sau khoảng 0,3 giây hiện dòng "Đang tìm địa điểm…" rồi tối đa 5 gợi ý, đầu tiên là "Chợ Hàn". Mỗi gợi ý có icon trong vòng tròn xám, tên đậm, địa chỉ nhỏ bên dưới.
- [ ] Danh sách **nổi đè lên** ô "Tên hoạt động" và ô "Loại"; các ô bên dưới **không bị đẩy xuống**.
- [ ] Nhấn ↓ hai lần rồi ↑ một lần: dòng sáng (nền xanh nhạt, vạch xanh bên trái) di chuyển theo. Rê chuột lên một dòng: dòng đó sáng.
- [ ] Nhấn Esc: danh sách đóng, **hộp thoại vẫn mở**, chữ `chợ` vẫn còn. Nhấn ↓: danh sách mở lại.
- [ ] Nhấn Enter khi dòng "Chợ Hàn" đang sáng: ô tìm đổi thành khung có icon ghim, "Chợ Hàn", địa chỉ, và nút "×". Ô "Tên hoạt động" được điền sẵn "Chợ Hàn" và con trỏ nằm trong ô đó. Hoạt động **chưa** bị lưu (hộp thoại vẫn mở).
- [ ] Sửa tên thành `Mua đặc sản`, bấm "Thêm hoạt động". Hộp đóng, thẻ mới có dòng "Chợ Hàn · " kèm địa chỉ.
- [ ] Bấm "+ Thêm hoạt động", gõ tên `Ăn trưa` **trước**, rồi tìm `bun cha ca` và bấm chuột vào gợi ý: tên vẫn là `Ăn trưa`, không bị thay.
- [ ] Trong khung địa điểm bấm "×": ô tìm hiện lại, trống, con trỏ nằm trong ô. Tìm `chợ cồn`, chọn, lưu: thẻ ghi "Chợ Cồn".
- [ ] Gõ `khong co noi nay`: danh sách ghi "Không tìm thấy địa điểm nào. Thử từ khoá khác."
- [ ] Mở menu "⋮" → "Sửa" của hoạt động "Mua đặc sản": khung địa điểm hiện "Chợ Hàn"; con trỏ nằm ở ô "Tên hoạt động", **không** nằm ở nút "×". Đổi tên rồi lưu: địa điểm giữ nguyên.
- [ ] "Sửa" lần nữa, bấm "×", tìm và chọn "Chợ Cồn", lưu: thẻ đổi sang "Chợ Cồn".
- [ ] (Chỉ đúng ở Commit 3 và 4; từ Commit 5 thay bằng bài `MT-UI-48`) "Sửa", bấm "×" rồi bấm "Lưu thay đổi" mà không chọn địa điểm khác: dưới ô "Địa điểm" hiện "Chưa hỗ trợ xoá thông tin này, hãy nhập giá trị mới", hoạt động không bị lưu sai.
- [ ] Thu hẹp cửa sổ cỡ điện thoại, mở hộp thêm hoạt động, tìm `chợ`: danh sách vẫn nổi trong hộp, cuộn hộp thoại thì thấy đủ các gợi ý.
- [ ] Tắt backend, gõ `chợ hàn`: sau một lúc danh sách ghi "Không kết nối được máy chủ, vui lòng thử lại".

**Kết quả:** Chưa chạy

### MT-UI-47 · Xem tất cả gợi ý địa điểm (Task 3.6 Commit 4)

Số kết quả dưới đây là của dữ liệu có sẵn (56 địa điểm). Cần backend và frontend đang chạy.

- [ ] Mở hộp "Thêm hoạt động", gõ `chua` vào ô "Địa điểm": có 3 gợi ý, **không có** dòng "Xem tất cả".
- [ ] Xoá và gõ `cho`: có 5 gợi ý, dòng cuối cùng là chữ xanh "Xem tất cả 7 kết quả".
- [ ] Bấm vào dòng đó: danh sách hiện đủ 7 gợi ý, vẫn nổi tại chỗ, các ô bên dưới không bị đẩy xuống, hộp thoại không đóng, con trỏ vẫn trong ô tìm.
- [ ] Xoá và gõ `da nang`: lại về 5 gợi ý và dòng "Xem tất cả 20 kết quả".
- [ ] Nhấn ↓ sáu lần: dòng sáng đi qua 5 gợi ý rồi tới dòng "Xem tất cả". Nhấn Enter: danh sách mở đủ, dòng sáng nằm ở gợi ý thứ sáu; hoạt động **không** bị lưu.
- [ ] Danh sách đầy đủ cao tối đa khoảng 5–6 dòng và có **thanh cuộn riêng**. Nhấn ↓ nhiều lần: danh sách tự cuộn theo dòng đang sáng. Cuộn chuột trong danh sách tới cuối: trang phía sau không cuộn theo.
- [ ] Cuối danh sách có dòng nhỏ "Chỉ hiện 20 kết quả đầu. Gõ từ khoá cụ thể hơn để thu hẹp."
- [ ] Gõ thêm ` cho` (thành `da nang cho`): danh sách thu về kết quả mới, không còn ở chế độ xem tất cả.
- [ ] Mở đủ danh sách của `cho`, bấm chọn gợi ý thứ bảy: khung địa điểm hiện đúng địa điểm đó.

**Kết quả:** Chưa chạy

### MT-UI-48 · Bỏ địa điểm của hoạt động (Task 3.6 Commit 5)

Cần một hoạt động đã có địa điểm (tạo theo `MT-UI-46`).

- [ ] Mở "⋮" → "Sửa" của hoạt động đó. Bấm "×" trong khung địa điểm: ô tìm hiện lại, trống.
- [ ] Bấm "Huỷ". Thẻ vẫn còn hàng địa điểm: chưa lưu thì chưa có gì thay đổi.
- [ ] "Sửa" lại, bấm "×", rồi bấm "Lưu thay đổi". Hộp đóng, thông báo "Đã lưu thay đổi", thẻ **không còn** hàng địa điểm. Không còn câu "Chưa hỗ trợ xoá thông tin này" như ở Commit 3.
- [ ] Tải lại trang (F5): thẻ vẫn không có hàng địa điểm.
- [ ] "Sửa" lần nữa: ô "Địa điểm" là ô tìm trống. Bấm "Lưu thay đổi" mà không đổi gì: hộp đóng, không có lỗi.
- [ ] Tìm và chọn lại đúng địa điểm cũ, lưu: hàng địa điểm trở lại.
- [ ] "Sửa", bấm "×", đồng thời đổi tên hoạt động, rồi lưu: cả hai thay đổi đều được lưu (tên mới, không còn địa điểm).
- [ ] "Sửa" một hoạt động có địa điểm, bấm "×", chọn một địa điểm **khác**, lưu: thẻ hiện địa điểm mới (đây là đổi, không phải bỏ).

**Kết quả:** Chưa chạy

### MT-UI-49 · Chọn loại hoạt động bằng 6 nút (Task 3.6 Commit 6)

- [ ] Mở hộp "Thêm hoạt động". Dưới ô "Tên hoạt động" là nhóm "Loại" gồm 6 nút xếp 3 cột × 2 hàng: Tham quan, Ăn uống, Di chuyển, Lưu trú, Mua sắm, Khác. Mỗi nút có icon màu riêng. Không còn ô chọn thả xuống.
- [ ] Nút "Khác" đang được chọn sẵn: viền đậm và nền nhạt màu xanh lá, chữ cùng màu.
- [ ] Bấm "Ăn uống": nút đó có viền và nền cam nhạt, "Khác" trở về viền xám. Chỉ một nút được chọn tại một thời điểm.
- [ ] Rê chuột lên một nút chưa chọn: viền đậm hơn một chút. Rê lên nút đang chọn: viền vẫn giữ màu của loại, không đổi sang xám.
- [ ] Nhấn Tab từ ô "Tên hoạt động": con trỏ vào nhóm "Loại", nút đang chọn có viền ngoài màu xanh. Nhấn → và ←: loại đổi theo. Nhấn Tab lần nữa: con trỏ sang ô "Giờ bắt đầu" (không phải đi qua từng nút).
- [ ] Nhập tên, chọn "Mua sắm", bấm "Thêm hoạt động": thẻ mới có nhãn "Mua sắm" và viền trái màu hồng tím.
- [ ] Mở "Sửa" hoạt động đó: nút "Mua sắm" đang được chọn. Đổi sang "Tham quan", lưu: thẻ đổi nhãn và màu viền.
- [ ] Thu hẹp cửa sổ cỡ điện thoại: 6 nút xếp 2 cột × 3 hàng, nút cao hơn, chữ không bị cắt.

**Kết quả:** Chưa chạy

### MT-UI-50 · Loại hoạt động được gợi ý theo địa điểm (Task 3.6 Commit 7)

Nhóm của các địa điểm dùng trong bài: Chợ Hàn là mua sắm, Bún chả cá 109 là ăn uống, Chùa Linh Ứng là tham quan.

- [ ] Mở "Thêm hoạt động" ("Khác" đang được chọn). Tìm `chợ hàn` và chọn: nút "Mua sắm" tự sáng, "Khác" tắt.
- [ ] Bấm "×" ở khung địa điểm, tìm `bun cha ca` và chọn: nút "Ăn uống" sáng thay cho "Mua sắm".
- [ ] Bấm nút "Lưu trú" (tự chọn loại). Bấm "×", tìm `linh ung` và chọn: "Lưu trú" **vẫn** được chọn, không bị đổi sang "Tham quan".
- [ ] Đóng hộp, mở lại "Thêm hoạt động". Bấm "Ăn uống" **trước**, rồi tìm và chọn `chợ hàn`: "Ăn uống" vẫn được chọn.
- [ ] Đóng hộp, mở lại. Chọn `chợ hàn` (ra "Mua sắm"), nhập tên, lưu: thẻ có nhãn "Mua sắm".
- [ ] Mở "Sửa" một hoạt động loại "Khác" chưa có địa điểm. Tìm và chọn `chợ hàn`: loại **vẫn là "Khác"** (sửa hoạt động có sẵn thì không tự đổi loại). Lưu: thẻ có địa điểm, loại không đổi.

**Kết quả:** Chưa chạy

### MT-UI-51 · Đặt vị trí cho điểm đến (Task 3.6 Commit 8)

- [ ] Vào "Tạo chuyến đi", qua bước 1, tới bước "Điểm đến". Có hai ô: "Tên điểm đến" và "Vị trí trên bản đồ" (ô tìm có kính lúp).
- [ ] Để trống tên, gõ `cau rong` vào ô vị trí và chọn "Cầu Rồng": dưới ô tìm hiện dòng có icon ghim, "Cầu Rồng", toạ độ `16.0612, 108.2279` và nút chữ "Bỏ vị trí". Ô "Tên điểm đến" được điền "Cầu Rồng".
- [ ] Sửa tên thành `Đà Nẵng`. Tìm và chọn `cho han`: dòng vị trí đổi sang "Chợ Hàn" và toạ độ mới; tên vẫn là `Đà Nẵng`.
- [ ] Nhấn Enter trong ô tìm khi không có gợi ý nào đang mở: wizard **không** nhảy sang bước 3.
- [ ] Bấm "Bỏ vị trí": dòng vị trí biến mất. Chọn lại một vị trí.
- [ ] Bấm "Tiếp", rồi "Quay lại": dòng vị trí vẫn còn (ghi "Đã chọn vị trí" kèm toạ độ).
- [ ] Hoàn tất wizard. Mở hộp "Sửa" của chuyến đi vừa tạo: dưới ô tìm ghi "Vị trí đã lưu" kèm đúng toạ độ đã chọn, **không có** nút "Bỏ vị trí".
- [ ] Trong hộp sửa, tìm và chọn một địa điểm khác, "Lưu thay đổi". Mở "Sửa" lại: toạ độ là của địa điểm mới.
- [ ] Tạo một chuyến đi khác **không** đặt vị trí (chỉ gõ tên): tạo được bình thường. Mở "Sửa": không có dòng vị trí; chọn một vị trí rồi lưu được.
- [ ] Mở "Sửa" và lưu mà không đổi gì: hộp đóng, không có lỗi.

**Kết quả:** Chưa chạy

### MT-UI-52 · Gợi ý địa điểm quanh điểm đến của chuyến đi (Task 3.6 Commit 9)

Cần hai chuyến đi: một chuyến **chưa** đặt vị trí điểm đến, và một chuyến đặt vị trí ở TP. Hồ Chí Minh (trong hộp "Sửa", ô "Vị trí trên bản đồ", tìm `ben thanh` và chọn "Chợ Bến Thành").

- [ ] Mở chuyến đi chưa có vị trí, "+ Thêm hoạt động", gõ `cho`: gợi ý đầu tiên là "Chợ Hàn", rồi "Chợ Cồn" (thứ tự chỉ theo tên).
- [ ] Mở chuyến đi ở TP. Hồ Chí Minh, "+ Thêm hoạt động", gõ `cho`: gợi ý đầu tiên là "Chợ Bến Thành", rồi "Chợ Bình Tây"; các chợ ở thành phố khác xuống dưới.
- [ ] Bấm "Xem tất cả 7 kết quả": vẫn đủ 7 chợ như ở chuyến đi kia, chỉ khác thứ tự.
- [ ] Trong hộp "Sửa" của chuyến đi ở TP. Hồ Chí Minh, đổi vị trí sang Hà Nội (tìm `ho hoan kiem`, chọn), lưu. Mở "+ Thêm hoạt động", gõ `cho`: "Chợ Đồng Xuân" đứng đầu.
- [ ] Ở bước "Điểm đến" của wizard (hoặc ô vị trí trong hộp sửa chuyến đi), gõ `cho`: thứ tự chỉ theo tên ("Chợ Hàn" đứng đầu), không phụ thuộc chuyến đi nào.

**Kết quả:** Chưa chạy

### MT-UI-53 · Bản đồ của ngày (Task 3.6 Commit 10)

Cần máy có mạng (nền bản đồ tải từ OpenStreetMap), cửa sổ trình duyệt rộng từ 1280px, và một ngày có 3 hoạt động có địa điểm ở Đà Nẵng (Chợ Hàn, Bún chả cá 109, Chùa Linh Ứng) cùng 1 hoạt động không có địa điểm.

- [ ] Mở ngày đó. Bên phải có cột bản đồ, bo góc, viền mảnh. Nền bản đồ là Đà Nẵng, **có màu** (biển xanh, công viên xanh lá; từ Commit 21 không còn bị làm xám); góc dưới phải có dòng "Leaflet | © OpenStreetMap contributors" và hai nút + / −.
- [ ] Có đúng **3** marker hình giọt nước, mũi nhọn chỉ vào đúng vị trí. Mỗi marker có màu và icon của loại hoạt động, và số 1, 2, 3 ở góc trên phải. Hoạt động không có địa điểm không có marker. Cả 3 marker nằm gọn trong khung.
- [ ] Rê chuột lên một marker: hiện chú thích "2. Bún chả cá" (số và tên hoạt động).
- [ ] Bấm nút + và −, kéo bản đồ bằng chuột, lăn chuột trên bản đồ: bản đồ phóng to, thu nhỏ, di chuyển.
- [ ] Cuộn trang xuống (ngày cần đủ dài, hoặc thu thấp cửa sổ): cột bản đồ **đứng yên**, ngang hàng với cột ngày bên trái. Bản đồ không đè lên khối tiêu đề ngày đang dính.
- [ ] Kéo thả hoạt động thứ ba lên đầu ngày: số trên các marker đổi theo thứ tự mới; bản đồ **không** nhảy, mức phóng không đổi.
- [ ] Mở hộp "Thêm hoạt động": lớp phủ tối che cả bản đồ; bản đồ không nổi lên trên hộp thoại. Thêm một hoạt động ở Sun World Bà Nà Hills (tìm `ba na`): bản đồ tự thu nhỏ để chứa cả 4 marker.
- [ ] "Sửa" một hoạt động, bấm "×" bỏ địa điểm, lưu: marker của nó biến mất, các số còn lại dồn lên.
- [ ] Sang một ngày chưa có địa điểm nào: không có marker (từ Commit 12 có thẻ hướng dẫn và bản đồ mở ở điểm đến, xem `MT-UI-55`).
- [ ] Thu cửa sổ xuống khoảng 1100px: cột bản đồ hẹp lại (360px), cột giữa vẫn đọc được. Thu dưới 1024px: cột bản đồ biến mất, trang như trước Task 3.6.
- [ ] Mở trang danh sách chuyến đi, bấm F12 → tab Network, tải lại: **không có** file `DayMapCanvas-….js` nào được tải. Mở một chuyến đi: lúc này file đó mới được tải.
- [ ] Ngắt mạng (F12 → Network → Offline) rồi tải lại trang chi tiết đã mở trước đó: nền bản đồ xám, không vỡ bố cục; phần lịch trình vẫn dùng được.

**Kết quả:** Chưa chạy

### MT-UI-54 · Đường nối các địa điểm trong ngày (Task 3.6 Commit 11)

Dùng ngày có 3 hoạt động có địa điểm của bài `MT-UI-53`.

- [ ] Trên bản đồ có một đường **nét đứt** màu xanh ngọc, mảnh, nối marker 1 → 2 → 3 theo đúng thứ tự. Đường nằm **dưới** marker, không che số hay icon.
- [ ] Đường là các đoạn thẳng giữa hai marker, không uốn theo phố; nhìn rõ trên nền bản đồ nhưng không lấn át marker.
- [ ] Kéo thả hoạt động thứ ba lên đầu ngày: đường vẽ lại theo thứ tự mới (3 cũ → 1 cũ → 2 cũ), bản đồ không nhảy.
- [ ] Rê chuột và bấm lên đường: không có gì xảy ra, con trỏ không đổi hình; kéo bản đồ qua chỗ có đường vẫn được.
- [ ] "Sửa" một hoạt động và bỏ địa điểm của nó, còn 2 địa điểm: đường chỉ còn một đoạn. Bỏ tiếp, còn 1 địa điểm: **không còn đường**.
- [ ] Sang ngày khác rồi quay lại: mỗi ngày chỉ có đường của riêng nó.
- [ ] Phóng to và thu nhỏ bản đồ: độ dày và kiểu nét đứt của đường không đổi.

**Kết quả:** Chưa chạy

### MT-UI-55 · Bản đồ của ngày chưa có địa điểm (Task 3.6 Commit 12)

Cần hai chuyến đi: chuyến A đã đặt vị trí điểm đến ở Đà Nẵng (bài `MT-UI-51`), chuyến B chưa đặt vị trí.

- [ ] Chuyến A, mở một ngày chưa có hoạt động nào: bản đồ hiện khu vực **Đà Nẵng** ở mức thành phố, không có marker và không có đường nối.
- [ ] Giữa bản đồ có một thẻ trắng nhỏ: icon bản đồ, dòng đậm "Ngày này chưa có địa điểm nào.", dòng nhỏ "Thêm địa điểm cho hoạt động để thấy trên bản đồ." Không có câu nào về việc đặt vị trí điểm đến.
- [ ] Kéo bản đồ ở vùng quanh thẻ, bấm + / −: bản đồ vẫn di chuyển và phóng được.
- [ ] Thêm một hoạt động **không** có địa điểm: thẻ vẫn còn. Thêm một hoạt động có địa điểm (Chợ Hàn): thẻ biến mất, có 1 marker, bản đồ phóng vào đó.
- [ ] "Sửa" hoạt động đó và bỏ địa điểm: thẻ hiện lại, bản đồ trở về khu vực Đà Nẵng.
- [ ] Chuyến B, mở một ngày chưa có địa điểm: bản đồ hiện **cả Việt Nam**; thẻ có thêm câu 'Đặt vị trí điểm đến trong "Sửa" chuyến đi để bản đồ mở đúng nơi bạn đến.'
- [ ] Ở chuyến B, bấm "Sửa", đặt vị trí điểm đến ở Hà Nội (tìm `ho hoan kiem`), lưu: không cần tải lại trang, bản đồ chuyển tới Hà Nội và câu về điểm đến biến mất.
- [ ] Mở hộp "Thêm hoạt động" khi thẻ đang hiện: thẻ nằm dưới lớp phủ tối như phần còn lại của trang.

**Kết quả:** Chưa chạy

### MT-UI-56 · Phóng bản đồ ra cả cửa sổ (Task 3.6 Commit 13)

> Trạng thái: Chạy một phần · từng lỗi BUG-UI-009 (bước 2 đạt sau khi sửa, chủ dự án xác nhận ngày 2026-10-04; các bước khác chưa có kết quả)

Dùng ngày có 3 địa điểm của bài `MT-UI-53`, cửa sổ rộng từ 1024px.

- [ ] Góc trên phải bản đồ có một nút vuông trắng nhỏ với icon hai mũi tên chéo. Rê chuột lên: chú thích "Phóng to bản đồ".
- [ ] Bấm nút: bản đồ mở **phủ cả cửa sổ**, che thanh trên cùng và cột lịch trình. Có đủ 3 marker, đường nét đứt, dòng ghi nguồn và nút + / −. Cả 3 marker nằm gọn trong khung.
- [ ] Góc trên phải giờ là nút "×" (chú thích "Thu nhỏ bản đồ"). Kéo, phóng to, thu nhỏ bản đồ được như thường.
- [ ] Nhấn Tab vài lần: con trỏ chỉ đi quanh các nút của bản đồ, không chạy ra phần trang phía sau.
- [ ] Nhấn Esc: bản đồ thu về cột bên phải, trang ở nguyên chỗ cũ, không bị cuộn. Con trỏ nằm ở nút "Phóng to bản đồ" (nhấn Enter là mở lại).
- [ ] Mở lại, bấm "×": thu về như trên.
- [ ] Cuộn trang xuống giữa một ngày dài rồi phóng to: bản đồ vẫn phủ cả cửa sổ, **không** bị khối tiêu đề ngày hay dải ngày đè lên. Thu nhỏ: trang vẫn ở vị trí đang cuộn.
- [ ] Sang một ngày chưa có địa điểm, phóng to: bản đồ ở điểm đến, thẻ "Ngày này chưa có địa điểm nào." nằm giữa.
- [ ] Thay đổi kích thước cửa sổ trong lúc đang phóng to: bản đồ luôn kín cửa sổ, không lộ khoảng trắng.

**Kết quả:** Chưa chạy

### MT-UI-57 · Rê thẻ hoạt động thì marker nổi bật (Task 3.6 Commit 14)

Dùng ngày có 3 hoạt động có địa điểm và 1 hoạt động không có, cửa sổ rộng từ 1024px.

- [ ] Rê chuột lên thẻ thứ hai: marker số 2 trên bản đồ **to hơn một chút** và có vòng sáng xanh quanh nó; mũi nhọn vẫn chỉ đúng vị trí cũ. Các marker khác không đổi.
- [ ] Rê sang thẻ thứ ba: marker 2 trở lại bình thường, marker 3 nổi bật. Rê ra ngoài mọi thẻ: không marker nào nổi bật.
- [ ] Rê lên thẻ của hoạt động **không có địa điểm**: không marker nào nổi bật, không có lỗi.
- [ ] Tạo hai hoạt động ở cùng một địa điểm (hai marker chồng lên nhau). Rê lần lượt lên hai thẻ: marker của thẻ đang rê **nổi lên trên**, thấy đúng số của nó.
- [ ] Không dùng chuột: nhấn Tab cho tới khi con trỏ vào trong một thẻ (tay nắm kéo hoặc nút "⋮"): marker của thẻ đó nổi bật. Tab ra khỏi thẻ: hết nổi bật.
- [ ] Kéo thả một thẻ sang vị trí khác rồi thả: không có marker nào bị kẹt ở trạng thái nổi bật sau khi rê chuột ra ngoài.
- [ ] Rê nhanh chuột qua lại nhiều thẻ: bản đồ không giật, không nhảy, mức phóng không đổi.

**Kết quả:** Chưa chạy

### MT-UI-58 · Bấm marker để tới thẻ hoạt động (Task 3.6 Commit 15)

Dùng một ngày **dài** (từ 6 hoạt động, ít nhất 3 có địa điểm, trong đó hoạt động cuối ngày có địa điểm), cửa sổ rộng từ 1024px, dùng chuột.

- [ ] Ở đầu trang, bấm marker của hoạt động **cuối ngày**: trang cuộn mượt xuống, thẻ của hoạt động đó dừng ở khoảng giữa màn hình, **không** bị khối tiêu đề ngày che. Thẻ có viền sáng xanh trong khoảng 2 giây rồi tắt.
- [ ] Trong lúc đó marker vừa bấm đang ở trạng thái nổi bật (to hơn, có vòng sáng). Bấm vào chỗ trống của trang: marker trở lại bình thường.
- [ ] Bấm marker số 1 khi đang ở cuối trang: trang cuộn ngược lên tới thẻ đầu tiên.
- [ ] Bấm liên tiếp hai marker khác nhau: viền sáng chuyển sang thẻ thứ hai, thẻ thứ nhất không bị kẹt viền.
- [ ] Bấm marker **không** mở ô tên nào (ở chế độ dùng chuột trên bản đồ nhỏ).
- [ ] Bấm "Phóng to bản đồ", rồi bấm một marker: phía trên marker hiện ô nhỏ ghi "2. Bún chả cá" (số và tên) và chữ xanh "Xem trong lịch trình". Bấm "×" của ô hoặc bấm ra chỗ trống của bản đồ: ô đóng.
- [ ] Mở lại ô tên, bấm "Xem trong lịch trình": bản đồ thu về cột bên phải, trang cuộn tới thẻ đó, thẻ có viền sáng.
- [ ] Bàn phím: nhấn Tab cho tới khi một marker có viền focus, nhấn Enter: kết quả như bấm chuột.
- [ ] Màn cảm ứng (hoặc F12 → chế độ giả lập thiết bị cảm ứng, bề rộng từ 1024px, tải lại trang): chạm marker trên bản đồ nhỏ mở **ô tên** thay vì cuộn ngay; chạm "Xem trong lịch trình" thì trang cuộn tới thẻ.
- [ ] Trong lúc đang kéo thả một thẻ, bản đồ và các marker vẫn hoạt động bình thường sau khi thả.

**Kết quả:** Chưa chạy

### MT-UI-59 · Tab "Lịch trình" / "Bản đồ" trên màn hình hẹp (Task 3.6 Commit 16)

Thu cửa sổ xuống dưới 1024px (hoặc F12 → giả lập điện thoại 390px). Dùng ngày có 3 địa điểm.

- [ ] Ngay dưới dải chip ngày có hai nút cạnh nhau trong một khung xám: "Lịch trình" (đang chọn: nền trắng, chữ xanh đậm) và "Bản đồ". Phía dưới là danh sách hoạt động như trước.
- [ ] Bấm "Bản đồ": danh sách biến mất, bản đồ hiện ra **rộng hết màn hình**, cao khoảng 2/3 màn hình, đủ 3 marker, đường nét đứt, dòng ghi nguồn, nút + / − và nút phóng to. Nút "Bản đồ" thành nút đang chọn.
- [ ] Bấm "Lịch trình": danh sách trở lại đúng chỗ cũ.
- [ ] Ở tab "Lịch trình", bấm "Sửa" ngày và gõ dở một tiêu đề (chưa lưu). Sang tab "Bản đồ" rồi quay lại: chữ đang gõ **vẫn còn**.
- [ ] Ở tab "Bản đồ", chạm một marker: mở ô tên "2. …" với "Xem trong lịch trình". Chạm vào đó: màn hình chuyển về tab "Lịch trình" và cuộn tới thẻ của hoạt động, thẻ có viền sáng.
- [ ] Ở tab "Bản đồ", chạm một chip ngày khác: vẫn ở tab "Bản đồ", bản đồ đổi sang địa điểm của ngày mới.
- [ ] Ở tab "Bản đồ" của một ngày chưa có địa điểm: bản đồ ở điểm đến kèm thẻ "Ngày này chưa có địa điểm nào."
- [ ] Bấm nút phóng to trên bản đồ: bản đồ kín màn hình; chạm marker → "Xem trong lịch trình": bản đồ thu lại, về tab "Lịch trình", cuộn tới thẻ.
- [ ] F12 → Network: ở tab "Lịch trình" trên màn hẹp, **không có** yêu cầu nào tới `tile.openstreetmap.org`; chỉ khi mở tab "Bản đồ" mới có.
- [ ] Kéo rộng cửa sổ quá 1024px: hai nút biến mất, danh sách và bản đồ hiện **cùng lúc** thành hai cột, dù trước đó đang ở tab nào. Thu hẹp lại: trở về tab đã chọn trước đó.
- [ ] Hai nút đủ lớn để chạm bằng ngón tay (cao 44px); dùng Tab và Enter trên bàn phím chuyển được tab.

**Kết quả:** Chưa chạy

### MT-UI-60 · Vùng chạm trên màn hình cảm ứng (Task 3.6 Commit 17)

Dùng điện thoại thật, hoặc F12 → giả lập thiết bị cảm ứng (ví dụ iPhone 12) rồi **tải lại trang**. Để so sánh, mở thêm một cửa sổ bình thường dùng chuột.

- [ ] Trên màn cảm ứng, nút "⋮" ở góc thẻ hoạt động là một ô vuông lớn (44px), chạm bằng ngón cái trúng ngay; menu vẫn mở đúng chỗ. Trên cửa sổ dùng chuột, nút vẫn nhỏ như cũ (28px).
- [ ] Dải chip ngày: mỗi chip cao hơn bản dùng chuột (44px so với 36px); dải vẫn cuộn ngang và dính ở mép trên.
- [ ] Mở hộp "Thêm hoạt động": nút "×" ở góc hộp to hơn, chạm dễ; tiêu đề hộp không bị đẩy lệch.
- [ ] Chọn một địa điểm rồi chạm "×" trong khung địa điểm: trúng ngay lần đầu, không chạm nhầm sang ô bên dưới.
- [ ] Tab "Bản đồ": marker to hơn bản dùng chuột (36px), mũi nhọn vẫn chỉ đúng vị trí; số và icon vẫn đọc được. Chạm marker mở ô tên **ngay phía trên** marker, không đè lên nó.
- [ ] Trong ô tên, dòng "Xem trong lịch trình" có vùng chạm cao (44px), chạm dễ.
- [ ] Trên cửa sổ dùng chuột, kiểm lại nhanh: thẻ hoạt động, hộp thoại, bản đồ trông **y như trước** commit này.

**Kết quả:** Chưa chạy

### MT-UI-61 · Tự thêm một địa điểm không có trong kết quả tìm kiếm (Task 3.6 Commit 18)

Cần máy có mạng (bản đồ nhỏ tải nền từ OpenStreetMap) và một chuyến đi đã đặt vị trí điểm đến ở Đà Nẵng.

- [ ] Mở "Thêm hoạt động". Dưới ô tìm "Địa điểm" có dòng chữ xanh "Không tìm thấy? Tự thêm địa điểm". Con trỏ lúc mở hộp vẫn nằm trong ô tìm, không nằm ở dòng chữ này.
- [ ] Gõ `nha ba ngoai`: danh sách ghi không tìm thấy. Bấm "Không tìm thấy? Tự thêm địa điểm": ô tìm được thay bằng một khung có ô "Tên địa điểm *" (con trỏ ở đây), nhãn "Vị trí *" và một bản đồ nhỏ đang ở khu vực **Đà Nẵng**, cùng hai nút "Quay lại tìm kiếm" và "Thêm địa điểm".
- [ ] Bấm "Thêm địa điểm" khi chưa nhập gì: dưới ô tên báo "Tên địa điểm không được để trống", dưới bản đồ báo "Bấm lên bản đồ để đặt vị trí của địa điểm" (chữ đỏ). Hoạt động không bị lưu, hộp thoại vẫn mở.
- [ ] Gõ tên `Nhà bà ngoại`. Phóng to bản đồ bằng nút + rồi bấm vào một điểm: hiện marker xanh ngọc hình giọt nước ngay chỗ bấm, và dòng "Đã đặt vị trí: …, …. Bấm chỗ khác để đổi." Bấm chỗ khác: marker dời theo, toạ độ đổi.
- [ ] Nhấn Enter trong ô tên: địa điểm được thêm (không phải hoạt động). Khung chuyển thành khung địa điểm đã chọn ghi "Nhà bà ngoại" (không có địa chỉ), ô "Tên hoạt động" được điền "Nhà bà ngoại", "Loại" vẫn là "Khác".
- [ ] Bấm "Thêm hoạt động": thẻ mới có dòng địa điểm "Nhà bà ngoại"; trên bản đồ của ngày có marker đúng chỗ đã bấm.
- [ ] Mở "Thêm hoạt động" lần nữa, bấm "Không tìm thấy? Tự thêm địa điểm" rồi "Quay lại tìm kiếm": ô tìm hiện lại và nhận con trỏ.
- [ ] Trong khung tự thêm, lăn chuột trên bản đồ nhỏ: bản đồ phóng; lăn chuột ở ngoài bản đồ: hộp thoại cuộn. Bản đồ nhỏ có dòng ghi nguồn.
- [ ] Ở một chuyến đi **chưa** đặt vị trí điểm đến: bản đồ nhỏ mở ở cả Việt Nam.
- [ ] Giả lập điện thoại: khung tự thêm vừa bề ngang hộp thoại; chạm lên bản đồ đặt được vị trí; hai nút đủ lớn để chạm.
- [ ] Đăng nhập bằng một tài khoản khác: tìm `nha ba ngoai` **không** ra địa điểm vừa thêm (địa điểm tự thêm là của riêng người tạo).

**Kết quả:** Chưa chạy

### MT-UI-62 · Bản đồ nhỏ ở điểm đến của chuyến đi (Task 3.6 Commit 19)

Cần máy có mạng.

- [ ] "Tạo chuyến đi", tới bước "Điểm đến": dưới ô "Vị trí trên bản đồ" có một bản đồ nhỏ đang hiện cả Việt Nam, có dòng ghi nguồn và nút + / −. Dòng gợi ý ghi "Tìm theo tên hoặc bấm lên bản đồ để chọn vị trí. …".
- [ ] Tìm `cau rong` và chọn "Cầu Rồng": trên bản đồ có marker xanh ngọc và bản đồ **tự chuyển tới Đà Nẵng**. Dưới bản đồ: "Cầu Rồng" kèm toạ độ `16.0612, 108.2279`.
- [ ] Phóng to rồi bấm vào một điểm khác trên bản đồ: marker dời tới đó, toạ độ đổi, nhãn đổi thành "Vị trí chọn trên bản đồ". Ô "Tên điểm đến" **không** bị đổi.
- [ ] Tìm `ho hoan kiem` và chọn: marker nhảy ra Hà Nội, bản đồ chuyển theo, nhãn là "Hồ Hoàn Kiếm".
- [ ] "Bỏ vị trí": marker và dòng toạ độ biến mất. Bấm lên bản đồ: có lại.
- [ ] Ở một wizard mới (bản đồ đang hiện cả Việt Nam), bấm thẳng lên khu vực miền Trung: có marker và bản đồ **tự phóng vào** quanh điểm vừa bấm (mức thành phố) để bấm lại cho chính xác. Bấm lần nữa khi đã phóng: bản đồ đứng yên, chỉ marker dời.
- [ ] Bấm "Tiếp" rồi "Quay lại": bản đồ mở ngay ở vị trí đã chọn, marker còn nguyên.
- [ ] Hoàn tất wizard. Trang chi tiết: bản đồ của một ngày trống mở ở đúng vị trí vừa đặt.
- [ ] Mở "Sửa chuyến đi": bản đồ nhỏ mở ở vị trí đã lưu (mức phố), nhãn "Vị trí đã lưu", không có "Bỏ vị trí". Bấm một điểm khác trên bản đồ, "Lưu thay đổi": bản đồ của ngày trống chuyển theo vị trí mới.
- [ ] Đóng hộp "Sửa chuyến đi" rồi **mở lại lần thứ hai**: bản đồ nhỏ vẫn hiện đủ nền, marker nằm giữa khung (`BUG-UI-009`).
- [ ] Trong hộp "Sửa chuyến đi", lớp phủ tối và nút "×" của hộp nằm **trên** bản đồ nhỏ; cuộn hộp thoại thì bản đồ cuộn theo, không đè lên phần khác.
- [ ] Giả lập điện thoại: bản đồ nhỏ vừa bề ngang; chạm để đặt vị trí được; kéo bản đồ bằng một ngón không làm cuộn trang.

**Kết quả:** Chưa chạy

---

### MT-UI-63 · Dải thời tiết dưới bản đồ (Task 3.7 Commit 2)

Cần: một chuyến đi **đã đặt vị trí điểm đến**, bắt đầu từ hôm qua và dài ít nhất 4 ngày (để có ngày đã qua, hôm nay và ngày sắp tới); một chuyến đi khác dài 20 ngày bắt đầu từ hôm nay. Máy chủ chạy với nguồn thời tiết giả (mặc định).

- [ ] Mở chuyến đi 4 ngày trên màn hình rộng (từ 1024px): dưới bản đồ có một khung trắng viền mảnh, chia thành các ô, mỗi ngày một ô. Bản đồ thấp hơn trước đúng bằng phần dải chiếm, không có vùng xám trong bản đồ.
- [ ] Mỗi ô có: dòng "N2 · ngày/tháng", một icon thời tiết, nhiệt độ dạng "32° / 25°" (cao trước, thấp sau, không có số lẻ), icon giọt nước và phần trăm mưa.
- [ ] Icon có màu: nắng màu vàng cam, mưa màu xanh dương, mây màu xám.
- [ ] Ngày **hôm qua** không có ô trong dải (từ Commit 14; chi tiết ở `MT-UI-75`).
- [ ] Ô của ngày đang xem có nền xanh nhạt và dòng đầu in đậm.
- [ ] Bấm ô của một ngày khác: trang chuyển sang ngày đó (URL đổi số ngày), ô vừa bấm thành ô được tô.
- [ ] Trong lúc dự báo đang tải (làm mới trang, hoặc giả lập mạng chậm trong DevTools): chỗ của dải là một khối xám nhấp nháy **cùng chiều cao**, bản đồ không bị nhảy khi dải hiện ra.
- [ ] Mở chuyến đi 20 ngày: dải chia trang, chuyển bằng nút (chi tiết ở `MT-UI-76`). Mở Ngày 15: dải tự lật tới trang có Ngày 15, **trang không tự cuộn dọc**.
- [ ] Thêm, sửa, kéo thả một hoạt động: trong tab Network của DevTools **không** có lần gọi `/weather/trips/…` mới.
- [ ] Dưới 1024px, tab "Bản đồ": dải nằm ngay dưới bản đồ, rộng hết màn hình. Tab "Lịch trình": xem `MT-UI-66`.
- [ ] Bàn phím: Tab đi qua từng ô, có vòng focus; Enter mở ngày đó. Trình đọc màn hình đọc đủ "Ngày 2, 13/10: Có mây, cao nhất 32°, thấp nhất 25°, khả năng mưa 20%".

Chuyến đi chưa đặt vị trí điểm đến: xem `MT-UI-64`. Dự báo không tải được: xem `MT-UI-65`.

**Kết quả:** Chưa chạy

---

### MT-UI-64 · Chuyến đi chưa đặt vị trí điểm đến thì được mời đặt (Task 3.7 Commit 3)

Cần: một chuyến đi **chưa đặt vị trí điểm đến** (tạo mới, bỏ qua ô "Vị trí trên bản đồ"), có ngày nằm trong 16 ngày tới.

- [ ] Mở chuyến đi đó: khung thời tiết dưới bản đồ không có ô ngày nào, chỉ có icon ghim gạch chéo và câu 'Đặt vị trí điểm đến trong "Sửa" chuyến đi để xem dự báo thời tiết.' Khung cao bằng dải thời tiết của các chuyến đi khác.
- [ ] Bấm "Sửa" chuyến đi, tìm `cau rong`, chọn "Cầu Rồng", "Lưu thay đổi": **không cần tải lại trang**, khung đổi thành các ô ngày có dự báo; bản đồ của ngày trống cũng chuyển về Đà Nẵng.
- [ ] "Sửa" lần nữa, dời ngày đi lùi lại 1 ngày, "Lưu thay đổi": ngày tháng trong các ô đổi theo, dự báo là của ngày mới.
- [ ] "Sửa", kéo dài chuyến đi thêm 2 ngày, "Lưu thay đổi": dải có thêm 2 ô.
- [ ] "Sửa", rút ngắn chuyến đi 2 ngày (các ngày bị bỏ không có hoạt động): dải bớt 2 ô, không có ô nào mang số ngày sai.

**Kết quả:** Chưa chạy

---

### MT-UI-65 · Dự báo không tải được thì trang vẫn dùng được (Task 3.7 Commit 4)

Cần: một chuyến đi đã đặt vị trí điểm đến. Dùng DevTools → Network → chuột phải vào một lần gọi `/weather/trips/…` → "Block request URL" để chặn riêng lời gọi thời tiết (máy chủ vẫn chạy).

- [ ] Chặn lời gọi thời tiết rồi tải lại trang: chuyến đi, các ngày, hoạt động và bản đồ hiện bình thường. Chỗ của dải là khối xám nhấp nháy trong vài giây (ứng dụng tự thử lại 3 lần), sau đó là icon mây có dấu chấm than và câu "Tạm thời không có dự báo." kèm chữ "Thử lại" màu xanh ngọc. Khung cao bằng dải bình thường, bản đồ không có vùng xám.
- [ ] Trong lúc khung đang báo lỗi: thêm một hoạt động, kéo thả, đổi ngày đều làm được như thường; không có thông báo lỗi nào khác trên trang.
- [ ] Bấm "Thử lại" khi còn chặn: chữ đổi thành "Đang thử lại…" và không bấm được, vài giây sau trở lại "Thử lại".
- [ ] Bỏ chặn, bấm "Thử lại": các ô ngày hiện ra với dự báo.
- [ ] Khi dải đang có dự báo, chặn lại rồi chuyển sang cửa sổ khác và quay lại (ứng dụng tự tải lại ngầm): dải **vẫn giữ dự báo đã tải**, không đổi thành câu báo lỗi.
- [ ] Bàn phím: Tab tới được "Thử lại", Enter có tác dụng như bấm chuột.

**Kết quả:** Chưa chạy

---

### MT-UI-66 · Thời tiết của ngày ở tiêu đề ngày trên màn hình hẹp (Task 3.7 Commit 5)

Cần: chuyến đi của `MT-UI-63` (đã đặt vị trí điểm đến, bắt đầu từ hôm qua, dài ít nhất 4 ngày) và chuyến đi chưa đặt vị trí điểm đến của `MT-UI-64`. Giả lập điện thoại trong DevTools (hoặc thu cửa sổ dưới 1024px).

- [ ] Mở Ngày 2 (hôm nay), tab "Lịch trình": dưới tên ngày và tiêu đề của ngày có **một dòng** chữ nhỏ: icon thời tiết có màu, tên tình trạng ("Có mây", "Có mưa"…), nhiệt độ "32° / 25°", icon giọt nước và phần trăm mưa. Các phần ngăn nhau bằng dấu chấm giữa.
- [ ] Con số trên dòng này trùng với ô của Ngày 2 trong dải ở tab "Bản đồ".
- [ ] Chuyển sang Ngày 3: dòng đổi theo dự báo của Ngày 3.
- [ ] Mở Ngày 1 (hôm qua, không có dự báo): **không có dòng thời tiết**, cũng không có khoảng trống thừa.
- [ ] Chuyến đi chưa đặt vị trí điểm đến: không có dòng thời tiết ở ngày nào.
- [ ] Chặn lời gọi thời tiết (như `MT-UI-65`) rồi tải lại: không có dòng thời tiết, không có thông báo lỗi ở tiêu đề ngày; tab "Bản đồ" vẫn có câu "Tạm thời không có dự báo."
- [ ] Màn rất hẹp (320px): dòng tự xuống hàng gọn, không tràn ngang.
- [ ] Bấm "Sửa" của ngày: form sửa hiện ra, dòng thời tiết ẩn; "Huỷ" thì hiện lại.
- [ ] Kéo cửa sổ rộng ra từ 1024px: dòng biến mất (đã có dải dưới bản đồ). Trong tab Network chỉ có **một** lần gọi `/weather/trips/…` cho cả dải và dòng này.

**Kết quả:** Chưa chạy

---

### MT-UI-67 · Thời gian và quãng đường giữa các hoạt động (Task 3.7 Commit 6)

Cần: một ngày có 3 hoạt động **liền nhau đều có địa điểm**, ví dụ ở Đà Nẵng: "Chợ Hàn", "Bún chả cá 109", "Cầu Rồng" (tìm bằng ô địa điểm). Nguồn bản đồ giả (mặc định).

- [ ] Dưới thẻ thứ nhất và thẻ thứ hai có một dòng chữ nhỏ màu xám dạng "2 phút · 998 m" (Chợ Hàn → Bún chả cá 109 là 998 m, 2 phút). Dưới thẻ cuối cùng không có gì.
- [ ] Bên trái dòng chữ, đoạn ray giữa hai thẻ là **nét đứt**; các đoạn ray khác vẫn nét liền. Không có icon ô tô hay người đi bộ.
- [ ] Rê chuột lên dòng chữ: hiện chú thích "Ước tính theo đường bộ, chưa tính kẹt xe".
- [ ] Con số trùng với `GET /api/v1/trips/{tripId}/days/{dayId}/route` trên Swagger: mét đổi sang "m" hoặc "km" một chữ số thập phân với dấu phẩy, giây làm tròn lên phút.
- [ ] Thêm hai hoạt động cùng một địa điểm đứng liền nhau: giữa chúng **không có** dòng di chuyển.
- [ ] Ngày chỉ có một hoạt động có địa điểm, hoặc không có: không có dòng nào, và trong tab Network **không có** lần gọi `/route` cho ngày đó.
- [ ] Bắt đầu kéo một thẻ: mọi dòng di chuyển ẩn đi, các thẻ **không nhảy vị trí** lúc bắt đầu kéo. Thả về chỗ cũ: các dòng hiện lại.
- [ ] Thêm, sửa một hoạt động **không đổi địa điểm**: trong tab Network không có lần gọi `/route` mới.
- [ ] Giả lập điện thoại: dòng di chuyển nằm gọn dưới thẻ, không tràn ngang.

Tính lại sau khi kéo thả hoặc đổi địa điểm: xem `MT-UI-68`. Chặng đi qua một hoạt động không có địa điểm: xem `MT-UI-69`.

**Kết quả:** Chưa chạy

---

### MT-UI-68 · Quãng đường được tính lại sau khi ngày thay đổi (Task 3.7 Commit 7)

Cần: ngày của `MT-UI-67` (3 hoạt động liền nhau đều có địa điểm) và một ngày khác có 2 hoạt động có địa điểm. Mở tab Network của DevTools, lọc theo `route`.

- [ ] Kéo thẻ thứ ba lên đầu ngày: sau khi thả, có **một** lần gọi `/route` mới; các dòng di chuyển hiện theo thứ tự mới (hai dòng, con số khác trước nếu các cặp đã đổi).
- [ ] Trên màn cảm ứng (giả lập điện thoại), bấm nút ↑ / ↓ của một thẻ: kết quả như kéo thả.
- [ ] Menu "⋮" → "Chuyển sang ngày…" đưa một hoạt động có địa điểm sang ngày kia: ngày đang xem mất dòng di chuyển liên quan và hai thẻ còn lại có dòng mới nối chúng. Bấm "Mở Ngày N" trong thông báo: ngày đó có thêm dòng di chuyển tới hoạt động vừa chuyển sang.
- [ ] Kéo một thẻ thả lên tên ngày ở cột trái: kết quả như bước trên.
- [ ] Thêm một hoạt động có địa điểm, có giờ bắt đầu nằm giữa hai hoạt động sẵn có: hoạt động vào giữa, và có hai dòng di chuyển mới nối nó với hai thẻ bên cạnh.
- [ ] Sửa một hoạt động, **đổi sang địa điểm khác**, lưu: dòng di chuyển của thẻ đó và của thẻ đứng trước **biến mất ngay** rồi hiện lại với con số mới; không có lúc nào con số cũ nằm cạnh địa điểm mới.
- [ ] Sửa một hoạt động, bấm "×" bỏ địa điểm, lưu: thẻ đó không còn dòng di chuyển (chặng đi qua nó: `MT-UI-69`).
- [ ] Xoá hoạt động ở giữa: hai thẻ còn lại có một dòng di chuyển nối thẳng chúng.
- [ ] Sửa chỉ tên của một hoạt động: các dòng di chuyển giữ nguyên, không nhấp nháy.
- [ ] Kéo thả sang ngày khác mà bị hỏi "Trùng giờ ở ngày mới", chọn "Huỷ": hoạt động về chỗ cũ, các dòng di chuyển như trước khi kéo.

**Kết quả:** Chưa chạy

---

### MT-UI-69 · Chặng di chuyển đi qua một hoạt động không có địa điểm (Task 3.7 Commit 8)

Cần: một ngày có 4 hoạt động theo thứ tự: "Chợ Hàn" (có địa điểm), "Nghỉ trưa, tự do" (**không** có địa điểm), "Cầu Rồng" (có), "Bún chả cá 109" (có).

- [ ] Dưới thẻ "Chợ Hàn" có dòng dạng "… phút · … km **tới Cầu Rồng**". Dưới thẻ "Nghỉ trưa, tự do" không có dòng nào. Dưới thẻ "Cầu Rồng" có dòng thường, không ghi tên đích.
- [ ] Số dòng di chuyển của ngày bằng số chặng mà `GET /api/v1/trips/{tripId}/days/{dayId}/route` trả về (2), và con số của từng dòng trùng với từng chặng.
- [ ] Đổi tên "Cầu Rồng" thành một tên rất dài (80 ký tự): dòng dưới "Chợ Hàn" cắt tên bằng "…", phần "phút · km" vẫn đủ, không tràn ngang (thử cả trên điện thoại giả lập).
- [ ] Sửa "Nghỉ trưa, tự do", gắn một địa điểm, lưu: dòng dưới "Chợ Hàn" đổi thành dòng thường tới thẻ kế tiếp (không còn "tới …"), và "Nghỉ trưa, tự do" có dòng riêng của nó.
- [ ] Bỏ địa điểm đó đi, lưu: trở lại như bước đầu.
- [ ] Thêm một hoạt động không có địa điểm nữa vào giữa "Chợ Hàn" và "Cầu Rồng": vẫn một dòng "… tới Cầu Rồng" dưới "Chợ Hàn".
- [ ] Trình đọc màn hình đọc dòng có tên đích là "Di chuyển, ước tính: … phút · … km tới Cầu Rồng".

**Kết quả:** Chưa chạy

---

### MT-UI-70 · Nhãn "Đã qua" và "Hôm nay" trên trang chuyến đi (Task 3.7 Commit 9)

Cần: một chuyến đi bắt đầu từ **hôm qua**, dài ít nhất 3 ngày (Ngày 1 đã qua, Ngày 2 là hôm nay, Ngày 3 sắp tới); một chuyến đi đã kết thúc từ tuần trước; một chuyến đi tháng sau.

- [ ] Màn hình rộng, cột trái: dưới "Ngày 1 · ngày/tháng" có dòng nhỏ "Đã qua" màu xám và cả mục nhạt hơn các mục khác; dưới "Ngày 2" có dòng "Hôm nay" màu xanh ngọc, in đậm; "Ngày 3" không có nhãn.
- [ ] Bấm vào Ngày 1 (đã qua): mở được như thường; khi đang được chọn, mục có nền xanh nhạt như mọi ngày đang xem và vẫn có dòng "Đã qua".
- [ ] Tiêu đề ngày ở cột giữa: sau "Ngày 1 · thứ, ngày/tháng/năm" có nhãn xám "Đã qua"; ở Ngày 2 là nhãn xanh "Hôm nay"; Ngày 3 không có nhãn.
- [ ] Thu hẹp cửa sổ dần: nhãn ở tiêu đề **không bị ngắt đôi** ("Hôm" một dòng, "nay" một dòng); thiếu chỗ thì cả nhãn xuống dòng dưới. Nút "Thêm hoạt động" vẫn trên một dòng.
- [ ] Các thẻ hoạt động của Ngày 1 **không** bị nhạt đi; thêm, sửa, xoá, kéo thả ở ngày đã qua vẫn làm được.
- [ ] Dưới 1024px: chip "Ngày 2 · …" có chấm xanh nhỏ phía trước; chip "Ngày 1 · …" có nền xám, chữ vẫn đọc rõ; chip đang chọn vẫn nền tối chữ trắng.
- [ ] Chuyến đi đã kết thúc: mọi ngày đều "Đã qua". Chuyến đi tháng sau: không ngày nào có nhãn.
- [ ] Đổi giờ của máy tính sang một múi giờ khác (ví dụ lùi 12 tiếng), tải lại trang: nhãn **không đổi** (tính theo giờ Việt Nam của tài khoản, không theo giờ máy). Trả giờ máy về như cũ.
- [ ] Ô của Ngày 1 trong dải thời tiết ghi "Chưa có" khớp với nhãn "Đã qua" (máy chủ và giao diện cùng coi đó là ngày đã qua).

**Kết quả:** Chưa chạy lại · từng lỗi BUG-UI-010 (chủ dự án thấy tên ngày bị xuống dòng ngày 2026-10-05; đã sửa ở Commit 16, kiểm lại bằng `MT-UI-77`)

---

### MT-UI-71 · Mở chuyến đi đang diễn ra thì vào ngày hôm nay (Task 3.7 Commit 10)

Cần: chuyến đi của `MT-UI-70` (bắt đầu từ hôm qua, dài ít nhất 3 ngày, nên hôm nay là Ngày 2); một chuyến đi tháng sau; một chuyến đi đã kết thúc.

- [ ] Ở trang danh sách, bấm thẻ của chuyến đi đang diễn ra: trang mở ở **Ngày 2**, URL là `/trips/{id}/days/2`, mục Ngày 2 ở cột trái được tô và có dòng "Hôm nay".
- [ ] Bấm nút Back của trình duyệt **một lần**: về trang danh sách (không bị kẹt ở một địa chỉ trung gian).
- [ ] Gõ thẳng `/trips/{id}` vào thanh địa chỉ: cũng mở Ngày 2.
- [ ] Gõ `/trips/{id}/days/1`: mở đúng Ngày 1, **không** bị chuyển sang hôm nay. F5 ở Ngày 3 vẫn ở Ngày 3.
- [ ] Gõ `/trips/{id}/days/99` (ngày không tồn tại): về Ngày 1 như trước.
- [ ] Bấm thẻ của chuyến đi tháng sau, rồi của chuyến đi đã kết thúc: cả hai mở Ngày 1.
- [ ] Đổi trạng thái của chuyến đi đang diễn ra sang "Nháp", mở lại từ danh sách: vẫn vào Ngày 2 (tính theo ngày, không theo trạng thái).
- [ ] Tạo một chuyến đi mới bắt đầu từ hôm qua: hoàn tất wizard thì trang mở ở Ngày 2.
- [ ] Chuyển một hoạt động sang ngày khác rồi bấm "Mở Ngày N" trong thông báo: mở đúng Ngày N.

**Kết quả:** Chưa chạy

---

### MT-UI-72 · Thời tiết trên thẻ ở trang danh sách (Task 3.7 Commit 11)

Cần 5 chuyến đi, đều **đã đặt vị trí điểm đến** trừ chuyến cuối: (a) đang diễn ra (bắt đầu hôm qua, 4 ngày); (b) bắt đầu sau 5 ngày nữa; (c) bắt đầu sau 2 tháng; (d) đã kết thúc tuần trước; (e) bắt đầu ngày mai nhưng **chưa đặt vị trí điểm đến**.

- [ ] Trang danh sách: ở hàng chân thẻ (a), bên phải "4 ngày · … hoạt động", có chữ "Hôm nay", một icon thời tiết có màu và nhiệt độ dạng "32° / 25°". Không có phần trăm mưa.
- [ ] Thẻ (b): cùng vị trí, chữ là "Ngày đi".
- [ ] Thẻ (c), (d), (e): hàng chân thẻ chỉ có "… ngày · … hoạt động", không có khoảng trống hay chữ báo lỗi.
- [ ] Con số trên thẻ (a) trùng với ô của ngày hôm nay trong dải thời tiết khi mở chuyến đi đó; trên thẻ (b) trùng với ô của Ngày 1.
- [ ] Tab Network, lọc `weather`: tải trang danh sách chỉ gọi `/weather/trips/…` cho (a), (b) và (e), **không** gọi cho (c) và (d).
- [ ] Bấm thẻ (a): trang chuyến đi mở ra và dải thời tiết hiện **ngay**, không có khối xám chờ tải.
- [ ] Thu cửa sổ về khoảng 1024px (3 cột hẹp) và về bề ngang điện thoại: thời tiết không đè lên "… ngày · … hoạt động"; thiếu chỗ thì xuống dòng thứ hai, vẫn sát mép phải; thẻ không bị vỡ.
- [ ] Chặn lời gọi thời tiết (như `MT-UI-65`) rồi tải lại trang danh sách: mọi thẻ hiện bình thường, chỉ không có thời tiết; không có thông báo lỗi.
- [ ] Giả lập mạng chậm: các thẻ hiện ngay, thời tiết hiện sau; tiêu đề và ngày đi của thẻ không bị xô lệch khi thời tiết hiện ra.
- [ ] Lọc theo trạng thái, tìm kiếm, sang trang 2 rồi quay lại: thời tiết của các thẻ vẫn đúng.

**Kết quả:** Chưa chạy

---

### MT-UI-73 · Hỏi hoàn thành chuyến đi đã qua ngày cuối (Task 3.7 Commit 12)

Cần: hai chuyến đi **đã kết thúc từ hôm qua trở về trước**, một chuyến trạng thái "Đã lên kế hoạch", một chuyến "Nháp" (tạo với ngày trong quá khứ); một chuyến đi kết thúc **hôm nay**; một chuyến đi đã qua có trạng thái "Đã hoàn thành".

- [ ] Mở chuyến đi đã qua, "Đã lên kế hoạch": hộp thoại hiện ngay, tiêu đề "Hoàn thành chuyến đi?", nội dung 'Chuyến đi **{tên}** đã kết thúc ngày dd/mm/yyyy. Lịch trình vẫn sửa được sau khi hoàn thành.', nút phụ "Để sau" bên trái, nút chính xanh "Hoàn thành chuyến đi" bên phải. Ngày trong câu đúng là ngày cuối của chuyến đi.
- [ ] Bấm "Hoàn thành chuyến đi": nút hiện trạng thái đang lưu, rồi hộp đóng; thông báo "Đã hoàn thành chuyến đi"; ô trạng thái ở đầu trang đổi thành "Đã hoàn thành" mà không cần tải lại. Về trang danh sách: huy hiệu của thẻ và số ở chip "Đã hoàn thành" đã đổi.
- [ ] Sau khi hoàn thành: thêm, sửa, xoá, kéo thả hoạt động vẫn làm được. Tải lại trang: **không** hỏi lại.
- [ ] Mở chuyến đi đã qua còn "Nháp": cũng được hỏi. Bấm "Để sau": hộp đóng, trạng thái giữ nguyên "Nháp", không có thông báo.
- [ ] Lần khác, đóng hộp bằng phím Esc và bằng nút "×": như "Để sau".
- [ ] Chuyến đi kết thúc **hôm nay**: không hỏi. Chuyến đi đã qua và "Đã hoàn thành": không hỏi. Chuyến đi tháng sau: không hỏi.
- [ ] Sau khi hoàn thành, tự đổi trạng thái về "Đang diễn ra" bằng ô trạng thái: hộp hỏi **hiện lại** (chuyến đi đã qua ngày cuối mà trạng thái lại chưa đóng).
- [ ] Tắt máy chủ rồi bấm "Hoàn thành chuyến đi": hộp **không đóng**, trong hộp có khung lỗi; bật máy chủ lại và bấm lần nữa thì thành công.
- [ ] Tên chuyến đi rất dài (200 ký tự): câu trong hộp tự xuống dòng, không tràn khỏi hộp.
- [ ] Điện thoại giả lập: hộp vừa màn hình, hai nút bấm được bằng ngón tay.

Sau khi bấm "Để sau": xem `MT-UI-74`.

**Kết quả:** Chưa chạy

---

### MT-UI-74 · "Để sau" thì không hỏi lại tới lần đăng nhập sau (Task 3.7 Commit 13)

Cần: hai chuyến đi đã qua ngày cuối, trạng thái chưa đóng (gọi là A và B).

- [ ] Mở A, bấm "Để sau". Tải lại trang (F5): **không** bị hỏi lại. Về danh sách rồi mở lại A: không hỏi.
- [ ] Mở B: vẫn được hỏi (mỗi chuyến đi nhớ riêng).
- [ ] Mở A trong một tab khác của cùng trình duyệt: không hỏi.
- [ ] Đóng hộp của B bằng Esc, tải lại trang: không hỏi lại B.
- [ ] DevTools → Application → Local Storage: có mục `trip-planner.complete-later` chứa mã số của A và B; **không có** token hay thông tin tài khoản nào.
- [ ] Đăng xuất: mục đó biến mất khỏi Local Storage. Đăng nhập lại, mở A: **được hỏi lại**.
- [ ] Bấm "Để sau" cho A, rồi tự đổi trạng thái của A sang "Đã hoàn thành" và lại về "Đang diễn ra": không hỏi (vẫn trong lần đăng nhập này).
- [ ] Sửa tay mục trong Local Storage thành `abc`, tải lại trang: trang không lỗi, A và B được hỏi lại.
- [ ] Mở cửa sổ ẩn danh, đăng nhập, mở A, "Để sau", tải lại: không hỏi lại (hoặc hỏi lại nếu trình duyệt chặn bộ nhớ), trang không lỗi trong cả hai trường hợp.

**Kết quả:** Chưa chạy

---

### MT-UI-75 · Dải thời tiết chỉ hiện ngày có dự báo (Task 3.7 Commit 14)

Cần: (a) chuyến đi đã đặt vị trí điểm đến, bắt đầu từ hôm qua, dài 4 ngày; (b) chuyến đi đã đặt vị trí, dài 20 ngày bắt đầu từ hôm nay; (c) chuyến đi đã đặt vị trí, đã kết thúc tuần trước; (d) chuyến đi đã đặt vị trí, bắt đầu sau 2 tháng.

- [ ] Chuyến (a): dải có 3 ô, bắt đầu từ ô của **hôm nay** (Ngày 2). **Không có ô** cho Ngày 1 đã qua, không có chữ "Chưa có" ở đâu.
- [ ] Vẫn ở chuyến (a), mở Ngày 1 (đã qua) từ cột trái: dải giữ nguyên 3 ô, không ô nào được tô nền xanh.
- [ ] Chuyến (b): dải có đúng 16 ô (Ngày 1 đến Ngày 16); Ngày 17 đến 20 không có ô.
- [ ] Chuyến (c): khung thời tiết có icon lịch và câu "Chuyến đi đã qua, không còn dự báo thời tiết." Khung cao như dải bình thường.
- [ ] Chuyến (d): khung có câu "Chưa có dự báo. Dự báo chỉ có cho 16 ngày tới, chuyến đi này bắt đầu sau đó."
- [ ] Chuyến chưa đặt vị trí điểm đến: vẫn là câu mời đặt vị trí của `MT-UI-64` (không phải hai câu trên).
- [ ] Bản đồ phía trên không có vùng xám ở cả bốn trường hợp.
- [ ] Dòng thời tiết ở tiêu đề ngày (màn hẹp) và thời tiết trên thẻ ở trang danh sách không đổi so với trước.

**Kết quả:** Chưa chạy

---

### MT-UI-76 · Chuyển trang dải thời tiết bằng nút (Task 3.7 Commit 15)

Cần: chuyến đi (b) của `MT-UI-75` (20 ngày bắt đầu từ hôm nay, 16 ngày có dự báo) và chuyến đi (a) (3 ngày có dự báo). Màn hình rộng từ 1280px (cột bản đồ 420px).

- [ ] Chuyến (b), Ngày 1: dải có **4 ô** (Ngày 1 đến 4), một nút "‹" ở đầu trái và một nút "›" ở đầu phải. **Không có thanh cuộn ngang**, kéo chuột hay lăn bánh xe ngang trên dải không làm gì.
- [ ] Nút "‹" đang mờ và không bấm được (đang ở trang đầu). Bấm "›": dải đổi sang Ngày 5 đến 8, cả 4 ô cùng đổi. Trang chuyến đi **không** đổi ngày (URL giữ nguyên).
- [ ] Bấm "›" tới trang cuối (Ngày 13 đến 16): nút "›" mờ đi. Bấm "‹" quay lại được.
- [ ] Đang ở trang Ngày 13–16, bấm ô "N14": trang chuyển sang Ngày 14, dải vẫn ở trang đó và ô N14 được tô.
- [ ] Ở cột trái bấm Ngày 6: dải **tự lật** về trang Ngày 5–8, ô N6 được tô.
- [ ] Ở cột trái bấm Ngày 18 (không có dự báo): dải giữ nguyên trang đang xem, không ô nào được tô.
- [ ] Chuyến (a) (3 ngày có dự báo): không có nút nào, 3 ô chia đều bề ngang.
- [ ] Tạo chuyến đi có đúng 4 ngày có dự báo: 4 ô, không nút. Có 5 ngày: có nút, trang 1 có 4 ô, trang 2 có **1 ô rộng bằng các ô ở trang 1** (phần còn lại để trống, ô không bị kéo giãn).
- [ ] Thu cửa sổ xuống khoảng 1100px (cột bản đồ 360px): mỗi trang còn 3 ô; ngày đang xem vẫn nằm trong trang đang hiện. Nới rộng lại: trở về 4 ô.
- [ ] Trong mỗi ô, "32° / 25°" nằm trên một dòng, không bị cắt chữ.
- [ ] Dưới 1024px, tab "Bản đồ": dải rộng hết màn hình, số ô mỗi trang nhiều hơn; trên điện thoại (390px) còn 3 ô mỗi trang và hai nút đủ lớn để chạm.
- [ ] Bàn phím: Tab đi qua nút "‹", các ô, nút "›"; Enter trên nút chuyển trang. Trình đọc màn hình đọc nút là "Các ngày trước" / "Các ngày sau".
- [ ] Bản đồ phía trên không có vùng xám; chiều cao dải không đổi khi chuyển trang.

**Kết quả:** Chưa chạy

---

### MT-UI-77 · Tên ngày ở cột giữa không bị xuống dòng (Task 3.7 Commit 16, BUG-UI-010)

Cần: một chuyến đi dài ít nhất 12 ngày có ngày rơi vào Chủ nhật và Thứ năm (tên thứ dài), trong đó có ngày hôm nay và ngày đã qua; một ngày có tiêu đề dài và ghi chú dài; một ngày có hơn 8 hoạt động để cuộn được.

- [ ] Màn hình 1366px và 1920px: dòng "Ngày 12 · Chủ nhật, dd/mm/yyyy" nằm trên **một dòng**, nhãn "Hôm nay" / "Đã qua" đứng ngay sau nó. Hàng thứ hai: tiêu đề của ngày ở bên trái, các nút "Sửa" và "+ Thêm hoạt động" ở bên phải.
- [ ] Thu cửa sổ về 1024px (ba cột hẹp nhất): tên ngày vẫn trên một dòng; nếu thiếu chỗ thì **cả nhãn** xuống dòng dưới, chữ của tên ngày không bị bẻ. Nút chính ghi "Thêm" (ngắn); từ 1280px trở lên ghi "Thêm hoạt động".
- [ ] Cuộn xuống quá một màn hình ở ngày dài: nút "↑ Đầu ngày" hiện ra cạnh "Sửa"; tên ngày **không đổi vị trí và không xuống dòng**. Khối tiêu đề vẫn dính ở trên, ngang hàng với cột ngày bên trái, các thẻ cuộn bên dưới không lộ ra phía trên khối.
- [ ] Ngày có tiêu đề dài và ghi chú dài: tiêu đề tự xuống dòng trong phần của nó, không đẩy các nút ra khỏi màn hình; "Đọc thêm" của ghi chú vẫn bấm được.
- [ ] Điện thoại giả lập 390px và 320px: tên ngày trên một dòng (ở 320px được phép xuống dòng nếu thật sự không vừa), dưới đó là tiêu đề của ngày, dòng thời tiết, và các nút "Sửa", "Thêm" ở bên phải.
- [ ] Bấm "Sửa" của ngày: form sửa hiện ra như trước; "Huỷ" trả lại khối tiêu đề hai hàng.
- [ ] Trình đọc màn hình vẫn đọc tên vùng của ngày theo dòng tiêu đề ("Ngày 2 · Thứ ba, …").

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
| BUG-UI-009 | MT-UI-56, bước 2 | 2026-10-04 | Task 3.6, sau Commit 19. Chủ dự án thử trên trình duyệt: bản đồ của ngày hiện bình thường ở cột bên phải, nhưng bấm "Phóng to bản đồ" thì khung phủ cả cửa sổ **không hiện bản đồ** | Thư viện bản đồ đo kích thước khung của nó đúng một lần, lúc bản đồ được tạo. Bản đồ phóng to nằm trong một hộp thoại, và hộp thoại chỉ được mở **sau khi** bản đồ bên trong đã được tạo: lúc đó khung còn ẩn, kích thước đo được là 0 × 0, nên bản đồ không tải ô nền nào. Hai lệnh kiểm tra mã nguồn không thấy được vì thứ tự này chỉ lộ ra khi chạy thật. Bản đồ nhỏ trong hộp "Sửa chuyến đi" (Commit 19, chưa commit) có cùng nguyên nhân từ lần mở thứ hai, tìm ra khi rà lại và sửa luôn trong Commit 19 | Bấm "Phóng to bản đồ" thì mở hộp thoại trước, rồi mới tạo bản đồ bên trong. Bản đồ chọn điểm tự đo lại mỗi khi khung của nó đổi kích thước | Đã sửa (`c712093`). Chủ dự án thử lại ngày 2026-10-04 và xác nhận bản đồ phóng to đã hiện. Phần của hộp "Sửa chuyến đi" (`72e97d5`) chưa được kiểm lại |
| BUG-UI-010 | MT-UI-70 (bước nhãn ở tiêu đề ngày), MT-UI-37 | 2026-10-05 | Task 3.7, sau Commit 15. Chủ dự án xem trên trình duyệt: ở cột giữa của trang chuyến đi, dòng "Ngày N · thứ, ngày/tháng/năm" bị xuống dòng với những ngày có tên thứ và ngày dài (ví dụ "Chủ nhật", "Thứ năm", ngày có hai chữ số) | Tên ngày nằm chung một hàng với các nút "Sửa" và "+ Thêm hoạt động" (và "↑ Đầu ngày" khi đã cuộn). Các nút không co lại, nên tên ngày chỉ được phần còn thừa. Từ khi có cột bản đồ (Task 3.6), cột giữa chỉ còn rộng 368px đến khoảng 530px: trừ các nút đi, tên ngày còn chừng 120 đến 280px trong khi nó cần khoảng 290px. Nhãn "Hôm nay" / "Đã qua" thêm ở Commit 9 của task này lấy thêm 70px nữa và làm lỗi lộ rõ. Lint, build và test tự động không thấy vì đây là kết quả hiển thị | Khối tiêu đề của ngày chia thành hai hàng: tên ngày và nhãn đứng riêng một hàng, rộng hết cột; hàng dưới là tiêu đề của ngày (bên trái) và các nút (bên phải). Ở bố cục ba cột dưới 1280px, nút chính dùng chữ ngắn "Thêm" như trên điện thoại | Đã sửa ở Commit 16 (`6955238`), chờ chủ dự án kiểm lại bằng `MT-UI-77` |

BUG-UI-003 đến BUG-UI-008 được tìm ra bằng cách đọc lại code ngày 2026-10-01, trước khi vào Phase 3, vì 41 bài kiểm tra thủ công của file này chưa được chạy. Ít nhất hai lỗi (006, 008) nằm đúng ở bước mà một bài có sẵn sẽ kiểm. Cột "Kết quả" của các bài `MT-UI` vẫn là "Chưa chạy": chỉ người thật chạy mới được ghi kết quả.

BUG-UI-002 cũng là loại lỗi chỉ thấy bằng mắt: mỗi phần (khối dính, chấm trên ray) đều đúng khi đứng riêng, lỗi chỉ xuất hiện khi hai phần chồng lên nhau lúc cuộn.

BUG-UI-001 không bị hai lệnh kiểm tra mã nguồn bắt được: mã nguồn đúng cú pháp và đúng kiểu dữ liệu, chỉ có kết quả hiển thị sai. Đây là loại lỗi mà chỉ người nhìn giao diện thật mới thấy, và là lý do mỗi mốc giao diện đều cần kiểm tra thủ công.

Bài học: không ghi đè một lớp giao diện bằng một lớp khác cùng thuộc tính. Thuộc tính nào cần thay đổi theo từng chỗ dùng thì đưa thành tuỳ chọn của component.

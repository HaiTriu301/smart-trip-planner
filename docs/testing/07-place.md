# 07 · Địa điểm

> Cập nhật: 2026-10-03 · build xanh tại commit `e75152c` (Task 3.5, 769 lượt test) · kiểm tra thủ công `MT-PLACE-01` đến `MT-PLACE-05` chưa chạy · [Về trang chính](README.md)

Địa điểm là một nơi có tên và toạ độ, ví dụ "Chùa Linh Ứng". Người dùng tìm địa điểm theo tên rồi gắn vào một hoạt động; từ đó hoạt động hiện được trên bản đồ. Tìm địa điểm làm ở Task 3.1, gắn vào hoạt động ở Task 3.2.

File này được ghi dần theo từng mốc. Mỗi mốc là một commit:

| Mốc | Nội dung | Phần trong file | Commit |
|---|---|---|---|
| 1 | Tách phần bỏ dấu tiếng Việt thành hàm dùng chung (chuẩn bị cho tìm kiếm không dấu) | A | `f0cd0f8` |
| 2 | Tìm địa điểm theo tên hoặc địa chỉ, trên dữ liệu có sẵn của Đà Nẵng | B, C | `458a670` |
| 3 | Địa điểm quanh điểm đến của chuyến đi được xếp trước | D | `0ff1b50` |
| 4 | Mở rộng dữ liệu có sẵn lên 56 địa điểm ở 5 điểm đến, kèm kiểm tra chính dữ liệu | E | `3ba2e85` |
| 5 | Kiểm toàn luồng tìm địa điểm qua mọi tầng | F | `2c84e02` |

Task 3.2, gắn địa điểm vào hoạt động (phần của hoạt động ghi ở [05-activity.md](05-activity.md)):

| Commit | Nội dung | Phần trong file | Mã commit |
|---|---|---|---|
| 1 | Bảng lưu địa điểm | G (`TC-PLACE-050` đến `055`) | `5b9088f` |
| 2 | Hỏi nguồn một địa điểm theo mã của nó | H (`TC-PLACE-057`) | `9c7d7af` |
| 3 | Chọn một kết quả tìm kiếm: lưu thành địa điểm có mã riêng của ứng dụng | G (`056`), H | `914f80e` |
| 4 | Nhiều người chọn cùng một địa điểm cùng lúc vẫn chỉ có một dòng | H (`TC-PLACE-063`, `064`) | `e04c842` |
| 5 | Tự thêm một địa điểm khi không kết quả tìm kiếm nào phù hợp | I | `db89c31` |
| 6 | Từ chối việc chọn địa điểm từ một nguồn không phải nguồn đang dùng | H (`TC-PLACE-083`, `084`) | `8ba7137` |
| 7 | Dọn test của hoạt động trước khi thêm địa điểm: mọi test tạo dữ liệu hoạt động qua một chỗ chung. Không đổi hành vi, không có kịch bản mới | (không có) | `19bad4a` |
| 8a, 8b | Hoạt động lưu và trả kèm địa điểm của nó | [05-activity.md](05-activity.md) phần O, P | `cb5b475`, `744c3ec` |
| 9 | Địa điểm nào được gắn vào hoạt động | J | `9abd7cd` |
| 10, 11 | Đổi và bỏ địa điểm của hoạt động | [05-activity.md](05-activity.md) phần R, S | `6c2140a`, `449e4ba` |
| 12 | Kiểm toàn luồng chọn và tự thêm địa điểm qua mọi tầng | K (và [05-activity.md](05-activity.md) phần T) | `532bc80` |

Bốn commit trên vốn là một mốc 17 file (`e492ed3`), được tách lại ngày 2026-10-01 theo yêu cầu của chủ dự án trước khi push. Kịch bản và kết quả không đổi; chỉ cách chia commit đổi.

Task 3.4, giữ tạm kết quả tìm địa điểm trong Redis (kết nối Redis và health ghi ở [01-platform.md](01-platform.md) phần F, G):

| Commit | Nội dung | Phần trong file | Mã commit |
|---|---|---|---|
| 3 | Quy tắc "hai lần tìm nào là cùng một câu hỏi" (khoá của bản giữ tạm) | L | `26780ce` |
| 4 | Giữ tạm kết quả tìm địa điểm trong Redis 24 giờ | M | `a93a4b5` |
| fix | Ghi vào Redis xong rồi mới trả về (BUG-PLACE-003, phát hiện khi làm Commit 6) | M (`TC-PLACE-106`), "Lỗi đã phát hiện" | `8b7c864` |

Task 3.5, quãng đường di chuyển trong một ngày:

| Commit | Nội dung | Phần trong file | Mã commit |
|---|---|---|---|
| 1 | Nguồn bản đồ ước lượng quãng đường và thời gian giữa các điểm liên tiếp | N | `eec39ad` |
| 2 | API quãng đường của một ngày, cho ngày mà mọi hoạt động đều có địa điểm | O | `2d2c40e` |
| 3 | Bỏ qua hoạt động không có địa điểm khi tính quãng đường | P | `727ae69` |
| 4 | Ngày có 0 hoặc 1 địa điểm: trả lời rỗng ngay, không hỏi nguồn bản đồ | Q | `159df83` |
| 5 | Kiểm toàn luồng quãng đường của một ngày qua mọi tầng | R | `e75152c` |

Vài từ dùng trong file:

| Từ | Nghĩa |
|---|---|
| Bỏ dấu | Đổi chữ có dấu thành chữ không dấu: "Chùa Linh Ứng" thành "Chua Linh Ung" |
| Nguồn địa điểm | Nơi ứng dụng lấy danh sách địa điểm: một file có sẵn (mock) hoặc dịch vụ bản đồ thật |
| Mã ở nguồn | Mã mà nguồn tự đặt cho địa điểm, ví dụ `da-nang-cho-han`. Kết quả tìm kiếm chỉ có mã này |
| Bản lưu | Bản chép của địa điểm trong database của ứng dụng, có mã riêng (`id`). Hoạt động trỏ tới bản lưu, nên trang chuyến đi không phải hỏi lại nguồn |
| Địa điểm tự thêm | Địa điểm do người dùng tự nhập tên và toạ độ, không lấy từ nguồn nào. Là của riêng người tạo |
| Chặng | Đoạn di chuyển giữa hai điểm liền nhau của một ngày, gồm quãng đường (mét) và thời gian (giây) |

---

## A. Bỏ dấu tiếng Việt

> **Yêu cầu:** design.md 10.2 "Quy ước Place API" (tìm không phân biệt dấu), 5.2 bảng `trips` (tên rút gọn) · **Kiểm bởi:** `VietnameseTextTest`, `SlugGeneratorTest`

Người dùng thường gõ không dấu ("linh ung") nhưng tên địa điểm có dấu. Để hai bên khớp nhau, cả từ khoá lẫn tên địa điểm được bỏ dấu trước khi so. Phần bỏ dấu này vốn nằm trong chức năng tạo tên rút gọn của chuyến đi; Mốc 1 tách nó ra để hai chức năng dùng chung, **không đổi hành vi nào**.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-PLACE-001 | Bỏ dấu "Chùa Linh Ứng", "Bún chả cá 109 Nguyễn Chí Thanh" và đủ các nguyên âm có dấu (ă, â, ê, ô, ơ, ư) | Ra chữ không dấu. Chữ hoa, chữ thường, số và khoảng trắng giữ nguyên | Đúng | Đạt |
| TC-PLACE-002 | Bỏ dấu "Đà Nẵng đẹp" | "Da Nang dep". Chữ Đ là một chữ cái riêng, phải được xử lý riêng, nếu không sẽ còn nguyên | Biên | Đạt |
| TC-PLACE-003 | Cùng chữ "Hà Nội" nhưng được mã hoá theo hai cách (dấu liền với chữ, hoặc dấu là một ký tự riêng đứng sau chữ) | Hai cách đều ra "Ha Noi". Bàn phím và thao tác dán có thể gửi lên một trong hai dạng | Biên | Đạt |
| TC-PLACE-004 | Bỏ dấu chữ vốn không có dấu, ký hiệu, chữ Nhật, chuỗi rỗng | Giữ nguyên, không làm mất ký tự nào | Biên | Đạt |
| TC-PLACE-005 | Sau khi tách hàm, chạy lại toàn bộ kịch bản về tên rút gọn của chuyến đi (`TC-TRIP-009` đến `TC-TRIP-014` ở [03-trip.md](03-trip.md)) | Tất cả vẫn đạt: tên rút gọn sinh ra không đổi | Đúng | Đạt |

## B. Tìm trong dữ liệu có sẵn

> **Yêu cầu:** design.md 7.2 (`MapProvider`), 10.2 "Quy ước Place API" · **Kiểm bởi:** `MockMapProviderTest`, `PlaceServiceTest`

Nguồn địa điểm là một file có sẵn: 13 địa điểm ở Đà Nẵng từ Mốc 2, mở rộng lên 56 địa điểm ở 5 điểm đến từ Mốc 4 (phần E). Tên, địa chỉ và toạ độ được tra từ OpenStreetMap ngày 2026-10-01. Test chạy trên chính file này, không gọi mạng.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-PLACE-006 | Tìm "linh ung", rồi "LINH ỨNG", rồi "Linh  Ung" (hai dấu cách) | Cả ba cách gõ đều ra đúng một kết quả: "Chùa Linh Ứng" | Đúng | Đạt |
| TC-PLACE-007 | Xem một kết quả tìm kiếm | Có đủ: nguồn, mã của địa điểm ở nguồn, tên, địa chỉ, vĩ độ, kinh độ, loại. Đây là những gì giao diện cần để hiện gợi ý và để chọn địa điểm sau này | Đúng | Đạt |
| TC-PLACE-008 | Tìm "son tra", chữ chỉ có trong **địa chỉ**, không có trong tên nào | Ra "Chùa Linh Ứng" (ở Phường Sơn Trà) | Đúng | Đạt |
| TC-PLACE-009 | Tìm nhiều từ: "cho hai chau", rồi "cho son tra" | Mọi từ đều phải có trong tên hoặc địa chỉ. Lần đầu ra "Chợ Hàn" và "Chợ Cồn"; lần sau không ra gì, vì không có chợ nào ở Sơn Trà trong dữ liệu | Biên | Đạt |
| TC-PLACE-010 | Tìm "cho", rồi "da nang" | Thứ tự: địa điểm có tên **bắt đầu** bằng từ khoá, rồi tên **chứa** từ khoá, cuối cùng là địa điểm chỉ khớp qua địa chỉ | Đúng | Đạt |
| TC-PLACE-011 | Tìm cùng một từ khoá nhiều lần | Luôn ra cùng danh sách, cùng thứ tự. Nhờ vậy test không lúc đạt lúc lỗi và người dùng không thấy gợi ý nhảy chỗ | Đúng | Đạt |
| TC-PLACE-012 | Tìm "da nang" và chỉ xin 3 kết quả | Trả đúng 3 kết quả, là 3 kết quả đứng đầu của danh sách đầy đủ | Biên | Đạt |
| TC-PLACE-013 | Tìm một từ khoá không khớp địa điểm nào | Danh sách rỗng, không báo lỗi | Biên | Đạt |
| TC-PLACE-014 | Từ khoá có khoảng trắng thừa ở đầu và cuối | Khoảng trắng bị cắt trước khi tìm; kết quả giữ nguyên thứ tự và đủ mọi thông tin khi trả về | Biên | Đạt |

Kiểm chứng ngược (2026-10-01): tạm tắt bước bỏ dấu thì 5 trong 8 test của `MockMapProviderTest` đỏ; bật lại thì xanh. Điều đó chứng minh các test thật sự bảo vệ quy tắc "tìm không phân biệt dấu".

## C. Tìm địa điểm qua API

> **Yêu cầu:** design.md 10.2 "Quy ước Place API", 6.3 · **Kiểm bởi:** `PlaceControllerTest`

`GET /api/v1/places/search?q=...&limit=...`. Ai đã đăng nhập cũng tìm được; kết quả không thuộc về chuyến đi nào.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-PLACE-015 | Người đã đăng nhập tìm "linh ung" | 200. Mỗi kết quả có nguồn, mã ở nguồn, tên, địa chỉ, toạ độ, loại, và **không có mã riêng của ứng dụng**: địa điểm chưa được lưu cho tới khi người dùng chọn nó | Đúng | Đạt |
| TC-PLACE-016 | Tìm mà không nêu số kết quả, rồi tìm với số kết quả 20 | Lần đầu lấy tối đa 8, lần sau tối đa 20 | Đúng | Đạt |
| TC-PLACE-017 | Tìm một từ khoá không khớp gì | 200 với danh sách rỗng, không phải lỗi | Biên | Đạt |
| TC-PLACE-018 | Tìm khi chưa đăng nhập | 401, không tìm gì cả | Bảo mật | Đạt |
| TC-PLACE-019 | Gọi mà không gửi từ khoá | 400 `VALIDATION_ERROR`, nêu tham số `q`: "Thiếu tham số bắt buộc" | Sai | Đạt |
| TC-PLACE-020 | Từ khoá chỉ có một ký tự, một ký tự kẹp giữa khoảng trắng, toàn khoảng trắng, hoặc rỗng | 400 trong cả bốn trường hợp: "Từ khoá tìm địa điểm cần ít nhất 2 ký tự" | Biên | Đạt |
| TC-PLACE-021 | Từ khoá dài **đúng 2** ký tự, **đúng 100** ký tự, rồi **101** ký tự | Hai lần đầu được nhận. Lần thứ ba 400: "Từ khoá tìm địa điểm không được vượt quá 100 ký tự" | Biên | Đạt |
| TC-PLACE-022 | Số kết quả là 0, 21, hoặc số âm | 400, nêu tham số `limit`: "Số kết quả phải nằm trong khoảng 1 đến 20" | Biên | Đạt |
| TC-PLACE-023 | Số kết quả không phải là số | 400 `VALIDATION_ERROR`, không phải lỗi 500 | Sai | Đạt |

## D. Ưu tiên địa điểm quanh một toạ độ

> **Yêu cầu:** design.md 10.2 "Quy ước Place API" · **Kiểm bởi:** `CoordinateTest`, `MockMapProviderTest`, `PlaceServiceTest`, `PlaceControllerTest`

Khi lên lịch cho một chuyến đi Đà Nẵng, người dùng gõ "chợ" là muốn chợ ở Đà Nẵng, không phải chợ Bến Thành. Giao diện sẽ gửi kèm toạ độ điểm đến của chuyến đi (`lat`, `lng`). Quy tắc xếp thứ tự khi có toạ độ:

1. Địa điểm **trong bán kính 50 km** quanh toạ độ đứng trước mọi địa điểm ở xa hơn.
2. Trong mỗi nhóm: theo độ khớp tên như phần B.
3. Cùng độ khớp tên: địa điểm gần hơn đứng trước.

Toạ độ chỉ đổi **thứ tự**, không bao giờ làm mất kết quả. Khoảng cách là đường chim bay.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-PLACE-024 | Tính khoảng cách Hà Nội đến TP. Hồ Chí Minh, Chợ Hàn đến Cầu Rồng, và một độ vĩ tuyến | Khoảng 1.144 km, 887 m và 111 km, sai lệch không đáng kể | Đúng | Đạt |
| TC-PLACE-025 | Tính khoảng cách theo chiều ngược lại, và từ một điểm tới chính nó | Hai chiều bằng nhau; tới chính nó là 0 | Biên | Đạt |
| TC-PLACE-026 | Tìm "cho" mà **không** gửi toạ độ, trên dữ liệu có chợ ở hai thành phố | Thứ tự chỉ theo tên, như phần B: Chợ Bến Thành, Chợ Hàn, Chợ Cồn, rồi "Siêu thị cạnh chợ Cồn" | Đúng | Đạt |
| TC-PLACE-027 | Cũng tìm "cho" nhưng gửi toạ độ trung tâm Đà Nẵng, rồi toạ độ trung tâm TP. Hồ Chí Minh | Với Đà Nẵng: cả ba địa điểm ở Đà Nẵng đứng trước, kể cả địa điểm khớp tên yếu hơn; Chợ Bến Thành xuống cuối. Với TP. Hồ Chí Minh: Chợ Bến Thành lên đầu | Đúng | Đạt |
| TC-PLACE-028 | Hai chợ cùng ở Đà Nẵng, tên cùng bắt đầu bằng từ khoá | Chợ Cồn (cách trung tâm 2,0 km) đứng trước Chợ Hàn (2,8 km) | Biên | Đạt |
| TC-PLACE-029 | Tìm "da nang" trên dữ liệu thật với toạ độ ở Sơn Trà, rồi ở Bà Nà | Hai lần ra **cùng tập kết quả**, chỉ khác thứ tự: gần Sơn Trà thì Chùa Linh Ứng đứng trước Bà Nà Hills, gần Bà Nà thì ngược lại. Gọi lại với cùng toạ độ ra cùng thứ tự | Đúng | Đạt |
| TC-PLACE-030 | Gửi đủ cả vĩ độ và kinh độ | Hai số được ghép thành một điểm và chuyển cho nguồn địa điểm | Đúng | Đạt |
| TC-PLACE-031 | Chỉ gửi vĩ độ, hoặc chỉ gửi kinh độ | 400 `VALIDATION_ERROR`, báo ở số **còn thiếu**: "Cần gửi đủ cả vĩ độ và kinh độ, hoặc bỏ cả hai". Không tìm gì cả | Sai | Đạt |
| TC-PLACE-032 | Vĩ độ 90,0000001 hoặc −91 | 400, báo ở `lat`: "Vĩ độ phải nằm trong khoảng -90 đến 90" | Biên | Đạt |
| TC-PLACE-033 | Kinh độ 180,0000001 hoặc −181 | 400, báo ở `lng`: "Kinh độ phải nằm trong khoảng -180 đến 180" | Biên | Đạt |
| TC-PLACE-034 | Vĩ độ đúng 90 và kinh độ đúng −180 | Được nhận | Biên | Đạt |
| TC-PLACE-035 | Vĩ độ không phải là số | 400 `VALIDATION_ERROR`, không phải lỗi 500 | Sai | Đạt |

Kiểm chứng ngược (2026-10-01): tạm cho nguồn địa điểm bỏ qua toạ độ thì 3 test của `MockMapProviderTest` đỏ (`TC-PLACE-027`, `028`, `029`); bật lại thì xanh.

## E. Dữ liệu có sẵn của 5 điểm đến

> **Yêu cầu:** design.md 7.2 (dữ liệu mock của `MapProvider`) · **Kiểm bởi:** `MockPlacesDataTest`, `MockMapProviderTest`

File `mock/places.json` có 56 địa điểm: Đà Nẵng 13, Hà Nội 12, Hội An 10, Đà Lạt 11, TP. Hồ Chí Minh 10. Tên, địa chỉ và toạ độ đều tra từ OpenStreetMap ngày 2026-10-01; địa chỉ theo tên phường và tỉnh thành hiện hành trên OpenStreetMap (ví dụ Hội An nay thuộc Đà Nẵng, Đà Lạt thuộc Lâm Đồng).

Các kịch bản dưới đây kiểm **chính dữ liệu**, không kiểm code. Một lỗi gõ trong file (đảo vĩ độ với kinh độ, trùng mã, loại viết sai) nếu không có chúng sẽ chỉ lộ ra khi một điểm đánh dấu rơi xuống biển.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-PLACE-036 | Đọc phần ghi nguồn của file | Có ghi "OpenStreetMap contributors" và giấy phép ODbL, đúng điều kiện sử dụng dữ liệu của họ | Đúng | Đạt |
| TC-PLACE-037 | Duyệt từng địa điểm | Địa điểm nào cũng có mã, tên, địa chỉ và đủ cả hai toạ độ | Đúng | Đạt |
| TC-PLACE-038 | So mã và tên của mọi địa điểm với nhau | Không mã nào trùng, không tên nào trùng (hai dòng gợi ý giống hệt nhau thì người dùng không phân biệt được) | Biên | Đạt |
| TC-PLACE-039 | Xem loại của từng địa điểm | Luôn là một trong 6 loại của hoạt động, để form gợi ý sẵn được loại | Đúng | Đạt |
| TC-PLACE-040 | So toạ độ của từng địa điểm với trung tâm điểm đến của nó | Địa điểm nào cũng thuộc một trong 5 điểm đến và nằm trong vòng 40 km quanh trung tâm. Bắt được lỗi đảo vĩ độ với kinh độ, thiếu chữ số, xếp nhầm thành phố | Biên | Đạt |
| TC-PLACE-041 | Đếm địa điểm của từng điểm đến | Mỗi điểm đến có ít nhất 8 địa điểm và đủ bốn loại: tham quan, ăn uống, lưu trú, di chuyển | Đúng | Đạt · từng lỗi BUG-PLACE-001 |
| TC-PLACE-042 | Tìm từng địa điểm bằng chính tên của nó, gõ không dấu | Địa điểm đó có trong 5 kết quả đầu | Đúng | Đạt |
| TC-PLACE-043 | Trên dữ liệu thật, tìm "cho" với toạ độ của TP. Hồ Chí Minh, của Hà Nội, rồi của Đà Nẵng | Chợ của đúng thành phố đó đứng đầu: Bến Thành và Bình Tây; Đồng Xuân; Cồn, Hàn rồi Hội An (cách Đà Nẵng 25 km, vẫn trong vòng 50 km) | Đúng | Đạt |

## F. Kiểm toàn luồng qua mọi tầng

> **Yêu cầu:** design.md 10.2 "Quy ước Place API", 7.1 · **Kiểm bởi:** `PlaceSearchFlowIntegrationTest`

Các phần trên kiểm từng tầng riêng, tầng bên cạnh được thay bằng bản giả. Phần này chạy **cả ứng dụng thật**: bộ lọc đăng nhập, controller, service, nguồn địa điểm do chính ứng dụng tự chọn, và phần đổi sang JSON. Không có gì bị thay thế.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-PLACE-044 | Khởi động ứng dụng với cấu hình mặc định, không có API key nào | Nguồn địa điểm duy nhất được nạp là file có sẵn. Ứng dụng tìm được địa điểm ngay, không cần mạng | Đúng | Đạt |
| TC-PLACE-045 | Người đã đăng nhập tìm "linh ung", rồi tìm "Chùa Linh Ứng" có dấu | Cả hai lần ra "Chùa Linh Ứng" với đủ nguồn, mã, địa chỉ, toạ độ, loại, và không có mã riêng của ứng dụng. Chữ có dấu đi qua đường dẫn không bị hỏng | Đúng | Đạt |
| TC-PLACE-046 | Tìm "cho" không kèm toạ độ, kèm toạ độ TP. Hồ Chí Minh, rồi kèm toạ độ Hà Nội | Chợ đứng đầu lần lượt là Chợ Hàn, Chợ Bến Thành, Chợ Đồng Xuân. Ba lần ra **cùng tập kết quả**, chỉ khác thứ tự | Đúng | Đạt |
| TC-PLACE-047 | Tìm "cho" chỉ xin 2 kết quả; tìm một từ khoá không khớp gì | Lần đầu đúng 2 kết quả đứng đầu; lần sau 200 với danh sách rỗng | Biên | Đạt |
| TC-PLACE-048 | Gọi thiếu từ khoá; từ khoá một ký tự; chỉ gửi vĩ độ | Cả ba đều 400 `VALIDATION_ERROR` với câu tiếng Việt đúng ở đúng tham số. Riêng lỗi "chỉ gửi vĩ độ" do tầng service phát hiện, nên chỉ test toàn luồng mới chứng minh được nó ra tới người gọi | Sai | Đạt |
| TC-PLACE-049 | Tìm khi không có token | 401 `UNAUTHORIZED` | Bảo mật | Đạt |

## G. Bảng lưu địa điểm

> **Yêu cầu:** design.md 5.2 bảng `places` · **Kiểm bởi:** `PlaceMappingTest`, `PlaceRepositoryTest`

Chạy trên MySQL thật. Phần này kiểm cái "tủ" chứa bản lưu: cột nào cũng giữ đúng dữ liệu, và các chốt chặn cuối cùng ở database hoạt động.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-PLACE-050 | Lưu một địa điểm rồi đọc lại | Nguồn, mã ở nguồn, tên, địa chỉ, loại đọc lại đúng. Toạ độ giữ đủ 7 chữ số thập phân (khoảng 1 cm trên mặt đất) | Đúng | Đạt |
| TC-PLACE-051 | Lưu địa điểm không có địa chỉ và không có loại | Lưu được; hai ô đó để trống | Biên | Đạt |
| TC-PLACE-052 | Lưu lần thứ hai cùng một địa điểm của cùng một nguồn | Database từ chối. Đây là chốt chặn khi hai người chọn cùng địa điểm đúng cùng lúc | Biên | Đạt |
| TC-PLACE-053 | Hai mã ở nguồn chỉ khác chữ hoa và chữ thường: `W123` và `w123` | Là **hai** địa điểm khác nhau, lưu được cả hai; tìm `w123` không ra `W123`. Mã không phải chữ để đọc, phải so từng ký tự | Biên | Đạt |
| TC-PLACE-054 | Ghi thẳng vào database một toạ độ ngoài khoảng (vĩ độ quá 90, kinh độ quá 180, cả hai chiều âm dương); rồi ghi đúng mép 90 và −180 | Bốn lần đầu bị từ chối, nêu đúng ràng buộc bị vi phạm. Đúng mép thì được nhận | Biên | Đạt · từng lỗi BUG-PLACE-002 |
| TC-PLACE-055 | Ghi địa điểm của nguồn OpenStreetMap, và hai địa điểm tự thêm không có mã ở nguồn | Cả ba lưu được: bảng đã sẵn sàng cho các mốc sau, và địa điểm tự thêm không đụng nhau ở ràng buộc không trùng | Đúng | Đạt |
| TC-PLACE-056 | Tìm bản lưu theo nguồn và mã ở nguồn, khi đã có và khi chưa có | Đã có thì trả đúng dòng đó; chưa có thì trả "không có" | Đúng | Đạt |

## H. Chọn một kết quả tìm kiếm

> **Yêu cầu:** design.md 10.2 "Quy ước Place API", rule 14.18 · **Kiểm bởi:** `MockMapProviderTest`, `PlaceServiceTest`, `PlaceControllerTest`, `PlaceSearchFlowIntegrationTest`

`POST /api/v1/places` với `{ provider, externalId }`. Người dùng nói "tôi chọn kết quả này"; máy chủ **tự hỏi lại nguồn** rồi lưu, và trả địa điểm có `id`. Tên và toạ độ không bao giờ lấy từ người gọi, vì bản lưu dùng chung cho mọi người.

Các kịch bản dưới đây được kiểm ở từng tầng (nguồn, service, controller). Riêng `TC-PLACE-063` chạy trên cả ứng dụng thật với MySQL. Toàn bộ luồng chọn địa điểm trên ứng dụng thật được kiểm ở phần K.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-PLACE-057 | Hỏi nguồn một địa điểm theo mã của nó; rồi hỏi bằng mã lạ, bằng tên, bằng mã viết hoa | Mã đúng thì trả đúng địa điểm đã hiện lúc tìm kiếm. Ba trường hợp sau đều "không có" | Đúng | Đạt |
| TC-PLACE-058 | Chọn một kết quả lần đầu | 200. Một bản lưu được tạo với tên, địa chỉ, toạ độ, loại **lấy từ nguồn**; phản hồi có `id` | Đúng | Đạt |
| TC-PLACE-059 | Chọn lại đúng kết quả đó | 200 với **cùng `id`**. Không hỏi nguồn, không ghi thêm; database vẫn một dòng | Đúng | Đạt |
| TC-PLACE-060 | Người gọi bỏ qua giao diện, gửi kèm tên "Giữa biển" và toạ độ giả cho một địa điểm có thật | Phần gửi thừa bị bỏ qua. Bản lưu vẫn là "Chùa Linh Ứng" với toạ độ của nguồn | Bảo mật | Đạt |
| TC-PLACE-061 | Mã ở nguồn có khoảng trắng thừa ở hai đầu | Khoảng trắng bị cắt, vẫn ra đúng địa điểm | Biên | Đạt |
| TC-PLACE-062 | Chọn một mã mà nguồn không có | 404 `RESOURCE_NOT_FOUND`. Không lưu gì | Sai | Đạt |
| TC-PLACE-063 | Tám người chọn cùng một địa điểm **đúng cùng lúc**, trên database thật | Cả tám nhận cùng một `id`, không ai gặp lỗi, database chỉ có một dòng | Biên | Đạt |
| TC-PLACE-064 | Database báo một lỗi khác lúc lưu, không phải lỗi trùng | Lỗi đó được báo ra nguyên vẹn, không bị nuốt, không bị đổi thành "không tìm thấy" | Sai | Đạt |
| TC-PLACE-065 | Gửi thiếu nguồn và mã chỉ có khoảng trắng | 400 `VALIDATION_ERROR`, liệt kê **cả hai** ô: "Thiếu nguồn của địa điểm", "Thiếu mã của địa điểm" | Sai | Đạt |
| TC-PLACE-066 | Mã dài 129 ký tự | 400: "Mã của địa điểm không được vượt quá 128 ký tự" | Biên | Đạt |
| TC-PLACE-067 | Gửi một nguồn mà hệ thống không có, ví dụ `GOOGLE` | 400 `VALIDATION_ERROR` | Sai | Đạt |
| TC-PLACE-068 | Chọn khi chưa đăng nhập | 401, không lưu gì | Bảo mật | Đạt |
| TC-PLACE-083 | Chọn với nguồn `MANUAL` kèm một mã có thật của dữ liệu có sẵn (nguồn đang dùng là `MOCK`) | 400 `VALIDATION_ERROR` ở ô `provider`: "Nguồn địa điểm này hiện không dùng được". Bị chặn trước khi đọc database hay hỏi nguồn; không lưu gì | Sai | Đạt |
| TC-PLACE-084 | Mọi kết quả tìm kiếm có mang đúng nguồn mà nguồn tự khai không | Có. Nhờ vậy kết quả nào hiện ra cũng chọn được, không bị quy tắc ở `TC-PLACE-083` từ chối nhầm | Đúng | Đạt |

Kiểm chứng ngược (2026-10-01): tạm bỏ bước "đọc lại dòng của người lưu trước" thì `TC-PLACE-063` và bản mô phỏng của nó ở tầng service đỏ; bật lại thì xanh. Khi chạy thật, log cho thấy đúng 1 request lưu được và 7 request đọc lại.

Kiểm chứng ngược (2026-10-02): tạm gỡ bước so nguồn thì test của `TC-PLACE-083` ở tầng service đỏ; bật lại thì xanh.

## I. Tự thêm một địa điểm

> **Yêu cầu:** design.md 10.2 "Quy ước Place API", rule 14.19 · **Kiểm bởi:** `PlaceMappingTest`, `PlaceServiceTest`, `PlaceControllerTest`

`POST /api/v1/places/manual` với `{ name, address, lat, lng, category }`. Dùng khi không kết quả tìm kiếm nào phù hợp, ví dụ nhà người quen. Khác với phần H, ở đây mọi thông tin là của người gọi, vì địa điểm này chỉ người tạo dùng. Chỉ tên và toạ độ là bắt buộc.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-PLACE-069 | Tự thêm "Nhà bà ngoại" với đủ tên, địa chỉ, toạ độ, loại "lưu trú" | 201. Địa điểm được lưu với nguồn `MANUAL`, không có mã ở nguồn; phản hồi có `id` và đúng những gì đã nhập | Đúng | Đạt |
| TC-PLACE-070 | Chỉ gửi tên và toạ độ | 201. Địa chỉ và loại để trống | Biên | Đạt |
| TC-PLACE-071 | Tên và địa chỉ có khoảng trắng thừa ở hai đầu; một lần khác địa chỉ chỉ toàn khoảng trắng | Khoảng trắng thừa bị cắt. Địa chỉ toàn khoảng trắng được coi là không có địa chỉ | Biên | Đạt |
| TC-PLACE-072 | Người tạo của địa điểm | Là người đang đăng nhập (lấy từ token) | Đúng | Đạt |
| TC-PLACE-073 | Người gọi bỏ qua giao diện, gửi kèm `createdBy` = 99 để ghi địa điểm dưới tên người khác | Phần gửi thừa bị bỏ qua. Người tạo vẫn là người trong token | Bảo mật | Đạt |
| TC-PLACE-074 | Tự thêm một địa điểm: ứng dụng có hỏi nguồn bản đồ hay tìm địa điểm trùng để gộp không | Không. Mỗi lần gọi là một địa điểm mới (trùng tên được phép, đã chốt ở Task 3.2) | Đúng | Đạt |
| TC-PLACE-075 | Lưu một địa điểm tự thêm vào database thật rồi đọc lại | Giữ đúng nguồn `MANUAL` và người tạo; mã ở nguồn để trống | Đúng | Đạt |
| TC-PLACE-076 | Lưu một địa điểm chép từ nguồn rồi đọc lại | Không có người tạo: địa điểm của nguồn dùng chung, không thuộc về ai | Đúng | Đạt |
| TC-PLACE-077 | Tên chỉ có khoảng trắng, không gửi toạ độ | 400 `VALIDATION_ERROR`, liệt kê **cả ba** ô: "Tên địa điểm không được để trống", "Thiếu vĩ độ của địa điểm", "Thiếu kinh độ của địa điểm". Không lưu gì | Sai | Đạt |
| TC-PLACE-078 | Toạ độ ngoài quả đất: vĩ độ `90.0000001` và `-91`, kinh độ `180.0000001` và `-181` | 400 ở đúng ô: "Vĩ độ phải nằm trong khoảng -90 đến 90" hoặc "Kinh độ phải nằm trong khoảng -180 đến 180" | Biên | Đạt |
| TC-PLACE-079 | Vĩ độ có 8 chữ số thập phân: `16.12345678` | 400: "Toạ độ có tối đa 7 chữ số thập phân". Không âm thầm làm tròn | Biên | Đạt |
| TC-PLACE-080 | Tên dài 201 ký tự và địa chỉ dài 501 ký tự | 400, liệt kê cả hai ô: tên tối đa 200, địa chỉ tối đa 500 ký tự | Biên | Đạt |
| TC-PLACE-081 | Loại không thuộc 6 loại hoạt động, ví dụ `CASINO` | 400 `VALIDATION_ERROR` | Sai | Đạt |
| TC-PLACE-082 | Tự thêm địa điểm khi chưa đăng nhập | 401, không lưu gì | Bảo mật | Đạt |

Quy tắc "địa điểm tự thêm của người khác không gắn được vào hoạt động của mình" (rule 14.19) được kiểm ở phần J.

**Điểm hở của Commit 5 đã đóng ở Commit 6:** sau Commit 5, `POST /api/v1/places` (phần H) chưa từ chối `provider` = `MANUAL`. Commit 6 chặn bằng lỗi 400 ở ô `provider` (`TC-PLACE-083`).

## J. Địa điểm nào được gắn vào hoạt động

> **Yêu cầu:** design.md rule 14.19, 10.2 "Quy ước Activity API" (đoạn "Địa điểm của activity") · **Kiểm bởi:** `PlaceServiceTest`

Khi một hoạt động gửi `placeId`, hệ thống kiểm địa điểm đó có dùng được không. Địa điểm chép từ nguồn là của chung. Địa điểm tự thêm là của riêng người tạo.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-PLACE-085 | Hai người khác nhau cùng gắn một địa điểm chép từ nguồn ("Chùa Linh Ứng") | Cả hai đều được | Đúng | Đạt |
| TC-PLACE-086 | Người tạo gắn địa điểm tự thêm của chính mình | Được | Đúng | Đạt |
| TC-PLACE-087 | Một người gắn địa điểm tự thêm **của người khác**; và gắn một mã không tồn tại | Cả hai bị từ chối bằng **cùng một lỗi**: 400 ở ô `placeId`, "Địa điểm không tồn tại". Người gọi không phân biệt được hai trường hợp, nên không dò ra được địa điểm riêng của ai | Bảo mật | Đạt |

Việc gắn vào hoạt động (thêm, rồi đổi và bỏ) ghi ở [05-activity.md](05-activity.md) từ phần Q.

## K. Kiểm toàn luồng chọn và tự thêm địa điểm qua mọi tầng

> **Yêu cầu:** design.md 10.2 "Quy ước Place API", rule 14.18, 14.19 · **Kiểm bởi:** `PlaceSearchFlowIntegrationTest`

Chạy cả ứng dụng thật với MySQL, không giả lập tầng nào. Các kịch bản ở phần H và I đã kiểm từng tầng; phần này chứng minh các tầng ghép lại đúng và database thật sự chứa điều mong đợi.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-PLACE-088 | Tìm "bun cha ca", lấy nguồn và mã của kết quả đầu, chọn nó hai lần | Lần đầu 200 với tên, địa chỉ, toạ độ, loại của "Bún chả cá 109". Lần hai trả **cùng mã**. Database có đúng một dòng | Đúng | Đạt |
| TC-PLACE-089 | Chọn "Chùa Linh Ứng" nhưng gửi kèm tên "Giữa biển" và toạ độ giả | 200. Dòng trong database mang tên và toạ độ **của nguồn**, không phải giá trị gửi lên | Bảo mật | Đạt |
| TC-PLACE-090 | Chọn một mã mà nguồn không có | 404 `RESOURCE_NOT_FOUND`. Database không có dòng nào | Sai | Đạt |
| TC-PLACE-091 | Chọn với nguồn `MANUAL` kèm mã có thật "da-nang-cho-han" | 400 ở ô `provider`: "Nguồn địa điểm này hiện không dùng được". Database không có dòng nào. Đây là bằng chứng trên ứng dụng thật rằng điểm hở của Commit 5 đã đóng | Sai | Đạt |
| TC-PLACE-092 | Chọn khi không có token | 401. Database không có dòng nào | Bảo mật | Đạt |
| TC-PLACE-093 | Tự thêm "Nhà bà ngoại" hai lần với cùng nội dung, body gửi kèm `createdBy` = 999999 | Hai lần đều 201 với **hai mã khác nhau** (không gộp theo tên); tên đã cắt khoảng trắng. Trong database: không có mã ở nguồn, người tạo là người trong token, không phải số gửi lên | Bảo mật | Đạt |
| TC-PLACE-094 | Tự thêm với vĩ độ 91 và không có kinh độ | 400, liệt kê cả hai ô. Database không có dòng nào | Sai | Đạt |

Luồng tiếp theo, gắn địa điểm vào hoạt động trên ứng dụng thật, ghi ở [05-activity.md](05-activity.md) phần T.

## L. Hai lần tìm nào là cùng một câu hỏi

> **Yêu cầu:** design.md 8.1 (khoá của `place:search`: từ khoá đã chuẩn hoá + `limit` + toạ độ làm tròn) · **Kiểm bởi:** `PlaceSearchCacheTest`

Từ Task 3.4, câu trả lời của một lần tìm địa điểm được giữ tạm 24 giờ để lần tìm giống hệt sau đó không phải hỏi lại nguồn. Muốn vậy phải định nghĩa "giống hệt": mỗi lần tìm được đổi thành một **khoá**, hai lần tìm ra cùng khoá thì dùng chung một câu trả lời. Khoá quá lỏng thì người dùng nhận câu trả lời của câu hỏi khác; quá chặt thì không ai dùng lại được gì. Commit 3 chỉ thêm quy tắc tạo khoá; việc giữ tạm bắt đầu từ Commit 4.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-PLACE-095 | Tìm "Chợ Hàn", 8 kết quả, quanh Đà Nẵng; rồi cùng lần tìm đó nhưng không có toạ độ | Khoá đọc được bằng mắt: `8\|16.0678,108.2208\|cho han` và `8\|-\|cho han`. Người vận hành nhìn vào Redis là biết ô nào của câu hỏi nào | Đúng | Đạt |
| TC-PLACE-096 | Gõ "chợ hàn", "cho han", "CHỢ HÀN", "  chợ   hàn "; rồi "Đà Nẵng" và "da nang" | Cùng một khoá: dấu, chữ hoa và khoảng trắng thừa không tạo ra câu hỏi mới | Đúng | Đạt |
| TC-PLACE-097 | Đổi từ khoá, đổi số kết quả muốn lấy (8 thành 20), đổi toạ độ (Đà Nẵng thành Hà Nội), bỏ toạ độ | Mỗi thay đổi ra một khoá khác: cả ba thứ đều làm đổi câu trả lời | Biên | Đạt |
| TC-PLACE-098 | Hai toạ độ cách nhau dưới khoảng 11 m; cùng một toạ độ viết với 4 và với 7 chữ số thập phân; một toạ độ lệch ở chữ số thứ tư | Hai trường hợp đầu cùng khoá; trường hợp cuối khác khoá | Biên | Đạt |
| TC-PLACE-099 | Người dùng gõ từ khoá trông giống hệt một khoá, có cả ký tự ngăn cách (`8\|-\|cho han`) | Không trùng với khoá của lần tìm "cho han": từ khoá đứng cuối khoá nên không thể bị đọc thành số kết quả hay toạ độ của câu hỏi khác | Bảo mật | Đạt |

Kiểm chứng ngược (2026-10-03), mỗi lần sửa một chỗ rồi trả lại: giữ nguyên dấu trong khoá thì `TC-PLACE-095`, `096` đỏ; bỏ số kết quả khỏi khoá thì `TC-PLACE-095`, `097` đỏ (2 trong 5 test mỗi lần).

## M. Giữ tạm kết quả tìm địa điểm trong Redis

> **Yêu cầu:** design.md 8.1 (`place:search`, 24 giờ; cache dùng chung, đặt ở bean riêng, lưu JSON có kiểu cố định) · **Kiểm bởi:** `PlaceSearchCacheIntegrationTest`, `PlaceServiceTest`

Lần đầu một câu hỏi được hỏi, nguồn địa điểm trả lời và câu trả lời được cất vào Redis. Trong 24 giờ sau đó, ai hỏi đúng câu đó (theo quy tắc ở phần L) thì nhận câu trả lời từ Redis, nguồn không bị hỏi. Cache dùng chung cho mọi người dùng vì kết quả tìm địa điểm không phải dữ liệu riêng của ai. Test chạy với Redis thật.

Cách chứng minh "câu trả lời đến từ Redis": sau lần tìm đầu, test **đánh tráo** câu trả lời đang cất trong Redis bằng một địa điểm bịa ra. Nếu lần tìm sau trả về địa điểm bịa đó, câu trả lời đúng là lấy từ Redis; nếu trả về địa điểm thật, tức là nguồn vẫn bị hỏi.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-PLACE-100 | Tìm "chợ hàn" lần đầu, rồi nhìn vào Redis | Có một ô mang khoá `place:search::8\|-\|cho han`, còn hạn gần đủ 24 giờ. Nội dung là JSON đọc được, có "Chợ Hàn", không chứa tên class Java | Đúng | Đạt |
| TC-PLACE-101 | Đánh tráo câu trả lời trong Redis, rồi tìm lại "chợ hàn", và "  CHO   HAN " | Cả hai lần đều nhận địa điểm đã đánh tráo: câu trả lời lấy từ Redis, và cách gõ khác nhau vẫn là một câu hỏi | Đúng | Đạt |
| TC-PLACE-102 | Sau khi đánh tráo, tìm từ khoá khác ("chợ cồn"), số kết quả khác (20), toạ độ khác (quanh Đà Nẵng) | Cả ba lần đều nhận địa điểm thật từ nguồn, không lần nào nhận địa điểm đánh tráo | Biên | Đạt |
| TC-PLACE-103 | Tìm "da nang" hai lần liên tiếp, lần hai lấy từ Redis | Hai danh sách bằng nhau hoàn toàn: cùng địa điểm, cùng thứ tự, toạ độ đúng tới chữ số cuối. Dữ liệu đọc lại vẫn đúng kiểu, không biến thành dạng thô | Đúng | Đạt |
| TC-PLACE-104 | Tìm một từ khoá không có kết quả, hai lần | Câu trả lời rỗng cũng được cất (`[]`), lần sau không hỏi lại nguồn | Biên | Đạt · từng lỗi BUG-PLACE-003 |
| TC-PLACE-106 | Tìm 40 từ khoá khác nhau liên tiếp, sau mỗi lần nhìn ngay vào Redis | Lần nào khoá cũng đã có mặt **ngay khi hàm tìm kiếm trả về**. Test này được thêm khi sửa BUG-PLACE-003 để ép lỗi "ghi ở nền" lộ ra chắc chắn, thay vì thỉnh thoảng | Biên | Đạt |
| TC-PLACE-105 | Hỏi một cache có tên gõ sai (`place:serach`) | Không tồn tại. Chỉ cache đã khai báo mới dùng được, để một lỗi gõ tên không lặng lẽ tạo ra cache không có hạn | Sai | Đạt |

`PlaceServiceTest` (phần B) từ commit này kiểm thêm: việc tìm kiếm luôn đi qua cache, không gọi thẳng nguồn.

Kiểm chứng ngược (2026-10-03), mỗi lần sửa một chỗ rồi trả lại:
- Bỏ dòng bật cache trên hàm tìm kiếm: `TC-PLACE-100`, `101`, `104` đỏ (3 trong 6 test).
- Thay cách lưu "kiểu cố định" bằng cách lưu "tự ghi tên class": `TC-PLACE-101`, `103` đỏ. Đây là cái bẫy đã được nhắc trước trong WORKFLOW: dữ liệu đọc lại không còn đúng kiểu.

**Điểm hở tạm thời sau Commit 4, đã đóng ở Commit 5:** Redis tắt thì tìm địa điểm báo lỗi 500. Xem [01-platform.md](01-platform.md) phần G (`TC-PLAT-038`, `MT-PLAT-06`).

## N. Quãng đường giữa các điểm liên tiếp (nguồn có sẵn)

> **Yêu cầu:** design.md 7.2 (`MapProvider.route`; mock: đường chim bay × 1,3, tốc độ 30 km/h), 10.2 "Quy ước Route" · **Kiểm bởi:** `MockMapProviderTest`

Từ Task 3.5, nguồn bản đồ trả lời thêm một câu hỏi: đi lần lượt qua các điểm này thì mỗi chặng dài bao nhiêu và mất bao lâu. Nguồn có sẵn (mock) không có bản đồ đường thật, nên ước lượng: lấy đường chim bay giữa hai điểm, nhân 1,3 (đường đi không bao giờ thẳng), và coi như đi với vận tốc 30 km/h. Nguồn thật (Task 3.8) sẽ tính theo đường thật. Commit 1 chỉ thêm khả năng này cho nguồn; chưa có API nào dùng cho tới Commit 2.

Ba điểm thử A, B, C nằm trên cùng một kinh tuyến, cách nhau 0,01 và 0,02 độ vĩ, để con số kiểm được bằng tay: 0,01 độ vĩ dài 1.111,95 m.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-PLACE-107 | Hỏi quãng đường từ A tới B (đường chim bay 1.111,95 m) | Một chặng: 1.446 m (1.111,95 × 1,3, làm tròn tới mét) và 174 giây (1.446 m với vận tốc 30 km/h, làm tròn tới giây) | Đúng | Đạt |
| TC-PLACE-108 | Hỏi A → B → C; rồi C → B → A; rồi A → C → B | Số chặng luôn bằng số điểm trừ 1, theo **đúng thứ tự đã gửi**: lần đầu 1.446 m rồi 2.891 m; lần hai ngược lại; lần ba 4.337 m rồi 2.891 m (đi quá B rồi quay lại). Nguồn không tự sắp lại điểm để đường ngắn hơn | Đúng | Đạt |
| TC-PLACE-109 | Hỏi cùng các điểm nhiều lần, và hỏi một nguồn được nạp dữ liệu địa điểm khác | Luôn ra cùng các chặng. Kết quả chỉ phụ thuộc vào toạ độ được gửi | Đúng | Đạt |
| TC-PLACE-110 | Hai điểm liền nhau có cùng toạ độ (A → A → B) | Chặng đầu là 0 m và 0 giây, chặng sau bình thường. Vẫn đủ 2 chặng | Biên | Đạt |
| TC-PLACE-111 | Hỏi với danh sách rỗng, rồi với chỉ một điểm | Không có chặng nào, không báo lỗi | Biên | Đạt |

Kiểm chứng ngược (2026-10-03): tạm đổi hệ số 1,3 thành 1,0 thì `TC-PLACE-107`, `108`, `110` đỏ (3 trong 5 test); trả lại thì xanh.

## O. Quãng đường của một ngày qua API

> **Yêu cầu:** design.md 10.2 "Quy ước Route", 6.3 (quyền xem chuyến đi) · **Kiểm bởi:** `RouteServiceTest`, `TripDayControllerTest`

`GET /api/v1/trips/{tripId}/days/{dayId}/route`. Ai xem được chuyến đi thì xem được quãng đường của các ngày trong đó. Câu trả lời gồm danh sách chặng (đi từ hoạt động nào tới hoạt động nào, bao nhiêu mét, bao nhiêu giây) và tổng của cả ngày. Hệ thống đi theo đúng thứ tự hoạt động đang hiển thị, không tự sắp lại.

Ở tầng service, nguồn bản đồ được thay bằng bản giả trả các con số cho sẵn: các kịch bản kiểm việc **ghép chặng với hoạt động và cộng tổng**, không kiểm công thức ước lượng (đã kiểm ở phần N).

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-PLACE-112 | Ngày có 3 hoạt động đều có địa điểm; nguồn trả 2 chặng 1.150 m / 138 giây và 8.400 m / 1.008 giây | 2 chặng: hoạt động 1 → 2 và 2 → 3, đúng con số của nguồn. Tổng 9.550 m và 1.146 giây, đúng bằng cộng tay hai chặng | Đúng | Đạt |
| TC-PLACE-113 | Thứ tự hiển thị của ngày không trùng thứ tự tạo (hoạt động tạo sau đứng trước) | Toạ độ được gửi cho nguồn theo **thứ tự hiển thị**, và chặng nối đúng các hoạt động theo thứ tự đó. Cả ngày chỉ hỏi nguồn một lần | Đúng | Đạt |
| TC-PLACE-114 | Chuyến đi không tồn tại hoặc đã xoá | Báo "không tìm thấy". Không đọc ngày, không đọc hoạt động, không hỏi nguồn | Sai | Đạt |
| TC-PLACE-115 | Mã ngày là ngày của **một chuyến đi khác** | Báo "không tìm thấy". Hoạt động của ngày đó không được đọc và nguồn không bị hỏi: người xem được chuyến đi này không dò được lịch trình của chuyến đi khác | Bảo mật | Đạt |
| TC-PLACE-116 | Người có quyền xem gọi API | 200. Mỗi chặng có `fromActivityId`, `toActivityId`, `distanceMeters`, `durationSeconds`; kèm `totalDistanceMeters`, `totalDurationSeconds` | Đúng | Đạt |
| TC-PLACE-117 | Gọi khi chưa đăng nhập | 401 `UNAUTHORIZED`, không tính gì | Bảo mật | Đạt |
| TC-PLACE-118 | Người đã đăng nhập nhưng không có quyền xem chuyến đi | 403 `FORBIDDEN`, không tính gì | Bảo mật | Đạt |
| TC-PLACE-119 | Ngày không thuộc chuyến đi, gọi qua API | 404 `RESOURCE_NOT_FOUND` | Sai | Đạt |
| TC-PLACE-120 | Mã ngày trên đường dẫn không phải là số (`/days/abc/route`) | 400 `VALIDATION_ERROR`, không phải lỗi 500; không tính gì | Sai | Đạt |

Kiểm chứng ngược (2026-10-03): tạm bỏ bước "ngày phải thuộc chuyến đi" thì `TC-PLACE-115` đỏ; trả lại thì xanh.

**Điểm hở tạm thời sau Commit 2, đã đóng ở Commit 3:** ngày có hoạt động **không có địa điểm** thì API báo lỗi 500. Xem phần P. Luồng trên ứng dụng thật (qua MySQL) được kiểm ở Commit 5.

## P. Hoạt động không có địa điểm

> **Yêu cầu:** design.md 10.2 "Quy ước Route" (chỉ tính giữa các hoạt động có địa điểm; cùng địa điểm thì chặng 0) · **Kiểm bởi:** `RouteServiceTest`

Không phải hoạt động nào cũng có địa điểm ("Nghỉ trưa", "Tự do mua sắm"). Hoạt động như vậy không phải một điểm dừng trên bản đồ, nên bị bỏ qua khi tính quãng đường; đường đi **không bị cắt** ở đó mà nối thẳng hai hoạt động có địa điểm ở hai bên. Số chặng luôn bằng số hoạt động có địa điểm trừ 1.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-PLACE-121 | Ngày có: Chợ Hàn, một hoạt động không có địa điểm, Chùa Linh Ứng | Một chặng nối thẳng hoạt động thứ nhất với hoạt động thứ ba. Nguồn bản đồ chỉ nhận hai toạ độ. Tổng bằng đúng chặng đó | Đúng | Đạt |
| TC-PLACE-122 | Hoạt động không có địa điểm đứng ở đầu ngày, giữa ngày và cuối ngày (6 hoạt động, 3 có địa điểm) | Đúng 2 chặng, nối 3 hoạt động có địa điểm theo thứ tự. Các hoạt động còn lại không xuất hiện trong chặng nào | Biên | Đạt |
| TC-PLACE-123 | Hai hoạt động liền nhau ở **cùng một địa điểm**, rồi một hoạt động ở nơi khác | Vẫn đủ 2 chặng: chặng đầu 0 m và 0 giây, chặng sau bình thường. Có địa điểm là được tính, dù không phải di chuyển; giao diện tự ẩn chặng 0 | Biên | Đạt |

Test viết trước, sửa sau (2026-10-03): trước khi sửa, `TC-PLACE-121` và `122` đỏ với lỗi `NullPointerException`, chính là lỗi 500 của điểm hở; `TC-PLACE-123` đã xanh sẵn vì không cần quy tắc mới. Sau khi thêm bước bỏ qua thì cả ba xanh.

## Q. Ngày không có gì để đi

> **Yêu cầu:** design.md 10.2 "Quy ước Route" (ngày có 0 hoặc 1 địa điểm: `legs` rỗng, không hỏi nguồn bản đồ) · **Kiểm bởi:** `RouteServiceTest`

Một chặng cần hai điểm dừng. Ngày chưa có hoạt động nào, hoặc chỉ có một hoạt động có địa điểm, thì không có gì để tính: câu trả lời là danh sách chặng rỗng và hai tổng bằng 0, **không phải lỗi**. Hệ thống trả lời ngay mà không hỏi nguồn bản đồ. Với nguồn có sẵn điều này không đổi kết quả; với nguồn thật (Task 3.8) nó tránh một lần gọi mạng vô ích, và tránh hỏi một câu mà dịch vụ tìm đường coi là sai.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-PLACE-124 | Ngày chưa có hoạt động nào | Không có chặng, tổng 0 m và 0 giây. Nguồn bản đồ không bị hỏi | Biên | Đạt |
| TC-PLACE-125 | Ngày có đúng một hoạt động, có địa điểm | Như trên | Biên | Đạt |
| TC-PLACE-126 | Ngày có 4 hoạt động nhưng chỉ một hoạt động có địa điểm | Như trên. Thứ được đếm là số địa điểm, không phải số hoạt động | Biên | Đạt |
| TC-PLACE-127 | Ngày có hoạt động nhưng không hoạt động nào có địa điểm | Như trên | Biên | Đạt |

Test viết trước, sửa sau (2026-10-03): trước khi sửa, cả bốn test đỏ ở đúng một chỗ, "nguồn bản đồ vẫn bị hỏi" (câu trả lời thì đã rỗng sẵn). Sau khi thêm bước chặn thì cả bốn xanh.

## R. Kiểm toàn luồng quãng đường qua mọi tầng

> **Yêu cầu:** design.md 10.2 "Quy ước Route", 6.3 · **Kiểm bởi:** `DayRouteFlowIntegrationTest`

Chạy cả ứng dụng thật với MySQL, không giả lập tầng nào: bộ lọc đăng nhập, kiểm quyền, database, và nguồn bản đồ do chính ứng dụng tự chọn (nguồn có sẵn). Hoạt động và địa điểm được tạo qua chính các API của người dùng. Con số trong bảng là của ba địa điểm thật trong dữ liệu Đà Nẵng: Chợ Hàn → Bún chả cá 109 là 998 m / 120 giây; Bún chả cá 109 → Chùa Linh Ứng là 8.827 m / 1.059 giây; Chùa Linh Ứng ↔ Chợ Hàn là 8.812 m / 1.057 giây.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-PLACE-128 | Ngày có Chợ Hàn, Bún chả cá, Chùa Linh Ứng. Xem quãng đường; rồi **kéo** Chùa Linh Ứng lên đầu ngày và xem lại | Lần đầu: 2 chặng 998 m và 8.827 m, tổng 9.825 m / 1.179 giây. Sau khi kéo: chặng đổi theo thứ tự mới (chùa → chợ 8.812 m, chợ → bún chả cá 998 m), tổng 9.810 m / 1.177 giây | Đúng | Đạt |
| TC-PLACE-129 | Chuyển hoạt động ở giữa sang Ngày 2 | Ngày 1 còn một chặng nối thẳng hai hoạt động còn lại (8.812 m). Ngày 2 chỉ có một địa điểm: không có chặng, tổng 0 | Đúng | Đạt |
| TC-PLACE-130 | Ngày có một hoạt động không có địa điểm ("Nghỉ trưa") nằm giữa; rồi bỏ địa điểm của một hoạt động; rồi bỏ thêm một cái nữa | Lần đầu: 200, "Nghỉ trưa" không nằm trong chặng nào, tổng vẫn 9.825 m (bằng chứng trên ứng dụng thật rằng điểm hở 500 của Commit 2 đã đóng). Bỏ một địa điểm: còn một chặng nối hai hoạt động còn địa điểm. Bỏ tiếp: không còn chặng, tổng 0, vẫn 200 | Biên | Đạt |
| TC-PLACE-131 | Xem quãng đường của một ngày chưa có hoạt động nào | 200, không có chặng, hai tổng bằng 0 | Biên | Đạt |
| TC-PLACE-132 | Không có token; người khác xem chuyến đi của chủ; người khác **mượn chuyến đi của chính mình** để hỏi ngày của chuyến đi kia; ngày không tồn tại; chuyến đi không tồn tại; mã ngày không phải số | Lần lượt 401, 403, **404** (và câu trả lời không chứa chặng nào: người có chuyến đi riêng không dò được lịch trình của người khác qua mã ngày), 404, 404, 400 | Bảo mật | Đạt |
| TC-PLACE-133 | Đếm số câu lệnh database của một lần xem quãng đường: ngày trống, ngày có 2 hoạt động, ngày có 7 hoạt động (6 có địa điểm) | Luôn là **4 câu** (kiểm quyền, chuyến đi, ngày, các hoạt động kèm địa điểm), không tăng theo số hoạt động. Ngày 7 hoạt động vẫn trả đủ 5 chặng | Đúng | Đạt |

Kiểm chứng ngược (2026-10-03): tạm gỡ bước "bỏ qua hoạt động không có địa điểm" thì `TC-PLACE-130` và `133` đỏ; trả lại thì xanh.

---

## Kiểm tra thủ công

Cần có: Docker đang chạy, backend chạy bản code mới nhất (profile `local`), một tài khoản đã xác thực email.

### MT-PLACE-01 · Tìm địa điểm trên Swagger

- [ ] Mở `http://localhost:8080/swagger-ui.html`. Gọi `POST /api/v1/auth/login`, chép `accessToken`, bấm "Authorize" và dán vào.
- [ ] Ở nhóm "Place", gọi `GET /api/v1/places/search` với `q` = `linh ung`. Trả 200, có đúng một kết quả "Chùa Linh Ứng" kèm địa chỉ "Đường Hoàng Sa, Phường Sơn Trà, Đà Nẵng" và toạ độ `16.1001567`, `108.2784112`.
- [ ] Chép hai số toạ độ, dán vào ô tìm kiếm của một bản đồ thật (ví dụ openstreetmap.org) dạng `16.1001567, 108.2784112`. Điểm hiện ra nằm ở chùa Linh Ứng trên bán đảo Sơn Trà.
- [ ] `q` = `cho`: hai kết quả đầu là "Chợ Hàn" và "Chợ Cồn".
- [ ] `q` = `da nang`, `limit` = `3`: đúng 3 kết quả.
- [ ] `q` = `a`: 400, thông báo "Từ khoá tìm địa điểm cần ít nhất 2 ký tự".
- [ ] (Mốc 3 và 4) `q` = `cho`, không điền toạ độ: "Chợ Hàn", "Chợ Cồn", "Chợ Đồng Xuân" đứng đầu (theo thứ tự trong file). Thêm `lat` = `10.7769`, `lng` = `106.7009` (TP. Hồ Chí Minh): "Chợ Bến Thành" và "Chợ Bình Tây" lên đầu. Đổi thành `lat` = `21.0285`, `lng` = `105.8542` (Hà Nội): "Chợ Đồng Xuân" lên đầu. Số kết quả không đổi.
- [ ] (Mốc 4) Lần lượt tìm `ho hoan kiem`, `chua cau`, `ho xuan huong`, `dinh doc lap`: mỗi lần ra đúng địa điểm đó. Chép toạ độ của một kết quả bất kỳ vào openstreetmap.org để xem điểm có nằm đúng chỗ không.
- [ ] (Mốc 3) `q` = `cho`, chỉ điền `lat` = `16`, bỏ trống `lng`: 400, thông báo "Cần gửi đủ cả vĩ độ và kinh độ, hoặc bỏ cả hai".
- [ ] `q` = `khong co noi nay`: 200, danh sách rỗng.
- [ ] Bấm "Authorize" → "Logout" rồi gọi lại: 401.

**Kết quả:** Chưa chạy

### MT-PLACE-02 · Chọn một kết quả tìm kiếm trên Swagger

Thêm ở Task 3.2 Commit 3. Cần backend chạy bản mới: log khởi động có dòng Flyway áp dụng phiên bản 9.

- [ ] Đăng nhập và "Authorize" như `MT-PLACE-01`. Gọi `GET /api/v1/places/search` với `q` = `bun cha ca`. Kết quả có `provider` = `MOCK`, `externalId` = `da-nang-bun-cha-ca-109`, không có `id`.
- [ ] Gọi `POST /api/v1/places` với body `{"provider": "MOCK", "externalId": "da-nang-bun-cha-ca-109"}`. Trả 200, có `id` (ghi lại số này), tên "Bún chả cá 109", toạ độ `16.0743887`, `108.2207958`.
- [ ] Gọi lại y nguyên. Trả 200 với **đúng `id` đó**.
- [ ] Chạy lệnh bên dưới (nhập mật khẩu database khi được hỏi). Chỉ có **một** dòng cho địa điểm này.
- [ ] Gọi `POST /api/v1/places` với body `{"provider": "MOCK", "externalId": "da-nang-chua-linh-ung", "name": "Giữa biển", "lat": 10, "lng": 115}`. Trả 200 với tên "Chùa Linh Ứng" và toạ độ thật, không phải giá trị vừa gửi.
- [ ] Body `{"provider": "MOCK", "externalId": "khong-co"}`: 404.
- [ ] (Commit 6) Body `{"provider": "MANUAL", "externalId": "da-nang-cho-han"}`: 400, ô `provider`, thông báo "Nguồn địa điểm này hiện không dùng được". Chạy lại lệnh bên dưới: không có dòng mới.
- [ ] Body `{"externalId": ""}`: 400, liệt kê hai ô `externalId` và `provider`.

```powershell
docker exec -it tripplanner-mysql mysql -u tripuser -p tripplanner -e "SELECT id, provider, external_id, name, lat, lng FROM places;"
```

**Kết quả:** Chưa chạy

### MT-PLACE-03 · Tự thêm một địa điểm trên Swagger

Thêm ở Task 3.2 Commit 5. Không có migration mới: bảng `places` của phiên bản 9 đã có sẵn cột người tạo.

- [ ] Đăng nhập và "Authorize" như `MT-PLACE-01`. Gọi `POST /api/v1/places/manual` với body `{"name": "Nhà bà ngoại", "address": "12 Lê Lợi, Đà Nẵng", "lat": 16.0471234, "lng": 108.2068765, "category": "ACCOMMODATION"}`. Trả **201**, có `id` (ghi lại số này), `provider` = `MANUAL`, tên, địa chỉ, toạ độ, loại đúng như đã nhập.
- [ ] Gọi lại y nguyên. Trả 201 với một **`id` khác**: trùng tên không bị gộp.
- [ ] Body chỉ có `{"name": "Điểm hẹn", "lat": 16, "lng": 108}`: 201, `address` và `category` là `null`.
- [ ] Chạy lệnh bên dưới. Có ba dòng `MANUAL`, cột `external_id` là `NULL`, cột `created_by` là mã của tài khoản đang đăng nhập (đối chiếu với `id` trong `GET /api/v1/users/me`).
- [ ] Body `{"name": "Điểm hẹn", "lat": 91, "lng": 108}`: 400, thông báo "Vĩ độ phải nằm trong khoảng -90 đến 90".
- [ ] Body `{"name": "  "}`: 400, liệt kê ba ô `name`, `lat`, `lng`.
- [ ] Body `{"name": "Điểm hẹn", "lat": 16, "lng": 108, "category": "CASINO"}`: 400.
- [ ] Bấm "Authorize" → "Logout" rồi gọi lại body đầu tiên: 401.

```powershell
docker exec -it tripplanner-mysql mysql -u tripuser -p tripplanner -e "SELECT id, provider, external_id, name, created_by FROM places WHERE provider = 'MANUAL';"
```

**Kết quả:** Chưa chạy

### MT-PLACE-04 · Xem kết quả tìm địa điểm được giữ trong Redis

Thêm ở Task 3.4 Commit 4. Cần `docker compose up -d mysql redis mailhog` và backend đang chạy.

- [ ] Chạy lệnh thứ nhất bên dưới để xoá sạch Redis của máy bạn (chỉ chứa bản tạm, xoá không mất gì).
- [ ] Trên Swagger, đăng nhập rồi gọi `GET /api/v1/places/search?q=chợ hàn`. Trả 200, có "Chợ Hàn".
- [ ] Chạy lệnh thứ hai. Có đúng một khoá: `place:search::8|-|cho han`.
- [ ] Chạy lệnh thứ ba. Ra một số gần 86400 (số giây của 24 giờ) và đang giảm dần.
- [ ] Gọi lại với `q=CHO HAN`. Chạy lại lệnh thứ hai: vẫn chỉ một khoá (hai cách gõ dùng chung một ô).
- [ ] Gọi với `q=chợ hàn&limit=20`. Chạy lại lệnh thứ hai: có thêm khoá `place:search::20|-|cho han`.

```powershell
docker exec tripplanner-redis redis-cli FLUSHALL
docker exec tripplanner-redis redis-cli KEYS "place:search*"
docker exec tripplanner-redis redis-cli TTL "place:search::8|-|cho han"
```

**Kết quả:** Chưa chạy

### MT-PLACE-05 · Xem quãng đường của một ngày trên Swagger

Thêm ở Task 3.5 Commit 2. Cần backend chạy bản code mới nhất.

- [ ] Đăng nhập và "Authorize" như `MT-PLACE-01`. Tạo một chuyến đi mới bằng `POST /api/v1/trips` (ghi lại `id` của chuyến đi), rồi gọi `GET /api/v1/trips/{tripId}/days` và ghi lại `id` của Ngày 1.
- [ ] Gọi `POST /api/v1/places` ba lần với `{"provider": "MOCK", "externalId": "..."}`, lần lượt `da-nang-cho-han`, `da-nang-bun-cha-ca-109`, `da-nang-chua-linh-ung`. Ghi lại ba `id` địa điểm.
- [ ] Gọi `POST /api/v1/trips/{tripId}/days/{dayId}/activities` ba lần, mỗi lần một địa điểm theo đúng thứ tự trên, ví dụ `{"title": "Chợ Hàn", "placeId": <id>}`. Ghi lại ba `id` hoạt động.
- [ ] Gọi `GET /api/v1/trips/{tripId}/days/{dayId}/route`. Trả 200 với 2 chặng: chặng đầu `distanceMeters` = `998`, `durationSeconds` = `120`; chặng sau `8827` và `1059`. `fromActivityId` / `toActivityId` đúng là ba hoạt động vừa tạo, theo thứ tự. Tổng `9825` và `1179`.
- [ ] Đổi `dayId` thành mã ngày của một chuyến đi khác (hoặc một số không tồn tại): 404.
- [ ] Đổi `dayId` thành `abc`: 400.
- [ ] Bấm "Authorize" → "Logout" rồi gọi lại: 401.
- [ ] (Từ Commit 3) Thêm một hoạt động không có địa điểm vào ngày đó rồi gọi lại: vẫn 200 với đúng 2 chặng như trên.
- [ ] (Từ Commit 4) Gọi với `dayId` của Ngày 2 (chưa có hoạt động nào): 200 với `"legs": []`, hai tổng bằng `0`.

**Kết quả:** Chưa chạy

---

## Lỗi đã phát hiện

| Mã lỗi | Test case | Ngày | Hiện tượng | Nguyên nhân | Cách sửa | Trạng thái |
|---|---|---|---|---|---|---|
| BUG-PLACE-003 | `TC-PLACE-104` (Task 3.4 Commit 6, test cũ của Commit 4) | 2026-10-03 | `PlaceSearchCacheIntegrationTest > emptyAnswerIsStoredToo` đỏ **lúc có lúc không**: 2 lần đỏ trong khoảng 10 lần chạy bộ 4 class (`ForecastCache`, `PlaceSearchCache`, `RedisDown`, `TripWeatherFlow`), chưa từng đỏ khi chạy một mình. Thông báo: `expected: "[]" but was: null` tại dòng đọc thẳng khoá `place:search::8\|-\|khong co dia diem nao ten nay` ngay sau khi tìm kiếm trả về danh sách rỗng. Không có dòng WARN nào của cache trong log; các test khác của class vẫn đạt; thứ tự class giống nhau ở lần đỏ và lần xanh | **Code sai**, test đúng. Bộ ghi cache mặc định của Spring Data Redis 4 **ghi vào Redis ở nền** khi dùng trình điều khiển Lettuce: hàm tìm kiếm trả về trước khi lệnh ghi tới Redis, nên đọc thẳng Redis ngay sau đó thỉnh thoảng chưa thấy khoá. Đã xác nhận bằng cách đọc bytecode của `DefaultRedisCacheWriter` (`writeAsynchronously`, cờ bật sẵn) và bằng một test ép lỗi lộ ra (`TC-PLACE-106`: 40 lần tìm liên tiếp, đỏ 2/2 lần khi chưa sửa). Ngoài test, cách ghi ở nền còn làm lỗi ghi không tới được bộ xử lý lỗi của Commit 5 | Cấu hình bộ ghi cache bằng `RedisCacheWriter.create(..., writer -> writer.immediateWrites())` trong `CacheConfig`: ghi xong rồi mới trả về. Giá phải trả là mỗi lần ghi cache chờ Redis khoảng một phần nghìn giây | Đã sửa, commit `8b7c864` |
| BUG-PLACE-002 | Test mới của Task 3.2 Mốc 1: database từ chối toạ độ ngoài khoảng | 2026-10-01 | `PlaceMappingTest > coordinatesOutsideTheGlobeAreRejectedByTheDatabase` đỏ cả 4 lượt. Database **có** từ chối (`Check constraint 'chk_places_lat' is violated`), nhưng test mong đợi loại lỗi `DataIntegrityViolationException` còn thực tế nhận `UncategorizedSQLException`. 63 lượt còn lại của năm class đạt | **Test sai**, hệ thống đúng. Spring chỉ đổi một số mã lỗi của MySQL sang loại "vi phạm ràng buộc dữ liệu"; lỗi của ràng buộc `CHECK` (mã 3819) không nằm trong số đó nên ra loại lỗi chung | Test không còn dựa vào loại lỗi, mà kiểm đúng tên ràng buộc bị vi phạm (`chk_places_lat`, `chk_places_lng`). Kiểm như vậy còn chặt hơn bản đầu. Code không đổi | Đã sửa, commit `5b9088f` |
| BUG-PLACE-001 | Test mới của Task 3.1 Mốc 4: mỗi điểm đến phải có đủ các loại địa điểm | 2026-10-01 | `MockPlacesDataTest > everyDestinationHasEnoughPlacesOfSeveralKinds() FAILED`: với Hội An, test mong đợi có đủ bốn loại tham quan, ăn uống, lưu trú, di chuyển; thực tế thiếu loại di chuyển (`could not find the following element(s): ["TRANSPORT"]`). 19 test còn lại của hai class đạt | **Dữ liệu thiếu**, test đúng. Khi soạn danh sách Hội An, lần tra "Bến xe Hội An" trên OpenStreetMap không ra kết quả dùng được nên điểm đến này bị bỏ trống loại di chuyển | Tra thêm và bổ sung "Cảng du lịch Cửa Đại" (bến tàu đi Cù Lao Chàm) vào Hội An | Đã sửa, commit `3ba2e85` |

BUG-PLACE-001 cho thấy vì sao dữ liệu cũng cần test: người soạn dữ liệu (ở đây là trợ lý lập trình) không nhận ra mình bỏ sót một loại cho tới khi test đếm hộ. Lỗi được ghi vào tài liệu trước khi bổ sung dữ liệu.

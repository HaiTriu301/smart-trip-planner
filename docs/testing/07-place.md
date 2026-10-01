# 07 · Địa điểm

> Cập nhật: 2026-10-01 · build xanh tại commit `2c84e02` (Task 3.1, 592 lượt test) · kiểm tra thủ công `MT-PLACE-01` chưa chạy · [Về trang chính](README.md)

Địa điểm là một nơi có tên và toạ độ, ví dụ "Chùa Linh Ứng". Người dùng tìm địa điểm theo tên rồi gắn vào một hoạt động; từ đó hoạt động hiện được trên bản đồ. Tìm địa điểm làm ở Task 3.1, gắn vào hoạt động ở Task 3.2.

File này được ghi dần theo từng mốc. Mỗi mốc là một commit:

| Mốc | Nội dung | Phần trong file | Commit |
|---|---|---|---|
| 1 | Tách phần bỏ dấu tiếng Việt thành hàm dùng chung (chuẩn bị cho tìm kiếm không dấu) | A | `f0cd0f8` |
| 2 | Tìm địa điểm theo tên hoặc địa chỉ, trên dữ liệu có sẵn của Đà Nẵng | B, C | `458a670` |
| 3 | Địa điểm quanh điểm đến của chuyến đi được xếp trước | D | `0ff1b50` |
| 4 | Mở rộng dữ liệu có sẵn lên 56 địa điểm ở 5 điểm đến, kèm kiểm tra chính dữ liệu | E | `3ba2e85` |
| 5 | Kiểm toàn luồng tìm địa điểm qua mọi tầng | F | `2c84e02` |

Vài từ dùng trong file:

| Từ | Nghĩa |
|---|---|
| Bỏ dấu | Đổi chữ có dấu thành chữ không dấu: "Chùa Linh Ứng" thành "Chua Linh Ung" |
| Nguồn địa điểm | Nơi ứng dụng lấy danh sách địa điểm: một file có sẵn (mock) hoặc dịch vụ bản đồ thật |

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

---

## Lỗi đã phát hiện

| Mã lỗi | Test case | Ngày | Hiện tượng | Nguyên nhân | Cách sửa | Trạng thái |
|---|---|---|---|---|---|---|
| BUG-PLACE-001 | Test mới của Task 3.1 Mốc 4: mỗi điểm đến phải có đủ các loại địa điểm | 2026-10-01 | `MockPlacesDataTest > everyDestinationHasEnoughPlacesOfSeveralKinds() FAILED`: với Hội An, test mong đợi có đủ bốn loại tham quan, ăn uống, lưu trú, di chuyển; thực tế thiếu loại di chuyển (`could not find the following element(s): ["TRANSPORT"]`). 19 test còn lại của hai class đạt | **Dữ liệu thiếu**, test đúng. Khi soạn danh sách Hội An, lần tra "Bến xe Hội An" trên OpenStreetMap không ra kết quả dùng được nên điểm đến này bị bỏ trống loại di chuyển | Tra thêm và bổ sung "Cảng du lịch Cửa Đại" (bến tàu đi Cù Lao Chàm) vào Hội An | Đã sửa, commit `3ba2e85` |

BUG-PLACE-001 cho thấy vì sao dữ liệu cũng cần test: người soạn dữ liệu (ở đây là trợ lý lập trình) không nhận ra mình bỏ sót một loại cho tới khi test đếm hộ. Lỗi được ghi vào tài liệu trước khi bổ sung dữ liệu.

# 08 · Thời tiết

> Cập nhật: 2026-10-05 · Task 3.8 Commit 8 (Open-Meteo có thử lại và ngắt mạch; kịch bản ở [01-platform.md](01-platform.md) phần H, `TC-PLAT-045`, `048`, `052`): build xanh, 955 lượt test · commit sửa `BUG-PLACE-004` (khoá của bản giữ tạm dự báo có tên nguồn; `TC-WEATHER-038`, `056`) tại `08bd0ad`, 935 lượt test · Commit 2 (nguồn dự báo lỗi thì vẫn trả lời, không có dự báo) tại `8b7a4c3`, build xanh, 821 lượt test · Commit 1 tại `90f0eb5` (818 lượt test) · phần giao diện (Commit 3) ghi ở [06-itinerary-ui.md](06-itinerary-ui.md), `MT-UI-90` · trước đó build xanh tại `957550e` (Task 3.4, 742 lượt test) · kiểm tra thủ công `MT-WEATHER-01`, `MT-WEATHER-02`, `MT-WEATHER-03` chưa chạy · [Về trang chính](README.md)

Mỗi ngày của chuyến đi có một dự báo thời tiết: trời thế nào, nhiệt độ thấp nhất và cao nhất, khả năng mưa. Dự báo lấy theo điểm đến của chuyến đi. Phần backend làm ở Task 3.3; giao diện ở Task 3.7; nguồn dự báo thật (Open-Meteo) ở Task 3.8.

File này được ghi dần theo từng commit của Task 3.3:

| Commit | Nội dung | Phần trong file | Mã commit |
|---|---|---|---|
| 1 | Khuôn dữ liệu của một dự báo và "cổng" để hỏi dự báo. Chưa có hành vi, không có kịch bản | (không có) | `8e6ac29` |
| 2 | Nguồn dự báo giả: không cần mạng, luôn trả cùng kết quả cho cùng câu hỏi | A | `df402db` |
| 3 | Xem dự báo từng ngày của một chuyến đi (`GET /api/v1/weather/trips/{tripId}`) | B, C | `2413c06` |
| 4 | Nói rõ khi chuyến đi chưa có điểm đến (`status`), để giao diện mời chọn điểm đến | D | `5923b50` |
| 5 | "Hôm nay" là ngày nào đối với một tài khoản, tính theo múi giờ của tài khoản đó (chuẩn bị cho giới hạn 16 ngày) | E | `f7d0704` |
| 6 | Chỉ có dự báo cho 16 ngày tính từ hôm nay; ngày đã qua hoặc xa hơn ghi "chưa có dự báo" | F | `6f58daf` |
| 7 | Kiểm toàn luồng xem thời tiết qua mọi tầng, trên MySQL thật | G | `2085c1e` |

Task 3.4, giữ tạm dự báo trong Redis (kết nối Redis, health và Redis tắt ghi ở [01-platform.md](01-platform.md) phần F, G):

| Commit | Nội dung | Phần trong file | Mã commit |
|---|---|---|---|
| 6 | Giữ tạm dự báo thời tiết trong Redis 3 giờ | H | `957550e` |

Task 3.8, nguồn dự báo thật:

| Commit | Nội dung | Phần trong file | Mã commit |
|---|---|---|---|
| 1 | Nguồn dự báo thật Open-Meteo, bật bằng cấu hình `app.providers.weather=open-meteo`; mặc định vẫn là nguồn giả | I | `90f0eb5` |
| 2 | Nguồn dự báo lỗi: vẫn trả lời 200 với trạng thái `UNAVAILABLE`, không có dự báo | J | `8b7a4c3` |

Vài từ dùng trong file:

| Từ | Nghĩa |
|---|---|
| Nguồn dự báo | Nơi ứng dụng lấy dự báo: một bộ sinh số giả (mock) hoặc dịch vụ thời tiết thật |
| Nguồn giả | Bộ sinh số có sẵn trong ứng dụng. Số là giả, nhưng cùng một nơi và cùng một ngày thì luôn ra cùng một dự báo |
| Nguồn thật (Open-Meteo) | Dịch vụ dự báo thời tiết trên mạng, miễn phí cho mục đích phi thương mại, không cần đăng ký. Có dự báo cho hôm nay và 15 ngày sau |
| Mã thời tiết | Con số Open-Meteo dùng để nói trời thế nào (ví dụ 0 là trời quang, 95 là dông). Ứng dụng đổi con số này về một trong 7 tình trạng |
| Máy chủ giả | Một chương trình nhỏ chạy ngay trong lúc test, đóng vai dịch vụ bên ngoài. Nhờ nó test không cần mạng mà vẫn thử được cả lúc dịch vụ trả lỗi hoặc không trả lời |
| Tình trạng | Một trong 7 giá trị: trời quang, ít mây, nhiều mây, sương mù, mưa, dông, tuyết. Giao diện chọn icon theo giá trị này |
| Khả năng mưa | Số từ 0 đến 100 (phần trăm) |
| Múi giờ của tài khoản | Múi giờ lưu trong hồ sơ người dùng, mặc định là giờ Việt Nam (`Asia/Ho_Chi_Minh`). "Hôm nay" của người dùng được tính theo múi giờ này |

---

## A. Nguồn dự báo giả

> **Yêu cầu:** design.md 7.2 (`WeatherProvider`), 10.2 "Quy ước Weather API" · **Kiểm bởi:** `MockWeatherProviderTest`

Khi chưa nối với dịch vụ thời tiết thật, ứng dụng dùng nguồn giả. Nó phải **ổn định**: test tự động không được lúc đạt lúc lỗi, và người dùng tải lại trang phải thấy cùng một dự báo. Nó cũng phải **hợp lý**: không có ngày nào "trời quang, khả năng mưa 90%".

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-WEATHER-001 | Hỏi dự báo 7 ngày của Đà Nẵng hai lần, rồi hỏi lần thứ ba sau khi "khởi động lại" nguồn | Cả ba lần ra cùng một danh sách | Đúng | Đạt |
| TC-WEATHER-002 | Hỏi riêng một ngày, rồi hỏi cả tuần có chứa ngày đó | Dự báo của ngày đó giống nhau ở cả hai lần: nó không phụ thuộc vào việc được hỏi cùng những ngày nào | Biên | Đạt |
| TC-WEATHER-003 | Hỏi từ ngày 05/10 đến 07/10, rồi hỏi một khoảng chỉ có một ngày | Mỗi ngày trong khoảng có đúng một dự báo, ngày sớm đứng trước, tính cả ngày cuối. Khoảng một ngày ra một dự báo | Biên | Đạt |
| TC-WEATHER-004 | Hỏi 30 ngày ở Đà Nẵng và 30 ngày ở Hà Nội | Hai nơi ra hai chuỗi dự báo khác nhau; 30 ngày ở một nơi không phải 30 bản sao của một ngày | Đúng | Đạt |
| TC-WEATHER-005 | Hỏi hai điểm cách nhau dưới khoảng 11 m (toạ độ giống nhau tới chữ số thập phân thứ tư), rồi một điểm xa hơn | Hai điểm sát nhau dùng chung một dự báo; điểm xa hơn có dự báo khác. Cách làm tròn này trùng với khoá lưu tạm sẽ dùng ở Task 3.4 | Biên | Đạt |
| TC-WEATHER-006 | Xem dự báo của 365 ngày liên tiếp | Khả năng mưa từ 0 đến 100. Nhiệt độ cao nhất từ 24 đến 35 độ, nhiệt độ thấp nhất luôn thấp hơn 4 đến 8 độ. Mọi nhiệt độ có đúng một chữ số thập phân | Biên | Đạt |
| TC-WEATHER-007 | Xem tình trạng của 365 ngày đó | Ngày có khả năng mưa từ 60% trở lên luôn là "mưa" hoặc "dông"; ngày dưới 60% không bao giờ là hai tình trạng đó. Trong một năm xuất hiện đủ 5 tình trạng của khí hậu ấm; không có "sương mù" hay "tuyết" | Đúng | Đạt |

Kiểm chứng ngược (2026-10-03): tạm cho tình trạng được chọn ngẫu nhiên, không theo khả năng mưa, thì `TC-WEATHER-007` đỏ (1 trong 7 test); trả lại code thì xanh. Điều đó chứng minh test thật sự bảo vệ quy tắc "tình trạng đi theo khả năng mưa".

## B. Ghép dự báo vào từng ngày của chuyến đi

> **Yêu cầu:** design.md 10.2 "Quy ước Weather API", rule 14.20 · **Kiểm bởi:** `WeatherServiceTest`

Ứng dụng hỏi nguồn dự báo **một lần cho cả chuyến đi**, tại toạ độ điểm đến của chuyến đi, rồi đặt dự báo của ngày nào vào đúng ngày đó. Kết quả luôn có đủ mọi ngày của chuyến đi; ngày không có dự báo thì ô dự báo để trống.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-WEATHER-008 | Chuyến đi 3 ngày (05/10 đến 07/10) có toạ độ điểm đến; nguồn có dự báo cho cả 3 ngày | Trả 3 phần tử theo thứ tự ngày, mỗi phần tử có mã của ngày, ngày lịch và dự báo (tình trạng, nhiệt độ thấp / cao, khả năng mưa). Nguồn được hỏi đúng một lần, với toạ độ điểm đến và khoảng ngày của chuyến đi | Đúng | Đạt |
| TC-WEATHER-009 | Nguồn trả dự báo lộn thứ tự, thiếu ngày giữa, và thừa một ngày không thuộc chuyến đi | Ngày đầu và ngày cuối nhận đúng dự báo của mình; ngày giữa không có dự báo; ngày thừa bị bỏ qua. Dự báo được ghép theo **ngày**, không theo vị trí trong danh sách | Biên | Đạt |
| TC-WEATHER-010 | Nguồn trả hai dự báo cho cùng một ngày | Không lỗi; dự báo đầu tiên của ngày đó được dùng | Biên | Đạt |
| TC-WEATHER-011 | Chuyến đi chưa có toạ độ điểm đến | Vẫn trả đủ 3 ngày, không ngày nào có dự báo. Nguồn dự báo **không bị hỏi**. Từ Commit 4: trạng thái là "chưa có điểm đến". Từ Commit 6: cũng không cần tra "hôm nay" | Biên | Đạt |
| TC-WEATHER-012 | Chuyến đi không tồn tại hoặc đã xoá | Báo "không tìm thấy". Không đọc các ngày, không hỏi nguồn | Sai | Đạt |

Kiểm chứng ngược (2026-10-03): tạm bỏ bước kiểm "chuyến đi có toạ độ chưa" thì `TC-WEATHER-011` đỏ (1 trong 5 test); trả lại code thì xanh.

**Điểm hở tạm thời sau Commit 3, đã đóng ở Commit 6:** từ Commit 3 đến Commit 5, ngày đã qua và ngày quá xa vẫn có dự báo, vì nguồn giả trả mọi ngày được hỏi. Quy tắc "chỉ 16 ngày tới" (design rule 14.20) được thêm ở Commit 6; xem phần F. Các kịch bản của phần B từ Commit 6 chạy với "hôm nay" là 05/10, ngày đầu của chuyến đi.

## C. Endpoint xem thời tiết của chuyến đi

> **Yêu cầu:** design.md 10.2 "Weather", 6.2 (quyền xem chuyến đi) · **Kiểm bởi:** `WeatherControllerTest`

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-WEATHER-013 | Người có quyền xem gọi `GET /api/v1/weather/trips/5` | 200. Từ Commit 4 có `status` = `OK`. Từ Commit 6, "hôm nay" được tính cho đúng tài khoản đang đăng nhập (mã lấy từ token, không lấy từ request). Mỗi ngày có `dayId`, `date`, `forecast`; `forecast` gồm `condition`, `tempMin`, `tempMax`, `precipitationProbability`. Ngày không có dự báo thì `forecast` là `null` | Đúng | Đạt |
| TC-WEATHER-014 | Gọi khi chưa đăng nhập | 401 `UNAUTHORIZED`, không chạm tới phần xử lý | Bảo mật | Đạt |
| TC-WEATHER-015 | Người không có quyền xem chuyến đi gọi | 403 `FORBIDDEN`, không chạm tới phần xử lý | Bảo mật | Đạt |
| TC-WEATHER-016 | Chuyến đi không tồn tại hoặc đã xoá | 404 `RESOURCE_NOT_FOUND` | Sai | Đạt |
| TC-WEATHER-017 | Mã chuyến đi không phải số (`/weather/trips/abc`) | 400 `VALIDATION_ERROR`, không chạm tới phần xử lý | Sai | Đạt |

## D. Chuyến đi chưa có điểm đến

> **Yêu cầu:** design.md 10.2 "Quy ước Weather API" (`status`), 5.2 bảng `trips` (điểm đến được chọn sau) · **Kiểm bởi:** `WeatherServiceTest`, `WeatherControllerTest`

Người dùng được tạo chuyến đi trước rồi chọn điểm đến sau. Khi đó không có nơi nào để hỏi dự báo. Đây **không phải lỗi**: hệ thống vẫn trả 200 và ghi rõ lý do bằng `status`, để giao diện hiện câu mời chọn điểm đến thay vì "chưa có dự báo".

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-WEATHER-018 | Chuyến đi chỉ có một nửa toạ độ (có vĩ độ, thiếu kinh độ) | Coi như chưa có điểm đến: `status` = `NO_DESTINATION`, nguồn dự báo không bị hỏi. Việc tạo và sửa chuyến đi vốn đã từ chối nửa toạ độ; đây là lớp chặn thứ hai để trang không hỏng | Biên | Đạt |
| TC-WEATHER-019 | Chuyến đi **có** điểm đến nhưng nguồn không có dự báo cho ngày nào | `status` = `OK`, đủ 3 ngày, không ngày nào có dự báo. "Chưa có dự báo" khác "chưa có điểm đến": giao diện không được mời chọn điểm đến ở đây | Biên | Đạt |
| TC-WEATHER-020 | Gọi endpoint cho chuyến đi chưa có điểm đến | **200** (không phải mã lỗi), `status` = `NO_DESTINATION`, các ngày có `forecast` là `null` | Đúng | Đạt |

`TC-WEATHER-008` (có điểm đến → `OK`) và `TC-WEATHER-011` (chưa có toạ độ → `NO_DESTINATION`) ở phần B cũng kiểm `status` từ commit này.

Kiểm chứng ngược (2026-10-03): tạm coi "có một trong hai toạ độ" là đã có điểm đến thì `TC-WEATHER-018` đỏ (1 trong 7 test); trả lại code thì xanh.

## E. "Hôm nay" theo múi giờ của tài khoản

> **Yêu cầu:** design.md rule 14.22 ("hôm nay" theo `users.timezone`), 10.2 "Quy ước Weather API" · **Kiểm bởi:** `UserServiceTest`, `UserRepositoryTest`

Dự báo chỉ có cho 16 ngày tính từ **hôm nay**, nên trước hết hệ thống phải biết hôm nay là ngày nào. Cùng một thời điểm, ở Việt Nam có thể đã sang ngày mới trong khi ở Mỹ vẫn là ngày hôm trước. Hệ thống lấy múi giờ trong hồ sơ của tài khoản đang đăng nhập để tính. Commit 5 chỉ thêm phần tính ngày; Commit 6 mới dùng nó cho thời tiết.

Test dùng một đồng hồ đứng yên tại thời điểm chọn sẵn, nên kết quả không phụ thuộc lúc chạy test.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-WEATHER-021 | Lúc 23:30 ngày 05/10 theo giờ quốc tế (UTC), hỏi "hôm nay" của ba tài khoản ở ba múi giờ: Việt Nam, UTC, Los Angeles | Việt Nam: 06/10 (ở đó đã 06:30 sáng hôm sau). UTC: 05/10. Los Angeles: 05/10 | Đúng | Đạt |
| TC-WEATHER-022 | Tài khoản giờ Việt Nam, hỏi lúc 16:59:59 UTC rồi lúc 17:00:00 UTC | Lần đầu là 05/10, lần sau là 06/10: ngày đổi đúng lúc nửa đêm ở Việt Nam (UTC+7) | Biên | Đạt |
| TC-WEATHER-023 | Múi giờ lưu trong hồ sơ là một giá trị không đọc được ("Mars/Olympus", hoặc chuỗi rỗng) | Không báo lỗi: dùng giờ Việt Nam và ghi cảnh báo vào log để người vận hành biết | Sai | Đạt |
| TC-WEATHER-024 | Hỏi "hôm nay" của một tài khoản không tồn tại hoặc đã xoá | Báo "không tìm thấy" | Sai | Đạt |
| TC-WEATHER-025 | Đọc múi giờ của tài khoản đặt "Europe/Paris" và của tài khoản không đặt gì, trên MySQL thật | Ra "Europe/Paris" và giá trị mặc định "Asia/Ho_Chi_Minh" | Đúng | Đạt |
| TC-WEATHER-026 | Đọc múi giờ của tài khoản đã xoá và của một mã không tồn tại, trên MySQL thật | Không ra gì: tài khoản đã xoá không còn được tính | Biên | Đạt |

Kiểm chứng ngược (2026-10-03): tạm bỏ bước đổi sang múi giờ của tài khoản (luôn tính theo UTC) thì `TC-WEATHER-021`, `022`, `023` đỏ (3 trong 4 test của `UserServiceTest`); trả lại code thì xanh.

## F. Chỉ có dự báo cho 16 ngày tới

> **Yêu cầu:** design.md rule 14.20 (16 ngày tính từ hôm nay), rule 14.22 ("hôm nay" theo múi giờ tài khoản) · **Kiểm bởi:** `WeatherServiceTest`

Dịch vụ dự báo thật chỉ dự báo được khoảng 16 ngày. Quy tắc được đặt ở phần xử lý của ứng dụng chứ không ở nguồn, nên nguồn giả cũng tuân theo: người dùng thấy cùng một hành vi trước và sau khi nối dịch vụ thật. Khoảng có dự báo là **hôm nay và 15 ngày sau đó**. Ngày của chuyến đi nằm ngoài khoảng này vẫn có trong kết quả, nhưng ô dự báo để trống. Ứng dụng chỉ hỏi nguồn đúng phần chuyến đi nằm trong khoảng; không có ngày nào nằm trong thì không hỏi.

Chuyến đi mẫu: 05/10 đến 07/10, trừ khi ghi khác.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-WEATHER-027 | Hôm nay là 06/10 (chuyến đi đang diễn ra) | 05/10 đã qua: không có dự báo. 06/10 (hôm nay) và 07/10 có dự báo. Nguồn chỉ bị hỏi từ 06/10 đến 07/10 | Biên | Đạt |
| TC-WEATHER-028 | Hôm nay là 05/10, chuyến đi từ 19/10 đến 22/10 | 19/10 và 20/10 có dự báo; 20/10 là ngày thứ 16 tính cả hôm nay. 21/10 (ngày thứ 17) và 22/10 không có. Nguồn chỉ bị hỏi từ 19/10 đến 20/10 | Biên | Đạt |
| TC-WEATHER-029 | Hôm nay là 08/10, chuyến đi đã kết thúc | Đủ 3 ngày, không ngày nào có dự báo, trạng thái vẫn `OK` (chuyến đi có điểm đến). Nguồn **không bị hỏi** | Biên | Đạt |
| TC-WEATHER-030 | Hôm nay là 19/09: ngày cuối của khoảng dự báo là 04/10, ngay trước ngày đầu chuyến đi | Không ngày nào có dự báo. Nguồn **không bị hỏi** | Biên | Đạt |
| TC-WEATHER-031 | Hôm nay là 20/09: ngày cuối của khoảng dự báo đúng là 05/10, ngày đầu chuyến đi | Chỉ 05/10 có dự báo. Nguồn bị hỏi đúng một ngày đó | Biên | Đạt |
| TC-WEATHER-032 | Hôm nay là 06/10; nguồn được hỏi 06/10 đến 07/10 nhưng trả thừa cả dự báo của 05/10 | 05/10 vẫn không có dự báo: nguồn trả thừa không làm khoảng rộng ra | Biên | Đạt |

Kiểm chứng ngược (2026-10-03), mỗi lần sửa một chỗ rồi trả lại:
- Cho khoảng dài 17 ngày thay vì 16: `TC-WEATHER-028`, `030`, `031` đỏ (3 trong 13 test).
- Bỏ bước lọc kết quả nguồn trả thừa: `TC-WEATHER-032` đỏ.
- Cho khoảng bắt đầu từ ngày đầu chuyến đi thay vì hôm nay: `TC-WEATHER-027`, `029`, `032` đỏ.

## G. Kiểm toàn luồng xem thời tiết qua mọi tầng

> **Yêu cầu:** design.md 10.2 "Weather", rule 14.20, 14.22, 6.2 (quyền xem), CLAUDE.md mục 8 (không N+1) · **Kiểm bởi:** `TripWeatherFlowIntegrationTest`

Các phần trên kiểm từng mảnh riêng, với những mảnh xung quanh được thay bằng đồ giả. Phần này chạy **cả ứng dụng thật** trên MySQL thật: tạo chuyến đi qua API, rồi xem thời tiết qua API. Chỉ có đồng hồ là đứng yên tại 03:00 ngày 05/10/2026 theo giờ UTC (10:00 sáng ngày 05/10 ở Việt Nam, 20:00 tối ngày 04/10 ở Los Angeles), để kết quả không phụ thuộc ngày chạy test.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-WEATHER-033 | Tạo chuyến đi 19 ngày (04/10 đến 22/10) có điểm đến Đà Nẵng, rồi xem thời tiết hai lần | `status` = `OK`, đủ 19 phần tử, đúng mã ngày và đúng thứ tự như trang lịch trình. 04/10 (hôm qua) không có dự báo; 05/10 đến 20/10 (16 ngày) có; 21/10 và 22/10 không. Số liệu đúng là số của nguồn giả tại toạ độ điểm đến. Lần xem thứ hai giống hệt lần đầu | Đúng | Đạt |
| TC-WEATHER-034 | Tạo chuyến đi chưa có điểm đến, xem thời tiết; chọn điểm đến (sửa chuyến đi), xem lại | Lần đầu: 200, `NO_DESTINATION`, không ngày nào có dự báo. Sau khi chọn điểm đến: `OK`, cả 3 ngày có dự báo | Đúng | Đạt |
| TC-WEATHER-035 | Cùng một thời điểm, hai người ở hai múi giờ (Việt Nam, Los Angeles) xem chuyến đi 04/10 đến 06/10 của mình | Người ở Việt Nam: 04/10 đã qua, không có dự báo. Người ở Los Angeles: 04/10 là hôm nay, có dự báo. Ngày cả hai cùng thấy thì dự báo giống nhau. Múi giờ được đọc từ hồ sơ tài khoản trong database | Biên | Đạt |
| TC-WEATHER-036 | Chưa đăng nhập; người lạ; mã chuyến đi không tồn tại; chuyến đi đã xoá | Lần lượt 401, 403, 404, 404. Chuyến đi đã xoá thì chủ chuyến đi và người lạ đều nhận 404 | Bảo mật | Đạt |
| TC-WEATHER-037 | Đếm số câu SQL khi xem thời tiết của chuyến đi 3 ngày, 30 ngày, và 30 ngày chưa có điểm đến | 4 câu cho cả chuyến 3 ngày lẫn 30 ngày (quyền, chuyến đi, các ngày, múi giờ): số câu không tăng theo số ngày. 3 câu khi chưa có điểm đến (không cần tra múi giờ) | Biên | Đạt |

Kiểm chứng ngược (2026-10-03): tạm cho "hôm nay" luôn tính theo giờ Việt Nam, bỏ qua múi giờ của tài khoản, thì `TC-WEATHER-035` đỏ (1 trong 5 test); trả lại code thì xanh.

## H. Giữ tạm dự báo trong Redis

> **Yêu cầu:** design.md 8.1 (`weather:forecast`, 3 giờ; khoá = toạ độ làm tròn 4 chữ số + ngày đầu + ngày cuối) · **Kiểm bởi:** `ForecastCacheIntegrationTest`, `WeatherServiceTest`

Cùng cách làm với tìm địa điểm ([07-place.md](07-place.md) phần L, M): mỗi câu hỏi "dự báo ở điểm này, từ ngày này tới ngày này" là một khoá; câu trả lời được cất 3 giờ, vì dự báo được cập nhật vài lần mỗi ngày. Test chạy với Redis thật và chứng minh "lấy từ Redis" bằng cách đánh tráo câu trả lời đã cất.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-WEATHER-038 | Hỏi dự báo Đà Nẵng từ 05/10 đến 07/10 lần đầu, rồi nhìn vào Redis | Có ô mang khoá `weather:forecast::mock\|16.0678,108.2208:2026-10-05:2026-10-07` (tên nguồn đứng đầu từ lần sửa BUG-PLACE-004), còn hạn gần đủ 3 giờ. Nội dung là JSON đọc được với ngày dạng `2026-10-05`, không chứa tên class Java | Đúng | Đạt |
| TC-WEATHER-039 | Đánh tráo câu trả lời trong Redis bằng "tuyết ở Đà Nẵng", rồi hỏi lại; hỏi với một điểm cách 8 m | Cả hai lần nhận "tuyết": câu trả lời lấy từ Redis, và hai điểm sát nhau là một điểm | Đúng | Đạt |
| TC-WEATHER-040 | Sau khi đánh tráo, hỏi Hà Nội cùng khoảng ngày; hỏi Đà Nẵng nhưng ngắn hơn một ngày | Cả hai lần nhận dự báo thật từ nguồn. Khoảng ngày khác là câu hỏi khác, dù có trùng ngày với câu đã cất | Biên | Đạt |
| TC-WEATHER-041 | Hỏi 16 ngày hai lần liên tiếp, lần hai lấy từ Redis | Hai danh sách bằng nhau hoàn toàn: ngày, tình trạng, nhiệt độ tới chữ số thập phân. Dữ liệu đọc lại vẫn đúng kiểu | Đúng | Đạt |
| TC-WEATHER-056 | (Task 3.8, `ForecastCacheTest`) Cùng một câu hỏi (Đà Nẵng, 05/10 đến 07/10) hỏi nguồn giả lập và hỏi Open-Meteo; một điểm cách 8 m; một khoảng ngày ngắn hơn | Hai nguồn ra hai khoá khác nhau: `mock\|16.0678,...` và `open-meteo\|16.0678,...`, nên con số giả lập không bao giờ hiện như dự báo thật sau khi đổi nguồn. Điểm cách 8 m cùng khoá; khoảng ngày khác thì khác khoá | Biên | Đạt · từng lỗi BUG-PLACE-004 |
| TC-WEATHER-042 | Xem danh sách cache đã khai báo | Đúng ba cache: tìm địa điểm, quãng đường (thêm ở Task 3.8 Commit 7) và dự báo, không có cái nào khác | Biên | Đạt |

`WeatherServiceTest` (phần B, F) từ commit này chạy với bản giả của lớp cache thay cho bản giả của nguồn: mọi kịch bản cũ giữ nguyên kết quả. Redis tắt mà xem thời tiết vẫn 200: [01-platform.md](01-platform.md) `TC-PLAT-039`.

Kiểm chứng ngược (2026-10-03): bỏ dòng bật cache trên hàm hỏi dự báo thì `TC-WEATHER-038`, `039` đỏ (2 trong 5 test); trả lại thì xanh.

---

## I. Nguồn dự báo thật (Open-Meteo)

> **Yêu cầu:** design.md 7.2 (bảng dịch vụ, quy ước chung), 7.3 (giới hạn thời gian), 10.2 (7 tình trạng) · **Kiểm bởi:** `OpenMeteoWeatherProviderTest`

Khi bật nguồn thật, ứng dụng hỏi Open-Meteo qua mạng thay cho bộ sinh số giả. Test **không gọi mạng**: câu trả lời do máy chủ giả đưa ra. Câu trả lời mẫu là một câu trả lời thật của Open-Meteo cho Đà Nẵng, lưu ngày 2026-10-05, gồm 16 ngày từ 05/10 đến 20/10.

Mỗi lần hỏi, ứng dụng lấy **toàn bộ** dự báo của điểm đó (16 ngày) rồi tự giữ lại những ngày cần. Lý do: nếu hỏi đúng một khoảng ngày mà có một ngày nằm ngoài phần Open-Meteo có, dịch vụ từ chối cả câu hỏi; trong khi "hôm nay" ở điểm đến có thể lệch một ngày so với "hôm nay" của người xem.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-WEATHER-043 | Hỏi dự báo Đà Nẵng từ 06/10 đến 08/10, dịch vụ trả câu trả lời mẫu 16 ngày | Đúng 3 ngày, đúng số của mẫu: 06/10 mưa, 23,8 đến 29,0 độ, 98% mưa; 07/10 và 08/10 dông | Đúng | Đạt |
| TC-WEATHER-044 | Xem ứng dụng gửi gì cho dịch vụ | Một lần gọi, hỏi đủ 16 ngày, ngày tính theo giờ tại điểm đến, toạ độ làm tròn 4 chữ số thập phân, và có ghi tên ứng dụng (`User-Agent`) như điều khoản của dịch vụ yêu cầu | Đúng | Đạt |
| TC-WEATHER-045 | Hỏi từ 19/10 đến 22/10 trong khi dịch vụ chỉ có tới 20/10 | Nhận 19/10 và 20/10. Hai ngày dịch vụ không có thì vắng mặt, không phải lỗi | Biên | Đạt |
| TC-WEATHER-046 | Hỏi một khoảng nằm hẳn sau phần dịch vụ có (21/10 đến 23/10) | Danh sách rỗng, không báo lỗi | Biên | Đạt |
| TC-WEATHER-047 | Dịch vụ trả lần lượt từng mã trong 28 mã thời tiết của nó | Mỗi mã ra đúng một trong 7 tình trạng: 0 và 1 là trời quang, 2 ít mây, 3 nhiều mây, 45 và 48 sương mù, mưa phùn / mưa / mưa rào đều là mưa, các mã tuyết là tuyết, 95 / 96 / 99 là dông | Đúng | Đạt |
| TC-WEATHER-048 | Một ngày bị dịch vụ bỏ trống một giá trị (mã thời tiết, nhiệt độ thấp, nhiệt độ cao hoặc khả năng mưa), hoặc mang một mã thời tiết không có trong bảng | Ngày đó vắng mặt. Ứng dụng không tự bịa số cho ngày thiếu dữ liệu | Biên | Đạt |
| TC-WEATHER-049 | Dịch vụ trả mã lỗi: 400, 429 (gọi quá nhiều), 500, 502, 503 | Ứng dụng báo "dịch vụ bên ngoài không sẵn sàng" (`PROVIDER_UNAVAILABLE`), có ghi tên dịch vụ để người vận hành tra log | Sai | Đạt |
| TC-WEATHER-050 | Dịch vụ trả 200 nhưng nội dung không đọc được: không phải JSON, JSON rỗng, thiếu phần dự báo theo ngày, thiếu danh sách ngày, hoặc số ngày và số giá trị không bằng nhau | `PROVIDER_UNAVAILABLE`. Ứng dụng không đoán dự báo từ dữ liệu lệch | Sai | Đạt |
| TC-WEATHER-051 | Dịch vụ nhận câu hỏi rồi im lặng (máy chủ giả không bao giờ trả lời), ứng dụng chỉ chờ 0,2 giây | Hết thời gian chờ thì bỏ cuộc và báo `PROVIDER_UNAVAILABLE`, không treo | Sai | Đạt |
| TC-WEATHER-052 | Không kết nối được tới dịch vụ (địa chỉ không có ai nghe) | `PROVIDER_UNAVAILABLE` | Sai | Đạt |

Giới hạn thật khi chạy là 2 giây để kết nối và 3 giây chờ trả lời, đặt trong cấu hình chung (`spring.http.clients`). `TC-WEATHER-051` chứng minh ứng dụng xử lý đúng khi hết thời gian chờ, với một giới hạn ngắn đặt riêng cho test; **chưa có test tự động nào** chứng minh con số 3 giây trong cấu hình được áp dụng lúc chạy thật. Đã kiểm bằng tay ngày 2026-10-05: ứng dụng khởi động được với `app.providers.weather=open-meteo`.

Ở Commit 1, khi bật nguồn thật mà Open-Meteo lỗi thì `GET /api/v1/weather/trips/{id}` trả **503**. Commit 2 đã đổi thành 200 kèm trạng thái "tạm thời không có dự báo" (phần J).

---

## J. Nguồn dự báo không trả lời

> **Yêu cầu:** design.md 7.3 ("Khi provider lỗi"), 10.2 (dòng "Provider lỗi") · **Kiểm bởi:** `WeatherServiceTest`, `TripWeatherFlowIntegrationTest`

Thời tiết là thông tin phụ của trang chuyến đi. Khi nguồn dự báo lỗi (sập, quá chậm, trả nội dung không đọc được), ứng dụng vẫn trả lời bình thường (200) với trạng thái `UNAVAILABLE` ("tạm thời không có dự báo") thay vì báo lỗi 503. Lịch trình vẫn dùng được.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-WEATHER-053 | Chuyến đi 3 ngày có điểm đến, nguồn dự báo báo "không sẵn sàng" | Không báo lỗi. Trạng thái `UNAVAILABLE`, vẫn đủ 3 ngày theo thứ tự, không ngày nào có dự báo | Sai | Đạt |
| TC-WEATHER-054 | Lúc lấy dự báo xảy ra một lỗi khác, không phải "nguồn không sẵn sàng" (ví dụ lỗi lập trình) | Lỗi đó **không** bị che thành "tạm thời không có dự báo": vẫn được báo ra để người vận hành thấy | Sai | Đạt |
| TC-WEATHER-055 | Qua mọi tầng với MySQL và Redis thật: chuyến đi 3 ngày ở Huế, nguồn dự báo lỗi ở lần hỏi đầu rồi hoạt động lại | Lần đầu: 200, `UNAVAILABLE`, 3 ngày không có dự báo, log có một dòng cảnh báo ghi mã chuyến đi. Lần sau: `OK`, cả 3 ngày có dự báo. Câu trả lời "không có dự báo" không bị giữ lại trong Redis, nên nguồn được hỏi lại (đúng 2 lần hỏi) | Sai | Đạt |

Chuyến đi không cần hỏi nguồn (chưa có điểm đến, đã kết thúc, hoặc còn quá xa) không bị ảnh hưởng: nguồn không được gọi nên trạng thái vẫn như cũ (`TC-WEATHER-018`, `TC-WEATHER-029`, `TC-WEATHER-030`).

Giao diện chưa đổi ở commit này: khi nhận `UNAVAILABLE`, thẻ thời tiết ghi nhầm lý do ("Chưa có dự báo. Dự báo chỉ có cho 16 ngày tới..."). Commit 3 sửa thành "Tạm thời không có dự báo." kèm "Thử lại" (`MT-UI-90`).

---

## Kiểm tra thủ công

### MT-WEATHER-01 · Xem thời tiết của chuyến đi trên Swagger

Thêm ở Task 3.3 Commit 3. Bổ sung `status` ở Commit 4, giới hạn 16 ngày ở Commit 6. Ngày của chuyến đi trong các bước dưới đây tính theo **ngày chạy bài**.

- [ ] Chạy backend, mở `http://localhost:8080/swagger-ui.html`, đăng nhập và "Authorize".
- [ ] Tạo một chuyến đi 3 ngày **bắt đầu từ hôm nay**, có `destinationLat` = 16.0678, `destinationLng` = 108.2208 (`POST /api/v1/trips`). Ghi lại `id`.
- [ ] Gọi `GET /api/v1/weather/trips/{id}`. Trả **200**, `status` là `OK`, `days` có đúng 3 phần tử theo thứ tự ngày; mỗi phần tử có `forecast` với `condition`, `tempMin` nhỏ hơn `tempMax`, `precipitationProbability` từ 0 đến 100.
- [ ] Gọi lại lần nữa: kết quả giống hệt lần trước.
- [ ] Tạo một chuyến đi 20 ngày bắt đầu từ hôm nay, cùng toạ độ, rồi gọi: 16 ngày đầu có `forecast`, 4 ngày cuối `forecast` là `null`; `status` vẫn là `OK`.
- [ ] Tạo một chuyến đi 3 ngày đã kết thúc từ tuần trước, cùng toạ độ, rồi gọi: `status` là `OK`, `forecast` của cả 3 ngày là `null`.
- [ ] Tạo một chuyến đi không có toạ độ điểm đến rồi gọi: 200, `status` là `NO_DESTINATION`, đủ số ngày, `forecast` của mọi ngày là `null`.
- [ ] Gọi với mã chuyến đi không tồn tại (ví dụ 999999): 404.
- [ ] Đăng nhập bằng một tài khoản khác rồi gọi với mã chuyến đi đầu tiên: 403.
- [ ] Bấm "Authorize" → "Logout" rồi gọi lại: 401.

**Kết quả:** Chưa chạy

### MT-WEATHER-02 · Xem dự báo thật từ Open-Meteo

Thêm ở Task 3.8 Commit 1. Cần có mạng. Ngày của chuyến đi tính theo **ngày chạy bài**.

- [ ] Dừng backend đang chạy. Chạy lại với nguồn thật: `./gradlew bootRun --args='--spring.profiles.active=local --app.providers.weather=open-meteo'` (trong IntelliJ: thêm `--app.providers.weather=open-meteo` vào Program arguments).
- [ ] Mở `http://localhost:8080/swagger-ui.html`, đăng nhập và "Authorize".
- [ ] Tạo một chuyến đi 3 ngày **bắt đầu từ hôm nay**, có `destinationLat` = 16.0678, `destinationLng` = 108.2208 (Đà Nẵng). Ghi lại `id`.
- [ ] Gọi `GET /api/v1/weather/trips/{id}`: 200, `status` là `OK`, cả 3 ngày có `forecast`.
- [ ] Mở `https://open-meteo.com/en/docs`, nhập cùng toạ độ, chọn các giá trị theo ngày (nhiệt độ cao nhất, thấp nhất, khả năng mưa): nhiệt độ và khả năng mưa của 3 ngày khớp với kết quả ở bước trước.
- [ ] Tạo một chuyến đi 3 ngày ở một nơi lạnh (ví dụ Reykjavik: `destinationLat` = 64.1466, `destinationLng` = -21.9426) rồi gọi: nhiệt độ thấp hơn hẳn Đà Nẵng. Đây là dấu hiệu số không còn là số giả (nguồn giả luôn cho 24 đến 35 độ).
- [ ] Mở trang chuyến đi trên giao diện: thẻ thời tiết hiện đúng các số đó.
- [ ] Dừng backend, chạy lại như bình thường (không có tham số trên): thời tiết trở lại số của nguồn giả.

**Kết quả:** Chưa chạy

### MT-WEATHER-03 · Nguồn dự báo thật không trả lời

Thêm ở Task 3.8 Commit 2. Không cần mạng: nguồn thật được trỏ tới một địa chỉ không có ai nghe.

- [ ] Dừng backend đang chạy. Chạy lại: `./gradlew bootRun --args='--spring.profiles.active=local --app.providers.weather=open-meteo --app.providers.open-meteo.base-url=http://localhost:9'`.
- [ ] Mở Swagger, đăng nhập và "Authorize". Tạo một chuyến đi 3 ngày **bắt đầu từ hôm nay**, có `destinationLat` = 10.3460, `destinationLng` = 107.0843 (Vũng Tàu; dùng một nơi chưa xem thời tiết trong 3 giờ qua, nếu không Redis trả lời thay).
- [ ] Gọi `GET /api/v1/weather/trips/{id}`: **200** (không phải 503), `status` là `UNAVAILABLE`, `days` có 3 phần tử, `forecast` của cả 3 là `null`.
- [ ] Xem log của backend: có một dòng `WARN` bắt đầu bằng `No forecast for trip`, không có dòng `ERROR` nào cho lần gọi này.
- [ ] Mở trang chuyến đi đó trên giao diện: lịch trình hiện bình thường, không có ô báo lỗi.
- [ ] Dừng backend, chạy lại với nguồn thật và địa chỉ đúng (bỏ tham số `base-url`), gọi lại: `status` là `OK`, có dự báo.
- [ ] Dừng backend, chạy lại như bình thường.

**Kết quả:** Chưa chạy

---

## Lỗi đã phát hiện

Chưa có lỗi nào của riêng tính năng này. Một lỗi chung của các bản giữ tạm chạm tới dự báo: `BUG-PLACE-004` (khoá không có tên nguồn), ghi ở [07-place.md](07-place.md), đã sửa ngày 2026-10-05.

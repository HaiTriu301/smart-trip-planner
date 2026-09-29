# 05 · Hoạt động trong ngày

> Cập nhật: 2026-09-29 · build xanh tại commit `50bb13c` · [Về trang chính](README.md)

Hoạt động là một việc cần làm trong một ngày của chuyến đi, ví dụ "Ăn trưa" từ 11:30 đến 13:00. Làm ở Task 2.3, hoàn thành ngày 2026-09-29.

File này được ghi dần theo từng mốc của task. Mỗi mốc là một commit:

| Mốc | Nội dung | Phần trong file | Commit |
|---|---|---|---|
| 1 | Bảng dữ liệu của hoạt động | A | `3c4c294` |
| 2 | Thêm hoạt động | B | `facc713` |
| 3 | Chặn hoạt động trùng giờ | C | `0abaf97` |
| 4 | Xem danh sách hoạt động | D | `ea2df7a` |
| 5 | Sửa hoạt động | E | `a42bc99` |
| 6 | Xoá hoạt động | F | `6865849` |
| 7 | Chặn đổi ngày làm mất hoạt động | G | `ac37d1f` |
| 8 | Chi tiết chuyến đi kèm hoạt động | H | `e0e2a21` |
| 9 | Kiểm toàn luồng | I | `50bb13c` |

Vài từ dùng trong file:

| Từ | Nghĩa |
|---|---|
| Giờ trong ngày | Giờ không kèm ngày, ví dụ 09:00. Ngày lịch lấy theo ngày của chuyến đi |
| Số phiên bản | Số đếm tăng 1 sau mỗi lần sửa, dùng để phát hiện hai người sửa cùng lúc |
| Số thứ tự | Vị trí của hoạt động trong ngày. Các số cách nhau 1000 để sau này chèn vào giữa được |

---

## A. Lưu trữ hoạt động

> **Yêu cầu:** design.md 5.2 bảng `activities` · **Kiểm bởi:** `ActivityMappingTest`

Các kiểm tra này chạy thẳng trên database. Chúng là lớp bảo vệ cuối cùng, chặn dữ liệu sai kể cả khi phần kiểm tra ở ứng dụng có lỗi.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-ACT-001 | Lưu một hoạt động chỉ có tên | Lưu được. Loại mặc định là "khác", số phiên bản là 0, giờ, ghi chú, chi phí để trống | Đúng | Đạt |
| TC-ACT-002 | Lưu một hoạt động đầy đủ thông tin rồi đọc lại | Mọi thông tin đọc lại đúng, kể cả ghi chú dài, số tiền và người tạo | Đúng | Đạt |
| TC-ACT-003 | Lưu hoạt động từ 03:00 đến 08:15 trên máy đặt múi giờ Việt Nam | Database giữ đúng 03:00 và 08:15, không bị dịch theo múi giờ | Biên | Đạt · từng lỗi BUG-ACT-001 |
| TC-ACT-004 | Sửa một hoạt động | Số phiên bản tăng thêm 1 | Đúng | Đạt |
| TC-ACT-005 | Ghi thẳng vào database một hoạt động có giờ kết thúc trước giờ bắt đầu | Database từ chối | Sai | Đạt |
| TC-ACT-006 | Ghi thẳng vào database một hoạt động có giờ kết thúc nhưng không có giờ bắt đầu | Database từ chối | Sai | Đạt |
| TC-ACT-007 | Lưu một hoạt động chỉ có giờ bắt đầu | Lưu được | Biên | Đạt |
| TC-ACT-008 | Ghi thẳng vào database một hoạt động có chi phí âm | Database từ chối | Sai | Đạt |
| TC-ACT-009 | Xoá một ngày khỏi database | Các hoạt động của ngày đó bị xoá theo | Đúng | Đạt |
| TC-ACT-010 | Xoá hẳn một chuyến đi khỏi database | Các ngày và hoạt động của nó bị xoá theo | Đúng | Đạt |
| TC-ACT-011 | Xoá hẳn tài khoản của người đã tạo hoạt động | Database chặn lại, hoạt động vẫn còn | Sai | Đạt |

---

## B. Thêm hoạt động

> **Yêu cầu:** design.md 10.2 "Quy ước Activity API", rule 14.5, 6.2 · **Kiểm bởi:** `ActivityControllerTest`, `ActivityServiceTest`, `ActivityRepositoryTest`

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-ACT-012 | Thêm hoạt động với đầy đủ thông tin | 201. Người tạo là **người đang đăng nhập**. Khoảng trắng thừa ở tên và ghi chú được cắt | Đúng | Đạt |
| TC-ACT-013 | Thêm hoạt động chỉ với tên | 201. Loại là "khác", các ô còn lại để trống | Đúng | Đạt |
| TC-ACT-014 | Thêm hoạt động đầu tiên của một ngày | Số thứ tự là 1000 | Biên | Đạt |
| TC-ACT-015 | Thêm vào ngày đang có số thứ tự lớn nhất là 2500 | Số thứ tự mới là 3500. Hoạt động của ngày khác không ảnh hưởng | Đúng | Đạt |
| TC-ACT-016 | Nhập chi phí mà không chọn tiền tệ | Lấy tiền tệ của chuyến đi | Đúng | Đạt |
| TC-ACT-017 | Nhập chi phí kèm một tiền tệ khác với chuyến đi | Giữ tiền tệ người dùng chọn | Đúng | Đạt |
| TC-ACT-018 | Ghi chú chỉ có khoảng trắng | Lưu như không có ghi chú | Biên | Đạt |
| TC-ACT-019 | Gửi giờ có cả giây, ví dụ 09:00:45 | Phần giây bị bỏ, lưu 09:00. Phản hồi luôn hiện giờ dạng giờ và phút | Biên | Đạt |
| TC-ACT-020 | Chỉ nhập giờ bắt đầu | Chấp nhận | Biên | Đạt |
| TC-ACT-021 | Giờ kết thúc sau giờ bắt đầu đúng 1 phút | Chấp nhận | Biên | Đạt |
| TC-ACT-022 | Nhập giờ kết thúc mà không nhập giờ bắt đầu | 400, lỗi ở ô giờ bắt đầu | Sai | Đạt |
| TC-ACT-023 | Giờ kết thúc **bằng** giờ bắt đầu | 400, lỗi ở ô giờ kết thúc | Biên | Đạt |
| TC-ACT-024 | Giờ kết thúc trước giờ bắt đầu | 400: "Giờ kết thúc phải sau giờ bắt đầu" | Sai | Đạt |
| TC-ACT-025 | Hai giờ chỉ khác nhau ở phần giây, ví dụ 09:00:10 và 09:00:50 | 400, vì sau khi bỏ giây thì hai giờ bằng nhau | Biên | Đạt |
| TC-ACT-026 | Gửi form sai nhiều ô: tên trống, chi phí âm, tiền tệ viết thường, đường dẫn đặt chỗ không bắt đầu bằng http | 400, liệt kê đủ bốn ô sai. Không lưu gì | Sai | Đạt |
| TC-ACT-027 | Tên dài 201 ký tự | 400: "Tên hoạt động không được vượt quá 200 ký tự" | Biên | Đạt |
| TC-ACT-028 | Gửi loại không tồn tại, hoặc giờ không có thật như 25:00 | 400 `VALIDATION_ERROR` | Sai | Đạt |
| TC-ACT-029 | Thêm hoạt động khi chưa đăng nhập | 401 | Bảo mật | Đạt |
| TC-ACT-030 | Người lạ thêm hoạt động vào chuyến đi của người khác | 403, không lưu gì | Bảo mật | Đạt |
| TC-ACT-031 | Thêm vào một ngày không thuộc chuyến đi ghi trên đường dẫn | 404. Có quyền trên chuyến A cũng không thêm được vào ngày của chuyến B | Bảo mật | Đạt |
| TC-ACT-032 | Thêm vào chuyến đi không tồn tại hoặc đã xoá | 404 | Sai | Đạt |
| TC-ACT-033 | Mã ngày trên đường dẫn là chữ | 400 `VALIDATION_ERROR` | Sai | Đạt |

Hai giới hạn đã biết, thuộc thiết kế chứ không phải lỗi:

- Hạn mức 10 hoạt động mỗi ngày của gói miễn phí chưa được kiểm, vì tính năng đó thuộc Phase 6.
- Hai người thêm hoạt động vào cùng một ngày đúng cùng lúc có thể nhận cùng số thứ tự. Khi đó hoạt động tạo trước đứng trước.

---

## C. Chặn hoạt động trùng giờ

> **Yêu cầu:** design.md rule 14.4, 10.2 "Quy ước Activity API" · **Kiểm bởi:** `ActivityServiceTest`, `ActivityRepositoryTest`, `ActivityControllerTest`

Hai hoạt động trùng giờ khi chúng có chung ít nhất một phút. Chỉ hoạt động có **đủ cả** giờ bắt đầu và giờ kết thúc mới được đem ra so.

Bảng quyết định, với ngày đã có hoạt động "Ăn sáng" từ 09:00 đến 10:00:

| Hoạt động mới | Có chung phút nào không | Kết quả |
|---|---|---|
| 09:30 đến 10:30 | Có, từ 09:30 đến 10:00 | Từ chối |
| 09:59 đến 10:30 | Có, đúng một phút | Từ chối |
| 10:00 đến 11:00 | Không, chỉ chạm đầu nhau | Chấp nhận |
| 09:30, không có giờ kết thúc | Không xét | Chấp nhận |
| 09:30 đến 10:30, kèm `allowOverlap=true` | Có, nhưng người dùng đã xác nhận | Chấp nhận |

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-ACT-034 | Ngày đã có hoạt động 09:00 đến 10:00. Thêm hoạt động có khoảng giờ bắt đầu bên trong, kết thúc bên trong, nằm trọn bên trong, bao trùm, hoặc trùng khít | 409 trong cả năm trường hợp, không lưu gì | Sai | Đạt · từng lỗi BUG-ACT-002 |
| TC-ACT-035 | Hoạt động mới trùng giờ với nhiều hoạt động trong ngày | 409. Thông báo nêu hoạt động bắt đầu sớm nhất trong số bị trùng | Sai | Đạt · từng lỗi BUG-ACT-002 |
| TC-ACT-036 | Ngày có hoạt động 03:00 đến 04:00. Thêm hoạt động 03:30 đến 05:00, rồi thêm hoạt động 20:00 đến 21:00 | Lần đầu bị từ chối. Lần sau được chấp nhận. Giờ sáng sớm không bị dịch theo múi giờ | Biên | Đạt · từng lỗi BUG-ACT-002 |
| TC-ACT-037 | Thêm hoạt động chạm đầu hoạt động đã có: 10:00 đến 11:00, hoặc 08:00 đến 09:00 | Chấp nhận. Hai hoạt động được phép nối tiếp nhau không cần khoảng nghỉ | Biên | Đạt |
| TC-ACT-038 | Thêm hoạt động chỉ chung đúng một phút: 09:59 đến 10:30, hoặc 08:00 đến 09:01 | 409 | Biên | Đạt |
| TC-ACT-039 | Hoạt động mới không có giờ, hoặc chỉ có giờ bắt đầu | Luôn thêm được, hệ thống không kiểm trùng giờ | Biên | Đạt |
| TC-ACT-040 | Trong ngày có hoạt động chưa xếp giờ, hoạt động chỉ có giờ bắt đầu, và ngày khác có hoạt động cùng giờ | Cả ba đều không chặn hoạt động mới | Đúng | Đạt |
| TC-ACT-041 | Thêm hoạt động trùng giờ, kèm `allowOverlap=true` | 201, hoạt động được lưu | Đúng | Đạt |
| TC-ACT-042 | Gửi giờ có giây: 10:00:30 đến 11:00, trong khi đã có hoạt động 09:00 đến 10:00 | Chấp nhận. Phần giây bị bỏ trước khi kiểm, nên hai hoạt động chỉ chạm đầu nhau | Biên | Đạt |
| TC-ACT-043 | Xem phản hồi khi bị từ chối vì trùng giờ | 409 `ACTIVITY_TIME_CONFLICT`. Chi tiết nằm ở ô giờ bắt đầu: "Trùng giờ với hoạt động "Ăn sáng" (09:00 - 10:00)" | Sai | Đạt |
| TC-ACT-044 | Gửi `allowOverlap` với giá trị không phải `true` hoặc `false` | 400 `VALIDATION_ERROR` | Sai | Đạt |

---

## D. Xem danh sách hoạt động của một ngày

> **Yêu cầu:** design.md 10.2 "Quy ước Activity API", rule 14.5, 6.2 · **Kiểm bởi:** `ActivityControllerTest`, `ActivityServiceTest`, `ActivityRepositoryTest`

Danh sách xếp theo **số thứ tự**, không xếp theo giờ. Người dùng tự quyết định thứ tự bằng cách kéo thả ở Task 2.4.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-ACT-045 | Chủ sở hữu xem hoạt động của một ngày | 200, các hoạt động xếp theo số thứ tự tăng dần. Hoạt động có giờ sớm hơn nhưng số thứ tự lớn hơn vẫn đứng sau | Đúng | Đạt |
| TC-ACT-046 | Ngày khác của cùng chuyến đi cũng có hoạt động | Danh sách chỉ có hoạt động của ngày được hỏi | Đúng | Đạt |
| TC-ACT-047 | Hai hoạt động có cùng số thứ tự | Hoạt động tạo trước đứng trước | Biên | Đạt |
| TC-ACT-048 | Xem một ngày chưa có hoạt động nào | 200 với danh sách rỗng, không phải lỗi | Biên | Đạt |
| TC-ACT-049 | Xem khi chưa đăng nhập | 401 | Bảo mật | Đạt |
| TC-ACT-050 | Người lạ xem hoạt động của chuyến đi người khác | 403 | Bảo mật | Đạt |
| TC-ACT-051 | Người chỉ có quyền xem, không có quyền sửa | Vẫn xem được danh sách | Bảo mật | Đạt |
| TC-ACT-052 | Xem một ngày không thuộc chuyến đi ghi trên đường dẫn | 404 | Bảo mật | Đạt |
| TC-ACT-053 | Xem hoạt động của chuyến đi không tồn tại hoặc đã xoá | 404 | Sai | Đạt |

`TC-ACT-051` chuẩn bị cho Phase 4. Hiện chỉ có chủ sở hữu, nên quyền xem và quyền sửa luôn đi cùng nhau. Test này khoá lại việc endpoint xem dùng đúng quyền xem.

---

## E. Sửa hoạt động

> **Yêu cầu:** design.md 10.2 "Quy ước Activity API", rule 14.4, 6.2 · **Kiểm bởi:** `ActivityControllerTest`, `ActivityServiceTest`, `ActivityRepositoryTest`

Quy ước khi sửa, áp dụng cho từng ô:

| Giá trị gửi lên | Ý nghĩa |
|---|---|
| Không gửi ô đó | Giữ nguyên |
| Gửi giá trị mới | Đổi sang giá trị mới |
| Gửi chuỗi rỗng, chỉ với ghi chú và đường dẫn đặt chỗ | Xoá nội dung ô đó |

Mọi kịch bản bên dưới bắt đầu từ hoạt động "Ăn sáng" 09:00 đến 10:00, có ghi chú, chi phí 50.000 VND và đường dẫn đặt chỗ.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-ACT-054 | Chỉ gửi tên mới, có khoảng trắng thừa | Tên đổi và được cắt khoảng trắng. Mọi ô khác giữ nguyên. Ngày, số thứ tự và người tạo **không bao giờ** đổi ở đây | Đúng | Đạt |
| TC-ACT-055 | Gửi giá trị mới cho mọi ô | Tất cả đều đổi | Đúng | Đạt |
| TC-ACT-056 | Gửi ghi chú và đường dẫn đặt chỗ là chuỗi rỗng | Hai ô đó bị xoá, các ô khác giữ nguyên | Biên | Đạt |
| TC-ACT-057 | Gửi giờ mới có cả giây | Phần giây bị bỏ | Biên | Đạt |
| TC-ACT-058 | Chỉ gửi giờ kết thúc 08:00, sớm hơn giờ bắt đầu đang lưu | 400, lỗi ở ô giờ kết thúc. Hoạt động không đổi | Sai | Đạt |
| TC-ACT-059 | Chỉ gửi giờ bắt đầu 10:30, muộn hơn giờ kết thúc đang lưu | 400, lỗi ở ô giờ kết thúc. Hoạt động không đổi | Sai | Đạt |
| TC-ACT-060 | Hoạt động chưa xếp giờ. Chỉ gửi giờ kết thúc | 400, lỗi ở ô giờ bắt đầu | Sai | Đạt |
| TC-ACT-061 | Hoạt động chưa xếp giờ. Gửi đủ giờ bắt đầu và kết thúc | Lưu được | Đúng | Đạt |
| TC-ACT-062 | Đổi giờ thành 09:30 đến 10:30, trong khi "Cà phê" đang ở 10:00 đến 11:00 | 409, nêu tên "Cà phê". Giờ của hoạt động không đổi | Sai | Đạt |
| TC-ACT-063 | Kéo dài giờ kết thúc của chính hoạt động, không chạm hoạt động nào khác | Lưu được. Hoạt động không bị coi là trùng với giờ cũ của chính nó | Biên | Đạt |
| TC-ACT-064 | Hoạt động đang trùng giờ với hoạt động khác do đã được cho phép trước đó. Chỉ đổi tên, hoặc gửi lại đúng giờ đang lưu | Lưu được. Hệ thống chỉ kiểm trùng khi khoảng giờ **thật sự thay đổi** | Biên | Đạt |
| TC-ACT-065 | Đổi giờ làm trùng hoạt động khác, kèm `allowOverlap=true` | Lưu được | Đúng | Đạt |
| TC-ACT-066 | Hoạt động chưa có chi phí và tiền tệ. Chỉ gửi chi phí | Tiền tệ lấy theo chuyến đi | Đúng | Đạt |
| TC-ACT-067 | Hoạt động đang dùng VND. Chỉ gửi chi phí mới | Chi phí đổi, tiền tệ vẫn là VND | Đúng | Đạt |
| TC-ACT-068 | Gửi nội dung rỗng `{}` | 200, không có gì thay đổi | Biên | Đạt |
| TC-ACT-069 | Gửi form sai nhiều ô: tên chỉ có khoảng trắng, chi phí âm, tiền tệ viết thường, đường dẫn không bắt đầu bằng http | 400, liệt kê đủ bốn ô sai | Sai | Đạt |
| TC-ACT-070 | Sửa khi chưa đăng nhập | 401 | Bảo mật | Đạt |
| TC-ACT-071 | Người lạ sửa hoạt động của chuyến đi người khác | 403 | Bảo mật | Đạt |
| TC-ACT-072 | Sửa hoạt động của chuyến B qua đường dẫn của chuyến A | 404, hoạt động không đổi | Bảo mật | Đạt |
| TC-ACT-073 | Sửa hoạt động của chuyến đi không tồn tại hoặc đã xoá, hoặc mã hoạt động không tồn tại | 404 | Sai | Đạt |
| TC-ACT-074 | Mã hoạt động trên đường dẫn là chữ | 400 `VALIDATION_ERROR` | Sai | Đạt |

Giới hạn đã biết, thuộc thiết kế chứ không phải lỗi: giờ, chi phí và tiền tệ đã đặt thì **chưa xoá trắng được**. Người dùng chỉ đổi được sang giá trị khác.

---

## F. Xoá hoạt động

> **Yêu cầu:** design.md 5.2 bảng `activities`, 10.2 "Quy ước Activity API", 6.2 · **Kiểm bởi:** `ActivityControllerTest`, `ActivityServiceTest`, `ActivityRepositoryTest`

Hoạt động bị **xoá hẳn**, khác với chuyến đi là xoá mềm. Hoạt động đã xoá không khôi phục được.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-ACT-075 | Chủ sở hữu xoá một hoạt động | 200, phản hồi không có dữ liệu. Hoạt động bị xoá | Đúng | Đạt |
| TC-ACT-076 | Xoá khi chưa đăng nhập | 401 | Bảo mật | Đạt |
| TC-ACT-077 | Người lạ xoá hoạt động của chuyến đi người khác | 403, hoạt động vẫn còn | Bảo mật | Đạt |
| TC-ACT-078 | Người chỉ có quyền xem, không có quyền sửa | 403. Xoá cần quyền sửa | Bảo mật | Đạt |
| TC-ACT-079 | Xoá hoạt động của chuyến B qua đường dẫn của chuyến A | 404, hoạt động vẫn còn | Bảo mật | Đạt |
| TC-ACT-080 | Xoá hoạt động của chuyến đi không tồn tại hoặc đã xoá | 404 | Sai | Đạt |
| TC-ACT-081 | Mã hoạt động trên đường dẫn là chữ | 400 `VALIDATION_ERROR` | Sai | Đạt |

---

## G. Chặn đổi ngày làm mất hoạt động

> **Yêu cầu:** design.md rule 14.3, 10.2 "Quy ước Trip API", 10.3 · **Kiểm bởi:** `TripDayReconcileTest`, `ActivityRepositoryTest`, `TripServiceTest`, `TripControllerTest`

Rút ngắn chuyến đi làm một số ngày bị cắt. Ngày bị cắt thì hoạt động của nó cũng bị xoá theo. Hệ thống không được làm việc đó một cách âm thầm.

Bảng quyết định:

| Thay đổi ngày của chuyến đi | Ngày bị cắt có hoạt động | Kèm `force=true` | Kết quả |
|---|---|---|---|
| Rút ngắn | Có | Không | Từ chối 409, không lưu gì |
| Rút ngắn | Có | Có | Cắt ngày, xoá hoạt động của ngày đó |
| Rút ngắn | Không | Không | Cắt ngày như bình thường |
| Dời nguyên khối | Không có ngày nào bị cắt | Không | Dời, hoạt động đi theo ngày |
| Kéo dài | Không có ngày nào bị cắt | Không | Thêm ngày trống |

Mọi kịch bản bên dưới bắt đầu từ một chuyến đi 01/10 đến 03/10 với ba ngày A, B, C.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-ACT-082 | Ngày C có 2 hoạt động. Rút chuyến đi còn 01/10 đến 02/10 | 409. Cả ba ngày và mọi hoạt động vẫn còn | Sai | Đạt |
| TC-ACT-083 | Ngày A có 1 hoạt động. Rút chuyến đi còn 02/10 đến 03/10 | 409. Cắt ở đầu bị chặn giống cắt ở cuối | Sai | Đạt |
| TC-ACT-084 | Yêu cầu bị chặn có kèm cả tên mới của chuyến đi | Tên mới **cũng không được lưu**. Một yêu cầu bị từ chối thì không để lại thay đổi nào | Sai | Đạt |
| TC-ACT-085 | Như `TC-ACT-082`, kèm `force=true` | Ngày C và 2 hoạt động của nó bị xoá. Hoạt động của ngày A giữ nguyên | Đúng | Đạt |
| TC-ACT-086 | Ngày A và B có hoạt động, ngày C trống. Rút chuyến đi còn 01/10 đến 02/10 | Được phép mà không cần `force`, vì ngày bị cắt không có gì để mất | Biên | Đạt |
| TC-ACT-087 | Chuyến đi có hoạt động. Dời nguyên khối sang 08/10 đến 10/10 | Không bị chặn. Hoạt động của ngày A giờ nằm ở 08/10, của ngày C ở 10/10 | Đúng | Đạt |
| TC-ACT-088 | Chuyến đi có hoạt động. Kéo dài cả hai đầu | Không bị chặn, hoạt động giữ nguyên ngày | Đúng | Đạt |
| TC-ACT-089 | Cả ba ngày đều có hoạt động. Đổi thành 08/10 đến 12/10, tức vừa dời vừa đổi số ngày | 409, vì không ngày cũ nào còn nằm trong khoảng mới | Biên | Đạt |
| TC-ACT-090 | Đếm những gì sẽ mất khi rút ngắn, trong khi chuyến đi khác cũng có hoạt động cùng ngày | Chỉ đếm hoạt động của đúng chuyến đi đang sửa, và chỉ ở những ngày nằm ngoài khoảng mới | Đúng | Đạt |
| TC-ACT-091 | Xem phản hồi khi bị chặn | 409 `TRIP_DAY_HAS_ACTIVITIES`. Chi tiết: "2 hoạt động trong 1 ngày sẽ bị xoá nếu đổi ngày" | Sai | Đạt |
| TC-ACT-092 | Gửi `force` với giá trị không phải `true` hoặc `false` | 400 `VALIDATION_ERROR` | Sai | Đạt |

**Kiểm chứng ngược.** Bước chặn trong code đã được tạm gỡ, rồi test được chạy lại. Ba kịch bản `TC-ACT-082`, `TC-ACT-083` và `TC-ACT-089` chuyển sang lỗi, sau đó code được khôi phục. Điều này chứng minh các test thật sự bảo vệ quy tắc, chứ không đạt một cách tình cờ.

`TC-ACT-084` được kiểm ở tầng service. Bản kiểm đi qua mọi tầng trên database thật là `TC-ACT-109`.

---

## H. Chi tiết chuyến đi kèm hoạt động

> **Yêu cầu:** design.md 10.2 "Quy ước Trip API", CLAUDE.md mục 8 · **Kiểm bởi:** `TripDayServiceTest`, `TripServiceTest`, `TripControllerTest`, `ActivityRepositoryTest`, `TripDayFlowIntegrationTest`

Màn hình chính của ứng dụng cần cả chuyến đi, các ngày và các hoạt động. Một lần gọi chi tiết chuyến đi trả về tất cả, để trang web không phải gọi riêng cho từng ngày.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-ACT-093 | Xem chi tiết chuyến đi có hoạt động ở nhiều ngày | Mỗi ngày kèm đúng các hoạt động của nó. Hoạt động của ngày này không lẫn sang ngày khác | Đúng | Đạt |
| TC-ACT-094 | Xem thứ tự hoạt động trong từng ngày | Xếp theo số thứ tự tăng dần, giống danh sách hoạt động của ngày | Đúng | Đạt |
| TC-ACT-095 | Chuyến đi có ngày chưa có hoạt động | Ngày đó vẫn xuất hiện, với danh sách hoạt động rỗng chứ không bị thiếu | Biên | Đạt |
| TC-ACT-096 | Chuyến đi chưa có hoạt động nào | Mọi ngày đều có danh sách rỗng | Biên | Đạt |
| TC-ACT-097 | Hệ thống có chuyến đi khác cũng có hoạt động | Chi tiết chỉ chứa hoạt động của đúng chuyến đi được hỏi | Bảo mật | Đạt |
| TC-ACT-098 | Xem chi tiết chuyến đi 2 ngày không có hoạt động, rồi chuyến đi 60 ngày có 30 hoạt động | Cả hai lần đều tốn đúng **4 câu truy vấn**. Thêm ngày hay thêm hoạt động không làm tăng số câu truy vấn | Biên | Đạt |
| TC-ACT-099 | Xem chi tiết chuyến đi không tồn tại | 404, hệ thống không tải ngày hay hoạt động | Sai | Đạt |
| TC-ACT-100 | Xem danh sách ngày, hoặc sửa tiêu đề ngày | Phản hồi vẫn gọn như cũ, **không** kèm hoạt động | Đúng | Đạt |

`TC-ACT-098` thay cho `TC-DAY-028`. Trước Mốc 8 con số là 3 câu truy vấn. Câu thứ tư là câu lấy hoạt động của cả chuyến đi.

---

## I. Kiểm toàn luồng qua mọi tầng

> **Yêu cầu:** design.md mục 16, rule 14.3, 14.4 · **Kiểm bởi:** `ActivityFlowIntegrationTest`

Các phần B đến H kiểm từng tầng riêng lẻ, phần lớn bằng thành phần giả lập. Phần này đi qua **mọi tầng cùng lúc** trên database thật: đăng nhập bằng token, kiểm quyền, xử lý nghiệp vụ, ghi xuống MySQL. Đây là nơi duy nhất chứng minh được bốn điều:

- Giờ được lưu và so sánh đúng như đã nhập, dù máy chạy múi giờ nào.
- Một yêu cầu bị từ chối thật sự không để lại thay đổi nào.
- Cắt ngày chỉ xoá hoạt động khi người dùng đã xác nhận.
- Số câu truy vấn không tăng theo số hoạt động.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-ACT-101 | Chủ sở hữu thêm ba hoạt động, xem danh sách, sửa, xem chi tiết chuyến đi, xoá, rồi thêm tiếp | Mọi bước trả đúng. Số phiên bản tăng khi sửa. Hoạt động thêm sau khi xoá vẫn nằm cuối ngày | Đúng | Đạt |
| TC-ACT-102 | Thêm hoạt động 03:00 đến 08:15 qua API, rồi đọc thẳng giá trị trong database | Database giữ đúng 03:00 và 08:15, cả sau khi sửa giờ kết thúc | Biên | Đạt |
| TC-ACT-103 | Đi qua mọi tầng với quy tắc trùng giờ: thêm, thêm có xác nhận, sửa giờ, đổi tên hoạt động đang trùng | Từng bước trả đúng mã. Yêu cầu bị từ chối không làm đổi dữ liệu | Sai | Đạt · từng lỗi BUG-ACT-003 |
| TC-ACT-104 | Hoạt động "Đi chợ" đang trùng với "Cà phê" do "Cà phê" được thêm có xác nhận. Kéo dài giờ của "Đi chợ" | 409. Khoảng giờ đã đổi thì được kiểm lại, kể cả với hoạt động mà nó vốn đang trùng | Biên | Đạt |
| TC-ACT-105 | Gửi giờ sai quy tắc khi thêm và khi sửa | 400 với thông báo tiếng Việt ở đúng ô. Không có gì được lưu, số phiên bản không đổi | Sai | Đạt |
| TC-ACT-106 | Người lạ và khách chưa đăng nhập thử xem, thêm, sửa, xoá | 403 cho người lạ, 401 cho khách. Dữ liệu không đổi | Bảo mật | Đạt |
| TC-ACT-107 | Chủ sở hữu của hai chuyến đi dùng mã ngày hoặc mã hoạt động của chuyến này trên đường dẫn của chuyến kia | 404 ở cả bốn thao tác. Dữ liệu không đổi | Bảo mật | Đạt |
| TC-ACT-108 | Xoá mềm chuyến đi rồi thử xem, thêm, sửa, xoá hoạt động của nó | 404 ở cả bốn thao tác. Hoạt động vẫn còn trong database, chờ khi chuyến đi được khôi phục | Sai | Đạt |
| TC-ACT-109 | Gửi một yêu cầu vừa đổi tên chuyến đi vừa rút ngắn làm mất 2 hoạt động | 409. Tên, ngày và số phiên bản của chuyến đi giữ nguyên. Mọi ngày và hoạt động vẫn còn | Sai | Đạt |
| TC-ACT-110 | Thêm rồi cắt một ngày trống, dời nguyên khối, rồi rút ngắn kèm `force=true` | Ba bước đầu không bị chặn và hoạt động đi theo ngày. Bước cuối xoá đúng ngày bị cắt và hoạt động của nó | Đúng | Đạt |
| TC-ACT-111 | Xem danh sách của một ngày trống, rồi của một ngày có 20 hoạt động | Cả hai lần đều tốn đúng 4 câu truy vấn | Biên | Đạt |

`TC-ACT-109` là bản kiểm trên database thật của `TC-ACT-084`.

---

## Kiểm tra thủ công

Làm trên trang Swagger `http://localhost:8080/swagger-ui.html`. Cần có: backend đang chạy bản code mới nhất, đã đăng nhập bằng một tài khoản đã xác thực email, và một chuyến đi từ `2026-10-01` đến `2026-10-02`.

Lấy mã ngày bằng cách gọi danh sách ngày của chuyến đi. Các bước bên dưới dùng Ngày 1.

### MT-ACT-01 · Thêm hoạt động

- [ ] Thêm hoạt động với nội dung bên dưới. Trả 201, tên hiển thị đúng dấu tiếng Việt, giờ là `11:30` và `13:00`, số thứ tự `1000`, tiền tệ trùng với chuyến đi.
- [ ] Thêm hoạt động thứ hai chỉ với `{ "title": "Dạo hồ Xuân Hương" }`. Trả 201, loại là `OTHER`, số thứ tự `2000`.
- [ ] Chạy câu lệnh SQL bên dưới trong MySQL. Có hai dòng, cột giờ của dòng đầu là `11:30:00` và `13:00:00`.

```json
{
  "title": "Ăn trưa lẩu gà lá é",
  "type": "FOOD",
  "startTime": "11:30",
  "endTime": "13:00",
  "note": "Đặt bàn trước",
  "costAmount": 350000
}
```

```sql
SELECT id, title, start_time, end_time, order_index, currency, created_by FROM activities ORDER BY id DESC LIMIT 5;
```

**Kết quả:** Chưa chạy · **Ngày:** · **Ghi chú:**

### MT-ACT-02 · Quy tắc về giờ

- [ ] Thêm hoạt động có `"startTime": "10:00"` và `"endTime": "09:00"`. Trả 400, thông báo "Giờ kết thúc phải sau giờ bắt đầu".
- [ ] Thêm hoạt động chỉ có `"endTime": "09:00"`. Trả 400, lỗi ở ô `startTime`.
- [ ] Thêm hoạt động có `"startTime": "03:00"` và `"endTime": "08:15"`. Trả 201. Chạy lại câu lệnh SQL ở `MT-ACT-01`, cột giờ là `03:00:00` và `08:15:00`.

Bước cuối kiểm lại lỗi `BUG-ACT-001` trên database thật của máy bạn.

**Kết quả:** Chưa chạy · **Ngày:** · **Ghi chú:**

### MT-ACT-03 · Người lạ và đường dẫn sai

Cần có: tài khoản thứ hai đã xác thực email, và một chuyến đi thứ hai của tài khoản thứ nhất.

- [ ] Đăng nhập bằng tài khoản thứ hai, thêm hoạt động vào chuyến đi của tài khoản thứ nhất. Trả 403.
- [ ] Đăng nhập lại bằng tài khoản thứ nhất. Dùng mã của chuyến đi thứ nhất kèm mã ngày của chuyến đi thứ hai. Trả 404.

**Kết quả:** Chưa chạy · **Ngày:** · **Ghi chú:**

### MT-ACT-04 · Trùng giờ

Cần có: một ngày **chưa có hoạt động nào**. Dùng Ngày 2 của chuyến đi nếu Ngày 1 đã dùng ở các test trên.

- [ ] Thêm "Ăn sáng" từ `09:00` đến `10:00`. Trả 201.
- [ ] Thêm "Cà phê" từ `09:30` đến `10:30`. Trả 409 `ACTIVITY_TIME_CONFLICT`, chi tiết nêu tên "Ăn sáng" và giờ 09:00 - 10:00.
- [ ] Thêm lại "Cà phê" như trên, điền `true` vào ô tham số `allowOverlap`. Trả 201.
- [ ] Thêm "Đi chợ" từ `10:30` đến `11:30`. Trả 201, vì chỉ chạm đầu "Cà phê".
- [ ] Thêm "Săn mây" từ `03:00` đến `04:00`, rồi thêm "Ngắm bình minh" từ `03:30` đến `05:00`. Lần đầu trả 201, lần sau trả 409 nêu tên "Săn mây".

Bước cuối kiểm lại lỗi `BUG-ACT-002` trên máy của bạn.

**Kết quả:** Chưa chạy · **Ngày:** · **Ghi chú:**

### MT-ACT-05 · Xem danh sách hoạt động

Cần có: một ngày đã có ít nhất ba hoạt động từ các test trước, và một ngày chưa có hoạt động nào.

- [ ] Gọi danh sách hoạt động của ngày đã có hoạt động. Trả 200, số thứ tự tăng dần 1000, 2000, 3000.
- [ ] So với thứ tự bạn đã thêm. Danh sách theo thứ tự thêm vào, không theo giờ.
- [ ] Gọi danh sách của ngày chưa có hoạt động. Trả 200 với `"data": []`.
- [ ] Đăng nhập bằng tài khoản thứ hai rồi gọi danh sách của chuyến đi này. Trả 403.

**Kết quả:** Chưa chạy · **Ngày:** · **Ghi chú:**

### MT-ACT-06 · Sửa hoạt động

Cần có: một ngày có "Ăn sáng" từ `09:00` đến `10:00` kèm ghi chú, và "Cà phê" từ `10:00` đến `11:00`. Ghi lại số phiên bản của "Ăn sáng" trước khi bắt đầu.

- [ ] Sửa "Ăn sáng", chỉ gửi `{ "title": "Ăn sáng muộn" }`. Trả 200, giờ và ghi chú giữ nguyên, số phiên bản tăng 1.
- [ ] Gửi `{ "note": "" }`. Trả 200, ghi chú trở về `null`.
- [ ] Gửi `{ "endTime": "10:30" }`. Trả 409, nêu tên "Cà phê". Gọi danh sách hoạt động, giờ kết thúc vẫn là `10:00`.
- [ ] Gửi lại như trên, điền `true` vào ô tham số `allowOverlap`. Trả 200, giờ kết thúc là `10:30`.
- [ ] Gửi `{ "title": "Ăn sáng" }`, không kèm `allowOverlap`. Trả 200, dù hoạt động đang trùng giờ với "Cà phê".
- [ ] Gửi `{ "endTime": "08:00" }`. Trả 400, thông báo "Giờ kết thúc phải sau giờ bắt đầu".

**Kết quả:** Chưa chạy · **Ngày:** · **Ghi chú:**

### MT-ACT-07 · Xoá hoạt động

Cần có: một ngày có ba hoạt động với số thứ tự 1000, 2000, 3000. Ghi lại mã của hoạt động ở giữa.

- [ ] Xoá hoạt động ở giữa. Trả 200 với `"data": null`.
- [ ] Gọi danh sách hoạt động của ngày. Còn hai hoạt động, số thứ tự là 1000 và 3000, không bị đánh lại.
- [ ] Xoá lại đúng hoạt động đó. Trả 404.
- [ ] Chạy câu lệnh SQL bên dưới với mã đã ghi. Kết quả là 0 dòng, vì hoạt động bị xoá hẳn.
- [ ] Thêm một hoạt động mới vào ngày đó. Số thứ tự là 4000.

```sql
SELECT id, title FROM activities WHERE id = <mã đã ghi>;
```

**Kết quả:** Chưa chạy · **Ngày:** · **Ghi chú:**

### MT-ACT-08 · Rút ngắn chuyến đi đang có hoạt động

Cần có: một chuyến đi **mới** từ `2026-10-01` đến `2026-10-03`. Thêm một hoạt động vào Ngày 1 và hai hoạt động vào Ngày 3. Ghi lại tên và số phiên bản của chuyến đi.

- [ ] Sửa chuyến đi, gửi `{ "title": "Tên thử", "endDate": "2026-10-02" }`. Trả 409 `TRIP_DAY_HAS_ACTIVITIES`, chi tiết ghi "2 hoạt động trong 1 ngày sẽ bị xoá nếu đổi ngày".
- [ ] Xem chi tiết chuyến đi. Tên, ngày kết thúc và số phiên bản đều như cũ. Vẫn còn 3 ngày.
- [ ] Gọi danh sách hoạt động của Ngày 3. Vẫn còn đủ hai hoạt động.
- [ ] Gửi lại đúng nội dung đó, điền `true` vào ô tham số `force`. Trả 200, tên đổi, ngày kết thúc là `2026-10-02`.
- [ ] Gọi danh sách ngày. Còn 2 ngày. Gọi danh sách hoạt động của Ngày 1, hoạt động vẫn còn.
- [ ] Chạy câu lệnh SQL bên dưới. Chỉ còn một dòng, là hoạt động của Ngày 1.

```sql
SELECT a.id, a.title, d.date FROM activities a JOIN trip_days d ON d.id = a.trip_day_id ORDER BY a.id DESC LIMIT 5;
```

**Kết quả:** Chưa chạy · **Ngày:** · **Ghi chú:**

### MT-ACT-09 · Chi tiết chuyến đi kèm hoạt động

Cần có: một chuyến đi 3 ngày. Ngày 1 có hai hoạt động, Ngày 2 có một hoạt động, Ngày 3 chưa có gì.

- [ ] Gọi chi tiết chuyến đi. Phần `days` có 3 ngày, mỗi ngày có ô `activities`.
- [ ] Xem Ngày 1. Có hai hoạt động, số thứ tự 1000 rồi 2000.
- [ ] Xem Ngày 3. Ô `activities` là `[]`, không phải `null` và không bị thiếu.
- [ ] Gọi danh sách ngày của chuyến đi. Các ngày **không** có ô `activities`.

**Kết quả:** Chưa chạy · **Ngày:** · **Ghi chú:**

---

## Lỗi đã phát hiện

Mốc 1 phát sinh `BUG-ACT-001`, Mốc 3 phát sinh `BUG-ACT-002`, Mốc 9 phát sinh `BUG-ACT-003`. Các mốc còn lại không phát sinh lỗi: test mới đều đạt ngay lần chạy đầu.

| Mã lỗi | Test case | Ngày | Hiện tượng | Nguyên nhân | Cách sửa | Trạng thái |
|---|---|---|---|---|---|---|
| BUG-ACT-003 | TC-ACT-103 | 2026-09-29 | Kéo dài giờ kết thúc của "Đi chợ" từ 11:00 lên 11:30 thì bị từ chối. Test báo: mong đợi 200, thực tế 409 | **Test sai.** Hệ thống làm đúng: khoảng mới 10:00 đến 11:30 trùng với "Cà phê" 09:30 đến 10:30. Người viết test quên rằng "Cà phê" đã được thêm có xác nhận ở bước trước | Sửa kịch bản trong test: bước này mong đợi 409, và việc "không tự trùng với chính mình" được kiểm bằng một hoạt động không trùng ai. Code không đổi | Đã sửa, commit `50bb13c` |
| BUG-ACT-002 | TC-ACT-034, TC-ACT-035, TC-ACT-036 | 2026-09-29 | Câu truy vấn tìm hoạt động trùng giờ trả về danh sách rỗng, dù trong ngày có hoạt động trùng giờ. Test báo: mong đợi `["Ăn sáng"]`, thực tế `[]` | Cách sửa của BUG-ACT-001 chỉ áp dụng cho giờ **được lưu**. Giờ **đem ra so sánh** trong câu truy vấn vẫn bị đổi sang giờ quốc tế, nên 09:30 bị so như 02:30 | Không so giờ trong câu truy vấn nữa. Hệ thống lấy các hoạt động có giờ của ngày, rồi so trong code | Đã sửa, commit `0abaf97` |
| BUG-ACT-001 | TC-ACT-003 | 2026-09-29 | Hoạt động 03:00 đến 08:15 bị lưu thành 20:00 đến 01:15. Giờ kết thúc nằm trước giờ bắt đầu nên database từ chối | Ứng dụng đổi mọi giá trị thời gian sang giờ quốc tế trước khi lưu. Điều đó đúng với một thời điểm cụ thể, nhưng sai với giờ trong ngày không kèm ngày | Hai cột giờ của hoạt động được ghi nguyên văn, không chuyển đổi | Đã sửa, commit `3c4c294` |

BUG-ACT-001 nguy hiểm vì nó ẩn trong sử dụng hằng ngày. Với giờ từ 07:00 trở đi, giá trị bị lệch vẫn giữ đúng thứ tự trước sau, nên hệ thống vẫn chạy. Lỗi chỉ lộ ra với hoạt động trước 07:00 sáng. Ngoài ra mọi phép so trùng giờ ở Mốc 3 sẽ so trên giá trị sai.

Bài học: giá trị ứng dụng đọc lại đúng vẫn có thể sai trong database. Phải kiểm trực tiếp giá trị đã lưu.

BUG-ACT-002 là hệ quả của BUG-ACT-001: sửa một lỗi ở chỗ ghi dữ liệu không có nghĩa là chỗ đọc và chỗ so sánh cũng đã đúng. Nếu không có test chạy trên database thật, lỗi này sẽ cho phép mọi hoạt động trùng giờ lọt qua mà không ai biết, vì hệ thống không báo lỗi gì cả.

Thứ tự xử lý BUG-ACT-002 đúng theo quy tắc "ghi trước, sửa sau": test đỏ, ghi lỗi với trạng thái đang mở, tìm nguyên nhân, sửa, chạy lại, đóng lỗi.

BUG-ACT-003 cho thấy vì sao bước "xác định ai sai" quan trọng. Nếu sửa code cho test đạt, hệ thống sẽ cho phép kéo dài một hoạt động đè lên hoạt động khác mà không hỏi lại người dùng. Log của server ghi rõ hoạt động nào bị trùng, và đó là căn cứ để kết luận test sai. Lỗi này cũng làm rõ một quy tắc chưa được viết ra, nay đã thành `TC-ACT-104`.

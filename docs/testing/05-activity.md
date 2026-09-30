# 05 · Hoạt động trong ngày

> Cập nhật: 2026-09-30 · build xanh tại commit `eb1ad5e` (merge Task 2.5) · [Về trang chính](README.md)

Hoạt động là một việc cần làm trong một ngày của chuyến đi, ví dụ "Ăn trưa" từ 11:30 đến 13:00. Thêm, xem, sửa, xoá làm ở Task 2.3, hoàn thành ngày 2026-09-29. Sắp xếp lại bằng kéo thả làm ở Task 2.4, hoàn thành ngày 2026-09-30.

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

Task 2.4, sắp xếp lại:

| Mốc | Nội dung | Phần trong file | Commit |
|---|---|---|---|
| 1 | Sắp xếp lại theo lô | J | `819651a` |
| 2 | Chặn trùng giờ khi chuyển ngày | K | `bdfe725` |
| 3 | Đánh lại số thứ tự khi khoảng cách quá nhỏ | L | `e56f21a` |
| 4 | Kiểm toàn luồng | M | `1b30b1f` |

Vài từ dùng trong file:

| Từ | Nghĩa |
|---|---|
| Giờ trong ngày | Giờ không kèm ngày, ví dụ 09:00. Ngày lịch lấy theo ngày của chuyến đi |
| Số phiên bản | Số đếm tăng 1 sau mỗi lần sửa, dùng để phát hiện hai người sửa cùng lúc |
| Số thứ tự | Vị trí của hoạt động trong ngày. Các số cách nhau 1000 để sau này chèn vào giữa được |
| Lô | Nhiều lần di chuyển gửi trong một lần gọi. Cả lô được áp dụng cùng lúc, hoặc không gì cả |

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
| TC-ACT-156 | Thêm hoạt động có ghi chú đúng 255 chữ có dấu tiếng Việt | 201. Giới hạn tính theo số ký tự (giới hạn 255 chốt ở Task 2.5, trước đó là 5000) | Biên | Đạt |
| TC-ACT-157 | Thêm hoạt động có ghi chú 256 ký tự | 400, lỗi ở ô ghi chú: "Ghi chú của hoạt động không được vượt quá 255 ký tự" | Biên | Đạt |

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
| TC-ACT-158 | Sửa ghi chú thành 256 ký tự | 400, lỗi ở ô ghi chú. Không sửa gì | Biên | Đạt |

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

## J. Sắp xếp lại hoạt động

> **Yêu cầu:** design.md 10.2 "Quy ước Reorder", 11.3, rule 14.5 · **Kiểm bởi:** `ActivityServiceTest`, `ActivityControllerTest`, `ActivityRepositoryTest`, `TripDayRepositoryTest`, `ActivityMappingTest`

Người dùng kéo thả hoạt động. Trang web gửi lên **những hoạt động bị di chuyển**, mỗi hoạt động kèm ngày đích và số thứ tự mới.

Ví dụ kéo "Chợ đêm" vào giữa hai hoạt động của một ngày:

| Hoạt động | Trước | Sau |
|---|:--:|:--:|
| Ăn sáng | 1000 | 1000 |
| Chợ đêm | 3000 | 1500 |
| Tham quan | 2000 | 2000 |

Mọi kịch bản bên dưới bắt đầu từ: Ngày 1 có "Ăn sáng" 1000 và "Tham quan" 2000, Ngày 2 có "Chợ đêm" 1000.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-ACT-112 | Kéo "Tham quan" lên trên "Ăn sáng" trong cùng ngày | 200. Chỉ "Tham quan" đổi số thứ tự. Phản hồi là Ngày 1 với thứ tự mới | Đúng | Đạt |
| TC-ACT-113 | Kéo "Chợ đêm" từ Ngày 2 sang giữa hai hoạt động của Ngày 1 | "Chợ đêm" thuộc Ngày 1. Phản hồi có cả hai ngày, xếp theo ngày lịch | Đúng | Đạt |
| TC-ACT-114 | Ngày 2 mất hoạt động duy nhất của nó | Ngày 2 vẫn có trong phản hồi, với danh sách rỗng, để trang web vẽ lại | Biên | Đạt |
| TC-ACT-115 | Một lần gọi di chuyển cả ba hoạt động, có cái đổi ngày, có cái không | Cả ba được áp dụng cùng lúc | Đúng | Đạt |
| TC-ACT-116 | Xem nội dung hoạt động sau khi kéo thả | Tên và người tạo không đổi. Kéo thả chỉ đổi vị trí | Đúng | Đạt |
| TC-ACT-117 | Xem số phiên bản sau khi kéo thả, rồi sau khi sửa tên | Kéo thả **không** tăng số phiên bản. Sửa tên sau đó vẫn tăng | Biên | Đạt |
| TC-ACT-118 | Một hoạt động xuất hiện hai lần trong danh sách | 400 ở ô `items`: "Hoạt động 21 xuất hiện nhiều lần trong danh sách". Không hoạt động nào di chuyển | Sai | Đạt |
| TC-ACT-119 | Danh sách có một dòng hợp lệ và một hoạt động không thuộc chuyến đi | 404 cho cả lô. Dòng hợp lệ **cũng không** được áp dụng | Bảo mật | Đạt |
| TC-ACT-120 | Ngày đích của một dòng không thuộc chuyến đi | 404 cho cả lô, không hoạt động nào di chuyển | Bảo mật | Đạt |
| TC-ACT-121 | Sắp xếp trong chuyến đi không tồn tại hoặc đã xoá | 404, hệ thống không tải hoạt động hay ngày | Sai | Đạt |
| TC-ACT-122 | Gửi danh sách rỗng, hoặc không gửi danh sách | 400 ở ô `items` | Sai | Đạt |
| TC-ACT-123 | Dòng thiếu mã hoạt động, dòng thiếu mã ngày, số thứ tự bằng 0, số thứ tự vượt 1 tỷ | 400. Chi tiết nêu rõ dòng nào và ô nào, ví dụ `items[0].orderIndex` | Sai | Đạt |
| TC-ACT-124 | Số thứ tự bằng 1, và bằng đúng 1 tỷ | Chấp nhận | Biên | Đạt |
| TC-ACT-125 | Gửi đúng 200 dòng, rồi gửi 201 dòng | 200 dòng được chấp nhận. 201 dòng bị từ chối 400 | Biên | Đạt |
| TC-ACT-126 | Gửi một mảng trần thay vì đối tượng có ô `items` | 400 | Sai | Đạt |
| TC-ACT-127 | Sắp xếp khi chưa đăng nhập | 401 | Bảo mật | Đạt |
| TC-ACT-128 | Người lạ sắp xếp hoạt động của chuyến đi người khác | 403 | Bảo mật | Đạt |
| TC-ACT-129 | Tra hoạt động và ngày theo danh sách mã, trong đó có mã của chuyến đi khác và mã không tồn tại | Chỉ trả về hoạt động và ngày của đúng chuyến đi. Ngày xếp theo ngày lịch | Bảo mật | Đạt |

**Kiểm chứng ngược cho `TC-ACT-117`.** Phần khai báo loại vị trí khỏi số phiên bản đã được tạm gỡ, rồi test được chạy lại. Hai test chuyển sang lỗi, sau đó code được khôi phục.

Ba giới hạn đã biết ở mốc này, sẽ được xử lý ở các mốc sau của Task 2.4:

- Chuyển hoạt động sang ngày khác chưa kiểm trùng giờ. Đã xử lý ở Mốc 2, xem phần K.
- Khoảng cách giữa các số thứ tự chưa được đo lại. Đã xử lý ở Mốc 3, xem phần L.
- Hai người kéo thả cùng lúc thì người ghi sau thắng. Thuộc Task 5.3.

---

## K. Chặn trùng giờ khi chuyển hoạt động sang ngày khác

> **Yêu cầu:** design.md rule 14.4, 10.2 "Quy ước Reorder" · **Kiểm bởi:** `ActivityServiceTest`, `ActivityControllerTest`, `ActivityRepositoryTest`

Hoạt động mang theo giờ của nó khi đổi ngày. Hệ thống so nó với **những gì ngày đích sẽ chứa sau khi cả lô được áp dụng**.

Bảng quyết định, với Ngày 1 đang có "Ăn trưa" từ 11:30 đến 13:00:

| Thao tác | Được so với "Ăn trưa" không | Kết quả |
|---|---|---|
| Chuyển "Cà phê" 12:00 đến 12:30 từ Ngày 2 sang Ngày 1 | Có | Từ chối 409 |
| Chuyển "Cà phê" 13:00 đến 13:30 sang Ngày 1 | Có, nhưng chỉ chạm đầu | Chấp nhận |
| Như dòng đầu, kèm `allowOverlap=true` | Không kiểm | Chấp nhận |
| Đổi vị trí "Cà phê" trong chính Ngày 2 | Không kiểm, vì giờ không đổi | Chấp nhận |
| Chuyển "Cà phê" sang Ngày 1 và "Ăn trưa" sang Ngày 2 trong cùng một lần gọi | Không, vì "Ăn trưa" rời Ngày 1 | Chấp nhận |

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-ACT-130 | Lô có hai dòng. Dòng thứ hai chuyển "Cà phê" sang ngày đang có hoạt động trùng giờ | 409. Chi tiết trỏ đúng dòng thứ hai. **Cả hai dòng** đều không được áp dụng | Sai | Đạt |
| TC-ACT-131 | Chuyển hoạt động sang ngày mà nó chỉ chạm đầu hoạt động khác | Chấp nhận | Biên | Đạt |
| TC-ACT-132 | Chuyển hoạt động trùng giờ, kèm `allowOverlap=true` | Chấp nhận | Đúng | Đạt |
| TC-ACT-133 | Hai hoạt động của một ngày đang trùng giờ nhau. Đổi vị trí của chúng trong ngày | Chấp nhận, hệ thống không kiểm trùng giờ | Biên | Đạt |
| TC-ACT-134 | Chuyển hoạt động chỉ có giờ bắt đầu, hoặc không có giờ | Chấp nhận, hệ thống không kiểm trùng giờ | Biên | Đạt |
| TC-ACT-135 | Hai hoạt động trùng giờ nhau cùng được chuyển tới một ngày đang trống giờ đó | 409. Chi tiết báo **cả hai dòng**, mỗi dòng nêu hoạt động kia | Sai | Đạt |
| TC-ACT-136 | Hai hoạt động trùng giờ đổi ngày cho nhau trong một lần gọi | Chấp nhận. Hoạt động rời khỏi ngày thì không còn được tính ở ngày đó | Biên | Đạt |
| TC-ACT-137 | Lô vừa đổi vị trí "Ăn trưa" trong Ngày 1, vừa chuyển "Cà phê" trùng giờ tới Ngày 1 | 409. Hoạt động ở lại ngày vẫn chặn hoạt động chuyển tới | Sai | Đạt |
| TC-ACT-138 | Lấy các hoạt động có giờ của nhiều ngày, trong đó có hoạt động 03:00 đến 04:00 | Chỉ trả hoạt động có đủ hai giờ của đúng các ngày được hỏi. Giờ sáng sớm đọc ra đúng 03:00 | Đúng | Đạt |
| TC-ACT-139 | Xem phản hồi khi bị từ chối | 409 `ACTIVITY_TIME_CONFLICT`. Chi tiết: "Hoạt động "Cà phê" trùng giờ với hoạt động "Ăn trưa" (11:30 - 13:00) trong ngày mới" | Sai | Đạt |
| TC-ACT-140 | Gửi `allowOverlap` với giá trị không phải `true` hoặc `false` | 400 `VALIDATION_ERROR` | Sai | Đạt |

**Kiểm chứng ngược.** Bước kiểm trùng giờ đã được tạm tắt, rồi test được chạy lại. Ba kịch bản `TC-ACT-130`, `TC-ACT-135` và `TC-ACT-137` chuyển sang lỗi, sau đó code được khôi phục.

---

## L. Đánh lại số thứ tự khi khoảng cách quá nhỏ

> **Yêu cầu:** design.md rule 14.5, 10.2 "Quy ước Reorder" · **Kiểm bởi:** `ActivityServiceTest`

Mỗi lần kéo vào giữa hai hoạt động, khoảng cách bị chia đôi: 1500, 1250, 1125, 1062, 1031, 1015, 1007. Tới lúc nào đó sẽ hết chỗ. Vì vậy sau mỗi lần sắp xếp, hệ thống đo lại ngày vừa nhận hoạt động. Có hai hoạt động liền kề cách nhau dưới 10 thì cả ngày được đánh lại thành 1000, 2000, 3000, giữ nguyên thứ tự đang có.

| Ngày trước khi đo | Khoảng cách nhỏ nhất | Kết quả |
|---|:--:|---|
| 1000, 1010, 2000 | 10 | Giữ nguyên |
| 1000, 1009, 2000 | 9 | Đánh lại: 1000, 2000, 3000 |
| 10, 1000 | 10, tính từ 0 | Giữ nguyên |
| 9, 1000 | 9, tính từ 0 | Đánh lại: 1000, 2000 |
| 1000, 1000 | 0 | Đánh lại, hoạt động tạo trước đứng trước |

Mọi kịch bản bên dưới bắt đầu từ: Ngày 1 có "Ăn sáng" 1000 và "Tham quan" 2000, Ngày 2 có "Chợ đêm" 1000.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-ACT-141 | Kéo "Tham quan" tới số 1010, cách "Ăn sáng" đúng 10 | Giữ nguyên 1000 và 1010 | Biên | Đạt |
| TC-ACT-142 | Kéo "Tham quan" tới số 1009, cách "Ăn sáng" 9 | Cả ngày được đánh lại: "Ăn sáng" 1000, "Tham quan" 2000. Thứ tự không đổi | Biên | Đạt |
| TC-ACT-143 | Kéo lên đầu ngày với số 10, rồi kéo hoạt động khác lên đầu với số 9 | Số 10 được giữ. Số 9 làm cả ngày được đánh lại, vì khoảng cách tính cả từ 0 | Biên | Đạt |
| TC-ACT-144 | Kéo "Tham quan" tới đúng số 1000 của "Ăn sáng" | Cả ngày được đánh lại. "Ăn sáng" đứng trước vì được tạo trước | Biên | Đạt |
| TC-ACT-145 | Kéo "Chợ đêm" từ Ngày 2 tới số 1005 của Ngày 1 | Ngày 1 được đánh lại thành 1000, 2000, 3000, gồm cả hoạt động vừa chuyển tới. Không hoạt động nào đổi ngày ngoài ý muốn | Đúng | Đạt |
| TC-ACT-146 | Xem phản hồi sau khi đánh lại | Phản hồi mang số thứ tự mới, không phải số đã gửi lên | Đúng | Đạt |
| TC-ACT-147 | Ngày 2 đang chật, nhưng lần kéo chỉ đụng Ngày 1 | Ngày 2 giữ nguyên. Chỉ ngày nhận hoạt động mới được đo | Biên | Đạt |
| TC-ACT-148 | Ngày 1 đang chật. Kéo một hoạt động từ Ngày 1 sang Ngày 2 | Ngày 1 không được đánh lại, vì nó chỉ mất hoạt động chứ không nhận thêm | Biên | Đạt |

Việc số thứ tự đã đánh lại thật sự được ghi xuống database, chứ không chỉ nằm trong phản hồi, được kiểm trên MySQL thật ở `TC-ACT-154`.

---

## M. Kiểm toàn luồng sắp xếp lại qua mọi tầng

> **Yêu cầu:** design.md 10.2 "Quy ước Reorder", 11.3, rule 14.4, 14.5 · **Kiểm bởi:** `ActivityReorderFlowIntegrationTest`

Các phần J, K, L kiểm từng tầng bằng thành phần giả lập. Phần này chạy cả ứng dụng trên MySQL thật. Bốn điều chỉ chứng minh được ở đây: lô bị từ chối không để lại dấu vết, số đánh lại được ghi xuống database, kéo thả không tăng số phiên bản trên một lệnh UPDATE thật, và giờ được so đúng dù máy chạy múi giờ nào.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-ACT-149 | Chủ sở hữu kéo thả trong ngày, sang ngày khác, rồi dọn trống một ngày. Xem lại bằng chi tiết chuyến đi | Database và chi tiết chuyến đi đều đúng thứ tự mới. Ngày trống vẫn có trong phản hồi. Số phiên bản mọi hoạt động vẫn là 0, tên không đổi | Đúng | Đạt |
| TC-ACT-150 | Gửi lô có dòng hợp lệ kèm dòng sai: mã không tồn tại, ngày của chuyến khác, dòng trùng, số thứ tự 0. Rồi thử qua đường dẫn chuyến khác, bằng người lạ, và khi chưa đăng nhập | Mỗi lần trả đúng mã 404, 400, 403 hoặc 401. Sau tất cả, vị trí trong database y như trước | Bảo mật | Đạt |
| TC-ACT-151 | Xoá mềm chuyến đi rồi sắp xếp hoạt động của nó | 404, vị trí không đổi | Sai | Đạt |
| TC-ACT-152 | Chuyển 03:30 đến 05:00 tới ngày có 03:00 đến 04:00, rồi chuyển 20:00 đến 21:00 tới cùng ngày đó | Lần đầu 409, chi tiết trỏ đúng dòng và nêu tên hai hoạt động. Lần sau 200. Tiếp đó chạm đầu được, có xác nhận được, và đổi chỗ trong ngày không bị kiểm | Biên | Đạt |
| TC-ACT-153 | Hai hoạt động trùng giờ đổi ngày cho nhau trong một lần gọi | 200, mỗi hoạt động nằm ở ngày mới của nó | Biên | Đạt |
| TC-ACT-154 | Kéo một hoạt động vào cùng một chỗ bảy lần, khoảng cách co từ 500 xuống 7 | Lần thứ bảy phản hồi ghi 1000, 2000, 3000, và database cũng lưu đúng ba số đó. Số phiên bản vẫn là 0 | Biên | Đạt |
| TC-ACT-155 | Sắp xếp một hoạt động trong ngày có 2 hoạt động, rồi trong ngày có 22 hoạt động | Cả hai lần đều tốn đúng 6 câu SQL. Ngày đông hoạt động không tốn thêm câu nào | Biên | Đạt |

Sáu câu SQL của `TC-ACT-155`: kiểm quyền, kiểm chuyến đi còn sống, tải hoạt động của lô, tải các ngày liên quan, ghi hoạt động bị di chuyển, tải lại hoạt động của các ngày để trả về.

---

## Kiểm tra thủ công

Làm trên trang Swagger `http://localhost:8080/swagger-ui.html`. Cần có: backend đang chạy bản code mới nhất, đã đăng nhập bằng một tài khoản đã xác thực email, và một chuyến đi từ `2026-10-01` đến `2026-10-02`.

Lấy mã ngày bằng cách gọi danh sách ngày của chuyến đi. Các bước bên dưới dùng Ngày 1.

### MT-ACT-01 · Thêm hoạt động

- [x] Thêm hoạt động với nội dung bên dưới. Trả 201, tên hiển thị đúng dấu tiếng Việt, giờ là `11:30` và `13:00`, số thứ tự `1000`, tiền tệ trùng với chuyến đi.
- [x] Thêm hoạt động thứ hai chỉ với `{ "title": "Dạo hồ Xuân Hương" }`. Trả 201, loại là `OTHER`, số thứ tự `2000`.
- [x] Chạy câu lệnh SQL bên dưới trong MySQL. Có hai dòng, cột giờ của dòng đầu là `11:30:00` và `13:00:00`.

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

**Kết quả:** Đạt · **Ngày:** 2026-09-30 · **Ghi chú:** chủ dự án tự chạy trên Swagger

### MT-ACT-02 · Quy tắc về giờ

- [x] Thêm hoạt động có `"startTime": "10:00"` và `"endTime": "09:00"`. Trả 400, thông báo "Giờ kết thúc phải sau giờ bắt đầu".
- [x] Thêm hoạt động chỉ có `"endTime": "09:00"`. Trả 400, lỗi ở ô `startTime`.
- [x] Thêm hoạt động có `"startTime": "03:00"` và `"endTime": "08:15"`. Trả 201. Chạy lại câu lệnh SQL ở `MT-ACT-01`, cột giờ là `03:00:00` và `08:15:00`.

Bước cuối kiểm lại lỗi `BUG-ACT-001` trên database thật của máy bạn.

**Kết quả:** Đạt · **Ngày:** 2026-09-30 · **Ghi chú:** chủ dự án tự chạy trên Swagger

### MT-ACT-03 · Người lạ và đường dẫn sai

Cần có: tài khoản thứ hai đã xác thực email, và một chuyến đi thứ hai của tài khoản thứ nhất.

- [x] Đăng nhập bằng tài khoản thứ hai, thêm hoạt động vào chuyến đi của tài khoản thứ nhất. Trả 403.
- [x] Đăng nhập lại bằng tài khoản thứ nhất. Dùng mã của chuyến đi thứ nhất kèm mã ngày của chuyến đi thứ hai. Trả 404.

**Kết quả:** Đạt · **Ngày:** 2026-09-30 · **Ghi chú:** chủ dự án tự chạy trên Swagger

### MT-ACT-04 · Trùng giờ

Cần có: một ngày **chưa có hoạt động nào**. Dùng Ngày 2 của chuyến đi nếu Ngày 1 đã dùng ở các test trên.

- [x] Thêm "Ăn sáng" từ `09:00` đến `10:00`. Trả 201.
- [x] Thêm "Cà phê" từ `09:30` đến `10:30`. Trả 409 `ACTIVITY_TIME_CONFLICT`, chi tiết nêu tên "Ăn sáng" và giờ 09:00 - 10:00.
- [x] Thêm lại "Cà phê" như trên, điền `true` vào ô tham số `allowOverlap`. Trả 201.
- [x] Thêm "Đi chợ" từ `10:30` đến `11:30`. Trả 201, vì chỉ chạm đầu "Cà phê".
- [x] Thêm "Săn mây" từ `03:00` đến `04:00`, rồi thêm "Ngắm bình minh" từ `03:30` đến `05:00`. Lần đầu trả 201, lần sau trả 409 nêu tên "Săn mây".

Bước cuối kiểm lại lỗi `BUG-ACT-002` trên máy của bạn.

**Kết quả:** Đạt · **Ngày:** 2026-09-30 · **Ghi chú:** chủ dự án tự chạy trên Swagger

### MT-ACT-05 · Xem danh sách hoạt động

Cần có: một ngày đã có ít nhất ba hoạt động từ các test trước, và một ngày chưa có hoạt động nào.

- [x] Gọi danh sách hoạt động của ngày đã có hoạt động. Trả 200, số thứ tự tăng dần 1000, 2000, 3000.
- [x] So với thứ tự bạn đã thêm. Danh sách theo thứ tự thêm vào, không theo giờ.
- [x] Gọi danh sách của ngày chưa có hoạt động. Trả 200 với `"data": []`.
- [x] Đăng nhập bằng tài khoản thứ hai rồi gọi danh sách của chuyến đi này. Trả 403.

**Kết quả:** Đạt · **Ngày:** 2026-09-30 · **Ghi chú:** chủ dự án tự chạy trên Swagger

### MT-ACT-06 · Sửa hoạt động

Cần có: một ngày có "Ăn sáng" từ `09:00` đến `10:00` kèm ghi chú, và "Cà phê" từ `10:00` đến `11:00`. Ghi lại số phiên bản của "Ăn sáng" trước khi bắt đầu.

- [x] Sửa "Ăn sáng", chỉ gửi `{ "title": "Ăn sáng muộn" }`. Trả 200, giờ và ghi chú giữ nguyên, số phiên bản tăng 1.
- [x] Gửi `{ "note": "" }`. Trả 200, ghi chú trở về `null`.
- [x] Gửi `{ "endTime": "10:30" }`. Trả 409, nêu tên "Cà phê". Gọi danh sách hoạt động, giờ kết thúc vẫn là `10:00`.
- [x] Gửi lại như trên, điền `true` vào ô tham số `allowOverlap`. Trả 200, giờ kết thúc là `10:30`.
- [x] Gửi `{ "title": "Ăn sáng" }`, không kèm `allowOverlap`. Trả 200, dù hoạt động đang trùng giờ với "Cà phê".
- [x] Gửi `{ "endTime": "08:00" }`. Trả 400, thông báo "Giờ kết thúc phải sau giờ bắt đầu".

**Kết quả:** Đạt · **Ngày:** 2026-09-30 · **Ghi chú:** chủ dự án tự chạy trên Swagger

### MT-ACT-07 · Xoá hoạt động

Cần có: một ngày có ba hoạt động với số thứ tự 1000, 2000, 3000. Ghi lại mã của hoạt động ở giữa.

- [x] Xoá hoạt động ở giữa. Trả 200 với `"data": null`.
- [x] Gọi danh sách hoạt động của ngày. Còn hai hoạt động, số thứ tự là 1000 và 3000, không bị đánh lại.
- [x] Xoá lại đúng hoạt động đó. Trả 404.
- [x] Chạy câu lệnh SQL bên dưới với mã đã ghi. Kết quả là 0 dòng, vì hoạt động bị xoá hẳn.
- [x] Thêm một hoạt động mới vào ngày đó. Số thứ tự là 4000.

```sql
SELECT id, title FROM activities WHERE id = <mã đã ghi>;
```

**Kết quả:** Đạt · **Ngày:** 2026-09-30 · **Ghi chú:** chủ dự án tự chạy trên Swagger

### MT-ACT-08 · Rút ngắn chuyến đi đang có hoạt động

Cần có: một chuyến đi **mới** từ `2026-10-01` đến `2026-10-03`. Thêm một hoạt động vào Ngày 1 và hai hoạt động vào Ngày 3. Ghi lại tên và số phiên bản của chuyến đi.

- [x] Sửa chuyến đi, gửi `{ "title": "Tên thử", "endDate": "2026-10-02" }`. Trả 409 `TRIP_DAY_HAS_ACTIVITIES`, chi tiết ghi "2 hoạt động trong 1 ngày sẽ bị xoá nếu đổi ngày".
- [x] Xem chi tiết chuyến đi. Tên, ngày kết thúc và số phiên bản đều như cũ. Vẫn còn 3 ngày.
- [x] Gọi danh sách hoạt động của Ngày 3. Vẫn còn đủ hai hoạt động.
- [x] Gửi lại đúng nội dung đó, điền `true` vào ô tham số `force`. Trả 200, tên đổi, ngày kết thúc là `2026-10-02`.
- [x] Gọi danh sách ngày. Còn 2 ngày. Gọi danh sách hoạt động của Ngày 1, hoạt động vẫn còn.
- [x] Chạy câu lệnh SQL bên dưới. Chỉ còn một dòng, là hoạt động của Ngày 1.

```sql
SELECT a.id, a.title, d.date FROM activities a JOIN trip_days d ON d.id = a.trip_day_id ORDER BY a.id DESC LIMIT 5;
```

**Kết quả:** Đạt · **Ngày:** 2026-09-30 · **Ghi chú:** chủ dự án tự chạy trên Swagger

### MT-ACT-09 · Chi tiết chuyến đi kèm hoạt động

Cần có: một chuyến đi 3 ngày. Ngày 1 có hai hoạt động, Ngày 2 có một hoạt động, Ngày 3 chưa có gì.

- [x] Gọi chi tiết chuyến đi. Phần `days` có 3 ngày, mỗi ngày có ô `activities`.
- [x] Xem Ngày 1. Có hai hoạt động, số thứ tự 1000 rồi 2000.
- [x] Xem Ngày 3. Ô `activities` là `[]`, không phải `null` và không bị thiếu.
- [x] Gọi danh sách ngày của chuyến đi. Các ngày **không** có ô `activities`.

**Kết quả:** Đạt · **Ngày:** 2026-09-30 · **Ghi chú:** chủ dự án tự chạy trên Swagger

### MT-ACT-10 · Kéo thả hoạt động

Làm trên Swagger với endpoint sắp xếp lại. Cần có: một chuyến đi có Ngày 1 với ba hoạt động A, B, C theo thứ tự 1000, 2000, 3000, và Ngày 2 chưa có gì. Ghi lại mã của ba hoạt động, mã của hai ngày, và số phiên bản của C.

- [x] Gửi nội dung bên dưới, thay mã thật vào. Trả 200, phần `data` có một ngày, thứ tự là A, C, B.
- [x] Xem số phiên bản của C trong phản hồi. Vẫn bằng giá trị đã ghi.
- [x] Gửi một dòng chuyển A sang Ngày 2 với số thứ tự 1000. Trả 200, phần `data` có hai ngày.
- [x] Gửi hai dòng: dòng đầu hợp lệ, dòng sau dùng mã hoạt động `999999`. Trả 404.
- [x] Gọi danh sách hoạt động của Ngày 1. Thứ tự **không đổi** so với trước bước trên.

```json
{ "items": [ { "activityId": <mã của C>, "dayId": <mã Ngày 1>, "orderIndex": 1500 } ] }
```

**Kết quả:** Đạt · **Ngày:** 2026-09-30 · **Ghi chú:** chủ dự án tự chạy trên Swagger

### MT-ACT-11 · Chuyển hoạt động có giờ sang ngày khác

Cần có: Ngày 1 có "Săn mây" từ `03:00` đến `04:00` và "Ăn trưa" từ `11:30` đến `13:00`. Ngày 2 có "Ngắm bình minh" từ `03:30` đến `05:00` và "Cà phê" từ `13:00` đến `13:30`.

- [x] Chuyển "Ngắm bình minh" sang Ngày 1. Trả 409, chi tiết nêu tên "Săn mây" và giờ 03:00 - 04:00.
- [x] Gọi danh sách hoạt động của Ngày 2. "Ngắm bình minh" vẫn ở đó.
- [x] Chuyển "Cà phê" sang Ngày 1. Trả 200, vì nó chỉ chạm đầu "Ăn trưa".
- [x] Chuyển lại "Ngắm bình minh" sang Ngày 1, điền `true` vào ô tham số `allowOverlap`. Trả 200.
- [x] Đổi vị trí "Ngắm bình minh" và "Săn mây" trong Ngày 1, không kèm `allowOverlap`. Trả 200, dù hai hoạt động đang trùng giờ.

Bước đầu kiểm lại lỗi `BUG-ACT-002` cho trường hợp chuyển ngày, trên máy của bạn.

**Kết quả:** Đạt · **Ngày:** 2026-09-30 · **Ghi chú:** chủ dự án tự chạy trên Swagger

### MT-ACT-12 · Đánh lại số thứ tự

Cần có: một ngày có ba hoạt động A, B, C với số thứ tự 1000, 2000, 3000.

- [x] Kéo C vào giữa A và B bằng cách gửi lần lượt các số 1500, 1250, 1125, 1062, 1031, 1015. Mỗi lần trả 200.
- [x] Gọi danh sách hoạt động. Thứ tự là A 1000, C 1015, B 2000.
- [x] Gửi số 1007 cho C. Trả 200, phản hồi ghi A 1000, C 2000, B 3000.
- [x] Gọi lại danh sách hoạt động. Số thứ tự trong database cũng là 1000, 2000, 3000.

**Kết quả:** Đạt · **Ngày:** 2026-09-30 · **Ghi chú:** chủ dự án tự chạy trên Swagger

---

## Lỗi đã phát hiện

Task 2.3: Mốc 1 phát sinh `BUG-ACT-001`, Mốc 3 phát sinh `BUG-ACT-002`, Mốc 9 phát sinh `BUG-ACT-003`. Các mốc còn lại không phát sinh lỗi.

Task 2.4: cả bốn mốc không phát sinh lỗi, test mới đều đạt ngay lần chạy đầu.

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

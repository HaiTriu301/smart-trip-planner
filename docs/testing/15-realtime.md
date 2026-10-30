# 15 · Đồng chỉnh sửa thời gian thực

> Cập nhật: 2026-10-30 · **Task 5.1 xong** (10 commit `2ca5dd1` đến `fd76fc7` trên nhánh `feat/T5.1-stale-version`; backend 1547 lượt test, giao diện 244 test xanh; chủ dự án đã chạy 6 bài thủ công `MT-RT-01` đến `06` ngày 2026-10-30, đều đạt ở lần chạy đầu, gồm hai bài nghiệm thu `MT-RT-05`, `06` và bước kiểm lại `BUG-UI-014` ở `MT-RT-03`) · Mốc 10 (máy chủ từ chối sửa chuyến đi trên bản cũ: 409, kể cả kèm `force=true`) tại `fd76fc7` · Mốc 9 (các test có sẵn gửi kèm số phiên bản khi sửa chuyến đi, không đổi hành vi) tại `dd3d461` · Mốc 8 (máy chủ từ chối sửa hoạt động trên bản cũ: 409) tại `ebc6c82`; `MT-RT-05` chưa chạy · Mốc 7 (các test có sẵn gửi kèm số phiên bản khi sửa hoạt động, không đổi hành vi) tại `fddd351` · Mốc 6 (form "Sửa chuyến đi" gặp 409 thì giữ chữ và báo; chuyến đi đã bị xoá thì báo và đóng) tại `e08bba9`; `MT-RT-04` chưa chạy · Mốc 5 (form "Sửa chuyến đi" gửi kèm số phiên bản, so với bản lúc mở form; sửa `BUG-UI-014`) tại `dc3c8f3`; `MT-RT-03` chưa chạy · Mốc 4 (form "Sửa hoạt động" gặp 409 thì giữ chữ và báo; hoạt động đã bị xoá thì báo và đóng) tại `2df7208`; `MT-RT-02` chưa chạy · Mốc 3 (form "Sửa hoạt động" gửi kèm số phiên bản) tại `f91af35`; `MT-RT-01` chưa chạy · Mốc 2 (hai lần lưu cùng lúc một dòng trả 409 thay vì 500) tại `7a0fae3`, build xanh 1534 lượt test · Mốc 1 (đổi trạng thái chuyến đi, tạo / thu hồi liên kết chia sẻ không làm tăng số phiên bản) tại `2ca5dd1`, build xanh 1532 lượt test · [Về trang chính](README.md)

Nhiều người cùng sửa một chuyến đi. File này ghi lại cả Phase 5: **chống ghi đè** (Task 5.1), kết nối thời gian thực (Task 5.2), báo thay đổi cho người đang xem (Task 5.3), danh sách người đang xem (Task 5.4) và giao diện (Task 5.5).

**Chống ghi đè** (design.md 11.5, rule 14.29): mỗi chuyến đi và mỗi hoạt động có một **số phiên bản**, tăng 1 sau mỗi lần sửa. Form sửa gửi kèm số phiên bản của bản mà người dùng đang thấy. Nếu trong lúc đó người khác đã lưu (số phiên bản đã tăng), máy chủ từ chối với mã 409 `STALE_VERSION` và không ghi gì. Người lưu sau vẫn giữ được chữ đang gõ và chọn lưu đè hoặc huỷ để xem bản mới. Không ai mất dữ liệu mà không biết.

Các mốc của Task 5.1:

| Mốc | Nội dung | Phần trong file | Mã commit |
|---|---|---|---|
| 1 | Đổi trạng thái chuyến đi, tạo hoặc thu hồi liên kết chia sẻ không làm tăng số phiên bản, để không làm hỏng form "Sửa chuyến đi" đang mở của người khác | A | `2ca5dd1` |
| 2 | Hai lần lưu cùng một chuyến đi hoặc hoạt động rơi vào cùng lúc: lần sau nhận 409 "dữ liệu đã bị người khác thay đổi", không còn lỗi 500 | B | `7a0fae3` |
| 3 | Form "Sửa hoạt động" gửi kèm số phiên bản của hoạt động đang hiển thị. Máy chủ chưa đọc số này (tới Mốc 8), nên người dùng chưa thấy gì khác | C | `f91af35` |
| 4 | Form "Sửa hoạt động" bị từ chối vì người khác vừa lưu: form vẫn mở, giữ chữ, hiện dòng báo vàng, ngầm nhận số phiên bản mới. Hoạt động vừa bị xoá: báo và đóng form. Phần "vừa được người khác sửa" chỉ thử được bằng tay từ Mốc 8 | D | `2df7208` |
| 5 | Form "Sửa chuyến đi" gửi kèm số phiên bản, và so các ô với chuyến đi **lúc mở form** chứ không với bản trang vừa tự tải lại. Sửa luôn một lỗi có từ Task 2.5: quay lại tab trong lúc form đang mở thì những ô không sửa có thể ghi đè việc người khác vừa lưu | E | `dc3c8f3` |
| 6 | Form "Sửa chuyến đi" bị từ chối vì người khác vừa lưu: form vẫn mở, giữ chữ, hiện dòng báo vàng, ngầm nhận số phiên bản mới. Chuyến đi vừa bị xoá: báo và đóng form. Phần "vừa được người khác sửa" chỉ thử được bằng tay từ Mốc 10 | F | `e08bba9` |
| 7 | Chuẩn bị cho Mốc 8, không đổi hành vi: mọi test có sẵn gửi lệnh sửa hoạt động nay gửi kèm số phiên bản đúng với database lúc gửi. Máy chủ chưa đọc số này nên kết quả không đổi; từ Mốc 8 các test này vẫn đúng mà không phải sửa thêm | — | `fddd351` |
| 8 | Máy chủ bắt buộc số phiên bản khi sửa hoạt động; lệch thì trả 409 và không ghi gì. Từ đây dòng báo vàng của form hoạt động hiện ra thật | G | `ebc6c82` |
| 9 | Chuẩn bị cho Mốc 10, không đổi hành vi: mọi test có sẵn gửi lệnh sửa chuyến đi nay gửi kèm số phiên bản đúng với database lúc gửi | — | `dd3d461` |
| 10 | Máy chủ bắt buộc số phiên bản khi sửa chuyến đi; lệch thì trả 409 và không ghi gì, kể cả khi đã trả lời "Vẫn đổi ngày". Từ đây dòng báo vàng của form chuyến đi hiện ra thật | H | `fd76fc7` |

## A. Những thay đổi không làm tăng số phiên bản của chuyến đi

> **Yêu cầu:** design.md 11.5 "Không khoá", 5.2 (`trips.version`), 10.2 "Quy ước Trip API" (`PATCH /{id}/status` không có `version`) · **Kiểm bởi:** `TripFlowIntegrationTest`, `ShareLinkFlowIntegrationTest`

Số phiên bản chỉ bảo vệ những gì form "Sửa chuyến đi" thay đổi (tên, mô tả, điểm đến, ngày, ngân sách...). Trạng thái chuyến đi (nháp, đã lên kế hoạch...) và chế độ hiển thị (riêng tư, có liên kết) được đổi bằng nút riêng, không qua form. Nếu hai việc này làm tăng số phiên bản, thì từ Mốc 10, khi máy chủ bắt đầu kiểm số phiên bản, người đang mở form sẽ bị từ chối oan chỉ vì ai đó vừa bấm đổi trạng thái. Trước mốc này, đổi trạng thái làm số phiên bản tăng 1 (`TC-TRIP-043` ghi đúng như vậy).

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-RT-001 | Chủ chuyến đổi chuyến đi mới tạo (phiên bản 0) từ nháp sang "đã lên kế hoạch" | 200, trạng thái mới, số phiên bản **vẫn là 0** | Đúng | Đạt |
| TC-RT-002 | Đổi tiếp qua cả 5 trạng thái (đang đi, hoàn thành, lưu trữ, về nháp) | Mỗi lần 200, database lưu đúng trạng thái; sau cả loạt, số phiên bản trong database vẫn là 0 | Đúng | Đạt |
| TC-RT-003 | Sau loạt đổi trạng thái ở trên, sửa tên chuyến đi | 200, số phiên bản thành 1: những gì form sửa vẫn được khoá | Đúng | Đạt |
| TC-RT-004 | Chủ chuyến tạo liên kết chia sẻ, rồi thu hồi liên kết đó (liên kết duy nhất) | Tạo xong: chuyến đi thành "có liên kết", số phiên bản vẫn 0. Thu hồi xong: chuyến đi về "riêng tư", số phiên bản vẫn 0 | Đúng | Đạt |

## B. Hai lần lưu cùng lúc

> **Yêu cầu:** design.md 11.5, 10.3 (bảng exception: `ObjectOptimisticLockingFailureException` → `STALE_VERSION` 409) · **Kiểm bởi:** `TripRepositoryTest`, `GlobalExceptionHandlerTest`

Hai người bấm Lưu gần như cùng lúc. Cả hai request đọc cùng một số phiên bản, request đầu ghi xong và tăng số phiên bản, request sau ghi với số phiên bản cũ nên database không tìm thấy dòng nào khớp. Trước mốc này, tình huống đó rơi vào lỗi chung và người dùng thấy "Đã có lỗi xảy ra" (500), log ghi như một lỗi của chương trình. Nay người lưu sau nhận 409 giống như khi gửi số phiên bản cũ (từ Mốc 8, 10), và log chỉ ghi một dòng cảnh báo. Lỗi này có trên bản chạy thật từ Phase 2 (ghi nợ từ rà soát Task 2.7).

Không thể cho hai request rơi đúng vào cùng một khoảnh khắc trong test mà kết quả lần nào cũng như nhau, nên mốc này kiểm hai nửa riêng: database thật báo đúng loại lỗi khi số phiên bản đã đổi, và lỗi đó được trả về thành 409.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-RT-005 | Đọc một chuyến đi (phiên bản 0); một request khác lưu cùng dòng đó (phiên bản thành 1); rồi lưu bản đã đọc với tên mới | Database không ghi; hệ thống báo đúng loại lỗi "khoá lạc quan", kèm tên loại dữ liệu (chuyến đi) và mã của dòng | Biên | Đạt |
| TC-RT-006 | Một request gặp lỗi "khoá lạc quan" ở trên | 409, mã `STALE_VERSION`, câu "Dữ liệu đã bị người khác thay đổi, vui lòng tải lại"; log có đúng một dòng cảnh báo (WARN) ghi loại dữ liệu, mã dòng, method và đường dẫn, không in chi tiết lỗi của chương trình | Đúng | Đạt |

## C. Form "Sửa hoạt động" gửi số phiên bản

> **Yêu cầu:** design.md 10.2 "Quy ước Activity API" (dòng `version`), 11.5 · **Kiểm bởi:** `MT-RT-01` (không có hàm tính toán mới để test tự động: phần so sánh các ô đã sửa giữ nguyên, chỉ đổi tên)

Từ mốc này, mỗi lần lưu form "Sửa hoạt động" gửi thêm số phiên bản của hoạt động lúc mở form. Form không sửa ô nào thì vẫn đóng ngay, không gửi gì, như trước. Máy chủ hiện bỏ qua trường lạ trong body (cấu hình sẵn của Spring Boot, đã kiểm trong mã của thư viện), nên mốc này chạy được trước khi máy chủ bắt buộc số phiên bản ở Mốc 8.

## D. Form "Sửa hoạt động" khi lần lưu bị từ chối

> **Yêu cầu:** design.md 11.5 "Giao diện gặp 409", quyết định 3 và 16 của Phase 5 (WORKFLOW.md), UI_GUIDE 10 (dòng `STALE_VERSION`) · **Kiểm bởi:** `api/errors.test.ts`, `features/itinerary/findActivity.test.ts` (Vitest), `MT-RT-02`

Khi máy chủ trả 409 `STALE_VERSION`, form làm bốn việc: giữ nguyên mọi ô đang gõ; hiện dòng báo vàng "Hoạt động này vừa được người khác sửa. Bấm Lưu lần nữa để ghi đè bằng nội dung của bạn, hoặc Huỷ để xem bản mới."; tải lại chuyến đi; tìm hoạt động trong bản vừa tải để lấy số phiên bản mới. Nếu hoạt động không còn trong bản vừa tải, hoặc máy chủ trả 404, form báo "Hoạt động này vừa bị xoá" và đóng.

Lần Lưu thứ hai chỉ gửi những ô **người dùng đã sửa** so với lúc mở form (quyết định 16, chốt 2026-10-29). Ví dụ A sửa ghi chú, B sửa giờ: sau khi B lưu lần hai, hoạt động có ghi chú của A và giờ của B. Ô người dùng không động tới không ghi đè việc người kia vừa làm.

Trong lúc tải lại, nút Lưu vẫn quay và không đóng được hộp thoại. Không tải lại được (mất mạng) thì form vẫn báo, giữ số phiên bản cũ, lần Lưu sau sẽ hỏi lại máy chủ.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-RT-007 | Nhận ra câu trả lời "người khác vừa lưu" | Đúng với 409 mã `STALE_VERSION`; sai với 409 trùng giờ (`ACTIVITY_TIME_CONFLICT`), 409 không có nội dung, và lỗi mạng | Đúng | Đạt |
| TC-RT-008 | Nhận ra câu trả lời "không còn tồn tại" | Đúng với 404 mã `RESOURCE_NOT_FOUND` của máy chủ; sai với 403, với 404 không phải do máy chủ ứng dụng trả (trang lỗi của proxy) và với lỗi không đến từ request | Đúng | Đạt |
| TC-RT-009 | Tìm hoạt động trong chuyến đi vừa tải lại, kể cả khi người khác đã chuyển nó sang ngày khác | Tìm thấy ở bất kỳ ngày nào, với số phiên bản và ngày mới nhất | Đúng | Đạt |
| TC-RT-010 | Tìm hoạt động đã bị xoá, trong một ngày trống, hoặc khi chuyến đi không còn ngày nào | Không tìm thấy: form coi như hoạt động đã bị xoá | Biên | Đạt |

## E. Form "Sửa chuyến đi" gửi số phiên bản

> **Yêu cầu:** design.md 10.2 "Quy ước Trip API" (dòng `version`), 11.5, quyết định 16 của Phase 5 · **Kiểm bởi:** `MT-RT-03` (không có hàm tính toán mới: phần so sánh các ô đã sửa chỉ đổi tên)

Mỗi lần lưu form "Sửa chuyến đi" gửi thêm số phiên bản của chuyến đi lúc mở form. Lần gửi lại sau câu hỏi "Xoá hoạt động khi đổi ngày?" (kèm `force=true`) mang cùng số đó.

Mốc này cũng sửa một lỗi có từ Task 2.5, chưa ai báo. Form lấy chuyến đi từ bộ nhớ của trang, mà trang tự tải lại mỗi khi người dùng quay về tab. Trước mốc này, form so các ô với bản vừa tải lại: nếu người khác vừa đổi tên chuyến đi, ô tên trong form (vẫn là tên cũ) bị coi là "đã sửa" và gửi đi, tên mới của người kia mất mà không ai biết. Nay form chụp lại chuyến đi lúc mở và so với bản chụp đó, nên chỉ những ô người dùng thật sự sửa mới được gửi. Bản chụp cũng là nơi lấy số phiên bản: nếu lấy từ bản vừa tải lại, lần lưu sẽ luôn khớp với máy chủ và việc chống ghi đè vô tác dụng.

## F. Form "Sửa chuyến đi" khi lần lưu bị từ chối

> **Yêu cầu:** design.md 11.5 "Giao diện gặp 409", 10.2 (`force=true` với số phiên bản cũ vẫn 409), quyết định 3 và 16 của Phase 5, UI_GUIDE 10 · **Kiểm bởi:** `MT-RT-04`; các hàm nhận ra câu trả lời dùng lại `TC-RT-007`, `TC-RT-008`

Giống form hoạt động (phần D), với câu "Chuyến đi này vừa được người khác sửa. Bấm Lưu lần nữa để ghi đè bằng nội dung của bạn, hoặc Huỷ để xem bản mới." Khác ở ba chỗ:

- Form tải lại chính chuyến đi để lấy số phiên bản mới. Tải lại mà máy chủ báo chuyến đi không còn thì coi như đã bị xoá.
- Câu hỏi "Xoá hoạt động khi đổi ngày?" là câu hỏi về **bản cũ** của chuyến đi. Nếu bấm "Vẫn đổi ngày" mà máy chủ trả "người khác vừa lưu", câu hỏi đóng lại và dòng báo vàng hiện trong form. Bấm Lưu lần nữa thì máy chủ kiểm lại ngày trên bản mới và hỏi lại nếu cần.
- Thời tiết và bình luận được tải lại cùng chuyến đi: người kia có thể đã đổi điểm đến hoặc rút ngắn chuyến đi (xoá các hoạt động cùng bình luận của chúng).

Chuyến đi vừa bị xoá: toast đỏ "Chuyến đi này vừa bị xoá", form đóng, trang báo không còn chuyến đi.

## G. Máy chủ từ chối sửa hoạt động trên bản cũ

> **Yêu cầu:** design.md 10.2 "Quy ước Activity API" (dòng `version`), 11.5, 16 ("Hai client sửa cùng activity → client sau nhận 409"), rule 14.29 · **Kiểm bởi:** `ActivityServiceTest`, `ActivityControllerTest`, `ActivityFlowIntegrationTest`, `MT-RT-05`

Từ mốc này, lệnh sửa hoạt động **bắt buộc** có số phiên bản. Máy chủ so số đó với bản đang lưu **trước mọi quy tắc khác** (giờ, trùng giờ, địa điểm): lệch thì trả 409 `STALE_VERSION` và không ghi gì. Nhờ vậy người dùng không bị hỏi "Vẫn lưu?" về chuyện trùng giờ trên một bản người khác đã thay. Lần gửi lại sau câu hỏi đó (kèm `allowOverlap=true`) cũng phải mang số phiên bản đang lưu.

Đây là mốc làm cho dòng báo vàng của form hoạt động (phần D) xuất hiện thật. Mọi test có sẵn gửi lệnh sửa hoạt động (đã thêm số phiên bản ở Mốc 7) đều xanh với quy tắc mới.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-RT-011 | Hai người cùng mở "Sửa" một hoạt động ở phiên bản 0. Người A lưu ghi chú mới (thành phiên bản 1); người B lưu tên và giờ mới, vẫn gửi phiên bản 0 | A: 200, phiên bản 1. B: 409 `STALE_VERSION`, "Dữ liệu đã bị người khác thay đổi, vui lòng tải lại"; database giữ nguyên đúng như sau lần lưu của A | Đúng | Đạt |
| TC-RT-012 | Sau đó B gửi lại tên và giờ mới với phiên bản 1 | 200, phiên bản 2: tên và giờ của B, **ghi chú của A vẫn còn** vì B không gửi ghi chú (quyết định 16) | Đúng | Đạt |
| TC-RT-013 | Gửi lệnh sửa không có số phiên bản | 400, lỗi ở ô `version`: "Thiếu số phiên bản của dữ liệu, hãy tải lại trang rồi thử lại"; không gọi tới phần xử lý | Sai | Đạt |
| TC-RT-014 | Gửi phiên bản 2 trong khi bản đang lưu là 3 | Bị từ chối với mã `STALE_VERSION`; tên hoạt động không đổi, không ghi gì; log ghi "Activity 31: client sent version 2, stored version is 3" | Sai | Đạt |
| TC-RT-015 | Gửi phiên bản cũ kèm giờ trùng với hoạt động khác và một địa điểm mới | Bị từ chối vì phiên bản cũ; máy chủ không kiểm trùng giờ, không kiểm địa điểm | Biên | Đạt |
| TC-RT-016 | Gửi lại với "Vẫn lưu" (`allowOverlap=true`) nhưng mang phiên bản cũ | Vẫn bị từ chối vì phiên bản cũ; giờ không đổi | Biên | Đạt |
| TC-RT-017 | Gửi đúng phiên bản đang lưu (4) | Lưu bình thường | Đúng | Đạt |

Câu trả lời 409 được trả về đúng định dạng (mã, câu tiếng Việt) cũng được kiểm ở tầng controller. Lỗi phân quyền (người lạ, người chỉ xem) đã có sẵn trong ma trận quyền (`TripPermissionMatrixIntegrationTest`, dòng "Sửa hoạt động").

## H. Máy chủ từ chối sửa chuyến đi trên bản cũ

> **Yêu cầu:** design.md 10.2 "Quy ước Trip API" (dòng `version`: "khác bản đang lưu → 409, không ghi gì, kể cả khi gửi kèm `force=true`"), 11.5, rule 14.29 · **Kiểm bởi:** `TripServiceTest`, `TripControllerTest`, `TripFlowIntegrationTest`, `MT-RT-06`

Như phần G, cho form "Sửa chuyến đi". Máy chủ so số phiên bản **trước khi** gộp các ô và trước khi thêm / bớt ngày. Câu "Vẫn đổi ngày" (`force=true`) là câu trả lời cho bản người dùng đã thấy, nên không được xoá ngày trên một bản người khác đã thay: gửi kèm `force=true` mà số phiên bản cũ thì vẫn bị từ chối và không ngày nào bị xoá. Đổi trạng thái chuyến đi ở giữa không làm số phiên bản đổi (phần A), nên không làm form của ai bị từ chối.

| Mã | Kịch bản | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|
| TC-RT-018 | Hai form cùng mở chuyến đi 3 ngày ở phiên bản 0; ở giữa, chủ chuyến đổi trạng thái. Form A lưu mô tả (thành phiên bản 1). Form B rút còn 2 ngày, đổi tên, gửi phiên bản 0 kèm `force=true` | Đổi trạng thái: 200, không ảnh hưởng gì. A: 200, phiên bản 1. B: 409 `STALE_VERSION`; tên và ngày kết thúc trong database không đổi, chuyến đi vẫn đủ 3 ngày | Đúng | Đạt |
| TC-RT-019 | B gửi lại với phiên bản 1 (vẫn `force=true`) | 200, phiên bản 2: tên và ngày kết thúc của B, **mô tả của A vẫn còn**, trạng thái "đã lên kế hoạch" giữ nguyên; chuyến đi còn 2 ngày | Đúng | Đạt |
| TC-RT-020 | Gửi lệnh sửa chuyến đi không có số phiên bản | 400, lỗi ở ô `version`: "Thiếu số phiên bản của dữ liệu, hãy tải lại trang rồi thử lại"; không gọi tới phần xử lý | Sai | Đạt |
| TC-RT-021 | Gửi phiên bản 2 trong khi bản đang lưu là 3, kèm tên và ngày mới | Bị từ chối với mã `STALE_VERSION`; tên và ngày không đổi, không thêm / bớt ngày, không ghi gì; log ghi "Trip 5: client sent version 2, stored version is 3" | Sai | Đạt |
| TC-RT-022 | Gửi phiên bản cũ kèm `force=true` | Vẫn bị từ chối, không ngày nào bị xoá | Biên | Đạt |
| TC-RT-023 | Gửi đúng phiên bản đang lưu (4) | Lưu bình thường | Đúng | Đạt |

Câu trả lời 409 (kèm `force=true`) được kiểm cả ở tầng controller. Lỗi phân quyền đã có trong ma trận quyền (dòng "Sửa chuyến đi").

## Kiểm tra thủ công

Bài nghiệm thu của Task 5.1 là `MT-RT-05` (hoạt động) và `MT-RT-06` (chuyến đi).

### MT-RT-01 · Form "Sửa hoạt động" gửi số phiên bản · Đạt (2026-10-30)

Cần có: backend và frontend đang chạy (`npm run dev`), một chuyến đi có ít nhất một hoạt động.

- [x] Mở trang chuyến đi, mở DevTools tab Network. Bấm "Sửa" một hoạt động, đổi tên, bấm "Lưu thay đổi". Request `PATCH .../activities/{id}` có body chứa tên mới và `"version": n`, với `n` bằng `version` của hoạt động đó trong response `GET /trips/{id}` (tab Network, phần `days[].activities[]`). Trả 200, toast "Đã lưu thay đổi".
- [x] Mở "Sửa" hoạt động đó lần nữa. Lần này request gửi `"version": n + 1` (đúng số mới sau lần lưu trước).
- [x] Mở "Sửa", không đổi gì, bấm "Lưu thay đổi". Form đóng ngay, tab Network không có request `PATCH` nào.
- [x] Mở "Sửa" một hoạt động, đổi giờ bắt đầu cho trùng với một hoạt động khác, bấm Lưu. Hộp "Trùng giờ với hoạt động khác" hiện ra; bấm "Vẫn lưu". Request thứ hai (`?allowOverlap=true`) mang **cùng** `version` với request đầu. Trả 200.

### MT-RT-02 · Lưu một hoạt động người khác vừa xoá · Đạt (2026-10-30)

Cần có: hai tab cùng đăng nhập một tài khoản (hoặc chủ chuyến và một thành viên "Chỉnh sửa"), cùng mở một chuyến đi có ít nhất một hoạt động.

- [x] Tab 1: bấm "Sửa" một hoạt động, đổi tên, **chưa** bấm Lưu.
- [x] Tab 2: xoá chính hoạt động đó.
- [x] Tab 1: bấm "Lưu thay đổi". Toast đỏ "Hoạt động này vừa bị xoá", hộp thoại đóng, thẻ hoạt động biến mất khỏi ngày mà không phải tải lại trang.

### MT-RT-03 · Form "Sửa chuyến đi" gửi số phiên bản và chỉ gửi ô đã sửa · Đạt (2026-10-30) · bước 3 kiểm lại `BUG-UI-014`

Cần có: hai tab cùng đăng nhập (hoặc chủ chuyến và một thành viên "Chỉnh sửa"), cùng mở một chuyến đi; DevTools tab Network ở tab 1.

- [x] Tab 1: bấm "Sửa" chuyến đi, đổi tên, bấm "Lưu thay đổi". Request `PATCH /trips/{id}` có tên mới và `"version": n`, với `n` là `version` trong response `GET /trips/{id}` lúc mở form. Trả 200.
- [x] Tab 1: mở lại "Sửa" chuyến đi, **chưa** sửa gì. Tab 2: đổi tên chuyến đi thành "Tên của tab 2", lưu.
- [x] Quay về tab 1 (trang tự tải lại khi quay về tab, form vẫn mở với tên cũ). Chỉ sửa ô mô tả, bấm Lưu. Request chỉ có `description` và `"version"` bằng số **lúc mở form** (không phải số mới), nên máy chủ trả 409 và form hiện khung vàng thay vì lưu đè (từ Mốc 10). Bấm Lưu lần nữa: request vẫn chỉ có `description`, với số phiên bản mới; trả 200 và tên chuyến đi vẫn là "Tên của tab 2". Trước Mốc 5, lần lưu đầu đã gửi lại cả tên cũ và tên của tab 2 mất mà không ai biết.
- [x] Rút ngắn chuyến đi sao cho một ngày có hoạt động nằm ngoài khoảng mới, bấm Lưu. Hộp "Xoá hoạt động khi đổi ngày?" hiện ra; bấm "Vẫn đổi ngày". Request thứ hai (`?force=true`) mang **cùng** `version` với request đầu.

### MT-RT-04 · Lưu một chuyến đi người khác vừa xoá · Đạt (2026-10-30)

Cần có: hai tab cùng đăng nhập tài khoản chủ chuyến, cùng mở một chuyến đi.

- [x] Tab 1: bấm "Sửa" chuyến đi, đổi tên, **chưa** bấm Lưu.
- [x] Tab 2: xoá chuyến đi.
- [x] Tab 1: bấm "Lưu thay đổi". Toast đỏ "Chuyến đi này vừa bị xoá", hộp thoại đóng, trang báo không còn chuyến đi (không phải màn hình trắng hay "Đã có lỗi xảy ra").

### MT-RT-05 · Hai người cùng sửa một hoạt động (nghiệm thu Task 5.1, phần hoạt động) · Đạt (2026-10-30)

Cần có: hai trình duyệt (hoặc một cửa sổ thường và một cửa sổ ẩn danh), đăng nhập chủ chuyến và một thành viên "Chỉnh sửa", cùng mở một chuyến đi có một hoạt động có ghi chú.

- [x] Cả hai bấm "Sửa" cùng một hoạt động.
- [x] A đổi ghi chú, bấm "Lưu thay đổi". Toast "Đã lưu thay đổi".
- [x] B đổi tên và giờ bắt đầu (không đụng ghi chú), bấm "Lưu thay đổi". Form **không đóng**; tên và giờ B vừa gõ vẫn còn; phía trên các ô hiện khung vàng có icon tam giác: "Hoạt động này vừa được người khác sửa. Bấm Lưu lần nữa để ghi đè bằng nội dung của bạn, hoặc Huỷ để xem bản mới." Trong lúc chờ, nút Lưu quay và không đóng được hộp thoại.
- [x] B bấm "Lưu thay đổi" lần nữa. Toast "Đã lưu thay đổi", form đóng. Thẻ hoạt động có tên và giờ của B **và ghi chú của A**. Ở trình duyệt A, tải lại trang: thấy đúng như vậy.
- [x] Làm lại từ đầu, nhưng ở bước của B bấm "Huỷ" thay vì lưu lần nữa. Thẻ hoạt động của B hiện ngay ghi chú mới của A, không phải tải lại trang.
- [x] Làm lại, lần này B đổi giờ cho trùng với hoạt động khác. B nhận dòng báo vàng (không phải hộp "Trùng giờ"); bấm Lưu lần nữa thì mới hiện hộp "Trùng giờ với hoạt động khác"; "Vẫn lưu" thì lưu được.

### MT-RT-06 · Hai người cùng sửa chuyến đi (nghiệm thu Task 5.1, phần chuyến đi) · Đạt (2026-10-30)

Cần có: như `MT-RT-05` (chủ chuyến ở trình duyệt A, thành viên "Chỉnh sửa" ở trình duyệt B), chuyến đi có mô tả.

- [x] Cả hai bấm "Sửa" chuyến đi.
- [x] A đổi mô tả, bấm "Lưu thay đổi". Toast "Đã lưu thay đổi".
- [x] B đổi tên (không đụng mô tả), bấm "Lưu thay đổi". Form không đóng, tên vẫn còn, khung vàng: "Chuyến đi này vừa được người khác sửa. Bấm Lưu lần nữa để ghi đè bằng nội dung của bạn, hoặc Huỷ để xem bản mới."
- [x] B bấm "Lưu thay đổi" lần nữa. Chuyến đi có tên của B **và mô tả của A**.
- [x] Cả hai bấm "Sửa" lần nữa. Trong lúc B đang mở form, A đổi trạng thái chuyến đi (menu trạng thái ở đầu trang) rồi tạo một liên kết chia sẻ. B đổi tên, bấm Lưu: **lưu được ngay**, không có khung vàng.
- [x] Cả hai bấm "Sửa". A đổi tên, lưu. B rút ngắn chuyến đi sao cho một ngày có hoạt động bị cắt, bấm Lưu: B nhận khung vàng, **không** có hộp "Xoá hoạt động khi đổi ngày?", không ngày nào mất. B bấm Lưu lần nữa: lúc này hộp "Xoá hoạt động khi đổi ngày?" mới hiện; "Vẫn đổi ngày" thì lưu được.

## Lỗi đã phát hiện

| Mã lỗi | Test case | Ngày | Hiện tượng | Nguyên nhân | Cách sửa | Trạng thái |
|---|---|---|---|---|---|---|
| BUG-UI-014 | MT-RT-03 bước 3 | 2026-10-29 | Tìm ra khi đọc code lúc làm Mốc 5 (Task 5.1), chưa ai gặp trên trình duyệt. Người dùng mở "Sửa chuyến đi", chuyển sang tab khác, trong lúc đó người khác đổi tên chuyến đi; quay lại, chỉ sửa mô tả rồi lưu: tên chuyến đi bị đổi về tên cũ, việc của người kia mất mà không ai biết. Có từ Task 2.5 | Form so các ô với chuyến đi lấy từ bộ nhớ của trang, mà trang tự tải lại khi quay về tab. Ô tên trong form vẫn là tên cũ nên khác bản vừa tải, bị coi là "đã sửa" và gửi đi. Form hoạt động không bị: nó đã giữ bản chụp lúc mở từ trước | Form chụp lại chuyến đi lúc mở (`base` trong `EditTripDialog`) và so các ô, kiểu thay đổi ngày, cùng số phiên bản gửi đi với bản chụp đó | Đã sửa (`dc3c8f3`, Mốc 5). Chủ dự án đã kiểm lại bằng `MT-RT-03` bước 3 ngày 2026-10-30 |

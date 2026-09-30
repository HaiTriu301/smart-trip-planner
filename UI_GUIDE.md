# UI_GUIDE.md — Hệ thống giao diện Smart Trip Planner

> Tài liệu thiết kế giao diện. Dùng cùng `design.md` (kiến trúc) và `WORKFLOW.md` (lịch trình).
> Mọi màn hình frontend phải tuân theo token và quy tắc trong file này.

---

## 1. Ý tưởng chủ đạo

**"Bảng giờ tàu" (transit board).**

Sản phẩm này về bản chất là *thời gian* đặt cạnh *địa điểm*. Ngôn ngữ thị giác lấy từ bản đồ tàu điện và bảng giờ khởi hành ở nhà ga:

- Mỗi ngày trong chuyến đi là một **thanh ray dọc** chia theo giờ.
- Mỗi hoạt động là một **"ga"** gắn vào ray, có chấm màu theo loại hoạt động.
- Khoảng trống giữa hai ga là **đoạn nét đứt** hiển thị thời gian di chuyển.
- Màu của loại hoạt động dùng nhất quán ở **ba nơi**: chấm trên ray, viền trái của thẻ, marker trên bản đồ. Nhìn một lần là hiểu danh sách và bản đồ đang nói về cùng một thứ.

**Điểm bạo nhất chỉ ở một chỗ:** thanh ray thời gian. Mọi thứ còn lại giữ im lặng — nền phẳng, viền mảnh, bóng đổ rất nhẹ, không gradient trang trí.

**Ba thứ cố tình tránh** vì là mặc định phổ biến, không phải lựa chọn:
- Nền kem `#F4F1EA` + serif tương phản cao + cam đất `#D97757`
- Mọi khối nội dung bị cắt thành thẻ bo góc giống hệt nhau với cùng một bóng xám
- Nhãn chữ IN HOA giãn chữ đặt phía trên mỗi tiêu đề

---

## 2. Nguyên tắc

1. **Bản đồ và danh sách luôn đồng bộ.** Hover một hoạt động → marker tương ứng phóng to. Click marker → cuộn tới hoạt động đó. Không bao giờ để hai bên dùng màu khác nhau cho cùng một thứ.
2. **Thời gian là cấu trúc, không phải nhãn.** Giờ nằm trên ray, căn phải, dùng chữ số tabular để các cột số thẳng hàng.
3. **Màu mang thông tin.** Không dùng màu để trang trí. Mỗi màu trong hệ thống có một nghĩa cố định.
4. **Chuyển động chỉ trả lời hành động.** Kéo thả, mở dialog, xác nhận — có chuyển động. Không có hiệu ứng "trượt lên khi cuộn" cho từng khối.
5. **Trạng thái rỗng là lời mời, không phải thông báo lỗi.** Màn hình chưa có chuyến đi phải có nút tạo ngay ở đó.
6. **Người khác đang sửa phải thấy được.** Realtime là tính năng cốt lõi, giao diện phải thể hiện: avatar người đang xem, viền nhấp nháy khi ai đó vừa sửa một hoạt động.

---

## 3. Bảng màu

### 3.1. Màu nền tảng (6 giá trị)

| Tên | Hex | Dùng ở đâu |
|---|---|---|
| `ink` | `#10242B` | Chữ chính, thanh điều hướng trên cùng, nền footer |
| `paper` | `#F4F6F5` | Nền trang (hơi lạnh, không phải kem) |
| `surface` | `#FFFFFF` | Nền thẻ, panel, dialog |
| `jade` | `#0B7A6B` | Màu thương hiệu, nút chính, link, trạng thái đang chọn |
| `tide` | `#D7E4E1` | Viền, đường kẻ, nền nhạt của trạng thái hover |
| `sun` | `#E0A33C` | Thời tiết, huy hiệu Premium, cảnh báo nhẹ |

### 3.2. Thang xám (dẫn xuất từ `ink`, hơi ngả xanh)

```
gray-50   #F7F9F8      gray-500  #6B8085
gray-100  #EDF1F0      gray-600  #52666B
gray-200  #DCE4E2      gray-700  #3C4F55
gray-300  #C3CFCD      gray-800  #24383E
gray-400  #94A6A7      gray-900  #10242B
```

### 3.3. Màu ngữ nghĩa

| Ý nghĩa | Hex | Ghi chú |
|---|---|---|
| Thành công | `#1D7A4C` | Lưu xong, thanh toán thành công |
| Cảnh báo | `#E0A33C` | Thời tiết xấu, sắp hết hạn mức |
| Lỗi | `#C2453B` | Lỗi form, xoá, 4xx/5xx |
| Thông tin | `#2D6FA8` | Ghi chú, gợi ý |
| Premium | `#8A6A1F` chữ trên nền `#FBF1DC` | Huy hiệu gói Premium, không dùng gradient vàng |

### 3.4. Màu tuyến theo loại hoạt động ⭐

Đây là phần quan trọng nhất. Sáu màu này phải phân biệt được cả khi ở cạnh nhau trên bản đồ, và phân biệt được với người mù màu đỏ–lục (mỗi màu khác nhau về cả sắc độ lẫn độ sáng).

| Loại | Nhãn hiển thị | Hex | Icon (lucide) |
|---|---|---|---|
| `SIGHTSEEING` | Tham quan | `#2D7DD2` | `landmark` |
| `FOOD` | Ăn uống | `#E0662F` | `utensils` |
| `TRANSPORT` | Di chuyển | `#64797F` | `bus` |
| `ACCOMMODATION` | Lưu trú | `#7A5BA6` | `bed-double` |
| `SHOPPING` | Mua sắm | `#BE3C79` | `shopping-bag` |
| `OTHER` | Khác | `#4F8A62` | `map-pin` |

**Quy tắc dùng:** màu tuyến chỉ xuất hiện ở ba vị trí — chấm trên ray thời gian, viền trái 3px của thẻ hoạt động, marker trên bản đồ. Không tô nền thẻ bằng màu tuyến (sẽ thành cầu vồng, mất khả năng đọc).

### 3.5. Chế độ tối

| Token | Sáng | Tối |
|---|---|---|
| `--bg` | `#F4F6F5` | `#0C1B20` |
| `--surface` | `#FFFFFF` | `#14282E` |
| `--surface-raised` | `#FFFFFF` | `#1B333A` |
| `--text` | `#10242B` | `#E6EDEB` |
| `--text-muted` | `#52666B` | `#9AB0B2` |
| `--border` | `#D7E4E1` | `#27444B` |
| `--brand` | `#0B7A6B` | `#2FA894` (sáng hơn để đủ tương phản trên nền tối) |

Màu tuyến ở chế độ tối tăng độ sáng 12%, giữ nguyên sắc độ.

---

## 4. Chữ

### 4.1. Font

**Một họ chữ duy nhất: Be Vietnam Pro** (Google Fonts, weight 300–800).

Lý do chọn: được thiết kế riêng cho tiếng Việt, dấu thanh đặt đúng vị trí và không va vào chữ hoa — điều mà Inter, Roboto hay Poppins đều xử lý kém với các tổ hợp như "Ế", "Ợ", "Ỡ". Sản phẩm này hiển thị tên địa điểm tiếng Việt ở khắp nơi nên đây là lựa chọn kỹ thuật, không phải thẩm mỹ.

Không dùng font thứ hai. Phân cấp tạo bằng **độ đậm và kích thước**, không bằng cách đổi họ chữ.

```html
<link href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@300;400;500;600;700;800&display=swap" rel="stylesheet">
```

Fallback: `"Be Vietnam Pro", ui-sans-serif, system-ui, "Segoe UI", sans-serif`

### 4.2. Thang cỡ chữ

| Vai trò | Cỡ / dòng | Weight | Tracking |
|---|---|---|---|
| Display (chỉ trang landing) | 56 / 60 | 800 | -0.03em |
| H1 (tiêu đề trang) | 32 / 40 | 700 | -0.02em |
| H2 (tiêu đề khối) | 24 / 32 | 700 | -0.01em |
| H3 (tiêu đề thẻ) | 18 / 26 | 600 | 0 |
| Body | 15 / 24 | 400 | 0 |
| Body nhỏ | 13 / 20 | 400 | 0 |
| Nhãn form | 13 / 18 | 500 | 0 |
| Chú thích | 12 / 16 | 400 | 0.01em |
| **Giờ trên ray** | 13 / 16 | 600 | 0.02em, `font-variant-numeric: tabular-nums` |

Độ dài dòng tối đa **68 ký tự** cho đoạn văn (mô tả chuyến đi, ghi chú).

Không dùng chữ IN HOA cho nhãn. Dùng sentence case: "Ngày khởi hành", không phải "NGÀY KHỞI HÀNH".

---

## 5. Token bố cục

### 5.1. Khoảng cách (thang 4px)

```
space-1  4px     space-5  20px
space-2  8px     space-6  24px
space-3  12px    space-8  32px
space-4  16px    space-12 48px
                 space-16 64px
```

### 5.2. Bo góc — **khác nhau theo cấp bậc**, không dùng một giá trị cho tất cả

| Thành phần | Radius |
|---|---|
| Ô nhập, nút, badge | `6px` |
| Thẻ hoạt động, thẻ chuyến đi | `10px` |
| Dialog, panel lớn | `14px` |
| Avatar, chấm trên ray | `9999px` |
| Ảnh bìa chuyến đi | `10px 10px 0 0` |

### 5.3. Đổ bóng — rất nhẹ, ngả xanh theo `ink`

```css
--shadow-sm: 0 1px 2px rgba(16, 36, 43, .06);
--shadow-md: 0 2px 8px rgba(16, 36, 43, .08);
--shadow-lg: 0 8px 24px rgba(16, 36, 43, .12);   /* chỉ dialog và dropdown */
```

Thẻ ở trạng thái nghỉ dùng **viền `1px solid tide`**, không dùng bóng. Bóng chỉ xuất hiện khi phần tử nổi lên trên mặt phẳng khác (dialog, popover, thẻ đang được kéo).

### 5.4. Khung lưới

- Chiều rộng tối đa nội dung: `1280px`
- Padding ngang: `16px` (mobile) → `24px` (tablet) → `32px` (desktop)
- Khoảng cách giữa các khối: `32px`

---

## 6. File token — dán thẳng vào dự án

`frontend/src/styles/tokens.css` (Tailwind v4 dùng `@theme`):

```css
@import "tailwindcss";

@theme {
  /* Nền tảng */
  --color-ink: #10242B;
  --color-paper: #F4F6F5;
  --color-jade: #0B7A6B;
  --color-jade-dark: #095E52;
  --color-jade-light: #E6F2EF;
  --color-tide: #D7E4E1;
  --color-sun: #E0A33C;

  /* Xám */
  --color-gray-50:  #F7F9F8;
  --color-gray-100: #EDF1F0;
  --color-gray-200: #DCE4E2;
  --color-gray-300: #C3CFCD;
  --color-gray-400: #94A6A7;
  --color-gray-500: #6B8085;
  --color-gray-600: #52666B;
  --color-gray-700: #3C4F55;
  --color-gray-800: #24383E;
  --color-gray-900: #10242B;

  /* Ngữ nghĩa */
  --color-success: #1D7A4C;
  --color-warning: #E0A33C;
  --color-danger:  #C2453B;
  --color-info:    #2D6FA8;

  /* Màu tuyến hoạt động */
  --color-act-sightseeing:   #2D7DD2;
  --color-act-food:          #E0662F;
  --color-act-transport:     #64797F;
  --color-act-accommodation: #7A5BA6;
  --color-act-shopping:      #BE3C79;
  --color-act-other:         #4F8A62;

  /* Chữ */
  --font-sans: "Be Vietnam Pro", ui-sans-serif, system-ui, sans-serif;

  /* Bo góc */
  --radius-control: 6px;
  --radius-card: 10px;
  --radius-panel: 14px;

  /* Bóng */
  --shadow-sm: 0 1px 2px rgba(16, 36, 43, .06);
  --shadow-md: 0 2px 8px rgba(16, 36, 43, .08);
  --shadow-lg: 0 8px 24px rgba(16, 36, 43, .12);
}

:root {
  --bg: var(--color-paper);
  --surface: #FFFFFF;
  --text: var(--color-ink);
  --text-muted: var(--color-gray-600);
  --border: var(--color-tide);
  --brand: var(--color-jade);
}

[data-theme="dark"] {
  --bg: #0C1B20;
  --surface: #14282E;
  --text: #E6EDEB;
  --text-muted: #9AB0B2;
  --border: #27444B;
  --brand: #2FA894;
}

/* Số liệu thời gian luôn thẳng cột */
.tabular { font-variant-numeric: tabular-nums; }

/* Tôn trọng thiết lập giảm chuyển động của hệ điều hành */
@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    animation-duration: .01ms !important;
    transition-duration: .01ms !important;
  }
}
```

---

## 7. Thành phần

### 7.1. Nút

| Kiểu | Nền | Chữ | Viền | Dùng khi |
|---|---|---|---|---|
| Primary | `jade` | trắng | không | Hành động chính, mỗi màn hình **chỉ một** |
| Secondary | trắng | `gray-800` | `1px tide` | Hành động phụ |
| Ghost | trong suốt | `gray-600` | không | Hành động thứ yếu trong thẻ |
| Danger | trắng | `danger` | `1px danger` | Xoá. Chỉ nút trong dialog xác nhận mới tô nền đỏ |
| Premium | `#FBF1DC` | `#8A6A1F` | `1px #EBD9AE` | Nâng cấp gói |

Kích thước: cao `36px` (mặc định), `44px` (trên mobile và nút chính của form). Padding ngang `16px`. Vùng chạm tối thiểu `44×44px` trên mobile.

Chữ trên nút nói đúng việc sẽ xảy ra: "Lưu thay đổi", "Tạo chuyến đi", "Mời thành viên" — không dùng "Gửi", "OK", "Xác nhận" chung chung. Không thêm mũi tên `→` vào chữ trên nút.

Trạng thái loading: chữ giữ nguyên, thêm spinner 14px bên trái, nút bị disabled. Không đổi chữ thành "Đang xử lý...".

### 7.2. Ô nhập

```
Nhãn (13px, weight 500, gray-700)
┌──────────────────────────────────────┐   cao 40px
│ Giá trị (15px)                       │   viền 1px tide, radius 6px
└──────────────────────────────────────┘
Chú thích hoặc lỗi (12px)
```

- Focus: viền `jade` + ring `3px rgba(11,122,107,.15)`. Ring phải nhìn thấy được khi dùng bàn phím.
- Lỗi: viền `danger`, chữ lỗi màu `danger` phía dưới, kèm icon `alert-circle` 14px.
- Lỗi hiển thị **sau khi rời khỏi ô**, không hiện ngay lúc đang gõ.
- Trường bắt buộc đánh dấu bằng dấu `*` màu `danger` sau nhãn, không dùng chữ "(bắt buộc)".

### 7.3. Thẻ hoạt động — thành phần quan trọng nhất

```
┌─┬────────────────────────────────────────────────┐
│ │ 09:00 – 11:30        Tham quan          ⋮      │  ← viền trái 3px màu tuyến
│ │ Chùa Linh Ứng                                  │  ← 16px weight 600
│ │ 📍 Bãi Bụt, Sơn Trà, Đà Nẵng                   │  ← 13px gray-500
│ │ 🌤 28°C, ít mây          💰 0đ         👤 Trieu │  ← 12px, hàng meta
└─┴────────────────────────────────────────────────┘
```

- Nghỉ: nền trắng, viền `1px tide`, viền trái 3px màu tuyến.
- Hover: nền `gray-50`, hiện nút kéo (`grip-vertical`) ở mép trái và menu `⋮` ở mép phải.
- Đang kéo: `shadow-lg`, nghiêng `2deg`, opacity 0.9. Vị trí thả hiện một đường ngang màu `jade` dày 2px.
- Vừa được người khác sửa (realtime): viền ngoài nhấp nháy màu `jade` trong 1.2 giây rồi tắt, kèm chip nhỏ "Trieu vừa sửa" biến mất sau 3 giây.
- Trùng giờ với hoạt động khác: thêm dải nền `warning` mờ 8% và icon cảnh báo cạnh giờ.

### 7.4. Thanh ray thời gian

```
08:00 ─┬─
       │
09:00 ─●──── [Thẻ: Chùa Linh Ứng]
       │
       ┊ 25 phút · 8,4 km          ← nét đứt, chữ 12px gray-400
       │
12:00 ─●──── [Thẻ: Bún chả cá 109]
       │
```

- Ray: đường dọc `1px` màu `tide`, cách mép trái 48px.
- Chấm: 10px, nền trắng, viền 3px màu tuyến của hoạt động.
- Đoạn di chuyển: nét đứt `2px dashed gray-300`, chữ căn giữa.
- Khoảng trống trên 3 tiếng giữa hai hoạt động: hiện nút mờ "+ Thêm hoạt động vào khoảng này".

### 7.5. Huy hiệu (badge)

Cao 20px, radius 6px, chữ 12px weight 500, padding ngang 8px. Nền là màu gốc pha loãng 12%, chữ là màu gốc đậm.

Dùng cho: trạng thái chuyến đi (Nháp / Đã lên kế hoạch / Đang đi / Hoàn thành), vai trò thành viên (Chủ sở hữu / Chỉnh sửa / Chỉ xem), gói Premium.

### 7.6. Dialog

Rộng tối đa `480px` (form đơn giản) hoặc `640px` (form hoạt động). Radius 14px, `shadow-lg`, lớp phủ nền `rgba(16,36,43,.45)`.

Nút xếp ở góc phải dưới: hành động phụ bên trái, hành động chính bên phải. Trên mobile, dialog trượt lên từ đáy và chiếm tối đa 90% chiều cao.

### 7.7. Thông báo (toast)

Góc dưới bên phải trên desktop, trên cùng ở mobile. Rộng 360px, tự tắt sau 4 giây, có nút đóng. Thao tác xoá kèm nút "Hoàn tác" và giữ toast 8 giây.

Chữ trong toast dùng thể hoàn thành khớp với nút đã bấm: bấm "Lưu thay đổi" → toast "Đã lưu thay đổi".

### 7.8. Khung xương (skeleton)

Dùng khung xương cho danh sách chuyến đi và lịch trình, **không dùng spinner toàn trang**. Khối xám `gray-100`, hiệu ứng sáng chạy ngang chu kỳ 1.6 giây. Số lượng khung xương bằng số phần tử thường thấy (3 thẻ chuyến đi, 4 hoạt động).

Spinner chỉ dùng bên trong nút và khi tải bản đồ.

### 7.9. Trạng thái rỗng

Ba phần: hình minh hoạ đường nét đơn giản (không dùng ảnh 3D hay illustration nhiều màu), một câu nói rõ việc cần làm, một nút hành động.

| Màn hình | Câu chữ | Nút |
|---|---|---|
| Chưa có chuyến đi | "Chưa có chuyến đi nào. Tạo chuyến đầu tiên để bắt đầu lên lịch trình." | Tạo chuyến đi |
| Ngày chưa có hoạt động | "Ngày này còn trống. Thêm địa điểm bạn muốn ghé." | Thêm hoạt động |
| Chưa mời ai | "Chỉ mình bạn thấy chuyến đi này. Mời bạn bè để cùng chỉnh sửa." | Mời thành viên |
| Chưa có chi phí | "Chưa ghi khoản nào. Thêm chi phí để theo dõi ngân sách." | Thêm chi phí |

---

## 8. Bố cục từng màn hình

### 8.1. Chi tiết chuyến đi — màn hình trung tâm

```
┌──────────────────────────────────────────────────────────────────────┐
│ ← Đà Nẵng 4 ngày   [Đang đi]        👤👤👤  [Chia sẻ] [⋮]           │ 56px, nền ink
├────────────┬──────────────────────────────┬──────────────────────────┤
│ NGÀY       │  Thứ 5, 12/03 · Ngày 1       │                          │
│            │                              │                          │
│ ● Ngày 1   │  08:00 ─●── Chùa Linh Ứng    │      [ BẢN ĐỒ ]          │
│   12/03    │         ┊ 25 phút · 8,4 km   │                          │
│            │  12:00 ─●── Bún chả cá 109   │   marker theo màu tuyến  │
│   Ngày 2   │         ┊ 10 phút            │   đường nối theo thứ tự  │
│   13/03    │  14:30 ─●── Cầu Rồng         │                          │
│            │                              │   dính (sticky) khi cuộn │
│   Ngày 3   │  [+ Thêm hoạt động]          │                          │
│   14/03    │                              │                          │
│            │                              ├──────────────────────────┤
│ Tổng quan  │                              │ 🌤 28°C  ít mây          │
│ Chi phí    │                              │ Cảnh báo: mưa chiều 14/03│
│ Thành viên │                              │                          │
└────────────┴──────────────────────────────┴──────────────────────────┘
   200px                linh hoạt                      420px
```

- Cột trái: danh sách ngày + điều hướng phụ. Ngày đang chọn có thanh dọc `jade` 3px ở mép trái.
- Cột giữa: ray thời gian, cuộn độc lập.
- Cột phải: bản đồ dính, cao toàn màn hình trừ header; dải thời tiết nằm dưới bản đồ.
- Dưới 1024px: bản đồ chuyển thành tab ngang (Lịch trình / Bản đồ / Chi phí).
- Dưới 768px: cột ngày thành thanh chip cuộn ngang ở trên cùng.

### 8.2. Danh sách chuyến đi

Lưới thẻ 3 cột (desktop) / 2 (tablet) / 1 (mobile). Mỗi thẻ: ảnh bìa tỉ lệ 16:9, tên chuyến, khoảng ngày, badge trạng thái, avatar thành viên chồng nhau, và một dòng meta "4 ngày · 12 hoạt động".

Thanh lọc nằm ngang phía trên: ô tìm kiếm + chip trạng thái + sắp xếp. Không dùng sidebar lọc cho màn hình này.

### 8.3. Trang chuyến đi công khai (share link)

Bố cục một cột, rộng tối đa 760px, canh giữa. Không có thanh điều hướng của ứng dụng, chỉ có logo nhỏ và nút "Tạo chuyến đi của bạn" ở cuối. Bản đồ đặt dưới phần tóm tắt, không dính.

### 8.4. Landing

Hero **không** dùng khối chữ lớn với gradient. Thay vào đó: hiển thị một lịch trình thật đang chạy — thanh ray với ba hoạt động và bản đồ nhỏ bên cạnh, các chấm màu tuyến sáng dần theo thứ tự trong 2 giây khi trang load (đây là khoảnh khắc chuyển động duy nhất của cả trang). Tiêu đề đặt bên trái, ngắn, một câu.

### 8.5. Trang nâng cấp

Hai cột so sánh Free và Premium. Không dùng thẻ "phổ biến nhất" phóng to. Đánh dấu giới hạn đang chạm phải của người dùng bằng dòng highlight nền `warning` 8% — ví dụ khi họ đã có 3 chuyến đi, dòng "Số chuyến đi" được tô sáng.

---

## 9. Bản đồ và thời tiết

**Bản đồ:** dùng tile CartoDB Positron (xám nhạt, chữ mờ) thay vì OSM mặc định — OSM mặc định quá nhiều màu, marker sẽ chìm. Ở chế độ tối dùng CartoDB Dark Matter.

**Marker:** hình giọt nước 28px, nền màu tuyến, icon trắng bên trong, số thứ tự trong ngày ở góc. Marker đang hover phóng to 1.15 lần và có vòng sáng.

**Đường nối:** đường liền `2px` màu `jade` mờ 60%, nối các điểm theo đúng thứ tự trong ngày. Không vẽ đường giữa các ngày khác nhau.

**Thời tiết:** dải ngang dưới bản đồ, mỗi ngày một ô: icon + nhiệt độ cao/thấp + xác suất mưa. Ngày có cảnh báo hiện viền `warning`. Icon thời tiết dùng bộ nét đơn giản (lucide `cloud-rain`, `sun`, `cloud-sun`), không dùng icon màu đầy đặn.

---

## 10. Giọng văn

- Xưng hô: gọi người dùng là "bạn", ứng dụng không tự xưng.
- Câu chủ động, thì hiện tại: "Lưu thay đổi" chứ không "Thay đổi sẽ được lưu".
- Lỗi nói rõ chuyện gì xảy ra và cách sửa, không xin lỗi: "Ngày kết thúc phải sau ngày bắt đầu" — không phải "Rất tiếc, đã có lỗi xảy ra".
- Một hành động giữ nguyên tên qua cả luồng: nút "Mời" → dialog "Mời thành viên" → toast "Đã gửi lời mời".

Ví dụ thông báo lỗi khớp với mã lỗi backend:

| errorCode | Chữ hiển thị |
|---|---|
| `ACTIVITY_TIME_CONFLICT` | "Khung giờ này trùng với hoạt động khác trong ngày." |
| `QUOTA_EXCEEDED` | "Gói miễn phí cho phép tối đa 3 chuyến đi. Nâng cấp để tạo thêm." |
| `PREMIUM_REQUIRED` | "Tính năng này dành cho gói Premium." |
| `FORBIDDEN` | "Bạn chỉ có quyền xem chuyến đi này." |
| `STALE_VERSION` | "Người khác vừa sửa hoạt động này. Tải lại để xem bản mới nhất." |
| `RATE_LIMIT_EXCEEDED` | "Bạn thao tác hơi nhanh. Thử lại sau một phút." |

---

## 11. Điểm ngắt responsive

| Tên | Bề rộng | Thay đổi chính |
|---|---|---|
| mobile | < 640px | Một cột, ngày thành chip ngang, dialog trượt từ đáy, bản đồ trong tab riêng |
| tablet | 640–1023px | Hai cột, bản đồ trong tab |
| desktop | 1024–1279px | Ba cột, bản đồ 360px |
| wide | ≥ 1280px | Ba cột, bản đồ 420px, nội dung giới hạn 1280px |

Kéo thả trên mobile: dùng nút mũi tên lên/xuống trên mỗi thẻ thay cho kéo thả, vì kéo thả trên màn cảm ứng xung đột với thao tác cuộn.

---

## 12. Khả năng tiếp cận — kiểm tra trước khi merge

- [ ] Tương phản chữ thường ≥ 4.5:1, chữ lớn ≥ 3:1 (kiểm bằng DevTools). `jade #0B7A6B` trên trắng đạt 4.83:1.
- [ ] Không dùng riêng màu để truyền tin: loại hoạt động luôn có icon + chữ kèm màu.
- [ ] Mọi phần tử tương tác có viền focus nhìn thấy được khi dùng phím Tab.
- [ ] Dialog bẫy focus bên trong, đóng bằng phím Esc, trả focus về nút đã mở nó.
- [ ] Ảnh có `alt`, icon trang trí có `aria-hidden="true"`.
- [ ] Vùng chạm ≥ 44×44px trên mobile.
- [ ] Toast dùng `role="status"`, thông báo lỗi dùng `role="alert"`.
- [ ] `prefers-reduced-motion` được tôn trọng (đã có trong tokens.css).

---

## 13. Prompt cho Stitch

### 13.1. Cách dùng

Stitch làm tốt nhất khi mỗi lần chỉ dựng **một màn hình** và prompt nêu rõ màu hex, font, và danh sách thành phần. Quy trình:

1. Dán **đoạn mở đầu** ở mục 13.2 vào đầu mọi prompt.
2. Nối tiếp bằng prompt của màn hình cần dựng (13.3).
3. Dựng theo thứ tự: Đăng nhập → Danh sách chuyến đi → Chi tiết chuyến đi → còn lại. Màn chi tiết là khó nhất, đừng làm đầu tiên.
4. Sửa từng phần bằng câu ngắn: "làm cột bản đồ rộng 420px", "đổi viền trái thẻ thành 3px".
5. Xuất sang Figma, rồi lấy CSS/khoảng cách làm tham chiếu khi code React. **Không dán thẳng code Stitch vào dự án** — nó không dùng token của bạn và không có logic.

### 13.2. Đoạn mở đầu (dán trước mọi prompt)

```
Design a web app screen for "Smart Trip Planner", a travel itinerary planner for
Vietnamese users. Desktop 1440px wide.

Visual direction: transit timetable. Time is the structural spine of the layout.
Clean, flat, functional. No gradients, no decorative shadows, no glassmorphism.

Typography: Be Vietnam Pro only. Sentence case, never all caps labels.
H1 32px/700, H2 24px/700, card title 18px/600, body 15px/400, caption 12px/400.

Colors:
- page background #F4F6F5
- card surface #FFFFFF
- text #10242B, muted text #52666B
- border #D7E4E1 (1px)
- brand green #0B7A6B for primary buttons and links
- amber #E0A33C for weather and premium
- red #C2453B for errors
Activity type colors: sightseeing #2D7DD2, food #E0662F, transport #64797F,
accommodation #7A5BA6, shopping #BE3C79, other #4F8A62.

Corner radius by hierarchy: inputs and buttons 6px, cards 10px, dialogs 14px.
Cards use a 1px border, not a drop shadow.
All interface text in Vietnamese.
```

### 13.3. Prompt từng màn hình

**Đăng nhập**
```
Centered login card, 400px wide, on the page background. Above the card: a small
wordmark "Smart Trip Planner" with a simple pin-and-route mark.
Card contains: heading "Đăng nhập", email field labeled "Email", password field
labeled "Mật khẩu" with a show/hide eye icon, a "Quên mật khẩu?" text link aligned
right, a full-width green primary button "Đăng nhập", and below the card the line
"Chưa có tài khoản? Đăng ký".
Fields are 40px tall with 6px radius and a 1px #D7E4E1 border. Show one field in
its focus state with a green border and a soft green focus ring.
```

**Danh sách chuyến đi**
```
Top navigation bar 56px tall, background #10242B, white wordmark on the left,
search field in the middle, notification bell and user avatar on the right.

Page heading "Chuyến đi của bạn" with a green primary button "Tạo chuyến đi" on
the right. Below: a filter row with status chips (Tất cả, Nháp, Đã lên kế hoạch,
Đang đi, Hoàn thành) and a sort dropdown.

A 3-column grid of 6 trip cards. Each card: 16:9 cover photo of a Vietnamese
destination with 10px top corners, then trip title 18px/600, date range in muted
text, a status badge, a row of 3 overlapping member avatars, and a meta line
"4 ngày · 12 hoạt động".
```

**Chi tiết chuyến đi — màn hình chính**
```
Three-column layout below a 56px dark top bar. The top bar shows a back arrow,
trip title "Đà Nẵng 4 ngày", a status badge, three overlapping member avatars on
the right, a "Chia sẻ" secondary button and a kebab menu.

LEFT COLUMN, 200px: a vertical list of days — "Ngày 1 / Thứ 5, 12/03" through
"Ngày 4". The selected day has a 3px green bar on its left edge and a light green
background. Below the days, secondary links: Tổng quan, Chi phí, Thành viên.

MIDDLE COLUMN, flexible: heading "Thứ 5, 12/03 · Ngày 1". Then a vertical timeline
rail — a 1px grey vertical line 48px from the left edge, with times in bold
tabular figures on its left. At each activity a 10px white dot with a 3px colored
ring sits on the rail, connecting to an activity card on the right.

Each activity card: 1px border, 10px radius, and a 3px colored left edge matching
the activity type. Inside: time range "09:00 – 11:30" and type label on the first
row, place name 16px/600 on the second, address with a pin icon in muted text on
the third, and a meta row with a weather chip and a cost.

Between two cards, a dashed vertical segment with centered caption "25 phút · 8,4 km".
Show 3 activities: a blue sightseeing one, an orange food one, a grey transport one.
End with a dashed ghost button "+ Thêm hoạt động".

RIGHT COLUMN, 420px: a light grey street map filling the height, with three
teardrop markers colored to match the activities and a thin green line connecting
them in order. Beneath the map, a weather strip of four day cells, each with a
line icon, high/low temperature and rain chance; one cell has an amber border.
```

**Dialog thêm hoạt động**
```
A modal dialog 640px wide, 14px radius, over a dark translucent overlay.
Title "Thêm hoạt động", close icon top right.
Fields: place search input with a magnifier icon and a dropdown of 3 suggestion
rows (each with place name, address, and a small category icon); a 6-option
segmented control for activity type, each option showing its icon and color;
two time inputs side by side labeled "Bắt đầu" and "Kết thúc"; a cost input with
"đ" suffix; a notes textarea.
Footer right-aligned: ghost button "Huỷ" and green primary button "Thêm hoạt động".
```

**Chia sẻ và thành viên**
```
A panel titled "Chia sẻ chuyến đi".
Section one: an email input with a role dropdown (Chỉnh sửa / Chỉ xem) and a
green "Mời" button. Below, a list of 3 members — avatar, name, email, a role
dropdown, and a remove icon. The owner row shows a "Chủ sở hữu" badge and no
dropdown.
Section two, separated by a 1px divider: "Liên kết công khai" with a toggle, a
read-only URL field with a copy button, an expiry date picker, and a small muted
line "Ai có liên kết đều xem được chuyến đi này."
```

**Trang nâng cấp Premium**
```
Page heading "Nâng cấp tài khoản". Two comparison cards side by side, equal size,
neither enlarged. Left card "Miễn phí" with price "0đ". Right card "Premium" with
"99.000đ / tháng", a 1px amber border and a small amber "Premium" badge.
Below each price, a feature list comparing: số chuyến đi, hoạt động mỗi ngày,
thành viên mời được, gợi ý lịch trình bằng AI, xuất file PDF, cảnh báo thời tiết.
Use a check icon for included and a dash for not included.
In the free card, highlight the "Số chuyến đi: 3" row with a soft amber background
to show the user has reached that limit.
Buttons: "Gói hiện tại" disabled on the left, green "Nâng cấp ngay" on the right.
```

**Trạng thái rỗng**
```
An empty state centered in the trips page content area: a simple two-color line
illustration of a folded map with a route on it, about 160px wide, then the line
"Chưa có chuyến đi nào. Tạo chuyến đầu tiên để bắt đầu lên lịch trình." in muted
text, then a green primary button "Tạo chuyến đi". No card, no border around it.
```

**Mobile — chi tiết chuyến đi**
```
Same trip detail screen at 390px wide. Days become a horizontally scrollable chip
row under the header. Below it, three tabs: Lịch trình, Bản đồ, Chi phí, with
Lịch trình active. The timeline rail sits at 36px from the left edge. Activity
cards are full width with the 3px colored left edge kept. A floating green circular
button with a plus icon sits at the bottom right, 56px diameter.
```

### 13.4. Điều cần sửa lại sau khi Stitch trả kết quả

Stitch hay mắc bốn lỗi này, kiểm và sửa ngay:

1. Tự thêm gradient hoặc bóng đổ đậm → yêu cầu "remove all shadows, use 1px borders only".
2. Dùng cùng một radius cho mọi thứ → yêu cầu "inputs 6px, cards 10px, dialog 14px".
3. Đặt nhãn IN HOA → yêu cầu "sentence case labels, no uppercase".
4. Bỏ dấu tiếng Việt hoặc dùng font không có dấu → yêu cầu "Be Vietnam Pro, all text in Vietnamese with full diacritics".

---

## 14. Thứ tự dựng giao diện

Khớp với WORKFLOW:

| Task | Màn hình cần có |
|---|---|
| 1.5 | Đăng nhập, đăng ký, quên mật khẩu, xác thực email |
| 2.5 | Danh sách chuyến đi, wizard tạo chuyến, chi tiết chuyến đi (ray thời gian, chưa có bản đồ) |
| 3.4 | Bản đồ + dải thời tiết trong cột phải |
| 4.4 | Panel chia sẻ, danh sách thành viên, trang công khai, luồng bình luận |
| 5.3 | Avatar người đang xem, hiệu ứng nhấp nháy khi người khác sửa |
| 6.4 | Trang nâng cấp, modal chạm hạn mức, trang kết quả thanh toán |
| 7.x | Trang chi phí (biểu đồ), màn gợi ý AI |
| 8.2 | Dashboard admin |
| 8.5 | Landing page |

**Làm landing page cuối cùng.** Lúc đó bạn đã có ảnh chụp màn hình thật của sản phẩm để đưa vào, thay vì phải vẽ mockup giả.

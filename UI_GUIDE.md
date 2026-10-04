# UI_GUIDE.md — Hệ thống giao diện Smart Trip Planner

> Tài liệu thiết kế giao diện. Dùng cùng `design.md` (kiến trúc, API) và `WORKFLOW.md` (lịch trình).
> Mọi màn hình frontend phải dùng token và thành phần trong file này.
>
> **Cập nhật 2026-10-01** theo code thật của Task 2.6 (nhánh `feat/T2.6-ui-guide`). Mỗi mục ghi rõ:
> **Đã làm** · **Phase N** (làm cùng tính năng của phase đó) · **Chưa áp dụng** (đã chốt hướng, chưa làm).

---

## 1. Ý tưởng chủ đạo

**"Bảng giờ tàu" (transit board).** Sản phẩm này về bản chất là *thời gian* đặt cạnh *địa điểm*. Ngôn ngữ thị giác lấy từ bản đồ tàu điện và bảng giờ khởi hành ở nhà ga:

- Mỗi ngày trong chuyến đi là một **thanh ray dọc**. **Đã làm**
- Mỗi hoạt động là một **"ga"** gắn vào ray, có chấm màu theo loại hoạt động. **Đã làm**
- Khoảng giữa hai ga là **đoạn nét đứt** ghi thời gian và quãng đường di chuyển. **Phase 3** (cần API lộ trình)
- Màu của loại hoạt động dùng nhất quán ở **ba nơi**: chấm trên ray, viền trái của thẻ, marker trên bản đồ. Hai nơi đầu **Đã làm**, marker **Phase 3**.
- Thanh bước của wizard tạo chuyến đi cũng là một tuyến tàu ngắn (các bước là "ga"). **Đã làm**

**Điểm bạo nhất chỉ ở một chỗ:** thanh ray thời gian. Mọi thứ còn lại giữ im lặng: nền phẳng, viền mảnh, bóng rất nhẹ, không gradient trang trí.

**Ba thứ cố tình tránh**, vì là mặc định phổ biến chứ không phải lựa chọn:
- Nền kem `#F4F1EA` + serif tương phản cao + cam đất `#D97757`
- Mọi khối nội dung bị cắt thành thẻ bo góc giống hệt nhau với cùng một bóng xám (các ngày của lịch trình được ngăn bằng **đường kẻ mảnh**, không bọc khung)
- Nhãn chữ IN HOA giãn chữ đặt phía trên tiêu đề

---

## 2. Nguyên tắc

1. **Bản đồ và danh sách luôn đồng bộ.** Rê chuột lên hoạt động thì marker phóng to; bấm marker thì cuộn tới hoạt động. Hai bên không bao giờ dùng màu khác nhau cho cùng một thứ. **Phase 3**
2. **Thời gian là cấu trúc, không phải nhãn.** Giờ nằm trên ray, căn phải, chữ số đều cột (`tabular-nums`). **Đã làm**
3. **Màu mang thông tin.** Mỗi màu có một nghĩa cố định; không dùng màu để trang trí.
4. **Chuyển động chỉ trả lời hành động.** Có chuyển động khi kéo thả, mở hộp thoại, hiện thông báo. Không có hiệu ứng "trượt lên khi cuộn", không có chấm nhấp nháy liên tục (chấm "Đang diễn ra" đứng yên). Khung xương là ngoại lệ duy nhất, vì nó báo đang tải.
5. **Trạng thái rỗng là lời mời, không phải thông báo lỗi.** Màn hình trống luôn có nút làm việc tiếp theo ngay tại chỗ. **Đã làm**
6. **Không đặt chỗ giữ trước cho chức năng chưa có.** Không vẽ chuông thông báo, ảnh thành viên, số liệu thống kê hay tab trống khi hệ thống chưa có dữ liệu thật cho chúng.
7. **Người khác đang sửa phải thấy được**: ảnh người đang xem, viền nhấp nháy khi ai đó vừa sửa một hoạt động. **Phase 5**

---

## 3. Bảng màu

Mọi màu nằm trong `frontend/src/styles/tokens.css` (mục 6). **Không dùng** màu mặc định của Tailwind như `sky-*`, `slate-*`, `red-*` trong code; chỉ dùng token.

### 3.1. Màu nền tảng

| Token | Hex | Dùng ở đâu |
|---|---|---|
| `ink` | `#10242B` | Chữ chính, thanh điều hướng, chân trang, chip đang chọn |
| `paper` | `#F4F6F5` | Nền trang (hơi lạnh, không phải kem) |
| `white` | `#FFFFFF` | Nền thẻ, ô nhập, hộp thoại |
| `jade` | `#0B7A6B` | Màu thương hiệu: nút chính, link, vòng focus, trạng thái đang chọn |
| `jade-dark` | `#095E52` | Nút chính khi rê chuột, chữ trên nền `jade-light` |
| `jade-light` | `#E6F2EF` | Nền nhạt của mục đang chọn (ngày trong cột trái) |
| `tide` | `#D7E4E1` | Viền, đường kẻ, đường ray |
| `sun` | `#E0A33C` | Thời tiết, huy hiệu Premium |

### 3.2. Thang xám (dẫn xuất từ `ink`, hơi ngả xanh)

Thay thế hẳn thang `gray-*` của Tailwind.

```
gray-50   #F7F9F8      gray-500  #6B8085
gray-100  #EDF1F0      gray-600  #52666B   ← chữ phụ
gray-200  #DCE4E2      gray-700  #3C4F55   ← nhãn ô nhập
gray-300  #C3CFCD      gray-800  #24383E
gray-400  #94A6A7      gray-900  #10242B
```

### 3.3. Màu ngữ nghĩa

| Token | Hex | Ghi chú |
|---|---|---|
| `success` | `#1D7A4C` | Đã lưu, trạng thái "Đã hoàn thành" |
| `warning` | `#E0A33C` | Trùng giờ, thời tiết xấu, sắp hết hạn mức. Chữ trên nền `warning` nhạt luôn để màu tối (vàng không đủ tương phản) |
| `danger` | `#C2453B` | Lỗi form, xoá, lỗi 4xx/5xx |
| `info` | `#2D6FA8` | Ghi chú, trạng thái "Đã lên kế hoạch" |
| Premium | chữ `#8A6A1F` trên nền `#FBF1DC` | Huy hiệu gói Premium, không dùng gradient vàng. **Phase 6** |

### 3.4. Màu tuyến theo loại hoạt động ⭐

Sáu màu này phải phân biệt được khi đứng cạnh nhau trên bản đồ, và phân biệt được với người mù màu đỏ–lục (khác nhau cả về sắc độ lẫn độ sáng).

| Loại | Nhãn | Token | Hex | Icon (lucide) |
|---|---|---|---|---|
| `SIGHTSEEING` | Tham quan | `act-sightseeing` | `#2D7DD2` | `Landmark` |
| `FOOD` | Ăn uống | `act-food` | `#E0662F` | `Utensils` |
| `TRANSPORT` | Di chuyển | `act-transport` | `#64797F` | `Bus` |
| `ACCOMMODATION` | Lưu trú | `act-accommodation` | `#7A5BA6` | `BedDouble` |
| `SHOPPING` | Mua sắm | `act-shopping` | `#BE3C79` | `ShoppingBag` |
| `OTHER` | Khác | `act-other` | `#4F8A62` | `MapPin` |

**Quy tắc:** màu tuyến chỉ xuất hiện ở chấm trên ray, viền trái 3px của thẻ hoạt động, marker trên bản đồ, icon của gợi ý địa điểm (mục 7.12), nút đang chọn của ô "Loại" (viền và nền mờ 8%, mục 7.13), cùng icon và chữ tên loại. Không tô nền thẻ bằng màu tuyến. Code nằm ở `features/itinerary/activityType.ts` (`ACTIVITY_ROUTE`); tên lớp được viết đầy đủ để Tailwind tìm thấy.

### 3.5. Màu trạng thái chuyến đi

Nhãn giữ theo code (không theo mockup).

| Trạng thái | Nhãn | Tông huy hiệu |
|---|---|---|
| `DRAFT` | Nháp | `neutral` (xám) |
| `PLANNED` | Đã lên kế hoạch | `info` |
| `ONGOING` | Đang diễn ra | `brand` (jade). Chip lọc có chấm jade đứng yên |
| `COMPLETED` | Đã hoàn thành | `success` |
| `ARCHIVED` | Đã lưu trữ | `muted` (xám nhạt) |

### 3.6. Chế độ tối — **Chưa áp dụng**

Biến `--bg`, `--surface`, `--text`, `--text-muted`, `--border`, `--brand` đã khai báo sẵn cho cả hai chế độ trong `tokens.css`, nhưng chưa có nút chuyển và các thành phần vẫn dùng token sáng trực tiếp. Khi làm: kiểm tra lại độ tương phản trên mọi màn hình; màu tuyến ở chế độ tối tăng độ sáng 12%, giữ nguyên sắc độ.

| Biến | Sáng | Tối |
|---|---|---|
| `--bg` | `#F4F6F5` | `#0C1B20` |
| `--surface` | `#FFFFFF` | `#14282E` |
| `--text` | `#10242B` | `#E6EDEB` |
| `--text-muted` | `#52666B` | `#9AB0B2` |
| `--border` | `#D7E4E1` | `#27444B` |
| `--brand` | `#0B7A6B` | `#2FA894` |

---

## 4. Chữ

### 4.1. Font

**Một họ chữ duy nhất: Be Vietnam Pro** (Google Fonts, weight 300–800), nạp bằng thẻ `<link>` trong `frontend/index.html`.

Lý do: được thiết kế riêng cho tiếng Việt, dấu thanh đặt đúng vị trí và không va vào chữ hoa ("Ế", "Ợ", "Ỡ"). Sản phẩm hiển thị tên địa điểm tiếng Việt ở khắp nơi, nên đây là lựa chọn kỹ thuật.

Không dùng font thứ hai. Phân cấp bằng **độ đậm và kích thước**. Fallback: `"Be Vietnam Pro", ui-sans-serif, system-ui, "Segoe UI", sans-serif`.

### 4.2. Thang cỡ chữ

| Vai trò | Cỡ / dòng | Weight | Tracking | Dùng ở |
|---|---|---|---|---|
| Display | 56 / 60 | 800 | -0.03em | Trang landing (**Phase 8**) |
| H1 | 32 / 40 | 700 | -0.02em | Tiêu đề trang: danh sách, chi tiết, tạo chuyến đi |
| H1 trang đăng nhập | 28 / 36 | 700 | -0.02em | Tiêu đề trong khung đăng nhập |
| Tiêu đề ngày, hộp thoại, thẻ chuyến đi | 18 / 26 | 600 | 0 | |
| Tên hoạt động | 16 / 24 | 600 | 0 | |
| Body | 15 / 24 | 400 | 0 | Mặc định của `body` |
| Body nhỏ | 13 / 20 | 400 | 0 | Ghi chú hoạt động, chip |
| Nhãn form | 13 / 18 | 500 | 0 | |
| Chú thích, gợi ý, lỗi dưới ô | 12 / 16 | 400 | 0 | |
| **Giờ trên ray** | 13 / 16 | 600 | 0.02em, `tabular-nums` | |

- Đoạn văn (mô tả chuyến đi, ghi chú) rộng tối đa **68 ký tự** (`max-w-[68ch]`).
- Không dùng chữ IN HOA cho nhãn. Dùng sentence case: "Ngày khởi hành", không phải "NGÀY KHỞI HÀNH".
- Số liệu (ngày, giờ, tiền, số đếm) dùng lớp `tabular` để thẳng cột.

---

## 5. Token bố cục

### 5.1. Khoảng cách

Thang 4px của Tailwind (`1` = 4px): `space-1` 4px, `2` 8px, `3` 12px, `4` 16px, `5` 20px, `6` 24px, `8` 32px, `12` 48px, `16` 64px.

### 5.2. Bo góc — **khác nhau theo cấp bậc**

| Thành phần | Token | Giá trị |
|---|---|---|
| Ô nhập, nút, chip, huy hiệu, menu | `rounded-control` | 6px |
| Thẻ hoạt động, thẻ chuyến đi, khung trang đăng nhập, khung báo | `rounded-card` | 10px |
| Hộp thoại | `rounded-panel` | 14px (trên điện thoại chỉ bo hai góc trên) |
| Ảnh đại diện, chấm trên ray, ô số đếm | `rounded-full` | 9999px |

### 5.3. Đổ bóng — rất nhẹ, ngả xanh theo `ink`

```css
--shadow-sm: 0 1px 2px rgba(16, 36, 43, .06);
--shadow-md: 0 2px 8px rgba(16, 36, 43, .08);   /* menu thả xuống, thông báo */
--shadow-lg: 0 8px 24px rgba(16, 36, 43, .12);  /* hộp thoại, thẻ đang được kéo */
```

Thẻ ở trạng thái nghỉ dùng **viền `1px tide`**, không dùng bóng. Bóng chỉ xuất hiện khi phần tử nổi lên trên mặt phẳng khác.

### 5.4. Khung trang

- Nội dung rộng tối đa `1280px`.
- Lề ngang: `16px` (điện thoại) → `24px` (≥ 640px) → `32px` (≥ 1024px).
- Khoảng cách giữa các khối lớn: `24–32px`.

---

## 6. File token

`frontend/src/styles/tokens.css`, được nạp từ `frontend/src/index.css` ngay sau `@import "tailwindcss"`. Tailwind v4 biến mỗi biến trong `@theme` thành lớp tiện ích: `--color-jade` thành `bg-jade` / `text-jade` / `border-jade`; `--radius-card` thành `rounded-card`; `--animate-shimmer` thành `animate-shimmer`.

```css
@theme {
  /* Nền tảng */
  --color-ink: #10242B;  --color-paper: #F4F6F5;
  --color-jade: #0B7A6B; --color-jade-dark: #095E52; --color-jade-light: #E6F2EF;
  --color-tide: #D7E4E1; --color-sun: #E0A33C;

  /* Xám (thay thang gray của Tailwind) */
  --color-gray-50: #F7F9F8;  /* … tới */ --color-gray-900: #10242B;

  /* Ngữ nghĩa */
  --color-success: #1D7A4C; --color-warning: #E0A33C; --color-danger: #C2453B; --color-info: #2D6FA8;

  /* Màu tuyến hoạt động */
  --color-act-sightseeing: #2D7DD2; --color-act-food: #E0662F; --color-act-transport: #64797F;
  --color-act-accommodation: #7A5BA6; --color-act-shopping: #BE3C79; --color-act-other: #4F8A62;

  --font-sans: "Be Vietnam Pro", ui-sans-serif, system-ui, "Segoe UI", sans-serif;
  --radius-control: 6px; --radius-card: 10px; --radius-panel: 14px;
  --shadow-sm: …; --shadow-md: …; --shadow-lg: …;

  /* Khung xương: dải sáng chạy ngang mỗi 1,6 giây */
  --animate-shimmer: shimmer 1.6s linear infinite;
  @keyframes shimmer { from { background-position: 200% 0; } to { background-position: -200% 0; } }
}

:root { --bg: …; --surface: …; --text: …; --text-muted: …; --border: …; --brand: …; }
[data-theme="dark"] { /* mục 3.6, chưa dùng */ }

.tabular { font-variant-numeric: tabular-nums; }

@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after { animation-duration: .01ms !important; transition-duration: .01ms !important; }
}
```

File thật là nguồn chính xác; khối trên chỉ tóm tắt.

---

## 7. Thành phần

Tất cả **tự viết** trong `frontend/src/components/`, không dùng shadcn/ui (chốt ở Task 2.5). Thư viện được phép:

| Thư viện | Dùng cho |
|---|---|
| `lucide-react` | Mọi icon. Icon trang trí luôn có `aria-hidden` |
| `@radix-ui/react-dropdown-menu` | Menu "⋮" (bàn phím, focus, trình đọc màn hình); giao diện dùng token của dự án |
| `@dnd-kit/core`, `@dnd-kit/sortable` | Kéo thả hoạt động |

### 7.0. Quy tắc bắt buộc khi viết giao diện

1. **Không ghi đè lớp của component bằng một lớp cùng thuộc tính qua `className`** (ví dụ `w-auto` đè `w-full`, `px-2` đè `px-4`, `hover:text-red-700` đè `hover:text-gray-900`). Tailwind không đảm bảo lớp nào thắng (BUG-UI-001). Thuộc tính thay đổi theo chỗ dùng phải là **prop**: `variant`, `size`, `fullWidth`.
2. Viền khác màu ở một cạnh thì ghi riêng từng cạnh (`border-y border-r border-l-[3px] border-y-tide border-r-tide border-l-act-food`), không dùng `border` rồi đè cạnh trái.
3. Chỉ dùng token (mục 3); không dùng màu mặc định của Tailwind.
4. Mỗi màn hình chỉ có **một** nút `primary`.
5. Chữ trên nút nói đúng việc sẽ xảy ra: "Lưu thay đổi", "Thêm hoạt động", "Tạo chuyến đi", "Xoá chuyến đi". Không dùng "Gửi", "OK", "Xác nhận", "Lưu" trống không; không thêm mũi tên `→` vào chữ.

### 7.1. Nút — `Button`, `LinkButton`

| `variant` | Giao diện | Dùng khi |
|---|---|---|
| `primary` | nền `jade`, chữ trắng | Hành động chính của màn hình |
| `secondary` | nền trắng, viền `tide`, chữ `gray-800` | Hành động phụ, "Huỷ" |
| `ghost` | trong suốt, chữ `gray-600` | Hành động thứ yếu lặp lại nhiều lần ("Sửa" ngày) |
| `ghost-inverse` | trong suốt, chữ `gray-300` | Trên thanh điều hướng tối ("Đăng xuất") |
| `danger` | nền trắng, viền + chữ `danger` | Nút "Xoá" bên ngoài hộp xác nhận |
| `danger-solid` | nền `danger`, chữ trắng | **Chỉ** nút xác nhận trong hộp xoá (`ConfirmDialog variant="danger"` tự chọn) |

| `size` | Cao | Dùng khi |
|---|---|---|
| `sm` | 32px | Nút nhỏ trong thẻ, trong tiêu đề ngày |
| `md` (mặc định) | 36px, **44px trên điện thoại** | Mọi nút thường |
| `lg` | 44px | Nút gửi chính của form đăng nhập / đăng ký / đặt lại mật khẩu |

- `fullWidth` (mặc định `true`) cho nút rộng hết khung trong form; `false` cho nút vừa chữ.
- Đang xử lý (`isLoading`): **giữ nguyên chữ**, thêm vòng quay 14px bên trái, nút bị khoá, có `aria-busy`.
- Vòng focus: `3px jade/25`, chỉ hiện khi dùng bàn phím (`focus-visible`).
- `LinkButton`: link của React Router trông như nút (ví dụ "Tạo chuyến đi"), để mở được trong tab mới.

### 7.2. Ô nhập — `FormField`, `SelectField`, `TextAreaField`, `PasswordField`

```
Nhãn * (13px, 500, gray-700)                 [hành động bên phải, vd "Quên mật khẩu?"]
┌──────────────────────────────────────┐    cao 40px (ô một dòng và ô chọn)
│ Giá trị (15px)                       │    viền 1px tide, bo 6px, nền trắng
└──────────────────────────────────────┘
Gợi ý (12px, gray-500)  hoặc  ⓘ Lỗi (12px, danger, icon 14px)
```

- Khung chung: `FieldShell` (nhãn, dấu `*`, `hint`, lỗi, `labelAction`). Kiểu ô: `controlClass()` trong `fieldStyles.ts`. **`controlClass` không chứa độ rộng và lề ngang**; mỗi ô tự thêm `w-full px-3` (hoặc `pl-9` khi có icon bên trái).
- Rê chuột: viền `gray-300`. Focus: viền `jade` + vòng `3px jade/15`. Lỗi: viền `danger`.
- **Ô bắt buộc** có dấu `*` màu `danger` sau nhãn và thuộc tính `required`. Ô tuỳ chọn không đánh dấu; không dùng chữ "(bắt buộc)" hay "(không bắt buộc)".
- Giới hạn độ dài và hướng dẫn nằm ở dòng **gợi ý dưới ô** ("Tối đa 255 ký tự"). Khi có lỗi, dòng lỗi thay chỗ dòng gợi ý; `aria-describedby` trỏ tới dòng đang hiện.
- **Lỗi hiện khi rời ô** (`useForm({ mode: 'onTouched' })`), sau đó cập nhật theo từng phím gõ. Bấm nút gửi thì kiểm tra tất cả.
- Mọi `<form>` có `noValidate` để trình duyệt không hiện bong bóng lỗi riêng.
- `PasswordField`: nút con mắt ở cuối ô để hiện/ẩn mật khẩu (`aria-pressed`, nhãn "Hiện mật khẩu" / "Ẩn mật khẩu").

### 7.3. Thẻ hoạt động — thành phần quan trọng nhất ⭐ `ActivityCard`

```
┌─┬───────────────────────────────────────────────────────┐
│ │ ⠿  09:00 – 11:30   🏛 Tham quan                    ⋮   │ ← viền trái 3px màu tuyến
│ │    Chùa Linh Ứng                                      │ ← 16px / 600
│ │    📍 Đường Hoàng Sa, Phường Sơn Trà, Đà Nẵng         │ ← địa điểm 13px, khi có
│ │    Đi sớm tránh nắng… Đọc thêm                        │ ← ghi chú 13px, tối đa 2 dòng
│ │    👛 350.000 ₫    Link đặt chỗ ↗                     │ ← 12px, hàng meta
└─┴───────────────────────────────────────────────────────┘
```

- **Nghỉ:** nền trắng, viền `tide` 1px ở ba cạnh, viền trái 3px màu tuyến.
- **Rê chuột hoặc Tab tới thẻ:** nền `gray-50`; hiện tay nắm kéo (`GripVertical`) ở mép trái và menu "⋮" ở mép phải. **Màn hình cảm ứng:** hai nút này luôn hiện.
- **Menu "⋮"** (`ActivityMenu`, Radix): "Sửa", "Chuyển sang ngày…" (không có khi chuyến đi chỉ có 1 ngày), "Xoá" (chữ đỏ). Dùng được bằng bàn phím: Enter mở, mũi tên chọn, Esc đóng.
- **Đang kéo:** thẻ bay theo con trỏ có `shadow-lg`, nghiêng 2°, trong suốt 90%. Vị trí sẽ thả hiện một **đường ngang jade 2px**.
- **Trùng giờ** với hoạt động khác trong ngày (đã lưu bằng "Vẫn lưu"): nền `warning` mờ 8% và icon tam giác cảnh báo cạnh giờ. Tính ở giao diện bằng `lib/timeOverlap.ts`, cùng quy tắc với backend (chạm đầu nhau không tính).
- Không có giờ: dòng giờ ghi "Chưa đặt giờ".
- **Hàng địa điểm** (Task 3.6, **Đã làm**): ngay dưới tên, icon ghim `MapPin` xám + chữ 13px `gray-600`, được xuống dòng. Nội dung: "Tên địa điểm · địa chỉ". Tên hoạt động trùng tên địa điểm (không phân biệt hoa thường) thì chỉ ghi địa chỉ, để không lặp chữ; địa điểm không có địa chỉ thì ghi tên. Hoạt động chưa gắn địa điểm: **không có hàng này**, không ghi "Chưa gắn địa điểm" (mockup Stitch có, đã bỏ vì lặp trên mọi thẻ).
- Ô thời tiết trong hàng meta: hoãn ngày 2026-10-02 cùng cảnh báo ngoài trời (design rule 14.21).
- **Phase 5:** người tạo trong hàng meta; khi người khác vừa sửa thì viền ngoài nhấp nháy jade 1,2 giây, kèm chip "Trieu vừa sửa" biến mất sau 3 giây.

### 7.4. Thanh ray thời gian ⭐

```
 08:00 ─●── [Thẻ: Chùa Linh Ứng]
         │
         ┊ 25 phút · 8,4 km        ← Phase 3
         │
 12:00 ─●── [Thẻ: Bún chả cá 109]
         │
     — ─●── [Thẻ: Chợ đêm]          ← hoạt động không có giờ
```

- Các ga xếp theo **thứ tự trong ngày** (do kéo thả quyết định), **không** theo tỉ lệ thời gian, vì hệ thống cho phép hoạt động không có giờ và cho phép sắp xếp tự do.
- Lưới mỗi hàng: cột giờ 40px (căn phải) → cột ray 16px (tâm ray ở 48px) → thẻ.
- Ray: đường dọc 1px `tide`, chỉ vẽ khi ngày có hoạt động.
- Chấm: 10px, nền trắng, viền 3px màu tuyến.
- Ngày trống: khung nét đứt "Ngày này còn trống. Thêm địa điểm bạn muốn ghé." + nút "Thêm hoạt động".
- "+ Thêm hoạt động" là **nút chính của trang**, đặt ở góc phải tiêu đề ngày (trên điện thoại chữ rút gọn "+ Thêm"), không đặt ở cuối ray.
- **Chưa áp dụng:** đoạn nét đứt ghi thời gian di chuyển (cần API lộ trình, Phase 3); nút mờ "+ Thêm hoạt động vào khoảng này" khi hai hoạt động cách nhau trên 3 tiếng.

### 7.5. Huy hiệu — `Badge`

Cao 20px, bo 6px, chữ 12px / 500, lề ngang 8px.
- `surface="tint"` (mặc định): nền là màu gốc pha 12%, chữ màu gốc đậm.
- `surface="solid"`: nền trắng + viền mảnh; dùng khi huy hiệu nằm đè lên ảnh (trạng thái trên ảnh bìa thẻ chuyến đi).

Tông: `neutral`, `muted`, `brand`, `info`, `success`, `warning` (mục 3.5). Dùng cho trạng thái chuyến đi, vai trò thành viên (**Phase 4**), gói Premium (**Phase 6**). Ô chọn trạng thái trên trang chi tiết (`TripStatusSelect`) dùng cùng màu với huy hiệu.

### 7.6. Hộp thoại — `Modal`, `ConfirmDialog`

- Thẻ `<dialog>` gốc mở bằng `showModal()`: trình duyệt lo lớp phủ, giữ focus trong hộp, phím Esc.
- `size="md"` rộng 480px (form đơn giản, hộp xác nhận); `size="lg"` rộng 640px (form hoạt động).
- Bo 14px, viền `tide`, `shadow-lg`, lớp phủ `ink` 45%. Góc trên bên phải có nút "×".
- Trên điện thoại: nằm sát mép dưới, rộng hết màn hình, cao tối đa 90%, chỉ bo hai góc trên.
- Nút xếp ở góc phải dưới: hành động phụ bên trái, hành động chính bên phải.
- Khi hộp mở, con trỏ nằm ở **phần tử đầu tiên của nội dung**: ô nhập đầu tiên của form, hoặc nút "Huỷ" của hộp xác nhận (nhấn Enter ngay không bao giờ xoá nhầm). Không nằm ở nút "×", và bỏ qua mọi nút mang `data-no-initial-focus` (nút bỏ một thứ gì đó, Task 3.6). `Modal` tự làm việc này; **không** đặt `autoFocus` cho ô bên trong hộp thoại, vì nó không có tác dụng khi hộp còn đang ẩn (BUG-UI-003, Task 2.7).
- `ConfirmDialog` dùng trước mọi thao tác mất dữ liệu (xoá, đổi ngày làm mất hoạt động) hoặc cần nghĩ lại (trùng giờ, vừa dời vừa đổi độ dài chuyến đi).
- **Chưa áp dụng:** trả focus về đúng nút đã mở hộp khi hộp được mở từ menu "⋮" (menu đã biến mất khi hộp đóng).

### 7.7. Thông báo — `Toaster` + `toast` (`stores/toastStore.ts`)

- Góc dưới bên phải trên máy tính, trên cùng trên điện thoại; rộng 360px; tự tắt sau 4 giây; có nút "×".
- Vùng chứa là `aria-live="polite"`; thông báo lỗi có `role="alert"`.
- Chữ dùng thể hoàn thành, khớp với nút đã bấm: "Lưu thay đổi" → "Đã lưu thay đổi"; "Thêm hoạt động" → "Đã thêm hoạt động". Hiện có: "Đã lưu thay đổi", "Đã thêm hoạt động", "Đã xoá hoạt động", "Đã đổi trạng thái", "Đã xoá chuyến đi", `Đã chuyển "…" sang Ngày N`.
- Toast có thể kèm **một link** dẫn tới kết quả không nằm trên màn hình: `Đã chuyển "…" sang Ngày 3` + "Mở Ngày 3".
- Lỗi của thao tác không có form (kéo thả, đổi trạng thái) báo bằng toast đỏ, không báo bằng khung đỏ nằm xa chỗ thao tác.
- **Không có nút "Hoàn tác"** cho tới khi API hỗ trợ khôi phục: hoạt động bị xoá cứng ngay.

### 7.8. Khung xương — `Skeleton`

- Dùng cho danh sách chuyến đi (3 thẻ) và trang chi tiết (đầu trang + 4 thẻ). Không dùng vòng quay toàn trang cho các màn này.
- Khối `gray-100` có dải sáng chạy ngang chu kỳ 1,6 giây (`animate-shimmer`); đứng yên khi hệ điều hành bật giảm chuyển động.
- Vòng quay chỉ dùng bên trong nút, khi khôi phục phiên lúc mở ứng dụng (`FullPageSpinner`), và khi tải bản đồ (Phase 3).

### 7.9. Trạng thái rỗng — `EmptyState`

Ba phần: hình minh hoạ nét đơn hai màu (bản đồ gấp có tuyến đường), một câu nói rõ việc cần làm, một nút hành động. Không có khung bao quanh.

| Màn hình | Câu chữ | Nút | |
|---|---|---|---|
| Chưa có chuyến đi | "Chưa có chuyến đi nào. Tạo chuyến đầu tiên để bắt đầu lên lịch trình." | Tạo chuyến đi | **Đã làm** |
| Lọc không ra kết quả | "Không tìm thấy chuyến đi nào phù hợp. Thử từ khoá khác hoặc bỏ bớt bộ lọc." | Xoá bộ lọc | **Đã làm** |
| Ngày chưa có hoạt động | "Ngày này còn trống. Thêm địa điểm bạn muốn ghé." (khung nét đứt trên ray) | Thêm hoạt động | **Đã làm** |
| Chưa mời ai | "Chỉ mình bạn thấy chuyến đi này. Mời bạn bè để cùng chỉnh sửa." | Mời thành viên | **Phase 4** |
| Chưa có chi phí | "Chưa ghi khoản nào. Thêm chi phí để theo dõi ngân sách." | Thêm chi phí | **Phase 7** |

### 7.10. Ghi chú dài — `ExpandableText`

Mô tả chuyến đi, ghi chú ngày, ghi chú hoạt động hiện tối đa **2 dòng**. Nút "Đọc thêm" chỉ xuất hiện khi chữ thật sự dài hơn 2 dòng (đo lại khi đổi kích thước cửa sổ), bấm thì mở hết và đổi thành "Thu gọn". Giữ các chỗ xuống dòng người dùng tự gõ; chuỗi dài không khoảng trắng được bẻ dòng (`wrap-anywhere`).

### 7.11. Logo — `Logo`

Ô vuông `jade` bo 6px chứa icon ghim trắng, cạnh chữ "Smart Trip Planner".
- `tone="dark"`: chữ trắng, trên thanh điều hướng.
- `tone="light"` + `tagline`: cỡ lớn kèm dòng phụ "Kế hoạch hành trình theo dòng thời gian", trên các trang đăng nhập.

### 7.12. Ô tìm địa điểm — `PlaceSearchField`, `ActivityPlaceField`

```
Địa điểm
┌──────────────────────────────────────┐
│ 🔍 chợ                               │   ô tìm 40px, kính lúp bên trái
└──────────────────────────────────────┘
┌──────────────────────────────────────┐   danh sách NỔI đè lên các ô bên dưới,
│▌(🛍) Chợ Hàn                          │   không đẩy chúng xuống
│▌     119 Trần Phú, Phường Hải Châu   │   dòng đang chọn: nền jade-light + vạch jade 3px
│ (🛍) Chợ Cồn                          │   icon nhóm trong vòng tròn gray-100 32px
│      290 Hùng Vương, Phường Hải Châu │   tên 15px/600, địa chỉ 13px gray-600
└──────────────────────────────────────┘
```

- **`PlaceSearchField`** (`features/places`): chỉ tìm và báo lại kết quả được chọn; dùng lại được ở form khác. Gửi yêu cầu sau khi ngừng gõ **300ms**, từ **2 ký tự**, tối đa 100 ký tự. Khoá truy vấn `['places', 'search', từ khoá, 20]`, không nằm dưới `['trip', id]`.
- **5 gợi ý đầu, rồi "Xem tất cả N kết quả":** một lần tìm lấy về tối đa 20 kết quả (giới hạn của máy chủ) nhưng chỉ hiện 5. Còn nữa thì cuối danh sách có dòng chữ `jade` "Xem tất cả 12 kết quả"; bấm (hoặc ↓ tới đó rồi Enter) thì danh sách hiện đủ, cao tối đa 320px và **có thanh cuộn riêng**, vẫn nổi tại chỗ. Đủ 20 kết quả thì có dòng nhắc "Chỉ hiện 20 kết quả đầu. Gõ từ khoá cụ thể hơn để thu hẹp." Gõ tiếp thì danh sách về lại 5 dòng.
- Danh sách có viền `tide`, bo 6px, `shadow-md`. Icon của gợi ý theo nhóm địa điểm (cùng icon và màu với loại hoạt động, mục 3.4); nhóm lạ hoặc không có nhóm: ghim xám.
- **Quanh điểm đến:** trong form hoạt động, ô tìm gửi kèm toạ độ điểm đến của chuyến đi; địa điểm trong vòng 50 km quanh đó đứng trước (gõ "chợ" trong chuyến đi Đà Nẵng thì chợ ở Đà Nẵng lên đầu). Chỉ đổi thứ tự, không lọc bớt. Chuyến đi chưa đặt vị trí điểm đến: thứ tự chỉ theo tên. Ô "Vị trí trên bản đồ" của chuyến đi không gửi toạ độ.
- Trong lúc chờ: "Đang tìm địa điểm…". Không có kết quả: "Không tìm thấy địa điểm nào. Thử từ khoá khác." Máy chủ lỗi: câu của máy chủ.
- **Bàn phím** (kiểu combobox): ↓ / ↑ di chuyển, Enter chọn dòng đang sáng, Esc **chỉ đóng danh sách** (không đóng hộp thoại). Enter trong ô tìm không bao giờ gửi form bên ngoài.
- **`ActivityPlaceField`** (`features/itinerary`): ô "Địa điểm" của form hoạt động, đứng **đầu form**. Chọn một gợi ý thì địa điểm được lưu ngay (`POST /places`) và ô tìm đổi thành **khung địa điểm đã chọn**: nền `gray-50`, viền `tide`, icon ghim `jade`, tên 15px/500, địa chỉ 13px `gray-600`, nút "×" bên phải để quay lại ô tìm (con trỏ vào ô tìm).
- **Bỏ địa điểm:** bấm "×" rồi lưu mà không chọn địa điểm khác thì hoạt động **không còn địa điểm** (`clearPlace: true`); thẻ mất hàng địa điểm. Bấm "×" rồi chọn địa điểm khác là **đổi** (`placeId`). Bấm "×" rồi "Huỷ" thì không có gì thay đổi. Không cần hộp hỏi lại: địa điểm vẫn được lưu trong hệ thống và gắn lại được bằng một lần tìm.
- Chọn địa điểm khi ô "Tên hoạt động" **còn trống** thì tên địa điểm được điền vào, vẫn sửa được; tên đã có chữ thì không bị đụng tới. Sau khi chọn, con trỏ sang ô tên.
- **Gợi ý loại:** khi **thêm** hoạt động, chọn địa điểm thì "Loại" tự đổi theo nhóm của địa điểm (Chợ Hàn → "Mua sắm"), miễn là người dùng **chưa tự chọn loại** trong lần mở hộp thoại đó. Đã bấm hoặc dùng phím vào nhóm "Loại" thì lựa chọn của người dùng được giữ. Chọn địa điểm khác (sau "×") thì gợi ý đổi theo. Địa điểm không có nhóm, hoặc nhóm lạ: không đổi gì. Khi **sửa** hoạt động có sẵn: không bao giờ tự đổi loại.
- **Tự thêm địa điểm** (`ManualPlaceForm`, Task 3.6): dưới ô tìm có dòng chữ `jade` 13px **"Không tìm thấy? Tự thêm địa điểm"**. Bấm thì ô tìm nhường chỗ cho một khung nền `gray-50`, viền `tide`: ô "Tên địa điểm *" (ví dụ "Nhà bà ngoại", con trỏ vào đây), nhãn "Vị trí *" với **bản đồ nhỏ cao 240px** (`PointPicker`, mục 9) và dòng gợi ý "Phóng to rồi bấm lên bản đồ để đặt vị trí."; bấm lên bản đồ thì có một marker `jade` và dòng "Đã đặt vị trí: 16.0471, 108.2069. Bấm chỗ khác để đổi." Bản đồ mở ở điểm đến của chuyến đi (chưa có thì cả Việt Nam). Hai nút ở góc phải: ghost "Quay lại tìm kiếm" và nút phụ "Thêm địa điểm" (không phải nút chính: nút chính của hộp thoại vẫn là "Thêm hoạt động"). Thiếu tên hoặc chưa đặt vị trí thì báo lỗi ngay tại chỗ sau khi bấm "Thêm địa điểm". Thêm xong thì khung địa điểm đã chọn hiện ra như khi chọn một gợi ý, và tên hoạt động còn trống được điền tên địa điểm. Enter trong ô tên chỉ thêm địa điểm, không lưu hoạt động. Địa điểm tự thêm là của riêng người tạo (design rule 14.19); không có địa chỉ và nhóm, nên loại hoạt động không được gợi ý.
- Khi mở hộp sửa một hoạt động đã có địa điểm, con trỏ **không** nằm ở nút "×" của khung (nhấn Enter sẽ bỏ nhầm địa điểm) mà ở ô tên: nút đó mang `data-no-initial-focus` (mục 7.6).

### 7.13. Chọn loại hoạt động — `ActivityTypeField`

```
Loại
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│ 🏛 Tham quan │ │ 🍴 Ăn uống    │ │ 🚌 Di chuyển  │   3 cột × 2 hàng (điện thoại: 2 cột × 3 hàng)
└──────────────┘ └──────────────┘ └──────────────┘   cao 40px (điện thoại 44px), bo 6px, viền tide
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│ 🛏 Lưu trú   │ │ 🛍 Mua sắm    │ │ 📍 Khác       │
└──────────────┘ └──────────────┘ └──────────────┘
```

- Thay ô chọn thả xuống ở form hoạt động (Task 3.6): cả 6 loại hiện sẵn, một lần bấm là chọn.
- Mỗi nút: icon của loại theo **màu tuyến** (mục 3.4) + nhãn `ink` 15px. Nút **đang chọn**: viền 2px màu tuyến, nền màu tuyến mờ 8%, nhãn đổi sang màu tuyến và đậm 500. Luôn có đúng một nút được chọn; hoạt động mới mặc định "Khác".
- Rê chuột lên nút chưa chọn: viền `gray-300`. Tab tới nhóm: viền ngoài `jade` 2px quanh nút đang có con trỏ.
- Bên dưới là các ô radio thật (ẩn sau nhãn): Tab vào nhóm một lần, **phím mũi tên** đổi loại, trình đọc màn hình đọc là nhóm "Loại" gồm 6 lựa chọn.

---

## 8. Bố cục từng màn hình

### 8.0. Khung chung (`AppLayout`) — **Đã làm**

```
┌──────────────────────────────────────────────────────────────────────┐
│ [📍 Smart Trip Planner]   [🔍 Tìm chuyến đi…        ]   Trieu  ⎋ Đăng xuất │ 56px, nền ink
├──────────────────────────────────────────────────────────────────────┤
│                         nội dung, tối đa 1280px                       │
├──────────────────────────────────────────────────────────────────────┤
│ © 2026 Smart Trip Planner                                            │ nền ink
└──────────────────────────────────────────────────────────────────────┘
```

- **Ô tìm chuyến đi** (`TripSearchBox`) nằm giữa thanh điều hướng, nền trong mờ, chữ trắng. Chỉ tìm chuyến đi theo tên hoặc điểm đến (chưa tìm được theo hoạt động). Ở `/trips`: lọc khi ngừng gõ 300 ms, không thêm bước vào lịch sử trình duyệt, giữ chip trạng thái và cách sắp xếp. Ở trang khác: Enter mở `/trips?q=…`.
- Dưới 768px: ô tìm xuống thành hàng riêng dưới logo, rộng hết chiều ngang.
- Không có chuông thông báo, không có mục menu cho trang chưa tồn tại.

### 8.1. Chi tiết chuyến đi — màn hình trung tâm

```
┌──────────────────────────────────────────────────────────────────────────┐
│ ‹ Chuyến đi của bạn                                                      │
│ Đà Nẵng 4 ngày (tối đa 2 dòng)               [Đang diễn ra ▾] [✎ Sửa] [🗑 Xoá] │
│ 📍 Đà Nẵng   📅 12/03 – 15/03/2026   🕒 4 ngày   👛 Ngân sách 5.000.000 ₫   │
│ Mô tả… (tối đa 2 dòng, Đọc thêm)                                         │
├────────────┬───────────────────────────────────────────┬─────────────────┤
│ ▌Ngày 1  3 │ Ngày 1 · Thứ năm, 12/03  ✎ Sửa [+ Thêm hoạt động] │           │
│  12/03     │ Khám phá bán đảo Sơn Trà                  │   [ BẢN ĐỒ ]    │
│  Ngày 2  1 │ 08:00 ─●── [Chùa Linh Ứng            ⋮]  │                 │
│  13/03     │ 12:00 ─●── [Bún chả cá 109           ⋮]  │   Phase 3       │
│  Ngày 3  0 │ 14:30 ─●── [Cầu Rồng                 ⋮]  │                 │
│            │───────────────────────────────────────────├─────────────────┤
│            │                               [ Ngày 2 › ] │ 🌤 thời tiết    │
└────────────┴───────────────────────────────────────────┴─────────────────┘
    200px                  linh hoạt                        420px (Phase 3)
```

**Đã làm:**
- **Đầu trang:** link quay lại, tên 32px (tối đa 2 dòng, rê chuột xem đủ), cụm ô trạng thái + "Sửa" + "Xoá" **cố định ở góc phải** (không bị tên dài đẩy xuống; trên điện thoại nằm hàng riêng dưới tên), hàng thông tin có icon, mô tả thu gọn.
- **Máy chủ không trả lời:** nếu chuyến đi đã tải được, trang **giữ nguyên** (kể cả hộp thoại đang mở) và hiện khung lỗi "… Đang hiển thị dữ liệu đã tải trước đó." kèm nút phụ "Thử lại" ngay trên tên chuyến đi, cùng cách trang danh sách đang làm. Chưa tải được gì thì chỉ có khung lỗi, "Thử lại" và link về danh sách. Chuyến đi không còn (404) hoặc không có quyền (403): chỉ thông báo, không có "Thử lại" (Task 2.7, BUG-UI-005).
- **Một ngày một trang:** URL `/trips/:id/days/:dayIndex` (số thứ tự ngày, 1..n). `/trips/:id` và số ngày không tồn tại (ví dụ sau khi rút ngắn chuyến đi) tự chuyển về ngày 1. F5, nút Back và link gửi cho người khác giữ đúng ngày. (Trước đây xếp dọc mọi ngày trên một trang; đổi ở Task 2.6 commit 10 để khớp với bản đồ từng ngày ở Phase 3.)
- **Cột trái (200px):** các ngày kèm số hoạt động, mỗi mục là một link. Ngày đang xem có nền `jade-light` + vạch `jade` 3px ở mép trái (`aria-current="page"`).
- **Cột giữa:** chỉ ngày đang xem: tiêu đề ngày với nút "Sửa" và nút chính "+ Thêm hoạt động", rồi đến ray. Dưới ray là nút "‹ Ngày trước" / "Ngày sau ›"; bấm "Ngày sau" ở cuối một ngày dài thì trang cuộn lên tiêu đề của ngày mới.
- **Ngày dài** (Task 2.6 commit 13): từ 1024px, **khối mô tả ngày** (tên ngày, tiêu đề, ghi chú, "Sửa", "+ Thêm hoạt động") dính ở trên khi cuộn, **ngang hàng với cột ngày bên trái** (cả hai cách mép trên 24px); nền `paper` che các thẻ cuộn qua bên dưới, viền `tide` ở đáy khối. Cả trang vẫn chỉ có **một thanh cuộn**: không dùng khung cao cố định có thanh cuộn riêng (hai thanh cuộn lồng nhau khó dùng, cản kéo thả, và cột bản đồ Phase 3 sẽ dính theo trang). Màn hình hẹp: khối mô tả **không** dính, vì sẽ chiếm gần nửa màn hình.
- **Nút quay lên** (`BackToTopButton`), chỉ hiện khi đã cuộn quá một chiều cao màn hình:
  - Từ 1024px: nút ghost **"↑ Đầu ngày"** trong khối mô tả ngày đang dính, cạnh "Sửa" (về chỗ hoạt động đầu tiên nằm ngay dưới khối, như lúc mới mở ngày), và một nút tròn `ink` **nhỏ 40px** ở **góc dưới phải** màn hình, cách mép dưới **96px**, **"Lên đầu trang"** (về tên chuyến đi). Độ cao này để nút nằm trên một thông báo (toast) đang hiện ở cùng góc và không chạm footer khi cuộn tới cuối trang; cột trái chỉ có danh sách ngày.
  - Dưới 1024px chỉ một nút tròn `ink` 48px, **"Về đầu ngày"** (đầu ngày nằm ngay dưới dải chip). Cách mép dưới 56px để không chạm footer. Điện thoại: góc phải (toast ở trên). Máy tính bảng: góc **trái**, vì toast chiếm góc dưới phải.
  - Bấm thì focus chuyển tới chỗ vừa cuộn về (phần ngày, hoặc tên chuyến đi).
  - Đích cuộn "đầu ngày" là **cả phần ngày** (`#day-start`), không phải tiêu đề: tiêu đề nằm trong khối dính nên trình duyệt coi như luôn hiện, cuộn tới nó không có tác dụng. Chuyển sang ngày khác từ cuối một ngày dài cũng cuộn về `#day-start`.

**Cột phải, bản đồ** (Task 3.6, **Đã làm**): từ 1024px có cột thứ ba rộng 360px (từ 1280px: 420px), dính khi cuộn ngang hàng với cột ngày (cách mép trên 24px) và cao bằng màn hình trừ 48px. Khoảng cách giữa các cột 24px (từ 1280px: 32px). Bản đồ vẽ theo bản đang hiển thị của ngày, nên số trên marker đổi theo ngay khi kéo thả. Dưới 1024px bản đồ nằm ở tab "Bản đồ" (bảng "Màn hình hẹp" bên dưới). Dải thời tiết bên dưới bản đồ: Task 3.7.

**Task 3.7 (chốt 2026-10-05; `design.md` rule 14.20, 14.22) — chưa làm:**
- **Dải thời tiết** (**Đã làm**) nằm dưới bản đồ trong cột phải, cách bản đồ 12px; bản đồ thấp đi đúng bằng chiều cao của dải và khoảng cách đó (cao bằng màn hình trừ 48px trừ 124px). Chi tiết ở mục 9.
- **Đoạn di chuyển** giữa các thẻ hoạt động: nằm trong hàng của thẻ **xuất phát**, ngay dưới thẻ, cao khoảng 28px: ray nét đứt xám 2px và một dòng chữ 12px `gray-500` "12 phút · 3,2 km". Không viền, không nền, **không icon phương tiện** (API chỉ tính một phương tiện). Thẻ kế tiếp không phải đích (ở giữa có hoạt động không có địa điểm) thì ghi thêm tên đích: "12 phút · 3,2 km tới Cầu Rồng" (tên dài thì cắt bằng "…"). Rê chuột hiện "Ước tính theo đường bộ, chưa tính kẹt xe". Dưới 1 km ghi mét ("998 m"), từ 1 km ghi một chữ số thập phân ("8,4 km"); thời gian làm tròn lên phút, từ 60 phút ghi "1 giờ 5 phút". Chặng 0 m (hai hoạt động cùng một địa điểm) không hiện. Đang kéo thẻ thì mọi đoạn di chuyển ẩn; sau khi lưu, số mới hiện lại. Không hiện tổng di chuyển của ngày.
- **Ngày đã qua và hôm nay** ("hôm nay" tính theo múi giờ của tài khoản, hiện là giờ Việt Nam):
  - Cột trái: dưới dòng "Ngày N · ngày tháng" có một dòng nhỏ 12px: "Hôm nay" (`jade-dark`, đậm) hoặc "Đã qua" (`gray-500`). Mục của ngày đã qua nhạt đi (chữ `gray-500`), vẫn bấm được.
  - Chip ngày (dưới 1024px): chip hôm nay có chấm `jade` 6px trước chữ; chip đã qua dùng chữ `gray-500` (không làm mờ cả chip: vẫn là link, phải đủ tương phản).
  - Tiêu đề ngày: `Badge` "Hôm nay" (tông jade) hoặc "Đã qua" (tông xám) đi liền sau ngày tháng, **không ngắt dòng giữa nhãn**; thiếu chỗ thì cả nhãn xuống dòng dưới.
  - Thẻ hoạt động của ngày đã qua **không** nhạt đi: vẫn sửa được và phải dễ đọc.
  - Mở `/trips/:id` (không kèm số ngày) khi hôm nay nằm trong chuyến đi thì vào thẳng ngày hôm nay thay vì Ngày 1. Link có số ngày giữ nguyên.
- **Hỏi hoàn thành:** mở một chuyến đi đã qua ngày cuối mà trạng thái còn là Nháp, Đã lên kế hoạch hoặc Đang diễn ra → `ConfirmDialog`: tiêu đề "Hoàn thành chuyến đi?", nội dung 'Chuyến đi "{tên}" đã kết thúc ngày {dd/mm/yyyy}. Lịch trình vẫn sửa được sau khi hoàn thành.', nút phụ "Để sau", nút chính "Hoàn thành chuyến đi". Xong: toast "Đã hoàn thành chuyến đi", ô trạng thái đổi theo. "Để sau": không hỏi lại về chuyến đi đó tới lần đăng nhập sau.
- (**Đã làm**) Trên màn hẹp, dưới tiêu đề của ngày và trên ghi chú có **một dòng thời tiết của ngày đang xem** (`features/weather/DayWeatherLine`; 13px, `gray-600`): icon có màu, tên tình trạng ("Có mây"), "32° / 25°", icon giọt nước và "20%", ngăn nhau bằng dấu "·" màu `gray-300`; thiếu chỗ thì tự xuống hàng. Ngày không có dự báo, đang tải hoặc tải lỗi thì không có dòng này (không chừa chỗ, không báo lỗi). Từ 1024px không hiện (đã có dải dưới bản đồ).

**Màn hình hẹp:**

| Bề rộng | Bố cục |
|---|---|
| < 1024px | Cột trái thành **dải chip ngày** (link) cuộn ngang, dính ở mép trên khi cuộn; chip của ngày đang xem tô `ink` và tự cuộn vào tầm nhìn. **Đã làm** |
| < 1024px | Ngay dưới dải chip ngày có **hai nút gạt** "Lịch trình" / "Bản đồ" (`ViewSwitch`): khung nền `gray-200` bo 6px, mỗi nút cao 44px có icon và chữ; nút đang chọn nền trắng, chữ `jade-dark` đậm, `shadow-sm` (chốt 2026-10-03 theo mockup, thay cho kiểu chữ gạch chân). Mỗi lúc chỉ hiện một trong hai: danh sách của ngày, hoặc bản đồ rộng hết màn hình, cao 70% màn hình (ít nhất 320px). Phần danh sách chỉ bị ẩn, không bị huỷ, nên việc đang sửa dở trong ngày không mất khi sang xem bản đồ; bản đồ chỉ được tạo khi tab của nó đang mở. Ở tab "Bản đồ", chạm marker mở ô tên; "Xem trong lịch trình" chuyển về tab "Lịch trình" rồi cuộn tới thẻ. Đổi ngày giữ nguyên tab đang xem. **Đã làm** (Task 3.6). Tab Chi phí: Phase 7 |

**Sắp xếp hoạt động:**
- Chuột / bút: kéo thả bằng tay nắm; bàn phím: Tab tới tay nắm, Space nhấc, mũi tên di chuyển, Space thả.
- **Màn hình cảm ứng:** không kéo thả (xung đột với cuộn trang); mỗi thẻ có **nút ↑ / ↓** 44px, chỉ đổi thứ tự **trong** ngày (hoạt động đầu ngày không lên được, cuối ngày không xuống được). Việc chọn tay nắm hay mũi tên dựa vào **loại con trỏ** (`pointer-coarse`), không dựa vào độ rộng màn hình.
- **Chuyển sang ngày khác:** trên máy tính, kéo thẻ **thả lên tên ngày ở cột trái** (mục đó có viền `jade` khi rê qua; chỉ tính khi con trỏ nằm đúng trên mục, nên kéo trong ngày không bị hút sang). Mọi thiết bị: menu "⋮" → **"Chuyển sang ngày…"** (hộp chọn ngày). Hoạt động xuống **cuối** ngày đích và biến khỏi ngày đang xem, nên thông báo có link "Mở Ngày N". Vẫn hỏi lại khi trùng giờ ở ngày mới.
- **Tự xếp theo giờ** (Task 2.6 commit 12, backend làm): thêm hoạt động có giờ bắt đầu, hoặc đổi giờ bắt đầu, thì hoạt động vào ngay trước hoạt động đầu tiên bắt đầu muộn hơn; không có thì xuống cuối ngày. Hoạt động không giờ và thứ tự đã kéo giữ nguyên; sửa tên, ghi chú, chi phí, giờ kết thúc không làm di chuyển.
- Kéo thả và nút mũi tên dùng chung một đường lưu: cập nhật giao diện ngay, hỏi lại khi trùng giờ ở ngày mới, trả về chỗ cũ khi lỗi.

### 8.2. Danh sách chuyến đi — **Đã làm**

- Tiêu đề "Chuyến đi của bạn" + dòng phụ; nút "+ Tạo chuyến đi" bên phải.
- **Hàng lọc** giữa hai đường kẻ mảnh:
  - Chip trạng thái "Tất cả" + 5 trạng thái, mỗi chip có **số chuyến đi** (từ `GET /trips/status-counts`, tính theo từ khoá đang tìm). Chip đang chọn nền `ink` chữ trắng; "Đang diễn ra" có chấm `jade` đứng yên.
  - Ô "Sắp xếp": Tạo gần đây nhất / Ngày đi sớm nhất / Ngày đi muộn nhất / Tên A → Z.
  - Mọi bộ lọc lưu trên URL (`?q=&status=&sort=&page=`).
- **Lưới** 3 cột (≥ 1024px) / 2 cột (≥ 640px) / 1 cột.
- **Thẻ:** ảnh bìa 16:9 (chưa có ảnh thì là khối `gray-100` có icon ghim, không gradient), huy hiệu trạng thái `solid` ở góc trên phải ảnh, tên điểm đến ở góc dưới trái ảnh (nền `ink` 85%), tên chuyến 1 dòng, ngày đi có icon lịch, chân thẻ "5 ngày · 12 hoạt động". Rê chuột: viền và tên chuyển `jade`.
- **Thời tiết trên thẻ** (Task 3.7, chưa làm; design rule 14.20): ở **hàng chân thẻ, căn phải**, 13px `gray-600`: chữ "Hôm nay" (chuyến đang đi) hoặc "Ngày đi" (chuyến sắp đi, ngày đầu trong 16 ngày tới), icon thời tiết, "32° / 25°". Không có xác suất mưa. Chuyến đi đã qua, còn xa hơn, chưa đặt vị trí điểm đến, hoặc dự báo không tải được: không hiện gì (không có chỗ trống, không có chữ báo lỗi). Thẻ hiện ngay, thời tiết hiện sau khi tải xong. Không đặt ở hàng ngày đi: không đủ chỗ khi thẻ hẹp (mockup Task 3.7).
- **Không làm** (có trong mockup nhưng không có dữ liệu thật): ảnh thành viên (**Phase 4**), thanh "Phân bổ lịch trình" nhiều màu và chú thích màu (cần số hoạt động theo từng loại cho mỗi chuyến, có thể thêm sau), khối "Thống kê hành trình tổng quan".

### 8.3. Trang đăng nhập, đăng ký, quên / đặt lại mật khẩu, xác thực email — **Đã làm**

- Logo cỡ lớn kèm dòng phụ; khung 400px, viền mảnh, không bóng; tiêu đề 28px + dòng phụ.
- "Quên mật khẩu?" nằm cùng hàng với nhãn "Mật khẩu", căn phải; ô mật khẩu có nút con mắt; quy tắc mật khẩu là dòng gợi ý dưới ô.
- Nút gửi chính cao 44px, rộng hết khung.
- Dòng chuyển trang ("Chưa có tài khoản? Đăng ký") nằm **dưới** khung.
- Không có: dải trạng thái hệ thống, mã phiên bản, bảng chú thích màu (chỉ có trong mockup).

### 8.4. Tạo chuyến đi (`/trips/new`) — **Đã làm**

- Tiêu đề 32px + dòng phụ "Ba bước: thông tin chung, điểm đến và ngày đi."
- Thanh bước là một tuyến ngắn: 3 "ga" đánh số nối bằng ray. Ga đã qua: nền `jade` có dấu ✓; ga hiện tại: viền `jade`, chữ đậm; ray đã đi qua chuyển `jade`.
- Bước "Điểm đến" (Task 3.6, **Đã làm**), dùng chung với hộp "Sửa chuyến đi":
  - **"Tên điểm đến":** ô chữ tự gõ như trước ("Ví dụ: Đà Lạt").
  - **"Vị trí trên bản đồ":** ô tìm địa điểm (mục 7.12) với gợi ý "Tìm theo tên hoặc bấm lên bản đồ để chọn vị trí. Vị trí này dùng cho bản đồ và dự báo thời tiết." Chọn một gợi ý chỉ lấy **toạ độ** của nó; không địa điểm nào được lưu. Ô tìm luôn còn đó để đổi vị trí.
  - **Bản đồ nhỏ cao 320px** ngay dưới ô tìm (`PointPicker`, mục 9): mở ở vị trí đang có (mức phố), chưa có thì cả Việt Nam. **Bấm lên bản đồ** đặt hoặc dời vị trí; chọn một gợi ý ở ô tìm thì marker nhảy tới đó và bản đồ chuyển theo. Hai cách dùng lẫn nhau được: tìm tới gần đúng rồi bấm để chỉnh.
  - Đã có vị trí: dưới bản đồ là một dòng 13px gồm icon ghim `jade`, một nhãn, và toạ độ 4 chữ số thập phân bằng số thẳng cột (`16.0612, 108.2279`). Nhãn là tên địa điểm vừa chọn ở ô tìm, "Vị trí chọn trên bản đồ" sau một lần bấm lên bản đồ, hoặc "Vị trí đã lưu" khi mở hộp sửa mà chưa đổi gì.
  - Chọn vị trí khi ô tên **còn trống** thì tên địa điểm được điền vào, vẫn sửa được.
  - Nút chữ "Bỏ vị trí" chỉ có khi vị trí **chưa được lưu** (wizard, hoặc chuyến đi chưa có vị trí). Vị trí đã lưu chỉ đổi được, chưa bỏ được (máy chủ chưa hỗ trợ xoá trắng).

### 8.5. Trang chuyến đi công khai (share link) — **Phase 4**

Một cột, rộng tối đa 760px, canh giữa. Không có thanh điều hướng của ứng dụng, chỉ có logo nhỏ và nút "Tạo chuyến đi của bạn" ở cuối. Bản đồ đặt dưới phần tóm tắt, không dính.

### 8.6. Landing — **Phase 8**

Hero **không** dùng khối chữ lớn với gradient. Hiển thị một lịch trình thật: thanh ray với ba hoạt động và bản đồ nhỏ bên cạnh; các chấm màu tuyến sáng dần theo thứ tự trong 2 giây khi trang tải (khoảnh khắc chuyển động duy nhất của cả trang). Tiêu đề bên trái, ngắn, một câu. Làm cuối cùng, để dùng ảnh chụp sản phẩm thật.

### 8.7. Trang nâng cấp — **Phase 6**

Hai cột so sánh Free và Premium, không có thẻ "phổ biến nhất" phóng to. Dòng giới hạn mà người dùng đang chạm tới được tô nền `warning` 8% (ví dụ đã có 3 chuyến đi thì dòng "Số chuyến đi" được tô).

---

## 9. Bản đồ và thời tiết — **Phase 3**

- **Bản đồ** (Task 3.6, **Đã làm**): `DayMap` + `DayMapCanvas` (`features/itinerary`), thư viện Leaflet tải lười. Nền là tile chuẩn của **OpenStreetMap** (không cần API key), **giữ màu gốc** (biển xanh, công viên xanh lá). Bản đầu làm nhạt nền bằng CSS (giảm màu 85%) cho marker nổi; chủ dự án thấy cả trang bị xám nên bỏ bộ lọc ngày 2026-10-04. Marker vẫn tách khỏi nền nhờ viền trắng 2px và bóng. Góc dưới phải ghi nguồn "Leaflet | © OpenStreetMap contributors" (bắt buộc theo điều khoản) và hai nút phóng to / thu nhỏ. CartoDB Positron (kiểu nền của mockup) cần API key từ 2026: là tuỳ chọn ở Task 3.8 (design.md 3.2). Chế độ tối: chưa làm.
- Bản đồ luôn đóng khung vừa mọi địa điểm của ngày (một địa điểm: mức phố). Khung chỉ tính lại khi **tập địa điểm** đổi; kéo thả đổi thứ tự không làm bản đồ nhảy, và mức phóng người dùng tự chỉnh được giữ.
- **Ngày chưa có địa điểm nào** (Task 3.6, **Đã làm**): bản đồ mở ở **điểm đến của chuyến đi** (mức thành phố), không có marker. Giữa bản đồ có một thẻ trắng nhỏ (rộng tối đa 280px, bo 10px, viền `tide`, `shadow-md`): icon bản đồ xám, dòng đậm "Ngày này chưa có địa điểm nào.", dòng nhỏ "Thêm địa điểm cho hoạt động để thấy trên bản đồ." Chuyến đi **chưa đặt vị trí điểm đến**: bản đồ hiện cả Việt Nam, và thẻ có thêm câu 'Đặt vị trí điểm đến trong "Sửa" chuyến đi để bản đồ mở đúng nơi bạn đến.' Quanh thẻ, bản đồ vẫn kéo và phóng được. Thẻ biến mất ngay khi ngày có địa điểm đầu tiên.
- **Phóng to bản đồ** (Task 3.6, **Đã làm**): góc trên phải bản đồ có nút vuông trắng 36px (màn cảm ứng: 44px), viền `tide`, icon `Maximize2`, nhãn "Phóng to bản đồ". Bấm thì bản đồ của ngày mở **phủ cả cửa sổ** trong một hộp thoại gốc của trình duyệt: cùng marker, đường nối và thẻ "chưa có địa điểm"; nút ở góc trên phải đổi thành "×" ("Thu nhỏ bản đồ"). Đóng bằng nút đó hoặc phím **Esc**; con trỏ trở về nút "Phóng to bản đồ". Trong lúc mở, phần còn lại của trang không bấm được. Bản phóng to là một bản đồ thứ hai, nên mức phóng và vị trí đang xem của bản nhỏ không mang theo (cả hai đều tự đóng khung vừa các địa điểm của ngày).
- **Bản đồ chọn một điểm** (`components/map/PointPicker`, Task 3.6): bản đồ nhỏ bo 6px, viền `tide`, cùng nền, dòng ghi nguồn và nút + / − như bản đồ của ngày. Bấm (hoặc chạm) lên bản đồ đặt một marker giọt nước `jade` có icon ghim trắng; bấm chỗ khác thì marker dời tới đó. Toạ độ làm tròn 7 chữ số thập phân. Khi điểm vừa đặt không nhìn rõ được, bản đồ tự chuyển tới nó ở mức thành phố: điểm nằm ngoài khung nhìn (một kết quả tìm kiếm ở nơi khác), hoặc bản đồ còn đang ở mức cả nước (một lần bấm ở mức đó quá thô, nên bản đồ phóng vào để bấm lại cho chính xác). Bản đồ đã đủ gần thì giữ nguyên khung nhìn khi bấm. Chọn điểm cần chuột hoặc ngón tay; form nào dùng bản đồ này cũng có ô tìm địa điểm cho người dùng bàn phím. Nguồn nền và các mức phóng chuẩn của mọi bản đồ nằm ở `lib/mapTiles.ts`.
- Khung bản đồ là một lớp riêng (`isolate`), bo 10px, viền `tide`: các lớp của thư viện bản đồ (z-index tới 1000) không đè lên khối tiêu đề ngày đang dính hay lớp phủ của hộp thoại (bẫy BUG-UI-002).
- **Marker:** hình giọt nước 28px (**36px trên màn hình cảm ứng**, icon và số lớn theo), nền màu tuyến, viền trắng 2px, icon trắng bên trong (cùng icon lucide ở mục 3.4), số thứ tự ở góc trên phải trong vòng tròn trắng 16px. Số đếm **riêng các hoạt động có địa điểm** (1, 2, 3...), theo thứ tự trong ngày; hoạt động không có địa điểm không có marker và không chiếm số. Marker do ứng dụng tự vẽ bằng HTML, không dùng ảnh marker của Leaflet. Rê chuột lên marker hiện "số. tên hoạt động". **Liên kết thẻ và marker** (Task 3.6, **Đã làm**): rê chuột lên một thẻ hoạt động, hoặc Tab vào trong thẻ, thì marker của hoạt động đó phóng to 1,15 lần (lớn lên từ mũi nhọn, mũi vẫn chỉ đúng chỗ), có vòng sáng `jade` mờ 35% dày 4px và nổi lên trên các marker khác; rời thẻ thì trở lại sau 150ms. Thẻ của hoạt động không có địa điểm không gây gì. Trạng thái này giữ ở `stores/mapLinkStore.ts`, chỉ marker liên quan vẽ lại.
- **Bấm marker** (Task 3.6, **Đã làm**), hai kiểu:
  - *Đi thẳng tới thẻ*: bản đồ nhỏ cạnh danh sách, khi dùng chuột. Trang cuộn mượt đưa thẻ của hoạt động vào **giữa màn hình** (không nhắm mép trên, vì ở đó có khối tiêu đề ngày đang dính), thẻ có viền sáng `jade` mờ 40% trong 2 giây và nhận con trỏ; marker của nó vì thế vẫn nổi bật. Người tắt hiệu ứng chuyển động trong hệ điều hành thì trang nhảy thẳng tới thẻ.
  - *Qua ô tên*: trên **màn cảm ứng** (ngón tay không rê được để đọc chú thích) và trên **bản đồ đang phóng to** (thẻ bị che). Bấm marker mở một ô nhỏ phía trên marker: "số. tên hoạt động" (14px/600) và nút chữ `jade` **"Xem trong lịch trình"**. Bấm nút thì bản đồ phóng to tự thu lại (nếu đang mở) rồi làm như kiểu đi thẳng. Ô đóng bằng "×" của nó hoặc bấm ra ngoài.
  - Bàn phím: Tab tới marker, Enter có tác dụng như bấm chuột.
- **Đường nối** (Task 3.6, **Đã làm**): đường **nét đứt** 2px `jade` mờ 70% (đoạn 6px, hở 6px), nối các marker theo đúng thứ tự trong ngày, nằm dưới marker. Nét đứt vì đây là đường thẳng từ điểm này tới điểm kế tiếp, không phải đường đi thật; nét liền dễ bị đọc nhầm thành một con phố của nền bản đồ (chốt 2026-10-03 khi duyệt mockup, bản đầu ghi nét liền mờ 60%). Ngày có dưới 2 địa điểm thì không có đường. Hai hoạt động cùng một địa điểm: marker chồng lên nhau, đường không đổi. Không vẽ đường giữa các ngày khác nhau. Kéo thả đổi thứ tự thì đường vẽ lại ngay.
- **Thời tiết** (Task 3.7, **Đã làm**): dải ngang dưới bản đồ (`features/weather/WeatherStrip`), khung trắng viền `tide` bo 10px, **cao cố định 112px ở mọi trạng thái** (bản đồ phía trên chỉ đo khung của nó một lần, nên dải không được đổi chiều cao), mỗi ngày một ô ngăn nhau bằng vạch `tide`. Các ô chia đều chiều ngang, rộng tối thiểu 96px; nhiều ngày hơn chỗ chứa thì dải cuộn ngang. Mỗi ô là **link tới ngày đó**: "N2 · 13/10" (12px, `gray-500`), icon 20px, "32° / 25°" (14px/600, làm tròn tới độ), icon giọt nước và "60%" (12px). Ô của ngày đang xem có nền `jade-light`, chữ đầu ô `jade-dark` đậm, và tự cuộn vào tầm nhìn. Ngày không có dự báo (đã qua, hoặc xa hơn 16 ngày): icon `CloudOff` màu `gray-400` và chữ "Chưa có" (cả ô có nhãn đọc "Chưa có dự báo"). Chuyến đi chưa đặt vị trí điểm đến: thay cả dải bằng icon `MapPinOff` màu `gray-400` và câu 'Đặt vị trí điểm đến trong "Sửa" chuyến đi để xem dự báo thời tiết.' Không tải được: icon `CloudAlert` màu `gray-400`, "Tạm thời không có dự báo." kèm nút chữ `jade` "Thử lại" (đang thử: "Đang thử lại…", không bấm được); đã có dữ liệu cũ thì giữ dữ liệu cũ, không báo gì. Đang tải: khung xương cùng chiều cao. Viền `warning` cho ngày có cảnh báo: hoãn (design rule 14.21).
- **Icon thời tiết** (**Đã làm**; lucide, nét đơn, **có màu**, chốt 2026-10-05 theo mockup): `CLEAR` → `Sun` (`sun`), `PARTLY_CLOUDY` → `CloudSun` (`sun`), `CLOUDY` → `Cloud` (`gray-500`), `FOG` → `CloudFog` (`gray-500`), `RAIN` → `CloudRain` (`info`), `THUNDERSTORM` → `CloudLightning` (`info`), `SNOW` → `CloudSnow` (`info`). Tên tiếng Việt (dùng cho nhãn đọc và dòng thời tiết trên màn hẹp): Trời nắng, Nắng nhẹ, Có mây, Sương mù, Có mưa, Mưa dông, Có tuyết. Bảng này nằm ở `features/weather/weatherCondition.ts`, dùng chung cho dải, dòng trên màn hẹp và thẻ ở danh sách.

---

## 10. Giọng văn

- Gọi người dùng là "bạn"; ứng dụng không tự xưng.
- Câu chủ động, thì hiện tại: "Lưu thay đổi", không phải "Thay đổi sẽ được lưu".
- Lỗi nói rõ chuyện gì xảy ra và cách sửa, không xin lỗi: "Ngày kết thúc phải bằng hoặc sau ngày bắt đầu", không phải "Rất tiếc, đã có lỗi xảy ra".
- Một hành động giữ nguyên tên qua cả luồng: nút "Thêm hoạt động" → hộp "Thêm hoạt động" → nút "Thêm hoạt động" → toast "Đã thêm hoạt động".

**Câu báo lỗi** đến từ backend (`messages.properties`, CLAUDE.md rule 14); giao diện không tự dịch mã lỗi. Bảng dưới là câu chữ **mong muốn** — **Chưa áp dụng**: khi chốt, sửa trong `messages.properties` bằng một commit backend.

| errorCode | Câu hiện tại (backend) | Câu mong muốn |
|---|---|---|
| `ACTIVITY_TIME_CONFLICT` | "Hoạt động bị trùng giờ với một hoạt động khác trong ngày" | "Khung giờ này trùng với hoạt động khác trong ngày." |
| `FORBIDDEN` | "Bạn không có quyền thực hiện thao tác này" | "Bạn chỉ có quyền xem chuyến đi này." (khi đã có vai trò chỉ xem, Phase 4) |
| `QUOTA_EXCEEDED` | — (Phase 6) | "Gói miễn phí cho phép tối đa 3 chuyến đi. Nâng cấp để tạo thêm." |
| `PREMIUM_REQUIRED` | — (Phase 6) | "Tính năng này dành cho gói Premium." |
| `STALE_VERSION` | — (Phase 5) | "Người khác vừa sửa hoạt động này. Tải lại để xem bản mới nhất." |
| `RATE_LIMIT_EXCEEDED` | — (Phase 8) | "Bạn thao tác hơi nhanh. Thử lại sau một phút." |

---

## 11. Điểm ngắt responsive

| Bề rộng | Thay đổi chính | |
|---|---|---|
| < 640px | Một cột thẻ; hộp thoại trượt từ đáy; nút cao 44px | **Đã làm** |
| < 768px | Ô tìm kiếm xuống hàng riêng dưới logo | **Đã làm** |
| < 1024px | Cột ngày thành dải chip ngang dính trên cùng; lưới chuyến đi 2 cột (≥ 640px) | **Đã làm** |
| ≥ 1024px | Cột ngày 200px bên trái; lưới 3 cột | **Đã làm** |
| ≥ 1024px | Thêm cột bản đồ 360px (≥ 1280px: 420px), khoảng cách cột 24px (≥ 1280px: 32px) | **Đã làm** (Task 3.6) |
| < 1024px | Hai nút gạt "Lịch trình" / "Bản đồ" dưới dải chip ngày; mỗi lúc hiện một trong hai | **Đã làm** (Task 3.6) |

**Cảm ứng hay chuột** quyết định theo loại con trỏ (`pointer-coarse`), không theo độ rộng: màn hình cảm ứng luôn hiện tay nắm / menu "⋮" và dùng nút ↑ / ↓ thay kéo thả.

---

## 12. Khả năng tiếp cận — kiểm tra trước khi merge

- [ ] Tương phản chữ thường ≥ 4.5:1, chữ lớn ≥ 3:1 (DevTools). `jade #0B7A6B` trên trắng đạt 4.83:1; chữ trên nền `warning` nhạt để màu tối.
- [x] Không dùng riêng màu để truyền tin: loại hoạt động luôn có icon + chữ; khung báo lỗi và toast có icon; hoạt động trùng giờ có icon cảnh báo và chữ ẩn cho trình đọc màn hình.
- [x] Mọi phần tử tương tác có vòng focus nhìn thấy được khi dùng phím Tab.
- [x] Hộp thoại giữ focus bên trong, đóng bằng Esc. [ ] Trả focus về nút đã mở hộp khi mở từ menu "⋮".
- [x] Ảnh có `alt`; icon trang trí có `aria-hidden`.
- [x] Vùng chạm ≥ 44×44px trên màn hình cảm ứng: nút thường, nút ↑ / ↓, và từ Task 3.6 nút "⋮" của thẻ, chip ngày, nút "×" của hộp thoại, nút "×" của khung địa điểm, nút trên bản đồ, nút "Xem trong lịch trình". Các nút nhỏ giữ cỡ cũ khi dùng chuột và lớn lên theo loại con trỏ (`pointer-coarse`), không theo bề rộng màn hình. [ ] Còn thiếu: hai nút + / − của bản đồ (30px, cỡ mặc định của Leaflet); marker trên màn cảm ứng là 36px, dưới ngưỡng 44px (chốt 2026-10-03: to hơn nữa thì các marker gần nhau che nhau).
- [x] Toast trong vùng `aria-live`, lỗi dùng `role="alert"`.
- [x] Tôn trọng `prefers-reduced-motion` (trong `tokens.css`).
- [ ] Lời đọc của dnd-kit trong lúc kéo thả bằng bàn phím vẫn bằng tiếng Anh; cần Việt hoá (`accessibility.announcements`).

Mọi mục ở trên phải được kiểm tra lại mỗi khi thêm màn hình mới.

---

## 13. Dùng Stitch để dựng mockup

Stitch làm tốt nhất khi mỗi lần chỉ dựng **một màn hình** và prompt nêu rõ màu hex, font, và danh sách thành phần.

1. Dán **đoạn mở đầu** (13.1) vào đầu mọi prompt, nối tiếp bằng prompt của màn hình (mục 15).
2. Sửa từng phần bằng câu ngắn: "làm cột bản đồ rộng 420px", "đổi viền trái thẻ thành 3px".
3. Xuất ảnh / HTML **chỉ để tham khảo** kích thước và bố cục. **Không dán code Stitch vào dự án**: nó không dùng token, không có logic.
4. **Lọc bỏ** những gì mockup tự thêm mà hệ thống không có. Hai mockup đã dùng (đăng nhập, danh sách) có: dải trạng thái hệ thống, mã phiên bản, menu "Bảng giờ tàu xe" / "Khám phá điểm đến", chuông thông báo, khối thống kê "tỉ lệ đúng lịch trình", mũi tên trong nút, nhãn IN HOA, gradient đè lên ảnh, chấm nhấp nháy. Tất cả đều không được đưa vào code.

Mockup đã có nằm trong thư mục `trip-planner-screenshots/stitch_smart_trip_planner_login/` (ngoài repo).

### 13.1. Đoạn mở đầu

```
Design a web app screen for "Smart Trip Planner", a travel itinerary planner for
Vietnamese users. Desktop 1440px wide.

Visual direction: transit timetable. Time is the structural spine of the layout.
Clean, flat, functional. No gradients, no decorative shadows, no glassmorphism,
no pulsing or looping animation.

Typography: Be Vietnam Pro only. Sentence case, never all caps labels.
H1 32px/700, block title 18px/600, body 15px/400, caption 12px/400.

Colors:
- page background #F4F6F5, card surface #FFFFFF
- text #10242B, muted text #52666B, border #D7E4E1 (1px)
- brand green #0B7A6B for primary buttons, links and focus
- amber #E0A33C for weather and warnings, red #C2453B for errors
Activity type colors: sightseeing #2D7DD2, food #E0662F, transport #64797F,
accommodation #7A5BA6, shopping #BE3C79, other #4F8A62.

Corner radius by hierarchy: inputs, buttons and chips 6px, cards 10px, dialogs 14px.
Cards use a 1px border, not a drop shadow. Buttons never contain arrows.
All interface text in Vietnamese with full diacritics.
Only show features listed in the prompt; do not invent menus, stats or badges.
```

### 13.2. Prompt cho từng màn hình

Xem **mục 15**: prompt của mọi màn hình đã làm và sắp làm, kèm danh sách dữ liệu có thật của từng màn.

Sau khi Stitch trả kết quả, kiểm và sửa ngay bốn lỗi hay gặp: tự thêm gradient hoặc bóng đậm; dùng cùng một bo góc cho mọi thứ; nhãn IN HOA; mất dấu tiếng Việt.

---

## 14. Thứ tự dựng giao diện

| Task | Màn hình | Trạng thái |
|---|---|---|
| 1.5 | Đăng nhập, đăng ký, quên / đặt lại mật khẩu, xác thực email | Đã làm, làm lại theo guide ở Task 2.6 |
| 2.5 | Danh sách chuyến đi, wizard tạo chuyến, chi tiết chuyến đi (ray thời gian, chưa có bản đồ) | Đã làm, làm lại theo guide ở Task 2.6 |
| 2.6 | Token, font, thành phần dùng chung, số đếm chip, số hoạt động trên thẻ, ô tìm trên thanh điều hướng, giao diện điện thoại | Đã làm (`feat/T2.6-ui-guide`) |
| 3.6 | Ô tìm địa điểm trong hộp thoại hoạt động, cột bản đồ, tab bản đồ trên điện thoại, tự thêm địa điểm, chọn điểm đến trong wizard | Phase 3 |
| 3.7 | Dải thời tiết, thời tiết trên thẻ danh sách, đoạn di chuyển giữa hai ga, ngày đã qua, hỏi hoàn thành chuyến đi (cảnh báo ngoài trời: hoãn) | Phase 3 |
| 4.4 | Panel chia sẻ, danh sách thành viên, trang công khai, bình luận, huy hiệu vai trò | Phase 4 |
| 5.3 | Ảnh người đang xem, hiệu ứng khi người khác sửa | Phase 5 |
| 6.4 | Trang nâng cấp, hộp báo chạm hạn mức, trang kết quả thanh toán | Phase 6 |
| 7.x | Trang chi phí (biểu đồ), màn gợi ý AI | Phase 7 |
| 8.2 | Dashboard admin | Phase 8 |
| 8.3 | Chế độ tối (nếu làm), Việt hoá lời đọc kéo thả, test component (Testing Library; Vitest đã có từ Task 3.7 cho hàm thuần) | Phase 8 |
| 8.5 | Landing page, ảnh chụp màn hình cho README | Phase 8 — làm cuối, dùng ảnh chụp sản phẩm thật |


---

## 15. Prompt Stitch cho từng màn hình

### 15.1. Cách dùng

1. Mở Stitch, dán **đoạn mở đầu** ở mục 13.1, xuống dòng, dán prompt của màn hình.
2. Mỗi lần chỉ dựng **một** màn hình (hoặc một trạng thái của màn hình).
3. Mỗi prompt đi kèm hai dòng tiếng Việt:
   - **Dữ liệu có thật:** những gì API trả về, được phép xuất hiện trên màn hình.
   - **Không được thêm:** những gì Stitch hay tự bịa ra cho màn này.

   Nếu kết quả có thứ nằm ngoài "Dữ liệu có thật", yêu cầu Stitch bỏ đi, hoặc bỏ qua khi code.
4. Màn hình **đã làm** (15.2): prompt mô tả đúng giao diện hiện tại. Dùng khi muốn dựng lại hoặc thử biến thể trước khi sửa code.
5. Màn hình **sắp làm** (15.3): prompt dựa trên `design.md` mục 10.2 (API) và 15 (màn hình). Đầu mỗi task, đọc lại API của task: nếu API đổi khi thiết kế chi tiết thì sửa prompt trước khi dựng.
6. Ảnh kết quả bỏ vào `trip-planner-screenshots/<tên-màn-hình>/` rồi nhắn "xem ảnh" khi bắt đầu code.

---

### 15.2. Màn hình đã làm

#### A. Đăng nhập — Task 1.5, làm lại ở Task 2.6

**Dữ liệu có thật:** email, mật khẩu; lỗi đăng nhập; trạng thái "email chưa xác thực" kèm form gửi lại mail.
**Không được thêm:** dải trạng thái hệ thống, mã phiên bản, múi giờ, bảng chú thích màu, đăng nhập Google / Facebook, "Ghi nhớ đăng nhập".

```
Centered sign-in page on the #F4F6F5 background, no top navigation.
Above the card: a 44px jade (#0B7A6B) rounded square with a white map-pin icon, next to
the wordmark "Smart Trip Planner" (24px/700) and under it the line
"Kế hoạch hành trình theo dòng thời gian" (14px, muted).
Card 400px wide, white, 1px #D7E4E1 border, 10px radius, 32px padding, no shadow.
Inside: heading "Đăng nhập" 28px/700, subline "Chào mừng bạn quay lại" in muted text.
Field "Email *" (the asterisk in red #C2453B). Field "Mật khẩu *" with the link
"Quên mật khẩu?" right-aligned on the same row as the label, and an eye icon button
at the right end of the input. Inputs are 40px tall, 6px radius.
Show the password field in its focus state: jade border and a soft jade ring.
A full-width jade primary button "Đăng nhập", 44px tall, no icon.
Below the card, centered: "Chưa có tài khoản? Đăng ký" with "Đăng ký" as a jade link.
```

#### B. Đăng ký — Task 1.5

**Dữ liệu có thật:** họ tên, email, mật khẩu, nhập lại mật khẩu; sau khi gửi là màn "Kiểm tra hộp thư".
**Không được thêm:** ô số điện thoại, ô ngày sinh, ô đồng ý điều khoản, đăng ký bằng mạng xã hội.

```
Same layout as the sign-in page (logo with tagline above a 400px bordered card).
Heading "Tạo tài khoản", subline "Lên kế hoạch chuyến đi đầu tiên của bạn".
Fields: "Họ tên *", "Email *", "Mật khẩu *" with an eye button and the helper text
"8–72 ký tự, có chữ hoa, chữ thường, chữ số và không chứa khoảng trắng." under it
(12px, muted), "Nhập lại mật khẩu *" with an eye button.
Show "Nhập lại mật khẩu" in its error state: red border, a small alert-circle icon and
the red 12px text "Mật khẩu xác nhận không khớp" replacing the helper.
Full-width jade button "Đăng ký", 44px. Below the card: "Đã có tài khoản? Đăng nhập".
```

Trạng thái sau khi gửi (dựng riêng):

```
Same card. Heading "Kiểm tra hộp thư". A green success alert (tinted #1D7A4C background
at 8%, green text, check-circle icon): "Đã gửi mail xác thực tới an@example.com. Mở link
trong mail để kích hoạt tài khoản rồi đăng nhập." Below, the muted line
"Không nhận được mail?", an email field and a secondary button "Gửi lại mail xác thực".
Below the card the same line as the form: "Đã có tài khoản? Đăng nhập".
```

#### C. Quên mật khẩu, đặt lại mật khẩu, xác thực email — Task 1.4, 1.5

**Dữ liệu có thật:** email (quên mật khẩu); mật khẩu mới + nhập lại (đặt lại); kết quả xác thực: thành công, link sai, link hết hạn.
**Không được thêm:** câu hỏi bảo mật, mã OTP, đếm ngược.

```
Same sign-in layout. Show three cards side by side as separate states:
1) "Quên mật khẩu": subline "Nhập email đăng ký, chúng tôi sẽ gửi link đặt lại mật khẩu",
   field "Email *", full-width jade button "Gửi link đặt lại". After sending, heading
   "Kiểm tra hộp thư" and a green alert "Nếu an@example.com thuộc một tài khoản đã xác thực,
   chúng tôi đã gửi link đặt lại mật khẩu. Link có hiệu lực trong 1 giờ."
2) "Đặt lại mật khẩu": fields "Mật khẩu mới *" (eye button, password rule as helper)
   and "Nhập lại mật khẩu mới *" (eye button), full-width jade button "Đổi mật khẩu".
3) "Xác thực email": a green success alert "Email đã được xác thực. Bạn có thể đăng nhập
   ngay." and a jade link "Đăng nhập".
```

#### D. Danh sách chuyến đi — Task 2.5, làm lại ở Task 2.6

**Dữ liệu có thật:** mỗi chuyến đi: tên, ảnh bìa (có thể không có), điểm đến (có thể không có), ngày đi – ngày về, số ngày, trạng thái, **số hoạt động**; số chuyến đi theo từng trạng thái (theo từ khoá đang tìm); phân trang 12 chuyến / trang.
**Không được thêm:** ảnh thành viên (Phase 4), thanh "phân bổ lịch trình" nhiều màu, chú thích màu loại hoạt động, khối thống kê, chuông thông báo, mục menu khác ngoài logo, chấm nhấp nháy.

```
Top bar 56px, background #10242B: on the left a 32px jade rounded square with a white
map-pin icon and the white wordmark "Smart Trip Planner"; in the middle a search field
with a magnifier icon, translucent white background, white text, placeholder
"Tìm chuyến đi theo tên hoặc điểm đến"; on the right the user name "Trieu" in light grey
and a ghost button "Đăng xuất" with a log-out icon.
Content max 1280px. Heading "Chuyến đi của bạn" 32px/700 with the subline
"Quản lý lịch trình và các điểm dừng của từng chuyến đi." and on the right a jade primary
button with a plus icon "Tạo chuyến đi".
Filter row between two 1px #D7E4E1 lines: status chips "Tất cả 6", "Nháp 1",
"Đã lên kế hoạch 2", "Đang diễn ra 1" (a small steady jade dot before the label),
"Đã hoàn thành 2", "Đã lưu trữ 0". Each count sits in a small rounded pill at the right of
the chip. The selected chip "Tất cả" is filled #10242B with white text; the others are
white with a 1px border. At the right end: label "Sắp xếp" and a select
"Tạo gần đây nhất".
A 3-column grid of 6 trip cards, 24px gaps. Each card: white, 1px border, 10px radius,
no shadow. A 16:9 cover photo of a Vietnamese destination (one card has no photo: a flat
light grey block with a large grey map-pin icon). On the photo, top-right, a status badge
with a white background, 1px tinted border and colored text; bottom-left, a small dark
label with a pin icon and the destination name. Below the photo: title 18px/600 on one
line, the date range "15/10/2026 – 19/10/2026" with a calendar icon, then a 1px divider
and a footer line with a clock icon "5 ngày · 12 hoạt động".
Pagination centered under the grid: "‹ Trước", "Trang 1 / 2", "Sau ›".
Dark #10242B footer with "© 2026 Smart Trip Planner" in small grey text.
```

Trạng thái rỗng (dựng riêng):

```
Same page with no trips. Instead of the grid, centered with no card around it: a simple
two-color line illustration of a folded map (grey strokes) with a dashed jade route
between two jade stops, about 160px wide; the muted line "Chưa có chuyến đi nào. Tạo
chuyến đầu tiên để bắt đầu lên lịch trình."; a jade primary button "Tạo chuyến đi".
Status chips all show 0.
```

Trạng thái đang tải: 3 thẻ khung xương màu `gray-100` cùng hình dạng thẻ thật (không cần dựng bằng Stitch).

#### E. Tạo chuyến đi (wizard 3 bước) — Task 2.5

**Dữ liệu có thật:** bước 1: tên *, mô tả (tối đa 1000 ký tự), đường dẫn ảnh bìa; bước 2: tên điểm đến; bước 3: ngày bắt đầu *, ngày kết thúc * (tối đa 60 ngày), ngân sách, tiền tệ.
**Không được thêm:** chọn thành viên, chọn phong cách du lịch, tải ảnh lên (Phase 6), bản đồ ở bước 2 (Phase 3, xem 15.3).

```
Same top bar as the trip list. Centered column 672px.
Heading "Tạo chuyến đi" 32px/700, subline "Ba bước: thông tin chung, điểm đến và ngày đi."
A white card, 1px border, 10px radius, 32px padding.
At the top of the card, a step indicator drawn as a short transit line: three numbered
28px circle stations joined by a 2px track, labels under them "Thông tin", "Điểm đến",
"Ngày đi". Station 1 is done (filled jade with a white check), station 2 is current
(white with a jade border, bold label), station 3 is upcoming (grey border, grey number).
The track is jade up to station 2 and #D7E4E1 after it.
Step 2 content: field "Điểm đến" with placeholder "Ví dụ: Đà Lạt" and the helper
"Chọn vị trí trên bản đồ sẽ có ở phiên bản sau.".
Footer: a secondary button "Quay lại" on the left, a jade primary button "Tiếp" on the right.
```

Bước 3 (dựng riêng): hai ô ngày cạnh nhau "Ngày bắt đầu *", "Ngày kết thúc *"; dòng "Chuyến đi dài 3 ngày. Tối đa 60 ngày."; ô "Ngân sách" và ô chọn "Tiền tệ" (VND) trên một hàng; nút chính "Tạo chuyến đi".

#### F. Chi tiết chuyến đi (hai cột, chưa có bản đồ) — Task 2.5, làm lại ở Task 2.6

**Dữ liệu có thật:** tên, trạng thái, điểm đến, ngày đi – ngày về, số ngày, ngân sách + tiền tệ, mô tả; mỗi ngày: số thứ tự, ngày, thứ, tiêu đề (có thể trống), ghi chú; mỗi hoạt động: tên, loại, giờ bắt đầu / kết thúc (có thể trống), ghi chú, chi phí, link đặt chỗ.
**Không được thêm:** ảnh thành viên, nút "Chia sẻ" (Phase 4), bản đồ và thời tiết (Phase 3), quãng đường giữa hai hoạt động (Phase 3), người tạo hoạt động, tab Chi phí.

```
Same top bar as the trip list. Under it, a small jade link "‹ Chuyến đi của bạn".
Header: trip title "Đà Nẵng 4 ngày" 32px/700 on the left (up to two lines); on the right,
in one fixed group, a status select shaped like a badge "Đang diễn ra ▾" (tinted jade),
a secondary button with a pencil icon "Sửa" and an outlined red button with a trash icon
"Xoá". Below, one line of facts with small grey icons: pin "Đà Nẵng", calendar
"12/03/2026 – 15/03/2026", clock "4 ngày", wallet "Ngân sách 5.000.000 ₫", then a
two-line description ending with the link "Đọc thêm".
Two columns below.
LEFT 200px, sticky: list of days "Ngày 1  12/03" with the day title under it in small grey
text and an activity count at the right. The current day has a light jade background and
a 3px jade bar on its left edge.
MIDDLE: only the chosen day. It starts with "Ngày 1 · Thứ năm, 12/03/2026" (18px/600),
the day title in jade under it, and on the right a small ghost button with a pencil icon
"Sửa" next to the jade primary button with a plus icon "Thêm hoạt động".
Then the rail: start times on the left (13px/600, tabular figures, right-aligned,
"—" when there is no time), a 1px #D7E4E1 vertical line at 48px, a 10px white dot with a
3px ring in the activity color on the line, and the activity card to the right.
Activity card: white, 1px border on three sides, a 3px left edge in the activity color,
10px radius. First row: "09:00 – 11:30" in bold tabular figures, then the type icon and
label in the activity color (e.g. landmark icon "Tham quan"), and a "⋮" menu at the far
right. Second row: title 16px/600 "Chùa Linh Ứng". Then a two-line note in 13px muted
text, then a meta row: wallet icon "350.000 ₫" and a jade link "Link đặt chỗ ↗".
Show 4 activities on day 1: blue sightseeing, orange food, grey transport, green other
without a time. Two of them overlap in time: their cards have a very light amber
background and a small warning triangle before the time.
Hover state on one card: light grey background, a grip-vertical handle at its left edge.
Under the rail, after a 1px line, a secondary button "Ngày 2 ›" on the right (and
"‹ Ngày 1" on the left when there is a previous day).
A small toast in the bottom-right corner: white card, 1px border, green check icon,
"Đã lưu thay đổi" and a close "×".
```

Ngày trống (dựng riêng): cùng trang, ngày 3 được chọn; ray thay bằng một khung nét đứt "Ngày này còn trống. Thêm địa điểm bạn muốn ghé." có nút phụ "Thêm hoạt động"; nút chính "+ Thêm hoạt động" vẫn ở tiêu đề ngày.

#### G. Hộp thoại thêm / sửa hoạt động (chưa tìm địa điểm) — Task 2.5

**Dữ liệu có thật:** tên *, loại (6 loại), giờ bắt đầu, giờ kết thúc, chi phí + tiền tệ, link đặt chỗ, ghi chú (tối đa 255 ký tự).
**Không được thêm:** ô tìm địa điểm và ô địa chỉ (Phase 3), chọn người tham gia, tải ảnh, nhắc giờ.

```
A modal dialog 640px wide, 14px radius, white, 1px border, over a #10242B overlay at 45%.
Title "Thêm hoạt động" 18px/600 and a close "×" icon top right.
Fields: "Tên hoạt động *"; "Loại" select showing "Tham quan"; "Giờ bắt đầu" and
"Giờ kết thúc" side by side; "Chi phí" with placeholder "Ví dụ: 350000" and a narrow
"Tiền tệ" select "VND" on the same row; "Link đặt chỗ" with placeholder "https://...";
"Ghi chú" textarea with the helper "Tối đa 255 ký tự".
Footer right-aligned: secondary "Huỷ" and jade primary "Thêm hoạt động".
```

Hộp hỏi lại khi trùng giờ (dựng riêng):

```
A smaller dialog 480px, title "Trùng giờ với hoạt động khác", a bullet line
'Trùng giờ với hoạt động "Ăn trưa lẩu gà lá é" (11:30 - 13:00)', the question
"Bạn vẫn muốn lưu hoạt động này?", buttons "Huỷ" (secondary) and "Vẫn lưu" (jade).
```

#### H. Hộp thoại sửa chuyến đi và các hộp xác nhận — Task 2.5

**Dữ liệu có thật:** như bước 1–3 của wizard, trên một form; dòng giải thích việc đổi ngày; hai hộp hỏi lại (vừa dời vừa đổi độ dài; đổi ngày làm mất hoạt động); hộp xoá chuyến đi.

```
Dialog 480px "Sửa chuyến đi": fields "Tên chuyến đi *", "Mô tả" (helper "Tối đa 1000 ký
tự"), "Đường dẫn ảnh bìa", "Điểm đến", two date fields, "Ngân sách" + "Tiền tệ".
Under the dates a red-tinted alert with an icon: "Vừa dời vừa đổi độ dài: chỉ giữ những
ngày còn nằm trong khoảng mới. Muốn dời cả chuyến rồi đổi độ dài, hãy làm hai lần."
Footer: "Huỷ" and jade "Lưu thay đổi".
Beside it, a confirmation dialog 480px "Xoá hoạt động khi đổi ngày?" with the text
"2 hoạt động trong 1 ngày sẽ bị xoá nếu đổi ngày." and "Thao tác này không hoàn tác được.",
buttons "Huỷ" (secondary) and "Vẫn đổi ngày" filled red #C2453B.
```

#### I. Chi tiết chuyến đi trên điện thoại — Task 2.6

**Dữ liệu có thật:** như màn F.
**Không được thêm:** tab Bản đồ / Chi phí (Phase 3 / 7), nút tròn nổi.

```
Same trip detail at 390px wide. The top bar holds the logo and "Đăng xuất" on the first
row and a full-width search field on a second row, both on the dark background.
Under the header, the title on two lines, then the status select and the "Sửa" / "Xoá"
buttons on their own row, right-aligned.
A sticky row of day chips that scrolls sideways: "Ngày 1 · 12/03" (filled #10242B, white
text) and outlined chips for the other days; only day 1 is shown below.
The day heading has a small "Sửa" and a jade button "+ Thêm".
The rail sits 48px from the left. Each activity card shows, instead of a drag handle, two
stacked 44px arrow buttons (up and down) at its left edge, and the "⋮" menu always visible.
At the bottom, "Ngày 2 ›" as a secondary button.
A dialog opened from the bottom edge like a sheet, full width, top corners rounded 14px.
```

---

### 15.3. Màn hình sắp làm

#### Phase 3 — Task 3.6 (bản đồ) và 3.7 (thời tiết, quãng đường): chi tiết chuyến đi ba cột

**Dữ liệu có thật (dự kiến, kiểm lại API đầu task):** như màn F + toạ độ và tên địa điểm của hoạt động (`place`), khoảng cách và thời gian giữa hai hoạt động liền nhau (`/days/{dayId}/route`), dự báo từng ngày (`/weather/trips/{tripId}`): tình trạng (7 giá trị, design 10.2), nhiệt độ cao / thấp, xác suất mưa; ngày ngoài 16 ngày tới không có dự báo.
**Không được thêm:** viền hoặc chip cảnh báo thời tiết (hoãn ngày 2026-10-02, design rule 14.21), lớp giao thông, điểm ưa thích quanh đó, đánh giá sao, ảnh người đang xem (Phase 5).

```
Trip detail as in the current version (header, 200px day list, middle rail), plus a third
column on the right, 420px, sticky under the top bar and filling the screen height.
The column holds a light grey street map in the CartoDB Positron style. On it, 28px
teardrop markers in the activity colors with a white type icon inside and the order number
in a small circle at the corner; one marker is hovered: scaled up 1.15x with a soft glow.
The markers of the day are joined in order by a 2px jade line at 60% opacity; no line
between different days.
Under the map, a weather strip: one cell per day with a line icon (sun, cloud-sun,
cloud-rain), "32° / 25°" and a rain chance "60%"; a day with no forecast yet shows
"Chưa có dự báo" in muted text.
In the middle rail, between two activity cards, a 2px dashed grey segment with the centered
caption "25 phút · 8,4 km" (12px, muted).
Each activity card gains a third row: a pin icon and the address in 13px muted text.
The hovered activity card in the middle is linked to the enlarged marker on the map.
```

Màn hình hẹp (dựng riêng): dưới 1024px, dưới dải chip ngày có hai tab "Lịch trình" / "Bản đồ"; tab Bản đồ chiếm hết chiều ngang, dải thời tiết nằm dưới bản đồ.

#### Phase 3 — Task 3.7: thời tiết, quãng đường, ngày đã qua (prompt đã dùng ngày 2026-10-05)

Kết quả: 4 màn trong `trip-planner-screenshots/stitch_smart_trip_planner_3.7/` (`detail_trip`, `detail_trip_mobile`, `list_weather_card`, `complete_dialog`). Những gì lấy và không lấy từ mockup: mục 8.1 "Task 3.7", 8.2 và 9. Prompt bên dưới là bản đã dán vào Stitch; **đã biết lệch với quyết định sau khi xem ảnh** ở hai chỗ: đoạn di chuyển không có ô viền và không có icon phương tiện; thời tiết trên thẻ nằm ở hàng chân thẻ với chữ "Hôm nay" / "Ngày đi" chứ không ở hàng ngày đi.

**Dữ liệu có thật:** như màn F + địa điểm của hoạt động, chặng di chuyển (phút, km), dự báo từng ngày (tình trạng, cao / thấp, xác suất mưa), nhãn ngày.
**Không được thêm:** viền hoặc chip cảnh báo thời tiết, tổng quãng đường của ngày, icon phương tiện, gió / độ ẩm / dự báo theo giờ, ảnh thành viên, mục menu trên thanh điều hướng.

```
Trip detail page, desktop 1440px, three columns under the trip header (title "Đà Nẵng 4 ngày",
status select, "Sửa", "Xoá", meta row with destination, dates, length, budget).
Left column 200px: list of 4 days, each row "Ngày 1 12/10", a muted day title and the activity
count at the right. Day 1 is faded (60% opacity) with a small grey caption "Đã qua" under the
date. Day 2 is the selected one: light jade background, 3px jade bar at the left, and a small
bold jade caption "Hôm nay". Days 3 and 4 are normal.
Middle column: heading "Ngày 2 · Thứ ba, 13/10/2026" followed by a small jade badge "Hôm nay",
ghost button "Sửa", jade primary button "+ Thêm hoạt động". Below, a vertical rail with 4
activity cards (time at the left, a colored dot on the rail, white card with title, type icon
and a pin row with the address): "Chợ Hàn" 08:00, "Nghỉ trưa, tự do" 10:00 (no address row),
"Cầu Rồng" 14:00, "Bún chả cá 109" 15:30.
Between cards, a travel segment: a 2px dashed grey vertical line with a centered 12px muted
caption. Under "Chợ Hàn": "12 phút · 3,2 km tới Cầu Rồng". Under "Nghỉ trưa, tự do": nothing.
Under "Cầu Rồng": "2 phút · 998 m".
Right column 420px, sticky: a street map with colored teardrop markers numbered 1 to 3 joined
by a dashed jade line, an expand button top-right. Directly under the map, a weather strip:
a white box with 1px #D7E4E1 border and 10px radius holding 4 equal cells in a row, one per
day. Each cell: "N1 · 12/10" in 12px muted text, a 20px line icon (sun, cloud-sun, cloud-rain),
"32° / 25°" in 14px/600, and a small droplet icon with "60%". The cell of day 2 has a light
jade background. The cell of day 1 shows only "N1 · 12/10" and the muted text "Chưa có dự báo".
```

Điện thoại:

```
Same trip on a 390px phone, tab "Lịch trình". Sticky row of day chips: "Ngày 1 · 12/10" with
faded text, "Ngày 2 · 13/10" selected (dark #10242B, white text) with a small jade dot,
"Ngày 3 · 14/10", "Ngày 4 · 15/10". Under it the two-button switch "Lịch trình" / "Bản đồ".
Day heading "Ngày 2 · Thứ ba, 13/10/2026" with a small jade badge "Hôm nay"; under the day
title one line of weather in 13px: a cloud-sun icon, "Có mây", "32° / 25°", a droplet icon
"20%". Then the rail with the same cards and the same travel segments between them.
```

Danh sách chuyến đi có thời tiết trên thẻ. **Dữ liệu có thật:** như màn D + dự báo của hôm nay (chuyến đang đi) hoặc của ngày khởi hành (chuyến sắp đi trong 16 ngày). **Không được thêm:** thời tiết trên thẻ của chuyến đã qua hoặc còn xa, xác suất mưa, dự báo nhiều ngày, nền thẻ đổi theo thời tiết, thẻ không có ngày đi.

```
Trip list page as in the current version, 3-column grid of 6 cards. On each card the date
row "13/10/2026 – 16/10/2026" has the calendar icon at the left. On three cards the same row
also carries, right-aligned, a small weather group in 13px muted text:
- card "Đang diễn ra": a cloud-sun line icon and "32° / 25°";
- two cards "Đã lên kế hoạch" leaving soon: "18/10 ·", a cloud-rain icon and "29° / 24°".
The other three cards (a completed trip, a draft far in the future, a trip with no
destination) show no weather at all. Nothing else on the cards changes.
```

Hộp hỏi hoàn thành:

```
Trip detail page dimmed behind a centered confirm dialog, 440px, white, 10px radius.
Title "Hoàn thành chuyến đi?". Body: "Chuyến đi "Đà Nẵng 4 ngày" đã kết thúc ngày
15/10/2026. Lịch trình vẫn sửa được sau khi hoàn thành." Buttons right-aligned:
secondary "Để sau", jade primary "Hoàn thành chuyến đi".
```

#### Phase 3 — Task 3.6: hộp thoại hoạt động có tìm địa điểm

**Dữ liệu có thật (dự kiến):** như màn G + ô tìm địa điểm (`/places/search`): mỗi gợi ý có tên, địa chỉ, nhóm địa điểm; nút bỏ địa điểm đã chọn.

```
The activity dialog (640px) as today, with a new first field "Địa điểm": a search input
with a magnifier icon and, open under it, a dropdown of 3 suggestions. Each suggestion row:
a small category icon in a light grey circle, the place name 15px/600 "Chùa Linh Ứng" and
the address in 13px muted text "Bãi Bụt, Sơn Trà, Đà Nẵng". The first row is highlighted.
Replace the "Loại" select with a 6-option segmented control: each option shows its type
icon and label, the selected one ("Tham quan") has a 2px border in the sightseeing blue
and a light blue background.
When a place is chosen, the field becomes a chip with the place name and a "×" to remove it.
```

#### Phase 3 — Task 3.6: bước "Điểm đến" của wizard có bản đồ

```
Wizard step 2 as today. Field "Tên điểm đến" stays a plain text input ("Đà Nẵng").
Under it a second field "Vị trí trên bản đồ": a search input with a magnifier icon and the
placeholder "Tìm một địa điểm ở nơi bạn đến", with a dropdown of 3 suggestions (name 15px/600,
address 13px muted). Under the fields a 320px tall light grey map (CartoDB Positron) showing
one jade teardrop marker; a helper line "Tìm theo tên hoặc bấm lên bản đồ để chọn vị trí."
and, after a choice, the coordinates in small tabular text "16.0544, 108.2022".
```

#### Phase 4 — Task 4.4: chia sẻ và thành viên

**Dữ liệu có thật (dự kiến):** thành viên: tên, email, ảnh đại diện, vai trò (Chủ sở hữu / Chỉnh sửa / Chỉ xem), trạng thái lời mời (đang chờ); liên kết chia sẻ: quyền (xem / sửa), ngày hết hạn, số lượt xem, thu hồi. Hạn mức gói FREE: 2 thành viên, 1 liên kết.
**Không được thêm:** nhóm thành viên, phân quyền theo từng ngày, trò chuyện trực tiếp.

```
A panel titled "Chia sẻ chuyến đi" opened from the trip detail header (dialog 640px).
Section "Mời thành viên": an email input, a role select "Chỉnh sửa" / "Chỉ xem" and a jade
button "Mời". Under it the helper "Gói miễn phí mời được tối đa 2 người mỗi chuyến đi."
A list of 3 members: 32px avatar, name, email in muted text, a role select and a remove
icon button. The owner row shows a "Chủ sở hữu" badge and no select. One pending invite
shows an amber badge "Đang chờ" and a ghost button "Gửi lại".
A 1px divider, then section "Liên kết chia sẻ": a toggle, a permission select "Chỉ xem",
an expiry date field, a read-only URL field with a "Sao chép" button, a muted line
"Ai có liên kết đều xem được chuyến đi này." and "12 lượt xem". A ghost-danger button
"Thu hồi liên kết".
```

Trạng thái rỗng: "Chỉ mình bạn thấy chuyến đi này. Mời bạn bè để cùng chỉnh sửa." + nút "Mời thành viên".

#### Phase 4 — trang chuyến đi công khai (`/share/:token`)

**Dữ liệu có thật (dự kiến):** như màn F nhưng chỉ đọc; không có thông tin tài khoản của người xem.

```
Public read-only trip page, one column 760px centered, no app navigation. At the top a
small logo. Trip title 32px/700, the facts line with icons, the description.
The day-by-day rail as in the app, without drag handles, "⋮" menus or add buttons.
A static map (not sticky) after the summary. At the very end a centered secondary button
"Tạo chuyến đi của bạn" and a small line "Được chia sẻ qua Smart Trip Planner".
```

#### Phase 4 — bình luận

**Dữ liệu có thật (dự kiến):** bình luận của chuyến đi hoặc của một hoạt động, trả lời một cấp (`parent_id`), người viết, thời gian, xoá bình luận của mình.

```
A right-side panel 360px "Bình luận" over the trip detail. A list of comments: avatar,
name, relative time "5 phút trước", text; one comment has one indented reply.
A comment attached to an activity shows a small chip with the activity title and its
colored dot. At the bottom a textarea "Viết bình luận…" and a jade button "Gửi bình luận".
```

#### Phase 5 — Task 5.3: đồng chỉnh sửa thời gian thực

**Dữ liệu có thật (dự kiến):** người đang xem chuyến đi (tên, ảnh); sự kiện "người khác vừa sửa / thêm / sắp xếp".

```
The trip detail header with, next to the action buttons, three overlapping 28px avatars
with white rings and "+2". One activity card has a jade outer glow (the "just edited"
moment) and a small jade chip at its top-right "Trieu vừa sửa".
A 480px dialog "Người khác vừa sửa hoạt động này" with the text "Tải lại để xem bản mới
nhất." and buttons "Huỷ" and jade "Tải lại".
```

#### Phase 6 — Task 6.4: nâng cấp, chạm hạn mức, kết quả thanh toán

**Dữ liệu có thật:** bảng hạn mức ở `design.md` mục 9 (số chuyến đi 3 / không giới hạn; hoạt động mỗi ngày 10 / không giới hạn; thành viên 2 / 20; liên kết 1 / 10; ảnh bìa tải lên; gợi ý AI 10 lần/ngày; xuất PDF / ICS; cảnh báo thời tiết qua email; lịch sử phiên bản 30 ngày); giá gói; gói hiện tại; mức đang dùng.
**Không được thêm:** gói thứ ba, thẻ "phổ biến nhất" phóng to, đồng hồ đếm ngược khuyến mãi.

```
Page "Nâng cấp tài khoản". Two equal comparison cards side by side, neither enlarged.
Left "Miễn phí" with "0 ₫". Right "Premium" with "99.000 ₫ / tháng", a 1px amber border and
a small badge "Premium" (text #8A6A1F on #FBF1DC).
Under each price, the same feature list: "Chuyến đi đang hoạt động", "Hoạt động mỗi ngày",
"Thành viên mỗi chuyến đi", "Liên kết chia sẻ", "Tải ảnh bìa", "Gợi ý lịch trình bằng AI",
"Xuất PDF / ICS", "Cảnh báo thời tiết qua email", "Lịch sử phiên bản", with values or a
check icon / a dash. In the free card the row "Chuyến đi đang hoạt động: 3" has a soft
amber background (8%) and the note "Bạn đã dùng 3/3".
Buttons: "Gói hiện tại" disabled under the free card, jade "Nâng cấp ngay" under Premium.
```

Hộp chạm hạn mức (dựng riêng):

```
Dialog 480px "Đã đạt giới hạn gói miễn phí": text "Gói miễn phí cho phép tối đa 3 chuyến
đi. Nâng cấp để tạo thêm.", buttons "Để sau" (secondary) and "Xem gói Premium" (jade).
```

Kết quả thanh toán: một cột 480px giữa trang, icon lớn (dấu ✓ xanh `success` hoặc dấu × xám), tiêu đề "Thanh toán thành công" / "Đã huỷ thanh toán", một câu, nút "Về chuyến đi của bạn".

#### Phase 7 — Task 7.1: chi phí

**Dữ liệu có thật (dự kiến):** khoản chi: tên, số tiền, tiền tệ, loại, ngày chi, người trả, cách chia cho từng người, ghi chú; tổng theo ngày, theo loại, so với ngân sách; bảng "ai nợ ai bao nhiêu".
**Không được thêm:** quét hoá đơn, tỉ giá trực tiếp, liên kết ngân hàng.

```
Trip expenses page under the trip detail header (tab "Chi phí" selected).
Top: three stat tiles "Đã chi 3.450.000 ₫", "Ngân sách 5.000.000 ₫", "Còn lại 1.550.000 ₫",
with a thin progress bar in jade (amber above 80%).
A bar chart of spending per day and a donut chart per category, flat colors, no gradients.
A table of expenses: date, title, category chip, paid by (avatar + name), amount right-
aligned in tabular figures; a jade button "Thêm chi phí".
A card "Chia tiền": rows "An → Bình 250.000 ₫" with arrows, and a muted line "Ít giao dịch
nhất để mọi người hoà".
```

#### Phase 7 — Task 7.2: gợi ý lịch trình bằng AI

**Dữ liệu có thật:** đầu vào: điểm đến, số ngày, sở thích (nhiều lựa chọn), nhịp độ (thong thả / vừa / dày), mức ngân sách; kết quả: lịch trình theo ngày, mỗi hoạt động có tên, giờ, loại, toạ độ; nút "Áp dụng" ghi vào chuyến đi. Chỉ gói Premium, 10 lần / ngày.

```
A dialog 640px "Gợi ý lịch trình" with a small Premium badge. Form: destination field,
days stepper "3 ngày", interest chips (Ẩm thực, Lịch sử, Thiên nhiên, Mua sắm, Về đêm)
with two selected, pace segmented control "Thong thả / Vừa / Dày", budget segmented
control "Tiết kiệm / Vừa / Thoải mái", jade button "Tạo gợi ý", a muted line
"Còn 8 lượt hôm nay".
Result state: the same rail style as the trip detail, grouped by day, read-only, with
buttons "Tạo lại" (secondary) and "Áp dụng vào chuyến đi" (jade).
```

#### Phase 8 — Task 8.2: quản trị

**Dữ liệu có thật:** người dùng mới, chuyến đi mới, doanh thu định kỳ hằng tháng (MRR), tỉ lệ chuyển sang Premium; danh sách người dùng (tìm kiếm, khoá / mở); nhật ký webhook thanh toán (lọc theo trạng thái, xử lý lại).

```
Admin dashboard with a left navigation (Tổng quan, Người dùng, Thanh toán) on the ink
color. Four stat tiles: "Người dùng mới 7 ngày", "Chuyến đi mới", "MRR", "Tỉ lệ Premium",
each with a small sparkline. A table "Người dùng": avatar, name, email, plan badge, status
badge (Hoạt động / Đã khoá), created date, a ghost action "Khoá". A table "Sự kiện thanh
toán" with status badges (Thành công / Lỗi) and a "Xử lý lại" button on failed rows.
```

#### Phase 8 — Task 8.5: landing

```
Landing page, no dark top bar: a white header with the logo, "Đăng nhập" (ghost) and
"Tạo tài khoản" (jade). Hero on two columns: on the left a short one-sentence title
"Lên lịch trình theo dòng thời gian" 56px/800 and a line of supporting text with a jade
button "Bắt đầu miễn phí"; on the right a real itinerary: a rail with three colored
stations and activity cards, next to a small map with matching markers. No gradient,
no stock photo collage. Below: three short feature blocks with line icons
(Lịch trình kéo thả, Bản đồ và thời tiết, Cùng nhau chỉnh sửa), then the Free / Premium
comparison from the upgrade page.
```

#### Chưa gán task: cài đặt tài khoản, thông báo

`design.md` có trang `/settings` (hồ sơ, đổi mật khẩu, thiết bị đang đăng nhập) và API thông báo, nhưng `WORKFLOW.md` chưa gán task. Khi gán, viết prompt theo mẫu trên:
- Settings: hồ sơ (tên, ảnh, múi giờ), đổi mật khẩu (ba ô có nút con mắt), danh sách thiết bị (trình duyệt, IP, lần dùng cuối, nút "Đăng xuất thiết bị").
- Thông báo: chuông trên thanh điều hướng có số chưa đọc, danh sách thả xuống, "Đánh dấu đã đọc tất cả". Chỉ thêm chuông vào thanh điều hướng khi API thông báo đã có (nguyên tắc 6).

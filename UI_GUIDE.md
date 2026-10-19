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
- Màu của loại hoạt động dùng nhất quán ở **ba nơi**: nút trên ray, chip loại trong thẻ (trước Task 3.9: viền trái của thẻ), marker trên bản đồ. **Đã làm**.
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

Bảng màu **"Teal Voyage"**, áp dụng từ Task 3.9 (2026-10-05) theo mockup `trip-planner-screenshots/stitch_action_button_ui_redesign/`. Bảng trước đó (Task 2.6): `ink #10242B`, `paper #F4F6F5`, `jade #0B7A6B`, `jade-dark #095E52`, `jade-light #E6F2EF`, `tide #D7E4E1`, thang xám ngả xanh lá. Tên token không đổi, chỉ đổi giá trị. Độ tương phản đã kiểm bằng công thức WCAG: chữ trắng trên `jade` 5,5:1; `jade` trên trắng 5,5:1; `jade-dark` trên `jade-light` 7,0:1; `gray-600` trên `paper` 7,2:1; `gray-500` trên trắng 4,8:1. Nền `paper` so với thẻ trắng chỉ tách nhau 1,05:1 (bảng cũ: 1,09:1), nên thẻ trắng luôn cần **viền `tide` hoặc bóng `shadow-md`**, không bao giờ để trơn. Viền `tide` được chọn đậm hơn bảng cũ để bù: 1,35:1 so với trắng (cũ 1,31:1) và 1,29:1 so với `paper` (cũ 1,20:1); giá trị nháp `#DFE6F3` trong kế hoạch chỉ đạt 1,25:1 nên không dùng.

| Token | Hex | Dùng ở đâu |
|---|---|---|
| `ink` | `#0B1C30` | Chữ chính (xanh navy), chip đang chọn |
| `paper` | `#F8F9FF` | Nền trang (trắng ngả xanh dương) |
| `white` | `#FFFFFF` | Nền thẻ, ô nhập, hộp thoại, thanh điều hướng |
| `jade` | `#0F766E` | Màu thương hiệu: nút chính, link, vòng focus, trạng thái đang chọn |
| `jade-dark` | `#005C55` | Nút chính khi rê chuột, chữ trên nền `jade-light` |
| `jade-light` | `#E3F5F2` | Nền nhạt của mục đang chọn (ngày trong cột trái) |
| `tide` | `#D5DEEE` | Viền, đường kẻ, đường ray |
| `sun` | `#E0A33C` | Thời tiết, huy hiệu Premium |

### 3.2. Thang xám (ngả xanh dương theo `ink`)

Thay thế hẳn thang `gray-*` của Tailwind.

```
gray-50   #F6F8FC      gray-500  #64748B
gray-100  #EFF4FF      gray-600  #475569   ← chữ phụ
gray-200  #E1E8F4      gray-700  #334155   ← nhãn ô nhập
gray-300  #C7D1E0      gray-800  #1E293B
gray-400  #94A3B8      gray-900  #0B1C30
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

**Quy tắc:** màu tuyến chỉ xuất hiện ở chấm trên ray, chip loại của thẻ hoạt động (nền mờ 12%, chữ và icon theo màu; từ Task 3.9, thay cho viền trái 3px của thẻ), marker trên bản đồ, icon của gợi ý địa điểm (mục 7.12), nút đang chọn của ô "Loại" (viền và nền mờ 8%, mục 7.13), chấm 8px trên chip hoạt động của bình luận (mục 8.1, Task 4.6, dùng màu `marker`), cùng icon và chữ tên loại. Không tô nền thẻ bằng màu tuyến. Code nằm ở `features/itinerary/activityType.ts` (`ACTIVITY_ROUTE`); tên lớp được viết đầy đủ để Tailwind tìm thấy.

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

**Một họ chữ duy nhất: Inter** (Google Fonts, weight 300–800), nạp bằng thẻ `<link>` trong `frontend/index.html`. Đổi từ Be Vietnam Pro ở Task 3.9 (2026-10-05).

Lý do: chủ dự án muốn chữ gần với mockup "Teal Voyage" hơn. Font của mockup là Plus Jakarta Sans; đã thử trên trang thật và chủ dự án thấy **nét quá mỏng**, nên chọn Inter: cùng kiểu chữ không chân hiện đại, nét dày và rõ hơn ở cỡ 12–14px (cỡ của phần lớn chữ trên trang chuyến đi), bề ngang hẹp hơn nên ít làm vỡ dòng ở các ô chật. Bản trên Google Fonts có bộ chữ tiếng Việt (đã kiểm: có khối `vietnamese`). Cần nhìn lại dấu thanh trên chữ hoa ("Ế", "Ợ", "Ỡ") ở tiêu đề lớn, bài `MT-UI-89`.

Không dùng font thứ hai. Phân cấp bằng **độ đậm và kích thước**. Fallback: `"Inter", ui-sans-serif, system-ui, "Segoe UI", sans-serif`.

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
| Thẻ hoạt động, thẻ chuyến đi, khung trang đăng nhập, khung báo | `rounded-card` | 12px (10px trước Task 3.9) |
| Hộp thoại | `rounded-panel` | 16px (trên điện thoại chỉ bo hai góc trên) |
| Ảnh đại diện, chấm trên ray, ô số đếm | `rounded-full` | 9999px |

### 5.3. Đổ bóng — rất nhẹ, ngả xanh theo `ink`

```css
--shadow-sm: 0 1px 2px rgba(11, 28, 48, .05);
--shadow-md: 0 1px 8px rgba(11, 28, 48, .06);   /* thẻ không viền của trang chuyến đi, menu thả xuống, thông báo */
--shadow-lg: 0 8px 24px rgba(11, 28, 48, .12);  /* hộp thoại, thẻ đang được kéo */
```

Từ Task 3.9 có hai kiểu thẻ ở trạng thái nghỉ: thẻ của **trang chuyến đi** (cột ngày, tiêu đề ngày, hoạt động, bản đồ, thời tiết) không viền, dùng `shadow-md`; thẻ ở **các trang khác** (danh sách chuyến đi, form) giữ **viền `1px tide`**, không bóng. Không để thẻ trắng trơn trên nền `paper` (mục 3.1).

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
  --color-ink: #0B1C30;  --color-paper: #F8F9FF;
  --color-jade: #0F766E; --color-jade-dark: #005C55; --color-jade-light: #E3F5F2;
  --color-tide: #D5DEEE; --color-sun: #E0A33C;

  /* Xám (thay thang gray của Tailwind) */
  --color-gray-50: #F6F8FC;  /* … tới */ --color-gray-900: #0B1C30;

  /* Ngữ nghĩa */
  --color-success: #1D7A4C; --color-warning: #E0A33C; --color-danger: #C2453B; --color-info: #2D6FA8;

  /* Màu tuyến hoạt động */
  --color-act-sightseeing: #2D7DD2; --color-act-food: #E0662F; --color-act-transport: #64797F;
  --color-act-accommodation: #7A5BA6; --color-act-shopping: #BE3C79; --color-act-other: #4F8A62;

  --font-sans: "Inter", ui-sans-serif, system-ui, "Segoe UI", sans-serif;
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

Tất cả nằm trong `frontend/src/components/`. **Không cài shadcn/ui** (chốt ở Task 2.5): không có CLI, `components/ui/`, `cn()` / `tailwind-merge` / `cva`. Từ Task 4.9 (2026-10-18) được **chép code của shadcn làm mẫu** cho ô chọn, khung bật ra và lịch, rồi viết lại bằng token và prop của dự án; quy tắc 7.0 giữ nguyên. Thư viện được phép:

| Thư viện | Dùng cho |
|---|---|
| `lucide-react` | Mọi icon. Icon trang trí luôn có `aria-hidden` |
| `@radix-ui/react-dropdown-menu` | Menu "⋮" (bàn phím, focus, trình đọc màn hình); giao diện dùng token của dự án |
| `@radix-ui/react-select` | Ô chọn kiểu mới (Task 4.9 Mốc 5) |
| `@radix-ui/react-popover` | Khung bật ra của ô ngày (Task 4.9 Mốc 9) |
| `react-day-picker` | Lịch, tiếng Việt (Task 4.9 Mốc 9); không nạp CSS của thư viện |
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

### 7.2. Ô nhập — `FormField`, `Select` / `SelectField`, `TextAreaField`, `PasswordField`

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
  - **Ngoại lệ:** form chỉ là một phần **không bắt buộc** của một màn có việc khác, mà con trỏ được đặt sẵn vào đó, chỉ kiểm khi bấm nút của chính form (`mode: 'onSubmit'`, `reValidateMode: 'onChange'`). Đang dùng: "Mời thành viên" trong hộp chia sẻ (BUG-UI-012, Task 4.9): người mở hộp để đổi vai trò hay tạo liên kết không bị báo "Email không được để trống".
- Mọi `<form>` có `noValidate` để trình duyệt không hiện bong bóng lỗi riêng.
- `PasswordField`: nút con mắt ở cuối ô để hiện/ẩn mật khẩu (`aria-pressed`, nhãn "Hiện mật khẩu" / "Ẩn mật khẩu").
- **Ô chọn `Select` / `SelectField`** (`components/Select.tsx`, Task 4.9; mẫu là Select của shadcn/ui, viết lại bằng token):
  ```
  Tiền tệ
  ┌──────────────────┐            ô đóng: như ô chữ 40px (cảm ứng 44px), mũi tên ChevronDown xoay khi mở
  │ VND            ⌄ │
  └──────────────────┘
  ┌────────────────────────┐      danh sách nổi cách ô 4px, rộng ít nhất bằng ô và 11rem
  │ VND                  ✓ │      dòng: nhãn 15px, dòng phụ 13px gray-600 (tuỳ chọn)
  │ Đồng Việt Nam          │      ✓ jade ở dòng đang chọn
  │▒USD▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒│      dòng đang trỏ (chuột / phím mũi tên): nền jade-light
  │▒Đô la Mỹ▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒│
  └────────────────────────┘      cao tối đa 288px rồi cuộn, không cao quá phần màn hình còn lại
  ```
  - Ô đóng chỉ hiện nhãn của lựa chọn, không hiện dòng phụ. Danh sách mở / đóng cùng hiệu ứng với menu (mục 7.16). Dòng 36px, **44px trên màn cảm ứng**.
  - Bàn phím (Radix): Enter / Space / ↓ mở, ↑ / ↓ di chuyển, Enter chọn, gõ một chữ thì nhảy tới dòng bắt đầu bằng chữ đó, **Esc chỉ đóng danh sách** (không đóng hộp thoại chứa nó).
  - `SelectField` = `Select` + `FieldShell` (nhãn, `*`, gợi ý, lỗi). Không dùng với `register()`: bọc trong `Controller` của react-hook-form và truyền `value` / `onValueChange` / `onBlur` / `ref`.
  - Đang dùng: "Tiền tệ" ở form chuyến đi (bước 2 của wizard, "Sửa chuyến đi") và form hoạt động, mỗi loại tiền một dòng phụ ("VND" / "Đồng Việt Nam"); "Ngày" trong hộp "Chuyển sang ngày khác" (ô ghi "Ngày 3", dòng phụ "Thứ hai, 20/10 · tiêu đề của ngày"). Thay ô `<select>` của trình duyệt.
  - Danh sách rộng tối đa bằng ô (ô hẹp: 20rem): dòng phụ dài xuống dòng, không kéo danh sách rộng quá hộp thoại.
  - **Cỡ** (`size`): `md` (mặc định) cao 40px, rộng hết khung, cho ô của form; `sm` cao 32px, chữ 13px, rộng vừa chữ (tối thiểu 120px), cho một dòng của danh sách: vai trò trong hộp chia sẻ (không có nhãn hiện ra, có `aria-label`). Cả hai cao 44px trên màn cảm ứng. Danh sách của hai cỡ giống nhau. Đang lưu (`disabled`): nền `gray-50`, con trỏ chờ.
  - Nút của ô là một hàng flex nên dùng `controlLookClass()` (kiểu của `controlClass()` không kèm `block`).
- **`TextAreaField` tự giãn** (Task 4.9): cao 3 dòng lúc trống (90px), cao dần theo nội dung tới **8 dòng** (210px) rồi mới có thanh cuộn; không có góc kéo đổi cỡ. Trình duyệt có `field-sizing: content` thì tự lo, trình duyệt khác thì ô đặt lại chiều cao mỗi lần gõ. Dùng cho mô tả chuyến đi, ghi chú hoạt động, ghi chú ngày, ô bình luận (Task 4.6). Ô đọc giá trị (để tính chiều cao và bộ đếm) lúc được dựng: form muốn làm trống ô sau khi gửi thì dựng lại ô bằng `key` mới, như `CommentForm`.
- **Cao cố định** (`autoGrow={false}`, Task 4.6 Mốc 11): luôn cao 3 dòng (90px), chữ dài thì cuộn bên trong ô. Cho ô mà việc giãn ra sẽ ép phần xung quanh: ô bình luận ở chân panel (giãn ra thì danh sách bình luận bị thu nhỏ). Ô ghi chú trong form vẫn tự giãn.
- **Nhãn ẩn** (`hideLabel`, Task 4.6): nhãn chỉ dành cho trình đọc màn hình, không hiện ra, khi chỗ đặt ô đã nói rõ ô dùng để làm gì. Hiện chỉ ô "Viết bình luận…" ở chân panel bình luận dùng (theo mockup); form thường luôn có nhãn hiện.
- **Bộ đếm ký tự** (Task 4.9): ô có giới hạn độ dài hiện "230 / 255" (12px, `gray-500`, số đều nhau) ở **bên phải** hàng gợi ý / lỗi, chỉ khi đã dùng **từ 80%** giới hạn (`lib/charCounter.ts`); chạm giới hạn thì số đậm `gray-800`. Dòng gợi ý "Tối đa 255 ký tự" vẫn ở bên trái. `FieldShell` nhận chữ bên phải đó qua `aside`.

### 7.3. Thẻ hoạt động — thành phần quan trọng nhất ⭐ `ActivityCard`

```
┌─────────────────────────────────────────────────────────┐
│ ⠿  09:00 – 11:30  (🏛 Tham quan)                     ⋮   │ ← giờ 15px/600, chip loại bo tròn
│    Chùa Linh Ứng                                        │ ← 18px / 600
│    📍 Đường Hoàng Sa, Phường Sơn Trà, Đà Nẵng           │ ← địa điểm 13px, khi có
│    Đi sớm tránh nắng… Đọc thêm                          │ ← ghi chú 13px, tối đa 2 dòng
│    ─────────────────────────────────────────────────    │ ← vạch tide, chỉ khi có chân thẻ
│    Chi phí dự kiến: 350.000 ₫          Link đặt chỗ ↗   │ ← 13px
└─────────────────────────────────────────────────────────┘
```

- **Nghỉ** (Task 3.9, theo mockup "Teal Voyage"): nền trắng, **không viền**, `shadow-md`, bo 12px. Loại hoạt động là một **chip** bo tròn cao 20px cạnh giờ: icon 12px và tên loại, nền màu tuyến mờ 12%, chữ màu tuyến. Chân thẻ (khi có chi phí hoặc link): vạch `tide` phía trên, "Chi phí dự kiến:" `gray-500` kèm số tiền `jade-dark` đậm ở bên trái, link `jade-dark` ở bên phải. Trước Task 3.9: viền `tide` 1px ở ba cạnh, viền trái 3px màu tuyến, loại là chữ có màu, hàng meta 12px có icon ví.
- **Rê chuột hoặc Tab tới thẻ:** (từ Task 3.9 nền không đổi) hiện tay nắm kéo (`GripVertical`) ở mép trái và menu "⋮" ở mép phải. **Màn hình cảm ứng:** hai nút này luôn hiện.
- **Menu "⋮"** (`ActivityMenu`, Radix): "Sửa", "Chuyển sang ngày…" (không có khi chuyến đi chỉ có 1 ngày), "Xoá" (chữ đỏ). Dùng được bằng bàn phím: Enter mở, mũi tên chọn, Esc đóng.
- **Nút bình luận** (Task 4.6, **Đã làm**: nút từ Mốc 7, số bình luận từ Mốc 8 (`lib/commentTree.commentCountsByActivity`, `DaySection` đọc danh sách bình luận một lần cho cả ngày, chung bộ nhớ đệm với nút ở đầu trang; nút có số thì rộng thêm, cao vẫn bằng "⋮"); xoá hoạt động hoặc lưu "Sửa chuyến đi" (rút ngắn chuyến làm mất hoạt động) thì danh sách bình luận tải lại, nên số ở đầu trang giảm ngay; `CommentOnActivityButton`, mở panel qua `stores/commentPanelStore`, cùng cỡ và kiểu rê chuột với nút "⋮"): ở mép phải hàng đầu, ngay bên trái menu "⋮" (người chỉ xem không có menu: nút đứng ở chỗ của menu). Icon `MessageCircle` 16px `gray-500`, cao 28px (44px trên màn cảm ứng), kèm số bình luận 12px/600 khi > 0 (tính cả trả lời). Chưa có bình luận thì nút chỉ hiện khi rê chuột / Tab tới thẻ như menu "⋮" (luôn hiện trên màn cảm ứng); có bình luận thì luôn hiện. Nhãn đọc "Bình luận về {tên hoạt động}" (kèm "{N} bình luận" khi > 0). Bấm thì mở panel bình luận chỉ với các bình luận của hoạt động này (mục 8.1). Mọi vai trò đều có nút (người chỉ xem cũng bình luận được, design 6.2); bản sao đi theo con trỏ lúc kéo và thẻ ở trang công khai không có. Không có mục "Bình luận" trong menu "⋮" (chốt 2026-10-19).
- **Đang kéo:** thẻ bay theo con trỏ có `shadow-lg`, nghiêng 2°, trong suốt 90%. Vị trí sẽ thả hiện một **đường ngang jade 2px**.
- **Trùng giờ** với hoạt động khác trong ngày (đã lưu bằng "Vẫn lưu"): nền là `warning` pha 10% vào trắng (nền đặc, không trong suốt: thẻ có bóng phải che được phần bên dưới) và icon tam giác cảnh báo cạnh giờ. Tính ở giao diện bằng `lib/timeOverlap.ts`, cùng quy tắc với backend (chạm đầu nhau không tính).
- Không có giờ: dòng giờ ghi "Chưa đặt giờ".
- **Hàng địa điểm** (Task 3.6, **Đã làm**): ngay dưới tên, icon ghim `MapPin` xám + chữ 13px `gray-600`, được xuống dòng. Nội dung: "Tên địa điểm · địa chỉ". Tên hoạt động trùng tên địa điểm (không phân biệt hoa thường) thì chỉ ghi địa chỉ, để không lặp chữ; địa điểm không có địa chỉ thì ghi tên. Hoạt động chưa gắn địa điểm: **không có hàng này**, không ghi "Chưa gắn địa điểm" (mockup Stitch có, đã bỏ vì lặp trên mọi thẻ).
- Ô thời tiết trong hàng meta: hoãn ngày 2026-10-02 cùng cảnh báo ngoài trời (design rule 14.21).
- **Phase 5:** người tạo trong hàng meta; khi người khác vừa sửa thì viền ngoài nhấp nháy jade 1,2 giây, kèm chip "Trieu vừa sửa" biến mất sau 3 giây.

### 7.4. Thanh ray thời gian ⭐

```
 ◉── [Thẻ: 08:00 – 09:30 · Chùa Linh Ứng]
 │
 ┊     25 phút · 8,4 km
 │
 ◉── [Thẻ: 12:00 · Bún chả cá 109]
 │
 ◉── [Thẻ: Chưa đặt giờ · Chợ đêm]
```

- Các ga xếp theo **thứ tự trong ngày** (do kéo thả quyết định), **không** theo tỉ lệ thời gian, vì hệ thống cho phép hoạt động không có giờ và cho phép sắp xếp tự do.
- Lưới mỗi hàng (Task 3.9, theo mockup "Teal Voyage"): cột nút 12px (tâm ở 6px) → thẻ, cách cột nút 6px: từ mép trái tới thẻ là 18px (bản đầu của Task 3.9: 36px; chủ dự án thấy trục chiếm nhiều chỗ nên thu lại hai lần, còn 24px rồi 18px). **Không còn cột giờ**: giờ nằm ở dòng đầu trong thẻ. Trước Task 3.9: cột giờ 40px → cột ray 16px (tâm ở 48px) → thẻ.
- Ray: đường dọc 2px `tide`, chỉ vẽ khi ngày có hoạt động. Các thẻ cách nhau 16px.
- Nút: chấm tròn 10px nền trắng, viền 2px màu tuyến (bản đầu của Task 3.9: đĩa 20px có chấm ở giữa); nằm ngang hàng với dòng giờ của thẻ (cách đầu thẻ 23px). Trước Task 3.9: chấm 10px viền 3px.
- Ngày trống: khung nét đứt "Ngày này còn trống. Thêm địa điểm bạn muốn ghé." + nút "Thêm hoạt động".
- "+ Thêm hoạt động" là **nút chính của trang**, đặt ở góc phải tiêu đề ngày (trên điện thoại chữ rút gọn "+ Thêm"), không đặt ở cuối ray.
- **Chưa áp dụng:** đoạn nét đứt ghi thời gian di chuyển (cần API lộ trình, Phase 3); nút mờ "+ Thêm hoạt động vào khoảng này" khi hai hoạt động cách nhau trên 3 tiếng.

### 7.5. Huy hiệu — `Badge`

Cao 20px, bo 6px, chữ 12px / 500, lề ngang 8px.
- `surface="tint"` (mặc định): nền là màu gốc pha 12%, chữ màu gốc đậm.
- `surface="solid"`: nền trắng + viền mảnh; dùng khi huy hiệu nằm đè lên ảnh (trạng thái trên ảnh bìa thẻ chuyến đi).

Tông: `neutral`, `muted`, `brand`, `info`, `success`, `warning` (mục 3.5). Dùng cho trạng thái chuyến đi, vai trò thành viên (**Phase 4**), gói Premium (**Phase 6**). Trạng thái trên trang chi tiết (`TripStatusSelect`) là một huy hiệu cùng màu có mũi tên, bấm thì mở menu (mục 7.16, từ Task 4.9; trước đó là ô `<select>` tô màu huy hiệu).

### 7.6. Hộp thoại — `Modal`, `ConfirmDialog`

- Thẻ `<dialog>` gốc mở bằng `showModal()`: trình duyệt lo lớp phủ, giữ focus trong hộp, phím Esc.
- `size="md"` rộng 480px (form đơn giản, hộp xác nhận); `size="lg"` rộng 640px (form hoạt động).
- Bo 14px, viền `tide`, `shadow-lg`, lớp phủ `ink` 45%. Góc trên bên phải có nút "×".
- Trên điện thoại: nằm sát mép dưới, rộng hết màn hình, cao tối đa 90%, chỉ bo hai góc trên.
- Nút xếp ở góc phải dưới: hành động phụ bên trái, hành động chính bên phải.
- Khi hộp mở, con trỏ nằm ở **phần tử đầu tiên của nội dung**: ô nhập đầu tiên của form, hoặc nút "Huỷ" của hộp xác nhận (nhấn Enter ngay không bao giờ xoá nhầm). Không nằm ở nút "×", và bỏ qua mọi nút mang `data-no-initial-focus` (nút bỏ một thứ gì đó, Task 3.6). `Modal` tự làm việc này; **không** đặt `autoFocus` cho ô bên trong hộp thoại, vì nó không có tác dụng khi hộp còn đang ẩn (BUG-UI-003, Task 2.7).
- **Nội dung dài** (Task 4.5): hộp cao tối đa 90% màn hình; khi nội dung dài hơn, **chỉ phần nội dung cuộn**, hàng tiêu đề và nút "×" đứng yên ở trên (hộp là một cột: tiêu đề cố định, thân cuộn). Trước Task 4.5 cả hộp cuộn, tiêu đề trôi mất.
- **Danh sách và lịch bật ra bên trong hộp** (Task 4.9): `<dialog>` mở bằng `showModal()` nằm trên mọi thứ khác của trang, nên thứ bật ra từ một ô trong hộp phải vẽ **vào chính hộp đó**, không vẽ ở cuối `<body>` (sẽ nằm dưới hộp, không bấm được). `Modal` đưa thẻ `<dialog>` của nó xuống qua `OverlayContainerContext` (`components/overlayContainer.ts`); `Select` đọc nó qua `useOverlayContainer()`. Thành phần bật ra mới phải làm giống vậy.
- **Kiểu panel** (`layout="panel"`, Task 4.6, **Đã làm**): cùng thẻ `<dialog>`, cùng lớp phủ `ink` 45%, Esc, giữ focus và hộp lồng nhau như trên, nhưng bám **mép phải**, rộng 360px, cao hết màn hình (`h-dvh`), bo 16px hai góc trái (`rounded-l-panel`), chỉ có viền trái `tide`, `shadow-lg`; `size` không có tác dụng. Đầu panel (tiêu đề + "×", đệm 16 / 20px) đứng yên, có vạch `tide` bên dưới; thân cuộn (đệm 16 / 20px). Dưới 1024px chiếm toàn màn hình, không bo góc, không viền. Mở ra thì con trỏ nằm ở **thân panel** (`data-modal-body`, `tabIndex={-1}`, phím mũi tên cuộn được), không ở ô nhập (điện thoại không bật bàn phím khi chỉ muốn đọc) và không ở nút đầu tiên của danh sách. Chân panel (`footer`, Task 4.6 Mốc 4) đứng yên dưới một vạch `tide`, đệm 16 / 20px, chứa ô nhập; chỉ kiểu panel có chân. **Bấm vào vùng tối thì panel đóng** (Task 4.6 Mốc 12; hộp thoại thường thì không, vì có form và bấm nhầm sẽ mất chữ): chỉ khi cả lúc nhấn lẫn lúc thả chuột đều ở vùng tối (bôi đen chữ trong panel rồi thả ra ngoài không đóng); đi qua cùng `onClose` với Esc và "×", nên lúc đang lưu thì không đóng; bấm vùng tối của một hộp hỏi lại mở trên panel chỉ là bấm vào hộp đó, panel không đóng. Chỗ dùng panel phải tự giữ lại thứ người dùng đang gõ (panel bình luận giữ bản nháp). **Hiệu ứng** (Mốc 13): mở thì trượt vào từ mép phải 200ms, vùng tối hiện dần; đóng (Esc, "×", bấm ra ngoài) thì trượt ra 150ms, vùng tối mờ dần, trong lúc đó không bấm được gì, xong mới đóng hẳn (`Modal` giữ nội dung tới hết hiệu ứng, có hẹn giờ dự phòng nếu trình duyệt bỏ qua hiệu ứng). Keyframe `panel-in` / `panel-out` / `backdrop-in` / `backdrop-out` ở `tokens.css`; giảm chuyển động của hệ điều hành thì tức thì (quy tắc giảm chuyển động từ Mốc 13 áp cả cho `::backdrop`). Hộp thoại thường không có hiệu ứng. Việc gì cần làm **sau** khi panel đóng hẳn (đưa con trỏ tới chỗ khác) phải chờ không còn `dialog[open]`: lúc đóng hẳn trình duyệt trả con trỏ về nút đã mở panel. Dùng cho panel bình luận (mục 8.1).
- **Hộp lồng nhau** (hộp hỏi lại mở bên trong một hộp khác): phím Esc chỉ đóng hộp trên cùng, hộp bên dưới giữ nguyên nội dung. `Modal` chặn React chuyển sự kiện Esc lên hộp cha (`BUG-UI-011`, Task 4.5).
- `ConfirmDialog` dùng trước mọi thao tác mất dữ liệu (xoá, đổi ngày làm mất hoạt động) hoặc cần nghĩ lại (trùng giờ, vừa dời vừa đổi độ dài chuyến đi).
- **Chưa áp dụng:** trả focus về đúng nút đã mở hộp khi hộp được mở từ menu "⋮" (menu đã biến mất khi hộp đóng).

### 7.7. Thông báo — `Toaster` + `toast` (`stores/toastStore.ts`)

- Góc dưới bên phải trên máy tính, trên cùng trên điện thoại; rộng 360px; tự tắt sau 4 giây; có nút "×".
- Vùng chứa là `aria-live="polite"`; thông báo lỗi có `role="alert"`.
- Chữ dùng thể hoàn thành, khớp với nút đã bấm: "Lưu thay đổi" → "Đã lưu thay đổi"; "Thêm hoạt động" → "Đã thêm hoạt động". Hiện có: "Đã lưu thay đổi", "Đã thêm hoạt động", "Đã xoá hoạt động", "Đã đổi trạng thái", "Đã xoá chuyến đi", `Đã chuyển "…" sang Ngày N`; Task 4.6 thêm "Đã xoá bình luận" (gửi bình luận **không** có toast: bình luận mới hiện ngay trong panel).
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
| Chưa mời ai (hộp chia sẻ, chỉ có dòng của chủ) | "Chỉ mình bạn thấy chuyến đi này. Mời bạn bè để cùng chỉnh sửa." | — không có nút: form "Mời" nằm ngay trên, mỗi màn một nút chính (chốt 2026-10-18, Task 4.5) | **Đã làm** |
| Chưa có bình luận (panel bình luận) | "Chưa có bình luận nào. Hỏi hoặc góp ý cho cả nhóm ở ô bên dưới." | — không có nút: ô nhập nằm ngay dưới (như hộp chia sẻ). Đang lọc theo một hoạt động: "Chưa có bình luận nào về hoạt động này." | **Đã làm** (Task 4.6 Mốc 4; câu khi lọc: Mốc 7) |
| Chưa có chi phí | "Chưa ghi khoản nào. Thêm chi phí để theo dõi ngân sách." | Thêm chi phí | **Phase 7** |

### 7.10. Ghi chú dài — `ExpandableText`

Mô tả chuyến đi, ghi chú ngày, ghi chú hoạt động hiện tối đa **2 dòng**. Nút "Đọc thêm" chỉ xuất hiện khi chữ thật sự dài hơn 2 dòng (đo lại khi đổi kích thước cửa sổ), bấm thì mở hết và đổi thành "Thu gọn". Giữ các chỗ xuống dòng người dùng tự gõ; chuỗi dài không khoảng trắng được bẻ dòng (`wrap-anywhere`).

### 7.11. Logo — `Logo`

Ô vuông `jade` bo 6px chứa icon ghim trắng, cạnh chữ "Smart Trip Planner".
- Từ Task 3.9 logo chỉ còn một kiểu: chữ `ink` trên nền sáng (thanh điều hướng nay nền trắng), không còn `tone`.
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

### 7.14. Ảnh đại diện — `Avatar` (Task 4.5)

Tròn 32px. Có ảnh (`avatarUrl`) thì hiện ảnh; chưa có (hiện mọi tài khoản đều chưa có) thì hiện chữ cái đầu 12px/600 trên nền `jade-light` chữ `jade-dark`; lời mời đang chờ dùng nền `warning` 15% chữ `gray-800` như mockup. Chữ cái đầu (`lib/initials.ts`): chữ đầu của từ đầu và từ cuối trong tên ("Trần Nhật Triều" → "TT", "Mai Anh" → "MA"; mockup ghi "TN", không theo), tên một từ thì một chữ, người được mời chưa có tài khoản (chưa có tên) thì chữ đầu của email. Chỉ để trang trí (`aria-hidden`): tên hoặc email luôn nằm cạnh. Dùng trong hộp chia sẻ và (Task 4.6) panel bình luận: bình luận gốc 32px, trả lời 24px (prop `size`: `md` 32px mặc định / `sm` 24px với chữ cái đầu 10px; Task 4.6 Mốc 2); ảnh người đang xem là Phase 5. `email` không bắt buộc (Task 4.6): tác giả bình luận luôn có tên và máy chủ không gửi email của họ.

### 7.15. Nút gạt — `SegmentedControl` (Task 4.9)

```
Vai trò
┌─────────────────────────────┐   rãnh gray-200, bo 6px, cao 40px (cảm ứng 44px), đệm 4px
│┌─────────────┐              │
││ ✎ Chỉnh sửa │  👁 Chỉ xem   │   phần đang chọn: nền trắng, shadow-sm, chữ ink 500
│└─────────────┘              │   phần còn lại: chữ gray-600, rê chuột thành ink
└─────────────────────────────┘
```

- Thay ô chọn thả xuống khi chỉ có **2–4 lựa chọn** và đủ chỗ cho tất cả trên một hàng: một lần bấm là chọn, không có gì mở ra. Đang dùng: "Vai trò" ở form mời (icon `Pencil` / `Eye`). Dòng thành viên trong hộp chia sẻ quá chật nên giữ ô chọn cỡ nhỏ.
- Chữ 14px, có thể kèm icon 16px. Các phần rộng bằng nhau và lấp đầy khung chứa; đặt trong một cột lưới `auto` thì rộng vừa chữ.
- Đổi màu khi chọn mờ dần 150ms; tắt khi hệ điều hành bật giảm chuyển động.
- Như `ActivityTypeField` (mục 7.13): các phần là **ô radio thật** ẩn sau nhãn, nằm trong `fieldset` có `legend`. Tab vào nhóm một lần, phím mũi tên đổi lựa chọn, vòng focus `jade` quanh phần có con trỏ; dùng thẳng `register()` của react-hook-form. Lỗi hiện dưới rãnh như ô thường.

### 7.16. Menu thả xuống — `menuStyles` (Task 4.9)

```
[ Đã lên kế hoạch ⌄ ]          huy hiệu 28px có mũi tên (xoay 180° khi mở)
┌──────────────────────────┐   khung trắng, viền tide, bo 6px, shadow-md, cách nút 4px
│ ● Nháp                   │   chấm 8px màu đậm của tông huy hiệu
│ ● Đã lên kế hoạch      ✓ │   ✓ jade ở dòng hiện tại
│ ● Đang diễn ra           │   dòng đang trỏ: nền gray-100
│ ● Đã hoàn thành          │
│ ● Đã lưu trữ             │
└──────────────────────────┘
```

- Mọi menu bật ra từ một nút (Radix `DropdownMenu`) dùng chung `components/menuStyles.ts`: `MENU_CONTENT` (khung) và `MENU_ITEM` (dòng 36px, **44px trên màn cảm ứng**, chữ 14px, icon 16px `gray-500`). Đang dùng: menu "⋮" của thẻ hoạt động (mục 7.3), trạng thái chuyến đi, sắp xếp danh sách chuyến đi (mục 8.2).
- **Mở / đóng:** khung mờ dần và lớn lên từ phía nút (gốc biến đổi do Radix tính, nên menu mở lên trên hay xuống dưới đều hợp) trong 140ms, đóng 100ms (`animate-pop-in` / `animate-pop-out` trong `tokens.css`); tắt khi hệ điều hành bật giảm chuyển động.
- Dòng đang trỏ (chuột hoặc phím mũi tên) nền `gray-100`: màu trung tính hợp cả dòng "Xoá" chữ `danger`. Danh sách của ô chọn (mục 7.2) dùng `jade-light`.
- **Trạng thái chuyến đi** (`TripStatusSelect`, trang chi tiết, chỉ người sửa được): nút là huy hiệu cùng tông với trạng thái (mục 3.5), chữ 12px/500, mũi tên `ChevronDown` 14px; menu liệt kê 5 trạng thái theo thứ tự, mỗi dòng chấm màu (`badgeDotClass`) + tên + ✓ ở trạng thái hiện tại. Chọn là **lưu ngay** (toast "Đã đổi trạng thái"); chọn lại trạng thái hiện tại không gửi gì. Đang lưu: huy hiệu hiện trạng thái vừa chọn, mũi tên thành vòng quay, nút bị khoá; lỗi: toast đỏ, huy hiệu về trạng thái cũ. Nhãn đọc "Trạng thái chuyến đi: {trạng thái}".
- Bàn phím (Radix): Enter / Space / ↓ mở menu, ↑ / ↓ di chuyển, Enter chọn, Esc đóng và trả focus về nút.

### 7.17. Ô giờ — `TimeField` (Task 4.9)

```
Giờ bắt đầu                     Giờ kết thúc
┌──────────────────┐            ┌──────────────────────────────┐
│ 🕘 09:30          │            │ 🕘 11:00     (1 giờ 30 phút)  │   thời lượng 13px gray-500 trong ô
└──────────────────┘            └──────────────────────────────┘
┌──────────────────┐            danh sách nổi dưới ô (như ô tìm địa điểm, mục 7.12),
│ 09:00            │            cao tối đa 240px rồi cuộn, mỗi 15 phút một dòng;
│▒09:30▒▒▒▒▒▒▒▒▒▒▒▒│            dòng khớp với chữ đang gõ / giá trị: nền jade-light
│ 09:45            │            ô giờ kết thúc: chỉ các giờ sau giờ bắt đầu, mỗi dòng
└──────────────────┘            kèm thời lượng ("10:30   1 giờ 30 phút")
```

- Thay `<input type="time">` của trình duyệt (cách hiện 24h hay SA / CH tuỳ máy). **Luôn 24h**, chữ số đều nhau, icon `Clock` bên trái, gợi ý trống "--:--". Bàn phím số trên điện thoại (`inputMode="numeric"`).
- **Gõ tay:** "9" → 09:00, "930" / "0930" → 09:30, "9:30", "9.30", "9h30" → 09:30, "21h" → 21:00 (`lib/timeInput.parseTimeInput`), đổi khi rời ô hoặc nhấn Enter. Phút sau dấu phân cách phải đủ 2 chữ số ("9:5" không rõ là 09:05 hay 09:50). Gõ thứ không phải giờ ("25", "abc"): ô giữ chữ đã gõ, dưới ô báo "Giờ không hợp lệ, hãy nhập dạng 09:30 (hoặc gõ 930)".
- **Danh sách:** bấm vào ô hoặc ↓ thì mở, mỗi 15 phút một dòng, danh sách tự cuộn tới dòng khớp với chữ đang gõ (hoặc giá trị đang có; ô trống: 08:00). ↑ / ↓ di chuyển, Enter chọn dòng đang sáng (không gửi form), Esc **chỉ đóng danh sách**, rời ô thì đóng. Bấm một dòng thì con trỏ vẫn ở trong ô. Danh sách hiện ra mờ dần như menu (mục 7.16).
- **Ô giờ kết thúc** (`durationFrom` = giờ bắt đầu): danh sách chỉ gồm các giờ sau giờ bắt đầu, mỗi dòng ghi thời lượng; khi ô có giờ hợp lệ sau giờ bắt đầu, thời lượng "(1 giờ 30 phút)" hiện ở mép phải trong ô (`lib/duration`). Giờ bắt đầu trống: danh sách đủ 24 giờ, không có thời lượng.
- Giá trị gửi lên máy chủ vẫn là "HH:mm" (hoặc bỏ trống) như trước; quy tắc "giờ kết thúc phải sau giờ bắt đầu" và "không xoá được giờ đã lưu" không đổi. Vai trò ARIA: `combobox` + `listbox`, như ô tìm địa điểm.

### 7.18. Lịch và ô khoảng ngày — `Calendar`, `DateRangeField` (Task 4.9)

```
Ngày đi – ngày về *
┌──────────────────────────────────────┐
│ 📅 12/03/2026 – 16/03/2026          ⌄ │   một ô cho cả hai ngày
└──────────────────────────────────────┘
5 ngày 4 đêm. Tối đa 60 ngày.               dòng gợi ý (lib/calendarDate.stayLength)
┌─────────────────────────────────────────────────────────┐
│ Từ 12/03/2026: chọn ngày về                              │   thẻ trắng bo 12px, viền tide,
│ ‹        Tháng 3 2026             Tháng 4 2026         › │   shadow-lg, cách ô 4px
│ T2 T3 T4 T5 T6 T7 CN         T2 T3 T4 T5 T6 T7 CN        │
│          (12)▒13▒▒14▒▒15▒(16)                            │   đầu / cuối: tròn jade chữ trắng
│                                                          │   giữa: dải jade-light
└─────────────────────────────────────────────────────────┘   hôm nay: viền tròn jade
```

- **`Calendar`** (`components/Calendar.tsx`; mẫu là Calendar của shadcn/ui trên `react-day-picker` 9, viết lại bằng token, không nạp CSS của thư viện): tiếng Việt, tuần bắt đầu thứ Hai, tiêu đề "Tháng 3 2026", tên thứ "T2 … CN". Ô ngày tròn 40px (44px trên cảm ứng); rê chuột nền `gray-100`. Không hiện ngày của tháng bên cạnh. Hai nút ‹ › 32px ở hai góc trên. Mỗi ngày chỉ một bộ lớp theo trạng thái (đã chọn / trong khoảng / hôm nay / không chọn được / thường), không lớp nào đè lớp nào. Thư viện lịch được **tải lười** lần đầu mở ô ngày (khung xương trong lúc chờ), không nằm trong gói chính.
- **`DateRangeField`**: ô trông như ô chọn (mục 7.2) với icon `CalendarDays`, ghi "12/03/2026 – 16/03/2026" (chuyến 1 ngày: một ngày), trống thì chữ mờ "Chọn ngày đi và ngày về". Bấm mở lịch nổi dưới ô (Radix Popover; trong hộp thoại thì vẽ vào hộp, mục 7.6): **2 tháng từ 640px, 1 tháng dưới đó**; tháng mở ra là tháng của ngày đi (chưa có: tháng hiện tại), con trỏ đặt sẵn ở ngày đã chọn hoặc hôm nay.
  - **Chọn:** lần bấm đầu là ngày đi, lần bấm thứ hai là ngày về, rồi lịch đóng và form nhận cả hai. Bấm một ngày trước ngày đi thì bắt đầu lại từ ngày đó; bấm cùng một ngày hai lần là chuyến 1 ngày. Dòng trên lịch nhắc "Chọn ngày đi" / "Từ 12/03/2026: chọn ngày về". Trong lúc chọn ngày về, các ngày quá giới hạn độ dài (60 ngày) bị mờ, không bấm được.
  - **Ngày đã qua vẫn chọn được** (chuyến đi có thể ghi lại sau khi đi), chỉ hôm nay có viền.
  - Esc hoặc bấm ra ngoài: lịch đóng, ngày **giữ như cũ** (chưa chọn đủ hai ngày thì không có gì thay đổi); Esc không đóng hộp thoại chứa ô.
  - Form vẫn giữ `startDate` và `endDate` riêng (`useController` cho từng ngày): lỗi của ngày nào cũng hiện dưới ô chung, và đưa con trỏ về ô đó. Lỗi chỉ được kiểm khi lịch đóng hoặc rời ô mà không mở lịch, không kiểm lúc con trỏ đi vào lịch.
  - Bàn phím (react-day-picker): ← → ↑ ↓ đi giữa các ngày, PageUp / PageDown đổi tháng, Enter / Space chọn.
- **`DatePickerField`**: cùng ô và cùng lịch nổi cho **một** ngày (1 tháng): ô ghi "25/10/2026", trống thì chữ mờ "Chọn ngày"; bấm một ngày là chọn và lịch đóng; `minDate` làm mờ các ngày trước nó; `defaultOpen` mở lịch ngay khi ô hiện ra. Đang dùng: hạn của liên kết chia sẻ ("Chọn ngày…", mục 8.1).

### 7.19. Chip lựa chọn — `ChoiceChips` (Task 4.9)

```
Hết hạn
(Không hết hạn) ( 7 ngày ) ( 30 ngày ) ( Chọn ngày… )     viên bo tròn cao 32px (cảm ứng 44px), chữ 14px
Ai có liên kết đều xem được cho tới khi bạn thu hồi        chip đang chọn: viền jade, nền jade-light, chữ jade-dark 500
```

- Một lựa chọn trong vài lựa chọn ngắn, **xuống dòng** được trên màn hẹp (khác nút gạt 7.15 phải nằm trên một hàng). Chip chưa chọn: viền `tide`, nền trắng, chữ `gray-700`, rê chuột viền `gray-300`. Đang dùng: hạn của liên kết chia sẻ.
- Như nút gạt: **ô radio thật** ẩn sau nhãn trong `fieldset` có `legend`; Tab vào nhóm một lần, phím mũi tên đổi lựa chọn, dùng thẳng `register()`. `onChipClick` chỉ chạy khi bấm / chạm vào chip (không chạy khi đổi bằng phím mũi tên): "Chọn ngày…" dùng nó để mở lịch. Dòng gợi ý hoặc lỗi dưới nhóm như ô thường.

---

## 8. Bố cục từng màn hình

### 8.0. Khung chung (`AppLayout`) — **Đã làm**

```
┌──────────────────────────────────────────────────────────────────────┐
│ [📍 Smart Trip Planner]   [🔍 Tìm chuyến đi…        ]   Trieu  ⎋ Đăng xuất │ 64px, nền trắng, vạch `tide` ở đáy
├──────────────────────────────────────────────────────────────────────┤
│                         nội dung, tối đa 1280px                       │
├──────────────────────────────────────────────────────────────────────┤
│ © 2026 Smart Trip Planner      Nguồn dữ liệu: Bản đồ và địa điểm …  │ nền trang, vạch `tide` ở trên
└──────────────────────────────────────────────────────────────────────┘
```

- **Thanh điều hướng** (Task 3.9, theo mockup "Teal Voyage"): nền **trắng**, cao 64px, vạch `tide` 1px ở đáy (thanh trắng trên nền trang gần trắng cần một ranh giới); logo chữ `ink`; tên người dùng `gray-700`; "Đăng xuất" là nút `ghost`. Chân trang không còn nền tối: chữ `gray-500` trên nền trang, vạch `tide` ở trên. Trước Task 3.9 cả hai có nền `ink`.
- **Ô tìm chuyến đi** (`TripSearchBox`) nằm giữa thanh điều hướng: dạng **viên bo tròn hết cỡ** (ngoại lệ của quy tắc bo 6px cho ô nhập, theo mockup), nền `gray-100`, không viền, chữ `ink`, icon và chữ gợi ý `gray-500`; rê chuột có viền `tide`; khi focus nền trắng, viền và vòng `jade`. Chỉ tìm chuyến đi theo tên hoặc điểm đến (chưa tìm được theo hoạt động). Ở `/trips`: lọc khi ngừng gõ 300 ms, không thêm bước vào lịch sử trình duyệt, giữ chip trạng thái và cách sắp xếp. Ở trang khác: Enter mở `/trips?q=…`.
- Dưới 768px: ô tìm xuống thành hàng riêng dưới logo, rộng hết chiều ngang.
- **Chân trang ghi nguồn dữ liệu** (Task 3.8): bên trái "© 2026 Smart Trip Planner", bên phải dòng "Nguồn dữ liệu: Bản đồ và địa điểm **© OpenStreetMap**, Quãng đường **OSRM**, Thời tiết **Open-Meteo**" (các nguồn cách nhau 12px, không có dấu ngăn). Ba tên là link mở tab mới tới trang của từng nguồn (`openstreetmap.org/copyright`, `project-osrm.org`, `open-meteo.com`): chữ `gray-700` 12px/500 có gạch chân (link giữa dòng chữ nhỏ phải nhận ra được mà không cần màu), rê chuột chuyển `jade-dark`, vòng focus như mọi link. Phần chữ còn lại `gray-500`. Dưới 640px hai phần xếp thành hai hàng, dòng nguồn tự xuống dòng giữa các nguồn, không cắt giữa tên. Dòng này hiện ở **mọi trang đã đăng nhập** và không phụ thuộc máy chủ đang dùng nguồn nào: điều khoản của ba dịch vụ yêu cầu ghi nguồn (design.md 7.2), và dữ liệu địa điểm có sẵn cũng lấy từ OpenStreetMap. Bản đồ vẫn có dòng ghi nguồn riêng ở góc (mục 9).
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
- **Huy hiệu vai trò** (Task 4.5, **Đã làm**): `Badge` tông nhạt ngay sau tên chuyến, ngang dòng đầu của tên (tên dài thì huy hiệu nằm cuối dòng đầu, tên vẫn tối đa 2 dòng): "Chủ sở hữu" `brand`, "Chỉnh sửa" `info`, "Chỉ xem" `neutral`, theo `myRole` của `GET /trips/{id}`. Thành viên luôn thấy; chủ chỉ thấy khi chuyến có người khác trong danh sách thành viên (lời mời đang chờ cũng tính), nên chuyến riêng không có huy hiệu "Chủ sở hữu".
- **Người chỉ xem** (`myRole = VIEWER`, Task 4.5, **Đã làm**): ô đổi trạng thái thay bằng `Badge` trạng thái cùng màu (không bấm được), không có nút "Sửa" / "Xoá", không bị hỏi "Hoàn thành chuyến đi?" (họ không đổi được trạng thái). Trong lịch trình: không có "Sửa" ngày và "+ Thêm hoạt động" ở khối tiêu đề ngày, không có tay kéo / nút ↑ ↓ / menu "⋮" trên thẻ (thẻ vẫn là đích khi bấm marker trên bản đồ), ngày trống là khung nét đứt chỉ ghi "Ngày này chưa có hoạt động." không có nút. Thẻ bản đồ của ngày chưa có địa điểm chỉ ghi "Ngày này chưa có địa điểm nào."; thẻ thời tiết của chuyến chưa đặt vị trí ghi "Chuyến đi chưa đặt vị trí điểm đến nên chưa có dự báo thời tiết." (không bảo họ vào "Sửa"). Cột ngày, chip ngày, nút ngày trước / sau, bản đồ, thời tiết, quãng đường giữ nguyên. Người chỉnh sửa thấy "Sửa" và ô trạng thái, không thấy "Xoá" (chỉ chủ). Quyết định dựa vào `myRole` máy chủ trả, không so `ownerId` ở giao diện.
- **Nút "Chia sẻ" / "Thành viên"** (Task 4.5, **Đã làm**): nút phụ ở cụm góc phải đầu trang, sau ô trạng thái và trước "Sửa" (người chỉ xem: sau huy hiệu trạng thái). Chủ chuyến thấy "Chia sẻ" (icon `Share2`), mọi người khác thấy "Thành viên" (icon `Users`). Mở hộp thoại 640px "Chia sẻ chuyến đi" / "Thành viên chuyến đi" (mục 15.3) với danh sách "Thành viên (N)" lấy từ `members` của `GET /trips/{id}` (không gọi API riêng): mỗi dòng ảnh hoặc chữ cái đầu (mục 7.14), tên đậm và email xám bên dưới (chưa có tài khoản: chỉ email), bên phải huy hiệu "Đang chờ" (`warning`, khi lời mời chưa được nhận) và huy hiệu vai trò. Cụm nút xuống dòng trên điện thoại rất hẹp thay vì tràn mép.
  - **Mời thành viên** (chỉ chủ chuyến, **Đã làm**): trên danh sách, mục "Mời thành viên" gồm ô "Email" (bắt buộc, gợi ý "ten@vidu.com"), nút gạt "Vai trò" ("Chỉnh sửa" mặc định / "Chỉ xem", mục 7.15, từ Task 4.9) và nút chính "Mời", một hàng từ 640px (nút canh ngang với ô, dưới nhãn), xếp dọc trên điện thoại. Mở hộp thì con trỏ nằm ở ô email. Lỗi của ô email chỉ hiện khi bấm "Mời" (không hiện khi rời ô: chủ chuyến có thể mở hộp để làm việc khác; BUG-UI-012, Task 4.9), sau đó cập nhật theo từng phím gõ. Gửi xong: toast "Đã gửi lời mời tới {email}", ô email được làm trống (vai trò giữ nguyên), người mới hiện trong danh sách với "Đang chờ". Email của chính chủ chuyến (400) và người đã là thành viên (409 `MEMBER_ALREADY_EXISTS`) báo lỗi ngay dưới ô email bằng câu của máy chủ; lỗi khác (mất mạng, chuyến vừa bị xoá) là khung đỏ trên các ô. Trong lúc đang gửi, Esc và "×" không đóng hộp. Chuyến chỉ có mình chủ: dưới form có câu "Chỉ mình bạn thấy chuyến đi này. Mời bạn bè để cùng chỉnh sửa.", không có nút (mục 7.9).
  - **Lời mời đang chờ** (**Đã làm**): dưới email có dòng 13px `gray-500` "Đã mời 12/10" (ngày gửi mail gần nhất, theo múi giờ tài khoản; mail có hạn 7 ngày mà máy chủ không trả ngày hết hạn, nên chủ chuyến nhìn ngày này để biết khi nào gửi lại). Ai cũng thấy dòng này. Chủ chuyến có thêm nút `ghost` cỡ `sm` "Gửi lại" sau các huy hiệu: gửi lại đúng `POST /members` với cùng email và vai trò (máy chủ phát token mới, link trong mail cũ hết dùng được), toast "Đã gửi lại lời mời tới {email}", ngày mời cập nhật. Lỗi (ví dụ người đó vừa nhận lời: 409) báo bằng toast đỏ và danh sách được tải lại. Trong lúc đang gửi, hộp không đóng bằng Esc / "×".
  - **Đổi vai trò** (chỉ chủ chuyến, **Đã làm**): trên mọi dòng trừ dòng của chủ, huy hiệu vai trò được thay bằng ô chọn nhỏ (`Select size="sm"`, mục 7.2, từ Task 4.9) cao 32px (44px trên màn cảm ứng), viền `tide`, chữ 13px, rộng tối thiểu 120px để không nhảy khi đổi giữa "Chỉnh sửa" / "Chỉ xem"; nhãn đọc "Vai trò của {tên hoặc email}". Chọn là lưu ngay, không có nút Lưu; trong lúc lưu ô bị khoá và giữ giá trị vừa chọn; xong: toast "Đã đổi vai trò". Lời mời đang chờ cũng đổi được (người đó nhận lời với vai trò mới). Lỗi (người đó vừa bị gỡ…): toast đỏ "Không đổi được vai trò: …", danh sách tải lại, ô về giá trị đang lưu. Dòng của chủ luôn là huy hiệu "Chủ sở hữu" (không hạ được vai trò của chủ, design rule 14.7). Người được đổi vai trò thấy thay đổi khi tải lại trang (chưa có cập nhật tức thời: Phase 5).
  - **Gỡ thành viên / huỷ lời mời** (chỉ chủ chuyến, **Đã làm**): nút phụ cỡ `sm` cuối mỗi dòng trừ dòng của chủ: "Gỡ" với người đã nhận lời, "Huỷ lời mời" với lời mời đang chờ (nhãn đọc kèm tên / email). Bấm thì mở `ConfirmDialog` kiểu `danger` chồng lên hộp chia sẻ: "Gỡ thành viên?" — "{tên} sẽ không còn xem được chuyến đi này. Bạn có thể mời lại sau." (nút "Huỷ" / "Gỡ thành viên"); hoặc "Huỷ lời mời?" — "Link trong mail mời gửi tới {email} sẽ không dùng được nữa. Bạn có thể mời lại sau." (nút "Giữ lời mời" / "Huỷ lời mời"). Xong: toast "Đã gỡ {tên}" / "Đã huỷ lời mời gửi tới {email}", dòng biến mất. Lỗi hiện trong hộp xác nhận. Esc chỉ đóng hộp xác nhận, hộp chia sẻ vẫn mở (`BUG-UI-011`).
  - **Liên kết chia sẻ** (chỉ chủ chuyến, **Đã làm**): dưới danh sách thành viên, sau một vạch `tide`. Tiêu đề "Liên kết chia sẻ" và dòng "Ai có liên kết đều xem được chuyến đi, không cần tài khoản.". Mỗi liên kết chưa thu hồi là một khung viền `tide` (mới nhất trước): ô địa chỉ chỉ đọc `{origin}/share/{token}` (chữ đơn cách 13px, nền `gray-50`, bấm vào là chọn hết) và nút phụ cỡ `sm` "Sao chép" (icon `Copy`) → toast "Đã sao chép liên kết"; trình duyệt không cho chép (không phải HTTPS / localhost) → toast đỏ "Không sao chép được. Hãy chọn liên kết và sao chép thủ công.". Dòng dưới 13px: "12 lượt xem · Không hết hạn" / "· Dùng được đến hết ngày 20/10/2026"; liên kết đã hết hạn có huy hiệu `muted` "Hết hạn", "· Hết hạn ngày 10/10/2026", chữ địa chỉ nhạt và nút "Sao chép" bị khoá. Ngày tính theo múi giờ tài khoản. Danh sách chỉ tải khi chủ mở hộp; đang tải là khung xương, lỗi là khung đỏ kèm "Thử lại", chưa có liên kết nào thì ghi "Chưa có liên kết nào.". Thành viên khác không thấy mục này.
  - **Tạo liên kết** (**Đã làm**; chọn nhanh từ Task 4.9): ngay dưới dòng giải thích, trên danh sách: nhóm chip "Hết hạn" (`ChoiceChips`, mục 7.19) **"Không hết hạn"** (mặc định) / **"7 ngày"** / **"30 ngày"** / **"Chọn ngày…"**; dưới chip một dòng 12px `gray-500` nói liên kết dùng được tới khi nào: "Ai có liên kết đều xem được cho tới khi bạn thu hồi", hoặc "Dùng được đến hết ngày 25/10/2026" (7 / 30 ngày tính từ hôm nay: tạo thứ Hai thì "7 ngày" dùng được hết thứ Hai tuần sau; `lib/linkExpiry.expiryDateFor`). "Chọn ngày…" hiện ô "Dùng được đến hết ngày *" rộng 220px (`DatePickerField`, mục 7.18; ngày trước hôm nay bị mờ); bấm chip bằng chuột / ngón tay thì lịch mở luôn, đi bằng phím mũi tên qua chip thì không. Rồi nút **phụ** "Tạo liên kết" (icon `Link2`; nút chính của hộp vẫn là "Mời") ở hàng riêng. Ngày cuối nghĩa là liên kết dùng được **hết ngày đó** theo múi giờ tài khoản (`lib/linkExpiry.expiryInstant`: mili giây cuối của ngày, tính cả ngày đổi giờ mùa hè). Xong: toast "Đã tạo liên kết", về lại "Không hết hạn", liên kết mới ở đầu danh sách. "Chọn ngày…" mà chưa chọn ngày: "Chọn ngày hết hạn trên lịch" dưới ô; ngày trước hôm nay: "Ngày hết hạn phải từ hôm nay trở đi"; máy chủ từ chối (400 ở `expiresAt`) hiện dưới ô ngày, hoặc dưới nhóm chip khi đang chọn nhanh; lỗi khác là khung đỏ trên form. Đang tạo thì Esc / "×" không đóng hộp.
  - **Thu hồi liên kết** (**Đã làm**): cuối dòng lượt xem của mỗi liên kết, căn phải, nút `danger` cỡ `sm` "Thu hồi" (không thêm kiểu nút "ghost-danger" của mockup), có cả trên liên kết đã hết hạn (liên kết hết hạn vẫn giữ nhãn "chia sẻ bằng liên kết" của chuyến cho tới khi thu hồi). Bấm thì mở `ConfirmDialog` kiểu `danger` chồng lên hộp chia sẻ: "Thu hồi liên kết?" — "Ai đang có liên kết này sẽ không mở được chuyến đi nữa. Thao tác này không hoàn tác được; bạn có thể tạo liên kết mới sau." (nút "Huỷ" / "Thu hồi liên kết"). Xong: toast "Đã thu hồi liên kết", liên kết biến khỏi danh sách. Lỗi (đã thu hồi ở tab khác: 404) hiện trong hộp hỏi.
- **Bình luận** (Task 4.6, **Đã làm**: Mốc 1–10; quyết định ở WORKFLOW Task 4.6, chủ dự án duyệt 2026-10-19; mockup Stitch duyệt cùng ngày, mục 15.3):
  - **Nút "Bình luận"** (**Đã làm**): nút phụ ở cụm góc phải đầu trang, ngay **trước** "Chia sẻ" / "Thành viên"; icon `MessageCircle`, kèm số bình luận của cả chuyến (tính cả trả lời) dạng "Bình luận · 5" khi > 0. Mọi vai trò đều thấy. Danh sách bình luận tải cùng lúc mở trang (số trên nút và trên thẻ cần nó), lỗi tải thì nút không có số.
  - **Panel** (`Modal` kiểu panel, mục 7.6; **Đã làm**): bám mép phải, rộng 360px, cao hết màn hình; dưới 1024px chiếm toàn màn. Tiêu đề "Bình luận" và "×" ở đầu; dưới tiêu đề, khi đang lọc theo một hoạt động (mở từ nút trên thẻ, Mốc 7), chip "{chấm màu tuyến} {tên hoạt động} ×" (viền `tide`, cao 28px, chữ 13px, tên dài cắt "…"; trình đọc màn hình đọc "Đang xem bình luận về hoạt động: …"). Chip nằm ở **đầu thân panel, dính lại khi cuộn** (nền trắng, vạch `tide` bên dưới) thay vì nằm trong hàng tiêu đề như mockup: hàng tiêu đề là phần chung của `Modal`. Bấm × (nhãn "Xem mọi bình luận của chuyến đi") thì xem bình luận của cả chuyến, chữ đang gõ giữ nguyên. Hoạt động không còn trong lịch trình (vừa bị xoá) vẫn có chip, ghi "Hoạt động không còn trong lịch trình", không có chấm. Trạng thái mở và hoạt động đang lọc nằm trong `stores/commentPanelStore`; rời trang chuyến đi hoặc sang chuyến khác thì panel đóng. Thân cuộn; ô nhập đứng yên ở chân panel. Mở panel thì danh sách tải lại và cuộn sẵn xuống cuối; tải lại ngầm không kéo người đang đọc xuống cuối. Panel mở thì không bấm được "Chia sẻ" (không mở cùng hộp chia sẻ). Đóng bằng Esc, "×" hoặc **bấm ra vùng tối** (Mốc 12, yêu cầu sau khi chạy thử; trước đó bấm ra ngoài không đóng).
  - **Một bình luận** (**Đã làm**; chip hoạt động từ Mốc 6: `ActivityChip`, tên hoạt động lấy theo mã trong `trip.days` qua `lib/commentTree.activitiesById`, trình đọc màn hình đọc "Về hoạt động: {tên}", rê chuột thấy đủ tên). Thời gian (`lib/relativeTime`, Mốc 3): tính giờ trước rồi mới tới lịch, nên đăng 23:00 đọc lúc 00:30 là "1 giờ trước" chứ không phải "Hôm qua"; "hôm qua", "N ngày trước" và năm tính theo lịch của múi giờ tài khoản; thời điểm hơi vượt đồng hồ máy người dùng vẫn là "Vừa xong". Chữ thời gian đọc "bây giờ" từ `hooks/useNow` (một bộ hẹn giờ chung cho cả trang, chỉ vẽ lại khi sang phút mới; chỉ phần chữ thời gian vẽ lại, không cả danh sách); rê chuột thấy `formatDateTimeIn` "12/10/2026 14:30": `Avatar` 32px bên trái; bên phải tên 14px/600 `ink` và thời gian 12px `gray-500` cùng hàng ("Vừa xong", "12 phút trước", "3 giờ trước", "Hôm qua lúc 14:30", "4 ngày trước", "12/10 lúc 14:30", khác năm "12/10/2025"; rê chuột thấy "12/10/2026 14:30"; theo múi giờ tài khoản, tự đổi mỗi phút); dưới là nội dung 14px `gray-800`, chữ thuần, giữ xuống dòng, chuỗi dài tự bẻ, không tự tạo link. Bình luận về một hoạt động có chip nhỏ cao 20px (chấm 8px màu tuyến + tên hoạt động, cắt "…", nền `gray-100`) giữa tên và nội dung; trả lời không lặp chip, và khi panel đang lọc theo một hoạt động thì không bình luận nào có chip (chip đã ở đầu panel; theo mockup, Mốc 7). Các bình luận cách nhau 16px, không khung, không vạch; rê chuột lên một bình luận thì nền của nó `gray-50` (bo 6px, tràn ra 8px mỗi bên nhờ lề âm nên chữ không xê dịch) để thấy nút xoá thuộc bình luận nào (theo mockup, Mốc 9); rê lên một trả lời thì chỉ trả lời đó đổi nền, bình luận gốc không.
  - **Trả lời** (**Đã làm**; nút "Trả lời" từ Mốc 5, nhãn đọc "Trả lời bình luận của {tên}", cao 44px trên màn cảm ứng, cách nội dung 4px): nằm dưới nội dung của bình luận gốc, cách 12px, **thẳng cột với nội dung** của gốc (thụt 44px = ảnh 32px + khoảng 12px, như mockup), `Avatar` 24px cách tên 8px; các trả lời cách nhau 12px, cũ trước. Danh sách trả lời đọc là "Trả lời bình luận của {tên}". Trả lời mà bình luận gốc không còn trong danh sách (vừa bị xoá) thì không hiện, không bị đẩy lên thành bình luận gốc (`lib/commentTree.commentThreads`). Dưới nội dung bình luận gốc có link-nút 12px/600 `gray-500` "Trả lời" (trả lời không có nút này: API chỉ cho một cấp).
  - **Ô nhập** (`CommentForm`, `TextAreaField` nhãn ẩn, mục 7.2; **Đã làm**). Đang lọc theo hoạt động (Mốc 7): bình luận mới gửi kèm `activityId` của hoạt động đó; trả lời thì không (máy chủ lấy hoạt động của bình luận gốc); hoạt động vừa bị xoá thì máy chủ trả 404, câu của máy chủ hiện trong khung đỏ trên ô. Chip "Trả lời {tên} ×" (Mốc 5): viên bo tròn cao 24px (32px trên màn cảm ứng) nền `jade-light`, chữ 12px `jade-dark`, tên đậm, tên dài cắt "…"; nút × đọc "Bỏ trả lời {tên}" và trả con trỏ về ô. Bấm "Trả lời" dưới bình luận khác thì chip đổi sang người đó; gửi xong hoặc đóng panel thì chip mất. Trả lời vừa gửi nằm dưới bình luận gốc của nó (giữa danh sách) nên danh sách cuộn **vừa đủ để thấy nó** (`scrollIntoView` `nearest`), không cuộn xuống cuối. Bình luận gốc bị xoá trong lúc đang trả lời: máy chủ trả 400 ở `parentId`, câu của máy chủ hiện trong khung đỏ trên ô, chữ đang gõ giữ nguyên. Bình luận vừa gửi vào danh sách ngay từ câu trả lời của máy chủ, rồi danh sách tải lại ngầm (thêm bình luận người khác vừa viết); sau khi gửi con trỏ ở lại ô trống để viết tiếp; Ctrl+Enter lúc đang gửi không gửi lần hai. **Bản nháp** (Mốc 12): đóng panel (bấm ra vùng tối, Esc, "×") thì chữ đang gõ dở được giữ trong `stores/commentPanelStore`, mở lại panel trên cùng chuyến thì chữ còn nguyên (chip "Trả lời …" thì không giữ); gửi xong hoặc rời trang chuyến đi thì bản nháp bị bỏ, sang chuyến khác không thấy nháp của chuyến trước. "Viết bình luận…", **cao cố định 3 dòng**, chữ dài cuộn bên trong ô (từ Mốc 11, theo yêu cầu sau khi chạy thử; trước đó tự giãn 3 → 8 dòng và đẩy danh sách lên), bộ đếm "1.800 / 2000" từ 80%; dưới ô là nút **chính** "Gửi bình luận" (icon `Send`) căn phải, nút chính duy nhất của panel; dưới 640px nút rộng hết hàng (`fullWidth` theo `useMediaQuery`, không ghi đè lớp; theo mockup). Ctrl+Enter (⌘+Enter) gửi, Enter xuống dòng. Bấm "Trả lời": trên ô hiện chip "Trả lời {tên} ×", con trỏ nhảy vào ô; × bỏ trả lời (nội dung đang gõ giữ nguyên). Đang lọc theo hoạt động thì bình luận gửi đi gắn vào hoạt động đó. Gửi xong: ô trống, chip trả lời biến mất, danh sách cuộn tới bình luận mới, không toast. Nội dung chỉ có khoảng trắng: "Nội dung bình luận không được để trống" khi bấm gửi. Lỗi máy chủ: khung đỏ trên ô, nội dung giữ nguyên. Đang gửi: Esc / "×" không đóng panel.
  - **Xoá** (**Đã làm**, Mốc 9; ai thấy nút: `features/comments/commentRules.canDeleteComment`, người chỉnh sửa và người chỉ xem chỉ xoá được bình luận của mình): tác giả và chủ chuyến thấy nút `ghost` nhỏ icon `Trash2` (28px, 44px trên màn cảm ứng; rê lên thì icon đỏ) ở cuối hàng tên (nhãn đọc "Xoá bình luận của {tên}"), hiện khi rê chuột / Tab tới bình luận, luôn hiện trên màn cảm ứng. Xoá xong, bình luận (và trả lời của nó) rời danh sách ngay rồi danh sách tải lại; đang trả lời đúng bình luận vừa xoá thì chip "Trả lời …" mất. `ConfirmDialog` kiểu `danger` "Xoá bình luận?" — "Bình luận sẽ bị xoá và không khôi phục được." (bình luận gốc có trả lời thêm "Các trả lời của bình luận này cũng bị xoá.") — nút "Huỷ" / "Xoá bình luận". Xong: toast "Đã xoá bình luận". Lỗi hiện trong hộp hỏi, danh sách tải lại.
  - **Bấm chip hoạt động** (**Đã làm**, Mốc 10; chip thành nút: rê chuột thì nền `gray-200`, nhãn đọc "Xem {tên} trong lịch trình", cao 32px trên màn cảm ứng, chưa tới 44px vì là viên nhỏ trong dòng chữ) trên một bình luận: panel đóng, trang chuyển sang ngày của hoạt động (không thêm bước vào lịch sử trình duyệt khi đã đứng đúng ngày) và thẻ được tô viền, cuộn vào giữa màn hình, nhận con trỏ, như "Xem trong lịch trình" của bản đồ; trên màn hẹp đang ở tab "Bản đồ" thì về tab "Lịch trình" trước. Cách làm: `mapLinkStore.revealWhenShown` ghi hoạt động cần hiện, `DayTimeline` hiện nó khi ngày của nó lên màn hình **và panel đã trượt ra xong** (từ Mốc 13: chờ không còn `dialog[open]`) rồi xoá; rời trang chuyến đi thì yêu cầu chưa thực hiện bị bỏ. Đang gửi bình luận thì chip không làm gì (panel không đóng được). Hoạt động không còn trong lịch trình đang hiển thị: không có chip.
  - **Trạng thái** (**Đã làm**): đang tải là 3 khung xương (tròn 32px + hai dòng); lỗi tải là khung đỏ kèm "Thử lại" (còn dữ liệu cũ thì giữ danh sách, khung đỏ ghi thêm "Đang hiển thị bình luận đã tải trước đó.", BUG-UI-005); rỗng: câu ở mục 7.9 (Mốc 1–3, khi chưa có ô nhập, chỉ ghi "Chưa có bình luận nào.").
- **Máy chủ không trả lời:** nếu chuyến đi đã tải được, trang **giữ nguyên** (kể cả hộp thoại đang mở) và hiện khung lỗi "… Đang hiển thị dữ liệu đã tải trước đó." kèm nút phụ "Thử lại" ngay trên tên chuyến đi, cùng cách trang danh sách đang làm. Chưa tải được gì thì chỉ có khung lỗi, "Thử lại" và link về danh sách. Chuyến đi không còn (404) hoặc không có quyền (403): chỉ thông báo, không có "Thử lại" (Task 2.7, BUG-UI-005).
- **Một ngày một trang:** URL `/trips/:id/days/:dayIndex` (số thứ tự ngày, 1..n). `/trips/:id` tự chuyển về ngày 1, hoặc về ngày hôm nay khi chuyến đi đang diễn ra (Task 3.7); số ngày không tồn tại (ví dụ sau khi rút ngắn chuyến đi) tự chuyển về ngày 1. F5, nút Back và link gửi cho người khác giữ đúng ngày. (Trước đây xếp dọc mọi ngày trên một trang; đổi ở Task 2.6 commit 10 để khớp với bản đồ từng ngày ở Phase 3.)
- **Cột trái** (Task 3.9, theo mockup "Teal Voyage"; rộng 200px, từ 1280px: 240px. Bản đầu của Task 3.9 rộng 220 / 280px với chữ lớn hơn; chủ dự án thấy to và thô nên thu gọn ở Commit 11): một **thẻ trắng không viền, `shadow-md`**, dính khi cuộn, cao tối đa bằng màn hình trừ 48px (danh sách ngày cuộn bên trong thẻ, phần đầu thẻ đứng yên). Đầu thẻ: "Kế hoạch các ngày" (16px/600) và dòng phụ in hoa 11px `gray-500` "13 ngày · 02/10 – 14/10". Mỗi ngày là một link cao khoảng 48px: "Ngày N" (14px/600), bên dưới "Thứ sáu, 02/10" (12px `gray-500`), bên phải là số hoạt động trong vòng tròn 24px. Ngày đang xem (`aria-current="page"`): nền `jade-light`, vạch `jade` 3px ở mép trái, tên `jade-dark`, vòng tròn nền `jade-dark` chữ trắng. Hôm nay: `Badge` "Hôm nay" cạnh tên, vòng tròn nền `jade-light`. Ngày đã qua **không** có nhãn và không nhạt đi ở cột này; tiêu đề của ngày cũng không còn ở đây (cả hai còn ở khối tiêu đề ngày). Trước Task 3.9: danh sách phẳng rộng 200px, có tiêu đề của ngày và nhãn "Đã qua".
- **Cột giữa:** chỉ ngày đang xem: khối tiêu đề của ngày, rồi đến ray. Khối tiêu đề là một **thẻ trắng không viền, `shadow-md`** gồm hai phần (Task 3.9, theo mockup "Teal Voyage"). **Phần trên** (đệm 16px, từ 1280px: 20px ngang): bên trái "Ngày N" (24px/700) và `Badge` "Hôm nay" / "Đã qua" ngay cạnh, dòng dưới "Thứ hai, 05/10/2026" (14px `gray-500`); bên phải nút "Sửa" kiểu `secondary` cỡ nhỏ có icon bút chì, và nút chính "+ Thêm hoạt động". Tên ngày cố ý ngắn: cột giữa có thể chỉ rộng 368px và các nút không co (BUG-UI-010); nhãn không bị ngắt đôi, thiếu chỗ thì cả nhãn xuống dòng. Nút chính ghi "Thêm" trên điện thoại và ở bố cục ba cột dưới 1280px; trên điện thoại rất hẹp (320px) hai nút tự xuống hàng dưới, vẫn sát mép phải. **Phần dưới**, sau một vạch `tide`: icon `Flag` và tiêu đề của ngày (`jade-dark`, 500), bên phải "N địa điểm" (12px `gray-500`, ẩn khi bằng 0) và nút "↑ Đầu ngày" (chỉ khi đã cuộn, màn rộng); rồi dòng thời tiết (màn hẹp) và ghi chú có "Đọc thêm". Form sửa tiêu đề và ghi chú của ngày hiện trong một thẻ cùng kiểu. Bản Task 3.7: hai hàng chữ trên nền trang, tên ngày viết đủ "Ngày 2 · Thứ ba, 13/10/2026". Dưới ray là nút "‹ Ngày trước" / "Ngày sau ›"; bấm "Ngày sau" ở cuối một ngày dài thì trang cuộn lên tiêu đề của ngày mới.
- **Ngày dài** (Task 2.6 commit 13): từ 1024px, **khối mô tả ngày** (tên ngày, tiêu đề, ghi chú, "Sửa", "+ Thêm hoạt động") dính ở trên khi cuộn, **ngang hàng với cột ngày bên trái** (cả hai cách mép trên 24px); phần bọc quanh thẻ có nền `paper` (24px phía trên, 12px phía dưới) che các thẻ cuộn qua bên dưới; từ Task 3.9 không còn viền `tide` ở đáy khối vì khối đã là thẻ có bóng. Cả trang vẫn chỉ có **một thanh cuộn**: không dùng khung cao cố định có thanh cuộn riêng (hai thanh cuộn lồng nhau khó dùng, cản kéo thả, và cột bản đồ Phase 3 sẽ dính theo trang). Màn hình hẹp: khối mô tả **không** dính, vì sẽ chiếm gần nửa màn hình.
- **Nút quay lên** (`BackToTopButton`), chỉ hiện khi đã cuộn quá một chiều cao màn hình:
  - Từ 1024px: nút ghost **"↑ Đầu ngày"** trong khối mô tả ngày đang dính, cạnh "Sửa" (về chỗ hoạt động đầu tiên nằm ngay dưới khối, như lúc mới mở ngày), và một nút tròn `ink` **nhỏ 40px** ở **góc dưới phải** màn hình, cách mép dưới **96px**, **"Lên đầu trang"** (về tên chuyến đi). Độ cao này để nút nằm trên một thông báo (toast) đang hiện ở cùng góc và không chạm footer khi cuộn tới cuối trang; cột trái chỉ có danh sách ngày.
  - Dưới 1024px chỉ một nút tròn `ink` 48px, **"Về đầu ngày"** (đầu ngày nằm ngay dưới dải chip). Cách mép dưới 56px để không chạm footer. Điện thoại: góc phải (toast ở trên). Máy tính bảng: góc **trái**, vì toast chiếm góc dưới phải.
  - Bấm thì focus chuyển tới chỗ vừa cuộn về (phần ngày, hoặc tên chuyến đi).
  - Đích cuộn "đầu ngày" là **cả phần ngày** (`#day-start`), không phải tiêu đề: tiêu đề nằm trong khối dính nên trình duyệt coi như luôn hiện, cuộn tới nó không có tác dụng. Chuyển sang ngày khác từ cuối một ngày dài cũng cuộn về `#day-start`.

**Cột phải, bản đồ** (Task 3.6, **Đã làm**): từ 1024px có cột thứ ba rộng 340px (từ 1280px: 380px; trước Task 3.9: 360px và 420px). Cột giữa nhờ vậy rộng khoảng 530px ở màn từ 1280px, bằng bản trước Task 3.9, dính khi cuộn ngang hàng với cột ngày (cách mép trên 24px). Từ Task 3.9 Commit 11 thẻ bản đồ cao **420px** như trong mockup (trước đó cao hết phần còn lại của màn hình); trên màn hình thấp nó tự thấp đi, ít nhất 240px, để thẻ thời tiết bên dưới vẫn vừa. Khoảng cách giữa các cột 24px (từ 1280px: 32px). Bản đồ vẽ theo bản đang hiển thị của ngày, nên số trên marker đổi theo ngay khi kéo thả. Dưới 1024px bản đồ nằm ở tab "Bản đồ" (bảng "Màn hình hẹp" bên dưới). Dải thời tiết bên dưới bản đồ: Task 3.7.

**Task 3.7 (chốt 2026-10-05; `design.md` rule 14.20, 14.22) — chưa làm:**
- **Thẻ thời tiết** (**Đã làm**; làm lại ở Task 3.9) nằm dưới thẻ bản đồ trong cột phải, cách 16px; thẻ bản đồ cao 420px, hoặc phần còn lại của màn hình sau khi trừ thẻ thời tiết và khoảng cách đó nếu phần này nhỏ hơn (ít nhất 240px). Chi tiết ở mục 9.
- **Đoạn di chuyển** giữa các thẻ hoạt động (`features/itinerary/TravelLeg`, **Đã làm**): nằm trong hàng của thẻ **xuất phát**, dưới thẻ 16px. Từ Task 3.9 (mockup "Teal Voyage") là một **viên bo tròn hết cỡ**, cao 28px, nền `gray-100`, **canh giữa bề ngang của thẻ**: icon `Route` 14px `gray-500`, thời gian 12px/600 `ink`, dấu "·", quãng đường 12px `gray-600` ("12 phút · 3,2 km"). Icon là hình đường đi, không phải phương tiện. Đoạn ray bên trái viên (cả khoảng hở phía trên và phía dưới) vẽ **nét đứt** 2px `gray-300` thay cho nét liền. Bản Task 3.7: một dòng chữ 12px `gray-500` sát mép trái của thẻ. Không viền, không nền, **không icon phương tiện** (API chỉ tính một phương tiện). Thẻ kế tiếp không phải đích (ở giữa có hoạt động không có địa điểm) thì ghi thêm tên đích: "12 phút · 3,2 km tới Cầu Rồng" (tên là tên của hoạt động đích; tên dài thì cắt bằng "…", phần số luôn hiện đủ). Rê chuột hiện "Ước tính theo đường bộ, chưa tính kẹt xe". Dưới 1 km ghi mét ("998 m"), từ 1 km ghi một chữ số thập phân ("8,4 km"); thời gian làm tròn lên phút, từ 60 phút ghi "1 giờ 5 phút". Chặng 0 m (hai hoạt động cùng một địa điểm) không hiện. Đang kéo thẻ thì mọi đoạn di chuyển ẩn nhưng **vẫn giữ chỗ** (các hàng không đổi chiều cao, thẻ không nhảy dưới con trỏ); sau khi lưu, số mới hiện lại. Không hiện tổng di chuyển của ngày.
- **Ngày đã qua và hôm nay** (**Đã làm**). "Hôm nay" tính theo múi giờ của tài khoản, hiện là giờ Việt Nam; component lấy qua `hooks/useToday`, trang để mở qua nửa đêm tự chuyển ngày trong vòng một phút:
  - Cột trái: nhãn "Hôm nay" cạnh tên ngày; từ Task 3.9 ngày đã qua không còn được đánh dấu ở cột này (mô tả ở mục "Cột trái" phía trên).
  - Chip ngày (dưới 1024px): chip hôm nay có chấm `jade` 6px trước chữ; chip đã qua có nền `gray-100`, chữ giữ `gray-600` (không làm mờ cả chip và không hạ màu chữ: vẫn là link, phải đủ tương phản). Chip đang chọn giữ nền `ink`.
  - Tiêu đề ngày: `Badge` "Hôm nay" (tông `brand`) hoặc "Đã qua" (tông `muted`) đi liền sau ngày tháng, **không ngắt dòng giữa nhãn**; thiếu chỗ thì cả nhãn xuống dòng dưới.
  - Thẻ hoạt động của ngày đã qua **không** nhạt đi: vẫn sửa được và phải dễ đọc.
  - Mở `/trips/:id` (không kèm số ngày: thẻ ở trang danh sách, sau khi tạo chuyến đi) khi hôm nay nằm trong chuyến đi thì vào thẳng ngày hôm nay thay vì Ngày 1, bất kể trạng thái của chuyến đi. Link có số ngày giữ nguyên; số ngày không tồn tại vẫn về Ngày 1.
- **Hỏi hoàn thành** (`features/trips/CompleteTripPrompt`, **Đã làm**): mở một chuyến đi đã qua ngày cuối mà trạng thái còn là Nháp, Đã lên kế hoạch hoặc Đang diễn ra → `ConfirmDialog`: tiêu đề "Hoàn thành chuyến đi?", nội dung 'Chuyến đi "{tên}" đã kết thúc ngày {dd/mm/yyyy}. Lịch trình vẫn sửa được sau khi hoàn thành.', nút phụ "Để sau", nút chính "Hoàn thành chuyến đi". Xong: toast "Đã hoàn thành chuyến đi", ô trạng thái đổi theo. "Để sau" (cũng như Esc và "×"): không hỏi lại về chuyến đi đó tới lần đăng nhập sau; tải lại trang không tính là đăng nhập lại. Danh sách mã chuyến đi đã "Để sau" nằm trong `localStorage` (`stores/completePromptStore`), bị xoá khi phiên kết thúc.
- (**Đã làm**) Trên màn hẹp, dưới tiêu đề của ngày và trên ghi chú có **một dòng thời tiết của ngày đang xem** (`features/weather/DayWeatherLine`; 13px, `gray-600`): icon có màu, tên tình trạng ("Có mây"), "32° / 25°", icon giọt nước và "20%", ngăn nhau bằng dấu "·" màu `gray-300`; thiếu chỗ thì tự xuống hàng. Ngày không có dự báo, đang tải hoặc tải lỗi thì không có dòng này (không chừa chỗ, không báo lỗi). Từ 1024px không hiện (đã có dải dưới bản đồ).

**Màn hình hẹp:**

| Bề rộng | Bố cục |
|---|---|
| < 1024px | Cột trái thành **dải chip ngày** (link) cuộn ngang, dính ở mép trên khi cuộn. Từ Task 3.9 mỗi chip là một viên bo tròn hết cỡ, nền trắng, `shadow-sm`, không viền; chip của ngày đang xem tô `jade-dark` chữ trắng (trước đó: `ink`) và tự cuộn vào tầm nhìn; hôm nay có chấm 6px (`jade`, hoặc trắng khi đang chọn); ngày đã qua nền `gray-100`. **Đã làm** |
| < 1024px | (Thẻ bản đồ ở tab "Bản đồ" cao 60% màn hình từ Task 3.9 Commit 11, trước đó 70%.) Ngay dưới dải chip ngày có **hai nút gạt** "Lịch trình" / "Bản đồ" (`ViewSwitch`): khung nền `gray-200` bo 12px (nút bên trong bo 8px; trước Task 3.9: 6px), mỗi nút cao 44px có icon và chữ; nút đang chọn nền trắng, chữ `jade-dark` đậm, `shadow-sm` (chốt 2026-10-03 theo mockup, thay cho kiểu chữ gạch chân). Mỗi lúc chỉ hiện một trong hai: danh sách của ngày, hoặc bản đồ rộng hết màn hình, cao 70% màn hình (ít nhất 320px). Phần danh sách chỉ bị ẩn, không bị huỷ, nên việc đang sửa dở trong ngày không mất khi sang xem bản đồ; bản đồ chỉ được tạo khi tab của nó đang mở. Ở tab "Bản đồ", chạm marker mở ô tên; "Xem trong lịch trình" chuyển về tab "Lịch trình" rồi cuộn tới thẻ. Đổi ngày giữ nguyên tab đang xem. **Đã làm** (Task 3.6). Tab Chi phí: Phase 7 |

**Sắp xếp hoạt động:**
- Chuột / bút: kéo thả bằng tay nắm; bàn phím: Tab tới tay nắm, Space nhấc, mũi tên di chuyển, Space thả.
- **Màn hình cảm ứng:** không kéo thả (xung đột với cuộn trang); mỗi thẻ có **nút ↑ / ↓** 44px, chỉ đổi thứ tự **trong** ngày (hoạt động đầu ngày không lên được, cuối ngày không xuống được). Việc chọn tay nắm hay mũi tên dựa vào **loại con trỏ** (`pointer-coarse`), không dựa vào độ rộng màn hình.
- **Chuyển sang ngày khác:** trên máy tính, kéo thẻ **thả lên tên ngày ở cột trái** (mục đó có viền `jade` khi rê qua; chỉ tính khi con trỏ nằm đúng trên mục, nên kéo trong ngày không bị hút sang). Mọi thiết bị: menu "⋮" → **"Chuyển sang ngày…"** (hộp chọn ngày; ô "Ngày" là `SelectField`, mục 7.2, từ Task 4.9). Hoạt động xuống **cuối** ngày đích và biến khỏi ngày đang xem, nên thông báo có link "Mở Ngày N". Vẫn hỏi lại khi trùng giờ ở ngày mới.
- **Tự xếp theo giờ** (Task 2.6 commit 12, backend làm): thêm hoạt động có giờ bắt đầu, hoặc đổi giờ bắt đầu, thì hoạt động vào ngay trước hoạt động đầu tiên bắt đầu muộn hơn; không có thì xuống cuối ngày. Hoạt động không giờ và thứ tự đã kéo giữ nguyên; sửa tên, ghi chú, chi phí, giờ kết thúc không làm di chuyển.
- Kéo thả và nút mũi tên dùng chung một đường lưu: cập nhật giao diện ngay, hỏi lại khi trùng giờ ở ngày mới, trả về chỗ cũ khi lỗi.

### 8.2. Danh sách chuyến đi — **Đã làm**

- Tiêu đề "Chuyến đi của bạn" + dòng phụ; nút "+ Tạo chuyến đi" bên phải.
- **Hàng lọc** giữa hai đường kẻ mảnh:
  - Chip trạng thái "Tất cả" + 5 trạng thái, mỗi chip có **số chuyến đi** (từ `GET /trips/status-counts`, tính theo từ khoá đang tìm). Chip đang chọn nền `ink` chữ trắng; "Đang diễn ra" có chấm `jade` đứng yên.
  - Nút "Sắp xếp" (từ Task 4.9): cao 40px (cảm ứng 44px), nền trắng, viền `tide` (rê chuột `gray-300`, menu đang mở `jade`), icon `ArrowUpDown`, chữ "Sắp xếp: " `gray-600` rồi thứ tự hiện tại đậm 500, mũi tên `ChevronDown`. Bấm mở menu (mục 7.16, canh phải với nút): Tạo gần đây nhất / Ngày đi sớm nhất / Ngày đi muộn nhất / Tên A → Z, ✓ ở thứ tự hiện tại; chọn là sắp xếp lại và về trang 1 (thứ tự lưu trên URL như trước).
  - Mọi bộ lọc lưu trên URL (`?q=&status=&sort=&page=`).
- **Lưới** 3 cột (≥ 1024px) / 2 cột (≥ 640px) / 1 cột.
- **Thẻ:** ảnh bìa 16:9 (chưa có ảnh thì là khối `gray-100` có icon ghim, không gradient), huy hiệu trạng thái `solid` ở góc trên phải ảnh, tên điểm đến ở góc dưới trái ảnh (nền `ink` 85%), tên chuyến 1 dòng, ngày đi có icon lịch, chân thẻ "5 ngày · 12 hoạt động". Rê chuột: viền và tên chuyển `jade`.
- **Chuyến được chia sẻ** (Task 4.5, **Đã làm**): chuyến mà người xem là thành viên, không phải chủ (`ownerId` khác người đang đăng nhập), có huy hiệu `solid` tông `info` "Được chia sẻ" ở góc trên **trái** ảnh (góc phải là trạng thái), và dưới dòng ngày đi một dòng 14px `gray-600` có icon `UserRound`: "Của {tên chủ}" (tên dài cắt bằng "…", rê chuột xem đủ). Bản kế hoạch ghi một huy hiệu "Được chia sẻ · của {tên chủ}"; tách hai vì tên chủ dài không vừa một huy hiệu cạnh huy hiệu trạng thái trên thẻ hẹp. Chuyến của chính mình không có gì thêm. Không có ảnh thành viên trên thẻ.
- **Thời tiết trên thẻ** (Task 3.7, **Đã làm**; `features/weather/TripCardWeather`; design rule 14.20): ở **hàng chân thẻ, căn phải**, 13px `gray-600`; thiếu chỗ thì xuống dòng thứ hai của chân thẻ, vẫn căn phải: chữ "Hôm nay" (chuyến đang đi) hoặc "Ngày đi" (chuyến sắp đi, ngày đầu trong 16 ngày tới), icon thời tiết, "32° / 25°". Không có xác suất mưa. Chuyến đi đã qua, còn xa hơn, chưa đặt vị trí điểm đến, hoặc dự báo không tải được: không hiện gì (không có chỗ trống, không có chữ báo lỗi). Thẻ hiện ngay, thời tiết hiện sau khi tải xong. Không đặt ở hàng ngày đi: không đủ chỗ khi thẻ hẹp (mockup Task 3.7).
- **Không làm** (có trong mockup nhưng không có dữ liệu thật): ảnh thành viên (**Phase 4**), thanh "Phân bổ lịch trình" nhiều màu và chú thích màu (cần số hoạt động theo từng loại cho mỗi chuyến, có thể thêm sau), khối "Thống kê hành trình tổng quan".

### 8.3. Trang đăng nhập, đăng ký, quên / đặt lại mật khẩu, xác thực email — **Đã làm**

- Logo cỡ lớn kèm dòng phụ; khung 400px, viền mảnh, không bóng; tiêu đề 28px + dòng phụ.
- "Quên mật khẩu?" nằm cùng hàng với nhãn "Mật khẩu", căn phải; ô mật khẩu có nút con mắt; quy tắc mật khẩu là dòng gợi ý dưới ô.
- Nút gửi chính cao 44px, rộng hết khung.
- Dòng chuyển trang ("Chưa có tài khoản? Đăng ký") nằm **dưới** khung.
- Không có: dải trạng thái hệ thống, mã phiên bản, bảng chú thích màu (chỉ có trong mockup).
- **Đến từ link mời** (Task 4.5, **Đã làm**; chưa đăng nhập mà mở `/invite`): đường quay lại `/invite` đi theo cả khi bấm qua lại "Đăng ký" ↔ "Đăng nhập". Trên form có khung `info`: trang đăng nhập "Đăng nhập bằng đúng email đã nhận lời mời để tham gia chuyến đi. Chưa có tài khoản thì đăng ký bằng email đó."; trang đăng ký "Đăng ký bằng đúng email đã nhận lời mời. Xác thực email xong, mở lại link trong mail mời để tham gia chuyến đi."; trang "Kiểm tra hộp thư" sau khi đăng ký thêm khung "Xác thực xong, mở lại link trong mail mời để tham gia chuyến đi." (link xác thực mở ở tab khác nên không tự quay lại được; quyết định 1 của Task 4.5). Nếu người dùng quay lại đúng tab đó và bấm "Đăng nhập" ở dưới khung thì đăng nhập xong vẫn về `/invite`.

### 8.4. Tạo chuyến đi (`/trips/new`) — **Đã làm**

- Tiêu đề 32px + dòng phụ "Ba bước: thông tin chung, điểm đến và ngày đi."
- Thanh bước là một tuyến ngắn: 3 "ga" đánh số nối bằng ray. Ga đã qua: nền `jade` có dấu ✓; ga hiện tại: viền `jade`, chữ đậm; ray đã đi qua chuyển `jade`.
- Bước "Ngày đi" (từ Task 4.9), dùng chung với hộp "Sửa chuyến đi": **một** ô "Ngày đi – ngày về *" có lịch (`DateRangeField`, mục 7.18) thay cho hai ô ngày của trình duyệt; dòng gợi ý dưới ô "5 ngày 4 đêm. Tối đa 60 ngày."; rồi "Ngân sách" và "Tiền tệ" (mục 7.2) trên một hàng. Trong hộp "Sửa chuyến đi", đổi ngày vẫn hiện khung giải thích dời / rút ngắn và hộp hỏi lại như trước.
- Bước "Điểm đến" (Task 3.6, **Đã làm**), dùng chung với hộp "Sửa chuyến đi":
  - **"Tên điểm đến":** ô chữ tự gõ như trước ("Ví dụ: Đà Lạt").
  - **"Vị trí trên bản đồ":** ô tìm địa điểm (mục 7.12) với gợi ý "Tìm theo tên hoặc bấm lên bản đồ để chọn vị trí. Vị trí này dùng cho bản đồ và dự báo thời tiết." Chọn một gợi ý chỉ lấy **toạ độ** của nó; không địa điểm nào được lưu. Ô tìm luôn còn đó để đổi vị trí.
  - **Bản đồ nhỏ cao 320px** ngay dưới ô tìm (`PointPicker`, mục 9): mở ở vị trí đang có (mức phố), chưa có thì cả Việt Nam. **Bấm lên bản đồ** đặt hoặc dời vị trí; chọn một gợi ý ở ô tìm thì marker nhảy tới đó và bản đồ chuyển theo. Hai cách dùng lẫn nhau được: tìm tới gần đúng rồi bấm để chỉnh.
  - Đã có vị trí: dưới bản đồ là một dòng 13px gồm icon ghim `jade`, một nhãn, và toạ độ 4 chữ số thập phân bằng số thẳng cột (`16.0612, 108.2279`). Nhãn là tên địa điểm vừa chọn ở ô tìm, "Vị trí chọn trên bản đồ" sau một lần bấm lên bản đồ, hoặc "Vị trí đã lưu" khi mở hộp sửa mà chưa đổi gì.
  - Chọn vị trí khi ô tên **còn trống** thì tên địa điểm được điền vào, vẫn sửa được.
  - Nút chữ "Bỏ vị trí" chỉ có khi vị trí **chưa được lưu** (wizard, hoặc chuyến đi chưa có vị trí). Vị trí đã lưu chỉ đổi được, chưa bỏ được (máy chủ chưa hỗ trợ xoá trắng).

### 8.5. Trang chuyến đi công khai (`/share/:token`) — **Đã làm** (Task 4.5)

Một cột, rộng tối đa 760px, canh giữa, trên nền `paper`. Không có thanh điều hướng của ứng dụng, chỉ có logo nhỏ ở góc trên trái (không phải link). Ai cũng mở được, đã đăng nhập hay chưa, và trang giống nhau với mọi người. Bản đồ đặt dưới phần tóm tắt, không dính.

- **Tóm tắt:** tên chuyến 32px/700 (không cắt, xuống dòng), dòng thông tin có icon (điểm đến, ngày đi, số ngày), mô tả thu gọn có "Đọc thêm". Không có ngân sách, trạng thái, tên chủ, thành viên.
- **Bản đồ chuyến đi** (Task 4.5 quyết định 2): một thẻ trắng `shadow-md` như thẻ bản đồ của ngày (mục 9): đầu thẻ icon `Map` và "Bản đồ chuyến đi", bên phải chip `jade-light` "N địa điểm"; bản đồ cao 320px đóng khung vừa **mọi địa điểm của cả chuyến**. Marker giọt nước màu loại hoạt động như mục 9 nhưng **không có số** và **không có đường nối** (các địa điểm thuộc nhiều ngày khác nhau). Rê chuột lên marker hiện "Ngày 2 · tên hoạt động"; bấm (hoặc chạm) mở ô tên cùng chữ đó, không có nút "Xem trong lịch trình". Con lăn chuột cuộn trang, không phóng bản đồ (trang dài); phóng bằng nút + / −. Không có nút "Mở rộng". Chuyến không có hoạt động nào gắn địa điểm: không có thẻ bản đồ. Dùng lại `DayMapCanvas` với `overview`.
- **Các ngày:** **mọi ngày trên một trang**, theo thứ tự (không có cột ngày, chip ngày, hay chuyển trang). Mỗi ngày: "Ngày 1 · Thứ năm, 12/03" (18px/600), dưới là tiêu đề của ngày với icon `Flag` màu `jade-dark` (theo trang trong ứng dụng; mockup đặt bên phải, không theo vì tên dài bị chen trên điện thoại). Rồi ray thời gian như mục 7.4 với thẻ chỉ đọc: cùng phần thân thẻ của lịch trình (`ActivityCardContent`), chip loại cạnh giờ (mockup đặt ở góc phải, không theo), không có tay kéo, menu "⋮", nút thêm, chi phí, link đặt chỗ. Không có ghi chú của ngày (máy chủ không gửi). Ngày trống: khung nét đứt "Ngày này chưa có hoạt động.".
- **Cuối trang**, sau một vạch `tide`, canh giữa: nút **phụ** "Tạo chuyến đi của bạn" (tới `/trips/new`: chưa đăng nhập thì qua trang đăng nhập), dòng 13px "Được chia sẻ qua Smart Trip Planner", dòng 12px "Bản đồ và địa điểm © OpenStreetMap" (link tới trang bản quyền, mở tab mới; điều khoản OSM yêu cầu ghi nguồn). Trang không có nút chính. Không có biểu tượng tài khoản và chân trang "Public Itinerary" của mockup.
- **Không tự tải lại:** mỗi lần tải thành công là một lượt xem của liên kết (design.md 10.2), nên trang hỏi máy chủ một lần, không tải lại khi quay lại tab hay khi có mạng lại.
- **Liên kết không còn hiệu lực** (sai, đã thu hồi, hết hạn, chuyến đã xoá: máy chủ trả cùng một 404; địa chỉ không đúng dạng 32 ký tự `[a-z0-9]` thì báo ngay, không hỏi máy chủ): thẻ trắng 420px `shadow-md` canh giữa, icon `Link2Off`, "Liên kết không còn hiệu lực", "Liên kết đã bị thu hồi, đã hết hạn, hoặc chuyến đi không còn.", nút phụ rộng hết thẻ "Tạo chuyến đi của bạn" (theo mockup "public-trip-error").
- Đang tải: khung xương (tên, dòng thông tin, ba khối). Không gọi được máy chủ: thẻ "Chưa tải được chuyến đi" với câu lỗi và nút "Thử lại".

### 8.6. Landing — **Phase 8**

Hero **không** dùng khối chữ lớn với gradient. Hiển thị một lịch trình thật: thanh ray với ba hoạt động và bản đồ nhỏ bên cạnh; các chấm màu tuyến sáng dần theo thứ tự trong 2 giây khi trang tải (khoảnh khắc chuyển động duy nhất của cả trang). Tiêu đề bên trái, ngắn, một câu. Làm cuối cùng, để dùng ảnh chụp sản phẩm thật.

### 8.7. Trang nâng cấp — **Phase 6**

Hai cột so sánh Free và Premium, không có thẻ "phổ biến nhất" phóng to. Dòng giới hạn mà người dùng đang chạm tới được tô nền `warning` 8% (ví dụ đã có 3 chuyến đi thì dòng "Số chuyến đi" được tô).

### 8.8. Trang nhận lời mời (`/invite?trip=&token=`) — **Đã làm** (Task 4.5)

Mở từ link trong mail mời. Nằm sau đăng nhập (chưa đăng nhập thì sang trang đăng nhập rồi quay lại), nhưng **không** có thanh điều hướng: logo nhỏ phía trên một thẻ trắng 420px, `shadow-md`, không viền, canh giữa trên nền `paper`, nội dung canh giữa. Không có form: mở ra là nhận lời ngay.

| Trạng thái | Nội dung thẻ |
|---|---|
| Đang nhận | Vòng quay 32px `jade` và "Đang nhận lời mời…" (`role="status"`), thường chỉ thoáng qua |
| Thành công | Không có thẻ: chuyển thẳng vào `/trips/{id}` kèm toast "Đã nhận lời mời tham gia chuyến đi". Đã là thành viên mà mở lại mail (máy chủ trả 400 vì token đã dùng, nhưng chuyến đi mở được): cũng vào thẳng, toast "Bạn đã là thành viên của chuyến đi này" |
| Không còn hiệu lực | Icon `Link2Off`, "Lời mời không còn hiệu lực", "Lời mời đã được dùng, đã hết hạn hoặc đã bị huỷ. Nhờ chủ chuyến đi gửi lại.", nút phụ rộng hết thẻ "Về danh sách chuyến đi". Dùng cho 400, 404 (chuyến đã xoá) và link thiếu / hỏng (kiểm ở trình duyệt, không hỏi máy chủ) |
| Sai tài khoản (403) | Icon `UserX`, "Lời mời này dành cho email khác", "Bạn đang đăng nhập bằng {email}.", nút phụ "Đăng xuất để đổi tài khoản": đăng xuất xong là trang đăng nhập, đăng nhập đúng tài khoản thì quay lại đây và nhận lời |
| Không gọi được máy chủ | Icon `CloudOff`, "Chưa nhận được lời mời", câu lỗi, nút phụ "Thử lại" (token chưa bị dùng nên gửi lại an toàn) |

Thanh chọn "Trạng thái 1 / 2 / 3" trong mockup Stitch là công cụ xem trước, không làm.

---

## 9. Bản đồ và thời tiết — **Phase 3**

- **Thẻ bản đồ** (Task 3.9, theo mockup "Teal Voyage"): bản đồ của ngày nằm trong một thẻ trắng không viền, `shadow-md`. Đầu thẻ (đệm 16px ngang, 12px dọc): icon `Map` `gray-500` và "Bản đồ lộ trình" (18px/600); bên phải là chip cao 24px nền `jade-light` chữ `jade-dark` "N điểm dừng" (số hoạt động có địa điểm của ngày; ẩn khi bằng 0). Bản đồ lấp kín phần còn lại của thẻ, không viền riêng, hai góc dưới bo theo thẻ. **Không có** dòng tổng cự ly trong ngày (có trong mockup, chủ dự án cho bỏ). Nhãn tên luôn hiện cạnh marker như mockup: không làm.
- **Bản đồ** (Task 3.6, **Đã làm**): `DayMap` + `DayMapCanvas` (`features/itinerary`), thư viện Leaflet tải lười. Nền là tile chuẩn của **OpenStreetMap** (không cần API key), **giữ màu gốc** (biển xanh, công viên xanh lá). Bản đầu làm nhạt nền bằng CSS (giảm màu 85%) cho marker nổi; chủ dự án thấy cả trang bị xám nên bỏ bộ lọc ngày 2026-10-04. Marker vẫn tách khỏi nền nhờ viền trắng 2px và bóng. Góc dưới phải ghi nguồn "Leaflet | © OpenStreetMap contributors" (bắt buộc theo điều khoản) và hai nút phóng to / thu nhỏ. CartoDB Positron (kiểu nền của mockup) cần API key từ 2026: là tuỳ chọn ở Task 3.8 (design.md 3.2). Chế độ tối: chưa làm.
- Bản đồ luôn đóng khung vừa mọi địa điểm của ngày (một địa điểm: mức phố). Khung chỉ tính lại khi **tập địa điểm** đổi; kéo thả đổi thứ tự không làm bản đồ nhảy, và mức phóng người dùng tự chỉnh được giữ.
- **Ngày chưa có địa điểm nào** (Task 3.6, **Đã làm**): bản đồ mở ở **điểm đến của chuyến đi** (mức thành phố), không có marker. Giữa bản đồ có một thẻ trắng nhỏ (rộng tối đa 280px, bo 10px, viền `tide`, `shadow-md`): icon bản đồ xám, dòng đậm "Ngày này chưa có địa điểm nào.", dòng nhỏ "Thêm địa điểm cho hoạt động để thấy trên bản đồ." Chuyến đi **chưa đặt vị trí điểm đến**: bản đồ hiện cả Việt Nam, và thẻ có thêm câu 'Đặt vị trí điểm đến trong "Sửa" chuyến đi để bản đồ mở đúng nơi bạn đến.' Quanh thẻ, bản đồ vẫn kéo và phóng được. Thẻ biến mất ngay khi ngày có địa điểm đầu tiên.
- **Phóng to bản đồ** (Task 3.6, **Đã làm**): góc trên **trái** bản đồ (từ Task 3.9; trước đó góc trên phải, chỉ có icon) có nút trắng cao 36px (màn cảm ứng: 44px), viền `tide`, icon `Maximize2` và chữ "Mở rộng" (13px/500), nhãn đọc "Phóng to bản đồ". Bấm thì bản đồ của ngày mở **phủ cả cửa sổ** trong một hộp thoại gốc của trình duyệt: cùng marker, đường nối và thẻ "chưa có địa điểm"; nút ở góc trên phải đổi thành "×" ("Thu nhỏ bản đồ"). Đóng bằng nút đó hoặc phím **Esc**; con trỏ trở về nút "Phóng to bản đồ". Trong lúc mở, phần còn lại của trang không bấm được. Bản phóng to là một bản đồ thứ hai, nên mức phóng và vị trí đang xem của bản nhỏ không mang theo (cả hai đều tự đóng khung vừa các địa điểm của ngày).
- **Bản đồ chọn một điểm** (`components/map/PointPicker`, Task 3.6): bản đồ nhỏ bo 6px, viền `tide`, cùng nền, dòng ghi nguồn và nút + / − như bản đồ của ngày. Bấm (hoặc chạm) lên bản đồ đặt một marker giọt nước `jade` có icon ghim trắng; bấm chỗ khác thì marker dời tới đó. Toạ độ làm tròn 7 chữ số thập phân. Khi điểm vừa đặt không nhìn rõ được, bản đồ tự chuyển tới nó ở mức thành phố: điểm nằm ngoài khung nhìn (một kết quả tìm kiếm ở nơi khác), hoặc bản đồ còn đang ở mức cả nước (một lần bấm ở mức đó quá thô, nên bản đồ phóng vào để bấm lại cho chính xác). Bản đồ đã đủ gần thì giữ nguyên khung nhìn khi bấm. Chọn điểm cần chuột hoặc ngón tay; form nào dùng bản đồ này cũng có ô tìm địa điểm cho người dùng bàn phím. Nguồn nền và các mức phóng chuẩn của mọi bản đồ nằm ở `lib/mapTiles.ts`.
- Khung bản đồ là một lớp riêng (`isolate`), không còn viền và bo góc riêng từ Task 3.9 (thẻ bọc ngoài lo việc đó): các lớp của thư viện bản đồ (z-index tới 1000) không đè lên khối tiêu đề ngày đang dính hay lớp phủ của hộp thoại (bẫy BUG-UI-002).
- **Marker:** hình giọt nước 28px (**36px trên màn hình cảm ứng**, icon và số lớn theo), nền màu tuyến, viền trắng 2px, icon trắng bên trong (cùng icon lucide ở mục 3.4), số thứ tự ở góc trên phải trong vòng tròn trắng 16px. Số đếm **riêng các hoạt động có địa điểm** (1, 2, 3...), theo thứ tự trong ngày; hoạt động không có địa điểm không có marker và không chiếm số. Marker do ứng dụng tự vẽ bằng HTML, không dùng ảnh marker của Leaflet. Rê chuột lên marker hiện "số. tên hoạt động". **Liên kết thẻ và marker** (Task 3.6, **Đã làm**): rê chuột lên một thẻ hoạt động, hoặc Tab vào trong thẻ, thì marker của hoạt động đó phóng to 1,15 lần (lớn lên từ mũi nhọn, mũi vẫn chỉ đúng chỗ), có vòng sáng `jade` mờ 35% dày 4px và nổi lên trên các marker khác; rời thẻ thì trở lại sau 150ms. Thẻ của hoạt động không có địa điểm không gây gì. Trạng thái này giữ ở `stores/mapLinkStore.ts`, chỉ marker liên quan vẽ lại.
- **Bấm marker** (Task 3.6, **Đã làm**), hai kiểu:
  - *Đi thẳng tới thẻ*: bản đồ nhỏ cạnh danh sách, khi dùng chuột. Trang cuộn mượt đưa thẻ của hoạt động vào **giữa màn hình** (không nhắm mép trên, vì ở đó có khối tiêu đề ngày đang dính), thẻ có viền sáng `jade` mờ 40% trong 2 giây và nhận con trỏ; marker của nó vì thế vẫn nổi bật. Người tắt hiệu ứng chuyển động trong hệ điều hành thì trang nhảy thẳng tới thẻ.
  - *Qua ô tên*: trên **màn cảm ứng** (ngón tay không rê được để đọc chú thích) và trên **bản đồ đang phóng to** (thẻ bị che). Bấm marker mở một ô nhỏ phía trên marker: "số. tên hoạt động" (14px/600) và nút chữ `jade` **"Xem trong lịch trình"**. Bấm nút thì bản đồ phóng to tự thu lại (nếu đang mở) rồi làm như kiểu đi thẳng. Ô đóng bằng "×" của nó hoặc bấm ra ngoài.
  - Bàn phím: Tab tới marker, Enter có tác dụng như bấm chuột.
- **Bản đồ toàn chuyến** (Task 4.5, trang công khai, mục 8.5): cùng nền, marker và ghi nguồn, nhưng không số, không đường nối, bấm marker chỉ mở ô tên, con lăn chuột không phóng bản đồ (`DayMapCanvas overview`).
- **Đường nối** (Task 3.6, **Đã làm**): đường **nét đứt** 2px `jade` mờ 70% (đoạn 6px, hở 6px), nối các marker theo đúng thứ tự trong ngày, nằm dưới marker. Nét đứt vì đây là đường thẳng từ điểm này tới điểm kế tiếp, không phải đường đi thật; nét liền dễ bị đọc nhầm thành một con phố của nền bản đồ (chốt 2026-10-03 khi duyệt mockup, bản đầu ghi nét liền mờ 60%). Ngày có dưới 2 địa điểm thì không có đường. Hai hoạt động cùng một địa điểm: marker chồng lên nhau, đường không đổi. Không vẽ đường giữa các ngày khác nhau. Kéo thả đổi thứ tự thì đường vẽ lại ngay.
- **Thẻ thời tiết** (`features/weather/WeatherStrip`; Task 3.7, làm lại ở Task 3.9 theo mockup "Teal Voyage"): một thẻ trắng không viền, `shadow-md`, dưới thẻ bản đồ 16px, **cao cố định 212px ở mọi trạng thái** (thẻ bản đồ phía trên nhận phần còn lại của màn hình, và bản đồ chỉ đo khung của nó một lần). **Đầu thẻ** (cao 52px): icon `CloudSun` màu `sun`, "Thời tiết dự báo" (18px/600); bên phải là tên điểm đến của chuyến đi (12px `gray-500`, cắt bằng "…") và, khi phải chia trang, hai nút vuông 28px viền `tide` (`ChevronLeft` / `ChevronRight`, nhãn đọc "Các ngày trước" / "Các ngày sau"; 44px trên màn cảm ứng). **Thân thẻ**: mỗi ngày có dự báo một **ô rời**, bo 6px, nền `gray-100`, cách nhau 8px; ô là **link tới ngày đó**: "N4 · 05/10" (12px), icon 24px có màu, nhiệt độ cao (18px/700 `ink`), nhiệt độ thấp ngay bên dưới (12px `gray-500`), giọt nước `info` và phần trăm mưa (12px). Ô của ngày đang xem: nền `jade-light`, dòng đầu `jade-dark` đậm; rê chuột ô khác: `gray-200`. Ngày đã qua và ngày xa hơn 16 ngày **không có ô**. Ô rộng tối thiểu 82px: cột 380px có 4 ô một trang, cột 340px có 3; đủ chỗ cho mọi ô thì không có nút và các ô chia đều; trang cuối thiếu ô thì các ô giữ nguyên bề rộng. Mỗi lần bấm nút lật **cả một trang**, không đổi ngày đang xem; mỗi khi đổi ngày, thẻ tự lật tới trang có ô đó; đang xem một ngày không có ô thì không ô nào được tô. Không cuộn ngang. Khi không có ô nào, thân thẻ là một dòng có icon `gray-400` trên nền `gray-50`: chưa đặt vị trí điểm đến → `MapPinOff`, 'Đặt vị trí điểm đến trong "Sửa" chuyến đi để xem dự báo thời tiết.'; chuyến đi đã hết → `CalendarCheck`, "Chuyến đi đã qua, không còn dự báo thời tiết."; chuyến đi còn xa → `CalendarClock`, "Chưa có dự báo. Dự báo chỉ có cho 16 ngày tới, chuyến đi này bắt đầu sau đó."; không tải được, hoặc máy chủ trả `status = UNAVAILABLE` vì nguồn dự báo không trả lời (Task 3.8) → `CloudAlert`, "Tạm thời không có dự báo." kèm nút chữ `jade` "Thử lại" (tải lỗi mà đã có dữ liệu cũ thì giữ dữ liệu cũ; câu trả lời `UNAVAILABLE` thì thay dữ liệu cũ). Đang tải: khung xương lấp thân thẻ. Không có hộp "Khuyến nghị" của mockup (thuộc gợi ý AI, Phase 7). Viền `warning` cho ngày có cảnh báo: hoãn (design rule 14.21). Bản Task 3.7: một dải cao 112px có viền, các ô liền nhau ngăn bằng vạch, nhiệt độ dạng "32° / 25°", hai nút chuyển trang ở hai đầu dải.
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

Visual direction: "Teal Voyage", clean and calm. Time is the structural spine of the
itinerary. Flat colors, no gradients, no glassmorphism, no pulsing or looping animation.

Typography: Inter only. Sentence case, never all caps labels.
H1 32px/700, block title 18px/600, body 15px/400, caption 12px/400.

Colors:
- page background #F8F9FF, card and dialog surface #FFFFFF
- text #0B1C30, muted text #475569, border #D5DEEE (1px)
- brand teal #0F766E for primary buttons, links and focus; hover #005C55;
  light teal #E3F5F2 for selected items
- amber #E0A33C for weather and warnings, red #C2453B for errors, blue #2D6FA8 for info
Activity type colors: sightseeing #2D7DD2, food #E0662F, transport #64797F,
accommodation #7A5BA6, shopping #BE3C79, other #4F8A62.

Corner radius: inputs, buttons and badges 6px, cards 12px, dialogs 14px.
White cards on the page background have no border and a very soft shadow
(0 1px 8px rgba(11,28,48,.06)); cards inside a dialog use a 1px border.
Buttons never contain arrows.
All interface text in Vietnamese with full diacritics.
Only show features listed in the prompt; do not invent menus, stats or badges.
```

Cập nhật 2026-10-18 (Task 4.5) theo bảng màu "Teal Voyage" của Task 3.9 (mục 3, `tokens.css`). Bản trước dùng nền `#F4F6F5`, xanh `#0B7A6B`, thẻ có viền thay vì bóng.

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
| 3.9 | Bảng màu mới cho toàn web và trang chuyến đi dạng thẻ theo mockup "Teal Voyage" (`trip-planner-screenshots/stitch_action_button_ui_redesign/`); kế hoạch và các quyết định ở `WORKFLOW.md` Task 3.9. Các mục 3, 5, 6, 8.1, 9 của file này được sửa dần theo từng commit của task | Phase 3 |
| 4.5 | Hộp thoại chia sẻ (mời, vai trò, gỡ, liên kết), huy hiệu vai trò, chế độ chỉ xem, thẻ "Được chia sẻ", trang nhận lời mời, trang công khai | Phase 4 (rà 2026-10-06) |
| 4.9 | Ô chọn kiểu mới (tiền tệ, ngày đích, vai trò), menu trạng thái và sắp xếp, nút gạt vai trò, ô giờ gõ nhanh, lịch chọn ngày đi – về, chip hạn liên kết, ô ghi chú tự giãn | Phase 4 (làm trước 4.6, 2026-10-18) |
| 4.6 | Panel bình luận, bình luận trên hoạt động | Phase 4 |
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

#### Phase 4 — Task 4.5: chia sẻ và thành viên (rà 2026-10-06, sửa 2026-10-18)

**Dữ liệu có thật:** thành viên: tên (lời mời gửi tới email chưa có tài khoản thì chưa có tên), email, ảnh đại diện (thường chưa có: hiện chữ cái đầu), vai trò (Chủ sở hữu / Chỉnh sửa / Chỉ xem), trạng thái lời mời (đang chờ) và ngày mời; liên kết chia sẻ: chỉ quyền xem, ngày hết hạn (tuỳ chọn), đã hết hạn hay chưa, số lượt xem, thu hồi. **Không có hạn mức** số thành viên hay số liên kết (design 9, chốt 2026-10-06). Thành viên không phải chủ chỉ thấy danh sách thành viên.
**Không được thêm:** nhóm thành viên, phân quyền theo từng ngày, trò chuyện trực tiếp, ô chọn quyền của liên kết (chỉ có xem), công tắc bật / tắt liên kết (tạo và thu hồi là hai nút), ảnh thành viên ở ngoài hộp thoại, ngày hết hạn của lời mời (API không trả), nút "Mời thành viên" ở trạng thái rỗng (form "Mời" đã ở ngay trên).

Hộp thoại của chủ chuyến:

```
A dialog 640px titled "Chia sẻ chuyến đi" over the trip detail page.
Section "Mời thành viên": one row with an email input, a role select "Chỉnh sửa" / "Chỉ xem"
and a jade primary button "Mời".
Section "Thành viên (4)": rows with a 32px round avatar (initials on a light jade circle when
there is no photo), name, email in muted 13px text, and on the right a small role select and a
gray icon button "Gỡ". The owner row has a "Chủ sở hữu" badge instead of the select and no
remove button. One pending invite with no account: initials from the email, only the email as
text, an amber badge "Đang chờ", a muted line "Đã mời 12/10" and a ghost button "Gửi lại".
A 1px divider, then section "Liên kết chia sẻ" with the muted line "Ai có liên kết đều xem được
chuyến đi, không cần tài khoản.", an optional date field "Hết hạn sau ngày" and a secondary
button "Tạo liên kết". Two link rows: a read-only URL field with a "Sao chép" button, under it
"12 lượt xem · Không hết hạn" and a small red outline button "Thu hồi". The second row has a
gray badge "Hết hạn" and "3 lượt xem · Hết hạn ngày 10/10".
```

Hộp thoại của thành viên không phải chủ (dựng như biến thể của hộp trên):

```
The same dialog seen by a member who is not the owner, titled "Thành viên chuyến đi": only the
member list, each row with a role badge instead of a select, no remove buttons, no invite form,
no share link section.
```

Trạng thái rỗng (danh sách chỉ có dòng của chủ): câu "Chỉ mình bạn thấy chuyến đi này. Mời bạn bè để cùng chỉnh sửa." dưới form mời, không có nút (mục 7.9).

Ngoài hộp thoại, cùng task (không cần mockup): nút phụ "Chia sẻ" (icon `Share2`, chủ chuyến) hoặc "Thành viên" (icon `Users`, thành viên khác) ở đầu trang chi tiết; huy hiệu vai trò cạnh tên chuyến (`Badge`: Chủ sở hữu `brand`, Chỉnh sửa `info`, Chỉ xem `neutral`), chỉ khi chuyến có thành viên khác hoặc người xem không phải chủ; người chỉ xem không thấy nút Sửa / Xoá / Thêm hoạt động / tay kéo / menu "⋮" / ô đổi trạng thái và không được hỏi "Hoàn thành chuyến đi?"; thẻ ở danh sách có huy hiệu "Được chia sẻ · của {tên chủ}" (`info`).

#### Phase 4 — Task 4.5: trang nhận lời mời (`/invite?trip=&token=`)

**Dữ liệu có thật:** kết quả của `POST /members/accept` và email của tài khoản đang đăng nhập. Trang không có form: mở ra là gọi ngay. Máy chủ không trả email được mời khi từ chối, nên trang không ghi được email đó.

```
A small centered card 420px on the paper background, app logo above it. While accepting: a
spinner and "Đang nhận lời mời…". On success the page navigates to the trip. Two error
states, each with an icon, a short explanation and a secondary button:
"Lời mời không còn hiệu lực" with the line "Lời mời đã được dùng, đã hết hạn hoặc đã bị huỷ.
Nhờ chủ chuyến đi gửi lại." and the button "Về danh sách chuyến đi";
"Lời mời này dành cho email khác" with the line "Bạn đang đăng nhập bằng an@example.com." and
the button "Đăng xuất để đổi tài khoản".
```

Cùng task (không cần mockup): trang đăng nhập / đăng ký mở từ link mời có thêm một dòng hướng dẫn "Đăng nhập (hoặc đăng ký) bằng đúng email được mời. Tài khoản mới: xác thực email xong thì mở lại link trong mail mời."

#### Phase 4 — Task 4.5: trang chuyến đi công khai (`/share/:token`)

**Dữ liệu có thật:** như màn F nhưng chỉ đọc: tên, mô tả, điểm đến, ngày đi, các ngày (tiêu đề của ngày, không có ghi chú của ngày) và hoạt động (giờ, loại, tên, ghi chú, địa điểm). Không có thông tin tài khoản của người xem, tên chủ chuyến, thành viên, bình luận, chi phí, link đặt chỗ, thời tiết và quãng đường (chưa có endpoint công khai).
**Không được thêm:** thanh điều hướng của ứng dụng, chuyển ngày bằng cột trái hay chip (mọi ngày nằm trên một trang), số thứ tự và đường nối trên bản đồ (chốt 2026-10-18), nút thích / chia sẻ mạng xã hội, số lượt xem.

```
Public read-only trip page, no app navigation, paper background, a small logo at the top left.
One column 760px centered: trip title 32px/700, the facts line with icons (destination, dates,
number of days), the description with "Đọc thêm".
A map card 320px high with every place of the trip: teardrop markers in the colour of the
activity type, no numbers, no connecting line.
Then every day in order: "Ngày 1 · Thứ năm, 12/03" and the day title in jade, then the timeline
rail with activity cards (time, type chip, title, place row, note), without drag handles, "⋮"
menus, add buttons, costs or booking links. An empty day shows a muted line "Chưa có hoạt động".
At the end a centered secondary button "Tạo chuyến đi của bạn", a small line "Được chia sẻ qua
Smart Trip Planner" and "Bản đồ và địa điểm © OpenStreetMap".
```

Trạng thái lỗi (liên kết sai, đã thu hồi, hết hạn, chuyến đã xoá: cùng một trang):

```
The same public page frame with only a small centered card 420px: a link-off icon, the title
"Liên kết không còn hiệu lực", the line "Liên kết đã bị thu hồi, đã hết hạn, hoặc chuyến đi
không còn." and a secondary button "Tạo chuyến đi của bạn".
```

#### Phase 4 — Task 4.9: ô chọn, ngày, giờ, ghi chú kiểu mới

**Dữ liệu có thật:** 7 loại tiền (VND, USD, EUR, JPY, KRW, THB, SGD), 5 trạng thái chuyến đi (mục 3.5), 2 vai trò mời được (Chỉnh sửa, Chỉ xem), giờ dạng HH:mm, ngày đi – ngày về (chuyến tối đa theo design 9), hạn liên kết chỉ chọn ngày; ghi chú hoạt động tối đa 255 ký tự, mô tả chuyến đi tối đa 1000.
**Không được thêm:** ô tìm trong danh sách tiền tệ, chọn giây, AM/PM, khung giờ gợi ý theo AI, chọn nhiều giá trị cùng lúc, nút "Hôm nay" trong lịch, khoá ngày đã qua ở lịch chọn ngày đi (chỉ hạn liên kết mới khoá), tấm trượt từ đáy trên điện thoại.

Bảng thành phần (dựng trước, để chốt kiểu chung):

```
A component sheet on the paper background showing form controls of the app, each with a small
caption above it. Row 1: a 40px select "Tiền tệ" closed, and the same select open as a floating
list (white, 1px border #D5DEEE, 6px radius, soft shadow) with rows "VND · Đồng Việt Nam",
"USD · Đô la Mỹ", "EUR · Euro"; the chosen row has a teal check on the right, the hovered row a
light teal background. Row 2: a segmented control "Chỉnh sửa | Chỉ xem" (36px, the chosen half
white with a soft shadow on a gray #EEF2F8 track). Row 3: a trip status badge "Đang lên kế hoạch"
with a small chevron, and its open menu: five rows with a colored dot and label, a check on the
current one. Row 4: a time input "09:30" focused with a floating list of times every 15 minutes,
"09:30" highlighted; a second time input "11:00" with a muted suffix "(1 giờ 30 phút)". Row 5: a
textarea "Ghi chú" with three lines of text and a muted counter "230 / 255" at the bottom right.
```

Lịch chọn ngày đi – ngày về (bước 2 của wizard tạo chuyến):

```
Step 2 "Ngày đi" of the create trip wizard. One field "Ngày đi – ngày về *" showing
"12/03/2026 – 16/03/2026" with a calendar icon, under it a muted line "5 ngày 4 đêm". The field is
open: a floating card with two months side by side in Vietnamese ("Tháng 3 2026", weekdays
"T2 T3 T4 T5 T6 T7 CN"), the range 12–16 March filled light teal with teal solid circles on the
start and end days, today outlined. Previous / next month arrows at the top corners.
```

Hộp chia sẻ với ô kiểu mới (biến thể của prompt Task 4.5 ở trên):

```
The "Chia sẻ chuyến đi" dialog. Invite row: email input, then the segmented control
"Chỉnh sửa | Chỉ xem", then the jade primary button "Mời". Member rows use a small 32px select
for the role. Section "Liên kết chia sẻ": label "Hết hạn", a row of chips "Không hết hạn"
(selected, light teal with teal border), "7 ngày", "30 ngày", "Chọn ngày…", and a secondary button
"Tạo liên kết".
```

#### Phase 4 — Task 4.6: bình luận

**Dữ liệu có thật:** bình luận của chuyến đi hoặc của một hoạt động, trả lời một cấp (`parent_id`), người viết (tên, ảnh: hiện mọi tài khoản đều chưa có ảnh nên là chữ cái đầu), thời điểm đăng, nội dung chữ thuần tối đa 2000 ký tự, xoá bình luận của mình (chủ chuyến xoá được của mọi người; xoá gốc mất luôn trả lời). Ai xem được chuyến đi đều đọc và đăng được, kể cả người chỉ xem. Không sửa bình luận. Panel và hộp thoại chia sẻ không mở cùng lúc; dưới 1024px panel chiếm toàn màn. Chi tiết ở mục 8.1 "Bình luận" (viết lại 2026-10-19 theo quyết định của WORKFLOW Task 4.6).
**Không được thêm:** sửa bình luận, thả cảm xúc / "thích", nhắc tên bằng @, đính kèm ảnh / tệp, định dạng chữ (đậm, link, markdown), trả lời nhiều cấp, "đã xem", chấm "đang gõ…", số người online, thông báo, ô tìm trong bình luận, sắp xếp mới nhất trước.

Panel trên máy tính (dựng trước, để chốt kiểu chung):

```
The trip detail page at 1440px, dimmed by a dark overlay, with a white side panel 360px tall as
the screen sliding in from the right edge, rounded only on its left corners, soft shadow. Header:
title "Bình luận" and an "×" button. Scrollable list, oldest at the top: each comment has a 32px
round avatar with initials on a light teal circle, the name in bold 14px and a muted 12px time
on the same line ("2 giờ trước", "Hôm qua lúc 14:30"), then the text in 14px gray, line breaks
kept. One comment has a small gray chip between the name and the text: an 8px orange dot and
"Bún chả cá 109". Under each root comment a small muted text button "Trả lời". One comment has
one reply indented 40px with a 24px avatar and no "Trả lời". No borders between comments, 16px
apart. Fixed footer: a textarea "Viết bình luận…" three lines tall and, under it on the right,
the teal primary button "Gửi bình luận" with a send icon.
```

Panel đang lọc theo một hoạt động, đang trả lời (biến thể của prompt trên):

```
The same comment panel opened from an activity card. Under the title a removable chip with a
blue dot "Chùa Linh Ứng ×". Only two comments about this activity. The pointer hovers the second
comment, which shows a small ghost trash icon button at the end of its name row. In the footer,
above the textarea, a small chip "Trả lời Mai Anh ×"; the textarea holds one line of text and is
focused with a teal ring; a muted counter is not shown.
```

3a. Nút bình luận trên thẻ hoạt động và ở đầu trang (máy tính):

```
The trip detail page at 1440px, no dialog open. In the trip header, on the right, the secondary
button "Bình luận · 5" with a speech bubble icon, placed just before the secondary button
"Chia sẻ". In the itinerary, two activity cards (white, soft shadow, 12px radius). In the top row
of each card, at the right edge just before the "⋮" button, a small gray speech bubble icon
button: the first card shows the count "3" next to the icon, the second card shows only the icon
because the pointer is hovering that card.
```

3b. Panel trên điện thoại (đoạn mở đầu đổi `Desktop 1440px wide.` thành `Mobile 390px wide.`):

```
The comment panel on a 390px phone, full screen, no rounded corners, no overlay visible. Header
"Bình luận" with an "×" button. The list as on desktop: 32px avatars with initials, name and
muted time on one line, text under it, one indented reply with a 24px avatar, a small chip with
an orange dot "Bún chả cá 109" on one comment, a muted "Trả lời" under each root comment. Fixed
at the bottom: the textarea "Viết bình luận…" and the full-width teal primary button
"Gửi bình luận" with a send icon.
```

**Kết quả duyệt mockup (2026-10-19, ảnh ở `trip-planner-screenshots/stitch_smart_trip_planner_4.6/`, chủ dự án đồng ý):** prompt 3 ban đầu ghép thẻ máy tính và panel điện thoại vào một màn (trái mục 15.1 điểm 2) nên tách thành 3a / 3b như trên.
- **Giữ:** khung panel (mép phải 360px, bo trái, lớp phủ 45%, đầu và chân đứng yên có vạch), một bình luận (ảnh 32px chữ cái đầu, tên đậm + thời gian xám cùng hàng, nội dung thẳng cột với tên, "Trả lời" nhỏ xám, không khung), trả lời thụt vào ảnh 24px, chip hoạt động nền xám có chấm màu tuyến, chip lọc dưới tiêu đề, thùng rác cuối hàng tên khi rê chuột, chip "Trả lời Mai Anh ×" nền xanh nhạt trên ô nhập, nút "Bình luận · 5" trước "Chia sẻ", nút bong bóng + số trước "⋮" trên thẻ, panel toàn màn trên điện thoại.
- **Giữ thêm (mockup có, mục 8.1 chưa ghi):** đang lọc theo hoạt động thì bình luận không lặp chip; nền `gray-50` khi rê chuột lên một bình luận; nút "Gửi bình luận" rộng hết hàng dưới 640px.
- **Không theo:** thanh menu trên cùng ("Lịch trình chi tiết · Thời gian biểu · Bản đồ tuyến · Khám phá", nút "Thảo luận", icon người dùng) và chân trang tự bịa; bố cục trang chuyến đi phía sau panel và kiểu thẻ hoạt động của mockup (giữ mục 8.1 và 7.3); nền xanh nhạt của nút bình luận khi rê chuột (dùng `gray-100` như "⋮"); phông có chân trong panel (chỉ Inter); khoảng trống lớn giữa hàng tên và nội dung; ô nhập co còn 1 dòng và vòng focus đặc 2px (giữ `TextAreaField` 3 → 8 dòng, vòng `jade/25` 3px); icon người trước tiêu đề, "Bình Luận" viết hoa và nền đầu panel xanh nhạt trên điện thoại; "Trả lời" đậm đen trên điện thoại.

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

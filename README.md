# Smart Trip Planner

Web app lên kế hoạch du lịch: tạo lịch trình theo ngày, gắn địa điểm lên bản đồ, xem thời tiết, chia sẻ & đồng chỉnh sửa real-time, theo dõi chi phí, nâng cấp Premium qua Stripe.

## Tính năng đã có

**Phase 1 — Xác thực (hoàn thành 2026-09-25)**

- Đăng ký tài khoản, mật khẩu băm BCrypt, validate cùng luật ở cả frontend (zod) và backend (Bean Validation)
- Xác thực email qua link gửi mail (bất đồng bộ, template Thymeleaf), gửi lại mail xác thực
- Đăng nhập bằng JWT: access token 15 phút chỉ giữ trong memory, refresh token 7 ngày trong cookie `HttpOnly` + `SameSite=Lax`, DB chỉ lưu SHA-256
- **Refresh token rotation** + phát hiện dùng lại token đã thu hồi (token theft → thu hồi mọi phiên)
- Frontend tự refresh khi access token hết hạn (`TOKEN_EXPIRED`), single-flight trong tab và giữa các tab (Web Locks) để không bao giờ xoay token song song
- Quên / đặt lại mật khẩu (link 1 giờ, dùng một lần, đặt lại xong thu hồi mọi phiên đăng nhập)
- Chống dò email: các endpoint công khai nhận email luôn trả cùng một phản hồi
- Mã lỗi thống nhất (`ErrorResponse` + `errorCode`), message tiếng Việt từ `messages.properties`

**Phase 2 — Chuyến đi và lịch trình (hoàn thành 2026-09-30)**

- Tạo, sửa, xoá chuyến đi; đổi ngày đi thì các ngày của lịch trình tự được tạo / dời theo
- Hoạt động trong từng ngày: thêm, sửa, xoá, kéo thả đổi thứ tự hoặc chuyển sang ngày khác; cảnh báo trùng giờ
- Trang lịch trình một ngày một trang, dùng được trên điện thoại

**Phase 3 — Địa điểm, bản đồ, thời tiết (hoàn thành 2026-10-05)**

- Tìm địa điểm theo tên và gắn vào hoạt động, hoặc tự thêm địa điểm bằng cách chấm một điểm trên bản đồ
- Bản đồ của từng ngày (Leaflet + OpenStreetMap), quãng đường và thời gian di chuyển giữa các hoạt động
- Dự báo thời tiết 16 ngày tới cho điểm đến của chuyến đi
- Kết quả từ nguồn bên ngoài được giữ tạm trong Redis; Redis tắt thì ứng dụng vẫn chạy
- Hai bộ nguồn dữ liệu, đổi bằng cấu hình: **dữ liệu có sẵn** (mặc định, không cần mạng) và **dữ liệu thật** từ OpenStreetMap (Photon, Nominatim, OSRM) và Open-Meteo, không cần API key
- Khi nguồn thật lỗi: tự thử lại, ngắt mạch khi dịch vụ sập (Resilience4j), trang chuyến đi vẫn dùng được

Lộ trình các phase tiếp theo (chia sẻ, realtime, Stripe, chi phí, AI gợi ý...): [WORKFLOW.md](WORKFLOW.md). Kịch bản test và tình trạng từng kịch bản: [docs/testing/README.md](docs/testing/README.md).

## Tech stack

| Phần | Công nghệ |
|---|---|
| Backend | Spring Boot 4.1 / Spring Security 7 / Java 21 / Gradle (Groovy DSL) |
| Database | MySQL 8 + Flyway (`ddl-auto: validate` ở mọi profile) |
| Cache | Redis 7 |
| Mail (dev) | MailHog |
| Frontend | React 19 + Vite 8 + TypeScript + Tailwind v4 |
| Frontend state | TanStack Query v5 (server state), Zustand (auth), React Router v7, react-hook-form + zod |
| Test | JUnit 5, Mockito, Testcontainers (MySQL thật trong Docker) |

## Cấu trúc repo

```
smart-trip-planner/
├── design.md            # Thiết kế hệ thống — nguồn sự thật
├── WORKFLOW.md          # Lịch trình thực hiện theo task
├── CLAUDE.md            # Quy ước code
├── .env.example         # Mẫu biến môi trường
├── docker-compose.yml   # mysql, redis, mailhog
├── backend/             # Spring Boot
└── frontend/            # React + Vite (proxy /api → backend khi dev)
```

## Yêu cầu

- JDK 21
- Docker Desktop (hạ tầng local + Testcontainers khi chạy test)
- Node.js 22+ và npm 10+

## Chạy local

```bash
# 1. Biến môi trường (ở thư mục gốc repo)
cp .env.example .env                 # PowerShell: Copy-Item .env.example .env
#    đổi JWT_SECRET thành chuỗi ngẫu nhiên >= 64 ký tự

# 2. Hạ tầng
docker compose up -d mysql redis mailhog

# 3. Backend — http://localhost:8080
cd backend
./gradlew build                      # compile + toàn bộ test (cần Docker đang chạy)
./gradlew bootRun --args='--spring.profiles.active=local'

# 4. Frontend — http://localhost:5173 (terminal khác)
cd frontend
cp .env.example .env                 # PowerShell: Copy-Item .env.example .env
npm install
npm run dev
```

| Địa chỉ | Dùng để |
|---|---|
| http://localhost:5173 | Ứng dụng web |
| http://localhost:8080/swagger-ui.html | Tài liệu + thử API |
| http://localhost:8025 | MailHog — xem mail xác thực / đặt lại mật khẩu |

**Dùng thử:** mở http://localhost:5173 → *Đăng ký* → mở MailHog, bấm link trong mail "Xác thực email" → *Đăng nhập* → vào trang *Chuyến đi của tôi*.

### Dùng dữ liệu thật (tuỳ chọn)

Mặc định ứng dụng chạy bằng dữ liệu có sẵn: 56 địa điểm ở 5 thành phố, quãng đường ước lượng theo đường chim bay, thời tiết giả lập. Muốn tìm được mọi địa điểm, có quãng đường theo đường thật và dự báo thật, chạy backend với hai tham số thêm (cần mạng, **không cần đăng ký hay API key**):

```bash
./gradlew bootRun --args='--spring.profiles.active=local --app.providers.map=osm --app.providers.weather=open-meteo'
```

| Tham số | Giá trị | Dữ liệu lấy từ |
|---|---|---|
| `app.providers.map` | `mock` (mặc định) \| `osm` | Photon (tìm địa điểm), Nominatim (lưu địa điểm đã chọn), OSRM (quãng đường, tính cho ô tô) |
| `app.providers.weather` | `mock` (mặc định) \| `open-meteo` | Open-Meteo |

Đây là các máy chủ công cộng miễn phí dùng chung, có điều khoản "dùng hợp lý" (Nominatim: tối đa 1 lần gọi mỗi giây; ứng dụng tự giữ giới hạn này). Chỉ nên bật khi chạy trên máy mình; **đừng bật trên bản deploy công khai** cho tới khi có hạn mức theo người dùng (Task 8.1). Test tự động không bao giờ gọi ra ngoài.

> - `./gradlew bootRun` đứng ở `80% EXECUTING` là bình thường (app đang chạy). Dừng bằng Ctrl+C.
> - Chạy/debug backend bằng IntelliJ: Run configuration Spring Boot, profile `local`, **Working directory = `backend/`** — sai thư mục thì không đọc được `../.env` (lỗi `Could not resolve placeholder 'MAIL_HOST'`).
> - macOS/Linux báo `permission denied` khi chạy `./gradlew`: `chmod +x gradlew && git update-index --chmod=+x gradlew`

## Tài liệu

- Thiết kế: [design.md](design.md)
- Lịch trình & tiến độ: [WORKFLOW.md](WORKFLOW.md)
- Quy ước code: [CLAUDE.md](CLAUDE.md)

## License

MIT

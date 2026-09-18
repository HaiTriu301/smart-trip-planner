# Smart Trip Planner

Web app lên kế hoạch du lịch: tạo lịch trình theo ngày, gắn địa điểm lên bản đồ, xem thời tiết, chia sẻ & đồng chỉnh sửa real-time, theo dõi chi phí, nâng cấp Premium qua Stripe.

## Tech stack

| Phần | Công nghệ |
|---|---|
| Backend | Spring Boot 4.1 / Java 21 / Gradle (Groovy DSL) |
| Database | MySQL 8 + Flyway |
| Cache | Redis 7 |
| Frontend | React 19 + Vite 8 + TypeScript + Tailwind v4 (TanStack Query, React Router v7, Zustand) |

## Cấu trúc repo

```
smart-trip-planner/
├── design.md        # Thiết kế hệ thống — nguồn sự thật
├── WORKFLOW.md      # Lịch trình thực hiện theo task
├── CLAUDE.md        # Quy ước code cho Claude Code
├── .env.example     # Mẫu biến môi trường
├── backend/         # Spring Boot
└── frontend/        # React + Vite (proxy /api → backend khi dev)
```

## Yêu cầu

- JDK 21
- Docker + Docker Compose
- Node.js 22+ và npm 10+ (Vite 8)

## Chạy local

```bash
# 1. Tạo file biến môi trường
cp .env.example .env

# 2. Kiểm tra toolchain
cd backend
./gradlew -v          # Gradle 9.x, JVM 21

# 3. Build & test
./gradlew build

# 4. Chạy hạ tầng + backend
docker compose up -d mysql redis mailhog
./gradlew bootRun --args='--spring.profiles.active=local'   # http://localhost:8080, Swagger: /swagger-ui.html

# 5. Chạy frontend (terminal khác)
cd frontend
cp .env.example .env      # PowerShell: Copy-Item .env.example .env
npm install
npm run dev               # http://localhost:5173 — trang chủ gọi /api/v1/ping qua proxy, hiện "pong"
```

> macOS/Linux báo `permission denied` khi chạy `./gradlew`:
> `chmod +x gradlew && git update-index --chmod=+x gradlew`

## Tài liệu

- Thiết kế: [design.md](design.md)
- Lịch trình: [WORKFLOW.md](WORKFLOW.md)

## License

MIT
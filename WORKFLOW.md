# WORKFLOW.md — Lịch trình thực hiện Smart Trip Planner

> File này nói **làm gì, theo thứ tự nào, tạo file nào, commit lúc nào**.
> Mỗi Task là một lần ngồi làm (1–3 tiếng). Làm xong một Task mới sang Task kế tiếp. Không nhảy cóc.

---

## A. Cách dùng file này

### A.1. Vòng lặp cho mỗi Task

```
1. Đọc phần Task trong file này
2. Đọc mục design.md được trỏ tới
3. git checkout -b <branch của task>
4. Duyệt bảng commit dự kiến của task (Claude đưa ra trước khi code, theo quy ước A.2 "Chia commit")
5. Lặp cho từng mốc: code 1 lát cắt dọc (code + test) → build xanh → commit → mốc tiếp theo
6. Chạy kiểm tra ở phần "Nghiệm thu"
7. git push + mở Pull Request trên GitHub + tự merge
8. Tick [x] vào bảng theo dõi ở mục cuối file này
9. Commit docs/testing/ trong cùng commit docs đóng task (nội dung đã được ghi dần sau mỗi mốc ở bước 5:
   test case mới, lỗi đã bắt được, bảng tổng hợp)
```

> **Ai làm gì:** các bước Git (3, commit ở bước 5, 7) do **tôi tự chạy**. Claude Code không tự tạo nhánh, không tự `git add` / `commit` / `push`, không mở PR — chỉ đưa ra lệnh tạo nhánh (nếu đang sai nhánh) và danh sách commit đề xuất (file cần add + message) để tôi tự commit. Chi tiết ở `CLAUDE.md` mục 9.

### A.2. Quy ước Git dùng xuyên suốt

- Nhánh chính: `main` (luôn chạy được, luôn xanh CI)
- Mỗi Task = 1 nhánh = 1 Pull Request. **Đừng commit thẳng vào `main`** — lịch sử PR trên GitHub là thứ nhà tuyển dụng nhìn thấy.
- **Ngoại lệ — được push thẳng lên `main`, không cần nhánh/PR:** thay đổi chỉ thuộc phần cài đặt/tài liệu, **không đụng code chức năng**:
  - Hạ tầng local: `docker-compose.yml`, `.env.example`, `.gitignore`
  - Tài liệu: `*.md` (`README.md`, `CLAUDE.md`, `WORKFLOW.md`, `design.md`), tick bảng theo dõi, tick phase
  - Điều kiện: không sửa gì trong `backend/src/`, `frontend/src/`, không đổi dependency/build (`build.gradle`, `libs.versions.toml`, `package.json`), không thêm migration
  - Nếu một task có cả cài đặt lẫn code (ví dụ Task 0.3 có `AppProperties.java` + `build.gradle`, Task 0.5 có `client.ts` + `package.json`) → vẫn tạo nhánh + PR như bình thường
  - Nhánh ghi trong task nào thuộc ngoại lệ này thì bỏ qua, commit message giữ nguyên
- Tên nhánh: `feat/T1.3-jwt-authentication`
- Commit message theo Conventional Commits:

```
feat(auth): add refresh token rotation
fix(trip): correct day generation when end date changes
test(trip): add permission test for viewer role
chore(ci): add jacoco coverage gate
docs(readme): add setup instructions
refactor(activity): extract reorder logic to service
```

- Commit nhỏ, mỗi commit làm **một việc**, và **code phải chạy được** sau mỗi commit.

#### Chia commit trong một task (chốt 2026-09-26, áp dụng từ Task 2.2)

1. **Lát cắt dọc:** mỗi commit là **một chức năng hoàn chỉnh** (create, get, list, update, delete...), đi qua đủ các tầng repository → dto → mapper → service → controller. Không chia theo tầng (commit entity, commit service, commit controller).
2. **Test đi cùng code:** test của chức năng nằm **trong chính commit đó**, không gom vào một commit `test(...)` ở cuối. Ngoại lệ duy nhất: integration test ghép toàn luồng của task là commit cuối.
3. **Mỗi commit tự đứng được:** build xanh; không có code / cấu hình / key message chưa có ai dùng; `git revert` một commit chỉ làm mất đúng chức năng đó.
4. **File hạ tầng nằm ở commit của chức năng đầu tiên cần nó:**
   - Migration tạo bảng + entity: **luôn chung một commit** (thiếu một trong hai thì `ddl-auto=validate` làm app không lên); là commit đầu của task nếu nhiều chức năng dùng bảng đó.
   - Migration nhỏ (thêm cột, index), `application.yml`, `messages.properties`, dependency: trong commit của chức năng dùng nó.
   - Sửa code cũ để chức năng mới dùng được (ví dụ `AppException` thêm `details`): commit `refactor(...)` riêng, đặt **trước** commit chức năng. Commit refactor không đổi hành vi.
   - Hạ tầng dùng chung: commit riêng **chỉ khi** nhiều chức năng dùng **và** có test riêng; còn lại đi cùng chức năng đầu tiên dùng nó.
   - Cấu hình không gắn chức năng nào (logging, profile...): commit `chore(...)` riêng.
   - Docs: commit riêng lên `main` (ngoại lệ ở trên).
5. **Quy mô gợi ý:** khoảng 3–8 file mỗi commit, một lý do để thay đổi. Bảng commit dự kiến phải ghi **số file của từng commit**; dòng nào quá 8 file thì tách, hoặc ghi ngay trong bảng vì sao không tách được (chốt 2026-10-01, Task 3.2).
   Điểm tách đã dùng: (a) migration + entity + test mapping là commit riêng đứng đầu; (b) một method mới của provider, có test riêng, là commit riêng ngay trước chức năng gọi nó; (c) endpoint ở dạng đơn giản nhất trước, mỗi quy tắc thêm (xử lý đồng thời, một kiểu từ chối) là commit riêng; (d) phần hiển thị tách khỏi phần ghi; (e) "đổi" tách khỏi "bỏ"; (f) trường mới làm vỡ nhiều chỗ tạo dữ liệu trong test → trước đó một commit `refactor(test)` gom chúng về một hàm, không đổi hành vi; (g) test toàn luồng chỉ ở commit cuối, trừ test là bằng chứng trực tiếp của một commit.
6. **Quy trình với Claude Code:** đầu task Claude đưa **bảng commit dự kiến** (tên commit + file) để duyệt trước khi code. Mỗi mốc = **một commit**: code → build xanh → Claude đưa lệnh `git add` + `git commit` → dừng chờ tôi commit → mốc tiếp theo. File dùng chung (service, controller, mapper, test class) được viết dần: commit nào chỉ chứa phần chức năng của commit đó cần.

> Task 0.1 → 2.1 làm theo cách chia cũ (mốc lớn, test tách riêng). Giữ nguyên lịch sử, không reset / force push.
>
> **Rà soát theo phase (chốt 2026-09-26):** Phase 3 trở đi vẫn ghi mốc theo kiểu cũ. **Đầu mỗi phase**, trước khi tạo nhánh cho task đầu tiên, rà lại cả phase: viết lại mốc theo lát cắt dọc, cập nhật những gì các phase trước làm thay đổi, rồi commit docs lên `main`. Phase chưa rà có dòng ⏳ ngay dưới tiêu đề; khối `Commit:` kiểu cũ của phase đó chỉ để tham khảo. Rà xong thì xoá dòng ⏳.
- Merge PR bằng **Squash and merge** nếu commit trong nhánh lộn xộn, dùng **Merge commit** nếu commit đã sạch.

### A.3. Mẫu mô tả Pull Request

```markdown
## Nội dung
Ngắn gọn task này làm gì.

## Thay đổi
- Thêm endpoint POST /api/v1/auth/register
- Thêm migration V2__create_users.sql
- Thêm UserService, AuthService

## Cách test
1. docker compose up -d mysql redis mailhog
2. ./gradlew bootRun --args='--spring.profiles.active=local'
3. POST /api/v1/auth/register với body {...} → 201
4. Mở http://localhost:8025 xem mail verify

## Checklist
- [ ] Đã chạy ./gradlew build, tất cả test xanh
- [ ] Đã thêm test cho happy path + case lỗi
- [ ] Không có secret hardcode
- [ ] Đã cập nhật CLAUDE.md mục 7 nếu xong phase
```

### A.4. Mẫu prompt khi nhờ Claude Code

```
Đọc design.md mục <số mục> và WORKFLOW.md Task <mã task>.
Implement <nội dung>.
Dùng Spring Boot 4.1 / Spring Security 7, không dùng cú pháp Boot 3.x.
Liệt kê file sẽ tạo trước, sau đó code đầy đủ không để TODO.
Viết kèm unit test cho service và test 403 cho phân quyền.
Cuối cùng chạy ./gradlew build và sửa tới khi xanh.
Không tự tạo nhánh, không tự commit — chỉ đưa ra danh sách commit đề xuất để tôi tự commit.
```

---

## PHASE 0 — Khởi tạo dự án

Mục tiêu: có một backend rỗng chạy được, kết nối DB, có Swagger, có format response chuẩn. **Không có nghiệp vụ nào ở phase này.**

### Task 0.1 — Tạo repo và khung thư mục

Nhánh: `chore/T0.1-init-repo`

**Việc làm:**
1. Tạo repo GitHub tên `smart-trip-planner`, chọn Add README, .gitignore = Java, License = MIT.
2. Clone về máy.
3. Vào https://start.spring.io tạo project:
   - Project: **Gradle - Groovy**, Language: **Java**, Spring Boot: **4.1.x** (bản stable, không phải SNAPSHOT/M/RC)
   - Group: `com.trieu`, Artifact: `tripplanner`, Package name: `com.trieu.tripplanner`
   - Packaging: **Jar**, Java: **21**
   - Dependencies: `Spring Web`, `Spring Data JPA`, `MySQL Driver`, `Flyway Migration`, `Validation`, `Lombok`, `Spring Boot DevTools`, `Spring Boot Actuator`, `Spring Configuration Processor`
4. Giải nén vào thư mục `backend/` trong repo. Kiểm tra có đủ `gradlew`, `gradlew.bat`, `gradle/wrapper/` — **phải commit các file này**.
5. Copy `design.md`, `CLAUDE.md`, `WORKFLOW.md` vào thư mục gốc repo.

**File tạo thêm ở gốc repo:**
```
.gitignore          ← thêm: .env, /backend/build, /backend/.gradle, /frontend/node_modules, /uploads
.env.example
README.md
```

`.env.example` (khớp file thật ở gốc repo; `MYSQL_ROOT_PASSWORD` bắt buộc vì docker-compose dùng `${VAR:?}` — thiếu là compose từ chối chạy; `allowPublicKeyRetrieval=true` cần cho MySQL 8 caching_sha2_password khi không dùng SSL):
```
DB_URL=jdbc:mysql://localhost:3306/tripplanner?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USER=tripuser
DB_PASSWORD=trippass
MYSQL_ROOT_PASSWORD=doi-mat-khau-root-nay
REDIS_HOST=localhost
REDIS_PORT=6379
JWT_SECRET=doi-chuoi-nay-thanh-chuoi-ngau-nhien-toi-thieu-64-ky-tu
APP_FRONTEND_URL=http://localhost:5173
MAIL_HOST=localhost
MAIL_PORT=1025
```

**Nghiệm thu:** `cd backend && ./gradlew -v` hiện Gradle 9.x + JVM 21, `java -version` ra 21.
Trên macOS/Linux nếu báo permission denied: `chmod +x gradlew` rồi `git update-index --chmod=+x gradlew`.

**Commit:**
```bash
git add .
git commit -m "chore: init spring boot project structure"
git commit -m "docs: add design, workflow and claude guidelines"
```

---

### Task 0.2 — Docker Compose hạ tầng

Nhánh: `chore/T0.2-docker-infra`

**File tạo:** `docker-compose.yml` ở gốc repo — services: `mysql:8.0` (port 3306, volume `mysql_data`, tạo sẵn database `tripplanner`), `redis:7-alpine` (port 6379), `mailhog` (1025 + 8025).

**Nghiệm thu:**
```bash
docker compose up -d
docker compose ps          # cả 3 service đều Up
# mở http://localhost:8025 thấy giao diện MailHog
```

**Commit:** `chore(infra): add docker compose for mysql, redis, mailhog`

---

### Task 0.3 — Cấu hình ứng dụng + Flyway migration đầu tiên

Nhánh: `chore/T0.3-config-flyway`

**File tạo theo thứ tự:**
```
backend/gradle/libs.versions.toml                                (version catalog)
backend/build.gradle                                          (bổ sung: testcontainers, mapstruct, annotation processor, cấu hình test { useJUnitPlatform() })
backend/src/main/resources/application.yml
backend/src/main/resources/application-local.yml
backend/src/test/resources/application-test.yml                 (test classpath, không đóng vào jar)
backend/src/main/resources/db/migration/V1__init_schema.sql     (tạo bảng flyway_history trống, hoặc chỉ 1 comment)
backend/src/main/java/com/trieu/tripplanner/config/properties/AppProperties.java
```

Điểm quan trọng trong `application.yml`:
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate      # KHÔNG BAO GIỜ để update/create ở đây
    open-in-view: false       # tránh lazy loading ngoài transaction
  flyway:
    enabled: true
management:
  endpoints:
    web:
      exposure:
        include: health,info
```

**Nghiệm thu:**
```bash
./gradlew bootRun --args='--spring.profiles.active=local'
curl http://localhost:8080/actuator/health   # {"status":"UP"}
```

**Commit:** `chore(config): add application profiles and flyway setup`

---

### Task 0.4 — Bộ khung chung: ApiResponse + Exception handler + Swagger

Nhánh: `feat/T0.4-common-layer`

Đọc trước: **design.md mục 10.1 và 10.3** (gồm bảng "Ánh xạ exception của framework")

> ⚙️ Trước khi tạo `messages.properties`: IntelliJ → Settings → Editor → File Encodings → *Default encoding for properties files* = **UTF-8**, bỏ tick *Transparent native-to-ascii conversion*. Nếu không, tiếng Việt sẽ bị lỗi font.

**File tạo theo thứ tự:**
```
common/ApiResponse.java              ← record chỉ cho THÀNH CÔNG: success, data, message, timestamp + static ok()
common/ErrorResponse.java            ← record chỉ cho LỖI: success, errorCode, message, details, timestamp, path + static of()
                                        (details = List<FieldError>, FieldError là record lồng {field, message})
common/PageResponse.java
common/constant/ErrorCode.java       ← enum, mỗi hằng có httpStatus + messageKey (KHÔNG chứa câu chữ)
src/main/resources/messages.properties  ← message tiếng Việt theo messageKey, UTF-8
exception/AppException.java          ← abstract, giữ ErrorCode
exception/ResourceNotFoundException.java
exception/BusinessRuleException.java
exception/GlobalExceptionHandler.java  ← @RestControllerAdvice
config/OpenApiConfig.java
config/CorsConfig.java
controller/HealthController.java     ← endpoint tạm GET /api/v1/ping để test
```

`GlobalExceptionHandler` trả về `ErrorResponse` (không dùng `ApiResponse` cho lỗi), phải bắt tối thiểu: `AppException`, `MethodArgumentNotValidException` (→ list field error), `ConstraintViolationException`, `Exception` (→ 500, log full stacktrace, trả message chung chung không lộ nội bộ).

> `AccessDeniedException` **chưa** bắt ở task này vì Spring Security đang tạm gỡ (Task 0.3) → class không tồn tại, code sẽ không compile. Handler này được thêm ở **Task 1.2** cùng lúc bật lại Security.

Bắt thêm các exception của Spring 7 — nếu thiếu, chúng rơi vào `Exception` và thành **500** (mapping đầy đủ ở design.md 10.3):
`NoResourceFoundException` (→ 404, **bắt buộc** để đạt nghiệm thu `/khong-ton-tai`), `HttpRequestMethodNotSupportedException` (→ 405), `HttpMessageNotReadableException` (→ 400), `MethodArgumentTypeMismatchException` (→ 400), `HandlerMethodValidationException` (→ 400).

**Nghiệm thu:**
- `GET /api/v1/ping` trả đúng envelope `{success, data, message, timestamp}`
- `GET /api/v1/khong-ton-tai` trả 404 đúng format `ErrorResponse` (`errorCode: RESOURCE_NOT_FOUND`), không phải trang lỗi mặc định của Spring
- Mở `http://localhost:8080/swagger-ui.html` thấy endpoint ping

**Commit (3 mốc):**
```
feat(common): add ApiResponse envelope and error codes
feat(common): add global exception handler
chore(docs): configure springdoc openapi
```

---

### Task 0.5 — Khởi tạo frontend

Nhánh: `chore/T0.5-init-frontend`

Đọc trước: **design.md mục 3.2** (stack + quy ước gọi API từ frontend)

Chạy ở **thư mục gốc repo** (thư mục `frontend/` rỗng có sẵn, create-vite dùng luôn):
```bash
npm create vite@latest frontend -- --template react-ts
#   → hỏi "Which linter to use?"          → ESLint
#   → hỏi "Install with npm and start now?" → No (cài tay bên dưới)
cd frontend && npm install
npm install -D tailwindcss @tailwindcss/vite
npm install axios @tanstack/react-query react-router-dom zustand react-hook-form zod @hookform/resolvers
```

> Scaffold kéo về bản mới nhất: React 19, React Router 7, Tailwind 4, Vite 8 (đã chốt trong design.md 3.2). Tailwind v4 **không** cần `tailwind.config.js`/`postcss.config.js`.

**File sửa / tạo / xoá (theo thứ tự):**
```
SỬA  vite.config.ts              plugins: [react(), tailwindcss()]; server.port 5173 + strictPort: true;
                                 server.proxy '/api' → http://localhost:8080 (changeOrigin)
SỬA  src/index.css               chỉ còn: @import "tailwindcss";
XOÁ  src/App.css, src/assets/*, public/icons.svg      ← rác template
SỬA  index.html                  lang="vi", <title>Smart Trip Planner</title>
MỚI  .env.example                VITE_API_URL=/api/v1  (đường dẫn tương đối → đi qua proxy)
     → copy thành .env:  Copy-Item .env.example .env   (chạy lệnh này trong terminal, KHÔNG dán vào file)
MỚI  src/vite-env.d.ts           /// <reference types="vite/client" /> + interface ImportMetaEnv { VITE_API_URL?: string }
MỚI  src/types/api.ts            ApiResponse<T>, FieldError, ErrorResponse (khớp backend common/)
MỚI  src/api/client.ts           axios.create({ baseURL: VITE_API_URL || '/api/v1', timeout, withCredentials: true })
MỚI  src/api/health.ts           ping(): GET /ping → ApiResponse<string>
SỬA  src/main.tsx                bọc <App/> trong QueryClientProvider
SỬA  src/App.tsx                 useQuery(['ping'], ping) → hiện 3 trạng thái: đang tải / lỗi đỏ / "pong" xanh lá
```

**Nghiệm thu** (3 terminal: `docker compose up -d mysql redis mailhog` · `./gradlew bootRun --args='--spring.profiles.active=local'` · `npm run dev`):
1. `npm run lint` và `npm run build` trong `frontend/` không lỗi.
2. Mở `http://localhost:5173` → card trắng trên nền xám nhạt, dòng **"Backend trả về: pong"** màu xanh lá (xanh = Tailwind đã nạp).
3. F12 → Network: request `ping` có host `localhost:5173` (đi qua proxy), không phải `8080`.
4. Tắt backend, reload → dòng đỏ "Không gọi được backend". Bật lại → về xanh.
5. Dòng lệnh (PowerShell dùng `curl.exe`, vì `curl` = `Invoke-WebRequest` sẽ ném lỗi ở mã 4xx):
   ```powershell
   curl.exe -s -i http://localhost:5173/api/v1/ping            # HTTP 200 + {"success":true,"data":"pong",...}
   curl.exe -s -i http://localhost:5173/api/v1/khong-ton-tai   # HTTP 404 + errorCode RESOURCE_NOT_FOUND
   curl.exe -s -i -X POST http://localhost:5173/api/v1/ping    # HTTP 405 + errorCode METHOD_NOT_ALLOWED
   ```

> Lỗi hay gặp: `Port 5173 is already in use` → có tiến trình Vite cũ còn sống. `Get-NetTCPConnection -LocalPort 5173 -State Listen | Select-Object OwningProcess` rồi `Stop-Process -Id <PID> -Force`. Không hạ `strictPort` để né lỗi này vì CORS backend chỉ cho phép đúng `localhost:5173`.
> `bootRun` đứng ở `80% EXECUTING` là app đang chạy bình thường.
> Sửa `App.tsx` mà trang không đổi → file chưa được lưu (Ctrl+S); terminal Vite phải in `hmr update /src/App.tsx`.

**Commit** (1 mốc, `git add frontend` — kiểm tra `git status` không có `node_modules/`, `dist/`, `.env`):
```
chore(frontend): init vite react typescript project
```

> ✅ Hết Phase 0 → sau khi merge PR, trên `main`: tick ☑ 0.5 ở bảng B, `CLAUDE.md` mục 7 tick `[x] Phase 0`, cập nhật phiên bản frontend trong `design.md` 3.2 / `CLAUDE.md` / `README.md` → commit `docs: mark phase 0 complete`

---

## PHASE 1 — Xác thực (Auth)

Đọc trước: **design.md mục 6 (Security) và mục 10.2 (Auth endpoints)**

Đây là phase khó nhất với người mới. Chia thành 5 task nhỏ, **đừng làm gộp**.

### Task 1.1 — Entity User + migration + repository

Nhánh: `feat/T1.1-user-entity`

Đọc trước: **design.md mục 5.2 bảng `users`** (gồm khối "Quy ước kiểu cột") và **mục 20** (đặt tên).

**Thứ tự file:**
```
1. model/BaseEntity.java                    @MappedSuperclass: id (IDENTITY), createdAt (@CreationTimestamp), updatedAt (@UpdateTimestamp),
                                            equals/hashCode theo id
2. model/enums/Role.java, Plan.java, UserStatus.java
3. model/User.java                          @Table("users"), @SQLDelete + @SQLRestriction (soft delete), @Enumerated(STRING),
                                            @Builder + @Builder.Default cho giá trị mặc định trùng DEFAULT trong SQL,
                                            @PrePersist/@PreUpdate trim + lowercase email, isPremium() tính cả planExpiresAt
4. resources/db/migration/V2__create_users_table.sql   DATETIME(6), ENUM gốc MySQL, UNIQUE uk_users_email, KEY idx_users_plan
5. repository/UserRepository.java           Optional<User> findByEmail, boolean existsByEmail
6. test: repository/UserRepositoryTest.java @DataJpaTest + @ActiveProfiles("test") + @Import(TestcontainersConfiguration)
                                            9 test: default value, đọc enum từ cột ENUM, không tìm thấy, email lowercase,
                                            existsByEmail, UNIQUE chặn trùng, soft delete (row còn, JPA không thấy), update giữ createdAt, isPremium
7. sửa TripPlannerApplicationTests           assert Flyway đã apply "1" và "2"
```

**Luồng tư duy:** viết SQL migration **trước hay sau** entity đều được, nhưng hai bên phải khớp tuyệt đối vì `ddl-auto=validate` sẽ báo lỗi khi khởi động nếu lệch. Đây là cơ chế bảo vệ, đừng tắt nó đi.

> Package test của Boot 4 đã đổi chỗ: `@DataJpaTest` ở `org.springframework.boot.data.jpa.test.autoconfigure`, `TestEntityManager` ở `org.springframework.boot.jpa.test.autoconfigure`. `@DataJpaTest` không thay DataSource bằng H2 khi có `@ServiceConnection`, nên test chạy trên MySQL 8 thật (H2 không có kiểu ENUM).

**Nghiệm thu:**
1. `./gradlew test --tests "*UserRepositoryTest"` xanh (cần Docker). Xem chi tiết: `build/reports/tests/test/index.html`.
2. `./gradlew bootRun --args='--spring.profiles.active=local'` → log có `Successfully applied 1 migration` (lần đầu) hoặc `Schema tripplanner is up to date`, và `Started TripPlannerApplication`, không có `Schema-validation`.
3. `docker exec -it tripplanner-mysql mysql -utripuser -p tripplanner` → `SELECT version, description, success FROM flyway_schema_history;` có dòng `2 | create users table | 1`; `SHOW CREATE TABLE users\G` đúng 15 cột.

> Bẫy đã gặp: (1) collation `utf8mb4_unicode_ci` làm `WHERE email = 'UPPER@..'` vẫn khớp row lowercase → muốn kiểm tra giá trị lưu thật phải đọc thô bằng `JdbcTemplate`; (2) `JdbcTemplate.queryForObject(..., Instant.class)` với cột DATETIME ném `TypeMismatchDataAccessException`, phải đọc `LocalDateTime`; (3) backend đang chạy có DevTools sẽ tự restart khi `./gradlew build` ghi class mới và apply migration luôn vào MySQL local.

**Commit:**
```
feat(user): add user entity and migration
test(user): add user repository tests
```

---

### Task 1.2 — Đăng ký + mã hoá mật khẩu

Nhánh: `feat/T1.2-registration`

Đọc trước: **design.md mục 6.3** (danh sách trắng + entry point), **mục 14 luật 13–14** (mật khẩu, email), **mục 10.2 Auth**.

**Thứ tự file:**
```
0. build.gradle                          + spring-boot-starter-security, + spring-boot-starter-security-test (đã tạm gỡ ở Task 0.3)
1. common/validation/PasswordConfirmed + PasswordConfirmedValidator + PasswordConfirmation   class-level constraint: confirmPassword == password
   dto/request/RegisterRequest.java      @PasswordConfirmed; email (@NotBlank @Email @Size(max=255)), password (@NotBlank @Size(8..72) @Pattern hoa+thường+số, ASCII),
                                         confirmPassword (@NotBlank, không lưu), fullName (@NotBlank @Size(max=120)); message = "{validation.*}" từ messages.properties
2. dto/response/UserResponse.java        record 12 field, KHÔNG có password
3. mapper/UserMapper.java                @Mapper(unmappedTargetPolicy = ERROR) — toResponse(User)
4. exception/EmailAlreadyExistsException.java   extends AppException(EMAIL_ALREADY_EXISTS); ctor (Throwable) cho race UNIQUE
5. exception/GlobalExceptionHandler.java + handler AccessDeniedException → 403 FORBIDDEN (hoãn từ 0.4)
6. resources/messages.properties         + 8 key validation.* tiếng Việt
7. security/SecurityErrorResponses.java (helper ghi JSON) + RestAuthenticationEntryPoint (401) + RestAccessDeniedHandler (403)
                                         ← kéo từ Task 1.3 lên để lỗi ở filter cũng trả ErrorResponse. KHÔNG @Component, SecurityConfig @Import
8. config/SecurityConfig.java            @EnableWebSecurity @EnableMethodSecurity; csrf off, cors(withDefaults) dùng bean corsFilter, STATELESS,
                                         formLogin/httpBasic/logout off, entryPoint + accessDeniedHandler, PUBLIC_PATHS permitAll, anyRequest authenticated;
                                         @Bean PasswordEncoder = BCryptPasswordEncoder(12)
9. service/AuthService.java + AuthServiceImpl.java   register(): normalize email → existsByEmail → encode → saveAndFlush
                                         (catch DataIntegrityViolationException → EmailAlreadyExistsException) → mapper
10. controller/AuthController.java       POST /api/v1/auth/register, @Valid, @ResponseStatus(CREATED), trả ApiResponse<UserResponse>
11. test/service/AuthServiceTest.java    Mockito thuần: hash $2a$12$ + matches, default USER/FREE/ACTIVE, normalize email, 409, race
12. test/controller/AuthControllerTest.java   @WebMvcTest(AuthController) @Import(SecurityConfig) @MockitoBean AuthService: 201, 400 details, 400 confirm lệch, 400 thiếu confirm, 409
13. test/config/SecurityConfigTest.java  ping public; 401 envelope; URL lạ chưa login → 401; @WithMockUser 200; POST không CSRF OK;
                                         @PreAuthorize sai role → 403 envelope; đúng role → 200; RestAccessDeniedHandler ghi JSON; BCrypt 12
14. test/integration/AuthRegistrationIntegrationTest.java   @SpringBootTest + MySQL thật: 201 + hash trong DB + email lowercase; 409 + vẫn 1 row
15. sửa test cũ: HealthControllerTest, CorsConfigTest → @Import(SecurityConfig); GlobalExceptionHandlerTest → + @WithMockUser;
    TripPlannerApplicationTests → /actuator/env và URL lạ cần @WithMockUser mới ra 404, thêm test anonymous → 401
```

> **Vì sao test cũ phải sửa:** có `spring-boot-starter-security` trên classpath, `@WebMvcTest` tự bật Security với cấu hình **mặc định** (khoá hết + CSRF) nếu không `@Import(SecurityConfig)`. Và vì filter chạy trước routing, mọi URL không nằm trong PUBLIC_PATHS đều 401 khi chưa login — kể cả URL không tồn tại.

> ⚠️ Đây là lần đầu dùng MapStruct. Nếu mapper sinh ra rỗng (các field đều null), nguyên nhân gần như chắc chắn là thứ tự `annotationProcessor` trong `build.gradle` sai — xem design.md mục 3.1. Sau khi sửa, chạy `./gradlew clean build` rồi kiểm tra file sinh ra ở `build/generated/sources/annotationProcessor/java/main/`.

**Luồng đăng ký (viết ra giấy trước khi code):**
```
POST /api/v1/auth/register {email, password, fullName}
  → validate DTO (Bean Validation, tự động)
  → AuthService.register()
      → check existsByEmail → nếu có, ném EmailAlreadyExistsException (409)
      → encode password bằng BCrypt strength 12
      → lưu User với emailVerified=false, role=USER, plan=FREE
      → (Task 1.4 sẽ thêm: sinh token + gửi mail)
  → trả 201 + UserResponse (KHÔNG bao giờ trả passwordHash)
```

**Nghiệm thu** (backend chạy `local`; dùng `curl.exe`, body chỉ ASCII vì Git Bash/PowerShell 5.1 làm hỏng UTF-8 trong `-d`. **Cập nhật 2026-09-30, BUG-AUTH-005:** JSON viết thẳng trong `-d` như dưới đây bị PowerShell 5.1 làm hỏng dấu ngoặc kép → 400; ghi JSON ra file rồi `-d "@file"`, xem `docs/testing/02-auth.md`):
```powershell
curl.exe -s -i -X POST localhost:8080/api/v1/auth/register -H "Content-Type: application/json" -d "{\"email\":\"Demo@Example.com\",\"password\":\"MatKhau123\",\"confirmPassword\":\"MatKhau123\",\"fullName\":\"Demo\"}"
#   → 201, data.email = "demo@example.com" (lowercase), không có passwordHash
# gọi lại y nguyên              → 409 EMAIL_ALREADY_EXISTS "Email đã được sử dụng"
# password "matkhau123"         → 400 VALIDATION_ERROR, details[0].field = "password"
# confirmPassword khác password → 400 VALIDATION_ERROR, details[0].field = "confirmPassword", "Mật khẩu xác nhận không khớp"
curl.exe -s -i localhost:8080/api/v1/khong-ton-tai     → 401 UNAUTHORIZED (chưa login), body ErrorResponse
curl.exe -s -i localhost:8080/api/v1/ping              → 200 (public)
# Swagger http://localhost:8080/swagger-ui.html có nhóm Auth → POST /register
```
```sql
SELECT id, email, LEFT(password_hash,7), role, plan, status, email_verified FROM users;   -- $2a$12$ | USER | FREE | ACTIVE | 0
```

> Log lúc khởi động có `Using generated security password: ...` là **bình thường** cho tới Task 1.3: Boot tự tạo user in-memory khi chưa có `UserDetailsService`. formLogin/httpBasic đã tắt nên mật khẩu này không dùng được vào đâu. Task 1.3 thêm `CustomUserDetailsService` thì dòng này biến mất.
> Kiểm tra mapper MapStruct: `build/generated/sources/annotationProcessor/java/main/com/trieu/tripplanner/mapper/UserMapperImpl.java` phải có 12 dòng `user.getXxx()`; nếu chỉ `new UserResponse(null, ...)` là sai thứ tự annotationProcessor.

**Commit:**
```
feat(auth): add user registration endpoint
test(auth): add registration service and controller tests
```

---

### Task 1.3 — Đăng nhập + JWT + refresh token rotation

Nhánh: `feat/T1.3-jwt-authentication`

Đọc trước: **design.md 6.1** (toàn bộ, gồm cookie, AuthResponse, thứ tự kiểm tra login, TOKEN_EXPIRED), **5.2 `refresh_tokens`**, **10.2 Auth + User**, **10.3** (2 mã mới `INVALID_CREDENTIALS`, `ACCOUNT_BLOCKED`).

> Đã có sẵn từ Task 1.2, **không làm lại**: `RestAuthenticationEntryPoint`, `RestAccessDeniedHandler`, `SecurityConfig` (stateless, CSRF off, PUBLIC_PATHS, PasswordEncoder). Task này chỉ **thêm** vào SecurityConfig: JWT filter, `AuthenticationManager`, và làm entry point phân biệt `TOKEN_EXPIRED`.

**Thứ tự file (theo 4 mốc commit):**
```
Mốc 1 — feat(auth): add jwt token provider and security config
 1. build.gradle + libs.versions.toml         jjwt-api (implementation), jjwt-impl (runtimeOnly), version 0.12.6 ở catalog. KHÔNG jjwt-jackson
 2. common/constant/ErrorCode.java            + INVALID_CREDENTIALS (401), ACCOUNT_BLOCKED (403); messages.properties + 2 key; ErrorCodeTest
 3. config/properties/JwtProperties.java      @ConfigurationProperties("app.jwt") @Validated: secret ≥ 64, accessTtl, refreshTtl (Duration), issuer
    application.yml (app.jwt.secret: ${JWT_SECRET}, ttl mặc định) + application-test.yml (secret test-only)
 4. security/JwtJsonCodec.java                Serializer/Deserializer cho JJWT dùng tools.jackson ObjectMapper (design 3.1)
 5. security/JwtTokenProvider.java            generateAccessToken(user) với claims sub/email/role/plan/jti/iss; parse → trả record JwtClaims;
                                              phân biệt ExpiredJwtException với JwtException khác
 6. security/CustomUserDetails.java           implements UserDetails: id, email, role, plan, emailVerified, status; isEnabled/isAccountNonLocked theo status
 7. security/CustomUserDetailsService.java    loadUserByUsername(email) → UserRepository.findByEmail → UsernameNotFoundException
 8. security/JwtAuthenticationFilter.java     OncePerRequestFilter: đọc "Authorization: Bearer", parse, đặt Authentication vào SecurityContext;
                                              lỗi → request.setAttribute("jwt.error", TOKEN_EXPIRED | UNAUTHORIZED), KHÔNG ném, để filter sau trả 401
 9. security/RestAuthenticationEntryPoint.java  sửa: đọc attribute "jwt.error" để chọn ErrorCode, mặc định UNAUTHORIZED
10. config/SecurityConfig.java                + addFilterBefore(JwtAuthenticationFilter, UsernamePasswordAuthenticationFilter)
                                              + @Bean AuthenticationManager = ProviderManager(DaoAuthenticationProvider(userDetailsService, passwordEncoder))
    config/OpenApiConfig.java                 + SecurityScheme bearer để Swagger có nút Authorize
    test: JwtTokenProviderTest (round-trip claims, hết hạn, sai secret), JwtAuthenticationFilterTest/SecurityConfigTest (token hợp lệ 200,
          hết hạn → 401 TOKEN_EXPIRED, giả → 401 UNAUTHORIZED)

Mốc 2 — feat(auth): add login endpoint with refresh token
11. model/RefreshToken.java + V3__create_refresh_tokens.sql + repository/RefreshTokenRepository.java
    (findByTokenHash, revoke theo user: @Modifying UPDATE ... SET revoked_at WHERE user_id = ? AND revoked_at IS NULL)
12. dto/request/LoginRequest.java (email, password @NotBlank), dto/response/AuthResponse.java (accessToken, tokenType, expiresIn, user)
13. service/RefreshTokenService.java          issue(user, userAgent, ip) → trả token thô + lưu hash; hash SHA-256 hex; revokeAll(userId)
14. exception/InvalidCredentialsException, AccountBlockedException, EmailNotVerifiedException (extends AppException)
15. AuthService.login(LoginRequest, HttpServletRequest meta) → AuthenticationManager.authenticate → kiểm tra BLOCKED → chưa verify → phát cặp token
16. controller/AuthController: POST /login set cookie (design 6.1) + trả AuthResponse
17. service/UserService + UserServiceImpl (getCurrentUser từ SecurityContext) + controller/UserController GET /api/v1/users/me
    test: AuthServiceTest (sai mật khẩu → INVALID_CREDENTIALS, BLOCKED → ACCOUNT_BLOCKED, chưa verify → EMAIL_NOT_VERIFIED, happy path),
          UserControllerTest (không token 401, có token 200)

Mốc 3 — feat(auth): add refresh token rotation and theft detection
18. AuthService.refresh(cookie) theo "Luồng refresh" bên dưới; AuthService.logout(cookie) revoke + xoá cookie
19. AuthController: POST /refresh, POST /logout (đọc @CookieValue("refresh_token"))
    test: RefreshTokenServiceTest, AuthServiceTest refresh (revoked → revokeAll, hết hạn → 401, hợp lệ → token cũ revoked + token mới)

Mốc 4 — test(auth): add authentication integration tests
20. integration/AuthFlowIntegrationTest (@SpringBootTest + MySQL): register → bật email_verified bằng JdbcTemplate → login → /users/me 200
    → refresh OK → dùng lại cookie cũ → 401 + mọi token của user revoked → login lại được
```

> Nghiệm thu tay cần user đã verify (Task 1.4 mới có luồng verify): `UPDATE users SET email_verified = 1 WHERE email = '...';`

> **Bẫy đã gặp khi làm 1.3** (chỉ integration test trên MySQL thật mới lộ, unit test với mock đều xanh):
> 1. **`@Transactional` rollback nuốt lệnh revoke.** `refresh()` gọi `revokeAll()` rồi ném `InvalidRefreshTokenException` → Spring rollback cả transaction, token bị trộm vẫn sống. Phải `@Transactional(noRollbackFor = {InvalidRefreshTokenException.class, AccountBlockedException.class})`. Quy tắc: **ghi DB rồi cố ý ném exception = phải khai báo noRollbackFor** (hoặc tách REQUIRES_NEW).
> 2. **`expiresIn` ra 899 thay vì 900**: tính `Duration.between(Instant.now(), expiresAt)` sau khi vài ms đã trôi. Tính từ `issuedAt` lưu trong `AccessToken`, không tính từ "bây giờ".
> 3. **Nano-giây vs `DATETIME(6)`**: `Instant.now()` có nano, MySQL giữ micro và **làm tròn** → test so `Instant` sau round-trip lúc xanh lúc đỏ. Trong test `truncatedTo(ChronoUnit.MICROS)` trước khi lưu.
> 4. `getContentAsString()` của `MockHttpServletResponse` ném checked `UnsupportedEncodingException`; lấy giá trị JSON bằng `bodyJson().extractingPath(...)` khi có thể.

**Luồng login:**
```
POST /auth/login {email, password}
  → AuthenticationManager.authenticate()
      → BadCredentialsException (không có user, soft-deleted, hoặc sai mật khẩu) → 401 INVALID_CREDENTIALS
  → status BLOCKED → 403 ACCOUNT_BLOCKED
  → chưa verify email → 403 EMAIL_NOT_VERIFIED
  → sinh access token (JWT HS256, 15 phút, claims sub/email/role/plan/jti/iss)
  → sinh refresh token random 64 ký tự
      → lưu SHA-256 hash vào DB kèm expiresAt, userAgent, ip
      → set cookie refresh_token: HttpOnly, SameSite=Lax, Path=/api/v1/auth, Max-Age=7d, Secure ở prod
  → trả { accessToken, tokenType: "Bearer", expiresIn, user }
```

**Luồng refresh (phần hay bị làm sai):**
```
POST /auth/refresh (đọc cookie)
  → hash token nhận được → tìm trong DB
  → không thấy → 401
  → thấy nhưng revokedAt != null → NGHI NGỜ ĐÁNH CẮP TOKEN
        → revoke toàn bộ refresh token của user đó → 401
  → hết hạn → 401
  → hợp lệ → revoke token cũ + phát cặp token mới (rotation)
```

**Nghiệm thu** (`curl.exe`, dùng cookie jar để giữ refresh cookie giữa các lệnh):
```powershell
# 0. đăng ký (Task 1.2) rồi bật cờ verify bằng SQL ở trên
# 1. login, lưu cookie
curl.exe -s -c cookies.txt -X POST localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d "{\"email\":\"demo@example.com\",\"password\":\"MatKhau123\"}"
#    → 200, data.accessToken (dán vào jwt.io: sub, email, role, plan, exp = iat + 900), header Set-Cookie: refresh_token=...; HttpOnly; SameSite=Lax
# 2. gọi /users/me
curl.exe -s -i localhost:8080/api/v1/users/me -H "Authorization: Bearer <accessToken>"     → 200 UserResponse
curl.exe -s -i localhost:8080/api/v1/users/me                                              → 401 UNAUTHORIZED
curl.exe -s -i localhost:8080/api/v1/users/me -H "Authorization: Bearer abc"               → 401 UNAUTHORIZED
#    (token hết hạn → 401 TOKEN_EXPIRED: test tự động kiểm tra, tay thì đợi 15 phút hoặc hạ app.jwt.access-ttl=5s trong .env tạm)
# 3. refresh: lần 1 OK, lưu cookie mới sang file khác để giữ lại cookie cũ
curl.exe -s -b cookies.txt -c cookies2.txt -X POST localhost:8080/api/v1/auth/refresh      → 200, accessToken mới, Set-Cookie mới
curl.exe -s -i -b cookies.txt -X POST localhost:8080/api/v1/auth/refresh                   → 401 (token cũ đã revoke → nghi trộm)
curl.exe -s -i -b cookies2.txt -X POST localhost:8080/api/v1/auth/refresh                  → 401 (token mới cũng bị revoke theo)
# 4. login sai mật khẩu → 401 INVALID_CREDENTIALS "Email hoặc mật khẩu không đúng"
# 5. logout với cookie hợp lệ → 200, Set-Cookie refresh_token=; Max-Age=0
```
```sql
SELECT id, user_id, LEFT(token_hash,8), expires_at, revoked_at, user_agent, ip_address FROM refresh_tokens;
-- sau bước 3 mọi dòng của user đều có revoked_at
```
Log khởi động **không còn** `Using generated security password` (đã có CustomUserDetailsService). Swagger có nút Authorize → dán access token → gọi /users/me.

**Commit (4 mốc, mỗi mốc build xanh — 86 / 109 / 122 / 126 test):**
```
feat(auth): add jwt token provider and security config
feat(auth): add login endpoint with refresh token
feat(auth): add refresh token rotation and theft detection
test(auth): add authentication integration tests
```
> Cách làm đúng là **từng mốc một**: code mốc N → build → commit → mới sang mốc N+1 (CLAUDE.md mục 4). File dùng chung
> (`AuthService`, `AuthServiceImpl`, `AuthController`, `AuthServiceTest`, `AuthControllerTest`) ở mốc 2 chỉ có `login()`,
> mốc 3 mới thêm `refresh()`/`logout()`. Làm cả task một lượt rồi mới chia commit sẽ không tách được vì các file này
> chứa code của nhiều mốc.

---

### Task 1.4 — Xác thực email + gửi mail bất đồng bộ

Nhánh: `feat/T1.4-email-verification`

Đọc trước: **design.md 5.2 `verification_tokens`**, **7.1** (provider `mail`), **10.2** (4 endpoint đều Public), **10.3** (`INVALID_TOKEN`), **14 luật 12, 15–17**.

> Quyết định 2026-09-24: `resend-verification` là **Public + `{email}`** (người chưa verify không có access token nên không thể là Auth); mail đi qua **`provider/mail`** (smtp/mock) theo 7.1; token sai → **`INVALID_TOKEN` 400**.

**Thứ tự file (theo 2 mốc commit, làm từng mốc):**
```
Mốc 1 — feat(auth): add email verification flow
 1. build.gradle                          + spring-boot-starter-mail, + spring-boot-starter-thymeleaf (cả hai trong BOM, không ghi version)
 2. common/constant/ErrorCode              + INVALID_TOKEN (400); messages.properties + error.invalid-token + validation.token.required
    exception/InvalidTokenException        extends AppException(INVALID_TOKEN, reason chỉ ghi log)
 3. config/properties/AppProperties        Providers + mail (@Pattern "mock|smtp"); AppPropertiesTest thêm case
    application.yml                        app.providers.mail: mock; app.mail-from; spring.threads.virtual.enabled: true
    application-local.yml                  spring.mail.host/port: ${MAIL_HOST}/${MAIL_PORT} + app.providers.mail: smtp (MailHog)
                                           (spring.mail.* KHÔNG để ở application.yml: profile test không có biến MAIL_HOST sẽ không khởi động được)
    application-test.yml                   app.providers.mail: mock (mặc định, ghi rõ cho dễ đọc)
 4. provider/mail/MailMessage (record to, subject, htmlBody), MailProvider (interface send(MailMessage)),
    MockMailProvider (@ConditionalOnProperty mail=mock: log + List<MailMessage> sent() cho test),
    SmtpMailProvider (@ConditionalOnProperty mail=smtp: JavaMailSender, MimeMessageHelper, from = app.mail-from)
 5. config/AsyncConfig                     @EnableAsync + AsyncConfigurer.getAsyncUncaughtExceptionHandler → log lỗi gửi mail (rule 12: không nuốt)
 6. service/MailService                    @Async sendVerificationMail(User, rawToken), sendPasswordResetMail(User, rawToken):
                                           render Thymeleaf (SpringTemplateEngine) → MailProvider.send. Link = appProperties.frontendUrl() + "/verify-email?token="
    resources/templates/mail/verify-email.html, reset-password.html   (tiếng Việt, inline CSS)
 7. model/VerificationToken (+ enums/VerificationTokenType) + V4__create_verification_tokens.sql + repository/VerificationTokenRepository
    (findByTokenHash; @Modifying UPDATE ... SET used_at WHERE user_id AND type AND used_at IS NULL)
 8. service/VerificationTokenService       issue(user, type) → hash + TTL theo type + vô hiệu token cũ, trả raw; consume(raw, type) → token hợp lệ hoặc InvalidTokenException
 9. dto/request/VerifyEmailRequest {token @NotBlank}, ResendVerificationRequest {email @NotBlank @Email}
10. AuthService: verifyEmail(token), resendVerification(email); register() gọi mailService.sendVerificationMail sau saveAndFlush
11. AuthController: POST /verify-email → 200 data null; POST /resend-verification → 200 data null (luôn, rule 14.15)
    test: VerificationTokenServiceTest (hash, TTL theo loại, vô hiệu token cũ, consume sai/hết hạn/đã dùng/sai loại → INVALID_TOKEN),
          MailServiceTest (render template có link đúng, gọi provider), AuthServiceTest verify/resend (đúng → emailVerified true + used_at;
          resend cho email lạ / đã verify → không gửi, không ném), AuthControllerTest 2 endpoint, VerificationTokenRepositoryTest,
          integration: register → MockMailProvider.sent() có mail → lấy token từ link → verify → login 200 (thay bước UPDATE SQL ở 1.3)

Mốc 2 — feat(auth): add forgot and reset password flow
12. dto/request/ForgotPasswordRequest {email}, ResetPasswordRequest {token, newPassword (luật 13), confirmPassword} implements PasswordConfirmation + @PasswordConfirmed
13. AuthService: forgotPassword(email) — chỉ gửi khi user tồn tại + verified + ACTIVE (rule 14.12), luôn im lặng;
    resetPassword(request) — consume token PASSWORD_RESET → encode mật khẩu mới → refreshTokenService.revokeAll(userId)
14. AuthController: POST /forgot-password, POST /reset-password → 200 data null
    test: AuthServiceTest forgot (email lạ / chưa verify → không gửi, không ném; hợp lệ → gửi) + reset (đổi hash, revokeAll, token dùng lần 2 → INVALID_TOKEN),
          AuthControllerTest (400 confirmPassword lệch, 400 INVALID_TOKEN), integration: forgot → mail → reset → login mật khẩu mới OK, mật khẩu cũ 401, refresh cookie cũ 401
```

**Lưu ý kỹ thuật:**
- `@Async` không có tác dụng khi gọi nội bộ trong cùng class (proxy); `MailService` là bean riêng, `AuthServiceImpl` gọi qua bean. Không đặt `@Async` trong `AuthServiceImpl`.
- Test luồng có mail: `MailService` chạy thread khác → dùng Awaitility (`await().untilAsserted(...)`, có sẵn trong starter-test) để chờ `MockMailProvider.sent()`; không `Thread.sleep`.
- `spring.threads.virtual.enabled: true` làm executor mặc định của `@Async` là virtual thread (design 3.1), không cần tự tạo executor.
- Token trong link là base64url 48 byte → 64 ký tự, hash SHA-256 như refresh token; dùng lại `RefreshTokenService.hash()` hoặc tách `common/util/TokenHashes`.
- `register()` vẫn 201 dù SMTP lỗi (mail async, lỗi vào log); nghiệm thu tay khi MailHog tắt vẫn tạo được user.

> **Bẫy đã gặp khi làm 1.4:**
> 1. Unit test render template bằng `new TemplateEngine()` (Thymeleaf thuần) đỏ `NoClassDefFoundError: ognl/PropertyAccessor`: engine thuần dùng OGNL, Boot không kéo OGNL. Dùng `org.thymeleaf.spring6.SpringTemplateEngine` (SpEL) như Boot.
> 2. Lấy token từ MailHog API v2 bằng `grep` trên JSON thất bại vì body HTML chứa `\"` và mã hoá quoted-printable (`=3D`, ngắt dòng `=\r\n`). Nghiệm thu tay: mở UI `localhost:8025` và bấm link, hoặc decode bằng Node (`Content.Body.replace(/=\r?\n/g,'').replace(/=([0-9A-F]{2})/g, ...)`).
> 3. `SecureTokens` gom sinh/hash token cho cả refresh và verification; `RefreshTokenService.hash()` chỉ còn uỷ quyền để test cũ không đổi.

**Nghiệm thu** (`docker compose up -d mysql redis mailhog`, backend profile `local`):
1. `POST /auth/register` → 201 → mở `http://localhost:8025` thấy mail "Xác thực email", link `http://localhost:5173/verify-email?token=...`.
2. Copy token → `curl.exe -s -i -X POST localhost:8080/api/v1/auth/verify-email -H "Content-Type: application/json" -d "{\"token\":\"...\"}"` → 200; gọi lại → 400 `INVALID_TOKEN`; MySQL `email_verified = 1`, `verification_tokens.used_at` có giá trị.
3. Login → 200 (không còn phải UPDATE SQL tay).
4. `POST /auth/resend-verification {"email":"khong-ton-tai@x.com"}` → 200 cùng message, MailHog không có mail mới; với email thật chưa verify → có mail mới, token cũ 400.
5. `POST /auth/forgot-password` → mail reset → `POST /auth/reset-password {token, newPassword, confirmPassword}` → 200 → login mật khẩu mới OK, mật khẩu cũ 401, cookie refresh cũ → 401.

**Commit (2 mốc):**
```
feat(auth): add email verification flow
feat(auth): add forgot and reset password flow
```

---

### Task 1.5 — Màn hình Auth phía frontend

Nhánh: `feat/T1.5-auth-ui`

**Thứ tự file:**
```
1. src/types/auth.ts
2. src/api/authApi.ts
3. src/stores/authStore.ts            zustand: accessToken trong memory, user info
4. src/api/client.ts                  thêm interceptor: 401 → gọi refresh → retry request cũ
5. src/features/auth/LoginForm.tsx, RegisterForm.tsx   (react-hook-form + zod)
6. src/pages/LoginPage.tsx, RegisterPage.tsx, VerifyEmailPage.tsx, ForgotPasswordPage.tsx
7. src/components/ProtectedRoute.tsx
8. src/App.tsx                        cấu hình router
```

> Kế thừa từ 1.3, interceptor ở `client.ts` phải:
> 1. Chỉ gọi `/auth/refresh` khi lỗi là **401 `TOKEN_EXPIRED`**; 401 `UNAUTHORIZED`/`INVALID_CREDENTIALS` → về login, không refresh.
> 2. **Single-flight**: nhiều request cùng nhận TOKEN_EXPIRED (mở nhiều tab, F5 nhiều lần) chỉ được gọi refresh **một** lần và xếp hàng chờ. Backend xoay token mỗi lần refresh; hai lần refresh song song bằng cùng cookie → lần sau bị coi là dùng lại token đã revoke → **mọi phiên bị thu hồi** và user bị đăng xuất khỏi tất cả tab.
> 3. Logout: `POST /auth/logout` cần access token còn hạn; nếu đã hết hạn thì refresh trước rồi logout, hoặc nếu refresh cũng 401 thì chỉ xoá state local (server đã không còn phiên).

**Nghiệm thu:** đăng ký → verify → login → vào được `/trips` (trang rỗng); F5 vẫn giữ đăng nhập nhờ refresh cookie; logout xoá sạch.

> **Thực tế khi làm 1.5 (lệch so với danh sách file ở trên):**
> - `src/api/auth.ts` thay cho `authApi.ts` (theo quy ước tên ở design.md 3.2); thêm `src/api/errors.ts` (đọc `ErrorResponse`, gắn `details` vào field của form).
> - Thêm `ResetPasswordPage` (mail của 1.4 trỏ tới `/reset-password`), `GuestRoute` (login xong tự chuyển về trang định vào, hoặc `/trips`), `TripsPage` tạm (Task 2.5 thay), component chung `FormField`/`Button`/`Alert`/`FullPageSpinner`, và `features/auth/schemas.ts`, `useLogout.ts`, `ResendVerificationForm.tsx`.
> - Khôi phục phiên khi F5: `main.tsx` gọi `refreshAccessToken()` **một lần ngoài React** (StrictMode chạy effect 2 lần → sẽ xoay token 2 lần).
> - Single-flight giữa các tab dùng Web Locks (`navigator.locks`): các tab dùng chung cookie, tab giữ khoá xoay xong thì tab sau gửi cookie mới.
> - Chưa cài shadcn/ui, giao diện hiện là Tailwind thuần.
>
> **Bẫy đã gặp khi làm 1.5:**
> 1. Chạy backend bằng IntelliJ báo `Could not resolve placeholder 'MAIL_HOST'`: working directory mặc định là gốc repo nên `../.env` trỏ sai chỗ. Đặt Working directory = `backend/` (xem CLAUDE.md mục 2).
> 2. Trang `/verify-email` gửi token dùng một lần: gọi bằng `useMutation` trong `useEffect` sẽ bị StrictMode gửi 2 lần → lần 2 trả `INVALID_TOKEN` dù đã verify thành công. Dùng `useQuery` (cache dedupe). F5 trang verify sau khi thành công sẽ báo lỗi — đúng, token đã dùng.
> 3. `resend-verification` / `forgot-password` luôn báo thành công kể cả với tài khoản đã verify hoặc email không tồn tại, MailHog không có mail mới — đúng rule 14.15 (chống dò email), không phải lỗi.
> 4. Không thấy bảng mới trong Database tool của IntelliJ: phải Refresh (`Ctrl+F5`) hoặc tick schema `tripplanner` ở Properties → Schemas.
> 5. Các tab dùng chung một phiên (cookie): logout ở tab này thì tab kia reload cũng về login; login ở tab này thì tab kia reload cũng vào luôn — đúng. Nếu **không** reload, tab kia vẫn giữ access token trong RAM tới khi hết hạn (≤ 15 phút) — chấp nhận theo design.md 6.1, đồng bộ tức thì để ở Task 8.3.

**Commit:**
```
feat(frontend): add auth pages and token refresh interceptor
```

> ✅ Hết Phase 1 → tick `[x] Phase 1` trong CLAUDE.md.
> **Lúc này hãy viết README lần đầu**: mô tả dự án, cách chạy. Ảnh chụp màn hình để dành tới Task 8.5 (quyết định 2026-09-25), chưa thêm ảnh ở phase này.

---

## PHASE 2 — Trip & Itinerary

Đọc trước: **design.md mục 5.2 (trips, trip_days, activities), mục 6.2 (khối "Triển khai theo giai đoạn"), mục 10.2 (Trip, Itinerary), mục 14 (business rules 1–5)**

> **Quyết định chung cho Phase 2 (2026-09-26):**
> - Mọi endpoint trip/day/activity dùng `@PreAuthorize("@tripPermission....")` ngay từ Phase 2. Bean `tripPermission` tạo ở Task 2.1, lúc này chỉ kiểm owner; Task 4.2 mở rộng.
> - Trip không tồn tại / đã xoá → **404**; tồn tại nhưng không có quyền → **403**.
>
> **Việc còn treo — đã chốt 2026-09-30:** `PATCH /trips/{id}/status` làm ở Task 2.5 (Mốc 5). `POST /trips/{id}/clone` gán Task 6.1 (cùng quota). `GET /trips/{id}/summary` gán Task 7.1 (cần tổng chi phí; quãng đường cần route của Phase 3). Trang chi tiết ở 2.5 tự đếm số ngày và số activity nên chưa cần `summary`.

### Task 2.1 — Trip CRUD

Nhánh: `feat/T2.1-trip-crud`

```
Mốc 1 — entity + migration
1. model/enums/TripStatus, TripVisibility
2. model/Trip.java                     @Version, soft delete (@SQLDelete + @SQLRestriction), owner LAZY
3. V5__create_trips_table.sql          có cột version BIGINT NOT NULL DEFAULT 0 (design.md 5.2)

Mốc 2 — CRUD endpoints (kèm test controller)
4. TripRepository (JpaSpecificationExecutor) + TripSpecifications: status, q (title/destination), from, to
5. common/SlugGenerator                bỏ dấu tiếng Việt → kebab-case + 6 ký tự ngẫu nhiên, sinh 1 lần khi tạo
6. exception/BusinessRuleException     thêm details theo field; GlobalExceptionHandler trả details
7. security/permission/TripPermissionEvaluator   bean "tripPermission": canView/canEdit/isOwner — chỉ kiểm owner,
                                       trip không tồn tại → true (để service trả 404)
8. dto/request/CreateTripRequest, UpdateTripRequest
9. dto/response/TripResponse, TripSummaryResponse (+ PageResponse nếu chưa có)
10. mapper/TripMapper
11. service/TripService + Impl
12. controller/TripController          GET /trips, POST /trips, GET/PATCH /trips/{id} (canView/canEdit), DELETE (isOwner)
13. messages.properties                message lỗi mới
14. test: @WebMvcTest mỗi endpoint happy + 401/403 + validate; unit test SlugGenerator, TripPermissionEvaluator

Mốc 3 — test service + integration
15. TripServiceTest: end < start, > 60 ngày → 400 details endDate; đổi title giữ nguyên slug; soft delete
16. Integration (Testcontainers): list phân trang + filter chỉ trả trip của chính user; user khác → 403; trip đã xoá → 404
```

**Nhớ:** `endDate >= startDate`, tối đa 60 ngày tính cả hai đầu → validate ở service, ném `BusinessRuleException(VALIDATION_ERROR)` với `details` ở field `endDate`. Danh sách trip chỉ lấy `deletedAt IS NULL` và của chính user. `slug` không đổi khi sửa title. Quota khi tạo trip để Phase 6. Quy ước PATCH từng phần, mặc định status/visibility, bộ lọc, phân trang, validate: design.md 10.2 khối "Quy ước Trip API".

**Commit:**
```
feat(trip): add trip entity and migration
feat(trip): add trip crud endpoints
test(trip): add trip service and integration tests
```

> **Thực tế khi làm 2.1 (2026-09-26):**
> - Chốt thêm khi code (đã ghi vào design.md 5.2 và 10.2 "Quy ước Trip API"): chỉ cho `sort` theo `createdAt`, `updatedAt`, `startDate`, `title` (cột khác → 400, chặn 500 và dò dữ liệu qua thứ tự); `description` ≤ 5000 ký tự; `title` được trim; `destination_name` nullable; `CHECK (end_date >= start_date)` ở DB; FK `owner_id` không `ON DELETE CASCADE`.
> - `AppException` mang được `details` (`FieldViolation` = field + message key + args) → lỗi nghiệp vụ trả `details` giống lỗi Bean Validation. Dùng `BusinessRuleException.invalidField(...)`.
> - Rule nhiều field (ngày, cặp toạ độ) kiểm ở service trên dữ liệu **đã gộp**, vì PATCH chỉ gửi `endDate` vẫn phải so với `startDate` đang lưu.
>
> **Bẫy đã gặp khi làm 2.1:**
> 1. Entity có `@Version` + `@SQLDelete`: Hibernate bind **cả id lẫn version** vào câu SQL xoá → phải viết `WHERE id = ? AND version = ?`; chỉ `WHERE id = ?` như bảng `users` sẽ lỗi.
> 2. `TEXT` cần `@Column(columnDefinition = "TEXT")`, `CHAR(3)` cần `@JdbcTypeCode(SqlTypes.CHAR)`, nếu không `ddl-auto=validate` báo lệch kiểu.
> 3. Kiểm slug trùng phải dùng **native query**: trip đã soft delete vẫn giữ slug trong UNIQUE key nhưng JPQL bị `@SQLRestriction` che.
> 4. Spring Data 4: `JpaSpecificationExecutor` có thêm `delete(Specification)` → `verify(repo).delete(any())` báo "reference to delete is ambiguous". Dùng `any(Trip.class)`.
> 5. `@WebMvcTest` không nạp bean `tripPermission` → mock bằng `@MockitoBean(name = "tripPermission")`.

---

### Task 2.2 — TripDay tự sinh

Nhánh: `feat/T2.2-trip-days`

Mỗi mốc là một commit, code + test cùng commit (quy ước A.2 "Chia commit"). Bảng file chi tiết Claude đưa ra đầu task để duyệt.

```
Mốc 1 — feat(trip): add trip day entity and migration
        model/TripDay.java, V6__create_trip_days.sql (UNIQUE (trip_id, date)), test mapping

Mốc 2 — feat(trip): generate trip days when a trip is created
        TripDayRepository, service/TripDayService.generateDays, TripServiceImpl.create gọi trong cùng transaction
        test: tạo trip 3 ngày → 3 TripDay đúng date + day_index

Mốc 3 — feat(trip): add trip day list endpoint
        GET /trips/{id}/days (canView), TripDayResponse, mapper, controller, test

Mốc 4 — feat(trip): add trip day title and note update
        PATCH /trips/{id}/days/{dayId} (canEdit), UpdateTripDayRequest: null = giữ nguyên, "" = xoá;
        day không thuộc trip → 404, test

Mốc 5 — feat(trip): reconcile trip days when trip dates change
        TripDayService.reconcileDays, TripServiceImpl.update gọi khi đổi startDate/endDate (design.md rule 14.3):
        cùng số ngày → dời nguyên khối (UPDATE hàng loạt có ORDER BY, tránh trùng UNIQUE (trip_id, date));
        khác số ngày → giữ theo ngày lịch: thêm ngày mới, xoá ngày bị cắt, đánh lại day_index;
        test trên MySQL thật: dời +1 / −1 ngày (không lỗi UNIQUE, title giữ nguyên), kéo dài / cắt ở đầu / cắt ở cuối

Mốc 6 — feat(trip): include days in trip detail
        GET /trips/{id} trả TripDetailResponse = TripResponse + days (POST/PATCH vẫn trả TripResponse);
        JOIN FETCH / @EntityGraph, kiểm số câu SQL — tránh N+1, test

Mốc 7 — test(trip): add trip day flow integration test
```

> ⚠️ **Chặn cắt ngày có activity (rule 14.3, `force=true`) chuyển sang Task 2.3** (quyết định 2026-09-26): ở 2.2 chưa có bảng `activities` nên chưa có gì để kiểm. Ở 2.2, `reconcileDays` xoá ngày bị cắt tự do.

> **Thực tế khi làm 2.2 (2026-09-28):** 7 commit theo lát cắt dọc đúng bảng đã duyệt, thêm `TripDayRepositoryTest` (truy vấn mới cần MySQL thật để chứng minh).
> - `TripDayService` (class, không interface): `generateDays` và `reconcileDays` dùng `Propagation.MANDATORY` — bắt buộc chạy trong transaction của `TripServiceImpl.create/update`, trip và ngày luôn commit / rollback cùng nhau.
> - `TripServiceImpl.update` chỉ gọi reconcile khi `startDate`/`endDate` thật sự đổi; gửi lại đúng ngày cũ thì không làm gì.
> - `GET /trips/{id}` = 3 câu SQL bất kể số ngày (quyền + trip + ngày), đo bằng Hibernate Statistics trong `TripDayFlowIntegrationTest`. Đọc `ownerId` qua proxy LAZY không nạp bảng `users`.
> - `TripDayRepository.findByIdAndTripId` + `requireLiveTrip` (`existsById`): chặn sửa ngày của trip khác qua URL sai và ngày của trip đã xoá mềm (`findByIdAndTripId` chỉ so FK, không bị `@SQLRestriction` của `Trip` lọc).
> - **Việc cho Task 2.3:** `deleteOutsideRange` là JPQL bulk delete trên `trip_days`. Khi có bảng `activities` (FK → `trip_days`), mốc "chặn cắt ngày có activity" phải quyết định xoá activity theo ngày (khi `force=true`) trước khi xoá ngày, nếu không MySQL chặn vì FK.
>
> **Bẫy đã gặp khi làm 2.2:**
> 1. MySQL kiểm `UNIQUE` **sau từng dòng**, không cuối câu lệnh: dời mọi ngày +1 bằng `UPDATE` tăng dần → `Duplicate entry ... uk_trip_days_trip_date`. Dùng `UPDATE ... ORDER BY date DESC` khi dời về sau, `ASC` khi dời về trước (native SQL, JPQL không có `UPDATE ... ORDER BY` / `DATE_ADD`). Đã kiểm chứng: đảo chiều `ORDER BY` thì test đỏ.
> 2. `remove(trip)` khi các `TripDay` của nó **đang được Hibernate quản lý** trong cùng persistence context → flush lỗi `TransientPropertyValueException`, dù `@SQLDelete` chỉ ghi `deleted_at`. Code thật an toàn vì `delete` chỉ nạp trip; test phải `clear()` rồi nạp lại trip.
> 3. MapStruct `uses = OtherMapper.class` mặc định sinh `@Autowired` **trên field** → vi phạm CLAUDE.md rule 4 và `Mappers.getMapper(...)` trong unit test có field `null` (NPE). Dùng `injectionStrategy = InjectionStrategy.CONSTRUCTOR`; test tạo bằng `new TripMapperImpl(new TripDayMapperImpl())`.
> 4. JsonPath trả `Integer` cho số nhỏ: `List<Long> ids = JsonPath.read(...)` → `ClassCastException` lúc dùng. Đọc qua `List<Number>` rồi `longValue()`.
> 5. `@DataJpaTest` không quét `@Component` của MapStruct → test service thật cần `@Import({TripDayService.class, TripDayMapperImpl.class})`.

---

### Task 2.3 — Activity CRUD + kiểm tra trùng giờ

Nhánh: `feat/T2.3-activity-crud`

```
Mốc 1 — feat(activity): add activity entity and migration
        model/enums/ActivityType, model/Activity.java (@Version), V7__create_activities.sql, test mapping

Mốc 2 — feat(activity): add create activity endpoint
        ActivityRepository, CreateActivityRequest, ActivityResponse, ActivityMapper, ActivityService + Impl.create
        (orderIndex = cuối ngày + 1000), POST /trips/{tripId}/days/{dayId}/activities (canEdit), test

Mốc 3 — feat(activity): reject overlapping activity times
        truy vấn tìm activity chồng giờ, validateNoTimeConflict trong create, allowOverlap=true bỏ qua;
        test: 2 activity trùng giờ → 409 ACTIVITY_TIME_CONFLICT, allowOverlap=true → 201

Mốc 4 — feat(activity): add activity list endpoint
        GET /trips/{tripId}/days/{dayId}/activities (canView), sắp theo orderIndex, test

Mốc 5 — feat(activity): add activity update endpoint
        PATCH /trips/{tripId}/activities/{activityId} (canEdit), dùng lại validateNoTimeConflict, test

Mốc 6 — feat(activity): add activity delete endpoint
        DELETE /trips/{tripId}/activities/{activityId} (canEdit), test

Mốc 7 — feat(trip): block date changes that drop days with activities
        (chuyển từ 2.2) reconcileDays: ngày bị cắt có activity → 409 TRIP_DAY_HAS_ACTIVITIES trừ khi
        PATCH /trips/{id}?force=true; test QUAN TRỌNG

Mốc 8 — feat(trip): include activities in trip detail
        GET /trips/{id} trả days + activities (TripDayDetailResponse), kiểm số câu SQL = 4 (không N+1), test

Mốc 9 — test(activity): add activity flow integration test
```

> **Quyết định khi duyệt bảng commit 2.3 (2026-09-29)** — chi tiết ở design.md 5.2 `activities`, 10.2 "Quy ước Activity API", 10.3, rule 14.3 và 14.4:
> - Cắt ngày có activity → mã lỗi mới **409 `TRIP_DAY_HAS_ACTIVITIES`**; `force` là query param của `PATCH /trips/{id}`.
> - `allowOverlap` là query param của `POST` / `PATCH` activity. Chỉ so trùng giữa activity có đủ giờ bắt đầu và kết thúc; chạm đầu nhau không tính là trùng.
> - V7 **không có cột `place_id`** (bảng `places` chưa tồn tại); Task 3.2 thêm cột + FK bằng migration riêng.
> - `PATCH` activity: `null` giữ nguyên, `""` xoá field văn bản (`note`, `bookingUrl`); chưa xoá trắng được giờ và chi phí.
> - Activity xoá cứng; FK `trip_day_id` `ON DELETE CASCADE` nên `deleteOutsideRange` của Task 2.2 không phải sửa câu lệnh, chỉ thêm bước kiểm tra trước khi xoá.
> - Quota 10 activity / ngày hoãn sang Task 6.1. `version` có trong response, kiểm `STALE_VERSION` ở Task 5.3.
>
> **Thực tế khi làm 2.3 (2026-09-29):** 9 commit theo lát cắt dọc đúng thứ tự bảng đã duyệt, thêm commit docs Mốc 0 trên `main`. 113 method test mới, toàn dự án 383 method / 411 lượt chạy.
> - Endpoint mới: `POST` / `GET /trips/{tripId}/days/{dayId}/activities`, `PATCH` / `DELETE /trips/{tripId}/activities/{activityId}`. Endpoint đổi: `PATCH /trips/{id}?force=`, `GET /trips/{id}` kèm `activities`.
> - `GET /trips/{id}` = 4 câu SQL, `GET .../activities` = 4 câu SQL, bất kể số ngày và số activity (đo bằng Hibernate Statistics trong `TripDayFlowIntegrationTest`, `ActivityFlowIntegrationTest`).
> - Từ task này có `docs/testing/`: test case ghi theo từng mốc, test đỏ ngoài dự kiến ghi **trước khi sửa** (CLAUDE.md mục 4 bước 9). Task 2.3 ghi 111 kịch bản tự động, 9 test thủ công, 3 lỗi.
> - **Việc cho Task 2.4:** `Activity.tripDay` và `orderIndex` chưa có setter (cố ý, chưa ai dùng); reorder thêm vào. Chuyển activity sang ngày khác phải quyết định có kiểm trùng giờ ở ngày mới hay không.
> - **Việc cho Task 2.5:** giao diện bắt `ACTIVITY_TIME_CONFLICT` → hỏi lại → `allowOverlap=true`; bắt `TRIP_DAY_HAS_ACTIVITIES` → hiện câu trong `details[0].message` → `force=true`.
>
> **Bẫy đã gặp khi làm 2.3** (ghi dần theo mốc; chi tiết ở `docs/testing/05-activity.md` mục "Lỗi đã phát hiện"):
> 1. **`LocalTime` + `hibernate.jdbc.time_zone=UTC`** (Mốc 1, BUG-ACT-001): mapping mặc định đi qua `java.sql.Time` nên giờ bị dịch theo múi giờ JVM (03:00 ở máy +07:00 lưu thành 20:00, vi phạm CHECK). Field `LocalTime` phải có `@JdbcType(LocalTimeJdbcType.class)`. Kiểm bằng cách đọc thô `CAST(col AS CHAR)`, vì đọc lại qua JPA vẫn ra đúng.
> 2. **Tham số `LocalTime` trong `@Query`** (Mốc 3, BUG-ACT-002): `@JdbcType` trên field **không** áp dụng cho tham số truy vấn; tham số vẫn bind kiểu `TIME` mặc định và bị dịch múi giờ, `start_time < :end` trả rỗng mà không báo lỗi. Không so giờ trong SQL: lấy ứng viên bằng `findTimedByTripDayId` rồi so trong Java (`ActivityServiceImpl.overlaps`). Xem log bind bằng `logging.level.org.hibernate.orm.jdbc.bind=trace`.
> 3. **`PATCH` đổi entity trước khi validate** là bẫy tiềm ẩn (Mốc 5, tránh được từ đầu): câu truy vấn kiểm trùng giờ làm Hibernate auto-flush, entity đang dở sẽ bị ghi xuống rồi mới rollback. `ActivityServiceImpl.update` tính giá trị đã gộp vào biến cục bộ, validate xong mới gán vào entity.
> 4. **`ON DELETE CASCADE` xoá không để lại dấu vết** (Mốc 7): `deleteOutsideRange` là bulk delete, MySQL tự xoá activity theo FK nên ứng dụng không hề biết đã mất gì. Phải **đếm trước khi xoá** (`countInDaysOutsideRange`). Đã kiểm chứng ngược: gỡ bước đếm thì 3 test của `TripDayReconcileTest` đỏ.
> 5. **Mốc 8 lệch kế hoạch:** việc ghép ngày với activity nằm ở `TripDayService.listWithActivities`, không nằm ở `TripServiceImpl` như bảng commit dự kiến. `TripServiceImpl` nhờ vậy bỏ được `TripDayRepository`; nếu làm theo kế hoạch nó sẽ có 9 dependency. `TripMapper` và `TripDayMapper` nhận DTO đã chuyển sẵn nên không cần `uses`, `TripMapperImpl` trở lại constructor không tham số.
> 6. **Test tích hợp dài dễ sai kịch bản** (Mốc 9, BUG-ACT-003): một bước mong đợi 200 nhưng nhận 409 vì bước trước đó đã thêm một activity chồng giờ bằng `allowOverlap=true`. Hệ thống đúng, test sai. Khi test đỏ, đọc log `Application error ...` của `GlobalExceptionHandler` trước khi nghi code.
> 7. **Mốc 3 lệch kế hoạch:** kế hoạch ghi "truy vấn tìm activity chồng giờ"; thực tế là truy vấn lấy ứng viên + so trong Java, vì bẫy số 2.

---

### Task 2.4 — Sắp xếp lại thứ tự (reorder)

Nhánh: `feat/T2.4-activity-reorder`

Mỗi mốc là một commit, code + test cùng commit (quy ước A.2 "Chia commit"). Sau mỗi mốc cập nhật `docs/testing/05-activity.md` (phần J trở đi, mã từ `TC-ACT-112`).

```
Mốc 1 — feat(activity): add batch reorder endpoint
        ReorderActivitiesRequest { items: [{activityId, dayId, orderIndex}] }, Activity.moveTo +
        @OptimisticLock(excluded = true) cho tripDay / orderIndex, ActivityService.reorder() 1 transaction,
        mọi activity/day phải thuộc trip trên URL, PUT /trips/{tripId}/activities/reorder (canEdit),
        response = các ngày bị ảnh hưởng kèm activities;
        test: đổi thứ tự trong ngày, kéo activity từ ngày 1 sang ngày 2, lô sai thì không lưu gì, version không tăng

Mốc 2 — feat(activity): reject time conflicts when an activity moves to another day
        chuyển ngày → kiểm trùng giờ ở ngày đích (dùng lại overlaps), ?allowOverlap=true bỏ qua,
        cùng ngày không kiểm; test: 409 ACTIVITY_TIME_CONFLICT, allowOverlap → 200

Mốc 3 — feat(activity): normalize order index when the gap is too small
        sau khi áp dụng lô: khoảng cách < 10 (tính cả từ 0 tới activity đầu) → đánh lại cả ngày 1000, 2000, 3000...;
        test biên: khoảng 10 giữ nguyên, khoảng 9 đánh lại

Mốc 4 — test(activity): add reorder flow integration test
```

**Cơ chế orderIndex:** đánh số cách nhau 1000. Chèn giữa hai activity 1000 và 2000 → index 1500, không phải update cả danh sách. Khi khoảng cách < 10 thì normalize lại cả ngày.

> **Thực tế khi làm 2.4 (2026-09-30):** 4 commit đúng bảng đã duyệt, thêm commit docs Mốc 0 trên `main`. 51 method test mới (toàn dự án 434 method / 462 lượt chạy), không test nào đỏ ngoài dự kiến. Kiểm chứng ngược 2 lần (gỡ `@OptimisticLock(excluded)` → 2 test đỏ; tắt kiểm trùng giờ → 3 test đỏ).
> - Endpoint mới: `PUT /trips/{tripId}/activities/reorder?allowOverlap=`, body `{ items: [...] }`, trả các ngày bị ảnh hưởng kèm activities. 6 câu SQL cho một lần kéo thả, không phụ thuộc số activity trong ngày (`ActivityReorderFlowIntegrationTest`).
> - `Activity.moveTo(day, index)` là cách duy nhất đổi vị trí; `tripDay` và `orderIndex` không có setter.
> - Normalize chỉ đo những ngày **nhận** activity; ngày chỉ mất activity không bị đánh lại. Khoảng cách tính cả từ 0.
> - **Việc cho Task 2.5:** giao diện gọi reorder với đúng những activity bị kéo; thay các ngày trong state bằng `data` trả về (đã có orderIndex normalize); 409 `ACTIVITY_TIME_CONFLICT` với `details[i].field = items[<i>].dayId` → hỏi lại → `allowOverlap=true`.
> - **Việc cho Task 5.3:** `version` không đổi khi reorder là cố ý (design.md 11.3); realtime chỉ cần phát `ACTIVITY_REORDERED` với các ngày trả về.
>
> **Bẫy đã gặp khi làm 2.4:** không có bẫy mới. Hai bẫy múi giờ của 2.3 vẫn áp dụng: `findTimedByTripDayIdIn` chỉ đọc giờ, so sánh trong Java.
>
> **Quyết định khi duyệt bảng commit 2.4 (2026-09-29)** — chi tiết ở design.md 10.2 "Quy ước Reorder", 11.3, rule 14.5:
> - Body là `{ "items": [...] }`, không phải mảng trần như bản design cũ.
> - Chuyển activity sang ngày khác có kiểm trùng giờ ở ngày đích; `allowOverlap` là query param. Tách thành mốc riêng (Mốc 2).
> - Reorder và normalize **không tăng `version`** của activity: `version` chỉ bảo vệ nội dung.
> - Tách 2 mốc ban đầu thành 4: thêm mốc kiểm trùng giờ và mốc integration test.


---

### Task 2.5 — Giao diện lịch trình

Nhánh: `feat/T2.5-itinerary-ui`

Frontend chưa có test runner (Vitest thêm ở task frontend sau) → điều kiện mỗi commit là `npm run lint` + `npm run build` xanh. Tên file API theo design.md 3.2: `src/api/trips.ts`, `src/api/activities.ts`.

```
Mốc 1 — feat(frontend): add trip list page
        types/trip.ts, api/trips.ts, features/trips/TripList.tsx, TripCard.tsx, pages/TripsPage.tsx (thay trang tạm),
        filter status / q, phân trang

Mốc 2 — feat(frontend): add create trip wizard
        features/trips/CreateTripWizard.tsx, route /trips/new
        (điểm đến nhập tên; map picker thêm ở Task 3.4 khi có Leaflet)

Mốc 3 — feat(frontend): add trip detail page with day timeline
        pages/TripDetailPage.tsx, features/itinerary/DayTimeline.tsx, sửa title/note của ngày

Mốc 4 — feat(frontend): add trip edit and delete
        form sửa thông tin trip (PATCH), xoá trip có xác nhận; đổi ngày làm mất activity → hỏi lại rồi gửi force=true;
        đổi cả ngày đi lẫn số ngày → hướng dẫn làm 2 bước: dời chuyến trước, đổi độ dài sau (design.md rule 14.3)

Mốc 5 — feat(frontend): add activity create, edit and delete
        types/activity.ts, api/activities.ts, ActivityCard.tsx, ActivityFormDialog.tsx;
        409 ACTIVITY_TIME_CONFLICT → hỏi "vẫn thêm?" → gửi allowOverlap=true

Mốc 6 — feat(frontend): add drag and drop activity reorder
        dependency dnd-kit (package.json trong commit này), features/itinerary/DragDropContainer.tsx, gọi API reorder
```

> **Thực tế khi làm 2.5 (2026-09-30):** PR #14, merge commit `eb1ad5e`. 10 commit thay vì 6 mốc dự kiến:
>
> | Commit | Nội dung |
> |---|---|
> | `9fd9635` feat(frontend): add trip list page | Danh sách, lọc trạng thái / tìm kiếm / phân trang lưu trên URL, `AppLayout` |
> | `db7be12` feat(frontend): add create trip wizard | Wizard 3 bước, `SelectField`, `TextAreaField` |
> | `e81b0c3` feat(frontend): add trip detail page with day timeline | Trang chi tiết, sửa ngày; `types/activity.ts` + `ActivityCard` chuyển lên mốc này (trang phải hiện activity ngay) |
> | `b017310` feat(frontend): add trip edit and delete | `Modal` (`<dialog>` gốc), `ConfirmDialog`, `force=true`, vừa dời vừa đổi độ dài → hỏi lại; gộp sửa BUG-UI-001, nút `ghost`, "Chưa có tiêu đề" |
> | `98d0d6f` feat(trip): add trip status endpoint | **Mốc mới** (backend + ô chọn trạng thái), giải quyết "Việc còn treo" |
> | `e5d877b` refactor(frontend): share money, url and currency validation | `lib/validation.ts` dùng chung cho trip và activity |
> | `f589842` feat(frontend): add activity create, edit and delete | `allowOverlap=true` sau khi hỏi, `ghost-danger` |
> | `b14516d` feat(frontend): add drag and drop activity reorder | dnd-kit core 6.3 / sortable 10.0, `lib/orderIndex.ts` |
> | `007adb7` feat(trip): shorten note and description limits | Góp ý của chủ dự án sau Mốc 7: ghi chú 255, mô tả 1000 (chỉ DTO, cột giữ `TEXT`) |
> | `35b95a1` fix(frontend): collapse long notes and keep trip header in place | `ExpandableText` ("Đọc thêm" khi quá 2 dòng), cụm trạng thái + nút cố định |
>
> Quyết định khi làm (chi tiết ở design.md 3.2, 5.2, 10.2, 15):
> - **Không dùng shadcn/ui** tới cuối dự án: component dùng chung tự viết trong `src/components/`.
> - Trang chi tiết **2 cột** (mục lục ngày + mọi ngày xếp dọc); cột bản đồ để Phase 3.
> - Rule 14.3 "vừa dời vừa đổi độ dài": giao diện **hỏi lại, không chặn** — chặn thì không bỏ được ngày đầu của chuyến đi (dời rồi rút ngắn sẽ xoá ngày cuối).
> - PATCH chưa xoá trắng được field tuỳ chọn → form **báo lỗi ngay ở ô** ("Chưa hỗ trợ xoá thông tin này…") thay vì âm thầm giữ giá trị cũ.
> - Form sửa chỉ gửi field đã đổi. Kéo thả chỉ gửi activity bị kéo, `orderIndex` = trung điểm (đầu ngày: nửa activity đầu; cuối ngày: +1000); không còn số nguyên trống thì gửi cả ngày 1000, 2000...
> - `PATCH /trips/{id}/status`: chuyển tự do, gửi lại trạng thái cũ → 200 không tăng version.
> - 479 lượt test backend (thêm 17: 9 cho status, 8 cho giới hạn độ dài). Frontend kiểm bằng 33 bài `MT-UI` trong `docs/testing/06-itinerary-ui.md`.
>
> **Bẫy đã gặp khi làm 2.5:**
> 1. **BUG-UI-001 — ghi đè lớp Tailwind bằng `className`:** `Button` có sẵn `w-full`, truyền thêm `className="w-auto"` thì không chắc lớp nào thắng (ở đây `w-full` thắng). Nút có `shrink-0` chiếm hết hàng, tiêu đề ngày bị bẻ mỗi chữ một dòng. Sửa: độ rộng / lề / kiểu nút thành prop (`fullWidth`, `size`, `variant`). `lint` và `build` không bắt được loại lỗi này, chỉ người nhìn giao diện mới thấy. Suýt lặp lại ở Mốc 6 (`hover:text-red-700` đè `ghost`) → thêm `variant="ghost-danger"`.
> 2. **Kiểu xuống dòng:** `sed -i` trên `*.java` ghi lại cả file không đổi nội dung với LF, `git status` báo "đã sửa" dù `git diff` trống. Sửa bằng script chỉ đụng file cần sửa và giữ nguyên kiểu xuống dòng của file.
> 3. **Chuỗi liền không khoảng trắng** (tên 160 chữ "a") tràn ngang trong flex: cần `min-w-0` + `wrap-anywhere` (Tailwind 4.1+), `break-words` không đủ.
>
> **Việc cho sau (từ thảo luận làm lại giao diện, 2026-09-30):** chủ dự án sẽ đưa một mẫu giao diện chung để làm lại toàn bộ frontend sau khi xong các phase. Để việc đó dễ và an toàn: (1) có test Vitest + Testing Library kiểm **hành vi** (chữ, vai trò), không kiểm class, trước khi làm lại — Task 8.3; (2) gom màu chủ đạo về `@theme` trong `index.css` (`bg-brand` thay `bg-sky-600`) — Task 8.3; (3) tách logic khỏi component lớn (`DragDropContainer`, `ActivityFormDialog`, `EditTripDialog`) thành hook — làm dần khi sửa file, xong trước khi làm lại giao diện; (4) làm lại giao diện trước Task 8.5 để ảnh chụp là bản cuối.

### Task 2.6 — Làm lại giao diện theo UI_GUIDE.md

Nhánh: `feat/T2.6-ui-guide` · **Task thêm ngoài kế hoạch** (2026-09-30): chủ dự án thử mockup Stitch cho trang đăng nhập và danh sách chuyến đi, chọn làm lại giao diện ngay sau Phase 2 thay vì đợi cuối dự án (xem "Việc cho sau" của Task 2.5). Nguồn sự thật về giao diện: `UI_GUIDE.md` (design.md 3.2). Điều kiện mỗi commit frontend: `npm run lint` + `npm run build` xanh; commit có backend: `./gradlew build` xanh.

> **Thực tế khi làm 2.6 (2026-10-01):** PR #15, merge commit `8f06d72` (Merge commit, giữ lịch sử). 16 commit:
>
> | Commit | Nội dung |
> |---|---|
> | `d949878` docs(ui): add ui guide | `UI_GUIDE.md` bản đầu |
> | `c3c8e4e` style(frontend): add ui guide tokens, font and shared components | `styles/tokens.css` (`@theme`), font, `lucide-react`, Badge, Toaster, Skeleton, EmptyState, Logo |
> | `6923e05` style(frontend): apply ui guide form conventions | `FieldShell`, `PasswordField`, nút `size` / `variant` mới |
> | `042b8e9` feat(frontend): restyle trip list per ui guide | Thẻ chuyến đi, chip trạng thái, sắp xếp |
> | `332ad85` feat(frontend): add timetable rail to trip detail | Dải thời gian (ray) có chấm theo loại activity, menu "⋮" (Radix) |
> | `521d566` feat(frontend): mobile trip detail with day chips and move buttons | Chip ngày dính, nút ↑ / ↓ theo `pointer-coarse` |
> | `efa9e73` style(frontend): restyle auth pages and create trip wizard | Trang đăng nhập / đăng ký / đặt lại mật khẩu, thanh bước dạng tuyến |
> | `9501e8e` feat(trip): show activity count on trip cards | Backend `activityCount`, đếm `GROUP BY`, test số câu SQL |
> | `a838d5b` feat(trip): add trip counts by status | Backend `GET /trips/status-counts`, số trên chip |
> | `7811124` feat(frontend): move trip search to the top bar | Ô tìm kiếm trên thanh điều hướng |
> | `3beda76` docs(ui): align ui guide with the implementation and add stitch prompts per screen | UI_GUIDE viết lại theo code, mục 15 prompt Stitch |
> | `dcf747b` docs(ui): rename the trial to task 2.6 | |
> | `3bd7fcf` feat(frontend): show one trip day per page | Route `/trips/:id/days/:dayIndex`, nút ngày trước / sau |
> | `c74a21f` feat(frontend): move an activity to another day | Thả lên tên ngày ở cột trái, menu "Chuyển sang ngày…", thông báo có link |
> | `db74c66` feat(activity): place activities by start time | Backend tự xếp theo giờ bắt đầu (design.md 10.2 "Xếp theo giờ") |
> | `b73fc79` feat(frontend): pin the day header and add back to top | Ngày dài: khối mô tả ngày dính, "Đầu ngày", nút tròn lên đầu trang |
>
> Quyết định khi làm (chi tiết ở design.md 3.2, 10.2, 14.5, 15 và `UI_GUIDE.md`):
> - Token qua `@theme` (`jade`, `ink`, `paper`, `tide`...) đã làm ở đây, không đợi Task 8.3. Component vẫn tự viết, chỉ thêm `lucide-react` (icon) và Radix Dropdown Menu (menu "⋮").
> - **Một ngày một trang** để khớp với bản đồ từng ngày ở Phase 3; chuyển ngày bằng thả lên tên ngày (chỉ tính khi con trỏ nằm đúng trên mục) hoặc menu "⋮" (dùng được trên mọi thiết bị).
> - **Xếp theo giờ** chỉ di chuyển activity vừa đổi giờ, không sắp lại cả ngày, để kéo thả vẫn tự do.
> - **Ngày dài:** cả trang một thanh cuộn, khối mô tả ngày dính (từ 1024px), không dùng khung cao cố định có thanh cuộn riêng (cản kéo thả, khó dùng trên điện thoại, cột bản đồ Phase 3 sẽ dính theo trang). Bản đầu (thanh gọn hiện khi tiêu đề khuất) bị chủ dự án bỏ trước khi commit.
> - Lịch sử commit giữ nguyên (không squash), PR merge bằng Merge commit.
> - 507 lượt test backend (thêm 28). Frontend kiểm bằng 41 bài `MT-UI` trong `docs/testing/06-itinerary-ui.md`, **chưa chạy** lúc đóng task.
>
> **Bẫy đã gặp khi làm 2.6:**
> 1. **BUG-ACT-004 — quy tắc đúng nhưng người dùng thấy lạ:** "không có activity muộn hơn → đặt sau activity có giờ cuối cùng" làm activity không giờ ở cuối ngày bị chen lên trước. Code khớp quy tắc nên không tự sửa: báo chủ dự án, chủ dự án đổi quy tắc thành "xuống cuối ngày".
> 2. **BUG-UI-002 — cùng `z-index`:** chấm trên ray (`z-10`) vẽ đè lên khối mô tả ngày đang dính (cũng `z-10`) vì nằm sau trong DOM. Sửa: `isolate` cho danh sách activity, `z` của chấm chỉ có tác dụng trong danh sách.
> 3. **Cuộn tới phần tử đang dính không có tác dụng:** tiêu đề nằm trong khối `sticky` luôn được coi là "đang hiện", `scrollIntoView` không cuộn. Đích cuộn phải là phần tử không dính (`section#day-start` với `scroll-margin`).
> 4. **`space-y-*` của Tailwind v4 cho phần tử con margin qua `:where()`** (độ ưu tiên 0): một phần tử cao 0 vẫn đẩy nội dung 16px. Phần tử `fixed` không bị ảnh hưởng vì nằm ngoài luồng.
> 5. **Prettier không được cấu hình trong dự án:** chạy `npx prettier --write` định dạng lại cả file theo mặc định, diff phình to. Không chạy Prettier, sửa tay theo kiểu của file.

### Task 2.7 — Sửa lỗi sau rà soát Phase 1–2

Nhánh: `fix/T2.7-review-fixes` · **Task thêm ngoài kế hoạch** (2026-10-01): rà soát Phase 1–2 trước khi vào Phase 3 (507 lượt test xanh, `lint` + `build` xanh, không vi phạm quy tắc CLAUDE.md) vẫn ra 9 lỗi hành vi. Sửa trước Phase 3 vì Phase 3 xây đúng lên các chỗ này (endpoint có query param bắt buộc, hộp thoại hoạt động, trang chi tiết chuyến đi).

Mỗi lỗi một commit, test đi cùng. Commit backend: `./gradlew build` xanh. Commit frontend: `npm run lint` + `npm run build` xanh và một bài `MT-UI` mới trong `docs/testing/06-itinerary-ui.md` (frontend chưa có test tự động).

```
Mốc 0 — docs (main, trước khi tạo nhánh)
        design.md 6.1 (token đã thu hồi được gửi lại), 5.2 refresh_tokens (cột revoked_reason),
        10.3 (mã 415, 406, thiếu tham số); ghi 9 lỗi vào docs/testing (BUG-..., trạng thái "Đang mở");
        sửa các chỗ tài liệu lệch ở mục "Ghi nợ" bên dưới; chốt 3 quyết định của Phase 3 và thêm Task 3.8

Mốc 1 — fix(api): return 4xx for unsupported media type and missing parameters
        Hiện tượng (đã chạy thử trên backend local, BUG-PLAT-003): POST sai Content-Type → 500;
        gọi endpoint công khai kèm Accept: text/xml → 401 UNAUTHORIZED với path "/error" (việc ghi lỗi JSON thất bại,
        container chuyển sang /error, /error lại đòi đăng nhập); thiếu query param bắt buộc cũng sẽ → 500.
        GlobalExceptionHandler: HttpMediaTypeNotSupportedException → 415 UNSUPPORTED_MEDIA_TYPE (mã mới),
        HttpMediaTypeNotAcceptableException → 406 NOT_ACCEPTABLE (mã mới),
        MissingServletRequestParameterException → 400 VALIDATION_ERROR, details ở tên tham số;
        mọi ErrorResponse đặt sẵn Content-Type application/json (không thương lượng theo Accept);
        ErrorCode, messages.properties, test từng trường hợp

Mốc 2 — fix(auth): treat only rotated refresh tokens as theft
        Hiện tượng: đặt lại mật khẩu ở máy A rồi đăng nhập lại; máy B còn cookie cũ mở app → mọi phiên mới của A
        bị thu hồi, lặp lại mỗi lần B tải trang (token bị thu hồi vì reset / logout đang bị coi như token bị trộm).
        V8__add_refresh_token_revoked_reason.sql (revoked_reason ENUM, nullable), model/enums/RevokedReason
        (ROTATED, LOGOUT, PASSWORD_RESET, BLOCKED, EXPIRED, REUSE_DETECTED), RefreshToken.revoke(now, reason),
        RefreshTokenService, RefreshTokenRepository, AuthServiceImpl.refresh:
          hết hạn → 401 (kiểm trước, để token cũ không còn là "nút tắt mọi phiên" vô thời hạn);
          đã thu hồi do ROTATED (hoặc dòng cũ chưa có lý do) → coi là trộm, thu hồi mọi phiên;
          đã thu hồi do lý do khác → chỉ 401;
        mọi lần refresh thất bại: response xoá cookie refresh_token (trình duyệt thôi gửi lại);
        test tích hợp: reset ở A → B gửi cookie cũ → 401, phiên mới của A còn sống; dùng lại token đã xoay → vẫn
        thu hồi mọi phiên

Mốc 3 — fix(frontend): focus the first field when a dialog opens
        Hiện tượng: mở "Thêm hoạt động" rồi gõ ngay → không vào chữ nào, con trỏ nằm ở nút "×" (cần chủ dự án thử
        để xác nhận trước khi sửa). components/Modal.tsx

Mốc 4 — fix(frontend): keep form dialogs open while saving
        Hiện tượng: bấm Esc lúc đang lưu → hộp đóng, lỗi trùng giờ / lỗi validate trả về không ai thấy.
        ActivityFormDialog.tsx, EditTripDialog.tsx (ConfirmDialog đã có chặn sẵn)

Mốc 5 — fix(frontend): keep the trip page when a background refetch fails
        Hiện tượng: đang mở trang chuyến đi, một lần tải lại ngầm lỗi → cả trang thành ô báo lỗi, form đang gõ mất.
        pages/TripDetailPage.tsx: còn dữ liệu thì giữ trang + báo lỗi nhẹ, chỉ thay trang khi chưa có dữ liệu

Mốc 6 — fix(frontend): do not retry requests the server rejected
        Hiện tượng: mở chuyến đi không tồn tại → chờ ~7 giây mới thấy báo lỗi (4xx bị thử lại 3 lần).
        lib/queryClient.ts (tách khỏi main.tsx): 4xx không thử lại, lỗi mạng / 5xx vẫn thử lại; main.tsx

Mốc 7 — fix(frontend): keep the space typed in the trip search
        Hiện tượng: gõ "đà " rồi ngừng 0,3 giây → dấu cách biến mất, gõ tiếp thành "đànẵng".
        features/trips/TripSearchBox.tsx

Mốc 8 — fix(frontend): ignore a drop on the day being viewed
        Hiện tượng: kéo thẻ rồi thả lên chính ngày đang xem ở cột trái → thẻ nhảy xuống cuối ngày.
        features/itinerary/DragDropContainer.tsx (đường menu "⋮" đã có kiểm này)

Mốc 9 — fix(frontend): clear cached data when the session is dropped
        Hiện tượng: phiên bị rớt, người khác đăng nhập cùng tab → thấy dữ liệu của người trước vài giây.
        api/client.ts, lib/queryClient.ts
```

> **Ghi nợ từ lần rà soát, không sửa ở task này:**
> - Hai lần refresh đúng cùng lúc đều thành công (rotation chưa nguyên tử; frontend đã chặn bằng khoá, chỉ kẻ tấn công mới gặp) → Task 8.1.
> - Hai lần sửa cùng lúc trả 500 thay vì 409 `STALE_VERSION` → Task 5.3, làm cho **cả Trip lẫn Activity** (dòng mốc của 5.3 hiện chỉ ghi Activity).
> - Danh sách chuyến đi sắp theo cột có giá trị trùng (`startDate`, `title`) phân trang không ổn định: thêm `id` làm khoá phụ; `?page=` vượt quá số trang hiện "Chưa có chuyến đi nào" → Task 8.3.
> - `PATCH /trips/{id}` gửi `description: ""` lưu chuỗi rỗng thay vì `NULL` (design 10.2 ghi "chưa hỗ trợ xoá trắng") → chốt cùng quy ước xoá field ở Task 3.2 (`clearPlace`).
> - Vùng chạm dưới 44px trên điện thoại (nút "⋮" 28px, chip ngày 36px) → Task 3.6 khi sửa trang chi tiết.
> - Tài liệu lệch, **đã sửa** trong commit docs Mốc 0: `docs/testing/README.md` ghi 491 lượt (thật là 507); design 10.2 ghi danh sách "luôn 2 câu SQL" (trang đầy là 3: có thêm câu đếm); design 10.3 ghi `UNAUTHORIZED` là "thiếu/hết hạn" (hết hạn là `TOKEN_EXPIRED`); design 17.3 ghi local "tất cả mock" (mail là `smtp`).
> - Tài liệu lệch, **chưa sửa**: Swagger của `POST` / `PATCH` activity còn ghi "nằm cuối ngày" / "không đổi thứ tự" (trước "Xếp theo giờ" của Task 2.6) → sửa trong code ở Task 3.2 khi đụng `ActivityController`. UI_GUIDE 7.0 "một nút chính mỗi màn" mâu thuẫn với màn rỗng của danh sách (15.2 D: nút ở đầu trang và nút trong khung rỗng đều là nút chính) → chờ chủ dự án chọn, rồi sửa UI_GUIDE và code ở Task 3.6.

> **Thực tế khi làm 2.7 (2026-10-01):** PR #16, merge commit `3842ec3` (Merge commit, giữ lịch sử), docs đóng task `2979e93`. 9 commit đúng bảng đã duyệt, mỗi lỗi một commit, thêm commit docs Mốc 0 trên `main` (`1400774`).
>
> | Commit | Lỗi | Nội dung |
> |---|---|---|
> | `622cba1` fix(api): return 4xx for unsupported media type and missing parameters | BUG-PLAT-003 | 415, 406, thiếu tham số → 400; mọi `ErrorResponse` đặt sẵn `Content-Type` JSON |
> | `d219c3a` fix(auth): treat only rotated refresh tokens as theft | BUG-AUTH-006 | V8 `revoked_reason`, thứ tự kiểm mới của `refresh`, 401 kèm xoá cookie |
> | `2cc814d` fix(frontend): focus the first field when a dialog opens | BUG-UI-003 | `Modal` tự đặt con trỏ vào phần tử đầu tiên của nội dung |
> | `009e696` fix(frontend): keep form dialogs open while saving | BUG-UI-004 | `useIsMutating` ở vỏ hộp thoại, Esc và "×" bị bỏ qua lúc đang lưu |
> | `752f383` fix(frontend): keep the trip page when a background refetch fails | BUG-UI-005 | Giữ trang + khung lỗi + "Thử lại"; 404 / 403 vẫn thay cả trang |
> | `ae2af3b` fix(frontend): do not retry requests the server rejected | BUG-UI-006 | `lib/queryClient.ts`: 4xx không thử lại |
> | `e4f4520` fix(frontend): keep the space typed in the trip search | BUG-UI-007 | Ô tìm kiếm chỉ chép từ URL khi URL nói khác với ô |
> | `dcfcfbe` fix(frontend): ignore a drop on the day being viewed | BUG-UI-008 | Thả lên ngày gốc của hoạt động = không làm gì |
> | `025799a` fix(frontend): clear cached data when the session is dropped | BUG-AUTH-007 | Phiên kết thúc (mọi đường) → `queryClient.clear()` |
>
> Khác với bảng đã duyệt (chi tiết ở design.md 6.1, 10.3 và `UI_GUIDE.md` 7.6, 8.1):
> - **Mốc 1:** biểu hiện thật của `Accept: text/xml` là **401 ở `/error`**, không phải 500. Việc ghi `ErrorResponse` thất bại, container chuyển sang `/error`, mà `/error` không nằm trong `PUBLIC_PATHS`. Sửa gốc: mọi `ErrorResponse` đặt sẵn `Content-Type: application/json`.
> - **Mốc 2:** lỗi 403 (tài khoản bị khoá lúc refresh) **không** xoá cookie, vì `AccountBlockedException` dùng chung với đăng nhập; lần gửi lại kế tiếp nhận 401 và cookie bị xoá ở đó.
> - **Mốc 3:** sửa ở `Modal` nên mọi hộp thoại được sửa cùng lúc; hộp xác nhận đặt con trỏ ở "Huỷ". Gỡ `autoFocus` ở ba form trong hộp thoại.
> - **Mốc 9:** không sửa `api/client.ts`; một `useAuthStore.subscribe` trong `lib/queryClient.ts` che mọi đường làm mất phiên, `useLogout` bỏ lệnh xoá riêng.
>
> Kiểm chứng: 532 lượt test backend (thêm 25). `BUG-PLAT-003` chạy lại trên máy chủ thật (cổng 8081); `BUG-AUTH-006` được tái hiện bằng test tích hợp **trước khi** sửa (mong đợi 1 phiên sống, thực tế 0). Bảy mốc frontend chỉ có `lint` + `build`; chủ dự án đã thử Mốc 3, các bài `MT-UI-43`, `MT-UI-44`, `MT-UI-12`, `MT-UI-02`, `MT-UI-26`, `MT-AUTH-07`, `MT-AUTH-08` **chưa chạy** lúc đóng task.
>
> **Bẫy đã gặp khi làm 2.7:**
> 1. **Lỗi mà 507 test xanh không thấy:** mọi test đều gửi request đúng kiểu và mọi kịch bản chỉ có một thiết bị. Rà soát bằng câu hỏi "người gọi làm sai thì sao" và "có hai thiết bị thì sao" mới ra. Test chỉ chứng minh code khớp yêu cầu; BUG-AUTH-006 là lỗi của chính yêu cầu (design 6.1 mâu thuẫn rule 14.16).
> 2. **`/error` không công khai:** bất cứ lỗi nào container tự xử lý (`sendError`) đều đi qua `/error` và ra 401 "Bạn cần đăng nhập". MockMvc không có bước chuyển này (chỉ thấy 406 rỗng), phải chạy máy chủ thật mới thấy.
> 3. **Test tự ghi thời gian vào DB (BUG-AUTH-008):** `JdbcTemplate` + `java.sql.Timestamp` ghi theo múi giờ của JVM, Hibernate đọc cột theo UTC (`hibernate.jdbc.time_zone`). "Một phút trước" ở máy +07:00 thành gần 7 tiếng sau. Để MySQL tự tính: `UTC_TIMESTAMP(6) - INTERVAL 1 MINUTE`.
> 4. **`autoFocus` trong `<dialog>`:** React gọi `focus()` lúc phần tử được gắn, khi hộp còn đóng nên không có tác dụng; `showModal()` sau đó đặt con trỏ vào phần tử bấm được đầu tiên (nút "×").
> 5. **Trạng thái "đang lưu" nằm trong form con, phím Esc do vỏ hộp xử lý:** không đẩy state lên bằng effect; đặt `mutationKey` rồi dùng `useIsMutating` ở vỏ.

> ✅ Hết Phase 2 → **đây là mốc "sản phẩm dùng được"**. Tick `[x] Phase 2` trong CLAUDE.md. Ảnh chụp màn hình **chưa** làm ở đây — để dành tới Task 8.5 khi project hoàn chỉnh (quyết định 2026-09-26). Trước khi sang Phase 3: rà lại Phase 3 theo quy ước A.2 "Rà soát theo phase".

---

## PHASE 3 — Place, Map, Weather (provider mock)

> **Đã rà theo quy ước A.2 "Chia commit" (2026-10-01):** 4 task kiểu cũ → 7 task, mỗi mốc là một lát cắt dọc. Bảng file chi tiết Claude đưa ra đầu từng task để duyệt.
> **Tài liệu đã khớp (2026-10-01, commit docs đầu Task 3.1):** `design.md` 3.2, 5.2 `places`, 7.2, 8.1, 10.2 (Place, Weather, route, địa điểm của activity), rule 14.18–14.21, 15; `UI_GUIDE.md` 14 và 15.3 đổi "Task 3.4" thành 3.6 / 3.7.

Đọc trước: **design.md mục 7 (Provider Abstraction), 8 (Cache), 10.2 (Place, Weather, `route`), 5.2 (`places`, `activities.place_id`)**; task giao diện đọc thêm **UI_GUIDE.md 8.1, 9, 11, 15.3**.

> **Vì sao đổi thứ tự (so với bản cũ):**
> - Task 3.1 cũ tạo provider mà chưa endpoint nào dùng (trái A.2 điểm 3). Nay provider đi cùng endpoint đầu tiên dùng nó.
> - Task 3.3 cũ đặt cache lên `WeatherService`, nhưng service này tới 3.4 cũ mới có. Nay cache đứng sau cả tìm địa điểm lẫn thời tiết.
> - `GET /days/{dayId}/route` có trong design 10.2 và UI_GUIDE 15.3 nhưng không có task → Task 3.5.
> - Task 3.4 cũ gộp weather backend với toàn bộ giao diện → tách thành 3.3 (backend), 3.6 và 3.7 (giao diện).
>
> **Bốn thứ khác nhau, đừng lẫn:**
> | Thứ | Lấy từ đâu ở Phase 3 | Qua backend? |
> |---|---|---|
> | Hình bản đồ (tile) | Máy chủ tile thật (CartoDB Positron), trình duyệt tự tải bằng Leaflet | Không |
> | Tìm địa điểm (tên → toạ độ) | `MapProvider` mock: `resources/mock/places.json` | Có |
> | Quãng đường giữa hai điểm | `MapProvider` mock: đường chim bay × hệ số | Có |
> | Dự báo thời tiết | `WeatherProvider` mock: số sinh từ seed, ổn định | Có |
>
> **Quyết định dùng để chia mốc (chủ dự án chốt cả 9 điểm ngày 2026-10-01):**
> 1. Chọn một kết quả tìm kiếm → `POST /places` `{provider, externalId}`; server **tự tra lại provider** rồi lưu snapshot, trả `id`. Không tin tên / toạ độ client gửi, vì snapshot dùng chung giữa các người dùng (`UNIQUE (provider, external_id)`). Activity chỉ nhận `placeId`. ✔
> 2. Bỏ địa điểm khỏi activity: `clearPlace: true` trong `PATCH` (`placeId` là số, không có `""` như field văn bản). ✔
> 3. Địa điểm tự thêm (`MANUAL`) là **riêng tư**: có `created_by`, chỉ người tạo gắn được vào activity; người khác thấy nó qua chuyến đi họ được xem. Hoãn `GET /places/{id}` và `GET /weather/forecast` (chưa màn nào dùng). ✔
> 4. Thời tiết lấy theo **toạ độ điểm đến của chuyến đi**, một nơi cho cả chuyến (chuyến đi qua nhiều nơi dùng chung dự báo của điểm đến; dự báo theo địa điểm của từng ngày để sau); chưa có toạ độ → 200 kèm trạng thái "chưa có điểm đến", không phải lỗi. Dự báo chỉ có cho **16 ngày tới** (giới hạn của dịch vụ dự báo thật): quy tắc nằm ở service nên mock cũng tuân theo; ngày đã qua hoặc xa hơn trả "chưa có dự báo". ✔
> 5. Cảnh báo ngoài trời: ngày có xác suất mưa ≥ 60% **và** có activity loại `SIGHTSEEING` → cảnh báo cấp ngày kèm danh sách activity. Nguồn dự báo: mock ở Task 3.3, Open-Meteo thật ở Task 3.8. ✔ **Hoãn ngày 2026-10-02** (bảng commit Task 3.3): phần cảnh báo chưa làm, dự báo vẫn làm như đã chốt.
> 6. Bỏ cache `trip:detail` (design 8.1): `GET /trips/{id}` đã chỉ 4 câu SQL, trong khi phải xoá cache ở 8 chỗ ghi và dễ ra dữ liệu cũ khi làm realtime (Phase 5). ✔
> 7. Dữ liệu mock: **56 địa điểm ở 5 điểm đến** (Hà Nội, Đà Nẵng, Hội An, Đà Lạt, TP. Hồ Chí Minh), toạ độ tra từ OpenStreetMap lúc soạn file, có ghi nguồn. Giảm so với ~120 đề xuất ban đầu vì provider thật có ngay ở Task 3.8; mock chỉ còn phục vụ test và chạy khi không có mạng. ✔
> 8. Test có Redis: thêm container Redis vào `TestcontainersConfiguration`; `@DataJpaTest` / `@WebMvcTest` không nạp cache nên không đổi. ✔
> 9. Provider thật (Photon / Nominatim, OSRM, Open-Meteo) + Resilience4j: **Task 3.8**, ngay sau 3.7 (không đợi Phase 8). Mặc định mọi profile vẫn `mock`; bản thật bật bằng cấu hình. Port ở 3.1 / 3.3 / 3.5 thiết kế theo hình dạng dữ liệu của API thật để tới 3.8 không phải sửa service. ✔
>
> **Phụ thuộc:** Task 2.7 Mốc 1 xong trước 3.1 (`/places/search?q=` là endpoint đầu tiên có query param bắt buộc; thiếu `q` phải ra 400, không phải 500). Số migration bên dưới tính theo V8 của Task 2.7; kiểm số lớn nhất trước khi tạo file (CLAUDE.md rule 7).

### Task 3.1 — Tìm địa điểm

Nhánh: `feat/T3.1-place-search` · Test ghi vào `docs/testing/07-place.md` (file mới).

```
Mốc 0 — docs (main): design.md + UI_GUIDE.md theo 9 quyết định đã chốt của Phase 3 (đã soạn 2026-10-01)

Mốc 1 — refactor(common): extract accent stripping from the slug generator
        common/util/VietnameseText.stripAccents (bỏ dấu tiếng Việt, kể cả Đ / đ), SlugGenerator gọi hàm này;
        không đổi hành vi: SlugGeneratorTest giữ nguyên và vẫn xanh, thêm test riêng cho hàm mới

Mốc 2 — feat(place): add place search endpoint with mock map provider
        provider/map/MapProvider (chỉ search(query, limit)), provider/map/dto/PlaceResult
        (provider, externalId, name, address, lat, lng, category), model/enums/PlaceProvider (MOCK),
        provider/map/MockMapProvider (@ConditionalOnProperty app.providers.map=mock, nạp resources/mock/places.json
        lúc khởi động; so khớp không dấu, không phân biệt hoa thường, trên tên và địa chỉ),
        places.json bản đầu (~12 địa điểm ở Đà Nẵng, toạ độ tra từ OpenStreetMap, có ghi nguồn),
        dto/response/PlaceResultResponse, mapper/PlaceMapper, service/PlaceService.search, controller/PlaceController
        GET /places/search?q=&limit= (Auth; q 2–100 ký tự sau khi trim, limit 1–20, mặc định 8), messages.properties;
        test: "linh ung" ra "Chùa Linh Ứng"; cùng input → cùng kết quả cùng thứ tự; 401; q thiếu / quá ngắn → 400

Mốc 3 — feat(place): rank search results near a coordinate
        provider/map/dto/Coordinate, search(query, limit, near); lat / lng tuỳ chọn (có đủ cả hai hoặc bỏ cả hai →
        nếu không, 400); kết quả khớp xếp gần trước (giao diện gửi toạ độ điểm đến của chuyến đi: tìm "chợ" trong
        chuyến Đà Nẵng ra chợ ở Đà Nẵng trước); test: cùng từ khoá, khác toạ độ → khác thứ tự

Mốc 4 — feat(place): extend mock places to five destinations
        places.json ~50 địa điểm (Hà Nội, Đà Nẵng, Hội An, Đà Lạt, TP. Hồ Chí Minh); test dữ liệu: externalId không
        trùng, toạ độ nằm trong Việt Nam, category hợp lệ, không thiếu tên.
        Chủ dự án xem lướt toạ độ vài địa điểm trên bản đồ thật.

Mốc 5 — test(place): add place search flow integration test
        đăng nhập → tìm → kết quả; MockMapProvider là bean duy nhất của MapProvider ở profile test và local
```

**Nhớ:** `category` của mock dùng đúng 6 tên của `ActivityType` để form gợi ý sẵn loại hoạt động. `PlaceProvider` (Java) lớn dần theo task: `MOCK` ở 3.1, `MANUAL` ở 3.2, `OSM` ở 3.8; cột ENUM của V9 khai báo sẵn cả ba.

> **Thực tế khi làm 3.1 (2026-10-01):** 5 commit đúng bảng đã duyệt, thêm commit docs Mốc 0 trên `main` (`1e4736f`). PR #17, merge commit `2efb64d` (Merge commit, giữ lịch sử), docs đóng task `2c35e6a`.
>
> | Commit | Nội dung |
> |---|---|
> | `f0cd0f8` refactor(common): extract accent stripping from the slug generator | `VietnameseText.stripAccents`, `SlugGenerator` gọi lại; hành vi không đổi |
> | `458a670` feat(place): add place search endpoint with mock map provider | `MapProvider`, `MockMapProvider`, `PlaceService`, `PlaceController`, 13 địa điểm Đà Nẵng |
> | `0ff1b50` feat(place): rank search results near a coordinate | `Coordinate` (haversine), `search(query, limit, near)`, `lat` / `lng` |
> | `3ba2e85` feat(place): extend mock places to five destinations | 56 địa điểm, `MockPlacesDataTest` kiểm chính dữ liệu |
> | `2c84e02` test(place): add place search flow integration test | Cả ứng dụng thật, không mock |
>
> Endpoint mới: `GET /api/v1/places/search?q=&limit=&lat=&lng=` (Auth). 60 lượt test mới, toàn dự án 592 lượt; 49 kịch bản trong `docs/testing/07-place.md`. Kiểm chứng ngược 2 lần (tắt bỏ dấu → 5 test đỏ; bỏ qua toạ độ → 3 test đỏ). Bài thủ công `MT-PLACE-01` **chưa chạy** lúc đóng task.
>
> Quyết định khi làm (chi tiết ở design.md 5.2, 7.2, 10.2 "Quy ước Place API"):
> - **Thứ tự kết quả:** không có toạ độ → tên bắt đầu bằng từ khoá, tên chứa từ khoá, rồi khớp qua địa chỉ; cùng hạng giữ thứ tự trong file. Có toạ độ → địa điểm trong **50 km** đứng trước, rồi độ khớp tên, rồi gần hơn trước. Không xếp thuần theo khoảng cách: trong một thành phố người dùng cần kết quả khớp tên nhất.
> - Mọi từ của từ khoá phải có trong "tên + địa chỉ" đã bỏ dấu; phần chữ để so được chuẩn bị một lần lúc khởi động.
> - `q` kiểm bằng `@Pattern` (còn ít nhất 2 ký tự sau khi bỏ khoảng trắng đầu cuối) + `@Size(max = 100)` ngay trên tham số; "đủ cả `lat` và `lng` hoặc bỏ cả hai" kiểm ở service.
> - Dữ liệu: **56** địa điểm (Đà Nẵng 13, Hà Nội 12, Đà Lạt 11, Hội An 10, TP. Hồ Chí Minh 10), không phải ~50. Mọi tên, địa chỉ, toạ độ lấy từ Nominatim ngày 2026-10-01, file có trường `source` ghi nguồn và giấy phép ODbL; không toạ độ nào viết theo trí nhớ.
> - Toạ độ dùng `BigDecimal` (khớp `DECIMAL(10,7)` của Task 3.2), chỉ đổi sang `double` lúc tính khoảng cách.
>
> **Bẫy đã gặp khi làm 3.1:**
> 1. **Dữ liệu cũng cần test (BUG-PLACE-001):** lần tra "Bến xe Hội An" không ra kết quả, điểm đến này bị bỏ trống loại di chuyển mà người soạn không nhận ra; `MockPlacesDataTest` đếm hộ. Ngoài đếm loại, test còn so từng địa điểm với trung tâm điểm đến của nó (dưới 40 km) để bắt đảo vĩ độ / kinh độ.
> 2. **Sáp nhập hành chính 2025 trên OpenStreetMap:** Hội An nay ghi "Phường Hội An, … Thành phố Đà Nẵng", Đà Lạt ghi "Phường Xuân Hương - Đà Lạt, Tỉnh Lâm Đồng". Hệ quả: tìm "da nang" ra cả địa điểm Hội An (23 kết quả), test "cùng tập kết quả" với `limit` 20 không còn đúng nghĩa → nới lên 100. Viết kỳ vọng theo tên địa điểm, đừng theo số lượng.
> 3. **Hai constructor trong một `@Component`:** `MockMapProvider` có constructor cho Spring (đọc file) và constructor cho test (nhận danh sách) → Spring không tự chọn, phải đặt `@Autowired` trên constructor chính. Đây không phải `@Autowired` trên field (rule 4).
> 4. **Nominatim:** tối đa 1 lần hỏi mỗi giây, phải gửi `User-Agent` định danh, và cấm dùng cho gợi ý khi đang gõ. Tên quán ăn / khách sạn hay thiếu hoặc khác chính tả ("Pho Thin", "Tan Ky"): thử tên khác, không tự điền. Nhiều kết quả thiếu số nhà hoặc tên đường: giữ đúng như nguồn.
> 5. **Script sửa nhiều file:** viết script Python ra file rồi chạy; nhét script dài có cả nháy đơn lẫn nháy kép vào heredoc của Bash đã hai lần làm lệnh hỏng trước khi chạy. Khi thay chuỗi, luôn kiểm số lần khớp trước khi ghi file (một lần đếm sai 17 thay vì 15 đã được chặn nhờ vậy).
>
> **Việc cho Task 3.2:** thêm `MapProvider.lookup(externalId)` (mock: tra theo `externalId` trong danh sách đã nạp); `PlaceProvider` thêm `MANUAL`, cột ENUM của V9 khai báo sẵn `MOCK`, `OSM`, `MANUAL`; `PlaceMapper` thêm chiều entity → `PlaceResponse`; kết quả tìm kiếm đã mang `provider` + `externalId` để `POST /places` dùng.
> **Việc cho Task 3.4:** khoá cache của tìm kiếm phải gồm từ khoá đã chuẩn hoá, `limit` và toạ độ làm tròn, vì `near` làm đổi thứ tự.
> **Việc cho Task 3.6:** giao diện gửi `lat` / `lng` của điểm đến chuyến đi; `category` của kết quả là một trong 6 loại hoạt động, dùng để gợi ý sẵn ô "Loại".
> **Việc cho Task 3.8:** quy tắc "50 km trước" là của mock; Photon có sẵn ưu tiên theo toạ độ. Hợp đồng của `search` chỉ hứa "quanh `near` đứng trước" và "cùng input cùng kết quả".

---

### Task 3.2 — Gắn địa điểm vào hoạt động

Nhánh: `feat/T3.2-activity-place` · Test: `07-place.md`, `05-activity.md`. Mỗi commit một việc, code + test cùng commit, số file ghi trong ngoặc vuông.

Kết quả của task (chưa có giao diện, xem trên Swagger): chọn một kết quả tìm kiếm → địa điểm có `id`; tạo hoạt động kèm `placeId` → mọi nơi trả về hoạt động đều có `place` (tên, địa chỉ, toạ độ).

```
Mốc 0 — docs (main): design.md 10.2 (lỗi placeId, địa điểm tự thêm), rule 14.22 (chuyến đi đã qua), UI_GUIDE 8.1,
        PR #17 của Task 3.1 (commit `56c9cde`)

Commit 1 — feat(place): add place entity and migration                                              [3 file]
        V9__create_places.sql (cột ENUM khai báo sẵn MOCK, OSM, MANUAL; external_id so từng ký tự), model/Place,
        PlaceMappingTest

Commit 2 — feat(place): look up a place of the map source by its id                                 [3 file]
        MapProvider.lookup(externalId), MockMapProvider.lookup, MockMapProviderTest

Commit 3 — feat(place): save a chosen search result as a place                                      [10 file]
        PlaceRepository, SavePlaceRequest, PlaceResponse, PlaceMapper.toResponse, PlaceService.getOrCreate,
        POST /places {provider, externalId} (Auth) → 200; externalId lạ → 404; messages.properties;
        test repository, service, controller.
        10 file vì một endpoint đi qua mỗi tầng một file; chia nhỏ hơn là chia theo tầng (A.2 điểm 1)

Commit 4 — feat(place): keep one row when the same place is picked at the same moment               [3 file]
        PlaceService: INSERT bị UNIQUE từ chối → đọc lại dòng của request thắng (không @Transactional, xem javadoc);
        PlaceServiceTest; test 8 request cùng lúc trên MySQL thật trong PlaceSearchFlowIntegrationTest

Commit 5 — feat(place): add manual place endpoint                                                   [~9 file]
        PlaceProvider.MANUAL, Place.createdBy, CreateManualPlaceRequest, PlaceService.createManual,
        POST /places/manual → 201; category để trống được, có gửi thì thuộc 6 loại hoạt động; không gộp theo tên

Commit 6 — feat(place): reject a pick from a source that is not active                              [~6 file]
        MapProvider.provider(); POST /places với provider khác nguồn đang bật (kể cả MANUAL) → 400

Commit 7 — refactor(test): build activity requests and responses through one helper                 [~5 file]
        gom các chỗ test tự `new CreateActivityRequest(...)` / `new ActivityResponse(...)` về một hàm; hành vi không đổi

Commit 8a — feat(activity): add place column to activities                                          [3 file]
        V10__add_place_to_activities.sql (FK không cascade), Activity.place (LAZY), ActivityMappingTest

Commit 8b — feat(activity): show the place of an activity in every response                         [9 file]
        ActivityResponse.place, ActivityMapper dùng PlaceMapper (injectionStrategy = CONSTRUCTOR), 4 truy vấn có kết quả
        thành response nạp kèm place (3 truy vấn danh sách + findByIdAndTripId của PATCH); số câu SQL không đổi:
        chi tiết chuyến đi 4, danh sách hoạt động 4, reorder 6.
        Bảng duyệt ghi một Commit 8 ~9 file; đo trên code là 12 (thiếu 3 file test phải đổi cách khởi tạo mapper)
        nên tách theo điểm (a) của A.2, chủ dự án đồng ý ngày 2026-10-02. 8b còn 9 file, 3 file trong đó chỉ đổi
        một dòng khởi tạo ActivityMapper

Commit 9 — feat(activity): attach a place when creating an activity                                 [~7 file]
        CreateActivityRequest.placeId, PlaceService.findAttachable; placeId không tồn tại hoặc MANUAL của người khác →
        cùng một lỗi 400 ở field placeId

Commit 10 — feat(activity): change the place of an activity                                         [~6 file]
        UpdateActivityRequest.placeId; update nhận người đang đăng nhập; version tăng

Commit 11 — feat(activity): remove the place of an activity                                         [~5 file]
        clearPlace: true; gửi cả placeId lẫn clearPlace → 400; sửa mô tả Swagger còn ghi "nằm cuối ngày" (ghi nợ Task 2.7)

Commit 12 — test(place): add place and activity place flow integration test                         [~2 file]
        cả ứng dụng thật: tìm → chọn (một dòng, cùng id, không nhận tên / toạ độ của client) → gắn → xem chi tiết →
        đổi → bỏ; đếm câu SQL
```

> **Chia lại ngày 2026-10-01:** bản đầu của task có 5 mốc; Mốc 1 gộp bảng, `lookup`, endpoint, xử lý đồng thời và test toàn luồng thành **17 file / 863 dòng** (commit `e492ed3`). Chủ dự án chỉ ra nó trái A.2 ("3–8 file, một lý do để thay đổi"). Commit chưa push nên được gỡ bằng `git reset --soft HEAD~1` và commit lại thành Commit 1–4 ở trên; phần còn lại chia thành Commit 5–12. Mỗi commit trong 1–3 được dựng riêng (bản `git archive` của HEAD + đúng file của commit) và build xanh trước khi commit: 602, 604, 618 lượt test. Task 3.1 Mốc 2 (13 file) và Mốc 3 (10 file) cũng quá cỡ nhưng đã merge, giữ nguyên lịch sử.

> **Quyết định khi duyệt bảng commit 3.2 (2026-10-01)** — chi tiết ở design.md 5.2 `places`, 10.2 "Quy ước Place API" và "Địa điểm của activity", rule 14.18, 14.19:
> - `placeId` sai (không tồn tại, hoặc `MANUAL` của người khác) → **400 `VALIDATION_ERROR` ở field `placeId`**, "Địa điểm không tồn tại", không phải 404: đây là lỗi của một ô trong form.
> - Địa điểm tự thêm **không gộp theo tên**; `category` không bắt buộc, có gửi thì thuộc 6 loại hoạt động.
> - Địa điểm không còn activity nào dùng **không bị dọn** ở task này.
> - Người được chia sẻ quyền sửa (Phase 4) **không** gắn được địa điểm `MANUAL` của chủ chuyến đi sang activity khác; họ vẫn thấy nó và sửa được activity. Xét lại ở Task 4.2.
> - `POST /places` luôn trả 200 (lưu mới hay đã có đều như nhau với người gọi).
>
> **Ý tưởng để sau (chưa gán task):** dọn địa điểm `MANUAL` không còn ai dùng; ô "đã làm" cho từng activity (design rule 14.22); hạn mức gói miễn phí chỉ tính chuyến đi chưa hoàn thành (xét ở Task 6.1 cùng rule 14.9).

**Nhớ:** có 5 chỗ chuyển `Activity → ActivityResponse` (list, create, update, reorder, trip detail) và 20 chỗ trong test tự tạo request / response của activity (6 `CreateActivityRequest`, 9 `UpdateActivityRequest`, 5 `ActivityResponse`): Commit 7 gom chúng lại trước khi Commit 8 thêm trường. Số file trong ngoặc là ước lượng; commit nào vượt 8 file khi làm thì dừng lại báo trước, không âm thầm gộp hay tách.

> **Thực tế khi làm 3.2 (2026-10-01 → 2026-10-02):** 13 commit code trên nhánh, sau commit docs Mốc 0 trên `main` (`56c9cde`). Bảng duyệt có 12 commit; Commit 8 tách thành 8a / 8b khi đo ra 12 file. PR #18, merge commit `8f38592` (Merge commit, giữ lịch sử), docs đóng task `48a7754`.
>
> | Commit | File | Nội dung |
> |---|:--:|---|
> | `5b9088f` feat(place): add place entity and migration | 3 | V9 `places`, `Place`, `PlaceMappingTest` |
> | `9c7d7af` feat(place): look up a place of the map source by its id | 3 | `MapProvider.lookup(externalId)` |
> | `914f80e` feat(place): save a chosen search result as a place | 10 | `POST /places`, `PlaceService.getOrCreate`; 10 file vì một endpoint đi qua mỗi tầng một file |
> | `e04c842` feat(place): keep one row when the same place is picked at the same moment | 3 | đọc lại dòng của request thắng khi UNIQUE từ chối; test 8 luồng trên MySQL |
> | `db89c31` feat(place): add manual place endpoint | 9 | `POST /places/manual`, `PlaceProvider.MANUAL`, `Place.createdBy` |
> | `8ba7137` feat(place): reject a pick from a source that is not active | 8 | `MapProvider.provider()`; 400 ở field `provider` |
> | `19bad4a` refactor(test): build activity requests and responses through one helper | 5 | `support/TestActivities`; 20 chỗ gọi constructor về một nơi |
> | `cb5b475` feat(activity): add place column to activities | 3 | V10 `activities.place_id`, `Activity.place` |
> | `744c3ec` feat(activity): show the place of an activity in every response | 9 | `ActivityResponse.place`, 4 truy vấn đọc kèm place |
> | `9abd7cd` feat(activity): attach a place when creating an activity | 8 | `placeId` ở POST, `PlaceService.findAttachable` |
> | `6c2140a` feat(activity): change the place of an activity | 8 | `placeId` ở PATCH, `update` nhận `userId` |
> | `449e4ba` feat(activity): remove the place of an activity | 8 | `clearPlace`, viết lại mô tả Swagger của POST / PATCH |
> | `532bc80` test(place): add place and activity place flow integration test | 2 | `ActivityPlaceFlowIntegrationTest` (mới), `PlaceSearchFlowIntegrationTest` |
>
> Endpoint mới: `POST /api/v1/places` (200), `POST /api/v1/places/manual` (201). Endpoint đổi: `POST .../days/{dayId}/activities` nhận `placeId`; `PATCH .../activities/{activityId}` nhận `placeId`, `clearPlace`; mọi phản hồi có activity thêm `place`. Migration V9, V10. 89 lượt test mới, toàn dự án **681 lượt** (627 method, 50 file test); `07-place.md` 94 kịch bản, `05-activity.md` 202 kịch bản. Kiểm chứng ngược 4 lần (bỏ đọc lại khi trùng dòng; bỏ so nguồn; bỏ đọc kèm place ở repository; bỏ đọc kèm place ở test toàn luồng: danh sách ngày tốn 9 câu SQL thay vì 4). Một lỗi test (`BUG-PLACE-002`), không lỗi code. Bài thủ công `MT-PLACE-02`, `MT-PLACE-03`, `MT-ACT-13`, `MT-ACT-14` **chưa chạy** lúc đóng task (và `MT-PLACE-01` của Task 3.1).
>
> Quyết định khi làm (đã ghi vào design.md 10.2):
> - `provider` không phải nguồn đang bật → 400 ở field `provider`, kiểm **trước** khi đọc database. Khi Task 3.8 bật `osm`, chọn với `MOCK` tự bị từ chối, không cần sửa service.
> - `placeId` được kiểm **trước** bước trùng giờ (create và update): lỗi 409 mời người dùng gửi lại với `allowOverlap=true`, lần gửi lại đó không được vấp một ô đã sai từ đầu.
> - Gửi cả `placeId` lẫn `clearPlace: true` → 400 ở field `clearPlace`, bị chặn trước khi tra địa điểm. `clearPlace: false` = không gửi. Bỏ địa điểm của activity không có địa điểm: 200, version không tăng.
> - Bốn truy vấn đọc kèm place, không phải ba như bảng duyệt: thêm `findByIdAndTripId` vì PATCH cũng trả activity.
> - `PlaceService.getOrCreate` **không** `@Transactional`: mỗi lệnh repository là một transaction ngắn, nên khi INSERT bị UNIQUE từ chối chỉ lệnh đó rollback và vẫn đọc lại được dòng của request thắng.
> - `ActivityServiceImpl` gọi `PlaceService` (service gọi service) để quy tắc "địa điểm nào dùng được" chỉ nằm một chỗ.
>
> **Bẫy đã gặp khi làm 3.2:**
> 1. **Commit quá cỡ:** xem "Chia lại ngày 2026-10-01" ở trên. Ước lượng số file hay thiếu các file test bị kéo theo khi một constructor đổi (mapper, record); đếm bằng `grep` các chỗ gọi trước khi ghi số vào bảng.
> 2. **`CHECK` của MySQL không ra `DataIntegrityViolationException` (BUG-PLACE-002):** lỗi 3819 được Spring đổi thành `UncategorizedSQLException`. Test ràng buộc `CHECK` nên kiểm tên ràng buộc trong thông báo, đừng kiểm loại exception. Vi phạm khoá ngoại và UNIQUE thì vẫn là `DataIntegrityViolationException`.
> 3. **Mapper có `uses`:** `Mappers.getMapper(ActivityMapper.class)` không dựng được nữa (không còn constructor rỗng); unit test dùng `new ActivityMapperImpl(Mappers.getMapper(PlaceMapper.class))`, test `@DataJpaTest` phải `@Import` thêm `PlaceMapperImpl`.
> 4. **Record không có builder:** thêm một trường làm vỡ mọi chỗ `new XxxRequest(...)` trong test. Gom về `support/TestActivities` trước (Commit 7) thì ba lần thêm trường sau đó chỉ sửa một file.
> 5. **`verifyNoInteractions(mock)` vỡ khi service bắt đầu hỏi mock một câu vô hại** (`mapProvider.provider()`): đổi sang `verify(mock, never()).lookup(any())`, nói đúng điều test muốn chặn.
> 6. **Script sửa nhiều file ghi từng file một:** một anchor sai ở file thứ bảy để lại sáu file đã sửa, chạy lại thì vấp chính các file đó. Script phải kiểm **mọi** anchor trước rồi mới ghi (đã làm từ Commit 11).
> 7. **Điểm hở giữa hai commit:** sau Commit 5, `POST /places` với `MANUAL` chưa bị chặn cho tới Commit 6. Chia nhỏ commit thì phải nói rõ điểm hở tạm thời trong báo cáo và có test đóng nó ở commit sau (`TC-PLACE-083`, `091`).
>
> **Việc cho Task 3.5:** `Activity.place` đã có toạ độ; quãng đường trong ngày đọc từ `findByTripDayIdOrderByOrderIndexAscIdAsc` (đã đọc kèm place), bỏ qua activity không có địa điểm.
> **Việc cho Task 3.6:** form hoạt động gửi `placeId` khi chọn kết quả (gọi `POST /places` trước), `clearPlace: true` khi bấm bỏ; chỉ gửi `placeId` khi người dùng **đổi** địa điểm. `ActivityResponse.place` có đủ dữ liệu để chấm lên bản đồ, không cần gọi thêm.
> **Việc cho Task 3.8:** `OsmMapProvider.provider()` trả `OSM`, thêm hằng `OSM` vào `PlaceProvider` (cột ENUM của V9 đã có sẵn, không cần migration); bản lưu `MOCK` cũ vẫn hiển thị trong activity nhưng không chọn mới được.
> **Việc cho Task 4.2:** cộng tác viên gửi lại đúng `placeId` mà activity đang có, khi đó là địa điểm `MANUAL` của chủ chuyến đi, sẽ nhận 400 theo quy tắc hiện tại. Quyết định: hoặc coi "gửi lại địa điểm đang có" là không đổi, hoặc giữ nguyên và để giao diện chỉ gửi `placeId` khi đổi (Task 3.6 đã làm theo hướng này).

---

### Task 3.3 — Thời tiết của chuyến đi

Nhánh: `feat/T3.3-trip-weather` · Test: `docs/testing/08-weather.md` (file mới).

Mỗi commit một việc, code + test cùng commit, số file ghi trong ngoặc vuông.

Kết quả của task (chưa có giao diện, xem trên Swagger): `GET /weather/trips/{tripId}` trả mỗi ngày của chuyến đi một phần tử; ngày nằm trong 16 ngày tới có dự báo (tình trạng, nhiệt độ thấp / cao, xác suất mưa).

```
Mốc 0 — docs (main): PR #18 của Task 3.2; design.md 10.2 (7 giá trị condition, kiểu nhiệt độ, nguồn múi giờ);
        hoãn cảnh báo ngoài trời (design 2.1, 10.2, rule 14.21; UI_GUIDE 7.3, 9, 14, 15.3; Task 3.7 Mốc 2); bảng commit này

Commit 1 — feat(weather): add forecast model and weather provider port                              [3 file]
        provider/weather/dto/WeatherCondition (7 giá trị), provider/weather/dto/DailyForecast
        (date, condition, tempMin, tempMax, precipitationProbability), provider/weather/WeatherProvider
        .forecast(lat, lng, from, to): mỗi ngày một DailyForecast, được phép thiếu ngày.
        Chưa có hành vi nên chưa có test; test của Commit 2 là test đầu tiên chạm tới chúng

Commit 2 — feat(weather): add mock weather provider                                                 [2 file]
        MockWeatherProvider (@ConditionalOnProperty app.providers.weather=mock): seed = hash(lat, lng làm tròn
        4 chữ số, date); xác suất mưa sinh trước, condition suy ra từ nó; nhiệt độ thấp < cao;
        MockWeatherProviderTest: cùng input → cùng kết quả, khác ngày / khác toạ độ → khác, mỗi ngày trong khoảng
        đúng một phần tử

Commit 3 — feat(weather): add trip forecast endpoint                                                [8 file]
        dto/response/ForecastResponse, TripWeatherDayResponse (dayId, date, forecast), TripWeatherResponse (days),
        mapper/WeatherMapper, service/WeatherService.forTrip(tripId) (ghép dự báo theo ngày, không theo vị trí),
        controller/WeatherController GET /weather/trips/{tripId} (@PreAuthorize canView);
        chuyến đi chưa có toạ độ: không gọi provider, mọi ngày forecast = null;
        test service + controller: 200, 401, 403 người lạ, 404 trip đã xoá, tripId không phải số → 400

Commit 4 — feat(weather): report a trip without destination                                         [6 file]
        dto/response/TripWeatherStatus (OK, NO_DESTINATION), TripWeatherResponse.status; mô tả Swagger; 2 file test.
        Bảng duyệt ghi model/enums và 5 file; đổi khi làm, xem "Thực tế khi làm 3.3"

Commit 5 — feat(user): tell today's date in the time zone of the account                            [5 file]
        config/ClockConfig (bean Clock), UserRepository.findTimezoneById, UserService.today(userId);
        múi giờ không hợp lệ → Asia/Ho_Chi_Minh + log WARN; UserRepositoryTest, UserServiceTest (mới):
        23:30 UTC thì ở Việt Nam đã sang ngày hôm sau. Chưa ai gọi cho tới Commit 6 (điểm tách (b) của A.2)

Commit 6 — feat(weather): limit the forecast to the next sixteen days                               [4 file]
        WeatherService.forTrip(tripId, userId) chỉ hỏi provider phần giao giữa chuyến đi và [hôm nay, hôm nay + 15];
        controller truyền id người đang đăng nhập; test biên với Clock cố định: ngày thứ 16 có, ngày thứ 17 không,
        hôm qua không, chuyến đi nằm trọn ngoài khoảng → không gọi provider

Commit 7 — test(weather): add trip weather flow integration test                                    [1 file]
        cả ứng dụng thật, Clock cố định: đăng nhập → tạo chuyến đi có điểm đến → xem thời tiết; người lạ 403;
        chuyến đi đã xoá 404; đếm câu SQL: 4 khi có điểm đến, 3 khi chưa có
```

> **Quyết định khi duyệt bảng commit 3.3 (2026-10-02)** — chi tiết ở design.md 10.2 "Quy ước Weather API":
> - `condition` có 7 giá trị, khai báo đủ ngay từ task này (hợp đồng với giao diện); mock chỉ sinh 5 giá trị hợp với Việt Nam.
> - Múi giờ của tài khoản đọc thẳng từ `users.timezone` (thêm 1 câu SQL mỗi request), không đưa vào JWT.
> - Nhiệt độ: độ C, một chữ số thập phân; xác suất mưa: số nguyên 0–100.
> - **Cảnh báo hoạt động ngoài trời hoãn** (Mốc 3 của bản cũ): chủ dự án thấy hiển thị tình trạng, nhiệt độ và xác suất mưa là đủ; quy tắc `SIGHTSEEING` còn thô (bảo tàng cũng bị cảnh báo). Response không có ô `warning`; thêm lại sau không làm hỏng client cũ. Xét lại khi làm quyền lợi Premium "Weather alert qua email" (design mục 9).
> - Bảng duyệt lần đầu có 6 commit; chủ dự án nhắc lại quy ước cắt lát nhỏ (mô hình dữ liệu đứng riêng, mỗi hành vi nhỏ một commit) nên chia lại thành 9, còn 7 sau khi hoãn cảnh báo.
>
> **Điểm hở tạm thời:** sau Commit 3, ngày đã qua và ngày quá xa vẫn có dự báo (mock trả mọi ngày được hỏi); Commit 6 đóng lại và có test.

**Vì sao mock phải ổn định:** test tích hợp không được đỏ ngẫu nhiên (CLAUDE.md rule 24), và người dùng tải lại trang phải thấy cùng dự báo.

> **Thực tế khi làm 3.3 (2026-10-02 → 2026-10-03):** 7 commit code trên nhánh, đúng bảng đã duyệt, sau commit docs Mốc 0 trên `main` (`8a0fe9e`). PR #19, merge commit `5cfa377` (Merge commit, giữ lịch sử), docs đóng task `9f99b55`.
>
> | Commit | File | Nội dung |
> |---|:--:|---|
> | `8e6ac29` feat(weather): add forecast model and weather provider port | 3 | `WeatherCondition` (7 giá trị), `DailyForecast`, `WeatherProvider.forecast` |
> | `df402db` feat(weather): add mock weather provider | 2 | `MockWeatherProvider`: số ổn định theo toạ độ làm tròn + ngày |
> | `2413c06` feat(weather): add trip forecast endpoint | 8 | `GET /weather/trips/{tripId}`, `WeatherService.forTrip`, `WeatherMapper`, 3 DTO |
> | `5923b50` feat(weather): report a trip without destination | 6 | `TripWeatherStatus`, `TripWeatherResponse.status`; bảng duyệt ghi 5, thêm `WeatherController` vì mô tả Swagger phải nhắc `status` |
> | `f7d0704` feat(user): tell today's date in the time zone of the account | 5 | `ClockConfig`, `UserRepository.findTimezoneById`, `UserService.today` |
> | `6f58daf` feat(weather): limit the forecast to the next sixteen days | 4 | `forTrip(tripId, userId)`, khoảng [hôm nay, hôm nay + 15] |
> | `2085c1e` test(weather): add trip weather flow integration test | 1 | `TripWeatherFlowIntegrationTest`, đồng hồ đứng yên, đếm câu SQL |
>
> Endpoint mới: `GET /api/v1/weather/trips/{tripId}` (canView). Không có migration, không đổi cấu hình (`app.providers.weather: mock` đã có sẵn). 37 lượt test mới, toàn dự án **718 lượt** (664 method, 55 file test); `08-weather.md` 37 kịch bản. Kiểm chứng ngược 8 lần (tình trạng không theo xác suất mưa; bỏ kiểm toạ độ; coi nửa toạ độ là có điểm đến; bỏ múi giờ tài khoản ở `UserService` và ở test toàn luồng; khoảng 17 ngày; bỏ lọc kết quả thừa; khoảng bắt đầu từ ngày đầu chuyến đi). Không có test đỏ ngoài dự kiến, không dòng `BUG-` mới. Bài thủ công `MT-WEATHER-01` **chưa chạy** lúc đóng task. Số câu SQL: 4 khi có điểm đến (quyền, chuyến đi, các ngày, múi giờ), 3 khi chưa có; không tăng theo số ngày.
>
> Quyết định khi làm (đã ghi vào design.md 10.2):
> - `WeatherService.forTrip` **không** `@Transactional`: hai lần đọc là hai lệnh ngắn; bọc cả method thì kết nối database bị giữ suốt lúc chờ nguồn dự báo, mà từ Task 3.8 đó là lời gọi mạng.
> - Dự báo ghép vào ngày **theo ngày lịch**, không theo vị trí. Nguồn lặp một ngày → giữ dự báo đầu; nguồn trả thừa ngoài khoảng đã hỏi → bỏ, không làm khoảng 16 ngày rộng ra.
> - Chuyến đi chỉ có một nửa toạ độ → `NO_DESTINATION`. Chưa có điểm đến thì không tra múi giờ. Có điểm đến mà không ngày nào có dự báo (đã qua, quá xa, nguồn không trả) → vẫn `OK`.
> - `TripWeatherStatus` nằm ở `dto/response`, không ở `model/enums` như bảng duyệt: `model/enums` chỉ dành cho enum gắn với cột database. `WeatherCondition` nằm ở `provider/weather/dto` và được response dùng chung.
> - "Hôm nay" của tài khoản: `UserService.today(userId)` trên bean `Clock`; tài khoản không còn → 404. `WeatherService` gọi `UserService` (service gọi service, như `ActivityServiceImpl` → `PlaceService`).
> - Mock: xác suất mưa sinh trước, tình trạng suy ra (dưới 20 `CLEAR`, 40 `PARTLY_CLOUDY`, 60 `CLOUDY`, 85 `RAIN`, còn lại `THUNDERSTORM`); nhiệt độ cao 24–35, thấp hơn 4–8 độ, sinh theo phần mười độ.
>
> **Bẫy đã gặp khi làm 3.3:**
> 1. **Bảng commit lần đầu vẫn quá thô:** 6 commit, trong đó một dòng ~8 file gộp provider + endpoint. Chủ dự án nhắc lại: mô hình dữ liệu đứng riêng, mỗi hành vi nhỏ một commit. Khi phân vân thì tách nhỏ hơn, đừng biện minh cho dòng lớn.
> 2. **Báo cáo sau commit quá ngắn:** commit 3 file không có hành vi vẫn phải báo đủ bốn phần (file, mục đích, từng file và method, luồng), kể cả khi luồng là "chưa có luồng chạy".
> 3. **Seed liền nhau:** `new Random(seed)` với các seed chênh nhau 1 (hai ngày liền nhau) cho số đầu tiên gần giống nhau. Trộn seed bằng một hàm băm 64 bit trước khi dùng.
> 4. **Ước lượng số file bỏ sót mô tả Swagger:** thêm một trường vào response thì mô tả ở controller cũng phải đổi (Commit 4: 6 file thay vì 5).
> 5. **Test thừa:** một test "hôm nay là của người gọi" chỉ lặp lại điều các stub đã chứng minh, đã bỏ trước khi commit (CLAUDE.md rule 25).
> 6. **Thay bean `Clock` trong test toàn luồng** bằng `@TestBean(methodName = ...)` tạo một context riêng (và một container MySQL riêng) cho class đó: build dài thêm khoảng 10 giây. Chấp nhận, vì test theo "hôm nay" mà không có đồng hồ đứng yên sẽ đỏ quanh nửa đêm.
> 7. **Script sửa tài liệu nhét vào heredoc của Bash lại hỏng** (dấu nháy trong nội dung tiếng Việt) lúc soạn tài liệu đóng task; không file nào bị ghi dở vì lệnh hỏng trước khi chạy. Script dài: ghi ra file `.py` rồi chạy, như đã ghi ở Task 3.1.
>
> **Việc cho Task 3.4:** lời gọi provider nằm trong method `private` của `WeatherService`; `@Cacheable` đặt ở đó sẽ không chạy (tự gọi trong cùng bean). Đặt cache ở một bean riêng bọc lời gọi provider, hoặc ngay trên bản hiện thực của provider. Khoá cache theo design 8.1 gồm `from` và `to` đã thu hẹp theo "hôm nay", nên mỗi ngày là một khoá mới cho chuyến đi đang diễn ra. `DailyForecast` là `record` có `LocalDate` và enum: kiểm serializer đọc lại đúng kiểu.
> **Việc cho Task 3.7:** response là `{ status, days[{ dayId, date, forecast }] }`; `forecast: null` + `status: OK` = "Chưa có dự báo", `NO_DESTINATION` = mời chọn điểm đến. 7 giá trị `condition` cần 7 icon. Không có ô `warning` (Mốc 2 hoãn). "Hôm nay" ở giao diện lấy từ `timezone` của `GET /users/me`, cùng nguồn với backend.
> **Việc cho Task 3.8:** `OpenMeteoWeatherProvider` đổi mã WMO về 7 giá trị (mưa phùn, mưa rào → `RAIN`), được phép trả thiếu ngày; service đã lọc và ghép theo ngày nên không phải sửa. Ngày của dự báo là ngày lịch **tại điểm đến**, trong khi khoảng 16 ngày tính theo "hôm nay" của tài khoản: lệch tối đa một ngày khi hai nơi khác múi giờ, quyết định có cần xử lý không. Provider lỗi → 200 không dự báo (design 10.2): cân nhắc thêm một giá trị `status` để giao diện ghi "tạm thời không có dự báo".

---

### Task 3.4 — Redis cache

Nhánh: `feat/T3.4-redis-cache` · Test: thêm phần "cache" vào `07-place.md` và `08-weather.md`; kết nối Redis và health ghi ở `01-platform.md`. Mỗi commit một việc, code + test cùng commit, số file ghi trong ngoặc vuông.

Kết quả của task (người dùng không thấy tính năng mới): kết quả tìm địa điểm được giữ trong Redis 24 giờ, dự báo thời tiết 3 giờ; Redis tắt thì API vẫn chạy.

```
Mốc 0 — docs (main): PR #19 của Task 3.3; design.md 8.1 (cache đặt ở bean riêng, dữ liệu lưu, thời gian chờ, health);
        bảng commit này

Commit 1 — feat(cache): connect the application to redis                                            [4 file]
        build.gradle (spring-boot-starter-data-redis, BOM quản version), application.yml spring.data.redis
        (REDIS_HOST / REDIS_PORT đã có trong .env; tắt Redis repository), TestcontainersConfiguration thêm container
        redis:7-alpine (@ServiceConnection name = "redis"), RedisConnectionIntegrationTest: ghi một khoá có hạn rồi
        đọc lại. Chưa chức năng nào dùng kết nối này cho tới Commit 4 (điểm tách (b) của A.2)

Commit 2 — feat(cache): keep health up when redis is down                                           [2 file]
        application.yml management.health.redis.enabled=false (chỉ báo Redis tự xuất hiện từ Commit 1),
        RedisDownIntegrationTest: dừng container Redis → /actuator/health vẫn UP (context riêng, @DirtiesContext)

Commit 3 — feat(cache): build the cache key of a place search                                       [2 file]
        service/PlaceSearchCache.keyOf(query, limit, near): từ khoá bỏ dấu, chữ thường, gộp khoảng trắng + limit +
        toạ độ làm tròn 4 chữ số (hoặc "không có toạ độ"); PlaceSearchCacheTest: "Chợ  Hàn" và "cho han" chung khoá,
        khác limit / khác toạ độ → khác khoá. Chưa ai gọi cho tới Commit 4

Commit 4 — feat(cache): cache place search in redis                                                 [7 file]
        build.gradle (spring-boot-starter-cache), config/CacheConfig (@EnableCaching, cache place:search TTL 24h,
        JSON có kiểu List<PlaceResult> bằng JacksonJsonRedisSerializer), common/constant/CacheNames,
        PlaceSearchCache thành bean có search(...) @Cacheable gọi MapProvider.search, PlaceService.search gọi qua nó,
        PlaceServiceTest; PlaceSearchCacheIntegrationTest (Redis thật): tìm 2 lần → provider bị gọi 1 lần; từ khoá
        khác → gọi lại; đọc lại đúng kiểu PlaceResult; khoá trong Redis có hạn 24h; cache được xoá giữa các test

Commit 5 — feat(cache): skip the cache when redis fails                                             [3 file]
        CacheConfig: CacheErrorHandler ghi log WARN rồi bỏ qua cache (không trả 500); application.yml
        spring.data.redis.timeout và connect-timeout = 1s; RedisDownIntegrationTest: dừng container → tìm địa điểm
        vẫn 200 và đủ kết quả, trong vài giây

Commit 6 — feat(cache): cache weather forecast in redis                                             [8 file]
        CacheNames + CacheConfig (weather:forecast TTL 3h, List<DailyForecast>), service/ForecastCache
        (keyOf: toạ độ làm tròn 4 chữ số + from + to; forecast(...) @Cacheable gọi WeatherProvider.forecast),
        WeatherService gọi qua nó, WeatherServiceTest; ForecastCacheIntegrationTest như Commit 4;
        RedisDownIntegrationTest thêm: Redis dừng → thời tiết vẫn 200
```

> **Quyết định khi duyệt bảng commit 3.4 (2026-10-03)** — chi tiết ở design.md 8.1:
> - **Cache đặt ở một bean riêng giữa service và provider** (`PlaceSearchCache`, `ForecastCache`), không ghi `@Cacheable` lên bản hiện thực của provider và không bọc provider bằng một bean cùng interface. Lý do: nhìn vào là biết cái gì được cache; Task 3.8 thêm provider thật mà không đụng tới cache; `@Cacheable` trên method `private` của `WeatherService` không chạy.
> - Redis là **cache dùng chung cho mọi người dùng**, nằm cạnh backend, không nằm ở máy người dùng. Chỉ cache câu trả lời của nguồn bên ngoài (không có dữ liệu riêng của ai). Khác bảng `places`: bảng là dữ liệu lâu dài mà activity trỏ tới; cache là bản tạm, mất không sao.
> - Kiểm "Redis tắt" bằng test **dừng hẳn container**, cộng một bài thủ công.
> - Thời gian chờ Redis: **1 giây** cho kết nối và cho mỗi lệnh (mặc định của thư viện là 60 giây).
> - Thứ tự commit chia lại so với bản đầu của buổi thảo luận: "bật cache" đi cùng cache đầu tiên chứ không cùng kết nối; health đứng ngay sau kết nối; "Redis lỗi" đứng trước cache thứ hai để điểm hở ngắn nhất.
>
> **Điểm hở tạm thời:** sau Commit 4, Redis tắt thì tìm địa điểm trả 500; Commit 5 đóng lại và có test.
>
> **Hạn chế đã biết, xét lại ở Task 3.8:** khoá tìm địa điểm bỏ dấu nên "chợ hàn" và "cho han" chung một ô cache (với Photon thật hai cách gõ có thể cho kết quả khác nhau); khoá thời tiết gồm cả khoảng ngày nên hai chuyến đi cùng điểm đến nhưng lệch ngày không dùng chung cache.

**Nhớ:** DTO được cache là `record`: kiểm serializer đọc lại đúng kiểu (không thành `Map`) ngay ở test của Commit 4. Tên class serializer Jackson 3 của Spring Data Redis 4, đã tra tài liệu chính thức ngày 2026-10-03: `JacksonJsonRedisSerializer` (một kiểu cố định) và `GenericJacksonJsonRedisSerializer`. Spring Boot 4.1.1 nối Redis trong test bằng `GenericContainer` + `@ServiceConnection(name = "redis")`, không cần thêm thư viện test.

> **Thực tế khi làm 3.4 (2026-10-03):** 7 commit code trên nhánh = 6 commit của bảng đã duyệt + 1 commit `fix` chen vào trước Commit 6, sau commit docs Mốc 0 trên `main` (`531f9c1`). PR #20, merge commit `11d7db6`; commit docs đóng task `c6deddd`.
>
> | Commit | File | Nội dung |
> |---|:--:|---|
> | `464cddf` feat(cache): connect the application to redis | 4 | starter data-redis, `spring.data.redis`, container Redis trong `TestcontainersConfiguration`, `RedisConnectionIntegrationTest` |
> | `6a3430d` feat(cache): keep health up when redis is down | 2 | `management.health.redis.enabled=false`, `RedisDownIntegrationTest` (dừng hẳn container) |
> | `26780ce` feat(cache): build the cache key of a place search | 2 | `PlaceSearchCache.keyOf`, dạng `8\|16.0678,108.2208\|cho han` |
> | `a93a4b5` feat(cache): cache place search in redis | 7 | starter cache, `CacheConfig`, `CacheNames`, `PlaceSearchCache.search` `@Cacheable`, `PlaceService` gọi qua nó |
> | `50acc15` feat(cache): skip the cache when redis fails | 3 | `CacheErrorHandler` ghi WARN rồi bỏ qua, `timeout` / `connect-timeout` 1s |
> | `8b7c864` fix(cache): write cache entries before returning instead of in the background | 2 | `RedisCacheWriter.create(..., writer -> writer.immediateWrites())`; BUG-PLACE-003 |
> | `957550e` feat(cache): cache weather forecast in redis | 8 | `ForecastCache`, `WeatherService` gọi qua nó; bảng duyệt ghi 7, thêm một dòng ở `PlaceSearchCacheIntegrationTest` (danh sách cache giờ có hai cái) |
>
> Không có endpoint mới, không có migration. Dependency mới: `spring-boot-starter-data-redis`, `spring-boot-starter-cache` (BOM quản version). Cấu hình mới trong `application.yml`: `spring.data.redis.*` (host, port, hai thời gian chờ 1s, tắt repository), `management.health.redis.enabled=false`. 24 lượt test mới, toàn dự án **742 lượt** (688 method, 60 file test); `07-place.md` +12 kịch bản, `08-weather.md` +5, `01-platform.md` +7 và 1 bài thủ công (`MT-PLAT-06`), `MT-PLACE-04` mới. Kiểm chứng ngược 8 lần; hai commit (2 và 5) viết test trước rồi mới sửa, test đỏ đúng như dự đoán (health `DOWN`; tìm địa điểm 500). Một lỗi code tìm ra khi làm: **BUG-PLACE-003** (xem bẫy 1). Bài thủ công `MT-PLACE-04`, `MT-PLAT-06` **chưa chạy** lúc đóng task. Build toàn bộ dài thêm khoảng 1 phút (container Redis cho mỗi context, một context riêng bị bỏ sau `RedisDownIntegrationTest`).
>
> Quyết định khi làm (đã ghi vào design.md 8.1):
> - **Ghi cache đồng bộ** (`immediateWrites`): bộ ghi mặc định của Spring Data Redis 4 ghi ở nền khi dùng Lettuce, làm test đọc ngay sau khi ghi đỏ thỉnh thoảng và làm lỗi ghi không tới `CacheErrorHandler`. Giá: mỗi lần ghi chờ Redis khoảng 1 ms.
> - Khoá: từ khoá đứng **cuối** khoá tìm địa điểm, để chuỗi người dùng gõ không bị đọc nhầm thành `limit` hay toạ độ; khoá đọc được bằng mắt khi nhìn vào Redis.
> - Cache chưa khai báo trong `CacheConfig` thì không tồn tại (`disableCreateOnMissingCache`): gõ sai tên cache lỗi ngay, không lặng lẽ tạo cache không có hạn.
> - Câu trả lời rỗng cũng được cất (`[]`), "không tìm thấy" không bị hỏi lại 24 giờ.
> - `CacheErrorHandler` dựa trên `LoggingCacheErrorHandler` của Spring, ghi thêm lý do lỗi, không in chồng lỗi.
> - Test "lấy từ Redis" chứng minh bằng **đánh tráo** câu trả lời đã cất rồi hỏi lại, không đếm số lần gọi provider: không cần spy bean, nên các class test cache dùng chung context với test toàn luồng và xoá cache trước / sau mỗi test.
>
> **Bẫy đã gặp khi làm 3.4:**
> 1. **BUG-PLACE-003, bộ ghi cache ghi ở nền:** `DefaultRedisCacheWriter.put` giao việc ghi cho một tác vụ nền khi `supportsAsyncRetrieve()` (Lettuce) và cờ `asynchronousWrites` (bật sẵn ở đường khởi tạo mặc định). Test đỏ 2 lần trong ~10 lần chạy, chưa bao giờ đỏ khi chạy một mình, không phụ thuộc thứ tự class. Tìm ra bằng cách đọc bytecode (`javap -c`) vì tài liệu không nói rõ; ép lộ bằng test 40 lần tìm liên tiếp (`TC-PLACE-106`, đỏ 2/2 khi chưa sửa). Sửa bằng `RedisCacheWriterConfigurer.immediateWrites()`.
> 2. **Tách commit sửa lỗi khỏi commit đang làm dở:** lỗi lộ ra giữa Commit 6, trong khi `CacheConfig` và một file test đã lẫn cả hai việc. Cách làm: tạm gỡ phần thời tiết khỏi hai file đó (giữ bản đầy đủ ở ngoài), chạy test của trạng thái "chỉ có sửa lỗi", đưa commit `fix`, rồi trả lại phần thời tiết và chạy build toàn bộ. Không dùng `git stash` vì Claude chỉ được chạy lệnh Git chỉ đọc.
> 3. **Đoán chữ ký API theo trí nhớ:** `LoggingCacheErrorHandler` nhận `org.apache.commons.logging.Log` (không phải SLF4J) và `logCacheError(Supplier<String>, RuntimeException)`; đoán sai làm biên dịch đỏ một lần. `javap -p` trên jar trong `~/.gradle/caches` cho câu trả lời trong vài giây.
> 4. **Test dừng container làm bẩn context dùng chung:** `@DirtiesContext(AFTER_CLASS)` là bắt buộc, và mỗi class như vậy tốn một lần khởi động ứng dụng. Gom mọi kịch bản "Redis tắt" vào một class.
> 5. **Redis dừng trên cùng máy bị từ chối kết nối ngay**, nên test không chứng minh được thời gian chờ 1 giây; nó chỉ phát huy khi máy chủ Redis không trả lời. Ghi rõ trong `01-platform.md` phần G thay vì nói quá.
> 6. **Thiếu `@ServiceConnection` thì test vẫn xanh trên máy có docker compose đang chạy:** ứng dụng lặng lẽ dùng Redis ở `localhost:6379`. `TC-PLAT-036` kiểm cổng của kết nối đúng là cổng ngẫu nhiên của container.
> 7. **Gradle bỏ qua test đã xanh:** lặp lại một lệnh `test --tests` không đổi gì thì không chạy lại ("BUILD SUCCESSFUL in 1s"); muốn chạy lại để bắt lỗi lúc có lúc không phải dùng `cleanTest test`.
>
> **Việc cho Task 3.5:** quãng đường của một ngày hiện chưa cache (mock tính tức thì); khi Task 3.8 nối OSRM thì thêm `RouteCache` theo cùng mẫu (bean riêng, khoá = chuỗi toạ độ làm tròn).
> **Việc cho Task 3.8:** provider thật không phải biết gì về cache; nhưng khoá tìm địa điểm bỏ dấu nên "chợ hàn" và "cho han" nhận cùng kết quả Photon trong 24 giờ, xét lại có cần giữ dấu trong khoá không. Lỗi của provider (timeout, 5xx) là ngoại lệ nên **không** bị cất vào cache; "stale-while-error" của design 7.3 cần đọc bản cũ đã hết hạn, Spring Cache không hỗ trợ sẵn, cần thiết kế riêng nếu làm.
> **Việc cho Task 8.1:** rate limit của `/places/search` tính **trước** cache hay sau cache (lượt trúng cache có tính vào hạn mức không), quyết định khi làm.

---

### Task 3.5 — Quãng đường trong ngày

Nhánh: `feat/T3.5-day-route` · Test: phần mới "Quãng đường trong ngày" trong `07-place.md` (mã từ `TC-PLACE-107`). Mỗi commit một việc, code + test cùng commit, số file ghi trong ngoặc vuông.

Kết quả của task (người dùng chưa thấy gì mới, giao diện ở Task 3.7 Mốc 3): `GET /api/v1/trips/{tripId}/days/{dayId}/route` trả khoảng cách và thời gian di chuyển giữa các hoạt động có địa điểm của một ngày. Chỉ backend, không migration, không dependency mới, không key message mới.

```
Mốc 0 — docs (main): PR #20 của Task 3.4; design.md 10.2 "Quy ước Route" (kiểu số, tổng, chặng 0, một phương tiện);
        bảng commit này

Commit 1 — feat(route): add travel legs to the map provider                                         [4 file]
        provider/map/dto/RouteLeg (distanceMeters, durationSeconds), MapProvider.route(List<Coordinate>): n điểm →
        n−1 chặng theo đúng thứ tự; MockMapProvider.route: đường chim bay × 1,3, tốc độ 30 km/h, làm tròn tới mét
        và giây; MockMapProviderTest: đúng công thức, số chặng = số điểm − 1, hỏi lại ra cùng kết quả, hai điểm
        trùng nhau → 0. Chưa ai gọi cho tới Commit 2 (điểm tách (b) của A.2)

Commit 2 — feat(route): add day route endpoint                                                      [6 file]
        dto/response/RouteLegResponse + DayRouteResponse, service/RouteService.forDay(tripId, dayId): kiểm chuyến
        đi và ngày (404), đọc hoạt động kèm địa điểm, gọi MapProvider.route, ghép id hoạt động, cộng tổng; không
        @Transactional. TripDayController GET /{dayId}/route (@PreAuthorize canView). RouteServiceTest: 3 hoạt
        động có địa điểm → 2 chặng đúng id và tổng; chuyến đi không tồn tại; ngày không thuộc chuyến đi.
        TripDayControllerTest: 200, 401, 403, 404, 400 (dayId không phải số)

Commit 3 — feat(route): skip activities without a place                                             [2 file]
        RouteService: chỉ lấy hoạt động có địa điểm; A (có) → B (không) → C (có) → một chặng A→C. RouteServiceTest:
        hoạt động không có địa điểm ở đầu / giữa / cuối ngày; hai hoạt động cùng địa điểm → chặng 0

Commit 4 — feat(route): answer an empty route without asking the map                                [2 file]
        RouteService: ngày có 0 hoặc 1 địa điểm → legs rỗng, tổng 0, không gọi provider. RouteServiceTest: ngày
        trống; 1 địa điểm; nhiều hoạt động nhưng chỉ 1 có địa điểm; provider không bị gọi

Commit 5 — test(route): add day route flow integration test                                         [1 file]
        integration/DayRouteFlowIntegrationTest (HTTP + MySQL thật): kéo thả đổi thứ tự → chặng đổi theo; bỏ địa
        điểm → chặng mất; chuyến đi của người khác → 403; đếm 4 câu SQL, không tăng theo số hoạt động
```

> **Quyết định khi duyệt bảng commit 3.5 (2026-10-03)** — chi tiết ở design.md 10.2 "Quy ước Route":
> - Hoạt động không có địa điểm nằm giữa hai hoạt động có địa điểm: backend trả một chặng nối hai hoạt động có địa điểm (A→C). Giao diện tự quyết cách hiện, dựa vào `fromActivityId` / `toActivityId`.
> - Hai hoạt động liền nhau ở cùng một địa điểm: vẫn có chặng, 0 m và 0 giây. Số chặng luôn = số hoạt động có địa điểm − 1.
> - `distanceMeters`, `durationSeconds` là số nguyên; tổng = tổng các chặng đã làm tròn (cộng tay các chặng luôn khớp tổng). Đổi sang "8,4 km" / "25 phút" là việc của giao diện.
> - **Một phương tiện**, không có tham số `mode`. Mock: 30 km/h. Thêm `mode` sau này không làm hỏng client cũ.
> - Endpoint đặt trong `TripDayController` (đường dẫn nằm dưới `/trips/{tripId}/days`); service mới `RouteService` là class không interface (CLAUDE.md rule 5). Không có mapper: response ghép từ chặng của provider và id hoạt động, không có entity nào được đổi thành DTO.
> - Chia lại so với bản 2 mốc cũ: method của provider đứng riêng; mỗi quy tắc (bỏ qua hoạt động không có địa điểm; không hỏi nguồn khi dưới 2 điểm) một commit.
>
> **Điểm hở tạm thời:** sau Commit 2, ngày có hoạt động không có địa điểm trả 500; Commit 3 đóng lại và có test.
>
> **Việc cho Task 3.7 (Mốc 3):** chặng A→C bắc qua một hoạt động không có địa điểm thì hiện ở đâu (mốc đang ghi "giữa hai thẻ liền nhau cùng có địa điểm", khi đó chặng này không hiện mà vẫn nằm trong tổng): hỏi chủ dự án. Chặng 0 m thì ẩn. Endpoint tính theo từng ngày: quyết định chỉ tải ngày đang mở hay mọi ngày. Ghi rõ con số là ước lượng.
> **Việc cho Task 3.8 (Mốc 3):** OSRM trả quãng đường theo đường thật, thời gian theo *profile* (vận tốc gán cho từng loại đường của OpenStreetMap, không có dữ liệu kẹt xe). Đọc lại tài liệu ở đầu task: máy chủ công cộng chạy profile nào (theo hiểu biết lúc lập kế hoạch: chỉ ô tô; không có profile xe máy), giới hạn số điểm mỗi lần gọi. OSRM lỗi → 503 hay 200 với `legs` rỗng: quyết định khi làm. Thêm `RouteCache` theo mẫu Task 3.4.

---

### Task 3.6 — Giao diện: địa điểm và bản đồ

Nhánh: `feat/T3.6-place-map-ui` · Trước khi code: chủ dự án dựng mockup Stitch theo UI_GUIDE 15.3 (ba cột, hộp thoại có tìm địa điểm, bước "Điểm đến" của wizard). Điều kiện mỗi commit: `npm run lint` + `npm run build` xanh, bài `MT-UI` mới trong `06-itinerary-ui.md`.

```
Mốc 1 — feat(frontend): pick a place in the activity form
        types/place.ts, api/places.ts, features/places/PlaceSearchField (ô tìm, danh sách gợi ý nằm TRONG hộp thoại,
        chờ 300ms sau khi ngừng gõ), chọn → POST /places → placeId vào form; chip tên địa điểm + "×" (clearPlace);
        ActivityCard thêm hàng địa chỉ; loại hoạt động gợi ý theo category của địa điểm

Mốc 2 — feat(frontend): add the day map to the trip detail
        dependency leaflet + react-leaflet (package.json trong commit này), components/map/TripMap.tsx (tải lười),
        tile CartoDB Positron + dòng ghi nguồn, marker giọt nước theo màu loại hoạt động + số thứ tự,
        đường nối theo thứ tự trong ngày, cột thứ ba dính khi cuộn (360px, từ 1280px là 420px), khung bản đồ `isolate`
        (bẫy BUG-UI-002); ngày chưa có địa điểm nào → bản đồ ở điểm đến của chuyến đi + câu hướng dẫn

Mốc 3 — feat(frontend): link activity cards and map markers
        store nhỏ (Zustand) giữ activity đang rê chuột; rê thẻ → marker phóng to; bấm marker → cuộn tới thẻ
        (đích cuộn là phần tử không dính, có scroll-margin — bẫy Task 2.6)

Mốc 4 — feat(frontend): show the map in a tab on small screens
        dưới 1024px: hai tab "Lịch trình" / "Bản đồ" dưới dải chip ngày; sửa vùng chạm < 44px (ghi nợ Task 2.7)

Mốc 5 — feat(frontend): add a place that is not in the search results
        "Không tìm thấy? Tự thêm địa điểm": tên + bấm lên bản đồ lấy toạ độ → POST /places/manual

Mốc 6 — feat(frontend): pick the trip destination from place search
        bước "Điểm đến" của wizard và hộp sửa chuyến đi: tìm → destinationName + destinationLat / Lng, bản đồ nhỏ
        có một marker; types/trip.ts, schemas (có đủ cả hai toạ độ hoặc bỏ cả hai)
```

**Nhớ:** khoá truy vấn mới không lồng dưới `['trip', id]` (mọi lần sửa activity sẽ kéo theo tải lại và kéo thả sẽ huỷ chúng). `ActivityFormDialog` (198 dòng) và `DragDropContainer` (476 dòng) tách bớt trước khi thêm, bằng commit `refactor` riêng nếu cần.

---

### Task 3.7 — Giao diện: thời tiết, quãng đường, ngày đã qua

Nhánh: `feat/T3.7-weather-route-ui` · Điều kiện commit như 3.6.

```
Mốc 1 — feat(frontend): add the weather strip under the map
        types/weather.ts, api/weather.ts, components/weather/WeatherStrip.tsx: mỗi ngày một ô (icon, cao / thấp,
        xác suất mưa); chuyến đi chưa có điểm đến → câu mời chọn điểm đến; ngày ngoài 16 ngày tới → "Chưa có dự báo"

Mốc 2 — HOÃN (2026-10-02, cùng cảnh báo ngoài trời của Task 3.3, design rule 14.21): viền `warning` ở ô ngày và
        chip thời tiết trên thẻ hoạt động. Giữ số mốc để các chỗ dẫn tới Mốc 3, 4, 5 không lệch

Mốc 3 — feat(frontend): show travel distance between activities
        api/routes.ts, đoạn nối "25 phút · 8,4 km" giữa hai thẻ liền nhau cùng có địa điểm (nằm trong từng hàng,
        ẩn khi đang kéo); tải lại sau khi kéo thả, đổi hoặc bỏ địa điểm

Mốc 4 — feat(frontend): mark past days and today on the trip page
        (design rule 14.22) lib tính "hôm nay" theo múi giờ của tài khoản; ngày đã qua nhạt hơn + nhãn "Đã qua",
        ngày hôm nay có nhãn "Hôm nay"; mở chuyến đi đang diễn ra thì vào ngày hôm nay thay vì Ngày 1.
        Vị trí và kiểu nhãn: hỏi chủ dự án trước khi code

Mốc 5 — feat(frontend): ask to complete a trip after its last day
        mở chuyến đi đã qua ngày cuối, trạng thái còn DRAFT / PLANNED / ONGOING → hộp xác nhận;
        "Hoàn thành" → PATCH /trips/{id}/status sang COMPLETED; "Để sau" → không hỏi lại tới lần đăng nhập sau;
        không khoá gì: lịch trình vẫn sửa được
```

> Mốc 4 và 5 thêm ngày 2026-10-01 khi thảo luận "hoạt động đã qua ngày thì xử lý thế nào". Không cần backend mới: dùng `timezone` trong `GET /users/me` và endpoint đổi trạng thái đã có.

---

### Task 3.8 — Provider thật: OpenStreetMap và Open-Meteo

Nhánh: `feat/T3.8-real-providers` · Chốt 2026-10-01: làm ngay sau 3.7 để tìm được mọi địa điểm và có dự báo thật, không đợi Phase 8. Các dịch vụ đều miễn phí, không cần API key, có giới hạn sử dụng hợp lý: **đọc lại điều khoản và tài liệu chính thức của từng dịch vụ ở đầu task** (thông tin dưới đây ghi theo hiểu biết lúc lập kế hoạch). Test không gọi mạng thật (CLAUDE.md rule 24): dùng máy chủ giả trả JSON mẫu. Test: `07-place.md`, `08-weather.md`.

```
Mốc 1 — feat(weather): add open-meteo weather provider
        provider/weather/OpenMeteoWeatherProvider (@ConditionalOnProperty app.providers.weather=open-meteo),
        gọi HTTP qua RestClient, đổi mã thời tiết WMO → condition của DailyForecast;
        test với máy chủ giả: JSON mẫu → đúng DailyForecast; 5xx / quá thời gian → PROVIDER_UNAVAILABLE

Mốc 2 — feat(place): add openstreetmap place search provider
        provider/map/OsmMapProvider (app.providers.map=osm): search bằng Photon (cho phép gợi ý khi đang gõ;
        Nominatim cấm dùng cho việc này), lookup theo externalId bằng Nominatim, header User-Agent định danh
        ứng dụng; địa điểm lưu với provider OSM; test với máy chủ giả

Mốc 3 — feat(route): add osrm routing to the osm provider
        OsmMapProvider.route gọi OSRM (các chặng giữa hai điểm liên tiếp); test với máy chủ giả

Mốc 4 — feat(provider): add time limit, retry and circuit breaker to real providers
        design.md 7.3: giới hạn 3 giây, thử lại 2 lần, ngắt mạch khi lỗi nhiều;
        thời tiết lỗi → trang chuyến đi vẫn mở, ô thời tiết ghi "tạm thời không có dự báo";
        tìm địa điểm lỗi → 503 PROVIDER_UNAVAILABLE; kiểm Resilience4j có bản chạy với Spring Boot 4 trước khi thêm
        dependency (version ở libs.versions.toml)

Mốc 5 — docs: README + .env.example hướng dẫn bật provider thật; bài kiểm tra thủ công: bật osm / open-meteo,
        tìm một địa điểm không có trong dữ liệu mock, xem dự báo thật của một chuyến đi trong 16 ngày tới
```

**Nhớ:** hạn mức gọi `/places/search` theo người dùng (design 8.2) tới Task 8.1 mới có; trước đó cache của Task 3.4 và độ trễ 300ms ở ô tìm kiếm là thứ giữ cho app không gọi dịch vụ công cộng quá nhiều. Không bật provider thật trên bản deploy công khai trước khi có rate limit.

> ✅ Hết Phase 3 → tick `[x] Phase 3` trong CLAUDE.md. Trước khi sang Phase 4: rà lại Phase 4 theo quy ước A.2 "Rà soát theo phase".

---

## PHASE 4 — Chia sẻ & Phân quyền

> ⏳ **Chưa rà theo quy ước A.2 "Chia commit"** — rà lại đầu phase trước khi làm; mốc / `Commit:` bên dưới là kiểu cũ, chỉ để tham khảo.

Đọc trước: **design.md mục 6.2 (ma trận quyền)** — đọc kỹ, đây là phần đáng giá nhất trong CV.

### Task 4.1 — TripMember

Nhánh: `feat/T4.1-trip-members`

```
1. model/TripMember.java + MemberRole + MemberStatus
2. V9__create_trip_members.sql
3. TripMemberRepository: findByTripIdAndUserId
4. service/SharingService: invite(), acceptInvite(), changeRole(), remove()
5. controller: 5 endpoint members
6. mail template: mời tham gia chuyến đi
```

**Commit:** `feat(sharing): add trip member invitation`

---

### Task 4.2 — Permission Evaluator ⭐

Nhánh: `feat/T4.2-permission-evaluator`

```
1. security/permission/TripPermissionEvaluator.java    ĐÃ CÓ từ Task 2.1 (chỉ kiểm owner) → mở rộng: member theo role, share link
2. Cache kết quả vào Redis TTL 5 phút, key perm:{userId}:{tripId}
3. Evict cache khi thay đổi member
4. Rà @PreAuthorize ở TOÀN BỘ endpoint (trip/day/activity đã có từ Phase 2), bổ sung cho member/share/comment/expense
   (từ Task 3.2: xét lại quy tắc "địa điểm MANUAL chỉ người tạo gắn được" khi chuyến đi có EDITOR — design rule 14.19)
5. test ĐẦY ĐỦ MA TRẬN: với từng vai trò (OWNER/EDITOR/VIEWER/người lạ) × từng hành động
```

Đây là task đáng để viết nhiều test nhất. Một bảng test parameterized đúng theo ma trận ở design.md mục 6.2 là thứ rất đáng show khi phỏng vấn.

**Commit:**
```
feat(security): extend trip permission evaluator with members and cache
feat(security): apply authorization to all trip endpoints
test(security): add full permission matrix tests
```

---

### Task 4.3 — Share link công khai

Nhánh: `feat/T4.3-share-links`

```
1. model/ShareLink.java + V10__create_share_links.sql
2. service: create, revoke, resolve(token)
3. controller/PublicTripController: GET /public/trips/{shareToken}  — không cần auth
4. SecurityConfig: permitAll cho /api/v1/public/**
5. test: link hết hạn → 404; link đã revoke → 404; link VIEW không sửa được
```

**Commit:** `feat(sharing): add public share links`

---

### Task 4.4 — Comment + giao diện chia sẻ

Nhánh: `feat/T4.4-comments-ui`

```
Backend: Comment entity + V11 + CRUD
Frontend: features/sharing/MembersPanel.tsx, ShareLinkDialog.tsx, CommentThread.tsx
          pages/PublicTripPage.tsx
```

**Commit:** `feat(comment): add trip comments` + `feat(frontend): add sharing panel and public trip view`

---

## PHASE 5 — Realtime WebSocket

> ⏳ **Chưa rà theo quy ước A.2 "Chia commit"** — rà lại đầu phase trước khi làm; mốc / `Commit:` bên dưới là kiểu cũ, chỉ để tham khảo.

Đọc trước: **design.md mục 11**

### Task 5.1 — Hạ tầng WebSocket

Nhánh: `feat/T5.1-websocket-setup`

```
1. dependency spring-boot-starter-websocket
2. config/WebSocketConfig.java              STOMP endpoint /ws, broker /topic, /queue
3. websocket/WebSocketAuthInterceptor.java  đọc JWT từ CONNECT frame
4. test: kết nối không token → bị từ chối
```

**Commit:** `feat(realtime): add websocket stomp configuration with jwt auth`

---

### Task 5.2 — Broadcast sự kiện

Nhánh: `feat/T5.2-trip-events`

```
1. websocket/event/TripEvent.java (+ các subtype)
2. service phát Spring ApplicationEvent khi tạo/sửa/xoá activity
3. websocket/TripEventListener.java   @TransactionalEventListener(phase = AFTER_COMMIT)
4. Kiểm tra quyền trước khi subscribe /topic/trips/{id}
5. test: rollback transaction → KHÔNG có event nào được gửi
```

**Commit:** `feat(realtime): broadcast activity changes after commit`

---

### Task 5.3 — Presence + chống ghi đè

Nhánh: `feat/T5.3-presence-locking`

```
1. Presence tracking bằng Redis SET, TTL 30s, heartbeat từ client
2. @Version trên Trip và Activity → xử lý OptimisticLockException → 409 STALE_VERSION
   (rà soát 2026-10-01: hiện hai lần sửa cùng lúc trả 500, cả ở PATCH /trips/{id})
3. Frontend: stores/tripCollabStore.ts, hooks/useTripSocket.ts, AvatarStack.tsx
4. test thủ công: mở 2 trình duyệt, sửa ở A → B cập nhật trong < 1s
```

**Commit:** `feat(realtime): add presence tracking and optimistic locking`

---

## PHASE 6 — Premium & Stripe

> ⏳ **Chưa rà theo quy ước A.2 "Chia commit"** — rà lại đầu phase trước khi làm; mốc / `Commit:` bên dưới là kiểu cũ, chỉ để tham khảo.

Đọc trước: **design.md mục 9 (feature gating) và mục 12 (luồng Stripe)**

### Task 6.1 — Quota Service

Nhánh: `feat/T6.1-quota-service`

```
1. common/constant/PlanLimits.java          bảng hạn mức theo design mục 9
2. service/QuotaService.java                assertCanCreateTrip, assertCanAddActivity, assertCanInviteMember
3. Gọi QuotaService ở đầu các service tương ứng
4. exception/QuotaExceededException → 402 kèm upgradeUrl
5. GET /users/me trả kèm quota hiện tại (đã dùng / tối đa)
6. test: user FREE tạo trip thứ 4 → 402
7. POST /trips/{id}/clone (canView + quota) — gán từ "Việc còn treo" Phase 2 (2026-09-30)
```

Làm task này **trước** khi tích hợp Stripe, vì gating chạy được với `plan` đổi tay trong DB.

**Commit:** `feat(billing): add quota service and plan limits`

---

### Task 6.2 — Payment provider mock

Nhánh: `feat/T6.2-payment-mock`

```
1. provider/payment/PaymentProvider.java interface
2. provider/payment/MockPaymentProvider.java   trả URL giả, nâng cấp plan ngay
3. model/Subscription.java + V12__create_subscriptions.sql
4. service/SubscriptionService
5. controller/BillingController: /plans, /checkout-session, /subscription
```

**Commit:** `feat(billing): add payment provider abstraction with mock`

---

### Task 6.3 — Stripe thật + webhook idempotent ⭐

Nhánh: `feat/T6.3-stripe-integration`

```
1. dependency stripe-java, config/properties/StripeProperties
2. provider/payment/StripePaymentProvider.java
3. model/PaymentEvent.java + V13__create_payment_events.sql   (event_id UNIQUE)
4. service/StripeWebhookService.java
5. controller: POST /billing/webhook  (SecurityConfig permitAll + bỏ CSRF)
6. test: gửi CÙNG MỘT event 2 lần → chỉ 1 subscription được tạo
7. scheduler/SubscriptionSyncScheduler.java   đối soát mỗi 6h
```

**Test local:**
```bash
stripe login
stripe listen --forward-to localhost:8080/api/v1/billing/webhook
# copy webhook signing secret vào .env
stripe trigger checkout.session.completed
```
Thẻ test: `4242 4242 4242 4242`, ngày hết hạn bất kỳ trong tương lai.

**Commit (3 mốc):**
```
feat(billing): add stripe checkout session
feat(billing): add idempotent stripe webhook handler
test(billing): add webhook duplicate event test
```

---

### Task 6.4 — Giao diện nâng cấp

Nhánh: `feat/T6.4-billing-ui`

```
pages/BillingPage.tsx, BillingSuccessPage.tsx
components/UpgradeModal.tsx        hiện khi API trả 402 QUOTA_EXCEEDED
```

**Commit:** `feat(frontend): add billing page and upgrade modal`

---

## PHASE 7 — Expense, AI, Export

> ⏳ **Chưa rà theo quy ước A.2 "Chia commit"** — rà lại đầu phase trước khi làm; mốc / `Commit:` bên dưới là kiểu cũ, chỉ để tham khảo.

### Task 7.1 — Expense + chia tiền

Nhánh: `feat/T7.1-expenses`

```
1. Expense, ExpenseShare + V14
2. service/ExpenseService: CRUD + validate tổng share = amount
3. service/SettlementService: thuật toán tối giản số giao dịch (greedy: gộp người nợ nhiều nhất với người được nợ nhiều nhất)
4. GET /expenses/summary, /expenses/settlement
5. test thuật toán settlement với 3-4 người
6. GET /trips/{id}/summary (canView): số ngày, số activity, tổng chi phí so với budget; quãng đường khi đã có route
   (Phase 3) — gán từ "Việc còn treo" Phase 2 (2026-09-30)
```

**Commit:** `feat(expense): add expense tracking and settlement algorithm`

---

### Task 7.2 — AI gợi ý lịch trình

Nhánh: `feat/T7.2-ai-suggestion`

```
1. provider/ai/AiProvider.java + MockAiProvider + ClaudeAiProvider
2. service/AiItineraryService: kiểm tra Premium → quota ngày → cache theo promptHash → gọi → parse JSON → validate schema → enrich toạ độ
3. model/AiSuggestionLog + V15
4. POST /ai/suggest-itinerary, POST /ai/trips/{id}/apply-suggestion
5. test: user FREE gọi → 402; JSON trả về hỏng → retry 1 lần rồi 503
```

**Commit:** `feat(ai): add itinerary suggestion with caching and quota`

---

### Task 7.3 — Export PDF / ICS

Nhánh: `feat/T7.3-export`

```
1. service/ExportService: PDF (OpenPDF hoặc Flying Saucer + template HTML), ICS (ical4j)
2. GET /trips/{id}/export?format=pdf|ics   chỉ owner + Premium
```

**Commit:** `feat(export): add pdf and ics export for premium users`

---

## PHASE 8 — Hoàn thiện & Deploy

> ⏳ **Chưa rà theo quy ước A.2 "Chia commit"** — rà lại đầu phase trước khi làm; mốc / `Commit:` bên dưới là kiểu cũ, chỉ để tham khảo.

### Task 8.1 — Rate limiting

Nhánh: `feat/T8.1-rate-limiting`

```
1. dependency bucket4j-redis
2. config/RateLimitConfig + filter/RateLimitFilter
3. Áp bảng hạn mức ở design mục 8.2, trả 429 + header X-RateLimit-*
4. test: gọi login 6 lần → lần 6 nhận 429
5. (ghi nợ Task 2.7) xoay refresh token phải nguyên tử: hai request cùng lúc với cùng một token hiện đều thành
   công. UPDATE có điều kiện `revoked_at IS NULL` và đòi đúng 1 dòng, hoặc khoá dòng khi đọc; tương tự cho
   token verify / reset dùng một lần
```

**Commit:** `feat(security): add redis based rate limiting`

---

### Task 8.2 — Admin dashboard

Nhánh: `feat/T8.2-admin`

```
Backend: AdminController + AdminService (stats, users, payment events, retry)
Frontend: pages/admin/* + guard theo role ADMIN
```

> ⚠️ **Nợ kỹ thuật hoãn từ 1.3 (quyết định 2026-09-24), phải xử lý trong task này khi thêm block/xoá user:**
> `AuthServiceImpl.refresh()`/`logout()` gọi `stored.getUser()` (lazy). User bị **soft-delete** thì `@SQLRestriction` làm Hibernate
> không load được → `EntityNotFoundException` → **500** thay vì 401. Sửa: lấy `userId` qua proxy (`stored.getUser().getId()`,
> không trigger load), `userRepository.findById(userId)` rỗng → `revokeAll(userId)` + `InvalidRefreshTokenException("user gone")`.
> Đồng thời AdminService khi block/xoá user phải gọi `refreshTokenService.revokeAll(userId)` để phiên đang sống chết ngay,
> không đợi access token hết hạn 15 phút. Test: soft-delete user bằng SQL rồi refresh → 401, 0 phiên sống.

**Commit:** `feat(admin): add admin dashboard endpoints and pages`

---

### Task 8.3 — Test coverage + dọn nợ kỹ thuật

Nhánh: `chore/T8.3-test-coverage`

```
1. Thêm plugin `jacoco` vào build.gradle, cấu hình jacocoTestCoverageVerification:
   ngưỡng 70% LINE cho package **/service/**, loại trừ dto/model/config khỏi báo cáo
   nối tasks: test.finalizedBy(jacocoTestReport), check.dependsOn(jacocoTestCoverageVerification)
2. Bổ sung test cho các service còn thiếu
3. Bật hibernate.generate_statistics ở local, tìm và sửa N+1 ở GET /trips/{id}
4. Rà lại checklist "Những lỗi tôi không muốn gặp lại" trong CLAUDE.md
5. Frontend — đồng bộ phiên giữa các tab (hoãn từ 1.5): `BroadcastChannel('auth')` phát `login`/`logout`,
   tab khác nhận `logout` → clearSession + queryClient.clear(); nhận `login` → refreshAccessToken()
6. Frontend — form đổi mật khẩu (hoãn từ 1.5): `register('password', { deps: ['confirmPassword'] })` ở RegisterForm
   (tương tự `newPassword` ở ResetPasswordPage) để lỗi "không khớp" tự mất khi sửa ô mật khẩu cho khớp
7. Frontend — test (từ 2.5): Vitest + Testing Library nếu task frontend trước chưa thêm; ưu tiên luồng trong
   docs/testing/06-itinerary-ui.md (trùng giờ → allowOverlap, force=true, kéo thả trả về chỗ cũ khi lỗi).
   Truy vấn theo chữ / role, không theo class, để test sống qua lần làm lại giao diện
8. ~~Frontend — màu chủ đạo (từ 2.5): khai báo `@theme`~~ — **đã làm ở Task 2.6** (`src/styles/tokens.css`, xem UI_GUIDE.md)
9. (ghi nợ Task 2.7) danh sách chuyến đi: thêm `id` làm khoá sắp xếp phụ để phân trang ổn định khi sắp theo cột
   có giá trị trùng; `?page=` vượt quá số trang → đưa về trang cuối thay vì hiện "Chưa có chuyến đi nào"
```

**Commit:** `chore(test): add jacoco coverage gate` + `perf(trip): fix n+1 query on trip detail`

---

### Task 8.4 — Dockerfile + CI

Nhánh: `chore/T8.4-ci-cd`

```
1. backend/Dockerfile      multi-stage gradle:9-jdk21 → eclipse-temurin:21-jre-alpine,
                           copy build.gradle + settings + gradle/ trước để cache layer,
                           non-root user, HEALTHCHECK
2. frontend/Dockerfile     build → nginx
3. frontend/nginx.conf     serve SPA + proxy /api
4. docker-compose.prod.yml
5. .github/workflows/ci.yml    actions/setup-java@v4 + gradle/actions/setup-gradle@v4
                               → ./gradlew build → ./gradlew jacocoTestReport
6. .github/workflows/cd.yml    build image → push GHCR → ssh deploy
```

> Kế thừa từ 1.3, khi tạo `application-prod.yml` và `nginx.conf`:
> - `app.jwt.cookie-secure: true` (cookie refresh chỉ đi qua HTTPS); `local` để `false`.
> - `ClientInfo.from()` tin `X-Forwarded-For` đầu tiên. Nginx phải **ghi đè** header này (`proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;`) để client không tự điền IP giả; backend đặt `server.forward-headers-strategy: native`.
> - `refresh_tokens` chưa có dọn dẹp: thêm scheduler xoá dòng `revoked_at`/`expires_at` quá 30 ngày (design 5.2).

**Nghiệm thu:** mở PR bất kỳ → thấy CI chạy và tick xanh trên GitHub.

**Commit:** `chore(ci): add github actions build and test pipeline`

---

### Task 8.5 — Deploy + README cuối cùng

Nhánh: `docs/T8.5-final-readme`

```
1. Thuê VPS (hoặc Railway/Render), cài docker, trỏ domain, bật HTTPS
2. Tạo tài khoản demo: demo@tripplanner.com / Demo@12345 (có sẵn 2 trip mẫu)
3. README hoàn chỉnh:
   - Ảnh GIF demo thao tác kéo thả + realtime 2 tab
   - Sơ đồ kiến trúc (mermaid)
   - Link live demo + link Swagger
   - Bảng tính năng
   - Hướng dẫn chạy: docker compose up
   - Những gì đã học / điểm kỹ thuật đáng chú ý
```

> **Từ 2.5 (2026-09-30):** chủ dự án dự định làm lại giao diện theo một mẫu chung sau khi xong các phase. Làm lại giao diện
> **trước** task này (sau 8.3, khi đã có test frontend và màu `@theme`) để ảnh chụp / GIF trong README là bản cuối.

**Commit:** `docs: add final readme with demo links and screenshots`

> 🎉 Xong. Ghim repo lên trang GitHub cá nhân.

---

## B. Bảng theo dõi tiến độ

| Phase | Task | Xong | Ngày |
|---|---|:--:|---|
| 0 | 0.1 Repo + Spring Initializr | ☑ | 2026-09-13 |
| 0 | 0.2 Docker Compose | ☑ | 2026-09-16 |
| 0 | 0.3 Config + Flyway | ☑ | 2026-09-16 |
| 0 | 0.4 ApiResponse + Exception + Swagger | ☑ | 2026-09-17 |
| 0 | 0.5 Init frontend | ☑ | 2026-09-18 |
| 1 | 1.1 User entity | ☑ | 2026-09-19 |
| 1 | 1.2 Đăng ký | ☑ | 2026-09-20 |
| 1 | 1.3 JWT + refresh rotation | ☑ | 2026-09-23 |
| 1 | 1.4 Verify email + reset password | ☑ | 2026-09-24 |
| 1 | 1.5 Auth UI | ☑ | 2026-09-25 |
| 2 | 2.1 Trip CRUD | ☑ | 2026-09-26 |
| 2 | 2.2 TripDay auto-gen | ☑ | 2026-09-28 |
| 2 | 2.3 Activity + trùng giờ | ☑ | 2026-09-29 |
| 2 | 2.4 Reorder | ☑ | 2026-09-30 |
| 2 | 2.5 Itinerary UI | ☑ | 2026-09-30 |
| 2 | 2.6 Làm lại giao diện theo UI_GUIDE | ☑ | 2026-10-01 |
| 2 | 2.7 Sửa lỗi sau rà soát Phase 1–2 | ☑ | 2026-10-01 |
| 3 | 3.1 Tìm địa điểm | ☑ | 2026-10-01 |
| 3 | 3.2 Gắn địa điểm vào hoạt động | ☑ | 2026-10-02 |
| 3 | 3.3 Thời tiết của chuyến đi | ☑ | 2026-10-03 |
| 3 | 3.4 Redis cache | ☑ | 2026-10-03 |
| 3 | 3.5 Quãng đường trong ngày | ☐ | |
| 3 | 3.6 UI: địa điểm + bản đồ | ☐ | |
| 3 | 3.7 UI: thời tiết + quãng đường + ngày đã qua | ☐ | |
| 3 | 3.8 Provider thật (OSM, Open-Meteo) | ☐ | |
| 4 | 4.1 Trip members | ☐ | |
| 4 | 4.2 Permission evaluator | ☐ | |
| 4 | 4.3 Share links | ☐ | |
| 4 | 4.4 Comment + UI | ☐ | |
| 5 | 5.1 WebSocket setup | ☐ | |
| 5 | 5.2 Broadcast events | ☐ | |
| 5 | 5.3 Presence + locking | ☐ | |
| 6 | 6.1 Quota service | ☐ | |
| 6 | 6.2 Payment mock | ☐ | |
| 6 | 6.3 Stripe + webhook | ☐ | |
| 6 | 6.4 Billing UI | ☐ | |
| 7 | 7.1 Expense + settlement | ☐ | |
| 7 | 7.2 AI suggestion | ☐ | |
| 7 | 7.3 Export | ☐ | |
| 8 | 8.1 Rate limiting | ☐ | |
| 8 | 8.2 Admin | ☐ | |
| 8 | 8.3 Coverage + N+1 | ☐ | |
| 8 | 8.4 CI/CD | ☐ | |
| 8 | 8.5 Deploy + README | ☐ | |

---

## C. Nguyên tắc cho người mới

1. **Không gộp task.** Mỗi lần chỉ giải quyết một vấn đề. Gộp 3 task thì khi lỗi bạn không biết lỗi ở đâu.
2. **Chạy thử sau mỗi file.** Đừng viết 10 file rồi mới chạy lần đầu.
3. **Commit khi code đang chạy được**, không commit khi đang dở.
4. **Đọc log lỗi từ dòng đầu tiên**, không phải dòng cuối. `Caused by:` ở cuối cùng mới là nguyên nhân gốc.
5. **Khi Claude Code sinh code, đọc hiểu trước khi chấp nhận.** Nếu có dòng không hiểu, hỏi lại. Bạn sẽ phải giải thích repo này khi phỏng vấn.
6. **Kẹt quá 45 phút ở một lỗi** → dừng, viết ra: đang làm gì, mong đợi gì, thực tế ra gì, đã thử gì. Thường viết xong là tự ra đáp án.
7. **Không nhảy sang tính năng mới khi tính năng cũ chưa có test.** Nợ test sẽ chồng lên rất nhanh.

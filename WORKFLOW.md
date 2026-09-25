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
4. Code theo đúng thứ tự file được liệt kê
5. Chạy kiểm tra ở phần "Nghiệm thu"
6. Commit theo các mốc được ghi sẵn
7. git push + mở Pull Request trên GitHub + tự merge
8. Tick [x] vào bảng theo dõi ở mục cuối file này
```

> **Ai làm gì:** các bước Git (3, 6, 7) do **tôi tự chạy**. Claude Code không tự tạo nhánh, không tự `git add` / `commit` / `push`, không mở PR — chỉ đưa ra lệnh tạo nhánh (nếu đang sai nhánh) và danh sách commit đề xuất (file cần add + message) để tôi tự commit. Chi tiết ở `CLAUDE.md` mục 9.

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

**Nghiệm thu** (backend chạy `local`; dùng `curl.exe`, body chỉ ASCII vì Git Bash/PowerShell 5.1 làm hỏng UTF-8 trong `-d`):
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

Đọc trước: **design.md mục 5.2 (trips, trip_days, activities), mục 14 (business rules 1–5)**

### Task 2.1 — Trip CRUD

Nhánh: `feat/T2.1-trip-crud`

```
1. model/Trip.java + enums TripStatus, TripVisibility
2. V5__create_trips_table.sql
3. TripRepository (+ Specification cho filter)
4. dto/request/CreateTripRequest, UpdateTripRequest
5. dto/response/TripResponse, TripSummaryResponse
6. mapper/TripMapper
7. service/TripService + Impl
8. controller/TripController
9. test: tạo trip, list phân trang, sửa, soft delete, user khác truy cập → 403
```

**Nhớ:** `endDate >= startDate`, tối đa 60 ngày → validate ở service, ném `BusinessRuleException`. Danh sách trip chỉ lấy `deletedAt IS NULL` và của chính user.

**Commit:**
```
feat(trip): add trip entity and migration
feat(trip): add trip crud endpoints
test(trip): add trip service tests
```

---

### Task 2.2 — TripDay tự sinh

Nhánh: `feat/T2.2-trip-days`

```
1. model/TripDay.java + V6__create_trip_days.sql + repository
2. service/TripDayService: generateDays(trip), reconcileDays(trip, oldRange, newRange)
3. TripService.create() gọi generateDays trong cùng transaction
4. TripService.update() gọi reconcileDays
5. controller: GET /trips/{id}/days, PATCH /trips/{id}/days/{dayId}
6. test QUAN TRỌNG: đổi ngày trip làm mất ngày đang có activity → phải chặn nếu không có force=true
```

**Commit:** `feat(trip): auto generate trip days from date range`

---

### Task 2.3 — Activity CRUD + kiểm tra trùng giờ

Nhánh: `feat/T2.3-activity-crud`

```
1. model/Activity.java + ActivityType + @Version
2. V7__create_activities.sql
3. ActivityRepository: findByTripDayIdOrderByOrderIndex, tìm activity chồng giờ
4. dto + mapper
5. service/ActivityService: create, update, delete, validateNoTimeConflict
6. controller/ActivityController
7. test: thêm 2 activity trùng giờ → 409 ACTIVITY_TIME_CONFLICT; allowOverlap=true → 201
```

**Commit:**
```
feat(activity): add activity crud with time conflict validation
test(activity): add time overlap test cases
```

---

### Task 2.4 — Sắp xếp lại thứ tự (reorder)

Nhánh: `feat/T2.4-activity-reorder`

```
1. dto/request/ReorderActivitiesRequest   List<{activityId, dayId, orderIndex}>
2. ActivityService.reorder()              1 transaction, batch update
3. PUT /trips/{tripId}/activities/reorder
4. test: kéo activity từ ngày 1 sang ngày 2, thứ tự đúng sau khi reorder
```

**Cơ chế orderIndex:** đánh số cách nhau 1000. Chèn giữa hai activity 1000 và 2000 → index 1500, không phải update cả danh sách. Khi khoảng cách < 10 thì normalize lại cả ngày.

**Commit:** `feat(activity): add batch reorder endpoint`

---

### Task 2.5 — Giao diện lịch trình

Nhánh: `feat/T2.5-itinerary-ui`

```
1. src/api/tripApi.ts, activityApi.ts
2. src/features/trips/TripList.tsx, TripCard.tsx, CreateTripWizard.tsx
3. src/features/itinerary/DayTimeline.tsx, ActivityCard.tsx, ActivityFormDialog.tsx
4. src/features/itinerary/DragDropContainer.tsx   (dnd-kit, gọi API reorder)
5. src/pages/TripsPage.tsx, TripDetailPage.tsx
```

**Commit:** `feat(frontend): add trip list and itinerary editor`

> ✅ Hết Phase 2 → **đây là mốc "sản phẩm dùng được"**. Chụp màn hình bỏ vào README.

---

## PHASE 3 — Place, Map, Weather (provider mock)

Đọc trước: **design.md mục 7 (Provider Abstraction) và mục 8 (Cache)**

### Task 3.1 — Tầng provider + mock data

Nhánh: `feat/T3.1-provider-abstraction`

```
1. provider/map/MapProvider.java            interface
2. provider/map/dto/PlaceResult.java, Coordinate.java
3. resources/mock/places.json               ~200 địa điểm VN tự soạn hoặc lấy từ OSM export
4. provider/map/MockMapProvider.java        @ConditionalOnProperty
5. provider/weather/WeatherProvider.java + MockWeatherProvider.java
6. application-local.yml: app.providers.map=mock, weather=mock
7. test: MockWeatherProvider trả kết quả ỔN ĐỊNH với cùng input (dùng seed)
```

**Vì sao mock phải deterministic:** để test integration không bị đỏ ngẫu nhiên.

**Commit:** `feat(provider): add map and weather provider abstraction with mock impl`

---

### Task 3.2 — Place: search, snapshot, gắn vào activity

Nhánh: `feat/T3.2-place-service`

```
1. model/Place.java + V8__create_places.sql + repository (unique provider+externalId)
2. service/PlaceService: search (gọi provider), getOrCreateSnapshot()
3. Activity thêm quan hệ tới Place
4. controller/PlaceController: GET /places/search, POST /places/manual
```

**Commit:** `feat(place): add place search and snapshot persistence`

---

### Task 3.3 — Redis cache

Nhánh: `feat/T3.3-redis-cache`

```
1. thêm dependency spring-boot-starter-data-redis
2. config/RedisConfig.java, config/CacheConfig.java   TTL theo từng cache name
3. common/constant/CacheNames.java
4. @Cacheable trên PlaceService.search, WeatherService.forecast
5. @CacheEvict khi cập nhật trip
6. test: gọi search 2 lần → provider chỉ được gọi 1 lần (verify bằng Mockito)
```

**Commit:**
```
feat(cache): add redis configuration and cache names
feat(cache): cache place search and weather forecast
```

---

### Task 3.4 — Weather cho trip + bản đồ frontend

Nhánh: `feat/T3.4-weather-and-map`

```
Backend: service/WeatherService, GET /weather/trips/{tripId}, cảnh báo activity ngoài trời
Frontend: cài leaflet react-leaflet, components/map/TripMap.tsx, components/weather/WeatherStrip.tsx
          hover activity → highlight marker
```

**Commit:** `feat(weather): add trip forecast endpoint` + `feat(frontend): add map and weather panel`

---

## PHASE 4 — Chia sẻ & Phân quyền

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
1. security/permission/TripPermissionEvaluator.java    canView(tripId, user), canEdit(), isOwner()
2. Cache kết quả vào Redis TTL 5 phút, key perm:{userId}:{tripId}
3. Evict cache khi thay đổi member
4. Thêm @PreAuthorize vào TOÀN BỘ endpoint của trip/day/activity/expense
5. test ĐẦY ĐỦ MA TRẬN: với từng vai trò (OWNER/EDITOR/VIEWER/người lạ) × từng hành động
```

Đây là task đáng để viết nhiều test nhất. Một bảng test parameterized đúng theo ma trận ở design.md mục 6.2 là thứ rất đáng show khi phỏng vấn.

**Commit:**
```
feat(security): add trip permission evaluator
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
2. @Version trên Activity → xử lý OptimisticLockException → 409 STALE_VERSION
3. Frontend: stores/tripCollabStore.ts, hooks/useTripSocket.ts, AvatarStack.tsx
4. test thủ công: mở 2 trình duyệt, sửa ở A → B cập nhật trong < 1s
```

**Commit:** `feat(realtime): add presence tracking and optimistic locking`

---

## PHASE 6 — Premium & Stripe

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

### Task 7.1 — Expense + chia tiền

Nhánh: `feat/T7.1-expenses`

```
1. Expense, ExpenseShare + V14
2. service/ExpenseService: CRUD + validate tổng share = amount
3. service/SettlementService: thuật toán tối giản số giao dịch (greedy: gộp người nợ nhiều nhất với người được nợ nhiều nhất)
4. GET /expenses/summary, /expenses/settlement
5. test thuật toán settlement với 3-4 người
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

### Task 8.1 — Rate limiting

Nhánh: `feat/T8.1-rate-limiting`

```
1. dependency bucket4j-redis
2. config/RateLimitConfig + filter/RateLimitFilter
3. Áp bảng hạn mức ở design mục 8.2, trả 429 + header X-RateLimit-*
4. test: gọi login 6 lần → lần 6 nhận 429
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
| 2 | 2.1 Trip CRUD | ☐ | |
| 2 | 2.2 TripDay auto-gen | ☐ | |
| 2 | 2.3 Activity + trùng giờ | ☐ | |
| 2 | 2.4 Reorder | ☐ | |
| 2 | 2.5 Itinerary UI | ☐ | |
| 3 | 3.1 Provider abstraction | ☐ | |
| 3 | 3.2 Place service | ☐ | |
| 3 | 3.3 Redis cache | ☐ | |
| 3 | 3.4 Weather + map UI | ☐ | |
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

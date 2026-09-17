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
- Tên nhánh: `feat/T1.2-jwt-authentication`
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

`.env.example`:
```
DB_URL=jdbc:mysql://localhost:3306/tripplanner?useSSL=false&serverTimezone=UTC
DB_USER=tripuser
DB_PASSWORD=trippass
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

```bash
npm create vite@latest frontend -- --template react-ts
cd frontend && npm install
npm install -D tailwindcss @tailwindcss/vite
npm install axios @tanstack/react-query react-router-dom zustand react-hook-form zod @hookform/resolvers
```

**File tạo:** `frontend/src/api/client.ts` (axios instance, baseURL từ `import.meta.env.VITE_API_URL`), `frontend/.env.example`, cấu hình proxy `/api` trong `vite.config.ts`.

**Nghiệm thu:** `npm run dev` → trang Vite mở được, gọi thử `/api/v1/ping` từ frontend thấy dữ liệu.

**Commit:** `chore(frontend): init vite react typescript project`

> ✅ Hết Phase 0 → sửa `CLAUDE.md` mục 7 tick `[x] Phase 0`, commit `docs: mark phase 0 complete`

---

## PHASE 1 — Xác thực (Auth)

Đọc trước: **design.md mục 6 (Security) và mục 10.2 (Auth endpoints)**

Đây là phase khó nhất với người mới. Chia thành 5 task nhỏ, **đừng làm gộp**.

### Task 1.1 — Entity User + migration + repository

Nhánh: `feat/T1.1-user-entity`

**Thứ tự file:**
```
1. model/BaseEntity.java                    @MappedSuperclass: id, createdAt, updatedAt (@CreationTimestamp)
2. model/enums/Role.java, Plan.java, UserStatus.java
3. model/User.java
4. resources/db/migration/V2__create_users_table.sql
5. repository/UserRepository.java           findByEmail, existsByEmail
6. test: repository/UserRepositoryTest.java (@DataJpaTest + Testcontainers)
```

**Luồng tư duy:** viết SQL migration **trước hay sau** entity đều được, nhưng hai bên phải khớp tuyệt đối vì `ddl-auto=validate` sẽ báo lỗi khi khởi động nếu lệch. Đây là cơ chế bảo vệ, đừng tắt nó đi.

**Nghiệm thu:** app khởi động không lỗi validate, `UserRepositoryTest` xanh, kiểm tra bảng `users` tồn tại trong MySQL.

**Commit:**
```
feat(user): add user entity and migration
test(user): add user repository tests
```

---

### Task 1.2 — Đăng ký + mã hoá mật khẩu

Nhánh: `feat/T1.2-registration`

**Thứ tự file:**
```
1. dto/request/RegisterRequest.java     @Email, @NotBlank, @Size(min=8) + regex mật khẩu mạnh
2. dto/response/UserResponse.java
3. mapper/UserMapper.java               MapStruct
4. config/SecurityConfig.java           tạm thời permitAll cho /api/v1/auth/**, /actuator/health, /actuator/info; khai báo bean PasswordEncoder
   ⚠️ Trước bước này: thêm lại spring-boot-starter-security + spring-boot-starter-security-test vào build.gradle (đã tạm gỡ ở Task 0.3)
   ⚠️ Cùng lúc: thêm handler AccessDeniedException → 403 FORBIDDEN (trả ErrorResponse) vào GlobalExceptionHandler + test (hoãn từ Task 0.4)
5. service/AuthService.java + AuthServiceImpl.java
6. controller/AuthController.java
7. test/service/AuthServiceTest.java    trùng email → ném EmailAlreadyExistsException
8. test/controller/AuthControllerTest.java  @WebMvcTest, body sai → 400 kèm details
```

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

**Nghiệm thu:** đăng ký thành công → 201; đăng ký lại cùng email → 409 `EMAIL_ALREADY_EXISTS`; kiểm tra DB thấy `password_hash` bắt đầu bằng `$2a$12$`.

**Commit:**
```
feat(auth): add user registration endpoint
test(auth): add registration service and controller tests
```

---

### Task 1.3 — Đăng nhập + JWT + refresh token rotation

Nhánh: `feat/T1.3-jwt-authentication`

**Thứ tự file:**
```
1. model/RefreshToken.java + V3__create_refresh_tokens.sql + RefreshTokenRepository
2. config/properties/JwtProperties.java     secret, accessTtl, refreshTtl
3. security/JwtTokenProvider.java           generate, parse, validate
4. security/CustomUserDetails.java
5. security/CustomUserDetailsService.java
6. security/JwtAuthenticationFilter.java    OncePerRequestFilter
7. security/RestAuthenticationEntryPoint.java + RestAccessDeniedHandler.java
8. config/SecurityConfig.java               cập nhật: stateless, thêm filter, khai báo route public
9. dto/request/LoginRequest.java, dto/response/AuthResponse.java
10. service/RefreshTokenService.java
11. AuthService: login(), refresh(), logout()
12. controller: thêm 3 endpoint
13. test: login sai mật khẩu → 401; gọi /users/me không token → 401; refresh token đã revoke → revoke toàn bộ
```

**Luồng login:**
```
POST /auth/login {email, password}
  → AuthenticationManager.authenticate()
  → nếu chưa verify email → 403 EMAIL_NOT_VERIFIED
  → sinh access token (JWT 15 phút)
  → sinh refresh token random 64 ký tự
      → lưu SHA-256 hash vào DB kèm expiresAt, userAgent, ip
      → set vào httpOnly cookie, SameSite=Lax, Secure ở prod
  → trả { accessToken, expiresIn, user }
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

**Nghiệm thu:** chuỗi curl login → lấy accessToken → gọi `/users/me` có token → 200; không token → 401; refresh 2 lần với cùng token cũ → lần 2 bị 401 và session bị xoá sạch.

**Commit (4 mốc):**
```
feat(auth): add jwt token provider and security config
feat(auth): add login endpoint with refresh token
feat(auth): add refresh token rotation and theft detection
test(auth): add authentication integration tests
```

---

### Task 1.4 — Xác thực email + gửi mail bất đồng bộ

Nhánh: `feat/T1.4-email-verification`

**Thứ tự file:**
```
1. model/VerificationToken.java + V4__create_verification_tokens.sql + repository
2. config/AsyncConfig.java              @EnableAsync, dùng virtual thread executor
3. service/MailService.java             @Async, Thymeleaf template
4. resources/templates/mail/verify-email.html, reset-password.html
5. AuthService: verifyEmail(), resendVerification(), forgotPassword(), resetPassword()
6. controller: thêm 4 endpoint
7. test: token hết hạn → 400; token đã dùng → 400
```

**Lưu ý:** token lưu dạng hash, TTL 24h cho verify / 1h cho reset, dùng xong đánh dấu `usedAt`. Mail gửi `@Async` để không chặn response — nhưng nhớ rằng `@Async` không chạy nếu gọi nội bộ trong cùng class.

**Nghiệm thu:** đăng ký → mở MailHog thấy mail → bấm link → `emailVerified = true` → login được.

**Commit:**
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

**Nghiệm thu:** đăng ký → verify → login → vào được `/trips` (trang rỗng); F5 vẫn giữ đăng nhập nhờ refresh cookie; logout xoá sạch.

**Commit:**
```
feat(frontend): add auth pages and token refresh interceptor
```

> ✅ Hết Phase 1 → tick `[x] Phase 1` trong CLAUDE.md.
> **Lúc này hãy viết README lần đầu**: mô tả dự án, cách chạy, ảnh chụp màn hình login.

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
| 0 | 0.5 Init frontend | ☐ | |
| 1 | 1.1 User entity | ☐ | |
| 1 | 1.2 Đăng ký | ☐ | |
| 1 | 1.3 JWT + refresh rotation | ☐ | |
| 1 | 1.4 Verify email + reset password | ☐ | |
| 1 | 1.5 Auth UI | ☐ | |
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

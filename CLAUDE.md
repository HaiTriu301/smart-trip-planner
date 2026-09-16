# CLAUDE.md

Hướng dẫn cho Claude Code khi làm việc trên repository này.

---

## 1. Dự án

**Smart Trip Planner** — web app lên kế hoạch du lịch: tạo lịch trình theo ngày, gắn địa điểm lên bản đồ, xem thời tiết, chia sẻ & đồng chỉnh sửa real-time, theo dõi chi phí, nâng cấp Premium qua Stripe.

- Backend: **Spring Boot 4.1.x / Java 21** / MySQL 8 / Redis 7 — thư mục `backend/`
- Frontend: React 18 + Vite + TypeScript + Tailwind — thư mục `frontend/`
- **`design.md` ở thư mục gốc là nguồn sự thật.** Trước khi implement bất kỳ tính năng nào, đọc mục tương ứng trong `design.md`. Nếu yêu cầu của tôi mâu thuẫn với `design.md`, dừng lại và hỏi, đừng tự chọn.
- **`WORKFLOW.md` là lịch trình thực hiện.** Task được làm theo đúng thứ tự trong đó. Nếu tôi nhờ làm một task thuộc phase sau khi phase trước chưa xong, nhắc tôi.

> ⚠️ Dự án dùng **Spring Boot 4 / Spring Framework 7 / Spring Security 7 / Jakarta EE 11 / Jackson 3**.
> Không sinh code theo cú pháp Spring Boot 3.x. Cụ thể: không dùng `WebSecurityConfigurerAdapter`, không dùng `com.fasterxml.jackson.databind` / `com.fasterxml.jackson.core` (Jackson 3 là `tools.jackson.databind` / `tools.jackson.core`) — **ngoại lệ:** annotation (`@JsonInclude`, `@JsonProperty`, `@JsonIgnore`...) vẫn nằm ở `com.fasterxml.jackson.annotation` vì Jackson 3 tiếp tục dùng `jackson-annotations` 2.x, không dùng API đã bị gỡ ở 4.0. Nếu không chắc một API còn tồn tại ở Boot 4 hay không, kiểm tra tài liệu chính thức trước khi dùng.

---

## 2. Lệnh thường dùng

```bash
# Hạ tầng (mysql, redis, mailhog)
docker compose up -d mysql redis mailhog

# Backend
cd backend
./gradlew bootRun --args='--spring.profiles.active=local'
./gradlew test                               # unit + slice test
./gradlew build                              # compile + toàn bộ test + đóng gói jar
./gradlew check jacocoTestCoverageVerification   # test + kiểm tra ngưỡng coverage
./gradlew test --tests "*TripServiceTest"    # chạy 1 class
./gradlew flywayInfo                         # trạng thái migration
./gradlew clean build --refresh-dependencies # khi đổi dependency mà IDE không nhận
./gradlew dependencies --configuration runtimeClasspath   # xem cây dependency

# Frontend
cd frontend
npm run dev        # http://localhost:5173
npm run build
npm run lint
npm run test       # vitest
npm run gen:api    # sinh type TS từ OpenAPI schema của backend

# Toàn hệ thống
docker compose up --build
```

Swagger: `http://localhost:8080/swagger-ui.html` — MailHog: `http://localhost:8025`

---

## 3. Nguyên tắc code — BẮT BUỘC

### Kiến trúc
1. Controller **chỉ** làm: nhận request DTO → gọi service → bọc `ApiResponse`. Không có `if` business, không truy cập repository.
2. Service chứa toàn bộ business rule, đánh dấu `@Transactional` ở đây (không ở controller/repository).
3. **Không bao giờ** trả Entity ra ngoài controller. Luôn map sang DTO bằng MapStruct.
4. Không dùng `@Autowired` trên field. Dùng constructor injection qua `@RequiredArgsConstructor`.
5. Service khai báo interface + `Impl` cho các service có business phức tạp (Trip, Activity, Subscription, Auth). Service đơn giản có thể là class trực tiếp.

### Database
6. **Mọi thay đổi schema đều qua Flyway migration mới.** Không sửa file migration đã commit. `ddl-auto` luôn là `validate` ở **mọi** profile, kể cả `test` (test dùng Testcontainers + Flyway nên schema thật luôn có sẵn, `validate` giúp bắt lệch entity/migration ngay trong test).
7. Đặt tên migration `V{n}__{snake_case_mo_ta}.sql`. Kiểm tra số thứ tự lớn nhất hiện có trước khi tạo file.
8. Không dùng `EAGER` fetch. Mặc định `LAZY`, dùng `@EntityGraph` hoặc `JOIN FETCH` khi cần.
9. Tiền tệ dùng `BigDecimal` + `DECIMAL(15,2)`. Không dùng `double`/`float`.
10. Soft delete qua `deleted_at` + `@SQLRestriction`.

### Lỗi & validate
11. Không `throw new RuntimeException(...)`. Dùng exception trong `exception/` với `ErrorCode` tương ứng (bảng mã lỗi ở `design.md` mục 10.3). Controller/service **không** tự dựng `ErrorResponse` — chỉ ném exception, `GlobalExceptionHandler` chuyển thành `ErrorResponse`.
12. Không bắt exception rồi nuốt. Nếu bắt, phải log kèm ngữ cảnh hoặc bọc lại thành `AppException`.
13. Mọi request DTO phải có Bean Validation annotation. Validate business (ví dụ trùng giờ) nằm ở service, không ở DTO.
14. Message lỗi trả về người dùng viết tiếng Việt, đặt trong `messages.properties` (UTF-8). `ErrorCode` chỉ giữ `httpStatus` + `messageKey`, **không** chứa câu chữ. Log viết tiếng Anh.

### Bảo mật
15. Mọi endpoint thao tác trên trip phải có `@PreAuthorize` dùng `TripPermissionEvaluator`. Không tự viết lại logic kiểm quyền trong service.
16. Không bao giờ tin `userId` từ request body — luôn lấy từ `SecurityContext`.
17. Không log password, token, JWT, Stripe secret, payload thẻ.
18. Secret chỉ đọc từ biến môi trường. Không hardcode, kể cả trong test.

### Third-party
19. Mọi lời gọi ra ngoài phải đi qua interface trong `provider/`. Service **không** import SDK của Stripe/Google/Anthropic trực tiếp.
20. Mặc định môi trường local là `mock` cho tất cả provider. Code mới phải chạy được khi chưa có API key nào.

### Realtime
21. Broadcast WebSocket chỉ được phát **sau khi transaction commit** (`@TransactionalEventListener(phase = AFTER_COMMIT)`).
22. Entity có sửa đồng thời (Activity, Trip) phải có `@Version`.

### Test
23. Mỗi API mới cần tối thiểu: 1 test happy path + 1 test lỗi phân quyền + 1 test validate.
24. Test không phụ thuộc thứ tự chạy, không dùng DB thật, không gọi mạng ngoài.
25. Không viết test chỉ để tăng coverage (test getter/setter, test mock trả về chính mock).

### Build (Gradle)
26. Dùng **Groovy DSL** (`build.gradle`, `settings.gradle`), không dùng Kotlin DSL. Cú pháp dependency dạng chuỗi: `implementation 'group:artifact:version'`.
27. Version của dependency khai báo tập trung ở `gradle/libs.versions.toml` (version catalog), không rải số version khắp `build.gradle`.
28. Không khai báo version cho các thư viện đã nằm trong BOM của Spring Boot — để plugin `io.spring.dependency-management` tự quản. Chỉ ghi version cho thư viện bên thứ ba (jjwt, mapstruct, bucket4j, stripe...).
29. Thứ tự annotation processor bắt buộc: `lombok` → `lombok-mapstruct-binding` → `mapstruct-processor`. Sai thứ tự thì MapStruct sinh mapper rỗng (xem design.md mục 3.1).
30. Không sửa `gradle-wrapper.properties` thủ công. Nâng Gradle bằng `./gradlew wrapper --gradle-version <x.y>`.

---

## 4. Quy trình khi tôi giao một task

Làm theo đúng thứ tự:

1. **Đọc `design.md`** mục liên quan. Nếu task chưa có trong design → nói ra và đề xuất bổ sung trước khi code.
2. **Liệt kê file sẽ tạo/sửa** trước khi bắt tay (danh sách ngắn, không cần giải thích dài).
3. **Migration trước** nếu có thay đổi schema.
4. **Code theo thứ tự**: entity → repository → dto → mapper → service → controller → test.
5. **Viết test** cùng lúc, không để lại sau.
6. **Chạy `./gradlew build`** và sửa cho tới khi xanh.
7. **Báo cáo**: file đã đổi, endpoint mới (method + path), cách test thủ công bằng curl hoặc Swagger.
8. **Đề xuất commit** (không tự commit, không tự tạo nhánh — xem mục 9): liệt kê lệnh `git add <file>` + `git commit -m "..."` cho từng mốc để tôi tự chạy.

---

## 5. Định dạng tôi mong muốn ở output

- Đưa **code hoàn chỉnh chạy được ngay**, không viết `// TODO: implement`, không bỏ trống thân method, không viết `... phần còn lại tương tự`.
- File mới: ghi rõ đường dẫn đầy đủ trước mỗi block code.
- File sửa: nêu rõ đoạn nào thay đổi, không in lại toàn bộ file nếu file dài.
- Không giải thích những kiến thức Spring cơ bản trừ khi tôi hỏi.
- Trả lời bằng tiếng Việt; comment trong code viết tiếng Anh.

---

## 6. Cấu trúc thư mục

```
smart-trip-planner/
├── design.md              ← nguồn sự thật, đọc trước khi code
├── CLAUDE.md
├── README.md
├── docker-compose.yml
├── .env.example
├── backend/
│   ├── build.gradle
│   ├── settings.gradle
│   ├── gradle/libs.versions.toml     ← version catalog, khai báo version tập trung
│   ├── gradlew, gradlew.bat, gradle/wrapper/
│   └── src/main/java/com/trieu/tripplanner/
│       ├── common/ config/ security/ exception/
│       ├── model/ repository/ dto/ mapper/
│       ├── service/ provider/ controller/ websocket/ scheduler/
│   ├── src/main/resources/
│   │   ├── application.yml, application-local.yml, application-prod.yml
│   │   ├── db/migration/V*.sql
│   │   ├── messages.properties
│   │   └── mock/places.json
│   └── src/test/resources/
│       └── application-test.yml      ← chỉ nằm trên test classpath, không đóng vào jar
└── frontend/
    └── src/{api,components,features,hooks,layouts,pages,stores,types,lib}
```

---

## 7. Trạng thái dự án

Cập nhật mục này sau mỗi phase hoàn thành.

- [ ] Phase 0 — Setup: project, docker-compose, Flyway, Swagger, ApiResponse, exception handler
- [ ] Phase 1 — Auth: JWT + refresh rotation, verify email, reset password
- [ ] Phase 2 — Trip + Itinerary: CRUD, auto-gen TripDay, Activity + reorder
- [ ] Phase 3 — Place + Weather (mock provider + Redis cache)
- [ ] Phase 4 — Sharing + Permission (member, share link, PermissionEvaluator)
- [ ] Phase 5 — Realtime WebSocket + optimistic locking
- [ ] Phase 6 — Premium + Stripe (quota, checkout, webhook idempotent)
- [ ] Phase 7 — Expense + AI suggest + Export PDF/ICS
- [ ] Phase 8 — Rate limit, Admin, coverage, CI/CD, deploy

---

## 8. Những lỗi tôi không muốn gặp lại

Khi review code, kiểm tra lại các điểm này:

- Trả Entity thay vì DTO ở controller
- Thiếu `@Transactional` khi ghi nhiều bảng trong một thao tác
- Query N+1 ở `GET /trips/{id}` (kiểm tra số câu SQL trong log)
- Quên `@PreAuthorize` trên endpoint thao tác trip
- Sửa file migration cũ thay vì tạo file mới
- Gọi Stripe/Map SDK trực tiếp trong service thay vì qua provider
- Broadcast WebSocket trước khi commit
- `catch (Exception e) { }` rỗng
- Hardcode `localhost:5173` trong code backend thay vì đọc từ `app.frontend-url`
- Dùng `String` cho tiền hoặc `double` cho amount

---

## 9. Git

> **Tôi tự thao tác Git.** Claude **không** tự chạy `git checkout -b` / `git switch -c` / `git branch`, `git add`, `git commit`, `git push`, không tạo Pull Request — kể cả khi task đã xong và build xanh.
> Claude chỉ được dùng lệnh Git **chỉ đọc** (`git status`, `git diff`, `git log`, `git branch --show-current`...) để kiểm tra.
> Nếu đang đứng sai nhánh so với task trong `WORKFLOW.md`, Claude nhắc tôi và đưa lệnh tạo nhánh để tôi tự chạy.
> Thay đổi chỉ thuộc cài đặt/tài liệu, không đụng code chức năng (docker-compose, `.env.example`, `.gitignore`, `*.md`) → commit thẳng lên `main`, không đề xuất tạo nhánh. Phạm vi chính xác ở `WORKFLOW.md` mục A.2. Có đụng `backend/src`, `frontend/src`, dependency/build hoặc migration → vẫn phải có nhánh + PR.
> Khi kết thúc task (bước 7 mục 4), Claude đưa ra **danh sách commit đề xuất**: mỗi commit gồm các file cần `git add` + commit message theo Conventional Commits, đúng các mốc commit ghi trong `WORKFLOW.md`.

- Branch: `feat/`, `fix/`, `refactor/`, `chore/`, `docs/` + mã task + mô tả kebab-case, ví dụ `feat/T1.2-registration`
- Commit: Conventional Commits — `feat(trip): add activity reorder endpoint`
- Không commit `.env`, `uploads/`, `backend/build/`, `backend/.gradle/`, `node_modules/`
- **Có commit** `gradlew`, `gradlew.bat` và `gradle/wrapper/` — thiếu wrapper thì người khác clone về không build được
- Không commit khi test đang đỏ

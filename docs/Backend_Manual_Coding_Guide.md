# Backend Manual Coding Guide — R1 Core Marketplace

> Roadmap để **tự tay gõ lại toàn bộ backend** (Spring Boot 4.1.2 · Java 21 · PostgreSQL 16), từng bước một, mỗi bước có lệnh kiểm chứng và một checkpoint commit.
>
> Đọc kèm: [`specs/001-home-services-marketplace/plan.md`](../specs/001-home-services-marketplace/plan.md) · [`tasks.md`](../specs/001-home-services-marketplace/tasks.md) · [`contracts/api-contracts.md`](../specs/001-home-services-marketplace/contracts/api-contracts.md) · [Data Model](Data_Model_Home_Services_Marketplace_v0.1.md) · [Detailed Design](Detailed_Design_Home_Services_Marketplace_v0.2.md) · [SRS](SRS_Home_Services_Marketplace_v0.3.md) · [HLR](HLR_Home_Services_Marketplace_v0.2.md) · [System Design](System_Design_Home_Services_Marketplace_v0.2.md)

---

## 0. Cách dùng guide này

### 0.1 Nguyên tắc

1. **Schema trước, entity sau.** Database là hợp đồng. Viết Flyway migration trước, rồi mới viết JPA entity khớp nó. `spring.jpa.hibernate.ddl-auto = validate` sẽ báo lỗi ngay khi boot nếu lệch — đó là tính năng, không phải phiền phức.
2. **Mỗi step là một đơn vị hoàn chỉnh.** Cuối mỗi step có mục **Xong khi** — chạy lệnh đó, thấy đúng kết quả mới sang step tiếp.
3. **Commit tại mỗi Checkpoint.** Không commit giữa step đang dở.
4. **Không copy file có sẵn.** Nếu bạn đang đọc lại repo này: hãy xoá/tạo mới file thay vì sửa dần, để tự chịu trách nhiệm từng dòng.
5. **Không ước lượng thời gian.** Tiến độ đo bằng số checkpoint đã pass.

### 0.2 Thứ tự bắt buộc

Dependency một chiều. Không nhảy cóc — mỗi phase chỉ dùng thứ đã có ở phase trước.

```
Phase 1  Walking skeleton (Gradle → migration → error model → shared kernel → security)
Phase 2  Identity & Access            (bảng: user_account, refresh_token)
Phase 3  Catalogue & Geo               (bảng: geo_area, service_category, service)
Phase 4  Customer profile & addresses  (bảng: customer_profile, customer_address)
Phase 5  Service requests              (bảng: service_request, request_version)
Phase 6  Provider self-service         (bảng: provider_profile, provider_service, provider_area, provider_availability)
Phase 7  Discovery & selection         (bảng: discovery_run, discovery_candidate, provider_selection)
Phase 8  Quotation · scheduling · booking
Phase 9  Fulfillment (state machine)
Phase 10 Payment & review
Phase 11 Admin & audit
Phase 12 Hardening + tests             (task T055/T056 còn nợ trong tasks.md)
Phase 13 ELK structured logging        (spec 002, độc lập — có thể bỏ qua ở lượt đầu)
```

**MVP path** = Phase 1→8. Đó là đường giao dịch tối thiểu chạy được (task T057 gọi là "smoke validation").

### 0.3 Quy ước kiến trúc (áp dụng cho MỌI module)

Một package = một domain. Trong mỗi domain, chỉ có đúng 4 loại file (cộng `dto/`):

| Layer | Vai trò | Quy tắc |
|---|---|---|
| `@RestController` | Chỉ HTTP: map route, nhận `@RequestBody`, gọi service, trả DTO. **Không** business logic, **không** truy cập repository. | Mỏng nhất có thể |
| `@Service` | Toàn bộ use case + `@Transactional`. Mọi kiểm tra quyền sở hữu, mọi chuyển trạng thái nằm ở đây. | Nơi duy nhất chứa luật |
| `Repository` | `JpaRepository<Entity, UUID>` + query dẫn xuất tên method. | Không tự viết `@Query` nếu tên method đủ diễn đạt |
| Entity | Ánh xạ bảng. Không lộ ra ngoài package. | Không `@Data` trên entity có quan hệ vòng |
| `dto/` | **Java `record`** cho request/response. | API boundary không bao giờ là entity |

Quy ước bắt buộc khác:
- Mọi lỗi đi qua `ApiException` + `ErrorCodes` — không `throw new RuntimeException("...")`.
- Mọi hành động ghi (create/confirm/accept/pay) có **audit**; các lệnh "đắt" có thêm **idempotency**.
- Thời gian lấy từ `TimeProvider`, không gọi `Instant.now()` rải rác.
- UUID làm khoá chính, `timestamptz` cho thời gian, tiền tệ mặc định `VND`.

---

## PHASE 1 — Walking skeleton

Mục tiêu phase: app boot được, DB rỗng có schema đầy đủ, mọi lỗi trả về JSON thống nhất, request có JWT thì có danh tính. **Chưa có domain nào.**

### Step 1 — Gradle project & dependency

**Tạo**: `backend/build.gradle`, `backend/settings.gradle`

**Viết gì**: Java toolchain 21; plugin `org.springframework.boot 4.1.2` + `io.spring.dependency-management`. Nhóm dependency (ghi đúng nhóm để sau này biết cái nào là dead code):

- Web: `spring-boot-starter-webmvc`, `spring-boot-starter-json`, `spring-boot-starter-validation`
- Persistence: `spring-boot-starter-data-jpa`, `runtimeOnly org.postgresql:postgresql`
- Migration: `spring-boot-starter-flyway`, `org.flywaydb:flyway-database-postgresql`
- Security: `spring-boot-starter-security`
- Docs: `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.2.0`
- JWT: `io.jsonwebtoken:jjwt-api` (compile) + `jjwt-impl`, `jjwt-jackson` (runtimeOnly)
- Lombok: `compileOnly` + `annotationProcessor`
- Test: các `*-test` starter tương ứng

> **Chú ý**: repo hiện tại còn có `spring-boot-starter-data-redis` (+ test starter) nhưng **không một dòng code nào dùng Redis**. Khi tự code, đừng thêm — thêm rồi thì tự hỏi "cái này để làm gì?".

**Xong khi**: `./gradlew build` chạy xong (chưa cần app chạy được).

---

### Step 2 — `application.yaml`

**Tạo**: `backend/src/main/resources/application.yaml`

**Viết gì**: 8 nhóm key, đúng thứ tự này:

```yaml
spring:
  application.name: home_services_marketplace
  datasource:      # jdbc:postgresql://localhost:5432/hsm  (user/pass hsm)
  jpa:
    hibernate.ddl-auto: validate     # schema do Flyway sở hữu
    open-in-view: false              # bắt buộc: không lazy-load ngoài transaction
    properties.hibernate.jdbc.time_zone: UTC
  flyway:
    enabled: true
    locations: classpath:db/migration
server.port: 8080
app:
  jwt:            # secret, access-ttl-minutes: 15, refresh-ttl-days: 7
  security.refresh-cookie:   # name: refresh_token, secure: true, same-site: Lax, path: /api/v1/auth
  time.zone: Asia/Ho_Chi_Minh
```

**Quyết định cần chốt**: `secure: true` nghĩa là cookie chỉ đi qua HTTPS — nhưng `localhost` được Chrome/Firefox coi là secure context nên dev vẫn chạy. Đừng "sửa" thành `false` cho tiện, nếu không bạn sẽ quên bật lại khi deploy.

> **Chú ý**: repo hiện tại có **cấu hình JWT trùng lặp** — `app.jwt.*` và `app.security.jwt.*` với 2 secret khác nhau, chỉ `app.jwt.*` được đọc. Khi tự code, chọn **một** chỗ. Cùng lý do: `app.security.allowed-origins` có trong yaml nhưng `SecurityConfig` hardcode danh sách khác → chọn một nguồn sự thật.

**Xong khi**: app boot, log báo `HikariPool started`, `Flyway ... No migration necessary` (hoặc bảng rỗng).

---

### Step 3 — Flyway `V1__init.sql` (toàn bộ schema)

**Tạo**: `backend/src/main/resources/db/migration/V1__init.sql`

Đây là step **nặng nhất** của phase. Chia nội dung theo phase để không rối:

**3a. Extensions + reference data**
```sql
CREATE EXTENSION IF NOT EXISTS btree_gist;   -- cần cho exclusion constraint ở bảng slot
CREATE EXTENSION IF NOT EXISTS pgcrypto;     -- gen_random_uuid()
```
- `geo_area(code PK · parent_code FK self · name · type CHECK(province|district) · active)`
- `service_category(id uuid PK default gen_random_uuid() · name · slug UNIQUE · description · active · created_at/updated_at timestamptz)`
- `service(id · category_id FK · name · slug UNIQUE · description · active · timestamps)`

**3b. Identity**
- `user_account(id · email · email_norm UNIQUE · password_hash · role CHECK(CUSTOMER|PROVIDER|ADMIN) · status CHECK(ACTIVE|SUSPENDED) · version bigint · timestamps)`
- `refresh_token(id · user_id FK · token_hash UNIQUE · expires_at · revoked_at · timestamps)`

**3c. Customer / Provider profile**
- `customer_profile(id · user_account_id UNIQUE FK · full_name · phone · timestamps)`
- `customer_address(id · customer_id FK · label · address_line · area_code FK geo_area · is_default · deleted_at · timestamps)`
- `provider_profile(id · user_account_id UNIQUE FK · business_name · description · experience_summary · verification_status · timestamps)`
- `provider_service(provider_id · service_id · PK cả hai)`
- `provider_area(provider_id · area_code · PK cả hai)`
- `provider_availability(id · provider_id · day_of_week 0..6 · start_time · end_time · CHECK(start_time < end_time))`

**3d. Request / Discovery**
- `service_request(id · customer_id FK · status · current_version int · timestamps)`
- `request_version(id · request_id FK · version · category_id · service_id · description · address_snapshot · area_code · preferred_start/end timestamptz · budget_amount · budget_currency · UNIQUE(request_id, version))`
- `discovery_run(id · request_id · request_version · no_result_reason · created_at)`
- `discovery_candidate(id · discovery_run_id FK · provider_id · reason_codes jsonb)`
- `provider_selection(id · request_id · request_version · provider_id · status · selected_at · UNIQUE(request_id, request_version))`

**3e. Quotation / Booking / Slot / Payment / Review**
- `quotation(id · request_id · selection_id · provider_id · status · total_amount · currency · timestamps)` + **partial unique** `uq_quote_active_selection ON quotation(selection_id) WHERE status = 'SUBMITTED'`
- `quotation_item(id · quotation_id FK · description · amount)`
- `provider_slot_reservation(id · provider_id · booking_id UNIQUE · start_at · end_at · status)` + **exclusion constraint**:
```sql
ALTER TABLE provider_slot_reservation
  ADD CONSTRAINT ex_provider_active_slot
  EXCLUDE USING gist (provider_id WITH =, tstzrange(start_at, end_at, '[)') WITH &&)
  WHERE (status = 'ACTIVE');
```
- `booking(id · quotation_id UNIQUE FK · customer_id · provider_id · status · scheduled_at · started_at · completed_at · cancelled_at · total_amount · currency · timestamps)`
- `payment_record(id · booking_id · adapter · external_ref · amount · currency · status · recorded_at · timestamps)` + `uq_payment_external ON (adapter, external_ref) WHERE external_ref IS NOT NULL` + `uq_booking_recorded_payment ON (booking_id) WHERE status = 'RECORDED'`
- `review(id · booking_id UNIQUE FK · customer_id · provider_id · rating 1..5 · comment · status · timestamps)`

**3f. Cross-cutting**
- `audit_event(id · actor_id · actor_role · action · entity_type · entity_id · before_summary jsonb · after_summary jsonb · reason · created_at)`
- `idempotency_record(id · actor_id · operation · idem_key · request_hash · status · response_body jsonb · timestamps · UNIQUE(actor_id, operation, idem_key))`

**Vì sao phải làm đúng ở đây** — 4 ràng buộc này thay thế cho rất nhiều `if` trong Java:
| Ràng buộc | Nó bảo vệ điều gì |
|---|---|
| `ex_provider_active_slot` (GiST) | Hai booking không bao giờ trùng giờ một provider, kể cả khi 2 request đến cùng lúc |
| `uq_quote_active_selection` | Một selection chỉ có 1 quotation đang hiệu lực |
| `uq_booking_recorded_payment` | Một booking chỉ được ghi nhận thanh toán một lần |
| `UNIQUE(actor_id, operation, idem_key)` | Nền tảng cho idempotency, chống double-submit |

**Xong khi**: `./gradlew bootRun` → log `Successfully applied 1 migration`; `psql` thấy đủ 24 bảng (`\dt`).

---

### Step 4 — Error model

**Tạo**: `shared/error/{ApiException, ErrorCodes, ErrorResponse, GlobalExceptionHandler}.java`

**Viết gì**
- `ErrorCodes`: class `final` chỉ chứa hằng `String` (NOT_FOUND, CONFLICT, VALIDATION, UNAUTHORIZED, FORBIDDEN, INTERNAL, MALFORMED, INVALID_STATE, SLOT_UNAVAILABLE, IDEMPOTENCY_CONFLICT, ALREADY_SELECTED, EMAIL_EXISTS, INVALID_CREDENTIALS, ACCOUNT_SUSPENDED, NOT_ELIGIBLE, EXPIRED, PAYMENT_ALREADY_RECORDED, REVIEW_EXISTS, REVIEW_NOT_ALLOWED, SERVICE_UNAVAILABLE).
- `ApiException extends RuntimeException`: mang `HttpStatus` + `code`, và các static factory `badRequest / unauthorized / forbidden / notFound / conflict / unprocessable / unavailable`.
- `ErrorResponse`: `record(String code, String message)`.
- `GlobalExceptionHandler` (`@RestControllerAdvice`): map `ApiException` → status tương ứng; `MethodArgumentNotValidException` → 400 `VALIDATION`; `HttpMessageNotReadableException` → 400 `MALFORMED`; `DataIntegrityViolationException` → 409 `CONFLICT`; `NoResourceFoundException` → 404; `Exception` → 500 `INTERNAL`.

**Quyết định**: 5xx log ở mức `error`, 4xx log ở mức `warn` (đừng để lỗi 400 của client làm rác dashboard error-rate sau này).

**Xong khi**: gọi một route không tồn tại → nhận JSON `{"code":"NOT_FOUND", ...}` chứ không phải trang HTML trắng.

---

### Step 5 — Shared kernel

**Tạo**: `shared/time/{TimeProvider,TimeConfig}.java`, `shared/json/JacksonConfig.java`, `shared/audit/{AuditEvent,AuditEventRepository,AuditService}.java`, `shared/idempotency/{IdempotencyRecord,IdempotencyRecordRepository,IdempotencyService}.java`

**Viết gì**
- `TimeConfig` → bean `Clock.systemUTC()`. `TimeProvider` (`@Component`) → `now()`, `zone()`, `inLocal(Instant)`; zone đọc từ `app.time.zone`.
- `JacksonConfig` → khai báo **tường minh** một bean `ObjectMapper` (JavaTimeModule, không ghi timestamp dạng số).
  > **Tại sao**: Spring Boot 4 auto-config `Jackson 3`, trong khi code shared (audit/idempotency/discovery) serialize bằng Jackson 2. Khai báo bean tường minh để hai thế giới không đánh nhau. Đây là loại lỗi chỉ lộ ra lúc runtime — gặp rồi mới thấy đau.
- `AuditService.record(...)`: ghi 1 dòng `audit_event` bất biến, `before/after` serialize từ **summary object** (không phải entity) qua `ObjectMapper`.
- `IdempotencyService`: `Reservation` record + 3 method `reserve / complete / replayBody`; private `handleExisting`.
  - `@Transactional(propagation = Propagation.MANDATORY)` cho `reserve` — nó **phải** nằm trong transaction của caller, nếu không thì rollback của caller không kéo theo idempotency record.
  - Xử lý race: hai request cùng key có thể cùng insert → bắt `DataIntegrityViolationException` rồi đọc lại bản ghi đã có.
  - Phân biệt 3 kết cục: `IN_PROGRESS` (đang chạy) · `COMPLETED` cùng `request_hash` (replay lại response cũ) · cùng key khác hash (`IDEMPOTENCY_CONFLICT`), đây là lỗi client.

**Xong khi**: viết một test nhỏ gọi `TimeProvider.now()` và `serialize` một object — pass.

---

### Step 6 — Security (JWT + filter chain)

**Tạo**: `shared/security/{AuthenticatedUser,JwtService,JwtAuthenticationFilter,RestAuthenticationEntryPoint,RestAccessDeniedHandler,SecurityConfig}.java`

**Viết gì**
- `AuthenticatedUser`: `record(UUID id, UserRole role) implements Principal` — đây là thứ `@AuthenticationPrincipal` inject vào controller.
- `JwtService`: HS256 qua `Keys.hmacShaKeyFor(secret bytes)`; `generateAccessToken(userId, role)` (subject = userId, claim `role`); `parse(token)` → `AuthenticatedUser`.
- `JwtAuthenticationFilter extends OncePerRequestFilter`: đọc `Authorization: Bearer`, parse, set `UsernamePasswordAuthenticationToken(user, null, [ROLE_<name>])` + details; lỗi `JwtException|IllegalArgumentException` thì clear context (đừng throw — để entry point xử lý 401).
- `RestAuthenticationEntryPoint` (401 JSON) + `RestAccessDeniedHandler` (403 JSON).
- `SecurityConfig`: `cors` → `csrf.disable()` → `STATELESS` → `exceptionHandling` → `authorizeHttpRequests` → `addFilterBefore`.

**Ba quyết định quan trọng, sai ở đây rất khó tìm**

1. **JwtAuthenticationFilter không được là `@Component`.** Tạo nó bằng `new JwtAuthenticationFilter(jwtService)` ngay trong `securityFilterChain()`, rồi `addFilterBefore(...)`. Nếu là bean, Spring Boot đăng ký nó **thêm một lần** như servlet filter thường → filter chạy 2 lần.
2. **Thứ tự matcher = thứ tự ưu tiên, cái đầu tiên khớp sẽ thắng.**
   ```
   /api/v1/auth/**                      → permitAll
   GET /api/v1/categories/**, /services/**, /areas/**  → permitAll
   /swagger-ui/**, /v3/api-docs/**      → permitAll
   /api/v1/admin/**                     → hasRole("ADMIN")
   /api/v1/provider/**                  → hasRole("PROVIDER")
   anyRequest()                         → authenticated()
   ```
   Phase sau thêm route public thì phải thêm vào đây, **trước** `anyRequest()`. Đây chính là bẫy tôi đã gặp: `GET /api/v1/areas` trả 401 vì thiếu matcher.
3. **CORS phải là một `CorsConfigurationSource`**, không phải `@CrossOrigin` rải rác. `allowCredentials(true)` thì `allowedOrigins` không được là `*`.

**Checkpoint 1**: `./gradlew bootRun`; `curl -i localhost:8080/api/v1/categories` → 200 `[]`; `curl -i localhost:8080/api/v1/me/addresses` → **401 JSON**; `/swagger-ui/index.html` → 200. Commit.

---

## PHASE 2 — Identity & Access

### Step 7 — Entity + enum

**Tạo**: `identity/{UserAccount,RefreshToken,UserRole,UserAccountStatus}.java` + 2 repository

**Viết gì**: `UserAccount` gồm `id, email, emailNorm, passwordHash, role, status, version (@Version), createdAt, updatedAt`. `RefreshToken` gồm `userId, tokenHash, expiresAt, revokedAt`. Repository cần `existsByEmailNorm(String)` và `findByEmailNorm(String)`.

> **Chú ý về `@Version`**: đây là optimistic lock. Nếu bạn thêm cột `version` vào bảng mà entity không có (hoặc ngược lại), `ddl-auto: validate` sẽ chặn boot ngay.

**Xong khi**: app boot, không lỗi validate.

---

### Step 8 — `AuthService`

**Tạo**: `identity/AuthService.java` (kèm inner `record AuthResult`)

**Viết gì**: 4 use case, tất cả `@Transactional`
- `register(RegisterRequest)`: normalize email → `email_norm` = lowercase; chặn trùng (`EMAIL_EXISTS`); hash bằng `PasswordEncoder`; tạo `UserAccount` **và** profile tương ứng (`CustomerProfile` nếu role CUSTOMER, `ProviderProfile` nếu PROVIDER, trạng thái verification ban đầu `DRAFT`); trả access token + refresh token.
- `login(LoginRequest)`: sai email/mật khẩu → cùng một `INVALID_CREDENTIALS` (đừng tiết lộ email có tồn tại hay không); `SUSPENDED` → `ACCOUNT_SUSPENDED`.
- `refresh(refreshToken)`: verify tồn tại + chưa revoke + chưa hết hạn → **rotate** (revoke token cũ, phát token mới), trả cặp mới.
- `logout(refreshToken)`: revoke, idempotent (gọi lại vẫn 204).

**Quyết định**: chỉ lưu **hash** của refresh token, không lưu raw. Access token không lưu server-side (stateless).

---

### Step 9 — `AuthController` + DTO

**Tạo**: `identity/{AuthController, dto/RegisterRequest, dto/LoginRequest, dto/AuthResponse, dto/UserResponse}.java`

**Viết gì**: 4 route `POST /api/v1/auth/{register,login,refresh,logout}`. Access token trả trong **body**; refresh token chỉ nằm trong **cookie** `HttpOnly`:
```java
ResponseCookie.from("refresh_token", value)
    .httpOnly(true).secure(true).sameSite("Lax").path("/api/v1/auth").maxAge(ttl).build();
```

**Xong khi**: register + login qua curl; `Set-Cookie` xuất hiện; `GET /api/v1/auth/refresh` với cookie đó trả access token mới.

---

### Step 10 — Seeder

**Tạo**: `seed/DataSeeder.java` (`@Component @Order(Ordered.LOWEST_PRECEDENCE) implements CommandLineRunner`)

**Viết gì**: `seedAdmin()` (email `admin@hsm.local`), `seedGeoAreas()` (tỉnh + quận mẫu: HN, HCM, HN-CG, HN-BD, HCM-Q1, HCM-Q3), `seedCatalog()` (2 category + 4 service).

**Quy tắc sống còn**: mỗi hàm phải **idempotent** (`if (repo.count() > 0) return;` hoặc `existsBy...`), vì `CommandLineRunner` chạy lại mỗi lần boot. Đừng để lần boot thứ hai tạo trùng dữ liệu.

**Checkpoint 2**: login admin → copy access token → `GET /api/v1/admin/users` (route này 403 cho tới Phase 11, nên tạm kiểm tra bằng `user_account` trong DB). Commit.

---

## PHASE 3 — Catalogue & Geo (public reads)

### Step 11 — Entity + repository

**Tạo**: `catalog/{GeoArea,ServiceCategory,Service}.java` + 3 repository

**Viết gì**: `GeoArea` dùng `code` làm `@Id` (kiểu `String`, không phải UUID — khác biệt có chủ ý). `Service` có quan hệ logic `categoryId` (UUID) — repo cần `findByCategoryIdAndActiveTrueOrderByNameAsc`, `findByActiveTrueOrderByNameAsc`, `existsBySlug`.

> **Bẫy đặt tên**: entity tên `Service` **trùng** với annotation `@Service`. Ở module khác muốn dùng nó thì phải viết đủ `ptit.home_services_marketplace.catalog.Service`. Nếu thấy khó chịu, đổi tên entity thành `CatalogService`… nhưng khi đó lại trùng class `CatalogService`. Repo này chọn cách cũ; bạn được quyền chọn cách khác, chỉ cần nhất quán.

**Xong khi**: boot không lỗi validate.

---

### Step 12 — `CatalogService` (chỉ public reads) + `CatalogController`

**Tạo**: `catalog/CatalogService.java`, `catalog/dto/{CategoryResponse,ServiceResponse,GeoAreaResponse}.java`, `catalog/CatalogController.java`

**Viết gì**: `listActiveCategories()`, `listActiveServices(categoryId)`, `getService(id)`, `listActiveAreas()` (map geo_area → `GeoAreaResponse(code, name, type, parentCode)`). Controller `@RequestMapping("/api/v1")` với `GET /categories`, `GET /categories/{id}/services`, `GET /services/{id}`, `GET /areas`.

> **Bài học từ repo này**: `GET /api/v1/areas` **không có sẵn** ban đầu, nên frontend không thể hiển thị dropdown chọn khu vực khi tạo địa chỉ (mà `AddressRequest.areaCode` là bắt buộc). Khi tự code, hãy tự hỏi ở mỗi phase: **client cần đọc gì để hoàn thành form này?** Nếu không ai cung cấp, đó là lỗ hổng API, không phải lỗi frontend.

**Xong khi**: `curl localhost:8080/api/v1/categories` trả 2 category đã seed; `/areas` trả danh sách khu vực **không cần token**.

---

## PHASE 4 — Customer profile & addresses

### Step 13 — Entity + repository + service

**Tạo**: `customer/{CustomerProfile,CustomerAddress}.java`, 2 repository, `customer/CustomerService.java`, `customer/dto/*`

**Viết gì**
- `getProfile` / `updateProfile` (chỉ ghi đè field không null — semantics "partial update").
- `listAddresses` / `createAddress` / `updateAddress` / `deleteAddress`.
- **Xoá mềm**: `deleteAddress` set `deleted_at`, không `DELETE`. Mọi truy vấn phải lọc `deleted_at IS NULL`.
- `requireArea(areaCode)`: bắt buộc, tra `geo_area`, phải tồn tại và `active` → nếu không, `unprocessable` `VALIDATION`.
- **Mọi method gọi `requireProfile(user)`**, và bước đầu kiểm tra `user.role() != CUSTOMER` → `FORBIDDEN`.

**Quy tắc**: kiểm tra quyền sở hữu nằm ở **service**, không phải controller. Controller không biết ai sở hữu gì.

**Xong khi**: tạo customer → thêm địa chỉ với `areaCode=HN-CG` OK; `areaCode=XYZ` → 422; xoá địa chỉ rồi `GET` không thấy nó nữa.

---

## PHASE 5 — Service requests (trái tim của hệ thống)

### Step 14 — Entity + enum (đọc kỹ phần versioning)

**Tạo**: `request/{ServiceRequest,RequestVersion,ServiceRequestStatus}.java` + 2 repository

**Mô hình cần nắm trước khi gõ 1 dòng nào**
- `service_request` giữ **trạng thái**; `request_version` giữ **nội dung**.
- Khi còn `DRAFT`, `request_version` hiện tại là **mutable** — sửa request nghĩa là sửa bản ghi version đó.
- Khi `confirm`, version hiện tại được **copy sang một dòng mới bất biến** và `current_version` tăng lên. Từ đó nội dung đóng băng, không sửa nữa.

Đây là lý do có 2 bảng thay vì 1. Nếu bạn gộp làm một, bạn sẽ mất khả năng "request đã confirm thì nội dung không đổi" — mà đó là yêu cầu về tính toàn vẹn lịch sử.

**Xong khi**: boot không lỗi validate.

---

### Step 15 — `RequestService`

**Tạo**: `request/RequestService.java` + `request/dto/{ServiceRequestUpsertRequest,ServiceRequestResponse,RequestVersionResponse}.java`

**Viết gì**: 6 use case
| Method | Luật cần nhớ |
|---|---|
| `createDraft` | Bắt buộc: `categoryId`, `serviceId`, `description`, `addressId`, `preferredStart`, `preferredEnd`. Tạo request `DRAFT` + version 1 |
| `updateDraft` | Chỉ khi `DRAFT`, nếu không → `INVALID_STATE`. Field null = giữ nguyên |
| `getById` / `listMyRequests` | Lọc theo chủ sở hữu |
| `confirm` | Chỉ khi `DRAFT`; copy version → bất biến → set `CONFIRMED`; **đi qua idempotency** |
| `cancel` | Không cho cancel hai lần; đồng thời `RELEASED` mọi provider_selection đang `ACTIVE` |

Các kiểm tra bắt buộc trong `createDraft`/`updateDraft` (theo đúng thứ tự này):
1. `categoryId`/`serviceId` không null → `VALIDATION`
2. category tồn tại + `active`; service tồn tại + `active`
3. **service phải thuộc category** (`service.categoryId == category.id`) — đây là loại kiểm tra dễ quên nhất
4. `addressId`: tồn tại, thuộc về customer, chưa xoá mềm, và **snapshot** `addressLine` + `areaCode` vào version (sau này sửa địa chỉ không làm thay đổi request cũ)
5. `preferredStart` **phải trước** `preferredEnd` → nếu không `VALIDATION`
6. `budgetCurrency` mặc định `VND`, và nếu có thì phải là **3 ký tự**

> **Chú ý về `confirm` + idempotency**: `idempotencyService.reserve(...)` phải được gọi **trong cùng transaction** với việc đổi trạng thái. Nếu bạn tách ra, một lần retry của client có thể confirm 2 lần hoặc fail giữa chừng để lại state rác.

**Xong khi**: draft → confirm → `status=CONFIRMED`, `currentVersion=2`; gọi `confirm` lần nữa cùng `Idempotency-Key` → trả lại đúng response cũ, không tạo version thứ 3.

---

### Step 16 — `RequestController`

**Tạo**: `request/RequestController.java`

**Viết gì**: `POST /service-requests`, `PUT /service-requests/{id}`, `GET /service-requests/{id}`, `GET /me/requests`, `POST /service-requests/{id}/confirm` (nhận `@RequestHeader("Idempotency-Key")` optional), `POST /service-requests/{id}/cancel`.

**Checkpoint 3**: chạy trọn vòng draft → confirm → cancel bằng curl; query `request_version` thấy 2 dòng version 1 (draft) và 2 (frozen). Commit.

---

## PHASE 6 — Provider self-service

### Step 17 — Entity + converter

**Tạo**: `provider/{ProviderProfile,ProviderService,ProviderArea,ProviderAvailability,ProviderVerificationStatus,LocalTimeMinutesConverter}.java` + 4 repository

**Viết gì**: `ProviderAvailability` lưu `dayOfWeek (short)`, `startTime`, `endTime`; enum verification `DRAFT → PENDING_VERIFICATION → VERIFIED | REJECTED | SUSPENDED`.

**Quyết định kỹ thuật đáng chú ý**: `LocalTimeMinutesConverter implements AttributeConverter<LocalTime, Integer>` — lưu giờ dưới dạng **số phút từ 00:00**, không dùng cột `time`.
> **Vì sao**: `java.sql.Time` bị JDBC/Hibernate dịch múi giờ khi bind → `09:00` có thể thành `02:00`. Đây chính là lý do repo có `V2__availability_minutes.sql` đổi `time` → `integer`. Nếu bạn làm đúng ngay từ V1, bạn không cần V2 — nhưng hãy nhớ quy tắc: **đừng bao giờ lưu giờ-trong-ngày bằng `TIME`**.

---

### Step 18 — `ProviderManagementService` + controller

**Tạo**: `provider/ProviderManagementService.java`, `provider/dto/*`, `provider/ProviderController.java`

**Viết gì**: các nhóm route dưới `/api/v1/provider` — profile (get/update), services (get/replace list), areas (get/replace list), availability (get/replace list), submit-verification.

- "replace" nghĩa là **xoá hết rồi ghi lại** danh sách (dùng `@RequestBody List<UUID>` / `List<String>`), chạy trong 1 transaction.
- Validate mọi `serviceId`/`areaCode` tồn tại + active trước khi ghi.
- Chỉ cho `replaceAreas` nếu provider không ở trạng thái `SUSPENDED`.
- Submit-verification: `DRAFT|REJECTED → PENDING_VERIFICATION`. Ghi audit.

> **Bẫy tên**: class service không thể tên `ProviderService` vì đã có **entity** `ProviderService`. Repo này đặt `ProviderManagementService`. Bạn có thể đặt khác, nhưng phải biết vì sao không dùng tên "hiển nhiên".

**Xong khi**: tạo provider → set 2 service, 1 area, 1 khung giờ; submit verification → `PENDING_VERIFICATION`.

---

## PHASE 7 — Discovery & provider selection

Phase này là **giá trị cốt lõi** của sản phẩm: biến một request đã confirm thành danh sách provider hợp lệ. Yêu cầu tuyệt đối: **deterministic, không AI, không ranking**.

### Step 19 — Entity + enum

**Tạo**: `discovery/{DiscoveryRun,DiscoveryCandidate,ProviderSelection,ProviderSelectionStatus}.java` + 3 repository

**Viết gì**: `DiscoveryRun` ghi lại mỗi lần chạy (request_id, request_version, `no_result_reason`); `DiscoveryCandidate` ghi từng provider đã đạt (kèm `reason_codes` JSON); `ProviderSelection` ghi lựa chọn của customer.

**Quyết định**: lưu lại **vết** của mỗi lần discovery. Khi khách hỏi "sao lúc đó không có ai?", bạn có dữ liệu để trả lời.

---

### Step 20 — `EligibilityEngine` (theo thứ tự filter nghiêm ngặt)

**Tạo**: `discovery/EligibilityEngine.java` (kèm inner `record EligibilityOutcome`, `EligibleCandidate`, hằng số reason-code)

**Viết gì**: pipeline lọc **có thứ tự**, dừng ở filter đầu tiên làm rỗng tập ứng viên và ghi lại `noResultReason`:

```
1. SERVICE        provider có offer đúng serviceId của request?
2. STATUS         provider đã VERIFIED chưa?
3. AREA           provider có cover areaCode của request?
4. AVAILABILITY   khung giờ của provider có giao với [preferredStart, preferredEnd]?
```

Mỗi provider đạt hết 4 filter được ghi `reasonCodes` (ví dụ `SERVICE_MATCH`, `PROVIDER_VERIFIED`, `AREA_MATCH`, `AVAILABILITY_MATCH`).

**Vì sao "có thứ tự" quan trọng**: nếu bạn lọc song song rồi mới tổng hợp, `noResultReason` sẽ vô nghĩa. Với pipeline tuần tự, mỗi lần trả rỗng đều trả lời được **"tắc ở bước nào"** — thông tin đó đi thẳng lên UI.

**Xong khi**: viết 1 test nhỏ cho 4 nhánh filter (đây là hàm dễ test nhất và đáng test nhất trong toàn bộ backend).

---

### Step 21 — `DiscoveryService` + `DiscoveryController`

**Tạo**: `discovery/DiscoveryService.java`, `discovery/dto/*`, `discovery/DiscoveryController.java`

**Viết gì**
- `findEligibleProviders(user, requestId)`: **chỉ chạy khi `status == CONFIRMED`**, nếu không → 409 `INVALID_STATE` (thông điệp nói rõ "requires a CONFIRMED request"). Gọi engine → lưu `DiscoveryRun` + `DiscoveryCandidate` → trả `EligibleProvidersResponse(requestId, requestVersion, discoveryRunId, noResultReason, providers)`.
- `selectProvider(user, requestId, SelectProviderRequest)`: cho phép `CONFIRMED` hoặc `PROVIDERS_FOUND`; **chặn nếu đã `PROVIDER_SELECTED`** (`ALREADY_SELECTED`); **chặn nếu `CANCELLED`**. **Re-validate eligibility tại thời điểm chọn** (task T035) — provider có thể đã bị suspend giữa lúc xem và lúc chọn. Ghi `ProviderSelection(ACTIVE)`, đổi request → `PROVIDER_SELECTED`, ghi audit.

**Hệ quả lên UI (bài học thật)**: vì discovery chỉ chạy ở `CONFIRMED`, **sau khi chọn provider thì không xem lại danh sách được nữa**. Nếu UI vẫn để nút "Change provider", nó là ngõ cụt. Khi code, luôn tự hỏi: *"sau khi state đổi, màn hình cũ còn gọi được API đó không?"*

**Checkpoint 4**: confirm request → `GET .../eligible-providers` → thấy provider đã seed; `PUT .../provider-selection` → request sang `PROVIDER_SELECTED`; gọi lại `eligible-providers` → 409. Commit.

---

## PHASE 8 — Quotation · scheduling · booking

Đây là phase khó nhất về tính đúng đắn. Phần "accept quotation" là **lệnh giao dịch** (consequential command) — phải hoặc thành công trọn vẹn, hoặc không đổi gì.

### Step 22 — Entity + repository

**Tạo**: `quotation/{Quotation,QuotationItem,QuotationStatus}.java`, `scheduling/{ProviderSlotReservation,SlotReservationStatus}.java`, `booking/{Booking,BookingStatus}.java` + repository

**Viết gì**: `QuotationRepository` cần thêm `findByIdWithLock(UUID)` với `@Lock(LockModeType.PESSIMISTIC_WRITE)`.
> **Vì sao cần lock**: hai request accept cùng lúc trên cùng quotation phải xếp hàng, không được tạo 2 booking. Partial unique `(booking.quotation_id)` là lưới an toàn cuối, lock là cách xử lý đúng.

---

### Step 23 — `QuotationService`

**Tạo**: `quotation/QuotationService.java`, `quotation/dto/*`, `quotation/QuotationController.java`

**Viết gì**: provider tạo nháp → submit → withdraw; customer accept/reject. Kiểm tra: chỉ provider của request đó mới được quote; `total_amount` tính từ items; `currency` nhất quán với `budgetCurrency` của request; không cho submit khi đã có quotation `SUBMITTED` khác (partial unique `uq_quote_active_selection` là chốt chặn ở DB).

---

### Step 24 — `SchedulingService` + map lỗi exclusion constraint

**Tạo**: `scheduling/SchedulingService.java`, `scheduling/ProviderSlotReservationRepository.java`

**Viết gì**: `reserve(providerId, bookingId, start, end)` tạo dòng `ACTIVE`; `release(bookingId)` chuyển sang `RELEASED`.

**Điểm mấu chốt**: khi hai booking trùng giờ, PostgreSQL ném lỗi vi phạm `ex_provider_active_slot`. Bạn **không** tự viết câu `SELECT ... FOR UPDATE` để kiểm tra trùng — bạn để DB làm và **map lỗi**:
```java
// trong luồng accept, bắt DataIntegrityViolationException có tên constraint ex_provider_active_slot
throw ApiException.conflict(ErrorCodes.SLOT_UNAVAILABLE, "...");
```
→ 409 `SLOT_UNAVAILABLE`. Kiểm tra `getMostSpecificCause().getMessage()` có chứa tên constraint trước khi map, để không nuốt nhầm các lỗi integrity khác.

---

### Step 25 — `BookingService`: luồng accept (transactional)

**Tạo**: `booking/BookingService.java`, `booking/dto/*`, `booking/BookingController.java`

**Viết gì** — đúng thứ tự trong **một** `@Transactional`:
```
1. lock quotation (PESSIMISTIC_WRITE)
2. kiểm tra: quotation thuộc customer này, status = SUBMITTED, chưa hết hạn, chưa accept
3. reserve slot  → nếu vi phạm exclusion constraint → SLOT_UNAVAILABLE (rollback hết)
4. tạo booking (CONFIRMED)
5. quotation  → ACCEPTED
6. provider_selection → CONVERTED
7. request → (trạng thái đóng)
8. audit
```
Bọc toàn bộ trong idempotency (client retry accept không được tạo 2 booking).

**Xong khi**: accept 2 lần cùng `Idempotency-Key` → chỉ 1 booking; tạo booking trùng giờ → 409 `SLOT_UNAVAILABLE`; query `provider_slot_reservation` chỉ thấy 1 dòng `ACTIVE`.

**Checkpoint 5**: chạy hết luồng request → discovery → quote → accept → booking thành công. Commit.

---

## PHASE 9 — Fulfillment (state machine)

### Step 26 — Vòng đời booking

**Sửa/tạo**: `booking/BookingService.java`, `booking/dto/{BookingResponse,CancelBookingRequest}.java`

**Viết gì**: `start`, `complete`, `cancel` + `releaseSlot`.
Bảng chuyển trạng thái (chỉ những mũi tên này hợp lệ, còn lại → `INVALID_STATE`):
```
CONFIRMED → IN_PROGRESS → COMPLETED
CONFIRMED → CANCELLED
IN_PROGRESS → CANCELLED
```
**Ma trận quyền**: cả customer và provider đều có thể cancel nhưng lý do khác nhau; chỉ provider được `start`/`complete`; chỉ customer được review (phase sau). Viết một method duy nhất `assertActorMay(...)` thay vì rải `if` khắp nơi.

**Quy tắc nhất quán**: `cancel`/`complete` **phải** release slot. Nếu quên, provider đó bị khoá giờ vĩnh viễn — lỗi rất khó phát hiện vì UI vẫn "chạy bình thường".

**Xong khi**: start → complete → slot `RELEASED`; `complete` lần 2 → 409.

---

## PHASE 10 — Payment & review

### Step 27 — `PaymentService` (idempotent callback)

**Tạo**: `payment/{PaymentRecord,PaymentStatus}.java`, `PaymentRecordRepository`, `PaymentService`, `payment/dto/*`, `PaymentController`

**Viết gì**: `attempt(bookingId)` tạo bản ghi `PENDING`; `recordResult(adapter, externalRef, result)` — đây là callback của cổng thanh toán giả lập, **phải idempotent** vì cổng có thể gọi lại. Hai partial unique index đã lo: `uq_payment_external` (một externalRef chỉ ghi nhận 1 lần) và `uq_booking_recorded_payment` (một booking chỉ có 1 payment `RECORDED`).
Chỉ cho thanh toán khi booking `COMPLETED`.

**Xong khi**: gọi `recordResult` 2 lần cùng `externalRef` → 1 bản ghi `RECORDED`, lần 2 trả kết quả cũ.

---

### Step 28 — `ReviewService` + reputation

**Tạo**: `review/{Review,ReviewStatus}.java`, `ReviewRepository`, `ReviewService`, `review/dto/*`, `ReviewController`

**Viết gì**: `createReview` chỉ cho customer của booking, booking phải `COMPLETED` **và** đã có payment `RECORDED` (review chỉ mở sau khi trả tiền), mỗi booking 1 review (`UNIQUE(booking_id)` + `REVIEW_EXISTS`). `getProviderReputation(providerId)` tính điểm trung bình + số lượng.

**Quyết định**: tính trung bình bằng **query ở DB**, không `findAll().stream().filter().mapToInt().average()` trong Java.
> Repo hiện tại đang làm cách in-memory ở `ReviewService`, `DashboardService`, `AdminReviewService`, `AdminUserService`. Nó chạy đúng nhưng sai hướng — khi tự code, hãy viết JPQL `averagePublishedRating` ngay từ đầu.

**Checkpoint 6**: hoàn tất luồng tới review; điểm reputation của provider thay đổi. Commit.

---

## PHASE 11 — Admin & audit

### Step 29 — Bốn nhóm quyền admin

**Tạo**: `admin/{AdminUserService,AdminProviderService,AdminReviewService,DashboardService}.java` + 6 controller + `admin/dto/*`

| Nhóm | Route | Việc |
|---|---|---|
| Users | `/api/v1/admin/users` | list, suspend, reactivate |
| Providers | `/api/v1/admin/providers` | list, verify, reject (kèm lý do) |
| Reviews | `/api/v1/admin/reviews` | list, hide, restore |
| Audit | `/api/v1/admin/audit` | list audit_event (chỉ đọc) |
| Dashboard | `/api/v1/admin/dashboard` | đếm request/booking/completion |
| Catalogue | `/api/v1/admin/{categories,services}` | delegate sang `CatalogService` |

**Quy tắc**: `admin` **không tự viết lại** logic đã có — `AdminCatalogController` gọi thẳng `CatalogService`. Mọi hành động admin đều ghi audit **kèm `reason`** và actor.

**Chặn tự khoá**: admin không được tự suspend chính mình / suspend admin cuối cùng.

**Checkpoint 7**: gọi mọi route admin không token → 401; token customer → 403; token admin → 200. Commit.

---

## PHASE 12 — Hardening & tests

Đây là phần `tasks.md` còn nợ: **T055** (integration tests) và **T056** (unit tests cho state machine).

### Step 30 — Test những chỗ đáng test

Ưu tiên theo thứ tự "rủi ro × dễ test":

1. `EligibilityEngine` — 4 nhánh filter + trường hợp rỗng, thuần logic, không cần DB
2. State machine của `ServiceRequest` và `Booking` — mọi cặp (trạng thái, hành động) hợp lệ/không hợp lệ
3. Idempotency — cùng key cùng body → replay; cùng key khác body → conflict
4. Booking accept — lock + slot conflict (Testcontainers PostgreSQL, vì exclusion constraint là tính năng của PG, **không** test được với H2)
5. Security — 401/403 theo từng matcher

> **Chú ý**: đừng test bằng H2. Toàn bộ giá trị của schema này nằm ở `EXCLUDE USING gist`, partial unique index, `jsonb`, `gen_random_uuid()` — H2 không có. Dùng Testcontainers.

### Step 31 — Dọn nợ kỹ thuật

Trước khi coi là xong, rà lại danh sách này (đây là những thứ repo hiện tại đang còn):
- [ ] Cấu hình JWT chỉ còn **một** chỗ (`app.jwt.*`), xoá `app.security.jwt.*`
- [ ] Quyết định dứt điểm về Redis: dùng thật (cache/session) hay xoá dependency
- [ ] Hoặc thêm `spring-boot-starter-actuator`, hoặc xoá matcher `/actuator/health` trong `SecurityConfig`
- [ ] CORS đọc từ property thay vì hardcode, hoặc xoá property thừa
- [ ] Thay các phép đếm/lọc in-memory trong admin/review bằng query DB
- [ ] Entity: chỉ dùng `@UuidGenerator`, bỏ `@GeneratedValue` thừa
- [ ] Thêm seed customer + provider demo để test end-to-end

**Xong khi**: `./gradlew test` xanh và `./gradlew build` xanh.

---

## PHASE 13 — ELK structured logging (spec 002)

Độc lập với domain — chỉ đụng `shared/logging/`, `application.yaml` và `infra/elk/`. Có thể bỏ qua ở lượt code đầu, làm sau khi MVP chạy.

### Step 32 — Trace filter + MDC

**Tạo**: `shared/logging/{LogContext,RequestTraceFilter}.java`

**Viết gì**: `LogContext` = danh mục key MDC (`traceId`, `userId`, `role`) + `setActor/clear`. `RequestTraceFilter` = `OncePerRequestFilter` với `@Order(Ordered.HIGHEST_PRECEDENCE)` (phải bọc **ngoài** security chain để cả 401/403 cũng có traceId): nhận `X-Request-Id` từ client **nếu hợp lệ**, nếu không thì sinh mới; trả lại qua response header; đặt MDC; `clear` trong `finally`.

**Bảo mật**: **không** tin header client. Chỉ nhận khi khớp `[A-Za-z0-9._-]{1,64}`; loại ký tự điều khiển (chống log injection). Có test riêng cho đúng việc này.

### Step 33 — Structured JSON logging + test

**Sửa**: `application.yaml` (thêm `logging.file.name`, `logging.structured.format.file: logstash`, `logging.structured.json.add.*`, `logging.logback.rollingpolicy.*`)
**Tạo**: `src/test/java/.../shared/logging/StructuredLoggingTest.java`

**Viết gì**: test khẳng định một log event có MDC `traceId`/`userId`/`role` được serialize thành **một dòng JSON** chứa đủ 3 field đó. Đây là "contract C1" với Logstash — nếu test này đỏ, pipeline log ở `infra/elk/` sẽ không parse được.

**Xong khi**: `./gradlew test` xanh; chạy app và thấy `backend/logs/hsm-backend.json` mỗi dòng là 1 JSON object.

> Phần `infra/elk/**` (Logstash pipeline, index template, ILM, Kibana dashboard) làm theo `specs/002-elk-logging/tasks.md` — nó là hạ tầng, không phải code Java.

---

## 14. Verification playbook

Chạy **sau mỗi checkpoint** và chạy trọn vẹn một lần ở cuối. Thay `<...>` bằng giá trị bạn nhận được.

### 14.1 Luồng happy path (copy-paste được)

```bash
B=http://localhost:8080/api/v1

# 1. identity
curl -s -X POST $B/auth/register -H 'Content-Type: application/json' \
  -d '{"email":"c1@test.local","password":"Test1234","role":"CUSTOMER","displayName":"Customer One"}'
curl -s -c cookies.txt -X POST $B/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"c1@test.local","password":"Test1234"}'      # -> accessToken + Set-Cookie
TOK=<accessToken>

# 2. address (areaCode phải tồn tại trong geo_area)
curl -s $B/me/addresses -H "Authorization: Bearer $TOK"     # -> []
curl -s -X POST $B/me/addresses -H "Authorization: Bearer $TOK" -H 'Content-Type: application/json' \
  -d '{"label":"Home","addressLine":"12 Le Loi","areaCode":"HN-CG","isDefault":true}'   # -> id

# 3. request lifecycle
curl -s -X POST $B/service-requests -H "Authorization: Bearer $TOK" -H 'Content-Type: application/json' \
  -d '{"categoryId":"<cat>","serviceId":"<svc>","description":"Tap is leaking badly.",
       "addressId":"<addr>","preferredStart":"2026-11-20T09:00:00+07:00",
       "preferredEnd":"2026-11-20T12:00:00+07:00","budgetAmount":500000,"budgetCurrency":"VND"}'
curl -s -X POST $B/service-requests/<req>/confirm -H "Authorization: Bearer $TOK" -H 'Idempotency-Key: k1'

# 4. discovery + selection
curl -s $B/service-requests/<req>/eligible-providers -H "Authorization: Bearer $TOK"
curl -s -X PUT $B/service-requests/<req>/provider-selection -H "Authorization: Bearer $TOK" \
  -H 'Content-Type: application/json' -d '{"providerId":"<prov>"}'
```

### 14.2 Kiểm tra ở tầng DB (SQL)

```sql
-- request đã confirm phải có 2 version, và version 2 là bản đóng băng
SELECT request_id, version, description FROM request_version WHERE request_id = '<req>' ORDER BY version;

-- mỗi hành động ghi phải để lại vết
SELECT action, entity_type, actor_id, created_at FROM audit_event ORDER BY created_at DESC LIMIT 10;

-- idempotency: gọi lại cùng key KHÔNG được sinh bản ghi thứ hai
SELECT actor_id, operation, idem_key, status FROM idempotency_record;

-- slot: mỗi booking chỉ 1 reservation ACTIVE
SELECT booking_id, status, start_at, end_at FROM provider_slot_reservation;

-- thử phá ràng buộc (phải bị DB chặn)
INSERT INTO provider_slot_reservation (provider_id, booking_id, start_at, end_at, status)
VALUES ('<prov>', '<other_booking>', '2026-11-20T10:00+07', '2026-11-20T11:00+07', 'ACTIVE');
-- -> ERROR: conflicting key value violates exclusion constraint "ex_provider_active_slot"
```

### 14.3 Kiểm tra an ninh (bắt buộc mỗi checkpoint)

| Tình huống | Mong đợi |
|---|---|
| Không có token gọi route protected | 401 JSON `UNAUTHORIZED` |
| Token customer gọi `/api/v1/admin/**` | 403 JSON `FORBIDDEN` |
| Token customer A đọc request của customer B | 404 (không phải 403 — đừng tiết lộ sự tồn tại) |
| Body JSON hỏng | 400 `MALFORMED` |
| Field bắt buộc thiếu | 400 `VALIDATION` |
| Vi phạm unique | 409 `CONFLICT` |

---

## 15. Guardrails — bẫy đã gặp trong chính repo này

| # | Bẫy | Cách tránh |
|---|---|---|
| 1 | Sửa `V1__init.sql` sau khi nó đã chạy | Migration là **append-only**. Sai thì thêm `V2__...sql` |
| 2 | Entity lệch schema | Viết migration trước; `ddl-auto: validate` sẽ chặn boot |
| 3 | Filter bị đăng ký 2 lần | `JwtAuthenticationFilter` tạo bằng `new`, **không** `@Component` |
| 4 | Route public trả 401 | Thêm matcher **trước** `anyRequest().authenticated()` |
| 5 | Lưu giờ-trong-ngày bằng `TIME` | Dùng số phút từ 00:00 + `AttributeConverter` |
| 6 | Tự `SELECT` để check trùng giờ | Để exclusion constraint làm, rồi map sang `SLOT_UNAVAILABLE` 409 |
| 7 | Quên release slot khi cancel/complete | Provider bị khoá giờ vĩnh viễn mà UI vẫn "bình thường" |
| 8 | Idempotency ngoài transaction của caller | `Propagation.MANDATORY` |
| 9 | `findAll().stream()` để đếm | Viết `count`/JPQL ngay từ đầu |
| 10 | Lazy-load ngoài transaction | `open-in-view: false` + load đủ dữ liệu trong service |
| 11 | Lộ entity ra API | Response luôn là `record` DTO |
| 12 | Kiểm tra quyền ở controller | Đưa hết vào service (`requireProfile`, `loadOwned`) |
| 13 | Log secret/token/mật khẩu | Không log; rà lại JSON log trước khi nộp (FR-006) |
| 14 | Seed tạo trùng mỗi lần boot | Check `count()/exists` trong mọi hàm seed |
| 15 | Trả 403 khi nên trả 404 | Với tài nguyên của người khác: **404** |
| 16 | `@GeneratedValue` + `@UuidGenerator` cùng lúc | Chỉ dùng một |
| 17 | Cấu hình trùng lặp / dependency không dùng | Mỗi key/dependency phải trả lời được "ai đọc nó?" |
| 18 | Test bằng H2 | Dùng Testcontainers PostgreSQL |

---

## 16. Bản đồ thứ tự & phụ thuộc

| Phase | Bảng DB | Phụ thuộc vào | Checkpoint |
|---|---|---|---|
| 1 | 24 bảng, toàn bộ | — | App boot, 401 JSON, `/swagger-ui` 200 |
| 2 | `user_account`, `refresh_token` | Phase 1 | register/login/refresh qua curl |
| 3 | `geo_area`, `service_category`, `service` | Phase 1 | `/categories`, `/areas` trả 200 không token |
| 4 | `customer_profile`, `customer_address` | Phase 3 | CRUD địa chỉ, `areaCode` sai → 422 |
| 5 | `service_request`, `request_version` | Phase 3, 4 | draft → confirm → version đóng băng |
| 6 | `provider_profile`, `provider_service`, `provider_area`, `provider_availability` | Phase 3 | Provider onboard + submit verification |
| 7 | `discovery_run`, `discovery_candidate`, `provider_selection` | Phase 5, 6 | eligible list → select → 409 khi gọi lại |
| 8 | `quotation`, `quotation_item`, `booking`, `provider_slot_reservation` | Phase 7 | accept → 1 booking, trùng giờ → 409 |
| 9 | (dùng lại) | Phase 8 | start/complete/cancel + slot released |
| 10 | `payment_record`, `review` | Phase 9 | payment idempotent, review 1 lần/booking |
| 11 | (dùng lại bao gồm `audit_event`) | Phase 2–10 | 401/403/200 đúng theo role |
| 12 | — | tất cả | `./gradlew test` + `build` xanh |
| 13 | — | Phase 1 | 1 dòng JSON log có `traceId` |

---

## 17. Nếu bị kẹt

Ba câu hỏi theo thứ tự, mỗi câu loại bỏ một nửa khả năng:

1. **Boot có qua không?** Không → lỗi schema/entity/bean (`ddl-auto: validate` thường chỉ đúng chỗ).
2. **Gọi được API chưa?** Không → matcher trong `SecurityConfig`, hoặc mapping route, hoặc DTO không deserialize được (400/401/404/405 luôn phân biệt rõ).
3. **Kết quả đúng nghiệp vụ chưa?** Không → luật nào trong `@Service` bị thiếu? Đối chiếu lại bảng "Viết gì" của step đó, và kiểm tra xem **DB có đang chặn đúng như mong đợi** không (query trực tiếp, đừng chỉ tin API).

Khi một hành động "đắt" (confirm/accept/pay) cho kết quả sai: kiểm tra **thứ tự** các bước trong transaction trước khi nghi ngờ logic. Phần lớn lỗi ở phase 8–10 là sai thứ tự hoặc thiếu `@Transactional`, không phải sai điều kiện.

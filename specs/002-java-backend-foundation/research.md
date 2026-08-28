# Phase 0 Research: Java Backend Foundation

## R-01 — Runtime and build versions

**Decision**: Java 21, Spring Boot 3.5.16 và Gradle Wrapper 8.14.5.

**Rationale**: Constitution v1.1.0 chốt Java 21/Spring Boot 3 cho Java runtime; ADR-0001
đang `Proposed` cung cấp chi tiết thảo luận về Modular Monolith và phải `Accepted` trước
khi implementation phụ thuộc được merge. Spring Boot 3.5.16 hỗ trợ Java 21 và Gradle 8.4
trở lên; Gradle 8.14.5 là patch mới nhất của nhánh 8 tương thích. Wrapper pin version để
bốn máy và CI dùng cùng build tool.

**Alternatives considered**: Spring Boot 4 bị loại vì trái major đã chốt; Gradle 9 bị
loại vì Boot 3.5 không công bố explicit support; Maven bị loại vì roadmap đã chốt Gradle
multi-module.

Nguồn: [Spring Boot 3.5 system requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html),
[Gradle releases](https://gradle.org/releases/).

## R-02 — Multi-project build layout

**Decision**: Kotlin DSL multi-project build, một `libs.versions.toml`, included
`build-logic` convention plugins và một root `check` aggregation path.

**Rationale**: Settings file khai báo toàn bộ subproject; version catalog giữ dependency
coordinate tập trung; convention plugin tránh copy/paste Java/test/compiler configuration
qua 16 project. Root command vẫn là `./gradlew clean check`.

**Alternatives considered**: Root `subprojects {}` block ngắn hơn nhưng tạo implicit
configuration khó tách; mỗi module tự pin version dễ drift; composite build cho từng
capability quá phức tạp cho một repository.

Nguồn: [Gradle multi-project builds](https://docs.gradle.org/current/userguide/multi_project_builds.html),
[Gradle version catalogs](https://docs.gradle.org/current/userguide/version_catalogs.html).

## R-03 — Module skeleton and architecture enforcement

**Decision**: Tạo đủ 13 capability project, hai app project và một
`architecture-tests` project. Skeleton dùng namespace riêng; public API nằm dưới
`..api..`, implementation tương lai dưới `..internal..`. ArchUnit 1.5.0 kiểm tra allowed
dependency, forbidden framework/provider/persistence dependency và cycle.

**Rationale**: Project boundary giúp Gradle chặn undeclared dependency; package rule bắt
trường hợp một dependency được khai báo nhưng code đi vào internal package. Dedicated
test project được phép đọc bytecode của mọi module mà không biến nó thành runtime owner.

**Alternatives considered**: Một project duy nhất chỉ dựa package convention bảo vệ yếu;
Java Platform Module System tăng packaging/reflection complexity chưa cần cho MVP;
Spring Modulith không được ADR yêu cầu và không thay thế build boundary.

Nguồn: [ArchUnit releases](https://github.com/TNG/ArchUnit/releases).

## R-04 — JWT verification

**Decision**: Dùng Spring Security Resource Server/Jose với issuer, explicit JWKS URI và
audience từ external configuration. `sub` phải parse thành UUID để tạo
`AuthenticatedUserContext`. Authentication flow được test qua test-only controller với
local test signing key/JWKS; không thêm public endpoint.

**Rationale**: Resource Server kiểm tra signature, `exp`/`nbf`, issuer và audience; dùng
explicit JWKS URI tránh buộc API gọi discovery endpoint khi startup. Supabase công bố
JWKS public key và `sub` là user identity. Test key làm suite mặc định offline và
deterministic.

**Alternatives considered**: Tự parse JWT có rủi ro bảo mật; gọi Auth `/user` cho từng
request làm Auth thành hot-path; dùng service-role/database lookup để xác thực vi phạm
ADR-0011; public `/me` vượt phạm vi.

Nguồn: [Spring Security JWT Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html),
[Supabase JWT guide](https://supabase.com/docs/guides/auth/jwts).

## R-05 — Health and database connectivity

**Decision**: Actuator liveness chỉ phản ánh process/application state; readiness bao
gồm DataSource health. API và Worker nhận JDBC settings từ environment. Thiếu property
bắt buộc thì fail-fast; database unreachable sau khi cấu hình thì application vẫn sống
và readiness báo DOWN. Remote verification chỉ chạy health connection, không business SQL.

**Rationale**: External outage không nên làm liveness restart loop; readiness có thể
ngừng nhận traffic/work. JDBC/Hikari và PostgreSQL driver đủ cho connection health,
không cần JPA/repository. Pool configuration phải cho phép application context khởi động
khi database tạm unavailable.

**Alternatives considered**: Đưa database vào liveness gây cascading restart; bỏ remote
verification trì hoãn lỗi config; JPA tạo mapping/persistence scope chưa cần.

Nguồn: [Spring Boot health probes](https://docs.spring.io/spring-boot/reference/actuator/endpoints.html),
[Spring Boot SQL database configuration](https://docs.spring.io/spring-boot/3.5/reference/data/sql.html).

## R-06 — Correlation and structured logging

**Decision**: `X-Correlation-Id` được nhận nếu nonblank và dài tối đa 128 ký tự; nếu
thiếu/không hợp lệ API tạo uppercase ULID. Filter đặt ID vào MDC, response header và
clear MDC trong `finally`. Dùng structured JSON console logging do Spring Boot cung cấp;
không thêm tracing/metrics backend.

**Rationale**: Khớp OpenAPI/conventions hiện tại, cho phép nối request–error–log và tránh
thread-pool context leak. Built-in structured logging giảm dependency. Authorization,
token, password và connection URL không được log.

**Alternatives considered**: Chỉ log text khó xử lý tự động; Micrometer tracing/backend
vượt scope; tin mọi client header không giới hạn tạo log injection/cardinality risk.

Nguồn: [Spring Boot logging](https://docs.spring.io/spring-boot/reference/features/logging.html),
[Spring Boot structured logging properties](https://docs.spring.io/spring-boot/3.5/appendix/application-properties/).

## R-07 — Test isolation

**Decision**: `./gradlew clean check` gồm unit, application-context, security fixture và
ArchUnit tests nhưng không gọi network/database. Remote Supabase check nằm trong task
`supabaseIntegrationTest`, chỉ chạy khi explicit environment configuration có mặt.

**Rationale**: Developer và CI có feedback ổn định không cần Docker/credential; remote
test vẫn cung cấp evidence theo clarification mà không làm test mặc định flaky. Test chỉ
đọc health, không dùng business table.

**Alternatives considered**: Test remote trong `check` làm build phụ thuộc network và
secret; Testcontainers cần Docker trái lựa chọn hiện tại; bỏ integration test không đáp
ứng SC-009.

## R-08 — Configuration and secret handling

**Decision**: Commit `application.yml` với placeholder environment variables và
`.env.example` chỉ chứa tên/giá trị giả. Runtime dùng `DATABASE_URL`,
`DATABASE_USERNAME`, `DATABASE_PASSWORD`, `SUPABASE_JWT_ISSUER`,
`SUPABASE_JWT_JWKS_URI`, `SUPABASE_JWT_AUDIENCE`. Không dùng Supabase service-role key.

**Rationale**: JWT verification cần public issuer/JWKS metadata; database health cần
server-side credential. Tách rõ biến secret và non-secret giúp scan/review an toàn.

**Alternatives considered**: Hard-code project config khó tái sử dụng môi trường; commit
development password vi phạm Constitution; dùng browser publishable key cho JDBC không
hợp lệ.

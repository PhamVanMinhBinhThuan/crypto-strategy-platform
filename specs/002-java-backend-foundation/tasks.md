# Danh sách công việc: Java Backend Foundation

**Đầu vào**: Các tài liệu trong `/specs/002-java-backend-foundation/`

**Điều kiện tiên quyết**: `plan.md`, `spec.md`, `research.md`, `data-model.md`,
`contracts/`, `quickstart.md`

**Owner chính**: Luật. Nghi Văn, Văn Minh và Tiến review boundary phục vụ feature mình
sở hữu trước khi F-002 merge.

**Kiểm chứng**: Mỗi acceptance/quality scenario cần evidence tương xứng và tự động hóa khi
khả thi; review có thể xem lại được dùng cho outcome cần đánh giá trực tiếp của thành viên.
Test của mỗi user story phải được viết trước implementation tương ứng; root `check` mặc
định không gọi external service.

## Định dạng: `[ID] [P?] [Story] Mô tả`

- **[P]**: Có thể làm song song vì thay đổi file khác và không phụ thuộc task chưa xong.
- **[Story]**: Ánh xạ tới user story trong `spec.md`.

## Giai đoạn 1: Khởi tạo build

**Mục tiêu**: Tạo một Gradle Wrapper và dependency catalog có version cố định.

- [x] T001 Tạo Gradle Wrapper 8.14.5 cùng root project metadata tại `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.properties`, `gradle/wrapper/gradle-wrapper.jar` và `settings.gradle.kts`
- [x] T002 Tạo root lifecycle/configuration build tại `build.gradle.kts` và `gradle.properties`
- [x] T003 Tập trung version/plugin/dependency alias tại `gradle/libs.versions.toml`
- [x] T004 [P] Bổ sung Java/Gradle/build/secret patterns còn thiếu vào `.gitignore`, tạo placeholder-only `.env.example` và tạo evidence template có commit/environment/non-secret configuration cùng trạng thái `Planned`/`Verified` tại `specs/002-java-backend-foundation/verification-evidence.md`

---

## Giai đoạn 2: Build foundation dùng chung

**Mục tiêu**: Mọi app/module dùng cùng compiler, test và dependency convention trước khi triển khai user story.

**⚠️ BẮT BUỘC**: Chưa user story nào bắt đầu cho đến khi convention plugins và toàn bộ project path được Gradle nhận diện.

- [x] T005 Tạo included convention build tại `build-logic/settings.gradle.kts` và `build-logic/build.gradle.kts`
- [x] T006 Tạo Java 21 library convention tại `build-logic/src/main/kotlin/crypto.java-library-conventions.gradle.kts`
- [x] T007 Tạo JUnit Platform/test-report convention tại `build-logic/src/main/kotlin/crypto.test-conventions.gradle.kts`
- [x] T008 Tạo Spring Boot runnable-application convention tại `build-logic/src/main/kotlin/crypto.spring-application-conventions.gradle.kts`
- [x] T009 Khai báo `apps:api`, `apps:worker`, 13 capability project và `architecture-tests` trong `settings.gradle.kts`
- [x] T010 [P] Tạo build script cho hai composition root tại `apps/api/build.gradle.kts` và `apps/worker/build.gradle.kts`
- [x] T011 Tạo Java-library build script tối thiểu tại `modules/domain/build.gradle.kts`, `modules/contracts/build.gradle.kts`, `modules/market-data/build.gradle.kts`, `modules/strategy-core/build.gradle.kts`, `modules/strategies/build.gradle.kts`, `modules/combination/build.gradle.kts`, `modules/backtesting/build.gradle.kts`, `modules/evaluation/build.gradle.kts`, `modules/experiment/build.gradle.kts`, `modules/search/build.gradle.kts`, `modules/leaderboard/build.gradle.kts`, `modules/news/build.gradle.kts`, `modules/persistence/build.gradle.kts` và verification build tại `architecture-tests/build.gradle.kts`

**Điểm kiểm tra**: `./gradlew projects` liệt kê đúng 2 app, 13 capability và architecture-test project.

---

## Giai đoạn 3: User Story 1 - Build và kiểm thử backend thống nhất (P1) 🎯 MVP

**Mục tiêu**: Thành viên mới build/test toàn backend foundation bằng một command mà không cần Docker, database, Redis hoặc provider.

**Kiểm thử độc lập**: Từ checkout sạch với JDK 21, chạy `./gradlew clean check`; mọi project tham gia và lỗi module/test được báo rõ.

### Kiểm thử User Story 1

- [x] T012 [US1] Tạo Gradle TestKit test cho Java/test/application convention tại `build-logic/src/test/kotlin/ConventionPluginsTest.kt`
- [x] T013 [US1] Tạo root project coverage test xác nhận đủ expected project path và `check` task tại `architecture-tests/src/test/java/com/cryptostrategy/platform/architecture/BuildStructureTest.java`

### Triển khai User Story 1

- [x] T014 [US1] Tạo buildable `api`/`internal` package skeleton cùng `package-info.java` tại `modules/domain/src/main/java/com/cryptostrategy/platform/domain/`, `modules/contracts/src/main/java/com/cryptostrategy/platform/contracts/`, `modules/market-data/src/main/java/com/cryptostrategy/platform/marketdata/`, `modules/strategy-core/src/main/java/com/cryptostrategy/platform/strategy/`, `modules/strategies/src/main/java/com/cryptostrategy/platform/strategies/`, `modules/combination/src/main/java/com/cryptostrategy/platform/combination/`, `modules/backtesting/src/main/java/com/cryptostrategy/platform/backtesting/`, `modules/evaluation/src/main/java/com/cryptostrategy/platform/evaluation/`, `modules/experiment/src/main/java/com/cryptostrategy/platform/experiment/`, `modules/search/src/main/java/com/cryptostrategy/platform/search/`, `modules/leaderboard/src/main/java/com/cryptostrategy/platform/leaderboard/`, `modules/news/src/main/java/com/cryptostrategy/platform/news/` và `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/`
- [x] T015 [P] [US1] Tạo API application entry point tối thiểu tại `apps/api/src/main/java/com/cryptostrategy/platform/api/ApiApplication.java`
- [x] T016 [P] [US1] Tạo Worker application entry point tối thiểu tại `apps/worker/src/main/java/com/cryptostrategy/platform/worker/WorkerApplication.java`
- [x] T017 [US1] Viết hướng dẫn prerequisite/build/test/add-module tại `README.md` và `docs/architecture/module-view.md` mà không thay đổi dependency decision
- [x] T018 [US1] Chạy `./gradlew clean check` từ checkout state hiện tại và ghi exact commit, environment/configuration, non-secret result/duration tại `specs/002-java-backend-foundation/verification-evidence.md`

**Điểm kiểm tra**: Một command build/test được toàn foundation khi external service đều tắt.

---

## Giai đoạn 4: User Story 2 - Bảo vệ ranh giới module (P1)

**Mục tiêu**: Dependency hợp lệ pass; internal import, forbidden technology dependency và cycle bị bắt tự động.

**Kiểm thử độc lập**: Chạy architecture test với positive/negative fixture và xác nhận rule cho thông báo đúng dependency vi phạm.

### Kiểm thử User Story 2

- [x] T019 [US2] Tạo dependency/canonical fixtures tại `architecture-tests/src/test/java/com/cryptostrategy/platform/architecture/fixtures/AllowedDomainDependency.java`, `architecture-tests/src/test/java/com/cryptostrategy/platform/architecture/fixtures/ForbiddenInternalDependency.java`, `architecture-tests/src/test/java/com/cryptostrategy/platform/architecture/fixtures/ForbiddenTechnologyDependency.java`, `architecture-tests/src/test/java/com/cryptostrategy/platform/architecture/fixtures/ForbiddenCycleA.java`, `architecture-tests/src/test/java/com/cryptostrategy/platform/architecture/fixtures/ForbiddenCycleB.java` và `architecture-tests/src/test/java/com/cryptostrategy/platform/architecture/fixtures/ForbiddenCanonicalBoundaryValue.java`
- [x] T020 [US2] Viết ArchUnit rule cho public/internal package và allowed dependency matrix tại `architecture-tests/src/test/java/com/cryptostrategy/platform/architecture/ModuleBoundaryTest.java`
- [x] T021 [US2] Viết ArchUnit rule cấm framework/provider/persistence trong pure modules, cấm cycle và enforce UUID/exact-decimal/UTC public-boundary convention tại `architecture-tests/src/test/java/com/cryptostrategy/platform/architecture/PurityAndCycleTest.java`

### Triển khai User Story 2

- [x] T022 [US2] Thêm ArchUnit 1.5.0 và project-under-test dependency cần thiết vào `architecture-tests/build.gradle.kts`
- [x] T023 [US2] Đồng bộ package ownership/public-boundary description với test rule tại `specs/002-java-backend-foundation/contracts/module-boundaries.md`
- [ ] T024 [US2] Nhờ Nghi Văn, Văn Minh và Tiến review allowed dependency phục vụ F-003/F-004/F-005 và ghi reviewer, commit, kết quả có thể xem lại tại `specs/002-java-backend-foundation/verification-evidence.md`

**Điểm kiểm tra**: Architecture rule có cả bằng chứng pass và bằng chứng chủ động bắt lỗi.

---

## Giai đoạn 5: User Story 3 - Khởi động và quan sát API foundation (P1)

**Mục tiêu**: API/Worker khởi động, health đúng semantics, cấu hình an toàn và request có correlation xuyên response/log.

**Kiểm thử độc lập**: Với fixture database UP/DOWN, kiểm tra liveness/readiness; khởi động Worker idle không Redis; gửi request correlation fixture và kiểm tra response/log/MDC.

### Kiểm thử User Story 3

- [x] T025 [US3] Viết API liveness/readiness và missing-configuration integration test tại `apps/api/src/test/java/com/cryptostrategy/platform/api/health/ApiHealthIntegrationTest.java`
- [x] T026 [P] [US3] Viết Worker startup/idle/health và missing-configuration test không Redis tại `apps/worker/src/test/java/com/cryptostrategy/platform/worker/WorkerHealthIntegrationTest.java`
- [x] T027 [US3] Viết correlation success/authentication/validation/unexpected-error, safe-envelope, MDC-cleanup và log-redaction test tại `apps/api/src/test/java/com/cryptostrategy/platform/api/observability/CorrelationIntegrationTest.java`
- [x] T028 [US3] Tạo remote connection-level Supabase readiness test cho cả API và Worker tại `apps/api/src/supabaseIntegrationTest/java/com/cryptostrategy/platform/api/health/SupabaseReadinessIntegrationTest.java`, `apps/worker/src/supabaseIntegrationTest/java/com/cryptostrategy/platform/worker/health/SupabaseReadinessIntegrationTest.java`, cấu hình source set/task trong `apps/api/build.gradle.kts`, `apps/worker/build.gradle.kts` và root aggregation tại `build.gradle.kts`

### Triển khai User Story 3

- [x] T029 [P] [US3] Tạo API/Worker external configuration model và placeholder config tại `apps/api/src/main/resources/application.yml` và `apps/worker/src/main/resources/application.yml`
- [x] T030 [US3] Cấu hình Actuator liveness/readiness và database-only readiness contribution tại `apps/api/src/main/java/com/cryptostrategy/platform/api/config/HealthConfiguration.java` và `apps/worker/src/main/java/com/cryptostrategy/platform/worker/config/HealthConfiguration.java`
- [x] T031 [US3] Cấu hình Worker runtime idle, không queue consumer tại `apps/worker/src/main/java/com/cryptostrategy/platform/worker/config/WorkerRuntimeConfiguration.java`
- [x] T032 [US3] Implement correlation ID resolution/generation, request filter và MDC cleanup tại `apps/api/src/main/java/com/cryptostrategy/platform/api/observability/CorrelationId.java` và `CorrelationIdFilter.java`
- [x] T033 [US3] Implement API error envelope/handler khớp catalog hiện tại tại `apps/api/src/main/java/com/cryptostrategy/platform/api/error/ErrorEnvelope.java` và `ApiExceptionHandler.java`
- [x] T034 [US3] Cấu hình structured JSON logging và secret redaction tại `apps/api/src/main/resources/application.yml`, `apps/worker/src/main/resources/application.yml` và `specs/002-java-backend-foundation/contracts/observability-boundary.md`
- [ ] T035 [US3] Chạy API/Worker health smoke test và root `supabaseIntegrationTest`, xác nhận captured operation không truy vấn/mutation business table, rồi ghi commit/environment/configuration/status/timing không chứa secret tại `specs/002-java-backend-foundation/verification-evidence.md`

**Điểm kiểm tra**: API/Worker liveness đạt trong 30 giây; database mất chỉ làm readiness DOWN; Worker vẫn idle; correlation nhất quán.

---

## Giai đoạn 6: User Story 4 - Xác thực identity tại application boundary (P2)

**Mục tiêu**: Bearer token hợp lệ tạo đúng UUID user context; mọi nhóm token lỗi bị từ chối trước handler mà không thêm public endpoint.

**Kiểm thử độc lập**: Chạy authentication integration matrix bằng local signing key/JWKS và test-only controller, không gọi Supabase Auth.

### Kiểm thử User Story 4

- [x] T036 [US4] Tạo local JWT signing/JWKS fixture và token factory tại `apps/api/src/test/java/com/cryptostrategy/platform/api/auth/JwtTestFixture.java`
- [x] T037 [US4] Tạo test-only protected controller và authentication matrix test tại `apps/api/src/test/java/com/cryptostrategy/platform/api/auth/AuthenticationIntegrationTest.java`

### Triển khai User Story 4

- [x] T038 [US4] Implement immutable authenticated user context tại `apps/api/src/main/java/com/cryptostrategy/platform/api/auth/AuthenticatedUserContext.java`
- [x] T039 [US4] Cấu hình Resource Server issuer/JWKS/audience/UUID-sub validation tại `apps/api/src/main/java/com/cryptostrategy/platform/api/config/SecurityConfiguration.java`
- [x] T040 [US4] Implement safe authentication failure mapping có correlation ID tại `apps/api/src/main/java/com/cryptostrategy/platform/api/auth/AuthenticationFailureHandler.java`
- [ ] T041 [US4] Xác nhận OpenAPI không có endpoint fixture mới và ghi authentication matrix result tại `specs/002-java-backend-foundation/verification-evidence.md`

**Điểm kiểm tra**: Toàn bộ invalid token fixture bị từ chối; valid token đưa đúng UUID vào handler; raw JWT không xuất hiện trong log/response.

---

## Giai đoạn 7: Rà soát chéo và hoàn thiện

**Mục tiêu**: Chứng minh foundation đủ ổn định để ba feature tiếp theo tách nhánh từ cùng commit.

- [ ] T042 Đối chiếu implementation với `specs/002-java-backend-foundation/spec.md`, `plan.md`, `data-model.md` và toàn bộ `contracts/`
- [ ] T043 Chạy `./gradlew clean check` cùng secret scan và hoàn thiện commit/environment/non-secret configuration cùng trạng thái `Verified` thực tế tại `specs/002-java-backend-foundation/verification-evidence.md`
- [ ] T044 Thực hiện toàn bộ validation phù hợp trong `specs/002-java-backend-foundation/quickstart.md` và cập nhật kết quả tại `specs/002-java-backend-foundation/verification-evidence.md`
- [ ] T045 Xác nhận không sửa OpenAPI business contract/applied migration, không âm thầm sửa ADR và chặn merge nếu ADR-0001/0002/0006/0007 chưa `Accepted`; ghi trạng thái tại `specs/002-java-backend-foundation/verification-evidence.md`
- [ ] T046 Luật tổ chức review cuối: Nghi Văn xác nhận Market/Data extension point, Văn Minh xác nhận Strategy extension point, Tiến xác nhận Experiment/Persistence/Worker extension point tại `specs/002-java-backend-foundation/verification-evidence.md`

---

## Quan hệ phụ thuộc và thứ tự thực hiện

- Giai đoạn 1 → Giai đoạn 2 là tuần tự và chặn mọi user story.
- US1 hoàn thiện buildable skeleton trước US2 vì ArchUnit cần bytecode/module structure.
- US2 có thể review song song với phần test của US3 sau khi T014 hoàn thành, nhưng edit build script chung phải tuần tự.
- US3 cung cấp error/correlation foundation cho US4; vì vậy US4 bắt đầu sau T033.
- Giai đoạn 7 chỉ bắt đầu khi US1–US4 đều pass.

### Cơ hội làm song song an toàn

- T004 có thể làm song song T001–T003.
- T010 có thể chuẩn bị song song T011 sau khi convention plugin API được chốt.
- T015 và T016 thay đổi hai application khác nhau.
- T026 có thể làm song song T025; T029 có thể chia theo API/Worker file.
- Nghi Văn, Văn Minh và Tiến có thể review ba phần dependency matrix song song ở T024/T046.

## Chiến lược triển khai

1. Hoàn thành Gradle/build foundation và toàn bộ project skeleton.
2. Chạy US1 để có một backend buildable — đây là MVP foundation nhỏ nhất.
3. Thêm architecture guard trước khi capability team tạo code thật.
4. Thêm health/config/correlation cho hai runtime.
5. Thêm JWT authentication boundary cho API.
6. Chạy review chéo và chỉ merge khi ba owner xác nhận extension point sử dụng được.

## Lưu ý

- Không thêm business entity/service/controller trong F-002.
- Test-only authentication controller không được nằm trong `src/main` hoặc OpenAPI.
- Root `check` không được gọi Supabase, Redis, Binance hoặc network.
- `supabaseIntegrationTest` chỉ đọc connection health và không in credential.
- Không sửa migration đã apply; schema correction phải là forward migration riêng.
- Nếu implementation cần dependency ngoài ADR-0002, dừng và review ADR thay vì mở rule âm thầm.

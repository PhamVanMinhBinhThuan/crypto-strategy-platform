# Bằng chứng kiểm chứng Java Backend Foundation

## Phạm vi

- Feature: `002-java-backend-foundation`
- Branch: `feature/002-java-backend-foundation`
- Trạng thái tổng thể: `In progress`
- Verification commit gần nhất: `bffc1776e00bcf75a919f30820bc5d7b2f617282`
- Local environment: macOS 26.5.2 arm64, OpenJDK 21.0.12.1, Gradle Wrapper 8.14.5
- Shared environment: Supabase development; credential chỉ lấy từ environment
- Non-secret configuration: `offline-check`, `api-local`, `worker-local`, `supabase-readiness`

Không ghi password, token, full JDBC URL hoặc secret value vào tài liệu này.

## Kết quả

| Evidence | Commit | Environment/configuration | Status | Result |
|---|---|---|---|---|
| Root `clean check` | `bffc1776e00bcf75a919f30820bc5d7b2f617282` | `offline-check`; external services unset | Verified | PASS, 1 giây clean run có build cache; không cần Docker, database, Redis hoặc provider |
| Gradle project/module boundary | `7fb41549324e1c967f3d65e65408b8670af074eb` | `offline-check` | Verified | 13/13 build-structure và architecture test PASS; positive cùng internal/technology/cycle/canonical negative fixture đều được kiểm tra |
| API/Worker startup và health | `7fb41549324e1c967f3d65e65408b8670af074eb` | local H2 connection fixture; `api-local`, `worker-local` | Verified | API 3/3 và Worker 2/2 test PASS; liveness độc lập database, readiness theo connection, Worker `IDLE` không Redis/queue consumer |
| Authentication matrix | `7fb41549324e1c967f3d65e65408b8670af074eb` | local RSA signing/JWKS fixture | Verified | 14/14 test PASS; 12 invalid/missing cases bị chặn trước handler, valid token cung cấp đúng UUID; không gọi Supabase Auth |
| Correlation/error/log redaction | `7fb41549324e1c967f3d65e65408b8670af074eb` | `api-local`; local H2 fixture | Verified | 5/5 test PASS; header/envelope/log dùng cùng ID, generated ULID hợp lệ, MDC được xóa và Authorization fixture không xuất hiện trong log/response |
| Supabase readiness | `bffc1776e00bcf75a919f30820bc5d7b2f617282` | `supabase-readiness`; Supabase shared development | Verified | API và Worker PASS trong root `supabaseIntegrationTest --rerun-tasks` (4 giây, 8/8 task executed); readiness chỉ gọi JDBC `Connection.isValid(2)`, không truy vấn/mutation business table; credential không xuất hiện trong output |
| Repository secret scan | `24d18317b975a9a96ee10f02957ef61b05385a0f` | tracked repository; private-key/cloud-token/JWT/credentialed-PostgreSQL-URI patterns | Verified | Không có match; `git diff --check` PASS; working tree sạch tại thời điểm scan |
| Cross-owner extension review | TBD | review artifact | Planned | Chưa review |

OpenAPI business contract tại `docs/api/openapi.yaml` không đổi trong commit verification;
protected authentication controller chỉ tồn tại dưới `src/test`.

## Đối chiếu governance

- Implementation đã được đối chiếu với `spec.md`, `plan.md`, `data-model.md` và bốn
  boundary contract; mã xác thực `AUTHENTICATION_REQUIRED` được đồng bộ vào error catalog.
- `docs/api/openapi.yaml`, migration đã apply và toàn bộ ADR không bị sửa trong F-002.
- ADR-0001, ADR-0002, ADR-0006 và ADR-0007 vẫn là `Proposed`; merge tiếp tục bị chặn
  cho tới khi review chuyển từng ADR liên quan sang `Accepted`.

## Quickstart validation

- `./gradlew projects`: PASS trong 2.0 giây; đủ 2 app, 13 capability,
  `architecture-tests` và included `build-logic`.
- API `bootRun`: startup khoảng 1.1 giây với database cố tình unreachable; liveness
  `200 UP`, readiness `503 DOWN`.
- Worker `bootRun`: startup khoảng 1.0 giây với database cố tình unreachable; liveness
  `200 UP`, readiness `503 DOWN`; automated test xác nhận mode `IDLE`.
- Offline authentication command nằm trong root `check` và 14/14 test PASS.
- `supabaseIntegrationTest`: PASS cho API và Worker trong 4 giây với Supabase shared
  development; custom source set dùng main output và API test dùng mock web context để
  load đúng application/security configuration. Chỉ JDBC connection validation được gọi.

## Cross-owner review

Kịch bản và checklist review nằm tại
`specs/002-java-backend-foundation/review-guide.md`. Điền kết quả thật dưới đây; không
đánh dấu `APPROVED` thay reviewer.

| Review | Reviewer | Commit | Scope | Build | Decision | Notes |
|---|---|---|---|---|---|---|
| T024 Boundary | Nghi Văn | TBD | Market/Data | TBD | TBD | TBD |
| T024 Boundary | Văn Minh | TBD | Strategy + Job/Attempt | TBD | TBD | TBD |
| T024 Boundary | Tiến | TBD | Persistence/Outbox/Worker | TBD | TBD | TBD |
| T046 Final | Nghi Văn | TBD | Market/Data | TBD | TBD | TBD |
| T046 Final | Văn Minh | TBD | Strategy + Job/Attempt | TBD | TBD | TBD |
| T046 Final | Tiến | TBD | Persistence/Outbox/Worker | TBD | TBD | TBD |

## Merge gate

- ADR-0001, ADR-0002, ADR-0006 và ADR-0007 phải `Accepted` trước khi merge.
- Nghi Văn xác nhận Market/Data extension point; Văn Minh xác nhận Strategy và
  Job–Execution Attempt contract extension point; Tiến xác nhận Experiment
  persistence/Outbox/Worker integration extension point.
- Chỉ đổi evidence từ `Planned` sang `Verified` sau khi có output thật có thể xem lại.

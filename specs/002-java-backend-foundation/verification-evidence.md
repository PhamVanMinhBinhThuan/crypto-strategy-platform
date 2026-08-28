# Bằng chứng kiểm chứng Java Backend Foundation

## Phạm vi

- Feature: `002-java-backend-foundation`
- Branch: `feature/002-java-backend-foundation`
- Trạng thái tổng thể: `In progress`
- Verification commit gần nhất: `24d18317b975a9a96ee10f02957ef61b05385a0f`
- Local environment: macOS 26.5.2 arm64, OpenJDK 21.0.12.1, Gradle Wrapper 8.14.5
- Shared environment: Supabase development; credential chỉ lấy từ environment
- Non-secret configuration: `offline-check`, `api-local`, `worker-local`, `supabase-readiness`

Không ghi password, token, full JDBC URL hoặc secret value vào tài liệu này.

## Kết quả

| Evidence | Commit | Environment/configuration | Status | Result |
|---|---|---|---|---|
| Root `clean check` | `24d18317b975a9a96ee10f02957ef61b05385a0f` | `offline-check`; external services unset | Verified | PASS, 17.5 giây clean run; không cần Docker, database, Redis hoặc provider |
| Gradle project/module boundary | `7fb41549324e1c967f3d65e65408b8670af074eb` | `offline-check` | Verified | 13/13 build-structure và architecture test PASS; positive cùng internal/technology/cycle/canonical negative fixture đều được kiểm tra |
| API/Worker startup và health | `7fb41549324e1c967f3d65e65408b8670af074eb` | local H2 connection fixture; `api-local`, `worker-local` | Verified | API 3/3 và Worker 2/2 test PASS; liveness độc lập database, readiness theo connection, Worker `IDLE` không Redis/queue consumer |
| Authentication matrix | `7fb41549324e1c967f3d65e65408b8670af074eb` | local RSA signing/JWKS fixture | Verified | 14/14 test PASS; 12 invalid/missing cases bị chặn trước handler, valid token cung cấp đúng UUID; không gọi Supabase Auth |
| Correlation/error/log redaction | `7fb41549324e1c967f3d65e65408b8670af074eb` | `api-local`; local H2 fixture | Verified | 5/5 test PASS; header/envelope/log dùng cùng ID, generated ULID hợp lệ, MDC được xóa và Authorization fixture không xuất hiện trong log/response |
| Supabase readiness | TBD | `supabase-readiness` | Planned | Chưa chạy |
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
- `supabaseIntegrationTest`: `Planned`; ba database và ba JWT environment variable chưa
  có trong shell, nên không chạy giả hoặc ghi evidence thành công giả.

## Merge gate

- ADR-0001, ADR-0002, ADR-0006 và ADR-0007 phải `Accepted` trước khi merge.
- Nghi Văn, Văn Minh và Tiến phải xác nhận extension point thuộc phạm vi của mình.
- Chỉ đổi evidence từ `Planned` sang `Verified` sau khi có output thật có thể xem lại.

# F014 Final Quality Gates

## EV-US5-QUALITY-001: Java, Web và Python full gates

- Criterion/requirement: T054, FR-022, FR-025, SC-007; chạy toàn bộ gate được repository công bố và ghi rõ mọi skip.
- Status: PARTIAL
- Commit SHA: `50c28d99c02a4ee28ed1109b231daa4397a22fe4`
- Working tree: dirty (implementation và tài liệu F014 chưa commit).
- Captured at: `2026-09-04T10:15:57Z`
- Environment/profile: macOS Darwin 25.5.0 arm64; Apple M2, 8 logical CPU, 16 GiB RAM; Temurin Java 21.0.12.1; Node 22.23.2; Python 3.12.10.
- Expected result: không có failure; format, lint, typecheck, unit/integration tests và production build pass; mọi dependency-backed skip được nêu tên.
- Observed result: Java và Web `BUILD/check` pass, Python pass; Java có 2 test Redis smoke skip theo cờ môi trường, vì vậy evidence tổng hợp giữ `PARTIAL` theo quy tắc evidence dù không có failure.
- Artifact links: `build/reports/tests/test/index.html`, `apps/api/build/reports/tests/test/index.html`, `apps/worker/build/reports/tests/test/index.html`, `apps/web/.next/`, `apps/sentiment/.pytest_cache/`.
- Limitations: Gradle `check` không bật các source set cần PostgreSQL/shared dependency; Web unit gate không thay thế Playwright; Python chạy core extra, không có TensorFlow runtime.
- Owner/reviewer: implementer F014 / pending reviewer.

## Kết quả chi tiết

| Gate   | Lệnh                                                                       | Kết quả                                                                                     |
| ------ | -------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------- |
| Java   | `JAVA_HOME=<JDK_21> ./gradlew clean check --no-daemon`                     | **PASS** — 545 tests, 0 failure, 0 error, 2 skipped; `BUILD SUCCESSFUL in 29s`              |
| Web    | `NEXT_PUBLIC_*=<safe build placeholders> npm run check` bằng Node 22       | **PASS** — Prettier, ESLint, TypeScript, 96 Vitest files/275 tests và Next production build |
| Python | `/tmp/f014-sentiment-baseline/bin/python -m pytest` trong `apps/sentiment` | **PASS** — 10 tests; 1 warning deprecation từ Starlette/AnyIO                               |

Hai test bị skip trong Java full gate:

1. `F014RecoveryScenarioTest.crashedConsumerIsReclaimedWithoutDuplicateOutcomeAndRedisLossDoesNotDeleteDurableState`
2. `RealtimeRedisRecoveryIntegrationTest.resumesNotificationDeliveryAfterRedisClientConnectionLoss`

Hai test này cần bật Redis smoke flag nên không tự chạy trong `check`. Lần chạy dependency-backed riêng đã pass với Redis thật và được lưu tại `failure-recovery.md`; skip vẫn được công bố, không bị tính thành pass của lệnh tổng.

## Remediation quan sát trong lần chạy

- Lần Web build đầu tiên dừng vì bốn biến public bắt buộc chưa được inject. Chạy lại bằng URL và key placeholder an toàn, `NEXT_PUBLIC_ENABLE_FIXTURES=false`; production build pass. Placeholder không phải credential và không được dùng làm evidence LIVE.
- Public 5xx logging được sửa để chỉ log error code, HTTP status và loại exception; full API tests xác nhận secret trong exception/cause/provider context không xuất hiện ở response hoặc log.
- Public reproduction DTO dùng ID do Execution sở hữu, loại bỏ dependency ngược API → Search; architecture tests pass.

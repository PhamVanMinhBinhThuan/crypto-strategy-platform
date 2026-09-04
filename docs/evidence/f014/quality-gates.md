# F014 Final Quality Gates

## EV-US5-QUALITY-001: Java, Web và Python full gates

- Criterion/requirement: T054, T061, FR-022, FR-025, SC-007; chạy toàn bộ gate được repository công bố và ghi rõ mọi skip.
- Status: VERIFIED
- Commit SHA: `0761a54bfcbb93c3b24cb216b19d6cc79e03e21b`
- Working tree: clean detached worktree khi bắt đầu gate; Next build cập nhật generated `next-env.d.ts` sau kiểm tra.
- Captured at: `2026-09-04T12:22:15Z`
- Environment/profile: macOS Darwin 25.5.0 arm64; Apple M2, 8 logical CPU, 16 GiB RAM; Temurin Java 21.0.12.1; Node 22.23.2; Python 3.12.10.
- Expected result: không có failure; format, lint, typecheck, tests và production build pass; dependency-backed skip phải được nêu tên và chạy bù.
- Observed result: Java và Web full gate pass, Python pass. Java full gate có 2 Redis smoke skip theo cờ môi trường; cả hai required scenario được chạy riêng với Redis thật và pass trên cùng SHA.
- Artifact links: `build/reports/tests/test/index.html`, `apps/api/build/reports/tests/test/index.html`, `apps/worker/build/reports/tests/test/index.html`, `apps/web/.next/`, `apps/sentiment/.pytest_cache/`.
- Limitations: Gradle `check` không bật PostgreSQL/shared dependency source sets; Web unit gate không thay thế Playwright; Python chạy core extra, không có TensorFlow runtime.
- Owner/reviewer: implementer F014 / pending reviewer.

## Kết quả chi tiết

| Gate | Lệnh | Kết quả |
|---|---|---|
| Java | `JAVA_HOME=<JDK_21> ./gradlew clean check --no-daemon` | **PASS** — 545 tests, 0 failure/error, 2 declared skips; `BUILD SUCCESSFUL in 33s` |
| Redis scenarios | `F014_REDIS_SMOKE=true F009_REDIS_SMOKE=true ./gradlew <2 focused tests>` | **PASS** — 2/2 previously skipped scenarios; `BUILD SUCCESSFUL in 32s` |
| Web | `NEXT_PUBLIC_*=<safe build placeholders> npm run check` bằng Node 22 | **PASS** — Prettier, ESLint, TypeScript, 96 Vitest files/275 tests và Next production build |
| Python | `/tmp/f014-sentiment-baseline/bin/python -m pytest` trong `apps/sentiment` | **PASS** — 10 tests; 1 warning deprecation từ Starlette/AnyIO |

Hai test được khai skip trong Java full gate và chạy lại riêng:

1. `F014RecoveryScenarioTest.crashedConsumerIsReclaimedWithoutDuplicateOutcomeAndRedisLossDoesNotDeleteDurableState`
2. `RealtimeRedisRecoveryIntegrationTest.resumesNotificationDeliveryAfterRedisClientConnectionLoss`

Skip của lệnh tổng vẫn được công bố và chỉ được bù bằng dependency-backed result trên cùng SHA, không bị tự tính thành pass.

## Remediation đã kiểm chứng

- Web build dùng URL/key placeholder an toàn và `NEXT_PUBLIC_ENABLE_FIXTURES=false`; placeholder không phải credential hay evidence LIVE.
- Public 5xx log chỉ giữ safe code, HTTP status và exception type; full API tests xác nhận message/cause/provider context không rò ra response hoặc log.
- Public reproduction DTO dùng ID do Execution sở hữu, không tạo dependency API → Search; architecture tests pass.

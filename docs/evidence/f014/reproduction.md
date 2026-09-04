# F014 Provenance and Reproduction Evidence

## Kết luận hiện tại

Canonical Result provenance, linked reproduction command và durable verification read boundary đã pass API/component/Playwright tests. Lần chạy PostgreSQL remote phát hiện database chưa áp dụng migration F006 nên chưa thể tạo evidence runtime thật. Vì vậy T044 và hồ sơ này vẫn `PARTIAL`; controlled browser journey không được dùng thay LIVE reproduction.

## EV-US3-001: Canonical provenance và linked reproduction

- Criterion/requirement: US3, FR-018–FR-021, SC-006.
- Status: PARTIAL
- Commit SHA: `50c28d99c02a4ee28ed1109b231daa4397a22fe4`
- Working tree: dirty (implementation F014 chưa commit).
- Captured at: `2026-09-04T09:36:00Z`
- Environment/profile: Java 21 API tests; Node 22.23.2 Vitest và Playwright desktop controlled profile.
- Non-secret configuration: source Experiment `experiment-source-014`; reproduction Experiment `experiment-reproduction-014`; deterministic verdict `MATCHED` trong controlled test.
- Command/action: chạy focused API tests cho `ReproduceExperimentIntegrationTest`, `BacktestResultApiTest`, documentation parity; chạy Vitest focused suite, TypeScript typecheck và `npm run test:e2e -- tests/e2e/f014-reproduction.spec.ts --project=desktop`.
- Expected result: Result truy được Experiment, Candidate, successful Attempt, Dataset version/checksum, Strategy version/parameters và assumptions; reproduction tạo run mới liên kết nguồn, không sửa source artifacts và trả verdict durable.
- Observed result: API/contract tests `BUILD SUCCESSFUL`; 9 focused Web tests pass; TypeScript pass; Playwright `1 passed`. UI mở canonical Result và hiển thị evidence chain, tạo reproduction bằng một POST có idempotency key, mở run mới có `reproducesExperimentId`, đọc public verification boundary và hiển thị `MATCHED` cho trades/metrics/fingerprints.
- Immutable-source assertion: trước và sau reproduction, Manifest, Candidate, accepted Result, Trade, Evaluation và Leaderboard revision nguồn giữ nguyên; target dùng runtime IDs mới nhưng giữ canonical fingerprints tương ứng.
- Artifact links: `apps/api/src/test/java/com/cryptostrategy/platform/api/experiment/ReproduceExperimentIntegrationTest.java`; `apps/api/src/test/java/com/cryptostrategy/platform/api/backtest/BacktestResultApiTest.java`; `apps/web/tests/e2e/f014-reproduction.spec.ts`; `apps/api/build/reports/tests/test/index.html`; `apps/web/playwright-report/index.html`.
- Limitations: browser data được route có điều khiển; chưa có source/result/verification ID từ live database. Không nâng record này thành `VERIFIED`.
- Owner/reviewer: project team / pending reviewer.

## EV-US3-DB-001: PostgreSQL integration attempt

- Criterion/requirement: T044 chạy reproduction thật trên durable storage.
- Status: BLOCKED
- Commit SHA: `50c28d99c02a4ee28ed1109b231daa4397a22fe4`
- Working tree: dirty (implementation F014 chưa commit).
- Captured at: `2026-09-04T09:21:22Z`
- Environment/profile: shared PostgreSQL configured by local server-only environment; Java 21.
- Command/action: `./gradlew :modules:persistence:experimentIntegrationTest --tests com.cryptostrategy.platform.persistence.internal.experiment.SearchReproductionIntegrationTest --no-daemon` sau khi inject ba biến `DATABASE_*` server-side.
- Expected result: tạo source graph, atomic copy, chạy verification và đọc `MATCHED` trong PostgreSQL.
- Observed result: test không tới bước reproduction vì schema remote trả `column "job_id" of relation "backtest_result" does not exist`. Cột này thuộc migration `20260901000100_f006_backtest_evaluation_leaderboard.sql`; do đó database đang chậm migration hơn code.
- Artifact links: `modules/persistence/build/reports/tests/experimentIntegrationTest/index.html`; `supabase/migrations/20260901000100_f006_backtest_evaluation_leaderboard.sql`.
- Limitations: không tự áp dụng migration lên shared database vì đó là thay đổi external state cần đúng quy trình/owner. Không có live verdict hoặc screenshot.
- Owner/reviewer: database owner / pending reviewer.

## Ảnh minh chứng có thể lấy

### Có thể lấy ngay nhưng phải ghi `CONTROLLED/TEST`

1. Trang Result hiển thị `Dataset evidence`, checksum, Strategy implementation/parameters, Candidate definition, successful Attempt và assumptions.
2. Trang source Experiment với nút `Reproduce Experiment` chỉ khả dụng khi `COMPLETED/STOPPED`.
3. Trang target Experiment hiển thị `Linked reproduction of Experiment ...` và verdict `MATCHED`, gồm ba dòng ordered trades, metrics, fingerprints.

### Ảnh nên dùng cho sheet chính sau khi unblock LIVE

1. Chạy migrations theo đúng quy trình và xác nhận schema F006/F010.
2. Đăng nhập bằng development user, hoàn thành một Experiment nhỏ.
3. Mở Top-K Result và chụp evidence chain cùng result/candidate ID.
4. Nhấn reproduce, chụp target Experiment ID khác source ID và liên kết nguồn.
5. Chờ verdict terminal rồi chụp `MATCHED` hoặc `MISMATCHED`; ghi verification ID và timestamp đã redact vào sheet.

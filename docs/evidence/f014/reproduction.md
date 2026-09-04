# F014 Provenance and Reproduction Evidence

## Kết luận hiện tại

Canonical Result provenance, linked reproduction command và durable verification read boundary đã pass API/component/Playwright tests. EV-US3-DB-002 đã áp dụng migration được version-control, chạy application service cùng verification coordinator trên PostgreSQL remote và ghi verdict/artifact IDs thật vào JUnit report. T044 đã hoàn thành; full authenticated browser journey và screenshot vẫn thuộc T029, không được thay bằng integration-test evidence.

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

## EV-US3-DB-002: PostgreSQL reproduction và immutable source

- Criterion/requirement: T044, FR-018–FR-021, SC-006; remediation cho EV-US3-DB-001.
- Status: VERIFIED
- Commit SHA: `0d87e16b34544bd8d45deafaea4ca7b6a6f2a02b`
- Captured at: `2026-09-04T12:52:45Z`
- Environment/profile: shared PostgreSQL 17.6, Java 21, Gradle `experimentIntegrationTest`; credential chỉ inject từ local server-only environment.
- Non-secret configuration: migration history `20260827000100` đến `20260904000100`; source fixture được tạo qua persistence/application boundary trong transaction và rollback sau test.
- Command/action: chạy `supabase db push --dry-run --include-all --skip-vault`; reconcile riêng history F010 sau khi kiểm tra đủ table/column/constraint/index/trigger/privilege; chạy `supabase db push`; xác nhận dry-run trả `upToDate=true`; chạy focused `SearchReproductionIntegrationTest --rerun-tasks`.
- Expected result: F006/F009/F010 schema cùng tồn tại; reproduction tạo Experiment/verification mới, giữ source graph bất biến và trả verdict deterministic.
- Observed result: sáu migration pending đã apply thành công; postflight có đủ `backtest_result.job_id`, fee components, F006 verification, standalone Backtest, F010 Search/verification và correlation ID `varchar(128)`. Focused PostgreSQL suite pass `4/4`; suite persistence đầy đủ trên cùng commit pass `29/29`, zero skip/failure. Verdict cuối `MATCHED`; idempotent replay giữ cùng reproduction Experiment và request hash khác trả conflict; source snapshot trước/sau bằng nhau.
- Artifact IDs: source Experiment `62000000000000000000000001`; reproduction Experiment `01M1P7K6YEMXKRFCS1AWP4BMS9`; verification `01M1P7K6YF0D7JZ1WMHYP00QBK`; Result source/target `62000000000000000000000075`/`62000000000000000000000085`; Evaluation source/target `62000000000000000000000076`/`62000000000000000000000086`; Leaderboard revision source/target `62000000000000000000000077`/`62000000000000000000000087`.
- Artifact links: `modules/persistence/build/test-results/experimentIntegrationTest/TEST-com.cryptostrategy.platform.persistence.internal.experiment.SearchReproductionIntegrationTest.xml`; `modules/persistence/build/reports/tests/experimentIntegrationTest/index.html`; `modules/persistence/src/experimentIntegrationTest/java/com/cryptostrategy/platform/persistence/internal/experiment/SearchReproductionIntegrationTest.java`; `supabase/migrations/20260901000100_f006_backtest_evaluation_leaderboard.sql`.
- Limitations: transaction rollback bảo đảm không để test record trong shared database; đây là dependency-backed PostgreSQL evidence, không phải screenshot authenticated Web. Full browser flow vẫn cần T029.
- Owner/reviewer: database owner / pending cross-owner reviewer.

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

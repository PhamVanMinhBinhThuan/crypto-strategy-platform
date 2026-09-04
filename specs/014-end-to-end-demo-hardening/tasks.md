# Tasks: F-014 — End-to-End Demo and Hardening

**Input**: Design documents from `/specs/014-end-to-end-demo-hardening/`

**Tests**: Bắt buộc vì specification yêu cầu evidence tự động cho acceptance, failure/recovery, security, performance và accessibility.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Có thể thực hiện song song vì không sửa cùng file và không phụ thuộc task chưa xong
- **[USn]**: Ánh xạ trực tiếp tới User Story trong `spec.md`

## Phase 1: Setup — Baseline và evidence structure

**Purpose**: Ghi trạng thái thật trước khi sửa và chuẩn bị nơi lưu evidence không chứa secret.

- [x] T001 Chạy baseline Java, web và sentiment gates theo `specs/014-end-to-end-demo-hardening/quickstart.md`, ghi command/pass/fail/skip vào `docs/evidence/f014/baseline.md`
- [x] T002 [P] Tạo bảng đủ 24 dòng rubric (23 cốt lõi + 1 mở rộng tùy chọn) với owner, requirement, trạng thái `PLANNED|BLOCKED|PARTIAL|VERIFIED`, evidence và remediation trong `docs/evidence/f014/rubric-matrix.md`
- [x] T003 [P] Tạo live/fixture demo profile mẫu không chứa credential theo contract trong `docs/demo/f014/demo-profiles.md`
- [x] T004 [P] Tạo template Evidence Record và quy tắc redaction trong `docs/evidence/f014/README.md`
- [x] T005 Audit scripts, health endpoints và environment variables hiện có; ghi startup dependency/gap vào `docs/demo/f014/environment-audit.md`

---

## Phase 2: Foundational — Blocking integration gaps

**Purpose**: Bảo đảm bốn Strategy và boundary nền tảng sẵn sàng trước các user story.

**⚠️ CRITICAL**: Phải hoàn thành trước khi triển khai các luồng demo.

- [x] T006 [P] Viết deterministic/contract tests cho RSI plugin trong `modules/strategies/src/test/java/com/cryptostrategy/platform/strategies/internal/rsi/RsiStrategyTest.java`
- [x] T007 [P] Viết deterministic/contract tests cho Bollinger plugin trong `modules/strategies/src/test/java/com/cryptostrategy/platform/strategies/internal/bollinger/BollingerBandsStrategyTest.java`
- [x] T008 [P] Viết deterministic/contract tests cho Support/Resistance plugin trong `modules/strategies/src/test/java/com/cryptostrategy/platform/strategies/internal/support/SupportResistanceStrategyTest.java`
- [x] T009 [P] Implement RSI Strategy và plugin qua contract hiện hữu trong `modules/strategies/src/main/java/com/cryptostrategy/platform/strategies/internal/rsi/`
- [x] T010 [P] Implement Bollinger Bands Strategy và plugin qua contract hiện hữu trong `modules/strategies/src/main/java/com/cryptostrategy/platform/strategies/internal/bollinger/`
- [x] T011 [P] Implement Support/Resistance Strategy và plugin qua contract hiện hữu trong `modules/strategies/src/main/java/com/cryptostrategy/platform/strategies/internal/support/`
- [x] T012 Đăng ký đủ MA, RSI, Bollinger Bands và Support/Resistance, đồng thời kiểm tra ID/version duy nhất trong `modules/strategies/src/main/java/com/cryptostrategy/platform/strategies/api/StrategyPlugins.java`
- [x] T013 Bổ sung registry/catalog tests cho bốn Strategy trong `modules/strategies/src/test/java/com/cryptostrategy/platform/strategies/api/StrategyPluginsTest.java`
- [x] T014 Kiểm chứng public Strategy catalog và validation dùng cùng contract cho cả bốn Strategy trong `apps/api/src/test/java/com/cryptostrategy/platform/api/strategy/StrategyApiIntegrationTest.java`
- [x] T015 Audit public API/WebSocket mappings so với `contracts/integrated-demo-contract.md`, ghi mọi gap có file/owner cụ thể vào `docs/evidence/f014/integration-gap-register.md`
- [x] T016 Chạy module architecture/contract gates sau remediation và lưu evidence vào `docs/evidence/f014/foundation-gates.md`

**Checkpoint**: Catalog bốn Strategy và các boundary nền tảng đã sẵn sàng.

---

## Phase 3: User Story 1 — Luồng nghiên cứu Strategy hoàn chỉnh (Priority: P1) 🎯 MVP

**Goal**: Người dùng đã đăng nhập chạy liên tục Market → Strategy → Search → Backtest → Evaluation → Leaderboard → Result/Trades → News/Sentiment.

**Independent Test**: Chạy browser journey trên live profile trong tối đa 10 phút, không sửa source/database; bốn chart và bốn Strategy hiện diện, Result có Trades và đúng bốn metrics.

### Tests for User Story 1

- [x] T017 [P] [US1] Viết Playwright journey cho bốn chart/timeframe độc lập và xác nhận fixture badge chỉ xuất hiện ở fixture profile trong `apps/web/tests/e2e/f014-market-demo.spec.ts`
- [x] T018 [P] [US1] Viết Playwright journey tạo Strategy cá nhân, tạo composite có conflict rule từ Strategy hệ thống, dùng nó trong Search rồi mở Leaderboard → Result trong `apps/web/tests/e2e/f014-research-flow.spec.ts`
- [x] T019 [P] [US1] Viết Playwright journey News/Sentiment trong `apps/web/tests/e2e/f014-news-demo.spec.ts`
- [x] T020 [P] [US1] Bổ sung backend integration test cho candidate → result → evaluation → leaderboard identity trong `apps/api/src/test/java/com/cryptostrategy/platform/api/experiment/F014ResearchFlowIntegrationTest.java`

### Implementation for User Story 1

- [x] T021 [P] [US1] Hardening four-chart loading, independent timeframe, freshness và reconnect state trong `apps/web/src/features/market/components/MarketDashboard.tsx`
- [x] T022 [P] [US1] Hiển thị đủ bốn published Strategy và nối hành động tạo/lưu Strategy cá nhân từ Strategy nền qua authorized API trong `apps/web/src/features/strategy/components/StrategyCatalog.tsx` và `apps/web/src/features/strategy/components/StrategyForm.tsx`
- [x] T023 [US1] Nối Strategy selection, Random Search, search space và finite stop condition vào request thật trong `apps/web/src/features/experiments/components/ExperimentConfigurationForm.tsx`
- [x] T024 [US1] Hardening start/progress/terminal/reconcile flow, loại bỏ mọi browser-side simulation khỏi live profile trong `apps/web/src/features/experiments/components/SearchView.tsx`
- [x] T025 [P] [US1] Bảo đảm Top-K revision mở đúng authoritative result trong `apps/web/src/features/leaderboard/components/LeaderboardTable.tsx`
- [x] T026 [P] [US1] Hiển thị Entry/Exit evidence, Trades, Return, Win Rate, Maximum Drawdown và Trade Count trong `apps/web/src/features/backtests/components/BacktestResultsView.tsx`
- [x] T027 [P] [US1] Hardening News/Sentiment live, empty và degraded states trong `apps/web/src/features/news/components/NewsWorkspace.tsx`
- [x] T028 [US1] Kiểm tra và remediate mapping/authorization gap đã ghi ở T015 tại Experiment, Strategy, Leaderboard, Backtest Result và News public boundaries trong `apps/api/src/main/java/com/cryptostrategy/platform/api/`
- [ ] T029 [US1] Chạy full live journey, lưu timing, screenshots/log IDs và kết quả thật vào `docs/evidence/f014/main-flow.md`

**Checkpoint**: Luồng demo chính hoạt động độc lập và có evidence thật.

---

## Phase 4: User Story 2 — Failure isolation và recovery (Priority: P1)

**Goal**: Dependency lỗi không làm mất durable state, tạo outcome trùng hoặc đánh lừa người dùng.

**Independent Test**: Tắt sentiment và interrupt worker/queue trong hai scenario riêng; Market/technical Backtest vẫn hợp lệ, job được recover và accepted result không trùng.

### Tests for User Story 2

- [x] T030 [P] [US2] Mở rộng sentiment isolation integration test cho end-to-end degraded/recovery state trong `apps/api/src/test/java/com/cryptostrategy/platform/api/news/NewsDegradedIsolationTest.java`
- [x] T031 [P] [US2] Viết worker/Redis interruption integration test chứng minh reclaim không tạo outcome trùng và durable Experiment, Job, Result, News, Sentiment, publication state vẫn đọc được trong `apps/worker/src/test/java/com/cryptostrategy/platform/worker/engine/F014RecoveryScenarioTest.java`
- [x] T032 [P] [US2] Mở rộng Redis snapshot/reconcile failure test trong `apps/api/src/test/java/com/cryptostrategy/platform/api/realtime/RealtimeRedisRecoveryIntegrationTest.java`
- [x] T033 [P] [US2] Viết Playwright degraded/recovery UI test trong `apps/web/tests/e2e/f014-failure-recovery.spec.ts`

### Implementation for User Story 2

- [x] T034 [US2] Remediate bounded retry, terminal status, reclaim và duplicate-safety gaps trong `apps/worker/src/main/java/com/cryptostrategy/platform/worker/engine/RecoverySweeperEngine.java`
- [x] T035 [P] [US2] Remediate market freshness/reconnect/backfill reconciliation gaps trong `apps/api/src/main/java/com/cryptostrategy/platform/api/realtime/SnapshotCoordinator.java`
- [x] T036 [P] [US2] Bảo đảm UI phân biệt ready/degraded/stale/recovering không chỉ bằng màu trong `apps/web/src/components/states/DegradedState.tsx`
- [ ] T037 [US2] Chạy hai failure scenario thật và lưu timeline, correlation IDs, duplicate assertions và recovery result vào `docs/evidence/f014/failure-recovery.md`

**Checkpoint**: Hai failure scenario có thể chạy lại và không phá luồng chính.

---

## Phase 5: User Story 3 — Provenance và reproduction (Priority: P1)

**Goal**: Mọi leaderboard result truy vết được và reproduction tạo run mới với kết luận deterministic.

**Independent Test**: Chọn một Top-K entry, truy tới dataset checksum, strategy/version/parameters, assumptions, candidate/attempt/trades; reproduce không overwrite và trả `MATCHED` hoặc `MISMATCHED`.

### Tests for User Story 3

- [x] T038 [P] [US3] Mở rộng reproduction integration assertions để chứng minh Manifest, Candidate, accepted Result, Trade, Evaluation và Leaderboard revision nguồn đều bất biến, run mới có linkage và verdict trong `apps/api/src/test/java/com/cryptostrategy/platform/api/experiment/ReproduceExperimentIntegrationTest.java`
- [x] T039 [P] [US3] Mở rộng result API provenance contract test trong `apps/api/src/test/java/com/cryptostrategy/platform/api/backtest/BacktestResultApiTest.java`
- [x] T040 [P] [US3] Viết Playwright provenance/reproduction journey trong `apps/web/tests/e2e/f014-reproduction.spec.ts`

### Implementation for User Story 3

- [x] T041 [US3] Remediate canonical result/provenance mapping gap trong `apps/api/src/main/java/com/cryptostrategy/platform/api/backtest/BacktestResultByIdController.java`
- [x] T042 [US3] Hiển thị đầy đủ evidence chain và reproduction comparison trong `apps/web/src/features/backtests/components/ResultEvidence.tsx`
- [x] T043 [US3] Nối reproduction command/state/reconciliation vào `apps/web/src/features/experiments/components/ExperimentActions.tsx`
- [ ] T044 [US3] Chạy reproduction thật, chứng minh source immutable và lưu verdict/artifact IDs vào `docs/evidence/f014/reproduction.md`

**Checkpoint**: Provenance và reproduction được kiểm chứng độc lập.

---

## Phase 6: User Story 4 — Runbook và evidence lặp lại được (Priority: P2)

**Goal**: Một thành viên khác có thể dựng, chạy main/fallback scenario và tìm evidence mà không cần hướng dẫn ngoài tài liệu.

**Independent Test**: Người không triển khai F014 làm theo runbook từ môi trường sạch, hoàn thành flow và xác định evidence của từng bước.

- [x] T045 [P] [US4] Viết startup, health, seed/setup, main flow, failure, fallback và cleanup runbook trong `docs/demo/f014/runbook.md`
- [x] T046 [P] [US4] Viết checklist demo có expected result, owner và evidence link trong `docs/demo/f014/demo-checklist.md`
- [x] T047 [P] [US4] Cập nhật entry point và liên kết F014 trong `README.md`
- [x] T048 [P] [US4] Đồng bộ luồng tích hợp và failure boundary trong `docs/architecture/data-flows.md`
- [x] T049 [US4] Thực hiện dry run bằng runbook, ghi mọi bước cần kiến thức ngoài tài liệu và remediation vào `docs/evidence/f014/runbook-dry-run.md`
- [x] T050 [US4] Hoàn tất 23 tiêu chí cốt lõi bằng evidence hoặc gap trung thực và chỉ khai dòng mở rộng nếu có bằng chứng vượt yêu cầu trong `docs/evidence/f014/rubric-matrix.md`

**Checkpoint**: Demo package có thể bàn giao cho người khác.

---

## Phase 7: User Story 5 — Final quality gates (Priority: P2)

**Goal**: Commit ứng viên vượt security, performance, accessibility/responsive và automated gates; limitation/skip được công bố.

**Independent Test**: Chạy release checklist trên đúng commit; không có secret thật, benchmark đủ ba lần, keyboard/viewport pass và mọi skip được liệt kê.

### Tests and verification for User Story 5

- [x] T051 [P] [US5] Viết keyboard và viewport 360/768/1024/1440 Playwright checks trong `apps/web/tests/e2e/f014-accessibility-responsive.spec.ts`
- [x] T052 [P] [US5] Chuẩn hóa workload và three-run benchmark harness trong `apps/api/src/test/java/com/cryptostrategy/platform/api/performance/F014DemoPerformanceTest.java`
- [x] T053 [P] [US5] Tạo script scan tracked files và browser artifact với allowlist an toàn trong `scripts/security/scan-demo-secrets.sh`
- [x] T054 [US5] Chạy Java/web/Python full gates và ghi pass/fail/skip theo commit vào `docs/evidence/f014/quality-gates.md`
- [x] T055 [US5] Chạy benchmark ba lần, lưu từng result, median và environment profile vào `docs/evidence/f014/performance.md`
- [x] T056 [US5] Chạy secret/security scan và lưu command/scope/redacted result vào `docs/evidence/f014/security.md`
- [x] T057 [US5] Chạy keyboard/responsive suite và lưu viewport-specific result vào `docs/evidence/f014/accessibility-responsive.md`
- [x] T058 [US5] Lập release checklist gồm dependency, known limitation, rollback/fallback và unresolved gate trong `docs/demo/f014/release-checklist.md`

**Checkpoint**: F014 chỉ được gọi hoàn tất khi mọi tiêu chí bắt buộc Verified hoặc gap còn lại được công bố là blocker, không tự nhận pass.

---

## Phase 8: Polish & Cross-Cutting Concerns

- [x] T059 [P] Đồng bộ trạng thái F014 và liên kết spec/demo/evidence trong `docs/implementation-roadmap.md`
- [x] T060 [P] Kiểm tra tài liệu F014 không chứa secret, số liệu giả, link hỏng hoặc tuyên bố financial advice trong `specs/014-end-to-end-demo-hardening/`
- [x] T061 Re-run toàn bộ `specs/014-end-to-end-demo-hardening/quickstart.md` trên commit cuối và cập nhật evidence commit SHA trong `docs/evidence/f014/`
- [x] T062 Review Constitution, ADR, ownership, public contract và UI-reference compliance; ghi kết luận trong `docs/evidence/f014/release-review.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- Phase 1 không có dependency.
- Phase 2 phụ thuộc Phase 1 và chặn tất cả User Story.
- US1, US2 và US3 cùng P1; sau foundation có thể làm song song, nhưng dry run tổng thể cần cả ba.
- US4 phụ thuộc US1–US3 vì runbook phải mô tả hành vi thật.
- US5 có thể chuẩn bị test song song, nhưng evidence cuối phụ thuộc luồng và runbook ổn định.
- Polish phụ thuộc tất cả story mục tiêu.

### User Story Dependencies

```text
Setup → Foundation ┬→ US1 Core flow ─┐
                   ├→ US2 Recovery ──┼→ US4 Runbook → US5 Final gates → Polish
                   └→ US3 Provenance ┘
```

### Parallel Opportunities

- T002–T004 có thể chạy song song sau T001 bắt đầu.
- Tests và implementations của ba Strategy T006–T011 tách theo package; T012 chờ cả ba.
- T017–T020 và các UI owner T021/T022/T025–T027 có thể chia theo capability.
- Failure tests T030–T033 và provenance tests T038–T040 độc lập theo runtime.
- Documentation T045–T048 và quality preparation T051–T053 có thể thực hiện song song khi behavior đã ổn định.

## Parallel Example: User Story 1

```text
Task T017: Market four-chart Playwright test
Task T018: Research-flow Playwright test
Task T019: News/Sentiment Playwright test
Task T020: Backend identity integration test
```

## Implementation Strategy

### MVP First

1. Hoàn thành Setup và Foundation.
2. Hoàn thành US1 và chạy live main-flow evidence.
3. Dừng để xác nhận luồng nghiên cứu cốt lõi thật sự hoạt động.

### Incremental Hardening

1. Thêm US2 để chứng minh failure isolation/recovery.
2. Thêm US3 để khóa provenance/reproduction.
3. Viết và dry-run US4 trên behavior đã ổn định.
4. Chạy US5, polish và đóng rubric matrix.

## Notes

- Test task được viết trước implementation liên quan và phải chứng minh failure trước khi sửa khi khả thi.
- `[P]` chỉ có nghĩa là khác file/không phụ thuộc trực tiếp; vẫn phải bảo toàn thay đổi chung trong worktree.
- Không đánh dấu `VERIFIED` bằng fixture cho tiêu chí live và không tính skipped test là pass.
- Mỗi task hoặc nhóm logic nên có commit/evidence rõ để truy vết.

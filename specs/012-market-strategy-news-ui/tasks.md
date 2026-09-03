# Tasks: Market, Strategy and News UI (F-012)

**Input**: Design documents từ `/specs/012-market-strategy-news-ui/`

**Prerequisites**: `spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/`, `quickstart.md`

**Organization**: Công việc được nhóm theo user story; test/contract evidence đi trước implementation
của từng slice để mỗi story có thể kiểm chứng độc lập.

## Phase 1: Setup

**Mục đích**: Khóa baseline F-011/F-009, feature boundaries và fixture surface trước khi làm UI.

- [ ] T001 Xác nhận branch, feature pointer, Node 22, clean baseline và F-011 handoff bằng `specs/012-market-strategy-news-ui/quickstart.md`, `.specify/feature.json` và `apps/web/docs/frontend-foundation-handoff.md`
- [ ] T002 [P] Tạo Market feature-first directory/barrel boundaries trong `apps/web/src/features/market/`
- [ ] T003 [P] Tạo Strategy feature-first directory/barrel boundaries trong `apps/web/src/features/strategy/`
- [ ] T004 [P] Tạo News feature-first directory/barrel boundaries trong `apps/web/src/features/news/`
- [ ] T005 [P] Tạo shared F-012 sanitized REST/realtime fixtures trong `apps/web/tests/fixtures/f012/`
- [ ] T006 [P] Thêm route ownership và feature import rules vào `apps/web/tests/architecture/f012-feature-boundaries.test.ts`
- [ ] T007 Thêm F-012 test discovery/coverage paths nếu cần mà không đổi runtime dependency trong `apps/web/vitest.config.ts`
- [ ] T008 Chạy baseline `npm run check` và ghi command/commit/result thật vào `specs/012-market-strategy-news-ui/quickstart.md`

## Phase 2: Foundational

**Mục đích**: Tạo typed adapter/state primitives và safety gates dùng chung cho cả ba route.

- [ ] T009 [P] Viết F-009 OpenAPI DTO parity tests cho Candle/Strategy/News trong `apps/web/tests/contracts/f012-openapi-parity.test.ts`
- [ ] T010 [P] Viết WebSocket Candle subscription/event parity tests trong `apps/web/tests/contracts/f012-realtime-parity.test.ts`
- [ ] T011 [P] Viết forbidden browser boundary tests cho Supabase business table, Binance, internal sentiment và duplicate clients trong `apps/web/tests/architecture/f012-browser-boundaries.test.ts`
- [ ] T012 [P] Tạo reusable Zod exact-string/UTC/cursor validation primitives trong `apps/web/src/features/shared/public-contract.ts`
- [ ] T013 [P] Tạo latest-request generation/abort helper chặn stale response trong `apps/web/src/features/shared/latest-request.ts`
- [ ] T014 [P] Tạo safe URL query canonicalization helper không chứa private payload trong `apps/web/src/features/shared/url-state.ts`
- [ ] T015 Tạo typed public DTO schemas cho Market/Strategy/News theo F-009 trong `apps/web/src/features/shared/f009-schemas.ts`
- [ ] T016 Tạo feature-safe API adapter wrapper trên duy nhất F-011 `ApiClient` trong `apps/web/src/features/shared/feature-api.ts`
- [ ] T017 Viết foundational schema/request/URL helper tests trong `apps/web/tests/foundation/f012-shared-contracts.test.ts`

**Checkpoint**: Contract parsing, request ownership, URL state và browser safety boundary sẵn sàng.

## Phase 3: User Story 1 — Theo dõi thị trường (P1)

**Goal**: Hiển thị Candle snapshot/realtime đúng selection, kết nối trung thực và chart accessible.

**Independent Test**: Mở `/market`, tải ordered Candle, nhận update, đổi selection và reconnect;
không duplicate/stale overwrite, snapshot vẫn đọc được khi realtime mất.

### Tests

- [ ] T018 [P] [US1] Viết Candle DTO/exact-decimal/UTC validation tests trong `apps/web/tests/market/market-api.contract.test.ts`
- [ ] T019 [P] [US1] Viết duplicate/stale/out-of-order/closed-Candle merge tests trong `apps/web/tests/market/candle-reducer.test.ts`
- [ ] T020 [P] [US1] Viết selection URL/default/back-forward và late-response tests trong `apps/web/tests/market/market-selection.test.tsx`
- [ ] T021 [P] [US1] Viết subscribe-confirm-buffer-snapshot/reconnect/gap tests trong `apps/web/tests/market/market-realtime.test.tsx`
- [ ] T022 [P] [US1] Viết chart geometry/bounded-window/OHLCV equivalent-summary tests trong `apps/web/tests/market/candle-chart.test.tsx`
- [ ] T023 [P] [US1] Viết loading/empty/error/rate-limit/connection-state component tests trong `apps/web/tests/market/market-dashboard.test.tsx`

### Implementation

- [ ] T024 [P] [US1] Implement Market selection/range/query view models trong `apps/web/src/features/market/model/market-selection.ts`
- [ ] T025 [P] [US1] Implement Candle exact-string view model và validation mapping trong `apps/web/src/features/market/model/candle.ts`
- [ ] T026 [US1] Implement historical Candle adapter qua F-011 client trong `apps/web/src/features/market/api/market-api.ts`
- [ ] T027 [US1] Implement deterministic bounded Candle reducer trong `apps/web/src/features/market/state/candle-reducer.ts`
- [ ] T028 [US1] Implement subscribe/confirmation/buffer/reconcile controller trong `apps/web/src/features/market/state/market-realtime-controller.ts`
- [ ] T029 [P] [US1] Implement pair/timeframe controls đồng bộ URL trong `apps/web/src/features/market/components/MarketControls.tsx`
- [ ] T030 [P] [US1] Implement accessible responsive SVG Candle chart và OHLCV summary trong `apps/web/src/features/market/components/CandleChart.tsx`
- [ ] T031 [P] [US1] Implement honest Market connection status trong `apps/web/src/features/market/components/MarketConnectionStatus.tsx`
- [ ] T032 [US1] Compose async/retry/realtime Market Dashboard trong `apps/web/src/features/market/components/MarketDashboard.tsx`
- [ ] T033 [US1] Thay F-011 Market placeholder bằng feature entry point trong `apps/web/app/(protected)/market/page.tsx`

**Checkpoint**: US1 chạy độc lập với Strategy/News và đáp ứng Market acceptance.

## Phase 4: User Story 2 — Khám phá và quản lý Strategy (P1)

**Goal**: Xem system/private Strategy và thực hiện create/version/publish/archive an toàn.

**Independent Test**: Load hai catalog, tạo SINGLE/COMPOSITE Strategy, tạo version, publish/archive;
validation đúng, version cũ bất biến và mutation luôn reconcile authoritative state.

### Tests

- [ ] T034 [P] [US2] Viết system/private Strategy DTO và inaccessible parity tests trong `apps/web/tests/strategy/strategy-api.contract.test.ts`
- [ ] T035 [P] [US2] Viết parameter kind/required/min/max/enum/exact-decimal tests trong `apps/web/tests/strategy/strategy-parameter-validator.test.ts`
- [ ] T036 [P] [US2] Viết lower/upper cross-rule và unsupported descriptor tests trong `apps/web/tests/strategy/strategy-cross-rule.test.ts`
- [ ] T037 [P] [US2] Viết SINGLE/COMPOSITE draft mapping tests trong `apps/web/tests/strategy/strategy-draft.test.ts`
- [ ] T038 [P] [US2] Viết create/version/publish/archive timeout/conflict/reconcile tests trong `apps/web/tests/strategy/strategy-mutations.test.tsx`
- [ ] T039 [P] [US2] Viết independent system/private loading/empty/error component tests trong `apps/web/tests/strategy/strategy-library.test.tsx`
- [ ] T040 [P] [US2] Viết immutable published-version và confirmation interaction tests trong `apps/web/tests/strategy/strategy-detail.test.tsx`

### Implementation

- [ ] T041 [P] [US2] Implement Strategy descriptor/private version typed models trong `apps/web/src/features/strategy/model/strategy.ts`
- [ ] T042 [P] [US2] Implement discriminated SINGLE/COMPOSITE draft models trong `apps/web/src/features/strategy/model/strategy-draft.ts`
- [ ] T043 [US2] Implement system/private list/detail/mutation adapter trong `apps/web/src/features/strategy/api/strategy-api.ts`
- [ ] T044 [US2] Implement schema-driven exact parameter validator trong `apps/web/src/features/strategy/state/strategy-parameter-validator.ts`
- [ ] T045 [US2] Implement draft/mutation/reconciliation controller trong `apps/web/src/features/strategy/state/strategy-controller.ts`
- [ ] T046 [P] [US2] Implement system/private catalog panels trong `apps/web/src/features/strategy/components/StrategyCatalog.tsx`
- [ ] T047 [P] [US2] Implement descriptor/version/provenance detail panel trong `apps/web/src/features/strategy/components/StrategyDetail.tsx`
- [ ] T048 [US2] Implement schema-driven SINGLE/COMPOSITE Strategy form trong `apps/web/src/features/strategy/components/StrategyForm.tsx`
- [ ] T049 [US2] Implement explicit publish/archive confirmation và mutation feedback trong `apps/web/src/features/strategy/components/StrategyActions.tsx`
- [ ] T050 [US2] Compose independent-source Strategy workspace trong `apps/web/src/features/strategy/components/StrategyWorkspace.tsx`
- [ ] T051 [US2] Thay F-011 Strategy placeholder bằng feature entry point trong `apps/web/app/(protected)/strategies/page.tsx`
- [ ] T052 [US2] Chạy owner A/B, immutable-version và retry integration scenarios theo `specs/012-market-strategy-news-ui/quickstart.md`

**Checkpoint**: US2 quản lý Strategy mà không phụ thuộc Market/News hoặc sửa F-009 contract.

## Phase 5: User Story 3 — Đọc News và Sentiment trung thực (P2)

**Goal**: News filter/pagination ổn định và sentiment degraded không chặn nội dung.

**Independent Test**: Filter/paginate News, mở item ANALYZED rồi inject mọi pending/failure state;
News vẫn đọc được và browser không gọi internal audit.

### Tests

- [ ] T053 [P] [US3] Viết News page/status/sentiment DTO validation tests trong `apps/web/tests/news/news-api.contract.test.ts`
- [ ] T054 [P] [US3] Viết cursor dedupe/stable-order/late-filter-response tests trong `apps/web/tests/news/news-reducer.test.ts`
- [ ] T055 [P] [US3] Viết ANALYZED/PENDING/ANALYZING/FAILED_RETRYABLE/FAILED mapping tests trong `apps/web/tests/news/sentiment-view.test.tsx`
- [ ] T056 [P] [US3] Viết safe external-link/internal-audit prohibition tests trong `apps/web/tests/news/news-security.test.tsx`
- [ ] T057 [P] [US3] Viết loading/empty/degraded/load-more component tests trong `apps/web/tests/news/news-feed.test.tsx`

### Implementation

- [ ] T058 [P] [US3] Implement News query/page/sentiment typed models trong `apps/web/src/features/news/model/news.ts`
- [ ] T059 [US3] Implement browser-safe public News adapter trong `apps/web/src/features/news/api/news-api.ts`
- [ ] T060 [US3] Implement query-generation/cursor/dedupe reducer trong `apps/web/src/features/news/state/news-reducer.ts`
- [ ] T061 [P] [US3] Implement URL-backed News filters trong `apps/web/src/features/news/components/NewsFilters.tsx`
- [ ] T062 [P] [US3] Implement safe Sentiment badge/degraded explanation/disclaimer trong `apps/web/src/features/news/components/SentimentStatus.tsx`
- [ ] T063 [US3] Implement News cards, safe links và bounded load-more trong `apps/web/src/features/news/components/NewsFeed.tsx`
- [ ] T064 [US3] Compose News workspace trong `apps/web/src/features/news/components/NewsWorkspace.tsx`
- [ ] T065 [US3] Thay F-011 News placeholder bằng feature entry point trong `apps/web/app/(protected)/news/page.tsx`

**Checkpoint**: US3 giữ News usable khi Sentiment unavailable và không ảnh hưởng route khác.

## Phase 6: User Story 4 — Trải nghiệm nhất quán và có thể phục hồi (P2)

**Goal**: Ba route responsive, accessible và dùng chung session/error/recovery semantics F-011.

**Independent Test**: Chạy loading/empty/4xx/5xx/session expiry/reconnect ở 360px và 1440px bằng
keyboard; focus/status/retry đúng và không lộ internal detail.

### Tests

- [ ] T066 [P] [US4] Viết cross-route loading/empty/error/retry consistency tests trong `apps/web/tests/ui/f012-shared-states.test.tsx`
- [ ] T067 [P] [US4] Viết session-expiry/logout cleanup/no-retry-loop tests trong `apps/web/tests/auth/f012-session-recovery.test.tsx`
- [ ] T068 [P] [US4] Viết keyboard/focus/live-region accessibility tests trong `apps/web/tests/accessibility/f012-accessibility.test.tsx`
- [ ] T069 [P] [US4] Viết 360px/1440px overflow/navigation component tests trong `apps/web/tests/accessibility/f012-responsive.test.tsx`
- [ ] T070 [P] [US4] Viết production mock/secret/error-redaction scope tests trong `apps/web/tests/security/f012-production-safety.test.ts`

### Implementation

- [ ] T071 [P] [US4] Chuẩn hóa feature loading/empty/error/degraded composition trên F-011 states trong `apps/web/src/features/shared/FeatureState.tsx`
- [ ] T072 [P] [US4] Thêm bounded accessible async status announcer trong `apps/web/src/features/shared/AsyncStatus.tsx`
- [ ] T073 [US4] Đồng bộ responsive layouts/focus styles cho F-012 trong `apps/web/app/globals.css`
- [ ] T074 [US4] Tích hợp shared recovery/session behavior vào ba feature entry points trong `apps/web/src/features/`
- [ ] T075 [US4] Chạy cross-route responsive/accessibility/session acceptance theo `specs/012-market-strategy-news-ui/quickstart.md`

**Checkpoint**: US4 chứng minh ba route nhất quán và không fork foundation F-011.

## Phase 7: Polish & Cross-Cutting Verification

- [ ] T076 [P] Thêm Market/Strategy/News Playwright journeys bằng controllable adapter trong `apps/web/tests/e2e/f012-market-strategy-news.spec.ts`
- [ ] T077 [P] Thêm SC-001 route readiness performance smoke trong `apps/web/tests/performance/f012-route-readiness.test.tsx`
- [ ] T078 [P] Thêm F-012 documentation/contract parity guard trong `apps/web/tests/contracts/f012-documentation-parity.test.ts`
- [ ] T079 Review không có F-013/Search/trading/wallet/provider/database scope creep trong `apps/web/tests/architecture/f012-scope-boundary.test.ts`
- [ ] T080 Chạy `npm run format:check`, `npm run lint`, `npm run typecheck`, `npm run test` và sửa toàn bộ failure/warning trong `apps/web/package.json`
- [ ] T081 Chạy production `npm run build` với mock mode off và ghi sanitized output trong `specs/012-market-strategy-news-ui/quickstart.md`
- [ ] T082 Chạy Playwright ở 360px/1440px và ghi commit/environment/config/result thật trong `specs/012-market-strategy-news-ui/quickstart.md`
- [ ] T083 Chạy non-production F-009 REST/WebSocket integration acceptance khi environment sẵn sàng và ghi evidence thật trong `specs/012-market-strategy-news-ui/quickstart.md`
- [ ] T084 Rà soát implementation-to-FR/SC traceability, git diff và chỉ đánh dấu Verified theo evidence thật trong `specs/012-market-strategy-news-ui/checklists/implementation-readiness.md`

## Dependencies & Execution Order

### Phase dependencies

- Phase 1 → Phase 2; shared contract/safety foundation block mọi story.
- US1 Market và US2 Strategy đều P1, độc lập sau Phase 2 và có thể làm song song.
- US3 News độc lập sau Phase 2; ưu tiên sau hai core P1 journeys.
- US4 cross-cutting phụ thuộc feature entry points của US1–US3.
- Polish phụ thuộc các story được chọn; production/evidence gate chạy cuối.

### Story graph

```text
Setup -> Foundation -> US1 Market ────┐
                   ├-> US2 Strategy ──┼-> US4 Consistency/Recovery -> Polish
                   └-> US3 News ──────┘
```

## Parallel Opportunities

- Setup: T002–T006 trên các directory/test riêng.
- Foundation: T009–T014 trước schema/adapter composition T015–T017.
- US1 tests T018–T023; components T029–T031 sau models/state contracts.
- US2 tests T034–T040; models T041/T042 và catalog/detail T046/T047.
- US3 tests T053–T057; model T058 và independent components T061/T062.
- US4 tests T066–T070; shared components T071/T072.
- US1, US2 và US3 có thể do ba người làm song song sau Phase 2, tránh sửa cùng route/style file.

## Implementation Strategy

### MVP first

1. Hoàn thành Setup + Foundation.
2. Deliver US1 Market Dashboard như slice đầu tiên có giá trị độc lập.
3. Deliver US2 Strategy để hoàn thành hai P1 journeys.

### Incremental delivery

1. Thêm US3 News/Sentiment degradation.
2. Hoàn tất US4 accessibility/resilience trên cả ba route.
3. Chạy Polish, integration và chỉ ghi evidence thật.

## Format Validation

- Mọi executable task dùng `- [ ] Tnnn` theo thứ tự T001–T084.
- Story tasks có `[USn]`; setup/foundation/polish không có story label.
- `[P]` chỉ dùng khi task không sửa cùng file hoặc không phụ thuộc output chưa hoàn thành.
- Mọi task nêu file hoặc directory đích cụ thể.

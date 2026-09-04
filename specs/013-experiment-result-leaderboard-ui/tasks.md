---

description: "Actionable implementation tasks for F-013 Experiment, Result and Leaderboard UI"
---

# Tasks: F-013 — Experiment, Result and Leaderboard UI

**Input**: Design documents from `specs/013-experiment-result-leaderboard-ui/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `quickstart.md`, `contracts/feature-adapter-contract.md`, and `contracts/realtime-reconciliation-contract.md`

**Tests**: Required. Within each slice, create failing contract/component/resilience evidence before implementation and run focused verification afterward.

**Organization**: Setup and foundation establish shared F-011/F-013 boundaries. The six canonical user stories then proceed mock/fixture first, visual and interaction verification second, released F-009 REST integration third, F-011 realtime integration fourth, and upstream-gated work last.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Safe to run in parallel because it targets isolated files and does not depend on an incomplete task.
- **[Story]**: Required only in User Story phases.
- Every task names its concrete repository path.

## Phase 1: Setup and Pre-flight

**Purpose**: Preserve the existing Next.js/F-011 application and establish only the directories and checks F-013 needs.

- [X] T001 Verify the active branch is `feature/013-experiment-result-leaderboard-ui`, F-013 artifacts resolve under `specs/013-experiment-result-leaderboard-ui/`, and inspect the ACTUAL F-010 release state; if no released F-010 public artifacts exist, preserve `BLOCKED_SEARCH_COORDINATOR` and record the pre-flight result in `specs/013-experiment-result-leaderboard-ui/tasks.md`
- [X] T002 Verify the protected placeholders and F-011 shell composition at `apps/web/app/(protected)/backtests/page.tsx`, `apps/web/app/(protected)/search/page.tsx`, and `apps/web/app/(protected)/layout.tsx`, then record reusable route constraints in `apps/web/tests/architecture/f013-route-foundation.test.ts`
- [X] T003 [P] Add F-013 test path and script-baseline assertions for the existing Vitest and Playwright setup in `apps/web/tests/architecture/f013-test-baseline.test.ts`
- [X] T004 [P] Establish feature barrel files and bounded folders without adding a second shell or client in `apps/web/src/features/backtests/index.ts`, `apps/web/src/features/experiments/index.ts`, and `apps/web/src/features/leaderboard/index.ts`
- [X] T005 [P] Document safe fixture-mode prerequisites, valid public API/WebSocket placeholders, real protected-route authentication, and the prohibition on committed credentials in `apps/web/README.md`
- [X] T006 Add a failing architecture baseline that rejects feature-owned raw `fetch`, raw `WebSocket`, prototype imports, Supabase business-table access, Redis/Worker/provider/internal-Java access, and service-role keys in `apps/web/tests/architecture/f013-boundaries.test.ts`

**Checkpoint**: Existing F-011 routes, scripts, environment rules, and target structure are confirmed without recreating the application.

---

## Phase 2: Shared Foundational Prerequisites

**Purpose**: Publish adapter-neutral types, deterministic injection, normalized errors, shared F-011 authentication recovery, and mock realtime behavior required by every story.

**⚠️ CRITICAL**: Complete this phase before user-story implementation. The real WebSocket resilience extension remains in US6 so fixture UI does not wait for a live transport.

- [X] T007 [P] Add shared sanitized async/error, exact-decimal, UTC instant, inaccessible, dependency-gate, and rate-limit view-model types in `apps/web/src/features/experiments/types/shared.ts`
- [X] T008 [P] Add failing `Retry-After` normalization tests covering valid delta-seconds, invalid/missing values, sanitized errors, and no raw `Response` exposure in `apps/web/tests/contracts/http-client-retry-after.contract.test.ts`
- [X] T009 Extend `PublicError` with optional safe `retryAfterSeconds` metadata and map response headers without changing existing sanitization in `apps/web/src/foundation/http/contracts.ts`, `apps/web/src/foundation/http/error-mapper.ts`, and `apps/web/src/foundation/http/api-client.ts`
- [X] T010 Run the focused F-011 HTTP contract suite and record the command, environment, and pass/fail result in `specs/013-experiment-result-leaderboard-ui/quickstart.md`; keep executable assertions only in `apps/web/tests/contracts/http-client-retry-after.contract.test.ts`
- [X] T011 [P] Add failing F-011 authentication-recovery contract tests proving Supabase `refreshSession()` success/failure, shared session-lifecycle recovery, HTTP `401 AUTHENTICATION_REQUIRED` invoking recovery exactly once without automatic request replay, and compatibility of existing typed `AuthClient` test doubles in `apps/web/tests/auth/auth-adapter.contract.test.ts`, `apps/web/tests/auth/session-lifecycle.test.tsx`, and `apps/web/tests/contracts/http-client.contract.test.ts`
- [X] T012 Extend the F-011 `AuthClient` with explicit `refreshSession()`, implement it in the Supabase adapter, add a shared `recoverAuthentication(client)` lifecycle that returns a refreshed session on success or performs existing private-state cleanup/sign-out/login handling on failure, inject that recovery callback into `ApiClient` from `ClientProvider`, and make HTTP 401 invoke recovery once while returning the original sanitized 401 without automatically replaying the request in `apps/web/src/foundation/auth/contracts.ts`, `apps/web/src/foundation/auth/supabase-auth-adapter.ts`, `apps/web/src/foundation/auth/session-lifecycle.ts`, `apps/web/src/foundation/http/api-client.ts`, and `apps/web/src/foundation/composition/client-provider.tsx`
- [X] T013 [P] Add failing public realtime contract tests for envelope listeners, status listeners, unsubscribe cleanup, confirmation/error delivery, and implementation substitutability in `apps/web/tests/contracts/realtime-listeners.contract.test.ts`
- [X] T014 Extend the published realtime interface with listener registration/removal and typed status metadata in `apps/web/src/foundation/realtime/contracts.ts`
- [X] T015 Extend the deterministic mock with `emit(...)`, status transitions, subscription confirmation/error helpers, and listener/subscription cleanup without opening a socket in `apps/web/src/foundation/testing/mock-realtime-client.ts`
- [X] T016 Run the focused public/mock realtime contract suite and record the command, environment, and pass/fail result in `specs/013-experiment-result-leaderboard-ui/quickstart.md`; keep executable assertions only in `apps/web/tests/contracts/realtime-listeners.contract.test.ts`
- [X] T017 [P] Add failing composition/mock-client tests proving explicit test/dev adapter selection, deterministic success/error `ApiResult` responders with request-method/header capture, production fixture rejection, and absence of static testing imports from the production client composition root in `apps/web/tests/architecture/f013-fixture-composition.test.ts`
- [X] T018 Refactor fixture construction behind a development-only composition boundary and extend the F-011 `MockApiClient` with finite deterministic method/path responders that can return success or sanitized error `ApiResult` values and capture request metadata needed for idempotency assertions, while keeping production `ClientProvider` dependent only on F-011 public interfaces in `apps/web/src/foundation/composition/client-provider.tsx`, `apps/web/src/foundation/composition/fixture-clients.ts`, and `apps/web/src/foundation/testing/mock-api-client.ts`
- [X] T019 Add deterministic scenario identifiers and a finite fixture registry shared by feature mock adapters in `apps/web/src/features/experiments/fixtures/scenarios.ts`
- [X] T020 Add architecture assertions that production routes/bootstrap cannot select fixture scenarios and production fixture-enabled composition fails in `apps/web/tests/architecture/f013-fixture-composition.test.ts`
- [X] T021 Run focused foundation HTTP, realtime, fixture-safety, and architecture tests using the existing `npm test` script and record the verification command in `specs/013-experiment-result-leaderboard-ui/quickstart.md`

**Checkpoint**: Components can consume real or explicit test/dev adapters without knowing which implementation is active; HTTP 401 delegates through one F-011 recovery callback without auto-replaying requests; mock realtime is finite and requires no live WebSocket.

---

## Phase 3: User Story 1 — Inspect Immutable Backtest Results and Trade History (Priority: P1)

**Goal**: Demonstrate an immutable result by either identity with exactly four released metrics, capital, trades, provenance, assumptions, safe failure states, and no browser calculations.

**Independent Test**: Render `/backtests` with no identity, fixture `backtestId`, fixture `resultId`, zero/many trades, extreme decimals, inaccessible, parity-blocked, retryable, and terminal scenarios; verify exactly four metrics, every released trade/provenance/assumption field, accessible full decimals, local table scrolling, and no ID conversion.

### Tests for User Story 1

- [X] T022 [P] [US1] Add failing lookup parser tests for none, distinct `resultId`, distinct `backtestId`, malformed, and conflicting query inputs in `apps/web/tests/features/backtests/backtest-lookup.test.ts`
- [X] T023 [P] [US1] Add failing mapper contract tests for DTO validation, exactly four metrics, exact decimal strings, UTC values, capital, trades, provenance, and assumptions in `apps/web/tests/features/backtests/backtest-result-mapper.test.ts`
- [X] T024 [P] [US1] Add failing deterministic fixture catalog tests for normal, zero trades, many trades, extreme decimals, inaccessible, result-ID parity blocked, retryable failure, terminal failure, 401, and 429 in `apps/web/tests/features/backtests/backtest-fixtures.test.ts`
- [X] T025 [P] [US1] Add failing component tests for result header, four-card metric hierarchy, capital, provenance, and assumptions in `apps/web/tests/features/backtests/result-summary.test.tsx`
- [X] T026 [P] [US1] Add failing trade-history tests for all released columns, zero/many rows, authoritative order, readable numerics, accessible full values, and table-local overflow in `apps/web/tests/features/backtests/trade-history.test.tsx`
- [X] T027 [US1] Add failing route/query tests for loading, refreshing, empty guidance, inaccessible, parity-blocked, retryable, rate-limited, auth failure, and terminal states in `apps/web/tests/features/backtests/backtests-page.test.tsx`

### Mock-first implementation for User Story 1

- [X] T028 [P] [US1] Implement immutable lookup, result, metric, capital, trade, provenance, assumption, and query-state types in `apps/web/src/features/backtests/types/backtest-result.ts`
- [X] T029 [US1] Implement strict F-009 result DTO validation and lossless view-model mapping with no metric or identity calculation in `apps/web/src/features/backtests/mappers/backtest-result-mapper.ts`
- [X] T030 [US1] Implement the finite Backtest fixture catalog and adapter responses in `apps/web/src/features/backtests/fixtures/backtest-result-fixtures.ts`
- [X] T031 [P] [US1] Implement the dark-terminal result header, exactly four metric cards, and capital summary in `apps/web/src/features/backtests/components/ResultSummary.tsx`
- [X] T032 [P] [US1] Implement provenance and execution-assumption panels with semantic labels and tabular values in `apps/web/src/features/backtests/components/ResultEvidence.tsx`
- [X] T033 [P] [US1] Implement the released trade-history columns, empty state, local horizontal scroll, and accessible full-value disclosure in `apps/web/src/features/backtests/components/TradeHistory.tsx`
- [X] T034 [US1] Implement adapter-neutral distinct read-by-result-ID/read-by-backtest-ID service methods and ownership-safe error mapping in `apps/web/src/features/backtests/service/backtest-result-service.ts`
- [X] T035 [US1] Implement lookup parsing, query lifecycle, consumption of the F-011 ApiClient's normalized 401/recovery outcome without feature-owned token handling or automatic replay, and Retry-After-gated retry while preserving safe snapshots in `apps/web/src/features/backtests/hooks/useBacktestResult.ts`
- [X] T036 [US1] Replace the protected placeholder with the composed fixture-capable Backtest Results view in `apps/web/app/(protected)/backtests/page.tsx`
- [X] T037 [US1] Match the approved Backtest Results hierarchy and responsive breakpoints without charts or prototype imports in `apps/web/app/globals.css`

### Released and gated integration for User Story 1

- [X] T038 [US1] Add failing F-009 adapter contract tests for `GET /api/v1/backtests/{backtestId}/result`, uniform ownership-safe `404 RESOURCE_NOT_FOUND`, sanitized 401/429 handling, and exact DTO mapping in `apps/web/tests/contracts/f013-backtest-api.contract.test.ts`
- [X] T039 [US1] Wire the released standalone backtest-result read through F-011 `ApiClient` in `apps/web/src/features/backtests/service/backtest-result-service.ts`
- [X] T040 [US1] Integrate canonical `GET /api/v1/backtest-results/{resultId}` only after F-009 implements result-ID lookup with Experiment-derived ownership and controller/OpenAPI parity tests in `apps/web/src/features/backtests/service/backtest-result-service.ts`
- [X] T041 [US1] Enable the candidate-result navigation E2E only after T040 and upstream ownership/parity evidence pass in `apps/web/tests/e2e/backtest-result-by-result-id.spec.ts`

**Checkpoint**: US1 is independently demonstrable in fixtures and production-integrated for the released standalone `backtestId` path; result-ID production coverage stays explicitly blocked.

---

## Phase 4: User Story 2 — Monitor Search Progress and Authoritative Job Lifecycle (Priority: P1)

**Goal**: Show authoritative Experiment and Job snapshots, public lifecycle states, safe progress, failures/retries, and fixture-driven freshness updates without browser orchestration.

**Independent Test**: Render `/search?id={experimentId}` from each Experiment and Job fixture, verify REST-shaped snapshots and safe states, then emit predefined progress/completion notifications and confirm they request authoritative refresh rather than directly mutating durable fields.

### Tests for User Story 2

- [X] T042 [P] [US2] Add failing Experiment, Job, and Candidate mapper tests for every released lifecycle/status, exact best-score strings, counts, UTC values, Candidate generation index/fingerprint/definition metadata, cursor fields, retry time, and safe failures in `apps/web/tests/features/experiments/experiment-job-mappers.test.ts`
- [X] T043 [P] [US2] Add failing finite fixture tests for CREATED, QUEUED, RUNNING, STOP_REQUESTED, STOPPED, COMPLETED, FAILED, F-010 unavailable, all relevant Job states, Candidate pages/discoveries, failure, retry schedule, 401, 404, 429, and 503 in `apps/web/tests/features/experiments/experiment-job-fixtures.test.ts`
- [X] T044 [P] [US2] Add failing component tests for lifecycle status, raw work counts, derived visual ratio, best score, retry schedule, Candidate discovery timeline, safe terminal error, and non-color-only semantics in `apps/web/tests/features/experiments/search-progress.test.tsx`
- [X] T045 [US2] Add failing query tests proving Experiment and Job states are separate, safe snapshots survive refresh/rate-limit errors, auth delegates to F-011, and progress event payloads never overwrite snapshots in `apps/web/tests/features/experiments/use-experiment-monitor.test.tsx`

### Mock-first implementation for User Story 2

- [X] T046 [P] [US2] Implement immutable Experiment, Job, Candidate summary/page, lifecycle, progress, discovery, and query-state types in `apps/web/src/features/experiments/types/experiment.ts`
- [X] T047 [US2] Implement strict Experiment, Job, and Candidate DTO/page validation and lossless presentation mapping without deriving candidate business outcomes in `apps/web/src/features/experiments/mappers/experiment-job-mappers.ts`
- [X] T048 [US2] Implement finite Experiment, Job, Candidate-page, and completion-notification snapshots without timers, random candidate generation, workers, or business simulation in `apps/web/src/features/experiments/fixtures/experiment-job-fixtures.ts`
- [X] T049 [P] [US2] Implement lifecycle/status header and accessible degraded-state presentation in `apps/web/src/features/experiments/components/ExperimentStatus.tsx`
- [X] T050 [P] [US2] Implement Job progress list with public job identities/states, raw accessible counts, retry schedule, and no worker identities in `apps/web/src/features/experiments/components/JobProgressList.tsx`
- [X] T051 [US2] Implement `CandidateDiscoveryTimeline` from authoritative Candidate-page data plus current-session `BACKTEST_COMPLETED` freshness hints, clearly separating durable candidate metadata from ephemeral completion notifications and performing no candidate generation or result synthesis in `apps/web/src/features/experiments/components/CandidateDiscoveryTimeline.tsx`
- [X] T052 [US2] Implement adapter-neutral Experiment, Job, Candidate-page, and Candidate-detail reads with uniform inaccessible handling, 401 delegation, Retry-After eligibility, opaque Candidate cursor preservation, and snapshot-preserving errors in `apps/web/src/features/experiments/service/experiment-service.ts`
- [X] T053 [US2] Implement separate Experiment/Job/Candidate query ownership and freshness-only refresh callbacks so `EXPERIMENT_PROGRESS_UPDATED` refreshes authoritative Experiment/Job state and `BACKTEST_COMPLETED` refreshes Candidate discovery data without directly overwriting durable snapshots in `apps/web/src/features/experiments/hooks/useExperimentMonitor.ts`
- [X] T054 [US2] Compose identifier parsing, fixture status/progress regions, and shared async states into the protected Search page in `apps/web/app/(protected)/search/page.tsx`
- [X] T055 [US2] Match the approved dense Search status hierarchy at 360/768/1024/1440+ without stage or worker simulation in `apps/web/app/globals.css`

### Released integration for User Story 2

- [X] T056 [US2] Add failing F-009 contracts for `GET /api/v1/experiments/{id}`, `GET /api/v1/jobs/{id}`, `GET /api/v1/experiments/{id}/candidates`, and `GET /api/v1/experiments/{id}/candidates/{candidateId}` including ownership-safe 404, cursor semantics, 401, 429, retry-scheduled, and terminal-failure results in `apps/web/tests/contracts/f013-experiment-job-api.contract.test.ts`
- [X] T057 [US2] Wire released Experiment, Job, Candidate-page, and Candidate-detail reads through F-011 `ApiClient` without component redesign in `apps/web/src/features/experiments/service/experiment-service.ts`

**Checkpoint**: US2 can be demonstrated from finite fixtures and released F-009 reads; notifications remain refresh hints, never a frontend Search pipeline.

---

## Phase 5: User Story 3 — Discover Top-K Strategies on the Leaderboard (Priority: P1)

**Goal**: Present authoritative ordered Top-K results, revision metadata, capped paging, and canonical result navigation without ranking or synthetic analytics.

**Independent Test**: Render empty, normal, cursor-paged, configured-Top-K, stale-revision, and newer-revision fixtures; verify only six released columns, preserved order/rank, 10/25/50/custom limits capped by configured Top-K, stale events ignored, newer events triggering REST refresh, and canonical result-ID navigation.

### Tests for User Story 3

- [X] T058 [P] [US3] Add failing Leaderboard mapper tests for the six released fields, exact score/drawdown strings, server order/rank, metadata, opaque cursor, and Top-K cap in `apps/web/tests/features/leaderboard/leaderboard-mapper.test.ts`
- [X] T059 [P] [US3] Add failing finite fixtures for empty, entries, cursor pages, configured Top-K, stale revision, newer revision, inaccessible, 401, and 429 in `apps/web/tests/features/leaderboard/leaderboard-fixtures.test.ts`
- [X] T060 [P] [US3] Add failing table tests for exactly Rank, Evaluation Result ID, Backtest Result ID, Score, Maximum Drawdown, Evaluation Fingerprint, empty state, and result navigation in `apps/web/tests/features/leaderboard/leaderboard-table.test.tsx`
- [X] T061 [P] [US3] Add failing limit/paging tests for initial 10, presets 10/25/50, custom 1–100, configured-Top-K cap, opaque cursors, and local table overflow in `apps/web/tests/features/leaderboard/leaderboard-controls.test.tsx`
- [X] T062 [US3] Add failing reconciliation tests proving revision less than or equal to rendered is ignored and a newer revision causes an authoritative read without local ranking in `apps/web/tests/features/leaderboard/leaderboard-reconciliation.test.tsx`

### Mock-first implementation for User Story 3

- [X] T063 [P] [US3] Implement immutable Leaderboard snapshot, entry, paging, revision, and query-state types in `apps/web/src/features/leaderboard/types/leaderboard.ts`
- [X] T064 [US3] Implement strict Leaderboard DTO validation preserving exact strings and server ordering in `apps/web/src/features/leaderboard/mappers/leaderboard-mapper.ts`
- [X] T065 [US3] Implement finite empty/page/Top-K/revision fixture responses without calculated scores or ranks in `apps/web/src/features/leaderboard/fixtures/leaderboard-fixtures.ts`
- [X] T066 [P] [US3] Implement revision metadata and accessible 10/25/50/custom capped limit and cursor controls in `apps/web/src/features/leaderboard/components/LeaderboardControls.tsx`
- [X] T067 [P] [US3] Implement the six-column table, empty state, tabular numerics, local scrolling, and `/backtests?resultId=` navigation in `apps/web/src/features/leaderboard/components/LeaderboardTable.tsx`
- [X] T068 [US3] Implement adapter-neutral capped reads with opaque cursors, safe snapshot retention, 401 delegation, and Retry-After-gated retries in `apps/web/src/features/leaderboard/service/leaderboard-service.ts`
- [X] T069 [US3] Implement paging and revision reconciliation that refreshes only for a newer revision in `apps/web/src/features/leaderboard/hooks/useLeaderboard.ts`
- [X] T070 [US3] Compose the fixture-first Leaderboard region into the protected Search page in `apps/web/app/(protected)/search/page.tsx`
- [X] T071 [US3] Match the approved Leaderboard hierarchy and responsive table-local scroll without prototype-only columns in `apps/web/app/globals.css`

### Released and gated integration for User Story 3

- [X] T072 [US3] Add failing F-009 contracts for `GET /api/v1/experiments/{id}/leaderboard`, validated limit/cursor, ordering, inaccessible, 401, and 429 behavior in `apps/web/tests/contracts/f013-leaderboard-api.contract.test.ts`
- [X] T073 [US3] Wire the released Leaderboard REST endpoint through F-011 `ApiClient` in `apps/web/src/features/leaderboard/service/leaderboard-service.ts`
- [X] T074 [US3] Verify Leaderboard-to-result production navigation only after T040 and upstream result-ID ownership/parity evidence pass; T041 candidate-result E2E may proceed independently after the same upstream parity gate in `apps/web/tests/e2e/leaderboard-result-navigation.spec.ts`

**Checkpoint**: US3 mock and REST Leaderboard paths are usable without F-009 result-ID parity; only the final destination E2E stays blocked.

---

## Phase 6: User Story 4 — Control Experiment Execution and Request Stop (Priority: P2)

**Goal**: Safely request Stop using one logical idempotency key, distinguish accepted request from durable completion, and reconcile conflicts or uncertain outcomes.

**Independent Test**: From QUEUED/RUNNING fixtures, confirm Stop, suppress rapid clicks, receive 202/STOP_REQUESTED without fabricating STOPPED, retry an uncertain outcome with the same key, create a new key only for a deliberate command, and refresh authoritative state after 409.

### Tests for User Story 4

- [X] T075 [P] [US4] Add failing Stop command-state tests for accepted, conflict, timeout/uncertain, inaccessible, 401, 429, and terminal failure fixtures in `apps/web/tests/features/experiments/stop-command.test.ts`
- [X] T076 [P] [US4] Add failing interaction tests for confirmation, focus restoration, submitting lock, rapid-click suppression, same-key retry, new deliberate key, and STOP_REQUESTED-versus-STOPPED presentation in `apps/web/tests/features/experiments/experiment-actions.test.tsx`
- [X] T077 [US4] Add failing F-009 Stop contract tests for POST path, one `Idempotency-Key`, 202 handling, 409 authoritative refresh, uncertain transport evidence, 401 no-auto-replay, and normalized 429 delay in `apps/web/tests/contracts/f013-stop-api.contract.test.ts`

### Implementation for User Story 4

- [X] T078 [P] [US4] Implement reusable logical command/key lineage and immutable command-state transitions in `apps/web/src/features/experiments/types/command-state.ts`
- [X] T079 [US4] Implement the Stop controller with confirmation lock, same-key uncertain retry, new-key deliberate command, 202 acceptance, and 409 refresh callback in `apps/web/src/features/experiments/hooks/useStopExperiment.ts`
- [X] T080 [US4] Implement accessible Stop confirmation, pending/conflict/rate-limit/inaccessible/error states, and no optimistic STOPPED transition in `apps/web/src/features/experiments/components/ExperimentActions.tsx`
- [X] T081 [US4] Wire released `POST /api/v1/experiments/{id}/stop` through F-011 `ApiClient`, preserving key/payload evidence across uncertain outcomes; rely on F-011's injected 401 recovery boundary and never auto-replay the mutation after auth recovery in `apps/web/src/features/experiments/service/experiment-command-service.ts`
- [X] T082 [US4] Integrate Stop controls with the authoritative Experiment snapshot on the protected Search page in `apps/web/app/(protected)/search/page.tsx`

**Checkpoint**: Stop is independently testable and production-integrated without ever fabricating terminal state or replaying an uncertain mutation automatically.

---

## Phase 7: User Story 5 — Configure and Submit Search and Reproduction with Dependency Gate Awareness (Priority: P2)

**Goal**: Provide a deterministic fixture-first configuration and reproduction UX while truthfully preserving the F-010 production gate.

**Independent Test**: Complete and validate every form field, submit deterministic fixture acceptance, then exercise production-shaped 503 responses for Start and Reproduce; verify drafts/evidence and logical keys remain available, dependency notices are accessible, and no Search orchestration runs in the browser.

### Tests for User Story 5

- [X] T083 [P] [US5] Add failing draft/validation tests for name, known/fixture dataset ID, fixture generator ID/version/seed, Strategy/plugin/version, descriptor parameters, positive finite stop bounds, and Top-K public range 1–100 / initial UI selection 10 / convenience presets 10/25/50 in `apps/web/tests/features/experiments/experiment-form-validation.test.ts`
- [X] T084 [P] [US5] Add failing Strategy descriptor mapper tests for released strategy/version/schema fields, ranges/options, and no production generator/dataset enum invention in `apps/web/tests/features/experiments/strategy-descriptor-mapper.test.ts`
- [X] T085 [P] [US5] Add failing form component tests for semantic labels, accessible validation, keyboard order, preserved draft after failure, and visible fixture-only source indications in `apps/web/tests/features/experiments/experiment-configuration-form.test.tsx`
- [X] T086 [US5] Add failing Start/Reproduce controller tests for independent state, one key per logical command, rapid-repeat lock, same-key uncertain retry, 401 no-auto-replay, 429 eligibility, 503 preservation, and fixture-only acceptance in `apps/web/tests/features/experiments/start-reproduce-commands.test.ts`
- [X] T087 [US5] Add failing F-009 contracts for Strategy discovery plus current Start/Reproduce 503 readiness markers and sanitized errors in `apps/web/tests/contracts/f013-create-reproduce-api.contract.test.ts`

### Mock-first implementation for User Story 5

- [X] T088 [P] [US5] Implement form draft, Strategy descriptor, parameter rule, stop-condition, Top-K, and validation types in `apps/web/src/features/experiments/types/experiment-configuration.ts`
- [X] T089 [US5] Implement strict released Strategy descriptor mapping and parameter validation without deriving production generator/dataset catalogs in `apps/web/src/features/experiments/mappers/strategy-descriptor-mapper.ts`
- [X] T090 [US5] Implement deterministic known dataset, fixture generator, Strategy descriptor, accepted Start/Reproduce, 401, 429, 503, and uncertain fixtures in `apps/web/src/features/experiments/fixtures/experiment-configuration-fixtures.ts`
- [X] T091 [US5] Implement form draft reducer and validation for descriptors, ranges/options, positive finite stop bounds, and capped Top-K in `apps/web/src/features/experiments/hooks/useExperimentConfiguration.ts`
- [X] T092 [US5] Implement the accessible dark-terminal configuration form with all required fields, initial UI selection 10, convenience presets 10/25/50, inline validation, and persistent evidence in `apps/web/src/features/experiments/components/ExperimentConfigurationForm.tsx`
- [X] T093 [US5] Implement independent Start and Reproduce controllers with key lineage, submitting locks, uncertain retry, F-011-owned normalized 401 recovery with no automatic mutation replay, normalized 429 handling, and draft/evidence preservation in `apps/web/src/features/experiments/hooks/useExperimentCommands.ts`
- [X] T094 [US5] Implement accessible dependency-unavailable and fixture-only accepted presentations without implying production success in `apps/web/src/features/experiments/components/DependencyGateNotice.tsx`
- [X] T095 [US5] Wire released `GET /api/v1/strategies` and production-shaped Start/Reproduce requests through F-011 `ApiClient` in `apps/web/src/features/experiments/service/experiment-command-service.ts`
- [X] T096 [US5] Compose configuration and Reproduce controls into the protected Search page without Pause/Resume or browser orchestration in `apps/web/app/(protected)/search/page.tsx`

### Upstream-gated integration for User Story 5

- [X] T097 [US5] Enable production Start Experiment success only after F-010 public artifacts are released and F-009 no longer returns `BLOCKED_SEARCH_COORDINATOR`, integrating solely through F-009/F-011 in `apps/web/src/features/experiments/service/experiment-command-service.ts`
- [X] T098 [US5] Enable production Reproduce Experiment success only after F-010 public artifacts are released and F-009 no longer returns `BLOCKED_SEARCH_COORDINATOR`, integrating solely through F-009/F-011 in `apps/web/src/features/experiments/service/experiment-command-service.ts`
- [X] T099 [US5] Add full production Search-success and Reproduce-success E2E only after T097/T098 and released F-010 integration evidence exist in `apps/web/tests/e2e/search-success.spec.ts`

**Checkpoint**: The full form and dependency-aware failure path are demonstrable now; production Start/Reproduce success remains unchecked and gated.

---

## Phase 8: User Story 6 — Realtime Connection Resilience, Disconnect Handling, and Snapshot Recovery (Priority: P2)

**Goal**: Demonstrate F-013 realtime reconciliation and disconnected/recovery UI first against the Phase 2 deterministic F-011 mock surface, then extend the one real F-011 transport and verify production-client integration.

**Independent Test**: Without a live WebSocket, use the Phase 2 mock client to prove confirmation/error handling, progress/completion freshness hints, duplicate/target/revision rules, stale/recovering/disconnected presentation, retry-exhaustion/manual-retry presentation, authoritative recovery, final terminal reads, and subscription cleanup. Separately, with fake sockets/timers, prove one real connection, a fresh ticket per attempt, message/status dispatch, 4001 auth recovery, finite jittered retries, exhaustion/manual reconnect, resubscription, and cleanup.

### Mock-first F-013 consumer tests and implementation

- [X] T100 [P] [US6] Add failing mock-client reconciliation tests proving each matching `SUBSCRIPTION_CONFIRMED` schedules the appropriate authoritative REST refresh and reconciles any buffered events before normal rendering, plus subscription error, progress, completion, Leaderboard update, duplicate IDs, wrong targets, late progress, stale/new revision, disconnect/reconnect, exhaustion/manual retry, and terminal final read in `apps/web/tests/realtime/f013-reconciliation.test.tsx`
- [X] T101 [P] [US6] Add failing mock-client lifecycle tests for target change, unmount, terminal release, isolated subscription errors, bounded dedup window, and private-state cleanup in `apps/web/tests/realtime/f013-subscription-lifecycle.test.tsx`
- [X] T102 [US6] Implement bounded event-ID deduplication, target matching, recovery buffering, and freshness/revision decisions without claiming exactly-once delivery in `apps/web/src/features/experiments/hooks/realtime-reconciler.ts`
- [X] T103 [P] [US6] Implement Experiment subscription ownership so a matching `SUBSCRIPTION_CONFIRMED` schedules an authoritative Experiment/Job REST refresh and then reconciles buffered Experiment events; also handle subscription errors, progress/completion refresh hints, stale/recovering status, terminal final REST read, and cleanup against the published F-011 realtime interface in `apps/web/src/features/experiments/hooks/useExperimentRealtime.ts`
- [X] T104 [P] [US6] Implement Leaderboard subscription ownership so a matching `SUBSCRIPTION_CONFIRMED` schedules an authoritative Leaderboard REST refresh and then reconciles buffered Leaderboard events; preserve revision-gated authoritative reads, stale/recovering status, and cleanup against the published F-011 realtime interface in `apps/web/src/features/leaderboard/hooks/useLeaderboardRealtime.ts`
- [X] T105 [US6] Implement connection status, stale snapshot, subscription error, retry-exhaustion, and manual-reconnect presentation driven by interface/mock status transitions with accessible live announcements in `apps/web/src/features/experiments/components/RealtimeStatus.tsx`
- [X] T106 [US6] Compose the mock-demonstrable F-011 realtime consumers into the protected Search page without raw sockets, tickets, or feature reconnect loops in `apps/web/app/(protected)/search/page.tsx`

**Mock-first checkpoint**: US6 reconciliation/status UI is independently demonstrable through `MockRealtimeClient`; no real WebSocket transport is required for T100–T106.

### F-011 real transport tests and implementation

- [X] T107 [US6] Add failing fake-socket transport tests for exactly one connection, incoming envelope dispatch, status observers, close metadata, fresh ticket per connect/reconnect, and automatic logical resubscription in `apps/web/tests/realtime/realtime-transport.test.ts`
- [X] T108 [US6] Add failing fake-timer resilience tests for finite exponential backoff with injected jitter, retry cap, exhaustion to disconnected, manual reconnect, timer cleanup, and no duplicate loops in `apps/web/tests/realtime/realtime-reconnect.test.ts`
- [X] T109 [US6] Add failing 4001/auth/logout tests proving the real realtime client receives the shared F-011 recovery dependency from composition, successful recovery obtains a fresh one-time ticket and reconnects without feature token access, failed recovery performs private-state/listener/subscription cleanup and safe login navigation, and no state-changing HTTP command is replayed in `apps/web/tests/realtime/realtime-auth-cleanup.test.ts` and `apps/web/tests/contracts/realtime-client.contract.test.ts`
- [X] T110 [US6] Extend the F-011 reconnect policy with configurable finite attempts and deterministic jitter seams in `apps/web/src/foundation/realtime/reconnect-policy.ts`
- [X] T111 [US6] Extend the single F-011 realtime client to accept the shared `recoverAuthentication` callback, dispatch incoming messages/status/close metadata, obtain a fresh ticket per attempt, handle close 4001 by calling recovery and reconnecting only on success, stop retrying when recovery fails, apply bounded reconnect/manual reconnect/automatic resubscription, and update `ClientProvider` to inject the same F-011 recovery dependency used by `ApiClient` in `apps/web/src/foundation/realtime/realtime-client.ts` and `apps/web/src/foundation/composition/client-provider.tsx`
- [X] T112 [US6] Ensure disconnect/logout clears sockets, timers, listeners, logical subscriptions, and private ephemeral state in `apps/web/src/foundation/realtime/realtime-client.ts` and `apps/web/src/foundation/auth/logout.ts`
- [X] T113 [US6] Run the focused realtime contract, transport, reconnect, and auth-cleanup suites in `apps/web/tests/contracts/realtime-client.contract.test.ts`, `apps/web/tests/realtime/realtime-transport.test.ts`, `apps/web/tests/realtime/realtime-reconnect.test.ts`, and `apps/web/tests/realtime/realtime-auth-cleanup.test.ts`; record only the command, environment, and pass/fail evidence in `specs/013-experiment-result-leaderboard-ui/quickstart.md`

### Released realtime contract and production-client integration

- [X] T114 [US6] Add released F-009 envelope shape and subscription command contract coverage for all consumed server events in `apps/web/tests/contracts/f013-realtime-events.contract.test.ts`
- [X] T115 [US6] Verify F-011/F-009 production-client integration proves matching `SUBSCRIPTION_CONFIRMED` triggers the appropriate authoritative Experiment/Job or Leaderboard REST recovery, buffered events reconcile only after that snapshot, and normal progress/revision recovery remains REST-authoritative; document that production Redis/runtime smoke is not yet verified in `apps/web/tests/realtime/f013-realtime-integration.test.ts` and `specs/013-experiment-result-leaderboard-ui/quickstart.md`

**Checkpoint**: F-013 owns reconciliation and presentation only; its mock consumer UI is independently demonstrable before the real transport slice. F-011 owns the sole real socket, ticketing, auth recovery, retry policy, resubscription, and cleanup.

---

## Phase 9: Polish and Cross-Cutting Verification

**Purpose**: Verify presentation, accessibility, architecture, mock safety, non-gated journeys, and repository-standard quality commands without converting blocked checks into passes.

- [X] T116 [P] Add accessibility tests for semantic form labels, accessible validation, live async/status announcements, non-color-only state, visible focus, dialog focus restoration, reduced motion, and full-value decimal disclosure in `apps/web/tests/accessibility/f013-accessibility.test.tsx`
- [X] T117 [P] Add responsive component tests for 360px, 768px, 1024px, and 1440px+ at 100% zoom, shell overflow containment, visible primary actions, and table-local scrolling in `apps/web/tests/features/f013-responsive.test.tsx`
- [X] T118 Implement any accessibility findings from T116 in `apps/web/src/features/backtests/components/TradeHistory.tsx`, `apps/web/src/features/experiments/components/ExperimentConfigurationForm.tsx`, `apps/web/src/features/experiments/components/ExperimentActions.tsx`, and `apps/web/src/features/experiments/components/RealtimeStatus.tsx`
- [X] T119 Implement any responsive findings from T117 using F-011 design tokens and approved dark-terminal hierarchy in `apps/web/app/globals.css`
- [X] T120 Strengthen architecture scanning for production fixture imports, fixture selectors, prototype imports, raw feature fetch/WebSocket, Supabase business tables, secrets, Redis, Worker, provider, and internal Java capability access in `apps/web/tests/architecture/f013-boundaries.test.ts`
- [X] T121 Add a production composition/build test proving fixture-enabled production is rejected and normal production routes have no feature fixture dependency in `apps/web/tests/architecture/f013-production-build.test.ts`
- [X] T122 [P] Add non-gated protected fixture journeys for Backtest standalone result, Experiment/Job monitoring, Leaderboard paging, Stop safety, Start/Reproduce dependency gates, 401 cleanup/login, and 429 retry eligibility in `apps/web/tests/e2e/f013-fixture-journeys.spec.ts`
- [X] T123 [P] Add responsive/keyboard Playwright coverage at 360, 768, 1024, and 1440 pixels with table-local overflow and primary-action assertions in `apps/web/tests/e2e/f013-responsive-accessibility.spec.ts`
- [X] T124 Run `npm run format:check` and apply only required formatting fixes under `apps/web/`
- [X] T125 Run `npm run lint` and resolve F-013 lint findings under `apps/web/app/(protected)/`, `apps/web/src/features/`, and `apps/web/tests/`
- [X] T126 Run `npm run typecheck` and resolve F-013 type failures under `apps/web/app/(protected)/`, `apps/web/src/features/`, and `apps/web/src/foundation/`
- [X] T127 Run `npm test` and resolve unit, component, contract, realtime, accessibility, and architecture failures under `apps/web/tests/`
- [X] T128 Run `npm run test:e2e` for non-gated Playwright paths and record blocked F-009/F-010 suites as unverified, never passed, in `specs/013-experiment-result-leaderboard-ui/quickstart.md`
- [X] T129 Run `npm run build` with production fixtures disabled and verify the fixture-enabled rejection case, recording reproducible evidence in `specs/013-experiment-result-leaderboard-ui/quickstart.md`
- [X] T130 Audit FR-001–FR-042, SC-001–SC-008, both contracts, and every non-gated Quickstart scenario against actual test evidence, leaving gated evidence Planned in `specs/013-experiment-result-leaderboard-ui/checklists/requirements.md`

---

## Dependencies and Execution Order

### Phase graph

```text
Phase 1 Setup
  ↓
Phase 2 Shared Foundation
  ├──→ US1 Backtest Result ───────────────┐
  ├──→ US2 Experiment/Job ──┐             │
  ├──→ US3 Leaderboard ─────┼─────────────┤
  │                         ├──→ US4 Stop │
  │                         └──→ US5 Form/Gated Start-Reproduce
  └──→ US6 Mock Realtime Consumers
              ↓
      F-011 Real Transport
              ↓
      Released Realtime Integration
              ↓
  Phase 9 Cross-cutting verification
```

### User Story dependencies

- **US1 (P1)** starts after Phase 2 and does not depend on F-010. Fixture and standalone `backtestId` work do not depend on the F-009 result-ID correction. The result-ID service integration is the shared gate; the candidate-result and Leaderboard-result E2E checks are independent sibling verifications after that integration and upstream parity evidence.
- **US2 (P1)** starts after Phase 2 and is independently demonstrable from REST-shaped fixtures and released reads. Its mock freshness callbacks use the Phase 2 mock listener surface, not the real transport.
- **US3 (P1)** starts after Phase 2 and is independently demonstrable. Leaderboard REST does not wait for result-ID parity; only destination E2E does.
- **US4 (P2)** depends on the US2 Experiment snapshot/service contract but is otherwise independent of US3, US5, and live realtime.
- **US5 (P2)** depends on shared command-state conventions; T097–T099 are released and completed after the F-010 gate was satisfied.
- **US6 (P2)** mock consumer/reconciliation work T100–T106 depends only on the Phase 2 published/mock realtime surface plus US2/US3 authoritative read callbacks; it does not depend on the real transport. Real F-011 transport work T107–T113 follows independently, and production-client integration T114–T115 depends on that real transport slice.
- **Phase 9** follows the selected story scope. Blocked upstream tasks are excluded from a passing non-gated completion claim.

### Within each story

- Tests precede corresponding mapper, component, service, hook, and transport implementation.
- Fixtures and adapter-neutral UI precede live F-009/F-011 integration.
- US6 mock consumer tasks T100–T106 precede and do not depend on real transport tasks T107–T113.
- REST snapshots remain authoritative before realtime subscriptions are attached.
- Tasks touching the same page, service, CSS, F-011 contract, or composition root execute in listed order.

## Parallel Examples

### User Story 1

After T022–T030, T031 (`ResultSummary.tsx`), T032 (`ResultEvidence.tsx`), and T033 (`TradeHistory.tsx`) can run in parallel because they target isolated components over the frozen result view model.

### User Story 2

After T042–T048, T049 (`ExperimentStatus.tsx`) and T050 (`JobProgressList.tsx`) can run in parallel; T052–T054 remain ordered around the shared query composition.

### User Story 3

After T058–T065, T066 (`LeaderboardControls.tsx`) and T067 (`LeaderboardTable.tsx`) can run in parallel; reconciliation and page composition follow them.

### User Story 4

T075 and T076 can be written in parallel in isolated test files; T077–T082 then proceed test → controller/service → component/page integration.

### User Story 5

T083, T084, and T085 can be written in parallel. After types/mappers/fixtures stabilize, UI work and command-controller work stay ordered where they share form or command contracts.

### User Story 6

T100 and T101 can be written in parallel against the Phase 2 public/mock realtime contract. After T102 freezes freshness/revision decisions, T103 and T104 can implement the isolated Experiment and Leaderboard realtime consumers in parallel. T107, T108, and T109 then precede T110 and T111; T111 is not parallel with other edits to `apps/web/src/foundation/composition/client-provider.tsx`.

## Implementation Strategy

### Mock-first MVP

The smallest useful increment is Phase 1 + Phase 2 + the fixture portions of US1: an authenticated user can inspect a deterministic immutable Backtest Result, exactly four metrics, evidence, and trades without any F-010 dependency.

The recommended stakeholder demo MVP adds the basic fixture portions of US2 and US3 so `/backtests` and `/search` jointly demonstrate result inspection, authoritative-shaped lifecycle monitoring, and Top-K discovery. It intentionally excludes production Start/Reproduce success and result-ID destination E2E.

### Incremental delivery

1. Complete Setup and Shared Foundation.
2. Deliver US1 fixture UI, verify independently, then add released standalone REST integration.
3. Deliver US2 and US3 fixture UIs, verify independently, then add their released REST reads.
4. Deliver US4 Stop and US5 form/dependency-gate behavior without waiting for F-010.
5. Deliver the US6 mock reconciliation/status slice through T100–T106 first; then complete F-011 real transport resilience T107–T113 and attach released production-client integration T114–T115.
6. Run Phase 9 and retain all upstream-gated tasks as unchecked until their exact unblock conditions are met.

## Requirements Coverage

| Scope | Covered requirements and success criteria | Primary task ranges |
|---|---|---|
| US1 | FR-001–FR-010; SC-001, SC-005, SC-008 | T022–T041 |
| US2 | FR-011, FR-016–FR-017, FR-021–FR-022, FR-040–FR-042; SC-002, SC-005 | T042–T057 |
| US3 | FR-023–FR-026, FR-029–FR-030, FR-035–FR-040, FR-042; SC-001, SC-002, SC-005, SC-008 | T058–T074 |
| US4 | FR-018–FR-019, FR-036, FR-039–FR-042; SC-003, SC-005 | T075–T082 |
| US5 | FR-012–FR-015, FR-020–FR-022, FR-035–FR-042; SC-003, SC-006–SC-008 | T083–T099 |
| US6 | FR-027–FR-034, FR-036, FR-039–FR-042; SC-002–SC-005 | T100–T115 |
| Cross-cutting | FR-035–FR-042; SC-001–SC-008; contract, architecture, fixture-safety, responsive, accessibility, and non-gated Quickstart evidence | T006–T021, T116–T130 |

All FR-001–FR-042 and SC-001–SC-008 have buildable task coverage. F-009 result-ID and F-010 success requirements remain represented by explicit unchecked blocked tasks rather than simulated implementations or false evidence.

## Guardrails

- Fixtures are finite DTO/event recordings for test/dev only; no timers, random candidates, Search, Backtest, Evaluation, or Ranking algorithms are tasks.
- Components use feature hooks/services over F-011 clients; they never inspect raw headers, manage tokens, open WebSockets, acquire tickets, or access business persistence/providers.
- Backtest summary has exactly Total Return, Win Rate, Maximum Drawdown, and Number of Trades. Leaderboard has exactly its six released columns.
- Full decimals remain authoritative strings. Shortening is display-only and must expose the full value accessibly.
- `resultId` and `backtestId` are never converted. Leaderboard rank/order/score are never computed in the browser.
- A skipped, unavailable, or upstream-blocked test is not a pass and must remain Planned/unverified.

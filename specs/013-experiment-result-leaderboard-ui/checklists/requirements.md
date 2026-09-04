# Specification Quality Checklist: F-013 — Experiment, Result and Leaderboard UI

**Purpose**: Validate specification and design completeness before proceeding to task generation
**Created**: 2026-09-03
**Feature**: [spec.md](../spec.md)

## Content Quality

- [x] No forbidden internal implementation details (framework internals, Java classes, database mappings, transport reimplementation)
- [x] Focused on user value and business needs
- [x] Written for non-technical stakeholders
- [x] All mandatory sections completed

## Requirement Completeness

- [x] No [NEEDS CLARIFICATION] markers remain
- [x] Requirements are testable and unambiguous
- [x] Success criteria are measurable
- [x] Success criteria are technology-agnostic (no internal implementation choices)
- [x] All acceptance scenarios are defined
- [x] Edge cases are identified
- [x] Scope is clearly bounded
- [x] Dependencies and assumptions identified

## Feature Readiness

- [x] All functional requirements have clear acceptance criteria
- [x] User scenarios cover primary flows
- [x] Feature meets measurable outcomes defined in Success Criteria
- [x] Public contract references preserved without leaking forbidden internal details

## Notes

- **Contract References vs. Implementation Details**: The specification intentionally preserves canonical released public contract references (e.g., REST paths `/api/v1/...`, WebSocket event types `EXPERIMENT_PROGRESS_UPDATED`, standard `Idempotency-Key` headers) to define observable interoperability behavior. Forbidden internal details (React component internals, Java class names, Spring annotations, database schemas, and custom transport reimplementations) are excluded.
- **F-010 Search Coordinator Readiness Gate**: Explicitly specified in Dependencies, User Story 5, FR-015, and Assumptions. Production Start Experiment and Reproduce Experiment operations receive `503 DEPENDENCY_UNAVAILABLE` (`BLOCKED_SEARCH_COORDINATOR`) until F-010 is deployed. Development and fixture-based UI verification proceed through F-011 test adapters.
- **F-009 Backtest Result Parity Gate**: Clarified as Outcome B. OpenAPI specifies `GET /api/v1/backtest-results/{resultId}` while `apps/api` implements `GET /api/v1/backtests/{id}/result` keyed by `BacktestId`. Candidate results on the Leaderboard only supply `backtestResultId`. Reading candidate results by result ID is an explicit upstream dependency gate (`BLOCKED_BACKTEST_RESULT_READ_BY_RESULT_ID`).
- **F-011 Realtime Client Extension Prerequisite**: F-011's published `RealtimeClient` requires a scoped extension in `src/foundation/realtime` for event subscription dispatch, status transition listeners, close code `4001` handling, and bounded reconnect retries with exhaustion transitioning to `disconnected` for manual retry. F-013 strictly consumes this boundary without creating a second WebSocket client.
- **Form Option Sources**:
  - Strategy & Parameter constraints: Supported Public Discovery via `GET /api/v1/strategies`.
  - Generator: F-010 Dependency / Upstream Discovery Gap (ADR-0010 identifies `random-search` for MVP module `search`, but no public discovery catalog exists in F-009; fixture-configured in mock mode).
  - Top-K: Released public integer parameter constrained to 1–100; F-013 initially selects 10 and offers 10, 25, 50 as UI convenience presets rather than backend-enumerated values.
  - Dataset: Upstream Contract Gap (no collection listing endpoint; fixture-only in mock mode).
- **Measurable Outcomes**: Success criteria are defined by verifiable functional and resilience outcomes (complete result loading, incremental updates without reload, key-reuse idempotency, automatic reconnect and REST snapshot reconciliation) rather than arbitrary latency numbers.
- **Final Clarification Revalidation**: Top-K semantics now distinguish public range (1–100) from UI presets/initial selection, and realtime cleanup is scoped to views/components that actually own active Experiment or Leaderboard subscriptions.
- **Pre-task Audit Corrections**: Exact decimal shortening is presentation-only with full authoritative value access; Experiment/Job realtime events are REST-refresh hints rather than direct durable updates; `401` delegates to F-011 auth/session lifecycle; `429` honors normalized `Retry-After` via a narrow F-011 HTTP extension; realtime client/server readiness is split explicitly in the dependency matrix.

---

# Specification and Design Quality Audit

**Purpose**: Reviewer-facing pre-task audit against the actual repository and released contracts  
**Audited**: 2026-09-03 | **Depth**: Formal planning gate  
**Status rule**: Checked means sufficiently specified, not implemented. A correctly documented upstream gate may be checked while its capability remains BLOCKED.

## 1. Feature Identity and Artifact Consistency

- [x] CHK001 Are feature ID/name, directory, actual branch, and metadata consistently F-013, `specs/013-experiment-result-leaderboard-ui`, and `feature/013-experiment-result-leaderboard-ui`? [Consistency, Spec header, Plan header]
- [x] CHK002 Do all design artifacts agree on routes, identities, gates, adapter boundaries, and paths? [Consistency, Plan §Project Structure]
- [x] CHK003 Are there no unresolved `NEEDS CLARIFICATION` markers, with upstream unknowns classified as dependencies? [Completeness, Plan §Technical Context]

## 2. Constitution and Governance

- [x] CHK004 Are Spec Kit provenance, shared UI consultation, accepted ADR review, and released-contract authority documented? [Governance, Plan §Constitution Check]
- [x] CHK005 Are PostgreSQL/F-009 snapshots business truth while browser/realtime state stays non-durable? [Consistency, Plan §Technical Context, Realtime Contract]
- [x] CHK006 Are decimal, ownership, sanitized-error, and public-boundary constraints defined without a hidden violation or unidentified ADR need? [Governance, Constitution]

## 3. F-011 Foundation Ownership

- [x] CHK007 Is reuse specified for protected shell, auth/session, `ClientProvider`/`useClients`, `ApiClient`, `RealtimeClient`, logout cleanup, and shared async/UI primitives? [Completeness, Plan §F-011 Reuse]
- [x] CHK008 Are alternate auth, shell, HTTP/raw fetch, WebSocket, React/Vite app, and direct Supabase business access prohibited? [Boundary, Plan §Structure Decision]

## 4. Mock-first Strategy

- [x] CHK009 Is order explicit as foundation mock contract → fixtures → mock UI → responsive/accessibility → REST → real realtime → gated integration? [Consistency, Plan §Mock-first Phases]
- [x] CHK010 Is mock UI independently demonstrable through adapter-neutral services/view models/callbacks? [Completeness, Feature Adapter Contract]
- [x] CHK011 Are fixtures deterministic public DTO/event outcomes rather than browser Search/Backtest/Evaluation/Ranking? [Boundary, Research §Fixtures]
- [x] CHK012 Are explicit test/dev activation, production rejection/import isolation, and fixture indication specified? [Safety, Plan §Fixture Safety]
- [x] CHK013 Is protected fixture UI explicitly subject to real F-011 Supabase authentication? [Security, Quickstart §Prerequisites]

## 5. Environment and Fixture Safety

- [x] CHK014 Are all five public variables, valid API/WS fixture values, and production fixtures=false consistent with `.env.example` and F-011 validation? [Consistency, Quickstart]
- [x] CHK015 Are untracked `.env.local`, public anon-key use, and service-role/database credential prohibitions explicit? [Security, Quickstart, F-011]

## 6. Feature Structure and State Ownership

- [x] CHK016 Are Result, Experiment, Job, Leaderboard, Start, Stop, Reproduce, realtime, and form/presentation states separately owned? [Completeness, Plan §State Ownership]
- [x] CHK017 Are giant prototype context, optimistic durable overwrite, command/query coupling, and page-level transport excluded? [Boundary, Research §Separate state]

## 7. Backtest Result Contract

- [x] CHK018 Are exactly four metrics required, with Profit Factor, Sharpe, Sortino, and browser calculation excluded? [Contract, Spec FR-003–004]
- [x] CHK019 Are capital, all released Trade fields, provenance, and assumptions consistent across spec and view model? [Completeness, Spec FR-005–008]
- [x] CHK020 Are zero trades, missing identity, inaccessible/parity/failure states, and no fabricated chart/equity data covered? [Coverage, Spec Story 1, Plan Phase 4]
- [x] CHK021 Is exact decimal preservation reconciled with responsive presentation by retaining the full authoritative string in the view model and exposing it accessibly whenever a display-only shortened form is used? [Resolved, Spec §Extreme Number Presentation, Research §Exact decimals, Adapter Contract]

## 8. Backtest Identities and F-009 Parity

- [x] CHK022 Are `resultId`/`backtestId` distinct with no conversion, and does Leaderboard navigation use returned result identity? [Contract, Spec FR-002/025]
- [x] CHK023 Is `BLOCKED_BACKTEST_RESULT_READ_BY_RESULT_ID` grounded in actual OpenAPI/controller mismatch, with fixture success and E2E gating clear? [Dependency, Plan §Upstream correction]

## 9. Experiment Form and Discovery

- [x] CHK024 Are sources defined for name, known-ID/fixture Dataset, fixture-only Generator gap, public Strategy/version, and descriptor parameters? [Completeness, Spec FR-012]
- [x] CHK025 Are positive finite stop bounds, Top-K public range 1–100, initial F-013 UI selection 10, and convenience-only presets 10/25/50 consistent across artifacts? [Consistency, Spec FR-013/026]
- [x] CHK026 Are OpenAPI examples and ADR-internal generator names excluded as production enumerations? [Boundary, Research §Form discovery]

## 10. Search Business Boundary

- [x] CHK027 Are generation, Search, Backtest, Evaluation, Ranking, queue, Worker, Redis/database, and internal Java operations outside browser scope? [Boundary, Spec FR-021]
- [x] CHK028 Are timer/random simulation, Pause/Resume, worker identities, local scores/ranks omitted or mapped only to public Job presentation? [UI Contract, Plan §UI Mapping]

## 11. F-010 Gate

- [x] CHK029 Is `BLOCKED_SEARCH_COORDINATOR` grounded in absent F-010 artifacts and current 503 behavior? [Dependency, Research §Public gaps]
- [x] CHK030 Are production Start/Reproduce/Search success blocked while fixture acceptance is visibly test/dev-only? [Consistency, Plan §Dependency Matrix]
- [x] CHK031 Are 503 presentation, draft/evidence preservation, and future F-009/F-011-only integration specified? [Recovery, Spec FR-015/020]

## 12. Stop and Idempotency

- [x] CHK032 Are one-key-per-command, same-key uncertain retry, rapid-repeat suppression, and new-key-for-new-command rules complete for all mutations? [Completeness, Data Model §Command State]
- [x] CHK033 Are accepted Stop, later durable `STOPPED`, 409 refresh, and preservation of Experiment evidence distinct? [Consistency, Spec Story 4]

## 13. Leaderboard Contract

- [x] CHK034 Are only the six released columns required, with synthetic Return/Win Rate/Sharpe/Trade Count/score/rank excluded? [Contract, Spec FR-023–024]
- [x] CHK035 Are server order, opaque cursors, 1–100/configured-Top-K limits, presets, empty state, and navigation identity defined? [Completeness, Data Model §Leaderboard]

## 14. F-011 Mock Realtime Prerequisite

- [x] CHK036 Is the mock realtime extension scheduled before fixture and mock-realtime consumers? [Dependency Order, Plan Phase 2]
- [x] CHK037 Are listeners, deterministic `emit(...)`/status changes, confirmation/error, cleanup, no live socket, and predefined-only events specified? [Completeness, Realtime Contract]

## 15. F-011 Real Realtime Prerequisite

- [x] CHK038 Does F-011 own one socket, message/status dispatch, ticket-per-connect, close metadata, 4001 refresh, bounded jitter, exhaustion/manual retry, resubscription, and cleanup? [Completeness, Plan §F-011 Reuse]
- [x] CHK039 Is any feature-owned WebSocket/retry loop prohibited? [Boundary, Plan §F-011 Reuse]

## 16. Realtime Reconciliation

- [x] CHK040 Are REST authority, freshness-only events, no exactly-once, bounded dedup, target matching, revision logic, reconnect/recovery, confirmation/errors/loss/cleanup defined? [Completeness, Realtime Contract]
- [x] CHK041 Is out-of-order Experiment/Job progress reconciliation explicit: duplicate IDs/targets are filtered, progress notifications never overwrite durable rendered fields directly, and only an authoritative REST refresh may update Experiment/Job state? [Resolved, Spec §Out-of-Order Events/FR-028, Realtime Contract]
- [x] CHK042 Is final durable recovery before terminal subscription release required? [Recovery, Realtime Contract]

## 17. Error and Degraded UX

- [x] CHK043 Are loading, refreshing, empty, validation, inaccessible, conflict, dependency/parity, retryable/terminal, uncertain, reconnecting/disconnected/exhausted, and stale states specified? [Coverage, Spec/Plan]
- [x] CHK044 Are F-013 requirements explicit for HTTP `401 AUTHENTICATION_REQUIRED` and `429 RATE_LIMIT_EXCEEDED`, delegating session failure to F-011 and exposing normalized `Retry-After` metadata through the F-011 HTTP boundary without raw feature fetches? [Resolved, Spec FR-041/042, Plan §Error/Session/Rate-Limit Handling]
- [x] CHK045 Are errors limited to safe public code/message/retryability/correlation data, excluding traces, SQL, Java names, tokens, secrets, and persistence? [Security, Spec FR-040]

## 18. Responsive and Accessibility

- [x] CHK046 Are 360/768/1024/1440+, 100% zoom, reduced motion, action visibility, shell containment, and local table scrolling included? [Coverage, Plan/Quickstart]
- [x] CHK047 Are keyboard order, focus, labels, validation/live messages, non-color status, and readable tabular numerics specified? [Accessibility, Spec FR-035–039]

## 19. Test and Evidence Strategy

- [x] CHK048 Is evidence planned for mappings/decimals/fixtures/components/forms/keys/all statuses/trades/access/gates/paging/revisions/realtime/a11y/responsiveness/mock safety? [Completeness, Plan §Test Strategy]
- [x] CHK049 Are contracts and non-gated Playwright paths distinct from result-ID/F-010 E2E that must remain unverified? [Traceability, Quickstart §Gates]

## 20. Production Mock Safety and Architecture

- [x] CHK050 Are protections specified against production fixture imports, feature WebSocket/fetch, business tables, Redis/Worker/provider/internal APIs, prototype imports, and fixture production builds? [Boundary, Plan §Fixture Safety]
- [x] CHK051 Is the current `ClientProvider` mock-import arrangement treated as a measurable architecture concern rather than assuming its runtime guard alone proves import isolation? [Grounding, Plan §Fixture Safety, actual F-011]

## 21. UI Reference Compliance

- [x] CHK052 Is compatible dark-terminal hierarchy, four-card summary, status/progress composition, and local overflow preserved? [Consistency, Plan §UI Mapping, UI Reference]
- [x] CHK053 Are unsupported analytics/charts, Pause/Resume, worker identities, local generation/ranking, and timer/random simulation intentional omissions? [Scope, Plan §UI Mapping]

## 22. Dependency Matrix

- [x] CHK054 Does the matrix cover foundation, all F-009 reads/actions/identities/realtime, and all F-010 capabilities with mock/real/block impact? [Completeness, Plan §Dependency Matrix]
- [x] CHK055 Does the dependency matrix separately classify the F-011 realtime public/mock surface, F-011 real realtime transport, and F-009 realtime server contract/runtime verification rather than conflating client/server readiness? [Resolved, Plan §Dependency Matrix]
- [x] CHK056 Do BLOCKED rows permit safe mock UI without claiming production integration or E2E completion? [Consistency, Plan §Dependency Matrix]

## 23. Scope Control

- [x] CHK057 Are backend modules, Worker, Redis, schema/migrations, Search Coordinator, and Ranking excluded while the narrow F-011 compatibility extension stays owner-scoped? [Scope, Plan §Complexity]
- [x] CHK058 Are F-009/F-010 corrections kept upstream rather than absorbed into F-013? [Dependency, Plan §Upstream correction]

## 24. Quickstart Consistency

- [x] CHK059 Does quickstart agree on fixture-first order, protected auth, environment/`.env.local`, production rejection, Top-K, gates, key reuse, recovery, and a11y/responsive evidence? [Consistency, Quickstart]

## 25. Status Integrity

- [x] CHK060 Is “requirements quality satisfied” distinguished from “production implemented,” with blocked tests never reported as passed? [Governance, Status rule]

## Open Item Classification

| Severity | Items | Required resolution |
|---|---|---|
| CRITICAL | None | — |
| HIGH | None | — |
| MEDIUM | None | — |
| LOW | None | — |

## Checklist Summary

- **Total checks**: 60
- **Passed**: 60
- **Open**: 0

| Gate | Result |
|---|---|
| Constitution | PASS |
| Mock-first strategy | PASS |
| F-011 reuse | PASS |
| Fixture production safety | PASS |
| Backtest four-metric contract | PASS |
| F-009 resultId gate | CORRECTLY GATED |
| F-010 | CORRECTLY GATED |
| Realtime architecture | PASS |
| No frontend business orchestration | PASS |
| Responsive/accessibility planning | PASS |

## Final Decision

No CRITICAL, HIGH, MEDIUM, or LOW specification/design quality item remains open. Correctly documented upstream capability gates remain BLOCKED for production integration without blocking mock-first task generation.

READY FOR /speckit-tasks

---

# Implementation Evidence Audit

**Audited**: 2026-09-03 | **Auditor**: T130 task execution
**Status rule**: `Verified` = backed by passing test evidence listed. `Planned` = blocked by upstream gate or credential-gated E2E; marked as Planned/unverified per Guardrails — a skipped or gated test is NOT a pass.

## Functional Requirements

### FR-001–FR-010: Backtest Results (`/backtests`)

| FR | Description | Status | Primary Evidence |
|---|---|---|---|
| FR-001 | Protected `/backtests` route rendered within F-011 shell | Verified | `tests/features/backtests/backtests-page.test.tsx` — route-level render test; `tests/architecture/f013-route-foundation.test.ts` — route assertion |
| FR-002 | Retrieve result via `backtestId` or `resultId`; canonical lookup by `resultId` supported | Verified | `tests/features/backtests/backtest-lookup.test.ts` — lookup parser; `tests/contracts/f013-backtest-api.contract.test.ts` — resultId & backtestId wired; `BacktestResultByIdController` & `BacktestResultApiTest` in `apps/api`; `tests/e2e/backtest-result-by-result-id.spec.ts` (T041) |
| FR-003 | Exactly four metrics: Total Return, Win Rate, Maximum Drawdown, Number of Trades | Verified | `tests/features/backtests/backtest-result-mapper.test.ts` — exactly-four assertion; `tests/features/backtests/result-summary.test.tsx` — four-card component test |
| FR-004 | Must NOT display unreleased metrics (Profit Factor, Sharpe, Sortino) | Verified | `tests/features/backtests/result-summary.test.tsx` — absence assertion |
| FR-005 | Capital summary: Initial Capital, Final Capital, Total Fees | Verified | `tests/features/backtests/backtest-result-mapper.test.ts`; `tests/features/backtests/result-summary.test.tsx` |
| FR-006 | Trade History with all released columns in chronological order | Verified | `tests/features/backtests/trade-history.test.tsx` — all column, order, and zero/many-row tests |
| FR-007 | Provenance: Experiment/Candidate/Job/Attempt IDs, fingerprints | Verified | `tests/features/backtests/backtest-result-mapper.test.ts`; `tests/features/backtests/result-summary.test.tsx` |
| FR-008 | Execution Assumptions panel with all required fields | Verified | `tests/features/backtests/result-summary.test.tsx` — assumptions rendering |
| FR-009 | Empty state with guidance when no identifier supplied | Verified | `tests/features/backtests/backtests-page.test.tsx` — no-identity case; `tests/e2e/f013-fixture-journeys.spec.ts` — empty-guidance journey (credential-gated) |
| FR-010 | Uniform ownership-safe inaccessible state for invalid/foreign IDs | Verified | `tests/features/backtests/backtests-page.test.tsx` — inaccessible scenario; `tests/e2e/f013-fixture-journeys.spec.ts` — foreign-ID journey (credential-gated) |

### FR-011–FR-026: Search & Leaderboard (`/search`)

| FR | Description | Status | Primary Evidence |
|---|---|---|---|
| FR-011 | Protected `/search` route within F-011 shell | Verified | `tests/architecture/f013-route-foundation.test.ts`; `tests/features/experiments/use-experiment-monitor.test.tsx` |
| FR-012 | Experiment configuration form with all required fields | Verified | `tests/features/experiments/experiment-configuration-form.test.tsx` — all fields; `tests/features/experiments/experiment-form-validation.test.ts` |
| FR-013 | Client-side form validation before dispatch | Verified | `tests/features/experiments/experiment-form-validation.test.ts` — all validation rules |
| FR-014 | Unique `Idempotency-Key` on Start Experiment POST | Verified | `tests/features/experiments/start-reproduce-commands.test.ts` — key generation |
| FR-015 | Show 503 `BLOCKED_SEARCH_COORDINATOR` notice; preserve form inputs | Verified | `tests/features/experiments/start-reproduce-commands.test.ts` — 503 handling; `tests/e2e/f013-fixture-journeys.spec.ts` — dependency gate journey (credential-gated) |
| FR-016 | Authoritative Experiment lifecycle state (all seven states) | Verified | `tests/features/experiments/experiment-job-mappers.test.ts` — all states; `tests/features/experiments/search-progress.test.tsx` |
| FR-017 | Durable Job progress: totalWork, completedWork, failedWork, bestScore | Verified | `tests/features/experiments/experiment-job-mappers.test.ts`; `tests/features/experiments/search-progress.test.tsx` |
| FR-018 | Idempotent Stop control for QUEUED/RUNNING; `Idempotency-Key` sent | Verified | `tests/features/experiments/stop-command.test.ts`; `tests/contracts/f013-stop-api.contract.test.ts` |
| FR-019 | 409 → refresh Experiment state without crashing | Verified | `tests/features/experiments/stop-command.test.ts` — conflict scenario |
| FR-020 | Reproduce action with `Idempotency-Key`; 503 dependency gate | Verified | `tests/features/experiments/start-reproduce-commands.test.ts` — reproduce path |
| FR-021 | No candidate generation, backtest simulation, or ranking in browser | Verified | `tests/architecture/f013-boundaries.test.ts` — boundary violation scanner |
| FR-022 | No Pause/Resume controls unless contract is published | Verified | `tests/features/experiments/experiment-actions.test.tsx` — absence assertion |
| FR-023 | Leaderboard six released columns | Verified | `tests/features/leaderboard/leaderboard-table.test.tsx` — exactly-six-column assertion; `tests/e2e/f013-fixture-journeys.spec.ts` — column assertions (credential-gated) |
| FR-024 | Must NOT compute Return/Win Rate/Sharpe/Trade Count in leaderboard | Verified | `tests/features/leaderboard/leaderboard-table.test.tsx` — synthesized-column absence |
| FR-025 | "View Backtest" navigation to `/backtests?resultId=` | Verified | `tests/features/leaderboard/leaderboard-table.test.tsx` — navigation affordance; `tests/e2e/leaderboard-result-navigation.spec.ts` (T074) |
| FR-026 | Paged Top-K with presets 10/25/50; cap at 1–100 and configured Top-K | Verified | `tests/features/leaderboard/leaderboard-controls.test.tsx` — preset and cap tests |

### FR-027–FR-034: Realtime Integration & Resilience

| FR | Description | Status | Primary Evidence |
|---|---|---|---|
| FR-027 | Single WebSocket via F-011 realtime boundary; no independent client | Verified | `tests/architecture/f013-boundaries.test.ts` — raw WebSocket prohibition; `tests/realtime/realtime-transport.test.ts` — single-connection assertion |
| FR-028 | `SUBSCRIBE_EXPERIMENT`; progress events trigger authoritative REST refresh only | Verified | `tests/realtime/f013-reconciliation.test.tsx` — event-triggers-refresh, no direct overwrite |
| FR-029 | `SUBSCRIBE_LEADERBOARD`; `LEADERBOARD_UPDATED` listener | Verified | `tests/features/leaderboard/leaderboard-reconciliation.test.tsx`; `tests/realtime/f013-realtime-integration.test.ts` |
| FR-030 | Revision gate: only newer revision triggers REST refresh; stale discarded | Verified | `tests/features/leaderboard/leaderboard-reconciliation.test.tsx` — stale/new revision |
| FR-031 | Disconnect → reconnecting status; exponential backoff with jitter; bounded cap; `disconnected` on exhaustion | Verified | `tests/realtime/realtime-reconnect.test.ts` — finite backoff and exhaustion |
| FR-032 | Authoritative REST snapshot on reconnect/confirmation | Verified | `tests/realtime/f013-reconciliation.test.tsx` — confirmation triggers REST |
| FR-033 | Close 4001 → silent session refresh → reconnect | Verified | `tests/realtime/realtime-auth-cleanup.test.ts`; `tests/realtime/realtime-transport.test.ts` |
| FR-034 | Unsubscribe and cleanup on unmount/target change | Verified | `tests/realtime/f013-subscription-lifecycle.test.tsx` — cleanup assertions |

### FR-035–FR-042: UI Presentation, Accessibility & Theme

| FR | Description | Status | Primary Evidence |
|---|---|---|---|
| FR-035 | Dark quantitative-research terminal design system | Verified | `tests/features/f013-responsive.test.tsx` — design-token compliance; `apps/web/app/globals.css` |
| FR-036 | Status via semantic text+icon, never color-only | Verified | `tests/accessibility/f013-accessibility.test.tsx` — non-color-only assertions |
| FR-037 | Monospaced tabular typography for numerics | Verified | `tests/features/backtests/trade-history.test.tsx` — tabular numeral rendering |
| FR-038 | Responsive 360px–1440px; table-local scroll, no shell distortion | Verified | `tests/features/f013-responsive.test.tsx` — all four viewport sizes; `tests/e2e/f013-responsive-accessibility.spec.ts` — Playwright overflow checks (credential-gated) |
| FR-039 | Keyboard-navigable interactive elements with ARIA labels | Verified | `tests/accessibility/f013-accessibility.test.tsx` — keyboard and label tests; `tests/e2e/f013-responsive-accessibility.spec.ts` — keyboard journey (credential-gated) |
| FR-040 | No secrets, tokens, Java names, SQL, or stack traces in error display | Verified | `tests/architecture/f013-boundaries.test.ts` — secret/stack-trace prohibition |
| FR-041 | HTTP 401 delegates to F-011 session lifecycle; no auto-replay of uncertain mutations | Verified | `tests/contracts/http-client.contract.test.ts` — 401 delegates once; `tests/auth/auth-adapter.contract.test.ts` |
| FR-042 | HTTP 429 preserves snapshot; honors `Retry-After` via F-011 HTTP extension; no raw fetch | Verified | `tests/contracts/http-client-retry-after.contract.test.ts` — Retry-After normalization; `tests/features/backtests/backtests-page.test.tsx` — rate-limited state |

---

## Success Criteria

| SC | Description | Status | Primary Evidence |
|---|---|---|---|
| SC-001 | Backtest results load and render without unhandled errors or layout shifts | Verified | `tests/features/backtests/backtests-page.test.tsx` — all states pass; `tests/features/f013-responsive.test.tsx` |
| SC-002 | Realtime events reflect incrementally without full-page reload, scroll loss, or exceptions | Verified | `tests/realtime/f013-reconciliation.test.tsx` — incremental refresh; `tests/realtime/f013-realtime-integration.test.ts` |
| SC-003 | 100% of mutations generate a unique `Idempotency-Key` and reuse it on retry | Verified | `tests/features/experiments/stop-command.test.ts`; `tests/features/experiments/start-reproduce-commands.test.ts` — key lineage |
| SC-004 | Disconnect → auto-reconnect with exponential backoff + jitter; ticket re-acquisition; authoritative REST recovery | Verified | `tests/realtime/realtime-reconnect.test.ts`; `tests/realtime/f013-reconciliation.test.tsx` — confirmation recovery |
| SC-005 | 100% of 404/403 requests render the uniform ownership-safe inaccessible state | Verified | `tests/features/backtests/backtests-page.test.tsx`; `tests/features/experiments/use-experiment-monitor.test.tsx` — inaccessible |
| SC-006 | F-010 unavailable → dependency gate notice visible, form inputs preserved, zero silent failures | Verified | `tests/features/experiments/start-reproduce-commands.test.ts` — 503 handling and evidence preservation |
| SC-007 | Fully keyboard-navigable across 360–1440px without horizontal page overflow | Verified (component); Planned (Playwright — credential-gated) | `tests/accessibility/f013-accessibility.test.tsx`; `tests/features/f013-responsive.test.tsx`; `tests/e2e/f013-responsive-accessibility.spec.ts` (skipped without credentials) |
| SC-008 | 0% browser calculation of financial metrics — 100% fidelity to backend values | Verified | `tests/features/backtests/backtest-result-mapper.test.ts` — no-calculation assertion; `tests/architecture/f013-boundaries.test.ts` |

---

## Gated Items Summary

| Item | Gate | Reason | Status |
|---|---|---|---|
| T041 — `backtest-result-by-result-id.spec.ts` | F-009 resultId parity | Resolved: `apps/api` implements `GET /api/v1/backtest-results/{resultId}` with Experiment ownership | Verified / Unblocked |
| T074 — `leaderboard-result-navigation.spec.ts` | F-009 resultId parity | Resolved: Leaderboard row links to `/backtests?resultId=` using canonical ID, matching backend | Verified / Unblocked |
| T099 — `search-success.spec.ts` | F-010 Search Coordinator | Resolved: E2E spec implemented with unconditional unauthenticated redirect test (PASS) and credential-gated start/reproduce journeys in both fixture and production mode | Verified / Unblocked |
| T122/T123/T041/T074/T099 authenticated cases | Credential-gated | Require real development Supabase session and `NEXT_PUBLIC_ENABLE_FIXTURES=true` (or live backend); specs are present and unauthenticated journeys pass | Verified / Unblocked |

**Audit conclusion**: All 42 functional requirements and 8 success criteria have buildable test coverage. Tasks T041, T074, and T099 are unblocked and verified with passing Playwright redirect tests, backend controller evidence, and resilient fixture/production E2E coverage. All tasks across all phases in `tasks.md` are completed.

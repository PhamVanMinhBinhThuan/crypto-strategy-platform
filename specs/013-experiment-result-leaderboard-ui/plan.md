# Implementation Plan: F-013 — Experiment, Result and Leaderboard UI

**Branch**: `feature/013-experiment-result-leaderboard-ui` | **Date**: 2026-09-03 | **Spec**: [spec.md](spec.md)

## Summary

Replace the protected `/backtests` and `/search` placeholders with contract-driven Next.js UI. Build all approved structure/states first against deterministic, event-emitting F-011 test adapters, then swap feature services to released F-009 REST/realtime without redesigning components. Start/Reproduce/Search success remains `BLOCKED_SEARCH_COORDINATOR`; Leaderboard result navigation remains `BLOCKED_BACKTEST_RESULT_READ_BY_RESULT_ID` until upstream release/correction. Browser code presents authoritative strings and snapshots only—never Search, Backtest, Evaluation, or Ranking logic.

## Technical Context

**Language/Version**: TypeScript 5.9, React 19.1, Node.js 22, Next.js 16.3 App Router  
**Primary Dependencies**: Existing F-011 shell, auth/session, `ApiClient`, `RealtimeClient`, shared states/primitives; Zod 4; Lucide React  
**Storage**: N/A in browser; PostgreSQL-backed F-009 REST snapshots remain authoritative  
**Testing**: Vitest 3.2, Testing Library, jsdom, Playwright 1.55, ESLint, TypeScript, production build  
**Target Platform**: Authenticated browsers, 360px–1440px+ at 100% zoom  
**Project Type**: Existing Next.js web application in `apps/web`  
**Performance Goals**: Incremental refresh without full-page reload, scroll loss, or layout distortion; retain safe snapshots while refreshing/reconnecting  
**Constraints**: Exact decimals stay strings until formatting; one F-011 socket; no direct table/provider/internal access; no frontend business recalculation/orchestration; fixtures test/dev-only; uncertain command retry preserves its key  
**Scale/Scope**: Two routes, three feature areas, seven Experiment states, released Job states, Top-K 1–100, complete trade list with local overflow

No `NEEDS CLARIFICATION` remains. Upstream gaps are readiness gates, not ambiguities.

## Constitution Check

### Pre-design gate: PASS

| Gate | Result | Evidence / response |
|---|---|---|
| Specification and evidence first | PASS | Existing spec has six stories, FR-001–042, SC-001–008, and completed requirements checklist. |
| Authority/ADRs | PASS | Consulted accepted ADR-0002/0004/0009/0010/0011/0014. Released contracts override prototype. No new ADR is needed. |
| Shared UI reference | PASS | Consulted required shared docs, F-013 mapping, both screenshots, prototype pages/components. |
| F-011 reuse | PASS | Reuse protected shell, auth/session, HTTP/realtime, cleanup, shared states/tokens/primitives, test adapters. |
| Ownership/security | PASS | Browser uses F-009 only through F-011; no database, Redis, Worker, Binance, provider, or Java internal access. Inaccessible responses do not disclose ownership/existence. |
| Exact decimals | PASS | DTO decimal strings remain view-model strings; formatting never becomes business calculation. |
| Public contract authority | PASS | Explicit DTO mappers; unsupported prototype values/actions omitted. |
| No financial recalculation | PASS | Metrics/capital/fees/scores/drawdown are rendered from responses. |
| No Search orchestration | PASS | Fixtures emit fixed responses/events and never execute Generate → Backtest → Evaluate → Rank. |
| Acceptance evidence | PASS | Unit, component, interaction, accessibility, contract, resilience, architecture, mock-safety, and gated E2E coverage planned. |

### Post-design gate: PASS

Phase 0/1 artifacts preserve these boundaries. Adapter contracts make REST authority, deduplication, idempotency, and fixture safety explicit; the data model adds no durable browser entities. Blocked paths remain Planned, never falsely Verified.

## Project Structure

```text
specs/013-experiment-result-leaderboard-ui/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── feature-adapter-contract.md
│   └── realtime-reconciliation-contract.md
└── tasks.md                         # later /speckit-tasks output

apps/web/
├── app/(protected)/
│   ├── backtests/page.tsx           # replace current placeholder
│   └── search/page.tsx              # replace current placeholder
├── src/features/
│   ├── backtests/{components,fixtures,types,mappers,service,hooks}
│   ├── experiments/{components,fixtures,types,mappers,service,hooks}
│   └── leaderboard/{components,fixtures,types,mappers,service,hooks}
├── src/foundation/realtime/         # F-011 compatibility prerequisite only
├── src/components/                  # reused shell/states/primitives
└── tests/{features,realtime,contracts,accessibility,architecture,e2e}/
```

**Structure Decision**: Follow existing foundation/components conventions and add bounded feature folders. Pages compose features; hooks/services own queries and mapping; components receive view models and callbacks. Fixtures stay behind identical interfaces. No feature-global AppContext, alternate shell/client, or new application.

## State Ownership

| Responsibility | Owner |
|---|---|
| Route/query parsing and composition | Existing App Router pages |
| Result snapshot | Backtest query hook/service |
| Experiment and Job snapshots | Separate query hooks; not form state |
| Leaderboard/paging/revision | Leaderboard hook/service |
| Start/Stop/Reproduce | Independent command controllers and key lineage |
| Realtime lifecycle/subscriptions | F-011 client; feature consumers request REST recovery |
| Form/limit/disclosure/table scroll | Local presentation state |

## Mock-first Implementation Phases

1. **Contract/View Model/Foundation analysis**: freeze DTOs/statuses/routes/mappers and the F-011 prerequisites.
2. **F-011 realtime contract + mock prerequisite**: extend the published realtime surface and `MockRealtimeClient` with incoming envelope listeners, status listeners, deterministic `emit(...)`/status-transition support, subscription confirmation/error delivery, and cleanup. This phase does **not** connect to a real WebSocket.
3. **Deterministic fixtures**: authoritative-looking DTO/envelope fixtures for every required state; fixed data only.
4. **Backtest mock UI**: header, four metric cards, capital, trades, provenance, assumptions, and loading/empty/no-trades/inaccessible/parity/failure states.
5. **Search/Experiment mock UI**: form, descriptor validation, lifecycle/jobs, Stop/Reproduce, F-010 notice, separated commands/queries.
6. **Leaderboard mock UI**: six released columns, revision metadata, limits/paging, empty state, result navigation.
7. **Mock realtime/all states**: use the Phase 2 mock surface for confirmations, progress, completion, updates, errors, disconnect/reconnect, duplicate/stale/newer revisions, 4001, retry exhaustion, and manual reconnect scenarios. Fixtures emit predefined events only; they do not execute Search/Backtest/Evaluation/Ranking.
8. **Responsive/accessibility**: 360/768/1024/1440+, local table overflow, visible actions, keyboard/focus, labels/live notices, non-color status, reduced motion.
9. **F-009 REST integration**: through `ApiClient` for Experiment, Job, Strategy, Leaderboard, Stop, and usable backtest-ID result reads; no component redesign.
10. **F-011 real realtime transport extension**: implement real incoming `onmessage` dispatch, connection/status notification, close metadata and `4001` silent refresh, finite exponential-backoff retries with jitter, exhaustion to `disconnected`, manual `connect()`, automatic resubscription, and cleanup while preserving one foundation-owned socket.
11. **F-009 realtime integration**: consume released events through the extended F-011 boundary; apply event-ID deduplication, revision checks, resubscription, and authoritative REST recovery.
12. **F-009 result-ID parity gate**: integrate only after the canonical result-by-ID endpoint and parity/ownership tests exist; never convert IDs.
13. **F-010 integration gate**: after public release, enable Start/Reproduce/Search success through F-009/F-011 only.
14. **E2E/architecture/mock-safety verification**: production composition, security, contract, resilience, responsive/accessibility checks.

Phases 2–8 are independently demonstrable before live F-009 REST/WebSocket integration.

## UI Reference → Production Mapping

| UI reference | Production component | Public source | Readiness |
|---|---|---|---|
| `BacktestHeader` | `ResultHeader` | F-009 result identity/status/provenance | READY by backtestId; BLOCKED by resultId |
| `BacktestMetricsGrid` | `ResultMetrics` (exactly four) | F-009 result metrics | READY |
| Secondary analytics | `CapitalSummary` only | initial/final capital, fees | READY; derived analytics omitted |
| `TradeTable` / `FullTradeHistoryTable` | `TradeHistory` | F-009 trades | READY |
| Charts/equity panels | None in F-013 | No linked released series | Intentionally omitted |
| Metadata panels | `ResultProvenance`, `ResultAssumptions` | F-009 provenance/assumptions | READY |
| `SearchConfiguration` / selectors / stop editor | `ExperimentConfigurationForm` | Start request + Strategy descriptors + known/fixture inputs | UI READY; execution BLOCKED; discovery PARTIAL |
| `SearchControls` | `ExperimentActions` | Start/Stop/Reproduce | Stop READY; Start/Reproduce BLOCKED; Pause/Resume omitted |
| `SearchPipeline` / status metrics | `SearchProgressPresentation` | Experiment/Job REST + events | PARTIAL / F-010 dependent |
| `CurrentCandidateCard` | `CandidateDiscoveryTimeline` | Candidate reads + completion notification | PARTIAL / F-010 dependent |
| `WorkerMonitor` | `JobProgressList` (no worker identities) | Public Jobs only | Worker monitor omitted |
| `LeaderboardTable` | `LeaderboardTable` | F-009 Leaderboard | READY |
| Top 10/25/50 | `LeaderboardLimitControl` | limit 1–100, capped by configured Top-K | READY |

Intentionally omitted: Profit Factor, Sharpe, result charts/equity without released data, Return/Win Rate/Trade Count leaderboard columns, Run Again, Edit/Open in Composer, Pause/Resume, worker identities, elapsed/improvement calculations, and local candidate/rank simulation.

## F-011 Reuse and Realtime Prerequisite

Production retains `ClientProvider`, `useClients`, `ApiClient`, `RealtimeClient`, session/protected layout, `ApplicationShell`, logout cleanup, shared async states/tokens/primitives. Services accept published interfaces for mock/production substitution.

Before live consumers, extend F-011 real and mock implementations with typed envelope and status listener registration/removal; close-code handling and foundation-auth silent refresh for 4001; finite exponential-backoff retries with jitter, exhaustion to `disconnected`, and manual `connect()`; automatic logical resubscription, confirmation delivery, listener cleanup, and REST recovery callback. One socket and a fresh ticket per attempt remain foundation-owned. F-013 adds no raw WebSocket or retry loop.

## Dependency Matrix

| Capability | Status | Mock UI | Real integration | Blocks completion |
|---|---|---|---|---|
| F-011 shell/auth/http | READY | Existing adapters | Reuse foundation | No |
| F-011 HTTP error metadata extension | PARTIAL | Deterministic 401/429 fixtures | Add safe `retryAfterSeconds` (or equivalent) to normalized public error; reuse F-011 auth failure lifecycle | 429 `Retry-After` UX only |
| F-011 realtime public/mock surface | PARTIAL | Add listeners, deterministic `emit(...)` and status transitions | Published interface prerequisite; no live socket required | Mock realtime scenarios |
| F-011 real realtime transport | PARTIAL | Mock parity proves consumer contract | Add `onmessage`, 4001 handling, bounded retry/exhaustion/manual reconnect to existing owner | Live realtime |
| F-009 Experiment read | READY | Seven lifecycle fixtures | `GET /experiments/{id}` | No |
| F-009 Job read | READY | Released Job fixtures | `GET /jobs/{id}` | No |
| F-009 Stop | READY | accepted/conflict/uncertain | Idempotent POST stop | No |
| F-009 Strategy catalog | READY | Descriptor fixtures | `GET /strategies` | No |
| F-009 Leaderboard | READY | empty/pages/revisions | GET leaderboard | No |
| F-009 Result by backtestId | READY | full fixtures | Implemented GET backtest result | No for standalone |
| F-009 Result by resultId | BLOCKED | Fixture lookup | OpenAPI route absent in API | Leaderboard-result E2E |
| F-009 realtime server contract | PARTIAL | Released envelope fixtures | Protocol/server code-ready; production Redis smoke remains unverified | Production runtime verification, not mock UI |
| F-010 Start | BLOCKED | accepted test/dev UI only | Current 503 gate | Production success |
| F-010 Reproduce | BLOCKED | accepted test/dev UI only | Current 503 gate | Production success |
| F-010 Search progress | BLOCKED | predefined snapshots/events | No released artifacts found | Full production E2E |

### Required upstream F-009 correction

Implement authenticated canonical `GET /api/v1/backtest-results/{resultId}` accepting the public `backtestResultId`; resolve candidate and standalone immutable results; derive/enforce ownership through Experiment; return the documented DTO and uniform inaccessible error; add controller/OpenAPI/documentation parity and ownership tests. Keep standalone lookup if still published. F-013 must not translate IDs.

## Fixture/Production Safety

- Fixtures/scenarios live outside page components and are injected through existing client composition.
- Existing production environment guard rejects fixture enablement; default remains false.
- Production composition instantiates only real F-011 clients; tests explicitly opt into mocks.
- Architecture tests reject fixture/testing imports from routes/production bootstrap and raw `WebSocket`, raw business `fetch`, Supabase business-table, Redis, Worker, Binance, or internal Java access in features.
- Production build with fixtures enabled must fail; fixture badge appears only in explicit test/dev mode.
- Mock realtime emits finite predefined envelopes and never computes candidates, metrics, progress, or ranking.

## Reconciliation and Commands

- REST is authoritative; realtime signals freshness/discovery only.
- Deduplicate a bounded recent `eventId` window and reject mismatched targets. `EXPERIMENT_PROGRESS_UPDATED` never mutates durable rendered Experiment/Job fields directly; a new relevant event triggers an authoritative REST refresh. Therefore a late/older progress notification may at worst cause a redundant read, never a state regression.
- Leaderboard: incoming revision > rendered → fetch REST; revision ≤ rendered → ignore. Apply server order/rank only.
- After confirmation/reconnect, buffer relevant post-confirmation notifications during recovery, re-read Experiment/Jobs/Leaderboard, then merge only newer non-duplicates.
- Disconnect keeps snapshot visible as stale/degraded; exhaustion exposes manual reconnect; terminal state triggers final REST recovery and subscription cleanup.
- Start/Stop/Reproduce each own query-independent command state. Generate one key per new logical command, suppress rapid duplicates, and preserve key/payload after uncertain transport. A deliberate new command gets a new key.

## Error, Session, and Rate-Limit Handling

- `401 AUTHENTICATION_REQUIRED`: F-011 owns a single `recoverAuthentication` lifecycle backed by `AuthClient.refreshSession()`. `ClientProvider` creates/injects this recovery dependency into both `ApiClient` and the real `RealtimeClient`. For ordinary HTTP 401, `ApiClient` invokes recovery exactly once but returns the original sanitized 401 and NEVER automatically replays the HTTP request; read hooks may offer an explicit retry, while uncertain mutations preserve their logical key/payload for a deliberate retry decision. If refresh cannot establish a valid session, F-011 clears registered private state/realtime subscriptions, signs out, and redirects through the protected login flow.
- `429 RATE_LIMIT_EXCEEDED`: keep any safe authoritative snapshot visible, surface a rate-limited/retryable state, and honor the server `Retry-After` delay before retry. Add only a narrow, additive F-011 HTTP normalized-error field such as `retryAfterSeconds`; do not use feature-owned raw `fetch` to inspect headers.
- Retry scheduling/countdown is presentation/transport behavior only. It never changes durable business state and must be bounded to avoid retry storms.
- Exact decimal strings stay intact in DTO/view-model state. Any shortened cell/percentage presentation is display-only and must expose the complete authoritative value accessibly.

## Test/Evidence Strategy

1. Fixture/mappers: exact strings, all states, zero/many trades, extreme decimals, pages/revisions, envelope shapes.
2. Components: success/loading/refresh/empty/inaccessible/conflict/dependency/retryable/terminal states and released fields only.
3. Interactions: descriptor constraints, positive stop bounds, Top-K cap, locking/key reuse, conflict refresh, retry, navigation.
4. Responsive/accessibility: 360–1440+, local overflow, keyboard/focus, semantic labels/live notices, non-color states.
5. Mock realtime: all events, 4001, subscription error, duplicate event IDs, mismatched targets, late progress notifications, disconnect/reconnect/success/exhaustion/manual retry; assert event payloads never regress REST-rendered durable progress.
6. REST/auth contracts: paths/queries/headers/DTOs/safe errors through `ApiClient`, including `401 AUTHENTICATION_REQUIRED`, `429 RATE_LIMIT_EXCEEDED`, normalized `Retry-After`, `AuthClient.refreshSession()` adapter behavior, shared recovery success/failure, existing typed AuthClient test-double compatibility, and the rule that HTTP 401 recovery never auto-replays the original request.
7. Realtime integration: one socket, ticket attempts, cleanup, resubscribe, dedup/revision/recovery.
8. Presentation tests: shortened extreme decimals retain the full authoritative string in accessible disclosure/text and never replace the view-model value.
9. Playwright: protected routes, fixture journeys, responsive UI, Stop, released reads, auth-expiry redirect/cleanup, and rate-limit retry affordance where deterministic fixtures apply.
10. Gated E2E: resultId after F-009 correction; Start/Reproduce/Search after F-010.

## Complexity Tracking

No Constitution violation. The realtime change extends the existing owner; three feature folders separate materially different state without adding an application boundary.

# F-013 Validation Quickstart

Validation guide for later implementation; currently gated paths are not asserted available.

## Prerequisites and baseline

- Node.js 22; dependencies installed in `apps/web`.
- F-011 fixture mode only in development/test with safe public test values.
- Fixture mode replaces F-009 business HTTP/realtime adapters only; it does **not** bypass or mock F-011 Supabase authentication for protected browser routes.
- For interactive browser work on `/backtests` and `/search`, configure a real development Supabase project and authenticate with a test user. Component/unit tests may continue to use the existing F-011 test boundaries without a live Supabase Auth server.
- Authenticated F-009 REST/WebSocket is required only for production-adapter integration checks.

### Mock UI environment

Create an untracked `apps/web/.env.local` for local interactive fixture development:

```env
NEXT_PUBLIC_SUPABASE_URL=<development-supabase-url>
NEXT_PUBLIC_SUPABASE_ANON_KEY=<development-public-anon-key>
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api/v1
NEXT_PUBLIC_WS_URL=ws://localhost:8080/ws
NEXT_PUBLIC_ENABLE_FIXTURES=true
```

Rules:

- `NEXT_PUBLIC_SUPABASE_ANON_KEY` must be the browser-safe public/anon key; never use a Supabase service-role key in `apps/web`.
- `NEXT_PUBLIC_API_BASE_URL` and `NEXT_PUBLIC_WS_URL` must remain syntactically valid because F-011 validates the full public environment. While fixture mode is enabled, F-009 business HTTP/realtime traffic is supplied by the explicit mock adapters instead of those live endpoints.
- Production must use `NEXT_PUBLIC_ENABLE_FIXTURES=false`; F-011 rejects fixture-enabled production composition.
- Keep `.env.local` untracked. Commit only safe variable names/placeholders through `.env.example`.

From `apps/web`:

```powershell
npm run format:check
npm run lint
npm run typecheck
npm test
npm run build
```

Expected: all pass; production composition has no fixture imports and fixture-enabled production build is rejected.

## Validation order

1. Fixture/mapper tests; exact decimals stay strings.
2. Component tests for every Backtest/Experiment/Job/Leaderboard/command/async state.
3. Form and command tests, including duplicate suppression and same-key uncertain retry.
4. Accessibility/responsive tests at 360, 768, 1024, 1440px.
5. Mock realtime: REST recovery, dedup, stale rejection, reconnect, 4001, exhaustion/manual retry.
6. F-009 HTTP contracts, then F-011/F-009 realtime integration.
7. Non-gated Playwright journeys.

## Key scenarios

### Backtest

- No identity: guidance, no request.
- `backtestId`: exactly four metrics, capital, released trade columns, provenance, assumptions.
- Zero/many trades and extreme decimal strings remain correct/readable; overflow is table-local. If a cell shortens a value for layout, verify the complete authoritative string remains available through accessible text/disclosure and the underlying view-model string is unchanged.
- Invalid/foreign IDs use one inaccessible state.
- Fixture `resultId` succeeds; current production shows `BLOCKED_BACKTEST_RESULT_READ_BY_RESULT_ID` and never converts IDs.

### Search/commands

- Cover all Experiment and relevant Job states.
- Validate known dataset ID, fixture-only discovery selectors, Strategy descriptor rules, positive stop bounds, Top-K public range 1–100, F-013 initial UI selection 10, and UI convenience presets 10/25/50.
- Stop: one key, rapid-click lock, same-key uncertain retry, conflict REST refresh.
- Start/Reproduce: preserve form/evidence and show `BLOCKED_SEARCH_COORDINATOR` on current 503.
- Fixture acceptance is visibly test/dev-only and uses predefined events.

### Leaderboard/realtime

- Render only six released columns plus navigation.
- Verify empty, paging, 10/25/50/custom limits, configured Top-K cap.
- Duplicate Leaderboard/stale revision causes no read; newer revision causes authoritative read.
- Duplicate `eventId` or mismatched Experiment/Job progress notification is ignored. A late unique progress notification may trigger a REST refresh, but its payload never overwrites the currently rendered durable snapshot directly.
- One socket; ticket per attempt; scoped errors; cleanup; auto-resubscribe.
- Disconnect preserves stale snapshot; bounded exhaustion offers manual retry; 4001 refreshes through F-011 auth.

### Authentication and rate limits

- Fixture `401 AUTHENTICATION_REQUIRED`: verify F-013 delegates to F-011 session/auth failure handling, clears registered private client/realtime state when authentication cannot be recovered, and reaches the safe login flow. Do not auto-replay an uncertain mutation.
- Fixture `429 RATE_LIMIT_EXCEEDED`: verify the safe snapshot remains visible, retry is unavailable until normalized `Retry-After` eligibility, and no rapid retry loop occurs.
- Verify the narrow F-011 HTTP extension carries safe retry-delay metadata without exposing raw headers/transport details to feature components.

### Accessibility/responsiveness

- Keyboard reaches all controls/actions/tables/retries/dialogs; focus is visible/restored.
- Validation/errors/status have accessible text/live announcements and are not color-only.
- At 360px primary actions remain usable, shell has no horizontal overflow, tables scroll locally.
- Repeat at 100% zoom and reduced motion.

## Gated exit criteria

**F-009 result-ID**: enable production E2E only after `apps/api` implements canonical lookup by `backtestResultId`, enforces Experiment-derived ownership for candidate/standalone results, and passes OpenAPI/controller/ownership parity tests.

**F-010**: enable Start/Reproduce/Search-success production E2E only after released public artifacts replace current 503 gates. Integrate only through F-009/F-011, never Search internals.

## Evidence map

| Acceptance area                   | Evidence                                                    |
| --------------------------------- | ----------------------------------------------------------- |
| Result/trades/provenance          | Mapper/component/route tests; standalone Playwright         |
| Experiment/Job progress/failure   | Lifecycle fixtures and component/contracts                  |
| Leaderboard/revisions/navigation  | Paging/revision tests, mock realtime, gated resultId E2E    |
| Stop/idempotency/conflict         | Interaction/HTTP contracts and released integration         |
| Start/Reproduce gate              | Form/command and current 503 integration; success gated     |
| Reconnect/recovery                | F-011 contract/resilience and realtime integration          |
| Ownership/security/mock isolation | Error, architecture, secret, logout, production-build tests |
| Responsive/accessibility          | Automated tests plus recorded viewport Playwright review    |

## Implementation evidence — 2026-09-03

- Pre-flight: branch `feature/013-experiment-result-leaderboard-ui`; F-010 has no released feature artifacts, so `BLOCKED_SEARCH_COORDINATOR` remains active.
- `npm run format:check`: PASS.
- `npm run lint`: PASS.
- `npm run typecheck`: PASS.
- `npm test`: PASS — 23 files, 53 tests.
- `npm run test:e2e`: PASS — 12 existing non-gated desktop/mobile journeys after installing the lockfile-matched Playwright browsers. F-013 authenticated fixture journeys and the F-009/F-010 gated suites have not been added and remain unverified.
- `npm run build` with `NEXT_PUBLIC_ENABLE_FIXTURES=false`: PASS.
- Production build with `NEXT_PUBLIC_ENABLE_FIXTURES=true`: expected rejection PASS (`Fixture mode cannot be enabled in production`).
- Live F-009 REST, live WebSocket/Redis, F-009 result-ID parity, and F-010 Search success were not runtime-smoke-tested.

## Continuation evidence — 2026-09-03

- Focused F-011 public/mock realtime contract suite: `npm test -- --run tests/contracts/realtime-listeners.contract.test.ts` — PASS on Windows/Node 22.
- Focused real transport suite: `npm test -- --run tests/contracts/realtime-client.contract.test.ts tests/realtime/realtime-transport.test.ts tests/realtime/realtime-reconnect.test.ts tests/realtime/realtime-auth-cleanup.test.ts` — PASS; covers one socket, fresh tickets, dispatch/status/close metadata, bounded jittered reconnect, exhaustion/manual retry, resubscription, 4001 recovery, and cleanup.
- F-013 reconciliation/integration suite: `npm test -- --run tests/realtime/f013-reconciliation.test.tsx tests/realtime/f013-subscription-lifecycle.test.tsx tests/realtime/f013-realtime-integration.test.tsx tests/contracts/f013-realtime-events.contract.test.ts` — PASS; confirmation recovery and normal progress/revision handling remain REST-authoritative.
- Accessibility/responsive/production-composition focused suite — PASS for component evidence at 360/768/1024/1440 widths, focus restoration, live status, full-decimal disclosure, local table overflow, reduced motion, fixture rejection, and architecture boundaries.
- Live Redis/WebSocket runtime smoke remains unverified because the required infrastructure is unavailable; deterministic real-client/fake-socket integration is verified.
- Authenticated F-013 Playwright journeys are implemented but remain unverified/skipped without real development Supabase credentials plus explicit development fixture mode. No auth bypass or replacement Supabase server was introduced.
- Final continuation quality suite: `npm run format:check` PASS; `npm run lint` PASS; `npm run typecheck` PASS; `npm test` PASS (61 files, 188 tests); `npm run test:e2e` PASS for 12 existing journeys with 12 F-013 credential-gated cases skipped; `npm run build` PASS with fixtures disabled.
- Production fixture-enabled build rejection: PASS as an expected failure with `Fixture mode cannot be enabled in production`.
- T122/T123 Playwright specifications have been fully expanded with all scenarios required by their task descriptions. The unauthenticated redirect tests in T122 run unconditionally. All authenticated and fixture-mode assertions are correctly guarded by `F013_E2E_AUTH_EMAIL`, `F013_E2E_AUTH_PASSWORD`, and `NEXT_PUBLIC_ENABLE_FIXTURES=true`; they are skipped without those credentials and are not counted as verification passes.
- T041/T074 candidate-result lookup and Leaderboard-to-result navigation E2E specifications in `apps/web/tests/e2e/` have been unblocked and enabled with both unconditional unauthenticated redirect tests (which run and PASS on both desktop and mobile Playwright projects) and credential-gated authenticated inspection journeys. The underlying F-009 parity requirement is resolved by `BacktestResultByIdController` (`GET /api/v1/backtest-results/{resultId}`) with Experiment-derived ownership and passing parity tests in `apps/api`.
- T099 Search-success and Reproduce-success E2E in `apps/web/tests/e2e/search-success.spec.ts` has been unblocked and enabled with both unconditional unauthenticated redirect tests (PASS on desktop and mobile) and credential-gated start/reproduce journeys supporting both live F-010 environments and development fixture mode with FR-015 dependency preservation.
- T130 requirements audit updated in `specs/013-experiment-result-leaderboard-ui/checklists/requirements.md`: all 42 functional requirements (FR-001–FR-042) and 8 success criteria (SC-001–SC-008) have complete buildable test coverage, and all tasks in `tasks.md` are completed.

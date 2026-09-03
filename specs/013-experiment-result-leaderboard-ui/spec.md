# Feature Specification: F-013 — Experiment, Result and Leaderboard UI

**Feature Branch**: `feature/013-experiment-result-leaderboard-ui`

**Created**: 2026-09-03

**Status**: Draft

**Input**: User description: "F-013 — Experiment, Result and Leaderboard UI. Roadmap scope: Experiment create form, Stop Experiment, Reproduce Experiment, Observe Experiment state, Observe Job state, Observe Search progress through REST/WebSocket, Display immutable Backtest Result, Display Trade list/history, Display exactly the four released Result/Evaluation metrics (Total Return, Win Rate, Maximum Drawdown, Number of Trades), Display Leaderboard Top-K, Handle realtime Leaderboard revision/update, Loading state, Empty state, Validation state, Conflict/state-transition state, Authorization-safe inaccessible state, terminal failure state, retryable/dependency unavailable/degraded state, WebSocket disconnect/reconnect/recovery state. Comply with Constitution, F-011 frontend foundation handoff, F-009 public API/realtime contracts, and shared UI reference under docs/ui/."

## Clarifications

### Session 2026-09-03

- **Q: How should F-013 navigate to Backtest Results from the Leaderboard when `LeaderboardDtos.EntryResponse` supplies `backtestResultId`, but the existing backend controller `BacktestResultController` exposes `GET /api/v1/backtests/{id}/result` keyed only by `BacktestId` of a standalone backtest, while OpenAPI documents `GET /api/v1/backtest-results/{resultId}`?**
  → **A: Outcome B: F-009 public-contract/implementation parity bug.** In OpenAPI, the canonical result lookup is `GET /api/v1/backtest-results/{resultId}` (taking `backtestResultId`), while the controller was implemented under `GET /api/v1/backtests/{id}/result` and looks up only standalone backtests. Because Search Candidate backtests have no `StandaloneBacktest` record with a `BacktestId`, candidate results cannot currently be fetched by `backtestResultId`. F-013 adopts the canonical OpenAPI navigation contract `/backtests?resultId={backtestResultId}` (and supports standalone `/backtests?backtestId={backtestId}`), and records an explicit upstream dependency gate (`BLOCKED_BACKTEST_RESULT_READ_BY_RESULT_ID`) until F-009 resolves the parity bug between OpenAPI and `apps/api`. In development/testing, F-011 mock adapters support lookup by both identifiers.

- **Q: Does the currently published F-011 `RealtimeClient` satisfy F-013 requirements for event consumption (`EXPERIMENT_PROGRESS_UPDATED`, `BACKTEST_COMPLETED`, `LEADERBOARD_UPDATED`, `SUBSCRIPTION_CONFIRMED`, `SUBSCRIPTION_ERROR`), close code `4001` handling, and reconnect state tracking without creating a second WebSocket client?**
  → **A: No. F-011's published `RealtimeClient` lacks incoming event listener/dispatch mechanics, status transition callbacks, and close code `4001` silent refresh.** Extending `RealtimeClient` is an **explicit F-011 foundation extension prerequisite**. To preserve the strict architectural boundary where F-011 owns realtime transport and F-013 consumes it, this extension must be implemented within `src/foundation/realtime` (either as an F-011 compatibility amendment or as a scoped foundation prerequisite task prior to F-013 UI implementation). F-013 MUST NOT create an independent WebSocket client.

- **Q: How are WebSocket reconnect retries, retry exhaustion, and manual reconnect bounded between F-011 and F-013?**
  → **A: Option A (Foundation Extension Prerequisite).** In accordance with `websocket-events.md` section 7.3, WebSocket reconnection must use exponential backoff with jitter and a bounded retry cap, transitioning to `disconnected` upon exhaustion to allow manual reconnect. Because F-011's current `realtime-client.ts` retries indefinitely, the F-011 realtime foundation extension prerequisite explicitly includes adding a bounded retry cap and `disconnected` transition on exhaustion. F-013 relies strictly on the F-011 client for transport lifecycle and triggers `connect()` for manual retry without implementing custom retry loops.

- **Q: What are the authoritative sources for the Experiment configuration form selectors (Dataset, Generator, Strategy/Plugin/Version, Parameter Metadata, Top-K)?**
  → **A:**
  - **Dataset**: **UPSTREAM CONTRACT GAP** (F-009 lacks a `GET /api/v1/datasets` collection listing endpoint; only single lookup `GET /api/v1/datasets/{datasetId}` and creation `POST /api/v1/datasets` exist; in development/testing, it is **FIXTURE-ONLY**).
  - **Generator**: **F-010 DEPENDENCY / UPSTREAM DISCOVERY GAP** (ADR-0010 identifies `random-search` as the MVP generator identity within module `search`, but no public discovery endpoint exists in F-009, and OpenAPI examples do not define a contract enumeration. Generator discovery and validation are owned by F-010 Search Coordinator; in development/testing, it is **FIXTURE-ONLY**).
  - **Strategy / Plugin / Version**: **SUPPORTED PUBLIC DISCOVERY** (`GET /api/v1/strategies` system catalog).
  - **Parameter Metadata**: **SUPPORTED PUBLIC DISCOVERY** (`GET /api/v1/strategies` parameter schema rules and constraints).
  - **Top-K Options**: **INTEGER PARAMETER WITH UI PRESETS** (the released public contract accepts `topK` from 1 to 100. F-013 uses 10 as the initial UI selection and offers 10, 25, 50 as convenience presets; these presets are not enumerated backend values).

- **Q: What are the grounded, technology-agnostic success criteria without arbitrary latency numbers?**
  → **A:** Replace arbitrary millisecond/second latency targets with verifiable functional outcomes:
  - SC-001: Completed backtest results load and render without unhandled errors or layout distortion, bounded only by transport latency.
  - SC-002: Realtime progress and leaderboard update events reflect incrementally on the existing UI without full-page reloads, loss of scroll position, or unhandled exceptions.
  - SC-003: 100% of state-changing commands generate a client `Idempotency-Key` and reuse the identical key on retry, ensuring duplicate-safe execution under server idempotency semantics.
  - SC-004: Connection loss triggers automatic reconnect using exponential backoff with jitter, re-acquires authentication tickets via REST, and recovers the authoritative REST snapshot without loss of terminal state.

- **Q: How should implementation choices (hex colors, framework-specific naming, internal classes) be handled in the specification?**
  → **A:** Remove exact hex color codes, internal class names, and framework references from the specification. Refer instead to semantic tokens, the dark quantitative-research terminal design system, and browser client code, deferring component names and styling implementation choices to `docs/ui/design-system.md` and `plan.md`.

---

## Dependencies and Readiness Gates

- **F-011 Web Foundation and Authentication**: Hard dependency. F-013 builds directly on the protected application shell, session lifecycle, and shared HTTP/realtime client boundaries. Production routes owned by F-013 are `/backtests` and `/search`.
  - *Prerequisite Foundation Extension*: F-011's published `RealtimeClient` interface must be extended with incoming event dispatch, connection status listeners, close code `4001` silent refresh, and a bounded retry policy with exhaustion transitioning to `disconnected` for manual retry before F-013 can consume live events in production. F-013 consumes this boundary without creating a separate WebSocket client.
- **F-009 Public API and Realtime**: Hard dependency for released REST snapshots and WebSocket notifications:
  - `GET /api/v1/backtests/{id}/result` / `GET /api/v1/backtest-results/{resultId}` (immutable Result read, 4 metrics, trade history, provenance)
  - `GET /api/v1/experiments/{id}` (authoritative Experiment lifecycle and metadata)
  - `POST /api/v1/experiments/{id}/stop` (idempotent Stop command)
  - `GET /api/v1/jobs/{id}` (durable Job progress and work counts)
  - `GET /api/v1/experiments/{id}/candidates` and `GET /api/v1/experiments/{id}/candidates/{candidateId}` (Candidate inspection)
  - `GET /api/v1/experiments/{id}/leaderboard` (Top-K ranked strategy entries)
  - `GET /api/v1/strategies` (Strategy catalog and parameter schema discovery)
  - WebSocket protocol over `/ws` with ticket authentication, subscription commands (`SUBSCRIBE_EXPERIMENT`, `SUBSCRIBE_LEADERBOARD`), and server events (`EXPERIMENT_PROGRESS_UPDATED`, `BACKTEST_COMPLETED`, `LEADERBOARD_UPDATED`, `SUBSCRIPTION_CONFIRMED`, `SUBSCRIPTION_ERROR`).
  - *Upstream Parity Gate (`BLOCKED_BACKTEST_RESULT_READ_BY_RESULT_ID`)*: OpenAPI documents `GET /api/v1/backtest-results/{resultId}`, but `apps/api` implements `GET /api/v1/backtests/{id}/result` keyed only by `BacktestId`. Candidate results on the Leaderboard only supply `backtestResultId`. Production lookup of candidate backtests by result ID is gated until F-009 resolves this parity gap.
- **F-010 Search Coordinator**: Explicit readiness gate (`BLOCKED_SEARCH_COORDINATOR`). F-010 is not yet released. The backend endpoints `POST /api/v1/experiments` (Start Experiment) and `POST /api/v1/experiments/{id}/reproductions` (Reproduce Experiment) return a stable `503 DEPENDENCY_UNAVAILABLE` with readiness marker `BLOCKED_SEARCH_COORDINATOR`. Production E2E execution of Search and Reproduction is gated behind F-010 availability. F-013 specifies full UI presentation, validation, dependency-unavailable handling, and fixture-driven verification via F-011 test adapters without simulating backend coordinator logic in the browser.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Inspect Immutable Backtest Results and Trade History (Priority: P1)

As a quantitative strategy researcher, I want to inspect the authoritative, immutable results and individual trade records of a completed backtest on the Backtest Results page (`/backtests`) so that I can evaluate trading performance, analyze trade execution details, and verify scientific provenance without risk of altered historical data.

**Why this priority**: Evaluating backtest outcomes is a primary business value of the platform. Backtest results are immutable evidence required for decision-making. If researchers cannot inspect results and verify provenance, downstream search ranking and strategy adoption cannot proceed.

**Independent Test**: Navigate to `/backtests` with a valid backtest identifier (or result identifier). Verify the system presents the four released performance metrics, trade list, execution assumptions, and provenance fingerprints. Verify that entering an invalid or foreign-owned identifier presents a uniform, ownership-safe inaccessible state without disclosing whether the resource belongs to another user.

**Acceptance Scenarios**:

1. **Given** an authenticated user and an owned completed backtest, **When** the user accesses `/backtests?resultId={backtestResultId}` (or `/backtests?backtestId={backtestId}`), **Then** the system renders exactly the four released metrics: Total Return, Win Rate, Maximum Drawdown, and Number of Trades with exact decimal precision, alongside initial capital, final capital, and total fees.
2. **Given** a backtest with recorded trades, **When** the user inspects the trade history table, **Then** trades are presented in chronological sequence displaying sequence number, trade side (`BUY`/`SELL` or `LONG`/`SHORT`), entry timestamp/price, exit timestamp/price, quantity, entry fee, exit fee, total fee, realized profit/loss, post-trade cash, and exit reason.
3. **Given** a backtest result, **When** the user views the provenance panel, **Then** the system displays the immutable fingerprints: Manifest fingerprint, Dataset fingerprint, Strategy fingerprint, and Result fingerprint, as well as the execution assumptions (fee rate, slippage rate, position mode, execution price rule, and rounding mode).
4. **Given** a strategy produced zero trades during backtesting, **When** the result is loaded, **Then** the system displays Total Return as 0%, Win Rate as 0%, Maximum Drawdown as 0%, Number of Trades as 0, an empty trade list message explaining no signals were generated, and does NOT treat this as an error.
5. **Given** a backtest identifier that does not exist or belongs to another user, **When** the user requests the result, **Then** the system displays a single, uniform ownership-safe inaccessible state (`Resource inaccessible`) without distinguishing between non-existence and unauthorized access.
6. **Given** the user navigates to `/backtests` without providing an identifier, **When** the page loads, **Then** the system displays a helpful empty state prompting the user to select an evaluated candidate from the Leaderboard or supply a valid identifier.

---

### User Story 2 - Monitor Search Progress and Authoritative Job Lifecycle (Priority: P1)

As a strategy researcher, I want to monitor the execution progress, candidate generation, and lifecycle states of an ongoing or completed Search experiment on the Search & Leaderboard page (`/search`) so that I can track computational progress, observe discovered alpha scores, and detect failures in real time.

**Why this priority**: Search runs can evaluate tens to hundreds of strategy candidates asynchronously over minutes or hours. Transparent visibility into running jobs, completed work, failed work, and best discovered scores keeps the researcher informed and prevents duplicate work submissions.

**Independent Test**: Open an active experiment on `/search?id={experimentId}`. Observe initial durable state from the REST snapshot, receive realtime `EXPERIMENT_PROGRESS_UPDATED` and `BACKTEST_COMPLETED` events, and verify progress bars, candidate counters (completedWork, failedWork, totalWork), and best score update accurately. Verify that a terminal failure in an asynchronous job displays clear, safe failure information.

**Acceptance Scenarios**:

1. **Given** an active experiment, **When** the user opens the experiment view on `/search`, **Then** the system fetches the authoritative REST snapshot and displays the experiment status (`QUEUED`, `RUNNING`, etc.), dataset identifier, and associated jobs.
2. **Given** an open experiment view with an active realtime connection, **When** the backend emits `EXPERIMENT_PROGRESS_UPDATED`, **Then** the progress indicator, completed work count, failed work count, total work count, and best score update smoothly without requiring a full page refresh.
3. **Given** a backtest finishes within the experiment, **When** a `BACKTEST_COMPLETED` event arrives with a candidate identifier and result identifier, **Then** the system records the discovery and updates the candidate discovery timeline.
4. **Given** an asynchronous job or experiment fails, **When** the terminal status (`FAILED`) is loaded or received, **Then** the system displays the terminal state badge along with the safe failure code and message (e.g., `JOB_EXECUTION_TIMEOUT`), without crashing or rendering generic network errors.
5. **Given** an experiment is in terminal state (`COMPLETED`, `STOPPED`, or `FAILED`), **When** the user views the page, **Then** realtime subscription is deactivated or concluded cleanly, and final durable state is preserved.

---

### User Story 3 - Discover Top-K Strategies on the Leaderboard (Priority: P1)

As a strategy researcher, I want to view a ranked Top-K leaderboard of the best strategy candidates found in an experiment on `/search`, inspect their evaluation scores and maximum drawdowns, and navigate directly to their detailed backtest results so that I can select promising candidates for strategy formulation.

**Why this priority**: The Leaderboard is the core analytical culmination of the Search pipeline. Researchers compare top candidates by score and drawdown to pick the most robust configurations.

**Independent Test**: Load the leaderboard for an experiment containing evaluated candidates. Verify entries display authoritative rank, score, maximum drawdown, and evaluation fingerprint. Change the Top-K limit filter (e.g., Top 10, Top 25, Top 50). Receive a realtime `LEADERBOARD_UPDATED` event and verify the table refreshes via authoritative REST snapshot. Click an entry to navigate to `/backtests` for detailed examination.

**Acceptance Scenarios**:

1. **Given** an experiment with evaluated candidates, **When** the leaderboard loads, **Then** the table displays entries in ascending rank order, showing Rank, Candidate Evaluation ID, Backtest Result ID, Score, Maximum Drawdown, and Evaluation Fingerprint.
2. **Given** the released leaderboard contract does NOT include Total Return, Win Rate, Sharpe Ratio, or Trade Count in the snapshot entry DTO, **When** the table is rendered, **Then** the system displays only the released public fields and provides an explicit "View Backtest" action to inspect full performance on `/backtests`, rather than synthesizing missing metrics in browser client code.
3. **Given** the user changes the Top-K selector, **When** a new limit is selected (via UI presets 10, 25, 50 or custom entry up to 100), **Then** the view requests or displays the corresponding page of entries within the experiment's configured Top-K limit and backend pagination bounds.
4. **Given** an active leaderboard view, **When** a `LEADERBOARD_UPDATED` realtime notification arrives with a revision number strictly greater than the currently displayed revision, **Then** the system triggers an authoritative REST fetch to update the leaderboard entries to the new revision.
5. **Given** a `LEADERBOARD_UPDATED` event arrives with a revision number less than or equal to the current revision, **When** processed, **Then** the system silently discards the stale or duplicate event without triggering redundant REST queries.
6. **Given** a newly started experiment with zero completed evaluations, **When** the leaderboard is viewed, **Then** the system displays a clear empty state: "No strategy candidates evaluated yet. Awaiting evaluation outcomes."
7. **Given** an entry in the leaderboard, **When** the user clicks "View Backtest", **Then** the system navigates to `/backtests?resultId={backtestResultId}`, loading the complete immutable result (or displaying the F-009 parity gate notice if the upstream result-by-ID lookup is blocked).

---

### User Story 4 - Control Experiment Execution and Request Stop (Priority: P2)

As a strategy researcher, I want to stop a running or queued experiment from `/search` so that I can halt unnecessary computation when sufficient candidates have been evaluated or when parameters need adjustment.

**Why this priority**: Search pipelines consume worker resources. Enabling researchers to stop long-running workloads promptly and safely prevents wasted execution and allows immediate iterative refinement.

**Independent Test**: Click "Stop Experiment" on an active experiment. Verify an idempotent stop command is dispatched with an Idempotency-Key header. Verify the UI updates to `STOP_REQUESTED` and subsequently `STOPPED`. Click stop again or simulate a retry to verify idempotent acceptance without conflicting state errors.

**Acceptance Scenarios**:

1. **Given** an experiment in `QUEUED` or `RUNNING` status, **When** the user clicks "Stop Experiment" and confirms the action, **Then** the system issues an authorized stop request with an Idempotency-Key header and transitions the visible state to `STOP_REQUESTED`.
2. **Given** the stop request is accepted (`202 Accepted`), **When** durable state updates to `STOPPED`, **Then** the system reflects `STOPPED` status, disables the stop control, and preserves all candidates and leaderboard revisions collected prior to stopping.
3. **Given** an experiment has already reached a terminal state (`STOPPED`, `COMPLETED`, or `FAILED`), **When** a stop request is issued or replayed, **Then** the system handles the conflict response (`409 INVALID_STATE_TRANSITION`) by refreshing to the latest durable state and informing the user that the experiment has already concluded.
4. **Given** an in-flight stop request experiences a network timeout or connection reset, **When** the user clicks retry, **Then** the system resubmits the request using the SAME logical idempotency key to prevent accidental duplicate operations.

---

### User Story 5 - Configure and Submit Search and Reproduction with Dependency Gate Awareness (Priority: P2)

As a strategy researcher, I want to configure a new Search experiment or request reproduction of an existing experiment from the Search interface, populate form selections from authoritative sources, validate configuration parameters locally, and receive clear dependency-readiness feedback regarding backend status.

**Why this priority**: Experiment creation is the entry point for automated discovery. Validating search space and stop conditions prevents malformed requests, while transparently surfacing option sources and the F-010 readiness gate prevents false assumptions about production readiness.

**Independent Test**: Complete the experiment creation form on `/search` with valid name, dataset, generator, search space parameters, stop conditions (max candidates, max duration), and Top-K limit. Submit the form against the current backend. Verify the system receives `503 DEPENDENCY_UNAVAILABLE` (readiness marker `BLOCKED_SEARCH_COORDINATOR`), retains user input, and displays an informative dependency gate alert explaining that Search Coordinator execution is pending release. Test reproduction of an existing experiment and observe the same dependency gate behavior. In development mock mode, verify the form successfully exercises the F-011 test adapter.

**Acceptance Scenarios**:

1. **Given** the experiment configuration form, **When** the user populates form fields:
   - Strategy: selected from the system catalog discovered via `GET /api/v1/strategies`.
   - Parameter ranges: bounded by the parameter rules and cross-parameter constraints discovered from the selected strategy descriptor.
   - Generator: configured via generator identifier and version string (source: F-010 DEPENDENCY / UPSTREAM DISCOVERY GAP; fixture-configured in mock mode).
   - Dataset: provided via known dataset identifier (or fixture selection in mock mode; source: UPSTREAM CONTRACT GAP).
   - Stop conditions: specified with positive maximum candidates and/or positive maximum duration seconds.
   - Top-K: specified within the released public contract range of 1 to 100; F-013 initially selects 10 and offers 10, 25, 50 as convenience presets rather than backend-enumerated values.
   **Then** the form performs client-side validation ensuring no required fields are blank, numeric bounds conform to discovered parameter rules, and at least one finite stop condition is defined.
2. **Given** invalid form inputs (e.g., negative duration, minimum parameter value exceeding maximum, or missing stop condition), **When** the user attempts submission, **Then** the system prevents request dispatch and displays inline field validation messages explaining the constraint.
3. **Given** valid form inputs submitted in the production environment where F-010 is not yet released, **When** the submission returns `503 DEPENDENCY_UNAVAILABLE` (`Search Coordinator is unavailable / BLOCKED_SEARCH_COORDINATOR`), **Then** the system preserves all form values, does NOT claim successful execution, and displays a prominent, accessible readiness notice informing the user that search orchestration requires the F-010 Search Coordinator capability.
4. **Given** a previously completed experiment, **When** the user initiates "Reproduce Experiment", **Then** the reproduction command is dispatched with an Idempotency-Key; upon receiving the `503 DEPENDENCY_UNAVAILABLE` response, the UI surfaces the same structured readiness gate notice without altering original experiment evidence.
5. **Given** a development or test environment configured with F-011 test adapters, **When** the user submits the form, **Then** the test adapter accepts the command and returns finite predefined responses and emits predefined events for UI verification, without timers, business orchestration, candidate generation, or execution of real backend coordinator code.

---

### User Story 6 - Realtime Connection Resilience, Disconnect Handling, and Snapshot Recovery (Priority: P2)

As an authenticated dashboard user, I want the system to handle WebSocket connection disconnects, token expirations, and reconnection transparently so that momentary network hiccups do not corrupt displayed data or leave the interface stuck in a stale state.

**Why this priority**: WebSocket connections are inherently transient in browser environments. Reliable reconnection with ticket re-acquisition and REST snapshot recovery ensures data integrity without forcing user re-authentication or full page reloads.

**Independent Test**: Establish an active monitoring session on `/search`. Simulate a network disconnection. Verify the connection indicator transitions from connected to reconnecting (displaying a non-intrusive status notice). When reconnect attempts exhaust under the foundation's bounded retry policy, verify the status transitions to disconnected with a manual retry affordance. Restore connectivity; verify the client silently acquires a new ticket via `POST /api/v1/realtime/ticket`, re-establishes the connection, resubscribes to active subscriptions, and executes an authoritative REST snapshot read to reconcile any missed progress or leaderboard revisions.

**Acceptance Scenarios**:

1. **Given** an active realtime connection, **When** the connection is lost or interrupted, **Then** the UI displays a reconnecting status notice, preserves the last known durable snapshot, and marks live freshness as suspended.
2. **Given** a severed connection, **When** the reconnect policy executes, **Then** it applies exponential backoff with jitter and bounded retry attempts, calling `POST /api/v1/realtime/ticket` to obtain a fresh one-time ticket for each new connection attempt.
3. **Given** the connection is closed with code `4001 REAUTHENTICATION_REQUIRED`, **When** the client detects this close code, **Then** it initiates a silent session refresh through the foundation session boundary, acquires a new ticket, and reconnects without requiring the user to re-enter credentials.
4. **Given** a successful reconnection, **When** the connection is re-established, **Then** the client automatically re-sends active `SUBSCRIBE_EXPERIMENT` and `SUBSCRIBE_LEADERBOARD` commands, waits for confirmation markers, and executes an authoritative REST fetch for Experiment, Job, and Leaderboard state to reconcile any missed updates.
5. **Given** all reconnect attempts are exhausted without recovery under the foundation's bounded retry policy, **When** the retry cap is reached, **Then** the foundation client transitions to disconnected, and the UI displays a persistent retry affordance allowing the user to initiate a manual reconnect via the foundation client.
6. **Given** a specific subscription receives a `SUBSCRIPTION_ERROR` (e.g., `EXPERIMENT_NOT_FOUND` or `WORKLOAD_SUBSCRIPTION_LIMIT_EXCEEDED`), **When** the error is received, **Then** the error is isolated to that specific component without closing the entire realtime connection.

---

### Edge Cases

- **Zero Trades in Backtest**: A strategy that never enters a position produces an empty trade list and zero return. This is valid financial evidence; the UI must render 0% metrics, an empty trade table notice, and must not display an error panel.
- **Out-of-Order WebSocket Events**: Realtime progress notifications never overwrite durable Experiment or Job state directly. The client rejects duplicate `eventId` values and mismatched subscription/Experiment targets; a new relevant `EXPERIMENT_PROGRESS_UPDATED` notification triggers an authoritative REST refresh, so an older notification cannot regress the rendered durable snapshot. Leaderboard notifications additionally use the released monotonic `revision`: only a revision greater than the rendered revision triggers a REST refresh.
- **Rapid Consecutive Action Clicks**: If a user repeatedly clicks "Stop Experiment" or "Submit", the UI immediately disables the trigger button and reuses the same generated logical Idempotency-Key for any pending in-flight request, preventing duplicate command dispatch.
- **Missing vs. Foreign-Owned Resource Access**: If a user pastes a URL with an arbitrary experiment or backtest identifier that belongs to another tenant or does not exist, the API returns `404` or `403` with public code `RESOURCE_INACCESSIBLE` or `EXPERIMENT_NOT_FOUND`. The UI displays a uniform "Resource Inaccessible" screen that reveals no information about whether the resource exists or who owns it.
- **Horizontal Overflow on Mobile and Small Viewports**: Tables with numerous columns (Leaderboard and Trade History) must maintain clean horizontal scrolling containers with frozen sticky left columns (e.g., Rank or Sequence) on viewports down to 360px wide, preventing table clipping or viewport distortion.
- **Extreme Number Presentation**: Large financial values or small fractions must preserve the authoritative decimal string in the view model. Presentation may shorten a value only for layout (for example, an ellipsis or display-only precision), but the complete authoritative string must remain available to the user through accessible full text, focus/hover disclosure, or an expanded cell. Presentation formatting must not replace the stored value or recompute business metrics.
- **Job Status vs. Experiment Status Mismatch**: An experiment may be `RUNNING` while individual backtest jobs finish or fail sequentially. The UI clearly differentiates overall Experiment Lifecycle from individual Job Progress without conflating the two.

---

## Requirements *(mandatory)*

### Functional Requirements

#### Backtest Results (`/backtests`)

- **FR-001**: The system MUST render the Backtest Results screen at production route `/backtests`, within the protected application shell provided by F-011.
- **FR-002**: When provided a `resultId` (or `backtestId`) via URL query parameter or route navigation, the system MUST retrieve the immutable Backtest Result via `GET /api/v1/backtest-results/{resultId}` (or `GET /api/v1/backtests/{backtestId}/result`). Because candidate results currently experience an upstream F-009 parity gate (`BLOCKED_BACKTEST_RESULT_READ_BY_RESULT_ID`), the system MUST display the structured dependency notice if candidate result lookup fails upstream.
- **FR-003**: The system MUST display exactly the four released performance metrics:
  1. **Total Return**: exact decimal formatted as a percentage with appropriate sign styling.
  2. **Win Rate**: exact decimal formatted as a percentage.
  3. **Maximum Drawdown**: exact decimal formatted as a percentage.
  4. **Number of Trades**: exact integer count.
- **FR-004**: The system MUST NOT display, compute, or synthesize unreleased metrics such as Profit Factor, Sharpe Ratio, or Sortino Ratio in the production UI unless provided by an authoritative released public API contract.
- **FR-005**: The system MUST display capital summary fields: Initial Capital, Final Capital, and Total Fees, as supplied by the Backtest Result response.
- **FR-006**: The system MUST display the detailed Trade History table displaying all trades returned in the result, including: sequence number, trade side (`BUY`/`SELL` or `LONG`/`SHORT`), entry timestamp (UTC), entry price, exit timestamp (UTC), exit price, quantity, entry fee, exit fee, total fee, realized profit/loss, post-trade cash balance, and exit reason.
- **FR-007**: The system MUST display the scientific Provenance information for the backtest: Experiment Identifier, Candidate Identifier, Job Identifier, Successful Attempt Identifier, Manifest Fingerprint, Dataset Fingerprint, Strategy Fingerprint, and Result Fingerprint.
- **FR-008**: The system MUST display the execution Assumptions: contract version, fee rate, slippage rate, position mode, execution price rule, force close at end flag, and rounding mode.
- **FR-009**: When no identifier parameter is supplied on `/backtests`, the system MUST display an informative empty state guiding the user to select an evaluated candidate from the Leaderboard on `/search` or enter an identifier.
- **FR-010**: When a backtest identifier is invalid, missing, or belongs to another user, the system MUST present a uniform, ownership-safe inaccessible state that does not disclose resource existence or foreign ownership.

#### Search & Leaderboard (`/search`)

- **FR-011**: The system MUST render the Search & Leaderboard screen at production route `/search`, within the protected application shell provided by F-011.
- **FR-012**: The system MUST provide an Experiment Configuration form allowing the user to configure:
  - Experiment Name (required text string)
  - Dataset Selection (selection of available frozen dataset snapshot; source: UPSTREAM CONTRACT GAP / FIXTURE-ONLY)
  - Generator Selection (generator identifier, version string, optional seed integer; source: F-010 DEPENDENCY / UPSTREAM DISCOVERY GAP, fixture-only in development)
  - Strategy Search Space (strategy plugin identifier, strategy version; source: SUPPORTED PUBLIC DISCOVERY via `GET /api/v1/strategies`)
  - Parameter Ranges (per-parameter ranges with minimum, maximum, or option lists bounded by discovered parameter rules from `GET /api/v1/strategies`)
  - Stop Conditions (finite stopping criteria: maximum candidates count and/or maximum duration in seconds)
  - Top-K count (released public contract range: 1–100; F-013 initial UI selection: 10; convenience presets: 10, 25, 50)
- **FR-013**: The system MUST perform client-side validation on the Experiment Configuration form, ensuring parameter minimums do not exceed maximums, stop conditions contain at least one positive finite bound, and all required selections are populated before dispatch.
- **FR-014**: When submitting a new Experiment (`POST /api/v1/experiments`), the system MUST generate and transmit a unique `Idempotency-Key` header with the request.
- **FR-015**: Because F-010 Search Coordinator is an unreleased dependency, when the backend returns `503 DEPENDENCY_UNAVAILABLE` (readiness marker `BLOCKED_SEARCH_COORDINATOR`), the system MUST NOT fail silently or treat the operation as accepted; instead, it MUST display a structured, accessible readiness gate notice informing the user that Search Coordinator orchestration is pending, while retaining user inputs in the form.
- **FR-016**: The system MUST display the authoritative Experiment lifecycle state retrieved via `GET /api/v1/experiments/{id}`, displaying one of the canonical states: `CREATED`, `QUEUED`, `RUNNING`, `STOP_REQUESTED`, `STOPPED`, `COMPLETED`, or `FAILED`.
- **FR-017**: The system MUST display durable Search Job progress retrieved via `GET /api/v1/jobs/{id}`, showing total work count, completed work count, failed work count, and best score.
- **FR-018**: The system MUST provide an idempotent "Stop Experiment" control for experiments in `QUEUED` or `RUNNING` status, issuing `POST /api/v1/experiments/{id}/stop` with an `Idempotency-Key` header.
- **FR-019**: If a stop command receives `409 INVALID_STATE_TRANSITION`, the system MUST refresh the experiment state from the REST API and present the current authoritative state without crashing.
- **FR-020**: The system MUST provide a "Reproduce Experiment" action for completed experiments, issuing `POST /api/v1/experiments/{id}/reproductions` with an `Idempotency-Key` header; in the event of `503 DEPENDENCY_UNAVAILABLE`, it MUST display the same structured dependency gate notification.
- **FR-021**: The system MUST NOT implement candidate generation, backtest simulation, evaluation calculations, ranking calculations, or mock execution loops in browser client code.
- **FR-022**: The system MUST NOT render functional "Pause" or "Resume" controls unless an authoritative F-009/F-010 public contract publishes those operations.
- **FR-023**: The system MUST display the Top-K Leaderboard retrieved via `GET /api/v1/experiments/{id}/leaderboard`, rendering exclusively released fields:
  1. Rank (positive integer)
  2. Evaluation Result ID (typed identifier)
  3. Backtest Result ID (typed identifier)
  4. Score (exact decimal string)
  5. Maximum Drawdown (exact decimal string)
  6. Evaluation Fingerprint (cryptographic hash string)
- **FR-024**: The system MUST NOT compute or fabricate Return, Win Rate, Sharpe Ratio, or Trade Count inside the Leaderboard table to match prototype columns when the released DTO does not supply them.
- **FR-025**: Each row in the Leaderboard table MUST provide a "View Backtest" navigation affordance that transitions the user to `/backtests?resultId={backtestResultId}`.
- **FR-026**: The Leaderboard MUST support paged Top-K viewing with UI convenience presets 10, 25, and 50. Requested page size MUST remain within the released public Leaderboard limit of 1–100 and MUST NOT exceed the Experiment's configured Top-K.

#### Realtime Integration & Resilience

- **FR-027**: The system MUST establish and maintain a single WebSocket connection via F-011's foundation realtime client boundary, obtaining short-lived authentication tickets via authenticated `POST /api/v1/realtime/ticket`. F-013 MUST NOT instantiate an independent WebSocket client.
- **FR-028**: When observing an active experiment, the system MUST dispatch `SUBSCRIBE_EXPERIMENT` with the target `experimentId`, routing matching `EXPERIMENT_PROGRESS_UPDATED` and `BACKTEST_COMPLETED` events through the foundation client's event listener interface. `EXPERIMENT_PROGRESS_UPDATED` is a freshness notification only: after duplicate-`eventId` and target checks, a relevant event MUST trigger an authoritative Experiment/Job REST refresh and MUST NOT directly overwrite durable rendered progress from the event payload.
- **FR-029**: When observing the leaderboard, the system MUST dispatch `SUBSCRIBE_LEADERBOARD` with the target `experimentId`, listening for `LEADERBOARD_UPDATED` events.
- **FR-030**: When `LEADERBOARD_UPDATED` is received, the system MUST compare the event's `revision` against the currently rendered revision:
  - If `event.revision > current.revision`, the system MUST fetch the latest authoritative snapshot via REST (`GET /api/v1/experiments/{id}/leaderboard`).
  - If `event.revision <= current.revision`, the event MUST be discarded as duplicate or stale.
- **FR-031**: When the realtime connection disconnects, the system MUST transition to a visible reconnecting state; the foundation realtime client applies exponential backoff with jitter up to a bounded retry cap, obtaining a new ticket on each attempt, and resubscribes to active subscriptions upon reconnect. If the retry cap is reached, the client transitions to disconnected, enabling the user to manually trigger reconnect.
- **FR-032**: Upon reconnecting or receiving subscription confirmation, the system MUST execute an authoritative REST snapshot read to synchronize state and recover any updates missed during the disconnection.
- **FR-033**: If the server closes the connection with code `4001 REAUTHENTICATION_REQUIRED`, the system MUST perform a silent session refresh through the foundation auth boundary, acquire a new ticket, and reconnect without interrupting user interaction.
- **FR-034**: When a view or component owning an active Experiment or Leaderboard realtime subscription is unmounted, changes its target identifier, or no longer requires live updates, the system MUST issue the corresponding unsubscribe command and clean up local event handlers.

#### UI Presentation, Accessibility & Theme

- **FR-035**: The system MUST adhere to the dark quantitative-research terminal design system established in F-011 and `docs/ui/design-system.md` (high-contrast surfaces, distinct semantic status accents, tabular numerics, and clear typography).
- **FR-036**: All status indicators (Experiment lifecycle, Job progress, connection state, trade side) MUST use semantic text and icons in addition to color; status MUST NEVER be communicated by color alone.
- **FR-037**: All numerical data (prices, returns, drawdowns, scores, sequence numbers, timestamps) MUST use monospaced tabular typography for aligned readability.
- **FR-038**: The UI MUST be fully responsive from 360px mobile viewports to 1440px+ desktop viewports, with data tables providing smooth horizontal scroll containers without distorting the outer application shell.
- **FR-039**: All interactive elements (buttons, inputs, selectors, dialogs) MUST have unique semantic identifiers, accessible ARIA labels, and full keyboard navigation support.
- **FR-040**: The system MUST NOT expose secrets, access tokens, internal Java class names, SQL queries, or raw backend stack traces in any error display or client log.
- **FR-041**: When a protected F-009 request returns `401 AUTHENTICATION_REQUIRED`, F-013 MUST delegate to the F-011 authentication/session lifecycle rather than implement feature-owned token handling. If F-011 cannot establish a valid authenticated session, private client state and active realtime subscriptions MUST be cleared and the user MUST be redirected to the protected login flow. F-013 MUST NOT automatically replay an uncertain state-changing command merely because authentication failed.
- **FR-042**: When a F-009 request returns `429 RATE_LIMIT_EXCEEDED`, the UI MUST preserve any safe authoritative snapshot, present a retryable/rate-limited state, honor a valid server `Retry-After` value before enabling automatic or user-triggered retry, and avoid retry storms. Because the current F-011 normalized `PublicError` does not expose `Retry-After`, a narrow additive F-011 HTTP-boundary extension MUST surface safe retry-delay metadata; F-013 MUST NOT bypass `ApiClient` with raw `fetch` to read that header.

---

### Key Entities

- **Backtest Result**: The immutable, authoritative outcome of executing a strategy configuration over a frozen dataset. Attributes: `backtestResultId`, `backtestId`, `status`, `metrics` (Total Return, Win Rate, Maximum Drawdown, Number of Trades), `initialCapital`, `finalCapital`, `totalFees`, `trades` list, `provenance`, `assumptions`, `completedAt`.
- **Trade**: An individual financial trade executed within a backtest. Attributes: `tradeId`, `sequence`, `side` (`BUY`/`SELL`), `entryTime`, `entryPrice`, `exitTime`, `exitPrice`, `quantity`, `entryFee`, `exitFee`, `totalFee`, `profitLoss`, `postTradeCash`, `exitReason`.
- **Experiment**: An overarching search run exploring strategy parameters across a dataset. Attributes: `experimentId`, `name`, `status` (`CREATED`, `QUEUED`, `RUNNING`, `STOP_REQUESTED`, `STOPPED`, `COMPLETED`, `FAILED`), `datasetId`, `jobIds`, `derivedFromExperimentId`, `reproducesExperimentId`, `startedAt`, `completedAt`, `failure` (code, message), `createdAt`.
- **Search Job**: The durable unit of background work managing candidate evaluation within an experiment. Attributes: `jobId`, `experimentId`, `type`, `status`, `totalWork`, `completedWork`, `failedWork`, `bestScore`, `queuedAt`, `startedAt`, `finishedAt`, `failure`.
- **Candidate Definition**: An individual parameter instantiation generated during an experiment. Attributes: `candidateId`, `experimentId`, `generationIndex`, `definition` (parameter map), `fingerprint`, `createdAt`.
- **Leaderboard Revision**: An immutable snapshot of the Top-K evaluated candidates for an experiment at a specific evaluation milestone. Attributes: `experimentId`, `revisionId`, `revision` (monotonic counter), `topK`, `rankingPolicyVersion`, `fingerprint`, `createdAt`, `items` (list of entries).
- **Leaderboard Entry**: A single ranked item within a leaderboard revision. Attributes: `rank`, `evaluationResultId`, `backtestResultId`, `score`, `maximumDrawdown`, `evaluationFingerprint`.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can load and inspect a completed Backtest Result with all four metrics, provenance, and trade history upon navigation without unhandled exceptions or layout shifts, bounded only by network transport.
- **SC-002**: Realtime progress and leaderboard update events reflect incrementally on the existing UI without full-page reloads, loss of scroll position, or unhandled exceptions.
- **SC-003**: 100% of state-changing mutation commands (Stop Experiment, Start Experiment, Reproduce Experiment) generate a unique client `Idempotency-Key` and reuse the identical key on command retry, ensuring duplicate-safe execution under the server's idempotency semantics.
- **SC-004**: When the realtime connection is interrupted, the client automatically executes the reconnect policy using exponential backoff with jitter, re-acquires authentication tickets via REST, and recovers the latest authoritative REST snapshot upon re-establishing connection without loss of terminal state.
- **SC-005**: 100% of tested unauthorized or non-existent resource requests (`404`/`403`) render the uniform, ownership-safe inaccessible state, leaking zero tenant or technical metadata.
- **SC-006**: When F-010 is unavailable, 100% of experiment creation and reproduction attempts clearly surface the readiness gate notice, preserving user form inputs with zero silent failures or unhandled promise rejections.
- **SC-007**: The entire interface is fully navigable and functional via keyboard across all supported viewports from 360px to 1440px+ without horizontal page overflow.
- **SC-008**: 0% calculation of financial metrics (Total Return, Win Rate, Drawdown) occurs in browser client code, guaranteeing 100% fidelity to backend financial evidence.

---

## Assumptions

1. **F-010 Readiness Gate**: F-010 Search Coordinator is not yet merged or deployed. Start Experiment and Reproduce Experiment endpoints return `503 DEPENDENCY_UNAVAILABLE` in the current backend. The F-013 UI specifies full presentation, input validation, and readiness handling, using F-011 test fixtures/adapters for mock demonstration until F-010 is available.
2. **F-009 Backtest Result Parity Gate**: Due to the parity bug between OpenAPI (`GET /api/v1/backtest-results/{resultId}`) and `apps/api` (`GET /api/v1/backtests/{id}/result`), reading candidate backtest results by `backtestResultId` is an explicit upstream dependency gate (`BLOCKED_BACKTEST_RESULT_READ_BY_RESULT_ID`).
3. **F-011 RealtimeClient Extension Prerequisite**: F-011's published `RealtimeClient` interface requires a scoped extension within `src/foundation/realtime` for event listener dispatch, status change listeners, close code `4001` handling, and bounded reconnect retries transitioning to `disconnected` before production event consumption can function.
4. **Form Option Sources**:
   - Strategy and parameter constraints are discovered dynamically via `GET /api/v1/strategies`.
   - Dataset is an upstream contract gap (no public collection listing; provided via known identifier or fixture selection in mock mode).
   - Generator is an F-010 dependency and upstream discovery gap (ADR-0010 identifies `random-search` for MVP within module `search`, but no public discovery catalog exists in F-009; configured via fixtures in mock mode).
   - Top-K is a released public integer parameter constrained to 1–100. F-013 initially selects 10 and offers 10, 25, 50 as UI convenience presets; these presets are not backend enumeration values.
5. **Backtest Metrics Scope**: In accordance with the F-013 roadmap and released `ResultDtos.MetricsResponse`, exactly four performance metrics are supported: Total Return, Win Rate, Maximum Drawdown, and Number of Trades. Additional prototype metrics (Profit Factor, Sharpe Ratio) are intentionally excluded from production presentation until an authoritative public contract is published.
6. **Leaderboard Columns Scope**: In accordance with the released `LeaderboardDtos.EntryResponse`, the Leaderboard table displays Rank, Candidate/Evaluation ID, Backtest Result ID, Score, Maximum Drawdown, and Evaluation Fingerprint. Additional prototype columns (Candidate Name, Return, Win Rate, Sharpe, Trades) are not present in the leaderboard snapshot DTO and are not synthesized on the client; full metrics are accessed via the "View Backtest" navigation affordance.
7. **Navigation and Route Defaults**: Accessing `/backtests` without an identifier parameter displays an empty state inviting the user to pick an evaluated strategy from `/search` or input an ID.
8. **Session and Transport Infrastructure**: F-013 strictly consumes F-011's foundation HTTP, realtime, session, and design tokens without duplicating singletons or authentication state.

# Feature Specification: Configurable Composite Search and Scalable Backtesting (F-015)

**Feature Branch**: `feature/015-configurable-composite-search`

**Created**: 2026-09-05

**Status**: Draft

**Input**: User description: "Allow a user to choose pair, timeframe, UTC start/end, create or select a frozen dataset, choose a strategy pool and parameter domains, configure composite policy, generator, stop conditions and Top-K, then execute composite candidates through Backtest, Evaluate and Leaderboard with worker-backed scale from 100 to 10,000 candidates."

**Supersedes**: The single-strategy/raw-dataset-ID configuration slice of F-010 and F-013 for new Search experiments. Historical specifications and existing experiment records remain unchanged.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Prepare an Authoritative Frozen Dataset (Priority: P1)

As a researcher, I choose one trading pair, one candle timeframe, and an explicit UTC start and end time, then create a frozen dataset or select a compatible frozen dataset before configuring a Search experiment.

**Why this priority**: Every candidate must be evaluated against the same auditable market-data snapshot. A Search result is not meaningful or reproducible without this input.

**Independent Test**: Select `BTC/USDT`, `1h`, and a valid UTC range, create the dataset, and verify that the returned selection displays the exact pair, timeframe, range, candle count, provider, and checksum and can be reused without downloading market data during candidate backtests.

**Acceptance Scenarios**:

1. **Given** an authenticated user and a valid pair, timeframe, and UTC range, **When** the user creates a dataset, **Then** the platform fetches authoritative historical candles once, freezes the resulting snapshot, and selects it for the draft experiment.
2. **Given** compatible frozen datasets already owned by the user, **When** the user chooses one, **Then** its full provenance is shown and its pair, timeframe, and date range become the authoritative experiment input.
3. **Given** an invalid, future, empty, unsupported, or reversed range, **When** the user attempts creation, **Then** the platform preserves the entered values and explains the validation or provider failure without creating a usable dataset.
4. **Given** a selected frozen dataset, **When** candidates are backtested, **Then** all candidates read the stored snapshot and no candidate independently fetches current candles from the market-data provider.

---

### User Story 2 - Configure and Start a Composite Strategy Search (Priority: P1)

As a researcher, I select the strategies allowed to participate, configure the parameter domain for each selected strategy, choose valid component-count and combination constraints, select an available generator, and set stop conditions and Top-K before starting Search.

**Why this priority**: This is the core correction to the current single-candidate behavior and directly represents the project requirement to search a strategy-combination space.

**Independent Test**: Select at least two strategies with multi-value parameter domains, allow composites of one to two components under Majority Vote, set Random Search, a fixed seed, 100 maximum candidates, and Top-10, then verify that the accepted experiment freezes the complete configuration and produces distinct deterministic composite candidate definitions.

**Acceptance Scenarios**:

1. **Given** a frozen dataset and at least one eligible strategy, **When** the user selects a strategy, **Then** the user can edit only that strategy's published parameter domains and sees validation for invalid ranges, steps, choices, or empty domains.
2. **Given** multiple selected strategies, **When** the user sets minimum and maximum component counts plus allowed combination constraints, **Then** the platform shows whether the resulting search space can produce the requested candidate count.
3. **Given** a valid Search configuration, **When** the user starts Search, **Then** the platform freezes the dataset provenance, strategy versions, parameter domains, combination policy, generator identity/version, random seed, stop conditions, and Top-K in a new immutable experiment.
4. **Given** the same frozen configuration and seed, **When** candidate generation is replayed, **Then** the ordered candidate definitions and fingerprints are identical regardless of candidate completion order.
5. **Given** an invalid or empty composite search space, **When** the user attempts to start, **Then** the request is rejected before any candidate job is allocated and each invalid field or constraint is identified.
6. **Given** a published composite selected as an eligible strategy artifact, **When** it has no parameter domains, **Then** it remains one valid immutable candidate rather than silently becoming the whole Search configuration.

---

### User Story 3 - Execute and Observe a Bounded Multi-Candidate Search (Priority: P1)

As a researcher, I can run hundreds or thousands of candidates through Generate, Backtest, Evaluate, and Rank while observing authoritative progress and safely stopping the run.

**Why this priority**: The assessment explicitly requires a queue/worker path and evidence that Backtest can scale beyond one candidate.

**Independent Test**: Run a finite 100-candidate experiment with enough search-space cardinality and an in-flight limit of four; verify that exactly 100 unique candidates reach terminal outcomes, the active count never exceeds four, completed slots are refilled until a stop condition is met, and Top-K remains ten.

**Acceptance Scenarios**:

1. **Given** a run with work remaining and an available execution slot, **When** a candidate completes or fails terminally, **Then** the coordinator allocates replacement work within a bounded interval until a stop condition, cancellation, or search-space exhaustion applies.
2. **Given** `Top-K = 10` and an in-flight limit of four, **When** 100 candidates are requested, **Then** the platform may process all 100 candidates while retaining only the best ten leaderboard entries; Top-K does not cap concurrency or total generated candidates.
3. **Given** duplicate, stale, or out-of-order queue deliveries, **When** workers process them, **Then** one logical candidate has at most one accepted business outcome and progress is not double-counted.
4. **Given** a worker or coordinator restart, **When** processing resumes, **Then** incomplete durable work is reclaimed or reconciled and the run continues without generating a different candidate sequence.
5. **Given** a user requests stop, a deadline expires, the maximum candidate count is reached, no-improvement termination is reached, or the finite search space is exhausted, **When** the authoritative rule wins, **Then** no new candidate is allocated and the run reaches the corresponding terminal status after in-flight outcomes are reconciled.
6. **Given** a running experiment, **When** the user views its Search screen, **Then** the screen distinguishes allocated, active, completed, failed, remaining-capacity, best-score, and elapsed-time values using authoritative data and clearly indicates stale or reconnecting realtime state.

---

### User Story 4 - Inspect and Reproduce Composite Leaderboard Results (Priority: P2)

As a researcher, I inspect the Top-K leaderboard and can understand which composite candidate, parameters, dataset, and metrics produced each ranking, open its Backtest result, and reproduce a terminal experiment.

**Why this priority**: A ranked score without candidate composition and provenance cannot be defended in the project evaluation or reproduced later.

**Independent Test**: Complete a composite Search, open a leaderboard entry, and verify that its rank, score, candidate summary, four released metrics, Backtest identity, strategy versions/parameters, combination policy, and dataset provenance all trace to durable records; then reproduce the experiment and compare candidate fingerprints.

**Acceptance Scenarios**:

1. **Given** evaluated candidates, **When** the leaderboard is read, **Then** it contains at most the configured Top-K entries in deterministic order and exposes only authoritative released metrics.
2. **Given** a composite leaderboard row, **When** the user inspects it, **Then** the component strategy versions, parameters, combination policy, candidate fingerprint, and Backtest identity are available without browser-side financial calculation.
3. **Given** a terminal experiment, **When** the user requests reproduction, **Then** the platform reuses the exact frozen dataset and ordered candidate definitions and reports whether the reproduced evidence matches.

### Edge Cases

- A requested market-data range is valid syntactically but contains no closed candles.
- The provider returns gaps, duplicates, partial pages, rate limiting, or a transient failure while creating a dataset.
- Two dataset-creation requests with the same idempotency identity are delivered more than once.
- A selected strategy is unpublished, no longer owned by the user, incompatible with the dataset timeframe, or changes after the draft was opened.
- Parameter domains are individually valid but their Cartesian/composite space is smaller than `maxCandidates`.
- Minimum components exceeds maximum components, either value exceeds the selected pool size, or constraints rule out every combination.
- A generator is displayed in an old draft but is no longer present in the authoritative generator registry.
- Candidate generation repeatedly proposes duplicates or cannot make progress.
- `maxCandidates` is smaller than Top-K, Top-K is smaller than the in-flight limit, or a no-improvement threshold cannot be reached before another stop condition.
- Stop, completion, deadline, retry, and replacement allocation race with one another.
- Some candidates fail while enough unexplored space remains; failures count as terminal outcomes but do not create duplicate replacements for the same generation index.
- Realtime delivery disconnects while durable progress continues.
- A historical single-strategy experiment created before F-015 is opened after the new contract is released.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST let an authenticated user specify exactly one trading pair, one supported timeframe, and explicit `start UTC` and `end UTC` values when creating a frozen dataset.
- **FR-002**: Dataset time ranges MUST use half-open semantics `[start, end)`, require `start < end`, reject future/unsupported ranges, and make the semantics visible to the user.
- **FR-003**: Dataset creation MUST obtain authoritative historical candles through the existing market-data capability, validate the completed range, and persist an immutable snapshot with provider, pair, timeframe, range, candle count, checksum, and creation time.
- **FR-004**: The system MUST allow users to list and select frozen datasets they are authorized to access, displaying enough provenance to distinguish similarly named or overlapping snapshots.
- **FR-005**: Every candidate in one experiment MUST use the same selected frozen dataset, and candidate Backtest execution MUST NOT download live or current market data.
- **FR-006**: The Search configuration MUST let users select one or more eligible published strategy artifacts as the allowed strategy pool.
- **FR-007**: For each selected parameterized strategy, the user MUST be able to configure typed parameter domains within that strategy version's published parameter schema.
- **FR-008**: Parameter domains MUST support the applicable finite forms already modeled by the platform—explicit choices and bounded numeric ranges with valid steps—and MUST be normalized before cardinality and fingerprinting.
- **FR-009**: The Search configuration MUST include minimum and maximum component counts and an explicit combination policy; the initial required executable policy is Majority Vote.
- **FR-010**: Weighted Vote MUST NOT be offered as executable until weight domains, normalization, validation, candidate persistence, and runtime execution are supported end to end.
- **FR-011**: The system MUST reject duplicate component identities, invalid component counts, incompatible artifacts, empty domains, impossible constraints, and a zero-cardinality search space before accepting Start Search.
- **FR-012**: The platform MUST expose only generators available in the authoritative generator registry; Random Search with a versioned identity is mandatory for F-015.
- **FR-013**: The Search configuration MUST accept a reproducible random seed, maximum-candidate limit, optional maximum duration, optional no-improvement candidate threshold, and Top-K target.
- **FR-014**: Top-K MUST control leaderboard retention only and MUST NOT limit the in-flight execution window or the total candidate budget.
- **FR-015**: Starting Search MUST atomically create an immutable experiment manifest and durable Search run that freeze dataset provenance, selected strategy versions, parameter domains, combination rules, generator identity/version/state, seed, stop conditions, and Top-K.
- **FR-016**: Each generated candidate definition MUST immutably identify its selected component strategy versions, parameter values, combination policy/configuration, generation index, and deterministic fingerprint.
- **FR-017**: With the same frozen manifest, generator version, and seed, candidate generation MUST produce the same ordered definitions independently of worker completion order.
- **FR-018**: The coordinator MUST allocate candidates through a bounded sliding window and MUST refill available slots while the run is active, capacity is available, unexplored candidates remain, and no stop condition has won.
- **FR-019**: The system MUST enforce both a global in-flight bound and a per-experiment in-flight bound without enqueueing the entire requested candidate budget at once.
- **FR-020**: Candidate processing MUST follow the durable `Candidate composite → Backtest → Evaluate → Rank` lifecycle and support multiple worker instances without requiring a change to candidate meaning or ranking semantics.
- **FR-021**: Duplicate, stale, retried, and out-of-order work deliveries MUST be idempotent and MUST NOT create duplicate candidates, results, evaluations, rankings, or progress increments.
- **FR-022**: Recovery and reconciliation MUST refill stranded active runs, repair missing dispatch intent, reclaim retryable work, and preserve the original candidate sequence after process restarts.
- **FR-023**: Maximum candidates, maximum duration, no-improvement, explicit stop, finite-space exhaustion, and terminal failure MUST have deterministic precedence and MUST prevent new allocations once their authoritative terminal decision applies.
- **FR-024**: A run whose configured search space contains fewer unique candidates than its maximum-candidate budget MUST terminate as exhausted after every unique valid candidate has reached a terminal outcome.
- **FR-025**: Search progress reads MUST expose authoritative allocated, active, completed, failed, remaining-capacity, configured maximum, elapsed time, current lifecycle, best score, and terminal reason values.
- **FR-026**: The browser MUST navigate from a successful Start Search command to the accepted experiment's monitor/leaderboard route and MUST recover from durable snapshots when realtime delivery is stale or disconnected.
- **FR-027**: Leaderboard reads MUST expose deterministic rank, score, Candidate and Backtest identities, composite summary, Total Return, Win Rate, Maximum Drawdown, Number of Trades, and candidate fingerprint when those values exist in the authoritative result.
- **FR-028**: Candidate detail MUST expose exact dataset provenance, component strategy versions and parameters, combination policy/configuration, generator provenance, and evaluation metric version without recomputing business results in the browser.
- **FR-029**: Existing historical single-strategy experiments and public clients MUST remain readable through an explicit backward-compatible contract or version transition.
- **FR-030**: Production and fixture/demo data MUST be visually distinguishable; fixture adapters MUST cover the same public requests and state transitions used by the Search screen without simulating Search, Backtest, Evaluation, or Ranking logic in the browser.
- **FR-031**: Verification MUST include deterministic generation, cardinality, validation, idempotency, refill, stop-race, restart recovery, bounded concurrency, public contract, browser interaction, and end-to-end tests.
- **FR-032**: Scale evidence MUST include a full 100-candidate integration run, a 1,000-candidate concurrency/performance run, and a controlled 10,000-candidate allocation/backpressure run with recorded environment, configuration, counts, timing, and failure results.
- **FR-033**: Before Start Search, the user MUST be able to configure positive simulated initial capital, a non-negative transaction fee rate, and a non-negative slippage rate. These assumptions MUST be validated, frozen once per Experiment, and applied equally to every Candidate Backtest; omitted values retain the released defaults for backward compatibility.

### Key Entities

- **Frozen Dataset**: An immutable, owner-scoped snapshot of closed candles for one pair, one timeframe, and one half-open UTC range, identified by provenance and checksum.
- **Strategy Pool Entry**: One immutable eligible strategy artifact/version and its configurable typed parameter domains.
- **Composite Search Space**: The strategy pool, component-count bounds, combination policy, constraints, and normalized parameter domains from which finite candidate definitions are generated.
- **Search Generator Descriptor**: The stable identity and version of a generator available for selection, including the configuration it accepts.
- **Experiment Manifest**: The immutable record that freezes all inputs required to explain and reproduce a Search run.
- **Search Run**: Durable generator and coordination state, candidate budget, in-flight policy, progress counters, stop state, and terminal reason for one experiment.
- **Composite Candidate Definition**: An immutable ordered set of strategy component versions and parameters plus combination policy, generation index, and fingerprint.
- **Candidate Execution**: The durable Backtest, Evaluation, and Ranking work and outcomes associated with one candidate definition.
- **Leaderboard Entry**: A deterministic Top-K projection linking rank and score to authoritative candidate, Backtest, metrics, and provenance.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: In a usability validation, a user can create or select a frozen dataset with explicit pair, timeframe, start, and end, configure a valid composite Search, and reach its monitoring screen without entering internal identifiers manually.
- **SC-002**: A valid 100-candidate run with sufficient unique search space records exactly 100 unique terminal candidate definitions, never exceeds its configured active-work limit, and retains exactly the configured Top-K or fewer when fewer candidates evaluate successfully.
- **SC-003**: Replaying the same frozen configuration and seed produces a 100% match in ordered candidate fingerprints, while changing any frozen strategy version, domain, policy, dataset checksum, generator version, or seed changes the manifest identity.
- **SC-004**: Duplicate delivery and restart validation produces zero duplicate accepted candidate results, evaluations, leaderboard entries, or double-counted progress.
- **SC-005**: After a terminal candidate frees a slot in an active run, replacement work becomes durably allocated within five seconds in the supported deployment profile, until a stop condition or search-space exhaustion applies.
- **SC-006**: A 1,000-candidate validation demonstrates bounded parallel execution and reports throughput improvement when worker capacity is increased, without changing candidate fingerprints or ranked outcomes.
- **SC-007**: A controlled 10,000-candidate validation completes allocation and queue/backpressure checks without materializing or enqueueing all candidates at once and without exceeding configured global or per-experiment active-work bounds.
- **SC-008**: Every displayed leaderboard row can be traced to one immutable dataset checksum, exact component versions and parameters, one candidate fingerprint, one authoritative Backtest result, and one metric version.
- **SC-009**: All Search configuration, progress, stop, leaderboard, candidate-detail, and reproduction states remain usable from 360 px through 1440 px widths and communicate loading, empty, invalid, retryable, disconnected, stopped, failed, and completed states without relying on color alone.

## Assumptions

- One Search experiment uses exactly one pair, one timeframe, and one frozen dataset. Multi-pair and multi-timeframe candidates are outside F-015.
- The requested dataset range belongs to data acquisition; every Backtest reads the resulting frozen snapshot rather than querying Binance or another provider itself.
- Selected strategies form the allowed pool. A generated candidate may use any unique subset whose size satisfies the configured component bounds and constraints.
- Majority Vote is the first required composite policy. Weighted Vote remains hidden/disabled until it has a complete executable contract.
- Random Search is the required generator. Other generators are displayed only when a registered, versioned implementation actually exists.
- Finite-space cardinality may be shown as an exact number or a safe lower/upper estimate when exact calculation would be impractical, but zero-space and obvious budget-over-space cases must be identified before Start.
- Failed candidate executions count toward terminal progress and the maximum-candidate budget; retry attempts do not create new candidate definitions.
- The four currently released Backtest metrics are Total Return, Win Rate, Maximum Drawdown, and Number of Trades. F-015 does not invent Sharpe Ratio or other metrics in the browser.
- Existing authentication, ownership-safe not-found behavior, idempotency, REST/realtime recovery, immutable-result rules, and forward-only database migration policy remain in force.
- Initial capital is simulated quote-asset capital, not a connected exchange wallet balance. Fee and slippage are entered as percentages in the browser and transported as exact decimal-rate strings.
- The shared Search & Leaderboard screenshot and prototype guide information hierarchy only; released contracts and this specification govern behavior.

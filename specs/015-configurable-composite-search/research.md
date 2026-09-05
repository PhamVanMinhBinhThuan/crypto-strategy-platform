# Research: F-015 Configurable Composite Search and Scalable Backtesting

## Decision 1: Dataset creation and Backtest data source

**Decision**: Creating a frozen dataset is the only stage that may fetch historical candles through the market-data provider. Every candidate Backtest reads the immutable stored snapshot identified by dataset ID and checksum.

**Rationale**: It makes every candidate comparable and preserves reproduction if Binance later changes or corrects history. The existing market-data capability already accepts pair, timeframe, start, and end and owns provider access.

**Alternatives considered**:

- Fetch Binance data per candidate: rejected because candidates could observe different data and multiply provider load.
- Use seeded database rows in production: rejected; seeds remain explicit fixture/demo inputs only.
- Let a Search span multiple timeframes: rejected for F-015 because dataset and Backtest contracts currently model one timeframe per experiment.

## Decision 2: Strategy pool semantics

**Decision**: Selected strategies form an allowed pool. Each generated candidate chooses a unique subset between `minComponents` and `maxComponents`, then chooses one value for every parameter domain of each selected component. Component order is canonical by immutable strategy identity/version.

**Rationale**: This matches the requested “strategy pool + parameter domains” model and prevents permutations of the same logical composite from becoming duplicate candidates.

**Alternatives considered**:

- One selected strategy per whole Search: current behavior and explicitly insufficient.
- Always include every selected strategy: prevents searching combinations and makes pool selection misleading.
- Treat a pre-published composite as a mutable template: rejected because published artifacts are immutable; it may participate as one pool entry/candidate instead.

## Decision 3: Combination policy scope

**Decision**: Majority Vote is the only required executable combination policy in F-015. Weighted Vote remains unavailable until its weight domains, normalization, validation, persistence, fingerprinting, and runtime execution exist end to end.

**Rationale**: The current platform has a combination capability, but exposing an unimplemented policy would recreate prototype-only behavior and invalidate candidate reproducibility.

**Alternatives considered**:

- Display Weighted Vote disabled: acceptable only with an explicit “not available” explanation, but hiding it is less confusing.
- Infer equal weights automatically: rejected because that is Majority Vote under a misleading name.

## Decision 4: Versioned search-space and candidate representation

**Decision**: Introduce a v2 canonical representation that contains pool entries, typed domains, component bounds, constraints, and policy. Candidate definitions contain exact component versions/parameters and policy configuration. Retain v1 deserialization for historical single-strategy records and queue messages.

**Rationale**: ADR-0010 already requires composite identity in candidates, while current `GeneratedCandidate` stores only one parameter set. An explicit version avoids ambiguous JSON evolution.

**Alternatives considered**:

- Overload flat parameter keys such as `strategyA.period`: rejected because it loses typed component identity and makes canonical ordering fragile.
- Rewrite old manifests: rejected by immutable evidence and forward-only migration rules.

## Decision 5: Random generation over finite space

**Decision**: Define a deterministic logical candidate index space from canonical component subsets and finite parameter combinations. Random Search traverses that finite space in a seeded deterministic permutation without materializing every candidate.

**Rationale**: It yields unique candidates, exact cardinality, bounded memory, restartable generator state, and identical order for the same seed. It also makes 10,000-candidate Search practical.

**Alternatives considered**:

- Rejection-sample random candidates with a duplicate set: degrades badly near exhaustion and grows memory with candidate count.
- Enumerate and shuffle the entire space: simple but violates bounded-memory goals for large domains.
- Depend on worker completion feedback for Random Search order: rejected because completion order is nondeterministic.

## Decision 6: Refill trigger and concurrency semantics

**Decision**: A durable coordination decision may request `FILL_AVAILABLE_SLOTS`; the Worker coordinator must execute allocation immediately after that decision and the reconciler must do the same for recovered runs. `targetWindow = min(requestedConcurrency, perExperimentLimit, globalAvailableCapacity)` and is independent of Top-K.

**Rationale**: Current code calculates an initial window but completion/reconciliation paths ignore the refill decision, so work can stop after the first window. Top-K is a leaderboard retention value, not an execution limit.

**Alternatives considered**:

- Periodic refill only: safe fallback but adds avoidable latency and does not satisfy prompt replacement.
- Enqueue all candidates at Start: rejected due to backpressure, stop responsiveness, and 10,000-candidate memory/queue growth.
- Use Redis locks for capacity: rejected; durable database version/fencing remains the correctness boundary.

## Decision 7: Candidate failures and stop accounting

**Decision**: A candidate definition consumes one generation index and one maximum-candidate budget unit. Its terminal failure increases failed/terminal progress; transport/execution retries remain attempts of the same candidate. Replacement allocation explores the next candidate only while another stop rule has not won.

**Rationale**: This prevents unbounded generation when a strategy always fails and keeps progress/reproduction stable.

**Alternatives considered**:

- Generate until `maxCandidates` successes: rejected because failure-heavy searches could run without a predictable budget.
- Reuse the same generation index after terminal failure: rejected because it changes candidate order and evidence.

## Decision 8: Public reads and UI authority

**Decision**: Add authoritative dataset-list, generator-list, Search-progress, candidate-detail, and enriched leaderboard representations at the existing API boundary. The browser validates for usability but does not create candidate definitions or calculate metrics.

**Rationale**: The shared UI policy forbids prototype business simulation, while the current leaderboard contract is too sparse to explain composite rows.

**Alternatives considered**:

- Join several unrelated endpoints in the browser and infer composition: rejected due to inconsistent snapshots and leaked orchestration.
- Render prototype Sharpe Ratio: rejected because it is not a released metric.

## Decision 9: Scale verification profile

**Decision**: Use three evidence layers: 100 candidates through the real durable pipeline, 1,000 candidates for comparative worker-concurrency throughput, and 10,000 candidates for bounded generator/allocation/queue backpressure. Each evidence file records hardware/runtime configuration and exact assertions.

**Rationale**: A single-process microbenchmark of 12 candidates is insufficient to support the rubric claim, while forcing 10,000 expensive financial simulations into every CI run would be unstable and wasteful.

**Alternatives considered**:

- One 10,000-candidate end-to-end CI test: rejected due to cost and flakiness.
- Unit tests only: rejected because they do not prove queue/worker integration.


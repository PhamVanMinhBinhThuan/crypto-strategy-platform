# Research: F-016 Sentiment Strategy

## Decision 1 — Feed frozen observations through StrategyContext

**Decision**: Extend `StrategyContext` with an immutable, generic supplemental-input collection and keep
the existing four-argument constructor for technical strategies.

**Rationale**: `Strategy.evaluate` remains pure and deterministic. Backtesting can inject the same
frozen data into Single and Composite strategies without depending on News implementation types.

**Rejected alternatives**: Calling Python/database from Strategy is non-deterministic; a global cache
hides provenance; adding News types to Backtesting creates a forbidden dependency.

## Decision 2 — News owns snapshot construction

**Decision**: News exposes a public use case that creates/loads an immutable sentiment snapshot from
News-owned records. Runtime composition maps it to Strategy Core's neutral frozen-input contract.

**Rationale**: News remains the only owner selecting persisted News/Sentiment records. Strategy Core
only defines what a deterministic evaluator may consume.

## Decision 3 — Freeze one release and ordered evidence before acceptance

**Decision**: A run freezes snapshot ID/version, asset, inclusive publication cutoff,
model/preprocessing release, ordered observations and SHA-256 fingerprint before jobs are created.
Search candidates all share that snapshot.

**Rationale**: Active model changes or later News cannot alter an accepted run. Failed snapshot
preflight creates no partial Backtest/Result.

## Decision 4 — Confidence-weighted polarity

**Decision**: Calculate `sum(polarity × confidence) / sum(confidence)` with `BigDecimal`, scale 10 and
`HALF_EVEN`. The aggregation contract is `sentiment-weighted-polarity-v1`.

**Rationale**: Exact versioned rounding is reproducible and preserves available confidence information.

## Decision 5 — HOLD is explicit missing-evidence behavior

**Decision**: Fewer observations than `minimumArticles`, or zero total confidence, returns HOLD with a
stable reason code. No synthetic NEUTRAL observation is created.

## Decision 6 — Reuse existing plugin, Composite and Search contracts

**Decision**: Register `sentiment-polarity` in `StrategyPlugins.trusted()` and publish the same typed
parameter schema used by technical plugins.

**Rationale**: Composer, Search and Composite remain generic and contain no per-strategy signal logic.

## Decision 7 — Additive UI/API integration only

**Decision**: Extend catalog/evidence responses additively, link News to the authoritative Composer
selection, and display snapshot/model/article evidence returned by the API.

## Decision 8 — Preserve failure isolation

**Decision**: Technical-only flows never invoke snapshot creation. Frozen snapshots remain replayable
while the live model is unavailable; new Sentiment runs fail during preflight with a safe error.

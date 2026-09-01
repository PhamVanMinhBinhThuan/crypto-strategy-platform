# ADR-0013: Backtest Execution and Cross-Capability Integration

**Status**: Proposed
**Date**: 2026-08-31
**Owners**: Văn Minh, Tiến Luật
**Extends**: ADR-0005
**Supersedes (scoped)**: ADR-0002 dependency rows for `backtesting`, `evaluation` and `leaderboard`; ADR-0009 MVP execution-price default

## Context

F-006 must execute a frozen Strategy against an immutable Dataset, persist one successful business result for a Candidate, evaluate it and project a deterministic Leaderboard. The implemented public contracts live in separate capability modules:

- F-003 exposes `DatasetCandleReader`, `CandleBatch`, `DatasetSnapshot` and dataset verification;
- F-004 exposes `StrategyRegistry`, `Strategy`, `StrategyContext`, typed parameters and Strategy provenance;
- F-005 exposes typed Experiment, Candidate, Job and Execution Attempt identities.

ADR-0002 currently permits `backtesting` to depend only on `domain` and `strategy-core`. That matrix cannot express the required typed integration without copying identifiers or contracts. Copying them would violate module ownership and make provenance ambiguous.

ADR-0009 also lists close of the signal Candle as the default execution price. F-006 selected the next Candle open so a decision produced only after a Candle closes cannot use a price from the already completed Candle. This avoids look-ahead bias but changes the earlier default.

Two implemented Strategy contracts are insufficient for deterministic streaming execution:

- `StrategyDescriptor.requiredLookback` is static while parameters such as `slowPeriod` can require a larger window;
- Composite execution exists behind an internal class and has no public factory that Backtesting may call.

## Drivers and Quality Scenarios

- Backtesting must reuse F-003/F-004/F-005 typed public contracts without importing another module's `internal` package.
- A signal based on closed Candle N must execute no earlier than the open of Candle N+1.
- A parameterized Strategy must receive enough historical Candles while memory remains bounded by batch plus declared lookback.
- Single and Composite Strategy provenance must materialize through the owner capability and produce deterministic decisions.
- Adding or replacing Strategy implementations must not require changes to Backtester, Evaluator, Leaderboard or UI.
- Architecture tests must reject new dependency edges not listed by the accepted module matrix.

## Decision

### Next-open execution

F-006 uses long-only, one-position execution. A decision evaluated at the close of Candle N is pending until Candle N+1 and executes at its open after adverse slippage. BUY while a position is open and SELL while no position is open are ignored. A remaining position is force-closed at the final Candle close according to the frozen assumptions.

This section supersedes only the MVP execution-price default in ADR-0009. All other reproducibility decisions in ADR-0009 remain unchanged. Existing experiments retain their recorded execution-price assumption and are not migrated silently; only new F-006 assumptions use `next-candle-open` by default.

### Explicit public dependency edges

The accepted dependency rows from ADR-0002 are superseded only for the following modules:

| Module | Additional public dependencies |
| --- | --- |
| `backtesting` | `market-data`, `strategy-core`, `combination`, `experiment` |
| `evaluation` | `backtesting`, `experiment` |
| `leaderboard` | `evaluation`, `experiment` |

These edges permit only published `api`, `port/in` and `port/out` types. No capability may import another module's internal implementation or persistence adapter. Persistence continues to depend on owner output ports; capability modules never depend on persistence.

ADR-0002, the Architecture Module View, Gradle declarations and `ModuleBoundaryTest` must be updated together before dependent implementation is merged. No runtime data migration is required for these build-time dependency changes.

### Parameter-aware lookback

Strategy ownership remains in F-004. The public Strategy registry/materialization boundary must resolve a required lookback from the exact frozen `StrategyReference` and `StrategyParameterSet`. A plugin may compute lookback from validated parameters; the descriptor value remains the minimum/default capability declaration.

Backtesting validates that the resolved parameters equal the frozen parameters and keeps only the resolved rolling window plus current batch/execution state. It must not guess lookback from parameter names or inspect a concrete Strategy implementation.

### Public Composite materialization

The `combination` capability publishes a factory/materializer returning the existing public `Strategy` abstraction for a frozen combination policy version and ordered component snapshots. It delegates component creation to `StrategyRegistry` and combination to the selected public `CombinationPolicy`.

Backtesting calls this boundary and does not reconstruct Composite semantics or import the internal `CompositeStrategy` class.

## Alternatives Considered

- **Execute at signal Candle close**: matches the old default but can introduce look-ahead bias because the decision is only known after that close.
- **Copy Dataset/Experiment DTOs into Backtesting**: avoids Gradle edges but creates duplicate identity and provenance contracts.
- **Put orchestration in Worker**: avoids a Backtesting dependency but moves business execution policy into a runtime and F-007 scope.
- **Keep every Candle in memory**: avoids dynamic lookback work but violates the bounded-memory driver for large Datasets.
- **Let Backtesting construct Composite internals**: creates cross-module internal coupling and duplicates ownership.

## Consequences

### Positive

- Execution timing is explicit and testable without look-ahead bias.
- Module dependencies represent real typed collaboration rather than copied DTOs.
- Parameterized strategies receive sufficient history with bounded memory.
- Composite semantics stay owned by the combination capability.

### Negative

- F-004 gains a small public contract extension for lookback and Composite materialization.
- ADR-0002 documentation and architecture tests must change atomically before dependent implementation is merged.
- Existing experiments using the earlier close-price default need their original assumptions/version preserved; they must not silently adopt `next-open`.

## Validation Plan

- Contract-test next-open execution and prove no signal uses a future Candle.
- Test MA with parameterized lookback larger than the current static descriptor value.
- Materialize Single and Composite provenance through public boundaries and compare decisions with the owner capability.
- Run ArchUnit and Gradle dependency-matrix tests for every new edge and forbidden internal access.
- Reproduce the same frozen run with different Candle batch sizes and verify identical Trades, metrics and fingerprints.

## Evidence

Planned — source contracts and gaps were reviewed during F-006 planning; implementation and automated evidence do not yet exist.

## Risks and Mitigations

- **Risk**: A broad dependency edge becomes permission to access internal code.
  **Mitigation**: Restrict imports to public packages and enforce them with ArchUnit.
- **Risk**: Changing the execution default changes historical meaning.
  **Mitigation**: Store the execution rule in versioned assumptions; old manifests retain the old version.
- **Risk**: A plugin reports too little lookback.
  **Mitigation**: Parameter-aware contract tests and fail-fast validation before accepting a Result.
- **Risk**: Composite order changes output.
  **Mitigation**: Preserve ordered component snapshots and include them in canonical fingerprints.

## References

- [F-006 specification](../../specs/006-backtest-evaluation-leaderboard/spec.md)
- [ADR-0002: Module Boundaries](0002-module-boundaries.md)
- [ADR-0005: Strategy Plugin Registry](0005-strategy-plugin-registry.md)
- [ADR-0009: Reproducible Experiments](0009-reproducible-experiments.md)

## Supersession

- Supersedes: ADR-0002 only for the `backtesting`, `evaluation` and `leaderboard` dependency rows; ADR-0009 only for the MVP execution-price default
- Superseded by: None

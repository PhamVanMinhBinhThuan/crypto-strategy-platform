# Research: F-006 Backtest, Evaluation and Leaderboard

## Decision 1: Dataset streaming and integrity

**Decision**: Reuse `GetDatasetUseCase`, `VerifyDatasetUseCase`, `DatasetCandleReader` and `CandleBatch`. Verify the frozen snapshot before execution and validate returned dataset identity, contiguous progress, `hasMore` behavior, membership/order and final count during the simulation pass.

**Rationale**: F-003 already owns normalization and checksum. Runtime guards prevent a bad/custom reader from producing a partial accepted Result while memory remains bounded.

**Alternatives considered**: Define a Backtesting reader copy (rejected: duplicate contract); load all Candles (rejected: memory); trust `CandleBatch` constructor alone (rejected: it does not validate requested dataset identity or whole-stream completion).

## Decision 2: Signal timing and look-ahead prevention

**Decision**: Build each `StrategyContext` only from closed Candles through the current Candle, evaluate at that Candle close, and defer the decision to the next Candle open. A final pending decision without a next Candle is not executed; an already open position is force-closed at the final close.

**Rationale**: The current Strategy contract requires evaluation time equal to the last closed Candle. Next-open execution avoids using an unavailable earlier price.

**Alternatives considered**: Execute at signal close (rejected by F-006 clarification and look-ahead risk); execute on current open (future information); introduce tick simulation (outside MVP).

## Decision 3: Position sizing, cost and decimal policy

**Decision**: Use 100% available cash for one long position. BUY fill is next open × `(1 + slippage)` and SELL fill is next open × `(1 - slippage)`; each side charges notional × fee rate. Trade values use scale 12, metrics use scale 10 and `HALF_EVEN`; intermediate values retain maximum practical precision.

**Rationale**: This is deterministic, matches existing database precision and is simple enough for MVP verification.

**Alternatives considered**: Fixed order size (adds sizing config); fee only on exit/no slippage (unrealistic); database-only rounding (runtime and persisted fingerprints can diverge).

## Decision 4: Parameter-aware Strategy materialization

**Decision**: Extend the F-004 public registry/plugin contract to resolve required lookback from the exact frozen parameters. Extend `combination` with a public factory that creates the existing `Strategy` abstraction for an ordered Composite snapshot.

**Rationale**: Static descriptor lookback is already inconsistent with dynamic MA parameters, and importing `CompositeStrategy` internal code is forbidden.

**Alternatives considered**: Infer lookback by parameter name (couples Backtester to strategies); keep whole Dataset (unbounded); recreate Composite rules in Backtesting (duplicate ownership).

## Decision 5: Typed configuration over existing Manifest maps

**Decision**: Define immutable `BacktestAssumptions`, `MetricVersion` and `RankingVersion` in their owner modules. An application-boundary mapper validates the existing frozen Manifest maps into these types. Domain engines receive only typed values and never read defaults.

**Rationale**: F-005 intentionally left these maps as F-006 extension points. This avoids changing the Manifest record while making execution deterministic.

**Alternatives considered**: Replace Manifest fields now (breaking F-005); pass maps into engines (weak validation); global defaults (break reproduction).

## Decision 6: Evaluation and eligibility

**Decision**: Calculate Total Return, Win Rate, Maximum Drawdown and Number of Trades. Normalize with fixed clamp functions and score `45% return + 30% win rate + 25% inverse drawdown`. Persist Evaluation below five Trades, but mark it ineligible for Leaderboard projection.

**Rationale**: Fixed transforms do not change when other Candidates arrive and remain reproducible.

**Alternatives considered**: Min-max across Experiment (population-dependent); trade-count score (rewards churn); return-only ranking (ignores risk).

## Decision 7: Leaderboard revisions

**Decision**: Build immutable Top-10 revisions sorted by score descending, drawdown ascending and `evaluationFingerprint` ascending. Create no new revision if canonical ordered content is unchanged.

**Rationale**: Stable tie-breaks are independent of Worker completion order and revision fingerprints prevent duplicate projections.

**Alternatives considered**: Candidate completion order (nondeterministic); mutable current table (loses history); Top-K configured globally (hidden default).

## Decision 8: Hierarchical fingerprints

**Decision**: Use SHA-256 over versioned canonical encodings: `backtest-v1`, `evaluation-v1` and `leaderboard-v1`. IDs/timestamps/Worker fields that do not affect business meaning are excluded; ordered Trades and entries retain semantic order.

**Rationale**: A hierarchy localizes mismatch while preserving end-to-end reproduction evidence.

**Alternatives considered**: One global hash (poor diagnosis); input-only hash (cannot prove output); database row serialization (storage-specific and unstable).

## Decision 9: Atomic persistence and successful Attempt predicate

**Decision**: Use capability-local transactions. Backtesting atomically stores Result, Trades and EquityCurveSummary; Evaluation atomically stores its Result; Leaderboard atomically stores Revision and Entries. Before inserting a Backtest Result, lock/verify the Candidate, BACKTEST Job and Attempt relation and require `AttemptStatus.SUCCEEDED`. A unique Candidate outcome provides idempotency; database constraints/triggers add defense in depth.

**Rationale**: Existing composite FKs prevent several cross-links but cannot express a status predicate. No partial outcome may survive inside a capability transaction, while a later capability failure must not erase an earlier accepted immutable result.

**Alternatives considered**: Application check without lock/DB defense (race); status-qualified duplicated table (extra model); one cross-capability transaction for the complete graph (couples independent capability lifecycles and contradicts incremental user stories).

## Decision 10: Forward-only schema evolution

**Decision**: Add a new migration after the current F-006 prerequisite. Add missing assumptions/fingerprint/version data, replace Evaluation uniqueness to include ranking version and protect immutable artifacts. Legacy total Trade `fee` cannot be split reliably into entry and exit components, so the migration aborts with an explicit diagnostic if any legacy Trade row exists; it never fabricates or divides historical fees. Never edit applied migrations and never push remote in F-006.

**Rationale**: The baseline contains the core tables but not all clarified reproducibility and immutability constraints.

**Alternatives considered**: Reuse text columns incompletely (loses evidence); edit baseline/prerequisite migration (breaks migration history); defer integrity entirely to code (weak shared-development safety).

## Governance decision

[ADR-0013](../../docs/adr/0013-backtest-execution-integration.md) records the next-open rule, new public dependency edges and F-004 contract extensions. It is `Accepted`; dependent implementation may merge only after the synchronized ADR-0002, Module View and architecture-test updates pass.

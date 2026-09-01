# Implementation Plan: Backtest, Evaluation and Leaderboard

**Branch**: `feature/006-backtest-evaluation-leaderboard` | **Date**: 2026-08-31 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/006-backtest-evaluation-leaderboard/spec.md`

## Summary

Implement deterministic, bounded-memory Backtesting over the existing F-003 Dataset batch contract; materialize frozen Single/Composite strategies through F-004 public APIs; create immutable Trade/Backtest Result, versioned Evaluation and immutable Top-10 Leaderboard revisions; and persist the complete lineage through capability-owned ports. Canonical decimal rules and hierarchical fingerprints make repeated runs independent of batch size and detect provenance/output drift.

## Technical Context

**Language/Version**: Java 21  
**Primary Dependencies**: Existing domain, market-data, strategy-core, combination and experiment public contracts; Spring JDBC only inside `modules/persistence`; no new production library required  
**Storage**: PostgreSQL/Supabase source of truth; forward-only SQL migration after `20260831000100_f006_prerequisite_integrity.sql`; no Redis dependency in F-006  
**Testing**: JUnit 5, existing test conventions, ArchUnit 1.5, PostgreSQL integration tests and SQL verification scripts  
**Target Platform**: Java modular-monolith backend on JVM 21; local/test PostgreSQL-compatible environment  
**Project Type**: Multi-module backend capability libraries with JDBC persistence adapters  
**Performance Goals**: O(number of Candles) simulation; memory bounded by one `CandleBatch` (maximum 5,000) plus resolved Strategy lookback and constant execution state; deterministic output across batch sizes  
**Constraints**: Long-only one-position execution; next-Candle-open fills; exact decimals (scale 12 trades, scale 10 metrics, `HALF_EVEN`); no partial durable outcome; no remote migration apply  
**Scale/Scope**: Frozen Datasets may contain millions of Candles; one canonical successful Result per Candidate; four required metrics; immutable Top-10 revisions

## Constitution Check

*GATE: Checked before research and re-checked after Phase 1 design.*

| Gate | Status | Evidence / action |
| --- | --- | --- |
| Specification and measurable acceptance criteria exist | PASS | `spec.md` and 16/16 requirements checklist |
| Existing ADRs reviewed before dependency/contract decisions | PASS | [ADR-0013](../../docs/adr/0013-backtest-execution-integration.md) is `Accepted` and explicitly supersedes only the affected ADR-0002 dependency rows and ADR-0009 execution-price default |
| Capability ownership and public boundaries preserved | PASS | Backtesting/Evaluation/Leaderboard own policy and ports; persistence owns adapters only |
| No provider/framework/storage leakage into domain policy | PASS | Public canonical contracts only; infrastructure stays in persistence |
| Immutable provenance and deterministic/versioned outputs | PASS | Frozen manifest inputs, exact decimals and `backtest-v1`/`evaluation-v1`/`leaderboard-v1` |
| Authorization and lineage are not inferred from client IDs | PASS | Result transaction verifies Experiment/Candidate/Job/Attempt relation and `SUCCEEDED` status |
| Durable truth independent of queue/cache | PASS | PostgreSQL-owned output; F-006 has no Redis dependency |
| Evidence is real and remains Planned until executed | PASS | Quickstart defines tests; no benchmark or test result is claimed |
| Applied migrations remain unchanged | PASS | New forward-only migration only; remote apply explicitly excluded |

**Planning gate result**: PASS. ADR-0013 is accepted. **Merge gate**: dependent implementation remains blocked until ADR-0002, Module View and architecture-test matrices are synchronized by T002–T004.

## Phase 0: Research Decisions

Research is consolidated in [research.md](research.md). All technical unknowns are resolved for planning; no `NEEDS CLARIFICATION` remains.

Key decisions:

1. Use `GetDatasetUseCase`/`VerifyDatasetUseCase` and `DatasetCandleReader`; additionally validate batch identity/progression while simulating.
2. Build `StrategyContext` from a resolved rolling lookback and execute its decision at the next Candle open.
3. Extend F-004 public materialization contracts rather than importing Composite internals or guessing lookback.
4. Parse the existing Manifest maps into immutable F-006 typed configs at the application boundary; canonical typed values drive execution/fingerprints.
5. Persist each capability-owned outcome atomically and idempotently: Result plus Trades plus EquityCurveSummary; Evaluation; and Leaderboard Revision plus Entries. A later capability failure does not roll back an earlier accepted immutable outcome.
6. Add a later forward migration for assumptions, fingerprints, successful-attempt enforcement support and database immutability.

## Phase 1: Design

### Capability flow

1. Resolve authenticated/authorized Experiment, frozen Manifest, Candidate, Backtest Job and Attempt through F-005 public boundaries.
2. Require `JobType.BACKTEST`, matching Experiment/Candidate and `AttemptStatus.SUCCEEDED` before accepting a durable Result.
3. Resolve and verify the F-003 `DatasetSnapshot` against `DatasetProvenanceSnapshot`.
4. Materialize Single or Composite Strategy from the exact F-004 provenance and resolve parameter-aware lookback.
5. Stream `CandleBatch` from sequence 0, keep a rolling Strategy window, evaluate only closed Candle prefixes and defer execution to the next Candle open.
6. Fail fast on invalid identity, sequence, ordering, membership, checksum or provenance; persist no partial business outcome.
7. Create canonical immutable Trades/Result, calculate `backtest-v1`, then evaluate metrics and `evaluation-v1`.
8. Include only Evaluation with at least five Trades, project deterministic Top 10 and create `leaderboard-v1` revision.
9. Persist through owner output ports in a transaction; duplicate Candidate outcome returns/rejects idempotently without a second result.

### Design artifacts

- [Data model](data-model.md)
- [Backtest contract](contracts/backtest-contract.md)
- [Evaluation contract](contracts/evaluation-contract.md)
- [Leaderboard contract](contracts/leaderboard-contract.md)
- [Persistence and reproduction contract](contracts/persistence-reproduction-contract.md)
- [Validation quickstart](quickstart.md)

### Database evolution

Create one new forward-only migration with a timestamp later than `20260831000100` to:

- store assumptions contract/version and hierarchical fingerprints;
- add `evaluation_fingerprint`, `leaderboard_revision.ranking_version` and revision fingerprint;
- add explicit `entry_fee` and `exit_fee` columns while preserving `fee` as the canonical total `entry_fee + exit_fee`; because legacy total `fee` cannot determine the two components, the forward-only migration MUST abort with an explicit diagnostic if any legacy Trade row exists, then add non-null columns and the equality constraint only on an empty table;
- persist the bounded-memory `EquityCurveSummary` fields and canonical curve digest needed for deterministic Maximum Drawdown and reproduction, not every equity point;
- replace Evaluation uniqueness with `(backtest_result_id, metric_version, ranking_version)`;
- enforce immutable completed artifacts with owner-aware sealing/trigger rules;
- reject Result creation unless the referenced Attempt is `SUCCEEDED` and belongs to the same BACKTEST Job/Candidate/Experiment;
- add query indexes and SQL tests without editing any applied migration.

Application validation remains mandatory even where the database supplies defense in depth.

### Test strategy

- **Unit**: execution state machine, next-open behavior, fee/slippage, forced close, metrics, normalization, tie-break, decimal edge cases.
- **Contract**: Dataset batch progression, Strategy/Composite materialization, parameter-aware lookback, F-005 identity/status reuse, FailureClassification non-duplication.
- **Reproduction**: same frozen input under different batch sizes produces identical Trades, metrics and fingerprints; one changed input changes the appropriate hierarchy.
- **Persistence integration**: capability-local atomic writes, idempotency, ownership/lineage, successful Attempt predicate, immutability and concurrency; tests also prove a later Evaluation/Leaderboard failure does not corrupt an accepted earlier outcome.
- **Database SQL**: cross-Experiment/Candidate/Attempt rejection, non-successful Attempt rejection, update/delete rejection and migration abort behavior.
- **Architecture**: approved dependency matrix, public-package-only access, no framework/JDBC/Redis/Binance/UI in policy modules, no cycles.
- **Regression**: Gradle `clean check` plus existing F-003/F-004/F-005 focused suites.

## Project Structure

### Documentation (this feature)

```text
specs/006-backtest-evaluation-leaderboard/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── checklists/requirements.md
├── contracts/
│   ├── backtest-contract.md
│   ├── evaluation-contract.md
│   ├── leaderboard-contract.md
│   └── persistence-reproduction-contract.md
└── tasks.md                         # created by speckit-tasks, not this phase
```

### Source Code (repository root)

```text
modules/backtesting/
├── build.gradle.kts
└── src/{main,test}/java/com/cryptostrategy/platform/backtesting/
    ├── api/                         # commands, immutable results and input/output ports
    └── internal/                    # engine, execution state, canonical encoder

modules/evaluation/
├── build.gradle.kts
└── src/{main,test}/java/com/cryptostrategy/platform/evaluation/
    ├── api/                         # evaluator contract, metric/ranking versions, result ports
    └── internal/                    # metric formulas, normalization, evaluation encoder

modules/leaderboard/
├── build.gradle.kts
└── src/{main,test}/java/com/cryptostrategy/platform/leaderboard/
    ├── api/                         # projection/query contracts and revision ports
    └── internal/                    # Top-K, tie-break and canonical encoder

modules/strategy-core/               # parameter-aware lookback public extension
modules/combination/                 # public Composite materialization boundary
modules/persistence/
└── src/{main,test}/java/com/cryptostrategy/platform/persistence/
    ├── api/                         # composition factory only
    └── internal/{backtesting,evaluation,leaderboard}/

architecture-tests/src/test/java/com/cryptostrategy/platform/architecture/
supabase/migrations/<later>_f006_backtest_evaluation_leaderboard.sql
supabase/tests/database/004_backtest_evaluation_leaderboard_test.sql
```

**Structure Decision**: Fill the three existing empty capability modules. Do not create a new service/module. Cross-capability collaboration uses public types only; JDBC stays in persistence.

## Post-Design Constitution Check

The Phase 1 model and contracts preserve ownership, immutable provenance, deterministic serialization, exact decimals, durable truth and scoped dependencies. No Kafka, Redis queue, Worker, public transport or UI is introduced. ADR-0013 is accepted; synchronized ADR-0002/module-view/architecture-test updates remain a merge prerequisite.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
| --- | --- | --- |
| Accepted scoped dependency changes in ADR-0013 | F-006 must consume existing typed F-003/F-004/F-005 contracts | Copying DTOs/IDs or moving business orchestration into Worker violates ownership and provenance |
| Public Strategy lookback/Composite materialization extensions | Streaming execution needs parameter-aware history and owner-defined composition | Keeping all Candles or importing internal Composite code violates memory and module boundaries |

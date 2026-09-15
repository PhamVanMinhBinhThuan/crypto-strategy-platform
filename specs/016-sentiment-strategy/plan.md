# Implementation Plan: Sentiment Strategy (F-016)

**Branch**: `feature/016-sentiment-strategy` | **Date**: 2026-09-09 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/016-sentiment-strategy/spec.md`

## Summary

Promote the existing frozen News/Sentiment results into a fifth system Strategy without allowing the
Strategy engine to call Python, PostgreSQL, the network, or the clock. A server-side snapshot builder
selects asset-relevant observations for one model release and freezes their ordered identities,
publication times, polarity and confidence before execution. Backtest injects this immutable input into
`StrategyContext`; the Sentiment plugin computes a confidence-weighted score and emits deterministic
BUY/SELL/HOLD decisions with evidence. Existing publication, Composite, Search, Result and reproduction
flows reuse the same versioned Strategy and immutable experiment provenance. The web client only
configures and presents authoritative API state.

## Technical Context

**Language/Version**: Java 21; TypeScript 5.9; React 19.1; Next.js 16.3.4; SQL for PostgreSQL

**Primary Dependencies**: Spring Boot 3.5.16, Spring JDBC, existing News, Strategy, Backtesting,
Experiment Execution, Search and Composite public APIs; Next App Router, Zod 4 and existing shared HTTP
client

**Storage**: PostgreSQL remains authoritative for News, Sentiment results and immutable experiment
artifacts; the frozen sentiment snapshot is persisted with experiment provenance; Redis remains
rebuildable job delivery only

**Testing**: JUnit 5.12.2, Spring Boot slice/integration tests, ArchUnit 1.5, Vitest 3.2.4/Testing Library,
Playwright 1.55, deterministic acceptance fixtures

**Target Platform**: Linux-hosted API and horizontally replicable Worker; evergreen browsers from
360 px to 1440 px+

**Project Type**: Modular-monolith API/Worker backend plus Next.js web application

**Performance Goals**: Sentiment evaluation is in-memory and bounded by the frozen observation count;
technical-only Backtests incur no Sentiment dependency; a demo run can select, execute and inspect a
Sentiment Strategy in under ten minutes

**Constraints**: No look-ahead; exact decimal and versioned rounding; immutable snapshot/model release;
no external I/O from `Strategy.evaluate`; one News identity contributes once; absent/zero-weight data
returns explained HOLD; no partial Result on snapshot failure; no browser-side score or signal logic

**Scale/Scope**: One new system plugin (five total), four configurable parameters, Single and Composite
execution, finite Search domains, one asset/model release per snapshot, Strategy/News/Search/Result UI
integration and reproducible evidence

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Pre-design result | Post-design result | Evidence |
|---|---|---|---|
| Spec-first and traceable tasks | PASS | PASS | F-016 has prioritized stories, FR-001–FR-025 and SC-001–SC-010; tasks trace to them. |
| Module ownership and one-way dependencies | PASS | PASS | News owns source records/snapshot construction; Strategy Core owns its immutable supplemental-input contract; runtime composition performs mapping. |
| Public contract compatibility | PASS | PASS | Existing four-argument `StrategyContext` remains available; REST additions are additive and schema-versioned. |
| Reproducibility and immutable evidence | PASS | PASS | Snapshot identity, ordered observation fingerprint, model/preprocessing release and Strategy parameters are frozen before Candidate execution. |
| Durable workflow correctness | PASS | PASS | Snapshot creation completes before job/outbox acceptance; Worker resolves frozen inputs and never calls the live model. |
| Security and ownership | PASS | PASS | Snapshot creation and reads remain owner-scoped through existing authenticated API boundaries; credentials never enter browser payloads. |
| UI shared-reference policy | PASS | PASS | Existing Strategy Composer, Search, Result and News screens are extended; no competing shell or prototype-only engine is introduced. |
| Quality/evidence honesty | PASS | PASS | Fixture/live states are labeled; tests cover boundaries, anti-look-ahead, deterministic output, isolation and provenance. |
| ADR governance | PASS | PASS | ADR-0018 records supplemental Strategy input and Sentiment snapshot ownership before dependent implementation. |

No constitution violation requires a complexity exception.

## Project Structure

### Documentation (this feature)

```text
specs/016-sentiment-strategy/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── sentiment-strategy-contract.md
│   ├── rest-api-contract.md
│   └── ui-contract.md
├── checklists/requirements.md
└── tasks.md
```

### Source Code (repository root)

```text
modules/strategy-core/
├── src/main/java/.../strategy/api/model/       # immutable supplemental input contract
└── src/test/java/.../strategy/api/model/        # validation/canonical-order tests

modules/strategies/
├── src/main/java/.../strategies/internal/sentiment/ # plugin and pure evaluator
└── src/test/java/.../strategies/internal/sentiment/ # threshold/look-ahead/determinism tests

modules/news/
├── src/main/java/.../news/api/                  # snapshot command/result and use-case port
├── src/main/java/.../news/internal/             # deterministic snapshot construction
└── src/test/java/.../news/                      # deduplication/release/cutoff tests

modules/backtesting/
├── src/main/java/.../backtesting/api/port/out/  # frozen supplemental-input resolver seam
├── src/main/java/.../backtesting/internal/      # inject inputs into each rolling context
└── src/test/java/.../backtesting/               # execution and failure tests

modules/experiment/ and modules/experiment-execution/
└── src/main/java/...                            # versioned snapshot provenance and start/reproduce freeze

modules/persistence/
├── src/main/java/.../persistence/internal/news/ # owner-port snapshot query/storage adapter
└── src/*IntegrationTest/java/...                # PostgreSQL ownership and immutable-read tests

apps/api/
├── src/main/java/.../api/strategy/              # catalog/evidence additive DTOs
├── src/main/java/.../api/experiment/            # freeze validation and safe errors
└── src/test/java/.../api/                        # contract/ownership tests

apps/worker/
└── src/main/java/.../worker/config/             # frozen input resolver composition only

apps/web/
├── src/features/strategies/                     # fifth plugin, parameters and provenance presentation
├── src/features/experiments/                    # Search domains and result evidence
├── src/features/news/                           # open authoritative Sentiment Strategy action
└── tests/                                       # contract/component/browser coverage

supabase/migrations/                              # forward-only snapshot/provenance persistence
docs/adr/0018-sentiment-strategy-snapshot-input.md
docs/api/ and docs/evidence/f016/                 # released contract and demo evidence
```

**Structure Decision**: Extend existing capabilities and composition roots. The generic immutable input
contract belongs to Strategy Core, source selection belongs to News, persistence implements News-owned
ports, and Backtesting only receives already-frozen inputs. No new deployable or direct cross-capability
database access is introduced.

## Delivery Phases

### Phase 0 - Architecture and contracts

- Accept ADR-0018 and freeze the supplemental-input/snapshot ownership decision.
- Specify canonical snapshot, aggregation, rounding, reason codes and REST/UI additions.
- Add failing contract/unit tests for the fifth plugin and anti-look-ahead rules.

### Phase 1 - Pure Strategy slice

- Add the immutable supplemental input model to `StrategyContext` with backward compatibility.
- Implement and register Sentiment Strategy parameters, validation and deterministic evaluator.
- Verify BUY/SELL/HOLD, insufficient data, zero weights, deduplication and evidence fingerprints.

### Phase 2 - Frozen News provenance

- Build owner-scoped sentiment snapshots from persisted News and Sentiment results.
- Enforce asset, publication cutoff, model release, stable order and unique identity.
- Persist immutable snapshot identity, observations and canonical fingerprint.

### Phase 3 - Backtest, Search and Composite integration

- Freeze snapshot before standalone Backtest or Search jobs are accepted.
- Resolve the same snapshot for every Candidate and inject it into rolling Strategy contexts.
- Preserve snapshot/model/parameters through Candidate, Result and reproduction evidence.

### Phase 4 - Web workflow

- Show Sentiment as the fifth system Strategy with its four parameter controls.
- Allow selection in Single/Composite Search and link from News to its Composer workspace.
- Present degraded/preflight states and authoritative Result evidence without client-side calculation.

### Phase 5 - Evidence and release hardening

- Run Java, web, architecture and browser gates.
- Capture deterministic, no-look-ahead, failure-isolation and full-flow evidence.
- Update API, architecture, runbook and rubric traceability documents.

## Complexity Tracking

No constitution violations or unjustified architecture additions are present.

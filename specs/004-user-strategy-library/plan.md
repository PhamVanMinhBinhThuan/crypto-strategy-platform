# Implementation Plan: Strategy Registry and User Strategy Library

**Branch**: `feature/004-strategy-registry` | **Date**: 2026-08-30 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/004-user-strategy-library/spec.md`

## Summary

F-004 introduces a pure Java Strategy contract, a startup-assembled fail-fast plugin registry, one deterministic Moving Average implementation, a majority-vote Composite implementation, and an owner-authorized private User Strategy library with immutable published versions. It reuses F-003 `Candle`, `TradingPair`, `Timeframe`, typed ULID support, `DatasetSnapshot`, and `CandleBatch` semantics without defining alternate market models or giving Strategy code access to Dataset storage.

The domain and application boundary live in `modules/strategy-core`; trusted implementations live in `modules/strategies`; majority-vote composition lives in `modules/combination`; PostgreSQL mapping lives in `modules/persistence`; and Spring wiring stays in `apps/api`. The existing DB-v2 tables are sufficient, so no migration or remote database apply is planned. A test-only interoperability harness proves that an external runner can turn F-003 batches into bounded rolling `StrategyContext` windows without adding a production dependency from `strategy-core` to `market-data`.

## Technical Context

**Language/Version**: Java 21

**Primary Dependencies**: JDK collections, records, sealed types, `BigDecimal`, SHA-256; Spring Boot 3.5.16 and Spring JDBC only in composition/persistence; existing F-003 public domain contracts

**Storage**: PostgreSQL/Supabase tables `strategy.strategy_version`, `strategy.user_strategy`, `strategy.user_strategy_version`, and `strategy.user_strategy_component`; PostgreSQL remains source of truth

**Testing**: JUnit Jupiter 5.12.2, ArchUnit 1.5.0, deterministic fixtures, repository SQL contract tests, and an optional isolated local-Supabase integration source set

**Target Platform**: JVM server on the existing Modular Monolith; no browser or public transport delivery in F-004

**Project Type**: Multi-module Gradle backend capability

**Performance Goals**: Strategy evaluation keeps at most the descriptor-required rolling lookback in its context; 100 identical evaluations return identical output; usable-Strategy listing uses independent cursor pages for system and private results, defaults each page to 20, rejects values outside 1–100, and orders every page deterministically

**Constraints**: No Spring/database/network/provider/UI dependency in Strategy logic; no complete Dataset materialization; exact decimal semantics; UTC `Instant`; complete resolved parameters in published snapshots; owner UUID required on every private command/query; no nested Composite; no remote migration apply

**Scale/Scope**: One deterministic system Strategy is required in F-004, with extension contracts supporting the four MVP Strategy types later; private owner-scoped libraries and version history; F-003 batch size remains at most 5000 while each Strategy receives only its required rolling window

## Constitution Check

*GATE: Planning may proceed. Dependent implementation MUST NOT be merged until the ADR merge gate below is resolved.*

| Constitution requirement | Pre-design status | Plan response |
|---|---|---|
| Specification before implementation | PASS | `spec.md` contains prioritized stories, acceptance scenarios, measurable outcomes, and five recorded clarifications. |
| ADR review before dependent merge | PASS FOR PLANNING / BLOCKED FOR MERGE | ADR-0011 is `Accepted`; ADR-0001, ADR-0002, ADR-0005, ADR-0009, and ADR-0012 remain `Proposed`. Tasks must include review, but implementation cannot change their status without owner approval. |
| One owner per capability/data | PASS | `strategy-core` owns Strategy catalog and User Strategy data; `combination` owns policy execution; `persistence` only implements output ports. |
| Dependency direction and provider isolation | PASS | `strategy-core` depends only on `domain`; `strategies` and `combination` depend on public Strategy/domain contracts; persistence depends on public Strategy ports. |
| Determinism and immutable evidence | PASS | Strategy has no clock/network/database/random access; published parameters are fully resolved; snapshots and `strategy-v1` fingerprints are immutable. |
| Versioned public contracts | PASS | Plugin, policy, descriptor, fingerprint, Strategy decision, and snapshot carry explicit versions. |
| Ownership authorization | PASS | Every private use case accepts authenticated UUID and every repository query includes the owner predicate; raw ULID never grants access. |
| Evidence is real and reviewable | PASS | Unit, contract, architecture, SQL, concurrency, and optional local integration evidence are planned; status stays `Planned` until a real run succeeds. |
| Database safety | PASS | Existing migration is not edited; no new migration is expected; local verification is non-production and remote apply requires separate approval. |

## Project Structure

### Documentation (this feature)

```text
specs/004-user-strategy-library/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── strategy-runtime-boundary.md
│   ├── user-strategy-boundary.md
│   ├── persistence-boundary.md
│   └── verification-matrix.md
└── tasks.md                  # generated by speckit-tasks
```

### Source Code (repository root)

```text
modules/domain/
└── src/main/java/com/cryptostrategy/platform/domain/api/
    ├── identity/             # reuse UlidIdentifier and Ulids
    └── market/               # reuse Candle, TradingPair, Timeframe

modules/strategy-core/
├── src/main/java/com/cryptostrategy/platform/strategy/
│   ├── api/
│   │   ├── error/            # stable Strategy error codes/exceptions
│   │   ├── model/            # IDs, descriptors, parameters, context, decisions, snapshots
│   │   ├── port/in/          # catalog and owner-scoped User Strategy use cases
│   │   └── port/out/         # catalog and User Strategy persistence ports
│   └── internal/
│       ├── application/      # owner-aware lifecycle services
│       ├── fingerprint/      # strategy-v1 canonical fingerprint
│       ├── parameter/        # schema/default/cross-field validation
│       └── registry/         # fail-fast plugin registry
└── src/test/java/com/cryptostrategy/platform/strategy/

modules/strategies/
├── src/main/java/com/cryptostrategy/platform/strategies/
│   ├── api/                  # trusted plugin contribution boundary
│   └── internal/ma/          # deterministic MA crossover Strategy/plugin
└── src/test/java/com/cryptostrategy/platform/strategies/

modules/combination/
├── src/main/java/com/cryptostrategy/platform/combination/
│   ├── api/                  # versioned combination policy boundary
│   └── internal/             # majority-vote policy and Composite Strategy
└── src/test/java/com/cryptostrategy/platform/combination/

modules/persistence/
├── src/main/java/com/cryptostrategy/platform/persistence/
│   ├── api/                  # public factory for Strategy adapters
│   └── internal/strategy/    # owner-scoped JDBC adapters, rows, SQL, translation
├── src/test/java/com/cryptostrategy/platform/persistence/internal/strategy/
└── src/strategyIntegrationTest/java/com/cryptostrategy/platform/persistence/strategy/

apps/api/
├── src/main/java/com/cryptostrategy/platform/api/config/StrategyConfiguration.java
└── src/test/java/com/cryptostrategy/platform/api/config/StrategyConfigurationTest.java

architecture-tests/
└── src/test/java/com/cryptostrategy/platform/architecture/
    ├── ModuleBoundaryTest.java
    └── StrategyArchitectureTest.java
```

**Structure Decision**: Extend the existing Modular Monolith modules rather than creating a new application or service. Strategy contracts and lifecycle policy stay in `strategy-core`; executable algorithms and combination policy are separate inbound dependents; JDBC is an outbound adapter; only `apps/api` composes them. The architecture dependency matrix must add `strategy` as an allowed public dependency of `persistence`, while continuing to reject access to `strategy.internal`, `strategies`, or `combination` implementation packages.

## Phase 0: Research Decisions

Research is consolidated in [research.md](research.md). It resolves contract placement, F-003 interoperability, typed identities, canonical parameters and fingerprints, registry lifecycle, authorization, concurrent publication, Composite behavior, and existing-schema reuse. No `NEEDS CLARIFICATION` item remains.

## Phase 1: Design and Contracts

- [data-model.md](data-model.md) defines runtime values, User Strategy aggregates, lifecycle transitions, ownership paths, canonical fingerprints, and existing-table mapping.
- [strategy-runtime-boundary.md](contracts/strategy-runtime-boundary.md) defines pure Strategy/plugin/registry behavior and F-003 compatibility.
- [user-strategy-boundary.md](contracts/user-strategy-boundary.md) defines owner-scoped commands, queries, snapshots, conflicts, and stable errors.
- [persistence-boundary.md](contracts/persistence-boundary.md) defines transaction, SQL ownership, catalog synchronization, immutability, and failure translation.
- [verification-matrix.md](contracts/verification-matrix.md) maps stories, requirements, quality goals, and Constitution rules to planned evidence.
- [quickstart.md](quickstart.md) documents local deterministic verification and optional local-Supabase validation without remote mutation.

## Post-Design Constitution Re-check

| Gate | Result | Evidence in design |
|---|---|---|
| Module ownership and dependency direction | PASS | Contract placement and allowed dependency graph are explicit; no Strategy-to-F003 storage dependency exists. |
| Determinism and reproducibility | PASS | Canonical parameters, `strategy-v1` fingerprint, rolling context, immutable publication, and exact version references are specified. |
| Authentication and authorization | PASS | UUID is supplied by the authenticated boundary; owner predicates are mandatory in ports and SQL; cross-owner lookup is non-disclosing. |
| Database source of truth and migration safety | PASS | Existing DB-v2 tables are reused; transaction and concurrency behavior are defined; no remote apply is authorized. |
| Evidence governance | PASS | Verification matrix marks all evidence `Planned`; quickstart distinguishes default offline checks from environment-dependent PostgreSQL checks. |
| ADR effectiveness | BLOCKED FOR MERGE | Team review must move applicable ADRs to `Accepted` or supersede them before dependent implementation is merged. Planning and task generation may continue. |

## Complexity Tracking

No Constitution violation is requested or justified. The Strategy/Strategies/Combination module split and persistence adapter already exist in the repository architecture and directly represent distinct ownership and change drivers.

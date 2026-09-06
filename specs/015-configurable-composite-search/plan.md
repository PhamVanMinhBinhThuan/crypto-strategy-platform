# Implementation Plan: Configurable Composite Search and Scalable Backtesting (F-015)

**Branch**: `feature/015-configurable-composite-search` | **Date**: 2026-09-05 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/015-configurable-composite-search/spec.md`

## Summary

Replace the raw Dataset-ID/single-strategy Search form with a contract-driven workflow that creates or selects an immutable market-data snapshot, configures a finite pool of versioned strategies and typed parameter domains, generates deterministic composite candidates, and monitors their Backtest → Evaluate → Rank lifecycle. Extend the existing F-010 durable coordinator rather than replacing it: persist a versioned composite search-space/candidate definition, decouple Top-K from execution concurrency, and refill the bounded in-flight window after terminal outcomes and reconciliation. Publish only authoritative configuration, progress, candidate, and leaderboard reads to the F-011 browser client. Preserve old single-strategy experiments through explicit v1-compatible decoding while new experiments use the F-015 representation.

## Technical Context

**Language/Version**: Java 21; TypeScript 5.9; React 19.1; Next.js 16.3.4; SQL migrations for PostgreSQL

**Primary Dependencies**: Spring Boot 3.5.16, Spring JDBC, Spring Data Redis Streams, Jackson, existing strategy/combination/backtesting/evaluation/leaderboard module APIs; Next App Router, Zod 4, existing F-011 HTTP/realtime clients

**Storage**: PostgreSQL is authoritative for frozen datasets, manifests, Search runs, candidates, jobs, results, evaluations, and Top-K; Redis Streams is rebuildable delivery only; browser state is non-authoritative

**Testing**: JUnit 5.12.2, Spring Boot tests, Testcontainers-backed custom integration source sets, ArchUnit 1.5, Vitest 3.2.4/Testing Library, Playwright 1.55, Gradle performance tests

**Target Platform**: Linux-hosted API and horizontally replicable Worker processes; evergreen browsers from 360 px to 1440 px+

**Project Type**: Modular-monolith API/Worker backend plus Next.js web application

**Performance Goals**: Refill a freed active slot within five seconds in the supported deployment profile; complete a reproducible 1,000-candidate benchmark with improved throughput at higher worker capacity; validate 10,000-candidate bounded allocation/backpressure without full eager enqueue

**Constraints**: Immutable provenance; one dataset/pair/timeframe/range per experiment; half-open UTC ranges; no per-candidate Binance calls; maximum active work bounded globally and per experiment; deterministic candidate identity independent of completion order; no browser-side generation/backtesting/evaluation/ranking; forward-only migration; historical v1 reads remain valid

**Scale/Scope**: 100 candidates in full integration evidence, 1,000 in concurrency/performance evidence, 10,000 in controlled allocation/backpressure evidence; one to many strategy pool entries and finite domains; Top-K bounded by accepted request policy

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Pre-design result | Post-design result | Evidence |
|---|---|---|---|
| Spec-first and traceable tasks | PASS | PASS | F-015 spec has testable FR/SC identifiers; tasks map by user story and exact paths. |
| Module ownership and one-way dependencies | PASS | PASS | Search owns generator models; Experiment Execution composes capabilities; Persistence implements owner ports; API/Worker only host orchestration. |
| Public contract compatibility | PASS | PASS | New request/read fields are additive/versioned; v1 manifest/candidate decoding is retained. |
| Reproducibility and immutable evidence | PASS | PASS | Manifest freezes dataset, pool, domains, policy, generator, seed, stop conditions; candidates persist exact composite definitions. |
| Durable workflow correctness | PASS | PASS | PostgreSQL/outbox remains authoritative; Redis delivery is idempotent and repairable; refill is driven by durable decisions. |
| Security and ownership | PASS | PASS | Dataset/catalog/experiment reads remain owner-scoped; safe not-found and redaction contracts remain unchanged. |
| UI shared-reference policy | PASS | PASS | UI-04 layout intent is reused through F-011; prototype business simulation and unsupported metrics/controls are excluded. |
| Quality/evidence honesty | PASS | PASS | Tests cover contract, deterministic generation, concurrency, recovery, browser interaction, and recorded 100/1,000/10,000 evidence. |
| ADR governance | PASS | PASS | ADR-0017 documents the cross-module composite-candidate/refill decision and extends rather than rewrites ADR-0010/0016. |

No constitution violation requires a complexity exception.

## Project Structure

### Documentation (this feature)

```text
specs/015-configurable-composite-search/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── rest-api-contract.md
│   ├── search-coordination-contract.md
│   └── ui-contract.md
├── checklists/requirements.md
└── tasks.md
```

### Source Code (repository root)

```text
modules/search/
├── src/main/java/.../search/api/model/          # pool, domains, composite candidate, run progress
├── src/main/java/.../search/internal/           # canonicalization and deterministic generation
└── src/test/java/.../search/                    # model/generator/cardinality tests

modules/experiment-execution/
├── src/main/java/.../execution/api/             # start/allocation/read orchestration contracts
├── src/main/java/.../execution/internal/        # manifest freeze, refill and recovery
└── src/test/java/.../execution/                  # service/refill/stop/reproduction tests

modules/persistence/
├── src/main/java/.../persistence/internal/      # versioned JSON/SQL mappings and composite transactions
└── src/experimentIntegrationTest/java/.../      # PostgreSQL concurrency and full finite-run tests

modules/contracts/
└── src/main/java/.../contracts/api/             # versioned queue/public DTOs only

apps/api/
├── src/main/java/.../api/dataset/               # create/list frozen datasets
├── src/main/java/.../api/experiment/            # validate/map/start/read candidate details
└── src/test/java/.../api/                        # REST and documentation parity tests

apps/worker/
├── src/main/java/.../worker/search/              # completion-triggered and reconciler refill
├── src/main/java/.../worker/backtest/            # existing bounded execution pool
└── src/test/java/.../worker/                     # refill/restart/backpressure/performance tests

apps/web/
├── src/features/experiments/                     # configuration, monitor and leaderboard UI
├── src/shared/                                   # existing HTTP/realtime client boundary
└── tests/                                        # Vitest and Playwright scenarios

supabase/migrations/                              # forward-only F-015 schema evolution
docs/adr/0017-composite-search-space-and-refill.md
docs/api/                                         # released REST/realtime documentation
docs/evidence/f015/                               # reproducible scale evidence
```

**Structure Decision**: Extend the existing modular-monolith capability ownership. No new deployable, direct database access from Worker, browser-side business engine, or alternate frontend foundation is introduced.

## Delivery Phases

### Phase 0 - Decisions and compatibility baseline

- Accept ADR-0017.
- Freeze v2 composite Search contracts and v1 compatibility behavior.
- Record current single-candidate/refill failures as regression tests before production changes.

### Phase 1 - Composite search domain and persistence

- Add canonical pool, component constraint, combination policy, and candidate definition models.
- Extend deterministic Random Search across finite composite/parameter space.
- Persist versioned manifest/search-space/candidate JSON through a forward migration.

### Phase 2 - Public configuration and dataset workflow

- Publish owner-scoped frozen-dataset listing and enriched dataset metadata.
- Publish registered generator descriptors and strategy schemas already owned by their capabilities.
- Accept/validate v2 Search configuration and expose candidate/progress/leaderboard detail reads.

### Phase 3 - Bounded worker execution

- Remove Top-K from execution-window calculation.
- Execute allocation after durable completion/reconciliation decisions request refill.
- Prove idempotent stop/retry/restart behavior and bounded global/per-experiment work.

### Phase 4 - Production Search UI

- Implement the guided configuration flow in UI-04, authoritative progress, Top-K table, detail links, and async/realtime states.
- Keep fixture adapters contract-equivalent and visibly labeled.

### Phase 5 - Evidence and release hardening

- Run full validation and capture 100/1,000/10,000 scale evidence.
- Update public API, data-flow, architecture, and assessment traceability documentation.

## Complexity Tracking

No constitution violations or unjustified architecture additions are present.


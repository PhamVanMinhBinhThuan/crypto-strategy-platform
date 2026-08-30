# Research: Strategy Registry and User Strategy Library

## Decision 1 — Keep Strategy runtime pure and independent of F-003 storage

**Decision**: `modules/strategy-core` depends on `modules/domain` and directly reuses canonical `Candle`, `TradingPair`, `Timeframe`, `Instant`, and exact decimal values. It does not depend on `modules/market-data`. A caller or future Backtest runner reads F-003 `CandleBatch` values and supplies a bounded rolling window through `StrategyContext`.

**Rationale**: The current dependency matrix permits Strategy to depend on Domain but not Market Data. This keeps evaluation deterministic and prevents Dataset storage concerns from entering the Strategy contract. A test-only interoperability harness can use both modules to prove the handoff without creating a production dependency.

**Alternatives considered**:

- Inject `DatasetCandleReader` into each Strategy: rejected because Strategy would own persistence traversal and violate the clarified boundary.
- Put every Dataset Candle in `StrategyContext`: rejected because Dataset size is unbounded and F-003 deliberately exposes bounded batches.
- Define another Candle type in Strategy: rejected because it duplicates the canonical F-003/domain model.

## Decision 2 — Use typed business ULIDs and separate semantic keys

**Decision**: `StrategyVersionId`, `UserStrategyId`, and `UserStrategyVersionId` implement the existing `UlidIdentifier` and validate through `Ulids`. Authenticated owner identity remains `UUID`. Human-stable plugin and policy keys are validated slug value objects; implementation and policy versions use normalized semantic version values; user revision is a positive integer.

**Rationale**: This matches the application identity rule and existing database columns while preserving readable registry keys such as `ma-crossover@1.0.0`. A plugin slug is a catalog key, not a substitute for a durable entity ULID.

**Alternatives considered**:

- Use UUID for all identities: rejected because it conflicts with the database baseline and F-003 typed ID convention.
- Treat plugin slug as the database primary key: rejected because durable references already use `strategy_version_id` and version history needs an immutable row identity.

## Decision 3 — Model parameters as typed canonical values

**Decision**: Strategy-core owns immutable parameter definitions and values for integer, exact decimal, boolean, text, and enumerated text. Validation rejects unknown fields, resolves declared defaults, enforces required/type/range/allowed-value rules and explicit cross-field constraints, and produces a name-sorted complete `StrategyParameterSet`.

**Rationale**: A typed model avoids raw provider/JSON objects in domain logic, uses exact decimals, gives one validation source to registry and persistence, and makes fingerprints deterministic. Persistence alone maps the model to/from JSONB.

**Alternatives considered**:

- `Map<String,Object>` throughout: rejected because numeric types, validation, and canonical serialization become ambiguous.
- Jackson `JsonNode` in Strategy-core: rejected because it leaks a serialization representation into the pure contract.
- Store only user-supplied values: rejected by clarification because future default changes would alter old meaning.

## Decision 4 — Register only trusted compile-time plugins and fail fast

**Decision**: `StrategyPlugin` publishes one descriptor and creates immutable Strategy instances from canonical parameters. `DefaultStrategyRegistry` receives the trusted plugin list at composition time, sorts descriptors deterministically, and fails construction on duplicate `pluginId + implementationVersion`, descriptor mismatch, or unsupported contract version. No runtime mutation API is exposed.

**Rationale**: This implements ADR-0005's extension point without classloader or untrusted-code risks. Failure during composition prevents startup order from silently selecting an implementation.

**Alternatives considered**:

- Runtime registration/hot loading: rejected because user-uploaded executable code and dynamic libraries are outside MVP.
- First/last registration wins: rejected because results would depend on assembly order.
- Switch statements over Strategy type: rejected because they violate QA-01.

## Decision 5 — Synchronize the runtime catalog through an output port

**Decision**: A `StrategyCatalogStore` output port registers or verifies each trusted descriptor against `strategy.strategy_version` by `plugin_id + version`. Each trusted plugin version declares one stable typed `StrategyVersionId` that is identical across environments. An existing matching identity/fingerprint is reused; conflicting descriptor content fails startup/configuration. A missing row is inserted with that declared ID before user configurations can reference it.

**Rationale**: The runtime registry and durable foreign-key catalog must agree. A source-declared stable ID makes manifests and fixtures portable across environments. Keeping synchronization behind an output port avoids JDBC in Strategy-core and prevents a private Strategy from referencing an unknown runtime implementation.

**Alternatives considered**:

- Seed every plugin only through migration: rejected because every plugin addition would require coupling source deployment to a schema-data migration.
- Ignore `strategy.strategy_version`: rejected because User Strategy versions have a foreign key to it and provenance requires durable descriptors.

## Decision 6 — Authorize private Strategies at both use-case and repository boundaries

**Decision**: Every private command/query contains the authenticated `UUID`; application services never accept an owner ID as editable business input, and persistence operations select/update using both target ULID and `owner_user_id`. Missing and cross-owner targets produce the same non-disclosing `STRATEGY_NOT_FOUND` result. System catalog queries are shared but still travel through the application boundary.

**Rationale**: This follows Accepted ADR-0011 and the Constitution rule that possession of an ID does not grant authorization. Defense in depth prevents a future service method from loading by raw ID and checking too late.

**Alternatives considered**:

- Query by Strategy ID and compare owner afterward: rejected because it loads private data before authorization and is easy to omit.
- Rely on foreign keys or browser RLS alone: rejected because integrity is not authorization and browser roles have no business-table access.

## Decision 7 — Use complete drafts and atomic publication with optimistic conflict

**Decision**: A version draft is created as a complete validated single or Composite configuration. Publication runs in one PostgreSQL transaction, rechecks owner/root state, referenced catalog rows, component count, canonical parameters, and fingerprint, then conditionally changes `DRAFT` to `PUBLISHED`. Unique version constraints and conditional lifecycle updates make exactly one concurrent request succeed; stale requests translate to `STRATEGY_CONFLICT` and must reload.

**Rationale**: The existing schema and triggers already protect published rows. A complete draft avoids partially meaningful fingerprints, while optimistic conflict matches the clarification without holding long application locks.

**Alternatives considered**:

- Last-write-wins: rejected because it can overwrite immutable provenance.
- Automatically assign two sequential versions to concurrent requests: rejected because it invents user intent and may persist two different configurations.
- Long-lived pessimistic locks across user editing: rejected because drafts are prepared outside a database transaction.

## Decision 8 — Keep User Strategy lifecycle separate from immutable versions

**Decision**: `UserStrategy` owns UUID, name, description, kind, `ACTIVE/ARCHIVED`, and timestamps. `UserStrategyVersion` owns positive `versionNo`, `DRAFT/PUBLISHED`, exact source, canonical parameters, fingerprint, and publication time. Archive is one-way and hides the root from normal listing but never removes published versions.

**Rationale**: Mutable organization metadata can change without changing historical meaning. This matches the existing DB-v2 design and Constitution reproducibility rule.

**Alternatives considered**:

- One mutable row per Strategy: rejected because an old Experiment would change meaning.
- Hard delete on archive: rejected because referenced provenance could disappear.

## Decision 9 — Support only flat majority-vote Composite in F-004

**Decision**: A Composite contains at least two distinct exact system Strategy versions with complete component parameters. It cannot contain User Strategy versions or another Composite. `MajorityVotePolicy@1.0.0` counts `BUY`, `SELL`, and `HOLD`; a unique maximum wins and any tie for maximum returns `HOLD`.

**Rationale**: This matches clarification A and the existing `user_strategy_component.strategy_version_id` foreign key. Flat components remove recursion and cycle detection while preserving the core Composite requirement.

**Alternatives considered**:

- Nested Composites: deferred because it requires recursive snapshots, cycle checks, and a different persistence reference.
- Weighted vote in F-004: deferred; the schema can hold weight, but the initial required policy is unweighted majority vote.

## Decision 10 — Define a versioned canonical fingerprint

**Decision**: Published single and Composite snapshots use `strategy-v1:sha256:<64 lowercase hex>`. Canonical input uses length-prefixed UTF-8 fields, explicit type tags, normalized exact decimals, UTC epoch-second/nanosecond timestamps where applicable, name-sorted parameter entries, and for majority vote a component order sorted by plugin key/version/durable ID rather than registration position.

**Rationale**: Type tags and length prefixes avoid delimiter ambiguity; sorted maps and order-insensitive majority components satisfy deterministic reproduction and SC-005. The algorithm version allows future changes without rewriting old fingerprints.

**Alternatives considered**:

- Hash ordinary JSON serialization: rejected because object ordering and numeric formatting can vary.
- Include mutable display name/description: rejected because rename must not change a published configuration fingerprint.
- Use database row position in majority fingerprint: rejected because component order is not semantically meaningful for majority vote.

## Decision 11 — Reuse DB-v2 without a new migration

**Decision**: Map F-004 to the existing baseline and `20260828000100_add_user_strategies_and_jobs.sql` tables. Do not edit either migration. Add JDBC adapters and tests only. If implementation discovers a missing invariant that cannot be enforced in application code, stop and propose a separately reviewed forward migration.

**Rationale**: The existing schema already provides owner FK, active-name uniqueness, kind checks, immutable published rows/components, version uniqueness, and browser privilege revocation. The user explicitly prohibited remote apply.

**Alternatives considered**:

- Rewrite the existing DB-v2 migration: rejected by the Constitution's forward-only migration rule.
- Create a speculative migration now: rejected because no required schema gap has been demonstrated.

## Decision 12 — Keep F-004 transport-free and prove compatibility through tests

**Decision**: F-004 exposes Java capability contracts and composition only. It adds no controller, REST/OpenAPI endpoint, WebSocket event, UI, Job, Search, or Backtest engine. A test-only MACD plugin proves QA-01, and a test-only F-003 batch harness proves bounded rolling-window compatibility.

**Rationale**: This preserves feature boundaries while still producing executable evidence for extension and Dataset safety.

**Alternatives considered**:

- Add public Strategy endpoints now: rejected because public delivery belongs to a later feature and was explicitly excluded.
- Implement Backtest traversal in F-004: rejected because Backtest behavior is F-006.

## Decision 13 — Treat ADR status as a merge gate, not a planning blocker

**Decision**: Planning and task generation continue because the Constitution explicitly permits `Proposed` ADRs for discussion and planning. Tasks must record review of ADR-0001, ADR-0002, ADR-0005, ADR-0009, and ADR-0012; dependent implementation cannot be merged until the team accepts or supersedes them.

**Rationale**: Automatically changing ADR status would fabricate approval, while refusing to plan would contradict the Constitution's planning allowance.

**Alternatives considered**:

- Mark ADRs `Accepted` during planning: rejected because acceptance requires owner/team review.
- Ignore ADR status: rejected because that would violate a non-negotiable Constitution gate.

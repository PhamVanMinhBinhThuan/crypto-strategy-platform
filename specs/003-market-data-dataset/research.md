# Phase 0 Research: Market Data and Dataset

**Feature**: F-003 Market Data and Dataset  
**Date**: 2026-08-29  
**Owner**: Nghi Văn

## Decision 1 — Canonical values and capability ownership

**Decision**: Place stable provider-neutral `Asset`, `TradingPair`, `Timeframe`, `Candle`, natural Candle key, and typed Market business IDs under `modules/domain`. Keep Market Data as their semantic owner and the only capability allowed to create durable Market records. Put Dataset models, use cases, ports, provider behavior, and business orchestration in `modules/market-data`.

**Rationale**: ADR-0002 permits later `backtesting` and `news` capabilities to depend on `domain`, but not on `market-data`. Candle, Timeframe, Trading Pair, and Asset identity are stable cross-capability inputs. Physical placement in `domain` avoids a forbidden future dependency without transferring schema/write ownership.

**Alternatives considered**:

- Keep every canonical type in `market-data.api`: locally cohesive, but later Backtest/News consumption would require an ADR dependency change.
- Copy market values into each consumer: rejected because identity, checksum, and provider normalization would diverge.
- Put them in `contracts`: rejected because these are canonical domain values, not transport DTOs.

## Decision 2 — Business identifiers

**Decision**: Use typed, application-generated uppercase Crockford ULIDs for Asset, Trading Pair, Candle, and Dataset Version. Keep UUID exclusively for Supabase authenticated user/owner identity.

**Rationale**: The applied baseline requires `varchar(26)` ULIDs for every Market business table, DB-02 identifies those as public business IDs, and F-003 forbids a silent schema redesign. A typed wrapper keeps the boundary canonical without exposing unvalidated strings.

**Alternatives considered**:

- UUID in Java plus hidden ULID in PostgreSQL: rejected because it creates two identities and an unnecessary mapping lifecycle.
- Convert Market tables to UUID: rejected because no schema migration is approved.
- Use raw opaque strings: rejected because it loses format/type validation and hides the F-002 contradiction.

**Required gate**: F-002's architecture test currently assumes every public `*Id` field is a UUID. Narrow the rule so authenticated user IDs remain UUID and recognized platform business-ID value objects may carry ULIDs. This is a reviewed foundation correction, not a database migration.

## Decision 3 — Module dependency direction and composition

**Decision**:

```text
domain -> JDK only
market-data -> domain
persistence -> domain + market-data public output ports + Spring JDBC
apps/api -> market-data public factory/input ports + persistence public factory
```

The provider and persistence abstractions are Market Data output ports. Binance implements the provider port inside `market-data.internal`; JDBC adapters implement persistence ports inside `persistence.internal`. Public module factories let `apps/api` compose implementations without importing another module's `internal` package.

**Rationale**: This is the ADR-0002 dependency matrix and F-002 extension point. The capability owns business policy; adapters depend inward on owner ports.

**Alternatives considered**:

- Market Data calling JDBC directly: rejected as a module/data ownership violation.
- `apps/api` constructing internal adapter classes: rejected by the internal-package architecture rule.
- Add integration DTOs to `modules/contracts`: rejected because F-003 has no public HTTP/WebSocket/queue contract.

## Decision 4 — Binance transport

**Decision**: Use one reusable Java 21 `HttpClient` with JDK WebSocket support behind internal REST/stream transport seams. Parse JSON with Jackson inside the Binance adapter. Use Binance's public market-data-only hosts by default and allow base-URI overrides for deterministic tests. Do not add a Binance SDK, WebFlux, credentials, or live-network acceptance tests.

**Rationale**: Java 21 already provides pooled HTTP and WebSocket transports; a small internal seam makes response codes, headers, frames, disconnects, and retry timing deterministic while ensuring provider models never cross `MarketDataProvider`. Binance public klines require no API key.

**Alternatives considered**:

- Binance SDK/connector: rejected because SDK types/lifecycle can leak through the port and couple fixtures to a provider library.
- Spring WebFlux: rejected because the feature does not otherwise need a reactive framework.
- Live Binance tests: rejected by the specification's determinism/offline requirement.

**Primary sources**: [Binance Spot REST API](https://developers.binance.com/en/docs/products/spot/rest-api), [Binance Market endpoints](https://developers.binance.com/en/docs/catalog/core-trading-spot-trading/api/rest-api/market), [Binance WebSocket streams](https://github.com/binance/binance-spot-api-docs/blob/master/web-socket-streams.md), [Java 21 HttpClient](https://docs.oracle.com/en/java/javase/21/docs/api/java.net.http/java/net/http/HttpClient.html), [Java 21 WebSocket.Listener](https://docs.oracle.com/en/java/javase/21/docs/api/java.net.http/java/net/http/WebSocket.Listener.html).

## Decision 5 — Historical pagination and closure

**Decision**: Historical queries use aligned UTC `[start,end)` ranges plus an explicit collection cutoff. The adapter pages `GET /api/v3/klines` with `timeZone=0`, `limit <= 1000`, and a time cursor advanced from the greatest accepted open time by one Timeframe. It rejects non-progress, excess pages, partial failure, malformed tuples, and unresolved gaps before returning a canonical batch.

Normalize Binance's inclusive millisecond close timestamp to the canonical exclusive interval boundary `openTime + timeframe`; first validate that the provider timestamp corresponds to that interval. A historical Candle is proven closed only when that canonical boundary is at or before the explicit cutoff. No ambient clock participates.

**Rationale**: Provider arrays and inclusive close-time conventions are adapter details. One exclusive boundary gives historical/realtime consumers a provider-neutral interval meaning and matches the existing API/database examples. Complete-before-return prevents partial provider pages from entering persistence.

**Alternatives considered**:

- Persist each page: rejected because a late provider failure would leave an apparently accepted partial input.
- Infer closure from system time: rejected as nondeterministic.
- Preserve Binance's inclusive final millisecond as canonical close time: rejected because it leaks provider interval convention and complicates cross-provider equality.

## Decision 6 — Rate limits and provider failures

**Decision**: Translate provider responses to stable F-003 categories. Retry only transient I/O/timeouts, selected `5xx`, and `429/418` rate-limit responses under configurable bounded attempts/elapsed time and capped exponential backoff. Honor `Retry-After`; never retry invalid query or mapping failures. Safe used-weight headers may be logged as operational metadata, never raw provider payloads or secret-bearing URLs.

**Rationale**: Binance documents `429`, escalating `418` bans, `Retry-After`, and weight headers. These values can change and therefore remain adapter/configuration evidence, not domain invariants.

**Alternatives considered**:

- Hard-code exchange-wide quotas in domain code: rejected because provider limits change.
- Retry every failure: rejected because invalid requests/mappings cannot recover and aggressive retries can extend bans.

## Decision 7 — Realtime sharing and recovery

**Decision**: Maintain a process-local reference-counted registry keyed by provider/pair/timeframe. Identical internal consumers share one upstream subscription; the final cancellation releases it and cancels pending recovery. Normalize open and closed updates, use Binance's final-kline flag for realtime closure, order updates of one Candle by provider event evidence, and never regress an accepted closed Candle to open/older state.

On disconnect, report stable `RECONNECTING`, reconnect with bounded capped backoff, intentionally REST-backfill from the last confirmed Candle, buffer concurrent stream updates, merge/deduplicate/verify continuity, persist recovered closed Candles, then report `CONNECTED`. Report `MARKET_DATA_GAP` rather than recovered state when continuity is unresolved.

**Rationale**: This implements the approved clarification and ADR-0003 while keeping the browser WebSocket protocol in F-009. Process-local sharing proves the capability without introducing Redis.

**Alternatives considered**:

- One Binance connection per consumer: rejected due duplicate connections and inconsistent recovery.
- Redis-backed subscription registry: rejected as unnecessary F-003 infrastructure.
- Let F-009 connect to Binance: rejected because provider isolation belongs to Market Data.

## Decision 8 — Candle deduplication and provider corrections

**Decision**: Accepted Candles are write-once. An identical natural key and canonical content is idempotent; the same key with different close time or OHLCV is an integrity conflict and never updates the row. F-003 does not implement provider corrections to already accepted Candle values.

**Rationale**: The baseline uniquely identifies a Candle by provider/pair/timeframe/open time and has no revision column/table. Overwriting would alter every Dataset referencing that row and violate reproducibility.

**Alternatives considered**:

- Update a referenced Candle: rejected because it corrupts immutable evidence.
- Invent a provider suffix/new identity: rejected because it corrupts canonical identity.
- Add Candle revision schema now: rejected because the approved F-003 scope has no such migration.

**Future gate**: Corrected OHLCV at an accepted key requires a separate specification, revision policy, ADR review, and forward migration. FR-031's “future snapshot under an explicitly supported revision policy” is future work, not an F-003 revision implementation.

## Decision 9 — Dataset finalization, immutability, and idempotency

**Decision**: The Dataset store exposes atomic finalize/read/verify only—no metadata/member update or delete. One transaction inserts new closed Candles, Dataset Version metadata, and contiguous membership. A unique-checksum conflict reloads the winner and returns it only when full provenance, count, and ordered membership match; otherwise it reports integrity conflict.

**Rationale**: This uses the baseline constraints plus owner application transactions to meet immutable/atomic requirements without triggers or schema changes. It handles concurrent retries deterministically.

**Alternatives considered**:

- Add immutability triggers: not required by the approved spec/baseline; application ports and verification are the deferred enforcement boundary.
- Add a separate idempotency table: unnecessary because checksum is already globally unique.
- Return any row with the same checksum: rejected because mismatched provenance/membership could hide corruption.

## Decision 10 — Checksum `candle-v1`

**Decision**: Publish a golden-fixture contract for SHA-256 over a versioned, length-safe UTF-8 byte stream. Start with the `candle-v1` marker; then serialize Candles sorted by open time with fixed fields: provider, canonical pair, timeframe, open time, close time, open, high, low, close, and volume. Use one UTC ISO representation and plain scale-insensitive decimal strings with canonical zero `0`. Store lowercase `sha256:<64 hex>`.

`dataset_version.version` carries `candle-v1`; `normalization_version` carries provider mapping provenance. The checksum covers versioned canonical Candle content/order, while finalization separately compares all Dataset provenance.

**Rationale**: This satisfies the Constitution and FR-028–FR-030 without hashing database serialization. The marker guarantees a checksum-contract change changes the digest.

**Alternatives considered**:

- Hash arbitrary JSON/database rows: rejected because field/object order and adapter details are unstable.
- Preserve decimal scale: rejected because `1.0` and `1.000` are logically equivalent in the approved spec.
- Hash Dataset metadata too: rejected because the spec explicitly treats provenance as separately verified metadata.

**Known baseline limitation**: Global `UNIQUE(checksum)` cannot store the same Candle content under different provenance. F-003 returns an integrity conflict rather than changing the schema. A different rule needs explicit schema/contract approval.

## Decision 11 — Persistence technology and verification

**Decision**: Implement adapters with Spring JDBC/transaction APIs in `modules/persistence`; keep the PostgreSQL driver runtime-owned by the application. Fully qualify `market.*`. Use the repository's local Supabase stack and real migrations for a separate `marketDataIntegrationTest` source set; keep the default `clean check` Docker-independent.

**Rationale**: JDBC is already the F-002/database convention. Local Supabase exercises the authoritative PostgreSQL schema, regex checks, precision, timestamps, and conflict behavior without duplicating DDL or mutating shared development.

**Alternatives considered**:

- H2-only persistence tests: rejected because it cannot faithfully prove PostgreSQL schema and conflict semantics.
- Test-only duplicate market DDL: rejected because it can drift from migrations.
- Shared remote Supabase acceptance tests: rejected because they require secrets and mutate shared state.
- Add another migration tool: prohibited by database decisions.

## Decision 12 — Configuration and evidence boundary

**Decision**: `apps/api` binds typed `platform.market-data` configuration for provider choice, public endpoint overrides, timeouts, page bounds, retry/reconnect bounds, normalization version, and checksum version. Public Binance market data requires no secret. Tests inject fixed transports, clock, scheduler, and jitter and never access live Binance.

**Rationale**: Composition is a runtime concern; canonical/application code remains framework-free and deterministic. Endpoint override enables fixture testing without provider leakage.

**Alternatives considered**:

- Hard-code hosts/retry values in the adapter: rejected because environments and controlled tests need bounded overrides.
- Put Spring configuration in domain/application code: rejected by dependency direction.
- Add a Worker/scheduler: rejected because no background job is in F-003 scope.

## Governance findings

- ADR-0001, ADR-0002, ADR-0003, ADR-0007, and ADR-0009 are `Proposed`. They support this plan but must become `Accepted` before dependent implementation merges.
- The typed ULID correction to F-002's broad UUID architecture rule requires cross-owner review before implementation can pass the architecture gate.
- No database migration, public API/controller, browser WebSocket protocol, Redis, queue, Worker job, authentication, Strategy, or Backtest component is required.

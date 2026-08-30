# Feature Specification: Market Data and Dataset

**Feature Branch**: `feature/003-market-data-dataset`

**Feature ID**: `F-003`

**Owner**: Nghi Văn

**Created**: 2026-08-29

**Status**: Implemented — local deterministic verification complete; PostgreSQL integration and ADR governance review remain pending

**Input**: User description: "Create the F-003 Market Data and Dataset capability on the existing Java foundation, including canonical market values, a provider-independent market-data boundary, Binance historical data normalization, closed-Candle validation, Candle and Dataset persistence, deterministic Dataset membership/checksums, deduplication, and deterministic tests."

## Clarifications

### Session 2026-08-29

- Q: Should F-003 include the upstream Binance realtime subscription, reconnect, and historical gap-recovery capability defined by ADR-0003, while leaving browser-facing WebSocket delivery to F-009? → A: Include upstream realtime subscription, bounded reconnect, deduplication, connection state, and REST gap recovery; exclude public WebSocket delivery.

### Session Model Design Decisions

- Q: What identifier type should be used for Dataset, Candle, etc.? → A: ULID must be used, aligning with the Database Baseline. The F-002 ArchUnit test that enforces UUID must be updated to support ULID for domain entities. User ID remains UUID as it is provided by Supabase Auth.
- Q: What type should be used for Dataset `version`? → A: Keep it as `String`, matching the existing database mapping.
- Q: What format should Dataset representations take to avoid memory issues? → A: `DatasetSnapshot` must only contain metadata. A paginated approach (e.g. `CandleBatch`) must be used for reading candles.
- Q: How should `TradingPair` be represented? → A: It should hold `baseAsset` and `quoteAsset` and expose a `canonicalSymbol()` method (e.g. returning "BTC/USDT").

## User Scenarios & Testing _(mandatory)_

### User Story 1 - Build a Reproducible Historical Dataset (Priority: P1)

As a strategy researcher, I want to create a frozen Dataset for one provider, Trading Pair, Timeframe, and historical range so that a later Experiment can use the exact same ordered market input without downloading current provider data again.

**Why this priority**: A reproducible Dataset is the prerequisite for every later Backtest, Experiment, and Leaderboard result. Without stable membership and provenance, downstream results cannot be audited or reproduced.

**Independent Test**: Construct a Dataset from a fixed set of closed historical Candles, then verify its ordered membership, provider, pair, timeframe, half-open range, Candle count, normalization version, and checksum without invoking Strategy or Backtest code.

**Acceptance Scenarios**:

1. **Given** a complete set of valid closed Candles for one provider, pair, timeframe, and requested range, **When** a Dataset is finalized, **Then** it contains every eligible Candle exactly once in ascending `openTime` order and records the required provenance, count, version, and checksum.
2. **Given** the same logical Candles supplied in different response batches or input orders, **When** the Dataset is constructed repeatedly under the same normalization and checksum versions, **Then** every construction produces the same ordered membership and checksum.
3. **Given** a finalized Dataset, **When** a caller attempts to replace, reorder, add, or remove a member, **Then** the mutation is rejected and the original Dataset remains unchanged.
4. **Given** a previously finalized Dataset with the same canonical membership and provenance, **When** an equivalent creation request is repeated, **Then** the operation resolves idempotently to the existing Dataset rather than creating a duplicate logical snapshot.

---

### User Story 2 - Obtain Canonical Historical and Realtime Candles (Priority: P1)

As a market-data consumer, I want historical provider records and realtime updates converted into canonical Candles so that Dataset creation and later consumers do not depend on Binance symbols, interval codes, payload shapes, numeric encodings, connection details, or errors.

**Why this priority**: Provider isolation is the architectural change point for Market Data and is required before persistence or Dataset construction can be trusted.

**Independent Test**: Run the provider contract against fixed Binance and non-Binance historical/realtime fixtures, including disconnect and recovery sequences, and verify that callers receive the same canonical market values and stable connection/error states without seeing provider-specific types.

**Acceptance Scenarios**:

1. **Given** a valid historical provider response, **When** it is normalized, **Then** the caller receives canonical Trading Pair, Timeframe, UTC timestamps, exact OHLCV values, and a closed status without provider-specific fields.
2. **Given** Binance-specific symbol, interval, timestamp, or numeric representations, **When** they cross the provider boundary, **Then** they are mapped to the canonical project representation before any domain or persistence operation.
3. **Given** a fixture provider that satisfies the same provider contract, **When** it supplies equivalent logical data, **Then** Dataset construction works without changes to its domain or persistence behavior.
4. **Given** malformed provider data, an unsupported pair/timeframe, provider unavailability, rate limiting, or an incomplete response, **When** the provider call completes, **Then** the caller receives a stable system error category and no partial invalid data enters persistence.
5. **Given** multiple internal consumers subscribe to the same provider, pair, and timeframe, **When** realtime updates arrive, **Then** one shareable upstream subscription supplies normalized updates without exposing provider connection details to consumers.
6. **Given** the upstream realtime connection is interrupted after a closed Candle was confirmed, **When** connectivity returns, **Then** F-003 reports stable connection-state changes, reconnects with bounded backoff, backfills from the last confirmed Candle, and emits no duplicate closed Candle.

---

### User Story 3 - Persist Valid Candles Without Duplication (Priority: P1)

As a platform operator, I want only valid, final Candles to become durable canonical records so that overlapping historical requests, retries, and duplicate provider responses do not corrupt market history or Dataset membership.

**Why this priority**: The database baseline assumes one canonical record per Candle identity. Violating that invariant would make checksums and downstream results unreliable.

**Independent Test**: Persist fixed historical batches containing exact duplicates, overlaps, invalid values, open Candles, and conflicting records; verify the resulting durable records and structured failures.

**Acceptance Scenarios**:

1. **Given** the same Candle identity appears repeatedly with identical canonical values, **When** one or more overlapping batches are accepted, **Then** exactly one durable Candle exists for that identity.
2. **Given** an existing Candle identity and a later record with different canonical values, **When** the later record is processed, **Then** the conflict is reported as an integrity failure and the accepted record is not overwritten.
3. **Given** a Candle that cannot be proven closed, **When** it is processed for the canonical historical flow, **Then** it is excluded or rejected before persistence and Dataset construction.
4. **Given** invalid timestamps, negative OHLCV, or inconsistent high/low values, **When** normalization or persistence is attempted, **Then** the invalid Candle is rejected without leaving a partial durable write.

---

### User Story 4 - Detect Incomplete or Inconsistent Dataset Input (Priority: P2)

As a strategy researcher, I want Dataset creation to detect gaps and mixed market scopes so that a Dataset cannot appear complete while silently containing missing or unrelated Candles.

**Why this priority**: A deterministic checksum alone cannot establish that the selected data covers the intended range or belongs to one market scope.

**Independent Test**: Attempt Dataset creation with a missing interval, mixed provider/pair/timeframe data, an open final Candle, and an out-of-range Candle; verify that finalization fails with no partial Dataset.

**Acceptance Scenarios**:

1. **Given** the requested half-open historical range contains a missing expected Candle interval, **When** Dataset finalization is requested, **Then** finalization fails with a market-data-gap outcome and no partial Dataset is accepted.
2. **Given** membership candidates from another provider, pair, timeframe, or outside the requested range, **When** Dataset finalization is requested, **Then** the inconsistent members are rejected and no mixed-scope Dataset is accepted.
3. **Given** a persistence failure while saving Candles, Dataset metadata, or membership, **When** the operation ends, **Then** it leaves neither an accepted partial Dataset nor a Dataset whose stored count/checksum disagrees with membership.

### Edge Cases

- The provider returns no Candles for the requested range.
- The requested start is equal to or later than the end, or either boundary is not a valid UTC instant.
- The requested range is not aligned to the selected Timeframe.
- The final interval is still open at the supplied collection cutoff.
- Provider pages arrive out of order, repeat their boundary Candle, or overlap by several intervals.
- Two concurrent requests attempt to persist the same Candle identities or finalize the same logical Dataset.
- Duplicate Candle identities contain equivalent decimal values with different textual scale, such as `1.0` and `1.000`.
- Duplicate Candle identities contain conflicting close time or OHLCV values.
- Dataset sequence numbers are duplicated, missing, or disagree with ascending Candle order.
- The calculated Candle count or checksum differs from the stored Dataset metadata.
- A Dataset is requested after one of its Candles has already been referenced by another finalized Dataset.
- The provider returns a supported interval with a symbol that cannot be mapped to a known canonical Trading Pair.
- A provider timeout or rate limit occurs after some pages have been retrieved.
- Realtime updates for the same open Candle are duplicated or arrive out of order.
- A reconnect backfill overlaps both the last confirmed closed Candle and newer realtime updates.
- The last internal subscriber leaves while a reconnect attempt is pending.

## Requirements _(mandatory)_

### Functional Requirements

- **FR-001**: F-003 MUST define the canonical market concepts Asset, Trading Pair, Timeframe, and Candle with Market Data as their single capability owner.
- **FR-002**: An Asset MUST have a stable identity, a canonical uppercase symbol, an optional display name, and an active state.
- **FR-003**: A Trading Pair MUST reference distinct base and quote Assets and MUST expose a canonical value in `BASE/QUOTE` form independently of any provider symbol.
- **FR-004**: The supported Timeframes MUST be exactly `1m`, `5m`, `15m`, `30m`, `1h`, `2h`, `4h`, and `1d` for this feature; unsupported values MUST be rejected.
- **FR-005**: A canonical Candle MUST contain provider, Trading Pair, Timeframe, open and close UTC instants, exact open/high/low/close/volume values, and finality status.
- **FR-006**: Candle values MUST reject negative OHLCV, a close instant not after the open instant, a high below open/low/close, and a low above open/high/close.
- **FR-007**: Candle identity MUST be the canonical combination of provider, Trading Pair, Timeframe, and open instant.
- **FR-008**: F-003 MUST expose a versioned Market Data provider contract that supports historical retrieval and realtime Candle subscriptions without exposing provider-specific request, response, error, connection, client, or SDK types to its consumers.
- **FR-009**: The Binance integration MUST translate canonical pair, timeframe, range, and subscription requests to provider representations and normalize all accepted historical responses and realtime updates before returning them to the canonical flow.
- **FR-010**: Historical ranges MUST use an inclusive start and exclusive end; accepted Candles MUST have `openTime` within that half-open range.
- **FR-011**: Historical retrieval MUST support bounded pagination and overlapping provider pages without depending on provider response order for its final result.
- **FR-012**: Provider records MUST be normalized to UTC instants and exact decimal values without binary floating-point loss or dependence on the machine's local timezone.
- **FR-013**: A provider record MAY enter the canonical historical flow only when closure is proven by provider finality semantics or by its close boundary relative to an explicit collection cutoff supplied to the operation; ambient domain clock state MUST NOT silently change the result.
- **FR-014**: A record whose closure cannot be proven MUST NOT be persisted or included in a Dataset.
- **FR-015**: Provider failures MUST be translated into stable categories covering invalid query, unavailable provider, rate limitation, unresolved data gap, and mapping failure; raw Binance errors MUST remain internal.
- **FR-016**: Network access, external configuration, and provider-specific behavior MUST remain behind the Market Data provider boundary, and no secret or privileged credential MAY be hard-coded, returned to callers, or written to evidence.
- **FR-017**: F-003 MUST define output ports for durable Asset/Trading Pair lookup needed by Candle operations, idempotent Candle persistence and range retrieval, Dataset finalization, Dataset lookup, and ordered membership retrieval.
- **FR-018**: Domain and application behavior MUST access durable market data only through the owner-defined ports; it MUST NOT access database tables, mappings, or adapter implementations directly.
- **FR-019**: Durable market records MUST conform to the existing `market.asset`, `market.trading_pair`, `market.candle`, `market.dataset_version`, and `market.dataset_candle` baseline without editing the applied baseline migration.
- **FR-020**: Only closed Candles MAY be stored in durable historical Candle records.
- **FR-021**: Reprocessing an identical Candle identity and canonical value MUST be idempotent and MUST NOT create a second logical or physical Candle.
- **FR-022**: A conflicting record for an accepted Candle identity MUST NOT overwrite the accepted record; it MUST produce an integrity failure that identifies the affected market scope without exposing provider payloads or secrets.
- **FR-023**: F-003 MUST support finalizing a Dataset for exactly one provider, Trading Pair, Timeframe, normalization version, and valid half-open historical range.
- **FR-024**: Dataset membership MUST reference the accepted canonical Candle records, use zero-based contiguous sequence numbers, contain no repeated Candle, and follow strictly ascending `openTime` order.
- **FR-025**: Every Dataset member MUST match the Dataset provider, Trading Pair, Timeframe, and range, and all expected Timeframe intervals in the finalized range MUST be present exactly once.
- **FR-026**: Dataset metadata MUST record a stable Dataset Version identity, version value, provider, Trading Pair, Timeframe, normalization version, range, positive Candle count, checksum, and creation instant.
- **FR-027**: Dataset count MUST equal the number of ordered membership records at finalization and whenever Dataset integrity is verified.
- **FR-028**: Dataset checksum MUST use SHA-256 with the baseline `sha256:` representation and a published, versioned canonicalization rule over ordered canonical Candle data.
- **FR-029**: The checksum canonicalization contract MUST fix field inclusion and order, UTC timestamp representation, exact decimal normalization, character encoding, and record ordering; equivalent decimal values and equivalent UTC instants MUST produce identical checksum input.
- **FR-030**: Changing any checksum-relevant Candle value, membership, order, or checksum-contract version MUST either change the checksum or be rejected as mutation of an accepted Dataset.
- **FR-031**: Finalized Dataset metadata and membership MUST be immutable. Provider corrections or different normalization MUST create a distinct future snapshot under an explicitly supported revision policy and MUST NOT mutate evidence already referenced by an Experiment.
- **FR-032**: Dataset construction and persistence MUST be atomic from the caller's perspective: a failure MUST NOT expose an accepted Dataset with partial membership or inconsistent metadata.
- **FR-033**: Equivalent Dataset construction requests, including requests assembled from differently ordered or overlapping fetch batches, MUST resolve deterministically without creating duplicate logical snapshots.
- **FR-034**: Tests MUST cover domain validation, closure filtering, UTC normalization, historical and realtime provider normalization, Binance fixture behavior, stable error/connection translation, shared subscriptions, disconnect/reconnect and gap recovery, stale/out-of-order update handling, Candle deduplication/conflict handling, ordered Dataset membership, gap detection, checksum determinism, atomic persistence, and concurrent duplicate attempts.
- **FR-035**: Automated tests MUST use deterministic fixtures, mocks, or controlled local persistence and MUST NOT require live Binance access, mutable provider data, real secrets, Strategy code, Backtest code, or user authentication.
- **FR-036**: The same provider contract verification suite MUST be usable against the Binance adapter and a deterministic fixture provider to demonstrate provider replaceability.
- **FR-037**: F-003 MUST NOT introduce Strategy, Backtest, Experiment lifecycle, Search, Evaluation, Leaderboard, authentication/ownership, public controller, browser WebSocket gateway, real trading, wallet, or order-placement behavior.
- **FR-038**: Public REST/WebSocket delivery, browser subscription limits/protocol, Redis caching, and general job/queue infrastructure are outside this feature. F-003 owns the upstream provider subscription, open-Candle normalization, connection state, reconnect, and historical gap-recovery boundary consumed by later public-delivery features.
- **FR-039**: Any discovered need to change the applied market schema MUST be recorded as an explicit inconsistency and handled only through a separately reviewed forward migration; F-003 MUST NOT silently redesign or patch the baseline.
- **FR-040**: Planning and implementation merge MUST treat the acceptance status of applicable architecture decisions as a governance gate under the Constitution.
- **FR-041**: Realtime subscriptions MUST expose stable connection states covering connecting, connected, reconnecting, and disconnected without exposing raw Binance connection details.
- **FR-042**: F-003 MUST share an upstream realtime stream among internal subscribers with the same provider, Trading Pair, and Timeframe and MUST release it when no subscriber remains.
- **FR-043**: Realtime processing MUST identify duplicate or stale updates by Candle identity and provider event ordering, MUST treat a closed Candle as final, and MUST NOT replace a closed Candle with an open or older update.
- **FR-044**: After a realtime disconnect, F-003 MUST reconnect using bounded backoff and retrieve historical Candles from the last confirmed closed Candle to fill any gap before reporting the subscription as fully recovered.

### Key Entities

- **Asset**: A canonical traded or quoted asset, identified independently of a provider-specific instrument code.
- **Trading Pair**: An ordered base/quote relationship between two distinct Assets, with canonical `BASE/QUOTE` representation and provider-symbol mappings kept outside the canonical value.
- **Timeframe**: A supported fixed Candle interval with a canonical code and deterministic duration/alignment semantics.
- **Candle**: One final OHLCV observation for a provider, Trading Pair, Timeframe, and opening instant; uniquely identified by that market scope and time.
- **Historical Market Query**: A provider-independent request for one pair, timeframe, half-open UTC range, and explicit collection cutoff.
- **Dataset Version**: An immutable historical snapshot for one provider/pair/timeframe/range and normalization version, described by count, checksum, provenance, and creation time.
- **Dataset Membership**: The zero-based ordered relationship between a Dataset Version and each canonical Candle included in it.
- **Normalization Version**: The version of provider-to-canonical mapping rules used to accept Candles into a Dataset.
- **Checksum Contract Version**: The versioned definition of canonical field selection, serialization, ordering, and digest behavior.

## Success Criteria _(mandatory)_

### Measurable Outcomes

- **SC-001**: For every reference Dataset fixture, 100 repeated constructions using shuffled records, duplicate pages, and overlapping ranges produce one logical Dataset with identical ordered membership, Candle count, and checksum.
- **SC-002**: 100% of open/unproven, malformed, negative, time-invalid, high/low-invalid, mixed-scope, out-of-range, and conflicting-duplicate Candle fixtures are rejected before they can appear in an accepted Dataset.
- **SC-003**: Reprocessing any accepted historical fixture at least three times leaves exactly one durable record per Candle identity and no duplicate Dataset membership.
- **SC-004**: The Binance fixture provider and a provider-neutral fixture implementation both pass the same contract scenarios, and replacing one with the other requires no change to Dataset construction outcomes.
- **SC-005**: For all UTC-offset and decimal-scale variants in the reference suite, logically equivalent Candles normalize to identical canonical values and checksum input with no precision loss.
- **SC-006**: Every finalized Dataset can be inspected to recover its provider, pair, timeframe, range, normalization version, exact ordered Candle membership, positive count, checksum, and creation time; all values agree in 100% of integrity checks.
- **SC-007**: Every simulated provider, mapping, duplicate-conflict, gap, and persistence failure returns the expected stable failure category and leaves zero newly accepted partial Datasets.
- **SC-008**: A reviewer can run the complete F-003 verification suite without internet access, Binance credentials, Strategy/Backtest modules, Redis, or user authentication, and obtain repeatable pass/fail results.
- **SC-009**: Architecture verification reports zero forbidden provider, persistence, framework, or internal-package dependencies from canonical Market Data behavior.
- **SC-010**: An accepted Dataset remains byte-for-byte equivalent in its checksum-relevant data after attempted member update, reorder, addition, removal, or conflicting Candle overwrite in every immutability scenario.
- **SC-011**: In every controlled disconnect scenario, upstream realtime recovery completes within 30 seconds after provider availability returns, every closed Candle in the gap is recovered exactly once, and no stale/open update replaces a closed Candle.

## Assumptions

- F-002 Java Backend Foundation is available on `main`; F-003 extends its existing build, module, configuration, error, and verification conventions rather than creating a new application layout.
- The applied database baseline remains authoritative. Its `dataset_version_id` is the stable Dataset Version identity exposed to downstream capabilities; this feature does not introduce a separate Dataset root entity.
- Durable market identifiers continue to follow the database baseline. Any mapping needed between durable identifiers and F-002 public-boundary identity conventions is resolved during planning without changing the accepted business identity or applied schema silently.
- Canonical Trading Pair values use `BASE/QUOTE`; provider/storage symbols such as `BTCUSDT` are mappings and are not exposed as the canonical pair value.
- Provider identity is normalized to one stable canonical value before Candle identity, deduplication, or checksum processing; provider casing differences do not create different logical providers.
- Historical range boundaries are aligned to the selected Timeframe. A non-aligned request is invalid rather than being silently rounded.
- A complete finalized Dataset contains one closed Candle for every expected interval in its half-open range. A missing interval is an integrity error rather than silently shortening the range.
- Exact duplicate provider records are safe to collapse. Conflicting values for an existing Candle identity are not treated as automatic provider corrections because the current baseline has no Candle-revision model.
- The checksum covers versioned canonical Candle content and order; Dataset metadata separately preserves provider, pair, timeframe, range, count, and normalization provenance and is verified alongside the checksum.
- Market data is shared platform data and has no per-user owner. Authentication and user authorization remain outside F-003, while later public access still goes through the Java application boundary.
- Binance historical data is the only real external provider required for this feature. A deterministic fixture provider exists solely for verification and demo fallback.
- No live Binance availability, throughput, latency, or rate-limit benchmark is claimed by this specification; historical and realtime provider behavior is verified with controlled fixtures, including disconnect and recovery timelines.

## Dependencies and Scope Boundaries

- **Depends on**: F-002 Java Backend Foundation and the applied database baseline.
- **Architectural inputs**: Constitution v1.1.0; ADR-0001, ADR-0002, ADR-0003, ADR-0007, ADR-0009, and ADR-0011; existing Market Data, data-flow, quality-scenario, and data-model documentation.
- **Provides to later features**: canonical frozen Dataset input and fixtures for Experiment/Backtest, plus provider-neutral historical retrieval and upstream realtime subscriptions for later public API/WebSocket integration.
- **Does not provide**: Strategy evaluation, Backtest execution, Experiment ownership/lifecycle, queue processing, browser endpoints/protocol, realtime browser delivery, or UI behavior.
- **Governance gate**: ADR-0001, ADR-0002, ADR-0003, ADR-0007, and ADR-0009 remain `Proposed`. This is an explicit governance gap: the team must review and accept or supersede them before merging implementation that depends on those decisions, as required by the Constitution.

# Feature Specification: Strategy Registry and User Strategy Library

**Feature Branch**: `feature/004-strategy-registry`

**Feature ID**: `F-004`

**Created**: 2026-08-30

**Status**: Draft

**Input**: User description: "Create the F-004 Strategy Registry and User Strategy Library capability, reusing the canonical Market Data and Dataset contracts delivered by F-003."

## Clarifications

### Session 2026-08-30

- Q: Trong MVP, Composite Strategy được phép chứa loại component nào? → A: Chỉ chứa exact system Strategy version và parameters; không cho Composite lồng nhau.
- Q: Khi đánh giá một Dataset lớn, thành phần nào chịu trách nhiệm đọc từng `CandleBatch` và tạo cửa sổ Candle vừa đủ cho Strategy? → A: Application/Backtest runner đọc từng batch và truyền rolling window giới hạn cho Strategy.
- Q: Khi user không nhập một parameter có giá trị mặc định, published Strategy version phải lưu parameters theo cách nào? → A: Lưu đầy đủ parameters sau khi áp dụng default và chuẩn hóa.
- Q: Nếu hai request cùng lúc tạo phiên bản tiếp theo hoặc publish cùng một User Strategy, hệ thống phải xử lý thế nào? → A: Một request thành công; request còn lại nhận conflict và phải tải trạng thái mới trước khi thử lại.
- Q: Khi số Candle trong `StrategyContext` ít hơn `requiredLookback`, Strategy phải trả kết quả nào? → A: Trả lỗi domain có cấu trúc `INSUFFICIENT_DATA`; runner xử lý giai đoạn warm-up.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Discover and Execute Trusted Strategies (Priority: P1)

As a strategy researcher, I want to discover the trusted Strategy types supported by the platform, understand their parameters, and create a deterministic Strategy instance so that I can analyze canonical market data without knowing implementation details.

**Why this priority**: A stable Strategy contract and registry are the change point required by every later Backtest, Search, Experiment, and Strategy-library flow.

**Independent Test**: Register a deterministic system Strategy, retrieve its descriptor by stable Strategy identity and version, validate parameters, evaluate the same ordered Candle context repeatedly, and verify that the same decision and evidence are produced without network, database, clock, provider, or UI access.

**Acceptance Scenarios**:

1. **Given** a registered Strategy version, **When** a consumer lists available system Strategies, **Then** it receives the Strategy identity, version, display information, supported signals, required lookback, and parameter rules needed to configure it.
2. **Given** valid parameters and sufficient canonical Candle history, **When** a consumer creates and evaluates the Strategy, **Then** it receives exactly one `BUY`, `SELL`, or `HOLD` decision associated with the evaluated Candle time and exact Strategy reference.
3. **Given** the same Strategy version, parameters, and ordered canonical Candle context, **When** evaluation is repeated, **Then** the decision, reason, and structured evidence are identical.
4. **Given** missing, unknown, wrongly typed, or out-of-range parameters, **When** Strategy creation is requested, **Then** validation fails before the Strategy can be evaluated and reports which rules were violated.
5. **Given** two registrations with the same Strategy identity and version, **When** the registry is assembled, **Then** the duplicate is rejected rather than silently replacing the first registration.
6. **Given** fewer canonical Candles than the Strategy's required lookback, **When** evaluation is requested, **Then** it returns a structured `INSUFFICIENT_DATA` domain error and the runner decides how to handle the warm-up period; the Strategy does not return a misleading `HOLD` decision.

---

### User Story 2 - Manage a Private Strategy Library (Priority: P1)

As an authenticated user, I want to save named private Strategy configurations and publish immutable versions so that I can reuse them safely and reproduce the exact configuration used by a later Experiment.

**Why this priority**: User ownership and immutable versioning are necessary for authorization and reproducibility; a mutable configuration alone cannot be trusted as Experiment provenance.

**Independent Test**: Use two authenticated user identities to create, list, publish, revise, and archive private Strategies; verify that each user can access only system Strategies plus their own private library and that a published version never changes.

**Acceptance Scenarios**:

1. **Given** an authenticated user and a valid registered Strategy version, **When** the user saves a private Strategy configuration, **Then** the Strategy is owned by that user and its parameters are validated against the referenced descriptor.
2. **Given** two different users, **When** each lists usable Strategies, **Then** both see the system catalog while each sees only their own private Strategy configurations.
3. **Given** one user knows another user's Strategy identifier, **When** the first user attempts to read, revise, publish, or archive it, **Then** the operation is denied without revealing private Strategy content.
4. **Given** a draft Strategy version that passes all validation, **When** its owner publishes it, **Then** its exact Strategy reference, parameters, fingerprint, and provenance become immutable.
5. **Given** a published Strategy version, **When** its owner changes the configuration, **Then** a new version is created and the earlier version remains unchanged and resolvable.
6. **Given** an archived Strategy, **When** it is omitted from normal library listings, **Then** its published versions remain resolvable for authorized reproduction and existing references.
7. **Given** two users choose the same Strategy name, **When** each saves it in their private library, **Then** both are allowed; duplicate active names for the same owner are rejected without altering the existing Strategy.
8. **Given** two requests based on the same current User Strategy version, **When** both concurrently create or publish its next version, **Then** exactly one succeeds and the other receives a conflict requiring a state reload before retry.

---

### User Story 3 - Build a Deterministic Composite Strategy (Priority: P2)

As a strategy researcher, I want to combine multiple exact Strategy versions under a versioned decision policy so that the combination behaves like any other Strategy and remains reproducible.

**Why this priority**: Composite Strategies are a core project capability, but they depend on the stable single-Strategy contract and immutable references from the first two stories.

**Independent Test**: Create a Composite from at least two exact component versions, evaluate fixed Candle fixtures in different component-registration orders, and verify the same combined decision, immutable snapshot, and fingerprint.

**Acceptance Scenarios**:

1. **Given** at least two valid component Strategy configurations and a supported policy, **When** a Composite is published, **Then** it records the policy identity/version and every component's exact Strategy version and parameters.
2. **Given** a valid Composite, **When** it is evaluated, **Then** consumers receive the same Strategy-decision contract used for a single Strategy and do not need Composite-specific branching.
3. **Given** a majority-vote Composite, **When** one signal has more votes than the others, **Then** that signal is returned; when the highest vote is tied, `HOLD` is returned.
4. **Given** the same logical majority-vote components in a different order, **When** the Composite is evaluated, **Then** component order does not change the result or canonical fingerprint.
5. **Given** fewer than two components, duplicate component references, an unsupported policy version, or invalid component parameters, **When** publication is attempted, **Then** the Composite is rejected without creating a published version.

---

### User Story 4 - Extend the Strategy Catalog Safely (Priority: P2)

As a maintainer, I want to add a new trusted Strategy type through the published Strategy extension contract so that downstream capabilities remain unchanged.

**Why this priority**: Minimal-impact Strategy extension is the primary modifiability goal for the project and the purpose of the registry boundary.

**Independent Test**: Add a test-only MACD Strategy and its descriptor/registration, then verify it can be listed, validated, created, and evaluated without changing Backtester, Evaluator, Leaderboard, Search pipeline, public market-data contracts, or UI behavior.

**Acceptance Scenarios**:

1. **Given** a new trusted MACD Strategy conforming to the extension contract, **When** it is registered, **Then** it is discoverable and executable through the same operations as existing Strategies.
2. **Given** the MACD extension, **When** architectural change evidence is inspected, **Then** changes are limited to Strategy implementation, registration, parameter description, and corresponding tests.
3. **Given** an untrusted uploaded script, dynamic library, prompt, or URL-based definition, **When** a user attempts to register it, **Then** it is rejected as outside the supported Strategy model.

### Edge Cases

- A Strategy request references an unknown, removed, or unsupported Strategy version.
- The provided Candle history is shorter than the descriptor's required lookback; evaluation returns `INSUFFICIENT_DATA`, and the runner handles the warm-up period without asking the Strategy to read more data.
- Candle input is empty, not ordered by open time, duplicated, belongs to multiple pairs/timeframes, or contains an open Candle where a closed Candle is required.
- Parameters are numerically valid individually but violate a cross-parameter rule, such as `fastPeriod >= slowPeriod`.
- A user attempts access using only a valid Strategy identifier but is not its owner.
- Two requests concurrently create the same active Strategy name or publish the next version for one owner; one operation succeeds and the stale operation receives a conflict without partial data.
- A Strategy is archived while a published version is referenced by an existing Experiment.
- A component Strategy version remains valid for reproduction but is no longer offered for creating new configurations.
- A Composite repeats a system Strategy version or cannot satisfy the greatest required lookback of its components; nested Composite and User Strategy components are not accepted in the MVP.
- Structured decision evidence contains unsupported, non-deterministic, secret, or presentation-specific content.
- A Dataset contains more Candles than can be held safely in memory; evaluation must consume bounded canonical Candle windows rather than embedding the complete Dataset in a Strategy snapshot.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST publish one versioned Strategy contract that accepts an immutable evaluation context and returns one standardized Strategy decision.
- **FR-002**: A Strategy decision MUST contain `BUY`, `SELL`, or `HOLD`, the evaluated market time, the exact Strategy reference, and deterministic diagnostic information suitable for verification without containing presentation markup.
- **FR-003**: Strategy evaluation MUST be deterministic for the same Strategy version, exact parameters, and identically ordered canonical market input.
- **FR-004**: Strategy evaluation MUST NOT fetch provider data, access durable storage, read the system clock, generate uncontrolled randomness, or mutate its input.
- **FR-005**: The Strategy context MUST reuse F-003 canonical Candle, Trading Pair, and Timeframe meanings and MUST NOT introduce alternate representations of those concepts.
- **FR-006**: For Dataset-backed evaluation, an application or Backtest runner MUST reuse F-003 Dataset metadata and bounded Candle-batch contracts, maintain the bounded rolling Candle window required by the descriptor, and pass that window to the Strategy; a Strategy MUST NOT read Dataset storage or embed an entire Dataset or unbounded Candle collection in its context or snapshot.
- **FR-007**: The system MUST publish a versioned Strategy descriptor containing stable identity, implementation version, display metadata, category, supported signals, required lookback, parameter definitions, defaults, allowed ranges or values, and cross-parameter rules.
- **FR-008**: The system MUST validate all supplied parameters against the same descriptor rules used for discovery, reject unknown or invalid values, apply declared defaults, and produce one complete canonical parameter set.
- **FR-009**: The Strategy Registry MUST list registered Strategy descriptors and resolve and create a Strategy by exact identity and version.
- **FR-010**: The Strategy Registry MUST reject duplicate identity-and-version registrations and MUST NOT silently replace a previously registered Strategy.
- **FR-011**: Consumers MUST be able to use a created Strategy without branching on its concrete type or implementation name.
- **FR-012**: F-004 MUST provide at least one trusted deterministic system Strategy and corresponding fixed-fixture behavior evidence.
- **FR-013**: System Strategy types MUST be available to all users, while executable implementation ownership remains with the platform rather than an end user.
- **FR-014**: An authenticated user MUST be able to create a named private Strategy definition that references an exact registered system Strategy version and exact validated parameters.
- **FR-015**: Every private Strategy MUST have exactly one authenticated owner, represented by the existing user identity supplied by the authentication boundary.
- **FR-016**: Every read or mutation of private Strategy data MUST verify the authenticated owner at the application boundary; possession of a Strategy identifier alone MUST NOT grant access.
- **FR-017**: One `ListUsableStrategies` application use case MUST return the shared system catalog plus only the authenticated user's active private Strategies and MUST NOT disclose other users' private metadata. Its system and private sections MUST use independent cursor pagination with a default page size of 20, a maximum page size of 100, and deterministic ordering; page sizes outside 1–100 MUST be rejected.
- **FR-018**: Active Strategy names MUST be unique for one owner without regard to letter case, while different owners MAY use the same name.
- **FR-019**: A user MUST be able to archive their Strategy definition; archive MUST preserve published versions required by provenance or existing references.
- **FR-020**: A private Strategy version MUST be a draft until validated and published, and publication MUST be a one-way lifecycle transition.
- **FR-021**: A published Strategy version and its components MUST NOT be updated or deleted; a configuration change MUST create a new monotonically increasing version under the same private Strategy, and concurrent next-version or publication attempts based on the same prior state MUST allow exactly one success while returning a conflict for stale attempts.
- **FR-022**: Every published Strategy version MUST retain exact Strategy or policy reference, the complete canonical parameter set after defaults are applied, owner path, lifecycle information, and a deterministic fingerprint calculated from those retained values rather than omitted input or future descriptor defaults.
- **FR-023**: The system MUST resolve an authorized published Strategy version into a complete immutable snapshot suitable for later Experiment provenance without depending on the owner's current Strategy name or active/archive state.
- **FR-024**: A Composite Strategy MUST implement the same evaluation contract as a single Strategy.
- **FR-025**: A Composite Strategy MUST contain at least two exact registered system Strategy versions with exact parameters and MUST record a versioned combination policy; published User Strategy versions and other Composites MUST NOT be components in the MVP.
- **FR-026**: The initial Composite policy MUST use majority vote, return the signal with the most votes, and return `HOLD` when the highest vote is tied.
- **FR-027**: Majority-vote evaluation and canonical Composite fingerprinting MUST be independent of component registration order.
- **FR-028**: Composite creation MUST reject invalid system Strategy versions, duplicate component references, unsupported policies, and any nested User Strategy or Composite reference.
- **FR-029**: Persistent Strategy records MUST be accessed only through the Strategy capability's published application or persistence boundaries; other capabilities MUST NOT depend on its internal storage representation.
- **FR-030**: Durable Strategy state MUST use the existing database ownership model and forward-only migration history; this feature MUST NOT modify an already applied migration or apply a migration to a remote environment without separate approval.
- **FR-031**: Business identifiers introduced by this feature MUST use typed ULID values, while authenticated Supabase user identities MUST remain UUID values.
- **FR-032**: Users MUST NOT upload or execute Strategy source code, scripts, dynamic libraries, prompts, or URL-based definitions in this feature.
- **FR-033**: Rule DSL, prompt-based Strategy creation, URL-based Strategy creation, Job, Execution Attempt, Search Generator, Backtest engine, and new public UI/REST delivery are outside F-004.
- **FR-034**: Adding a conforming MACD Strategy MUST require changes only to Strategy implementation, registration, descriptor/schema, and tests; it MUST NOT require changes to Backtester, Evaluator, Leaderboard, Search pipeline, UI, or F-003 public contracts.
- **FR-035**: Unit, contract, persistence, authorization, determinism, and architecture evidence MUST cover the affected acceptance scenarios; evidence status MUST remain planned until backed by an actual reproducible result.
- **FR-036**: When a Strategy context contains fewer Candles than the descriptor's required lookback, evaluation MUST return a structured `INSUFFICIENT_DATA` domain error; `HOLD` MUST remain a valid Strategy decision rather than representing missing input, and the runner MUST own warm-up handling.

### Key Entities

- **Strategy Reference**: Stable reference to one registered system Strategy identity and implementation version.
- **Strategy Descriptor**: Discoverable description of a Strategy's purpose, category, supported signals, required history, parameter rules, and version.
- **Strategy Parameters**: Complete canonical values, including resolved defaults, used to create one immutable Strategy instance and its fingerprint.
- **Strategy Context**: Immutable, bounded rolling window of canonical F-003 market input assembled by an application or Backtest runner at one evaluation time; it exposes neither Dataset storage nor a batch reader to the Strategy.
- **Strategy Decision**: Deterministic `BUY`, `SELL`, or `HOLD` output with exact provenance and structured diagnostic evidence.
- **System Strategy**: Trusted platform-owned executable Strategy type available to all users through the registry.
- **User Strategy**: Private, named, owner-scoped aggregate used to organize reusable configurations and archive lifecycle.
- **User Strategy Version**: Immutable published snapshot of one single or Composite user configuration; drafts exist only until validation and publication.
- **Composite Component**: Exact registered system Strategy version and parameters participating in one Composite version; it cannot reference a User Strategy or another Composite in the MVP.
- **Combination Policy**: Versioned deterministic rule that combines component decisions, initially majority vote.
- **Strategy Snapshot**: Complete immutable representation consumed by later Experiment provenance independently of mutable library metadata.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: For 100 repeated evaluations with the same Strategy version, exact parameters, and ordered Candle fixture, 100% produce the same decision and diagnostic evidence.
- **SC-002**: All invalid parameter fixtures, including unknown fields, missing required values, boundary violations, and cross-parameter violations, are rejected before evaluation with no partial published version; valid omitted optional values are materialized from defaults into the published canonical parameter set.
- **SC-003**: In two-user authorization tests, 100% of cross-owner list, read, revise, publish, and archive attempts are denied, while both users can discover the same system Strategy catalog.
- **SC-004**: Published Strategy and Composite versions retain equal canonical snapshot values and the same `strategy-v1` fingerprint after later rename, revision, or archive operations on the parent library entry; byte equality is required only for the explicitly versioned canonical fingerprint encoding.
- **SC-005**: Reordering the same majority-vote components across all test permutations changes neither the resulting decision nor canonical fingerprint; tied highest votes always produce `HOLD`.
- **SC-006**: A test-only MACD extension is listed, validated, created, and evaluated with zero changes to Backtester, Evaluator, Leaderboard, Search pipeline, UI, and F-003 public contracts.
- **SC-007**: Registry assembly rejects every duplicate Strategy identity-and-version fixture and never makes startup order determine the selected implementation.
- **SC-008**: Architecture verification finds zero Strategy-domain dependencies on provider adapters, network clients, framework runtime, persistence implementation, database mapping, system clock, Backtester, Search, Evaluator, Leaderboard, or UI.
- **SC-009**: Dataset-backed validation demonstrates that the runner consumes F-003 batches and maintains only the bounded rolling window required for evaluation; neither the complete Dataset membership nor its Candle collection appears in a Strategy context or snapshot.
- **SC-010**: Every published single and Composite Strategy fixture can be resolved to an owner-authorized immutable snapshot containing all references, versions, parameters, policy information, and fingerprint required by a later Experiment.
- **SC-011**: In concurrent next-version and publication tests, exactly one request succeeds, no version number or published snapshot is duplicated or overwritten, and every stale request receives a conflict.
- **SC-012**: Every insufficient-lookback fixture returns `INSUFFICIENT_DATA`, no such fixture returns `HOLD`, and evaluation performs no attempt to fetch additional Candle data.

## Assumptions

- F-002 Java foundation and F-003 Market Data and Dataset contracts are present on the feature branch and remain the canonical source for market values and typed ULID behavior.
- Supabase Auth remains the source of authenticated UUID user identities; F-004 does not store passwords, sessions, or refresh tokens.
- Existing Strategy database tables and forward migrations are reused where they satisfy this specification. Any required schema correction is proposed as a new migration and is not applied remotely without explicit approval.
- The MVP supports platform-owned trusted Strategy implementations and private user configurations only; sharing, teams, marketplace publication, and public user Strategies are deferred.
- The initial deterministic sample Strategy may be a moving-average crossover using fixed canonical Candle fixtures.
- The initial Composite behavior is majority vote with at least two components and `HOLD` for ties.
- Published Strategy versions may remain resolvable for reproduction even when no longer offered for creation of new configurations.
- F-004 creates internal capability contracts and evidence only; browser-facing UI and public delivery endpoints are planned in later features.
- ADR-0005 and ADR-0012 are relevant architectural decisions. Their current status must be reviewed during planning, and any Constitution merge gate must be satisfied before dependent implementation is merged.

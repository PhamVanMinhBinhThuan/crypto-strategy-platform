# Feature Specification: Public API and Realtime Delivery

**Feature Branch**: `009-public-api-realtime`

**Feature ID**: `F-009`

**Created**: 2026-09-01

**Status**: Draft

**Input**: User description: "Create F-009 Public API and Realtime from the approved roadmap and existing public contracts."

**Dependencies**:

- F-003 Market Data and Dataset for market history, Dataset snapshots, and realtime Candle subscriptions
- F-004 Strategy Registry and User Strategy Library for shared descriptors and owner-scoped Strategy operations
- F-005 Experiment Persistence and Ownership for durable Experiments, Candidates, Jobs, idempotency, and Outbox truth
- F-006 Backtest, Evaluation, and Leaderboard for immutable results and ranked projections
- F-007 Worker and Reliable Job Processing for durable progress and transient notification streams
- F-008 News and Sentiment for public News reads and protected sentiment provenance

## Overview

F-009 gives authenticated clients one consistent boundary for the platform's MVP business capabilities. It exposes shared market, Strategy, and News data; owner-scoped Strategy, Experiment, Job, Result, Candidate, and Leaderboard operations; and a realtime channel for Candle, progress, completion, and Leaderboard notifications.

This feature owns public request/response mapping, authentication enforcement, ownership-safe resource behavior, idempotency semantics, error classification, pagination, correlation, realtime subscription lifecycle, and contract compatibility. It does not reimplement the business rules or durable storage owned by F-003 through F-008.

The durable read response remains authoritative. Realtime notifications improve freshness but never become the only place where a user can learn the current state. A client that reconnects, misses messages, or receives duplicates can recover from an authorized snapshot without losing a terminal outcome.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Access Platform Capabilities Safely (Priority: P1)

As an authenticated user, I want one predictable application boundary that recognizes my identity and protects private resources so that I can use the platform without exposing my data or credentials.

**Why this priority**: Authentication, ownership, and safe errors protect every private operation. Shipping feature endpoints without this boundary would allow identifier guessing, inconsistent failures, or leakage of implementation details.

**Independent Test**: Exercise shared and owner-scoped operations with valid, missing, expired, malformed, and wrong-owner identities; verify shared reads are available only to authenticated users, private reads and commands resolve ownership from authoritative relationships, and failures use one safe response contract.

**Acceptance Scenarios**:

1. **Given** a valid authenticated user, **When** the user requests an available shared catalog or their own private resource, **Then** the request is processed using the authenticated identity and returns only authorized data.
2. **Given** no valid authenticated identity, **When** a client calls a business operation or opens the realtime boundary, **Then** access is rejected without revealing token-validation details.
3. **Given** User A knows an identifier owned by User B, **When** User A reads, stops, cancels, reproduces, publishes, archives, or subscribes to that resource, **Then** the platform returns the same ownership-safe inaccessible outcome used for a missing resource and reveals no private metadata.
4. **Given** a request fails validation or a known business rule, **When** the failure is returned, **Then** it has a stable code, safe message, structured details, correlation identifier, and UTC timestamp without a secret, stack trace, storage detail, or provider payload.
5. **Given** a public correlation identifier is valid, **When** the request crosses supported boundaries, **Then** the same identifier is returned and is available for tracing; an absent or invalid identifier is safely replaced.

---

### User Story 2 - Discover and Configure Reusable Inputs (Priority: P1)

As a strategy researcher, I want to browse market history and available Strategies and manage my private Strategy configurations so that I can prepare a reproducible Backtest or Experiment.

**Why this priority**: Users must discover valid Dataset and Strategy inputs before they can start work. Private Strategy operations are already owned by F-004 but have no approved browser-facing delivery until this feature.

**Independent Test**: With one authenticated user, retrieve canonical Candle history and Strategy descriptors, create and publish a private Strategy version, page through the user's usable Strategies, then verify another user cannot see or alter it.

**Acceptance Scenarios**:

1. **Given** a valid pair, timeframe, UTC range, and page request, **When** the user requests historical Candles, **Then** the platform returns canonical ordered Candles using exact decimal values, an exclusive end boundary, and deterministic continuation.
2. **Given** a valid historical selection, **When** the user creates or retrieves an immutable Dataset snapshot, **Then** the response exposes the exact Dataset identity, scope, range, membership count, checksum, and lifecycle needed to select it reproducibly.
3. **Given** registered Strategy versions, **When** the user lists the Strategy catalog, **Then** the response contains the identity, version, display information, required lookback, supported signals, and parameter rules needed to configure a Strategy.
4. **Given** valid Strategy parameters, **When** the user creates, versions, publishes, retrieves, lists, or archives a private Strategy, **Then** the operation delegates to the owner-scoped Strategy rules and returns a stable public representation of the resulting lifecycle state.
5. **Given** a published private Strategy version, **When** it is referenced by a later workload, **Then** the public response preserves its exact version and does not silently substitute current defaults or a newer version.
6. **Given** a malformed cursor, unsupported timeframe, invalid Strategy parameters, stale version expectation, or inaccessible identifier, **When** the request is processed, **Then** the client receives the documented validation, conflict, or inaccessible outcome and no partial mutation occurs.

---

### User Story 3 - Start and Control Durable Work (Priority: P1)

As an authenticated user, I want to start a single Backtest or finite Experiment and inspect or stop its durable work so that retries and asynchronous execution do not create duplicate outcomes or leave me unsure of current state.

**Why this priority**: Starting and controlling work is the core command path for the MVP. Its acceptance, idempotency, ownership, and durable status must be correct before realtime progress has value.

**Independent Test**: Submit the same valid workload repeatedly under one idempotency key, submit a changed payload under that key, inspect the accepted Experiment and Job, request stop/cancel, and repeat the flow as a second user.

**Acceptance Scenarios**:

1. **Given** an authorized Dataset and exact Strategy configuration, **When** the user starts a Backtest with a new idempotency key, **Then** one durable logical Job is accepted and the response identifies where its current state can be read.
2. **Given** an authorized Dataset, generator configuration, search space, ranking configuration, and finite stop condition, **When** the user starts an Experiment with a new idempotency key, **Then** one immutable Experiment request and one durable Search Job are accepted.
3. **Given** the same user, operation scope, idempotency key, and canonical payload, **When** the request is replayed while in progress or after completion, **Then** the original logical outcome is returned without re-executing the command.
4. **Given** the same user, operation scope, and idempotency key with a different canonical payload, **When** the request is submitted, **Then** it is rejected as an idempotency conflict and no second outcome is created.
5. **Given** an owned Experiment or Job in a stoppable state, **When** the user requests stop or cancellation, **Then** the durable state records the request and repeated equivalent requests remain safe.
6. **Given** an asynchronous Job later fails, **When** its owner reads the Job, **Then** the read succeeds with a terminal failed state and safe failure information rather than converting the historical outcome into a request-processing error.
7. **Given** a reproducible completed Experiment, **When** its owner requests reproduction, **Then** a new linked run is accepted and the original evidence remains unchanged.

---

### User Story 4 - Inspect Results and Current Progress (Priority: P2)

As an authenticated user, I want authoritative snapshots of my Experiments, Candidates, Jobs, Backtest Results, and Leaderboards so that I can inspect progress, compare outcomes, and recover after reconnecting.

**Why this priority**: Realtime delivery is transient. Complete, owner-authorized reads are required for correctness, recovery, reproducibility, and the F-010 dashboard.

**Independent Test**: Create owned and foreign Experiments with Jobs, Candidates, Results, and Leaderboard revisions; page and retrieve each supported representation, verify deterministic ordering and ownership isolation, and reconstruct the latest state without realtime messages.

**Acceptance Scenarios**:

1. **Given** an owned active or terminal Experiment, **When** the user retrieves it, **Then** the response identifies its lifecycle, durable progress, stop condition, immutable references, and related Job identities needed by the client.
2. **Given** an owned Experiment with many Candidates, **When** the user pages through Candidates, **Then** every authorized Candidate appears exactly once in deterministic order and no foreign Candidate appears.
3. **Given** an owned completed Backtest, **When** the user retrieves its immutable result, **Then** metrics, trades, exact decimal values, UTC times, and provenance references match the accepted result.
4. **Given** an owned Experiment with a Leaderboard, **When** the user requests its current Top-K, **Then** the response includes the ranking-policy version, current revision, deterministic order, and bounded continuation.
5. **Given** no realtime delivery, **When** the user reads the relevant Experiment, Job, Result, and Leaderboard resources, **Then** the user can determine the latest durable state and terminal outcome.

---

### User Story 5 - Follow Realtime Updates and Recover Gaps (Priority: P2)

As an authenticated dashboard user, I want one realtime connection with independent logical subscriptions so that up to four charts and my Experiment progress update promptly without reloading unrelated views.

**Why this priority**: Realtime visibility is a core user experience, but it must build on authoritative reads and owner-safe commands rather than replace them.

**Independent Test**: Open one authenticated connection, activate four Candle subscriptions plus owned Experiment and Leaderboard subscriptions, change one chart, inject duplicate/out-of-order notifications, disconnect and reconnect, and verify snapshot recovery without missing terminal state.

**Acceptance Scenarios**:

1. **Given** one authenticated client connection, **When** the client activates up to four valid Candle subscriptions and owned Experiment or Leaderboard subscriptions, **Then** each logical subscription is confirmed independently and receives only matching events.
2. **Given** one chart changes pair or timeframe, **When** its old subscription is removed and a replacement is activated, **Then** the other chart and workload subscriptions continue without reload or data reset.
3. **Given** a client requests an Experiment or Leaderboard subscription, **When** ownership is checked, **Then** no snapshot, confirmation containing private state, or subsequent event is delivered unless the authenticated user owns the resource.
4. **Given** a snapshot and concurrent notifications, **When** the client synchronizes, **Then** the published sequencing rules let it reach the latest state without an observable gap and safely discard duplicate or stale updates.
5. **Given** a disconnected or slow client, **When** delivery resumes, **Then** the client can resubscribe and use authorized reads to recover Candle gaps and durable workload state; the platform does not claim exactly-once delivery.
6. **Given** a failure isolated to one subscription, **When** the platform reports it, **Then** the other valid subscriptions remain active unless the connection has a serious protocol or security violation.
7. **Given** a Candle close, terminal Experiment or Job state, Backtest completion, or new Leaderboard revision, **When** intermediate updates are coalesced under pressure, **Then** the non-droppable outcome remains observable through delivery or the authoritative snapshot.

---

### User Story 6 - Read News Without Sentiment Coupling (Priority: P3)

As an authenticated user, I want to browse normalized News and available sentiment summaries so that sentiment delays or failures do not block the rest of the dashboard.

**Why this priority**: News improves the research experience but must remain failure-isolated from Market Data and technical Backtests.

**Independent Test**: List News with and without sentiment, filter and page deterministically, make the sentiment dependency unavailable, and verify News, Market, Strategy, and Backtest reads remain usable while protected audit provenance stays unavailable to browsers.

**Acceptance Scenarios**:

1. **Given** normalized News items, **When** an authenticated user lists them, **Then** the newest items are returned first with deterministic tie-breaking, bounded pagination, related assets, analysis state, and sentiment only when available.
2. **Given** sentiment is pending, retrying, or unavailable, **When** News is listed, **Then** News remains visible with an explicit analysis state and no fabricated sentiment value.
3. **Given** a browser client, **When** it attempts to access protected sentiment release provenance or the internal inference service, **Then** access is denied; the browser receives only the public News representation.
4. **Given** News or sentiment failure, **When** the user accesses Market Data, Strategies, or technical Backtest state, **Then** those capabilities remain available unless their own dependency has failed.

### Edge Cases

- Authentication expires after a realtime connection is established.
- A valid identifier exists but belongs to another user, or its parent relationship is inconsistent.
- Two users reuse the same idempotency key for equivalent or different commands.
- A replay arrives after the original command has completed or failed.
- A client sends unknown request fields, an unsupported contract version, an invalid cursor, an oversized body, or an unsupported content type.
- A requested page changes while new items or revisions are being created; continuation must remain deterministic and duplicate-safe.
- A client subscribes twice with the same subscription identifier, rapidly replaces a subscription, or unsubscribes an unknown identifier.
- An event is duplicated, delayed, delivered after unsubscribe, or older than the current Candle, progress snapshot, or Leaderboard revision.
- A client disconnects between snapshot retrieval and subscription confirmation, or while a terminal event is being published.
- A slow client exhausts its bounded delivery capacity.
- A shared upstream Market stream is still needed by another connection when one client unsubscribes.
- The queue or notification stream is unavailable while durable Experiment and Job state remains readable.
- Sentiment is unavailable while News items remain readable.
- Exact decimals contain trailing zeros, timestamps use a non-UTC offset, or identifiers are syntactically valid but inaccessible.

## Requirements *(mandatory)*

### Functional Requirements

#### Public Boundary and Security

- **FR-001**: The platform MUST provide one versioned client-facing contract for every F-009 operation and notification, with documented request, response, error, and compatibility behavior.
- **FR-002**: Every browser-facing business request and realtime connection MUST require an authenticated user identity; authentication provider administration and login user interface remain outside F-009.
- **FR-003**: Shared Market, system Strategy, and News data MUST still be read through the authenticated application boundary; browser clients MUST NOT access business storage, external market providers, or the sentiment inference service directly.
- **FR-004**: Every private resource read, command, and subscription MUST authorize through its authoritative owner relationship. A client-supplied resource identifier alone MUST NOT grant access.
- **FR-005**: A missing private resource and a resource owned by another user MUST produce the same public inaccessible outcome so callers cannot enumerate another user's data.
- **FR-006**: Internal service operations MUST be separated from browser operations, require a dedicated internal credential, and MUST NOT accept a browser user token as equivalent authority.
- **FR-007**: Request bodies MUST reject unknown fields, invalid formats, unsupported versions, and unsupported media types according to the published contract.
- **FR-008**: Public errors MUST contain a stable code, safe message, structured details, correlation identifier, and UTC timestamp; they MUST NOT contain credentials, raw tokens, stack traces, storage queries, internal class names, filesystem paths, or raw provider/service payloads.
- **FR-009**: Known failure causes MUST map consistently to the documented request, authentication, inaccessible, conflict, validation, rate-limit, dependency, timeout, and internal-error categories.
- **FR-010**: The platform MUST accept or generate a safe correlation identifier for each interaction and return it consistently across the response or notification path.

#### Read Contracts and Pagination

- **FR-011**: Public identifiers MUST be opaque to clients, exact numeric business values MUST be represented without binary rounding, and timestamps MUST have unambiguous UTC meaning.
- **FR-012**: Collection reads MUST use bounded pagination with deterministic ordering and opaque continuation; traversing an unchanged collection MUST not omit or repeat items.
- **FR-013**: The Market boundary MUST return canonical, ordered Candle history for a valid pair, timeframe, inclusive start, exclusive end, and bounded page request without leaking provider-specific representations; it MUST also expose creation and retrieval of immutable Dataset snapshots with their exact scope, range, membership count, checksum, and lifecycle.
- **FR-014**: The Strategy discovery boundary MUST expose the descriptor data required to select an exact Strategy version and validate user input.
- **FR-015**: F-009 MUST expose owner-scoped operations needed to create, retrieve, list, version, publish, and archive private Strategies while preserving F-004 lifecycle, concurrency, and immutability rules.
- **FR-016**: F-009 MUST expose authoritative owner-scoped reads for an Experiment, its Candidates, its durable Jobs, its immutable Backtest Results, and its current Leaderboard.
- **FR-017**: A failed asynchronous Job MUST remain a successful resource read with a failed lifecycle state and safe failure object; a historical execution failure MUST NOT be represented as failure to read the Job.
- **FR-018**: Backtest Result responses MUST preserve immutable metrics, trades, exact values, UTC times, and the provenance references required by F-006.
- **FR-019**: Leaderboard responses MUST identify the Experiment, current revision, ranking-policy version, deterministic rank order, and a result reference for each entry.
- **FR-020**: News reads MUST return normalized News independently of sentiment availability, expose an explicit analysis state, and include sentiment only when a valid result is available.
- **FR-021**: Protected sentiment audit provenance MUST remain an internal-only operation and MUST NOT be included in public News list responses.

#### Commands, Ownership, and Idempotency

- **FR-022**: Starting a Backtest MUST validate and freeze authorized Dataset, Strategy, and execution inputs before accepting exactly one durable logical Job.
- **FR-023**: Starting an Experiment MUST require authorized immutable inputs, a versioned generator and ranking configuration, a valid search space, and at least one finite stop condition before accepting exactly one durable Search Job.
- **FR-024**: Accepted asynchronous commands MUST return the durable resource and Job identities plus a stable location where current status can be retrieved.
- **FR-025**: Every command that can create durable work or duplicate a business effect MUST require an idempotency key scoped by authenticated user and operation.
- **FR-026**: Reusing an idempotency key with the same canonical payload MUST resolve to the original in-progress or completed logical outcome without re-executing the command.
- **FR-027**: Reusing an idempotency key in the same scope with a different canonical payload MUST return an idempotency conflict without creating another record or business outcome.
- **FR-028**: The platform MUST expose owner-scoped stop or cancellation operations for supported Experiment and Job states, preserve idempotent repeat behavior, and return a conflict for a disallowed state transition.
- **FR-029**: The platform MUST expose owner-scoped Experiment reproduction as creation of a new linked run; reproduction MUST NOT modify the original Experiment, Manifest, Result, Evaluation, Trade, or Leaderboard evidence.
- **FR-030**: Public F-009 code MUST invoke only published application boundaries of F-003 through F-008 and MUST NOT read or write another capability's internal storage representation.

#### Realtime Contract and Recovery

- **FR-031**: An authenticated client MUST be able to multiplex independent Candle, Experiment, and Leaderboard subscriptions over one realtime connection.
- **FR-032**: One connection MUST support at most four active Candle subscriptions; Experiment and Leaderboard subscription limits MUST be bounded and documented before implementation completion.
- **FR-033**: Realtime commands MUST be limited to subscribe, unsubscribe, and connection health. Commands that create, stop, cancel, publish, archive, or reproduce business state MUST use the request/response boundary.
- **FR-034**: Each command and event MUST use a versioned envelope containing event type, event version, unique event identity, UTC occurrence time, correlation identity, logical subscription identity, and a typed payload.
- **FR-035**: A subscription MUST NOT become active until its input and authorization are validated and the client receives an explicit confirmation or isolated subscription error.
- **FR-036**: The snapshot-and-event sequencing contract MUST let a correctly behaving client reach the latest state without an observable gap when snapshots and notifications overlap.
- **FR-037**: Candle updates MUST be identifiable by pair, timeframe, and opening instant; a closed Candle MUST not be overwritten by an older or open update.
- **FR-038**: Leaderboard notifications MUST carry a monotonic revision, and clients MUST be able to discard any revision not newer than the authoritative snapshot already applied.
- **FR-039**: Progress notifications MUST include enough resource identity and lifecycle context to trigger an authorized refresh, while durable Experiment and Job reads remain authoritative.
- **FR-040**: Delivery MUST be duplicate-safe rather than claiming exactly-once semantics. Reconnect MUST allow resubscription and recovery of Candle gaps and durable workload state through authorized reads.
- **FR-041**: An error isolated to one logical subscription MUST NOT terminate other subscriptions. Serious authentication, origin, size, rate, or protocol violations MAY terminate the connection using documented behavior.
- **FR-042**: Delivery capacity MUST be bounded. Intermediate open-Candle or progress updates MAY be coalesced, but a closed Candle, connection-state change, completion notification, terminal workload state, or latest Leaderboard revision MUST remain recoverable.
- **FR-043**: Realtime access MUST enforce an allowed client origin, bounded message size, bounded command rate, bounded subscription count, and a reauthentication or disconnect policy when the user's authentication expires.

#### Compatibility and Evidence

- **FR-044**: Request/response, error, and realtime documents MUST be updated together whenever F-009 changes a public field, code, operation, command, event, or compatibility rule.
- **FR-045**: Adding an optional response field MAY remain compatible within a contract version; removing, renaming, changing the type, or changing the meaning of a published field MUST require a new contract version and an explicit transition rule.
- **FR-046**: Automated contract evidence MUST detect drift between documentation, transport representations, error mappings, and representative consumer expectations.
- **FR-047**: Authorization evidence MUST cover missing identity, invalid identity, two-user ownership isolation, cross-parent identifiers, idempotency scoping, and private realtime subscriptions.
- **FR-048**: Recovery evidence MUST cover duplicate and stale events, notification loss, reconnect during synchronization, slow clients, queue/cache loss, and reconstruction from durable reads.
- **FR-049**: F-009 MUST NOT implement external market ingestion, Strategy or Search algorithms, Backtest/Evaluation/Leaderboard business rules, News collection or inference, browser screens, authentication-provider administration, or real-money trading.
- **FR-050**: An operation whose owning dependency is not ready MUST not be represented as successfully functional; implementation and release evidence for that operation remain blocked until its published application boundary is available.

### Key Entities

- **Authenticated Session Context**: The verified user identity and safe correlation context applied to one request or realtime connection; it contains no reusable privileged credential in public output.
- **Public Resource Representation**: A versioned, client-facing view of a shared or owner-scoped domain resource without internal storage or provider details.
- **Idempotent Command Receipt**: The owner-and-operation-scoped association between an idempotency key, canonical request meaning, and the original logical outcome.
- **Realtime Connection**: One authenticated client channel that owns a bounded set of logical subscriptions and has an explicit lifecycle.
- **Logical Subscription**: A client-chosen routing identity bound to one authorized Candle, Experiment, or Leaderboard interest until replaced, removed, or disconnected.
- **Realtime Event Envelope**: A versioned notification identity, time, correlation, subscription routing value, and event-specific payload.
- **Authoritative Snapshot**: The latest authorized durable representation used for initial rendering, reconciliation, and recovery after notification loss.
- **Public Error**: A stable, safe failure classification with structured client-actionable details and trace correlation.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: In automated two-user tests, 100% of missing, invalid, and cross-owner private operations and subscriptions are rejected without exposing another user's resource content or existence.
- **SC-002**: Replaying the same accepted command 100 times with the same user, scope, key, and payload produces exactly one logical business outcome; changing the payload under that key produces no additional outcome.
- **SC-003**: Under the agreed MVP demonstration load, at least 95% of bounded read requests present their complete response within 2 seconds, and at least 95% of valid start/stop commands present acceptance or rejection within 2 seconds.
- **SC-004**: At least 95% of realtime updates visible at the platform boundary become available to a connected, healthy client within 1 second of the corresponding normalized update or durable state change.
- **SC-005**: One client can maintain four independent Candle subscriptions plus owned Experiment and Leaderboard subscriptions for a 30-minute demonstration without one subscription reload resetting another.
- **SC-006**: In disconnect, duplicate, stale-event, and notification-loss tests, the client can reconcile to the latest authoritative Candle, terminal Job/Experiment state, and Leaderboard revision with no missing terminal outcome and no duplicate business effect.
- **SC-007**: 100% of documented request/response examples, public error mappings, realtime commands, and critical events pass automated compatibility checks against the released contract version.
- **SC-008**: Security verification finds zero browser-visible privileged credentials, raw authentication tokens, provider payloads, stack traces, storage queries, internal class names, or filesystem paths in public responses and notifications.
- **SC-009**: An authenticated user can complete the MVP flow—discover an input, start durable work, follow progress, and retrieve the resulting status or ranked outcome—without accessing internal services or business storage directly.
- **SC-010**: During a simulated News/sentiment failure, Market, Strategy, Experiment, Job, and technical Backtest reads retain their expected behavior, while News remains readable with an honest degraded analysis state.

## Assumptions

- The accepted authentication and ownership decisions remain authoritative: browser business access requires a verified user, shared business data still passes through the application boundary, and private resource existence is concealed with a uniform inaccessible response.
- Existing API and realtime documents are drafts to be reconciled by F-009, not immutable released contracts. Missing operations required by approved F-003 through F-008 application boundaries may be added during planning.
- The initial client is the F-010 browser dashboard. Native mobile clients, third-party developer keys, organization/tenant roles, public anonymous access, and external webhooks are outside the MVP.
- Durable snapshots are the source of truth. Realtime delivery is transient, at-least-once or lossy, and recoverable; it is not a durable event history.
- The exact synchronization marker used to close the snapshot/event race, connection and non-Candle subscription limits, heartbeat interval, command-rate limit, message-size limit, and authentication-expiry grace behavior will be selected in planning and then published as configuration or contract limits.
- The Search Coordinator implementation is a dependency rather than F-009 business logic. F-009 may define its public contract, but it cannot claim the start-Experiment flow as complete until that dependency can execute the published command.
- Database migration work is outside F-009 unless planning discovers a missing durable invariant owned by another capability; any such change requires a forward migration and the owning capability's review.
- Health and operational endpoints may have separate exposure rules, but they must not reveal business data or secrets and are not treated as browser business operations.

# Phase 0 Research: F-013

Repository inspection on 2026-09-03 found F-013 route placeholders, the released F-011 foundation, F-009 API/docs, and no F-010 feature directory. Approved screenshots/prototype sources were inspected as presentation evidence. Authority: Constitution → accepted ADRs → released contracts → F-011 → F-013 → UI reference.

## Decisions

### Feature services and view models over F-011

**Decision**: Pages compose hooks/services that depend on F-011 clients; DTO mappers create immutable view models; explicit fixture or production adapters supply data.

**Rationale**: Supports mock-first substitution, isolates DTO changes, preserves exact decimals, and avoids global feature context.

**Alternatives considered**: Page-local mocks (scattered); prototype AppContext (business simulation); direct fetch/WebSocket (duplicates F-011).

### Fixtures are authoritative recordings

**Decision**: Use fixed DTOs and finite explicitly triggered event sequences. Accepted Start/Reproduce UI may appear only under test/dev composition.

**Rationale**: Enables independent UI verification without backend logic.

**Alternatives considered**: Calculated/timer pipelines (forbidden); random data (non-repeatable); blocking all UI on dependencies.

### Exact decimals remain strings

**Decision**: Keep public financial/rate/metric/score strings unchanged through view models. Presentation may group/add symbols or show a shortened display-only form, but the complete authoritative string must remain available through accessible text/disclosure and the stored value is never replaced or used to recompute business metrics.

**Rationale**: Constitution and DTOs require exact semantics.

**Alternatives considered**: JavaScript numbers (precision loss); frontend recomputation; unnecessary decimal business library.

### REST-authoritative realtime merge

**Decision**: REST owns snapshots. Deduplicate event IDs; events trigger targeted recovery. Fetch Leaderboard only for strictly newer revision.

**Rationale**: F-009 disclaims exactly-once and full replay.

**Alternatives considered**: Treat event payload as full state; local reranking; exactly-once assumption.

### Extend realtime within F-011 ownership

**Decision**: Add incoming/status listeners, 4001 auth refresh, bounded retry/exhaustion, manual reconnect, and resubscription to F-011 real/mock clients.

**Rationale**: Current client has no `onmessage`/observers/close-code branch and retries indefinitely.

**Alternatives considered**: Second socket; polling-only; feature retry loops.

### Distinct result lookup identities

**Decision**: Parse `resultId` and `backtestId` as separate modes. Fixtures support both; production resultId shows the parity gate until fixed.

**Rationale**: Leaderboard supplies result identity while the controller accepts standalone backtest identity; no public conversion exists.

**Alternatives considered**: Reinterpret IDs; inspect persistence; hide navigation.

### Form discovery uses released sources

**Decision**: Strategy/version/parameters come from `GET /api/v1/strategies`; production dataset uses known ID; dataset/generator selections are fixture-configured until discovery contracts exist; Top-K is 1–100, initial 10, presets 10/25/50.

**Rationale**: Examples and ADR-internal identities are not public enumerations.

**Alternatives considered**: Hard-coded production catalogs; internal-service access; backend enum interpretation of presets.

### Reuse F-011 for authentication failure and Retry-After metadata

**Decision**: F-013 delegates `401 AUTHENTICATION_REQUIRED` to the existing F-011 session/authentication-failure lifecycle. For `429 RATE_LIMIT_EXCEEDED`, extend the F-011 normalized public-error boundary additively to expose safe `Retry-After` delay metadata; feature code does not bypass `ApiClient` to inspect raw responses.

**Rationale**: Authentication/session ownership already belongs to F-011, while the current `PublicError` drops `Retry-After`. A narrow owner-scoped extension preserves one HTTP boundary and lets F-013 implement the released F-009 rate-limit behavior safely.

**Alternatives considered**: Feature-owned token refresh (duplicates F-011); raw `fetch` for headers (bypasses F-011); immediate retry (violates rate-limit guidance).

### Separate state responsibilities

**Decision**: Result, Experiment, Jobs, Leaderboard queries; three command states/keys; realtime connectivity/subscriptions; and local form/presentation state remain separate.

**Rationale**: Transport degradation and uncertain commands must not invalidate durable snapshots.

**Alternatives considered**: Giant context/reducer; optimistic snapshot overwrite.

### Prototype is presentation only

**Decision**: Retain dense dark panels, hierarchy, four cards, status/progress regions, local table scrolling. Omit unsupported data/actions.

**Rationale**: Prototype contains random/timer Search simulation and unreleased analytics.

**Alternatives considered**: Port verbatim; ignore approved visual reference.

## UI mapping

| Reference | Production | Source | Readiness |
|---|---|---|---|
| Backtest metrics | `ResultMetrics` | F-009 result | READY |
| Trade tables | `TradeHistory` | F-009 trades | READY |
| Search configuration | `ExperimentConfigurationForm` | Start request + Strategy descriptors | UI READY / execution BLOCKED |
| Search pipeline | `SearchProgressPresentation` | Experiment/Job REST + events | PARTIAL |
| Leaderboard table | `LeaderboardTable` | F-009 Leaderboard | READY |
| Profit Factor, Sharpe, charts/equity | Omitted | No released data | OMITTED |
| Pause/Resume, worker monitor, local ranking | Omitted | No public contract | OMITTED |

## Public contract gaps

1. OpenAPI publishes result-ID lookup; `apps/api` implements only standalone backtest-ID lookup. Candidate results cannot be fetched by Leaderboard result ID.
2. F-011 realtime lacks event/status listeners, 4001 refresh, bounded exhaustion, and manual-disconnected lifecycle.
3. No F-010 artifacts are present; Start/Reproduce intentionally return dependency unavailable.
4. F-009 has dataset create/read-by-ID but no collection discovery.
5. No public generator catalog exists; ADR-0010 identities are not frontend enumeration.

These are upstream dependencies, not frontend-owned inventions.

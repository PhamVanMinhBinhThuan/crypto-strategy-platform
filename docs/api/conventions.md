# API Conventions

## Base and versioning

- Public Backend REST resources use `/api/v1`; WebSocket endpoint is `/ws`.
- Breaking payload/semantic changes create a new API or event version.
- Exact public endpoints are added only after their feature specification is approved.

## HTTP semantics

| Operation | Expected behavior |
| --- | --- |
| Create resource/job | `201 Created` or `202 Accepted` with stable resource/job ID |
| Read resource | `200 OK`; response uses internal canonical model |
| Stop/cancel command | Idempotent command; repeated request does not create extra effects |
| Validation error | `400 Bad Request` with stable error code and field details |
| Resource not found | `404 Not Found`; do not expose internal table/class names |
| Dependency unavailable | `503 Service Unavailable` for request-path dependency failure |

## Data types

- Timestamp: ISO-8601 UTC, for example `2026-08-12T10:15:30.123Z`.
- Price, volume, capital, fee and financial metrics: decimal serialized as canonical strings when precision could be lost.
- IDs: opaque stable strings (UUID/ULID implementation is not part of public semantics).
- Enum values: `UPPER_SNAKE_CASE`; unknown values are rejected unless contract explicitly supports forward-compatible fallback.
- Pair/timeframe: canonical application representation such as `BTC/USDT` and `5m`.

## Pagination and filtering

- Collection endpoints use bounded page/cursor input; unbounded historical/result reads are not allowed.
- Sort field/direction and filter behavior are explicit per feature contract.
- Results with business order, such as Candles and Leaderboard, declare deterministic ordering.

## Validation and security

- Backend validates all user/provider input even if the UI already validates.
- Unknown fields on commands are rejected unless the versioned schema explicitly permits them.
- Provider payload, credential, stack trace and internal class/table names never appear in public responses.
- Request size, subscription count and rate limits are enforced at the application boundary.

## Correlation and idempotency

- Incoming requests may carry `X-Correlation-ID`; Backend creates one if absent and returns/propagates it.
- Job commands use a stable idempotency key or job ID; duplicate delivery cannot duplicate business results.
- Async messages carry message/event ID, version, occurredAt and correlation ID.

## Deprecation

- Additive compatible changes stay in the same version.
- Breaking changes introduce a new version and coexist for a documented transition period.
- Deprecated fields/endpoints remain documented until all known consumers migrate.


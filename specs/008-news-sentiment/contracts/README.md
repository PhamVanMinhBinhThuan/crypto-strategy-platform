# F-008 Contract Design

This directory defines the planned language-neutral `sentiment-v1` boundary. It is a design artifact, not runtime implementation. During implementation, one canonical copy of these schemas and fixtures will be placed in `modules/contracts` resources and loaded by both Java and Python tests.

## Boundary ownership

- `modules/news` owns semantic inference request/outcome ports but does not depend on HTTP DTOs.
- `modules/contracts` owns Java transport DTOs and canonical schema resources.
- `apps/worker` maps News values to/from the HTTP contract.
- `apps/sentiment` validates the same contract with Pydantic and shared fixtures.

## Routes

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/sentiment/analyze` | One normalized News inference |
| GET | `/health/live` | Process liveness |
| GET | `/health/ready` | Loaded artifact and inference readiness |

The Java-facing public and protected read boundaries are specified in `public-news-api.md`. Canonical Java normalization/hash input is specified in `canonical-news-v1.md`.

All objects are closed (`additionalProperties: false`). Requests contain no URL. The internal endpoint is not browser-accessible and may require an environment-supplied bearer token.

## Exact decimals

`confidence` and `polarityScore` are JSON strings. Canonical values have no exponent, plus sign, unnecessary leading/trailing zero, or negative zero, and at most ten fractional digits. Model output is converted through decimal text and rounded to scale 10 using half-even rounding; canonical rendering then strips insignificant fractional zeros. Java parses to `BigDecimal` and validates range.

## Failure classification

- Permanent: malformed/oversized request, unsupported language, unsupported expected contract/model/preprocessing release, authentication failure, or response identity/provenance mismatch.
- Transient: not ready, capacity/rate limit, timeout, connection failure, and eligible `5xx`.

Pre-dispatch readiness/circuit rejection consumes no attempt. Any analysis POST actually dispatched consumes one attempt, including a raced `503`.

## Fixture expansion during implementation

Add invalid fixtures for missing/null/extra/mistyped/oversized fields, malformed ULID/hash/UTC values, numeric rather than string scores, noncanonical/out-of-range decimals, unsupported language/release, every echoed-field mismatch, safe permanent/transient errors, and all health states. Both runtimes must execute exactly the same fixture inventory.

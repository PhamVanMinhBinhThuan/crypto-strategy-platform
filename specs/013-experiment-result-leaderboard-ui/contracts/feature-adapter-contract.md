# F-013 Feature Adapter Contract

Internal UI boundary only; F-009 OpenAPI remains authoritative.

```text
Page/component → feature hook/service → F-011 ApiClient/RealtimeClient
                                      → explicit mock adapter (test/dev only)
```

Components consume view models/callbacks and do not know the adapter.

## Services

- Result: distinct `readByResultId` and `readByBacktestId`; common result view model; never convert IDs or calculate metrics.
- Experiment: read Experiment/Jobs/Strategies; Start/Stop/Reproduce send one `Idempotency-Key` per logical command. Production Start/Reproduce pass through dependency unavailable and never synthesize acceptance. Unknown transport outcome remains distinguishable from rejection.
- Leaderboard: read with validated/capped limit and opaque cursor; retain response order; navigate with returned backtest result ID as `resultId`.

## Mapping rules

- Validate DTO shape before mapping.
- Keep prices, quantities, money, rates, metrics, scores, drawdown as authoritative strings. Any shortened display form is presentation-only; retain and expose the complete original string accessibly.
- Format timestamps for presentation while retaining original UTC instant.
- Collapse 403/404 reads to one inaccessible state.
- Expose only sanitized public code/message/retryability/correlation metadata plus safe normalized retry-delay metadata (`retryAfterSeconds` or equivalent) when supplied by the F-011 HTTP boundary.
- `401 AUTHENTICATION_REQUIRED` delegates to F-011 auth/session failure handling; feature adapters never manage tokens.
- `429 RATE_LIMIT_EXCEEDED` preserves safe snapshots and waits for normalized `Retry-After` eligibility before retry; adapters never issue a raw `fetch` bypass.
- Never expose provider, persistence, transport implementation, or Java internals.

## Fixture substitution

- Fixtures return identical view-model/result and public-error shapes.
- Scenario selection is explicit and deterministic.
- Composition requires F-011 non-production guard.
- Any production dependency graph containing fixture/testing imports fails architecture tests.

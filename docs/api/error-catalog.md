# Error Catalog

## Error response

```json
{
  "code": "INVALID_MARKET_QUERY",
  "message": "The market query is invalid.",
  "correlationId": "01J...",
  "details": [{ "field": "timeframe", "reason": "UNSUPPORTED_VALUE" }]
}
```

`message` is safe for clients; provider response, secret and stack trace remain in protected logs.

## Error groups

| Group | Codes | Handling |
| --- | --- | --- |
| Market | `INVALID_MARKET_QUERY`, `MARKET_PROVIDER_UNAVAILABLE`, `MARKET_PROVIDER_RATE_LIMITED`, `MARKET_DATA_GAP`, `MARKET_DATA_MAPPING_FAILED` | Validate, retry/backfill when transient, expose degraded state |
| Strategy | `STRATEGY_NOT_FOUND`, `STRATEGY_VERSION_UNAVAILABLE`, `INVALID_STRATEGY_PARAMETERS`, `INSUFFICIENT_STRATEGY_DATA` | Reject candidate/request; do not retry permanent validation |
| Experiment/Job | `EXPERIMENT_NOT_FOUND`, `INVALID_EXPERIMENT_STATE`, `JOB_NOT_FOUND`, `JOB_TIMEOUT`, `JOB_FAILED`, `SEARCH_LIMIT_REQUIRED` | Preserve status/error summary; retry only transient failures |
| Realtime | `INVALID_SUBSCRIPTION`, `SUBSCRIPTION_LIMIT_EXCEEDED`, `UNSUPPORTED_EVENT_VERSION`, `REALTIME_UNAVAILABLE` | Scope error to subscription when safe |
| News/Sentiment | `NEWS_PROVIDER_UNAVAILABLE`, `INVALID_NEWS_ITEM`, `SENTIMENT_UNAVAILABLE`, `INVALID_SENTIMENT_RESPONSE` | Keep News durable; analysis pending/failed without impacting Market |
| Platform | `DEPENDENCY_UNAVAILABLE`, `RATE_LIMITED`, `INTERNAL_ERROR` | Stable generic response, correlation ID for investigation |

## Retry classification

- Retry with bounded exponential backoff: timeout, rate limit and retryable dependency `5xx`.
- Do not retry: invalid input, missing immutable version, unsupported contract or permanent business validation.
- A Strategy producing zero trades is a valid result, not an error.
- Unknown worker exceptions retry only to configured limit, then move to Dead Letter/FAILED.

## Logging rules

- Log correlationId, experimentId, candidateId and jobId when available.
- Redact credentials, tokens, database URLs and provider secrets.
- Do not log full copyrighted News content unless explicitly required in a protected development environment.
- Public responses never contain internal exception type or stack trace.


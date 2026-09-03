# Contract: News and Sentiment UI

## REST

Browser adapter calls only `GET /api/v1/news-items` with supported `tradingPairId`, repeated
`analysisStatus`, bounded limit and cursor. `/internal/news-items/{newsId}/sentiment` is forbidden.

Pages remain newest-first as returned by server and append with `newsId` dedupe. A response can update
the view only when its query generation matches the current URL filters.

## Sentiment presentation

| Analysis status | Sentiment payload | Presentation |
| --- | --- | --- |
| `ANALYZED` | Required valid payload | Label, confidence, polarity and informational disclaimer |
| `PENDING`, `ANALYZING` | Ignored/null | Non-blocking analysis-in-progress state |
| `FAILED_RETRYABLE` | Ignored/null | Degraded state with bounded refresh action |
| `FAILED` | Ignored/null | Stable unavailable state; News stays readable |

Invalid combinations are treated as safe degraded contract errors and never inferred locally.
External article URLs require valid HTTP(S), open with safe browser protections and do not receive
session/token data. UI never phrases sentiment as buy/sell advice or guaranteed outcome.

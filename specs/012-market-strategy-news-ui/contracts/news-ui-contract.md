# Contract: News and Sentiment UI

## REST

Browser adapter chỉ gọi `GET /api/v1/news-items` với repeated `analysisStatus`, bounded limit và
cursor. Không gửi `tradingPairId` cho tới khi có public catalog/mapping; endpoint internal sentiment
luôn bị cấm.

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

UI chỉ render title/source/url/publishedAt/relatedAssetIds/analysisStatus và public sentiment
label/confidence/polarity. Content, summary, provenance, aggregate score/trend/topics và Strategy
integration trong prototype bị loại khỏi MVP và không được suy diễn từ page items.

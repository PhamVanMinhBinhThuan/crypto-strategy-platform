# Java News API Contract Plan

## Public News list

Retain authenticated `GET /news-items` with bounded `limit`, opaque cursor, and optional canonical Trading Pair filter. Sort by `publishedAt DESC, newsId DESC`. A pair matches News associated with its base or quote Asset; matching both still returns one item.

Each item exposes canonical public News fields, related-asset identifiers, and `analysisStatus`. When status is `ANALYZED`, an optional sentiment object may contain only:

```json
{
  "label": "POSITIVE",
  "confidence": "0.82",
  "polarityScore": "0.64"
}
```

For every other state, sentiment is absent/null and is never synthesized as `NEUTRAL`. Public output excludes content hash, model/preprocessing/contract versions, model name, analyzed timestamp, attempts, lease/retry data, internal failure detail and service location.

## Protected audit boundary

Plan an internal service-token/network-protected endpoint such as `GET /internal/api/v1/news-items/{newsId}/sentiment-results`. It may return result/News identities, language, content hash, label/scores, analyzed time, and release model name/version plus preprocessing/contract version. It must not be exposed to the browser. If browser operator access is later required, first specify an operator authorization policy; F-008 does not invent one.

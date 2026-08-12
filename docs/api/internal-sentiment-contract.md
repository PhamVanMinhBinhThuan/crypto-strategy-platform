# Internal Sentiment Contract

**Status**: Proposed
**Audience**: Java Worker and Python Sentiment Service

The internal endpoint is versioned and not exposed to the browser:

```text
POST /api/v1/sentiment/analyze
```

## Request

```json
{
  "requestId": "01J...",
  "newsId": "01J...",
  "title": "Bitcoin rises after institutional adoption",
  "content": "Normalized article text...",
  "language": "en",
  "contentHash": "sha256:..."
}
```

## Response

```json
{
  "requestId": "01J...",
  "newsId": "01J...",
  "label": "POSITIVE",
  "confidence": 0.82,
  "polarityScore": 0.64,
  "modelVersion": "crypto-sentiment-v1.0.0",
  "analyzedAt": "2026-08-12T10:00:00Z"
}
```

## Invariants

- Label is `POSITIVE`, `NEUTRAL` or `NEGATIVE`; confidence is `[0,1]`; polarity is `[-1,1]`.
- requestId/newsId are echoed for correlation; modelVersion and UTC timestamp are required.
- Java validates the response before persistence.
- Idempotency identity is newsId + contentHash + modelVersion.
- Python is stateless, does not fetch request URLs and has no PostgreSQL/Redis credentials.
- Payload length and optional batch size are bounded by environment configuration.
- Timeout/429/5xx may retry with limit; invalid 4xx does not retry.

## Health

- `GET /health/live`: process is alive.
- `GET /health/ready`: model is loaded and inference-ready.

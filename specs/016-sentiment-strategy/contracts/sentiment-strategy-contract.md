# Sentiment Strategy Contract

## Plugin descriptor

| Field | Value |
|---|---|
| Plugin ID | `sentiment-polarity` |
| Implementation version | `1.0.0` |
| Category | `SENTIMENT` |
| Required candle lookback | `1` |
| Supplemental input | `sentiment-polarity` / `sentiment-snapshot-v1` |

## Parameters

| Name | Type | Default | Validation |
|---|---|---:|---|
| `lookbackHours` | integer | 24 | `> 0` |
| `minimumArticles` | integer | 3 | `> 0` |
| `buyThreshold` | decimal | 0.25 | `0 < value <= 1` |
| `sellThreshold` | decimal | -0.25 | `-1 <= value < 0` |

Cross-field rule: `sellThreshold < buyThreshold`.

## Evaluation

1. Select observations published in `[evaluationTime - lookbackHours, evaluationTime]`.
2. Deduplicate by `newsId` and use canonical order (`publishedAt`, `newsId`).
3. Too few articles → HOLD / `INSUFFICIENT_SENTIMENT_ARTICLES`.
4. Zero confidence sum → HOLD / `ZERO_SENTIMENT_WEIGHT`.
5. Compute weighted polarity with scale 10 and `HALF_EVEN`.
6. Score `>= buyThreshold` → BUY / `SENTIMENT_BUY_THRESHOLD`.
7. Score `<= sellThreshold` → SELL / `SENTIMENT_SELL_THRESHOLD`.
8. Otherwise → HOLD / `SENTIMENT_BETWEEN_THRESHOLDS`.

## Required evidence

- score when computable, eligible article count and lookback
- BUY/SELL thresholds and model version
- snapshot ID/fingerprint and eligible-evidence fingerprint

Evaluation performs no network, database, model or wall-clock access.

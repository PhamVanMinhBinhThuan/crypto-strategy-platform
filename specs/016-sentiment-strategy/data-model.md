# Data Model: F-016 Sentiment Strategy

## SentimentStrategyDefinition

- `pluginId`: `sentiment-polarity`; version `1.0.0`; category `SENTIMENT`
- `lookbackHours`: positive integer, default `24`
- `minimumArticles`: positive integer, default `3`
- `buyThreshold`: decimal in `(0, 1]`, default `0.25`
- `sellThreshold`: decimal in `[-1, 0)`, default `-0.25`
- invariant: `sellThreshold < buyThreshold`

## SentimentObservationSnapshot

- `newsId`, `sentimentResultId`, `assetId`
- `publishedAt` for eligibility; `analyzedAt` for audit only
- `polarityScore`, `confidence`, `contentHash`
- invariant: unique `newsId`; all results use the snapshot release

## SentimentSnapshot

- `snapshotId`, schema `sentiment-snapshot-v1`, `assetId`
- `modelName`, `modelVersion`, `preprocessingVersion`
- inclusive `publicationCutoff`
- observations ordered by `publishedAt`, then `newsId`
- count, canonical SHA-256 `fingerprint`, audit `createdAt`
- invariant: immutable after creation and verified before execution

## StrategySupplementalInput

- `inputType`, `schemaVersion`, `snapshotId`, `fingerprint`, release metadata
- immutable ordered observations
- invariant: duplicate input type is rejected in one context

## SentimentAggregate

- `evaluationTime`, `windowStart`
- ordered eligible identities, `articleCount`, `totalConfidence`, optional `score`
- `evidenceFingerprint`
- invariant: future observations never enter the aggregate

## SentimentStrategyDecision

- existing `StrategyDecision` signal/reference/time/reason
- evidence: score, count, lookback, thresholds, model version, snapshot and evidence fingerprints

## ExperimentSentimentProvenance

- experiment, snapshot/schema/fingerprint, model/preprocessing release
- Strategy/Composite version and exact parameters
- retained for Result and reproduction comparison

## Lifecycle

1. News owns persisted source and inference records.
2. Start preflight creates an immutable asset/release/cutoff snapshot.
3. Manifest references it before jobs are accepted.
4. Worker verifies and injects it for every Candidate.
5. Result retains snapshot and decision evidence.
6. Reproduction resolves the original snapshot without overwriting artifacts.

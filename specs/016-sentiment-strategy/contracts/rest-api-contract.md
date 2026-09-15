# REST API Contract Additions

All routes remain authenticated and owner scoped. Additions are backward-compatible.

## Strategy catalog

`GET /api/v1/strategy-plugins` includes `sentiment-polarity`, the four typed parameters
`lookbackHours`, `minimumArticles`, `buyThreshold` and `sellThreshold`, plus optional
`requiresSupplementalInput: ["sentiment-polarity"]` metadata.

## Start Backtest/Search

If the selected frozen Strategy contains `sentiment-polarity`, the server creates a snapshot from the
Dataset base asset/end time before accepting execution. The browser never submits observations, model
credentials, a score or signal.

Snapshot preflight failure uses the existing safe error envelope with
`SENTIMENT_SNAPSHOT_UNAVAILABLE`; no partial Result is created.

## Result evidence

Result reads add optional `sentimentProvenance`:

```json
{
  "snapshotId": "...",
  "snapshotFingerprint": "sha256:...",
  "schemaVersion": "sentiment-snapshot-v1",
  "model": {
    "name": "...",
    "version": "...",
    "preprocessingVersion": "..."
  },
  "articleCount": 4,
  "evidenceFingerprint": "sha256:..."
}
```

Technical-only Results omit this object.

## News-to-Strategy action

The News UI links to `/strategies?strategy=sentiment-polarity` after confirming the catalog capability.
It does not create a second definition or calculate sentiment locally.

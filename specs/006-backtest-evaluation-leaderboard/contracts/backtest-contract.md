# Backtest Public Contract

## Purpose

Expose deterministic Backtest execution without leaking persistence, Worker, provider or concrete Strategy implementations.

## Required inputs

The run command carries typed references to:

- authenticated owner context at the application boundary;
- Experiment, Candidate, BACKTEST Job and Execution Attempt;
- frozen Experiment Manifest and its fingerprint;
- typed Backtest Assumptions parsed from the Manifest;
- Dataset provenance and Strategy provenance.

The command does not carry raw Candle arrays or untyped replacement IDs.

## Required collaborators

- F-003 Dataset lookup, integrity verification and `DatasetCandleReader`;
- F-004 Strategy registry and parameter-aware lookback resolver;
- F-004 Combination materializer for Composite provenance;
- F-005 read boundaries for Experiment/Candidate/Job/Attempt lineage;
- Backtesting output port for atomic Result/Trade acceptance.

## Execution behavior

1. Validate frozen provenance and successful lineage.
2. Resolve Single/Composite Strategy and exact lookback.
3. Read batches from sequence 0 with requested size `1..5000`.
4. Validate dataset identity, contiguous members, next sequence, `hasMore`, Candle order and final count.
5. Evaluate closed Candle context; queue the decision for next Candle open.
6. Apply long-only position state, adverse slippage, two-sided fee and forced final close.
7. Produce immutable ordered Trades and `backtest-v1` Result.

## Failures

The contract returns/throws stable domain error codes at minimum for:

- invalid/missing Dataset or provenance mismatch;
- invalid batch identity/progression/order/duplicate/checksum;
- Strategy version/parameters/reference/lookback mismatch;
- Composite policy/materialization mismatch;
- invalid assumptions;
- invalid Job/Attempt/Candidate/Experiment lineage;
- Attempt not `SUCCEEDED`;
- duplicate canonical Candidate outcome;
- persistence/concurrency conflict.

F-006 does not map these errors to retry policy or redefine `FailureClassification`; the caller/F-007 owns that decision.

## Determinism

Changing batch size must not change decisions, Trades, balances or fingerprint. Runtime timestamps, Worker identity and batch size are excluded from `backtest-v1` unless they affect business meaning.


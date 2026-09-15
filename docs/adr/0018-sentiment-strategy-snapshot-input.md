# ADR-0018: Frozen Sentiment snapshot as Strategy supplemental input

**Status:** Accepted
**Date:** 2026-09-09

## Context

ADR-0008 keeps inference behind News and requires future Sentiment strategies to consume normalized
data through `StrategyContext`. F-016 must run in Backtest, Search and Composite without look-ahead,
mutable model calls or direct cross-capability table access. The current context only carries Candles.

## Decision

1. `strategy-core` owns an immutable supplemental-input contract. `StrategyContext` accepts zero or
   more inputs and retains its existing constructor for technical strategies.
2. News owns immutable Sentiment snapshot construction from its persisted records. Persistence
   implements only News-owned public ports.
3. Runtime composition maps the News snapshot to Strategy input. Backtesting receives frozen data and
   does not depend directly on News.
4. Snapshot creation finishes before a Sentiment Backtest/Search is accepted. One Search run shares one
   snapshot/model release across Candidates.
5. Evaluation filters by `publishedAt <= evaluationTime`, performs no I/O and returns stable evidence.
   `analyzedAt` is audit-only.
6. Snapshot and eligible-observation fingerprints use canonical order and versioned SHA-256 contracts.
7. Technical execution does not create Sentiment input. Frozen replay remains available while Python
   Sentiment is down.

## Consequences

- Sentiment participates in existing Single/Composite/Search flows as a normal plugin.
- Runs remain explainable after new News arrives or the active model changes.
- Strategy Core gains a generic input abstraction but no News/persistence/framework dependency.
- API/Worker composition gains explicit mapping and preflight failure handling.
- Future external signals need their own versioned snapshots; mutable lookups from Strategy stay banned.

## Supersedes and extends

This ADR extends ADR-0008 and ADR-0009. It does not change the Sentiment deployment boundary or
immutable experiment principles.

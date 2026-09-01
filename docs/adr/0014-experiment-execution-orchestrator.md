# ADR-0014: Experiment Execution Orchestration Module

**Status**: Accepted
**Date**: 2026-09-01
**Owners**: Văn Minh, Nghi Văn

## Context and Problem Statement

During the integration of Backtest, Evaluation, and Leaderboard (F-006), it became evident that these capabilities operate on a sequential pipeline (Experiment -> Strategy -> Backtest -> Evaluation -> Leaderboard). However, placing the cross-capability orchestration logic in any one of the domain modules (`persistence` or `backtesting`) creates dependency cycles. Specifically, `persistence` must depend on all capability domain logic to implement data access interfaces, meaning no capability can depend back on `persistence` for orchestrating the overall flow.

## Decision

We introduce a dedicated orchestration layer called `modules/experiment-execution`.

This module acts as an Application Service layer that orchestrates the execution pipelines (such as `ReproduceExperimentExecutionService`) across boundaries. It depends on the public API of capability modules (e.g., `experiment`, `backtesting`, `evaluation`, `leaderboard`) but does NOT expose its internal implementations to them. `persistence`, `api`, and `worker` are allowed to depend on `experiment-execution` to wire its orchestrators and provide database adapters.

## Consequences

- **Pros:** Eliminates dependency cycles; isolates complex, cross-domain workflow orchestration away from pure domain capabilities; allows `worker` and `api` to remain thin.
- **Cons:** Introduces one more module into the Gradle build graph and ArchUnit module boundary matrices.

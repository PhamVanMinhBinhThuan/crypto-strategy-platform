# Search Coordination Contract: F-015

## Ownership

- `modules/search`: canonical Search space, candidate definition, generator contract/registry, pure deterministic generation.
- `modules/experiment-execution`: start, allocation, completion, stop, recovery, and reproduction orchestration through public owner ports.
- `modules/persistence`: atomic durable adapters and versioned JSON/SQL mapping.
- `apps/worker`: stream delivery, bounded executors, scheduling, and invocation of public orchestration ports; no direct SQL.

## Allocation invariant

For each run:

```text
terminalOutcomes = completed + failed
active = allocated - terminalOutcomes
0 <= active <= min(requestedConcurrency, configuredPerExperimentLimit)
allocated <= maximumCandidates
leaderboardSize <= topK
```

Top-K is excluded from the active-window formula.

Global admission additionally requires `globalActive < configuredGlobalLimit`. If global capacity is temporarily unavailable, the durable run remains refillable and reconciliation retries later.

## Refill decision

`TrustedSearchCoordinationUseCase.reconcileCompletion` and `reconcileRun` return a durable decision. When the decision is `FILL_AVAILABLE_SLOTS`, the caller invokes allocation using the reloaded authoritative run version/fence.

Refill may occur only when:

- status allows generation;
- no explicit stop/deadline/maximum/no-improvement/exhaustion decision has won;
- `allocated < maximumCandidates`;
- `active < targetWindow`;
- generator reports unexplored valid space.

Completion-triggered refill is the low-latency path. Scheduled reconciliation is the repair path and must produce the same semantic result idempotently.

## Atomic allocation

One allocation commit atomically persists:

- next versioned generator state;
- immutable candidate definition and fingerprint;
- logical Backtest job;
- coordination decision/audit record;
- outbox intent for the Backtest job;
- Search progress/version update.

A competing allocator with a stale version/fence must lose without emitting a job or advancing generator state.

## Candidate resolution

Backtest execution resolves the exact candidate definition through an owner-published execution input. For schema v2 it builds/loads the component strategies and combination policy frozen by the candidate. It never substitutes the manifest's first strategy or an empty parameter set.

Legacy schema-v1 candidates resolve their strategy identity and parameters through the existing manifest mapping.

## Determinism

The Random generator's next output is a pure function of:

```text
generator ID/version
+ canonical Search space fingerprint
+ seed
+ persisted generator state / logical generation index
```

Random Search does not consume completion-order observations. A candidate fingerprint includes semantic component identities/versions/parameters and policy but excludes runtime IDs and completion timestamps.

## Failure and retry

- Delivery retry creates no new candidate.
- Execution retry belongs to the same logical Backtest job/candidate.
- Terminal candidate failure increments `failed` once.
- A new generation index may be allocated after terminal failure only if the run remains active.
- Duplicate/stale completion is acknowledged idempotently and does not trigger excess allocation.

## Stop precedence

Existing F-010 authoritative timestamp/version rules continue. Before every allocation proposal and commit, reload/revalidate stop state. Once a terminal or stopping decision wins, later completions may settle progress but cannot reopen generation.

## Scale evidence assertions

- 100-candidate integration: exact unique count, full durable lifecycle, window bound, Top-K independence.
- 1,000-candidate performance: same seed/configuration at at least two worker capacities; compare throughput and prove identical candidate fingerprints/outcomes under deterministic fixtures.
- 10,000-candidate backpressure: peak active and pending delivery remain bounded by configured windows/batches; no in-memory collection proportional to the full candidate budget.


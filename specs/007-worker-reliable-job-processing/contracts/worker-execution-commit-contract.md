# Contract: Worker Backtest Prepare/Commit Seam

**Capability owners**:
- Backtest computation/persistence: F-006 `modules/backtesting`
- Evaluation: F-006 `modules/evaluation`
- Job/Attempt lifecycle: F-005 `modules/experiment`
- Cross-capability completion coordinator: `modules/experiment-execution`

**Consumer**: `apps/worker`

---

## 1. Problem Solved

Current F-006 Result persistence requires the referenced Attempt to be `SUCCEEDED`, while F-005 success finalization also transitions the parent Job to `SUCCEEDED`.

The Worker must not:
- persist a Result against a RUNNING Attempt;
- mark the Job durably SUCCEEDED before a Backtest computation that may still fail;
- copy F-006 engine logic into Worker.

---

## 2. F-006 Prepare Contract

Conceptually:

```java
public interface PrepareBacktestUseCase {
    PreparedBacktestOutcome prepare(BacktestRunCommand command);
}
```

`prepare(...)`:
- uses the same frozen Dataset/Strategy/business rules as existing Backtest execution;
- performs deterministic computation;
- does not persist `BacktestResult`/`Trade`;
- may take long enough that it must run outside the short completion transaction.

Existing `RunBacktestUseCase` may remain as the existing convenience API. The Worker path uses the new prepare seam.

---

## 3. F-006 Commit Contract

Conceptually:

```java
public interface CommitPreparedBacktestUseCase {
    BacktestResult commit(PreparedBacktestOutcome prepared);
}
```

It persists through the existing Backtest Result store/lineage validation. It does not bypass the requirement that the referenced Attempt is `SUCCEEDED`.

---

## 4. Cross-Capability Completion Contract

Conceptually:

```java
public interface CompleteBacktestAttemptUseCase {
    CompletedBacktestAttempt complete(
        JobId jobId,
        AttemptId attemptId,
        PreparedBacktestOutcome prepared,
        MetricVersion metricVersion,
        RankingVersion rankingVersion
    );
}
```

The implementation performs one short transaction:

```text
F-005 finalizeSuccess(Job, Attempt)
-> F-006 commit prepared BacktestResult
-> F-006 EvaluateBacktestUseCase
-> F-005 recordTerminalProgress(SUCCEEDED, evaluation.overallScore)
-> COMMIT
```

If any step fails, all steps roll back.

---

## 5. Crash Semantics

- crash during `prepare`: no Result/Evaluation committed; stale RUNNING Attempt recovery applies.
- crash during completion transaction: transaction rollback preserves recoverable pre-commit state.
- crash after completion commit but before Redis notification/processed marker:
  - Job/Attempt/Result/Evaluation/progress are durable;
  - no Backtest re-execution;
  - Leaderboard reconciliation repairs missing fast-path ranking;
  - processed marker can be repaired before XACK.

---

## 6. Boundary Rules

- Worker does not call internal Backtest engine classes.
- Worker does not manage JDBC transactions directly.
- `modules/experiment-execution` may coordinate capability APIs but must not reimplement Backtest/Evaluation/Leaderboard algorithms.
- Long Backtest computation is outside the DB completion transaction.

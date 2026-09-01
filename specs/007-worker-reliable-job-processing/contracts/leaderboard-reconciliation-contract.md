# Contract: Leaderboard Reconciliation Boundary

**Owner**: F-006 `modules/leaderboard`  
**Consumer**: F-007 scheduled Ranking/Leaderboard reconciliation  
**Persistence access**: capability-owned; never direct from `apps/worker`

---

## 1. Public Use Case

Conceptually:

```java
public interface LeaderboardReconciliationUseCase {

    LeaderboardRevision projectEvaluation(
        ExperimentId experimentId,
        EvaluationResultId evaluationResultId
    );

    int reconcileBatch(int experimentLimit);
}
```

Exact type names may follow the repository's F-006 API naming conventions. The semantics below are required.

---

## 2. Fast-Path Projection

`projectEvaluation(...)`:

1. loads the canonical durable EvaluationResult by `evaluationResultId`;
2. validates its Experiment;
3. loads the durable leaderboard-eligible Evaluation set needed for deterministic ranking;
4. delegates to the existing Top-K/`ProjectLeaderboardUseCase` logic;
5. returns the current/new durable Revision.

The transient `overallScore` from Redis is not business authority.

---

## 3. Reconciliation

`reconcileBatch(...)` performs a bounded durable scan through F-006-owned persistence ports and recomputes Top-K for candidate Experiments.

It MUST NOT define “unprojected” as “EvaluationResult has no LeaderboardEntry”.

A valid Evaluation outside Top-K may have no entry forever.

Correctness rule:

```text
load all eligible durable evaluations
-> deterministic Top-K
-> compare/project through existing F-006 fingerprint/idempotency behavior
-> new revision only when projection changes
```

Repeated no-op reconciliation is valid.

---

## 4. Idempotency

Duplicate `candidate.evaluated.v1` messages and reconciliation races must not produce duplicate logical Leaderboard effects.

The existing F-006 revision fingerprint/advisory locking behavior remains the final durable guard.

---

## 5. Scope

This contract adds integration access to existing F-006 ranking behavior. It does not move ranking formulas or Top-K business logic into Worker.

# Contract: Candidate Evaluated Message (`candidate.evaluated.v1`)

**Stream**: `candidate.evaluated.v1`  
**Producer**: Backtest Worker after durable Backtest + Evaluation completion  
**Consumer**: Ranking Handler  
**Module**: `modules/contracts`

---

## 1. Payload Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "CandidateEvaluatedPayload",
  "type": "object",
  "required": [
    "experimentId",
    "jobId",
    "candidateId",
    "backtestResultId",
    "evaluationResultId",
    "overallScore"
  ],
  "properties": {
    "experimentId": { "type": "string", "pattern": "^[0-9A-HJKMNP-TV-Z]{26}$" },
    "jobId": { "type": "string", "pattern": "^[0-9A-HJKMNP-TV-Z]{26}$" },
    "candidateId": { "type": "string", "pattern": "^[0-9A-HJKMNP-TV-Z]{26}$" },
    "backtestResultId": { "type": "string", "pattern": "^[0-9A-HJKMNP-TV-Z]{26}$" },
    "evaluationResultId": { "type": "string", "pattern": "^[0-9A-HJKMNP-TV-Z]{26}$" },
    "overallScore": {
      "type": "number",
      "description": "Non-authoritative fast-path hint copied from the durable EvaluationResult."
    }
  },
  "additionalProperties": true
}
```

---

## 2. Java DTO

```java
package com.cryptostrategy.platform.contracts.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CandidateEvaluatedPayload(
    @JsonProperty("experimentId") String experimentId,
    @JsonProperty("jobId") String jobId,
    @JsonProperty("candidateId") String candidateId,
    @JsonProperty("backtestResultId") String backtestResultId,
    @JsonProperty("evaluationResultId") String evaluationResultId,
    @JsonProperty("overallScore") BigDecimal overallScore
) {
    public CandidateEvaluatedPayload {
        Objects.requireNonNull(experimentId);
        Objects.requireNonNull(jobId);
        Objects.requireNonNull(candidateId);
        Objects.requireNonNull(backtestResultId);
        Objects.requireNonNull(evaluationResultId);
        Objects.requireNonNull(overallScore);
    }
}
```

---

## 3. Reliability Invariants

1. The message is a transient fast-path notification, not durable ranking truth.
2. Ranking MUST resolve the canonical durable EvaluationResult using `evaluationResultId` through an F-006 public boundary.
3. `overallScore` is a hint/diagnostic value. If it differs from the durable EvaluationResult, durable state wins and the mismatch is logged safely.
4. Lost messages are repaired by Leaderboard reconciliation from durable Evaluation state.
5. Duplicate messages do not create duplicate logical Leaderboard effects.
6. A `SUCCEEDED` Backtest Job is the expected prerequisite for ranking; it is not a reason to skip projection.

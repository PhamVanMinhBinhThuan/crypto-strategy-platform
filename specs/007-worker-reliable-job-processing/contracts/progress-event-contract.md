# Contract: Progress Event (`progress.events.v1`)

**Channel**: transient Redis Stream `progress.events.v1`  
**Producer**: `apps/worker`  
**F-007 Consumer**: none  
**Future Consumer**: F-009 API/WebSocket gateway  
**Module**: `modules/contracts`

This is a cross-process notification boundary. It is not a Spring JVM-local `ApplicationEvent`.

---

## 1. Payload Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "ProgressEventPayload",
  "type": "object",
  "required": [
    "experimentId",
    "jobId",
    "completedWork",
    "failedWork",
    "totalWork",
    "eventType"
  ],
  "properties": {
    "experimentId": { "type": "string", "pattern": "^[0-9A-HJKMNP-TV-Z]{26}$" },
    "jobId": { "type": "string", "pattern": "^[0-9A-HJKMNP-TV-Z]{26}$" },
    "completedWork": { "type": "integer", "minimum": 0 },
    "failedWork": { "type": "integer", "minimum": 0 },
    "totalWork": { "type": "integer", "minimum": 0 },
    "bestScore": { "type": ["number", "null"] },
    "leaderboardRevisionId": { "type": ["string", "null"] },
    "eventType": {
      "type": "string",
      "enum": [
        "EXPERIMENT_PROGRESS_UPDATED",
        "BACKTEST_COMPLETED",
        "LEADERBOARD_UPDATED"
      ]
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
public record ProgressEventPayload(
    @JsonProperty("experimentId") String experimentId,
    @JsonProperty("jobId") String jobId,
    @JsonProperty("completedWork") int completedWork,
    @JsonProperty("failedWork") int failedWork,
    @JsonProperty("totalWork") int totalWork,
    @JsonProperty("bestScore") BigDecimal bestScore,
    @JsonProperty("leaderboardRevisionId") String leaderboardRevisionId,
    @JsonProperty("eventType") String eventType
) {
    public ProgressEventPayload {
        Objects.requireNonNull(experimentId);
        Objects.requireNonNull(jobId);
        Objects.requireNonNull(eventType);
    }
}
```

---

## 3. Invariants

1. **Transient notification only**: loss of `progress.events.v1` must not corrupt durable progress.
2. **Durable truth**: progress/count/score values originate from F-005/F-006 durable state.
3. **No double count**: Ranking does not increment Backtest completion counters.
4. **Future F-009**: F-009 maps these notifications to public WebSocket frames; F-007 does not implement WebSocket sessions.
5. **Cross-JVM**: the contract is serialized through Redis so separate Worker and API processes can communicate.

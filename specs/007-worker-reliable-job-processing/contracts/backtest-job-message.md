# Contract: Backtest Job Message (`backtest.jobs.v1`)

**Stream**: `backtest.jobs.v1`  
**Producer**: Outbox Publisher from F-005 BACKTEST `JobQueued`  
**Consumer**: Backtest Worker Group  
**Module**: `modules/contracts`

---

## 1. Payload Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "BacktestJobPayload",
  "type": "object",
  "required": [
    "experimentId",
    "jobId",
    "candidateId"
  ],
  "properties": {
    "experimentId": { "type": "string", "pattern": "^[0-9A-HJKMNP-TV-Z]{26}$" },
    "jobId": { "type": "string", "pattern": "^[0-9A-HJKMNP-TV-Z]{26}$" },
    "candidateId": { "type": "string", "pattern": "^[0-9A-HJKMNP-TV-Z]{26}$" }
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
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BacktestJobPayload(
    @JsonProperty("experimentId") String experimentId,
    @JsonProperty("jobId") String jobId,
    @JsonProperty("candidateId") String candidateId
) {
    public BacktestJobPayload {
        Objects.requireNonNull(experimentId);
        Objects.requireNonNull(jobId);
        Objects.requireNonNull(candidateId);
    }
}
```

---

## 3. Data and Security Boundaries

- no `ownerUserId`, JWT, credential, or session key;
- no Candle payload;
- no Strategy code/parameters;
- no Trade list;
- Worker resolves ownership and frozen execution through F-005;
- Dataset/Strategy provenance is loaded from durable frozen state;
- the message is routing/reference data only.

# Contract: Redis Message Envelope

**Scope**: all F-007 Redis messages  
**Module**: `modules/contracts`  
**Package**: `com.cryptostrategy.platform.contracts.api`

---

## 1. JSON Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "RedisMessageEnvelope",
  "type": "object",
  "required": [
    "messageId",
    "messageVersion",
    "messageType",
    "occurredAt",
    "correlationId",
    "payload"
  ],
  "properties": {
    "messageId": {
      "type": "string",
      "pattern": "^[0-9A-HJKMNP-TV-Z]{26}$"
    },
    "messageVersion": {
      "type": "integer",
      "minimum": 1
    },
    "messageType": {
      "type": "string",
      "pattern": "^[A-Z0-9_]+$"
    },
    "occurredAt": {
      "type": "string",
      "format": "date-time"
    },
    "correlationId": {
      "type": "string",
      "minLength": 1
    },
    "payload": {
      "type": "object"
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
import java.time.Instant;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MessageEnvelope<T>(
    @JsonProperty("messageId") String messageId,
    @JsonProperty("messageVersion") int messageVersion,
    @JsonProperty("messageType") String messageType,
    @JsonProperty("occurredAt") Instant occurredAt,
    @JsonProperty("correlationId") String correlationId,
    @JsonProperty("payload") T payload
) {
    public MessageEnvelope {
        Objects.requireNonNull(messageId);
        Objects.requireNonNull(messageType);
        Objects.requireNonNull(occurredAt);
        Objects.requireNonNull(correlationId);
        Objects.requireNonNull(payload);
        if (messageVersion < 1) throw new IllegalArgumentException("messageVersion");
    }
}
```

---

## 3. Compatibility Rules

- UTF-8 JSON.
- Consumers ignore unknown **optional** properties.
- Adding an optional property is non-breaking when old consumers can ignore it safely.
- Removing/renaming a required property, changing its type/meaning, or changing required semantics is breaking and requires a new `messageVersion` and stream version where applicable.
- Unknown message versions are rejected safely and must not create an infinite retry loop.
- Message contracts never carry credentials, raw SQL errors, stack traces, or Java serialized objects.

The schema intentionally allows additional properties so the compatibility rule is not contradicted by `additionalProperties: false`.

# Contract: Search Requests Reservation (`search.requests.v1`)

**Stream**: `search.requests.v1`  
**Status**: RESERVED / FUTURE SCOPE  
**Module**: `modules/contracts`

F-007 does not create a Search Coordinator consumer group and does not start Search execution from this stream.

---

## 1. Reserved Payload

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "SearchRequestPayload",
  "type": "object",
  "required": [
    "experimentId",
    "searchJobId"
  ],
  "properties": {
    "experimentId": { "type": "string", "pattern": "^[0-9A-HJKMNP-TV-Z]{26}$" },
    "searchJobId": { "type": "string", "pattern": "^[0-9A-HJKMNP-TV-Z]{26}$" }
  },
  "additionalProperties": true
}
```

Unknown optional properties may be ignored for forward compatibility. Breaking changes require a new version.

---

## 2. F-007 Boundary

- no Consumer Group on `search.requests.v1`;
- no Search Coordinator;
- no `RandomStrategyGenerator`;
- no StrategyGenerator registry implementation;
- no Search stop-condition execution.

F-005 SEARCH `JobQueued` events are treated as lifecycle notifications in F-007 and do not launch Search work.

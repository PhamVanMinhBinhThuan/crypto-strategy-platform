# Contract: Dead-Letter Diagnostic Message (`jobs.dead-letter.v1`)

**Stream**: `jobs.dead-letter.v1`  
**Producer**: Worker / Retry Orchestrator  
**Consumer**: operator/recovery tooling  
**Module**: `modules/contracts`

The stream is a best-effort diagnostic projection. Durable `experiment.job.status = FAILED` plus safe failure metadata is authoritative.

---

## 1. Payload Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "DeadLetterPayload",
  "type": "object",
  "required": [
    "experimentId",
    "jobId",
    "messageId",
    "failureClassification",
    "failureCode"
  ],
  "properties": {
    "experimentId": { "type": "string", "pattern": "^[0-9A-HJKMNP-TV-Z]{26}$" },
    "jobId": { "type": "string", "pattern": "^[0-9A-HJKMNP-TV-Z]{26}$" },
    "candidateId": { "type": ["string", "null"], "pattern": "^[0-9A-HJKMNP-TV-Z]{26}$" },
    "messageId": { "type": "string", "pattern": "^[0-9A-HJKMNP-TV-Z]{26}$" },
    "failureClassification": { "type": "string" },
    "failureCode": { "type": "string" },
    "safeDiagnosticReference": { "type": ["string", "null"] },
    "attemptCount": { "type": "integer", "minimum": 1 },
    "failedAt": { "type": "string", "format": "date-time" }
  },
  "additionalProperties": true
}
```

---

## 2. Invariants

- no stack traces;
- no credentials/secrets;
- no raw SQL error text;
- no internal Java class names;
- no user personal data;
- loss of this Redis message does not change durable failure truth;
- a diagnostic message may be regenerated from a durable FAILED Job;
- one dead-lettered Candidate does not stop other Jobs in the Experiment.

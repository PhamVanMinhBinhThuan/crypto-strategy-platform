# Contract: Search Requests v1 (`search.requests.v1`)

**Stream**: `search.requests.v1`  
**Status**: ACTIVE — F-010 Search Coordinator
**Module**: `modules/contracts`

F-007 đã reserve contract; F-010 kích hoạt consumer group `search-coordinators` mà không thay đổi
hai field bắt buộc cũ. Search Run/Manifest trong PostgreSQL vẫn là authority cho configuration.

---

## 1. Payload tương thích ngược

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
    "searchJobId": { "type": "string", "pattern": "^[0-9A-HJKMNP-TV-Z]{26}$" },
    "concurrencyHint": { "type": "integer", "minimum": 1, "maximum": 64, "default": 1 },
    "topKTarget": { "type": "integer", "minimum": 1, "maximum": 1000, "default": 10 }
  },
  "additionalProperties": true
}
```

`concurrencyHint` và `topKTarget` là optional để message F-007 đã reserve vẫn đọc được; khi vắng mặt,
consumer dùng default trong schema. Nếu được gửi, chúng phải là positive bounded hints. Unknown
optional properties được bỏ qua; breaking change phải dùng version mới.

---

## 2. Envelope và delivery

- Envelope dùng `messageType = SEARCH_REQUEST`, `messageVersion = 1`, `messageId`, `occurredAt` và
  `correlationId` theo contract chung.
- F-010 dùng group `search-coordinators`; không dùng `ranking-workers`.
- ACK chỉ sau durable transition hoặc khi message được chứng minh malformed/irrelevant/terminal.
- Redis chỉ là delivery layer; queue loss được phục hồi từ Outbox và durable Search state.

F-007 không tự chạy Search; runtime chỉ được kích hoạt bởi F-010 sau khi các readiness gate tương ứng
có evidence thật.

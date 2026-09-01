# Contract lỗi F-009

## REST envelope

```json
{
  "code": "REQUEST_VALIDATION_FAILED",
  "message": "The request is invalid.",
  "details": {"fieldErrors": [{"field": "limit", "reason": "must be between 1 and 200"}]},
  "correlationId": "01J...",
  "timestamp": "2026-09-01T00:00:00Z"
}
```

Mã là stable `UPPER_SNAKE_CASE`; client rẽ nhánh theo code, không theo message. Message
không chứa secret, stack trace, SQL, path, provider response hoặc internal class.

## WebSocket error

`SUBSCRIPTION_ERROR` giữ `code`, `message`, `details`, `retryable` và đúng
`subscriptionId`; lỗi một subscription không đóng connection nếu có thể cô lập.

## Phân loại

Validation/auth/not-found/state/idempotency/rate-limit mapping phải khớp
`docs/api/error-catalog.md`. Dependency/timeout dùng retry classification rõ; client không
retry vô hạn và POST không retry với key mới nếu chưa xác định outcome cũ.

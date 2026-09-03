# Data Model: Web Foundation and Authentication

F-011 không tạo business database table.

| Concept | Data/rule chính |
|---|---|
| Auth session | User ID, email, expiry, state; không đưa password/refresh token vào business state |
| Safe redirect | Chỉ internal path; từ chối external/protocol-relative URL |
| Request result | Data hoặc safe normalized error cùng correlation ID |
| Realtime state | Connection state và subscriptions cần khôi phục |
| UI state | Loading, success, empty, error, degraded |
| Fixture mode | Explicit dev/test flag; cấm production default |

```text
UNAUTHENTICATED -> SIGNING_UP -> PENDING_EMAIL_VERIFICATION -> UNAUTHENTICATED
UNAUTHENTICATED -> SIGNING_IN -> AUTHENTICATED -> REFRESHING -> AUTHENTICATED
AUTHENTICATED -> SIGNING_OUT -> UNAUTHENTICATED
UNAUTHENTICATED -> RECOVERY_REQUESTED -> RESETTING_PASSWORD -> UNAUTHENTICATED
```


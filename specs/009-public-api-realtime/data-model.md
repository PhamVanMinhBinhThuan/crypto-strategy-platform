# Data model F-009

F-009 chỉ mô tả các representation và lifecycle cần cho public boundary. Các bản ghi
nghiệp vụ vẫn thuộc capability owner; API không tạo bản sao durable ngoài idempotency
receipt/ticket state cần thiết cho boundary.

## Authenticated Session Context

| Field | Quy tắc |
|---|---|
| `userId` | UUID từ identity đã xác thực; không nhận quyền từ request body |
| `correlationId` | Opaque value, nhận hoặc sinh mới, đi xuyên request/event |
| `origin` | Được kiểm tra với allowlist ở WebSocket handshake |
| `authenticatedAt` | UTC instant; chỉ dùng cho policy/observability |

## Public Resource Representation

Các representation REST phải giữ identity typed/opaque, exact decimal string, UTC time
và chỉ chứa field cần cho client. Resource map gồm Dataset/Candle, Strategy, User Strategy,
Experiment, Candidate, Job, Backtest Result, Leaderboard và News. Ownership không được
đưa thành field có thể giả mạo; nó được suy ra từ authenticated context và parent chain.

## Idempotent Command Receipt

| Field | Quy tắc |
|---|---|
| `ownerId` | UUID scope của user |
| `operation` | Tên operation ổn định, ví dụ `START_BACKTEST` |
| `idempotencyKey` | Client key bounded theo contract |
| `canonicalRequestHash` | Hash của request sau canonicalization |
| `outcomeReference` | Resource/Job reference của lần xử lý đầu |
| `state` | `IN_PROGRESS`, `COMPLETED`, hoặc `FAILED` |

Cùng `(ownerId, operation, idempotencyKey)` phải có cùng hash; khác hash là conflict.
Receipt không thay thế durable Job/Experiment state.

## WebSocket Connection và Logical Subscription

| Entity | Quan hệ/lifecycle |
|---|---|
| `RealtimeConnection` | Một authenticated browser tab; `CONNECTING → ACTIVE → CLOSED` |
| `Subscription` | Thuộc một connection; `REQUESTED → ACTIVE → INACTIVE/FAILED` |
| `SubscriptionKind` | `CANDLES`, `EXPERIMENT`, hoặc `LEADERBOARD` |
| `SyncMarker` | Boundary dùng ghép snapshot với event; opaque với client ngoài contract |
| `EventEnvelope` | Event identity/version/time/correlation/subscription/payload |

Một connection tối đa bốn Candle subscriptions và giới hạn workload cấu hình. Unsubscribe,
expiry hoặc connection close phải giải phóng toàn bộ logical subscriptions thuộc connection.

## Public Error

`code`, `message`, `details`, `correlationId`, `timestamp` là bắt buộc. `details` có thể
chứa field errors, resource type/id, current state, allowed states, retryable và retry-after;
không chứa secret, stack trace, SQL, path hoặc provider payload.

## Invariants

- Collection cursor không được lặp/bỏ item khi source snapshot không đổi.
- Experiment/Candidate/Job/Result/Leaderboard phải cùng owner qua parent chain.
- Published Strategy, Manifest, Result, Trade, Evaluation và Leaderboard revision chỉ đọc.
- Reproduction tạo resource identity mới và giữ nguyên evidence gốc.
- Event duplicate/stale không tạo business mutation; terminal state/revision vẫn đọc được qua REST.

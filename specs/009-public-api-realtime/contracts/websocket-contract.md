# Contract WebSocket F-009

## Connection và envelope

Endpoint `/ws`, UTF-8 JSON, một connection mỗi browser tab. Handshake cần one-time ticket
ngắn hạn và Origin allowlist. Ticket do authenticated REST cấp, gắn user/Origin/JWT expiry,
chỉ consume một lần và không được log. Envelope bắt buộc: `eventType`, `eventVersion`, `eventId`,
`occurredAt` UTC, `correlationId`, `subscriptionId`, `payload`.

MVP không nhận reauthentication command. Connection đóng bằng code `4001`
`REAUTHENTICATION_REQUIRED` tại thời điểm sớm hơn giữa JWT expiry và maximum connection
lifetime; client refresh session, xin ticket mới, reconnect, resubscribe và đọc REST snapshot.

## Commands/events

Commands: `SUBSCRIBE_CANDLES`, `UNSUBSCRIBE_CANDLES`, `SUBSCRIBE_EXPERIMENT`,
`UNSUBSCRIBE_EXPERIMENT`, `SUBSCRIBE_LEADERBOARD`, `UNSUBSCRIBE_LEADERBOARD`, `PING`.

Events: `SUBSCRIPTION_CONFIRMED`, `CANDLE_UPDATED`, `MARKET_CONNECTION_STATUS_CHANGED`,
`EXPERIMENT_PROGRESS_UPDATED`, `BACKTEST_COMPLETED`, `LEADERBOARD_UPDATED`,
`SUBSCRIPTION_ERROR`, `PONG`.

Không nhận command tạo/stop/cancel/publish/archive/reproduce business state qua WebSocket.

## Sequencing và recovery

Subscription confirmation `ACTIVE` gắn synchronization marker/boundary opaque. Snapshot REST và events
được ghép theo marker; event duplicate/stale bị bỏ qua theo `eventId`, Candle identity,
hoặc Leaderboard revision. Sau reconnect client resubscribe và đọc REST để backfill; server
không cam kết exactly-once.

Update Candle đang mở có thể coalesce; Candle close, connection status, completion, terminal
state và latest Leaderboard revision phải recoverable.

## Limits

Mặc định bốn Candle subscriptions, bốn workload subscriptions, 64 KiB/message,
30 commands/10 giây/connection, heartbeat 30 giây và timeout 90 giây; cấu hình được ghi
trong deployment contract và test bằng clock/scheduler kiểm soát được.

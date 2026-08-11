# ADR-0004: Sử dụng WebSocket cho dữ liệu Realtime

**Status**: Proposed
**Date**: 2026-08-11
**Owners**: Tiến Luật

## Context

Crypto StrategyLab cần cập nhật giao diện mà không reload toàn trang:

- tối đa bốn Candlestick Chart với pair/timeframe độc lập;
- Candle đang hình thành và Candle vừa đóng;
- trạng thái kết nối Market Data Provider;
- tiến trình Random Search và Backtest;
- Top-K Leaderboard khi có candidate tốt hơn.

Polling liên tục từ Frontend tạo nhiều HTTP request trùng lặp, tăng độ trễ và khó đồng bộ bốn chart. Cho phép Frontend kết nối trực tiếp Binance lại làm rò rỉ provider contract và vi phạm [ADR-0003: Market Data Provider Adapter](0003-market-data-adapter.md).

Hệ thống cần một kênh server-push có thể multiplex nhiều subscription trên cùng kết nối, quản lý reconnect và giữ Frontend chỉ phụ thuộc contract của Crypto StrategyLab.

## Decision

### 1. Sử dụng native WebSocket với JSON protocol

Backend Spring Boot cung cấp một WebSocket endpoint của ứng dụng:

```text
/ws
```

Frontend sử dụng WebSocket API chuẩn của trình duyệt. MVP không dùng STOMP hoặc SockJS. Message được truyền bằng JSON với protocol do hệ thống định nghĩa và version hóa.

Historical Candle vẫn được tải bằng REST trước. WebSocket chỉ truyền update phát sinh sau đó và các event realtime khác.

```text
Frontend
   ├── REST: lấy historical candles / trạng thái ban đầu
   └── WebSocket: candle update / progress / leaderboard update
```

### 2. Một kết nối, nhiều logical subscription

Mỗi browser tab duy trì một WebSocket connection đến Backend. Bên trong kết nối đó có nhiều logical subscription thay vì mở một WebSocket cho mỗi chart.

Mỗi subscription có `subscriptionId` do Frontend tạo. Với Market Chart, subscription còn chứa:

```text
pair + timeframe
```

MVP giới hạn tối đa bốn Market subscription hoạt động trên một client connection, tương ứng tối đa bốn chart.

Ví dụ Chart 1 đổi từ `5m` sang `1h`:

1. Frontend gửi `UNSUBSCRIBE` cho subscription cũ của Chart 1.
2. Frontend gửi `SUBSCRIBE` mới với cùng chart/subscription identity và timeframe `1h`.
3. Backend chỉ đổi stream của Chart 1.
4. Chart 2–4 giữ nguyên dữ liệu và subscription.
5. Toàn bộ Dashboard không reload.

Backend được phép chia sẻ một upstream Binance stream cho nhiều client cùng subscribe một `pair + timeframe`. Stream được đóng khi không còn subscriber.

### 3. Message envelope

Mọi command và event dùng envelope chung:

```json
{
  "eventType": "CANDLE_UPDATED",
  "eventVersion": 1,
  "eventId": "01J...",
  "occurredAt": "2026-08-11T10:15:30.123Z",
  "correlationId": "01J...",
  "subscriptionId": "chart-1",
  "payload": {}
}
```

Quy tắc:

- `eventType`: tên command/event dạng `UPPER_SNAKE_CASE`;
- `eventVersion`: số nguyên tăng khi payload có breaking change;
- `eventId`: định danh duy nhất để deduplicate;
- `occurredAt`: ISO-8601 UTC;
- `correlationId`: liên kết request, job và event trong log;
- `subscriptionId`: định tuyến event đến đúng chart/job trên Frontend;
- `payload`: dữ liệu cụ thể, không chứa model riêng của Binance.

Chi tiết field cuối cùng được ghi trong [WebSocket Events](../api/websocket-events.md). ADR này quyết định cấu trúc và nguyên tắc protocol.

### 4. Client commands

WebSocket protocol hỗ trợ tối thiểu:

| Command | Mục đích |
|---|---|
| `SUBSCRIBE_CANDLES` | Theo dõi Candle theo pair/timeframe |
| `UNSUBSCRIBE_CANDLES` | Dừng subscription của một chart |
| `SUBSCRIBE_EXPERIMENT` | Theo dõi tiến trình Search/Backtest theo Experiment ID |
| `UNSUBSCRIBE_EXPERIMENT` | Dừng nhận event của Experiment |
| `SUBSCRIBE_LEADERBOARD` | Theo dõi Top-K theo leaderboard/search context |
| `UNSUBSCRIBE_LEADERBOARD` | Dừng nhận Leaderboard update |
| `PING` | Kiểm tra connection còn hoạt động |

Ví dụ subscribe chart:

```json
{
  "eventType": "SUBSCRIBE_CANDLES",
  "eventVersion": 1,
  "eventId": "01JCLIENT...",
  "occurredAt": "2026-08-11T10:15:00Z",
  "correlationId": "01JCORR...",
  "subscriptionId": "chart-1",
  "payload": {
    "pair": "BTC/USDT",
    "timeframe": "5m"
  }
}
```

Command làm thay đổi job như `START_SEARCH` hoặc `STOP_SEARCH` không đi qua WebSocket trong MVP. Frontend gọi REST API để tạo/dừng job, sau đó dùng WebSocket để theo dõi tiến trình. Cách này giữ command validation và HTTP status rõ ràng.

### 5. Server events

Backend phát tối thiểu các event:

| Event | Mục đích |
|---|---|
| `SUBSCRIPTION_CONFIRMED` | Xác nhận subscription đã hoạt động |
| `CANDLE_UPDATED` | Cập nhật nến đang mở hoặc nến đã đóng |
| `MARKET_CONNECTION_STATUS_CHANGED` | Báo trạng thái provider |
| `EXPERIMENT_PROGRESS_UPDATED` | Số candidate, bước hiện tại, thời gian và lỗi |
| `BACKTEST_COMPLETED` | Báo Backtest hoàn thành và ID kết quả |
| `LEADERBOARD_UPDATED` | Báo Top-K mới mà không reload trang |
| `SUBSCRIPTION_ERROR` | Lỗi chỉ thuộc một subscription |
| `PONG` | Phản hồi heartbeat |

`CANDLE_UPDATED.payload` sử dụng canonical Candle của [ADR-0003: Market Data Provider Adapter](0003-market-data-adapter.md), bao gồm `pair`, `timeframe`, OHLCV, `openTime`, `closeTime` và `closed`.

Event tiến trình Search/Backtest chỉ chứa dữ liệu cần cập nhật UI. Kết quả đầy đủ, trades và lịch sử Leaderboard được tải qua REST bằng ID trong event.

### 6. Ordering và deduplication

WebSocket giữ thứ tự message trong một connection, nhưng reconnect hoặc nhiều nguồn nội bộ vẫn có thể tạo duplicate/stale update.

Quy tắc xử lý:

- Frontend deduplicate theo `eventId` trong phạm vi cửa sổ gần nhất;
- Candle được nhận diện bằng `provider + pair + timeframe + openTime`;
- update mới hơn cho cùng Candle thay thế update cũ;
- Candle có `closed = true` là trạng thái cuối của khoảng nến đó;
- Frontend bỏ qua update cũ hơn trạng thái Candle hiện có;
- Leaderboard event mang revision/version để Frontend không áp dụng bản cũ;
- Event không được phụ thuộc vào delivery exactly-once.

### 7. Reconnect và phục hồi dữ liệu

Frontend quản lý các trạng thái:

```text
CONNECTING → CONNECTED → RECONNECTING → DISCONNECTED
```

Khi connection bị đóng ngoài ý muốn:

1. Frontend reconnect bằng exponential backoff có jitter và giới hạn tối đa.
2. UI hiển thị trạng thái `RECONNECTING`, không giả vờ dữ liệu vẫn realtime.
3. Sau khi kết nối lại, Frontend gửi lại toàn bộ active subscriptions.
4. Frontend gọi REST lấy candles từ mốc cuối đã biết để lấp gap.
5. Backend/Market Adapter cũng thực hiện provider reconnect và gap recovery theo ADR-0003.
6. Event realtime tiếp tục sau khi historical gap đã được hợp nhất và deduplicate.

Nếu retry vượt giới hạn, UI chuyển `DISCONNECTED` và cung cấp hành động thử lại thủ công.

### 8. Heartbeat và cleanup

- Client gửi `PING` định kỳ hoặc sử dụng WebSocket ping/pong nếu framework hỗ trợ phù hợp.
- Backend đóng connection không phản hồi sau timeout đã cấu hình.
- Khi browser tab đóng, Backend hủy toàn bộ logical subscriptions của connection đó.
- `UNSUBSCRIBE` phải giải phóng subscriber ngay cả khi upstream stream đang được client khác dùng chung.
- Timeout và interval cụ thể được cấu hình, không hard-code trong UI component.

### 9. Backpressure và hiệu năng UI

Nếu update đến nhanh hơn khả năng render:

- Backend có outbound buffer giới hạn;
- các update trung gian của cùng một Candle đang mở có thể được coalesce, chỉ giữ bản mới nhất;
- không được bỏ Candle close event, connection status hoặc job completion event;
- Frontend cập nhật chart theo batch/render frame thay vì render lại toàn bộ page cho từng tick;
- payload không gửi lại toàn bộ historical dataset ở mỗi event.

Các ngưỡng buffer/throttle cụ thể sẽ được xác định bằng đo đạc trong feature plan, không cố định trong ADR.

### 10. Security và giới hạn

MVP chưa có authentication phức tạp và WebSocket chỉ cung cấp dữ liệu đọc công khai hoặc trạng thái job demo. Tuy vậy Backend vẫn phải:

- giới hạn Origin theo cấu hình;
- validate `eventType`, version, pair, timeframe, ID và kích thước payload;
- giới hạn số subscription trên mỗi connection;
- rate-limit command subscribe/unsubscribe;
- không gửi stack trace, Binance credential hoặc internal error detail;
- không dùng WebSocket cho giao dịch tiền thật.

Nếu sau này có user/private experiment, authentication và authorization phải được bổ sung trước khi phát dữ liệu theo user.

## Alternatives Considered

- **HTTP Polling**: Dễ triển khai nhưng tạo nhiều request, độ trễ cao và kém hiệu quả với bốn chart cùng Search progress.
- **Server-Sent Events (SSE)**: Phù hợp server-push một chiều nhưng subscribe/unsubscribe nhiều chart phải kết hợp thêm HTTP command; WebSocket đơn giản hơn cho logical subscription hai chiều.
- **Frontend kết nối trực tiếp Binance WebSocket**: Giảm một hop nhưng làm Frontend phụ thuộc Binance, khó kiểm soát fallback và trái ADR-0003.
- **Một WebSocket connection cho mỗi chart**: Dễ ánh xạ chart nhưng tốn connection và gây khó reconnect/cleanup; một connection multiplexed phù hợp hơn.
- **STOMP over WebSocket**: Có topic/subscription semantics sẵn nhưng thêm protocol và broker abstraction chưa cần thiết cho MVP.
- **Gửi mọi command qua WebSocket**: Tạo một protocol thống nhất nhưng làm validation, idempotency và error handling của job command phức tạp hơn REST.

## Consequences

### Positive

- Một connection có thể cập nhật bốn chart, Search progress và Leaderboard.
- Đổi timeframe của một chart không làm reload chart khác.
- Frontend không phụ thuộc Binance event format.
- Protocol có version, correlation ID và error boundary rõ.
- Reconnect/resubscribe và gap recovery có quy tắc thống nhất.
- Có thể coalesce update để UI không bị giật khi lưu lượng tăng.

### Negative

- Backend phải quản lý connection, subscription registry và cleanup.
- Cần xử lý reconnect, duplicate và stale event ở cả Backend lẫn Frontend.
- Debug WebSocket khó hơn request/response HTTP thông thường.
- Một connection dùng nhiều subscription cần routing cẩn thận theo `subscriptionId`.
- Native JSON protocol cần tự duy trì documentation và compatibility.

## Affected Components

- `apps/api`
- `apps/web`
- `modules/contracts`
- `modules/market-data`
- `modules/search`
- `modules/leaderboard`
- `apps/worker`
- `docs/api/websocket-events.md`
- Realtime UI state và connection status component

## Validation

- Mở bốn chart trên một browser tab và xác nhận chỉ có một application WebSocket connection.
- Đổi timeframe Chart 1 và xác nhận Chart 2–4 không reload hoặc mất dữ liệu.
- Ngắt Backend WebSocket, xác nhận UI hiển thị `RECONNECTING`, tự reconnect và resubscribe.
- Tạo Candle gap trong lúc disconnect và xác nhận REST backfill hợp nhất không tạo Candle trùng.
- Gửi duplicate/out-of-order Candle event và xác nhận Frontend giữ trạng thái mới nhất.
- Chạy Random Search và xác nhận progress cùng Top-K cập nhật không reload trang.
- Click Stop Search qua REST và xác nhận WebSocket phát trạng thái dừng tương ứng.
- Đóng browser tab và xác nhận subscription được cleanup.
- Mô phỏng client render chậm và xác nhận buffer có giới hạn, Candle close không bị mất.
- Gửi command/payload không hợp lệ và xác nhận chỉ subscription lỗi, connection vẫn an toàn khi có thể.

## Risks and Mitigations

- **Risk**: Connection bị rò rỉ sau khi người dùng rời trang.

  **Mitigation**: Cleanup theo connection lifecycle, unsubscribe khi component unmount và idle timeout.
- **Risk**: Reconnect storm khi Backend hoặc Binance phục hồi.

  **Mitigation**: Exponential backoff có jitter, retry cap và giới hạn connection rate.
- **Risk**: Update quá nhanh làm chart giật hoặc tăng bộ nhớ.

  **Mitigation**: Bounded buffer, coalesce open-candle update và batch UI rendering.
- **Risk**: Event cũ ghi đè dữ liệu mới sau reconnect.

  **Mitigation**: Event ID, Candle identity, timestamp/revision check và historical reconciliation.
- **Risk**: Thay đổi payload làm Frontend cũ bị lỗi.

  **Mitigation**: `eventVersion`, additive change trong cùng version và contract test giữa API/Frontend.
- **Risk**: Một subscription lỗi làm ảnh hưởng toàn connection.

  **Mitigation**: Trả `SUBSCRIPTION_ERROR` theo `subscriptionId`; chỉ đóng connection với protocol/security violation nghiêm trọng.
- **Risk**: WebSocket endpoint bị lạm dụng khi chưa có authentication.

  **Mitigation**: Origin allowlist, input validation, subscription limit, rate limit và chỉ phát dữ liệu công khai trong MVP.

## References

- [Đề bài Crypto StrategyLab](../Crypto%20Strategy%20Lab%20%E2%80%93%20%C4%90%E1%BB%93%20%C3%A1n%20cu%E1%BB%91i%20k%E1%BB%B3.pdf)
- [Data Flows](../architecture/data-flows.md)
- [WebSocket Events](../api/websocket-events.md)
- [API Conventions](../api/conventions.md)
- [UI Stitch Guide](../ui/stitch-guide.md)
- [ADR-0001: Modular Monolith](0001-modular-monolith.md)
- [ADR-0002: Module Boundaries](0002-module-boundaries.md)
- [ADR-0003: Market Data Adapter](0003-market-data-adapter.md)
- [ADR-0006: Queue và Worker](0006-queue-worker-backtesting.md)

## Supersession

- Supersedes: None
- Superseded by: None


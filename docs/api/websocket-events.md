# WebSocket Protocol and Events

## 1. Mục đích và phạm vi

Tài liệu này định nghĩa contract WebSocket giữa `apps/web` và `apps/api` cho MVP Crypto StrategyLab.

WebSocket được dùng để:

- nhận Candle realtime cho tối đa bốn chart độc lập;
- nhận trạng thái kết nối Market Data;
- theo dõi tiến trình Experiment và Backtest;
- nhận thông báo Leaderboard đã thay đổi.

WebSocket không được dùng để:

- tải toàn bộ Historical Candles;
- tạo, chạy hoặc dừng Backtest/Search;
- tải toàn bộ Backtest Result, Trades hoặc Leaderboard history;
- kết nối Frontend trực tiếp với Binance;
- thực hiện giao dịch tiền thật.

Các thao tác trên dùng REST API. WebSocket chỉ phát update và ID để Frontend tải dữ liệu đầy đủ khi cần.

## 2. Connection

| Thuộc tính | Quy định |
| --- | --- |
| Endpoint | `/ws` |
| Local URL | `ws://localhost:<port>/ws` |
| Deployment URL | `wss://<demo-host>/ws` |
| Protocol | Native WebSocket với JSON message |
| STOMP/SockJS | Không dùng trong MVP |
| Encoding | UTF-8 |
| Số connection | Một connection cho mỗi browser tab |
| Authentication | Bắt buộc theo ADR-0011 |
| Origin | Backend chỉ chấp nhận Origin nằm trong allowlist cấu hình |

WebSocket upgrade phải xác thực user bằng secure cookie hoặc short-lived
one-time ticket lấy từ authenticated REST API. Không đặt Supabase access token
dài hạn trong query string. Subscription tới Experiment/Leaderboard phải kiểm
tra ownership trước khi gửi snapshot hoặc event.

## 3. Message Envelope

Mọi client command và server event đều dùng envelope sau:

```json
{
  "eventType": "CANDLE_UPDATED",
  "eventVersion": 1,
  "eventId": "01JCANDLEEVENT0000000000001",
  "occurredAt": "2026-08-16T02:04:30.123Z",
  "correlationId": "01JMARKETREQUEST000000000001",
  "subscriptionId": "chart-1",
  "payload": {}
}
```

| Field | Bắt buộc | Quy tắc |
| --- | --- | --- |
| `eventType` | Có | Tên command/event dạng `UPPER_SNAKE_CASE` |
| `eventVersion` | Có | Số nguyên dương; version hiện tại là `1` |
| `eventId` | Có | ID duy nhất do bên gửi tạo; client dùng để deduplicate event |
| `occurredAt` | Có | ISO-8601 UTC; command dùng thời gian client gửi, event dùng thời gian server phát |
| `correlationId` | Có | Liên kết message với REST request, job và log liên quan |
| `subscriptionId` | Có | ID do Frontend tạo; định tuyến event đến chart/Experiment/Leaderboard tương ứng |
| `payload` | Có | JSON object theo từng `eventType`; dùng `{}` nếu command không cần dữ liệu bổ sung |

Quy tắc chung:

- ID là string opaque; Frontend không phân tích cấu trúc ID.
- Price, OHLCV, score và metric decimal được serialize thành JSON string.
- Enum dùng `UPPER_SNAKE_CASE`, trừ timeframe dùng mã chuẩn như `5m`, `1h`.
- Payload không chứa Binance event, database entity, Java class name hoặc stack trace.
- Message chứa field lạ trong request bị từ chối để phát hiện sai contract sớm.
- Frontend phải bỏ qua response field mới chưa biết trong cùng version.

## 4. Client Commands

| Command | `subscriptionId` | Payload | Mục đích |
| --- | --- | --- | --- |
| `SUBSCRIBE_CANDLES` | ID chart, ví dụ `chart-1` | `pair`, `timeframe` | Theo dõi Candle realtime |
| `UNSUBSCRIBE_CANDLES` | ID chart đang theo dõi | `{}` | Dừng theo dõi Candle của chart |
| `SUBSCRIBE_EXPERIMENT` | ID logical subscription | `experimentId` | Theo dõi tiến trình Experiment/Backtest |
| `UNSUBSCRIBE_EXPERIMENT` | ID subscription hiện tại | `{}` | Dừng nhận event Experiment |
| `SUBSCRIBE_LEADERBOARD` | ID logical subscription | `experimentId` | Theo dõi revision của Top-K |
| `UNSUBSCRIBE_LEADERBOARD` | ID subscription hiện tại | `{}` | Dừng nhận Leaderboard update |
| `PING` | `connection` | `clientTime` | Kiểm tra application connection còn hoạt động |

### 4.1. `SUBSCRIBE_CANDLES`

```json
{
  "eventType": "SUBSCRIBE_CANDLES",
  "eventVersion": 1,
  "eventId": "01JSUBSCRIBECANDLE000000001",
  "occurredAt": "2026-08-16T02:00:00Z",
  "correlationId": "01JMARKETREQUEST000000000001",
  "subscriptionId": "chart-1",
  "payload": {
    "pair": "BTC/USDT",
    "timeframe": "5m"
  }
}
```

Payload:

| Field | Kiểu | Bắt buộc | Quy tắc |
| --- | --- | --- | --- |
| `pair` | string | Có | Canonical pair dạng `BASE/QUOTE`, ví dụ `BTC/USDT` |
| `timeframe` | string | Có | MVP UI dùng `5m`, `15m`, `1h`, `4h` |

Một connection có tối đa bốn Candle subscriptions hoạt động. Cùng một `subscriptionId` không được đại diện cho hai chart cùng lúc.

### 4.2. `UNSUBSCRIBE_CANDLES`

```json
{
  "eventType": "UNSUBSCRIBE_CANDLES",
  "eventVersion": 1,
  "eventId": "01JUNSUBCANDLE000000000001",
  "occurredAt": "2026-08-16T02:30:00Z",
  "correlationId": "01JMARKETREQUEST000000000001",
  "subscriptionId": "chart-1",
  "payload": {}
}
```

Khi đổi pair hoặc timeframe, Frontend gửi `UNSUBSCRIBE_CANDLES` cho subscription cũ rồi gửi `SUBSCRIBE_CANDLES` mới. Các chart khác không bị reload.

### 4.3. `SUBSCRIBE_EXPERIMENT`

```json
{
  "eventType": "SUBSCRIBE_EXPERIMENT",
  "eventVersion": 1,
  "eventId": "01JSUBEXPERIMENT0000000001",
  "occurredAt": "2026-08-16T02:05:00Z",
  "correlationId": "01JSEARCHREQUEST00000000001",
  "subscriptionId": "experiment-progress-1",
  "payload": {
    "experimentId": "01JEXPERIMENT0000000000001"
  }
}
```

`experimentId` phải tồn tại và được phép đọc. Subscription này nhận `EXPERIMENT_PROGRESS_UPDATED` và `BACKTEST_COMPLETED` liên quan.

### 4.4. `UNSUBSCRIBE_EXPERIMENT`

```json
{
  "eventType": "UNSUBSCRIBE_EXPERIMENT",
  "eventVersion": 1,
  "eventId": "01JUNSUBEXPERIMENT000000001",
  "occurredAt": "2026-08-16T02:20:00Z",
  "correlationId": "01JSEARCHREQUEST00000000001",
  "subscriptionId": "experiment-progress-1",
  "payload": {}
}
```

### 4.5. `SUBSCRIBE_LEADERBOARD`

```json
{
  "eventType": "SUBSCRIBE_LEADERBOARD",
  "eventVersion": 1,
  "eventId": "01JSUBLEADERBOARD000000001",
  "occurredAt": "2026-08-16T02:05:00Z",
  "correlationId": "01JSEARCHREQUEST00000000001",
  "subscriptionId": "leaderboard-1",
  "payload": {
    "experimentId": "01JEXPERIMENT0000000000001"
  }
}
```

### 4.6. `UNSUBSCRIBE_LEADERBOARD`

```json
{
  "eventType": "UNSUBSCRIBE_LEADERBOARD",
  "eventVersion": 1,
  "eventId": "01JUNSUBLEADERBOARD0000001",
  "occurredAt": "2026-08-16T02:20:00Z",
  "correlationId": "01JSEARCHREQUEST00000000001",
  "subscriptionId": "leaderboard-1",
  "payload": {}
}
```

### 4.7. `PING`

```json
{
  "eventType": "PING",
  "eventVersion": 1,
  "eventId": "01JPING00000000000000000001",
  "occurredAt": "2026-08-16T02:10:00Z",
  "correlationId": "01JCONNECTION0000000000001",
  "subscriptionId": "connection",
  "payload": {
    "clientTime": "2026-08-16T02:10:00Z"
  }
}
```

Khoảng gửi PING và timeout do cấu hình môi trường quyết định, không hard-code trong UI component.

## 5. Server Events

| Event | Phát khi | Payload chính |
| --- | --- | --- |
| `SUBSCRIPTION_CONFIRMED` | Subscribe hoặc unsubscribe đã được áp dụng | `subscriptionType`, `status` |
| `CANDLE_UPDATED` | Có trạng thái mới của Candle đang mở hoặc Candle vừa đóng | Canonical Candle |
| `MARKET_CONNECTION_STATUS_CHANGED` | Trạng thái kết nối nguồn Market Data thay đổi | `status`, `lastSuccessfulEventAt` |
| `EXPERIMENT_PROGRESS_UPDATED` | Trạng thái/tiến trình Search thay đổi | ID, status, stage và candidate counts |
| `BACKTEST_COMPLETED` | Một Backtest hoàn thành thành công | ID của result liên quan |
| `LEADERBOARD_UPDATED` | Top-K read model có revision mới | ID, revision và top summary |
| `SUBSCRIPTION_ERROR` | Một command/subscription không hợp lệ hoặc gặp lỗi | Error code chuẩn hóa |
| `PONG` | Backend nhận `PING` hợp lệ | `clientTime`, `serverTime` |

### 5.1. `SUBSCRIPTION_CONFIRMED`

```json
{
  "eventType": "SUBSCRIPTION_CONFIRMED",
  "eventVersion": 1,
  "eventId": "01JCONFIRMED000000000000001",
  "occurredAt": "2026-08-16T02:00:00.050Z",
  "correlationId": "01JMARKETREQUEST000000000001",
  "subscriptionId": "chart-1",
  "payload": {
    "subscriptionType": "CANDLES",
    "status": "ACTIVE"
  }
}
```

`subscriptionType` nhận một trong `CANDLES`, `EXPERIMENT`, `LEADERBOARD`. `status` nhận `ACTIVE` hoặc `INACTIVE`.

### 5.2. `CANDLE_UPDATED`

```json
{
  "eventType": "CANDLE_UPDATED",
  "eventVersion": 1,
  "eventId": "01JCANDLEEVENT0000000000001",
  "occurredAt": "2026-08-16T02:04:30.123Z",
  "correlationId": "01JMARKETREQUEST000000000001",
  "subscriptionId": "chart-1",
  "payload": {
    "pair": "BTC/USDT",
    "timeframe": "5m",
    "openTime": "2026-08-16T02:00:00Z",
    "closeTime": "2026-08-16T02:05:00Z",
    "open": "59210.25",
    "high": "59300.00",
    "low": "59190.40",
    "close": "59275.80",
    "volume": "11.702",
    "closed": false
  }
}
```

Canonical Candle payload:

| Field | Kiểu | Quy tắc |
| --- | --- | --- |
| `pair` | string | Pair chuẩn, không dùng Binance symbol |
| `timeframe` | string | Timeframe chuẩn của hệ thống |
| `openTime` | timestamp | Thời điểm mở nến UTC |
| `closeTime` | timestamp | Thời điểm kết thúc khoảng nến UTC |
| `open` | decimal string | Giá mở |
| `high` | decimal string | Giá cao nhất |
| `low` | decimal string | Giá thấp nhất |
| `close` | decimal string | Giá hiện tại/đóng |
| `volume` | decimal string | Khối lượng |
| `closed` | boolean | `true` khi nến đã đóng và không nhận update mới |

Frontend nhận diện Candle bằng `pair + timeframe + openTime`. Update mới hơn của cùng Candle thay thế update cũ; Candle `closed = true` là trạng thái cuối.

### 5.3. `MARKET_CONNECTION_STATUS_CHANGED`

```json
{
  "eventType": "MARKET_CONNECTION_STATUS_CHANGED",
  "eventVersion": 1,
  "eventId": "01JMARKETSTATUS000000000001",
  "occurredAt": "2026-08-16T02:06:00Z",
  "correlationId": "01JMARKETCONNECTION00000001",
  "subscriptionId": "chart-1",
  "payload": {
    "status": "RECONNECTING",
    "lastSuccessfulEventAt": "2026-08-16T02:05:58.400Z"
  }
}
```

`status` nhận một trong:

```text
CONNECTING, CONNECTED, RECONNECTING, DISCONNECTED
```

Frontend phải hiển thị rõ khi dữ liệu không còn realtime. Event không làm lộ error code hoặc payload riêng của Binance.

### 5.4. `EXPERIMENT_PROGRESS_UPDATED`

```json
{
  "eventType": "EXPERIMENT_PROGRESS_UPDATED",
  "eventVersion": 1,
  "eventId": "01JEXPERIMENTEVENT000000001",
  "occurredAt": "2026-08-16T02:10:00Z",
  "correlationId": "01JSEARCHREQUEST00000000001",
  "subscriptionId": "experiment-progress-1",
  "payload": {
    "experimentId": "01JEXPERIMENT0000000000001",
    "status": "RUNNING",
    "stage": "BACKTESTING",
    "generatedCandidates": 40,
    "completedCandidates": 35,
    "failedCandidates": 1,
    "maximumCandidates": 100,
    "elapsedSeconds": 85
  }
}
```

Experiment `status` trong MVP:

```text
CREATED, QUEUED, RUNNING, STOP_REQUESTED, STOPPED, COMPLETED, FAILED
```

`stage` mô tả bước hiện tại để hiển thị, tối thiểu hỗ trợ:

```text
GENERATING, BACKTESTING, EVALUATING, RANKING, FINALIZING
```

Các count là số nguyên không âm. `maximumCandidates` phản ánh Stop Condition hiện tại. Khi Stop Condition không dùng giới hạn candidate, field này có thể là `null`; quy tắc nullable cuối cùng phải được đồng bộ vào OpenAPI/feature contract.

### 5.5. `BACKTEST_COMPLETED`

```json
{
  "eventType": "BACKTEST_COMPLETED",
  "eventVersion": 1,
  "eventId": "01JBACKTESTEVENT00000000001",
  "occurredAt": "2026-08-16T02:11:00Z",
  "correlationId": "01JSEARCHREQUEST00000000001",
  "subscriptionId": "experiment-progress-1",
  "payload": {
    "experimentId": "01JEXPERIMENT0000000000001",
    "candidateId": "01JCANDIDATE00000000000001",
    "backtestResultId": "01JBACKTESTRESULT0000000001",
    "evaluationResultId": "01JEVALUATION0000000000001"
  }
}
```

Event chỉ mang ID cần thiết. Frontend gọi REST để lấy metrics, Trades và chi tiết Result.

### 5.6. `LEADERBOARD_UPDATED`

```json
{
  "eventType": "LEADERBOARD_UPDATED",
  "eventVersion": 1,
  "eventId": "01JLEADERBOARDEVENT00000001",
  "occurredAt": "2026-08-16T02:11:01Z",
  "correlationId": "01JSEARCHREQUEST00000000001",
  "subscriptionId": "leaderboard-1",
  "payload": {
    "experimentId": "01JEXPERIMENT0000000000001",
    "leaderboardId": "01JLEADERBOARD000000000001",
    "revision": 7,
    "topCandidateId": "01JCANDIDATE00000000000001",
    "topScore": "0.8125"
  }
}
```

Frontend chỉ áp dụng event có `revision` lớn hơn revision đang hiển thị. Sau đó Frontend gọi REST bằng `experimentId` hoặc `leaderboardId` để lấy Top-K mới; event không gửi toàn bộ Leaderboard history.

### 5.7. `PONG`

```json
{
  "eventType": "PONG",
  "eventVersion": 1,
  "eventId": "01JPONG00000000000000000001",
  "occurredAt": "2026-08-16T02:10:00.020Z",
  "correlationId": "01JCONNECTION0000000000001",
  "subscriptionId": "connection",
  "payload": {
    "clientTime": "2026-08-16T02:10:00Z",
    "serverTime": "2026-08-16T02:10:00.020Z"
  }
}
```

## 6. Subscription Error

Lỗi có thể cô lập cho một subscription được trả bằng `SUBSCRIPTION_ERROR`; Backend không đóng toàn bộ connection.

```json
{
  "eventType": "SUBSCRIPTION_ERROR",
  "eventVersion": 1,
  "eventId": "01JSUBSCRIPTIONERROR00000001",
  "occurredAt": "2026-08-16T02:00:00.030Z",
  "correlationId": "01JMARKETREQUEST000000000001",
  "subscriptionId": "chart-1",
  "payload": {
    "code": "INVALID_MARKET_QUERY",
    "message": "The requested timeframe is not supported.",
    "details": {
      "fieldErrors": [
        {
          "field": "payload.timeframe",
          "reason": "must be one of 1m, 5m, 15m, 30m, 1h, 2h, 4h, 1d"
        }
      ]
    },
    "retryable": false
  }
}
```

| Field | Bắt buộc | Quy tắc |
| --- | --- | --- |
| `code` | Có | Tái sử dụng code trong [Error Catalog](error-catalog.md) khi cùng nguyên nhân |
| `message` | Có | Thông báo ngắn, không chứa implementation detail |
| `details` | Có | Object có cấu trúc; dùng `{}` nếu không có chi tiết |
| `retryable` | Có | Cho biết retry cùng input có thể thành công hay không |

Các lỗi thường gặp:

| Code | Khi xảy ra |
| --- | --- |
| `REQUEST_VALIDATION_FAILED` | Envelope/payload thiếu field hoặc sai kiểu |
| `INVALID_MARKET_QUERY` | Pair/timeframe không hợp lệ |
| `MARKET_SUBSCRIPTION_LIMIT_EXCEEDED` | Vượt tối đa bốn Candle subscriptions |
| `EXPERIMENT_NOT_FOUND` | Experiment không tồn tại |
| `LEADERBOARD_NOT_FOUND` | Leaderboard/read model không tồn tại |
| `RATE_LIMIT_EXCEEDED` | Gửi command quá nhanh |
| `VERSION_CONFLICT` | Client gửi event version không được hỗ trợ |

Backend chỉ đóng connection khi có protocol hoặc security violation nghiêm trọng, payload vượt giới hạn, Origin không hợp lệ hoặc client tiếp tục gửi dữ liệu nguy hiểm/không hợp lệ. Không gửi stack trace, raw provider error hoặc secret cho client.

## 7. Lifecycle

### 7.1. Mở Dashboard

1. Frontend gọi REST để tải Historical Candles và trạng thái ban đầu.
2. Frontend mở một WebSocket connection đến `/ws`.
3. Khi connection mở, Frontend gửi các command `SUBSCRIBE_*` đang cần.
4. Backend validate và trả `SUBSCRIPTION_CONFIRMED` cho từng subscription.
5. Frontend chỉ coi chart/job đã subscribe sau khi nhận confirmation.

### 7.2. Đổi pair hoặc timeframe

1. Gửi `UNSUBSCRIBE_CANDLES` cho chart tương ứng.
2. Chờ confirmation `INACTIVE` hoặc cleanup local subscription theo timeout UI.
3. Tải Historical Candles mới bằng REST.
4. Gửi `SUBSCRIBE_CANDLES` mới bằng cùng `subscriptionId` của chart.
5. Hợp nhất REST data và realtime update theo Candle identity.

### 7.3. Reconnect

Frontend quản lý trạng thái:

```text
CONNECTING -> CONNECTED -> RECONNECTING -> DISCONNECTED
```

Khi connection đóng ngoài ý muốn:

1. Hiển thị `RECONNECTING`; không giả vờ dữ liệu vẫn realtime.
2. Reconnect bằng exponential backoff có jitter và giới hạn số lần.
3. Sau khi kết nối lại, gửi lại toàn bộ active subscriptions.
4. Gọi REST từ Candle cuối đã biết để lấp khoảng dữ liệu bị thiếu.
5. Merge và deduplicate historical/realtime data.
6. Nếu hết số lần retry, chuyển `DISCONNECTED` và cho phép người dùng thử lại thủ công.

Giá trị backoff, retry cap, heartbeat interval và timeout được cấu hình theo môi trường và chốt trong feature plan; UI component không hard-code riêng các giá trị này.

### 7.4. Cleanup

- Component unmount phải gửi command unsubscribe phù hợp.
- Khi browser tab đóng hoặc connection timeout, Backend hủy toàn bộ logical subscriptions thuộc connection.
- Dừng một logical subscription không được dừng upstream stream nếu client khác vẫn sử dụng cùng pair/timeframe.

## 8. Ordering, Deduplication và Delivery

Hệ thống không đảm bảo exactly-once delivery. Client phải xử lý duplicate và event cũ.

- Deduplicate event theo `eventId` trong một cửa sổ gần nhất.
- Với Candle, key phía Frontend là `pair + timeframe + openTime`.
- Update Candle mới hơn thay thế update cũ của cùng key.
- Không ghi đè Candle `closed = true` bằng update mở hoặc cũ hơn.
- Với Leaderboard, chỉ áp dụng `revision` lớn hơn revision hiện tại.
- Sau reconnect, REST backfill là cơ chế phục hồi khoảng thiếu; WebSocket không replay toàn bộ lịch sử.
- Event tiến trình chỉ phục vụ UI; trạng thái bền vững phải đọc lại qua REST khi cần xác nhận.

## 9. Backpressure và hiệu năng

- Backend dùng outbound buffer có giới hạn.
- Có thể coalesce các update trung gian của cùng một Candle đang mở và chỉ giữ bản mới nhất.
- Không được bỏ Candle close event, connection status, Backtest completion hoặc trạng thái Experiment cuối.
- Frontend batch chart updates theo animation/render frame, không render lại toàn trang cho từng tick.
- Không gửi Historical dataset, toàn bộ Trades hoặc Leaderboard history trong WebSocket event.
- Khi client quá chậm và không thể phục hồi an toàn, Backend được phép đóng connection; Frontend reconnect và đồng bộ lại bằng REST.

## 10. Validation, giới hạn và bảo mật

- Validate `eventType`, `eventVersion`, ID, timestamp, pair, timeframe và payload.
- Giới hạn kích thước message và số command trên một khoảng thời gian bằng cấu hình.
- Giới hạn tối đa bốn Candle subscriptions trên mỗi connection.
- Chỉ cho phép Origin trong allowlist.
- Không nhận `START_SEARCH`, `STOP_SEARCH` hoặc command thay đổi business state qua WebSocket.
- Không gửi credential, token, SQL, internal class name, stack trace hoặc raw Binance/Python response.
- `correlationId` không phải authentication token.

## 11. Versioning và compatibility

- `eventVersion` hiện tại là `1` cho từng `eventType`.
- Thêm field optional vào payload là thay đổi tương thích trong cùng version.
- Không đổi tên, xóa field, đổi kiểu hoặc đổi ý nghĩa field trong cùng version.
- Breaking change phải tạo event version mới và duy trì thời gian chuyển tiếp đã được nhóm thống nhất.
- Server từ chối command version không hỗ trợ bằng `SUBSCRIPTION_ERROR` với code `VERSION_CONFLICT` khi có thể cô lập.
- Frontend phải bỏ qua field response mới chưa biết nhưng không được im lặng chấp nhận enum làm thay đổi nghiệp vụ.
- Producer và consumer phải có contract test cho envelope, command và event quan trọng.

Khi feature contract được duyệt, cập nhật file này cùng `docs/api/openapi.yaml`, integration DTO và contract test trong cùng Pull Request. Không duy trì một payload khác chỉ trong source code hoặc `examples.md`.

## References

- [API Conventions](conventions.md)
- [API Examples](examples.md)
- [API Error Catalog](error-catalog.md)
- [ADR-0003: Market Data Adapter](../adr/0003-market-data-adapter.md)
- [ADR-0004: WebSocket Realtime](../adr/0004-websocket-realtime.md)
- [ADR-0006: Queue và Worker](../adr/0006-queue-worker-backtesting.md)
- [ADR-0007: PostgreSQL/Supabase và Redis Ownership](../adr/0007-postgresql-redis-ownership.md)

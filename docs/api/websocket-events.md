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
| Authentication | One-time ticket lấy qua authenticated REST; không nhận access token dài hạn trong URL/message |
| Origin | Backend chỉ chấp nhận Origin nằm trong allowlist cấu hình |
| Ticket lifetime | Mặc định 60 giây, cấu hình bằng `platform.security.websocket-ticket-lifetime` |
| Connection lifetime | Tối đa 30 phút hoặc tới khi JWT gốc hết hạn, tùy thời điểm nào đến trước |

Trình tự handshake bắt buộc:

1. Frontend dùng Bearer JWT hiện tại và exact `Origin` gọi
   `POST /api/v1/realtime/ticket`.
2. Backend trả `{ "ticket": "...", "expiresAt": "..." }`. Ticket được gắn với user,
   exact Origin, thời điểm hết hạn ticket và thời điểm hết hạn của JWT đã cấp ticket.
3. Frontend mở `/ws?ticket=<one-time-ticket>` với cùng Origin. Backend kiểm tra allowlist,
   consume ticket đúng một lần rồi mới tạo connection.
4. Ticket thiếu, sai, hết hạn hoặc đã dùng bị từ chối bằng cùng
   `401 WEBSOCKET_TICKET_INVALID`; Origin không hợp lệ trả `403 FORBIDDEN_ORIGIN`.

Ticket query là credential ngắn hạn và không được ghi vào access log, metric label,
error response hay tracing attribute. Không đặt Supabase access token dài hạn trong URL
và không gửi token/refresh token trong WebSocket message. Subscription tới
Experiment/Leaderboard phải kiểm tra ownership trước confirmation, snapshot hint hoặc event.

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
| `EXPERIMENT_PROGRESS_UPDATED` | Trạng thái/tiến trình durable work thay đổi | Experiment/Job ID, status và work counts khi có |
| `BACKTEST_COMPLETED` | Một Backtest hoàn thành thành công | ID của result liên quan |
| `LEADERBOARD_UPDATED` | Top-K read model có revision mới | ID, monotonic revision và REST snapshot URL |
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
    "status": "ACTIVE",
    "syncMarker": "01JSYNCMARKER0000000000001"
  }
}
```

`subscriptionType` nhận một trong `CANDLES`, `EXPERIMENT`, `LEADERBOARD`. `status` nhận
`ACTIVE` hoặc `INACTIVE`. Confirmation `ACTIVE` bắt buộc có `syncMarker`; confirmation
`INACTIVE` không có marker. Marker là string opaque, chỉ có ý nghĩa trong đúng lần kích
hoạt subscription và connection hiện tại; client không parse, so sánh thứ tự hoặc tái sử
dụng marker đó sau reconnect.
Backend phải đăng ký nguồn event trước, chụp synchronization boundary, rồi phát
`SUBSCRIPTION_CONFIRMED`. Event của subscription chỉ được phát sau confirmation đó.
Frontend giữ các event đến sau confirmation trong lúc tải authoritative REST snapshot,
sau đó merge bằng `eventId` và identity/revision của resource. Marker giúp client gắn
snapshot recovery với đúng activation, nhưng không biến WebSocket thành nguồn truth.

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
    "jobId": "01JJOB00000000000000000001",
    "status": "RUNNING",
    "completedWork": 35,
    "failedWork": 1,
    "totalWork": 100,
    "bestScore": "0.8125"
  }
}
```

Experiment `status` trong MVP:

```text
CREATED, QUEUED, RUNNING, STOP_REQUESTED, STOPPED, COMPLETED, FAILED
```

`completedWork`, `failedWork` và `totalWork` là số nguyên không âm lấy từ normalized
F-007 progress event. `bestScore` là exact decimal string và chỉ xuất hiện sau khi có kết
quả xếp hạng. Với terminal lifecycle event, payload tối thiểu có `experimentId`, `status`,
`snapshotUrl` và có `jobId` nếu producer cung cấp. Client luôn refresh REST snapshot thay
vì suy diễn state còn thiếu từ notification.

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
    "snapshotUrl": "/api/v1/experiments/01JEXPERIMENT0000000000001/leaderboard"
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
| `WORKLOAD_SUBSCRIPTION_LIMIT_EXCEEDED` | Vượt tối đa bốn Experiment/Leaderboard subscriptions cộng lại |
| `DUPLICATE_SUBSCRIPTION_ID` | `subscriptionId` đã được dùng trên connection hiện tại |
| `SUBSCRIPTION_NOT_FOUND` | Unsubscribe một logical subscription không hoạt động |
| `EXPERIMENT_NOT_FOUND` | Experiment không tồn tại |
| `LEADERBOARD_NOT_FOUND` | Leaderboard/read model không tồn tại |
| `MARKET_PROVIDER_UNAVAILABLE` | Nguồn Market Data tạm thời không dùng được |
| `MARKET_PROVIDER_RATE_LIMITED` | Nguồn Market Data đang rate-limit |
| `RATE_LIMIT_EXCEEDED` | Gửi command quá nhanh |
| `VERSION_CONFLICT` | Client gửi event version không được hỗ trợ |

Backend chỉ đóng connection khi có protocol hoặc security violation nghiêm trọng, payload vượt giới hạn, Origin không hợp lệ hoặc client tiếp tục gửi dữ liệu nguy hiểm/không hợp lệ. Không gửi stack trace, raw provider error hoặc secret cho client.

## 7. Lifecycle

### 7.1. Mở Dashboard

1. Frontend gọi REST để tải Historical Candles và trạng thái ban đầu.
2. Frontend dùng session hiện tại gọi `POST /api/v1/realtime/ticket` với exact Origin.
3. Frontend mở `/ws?ticket=<one-time-ticket>` trước `expiresAt`.
4. Khi connection mở, Frontend gửi các command `SUBSCRIBE_*` đang cần.
5. Backend validate và trả `SUBSCRIPTION_CONFIRMED` kèm `syncMarker` cho từng subscription.
6. Frontend chỉ coi chart/job đã subscribe sau khi nhận confirmation, rồi reconcile với
   authoritative REST snapshot theo marker/revision.

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
2. Nếu close code là `4001 REAUTHENTICATION_REQUIRED`, Frontend dùng refresh token qua
   auth flow thông thường để lấy JWT mới. Bước này diễn ra ngầm và không yêu cầu nhập lại
   mật khẩu khi refresh session còn hợp lệ.
3. Gọi `POST /api/v1/realtime/ticket` để lấy ticket mới cho mọi lần reconnect; không dùng
   lại ticket hoặc connection credential cũ.
4. Reconnect bằng exponential backoff có jitter và giới hạn số lần.
5. Sau khi kết nối lại, gửi lại toàn bộ active subscriptions.
6. Gọi REST từ Candle cuối đã biết và đọc lại durable workload/Leaderboard snapshot.
7. Merge và deduplicate historical/realtime data theo marker, event identity và revision.
8. Nếu refresh session hết hạn/bị thu hồi thì yêu cầu đăng nhập lại. Nếu hết số lần retry
   transport, chuyển `DISCONNECTED` và cho phép người dùng thử lại thủ công.

Giá trị backoff, retry cap, heartbeat interval và timeout được cấu hình theo môi trường và chốt trong feature plan; UI component không hard-code riêng các giá trị này.

### 7.4. Authentication expiry và giới hạn connection

MVP không hỗ trợ reauthentication ngay bên trong WebSocket. Deadline của connection là
thời điểm sớm hơn giữa `JWT exp` đã dùng để cấp ticket và
`connectedAt + platform.security.websocket-max-connection-lifetime` (mặc định 30 phút).

Tại deadline, Backend phải dừng phát private event, giải phóng subscription rồi đóng
connection bằng application close code `4001` và reason ổn định
`REAUTHENTICATION_REQUIRED`. Reason không cho biết token hết hạn, bị thu hồi hay connection
đạt giới hạn tuổi. Client thực hiện silent refresh, xin ticket mới, reconnect, resubscribe và
đọc authoritative REST snapshot. Chỉ khi refresh session không còn hợp lệ mới chuyển người
dùng về màn hình đăng nhập.

Các close code do server phát:

| Code | Reason | Xử lý |
| ---: | --- | --- |
| `4001` | `REAUTHENTICATION_REQUIRED` | Silent refresh, xin ticket mới và reconnect |
| `4002` | `HEARTBEAT_TIMEOUT` | Kiểm tra transport rồi reconnect và tải snapshot |
| `4008` | `RATE_LIMIT_EXCEEDED` hoặc `SLOW_CONSUMER` | Backoff, giảm tốc độ rồi reconnect |

### 7.5. Cleanup

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
- `syncMarker` chỉ xác định boundary của một lần activation; nó không phải global offset,
  cursor lịch sử hoặc bằng chứng exactly-once.

## 9. Backpressure và hiệu năng

- Backend dùng outbound buffer có giới hạn.
- Có thể coalesce các update trung gian của cùng một Candle đang mở và chỉ giữ bản mới nhất.
- Không được bỏ Candle close event, connection status, Backtest completion hoặc trạng thái Experiment cuối.
- Frontend batch chart updates theo animation/render frame, không render lại toàn trang cho từng tick.
- Không gửi Historical dataset, toàn bộ Trades hoặc Leaderboard history trong WebSocket event.
- Khi client quá chậm và không thể phục hồi an toàn, Backend được phép đóng connection; Frontend reconnect và đồng bộ lại bằng REST.
- Close code cho slow consumer là `4008 SLOW_CONSUMER`; cùng numeric code có thể được
  dùng với reason `RATE_LIMIT_EXCEEDED`, vì client phải phân nhánh theo cả code và reason.

## 10. Validation, giới hạn và bảo mật

- Validate `eventType`, `eventVersion`, ID, timestamp, pair, timeframe và payload.
- Giới hạn kích thước message và số command trên một khoảng thời gian bằng cấu hình.
- Giới hạn tối đa bốn Candle subscriptions trên mỗi connection.
- Giới hạn tối đa bốn workload subscriptions (Experiment và Leaderboard cộng lại) trên mỗi connection.
- Mặc định giới hạn message 64 KiB và 30 commands trong 10 giây trên mỗi connection.
- Mặc định heartbeat 30 giây, timeout 90 giây và connection lifetime 30 phút; mọi ngưỡng
  phải lấy từ cấu hình server.
- Workload notification được consume từ Redis Streams `progress.events.v1`,
  `lifecycle.events.v1` và `candidate.evaluated.v1`. Có thể đổi tên bằng
  `platform.realtime.streams.*`; khi stream gián đoạn, REST read vẫn hoạt động và là
  nguồn trạng thái authoritative.
- Search Coordinator consume `SEARCH_REQUEST` (`messageType`/`messageVersion` bằng
  `SEARCH_REQUEST`/`1`) từ `search.requests.v1` qua group riêng `search-coordinators`; completion
  `CANDIDATE_EVALUATED` chỉ là trigger để reload durable progress. Start/Reproduce chỉ nhận qua REST,
  còn WebSocket tiếp tục chỉ phân phối progress/lifecycle và hướng client reconcile snapshot.
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
- Ticket là transport credential ngoài event version; thay đổi field response ticket theo
  compatibility rule REST/OpenAPI, còn thay đổi cách bind user/Origin/expiry theo security
  contract phải được review cùng handshake tests.
- `syncMarker` là bắt buộc đối với `SUBSCRIPTION_CONFIRMED` trạng thái `ACTIVE` của version
  1 trước khi contract F-009 được phát hành. Sau khi phát hành, xóa/đổi tên/đổi ý nghĩa marker
  hoặc cho phép event chạy trước confirmation là breaking change và cần event version mới.
- Thêm close code khiến client phải rẽ nhánh, thay đổi retry/reconnect semantics hoặc nhận
  reauthentication message trong connection là thay đổi contract và phải cập nhật tài liệu,
  producer/consumer tests trong cùng Pull Request.

Khi feature contract được duyệt, cập nhật file này cùng `docs/api/openapi.yaml`, integration DTO và contract test trong cùng Pull Request. Không duy trì một payload khác chỉ trong source code hoặc `examples.md`.

## References

- [API Conventions](conventions.md)
- [API Examples](examples.md)
- [API Error Catalog](error-catalog.md)
- [ADR-0003: Market Data Adapter](../adr/0003-market-data-adapter.md)
- [ADR-0004: WebSocket Realtime](../adr/0004-websocket-realtime.md)
- [ADR-0006: Queue và Worker](../adr/0006-queue-worker-backtesting.md)
- [ADR-0007: PostgreSQL/Supabase và Redis Ownership](../adr/0007-postgresql-redis-ownership.md)

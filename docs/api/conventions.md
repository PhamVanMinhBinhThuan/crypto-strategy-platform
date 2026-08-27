# API Conventions

## 1. Mục đích và phạm vi

Tài liệu này quy định cách thiết kế HTTP API công khai của Java Backend (`apps/api`) và các nguyên tắc chung áp dụng cho integration contract.

Quy ước này không định nghĩa endpoint cụ thể của từng feature. Endpoint, request và response cụ thể được chốt trong feature spec/plan, sau đó cập nhật vào `docs/api/openapi.yaml`.

Các nguyên tắc kiến trúc bắt buộc:

- Frontend chỉ gọi Java REST API và WebSocket của hệ thống, không gọi trực tiếp Binance, Supabase hoặc Python Sentiment Service.
- API không trả model riêng của Binance, database entity, Java class name hoặc implementation detail.
- HTTP/WebSocket/queue DTO là integration contract; `apps/api` hoặc adapter phải mapping DTO sang command/query/model nội bộ trước khi gọi capability module.
- Tác vụ Backtest/Search dài chạy bất đồng bộ qua Job/Worker, không giữ HTTP request cho đến khi hoàn thành.
- Experiment Manifest và Result đã chốt là bất biến; thay đổi đầu vào tạo Experiment hoặc Result mới.

## 2. Base URL và transport

### Public REST API

```text
/api/v1
```

Ví dụ:

```text
GET  /api/v1/strategies
POST /api/v1/experiments
GET  /api/v1/experiments/{experimentId}
```

Host phụ thuộc môi trường và không được hard-code trong contract:

```text
Local:  http://localhost:<port>/api/v1
Demo:   https://<demo-host>/api/v1
```

### WebSocket

```text
/ws
```

WebSocket sử dụng protocol và version riêng được mô tả trong `docs/api/websocket-events.md`. Không đặt WebSocket event dưới REST path chỉ để giống HTTP API.

### Internal Sentiment API

```text
/api/v1/sentiment
```

Đây là API nội bộ giữa `apps/worker` và `apps/sentiment`, không phải API cho browser.

### Content type

- Request/response JSON dùng `Content-Type: application/json`.
- Client gửi `Accept: application/json` khi gọi REST API.
- Encoding mặc định là UTF-8.
- Môi trường triển khai dùng HTTPS/WSS; HTTP/WS chỉ dùng local development.

## 3. Resource naming

### Path

- Dùng danh từ số nhiều cho collection: `/strategies`, `/experiments`, `/news-items`.
- Dùng `kebab-case` cho path nhiều từ.
- Không dùng động từ cho CRUD thông thường.
- Không để phần mở rộng như `.json` trong URL.
- Không đưa tên database table, Java class hoặc provider vào public path.
- Chỉ dùng nested resource khi quan hệ sở hữu rõ và path không quá sâu.

Ví dụ:

```text
GET /api/v1/experiments/{experimentId}/results
GET /api/v1/backtest-results/{resultId}/trades
```

Không nên:

```text
GET /api/v1/getExperiments
GET /api/v1/binanceKlines
GET /api/v1/experiment_table
```

### Command không phải CRUD

Hành động thay đổi state rõ ràng dùng `POST` trên command sub-resource:

```text
POST /api/v1/experiments/{experimentId}/stop
POST /api/v1/experiments/{experimentId}/reproduce
```

Tên command dùng `kebab-case`. Command phải được Backend kiểm tra state transition; Frontend không tự đặt `status`.

### Query parameter

- Dùng `lowerCamelCase`: `startTime`, `endTime`, `strategyId`.
- Tên filter phải trùng tên field public khi có thể.
- Không tạo nhiều tên cho cùng một ý nghĩa, ví dụ không dùng lẫn `pair`, `symbol` và `ticker`.
- Pair chuẩn dùng dạng `BTC/USDT`; nếu nằm trong query string phải được URL-encode đúng.

### JSON field

- Dùng `lowerCamelCase`.
- Không dùng tên viết tắt khó hiểu.
- Field boolean nên thể hiện trạng thái rõ: `closed`, `hasMore`, `retryable`.
- Không đổi tên field chỉ vì tên Java class/property nội bộ thay đổi.

### Header

Custom header dùng dạng:

```text
X-Correlation-Id
Idempotency-Key
```

Không đưa business data vào custom header nếu dữ liệu đó thuộc request body hoặc query.

## 4. HTTP methods và status codes

| Trường hợp | Method | Status chính |
| --- | --- | ---: |
| Đọc resource/collection | `GET` | `200 OK` |
| Tạo resource hoàn thành ngay | `POST` | `201 Created` |
| Tạo Backtest/Search job bất đồng bộ | `POST` | `202 Accepted` |
| Thay toàn bộ resource có thể cập nhật | `PUT` | `200 OK` hoặc `204 No Content` |
| Cập nhật một phần resource được phép sửa | `PATCH` | `200 OK` hoặc `204 No Content` |
| Command như Stop/Reproduce | `POST` | `200 OK`, `202 Accepted` hoặc `204 No Content` |
| Xóa resource thực sự được phép xóa | `DELETE` | `204 No Content` |

Quy tắc:

- Response `201 Created` phải có header `Location` trỏ tới resource mới.
- Response `202 Accepted` trả tối thiểu ID và trạng thái ban đầu; nên có `Location` để client theo dõi.
- Không trả `200 OK` kèm error object.
- `DELETE` không được dùng để xóa Experiment Manifest, Dataset version hoặc Result bất biến đang được tham chiếu.
- Không dùng `PATCH status=...` để vượt qua state machine; dùng command endpoint đã công bố.

### Error status

| Status | Khi sử dụng |
| ---: | --- |
| `400 Bad Request` | JSON sai cú pháp, sai kiểu, thiếu field bắt buộc hoặc query không parse được |
| `404 Not Found` | Resource không tồn tại hoặc không được expose |
| `409 Conflict` | State transition không hợp lệ, version trùng hoặc idempotency key xung đột |
| `422 Unprocessable Content` | Request đúng cấu trúc nhưng vi phạm domain rule/constraint |
| `429 Too Many Requests` | Vượt rate limit hoặc quota |
| `500 Internal Server Error` | Lỗi không dự kiến trong hệ thống |
| `502 Bad Gateway` | Upstream trả response không hợp lệ |
| `503 Service Unavailable` | Provider/service/queue tạm thời không khả dụng |
| `504 Gateway Timeout` | Upstream timeout |

Khi trả `429` hoặc lỗi retryable phù hợp, server nên gửi `Retry-After` nếu xác định được thời gian thử lại.

## 5. Success response

### Resource đơn

Trả resource trực tiếp, không bọc trong `data` nếu không có nhu cầu metadata chung:

```json
{
  "experimentId": "01J...",
  "status": "RUNNING",
  "createdAt": "2026-08-13T08:30:00Z"
}
```

### Tác vụ bất đồng bộ

Response `202 Accepted` tối thiểu có:

```json
{
  "experimentId": "01J...",
  "jobId": "01J...",
  "status": "QUEUED"
}
```

Kết quả đầy đủ được đọc lại bằng REST. WebSocket chỉ phát progress/event và ID cần thiết, không nhúng toàn bộ trades hoặc Leaderboard history.

### Collection

Collection phân trang dùng envelope thống nhất:

```json
{
  "items": [],
  "nextCursor": null,
  "hasMore": false
}
```

Collection nhỏ, bất biến và có giới hạn rõ như danh sách Strategy Plugin vẫn nên dùng `items` để client có một cách xử lý thống nhất.

## 6. Error response

Mọi REST error dùng cấu trúc chung:

```json
{
  "code": "STRATEGY_PARAMETERS_INVALID",
  "message": "Strategy parameters are invalid.",
  "details": {
    "fieldErrors": [
      {
        "field": "parameters.fastPeriod",
        "reason": "must be less than slowPeriod"
      }
    ]
  },
  "correlationId": "01J...",
  "timestamp": "2026-08-13T08:30:00Z"
}
```

Quy tắc:

- `code` là mã ổn định để client xử lý, dùng `UPPER_SNAKE_CASE`.
- `message` ngắn gọn, an toàn để hiển thị hoặc debug; client không dựa vào nội dung message để rẽ nhánh.
- `details` là object có cấu trúc; không chứa stack trace, SQL, credential, file path hoặc response thô của Binance/Python.
- `correlationId` phải khớp request/log liên quan.
- `timestamp` là thời điểm Backend tạo error response.
- Error riêng của provider phải được ánh xạ sang error code của hệ thống.

Danh sách mã lỗi chính thức nằm trong `docs/api/error-catalog.md`.

## 7. Data types

### Timestamp và date

- Dùng ISO-8601 UTC.
- Timestamp phải có `Z`, ví dụ `2026-08-13T08:30:00Z`.
- Khi cần độ chính xác mili giây: `2026-08-13T08:30:00.123Z`.
- Không trả local timezone hoặc timestamp không có offset.
- Tên field thời gian dùng hậu tố rõ: `createdAt`, `openTime`, `publishedAt`.
- Khoảng thời gian được hiểu là inclusive/exclusive thế nào phải ghi trong endpoint contract.

### Decimal

Price, OHLCV, tiền, fee rate, score và metric cần độ chính xác được serialize thành JSON string:

```json
{
  "open": "64250.10",
  "volume": "12.345678",
  "feeRate": "0.001",
  "totalReturn": "0.1532"
}
```

Quy tắc:

- Backend dùng `BigDecimal` hoặc kiểu decimal tương đương.
- Không dùng binary floating point cho giá trị nghiệp vụ quan trọng.
- Không thêm dấu phân cách hàng nghìn hoặc ký hiệu `%`, `$` vào dữ liệu.
- Scale và rounding rule cụ thể phải được định nghĩa trong feature contract khi có ảnh hưởng kết quả.
- Count như `numberOfTrades`, `candleCount` dùng JSON integer.

### Identifier

- ID công khai là string opaque; client không phân tích cấu trúc hoặc tự sinh nếu contract không cho phép.
- Hệ thống ưu tiên ULID cho ID mới để phù hợp các contract hiện tại, ví dụ `01J...`.
- Tên field có hậu tố `Id`: `experimentId`, `candidateId`, `jobId`.
- Không expose database sequence hoặc composite primary key nếu không phải domain identity.

### Enum

- Giá trị enum dùng `UPPER_SNAKE_CASE`: `BUY`, `SELL`, `HOLD`, `STOP_REQUESTED`.
- Enum phải được liệt kê trong OpenAPI/feature contract.
- Thêm enum value có thể làm client cũ lỗi; phải được review như thay đổi compatibility.
- Client nên có fallback cho enum chưa biết khi phù hợp với UI.

### Pair và timeframe

```json
{
  "pair": "BTC/USDT",
  "timeframe": "5m"
}
```

- Pair dùng canonical format `BASE/QUOTE` viết hoa.
- Timeframe dùng một trong `1m`, `5m`, `15m`, `30m`, `1h`, `2h`, `4h`, `1d`.
- Không expose symbol/interval riêng của Binance như canonical contract.

### Boolean, null và collection rỗng

- Boolean dùng JSON `true`/`false`, không dùng `0/1` hoặc string.
- Field bắt buộc không được biến mất tùy trạng thái.
- Field optional chưa có giá trị có thể là `null` nếu contract công bố nullable.
- Collection không có phần tử trả `[]`, không trả `null`.
- Request có field không được contract định nghĩa bị từ chối để phát hiện typo sớm.
- Client phải bỏ qua response field mới chưa biết để hỗ trợ additive change.

## 8. Pagination, filtering và sorting

### Cursor pagination

Collection có thể tăng theo thời gian dùng cursor:

```text
GET /api/v1/news-items?limit=20&cursor=<opaque-cursor>
```

Quy tắc:

- `limit` có default và maximum được ghi trong endpoint contract.
- `cursor` là opaque; client không tự tạo hoặc sửa.
- Response trả `nextCursor` và `hasMore`.
- Không trộn cursor pagination và page-number pagination trong cùng endpoint.
- Endpoint không cần pagination phải có giới hạn dữ liệu rõ ràng.

### Filtering

```text
GET /api/v1/experiments?status=RUNNING&strategyId=ma-crossover
GET /api/v1/candles?pair=BTC%2FUSDT&timeframe=5m&startTime=...&endTime=...
```

- Filter không hỗ trợ phải bị từ chối, không được im lặng bỏ qua.
- Time range phải có giới hạn để tránh response quá lớn.
- Filter theo thời gian dùng timestamp UTC.

### Sorting

Quy ước:

```text
sort=createdAt,desc
```

- Chỉ cho phép field và direction được endpoint công bố.
- Sort mặc định phải deterministic.
- Nếu field chính bằng nhau, server thêm ID làm tie-break ổn định.
- Leaderboard tuân theo Ranking/tie-break version của Experiment, không nhận sort tùy ý làm thay đổi thứ hạng nghiệp vụ.

## 9. Validation

Backend luôn validate request, kể cả khi Frontend đã validate.

Thứ tự validation:

1. JSON syntax, content type và kiểu dữ liệu.
2. Field bắt buộc, length/range và enum.
3. Cross-field constraint, ví dụ `fastPeriod < slowPeriod`.
4. Resource reference/version có tồn tại.
5. State transition và domain rule.

Quy tắc:

- Strategy parameter validation dùng schema/validator từ Strategy Plugin Registry, không hard-code một bản khác trong Controller.
- Search candidate phải được validate trước khi persist/enqueue.
- Pair/timeframe được validate theo canonical model, không theo chuỗi tùy ý của provider.
- Validation error có field path rõ ràng trong `details.fieldErrors`.
- Không normalize âm thầm dữ liệu làm thay đổi ý nghĩa nghiệp vụ.

## 10. Correlation và tracing

- Client có thể gửi `X-Correlation-Id`.
- Nếu thiếu hoặc không hợp lệ, API tạo correlation ID mới.
- API trả `X-Correlation-Id` trong mọi response, kể cả error.
- Correlation ID được truyền xuyên HTTP, Outbox, Redis message, Worker, Sentiment request và WebSocket event liên quan.
- Log cho luồng job nên có thêm `experimentId`, `jobId` và `candidateId`.
- Correlation ID không phải authentication token và không chứa dữ liệu nhạy cảm.

## 11. Idempotency

### Safe/idempotent method

- `GET` không tạo business side effect.
- `PUT` và `DELETE` phải idempotent theo HTTP semantics khi được sử dụng.
- Command Stop phải an toàn khi gửi lại: nếu Experiment đã ở trạng thái dừng tương thích, không tạo lỗi hệ thống hoặc effect trùng.

### Tạo tác vụ hoặc resource quan trọng

Client gửi:

```text
Idempotency-Key: <opaque-unique-value>
```

Áp dụng cho các `POST` có nguy cơ tạo trùng như Start Backtest, Start Search hoặc Reproduce.

Quy tắc:

- Cùng key và cùng canonical request trả lại cùng resource/job logic.
- Cùng key nhưng payload khác trả `409 Conflict`.
- Backend lưu key cùng request fingerprint và kết quả cần thiết trong thời gian retention đã công bố.
- Idempotency ở HTTP không thay thế idempotency của queue; Worker vẫn xử lý at-least-once theo `messageId`, `jobId` và `candidateId`.

## 12. Concurrency và immutable resources

- Experiment Manifest, Candidate Definition, Dataset version và Result đã chốt không được update tại chỗ.
- Thay Strategy, dataset, fee, Search configuration hoặc assumption tạo Experiment mới.
- Runtime status được cập nhật qua state machine riêng, không nằm trong immutable manifest.
- API phải trả `409 Conflict` khi command không hợp lệ với trạng thái hiện tại.
- Nếu feature cho phép chỉnh resource mutable đồng thời, contract phải dùng version/revision hoặc conditional request; không dùng last-write-wins âm thầm.

## 13. Versioning và compatibility

### REST API

- Major version nằm trong path: `/api/v1`.
- Thay đổi additive tương thích được giữ trong cùng major version.
- Breaking change tạo major version mới, ví dụ `/api/v2`.
- Không dùng application release version làm API path version.

Thay đổi thường tương thích:

- thêm optional response field;
- thêm endpoint mới;
- thêm optional query parameter không đổi hành vi mặc định.

Thay đổi thường breaking:

- xóa/đổi tên field hoặc endpoint;
- đổi kiểu dữ liệu, ý nghĩa hoặc đơn vị;
- biến optional field thành required;
- đổi status code/error code mà client đang xử lý;
- thêm enum value khi consumer không có fallback;
- thay đổi default làm kết quả nghiệp vụ khác.

### WebSocket và queue

- WebSocket message có `eventVersion` riêng theo event type.
- Queue message có `messageVersion` và stream/version contract theo ADR-0006.
- Breaking payload change tạo version mới; producer và consumer cũ phải có kế hoạch chuyển đổi rõ.
- REST version, WebSocket event version, queue message version và Strategy/plugin version là các loại version độc lập.

### Contract source of truth

- Feature đang thiết kế: `specs/<feature>/contracts/`.
- Contract đã duyệt và tích hợp: `docs/api/openapi.yaml` hoặc `docs/api/websocket-events.md`.
- Không duy trì hai contract đã duyệt có nội dung mâu thuẫn.
- `modules/contracts` chứa code DTO/message dùng qua runtime boundary; không thay thế OpenAPI/tài liệu contract.

## 14. Deprecation

Khi cần bỏ một field hoặc endpoint:

1. Đánh dấu deprecated trong OpenAPI và tài liệu.
2. Ghi rõ contract thay thế và thời điểm dự kiến loại bỏ.
3. Thông báo cho tất cả producer/consumer trong Pull Request.
4. Giữ contract cũ ít nhất trong thời gian chuyển đổi đã thống nhất.
5. Chỉ xóa trong major version mới nếu đây là breaking change.

Không deprecate âm thầm bằng cách đổi behavior nhưng giữ nguyên schema.

## 15. OpenAPI và review

Mỗi endpoint được duyệt phải có trong `docs/api/openapi.yaml` với:

- method, path, summary và tag;
- path/query/header parameters;
- request schema và ví dụ;
- success response và các error response có thể xảy ra;
- enum, nullable, format, min/max và required field;
- pagination/filter/sort rule nếu có;
- idempotency requirement nếu có;
- trạng thái deprecated nếu có.

Checklist trước khi merge contract:

- Frontend và Backend cùng review field/name/type.
- Không rò rỉ Binance, Supabase, Redis, Python hoặc database model.
- Decimal, timestamp, ID và enum đúng conventions.
- Async operation dùng `202` và có cách theo dõi.
- Error code đã có trong `error-catalog.md`.
- Breaking change đã được version hóa.
- OpenAPI example và implementation/contract test thống nhất.

## References

- [ADR-0001: Modular Monolith](../adr/0001-modular-monolith.md)
- [ADR-0002: Module Boundaries](../adr/0002-module-boundaries.md)
- [ADR-0003: Market Data Adapter](../adr/0003-market-data-adapter.md)
- [ADR-0004: WebSocket Realtime](../adr/0004-websocket-realtime.md)
- [ADR-0005: Strategy Plugin Registry](../adr/0005-strategy-plugin-registry.md)
- [ADR-0006: Queue và Worker](../adr/0006-queue-worker-backtesting.md)
- [ADR-0007: PostgreSQL/Supabase và Redis](../adr/0007-postgresql-redis-ownership.md)
- [ADR-0008: Sentiment Service](../adr/0008-sentiment-service-boundary.md)
- [ADR-0009: Reproducible Experiments](../adr/0009-reproducible-experiments.md)
- [ADR-0010: Strategy Generator Contract](../adr/0010-strategy-generator-contract.md)

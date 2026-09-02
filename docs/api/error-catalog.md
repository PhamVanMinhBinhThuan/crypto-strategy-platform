# API Error Catalog

## 1. Mục đích và phạm vi

Tài liệu này là danh sách mã lỗi nền tảng của Crypto StrategyLab.

- Lỗi HTTP mô tả request REST thất bại ngay tại thời điểm xử lý.
- Lỗi Job mô tả Backtest/Search/Sentiment bất đồng bộ đã được nhận nhưng thất bại sau đó.
- Error riêng của endpoint được bổ sung khi feature spec và OpenAPI được duyệt.
- Client xử lý theo `code`, không rẽ nhánh theo `message`.

## 2. REST error response

Mọi REST error từ Java API sử dụng cùng cấu trúc:

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

| Field | Bắt buộc | Quy tắc |
| --- | --- | --- |
| `code` | Có | Mã ổn định dạng `UPPER_SNAKE_CASE` |
| `message` | Có | Thông báo ngắn, an toàn; không dùng làm logic client |
| `details` | Có | Object có cấu trúc; dùng `{}` khi không có chi tiết |
| `correlationId` | Có | Khớp `X-Correlation-Id` và log liên quan |
| `timestamp` | Có | ISO-8601 UTC do Backend tạo |

`details` có thể chứa:

```json
{
  "fieldErrors": [],
  "resourceType": "Experiment",
  "resourceId": "01J...",
  "currentState": "COMPLETED",
  "allowedStates": ["RUNNING"],
  "retryable": false,
  "retryAfterSeconds": null
}
```

Chỉ trả field thật sự phù hợp với lỗi. Không trả field với giá trị giả chỉ để lấp schema.

## 3. Nguyên tắc chọn HTTP status

| Status | Ý nghĩa trong hệ thống |
| ---: | --- |
| `400 Bad Request` | Request không đọc/parse/validate cấu trúc được |
| `401 Unauthorized` | Bearer token thiếu hoặc không vượt qua xác thực |
| `403 Forbidden` | Origin hoặc hành động bị policy từ chối |
| `404 Not Found` | Resource/version không tồn tại hoặc không được expose |
| `405 Method Not Allowed` | Method không được endpoint hỗ trợ |
| `409 Conflict` | Xung đột state, immutable resource, duplicate/version/idempotency |
| `415 Unsupported Media Type` | Request body không phải content type được hỗ trợ |
| `422 Unprocessable Content` | Request đúng cấu trúc nhưng vi phạm domain rule |
| `429 Too Many Requests` | Client vượt rate limit/quota của hệ thống |
| `500 Internal Server Error` | Lỗi không dự kiến trong hệ thống |
| `502 Bad Gateway` | Upstream trả response sai hoặc không ánh xạ được |
| `503 Service Unavailable` | Provider, database, queue hoặc service tạm không khả dụng |
| `504 Gateway Timeout` | Upstream không trả lời trong timeout |

Không dùng `404` để che mọi lỗi và không trả `200` kèm error object.

## 4. Request và validation

| Code | HTTP | Retry | Khi xảy ra | Client xử lý |
| --- | ---: | --- | --- | --- |
| `MALFORMED_JSON` | 400 | Không | JSON sai cú pháp hoặc không parse được | Sửa request body |
| `REQUEST_VALIDATION_FAILED` | 400 | Không | Thiếu field, sai kiểu/format, enum không hợp lệ | Hiển thị `fieldErrors` |
| `UNKNOWN_REQUEST_FIELD` | 400 | Không | Request chứa field ngoài contract | Sửa tên/xóa field |
| `INVALID_QUERY_PARAMETER` | 400 | Không | Query parameter không parse được hoặc không hỗ trợ | Sửa query |
| `INVALID_CURSOR` | 400 | Không | Cursor thiếu, sai hoặc hết hiệu lực | Tải lại collection từ đầu |
| `UNSUPPORTED_MEDIA_TYPE` | 415 | Không | `Content-Type` không được hỗ trợ | Gửi `application/json` |
| `METHOD_NOT_ALLOWED` | 405 | Không | Gọi sai HTTP method | Dùng method trong OpenAPI |
| `DOMAIN_RULE_VIOLATION` | 422 | Không | Dữ liệu đúng cấu trúc nhưng vi phạm quy tắc nghiệp vụ chung | Hiển thị message/fieldErrors |
| `INVALID_TIME_RANGE` | 422 | Không | `startTime/endTime` sai thứ tự hoặc vượt giới hạn endpoint | Chọn lại khoảng thời gian |
| `RATE_LIMIT_EXCEEDED` | 429 | Có | Client vượt giới hạn request/subscription | Chờ `Retry-After` rồi thử lại |

`REQUEST_VALIDATION_FAILED` dùng cho lỗi schema/format. Error domain cụ thể phải ưu tiên code cụ thể hơn nếu đã có trong catalog.

## 4.1. Authentication

| Code | HTTP | Retry | Khi xảy ra | Client xử lý |
| --- | ---: | --- | --- | --- |
| `AUTHENTICATION_REQUIRED` | 401 | Không tự động | Bearer token thiếu, malformed, hết hạn, sai signature/issuer/audience hoặc subject không hợp lệ | Đăng nhập hoặc refresh session; không dựa vào message để phân biệt nguyên nhân token |
| `WEBSOCKET_TICKET_INVALID` | 401 | Có sau khi refresh session | WebSocket ticket thiếu, sai, hết hạn hoặc đã được dùng | Không dùng lại ticket; refresh session nếu cần, gọi lại `POST /api/v1/realtime/ticket` rồi mở connection mới |

Các lỗi xác thực dùng chung một public code/message an toàn; chi tiết validation token
không được trả về client hoặc ghi raw JWT vào log. `WEBSOCKET_TICKET_INVALID` cũng không
phân biệt ticket thiếu, malformed, hết hạn hay đã dùng để tránh làm lộ trạng thái credential.

## 5. Resource, state và idempotency

| Code | HTTP | Retry | Khi xảy ra | Client xử lý |
| --- | ---: | --- | --- | --- |
| `RESOURCE_NOT_FOUND` | 404 | Không | Resource chung không tồn tại | Quay lại danh sách hoặc tải lại |
| `INVALID_STATE_TRANSITION` | 409 | Không | Command không hợp lệ với state hiện tại | Tải lại resource và cập nhật UI |
| `IMMUTABLE_RESOURCE` | 409 | Không | Cố sửa Manifest, Dataset version, Candidate hoặc Result đã chốt | Tạo resource/Experiment mới |
| `VERSION_CONFLICT` | 409 | Không | Revision/version gửi lên đã cũ hoặc trùng | Tải bản mới rồi thao tác lại |
| `IDEMPOTENCY_KEY_CONFLICT` | 409 | Không | Cùng `Idempotency-Key` nhưng payload khác | Tạo key mới hoặc dùng payload cũ |
| `DUPLICATE_RESOURCE` | 409 | Không | ID hoặc business key đã tồn tại | Dùng resource hiện có hoặc đổi input |
| `FORBIDDEN_ORIGIN` | 403 | Không | Origin thiếu, sai định dạng hoặc không khớp chính xác allowlist khi xin ticket/upgrade WebSocket | Dùng client/environment hợp lệ; không tự đổi Origin |

Không trả `DUPLICATE_RESOURCE` khi request có cùng idempotency key và cùng payload; trường hợp đó phải trả lại resource/job logic trước đó.

### 5.1. Quy tắc operation của F-009

Các operation tạo durable outcome dưới đây dùng scope idempotency ổn định
`authenticated user + operation + Idempotency-Key`:

| REST operation | Operation scope |
| --- | --- |
| `POST /api/v1/datasets` | `CREATE_DATASET` |
| `POST /api/v1/backtests` | `START_BACKTEST` |
| `POST /api/v1/experiments` | `START_EXPERIMENT` |
| `POST /api/v1/experiments/{experimentId}/stop` | `STOP_EXPERIMENT` |
| `POST /api/v1/experiments/{experimentId}/reproductions` | `REPRODUCE_EXPERIMENT` |
| `POST /api/v1/jobs/{jobId}/cancel` | `CANCEL_JOB` |

Cùng key và cùng canonical payload phải replay outcome ban đầu, kể cả khi outcome đang
xử lý, đã hoàn thành hoặc đã thất bại. Cùng key trong cùng scope nhưng payload khác trả
`IDEMPOTENCY_KEY_CONFLICT`; cùng key của user hoặc operation khác là scope độc lập.

`START_EXPERIMENT` và `REPRODUCE_EXPERIMENT` hiện là contract được giữ chỗ nhưng luôn
trả `503 DEPENDENCY_UNAVAILABLE` cho tới khi Search Coordinator có published application
boundary. OpenAPI gắn readiness marker `BLOCKED_SEARCH_COORDINATOR`; hai operation này
không claim receipt và không tạo graph một phần trong lúc bị gate.

Với resource private, identifier không tồn tại và identifier thuộc user khác phải dùng
cùng public inaccessible code/status được operation công bố. Response không được chứa
owner, parent ID, state hoặc metadata giúp phân biệt hai trường hợp. `resourceId` chỉ được
trả khi bản thân contract lỗi cho phép công bố identifier đó cho user hiện tại.

## 6. Market data

Các code này chuẩn hóa lỗi provider theo ADR-0003; không trả Binance error code cho Frontend.

| Code | HTTP | Retry | Khi xảy ra | Client xử lý |
| --- | ---: | --- | --- | --- |
| `INVALID_MARKET_QUERY` | 422 | Không | Pair, timeframe, limit hoặc time range không hợp lệ | Sửa bộ lọc |
| `MARKET_PROVIDER_UNAVAILABLE` | 503 | Có | Provider mất kết nối hoặc không phản hồi | Hiển thị disconnected và retry có backoff |
| `MARKET_PROVIDER_RATE_LIMITED` | 503 | Có | Upstream provider rate-limit Backend | Chờ `Retry-After`; không retry dồn dập |
| `MARKET_PROVIDER_TIMEOUT` | 504 | Có | Provider không trả lời trong timeout | Cho phép thử lại |
| `MARKET_DATA_GAP` | 503 | Có | Khoảng Candle thiếu và chưa phục hồi được | Hiển thị dữ liệu chưa đầy đủ, thử lại sau |
| `MARKET_DATA_MAPPING_FAILED` | 502 | Có điều kiện | Payload provider không ánh xạ được sang canonical model | Báo lỗi dữ liệu; không tự sửa payload |
| `MARKET_SUBSCRIPTION_LIMIT_EXCEEDED` | 422 | Không | Client vượt tối đa bốn Market subscriptions | Giảm số chart/subscription |

`MARKET_PROVIDER_RATE_LIMITED` khác `RATE_LIMIT_EXCEEDED`: mã thứ nhất là Binance/upstream giới hạn Backend; mã thứ hai là chính Crypto StrategyLab giới hạn client.

## 7. Strategy và composite

| Code | HTTP | Retry | Khi xảy ra | Client xử lý |
| --- | ---: | --- | --- | --- |
| `STRATEGY_NOT_FOUND` | 404 | Không | Strategy ID không tồn tại | Tải lại Strategy Registry |
| `STRATEGY_VERSION_NOT_FOUND` | 404 | Không | Version được yêu cầu không còn khả dụng | Chọn version hợp lệ hoặc báo không tái lập được |
| `STRATEGY_PARAMETERS_INVALID` | 422 | Không | Parameter sai type/range/cross-field constraint | Hiển thị lỗi tại field |
| `STRATEGY_INPUT_INSUFFICIENT` | 422 | Không | Dataset thiếu lookback/input bắt buộc | Chọn dataset/range phù hợp |
| `COMPOSITE_STRATEGY_INVALID` | 422 | Không | Composite có ít hơn hai Strategy, trùng/sai child hoặc policy không hợp lệ | Sửa cấu hình composite |
| `COMBINATION_POLICY_NOT_FOUND` | 404 | Không | Policy ID/version không tồn tại | Tải lại policy/strategy metadata |
| `STRATEGY_GENERATOR_NOT_FOUND` | 404 | Không | Generator ID/version không được đăng ký | Chọn generator được hỗ trợ |
| `SEARCH_SPACE_INVALID` | 422 | Không | Search Space mâu thuẫn Strategy schema hoặc không sinh được candidate hợp lệ | Sửa range/constraint |

Strategy exception nội bộ không được trả Java class name hoặc stack trace trong `details`.

## 8. Dataset, Backtest và Evaluation

| Code | HTTP | Retry | Khi xảy ra | Client xử lý |
| --- | ---: | --- | --- | --- |
| `DATASET_NOT_FOUND` | 404 | Không | Dataset ID/version không tồn tại | Chọn dataset khác |
| `DATASET_NOT_READY` | 409 | Có | Dataset đang chuẩn bị hoặc freeze chưa xong | Theo dõi trạng thái rồi thử lại |
| `DATASET_INTEGRITY_FAILED` | 409 | Không | Checksum/count không khớp manifest | Không chạy; yêu cầu tạo/freeze lại dataset |
| `BACKTEST_CONFIGURATION_INVALID` | 422 | Không | Capital, fee, assumption hoặc execution rule không hợp lệ | Sửa cấu hình |
| `BACKTEST_RESULT_NOT_FOUND` | 404 | Không | Result ID không tồn tại | Tải lại Experiment/result list |
| `EVALUATION_RESULT_NOT_FOUND` | 404 | Không | Evaluation Result chưa có hoặc không tồn tại | Kiểm tra Job/Experiment status |
| `RANKING_POLICY_NOT_FOUND` | 404 | Không | Ranking formula ID/version không tồn tại | Chọn policy/version hợp lệ |
| `LEADERBOARD_NOT_FOUND` | 404 | Không | Leaderboard/read model không tồn tại | Kiểm tra Experiment hoặc chờ kết quả |

Strategy không tạo Trade là kết quả Backtest hợp lệ, không phải error.

## 9. Experiment và Search

| Code | HTTP | Retry | Khi xảy ra | Client xử lý |
| --- | ---: | --- | --- | --- |
| `EXPERIMENT_NOT_FOUND` | 404 | Không | Experiment ID không tồn tại | Quay lại danh sách |
| `EXPERIMENT_MANIFEST_INVALID` | 422 | Không | Manifest thiếu version, dataset, Strategy hoặc assumption bắt buộc | Sửa cấu hình trước khi start |
| `EXPERIMENT_ALREADY_STARTED` | 409 | Không | Cố start Experiment không còn ở `CREATED` | Tải trạng thái hiện tại |
| `EXPERIMENT_NOT_STOPPABLE` | 409 | Không | Stop khi Experiment không ở state cho phép | Cập nhật UI theo state mới |
| `STOP_CONDITION_REQUIRED` | 422 | Không | Search không có Stop Condition hữu hạn | Thêm ít nhất một điều kiện dừng |
| `STOP_CONDITION_INVALID` | 422 | Không | Giá trị stop condition âm, bằng 0 hoặc mâu thuẫn | Sửa điều kiện dừng |
| `EXPERIMENT_REPRODUCTION_UNAVAILABLE` | 409 | Không | Thiếu dataset, plugin, generator hoặc artifact version cũ | Hiển thị artifact thiếu |
| `EXPERIMENT_FINGERPRINT_MISMATCH` | 409 | Không | Manifest không khớp fingerprint đã lưu | Dừng chạy và kiểm tra dữ liệu |
| `CANDIDATE_DUPLICATE` | 409 | Không | Candidate fingerprint đã tồn tại trong Experiment | Generator/Search Coordinator sinh candidate khác |

## 10. Worker, queue và infrastructure

| Code | HTTP | Retry | Khi xảy ra | Client xử lý |
| --- | ---: | --- | --- | --- |
| `JOB_NOT_FOUND` | 404 | Không | Job ID không tồn tại | Tải lại Experiment |
| `QUEUE_UNAVAILABLE` | 503 | Có | Redis Streams hoặc publisher tạm không hoạt động | Giữ trạng thái chờ và thử lại sau |
| `DATABASE_UNAVAILABLE` | 503 | Có | PostgreSQL/Supabase tạm không khả dụng | Hiển thị service unavailable, retry có backoff |
| `DEPENDENCY_UNAVAILABLE` | 503 | Có | Dependency nội bộ tạm lỗi hoặc capability owner chưa ready, gồm Search Coordinator hiện tại | Thử lại có giới hạn; không xem operation là đã accept |
| `UPSTREAM_RESPONSE_INVALID` | 502 | Có điều kiện | Upstream trả payload không đúng contract | Báo lỗi; Backend ghi correlation ID |
| `UPSTREAM_TIMEOUT` | 504 | Có | Dependency quá timeout | Thử lại theo policy |
| `INTERNAL_ERROR` | 500 | Có điều kiện | Lỗi không dự kiến, không có code an toàn cụ thể | Hiển thị lỗi chung và cung cấp correlation ID |

Không dùng `INTERNAL_ERROR` thay cho error đã biết. Client không được tự retry vô hạn các lỗi `500`.

## 11. News và Sentiment

| Code | HTTP | Retry | Khi xảy ra | Client xử lý |
| --- | ---: | --- | --- | --- |
| `NEWS_ITEM_NOT_FOUND` | 404 | Không | News Item không tồn tại | Tải lại danh sách News |
| `NEWS_CONTENT_INVALID` | 422 | Không | Title/content/language hoặc kích thước không hợp lệ | Sửa/bỏ item |
| `SENTIMENT_RESULT_NOT_FOUND` | 404 | Không | Chưa có result cho News/model version | Hiển thị trạng thái đang chờ/chưa có |
| `SENTIMENT_SERVICE_UNAVAILABLE` | 503 | Có | Python Service chưa ready, circuit mở hoặc không kết nối được | News vẫn hiển thị; thử lại bất đồng bộ |
| `SENTIMENT_SERVICE_TIMEOUT` | 504 | Có | Inference quá timeout | Giữ pending/retry theo policy |
| `SENTIMENT_RESPONSE_INVALID` | 502 | Có điều kiện | Label/range/version trong response sai contract | Không lưu result; ghi correlation ID |
| `SENTIMENT_MODEL_NOT_AVAILABLE` | 503 | Có điều kiện | Model/version yêu cầu chưa được load hoặc không còn phục vụ | Dùng version hợp lệ hoặc báo không tái lập được |

Sentiment lỗi không được làm Market Dashboard, Market Data hoặc technical Strategy API trả lỗi theo.

## 12. Async Job failure

Request tạo Backtest/Search thường đã thành công với `202 Accepted`. Nếu xử lý thất bại sau đó, endpoint đọc Job/Experiment vẫn trả `200 OK` cùng trạng thái `FAILED`; không đổi thành HTTP `500` chỉ vì Job đã thất bại.

Ví dụ:

```json
{
  "jobId": "01J...",
  "experimentId": "01J...",
  "candidateId": "01J...",
  "type": "BACKTEST",
  "status": "FAILED",
  "totalWork": 1,
  "completedWork": 0,
  "failedWork": 1,
  "bestScore": null,
  "queuedAt": "2026-08-13T08:30:00Z",
  "startedAt": "2026-08-13T08:30:01Z",
  "finishedAt": "2026-08-13T08:45:00Z",
  "nextRetryAt": null,
  "failure": {
    "code": "JOB_EXECUTION_TIMEOUT",
    "message": "The job exceeded its execution timeout."
  },
  "createdAt": "2026-08-13T08:30:00Z",
  "updatedAt": "2026-08-13T08:45:00Z"
}
```

Retry timing được biểu diễn bằng lifecycle (`RETRY_SCHEDULED`) và `nextRetryAt`; public
Job failure không công bố worker attempt hay internal retry policy.

Catalog failure code nền tảng:

| Failure code | Retry | Khi xảy ra |
| --- | --- | --- |
| `JOB_EXECUTION_TIMEOUT` | Có | Job vượt execution timeout |
| `JOB_RETRY_EXHAUSTED` | Không tự động | Job đã hết số lần retry |
| `JOB_CANCELLED` | Không | Job bị hủy do Stop request |
| `JOB_MESSAGE_INVALID` | Không | Queue message sai version/schema/reference |
| `JOB_DEPENDENCY_UNAVAILABLE` | Có | Database/Redis/service tạm lỗi |
| `JOB_DATASET_NOT_READY` | Có | Dataset chưa sẵn sàng |
| `JOB_DATASET_INTEGRITY_FAILED` | Không | Dataset checksum không hợp lệ |
| `JOB_STRATEGY_VERSION_UNAVAILABLE` | Không | Plugin version không resolve được |
| `JOB_STRATEGY_PARAMETERS_INVALID` | Không | Candidate chứa parameter không hợp lệ |
| `JOB_BACKTEST_FAILED` | Theo phân loại | Backtester gặp lỗi, không phải kết quả “không có Trade” |
| `JOB_EVALUATION_FAILED` | Theo phân loại | Không tính được metrics hợp lệ |
| `JOB_SENTIMENT_ANALYSIS_FAILED` | Theo phân loại | Sentiment job hết retry hoặc response sai |
| `JOB_INTERNAL_ERROR` | Có giới hạn | Lỗi chưa phân loại, sau giới hạn chuyển Dead Letter |

Failure object không chứa stack trace hoặc queue payload thô. Chi tiết kỹ thuật nằm trong log theo `correlationId`, `jobId`, `experimentId` và `candidateId`.

## 13. WebSocket errors

WebSocket không dùng HTTP error response sau khi connection đã được thiết lập. Lỗi subscription dùng event `SUBSCRIPTION_ERROR` với envelope của `docs/api/websocket-events.md`.

Payload tối thiểu:

```json
{
  "code": "INVALID_MARKET_QUERY",
  "message": "The requested timeframe is not supported.",
  "details": {},
  "retryable": false
}
```

Quy tắc:

- Lỗi của một subscription phải gắn đúng `subscriptionId` và không đóng toàn connection nếu có thể cô lập.
- Chỉ đóng connection khi protocol/security violation nghiêm trọng.
- WebSocket error code nên tái sử dụng mã REST khi cùng một nguyên nhân.
- Reconnect không được giả định exactly-once; client vẫn deduplicate event.

## 14. Retry rules

| Nhóm | Client có thể retry? | Quy tắc |
| --- | --- | --- |
| Validation, not found, immutable, state conflict | Không | Phải sửa input/state trước |
| `429` | Có | Chờ `Retry-After`, dùng backoff |
| `502` | Có điều kiện | Chỉ retry operation idempotent hoặc có `Idempotency-Key` |
| `503`, `504` | Có | Backoff có jitter và giới hạn số lần |
| `500` | Có điều kiện | Không retry vô hạn; dùng correlation ID để điều tra |
| Async transient failure | Worker xử lý | Theo retry policy, attempt limit và Dead Letter |

Client không tự retry `POST` tạo job nếu không dùng cùng `Idempotency-Key`.

## 15. Logging và bảo mật

### Được phép log

- `code`, HTTP status và retry classification;
- `correlationId`, `experimentId`, `jobId`, `candidateId` khi có;
- endpoint/method, latency và upstream name;
- provider/internal error code đã sanitize;
- exception stack trace chỉ trong server log phù hợp, không trả client.

### Không được log hoặc trả client

- password, token, API key, Supabase service-role key;
- Binance credential;
- Authorization header, cookie hoặc secret environment variable;
- one-time WebSocket ticket, kể cả query string của request upgrade;
- SQL chứa dữ liệu nhạy cảm;
- toàn bộ News content nếu không cần thiết;
- raw Python/Binance response có dữ liệu không được công bố;
- Java/Python stack trace, classpath hoặc file path trong API response.

Production log cần tránh ghi toàn bộ request body. Nếu cần debug fixture, phải dùng dữ liệu test không chứa secret.

## 16. Quy trình thêm hoặc sửa error code

Khi feature cần error mới:

1. Xác nhận chưa có code hiện tại cùng ý nghĩa.
2. Chọn nhóm và HTTP status theo conventions.
3. Tên code mô tả nguyên nhân ổn định, không mô tả câu chữ UI.
4. Ghi rõ retry classification và hành vi client.
5. Cập nhật file này, feature contract và `openapi.yaml` trong cùng Pull Request.
6. Producer và consumer cùng review.
7. Thêm contract test cho code/status/details quan trọng.

Không đổi ý nghĩa của code đã phát hành. Nếu semantics thay đổi breaking, tạo code/version mới và deprecate code cũ.

## References

- [API Conventions](conventions.md)
- [WebSocket Events](websocket-events.md)
- [ADR-0003: Market Data Adapter](../adr/0003-market-data-adapter.md)
- [ADR-0004: WebSocket Realtime](../adr/0004-websocket-realtime.md)
- [ADR-0005: Strategy Plugin Registry](../adr/0005-strategy-plugin-registry.md)
- [ADR-0006: Queue và Worker](../adr/0006-queue-worker-backtesting.md)
- [ADR-0007: PostgreSQL/Supabase và Redis](../adr/0007-postgresql-redis-ownership.md)
- [ADR-0008: Sentiment Service](../adr/0008-sentiment-service-boundary.md)
- [ADR-0009: Reproducible Experiments](../adr/0009-reproducible-experiments.md)
- [ADR-0010: Strategy Generator Contract](../adr/0010-strategy-generator-contract.md)

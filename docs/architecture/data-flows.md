# Dynamic Views and Data Flows

**Status**: Draft — Target MVP Architecture

**Last Updated**: 2026-08-14

**Owner**: Văn Minh

Các flow dưới đây là runtime story cấp kiến trúc. Payload chi tiết thuộc [API documentation](../api/README.md); state và identity thuộc [Data Model Overview](data-model-overview.md).

## 1. Historical Market Data

```mermaid
sequenceDiagram
    actor User
    participant Web
    participant API
    participant Market as Market Data Module
    participant Cache as Redis Cache
    participant Binance as Binance Adapter/API
    participant DB as PostgreSQL

    User->>Web: Chọn pair/timeframe/range
    Web->>API: Historical Candle query
    API->>Market: Query chuẩn
    Market->>Cache: Tìm canonical range
    alt Cache hit đầy đủ
        Cache-->>Market: Candle range
    else Cache miss hoặc thiếu range
        loop Pagination có kiểm soát
            Market->>Binance: Kline request
            Binance-->>Market: Provider payload/rate-limit state
        end
        Market->>Market: Validate, normalize, sort, deduplicate
        Market->>DB: Persist closed Candle/Dataset reference idempotently
        Market->>Cache: Cache range theo TTL/version
    end
    Market-->>API: Canonical Candle list
    API-->>Web: Internal market contract
```

Quy tắc:

- Query dùng pair, timeframe, start/end hoặc limit chuẩn; adapter chịu trách nhiệm ánh xạ symbol/interval.
- Pagination đáp ứng giới hạn mỗi response và phối hợp rate-limit/exponential backoff có giới hạn.
- Candle được sắp tăng theo `openTime`; identity là `provider + pair + timeframe + openTime`.
- Provider array/object và error riêng không được đi ra ngoài `market-data`.
- Cache miss không phải lỗi; PostgreSQL/provider là nguồn đọc lại.
- Request sai là lỗi chuẩn không retry; timeout/429/5xx có thể retry theo policy.

## 2. Realtime Market Data and Recovery

```mermaid
sequenceDiagram
    participant Web
    participant API
    participant Market as Market Data Module
    participant Binance
    participant DB as PostgreSQL

    Web->>API: SUBSCRIBE_CANDLES(subscriptionId, pair, timeframe)
    API->>Market: subscribeCandles(query, handler)
    Market->>Binance: Open hoặc reuse upstream stream
    Binance-->>Market: Kline update
    Market->>Market: Normalize + order/deduplicate
    Market-->>API: Candle update + connection state
    API-->>Web: CANDLE_UPDATED

    Binance--xMarket: Disconnect
    Market-->>API: RECONNECTING
    API-->>Web: Connection status
    loop Exponential backoff có jitter và giới hạn
        Market->>Binance: Reconnect
    end
    Market->>Binance: Historical backfill từ last confirmed Candle
    Binance-->>Market: Missing/overlapping Candles
    Market->>Market: Sort + deduplicate + gap check
    Market->>DB: Persist newly closed Candles idempotently
    Market-->>API: Recovered updates + CONNECTED
    API-->>Web: Snapshot/delta + connection state
```

Quy tắc:

- Một upstream Binance stream được chia sẻ cho nhiều subscriber cùng pair/timeframe và đóng khi subscriber count bằng 0.
- WebSocket giữ thứ tự trong một connection nhưng client vẫn deduplicate theo `eventId` và bỏ stale Candle/revision sau reconnect.
- Update mới hơn thay update cũ của cùng open Candle; `closed=true` là trạng thái cuối của khoảng nến.
- Backend coalesce update trung gian khi outbound chậm nhưng không bỏ Candle close, connection state hoặc completion event.
- Frontend render theo batch/frame, không nhận lại toàn bộ history ở mỗi tick.

## 3. Strategy, Backtest và Evaluation

```mermaid
flowchart LR
    MANIFEST["Immutable Experiment Manifest"] --> DATASET["Frozen Dataset"]
    MANIFEST --> DEFINITION["Strategy / Composite Definition"]
    DEFINITION --> REGISTRY["Strategy Registry"]
    REGISTRY --> STRATEGY["Strategy instance"]
    DATASET --> BACKTEST["Backtester"]
    STRATEGY --> BACKTEST
    BACKTEST --> TRADES["Trade sequence + Backtest Result"]
    TRADES --> EVALUATOR["Evaluator"]
    EVALUATOR --> METRICS["Return / Win Rate / MDD / Trades"]
```

Main steps:

1. Worker đọc manifest, Dataset version/checksum và Candidate Definition bằng ID.
2. Registry resolve đúng Strategy/Composite version và validate exact parameters.
3. Backtester truyền ordered Candle/`evaluationTime` vào Strategy, diễn giải BUY/SELL/HOLD theo assumptions đã lưu.
4. Backtester tạo Trade sequence và Result; Strategy không truy cập network/database.
5. Evaluator tính bốn metrics bắt buộc độc lập với Strategy/Search/Ranking.
6. Result và Evaluation Result được persist immutable; retry cùng Candidate không tạo business result trùng.

Nếu Strategy version/dataset không resolve hoặc checksum sai, job thất bại có cấu trúc và không tạo Result một phần. “Không có Trade” là business result hợp lệ, không phải infrastructure error.

## 4. Search, Queue và Leaderboard

```mermaid
sequenceDiagram
    actor User
    participant Web
    participant API
    participant DB as PostgreSQL/Outbox
    participant Publisher as Outbox Publisher
    participant Redis as Redis Streams
    participant Coordinator as Search Coordinator
    participant Worker as Backtest Worker
    participant Ranking as Ranking Handler

    User->>Web: Start Search
    Web->>API: REST create/start Experiment
    API->>DB: Transaction: Manifest + Job + Outbox
    API-->>Web: 202 + experimentId/jobId/QUEUED
    Publisher->>DB: Read unpublished event
    Publisher->>Redis: Publish search request
    Redis-->>Coordinator: Deliver through consumer group
    loop Bounded batches until Stop Condition
        Coordinator->>Coordinator: Generate + validate + deduplicate Candidate
        Coordinator->>DB: Persist Candidate + Backtest Job/Outbox
        Publisher->>Redis: Publish Backtest Job
        Redis-->>Worker: Deliver at least once
        Worker->>DB: Check Job/idempotency + load references
        Worker->>Worker: Backtest → Evaluate
        Worker->>DB: Transaction: Result + Evaluation + Outbox
        Worker->>Redis: Acknowledge after durable commit
        Redis-->>Ranking: CANDIDATE_EVALUATED
        Ranking->>DB: Idempotent score + Top-K revision
        Ranking-->>API: Progress/Leaderboard event
        API-->>Web: WebSocket progress/revision
    end
```

Stop và backpressure:

- Stop Condition thuộc Search Coordinator: `maxCandidates`, `maxDuration`, `maxIterationsWithoutImprovement` hoặc user stop.
- `STOP_REQUESTED` ngừng sinh candidate; queued job bị skip/cancel; running job kết thúc tại safe checkpoint.
- Coordinator giới hạn `QUEUED + RUNNING`, sinh batch nhỏ và giảm tốc khi queue vượt threshold.
- Worker concurrency và job timeout cấu hình theo môi trường; Dataset chỉ truyền bằng reference.

Reliability:

- Delivery là at-least-once; `jobId`, `candidateId`, unique constraint và Processed Message ngăn effect trùng.
- Worker chỉ acknowledge sau durable commit; Worker chết trước ack cho phép consumer khác reclaim.
- Transient error retry có giới hạn/backoff; permanent validation error không retry; hết retry đưa Dead Letter và đánh dấu `FAILED`.
- Transactional Outbox tránh “DB đã lưu nhưng Redis chưa nhận”; recovery scan republish event/job chưa hoàn thành.
- Top-K dùng revision và stable tie-break, không phụ thuộc thứ tự Worker hoàn thành.

## 5. News và Sentiment

```mermaid
sequenceDiagram
    participant Provider as News Provider Adapter
    participant News as Java News Module
    participant DB as PostgreSQL
    participant Worker
    participant Sentiment as Python/FastAPI
    participant API
    participant Web

    Provider-->>News: Raw news payload
    News->>News: Normalize, sanitize, contentHash, deduplicate
    News->>DB: Store News Item as PENDING
    Worker->>DB: Claim analysis job; mark ANALYZING
    Worker->>Sentiment: Versioned internal HTTP request
    alt Valid response
        Sentiment-->>Worker: Label, confidence, score, modelVersion
        Worker->>Worker: Validate contract/ranges
        Worker->>DB: Store immutable Sentiment Result; ANALYZED
    else Timeout/429/5xx
        Worker->>Worker: Limited retry/backoff/circuit breaker
        Worker->>DB: Keep PENDING/FAILED_RETRYABLE or FAILED
    end
    API->>DB: Read News/Sentiment view
    API-->>Web: News Item + analysis state/result
```

Quy tắc:

- Python Service stateless, tải model khi startup, không crawl News và không truy cập PostgreSQL/Redis.
- `newsId + contentHash + modelVersion` xác định một logical result; model/content mới tạo version mới.
- Circuit breaker và concurrency limit bảo vệ Worker/model; Sentiment lỗi chỉ làm News/Sentiment degraded.
- Technical Strategy, Backtest và realtime chart không chờ Sentiment.
- Sentiment Strategy tương lai chỉ nhận frozen `sentimentData` trong StrategyContext; không gọi Python trực tiếp và không dùng News xuất bản sau `evaluationTime`.

# Dynamic Views and Data Flows

**Status**: Proposed baseline
**Last Updated**: 2026-08-12
**Owners**: Tiến Luật và Flow Owners

## 1. Historical Market Data

```mermaid
sequenceDiagram
    participant Web
    participant API
    participant Market as Market Data Module
    participant Binance
    participant DB as PostgreSQL
    Web->>API: Candle query (pair/timeframe/range)
    API->>Market: canonical query
    Market->>DB: read closed candles
    alt cache/database gap
        Market->>Binance: paginated historical klines
        Binance-->>Market: provider payload
        Market->>Market: validate/normalize/deduplicate
        Market->>DB: persist idempotently
    end
    Market-->>API: ordered canonical candles
    API-->>Web: internal contract
```

- Input dùng pair, timeframe, UTC range hoặc limit; output tăng dần theo `openTime`.
- Provider errors được chuyển thành stable error catalog; không trả Binance payload/error trực tiếp.
- Backtest dataset chỉ nhận closed candles và được freeze/checksum trước execution.

## 2. Realtime Market Data

```mermaid
sequenceDiagram
    participant Web
    participant API as WebSocket Gateway
    participant Market as Binance Adapter
    participant Binance
    Web->>API: SUBSCRIBE_CANDLES(subscriptionId)
    API->>Market: subscribe(pair,timeframe)
    Market->>Binance: open/reuse stream
    Binance-->>Market: kline updates
    Market-->>API: canonical CandleUpdated
    API-->>Web: CANDLE_UPDATED
    Binance--xMarket: disconnect
    Market-->>API: RECONNECTING
    Market->>Binance: reconnect with backoff
    Market->>Binance: REST backfill since last closed candle
    Market->>Market: reconcile/deduplicate
    Market-->>API: recovered updates + CONNECTED
```

- Một browser tab dùng một WebSocket và nhiều logical subscriptions, tối đa bốn market subscriptions.
- Đổi timeframe chỉ unsubscribe/resubscribe chart tương ứng.
- Identity `provider + pair + timeframe + openTime`, event time và `closed` ngăn stale/duplicate update.
- Frontend reconnect, resubscribe và REST backfill nếu application WebSocket bị ngắt.

## 3. Strategy, Backtest và Evaluation

```mermaid
flowchart LR
    DATA[Immutable Dataset] --> CONTEXT[StrategyContext]
    REGISTRY[Strategy Registry] --> STRATEGY[Strategy / Composite]
    CONTEXT --> STRATEGY
    STRATEGY --> SIGNALS[BUY / SELL / HOLD]
    SIGNALS --> BACKTEST[Backtester]
    DATA --> BACKTEST
    BACKTEST --> RESULT[BacktestResult + Trades]
    RESULT --> EVALUATOR[Evaluator]
    EVALUATOR --> METRICS[EvaluationResult]
    METRICS --> RANK[Ranking Policy]
```

1. Registry resolve exact Strategy/Composite version và parameters.
2. Strategy phân tích ordered context, không gọi provider/database.
3. Backtester diễn giải signals theo immutable simulation assumptions.
4. Evaluator tính Return, Win Rate, Maximum Drawdown và Number of Trades.
5. Ranking áp versioned formula/tie-break và cập nhật Top-K idempotently.
6. Validation error dừng candidate; zero trade là valid business result, không phải system error.

## 4. Search, Queue và Leaderboard

```mermaid
sequenceDiagram
    participant Web
    participant API
    participant DB as PostgreSQL/Outbox
    participant Redis
    participant Search as Search Coordinator
    participant Worker
    participant Rank as Ranking Handler
    Web->>API: Start Search
    API->>DB: Experiment + Job + Outbox
    API-->>Web: experimentId + jobId
    DB-->>Redis: search request
    Redis-->>Search: consume request
    loop bounded candidates until stop condition
        Search->>DB: immutable CandidateDefinition
        Search->>Redis: BacktestJob reference
        Redis-->>Worker: at-least-once delivery
        Worker->>DB: load manifest/dataset
        Worker->>DB: Result + Evaluation + Outbox
        DB-->>Redis: CandidateEvaluated
        Redis-->>Rank: update Top-K idempotently
        Rank->>DB: Leaderboard revision
        Rank-->>API: progress/update event
        API-->>Web: WebSocket update
    end
```

### Job contract

Message chứa `messageType/version/id`, correlation ID, job/experiment/candidate/dataset references, strategy version và attempt; không chứa Candle dataset, secret, class name hay Java serialized object.

### State and stop conditions

- Experiment: `CREATED → QUEUED → RUNNING → COMPLETED|FAILED|STOP_REQUESTED → STOPPED`.
- Job: `QUEUED → RUNNING → SUCCEEDED|RETRY_SCHEDULED|FAILED|CANCELLED`.
- Mỗi Search có ít nhất một giới hạn: max candidates, max duration hoặc iterations without improvement.
- Stop ngừng candidate mới, cancel queued jobs và cho running job kết thúc tại safe checkpoint.
- At-least-once delivery yêu cầu unique constraints, processed-message record và idempotent handlers.

## 5. News và Sentiment

```mermaid
sequenceDiagram
    participant Provider as News Provider
    participant News as News Module
    participant DB as PostgreSQL
    participant Worker
    participant ML as Sentiment Service
    Provider-->>News: provider news payload
    News->>News: normalize/deduplicate
    News->>DB: NewsItem(PENDING)
    DB-->>Worker: analysis job
    Worker->>ML: normalized text + IDs/hash
    alt valid response
        ML-->>Worker: label/score/model version
        Worker->>DB: SentimentResult + ANALYZED
    else timeout/unavailable
        Worker->>DB: retry state or FAILED
    end
```

- News Provider nằm sau abstraction và trả canonical NewsItem.
- Sentiment Service stateless, không crawl, không truy cập shared database và không quyết định Strategy signal.
- Timeout/retry/circuit breaker cô lập failure; Market/technical strategies không chờ Sentiment.
- Sentiment dùng trong Backtest phải frozen theo event time/model version để tránh look-ahead bias.

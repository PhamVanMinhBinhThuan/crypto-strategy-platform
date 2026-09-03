# API Examples

Tài liệu này cung cấp payload mẫu để Frontend, Backend và Worker cùng hình dung cách giao tiếp. Đây **không phải contract chính thức**. Tên endpoint và field phải được cập nhật theo feature spec, OpenAPI và WebSocket contract sau khi được duyệt.

Các ví dụ tuân theo [API Conventions](conventions.md) và [API Error Catalog](error-catalog.md). Giá trị ID, thời gian, giá và kết quả bên dưới chỉ là dữ liệu minh họa.

## 1. Market Dashboard

### 1.1. Lấy Historical Candles

```http
GET /api/v1/candles?pair=BTC%2FUSDT&timeframe=5m&startTime=2026-08-16T01%3A00%3A00Z&endTime=2026-08-16T02%3A00%3A00Z
Accept: application/json
X-Correlation-Id: 01JMARKETREQUEST000000000001
```

```json
{
  "items": [
    {
      "pair": "BTC/USDT",
      "timeframe": "5m",
      "openTime": "2026-08-16T01:00:00Z",
      "closeTime": "2026-08-16T01:05:00Z",
      "open": "59120.10",
      "high": "59280.50",
      "low": "59080.00",
      "close": "59210.25",
      "volume": "18.425",
      "closed": true
    }
  ],
  "nextCursor": null,
  "hasMore": false
}
```

### 1.2. Subscribe Realtime Candle

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

### 1.3. Realtime Candle Event

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

## 2. Strategy và Backtesting

### 2.1. Lấy danh sách Strategy

```http
GET /api/v1/strategies
Accept: application/json
```

```json
{
  "items": [
    {
      "strategyId": "ma-crossover",
      "version": "1.0.0",
      "displayName": "MA Crossover",
      "supportedSignals": ["BUY", "SELL", "HOLD"],
      "parameters": [
        {
          "name": "fastPeriod",
          "type": "INTEGER",
          "required": true,
          "minimum": 2,
          "maximum": 100,
          "defaultValue": 20
        },
        {
          "name": "slowPeriod",
          "type": "INTEGER",
          "required": true,
          "minimum": 3,
          "maximum": 300,
          "defaultValue": 50
        }
      ]
    }
  ],
  "nextCursor": null,
  "hasMore": false
}
```

### 2.2. Bắt đầu Backtest

```http
POST /api/v1/backtests
Content-Type: application/json
Accept: application/json
Idempotency-Key: backtest-demo-001
```

```json
{
  "datasetId": "01JDATASET00000000000000001",
  "strategy": {
    "strategyId": "ma-crossover",
    "version": "1.0.0",
    "parameters": {
      "fastPeriod": 20,
      "slowPeriod": 50
    }
  },
  "configuration": {
    "initialCapital": "10000.00",
    "feeRate": "0.001",
    "slippageRate": "0",
    "positionMode": "LONG_ONLY",
    "executionPriceRule": "NEXT_CANDLE_OPEN",
    "forceCloseAtEnd": true,
    "roundingMode": "HALF_EVEN"
  }
}
```

Response dự kiến: `202 Accepted`.

```json
{
  "backtestId": "01JBACKTEST0000000000000001",
  "jobId": "01JJOB00000000000000000001",
  "status": "QUEUED"
}
```

### 2.3. Đọc kết quả Backtest

```http
GET /api/v1/backtest-results/01JBACKTESTRESULT0000000001
Accept: application/json
```

```json
{
  "backtestResultId": "01JBACKTESTRESULT0000000001",
  "backtestId": "01JBACKTEST0000000000000001",
  "status": "COMPLETED",
  "metrics": {
    "totalReturn": "0.1245",
    "winRate": "0.5833",
    "maximumDrawdown": "0.0710",
    "numberOfTrades": 24
  },
  "trades": [
    {
      "tradeId": "01JTRADE000000000000000001",
      "sequence": 0,
      "side": "LONG",
      "entryTime": "2026-07-01T08:00:00Z",
      "entryPrice": "61200.00",
      "exitTime": "2026-07-01T12:00:00Z",
      "exitPrice": "61850.00",
      "quantity": "0.153186274510",
      "entryFee": "9.375000000000",
      "exitFee": "9.474570588235",
      "totalFee": "18.849570588235",
      "profitLoss": "80.727352941176",
      "postTradeCash": "10080.727352941176",
      "exitReason": "STRATEGY_SELL"
    }
  ],
  "provenance": {
    "experimentId": "01JEXPERIMENT0000000000001",
    "candidateId": "01JCANDIDATE00000000000001",
    "jobId": "01JJOB00000000000000000001",
    "successfulAttemptId": "01JATTEMPT00000000000000001",
    "manifestFingerprint": "sha256:manifest",
    "datasetFingerprint": "sha256:dataset",
    "strategyFingerprint": "sha256:strategy",
    "resultFingerprint": "sha256:result"
  },
  "assumptions": {
    "assumptionsVersion": "backtest-assumptions-v1",
    "initialCapital": "10000.000000000000",
    "feeRate": "0.001000000000",
    "slippageRate": "0.000000000000",
    "positionMode": "LONG_ONLY",
    "executionPriceRule": "NEXT_CANDLE_OPEN",
    "forceCloseAtEnd": true,
    "roundingMode": "HALF_EVEN"
  },
  "initialCapital": "10000.000000000000",
  "finalCapital": "11245.000000000000",
  "totalFees": "42.500000000000",
  "completedAt": "2026-07-01T12:00:01Z"
}
```

## 3. Search và Leaderboard

### 3.1. Bắt đầu Experiment

```http
POST /api/v1/experiments
Content-Type: application/json
Accept: application/json
Idempotency-Key: search-demo-001
```

```json
{
  "name": "BTC MA random search",
  "datasetId": "01JDATASET00000000000000001",
  "generator": {
    "generatorId": "random-search",
    "version": "1.0.0",
    "seed": 20260816
  },
  "searchSpace": {
    "strategyId": "ma-crossover",
    "strategyVersion": "1.0.0",
    "parameters": {
      "fastPeriod": { "minimum": 5, "maximum": 30 },
      "slowPeriod": { "minimum": 40, "maximum": 120 }
    }
  },
  "stopCondition": {
    "maximumCandidates": 100,
    "maximumDurationSeconds": 300
  },
  "topK": 10
}
```

Response `202 Accepted` được trả sau khi Experiment, Manifest, SEARCH Job, Search Run,
Outbox intent và idempotency receipt đã commit atomically.

```json
{
  "experimentId": "01JEXPERIMENT0000000000001",
  "jobId": "01JJOB00000000000000000002",
  "status": "QUEUED"
}
```

Header `Location: /api/v1/experiments/01JEXPERIMENT0000000000001` trỏ tới snapshot
authoritative. Reproduce dùng `POST /api/v1/experiments/{id}/reproductions`, cũng trả `202` và
identity mới; source không đổi, verification bắt đầu durable ở `PENDING` rồi chạy sau terminal.

### 3.2. Experiment Progress Event

```json
{
  "eventType": "EXPERIMENT_PROGRESS_UPDATED",
  "eventVersion": 1,
  "eventId": "01JEXPERIMENTEVENT000000001",
  "occurredAt": "2026-08-16T02:10:00Z",
  "correlationId": "01JSEARCHREQUEST00000000001",
  "subscriptionId": "experiment-01JEXPERIMENT0000000000001",
  "payload": {
    "experimentId": "01JEXPERIMENT0000000000001",
    "jobId": "01JJOB00000000000000000002",
    "status": "RUNNING",
    "completedWork": 35,
    "failedWork": 1,
    "totalWork": 100,
    "bestScore": "0.8125"
  }
}
```

### 3.3. Đọc Top-K Leaderboard

```http
GET /api/v1/experiments/01JEXPERIMENT0000000000001/leaderboard?limit=10
Accept: application/json
```

```json
{
  "experimentId": "01JEXPERIMENT0000000000001",
  "revisionId": "01JLEADERBOARD000000000001",
  "revision": 7,
  "topK": 10,
  "rankingPolicyVersion": "ranking-v1",
  "fingerprint": "sha256:leaderboard",
  "createdAt": "2026-08-16T02:11:01Z",
  "items": [
    {
      "rank": 1,
      "evaluationResultId": "01JEVALUATION0000000000001",
      "backtestResultId": "01JBACKTESTRESULT0000000001",
      "score": "0.8125",
      "maximumDrawdown": "0.0710",
      "evaluationFingerprint": "sha256:evaluation"
    }
  ],
  "nextCursor": null,
  "hasMore": false
}
```

## 4. News và Sentiment

### 4.1. Lấy danh sách News

```http
GET /api/v1/news-items?tradingPairId=01JPAIR0000000000000000001&limit=20
Accept: application/json
```

```json
{
  "items": [
    {
      "newsId": "01JNEWS000000000000000001",
      "title": "Example cryptocurrency market update",
      "source": "Example News",
      "url": "https://example.com/news/crypto-market-update",
      "publishedAt": "2026-08-16T01:30:00Z",
      "analysisStatus": "ANALYZED",
      "relatedAssetIds": ["01JASSET000000000000000001"],
      "sentiment": {
        "label": "POSITIVE",
        "confidence": "0.82",
        "polarityScore": "0.64"
      }
    }
  ],
  "nextCursor": null,
  "hasMore": false
}
```

## 5. Error

### 5.1. Strategy parameters không hợp lệ

Response: `422 Unprocessable Content`.

```json
{
  "code": "STRATEGY_PARAMETERS_INVALID",
  "message": "Strategy parameters are invalid.",
  "details": {
    "fieldErrors": [
      {
        "field": "strategy.parameters.fastPeriod",
        "reason": "must be less than slowPeriod"
      }
    ],
    "retryable": false
  },
  "correlationId": "01JBACKTESTREQUEST0000000001",
  "timestamp": "2026-08-16T02:05:00Z"
}
```

### 5.2. Async Job thất bại

Request đọc Job vẫn trả `200 OK`; trạng thái thất bại nằm trong resource.

```json
{
  "jobId": "01JJOB00000000000000000002",
  "experimentId": "01JEXPERIMENT0000000000001",
  "candidateId": "01JCANDIDATE00000000000001",
  "type": "BACKTEST",
  "status": "FAILED",
  "totalWork": 1,
  "completedWork": 0,
  "failedWork": 1,
  "bestScore": null,
  "queuedAt": "2026-08-16T02:00:00Z",
  "startedAt": "2026-08-16T02:00:01Z",
  "finishedAt": "2026-08-16T02:15:00Z",
  "nextRetryAt": null,
  "failure": {
    "code": "JOB_EXECUTION_TIMEOUT",
    "message": "The job exceeded its execution timeout."
  },
  "createdAt": "2026-08-16T02:00:00Z",
  "updatedAt": "2026-08-16T02:15:00Z"
}
```

## 6. Readiness còn bị chặn

Chỉ Start/Reproduce Experiment chưa ready vì Search Coordinator nằm ngoài F-009 chưa tồn
tại. Backtest đơn lẻ, các authoritative read, stop/cancel, realtime Candle/workload, News
và internal audit đã có transport boundary; readiness cuối vẫn cần PostgreSQL/Redis smoke
trong môi trường integration.
- Pagination limit, time-range limit và sorting được hỗ trợ.
- WebSocket payload cuối cùng và compatibility rule.
- Internal API giữa Java Worker và Python Sentiment Service.

Sau khi các mục trên được duyệt, cập nhật ví dụ này đồng thời với `openapi.yaml` và `websocket-events.md`; không coi `examples.md` là nguồn contract độc lập.

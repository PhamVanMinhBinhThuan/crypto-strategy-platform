# F014 Demo Profiles

Hai profile dùng cùng public contract và business flow. Khác biệt duy nhất là nguồn adapter: `LIVE` dùng service/provider thật, còn `FIXTURE` dùng deterministic adapters để fallback hoặc diễn tập. Không profile nào được lưu credential thật trong repository.

## Quy tắc chung

- Copy tên biến từ `.env.example` và `apps/web/.env.example` sang môi trường local không được commit.
- Giá trị `<...>` dưới đây chỉ là placeholder; không copy secret vào tài liệu, screenshot, log hoặc browser variable.
- Browser chỉ được nhận `NEXT_PUBLIC_*` values. Database password, service token và Redis credential không được đưa vào `apps/web`.
- Dùng cùng symbol, timeframe, strategy/search configuration giữa các lần chạy để evidence có thể so sánh.
- Mỗi lần demo ghi profile, commit SHA, UTC timestamp và mọi override không nhạy cảm.

## Cấu hình nghiệp vụ cố định

| Thành phần | Giá trị demo |
|---|---|
| Symbol chính | `BTCUSDT` |
| Bốn chart | `5m`, `15m`, `1h`, `4h` |
| Strategy | Moving Average, RSI, Bollinger Bands, Support/Resistance |
| Composite | Chọn ít nhất hai Strategy; conflict rule phải hiển thị/giải thích được |
| Generator | Random Search |
| Seed | Một số nguyên cố định và ghi trong evidence của lần chạy |
| Stop condition | Candidate limit hữu hạn; giá trị chính thức được khóa sau dry run |
| Leaderboard | Top-K cố định; giá trị chính thức được khóa sau dry run |
| Result metrics | Return, Win Rate, Maximum Drawdown, Number of Trades |

## LIVE profile

### Mục đích

Đây là profile chính để lấy evidence cho Binance historical/realtime data, luồng API–Worker–database–Redis, Search/Backtest/Evaluation/Leaderboard và News/Sentiment thật.

### Non-secret environment shape

```dotenv
# API + Worker durable storage; giá trị thật chỉ nằm ở local/CI secret store
DATABASE_URL=<jdbc-postgresql-url>
DATABASE_USERNAME=<server-username>
DATABASE_PASSWORD=<server-password>

# Authentication metadata; anon key là browser-safe nhưng vẫn inject ngoài tài liệu
SUPABASE_JWT_ISSUER=<supabase-auth-issuer>
SUPABASE_JWT_JWKS_URI=<supabase-jwks-uri>
SUPABASE_JWT_AUDIENCE=authenticated

# API market adapter
PLATFORM_MARKET_DATA_PROVIDER=binance
PLATFORM_MARKET_DATA_BINANCE_REST_BASE_URL=https://api.binance.com
PLATFORM_MARKET_DATA_BINANCE_WEBSOCKET_BASE_URL=wss://stream.binance.com:9443
PLATFORM_REALTIME_MAX_CANDLE_SUBSCRIPTIONS=4
PLATFORM_FEATURE_SEARCH_START_ENABLED=true
PLATFORM_FEATURE_SEARCH_REPRODUCE_ENABLED=true

# News + isolated Sentiment service
NEWS_ENABLED=true
SENTIMENT_SERVICE_URL=http://localhost:8000
SENTIMENT_SERVICE_TOKEN=<shared-service-token-min-16-chars>
SENTIMENT_BUNDLE_PATH=<absolute-local-model-bundle-path>

# Web: chỉ browser-safe variables
NEXT_PUBLIC_SUPABASE_URL=<https-supabase-project-url>
NEXT_PUBLIC_SUPABASE_ANON_KEY=<public-anon-key>
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api/v1
NEXT_PUBLIC_WS_URL=ws://localhost:8080/ws
NEXT_PUBLIC_ENABLE_FIXTURES=false
```

Worker Redis mặc định dùng `localhost:6379`. Nếu override, dùng Spring relaxed-binding variables tương ứng với `worker.redis.*`; password/username chỉ inject ngoài repository. Mỗi Worker instance phải có consumer name riêng khi chạy nhiều instance.

### Readiness gate

LIVE chỉ được bắt đầu khi:

1. PostgreSQL/Supabase và Redis reachable.
2. API liveness/readiness trả healthy trên port `8080`.
3. Worker readiness trả healthy trên port `8081`.
4. Sentiment `/health/live` pass; `/health/ready` pass hoặc được ghi rõ degraded nếu đang chạy failure scenario.
5. Web hiển thị **không có** badge `FIXTURE DATA`.
6. Market response/progress đến từ public API/WebSocket, không từ mock client.

Nếu một dependency chính không ready, không được âm thầm đổi sang fixture rồi ghi evidence là LIVE.

## FIXTURE fallback profile

### Mục đích và giới hạn

Profile này dùng để diễn tập UI, trình bày cấu trúc flow hoặc tiếp tục buổi demo khi external dependency không phục hồi kịp. Nó không chứng minh Binance live, remote persistence, Redis recovery hoặc external Sentiment integration.

```dotenv
NEXT_PUBLIC_SUPABASE_URL=https://example.invalid
NEXT_PUBLIC_SUPABASE_ANON_KEY=<non-secret-placeholder-at-least-16-chars>
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api/v1
NEXT_PUBLIC_WS_URL=ws://localhost:8080/ws
NEXT_PUBLIC_ENABLE_FIXTURES=true
```

- Chỉ bật trong development/demo; `parsePublicEnvironment` từ chối fixture mode khi production.
- Application shell và Search form phải hiển thị badge `FIXTURE DATA` trong suốt phiên.
- Evidence ghi rõ `mode=FIXTURE`, lý do fallback và tiêu chí nào không thể Verified.
- Fixture phải deterministic/versioned; không sửa dữ liệu giữa buổi để tạo kết quả đẹp hơn.

## Quy tắc chuyển profile

### LIVE → FIXTURE

Chỉ chuyển khi dependency ngoài không phục hồi trong thời gian đã định của runbook. Dừng phiên live hiện tại, ghi incident/dependency, khởi động lại Web với fixture flag và xác nhận badge trước khi tiếp tục.

### FIXTURE → LIVE

Không hot-switch trong cùng process. Tắt Web fixture, xác nhận dependencies ready, khởi động lại với `NEXT_PUBLIC_ENABLE_FIXTURES=false`, rồi kiểm tra badge đã biến mất và request đi qua API thật.

Không trộn screenshot/log/result của hai profile vào cùng một Evidence Record.

## Cleanup

- Dừng Web, API, Worker và Sentiment theo thứ tự ngược startup.
- Giữ durable evidence/result cần cho reproduction; chỉ xóa dữ liệu demo theo cơ chế cleanup được owner công bố.
- Không xóa hoặc overwrite accepted Result, Trade, Evaluation, Leaderboard revision hay reproduction nguồn.
- Unset credential khỏi shell/session dùng để trình chiếu và không commit file local environment.

## Inventory phần nâng cao

T003 chỉ ghi nhận ứng viên đã thấy trong code; chưa mục nào được tự khai điểm nâng cao cho tới khi có code link, test/demo và measurement chứng minh phần vượt yêu cầu cốt lõi.

| Ứng viên | Dấu hiệu implementation hiện có | Trạng thái | Evidence cần trước khi demo nâng cao |
|---|---|---|---|
| Redis Streams | Worker publishers/consumers, outbox và recovery components | CANDIDATE — chưa Verified | Demo queue purpose, failure/retry và durable recovery; phân biệt phần core reliability với phần vượt yêu cầu |
| Worker pool/concurrency | Fixed thread pool, concurrency limits và per-experiment in-flight controls | CANDIDATE — chưa Verified | Benchmark 1 so với 3 Worker hoặc concurrency profile, throughput và duplicate safety |
| Loop engineering | Generate → Backtest → Evaluate → Rank, stop/reconciliation components | CANDIDATE — chưa Verified | Trace trọn vòng, progress, stop condition và deterministic evidence; chỉ khai phần vượt Random Search cơ bản |
| Sentiment service tách biệt | FastAPI runtime, versioned model metadata, circuit breaker/concurrency guard | CANDIDATE — chưa Verified | Demo model/service replacement hoặc isolation có measurement; pipeline sentiment cơ bản không tự động là nâng cao |

Hiện chưa thấy implementation cho Domain-guided Search, Genetic/Evolutionary/Bayesian/RL/LLM/Agent Search, trading Long/Short và risk rules nâng cao, multiple exchanges, price prediction, market regime, Kafka/RabbitMQ, CQRS hoặc Event Sourcing. Những mục này không được đưa vào demo như tính năng đã có.

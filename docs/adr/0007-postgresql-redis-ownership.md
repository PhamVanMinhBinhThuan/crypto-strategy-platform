# ADR-0007: Phân chia trách nhiệm PostgreSQL/Supabase và Redis

**Status**: Proposed
**Date**: 2026-08-11
**Owners**: Tiến Luật

## Context

Crypto StrategyLab có nhiều nhóm dữ liệu với đặc tính khác nhau:

- Candle lịch sử và Candle realtime;
- Strategy Definition, version và parameters;
- Dataset và Experiment cần tái lập;
- Candidate Job, Backtest Result, Trade và Evaluation Metrics;
- Top-K Leaderboard;
- News và Sentiment Result;
- queue, progress, connection state và cache ngắn hạn.

PostgreSQL phù hợp dữ liệu cần transaction, quan hệ và truy vấn lâu dài. Redis phù hợp cache, queue và trạng thái ngắn hạn nhưng không nên là nguồn dữ liệu duy nhất cho Experiment hoặc kết quả Backtest.

Nhóm dự kiến sử dụng Supabase để cung cấp PostgreSQL cho môi trường dùng chung/demo. Nếu Frontend truy cập trực tiếp Supabase và Backend cũng truy cập cùng bảng, business rule và data ownership sẽ bị phân tán. Nếu lưu job/result chỉ trong Redis, dữ liệu có thể mất hoặc khó tái lập khi Redis restart.

Theo [ADR-0001: Sử dụng Modular Monolith](0001-modular-monolith.md) và [ADR-0002: Ranh giới giữa các Module](0002-module-boundaries.md), dữ liệu phải có module owner rõ ràng. Theo [ADR-0006: Queue và Worker cho Backtest/Search](0006-queue-worker-backtesting.md), PostgreSQL là nguồn sự thật còn Redis Streams dùng để phân phối job.

## Decision

### 1. Sử dụng Supabase-hosted PostgreSQL làm database bền vững

Môi trường shared development/demo sử dụng PostgreSQL do Supabase cung cấp. Java Backend kết nối bằng PostgreSQL/JDBC thông qua persistence adapter.

Local development có thể chạy PostgreSQL tương thích bằng Docker Compose hoặc Supabase Local. Code nghiệp vụ không được phụ thuộc API riêng của Supabase, để có thể chạy trên PostgreSQL tiêu chuẩn.

```text
apps/web
   ↓ REST / WebSocket
apps/api
   ↓ Persistence Ports
modules/persistence
   ↓ JDBC/PostgreSQL driver
Supabase PostgreSQL hoặc Local PostgreSQL
```

Frontend không truy cập trực tiếp bảng Supabase trong MVP. Không đặt Supabase service-role key trong Next.js client hoặc browser bundle.

### 2. PostgreSQL là nguồn sự thật

Những dữ liệu sau phải được lưu bền vững trong PostgreSQL/Supabase:

| Nhóm dữ liệu             | Ví dụ                                                                        |
| ------------------------ | ---------------------------------------------------------------------------- |
| Market reference/history | Trading Pair, closed Candle, Dataset metadata                                |
| Strategy                 | Strategy Definition, plugin ID, version, parameters, Composite Definition    |
| Experiment               | Dataset reference, Strategy versions, configuration, random seed, trạng thái |
| Job/Candidate            | Job ID, candidate ID, status, attempt, error summary                         |
| Backtest                 | Trades, Return, Win Rate, Max Drawdown, Number of Trades                     |
| Evaluation/Ranking       | Score, Evaluation Result, Top-K read model hoặc revision                     |
| News                     | Title, content/summary, source, URL, published/crawled time, related coins   |
| Sentiment                | Label, score, model version, analyzed time                                   |
| Reliability              | Transactional Outbox, processed message/idempotency record                   |

Nếu Redis bị xóa hoàn toàn, hệ thống phải có thể khôi phục trạng thái nghiệp vụ quan trọng từ PostgreSQL và republish các job/event chưa hoàn thành.

### 3. Redis dùng cho dữ liệu tạm thời và phân phối công việc

Redis được sử dụng cho:

| Mục đích       | Dữ liệu                                                                            |
| -------------- | ---------------------------------------------------------------------------------- |
| Queue          | Redis Streams cho Search request, Backtest job, Candidate Evaluated và Dead Letter |
| Cache          | Historical query phổ biến, Strategy descriptor, Top-K read cache                   |
| Realtime state | Latest/open Candle, provider connection status, stream subscriber count            |
| Progress cache | Snapshot tiến trình Search để đọc nhanh, nhưng không phải bản duy nhất             |
| Rate limiting  | Đếm request/subscription theo connection hoặc client                               |
| Coordination   | Consumer group/pending job và lock ngắn hạn khi thật sự cần                        |

Redis không được dùng làm nơi duy nhất lưu:

- Strategy Definition/version;
- Experiment configuration;
- Backtest Result hoặc Trade;
- Evaluation Result hoặc Leaderboard lịch sử;
- News/Sentiment đã được chấp nhận;
- Outbox event chưa publish;
- credential hoặc secret dài hạn.

Mọi cache key phải có namespace và version, ví dụ:

```text
crypto-lab:v1:market:latest:BTCUSDT:5m
crypto-lab:v1:leaderboard:<experimentId>:top10
```

TTL cụ thể được cấu hình theo môi trường và feature plan, không hard-code rải rác trong business module.

### 4. Quyền sở hữu dữ liệu theo module

| Owner module                    | Dữ liệu sở hữu                                                     |
| ------------------------------- | ------------------------------------------------------------------ |
| `market-data`                   | Pair, Candle, Dataset market metadata                              |
| `strategy-core` / `combination` | Strategy Definition, version, parameters và Composite Definition   |
| `experiment`                    | Experiment Manifest, runtime status và reproduction metadata       |
| `search`                        | Candidate, Search configuration và Stop Condition state             |
| `backtesting`                   | Backtest Job execution, Trade và simulation result                 |
| `evaluation`                    | Metrics và Evaluation Result                                       |
| `leaderboard`                   | Ranking score, Top-K entry và revision                             |
| `news`                          | News Item, provider metadata, Sentiment Result và model version     |
| Platform persistence            | Outbox, processed message và migration metadata                    |

Quy tắc ownership:

1. Chỉ output port của owner module được phép ghi dữ liệu thuộc module đó.
2. Module khác không import trực tiếp repository implementation của owner.
3. Module khác đọc dữ liệu qua public query/application service hoặc read model đã công bố.
4. Foreign key vật lý không đồng nghĩa module được phép cập nhật bảng của nhau.
5. Cross-module report/read model được tạo rõ ràng, không dùng SQL join tùy ý trong business service.
6. Thay đổi schema phải có migration và review bởi owner của dữ liệu bị ảnh hưởng.

### 5. Tổ chức schema và migration

Thiết kế bảng/column cụ thể chỉ được chốt sau feature spec và plan. Khi thiết kế database, nhóm sẽ dùng logical schema hoặc table prefix để thể hiện ownership, ví dụ:

```text
market.*
strategy.*
experiment.*
news.*
platform.*
```

Nếu công cụ hoặc Supabase environment không thuận tiện với nhiều PostgreSQL schema, nhóm có thể dùng một application schema với table prefix, nhưng dependency/ownership rule không thay đổi.

Mọi thay đổi database phải:

- nằm trong migration file được version control;
- chạy được từ database trống theo đúng thứ tự;
- có migration rollback/forward-fix plan phù hợp;
- không chỉnh tay production/demo schema qua Supabase Dashboard mà không tạo migration tương ứng;
- có seed data riêng cho local/demo, không trộn vào migration production.

Công cụ migration Java/Supabase cụ thể được chốt trong implementation plan. Repository migration là nguồn sự thật, không phải trạng thái thủ công trên Dashboard.

### 6. Candle storage và cache

- Closed Candle được phép lưu PostgreSQL để dùng Historical Chart và Backtest.
- Open Candle có thể chỉ nằm trong memory/Redis và được cập nhật liên tục.
- Khi Candle đóng, hệ thống persist idempotently theo Candle identity của [ADR-0003: Market Data Provider Adapter](0003-market-data-adapter.md).
- Không lưu mọi realtime tick nếu MVP chỉ cần Candle/Kline update.
- Redis có thể cache historical range phổ biến; cache miss đọc PostgreSQL/provider.
- Cache Candle bị mất không làm mất historical source of truth.

### 7. Experiment, version và kết quả

Một Experiment phải tham chiếu chính xác:

- Dataset ID/version/checksum;
- Strategy/plugin ID và version;
- exact parameters;
- Composite policy/version;
- pair, timeframe và historical range;
- fee/backtest assumptions;
- random seed và Search configuration;
- result, metrics và trades tương ứng.

Quy tắc versioning và tái lập được chi tiết trong [ADR-0009: Reproducible Experiments](0009-reproducible-experiments.md).

Không overwrite Strategy Definition hoặc Result cũ khi logic thay đổi. Tạo version/result mới và giữ reference của Experiment cũ.

### 8. Leaderboard

Evaluation Result trong PostgreSQL là dữ liệu gốc để tính ranking. Top-K có thể được:

- lưu thành read model/revision trong PostgreSQL để audit;
- cache trong Redis để đọc nhanh và phát realtime;
- rebuild từ Evaluation Result nếu Redis cache mất.

Ranking Handler phải update Top-K idempotently theo Candidate/Evaluation ID. Redis cache không được quyết định một candidate đã được xếp hạng hay chưa.

### 9. Consistency giữa PostgreSQL và Redis

Không dùng distributed transaction giữa PostgreSQL và Redis.

Đối với command/event quan trọng:

1. Ghi business state và Outbox Event trong cùng PostgreSQL transaction.
2. Outbox Publisher gửi message sang Redis Streams.
3. Consumer xử lý idempotently.
4. Cache chỉ được invalidate/update sau khi transaction database thành công.
5. Recovery process tìm Job/Outbox chưa hoàn thành để republish.

Quy trình này tuân theo Transactional Outbox trong [ADR-0006: Queue và Worker](0006-queue-worker-backtesting.md).

### 10. Failure behavior

#### Khi Redis lỗi

- API vẫn có thể đọc dữ liệu bền vững từ PostgreSQL nếu use case không cần queue/cache.
- Search/Backtest job mới được ghi PostgreSQL/Outbox và chờ publish lại.
- Worker queue và cache tạm thời không khả dụng.
- Market realtime có thể tiếp tục qua in-process adapter/WebSocket nếu không phụ thuộc Redis Pub/Sub.
- Không đánh dấu Job đã enqueue nếu message chưa được publish/recoverable.

#### Khi PostgreSQL/Supabase lỗi

- Không chấp nhận command cần ghi bền vững như Start Search hoặc lưu Backtest.
- Không coi Redis cache là dữ liệu chính để tiếp tục ghi kết quả.
- Realtime Market forwarding có thể tiếp tục ở chế độ giới hạn nếu không cần persistence.
- UI phải hiển thị trạng thái degraded/error rõ ràng.

#### Khi cả hai phục hồi

- Outbox/recovery republish Job chưa hoàn thành;
- cache được warm lại từ PostgreSQL/provider;
- duplicate được xử lý bằng idempotency constraint;
- không tự động tạo Experiment mới thay cho Experiment cũ.

### 11. Supabase security

- Database password/service credential chỉ tồn tại ở Backend/Worker environment.
- `.env` thật không commit Git; repository chỉ chứa `.env.example`.
- Frontend không nhận service-role key.
- Nếu Supabase REST/GraphQL API được bật, schema business không được public-write ngoài Backend.
- RLS có thể dùng như defense-in-depth, nhưng không thay thế authorization/business validation ở Java Backend.
- Demo seed không chứa secret hoặc dữ liệu nhạy cảm.
- Connection pool phải có giới hạn phù hợp với Supabase plan và số Worker.

## Alternatives Considered

- **Chỉ dùng PostgreSQL**: Đơn giản hạ tầng và có thể làm queue bằng `SKIP LOCKED`; khả thi, nhưng Redis Streams/cache phù hợp hơn với queue/realtime state đã chọn trong ADR-0006.
- **Chỉ dùng Redis**: Nhanh nhưng không phù hợp dữ liệu quan hệ, audit và reproducibility; mất Redis có thể mất toàn bộ kết quả.
- **Frontend truy cập trực tiếp Supabase**: Dựng CRUD nhanh nhưng bỏ qua Java Backend boundary, phân tán validation và làm lộ data model cho UI.
- **Database riêng cho mỗi module**: Isolation mạnh nhưng tăng migration, connection, transaction và vận hành quá mức cho Modular Monolith MVP.
- **MongoDB/document database**: Linh hoạt document nhưng Experiment, Strategy Version, Trade và Result có quan hệ rõ; PostgreSQL phù hợp hơn.
- **Time-series database riêng cho Candle**: Có lợi khi dữ liệu rất lớn nhưng chưa có bằng chứng cần thêm hạ tầng trong MVP.
- **Redis Pub/Sub thay Redis Streams cho Job**: Nhẹ nhưng không giữ pending/ack và dễ mất job khi consumer offline.

## Consequences

### Positive

- Phân biệt rõ durable state và ephemeral/cache state.
- Experiment và Backtest Result có thể audit, truy vấn và tái lập.
- Redis có thể mất cache mà không làm mất dữ liệu nghiệp vụ.
- Worker queue scale được bằng Redis Consumer Groups.
- Frontend không bị phụ thuộc schema Supabase.
- Có thể đổi Supabase sang PostgreSQL tiêu chuẩn với ảnh hưởng nhỏ.
- Data ownership hỗ trợ module boundary và review migration rõ ràng.

### Negative

- Phải vận hành và theo dõi cả PostgreSQL/Supabase lẫn Redis.
- Cần cache invalidation, Outbox Publisher và recovery process.
- Dữ liệu có thể eventual consistent giữa PostgreSQL read model và Redis cache.
- Logical ownership trong shared database cần discipline và architecture review.
- Supabase connection limit có thể ảnh hưởng khi scale nhiều Worker.
- Không tận dụng CRUD trực tiếp từ Supabase Frontend SDK trong MVP.

## Affected Components

- `modules/persistence`
- `modules/market-data`
- `modules/strategy-core`
- `modules/combination`
- `modules/search`
- `modules/backtesting`
- `modules/evaluation`
- `modules/experiment`
- `modules/leaderboard`
- `modules/news`
- `apps/api`
- `apps/worker`
- `apps/web`
- `infra/database`
- `infra/compose`
- Supabase project và Redis instance

## Validation

- Chạy migration từ database trống và seed được môi trường demo.
- Xác nhận Frontend chỉ gọi Java REST/WebSocket, không chứa Supabase service-role key.
- Xóa toàn bộ Redis cache và rebuild Top-K/cache từ PostgreSQL.
- Tạm dừng Redis, tạo Search request và xác nhận Job/Outbox không mất; bật lại để publish.
- Publish duplicate Job/Event và xác nhận Result/Leaderboard không bị ghi trùng.
- Xác nhận closed Candle persist idempotently; open Candle update không tạo nhiều hàng trùng.
- Dùng test kiến trúc/code review để ngăn module truy cập repository/table của owner khác.
- Chạy cùng test persistence trên local PostgreSQL và Supabase PostgreSQL.
- Xác nhận Experiment cũ vẫn tham chiếu đúng Strategy/Dataset version sau khi có version mới.
- Mô phỏng PostgreSQL lỗi và xác nhận hệ thống không ghi kết quả chỉ vào Redis.
- Kiểm tra connection pool không vượt giới hạn cấu hình khi tăng Worker.

## Risks and Mitigations

- **Risk**: Redis cache được dùng nhầm làm nguồn sự thật.

  **Mitigation**: Tài liệu ownership, repository interface rõ và recovery test xóa Redis định kỳ.

- **Risk**: Shared PostgreSQL tạo cross-module query tùy tiện.

  **Mitigation**: Schema/prefix theo owner, output port, migration review và read model được công bố.

- **Risk**: Frontend làm việc nhanh bằng cách gọi thẳng Supabase.

  **Mitigation**: Không cấp service-role key cho Frontend; API contract và code review bắt buộc qua Java Backend.

- **Risk**: Cache stale sau khi database cập nhật.

  **Mitigation**: Update/invalidate sau commit, TTL, versioned key và cho phép đọc lại source of truth.

- **Risk**: Redis Stream và PostgreSQL lệch trạng thái.

  **Mitigation**: Transactional Outbox, idempotent consumer và recovery scan.

- **Risk**: Supabase connection bị cạn khi nhiều Worker.

  **Mitigation**: Connection pool giới hạn, đo pool usage và tăng Worker theo khả năng database.

- **Risk**: Migration thủ công trên Dashboard không tồn tại trong Git.

  **Mitigation**: Repository migration là nguồn sự thật; mọi thay đổi phải có Pull Request.

- **Risk**: Lưu quá nhiều Candle làm database tăng nhanh.

  **Mitigation**: Chỉ lưu timeframe/dataset cần cho MVP, index/retention theo feature plan và không lưu mọi tick.

## References

- [Đề bài Crypto StrategyLab](../Crypto%20Strategy%20Lab%20%E2%80%93%20%C4%90%E1%BB%93%20%C3%A1n%20cu%E1%BB%91i%20k%E1%BB%B3.pdf)
- [Architecture Overview](../architecture/architecture-overview.md)
- [Data Model Overview](../architecture/data-model-overview.md)
- [Deployment View](../architecture/deployment-view.md)
- [API Conventions](../api/conventions.md)
- [ADR-0001: Modular Monolith](0001-modular-monolith.md)
- [ADR-0002: Module Boundaries](0002-module-boundaries.md)
- [ADR-0003: Market Data Adapter](0003-market-data-adapter.md)
- [ADR-0006: Queue và Worker](0006-queue-worker-backtesting.md)
- [ADR-0009: Reproducible Experiments](0009-reproducible-experiments.md)

## Supersession

- Supersedes: None
- Superseded by: None

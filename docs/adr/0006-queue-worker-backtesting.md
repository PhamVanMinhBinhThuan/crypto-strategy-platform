# ADR-0006: Queue và Worker cho Backtest/Search

**Status**: Proposed
**Date**: 2026-08-11
**Owners**: Luật

## Context

Random Search tạo nhiều Composite Strategy candidate. Mỗi candidate phải đi qua chuỗi xử lý:

```text
Generate
  → Backtest
  → Evaluate
  → Rank
  → Update Top-K
```

Một Backtest có thể tốn nhiều CPU và thời gian hơn HTTP request thông thường. Nếu API xử lý toàn bộ Search bằng một vòng lặp đồng bộ:

- request dễ timeout;
- API thread bị chiếm giữ;
- không thể theo dõi hoặc dừng Search an toàn;
- một candidate lỗi có thể làm hỏng toàn bộ vòng Search;
- khó retry từng job;
- khó tăng từ một lên nhiều Backtest Worker;
- Market Dashboard có thể bị ảnh hưởng bởi tải Backtest.

Đề bài yêu cầu có Stop Condition, theo dõi tiến trình, retry khi worker lỗi và có khả năng scale từ hàng trăm lên nhiều candidate hơn. Tuy nhiên, nhóm chỉ có bốn thành viên nên không chọn Kafka, Kubernetes hoặc nhiều Microservice phức tạp cho MVP.

Theo [ADR-0001: Sử dụng Modular Monolith](0001-modular-monolith.md), `apps/worker` có thể trở thành runtime riêng nhưng vẫn tái sử dụng các Java business modules. Theo [ADR-0002: Ranh giới giữa các Module](0002-module-boundaries.md), Search, Backtesting, Evaluation và Leaderboard phải giữ trách nhiệm độc lập.

## Decision

### 1. Xử lý Search/Backtest bất đồng bộ

API không chạy Random Search hoặc Backtest dài trực tiếp trong HTTP request.

Luồng chính:

```text
Frontend
   ↓ REST: Create Experiment / Start Search
apps/api
   ↓ lưu Experiment + Job
PostgreSQL/Supabase
   ↓ publish job
Redis Streams
   ↓ consumer group
apps/worker
   ↓ Backtest → Evaluate → persist result
PostgreSQL/Supabase
   ↓ progress/result event
Redis Streams → apps/api → WebSocket → Frontend
```

HTTP request tạo job trả về nhanh với:

```text
experimentId + jobId + initial status
```

Frontend theo dõi tiến trình và Leaderboard bằng WebSocket theo [ADR-0004: WebSocket cho dữ liệu Realtime](0004-websocket-realtime.md).

### 2. Redis Streams làm queue cho MVP

MVP sử dụng **Redis Streams với Consumer Groups** để phân phối job vì:

- Redis đã nằm trong tech stack;
- hỗ trợ nhiều consumer/worker;
- message được acknowledge sau khi xử lý;
- pending message có thể được worker khác reclaim;
- nhẹ hơn việc vận hành Kafka/RabbitMQ cho đồ án.

PostgreSQL/Supabase vẫn là nguồn dữ liệu chính cho Experiment, Job, Result và Trade. Redis không phải nguồn duy nhất để khôi phục trạng thái nghiệp vụ. Data ownership chi tiết được quyết định trong [ADR-0007: PostgreSQL và Redis Ownership](0007-postgresql-redis-ownership.md).

Các stream logic tối thiểu:

| Stream                   | Consumer              | Mục đích                                  |
| ------------------------ | --------------------- | ----------------------------------------- |
| `search.requests.v1`     | Search Coordinator    | Bắt đầu một Search Experiment             |
| `backtest.jobs.v1`       | Backtest Worker Group | Chạy Backtest và Evaluation cho candidate |
| `candidate.evaluated.v1` | Ranking Handler       | Cập nhật score và Top-K                   |
| `jobs.dead-letter.v1`    | Operator/Recovery     | Giữ job hết retry để kiểm tra             |

Tên stream vật lý có thể thêm environment prefix. Version trong tên stream hoặc message contract phải được duy trì khi có breaking change.

### 3. Runtime và trách nhiệm

| Thành phần          | Trách nhiệm                                                                                                |
| ------------------- | ---------------------------------------------------------------------------------------------------------- |
| `apps/api`          | Validate request, tạo Experiment, ghi command/outbox, trả ID, nhận event để đẩy WebSocket                  |
| Search Coordinator  | Đọc Search request, sinh candidate theo Search Algorithm, kiểm tra Stop Condition và giới hạn job đang chờ |
| Backtest Worker     | Tạo Strategy từ Registry, tải dataset, chạy Backtest, Evaluation và lưu kết quả                            |
| Ranking Handler     | Nhận Candidate Evaluated, tính score, cập nhật Top-K idempotently                                          |
| PostgreSQL/Supabase | Nguồn sự thật của Experiment, candidate, job, result, trades và leaderboard                                |
| Redis Streams       | Phân phối công việc và event bất đồng bộ                                                                   |

Search Generator chỉ sinh candidate và áp dụng Stop Condition. Backtester, Evaluator và Leaderboard không chứa logic Random Search. Strategy được tạo thông qua [ADR-0005: Strategy Contract và Plugin Registry](0005-strategy-plugin-registry.md).

`apps/worker` là Java/Spring Boot runtime riêng có thể chạy một hoặc nhiều instance. Worker dùng chung module `strategy-core`, `strategies`, `combination`, `backtesting` và `evaluation`; không sao chép business logic từ `apps/api`.

### 4. Job contract

Message queue chỉ chứa ID/reference và thông tin cần điều phối, không nhúng toàn bộ Candle dataset hoặc danh sách Trade.

Ví dụ `BacktestJob`:

```json
{
  "messageType": "BACKTEST_JOB_REQUESTED",
  "messageVersion": 1,
  "messageId": "01J...",
  "occurredAt": "2026-08-11T10:00:00Z",
  "correlationId": "01J...",
  "jobId": "01J...",
  "experimentId": "01J...",
  "candidateId": "01J...",
  "datasetId": "01J...",
  "strategyDefinitionId": "01J...",
  "strategyVersion": "1.0.0",
  "attempt": 1
}
```

Worker dùng các ID để tải dữ liệu chuẩn từ persistence. Message không chứa:

- Binance credential;
- toàn bộ historical candles;
- Java serialized object;
- Strategy class name nội bộ;
- thông tin nhạy cảm hoặc stack trace.

### 5. Delivery và idempotency

Redis Streams được sử dụng với delivery semantic **at-least-once**. Một job có thể được giao lại khi Worker crash trước lúc acknowledge.

Vì vậy:

- mọi message có `messageId` và `jobId` duy nhất;
- Worker kiểm tra trạng thái Job trước khi xử lý;
- `candidateId` là duy nhất trong một Experiment;
- ghi Backtest Result có unique/idempotency constraint theo Job hoặc Candidate;
- Ranking Handler không cộng một candidate hai lần;
- duplicate message đã hoàn tất được acknowledge mà không chạy lại business effect;
- Worker chỉ acknowledge sau khi kết quả cần thiết đã được lưu thành công.

Không giả định exactly-once delivery từ queue.

### 6. Transactional Outbox và phục hồi publish

Để tránh trạng thái “đã lưu Job nhưng chưa publish queue”:

1. API ghi Experiment/Job và Outbox Event trong cùng PostgreSQL transaction.
2. Outbox Publisher đọc event chưa publish và gửi vào Redis Stream.
3. Sau khi Redis xác nhận, event được đánh dấu đã publish.
4. Nếu publisher hoặc Redis lỗi, event vẫn còn trong PostgreSQL để retry.

Worker cũng ghi Result và event `CANDIDATE_EVALUATED` bằng cùng nguyên tắc. Outbox giúp PostgreSQL giữ vai trò nguồn sự thật khi Redis tạm thời không khả dụng.

MVP có thể dùng polling publisher đơn giản; không cần CDC platform hoặc Kafka Connect.

### 7. Trạng thái Experiment và Job

Experiment Search có state machine:

```text
CREATED
  → QUEUED
  → RUNNING
  → STOP_REQUESTED
  → STOPPED

RUNNING → COMPLETED
RUNNING → FAILED
```

Candidate Job có state machine:

```text
QUEUED → RUNNING → SUCCEEDED
                 → RETRY_SCHEDULED
                 → FAILED
                 → CANCELLED
```

State transition phải được kiểm tra ở Backend. Frontend không được tự đặt status bằng WebSocket payload.

### 8. Stop Condition và dừng thủ công

Mỗi Search Experiment phải có ít nhất một Stop Condition hữu hạn:

- `maxCandidates`;
- `maxDuration`;
- `maxIterationsWithoutImprovement`.

Không cho phép `while (true)` không có giới hạn.

Khi người dùng bấm Stop:

1. REST API chuyển Experiment sang `STOP_REQUESTED`.
2. Search Coordinator ngừng sinh candidate mới.
3. Job còn `QUEUED` được bỏ qua hoặc chuyển `CANCELLED`.
4. Job đang chạy kết thúc tại safe checkpoint; không kill thread đột ngột.
5. Kết quả hoàn thành trước lúc dừng vẫn được lưu và có thể xếp hạng theo policy đã chốt.
6. Khi không còn job cần xử lý, Experiment chuyển `STOPPED`.
7. WebSocket phát progress/status mới cho Frontend.

Pause/Resume có thể được thêm sau bằng state transition mới; không bắt buộc MVP.

### 9. Retry và Dead Letter

Lỗi được chia thành:

| Loại lỗi              | Ví dụ                                          | Xử lý                                    |
| --------------------- | ---------------------------------------------- | ---------------------------------------- |
| Transient             | Redis timeout, database connection tạm lỗi     | Retry có giới hạn và exponential backoff |
| Provider/Data tạm lỗi | Dataset đang được chuẩn bị                     | Retry theo policy của job                |
| Permanent validation  | Parameters sai, Strategy version không tồn tại | Không retry; đánh dấu FAILED             |
| Business result       | Strategy không tạo Trade                       | Kết quả hợp lệ, không phải lỗi           |
| Unknown/bug           | Exception không phân loại                      | Retry giới hạn, sau đó Dead Letter       |

Số lần retry, delay và timeout được cấu hình theo môi trường. Khi hết retry:

- Job chuyển `FAILED`;
- message được đưa vào `jobs.dead-letter.v1` hoặc ghi dấu cần can thiệp;
- progress tăng failed count;
- một candidate lỗi không làm toàn bộ Search thất bại trừ khi vượt ngưỡng failure policy.

### 10. Backpressure và giới hạn tài nguyên

Search Coordinator không enqueue toàn bộ không gian tìm kiếm cùng lúc.

Quy tắc:

- giới hạn số job `QUEUED + RUNNING` trên mỗi Experiment;
- sinh candidate theo batch nhỏ;
- Worker concurrency được cấu hình;
- mỗi job có execution timeout;
- Candle dataset được tham chiếu bằng `datasetId`, không copy vào Redis;
- ưu tiên bảo vệ Market API khỏi CPU/memory pressure của Backtest;
- khi queue vượt ngưỡng, API/Search giảm tốc độ sinh job thay vì tiếp tục vô hạn.

### 11. Scale Worker

Các instance `apps/worker` dùng cùng Redis Consumer Group:

```text
Redis Stream
   ├── Worker 1
   ├── Worker 2
   └── Worker 3
```

Mỗi message được giao cho một consumer trong group tại một thời điểm. Nếu Worker chết, pending job hết idle timeout có thể được Worker khác claim và xử lý idempotently.

Scale ngang chỉ thay đổi số Worker instance, không thay đổi Strategy, Backtest, Evaluation hoặc API contract.

### 12. Progress và observability

Hệ thống theo dõi tối thiểu:

- Experiment status;
- candidates generated/evaluated/succeeded/failed;
- queued và running jobs;
- current candidate và pipeline step;
- elapsed time;
- retry count và dead-letter count;
- queue lag/pending count;
- Backtest duration;
- current best score và Top-K revision;
- Worker instance/consumer xử lý Job.

Progress được lưu định kỳ trong PostgreSQL và phát thành event. Redis message không phải nơi duy nhất chứa progress.

Log phải có `correlationId`, `experimentId`, `candidateId` và `jobId` để truy vết toàn bộ flow.

## Alternatives Considered

- **Chạy đồng bộ trong HTTP request**: Đơn giản nhưng dễ timeout, chiếm API thread và không hỗ trợ scale/retry/stop tốt.
- **In-memory executor trong `apps/api`**: Không cần Redis nhưng mất job khi process restart và cạnh tranh tài nguyên với Market API.
- **PostgreSQL queue với `SKIP LOCKED`**: Giảm một hạ tầng và vẫn bền vững; có thể là fallback, nhưng Redis Streams đã có trong stack và phù hợp consumer group/progress event.
- **RabbitMQ**: Queue semantics mạnh nhưng thêm broker mới khi Redis đã đủ cho MVP.
- **Kafka**: Tốt cho event streaming quy mô lớn nhưng vận hành, partition và consumer management vượt nhu cầu đồ án.
- **Một Microservice riêng cho từng bước Generate/Backtest/Evaluate/Rank**: Scale độc lập nhưng làm tăng network contract và deployment complexity; MVP dùng module boundary cùng Worker runtime.
- **Exactly-once processing**: Khó đảm bảo xuyên Redis và PostgreSQL; at-least-once cộng idempotency thực tế và dễ kiểm chứng hơn.

## Consequences

### Positive

- API trả nhanh và không bị giữ bởi Backtest dài.
- Có thể scale nhiều Worker mà không sửa business modules.
- Retry và lỗi được cô lập theo candidate/job.
- Có Stop Condition, dừng thủ công và progress rõ ràng.
- PostgreSQL giữ trạng thái bền vững khi Redis hoặc Worker restart.
- Queue contract tách Search Generator khỏi Backtester/Ranking.
- Market Dashboard ít bị ảnh hưởng bởi CPU load của Backtest.

### Negative

- Cần vận hành cả PostgreSQL, Redis và Worker runtime.
- At-least-once yêu cầu idempotency ở nhiều bước.
- Transactional Outbox bổ sung bảng, publisher và cleanup.
- Kết quả Search có eventual consistency thay vì xuất hiện ngay lập tức.
- Debug luồng bất đồng bộ phức tạp hơn gọi hàm trực tiếp.
- Stop không thể luôn hủy ngay lập tức một computation đang ở giữa bước không an toàn.

## Affected Components

- `apps/api`
- `apps/worker`
- `modules/contracts`
- `modules/search`
- `modules/backtesting`
- `modules/evaluation`
- `modules/leaderboard`
- `modules/persistence`
- `infra/compose`
- PostgreSQL/Supabase schema
- Redis Streams và WebSocket progress flow

## Validation

- Tạo Search qua REST và xác nhận API trả ID trước khi Search hoàn thành.
- Chạy ít nhất hai Worker trong cùng consumer group và xác nhận job được phân phối.
- Kill một Worker khi đang xử lý, reclaim pending message và xác nhận không tạo Result trùng.
- Publish cùng `jobId` hai lần và xác nhận Backtest business effect/Leaderboard chỉ ghi một lần.
- Tạm dừng Redis sau khi API ghi Job; bật lại và xác nhận Outbox publish được Job.
- Mô phỏng lỗi transient và xác nhận retry theo policy.
- Mô phỏng lỗi permanent và xác nhận Job đi thẳng FAILED, không retry vô ích.
- Bấm Stop và xác nhận không sinh candidate mới, queued job được cancel/skip và Experiment kết thúc STOPPED.
- Chạy đến từng Stop Condition: max candidate, max duration và no improvement.
- Tăng số Worker và xác nhận throughput tăng mà API/Strategy contract không đổi.
- Kiểm tra WebSocket phát progress và `LEADERBOARD_UPDATED` mà UI không reload.
- Xác nhận Market Dashboard vẫn hoạt động khi Worker hoặc Search flow lỗi.

## Risks and Mitigations

- **Risk**: Duplicate delivery tạo Result hoặc Leaderboard entry trùng.

  **Mitigation**: Unique constraint, idempotency check theo Job/Candidate và handler có thể chạy lặp an toàn.

- **Risk**: Redis mất dữ liệu queue.

  **Mitigation**: PostgreSQL là nguồn sự thật, Transactional Outbox và recovery scan republish Job chưa hoàn thành.

- **Risk**: Search tạo job nhanh hơn Worker xử lý.

  **Mitigation**: Bounded in-flight jobs, batch generation, queue threshold và configurable concurrency.

- **Risk**: Worker crash để lại message pending.

  **Mitigation**: Consumer heartbeat, idle timeout, pending inspection và claim bởi Worker khác.

- **Risk**: Poison message bị retry vô hạn.

  **Mitigation**: Retry limit, error classification và Dead Letter Stream.

- **Risk**: Stop request không được Worker quan sát kịp.

  **Mitigation**: Worker kiểm tra Experiment state trước khi bắt đầu và tại safe checkpoint.

- **Risk**: Outbox tăng kích thước database.

  **Mitigation**: Index trạng thái publish, batch publisher và retention/cleanup định kỳ.

- **Risk**: Worker dùng quá nhiều CPU/RAM và ảnh hưởng máy demo.

  **Mitigation**: Giới hạn concurrency, job timeout, dataset size và cung cấp cấu hình demo nhỏ.

## References

- [Đề bài Crypto StrategyLab](../Crypto%20Strategy%20Lab%20%E2%80%93%20%C4%90%E1%BB%93%20%C3%A1n%20cu%E1%BB%91i%20k%E1%BB%B3.pdf)
- [Architecture Overview](../architecture/architecture-overview.md)
- [Data Flows](../architecture/data-flows.md)
- [WebSocket Events](../api/websocket-events.md)
- [UI Stitch Guide](../ui/stitch-guide.md)
- [ADR-0001: Modular Monolith](0001-modular-monolith.md)
- [ADR-0002: Module Boundaries](0002-module-boundaries.md)
- [ADR-0004: WebSocket Realtime](0004-websocket-realtime.md)
- [ADR-0005: Strategy Plugin Registry](0005-strategy-plugin-registry.md)
- [ADR-0007: PostgreSQL và Redis Ownership](0007-postgresql-redis-ownership.md)
- [ADR-0009: Reproducible Experiments](0009-reproducible-experiments.md)

## Supersession

- Supersedes: None
- Superseded by: None

# ADR-0008: Tách Sentiment Analysis thành Python Service

**Status**: Accepted
**Date**: 2026-08-11
**Owners**: Tiến Luật

## Context

Crypto StrategyLab cần:

- thu thập News liên quan đến coin/pair;
- chuẩn hóa và lưu News Item;
- phân loại sentiment thành Positive, Neutral hoặc Negative;
- hiển thị tỷ lệ sentiment trên giao diện;
- có khả năng sử dụng Sentiment Strategy trong tương lai.

News Collection và Sentiment Analysis là hai trách nhiệm khác nhau. News Collector làm việc với RSS, News API hoặc crawler. Sentiment Analysis sử dụng mô hình Machine Learning/NLP và có runtime, dependency, thời gian khởi động và failure mode khác Java Backend.

Nếu đưa thư viện ML Python trực tiếp vào Java Backend hoặc để News Crawler phụ thuộc cứng vào một model cụ thể:

- Java API khó build/deploy;
- lỗi hoặc hết bộ nhớ của model có thể ảnh hưởng Market Dashboard;
- đổi model buộc sửa News/Strategy flow;
- scale inference độc lập khó khăn;
- test News Collector phải khởi động model ML;
- dependency Python và Java bị trộn lẫn.

Đề bài yêu cầu chứng minh rằng News/Sentiment lỗi thì Realtime Chart vẫn hoạt động và việc thay Sentiment Model không làm Strategy Engine bị ảnh hưởng.

Theo [ADR-0001: Sử dụng Modular Monolith](0001-modular-monolith.md), Sentiment là một runtime boundary riêng. Theo [ADR-0002: Ranh giới giữa các Module](0002-module-boundaries.md), `apps/api`/`apps/worker` dùng integration contract để gọi Python Service; module `news` dùng contract nghiệp vụ của chính nó, còn Strategy chỉ nhận domain data qua `StrategyContext`. Không capability nào phụ thuộc implementation của model Python.

## Decision

### 1. Tách News Collection và Sentiment Analysis

Ranh giới trách nhiệm:

| Thành phần                 | Trách nhiệm                                                                              |
| -------------------------- | ---------------------------------------------------------------------------------------- |
| `modules/news`             | News Provider contract, thu thập, chuẩn hóa, deduplicate News Item; sở hữu Sentiment Result và output port |
| `modules/persistence`      | Lưu News Item và Sentiment Result qua output port                                        |
| `apps/api` / `apps/worker` | Điều phối collect/store/analyze, timeout, retry và phát trạng thái cho UI                |
| `apps/sentiment`           | Python FastAPI service, tải model và phân tích text                                      |
| `apps/web`                 | Hiển thị News và Sentiment qua Java API, không gọi Python trực tiếp                      |

Luồng chính:

```text
RSS / News API / Crawler Adapter
             ↓
Java News Module: Normalize + Deduplicate
             ↓
PostgreSQL: Store News Item
             ↓
Analysis Job / Worker
             ↓ HTTP
Python FastAPI Sentiment Service
             ↓
Java validates response
             ↓
PostgreSQL: Store Sentiment Result
             ↓
Java API → Frontend
```

Python Sentiment Service không crawl News, không gọi Binance và không quyết định Strategy signal trong MVP.

### 2. News Item thuộc Java News Module

Canonical News Item tối thiểu gồm:

| Field            | Ý nghĩa                                  |
| ---------------- | ---------------------------------------- |
| `newsId`         | ID nội bộ ổn định                        |
| `title`          | Tiêu đề đã chuẩn hóa                     |
| `content`        | Nội dung hoặc text dùng phân tích        |
| `source`         | Tên nguồn                                |
| `url`            | Liên kết bài gốc                         |
| `publishedAt`    | Thời gian bài được phát hành             |
| `crawledAt`      | Thời gian hệ thống thu thập              |
| `relatedCoins`   | Danh sách coin/pair liên quan            |
| `contentHash`    | Hỗ trợ deduplicate và cache kết quả      |
| `analysisStatus` | PENDING, ANALYZING, ANALYZED hoặc FAILED |

Provider-specific response không được đưa trực tiếp vào Sentiment Service hoặc Frontend.

### 3. Sentiment Service contract

`apps/sentiment` cung cấp HTTP API nội bộ có version:

```text
POST /api/v1/sentiment/analyze
```

Request ví dụ:

```json
{
  "requestId": "01J...",
  "newsId": "01J...",
  "title": "Bitcoin rises after institutional adoption",
  "content": "Normalized article text...",
  "language": "en",
  "contentHash": "sha256:..."
}
```

Response ví dụ:

```json
{
  "requestId": "01J...",
  "newsId": "01J...",
  "label": "POSITIVE",
  "confidence": 0.82,
  "polarityScore": 0.64,
  "modelVersion": "crypto-sentiment-v1.0.0",
  "analyzedAt": "2026-08-11T10:00:00Z"
}
```

Quy tắc contract:

- `label` chỉ nhận `POSITIVE`, `NEUTRAL` hoặc `NEGATIVE`;
- `confidence` nằm trong `[0, 1]`;
- `polarityScore` nằm trong `[-1, 1]`;
- `modelVersion` bắt buộc;
- timestamp dùng ISO-8601 UTC;
- `requestId` và `newsId` được trả lại để correlation/idempotency;
- Python không trả tên class model, stack trace hoặc file path nội bộ;
- Java Backend validate response trước khi lưu.

Endpoint batch có thể được bổ sung trong feature plan nếu cần throughput, nhưng phải sử dụng cùng item/result contract và giới hạn batch size.

### 4. Service stateless và không truy cập shared database

Python Sentiment Service:

- tải model khi khởi động;
- nhận text và trả kết quả;
- không đọc/ghi trực tiếp PostgreSQL/Supabase;
- không truy cập Redis Stream của Search/Backtest;
- không giữ business state của News Item;
- có thể chạy nhiều instance độc lập.

Module `news` sở hữu News Item và Sentiment Result; Java Backend/Worker điều phối qua public API và `persistence` triển khai output port của `news`. Cách này tránh hai runtime cùng sửa một bảng và giữ data ownership theo [ADR-0007: PostgreSQL/Supabase và Redis](0007-postgresql-redis-ownership.md).

### 5. Xử lý bất đồng bộ

Sentiment Analysis không nằm trên request path của Market Dashboard. Sau khi lưu News Item:

1. News Item có trạng thái `PENDING`.
2. Worker nhận analysis job hoặc tìm item đang chờ.
3. Trạng thái chuyển `ANALYZING`.
4. Worker gọi Python Service.
5. Response hợp lệ được lưu thành Sentiment Result và trạng thái `ANALYZED`.
6. Lỗi hết retry chuyển trạng thái `FAILED` nhưng giữ nguyên News Item.

News có thể hiển thị trước với trạng thái “Đang phân tích”. Market Chart, Strategy kỹ thuật và Backtest không chờ Sentiment Service.

Queue/job cụ thể có thể tái sử dụng nguyên tắc at-least-once, idempotency và Outbox của [ADR-0006: Queue và Worker](0006-queue-worker-backtesting.md), nhưng dùng job type/stream riêng để không cạnh tranh trực tiếp với Backtest queue.

### 6. Timeout, retry và circuit breaker

Java client gọi Sentiment Service phải có:

- connect timeout và response timeout cấu hình được;
- retry có giới hạn với exponential backoff cho timeout, `429` và `5xx`;
- không retry request `4xx` do dữ liệu không hợp lệ;
- circuit breaker mở khi lỗi liên tiếp vượt ngưỡng;
- concurrency limit để không làm model quá tải;
- correlation ID xuyên suốt log Java/Python.

Khi circuit breaker mở:

- không tiếp tục gọi model liên tục;
- News mới giữ trạng thái `PENDING` hoặc `FAILED_RETRYABLE` theo feature model;
- UI hiển thị Sentiment Service tạm không khả dụng;
- Market Dashboard và các module khác tiếp tục hoạt động.

Ngưỡng timeout, retry và circuit breaker được cấu hình theo môi trường, không hard-code trong business logic.

### 7. Idempotency và cache

- Một `newsId + contentHash + modelVersion` chỉ tạo một Sentiment Result logic.
- Gọi lại cùng request không được tạo kết quả trùng.
- Nếu content thay đổi, `contentHash` thay đổi và được phân tích lại.
- Nếu model version thay đổi, tạo Sentiment Result version mới; không overwrite kết quả cũ cần cho Experiment.
- Kết quả có thể cache theo `contentHash + modelVersion`, nhưng PostgreSQL vẫn là nguồn sự thật.
- Analysis job duplicate được xử lý idempotently theo nguyên tắc ADR-0006.

### 8. Model version và reproducibility

Mỗi kết quả bắt buộc lưu:

- model name/version;
- contract version;
- content hash;
- label, confidence và polarity score;
- analyzed timestamp;
- preprocessing version nếu việc preprocessing ảnh hưởng đáng kể kết quả.

Nếu Sentiment được dùng làm Strategy input, Experiment phải tham chiếu đúng Sentiment Result/model version theo [ADR-0009: Reproducible Experiments](0009-reproducible-experiments.md).

Thay model mới không sửa Java Strategy Engine. Java chỉ phụ thuộc versioned Sentiment Result contract.

### 9. Health và readiness

Python Service cung cấp:

```text
GET /health/live
GET /health/ready
```

- `live`: process FastAPI còn hoạt động;
- `ready`: model đã tải và có thể inference;
- service không nhận analysis request trước khi ready;
- Docker Compose dùng readiness để kiểm tra trạng thái nhưng Java vẫn phải xử lý runtime failure.

### 10. Security và dữ liệu đầu vào

- Sentiment endpoint chỉ được expose trong internal Docker/network hoặc private environment.
- Java và Python có thể dùng internal service token cấu hình qua environment.
- Không đặt secret trong source code hoặc Frontend.
- Service chỉ nhận text đã được Java thu thập; Python không tự fetch URL từ request để tránh SSRF.
- HTML/script được loại bỏ hoặc chuẩn hóa trước inference.
- Giới hạn độ dài title/content, batch size và request body.
- Không log toàn bộ nội dung bài báo hoặc secret khi không cần thiết.
- Chỉ lưu/truyền phần nội dung phù hợp với nguồn tin và phạm vi đồ án.

### 11. Observability

Theo dõi tối thiểu:

- request count và error rate;
- latency trung bình/p95;
- timeout/retry/circuit-breaker state;
- queue depth và số News đang PENDING/FAILED;
- số kết quả theo POSITIVE/NEUTRAL/NEGATIVE;
- model version đang phục vụ;
- model load time và readiness;
- request/correlation ID trong Java và Python log.

Không dùng tỷ lệ nhãn làm bằng chứng model chính xác. Chất lượng model cần fixture/evaluation dataset riêng.

### 12. Sentiment Strategy

Sentiment Strategy là phần mở rộng, không bắt buộc MVP. Nếu triển khai:

- Strategy chỉ nhận `sentimentData` đã chuẩn hóa trong `StrategyContext`;
- Strategy không gọi Python Service trực tiếp;
- Strategy descriptor/version được đăng ký theo [ADR-0005: Strategy Contract và Plugin Registry](0005-strategy-plugin-registry.md);
- Backtest dataset chỉ dùng News có `publishedAt <= evaluationTime`; `analyzedAt` được lưu để audit nhưng không quyết định tính hợp lệ lịch sử;
- nếu thiếu sentiment, behavior phải được feature spec quy định rõ.

## Alternatives Considered

- **Chạy NLP trực tiếp trong Java Backend**: Một runtime dễ deploy hơn nhưng hạn chế thư viện/model Python và làm tăng memory/failure impact lên Market API.
- **Để Python Service vừa crawl News vừa phân tích và ghi database**: Nhanh cho prototype nhưng trộn collection, inference và persistence; khó thay crawler/model độc lập.
- **Java và Python cùng truy cập chung bảng News**: Giảm số API call nhưng vi phạm data ownership, tạo race condition và schema coupling.
- **Frontend gọi Python Service trực tiếp**: Bỏ qua Java Backend, lộ internal service và làm validation/error handling phân tán.
- **Hosted Sentiment API bên thứ ba**: Triển khai nhanh nhưng phụ thuộc chi phí, quota, privacy và model version của provider.
- **Python consumer đọc trực tiếp Redis và ghi PostgreSQL**: Throughput tốt hơn nhưng làm Python phụ thuộc queue/schema; MVP ưu tiên HTTP boundary rõ và Java orchestration.
- **Phân tích đồng bộ khi người dùng mở trang News**: Dễ hiểu nhưng làm UI chậm và khiến request phụ thuộc thời gian inference.

## Consequences

### Positive

- Lỗi hoặc restart model không làm Market Dashboard ngừng hoạt động.
- Có thể thay model Python mà Java News/Strategy chỉ cần contract tương thích.
- Python tận dụng hệ sinh thái NLP/ML phù hợp.
- Sentiment Service có thể scale và deploy độc lập.
- News Collection, persistence và inference có trách nhiệm rõ.
- Model version và content hash hỗ trợ audit/reproducibility.
- News vẫn hiển thị được khi Sentiment đang chờ hoặc lỗi.

### Negative

- Có thêm Python service, HTTP contract và Docker container.
- Cần timeout, retry, circuit breaker và monitoring giữa hai runtime.
- Sentiment Result có eventual consistency sau News Item.
- Model load có thể làm startup chậm và dùng nhiều memory.
- Cần giữ contract Java/Python tương thích bằng test.
- Hai ngôn ngữ làm tăng yêu cầu setup cho thành viên.

## Affected Components

- `modules/news`
- `modules/contracts`
- `modules/persistence`
- `apps/api`
- `apps/worker`
- `apps/sentiment`
- `apps/web`
- `infra/compose`
- News/Sentiment database model
- News Sentiment UI và demo fallback

## Validation

- Contract test dùng cùng JSON fixture ở Java client và FastAPI test.
- Phân tích cùng text/model version và xác nhận response đúng enum/range/schema.
- Gửi request trùng `newsId + contentHash + modelVersion` và xác nhận không lưu kết quả trùng.
- Tắt Python Service và xác nhận Market Dashboard, realtime chart và technical Strategy vẫn hoạt động.
- Khi service tắt, News vẫn hiển thị với trạng thái pending/unavailable.
- Bật lại service và xác nhận pending analysis được retry thành công.
- Mô phỏng timeout/5xx và xác nhận retry/circuit breaker hoạt động có giới hạn.
- Gửi payload quá dài hoặc sai schema và xác nhận bị từ chối an toàn.
- Deploy hai Python instance và xác nhận Java client có thể phân phối request mà không thay contract.
- Thay model version và xác nhận tạo result version mới, không overwrite kết quả cũ.
- Kiểm tra `/health/live` và `/health/ready` phản ánh đúng trạng thái process/model.
- Xác nhận Python không có credential truy cập shared PostgreSQL trong MVP.

## Risks and Mitigations

- **Risk**: Python Service lỗi kéo theo News request chậm hoặc hết thread.

  **Mitigation**: Phân tích bất đồng bộ, timeout ngắn có cấu hình, concurrency limit và circuit breaker.

- **Risk**: Java/Python contract lệch nhau.

  **Mitigation**: Versioned OpenAPI/JSON Schema, shared fixtures và contract test trong CI.

- **Risk**: Model version mới làm kết quả Experiment cũ không tái lập được.

  **Mitigation**: Lưu model/preprocessing version, giữ result cũ và tham chiếu version trong Experiment.

- **Risk**: Crawler đưa HTML/script hoặc nội dung quá dài vào model.

  **Mitigation**: Normalize/sanitize, giới hạn payload và không cho Python tự fetch URL.

- **Risk**: News trùng bị phân tích nhiều lần.

  **Mitigation**: `contentHash`, idempotency key và result uniqueness theo model version.

- **Risk**: Model cho nhãn thiếu chính xác nhưng UI thể hiện quá chắc chắn.

  **Mitigation**: Hiển thị confidence, ghi rõ đây là phân tích cơ bản và đánh giá bằng fixture dataset.

- **Risk**: Sentiment Strategy tạo look-ahead bias trong Backtest.

  **Mitigation**: Chỉ dùng News có `publishedAt` không sau `evaluationTime`, freeze Sentiment Result/model version và lưu `analyzedAt` để audit.

- **Risk**: Python dependency/model làm Docker image quá lớn.

  **Mitigation**: Pin dependency, dùng model phù hợp MVP, cache image layer và đo startup/memory.

## References

- [Đề bài Crypto StrategyLab](../Crypto%20Strategy%20Lab%20%E2%80%93%20%C4%90%E1%BB%93%20%C3%A1n%20cu%E1%BB%91i%20k%E1%BB%B3.pdf)
- [Architecture Overview](../architecture/architecture-overview.md)
- [Data Flows](../architecture/data-flows.md)
- [Data Model Overview](../architecture/data-model-overview.md)
- [Deployment View](../architecture/deployment-view.md)
- [UI Stitch Guide](../ui/stitch-guide.md)
- [ADR-0001: Modular Monolith](0001-modular-monolith.md)
- [ADR-0002: Module Boundaries](0002-module-boundaries.md)
- [ADR-0005: Strategy Plugin Registry](0005-strategy-plugin-registry.md)
- [ADR-0006: Queue và Worker](0006-queue-worker-backtesting.md)
- [ADR-0007: PostgreSQL/Supabase và Redis](0007-postgresql-redis-ownership.md)
- [ADR-0009: Reproducible Experiments](0009-reproducible-experiments.md)

## Supersession

- Supersedes: None
- Superseded by: None

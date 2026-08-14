# Hoàn thiện tài liệu kiến trúc Crypto Strategy Lab

## Tóm tắt

Hoàn thiện toàn bộ `docs/architecture` dựa trên đề bài, slide kiến trúc và 10 ADR hiện có. Tài liệu mô tả **target architecture của MVP**, không tuyên bố hệ thống đã triển khai hoặc đạt các chỉ số chưa được đo.

Kiến trúc thống nhất:

- Modular Monolith cho Java Backend, Java 21 và Spring Boot 3.
- `apps/web`, `apps/api`, `apps/worker`, `apps/sentiment`.
- PostgreSQL/Supabase là nguồn dữ liệu bền vững.
- Redis Streams dùng cho queue, cache và trạng thái tạm.
- Python/FastAPI là boundary riêng cho Sentiment.
- PostgreSQL CRUD/state model + Leaderboard projection/read model + Transactional Outbox.
- Không dùng CQRS đầy đủ hoặc Event Sourcing trong MVP.

## Thay đổi tài liệu

### 1. Tổng quan và các view tĩnh

Hoàn thiện các tài liệu hiện có bằng Mermaid:

- `architecture-overview.md`: mục tiêu, phạm vi MVP, 8 architectural drivers, style kiến trúc, nguyên tắc, constraints, trade-off và bảng liên kết ADR-0001 đến ADR-0010.
- `system-context.md`: C4 Level 1 gồm User/Trader, Crypto Strategy Lab, Binance và News Providers; xác định system boundary, trust boundary và nội dung ngoài phạm vi như giao dịch tiền thật.
- `container-view.md`: C4 Level 2 gồm Web, API, Worker, Sentiment, PostgreSQL/Supabase và Redis; ghi rõ protocol, sync/async boundary, data ownership và failure isolation.
- `module-view.md`: C4 Level 3 thống nhất chính xác với ADR-0002, bao gồm `domain`, `contracts`, `market-data`, `strategy-core`, `strategies`, `combination`, `backtesting`, `evaluation`, `experiment`, `search`, `leaderboard`, `news`, `persistence`; mô tả public API/ports, allowed/forbidden dependency và ArchUnit enforcement.
- Phân biệt bốn bounded context: Market Data, Strategy, Experiment và News Intelligence.
- Dùng thống nhất tên dự án `Crypto Strategy Lab`, owner `Tiến Luật`, trạng thái `Draft — Target MVP Architecture` và ngày cập nhật thực tế.

### 2. Dynamic view và dữ liệu

Hoàn thiện `data-flows.md` với năm luồng:

1. Historical Market Data: query chuẩn → Binance Adapter → pagination/rate limit → canonical Candle → deduplicate/cache/persist.
2. Realtime Market Data: multiplex subscription → Candle update → WebSocket → ordering/deduplication → reconnect và historical gap recovery.
3. Strategy/Backtest: Dataset → Strategy Registry → Strategy/Composite → signal → Backtester → Trades → Evaluator.
4. Search/Leaderboard: REST tạo Experiment → PostgreSQL transaction/Outbox → Redis Stream → Worker → Evaluation → Ranking/Top-K → WebSocket progress; gồm stop, retry, dead letter, backpressure và idempotency.
5. News/Sentiment: Provider → normalize/deduplicate → PostgreSQL → analysis job → FastAPI → validate/store result; khi Sentiment lỗi, Market Dashboard vẫn hoạt động.

Hoàn thiện `data-model-overview.md`:

- Mô hình các entity chính: Candle, Dataset Version, Strategy/Composite Definition, immutable Experiment Manifest, Candidate Definition, Execution Attempt, Job, Backtest Result, Trade, Evaluation Result, Leaderboard Entry/Revision, News Item, Sentiment Result, Outbox Event và Processed Message.
- Gắn owner module, identity/version, lifecycle và immutable relationship.
- Thể hiện state machine của Experiment, Job và News analysis.
- Giải thích rõ PostgreSQL là source of truth; Redis chỉ là queue/cache.
- Ghi quyết định không dùng full CQRS/Event Sourcing: chỉ dùng task-oriented write model, Leaderboard projection/read model và immutable audit records; Outbox không phải Event Sourcing.

### 3. Deployment, quality và bằng chứng

Hoàn thiện `deployment-view.md`:

- Local: Docker Compose với Web, API, Worker, Sentiment, PostgreSQL và Redis.
- Demo/shared: Web/API/Worker/Sentiment, Supabase-hosted PostgreSQL và Redis được cấu hình theo môi trường.
- CI: build, unit/contract/architecture test; không mô tả hạ tầng chưa tồn tại như đã triển khai.
- Ghi startup dependencies, health/readiness, secrets, connection pool, backup/recovery và khả năng scale ngang Worker.
- Port, image, ARM64 và hosting chưa kiểm chứng được ghi là `Planned verification`, không đặt giá trị giả.

Hoàn thiện `quality-attributes.md` theo S–S–E–A–R–M và dùng các mục tiêu kiểm chứng ban đầu:

- Thêm MACD chỉ thay `strategies`, registration/schema/test; không sửa Backtester, Evaluator, Leaderboard hoặc UI.
- Thêm Generator mới chỉ thay implementation/registration trong Search; downstream pipeline không đổi.
- Thêm Market Provider mới chỉ cần adapter/configuration; public market contract và Frontend không đổi.
- Binance reconnect trong tối đa 30 giây ở demo test, backfill đủ closed Candle và không tạo duplicate.
- Tăng từ 1 lên 3 Worker đạt ít nhất 2× throughput trên cùng benchmark và không tạo Result trùng.
- Sentiment Service down không làm gián đoạn realtime chart; trạng thái degraded xuất hiện trong tối đa 5 giây.
- Reproduce tạo cùng Trade sequence, bốn metrics bắt buộc và fingerprint.
- Progress, queue, failure và best score được quan sát trong tối đa 5 giây, truy vết bằng correlation ID.
- Realtime update có p95 không quá 1 giây từ lúc Backend nhận event đến lúc UI áp dụng với bốn chart trong tải demo.
- Start Search 1.000 candidates trả `experimentId/jobId` trong tối đa 2 giây và dùng bounded queue.

Tạo `architecture-evidence.md`:

- Ma trận Requirement/Driver → PDF section → Architecture View → ADR → QA scenario → Test/Demo → Expected measure → Evidence status.
- Bao phủ extension, replaceability, scalability, realtime recovery, failure isolation, retry/duplicate/order, provenance và observability.
- Mọi evidence ban đầu ghi `Planned — chưa có implementation`; chỉ cập nhật thành `Verified` khi có log, benchmark hoặc test thật.
- Có ATAM-lite/trade-off matrix cho Plugin, Async Queue, Event-driven flow, Modular Monolith, Leaderboard projection, Redis và Sentiment Service.

Cập nhật `docs/architecture/README.md` thành reading order và index đầy đủ. Chỉ chỉnh liên kết nhỏ ở ADR index, root README hoặc demo checklist nếu cần để trỏ tới architecture view/evidence mới; không mở rộng API schema hay feature design.

## Kiểm tra hoàn thành

- Không còn `[Điền]`, `YYYY-MM-DD`, owner giả hoặc placeholder trong `docs/architecture`.
- Tất cả Mermaid diagram, Markdown fence và bảng đều cân bằng.
- Mọi local link trỏ tới file tồn tại.
- Tên container/module/event nhất quán với ADR-0001 đến ADR-0010.
- Context không chứa chi tiết container; Container không trộn module/class; Module View không mô tả deployment.
- Data flow thể hiện rõ sync/async boundary, failure point, retry, ordering và source of truth.
- Mỗi architectural driver có scenario đo được và architecture proof tương ứng.
- `git diff --check` không báo lỗi whitespace.
- Review `git diff --stat` và `git status` để bảo đảm chỉ thay đổi tài liệu trong phạm vi đã chốt.

## Giả định

- Repo hiện chưa có source code; mọi kiến trúc và ngưỡng hiệu năng đều là target MVP/planned verification.
- Không chuyển ADR từ `Proposed` sang `Accepted`.
- Không tạo benchmark, log hoặc bằng chứng giả.
- Mermaid được dùng trực tiếp trong Markdown; không tạo Draw.io hoặc ảnh export.
- Không thiết kế thêm public REST/WebSocket endpoint ngoài contract đã có.
- Branch hiện tại là `docs/adr-api-demo`; file `docs/KienTrucDoAn_slide.pdf` đang untracked và được giữ nguyên, không tự stage/commit.

# Architecture Overview

**Status**: Draft — Target MVP Architecture

**Last Updated**: 2026-08-14

**Owner**: Văn Minh

## Purpose

Crypto Strategy Lab là nền tảng thử nghiệm chiến lược giao dịch crypto: nhận dữ liệu Binance, hiển thị tối đa bốn chart realtime, kết hợp Strategy, Backtest trên dữ liệu lịch sử, đánh giá metrics, tìm kiếm candidate, duy trì Top-K và phân tích sentiment tin tức.

Trọng tâm là chứng minh hệ thống có thể thay đổi và phục hồi có kiểm soát; kết quả không phải lời khuyên giao dịch hoặc cam kết lợi nhuận.

## MVP Scope

- Historical và realtime Candle từ Binance qua provider adapter.
- Tối đa bốn chart có pair/timeframe độc lập.
- MA, RSI, Bollinger Bands và Support/Resistance dưới dạng Strategy plugin.
- Composite Strategy với Majority Vote; Weighted Combination là extension được phép.
- Backtest, Trade visualization và bốn metrics bắt buộc: Return, Win Rate, Maximum Drawdown, Number of Trades.
- Random Search, stop condition, queue/worker và Top-K Leaderboard.
- News pipeline: collect, normalize, persist, analyze sentiment.
- Experiment manifest/version để truy vết và tái lập.

Ngoài MVP: giao dịch tiền thật, upload plugin không tin cậy, Kubernetes, Kafka, microservice theo từng module, full CQRS và Event Sourcing.

## Architectural Drivers

| Driver | Câu hỏi kiến trúc |
| --- | --- |
| Modifiability | Thêm MACD phải sửa bao nhiêu module? |
| Replaceability/Maintainability | Random Search đổi sang generator khác có làm Backtester đổi không? |
| Scalability | Từ hàng trăm lên nhiều nghìn Backtest có tăng Worker mà giữ core contract không? |
| Performance | Search dài có làm HTTP API hoặc Market Dashboard bị giữ/chậm không? |
| Realtime | Candle mới đến bốn chart với độ trễ và tải UI được kiểm soát thế nào? |
| Reliability | Binance disconnect, Worker crash hoặc Redis lỗi được phục hồi ra sao? |
| Observability | Có truy được queue depth, job latency, retry, failure và best score không? |
| Reproducibility | Top-K result có truy về exact Strategy, Dataset, assumptions và software version không? |

Các driver được chuyển thành scenario đo được trong [Quality Attribute Scenarios](quality-attributes.md).

## Architecture Style

Backend cốt lõi là **Modular Monolith** theo [ADR-0001](../adr/0001-modular-monolith.md): business capability nằm trong module Java độc lập, được composition tại `apps/api` và tái sử dụng bởi `apps/worker`. Giao tiếp nội bộ đồng bộ được ưu tiên cho luồng đơn giản; queue/event được dùng tại boundary cần chạy nền, retry hoặc scale.

Hai runtime được tách vì driver cụ thể:

- `apps/worker`: tách tải CPU dài của Search/Backtest khỏi API và scale ngang.
- `apps/sentiment`: Python/FastAPI có dependency, startup và failure mode khác Java.

## Main Components

| Component | Trách nhiệm | Target technology | Logical owner |
| --- | --- | --- | --- |
| `apps/web` | Dashboard, bốn chart, progress, Leaderboard, Trade/News view | React/Next.js | Presentation |
| `apps/api` | REST, WebSocket, validation và application orchestration | Java 21, Spring Boot 3 | API/Tech Lead |
| `apps/worker` | Search/Backtest/Evaluation và background job orchestration | Java 21, Spring Boot 3 | Experiment |
| `apps/sentiment` | Phân tích text và trả Sentiment Result chuẩn | Python, FastAPI | News Intelligence |
| PostgreSQL/Supabase | Business source of truth và Outbox | PostgreSQL | Data owners |
| Redis | Streams, cache, progress/realtime state tạm thời | Redis Streams | Platform |

## Architecture Principles

1. Business policy không phụ thuộc Spring, database, Binance hoặc UI.
2. Module chỉ gọi public API/port/event của module khác; không import `internal` hoặc repository implementation.
3. Frontend chỉ phụ thuộc REST/WebSocket contract của hệ thống.
4. Provider response phải được mapping sang canonical model trước khi ra khỏi adapter.
5. PostgreSQL là source of truth; Redis có thể mất và được rebuild/recover.
6. Tác vụ dài dùng job ID và chạy bất đồng bộ; WebSocket chỉ phát update cần cho UI.
7. At-least-once delivery luôn đi kèm idempotency; không giả định exactly-once.
8. Experiment input/result/version đã chốt là immutable.
9. Failure của News/Sentiment không được lan sang Market/technical Strategy flow.
10. Công nghệ chỉ được thêm khi có driver, trade-off và verification plan.

## Main Flows

- [Historical và Realtime Market Data](data-flows.md#1-historical-market-data)
- [Strategy, Backtest và Evaluation](data-flows.md#3-strategy-backtest-và-evaluation)
- [Search, Queue và Leaderboard](data-flows.md#4-search-queue-và-leaderboard)
- [News và Sentiment](data-flows.md#5-news-và-sentiment)

## Key Decisions

| ADR | Quyết định | Status |
| --- | --- | --- |
| [0001](../adr/0001-modular-monolith.md) | Modular Monolith cho backend cốt lõi | Proposed |
| [0002](../adr/0002-module-boundaries.md) | Module boundary, public contract và dependency direction | Proposed |
| [0003](../adr/0003-market-data-adapter.md) | Market Data Port và provider adapter | Proposed |
| [0004](../adr/0004-websocket-realtime.md) | Native WebSocket, multiplex subscription và recovery | Proposed |
| [0005](../adr/0005-strategy-plugin-registry.md) | Strategy contract, plugin và registry | Proposed |
| [0006](../adr/0006-queue-worker-backtesting.md) | Redis Streams, Worker, idempotency và Outbox | Proposed |
| [0007](../adr/0007-postgresql-redis-ownership.md) | PostgreSQL source of truth, Redis queue/cache | Proposed |
| [0008](../adr/0008-sentiment-service-boundary.md) | Python Sentiment Service boundary | Proposed |
| [0009](../adr/0009-reproducible-experiments.md) | Immutable manifest, versioning và fingerprint | Proposed |
| [0010](../adr/0010-strategy-generator-contract.md) | Replaceable Strategy Generator contract | Proposed |

## Deliberate Trade-offs

| Chọn | Lợi ích | Giá phải trả |
| --- | --- | --- |
| Modular Monolith | Dễ phát triển/deploy cho nhóm nhỏ | Boundary cần build test và review để không suy thoái |
| Plugin/Registry | Thêm Strategy cục bộ | Phải quản lý descriptor, schema và version |
| Async Queue/Worker | Scale, retry, stop và fault isolation | Eventual consistency, duplicate và tracing phức tạp hơn |
| Redis + PostgreSQL | Queue/cache nhanh và state bền vững | Outbox, recovery và hai hạ tầng cần vận hành |
| Sentiment service riêng | Cô lập ML runtime/failure và scale riêng | HTTP contract, timeout và thêm container |
| Leaderboard projection | Đọc Top-K nhanh | Phải rebuild/synchronize projection |
| Không full CQRS/Event Sourcing | MVP đơn giản, audit bằng immutable records | Không có generic event replay hoặc temporal state đầy đủ |

## Constraints

- Nhóm bốn người và thời gian đồ án ngắn.
- Binance và News Provider là external dependency không kiểm soát được uptime/schema.
- Repo hiện ở giai đoạn tài liệu, chưa có source code để xác nhận benchmark.
- Các target performance/recovery là mục tiêu demo, không phải production SLA.

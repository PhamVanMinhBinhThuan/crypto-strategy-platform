# Architecture Overview

**Status**: Proposed baseline
**Last Updated**: 2026-08-12
**Owners**: Tiến Luật và Module Owners

## Purpose

Tài liệu tóm tắt kiến trúc mục tiêu của Crypto Strategy Lab và là điểm bắt đầu để đọc các C4 views, runtime flows, quality scenarios và ADR.

## System Summary

Crypto Strategy Lab nhận historical/realtime candle từ Binance, hiển thị tối đa bốn chart, chạy Strategy/Composite Strategy trên dataset bất biến, backtest và đánh giá candidate, duy trì Top-K Leaderboard, đồng thời thu thập News và phân tích Sentiment. MVP dùng Modular Monolith cho Java backend, một Worker runtime dùng chung business modules và một Python Sentiment Service được cô lập.

## Architectural Goals

- Thêm Strategy, Search Generator hoặc Market Provider mà không sửa downstream consumers.
- Scale Backtest bằng queue và worker pool, không chiếm API request thread.
- Cập nhật bốn chart realtime với subscription độc lập và gap recovery.
- Cô lập lỗi Binance, Worker và Sentiment theo boundary rõ ràng.
- Tái lập Experiment từ immutable manifest, version và checksum.
- Quan sát được progress, queue, failure, latency và provenance.

Mục tiêu đo được nằm trong [Quality Attribute Scenarios](quality-attributes.md).

## Architecture Style

- **Modular Monolith** cho Java Backend theo [ADR-0001](../adr/0001-modular-monolith.md).
- **Ports and Adapters/Clean dependency direction** tại Market, Strategy và Persistence boundaries.
- **Event-driven asynchronous pipeline** cho Search/Backtest qua Redis Streams.
- **CQRS-style read model** cho Leaderboard; không dùng Event Sourcing trong MVP.
- **Independent ML service boundary** cho Sentiment vì runtime và failure mode khác Java core.

## Main Components

| Component | Trách nhiệm | Công nghệ mục tiêu | Owner |
| --- | --- | --- | --- |
| Web Dashboard | Bốn chart, strategy configuration, progress, leaderboard, news | React/TypeScript | UI Owner |
| Application API | REST, WebSocket, orchestration và composition root | Java 21/Spring Boot 3 | Backend Owner |
| Backtest Worker | Search coordination, Backtest/Evaluation jobs | Java 21/Spring Boot 3 | Experiment Owner |
| Sentiment Service | Stateless NLP inference | Python/FastAPI | ML Owner |
| PostgreSQL/Supabase | Durable source of truth | PostgreSQL | Data Owner |
| Redis | Streams, cache và ephemeral coordination | Redis | Infra Owner |

## Architecture Principles

1. Domain policy phụ thuộc abstraction, không phụ thuộc framework/provider/database.
2. Frontend chỉ dùng internal REST/WebSocket contracts, không dùng Binance/Supabase schema trực tiếp.
3. Strategy deterministic, immutable và không gọi network/database.
4. Module sở hữu dữ liệu; shared database không cho phép truy cập chéo repository tùy ý.
5. Queue dùng at-least-once delivery với idempotency và Transactional Outbox.
6. Result, dataset và experiment input được version hóa và không overwrite.
7. Công nghệ chỉ được giữ khi giải quyết architectural driver và có validation plan.

## Main Flows

- [Historical và Realtime Market Data](data-flows.md#1-historical-market-data)
- [Strategy, Backtest và Evaluation](data-flows.md#3-strategy-backtest-và-evaluation)
- [Search, Queue và Leaderboard](data-flows.md#4-search-queue-và-leaderboard)
- [News và Sentiment](data-flows.md#5-news-và-sentiment)

## Key Decisions

Danh sách đầy đủ và trạng thái nằm trong [ADR Index](../adr/README.md). Mọi decision hiện là `Proposed` cho đến khi có evidence thực nghiệm.

## Constraints

- Nhóm bốn thành viên và thời gian đồ án hữu hạn.
- MVP bắt buộc Binance, bốn chart, bốn Strategy, Random Search, Backtest, Top-K và News/Sentiment pipeline.
- Không dùng Kafka, Kubernetes hoặc microservice-per-module khi chưa có nhu cầu đo được.
- External providers có rate limit, network failure và contract thay đổi ngoài kiểm soát.
- Ngưỡng trong quality scenarios là mục tiêu demo, không phải SLO production.

## Deferred Decisions

- Exact public REST endpoints được chốt trong từng feature specification.
- Cấu hình timeout, retry, buffer, cache TTL và connection pool được đo rồi chốt theo environment.
- Authentication/authorization ngoài dữ liệu public chưa thuộc MVP architecture baseline.

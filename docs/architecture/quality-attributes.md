# Quality Attribute Scenarios

**Status**: Baseline proposed
**Last Updated**: 2026-08-12
**Owners**: Tech Lead và Module Owners

Các scenario dùng mẫu **S-S-E-A-R-M**: Source, Stimulus, Environment, Artifact, Response và Measure. Các ngưỡng dưới đây là mục tiêu kiểm chứng cho môi trường demo, chưa phải cam kết production.

## QA-01 — Add New Strategy

| Field | Nội dung |
| --- | --- |
| Attribute | Modifiability |
| Source | Thành viên phát triển strategy |
| Stimulus | Bổ sung `MACDStrategy` với metadata và parameter schema mới |
| Environment | Hệ thống đang có MA, RSI, Bollinger Bands và Support/Resistance |
| Artifact | Strategy Plugin/Registry và composition root |
| Response | Plugin mới được đăng ký, hiển thị và chạy qua pipeline hiện hữu |
| Measure | Chỉ module `strategies` và composition registration thay đổi; không sửa Backtester, Evaluator, Leaderboard hoặc UI; contract và architecture tests pass |

## QA-02 — Replace Search Algorithm

| Field | Nội dung |
| --- | --- |
| Attribute | Modifiability, Maintainability |
| Source | Tech Lead |
| Stimulus | Thêm `DomainGuidedStrategyGenerator` bên cạnh Random Search |
| Environment | Search/Backtest pipeline đang hoạt động |
| Artifact | Strategy Generator contract và Search Registry |
| Response | Generator mới sinh `CandidateStrategy` theo cùng contract |
| Measure | Chỉ module `search` và composition registration thay đổi; Backtester, Evaluator và Leaderboard không đổi |

## QA-03 — Replace Market Data Provider

| Field | Nội dung |
| --- | --- |
| Attribute | Modifiability, Replaceability |
| Source | Tech Lead |
| Stimulus | Thêm fixture hoặc OKX Market Data Adapter |
| Environment | Dashboard và Backtester đang dùng canonical Candle |
| Artifact | Market Data Port và adapter layer |
| Response | Provider mới cung cấp historical/realtime data qua contract hiện hữu |
| Measure | Chỉ adapter/configuration và provider contract tests thay đổi; Frontend, Strategy và public market contract không đổi |

## QA-04 — Binance Disconnect

| Field | Nội dung |
| --- | --- |
| Attribute | Reliability |
| Source | Binance/network |
| Stimulus | WebSocket upstream bị ngắt trong khi chart đang chạy |
| Environment | Bốn chart có subscription hoạt động trong demo |
| Artifact | Binance Adapter, gap recovery và WebSocket gateway |
| Response | Báo trạng thái degraded, reconnect, backfill và deduplicate candles |
| Measure | Reconnect trong tối đa 30 giây ở bài test demo; không thiếu hoặc trùng closed Candle sau reconciliation |

## QA-05 — Scale Backtest Workers

| Field | Nội dung |
| --- | --- |
| Attribute | Scalability, Performance |
| Source | Operator |
| Stimulus | Tăng worker từ 1 lên 3 instance |
| Environment | Cùng benchmark dataset, candidate set và cấu hình tài nguyên |
| Artifact | Redis Streams, Worker group, PostgreSQL và Ranking Handler |
| Response | Job được chia cho consumer group và kết quả ghi idempotently |
| Measure | Throughput tối thiểu gấp 2 lần baseline 1 worker; không có Result/Leaderboard entry trùng; core contract không đổi |

## QA-06 — News/Sentiment Failure

| Field | Nội dung |
| --- | --- |
| Attribute | Availability, Fault Isolation |
| Source | Sentiment Service hoặc News Provider |
| Stimulus | Service ngừng phản hồi |
| Environment | Market Dashboard và technical strategies đang hoạt động |
| Artifact | News orchestration, circuit breaker và container boundary |
| Response | News/Sentiment chuyển degraded; Market, Strategy và Backtest tiếp tục |
| Measure | Realtime chart không gián đoạn; trạng thái degraded hiển thị trong tối đa 5 giây; không có lỗi Market do dependency Sentiment |

## QA-07 — Reproduce Experiment

| Field | Nội dung |
| --- | --- |
| Attribute | Reproducibility |
| Source | Người dùng hoặc giảng viên |
| Stimulus | Chạy lại một Experiment từ immutable manifest |
| Environment | Có dataset, strategy và result version mới trong hệ thống |
| Artifact | Experiment Manifest, versioned artifacts và Backtester |
| Response | Replay dùng đúng dataset, versions, parameters, seed và assumptions cũ |
| Measure | Trade sequence, Return, Win Rate, Maximum Drawdown, Number of Trades và fingerprint khớp kết quả mong đợi |

## QA-08 — Observe Running Experiment

| Field | Nội dung |
| --- | --- |
| Attribute | Observability |
| Source | Người dùng hoặc operator |
| Stimulus | Theo dõi Search đang chạy hoặc điều tra job lỗi |
| Environment | Nhiều candidate và worker đang hoạt động |
| Artifact | Progress store, logs, metrics và WebSocket events |
| Response | Hiển thị status, counts, queue lag, duration, failure và best score; log truy vết xuyên pipeline |
| Measure | Trạng thái mới xuất hiện trong tối đa 5 giây; log có correlationId, experimentId, candidateId và jobId |

## QA-09 — Realtime Four-Chart Update

| Field | Nội dung |
| --- | --- |
| Attribute | Realtime, Performance |
| Source | Binance Candle stream hoặc người dùng đổi timeframe |
| Stimulus | Candle update đến hoặc Chart 1 đổi từ 5m sang 1h |
| Environment | Bốn chart hoạt động trên một browser tab |
| Artifact | Market Adapter, WebSocket gateway và Dashboard |
| Response | Update được định tuyến đúng subscription; chỉ chart được đổi tải lại dữ liệu |
| Measure | p95 từ lúc Backend nhận event đến lúc UI áp dụng không quá 1 giây trong tải demo; Chart 2–4 không reload |

## QA-10 — Start Large Search

| Field | Nội dung |
| --- | --- |
| Attribute | Performance, Scalability |
| Source | Người dùng |
| Stimulus | Start Search với giới hạn 1.000 candidates |
| Environment | API và Worker chạy với bounded queue |
| Artifact | REST command, Outbox, Redis Streams và Worker pool |
| Response | API tạo Experiment/Job rồi xử lý bất đồng bộ, không enqueue vô hạn |
| Measure | API trả experimentId/jobId trong tối đa 2 giây; mọi job kết thúc ở trạng thái terminal; queue và throughput được ghi nhận |

## Verification Matrix

| Scenario | ADR chính | Architecture Proof | Trạng thái |
| --- | --- | --- | --- |
| QA-01 | ADR-0005 | AP-01 Strategy extension | Planned |
| QA-02 | ADR-0011 | AP-02 Search replacement | Planned |
| QA-03 | ADR-0003 | AP-03 Provider replacement | Planned |
| QA-04 | ADR-0003, ADR-0004 | AP-04 Realtime recovery | Planned |
| QA-05 | ADR-0006 | AP-05 Worker scale | Planned |
| QA-06 | ADR-0008 | AP-06 Failure isolation | Planned |
| QA-07 | ADR-0009 | AP-07 Reproduction | Planned |
| QA-08 | ADR-0006 | AP-08 Observability | Planned |
| QA-09 | ADR-0004 | AP-09 Realtime latency | Planned |
| QA-10 | ADR-0006 | AP-10 Async search | Planned |

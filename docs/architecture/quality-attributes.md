# Quality Attribute Scenarios

**Status**: Draft — Planned Verification

**Last Updated**: 2026-08-14

**Owner**: Văn Minh

Mỗi scenario dùng cấu trúc Source–Stimulus–Environment–Artifact–Response–Measure. Các measure là mục tiêu demo/kiểm chứng ban đầu, không phải production SLA hoặc kết quả đã đạt.

## Scenario Summary

| ID | Attribute | Architectural concern |
| --- | --- | --- |
| QA-01 | Modifiability | Thêm Strategy mới |
| QA-02 | Replaceability | Thay Search Generator |
| QA-03 | Modifiability | Thay Market Data Provider |
| QA-04 | Reliability | Binance disconnect và gap recovery |
| QA-05 | Scalability/Performance | Tăng số Backtest Worker |
| QA-06 | Availability/Fault isolation | Sentiment Service down |
| QA-07 | Reproducibility | Chạy lại Experiment |
| QA-08 | Observability | Quan sát Search đang chạy |
| QA-09 | Realtime/Performance | Cập nhật đồng thời bốn chart |
| QA-10 | Responsiveness/Scalability | Start Search 1.000 candidates |

## QA-01 — Add New Strategy

| Field | Scenario |
| --- | --- |
| Source | Developer/giảng viên |
| Stimulus | Yêu cầu thêm `MACDStrategy` với parameter schema mới |
| Environment | Codebase MVP có bốn Strategy và Plugin Registry |
| Artifact | `strategies`, `strategy-core`, composition registration |
| Response | Implement Strategy/Plugin, schema và test; Registry/API tự liệt kê Strategy mới |
| Measure | Chỉ thay module Strategy và registration/test; không sửa Backtester, Evaluator, Leaderboard hoặc UI business logic |

## QA-02 — Replace Search Generator

| Field | Scenario |
| --- | --- |
| Source | Developer/giảng viên |
| Stimulus | Thêm Fixture/Domain-guided Generator bên cạnh Random Search |
| Environment | Search Coordinator dùng `StrategyGenerator` Registry |
| Artifact | `search` public API và generator implementations |
| Response | Đăng ký generator mới theo ID/version, trả Candidate Definition chuẩn |
| Measure | Chỉ thay Search implementation/registration/test; queue job contract, Backtester, Evaluator và Leaderboard không đổi |

## QA-03 — Replace Market Data Provider

| Field | Scenario |
| --- | --- |
| Source | Developer/Operator |
| Stimulus | Chọn Fixture/OKX adapter thay Binance cho cùng query/subscription |
| Environment | Provider được chọn bằng configuration |
| Artifact | Market Data Port và provider adapter |
| Response | Adapter normalize dữ liệu/lỗi sang canonical contract |
| Measure | Chỉ thêm adapter/config/contract test; Frontend và public market contract không đổi |

## QA-04 — Binance Disconnect

| Field | Scenario |
| --- | --- |
| Source | Binance/network fault |
| Stimulus | Upstream WebSocket mất kết nối khi chart đang subscribe |
| Environment | Demo realtime với closed Candle đã xác nhận |
| Artifact | Binance Adapter, recovery logic và WebSocket status |
| Response | Báo `RECONNECTING`, reconnect bằng bounded exponential backoff, backfill từ last confirmed Candle, sort/deduplicate và báo `CONNECTED` |
| Measure | Phục hồi trong tối đa 30 giây ở bài test demo; đủ closed Candle trong gap và không tạo Candle trùng |

## QA-05 — Scale Backtest Workers

| Field | Scenario |
| --- | --- |
| Source | Operator |
| Stimulus | Tăng Worker instance từ 1 lên 3 |
| Environment | Cùng benchmark, Dataset, Candidate set, concurrency/job timeout và database plan |
| Artifact | Redis consumer group, Worker pool, PostgreSQL persistence |
| Response | Job được chia giữa Worker; pending job được reclaim; idempotency ngăn effect trùng |
| Measure | Throughput đạt ít nhất 2× baseline một Worker, không tạo Result/Leaderboard entry trùng; ghi queue lag và DB pool usage |

## QA-06 — Sentiment Service Failure

| Field | Scenario |
| --- | --- |
| Source | Sentiment process/model failure |
| Stimulus | Python Service timeout hoặc bị tắt |
| Environment | Market Dashboard, technical Strategy và News pipeline đang chạy |
| Artifact | Worker Sentiment client, circuit breaker và UI state |
| Response | Retry có giới hạn, mở circuit breaker, giữ News pending/failed-retryable và hiển thị degraded |
| Measure | Realtime chart không gián đoạn; trạng thái degraded xuất hiện trong tối đa 5 giây; technical Backtest tiếp tục |

## QA-07 — Reproduce Experiment

| Field | Scenario |
| --- | --- |
| Source | User/Reviewer |
| Stimulus | Yêu cầu reproduce một Top-K result |
| Environment | Manifest, Strategy/Dataset version và artifact được giữ nguyên |
| Artifact | Experiment Manifest, Dataset, Strategy Registry, Backtester/Evaluator |
| Response | Validate checksum/fingerprint và chạy Reproduction Run mới bằng exact input |
| Measure | Cùng Trade sequence, Return, Win Rate, Maximum Drawdown, Number of Trades và manifest fingerprint; không overwrite Result gốc |

## QA-08 — Observe Running Experiment

| Field | Scenario |
| --- | --- |
| Source | User/Operator |
| Stimulus | Search sinh, queue, retry, fail hoặc tạo best score mới |
| Environment | Experiment đang `RUNNING` |
| Artifact | PostgreSQL progress, Redis queue, log và WebSocket events |
| Response | Persist snapshot và phát progress/status/Leaderboard revision; log cùng correlation identifiers |
| Measure | Progress, queued/running/failed count và best score thấy được trong tối đa 5 giây; flow truy được bằng correlationId, experimentId, candidateId và jobId |

## QA-09 — Four-chart Realtime Latency

| Field | Scenario |
| --- | --- |
| Source | Binance realtime stream |
| Stimulus | Candle update cho các subscription đang hiển thị |
| Environment | Một browser có bốn chart trong tải demo đã định nghĩa |
| Artifact | Market Adapter, API WebSocket gateway và Web rendering |
| Response | Normalize, route theo logical subscription, coalesce intermediate open-Candle update và render theo frame/batch |
| Measure | p95 từ lúc Backend nhận event đến lúc UI áp dụng không quá 1 giây; không gửi lại toàn bộ historical dataset mỗi event |

## QA-10 — Start Search Responsiveness

| Field | Scenario |
| --- | --- |
| Source | User |
| Stimulus | Start Search với giới hạn 1.000 candidates |
| Environment | API, PostgreSQL và Queue healthy |
| Artifact | API, Experiment/Outbox và Search Coordinator |
| Response | Validate, persist Manifest/Job/Outbox, trả ID; Coordinator sinh bounded batches bất đồng bộ |
| Measure | HTTP trả `experimentId`, `jobId` và initial status trong tối đa 2 giây; request không chờ Search hoàn tất và queue không tăng vô hạn |

## Verification Matrix

| Scenario | Verification method | Evidence cần lưu |
| --- | --- | --- |
| QA-01 | Architecture test + change diff + contract test | Danh sách file/module đổi và test output |
| QA-02 | Fixture Generator change test | Diff và downstream regression result |
| QA-03 | Adapter contract suite | Contract result với Binance/Fixture |
| QA-04 | Disconnect/reconnect integration test | Timeline, missing/duplicate Candle assertion |
| QA-05 | Repeatable benchmark 1 vs 3 Worker | Throughput, queue lag, duplicate count, DB pool metric |
| QA-06 | Kill/timeout Sentiment resilience test | Chart heartbeat, degraded latency, circuit state |
| QA-07 | Reproduction comparison | Manifest/checksum/fingerprint, Trade/metric diff |
| QA-08 | Integration test + log trace | WebSocket timestamps và correlation trace |
| QA-09 | Four-chart latency test | Backend receive/UI apply timestamp và p95 report |
| QA-10 | API timing + bounded queue inspection | HTTP duration, returned IDs và max in-flight count |

Trạng thái và đường dẫn evidence được quản lý tại [Architecture Evidence](architecture-evidence.md).

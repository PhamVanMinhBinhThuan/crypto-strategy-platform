# Architecture Decision Records

ADR ghi lại quyết định kiến trúc quan trọng, driver, phương án thay thế, trade-off và cách thu thập evidence. Tất cả quyết định hiện ở trạng thái `Proposed` cho đến khi nhóm review và architecture proof tương ứng đạt measure.

## Decision Index

| ADR | Quyết định | Driver/QA | Status | Evidence |
| --- | --- | --- | --- | --- |
| [0001](0001-modular-monolith.md) | Modular Monolith cho backend cốt lõi | QA-01, QA-02, QA-05, QA-06 | Proposed | Planned |
| [0002](0002-module-boundaries.md) | Module boundaries và dependency direction | QA-01, QA-02, QA-03 | Proposed | Planned |
| [0003](0003-market-data-adapter.md) | Market Data Provider Adapter | QA-03, QA-04 | Proposed | Planned |
| [0004](0004-websocket-realtime.md) | WebSocket realtime protocol | QA-04, QA-09 | Proposed | Planned |
| [0005](0005-strategy-plugin-registry.md) | Strategy Contract và Plugin Registry | QA-01, QA-07 | Proposed | Planned |
| [0006](0006-queue-worker-backtesting.md) | Queue/Worker cho Search và Backtest | QA-05, QA-08, QA-10 | Proposed | Planned |
| [0007](0007-postgresql-redis-ownership.md) | PostgreSQL/Redis ownership | QA-05, QA-07 | Proposed | Planned |
| [0008](0008-sentiment-service-boundary.md) | News/Sentiment service boundary | QA-06, QA-07 | Proposed | Planned |
| [0009](0009-reproducible-experiments.md) | Strategy versioning và reproducible experiments | QA-07 | Proposed | Planned |
| [0010](0010-backtester-evaluator-separation.md) | Tách Backtester, Evaluator và Ranking | QA-01, QA-02, QA-07 | Proposed | Planned |
| [0011](0011-strategy-generator-contract.md) | Pluggable Strategy Generator | QA-02, QA-10 | Proposed | Planned |
| [0012](0012-cqrs-without-event-sourcing.md) | CQRS-style read model, không Event Sourcing | QA-05, QA-07, QA-08 | Proposed | Planned |

## Coverage

| Câu hỏi kiểm chứng của đề/slide | ADR/QA |
| --- | --- |
| Thêm MACD sửa những component nào? | ADR-0005, QA-01 |
| Random Search → Domain-guided có ảnh hưởng Backtester? | ADR-0011, QA-02 |
| Thay Binance/provider có làm Frontend đổi? | ADR-0003, QA-03 |
| Binance disconnect phục hồi thế nào? | ADR-0003/0004, QA-04 |
| 100 → 100.000 backtests scale ở đâu? | ADR-0006, QA-05/QA-10 |
| News/Sentiment down có làm Chart dừng? | ADR-0008, QA-06 |
| Duplicate/retry/event order xử lý thế nào? | ADR-0004/0006/0007 |
| Top-K truy được exact provenance? | ADR-0009/0012, QA-07 |
| Loop và worker có quan sát được? | ADR-0006, QA-08 |

## Conventions

- Tên file: `NNNN-ten-quyet-dinh.md`.
- Status hợp lệ: `Proposed`, `Accepted`, `Deprecated`, `Superseded`.
- ADR mới copy từ [template](template.md); không sửa lịch sử ADR đã bị thay thế.
- `Validation Plan` mô tả cách kiểm chứng; `Evidence` chỉ ghi artifact thực tế, không dùng kết quả minh họa.
- Requirement sources: [đề bài](../Crypto%20Strategy%20Lab%20%E2%80%93%20%C4%90%E1%BB%93%20%C3%A1n%20cu%E1%BB%91i%20k%E1%BB%B3.pdf) và [slide kiến trúc](../KienTrucDoAn_slide.pdf).


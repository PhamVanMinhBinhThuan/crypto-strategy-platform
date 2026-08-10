# Architecture Decision Records (ADR)

ADR ghi lại quyết định kiến trúc quan trọng, lý do lựa chọn và hệ quả.

## Danh sách file

| File | Quyết định |
| --- | --- |
| `template.md` | Mẫu ADR dùng chung |
| `0001-modular-monolith.md` | Kiến trúc Modular Monolith |
| `0002-module-boundaries.md` | Ranh giới và dependency giữa module |
| `0003-market-data-adapter.md` | Adapter cho Market Data Provider |
| `0004-websocket-realtime.md` | WebSocket cho dữ liệu realtime |
| `0005-strategy-plugin-registry.md` | Strategy Contract và Registry |
| `0006-queue-worker-backtesting.md` | Queue/Worker cho Backtest và Search |
| `0007-postgresql-redis-ownership.md` | Vai trò PostgreSQL và Redis |
| `0008-sentiment-service-boundary.md` | Tách Sentiment Service |
| `0009-reproducible-experiments.md` | Versioning và tái lập Experiment |

## Quy ước

- Tên file: `NNNN-ten-quyet-dinh.md`.
- Trạng thái: `Proposed`, `Accepted`, `Deprecated`, `Superseded`.
- ADR mới phải copy từ `template.md`.
- Quyết định bị thay thế phải tạo ADR mới, không xóa ADR cũ.


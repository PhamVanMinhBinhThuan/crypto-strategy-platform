# Demo Profile Contract

## Live profile

- Dùng Binance adapter và các service thật của stack.
- Khai báo symbol, bốn chart/timeframe, bốn Strategy, search seed/space/stop condition và Top-K.
- Credential inject ngoài repository; tài liệu chỉ dùng placeholder.
- Health check phân biệt ready, degraded và unavailable.

## Fixture fallback profile

- Chỉ bật chủ động trong môi trường demo/test.
- Dataset có version/checksum và UI có nhãn `DEMO/FIXTURE`.
- Không dùng để xác nhận provider live, realtime recovery hoặc external sentiment là Verified.
- Runbook ghi lý do chuyển fallback, giới hạn và cách quay lại live.

## Shared requirements

- Không sửa source/database thủ công giữa demo flow.
- Setup/cleanup idempotent ở mức có thể; rerun không tạo outcome business trùng.
- Log và evidence không chứa secret hoặc PII không cần thiết.

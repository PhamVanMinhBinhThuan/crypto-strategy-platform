# 7. 100.000 Backtests scale thế nào? — Answer V2

## Trả lời vấn đáp

Nhóm không chạy 100.000 Backtests ngay trong HTTP request vì sẽ timeout và dễ cạn RAM. API đóng băng Experiment Manifest, ghi Job và Outbox trong database rồi trả `202 Accepted`. Outbox Publisher đẩy job vào Redis Stream; nhiều Worker trong Consumer Group lấy job để chia tải. Mỗi Worker đọc dataset theo batch, chạy Backtest và ghi kết quả về PostgreSQL. Vì Worker độc lập và job có trạng thái bền vững, hệ thống có thể scale ngang bằng cách tăng Worker; frontend chỉ theo dõi progress qua polling hoặc WebSocket.

## Luồng xử lý

```text
API → Manifest + Job + Outbox → Redis Stream → Worker 1..N → PostgreSQL → Top-K
         trả 202 ngay                         đọc dữ liệu theo batch
```

## Các lớp bảo vệ

- **Queue–Worker:** tách tốc độ nhận request khỏi tốc độ xử lý.
- **Batching:** không nạp toàn bộ dataset lớn vào RAM.
- **Backpressure:** giới hạn backlog/admission, không nhận vô hạn.
- **Idempotency + recovery:** chạy lại an toàn khi message lặp hoặc Worker chết.
- **Top-K projection:** không sort toàn bộ kết quả mỗi lần mở Leaderboard.
- **Metrics:** theo dõi throughput, queue lag, thời gian job và failure rate.

## Trạng thái và trade-off

Worker runtime, Redis Stream adapter, Outbox, dedup/recovery test và batch reader đã có. Thêm Worker không bảo đảm throughput tăng tuyến tính vì PostgreSQL pool, lock và dataset I/O có thể thành bottleneck. Con số 100.000 là mục tiêu cần benchmark, không phải kết quả đã được chứng minh chỉ bằng sơ đồ.

## Câu chốt

> 100.000 Backtests được biến thành nhiều job nhỏ, xử lý bất đồng bộ và phân phối cho Worker stateless.

[Xem câu trả lời và bằng chứng chi tiết](answer.md)

# 8. Một service lỗi có làm hỏng toàn hệ thống không? — Answer V2

## Trả lời vấn đáp

Không phải mọi lỗi đều lan ra toàn hệ thống vì các capability có boundary và failure policy riêng. Nếu Sentiment Service down, News/Sentiment chuyển sang degraded hoặc chờ xử lý lại, còn Market chart và Technical Backtest vẫn chạy vì không phụ thuộc runtime ML. Nếu Binance WebSocket lỗi, Market tự reconnect, backfill khoảng trống và deduplicate; các Backtest dùng dataset đã đóng băng vẫn có thể tiếp tục. Timeout, retry có giới hạn, circuit breaker và durable state giúp giới hạn blast radius.

## Phạm vi ảnh hưởng

```text
Sentiment down → News/Sentiment degraded
              ↛ Market chart
              ↛ Technical Backtest

Binance realtime down → reconnect/backfill
                      ↛ dataset lịch sử đã frozen
```

## Nếu giảng viên hỏi sâu

- Retry chỉ dành cho lỗi tạm thời và phải có backoff/jitter.
- Lỗi vĩnh viễn cần trạng thái rõ hoặc Dead Letter Queue.
- Isolation không có nghĩa “không bao giờ ảnh hưởng”; PostgreSQL/Redis dùng chung vẫn là shared failure point.
- Cần E2E/chaos test để đo blast radius và thời gian phục hồi thực tế.

## Trạng thái và trade-off

Boundary và các cơ chế recovery chính đã có. Việc tách runtime, timeout và circuit breaker tăng cấu hình/observability nhưng ngăn một capability phụ kéo sập toàn hệ thống. Mức availability cụ thể vẫn cần đo.

## Câu chốt

> Thành phần phụ thuộc trực tiếp có thể degraded; thành phần độc lập vẫn phải hoạt động.

[Xem câu trả lời và bằng chứng chi tiết](answer.md)

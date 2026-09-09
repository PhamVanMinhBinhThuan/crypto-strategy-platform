# 14. Backtest “hoàn tất” nghĩa là gì về transaction? — Answer V2

## Trả lời vấn đáp

Một Backtest chỉ được coi là hoàn tất khi Trades, Metrics, trạng thái `COMPLETED` và Outbox Event mô tả kết quả được ghi nhất quán. Database changes và Outbox record phải commit trong cùng transaction; sau đó Outbox Publisher mới gửi event sang Redis. Nếu Worker crash sau commit nhưng trước publish, publisher sẽ gửi lại. Nếu crash giữa lúc xử lý, stale attempt được reclaim và job chạy lại an toàn nhờ idempotency. Leaderboard chỉ đọc kết quả đã hoàn tất, không đọc dữ liệu nửa chừng.

## Tình huống crash

```text
1. Ghi Trades + Metrics
2. Đổi Status = COMPLETED
3. Ghi Outbox Event
4. COMMIT một DB transaction
5. Publisher gửi Redis và đánh dấu published
```

Crash trước bước 4 thì rollback; crash sau bước 4 thì Outbox vẫn còn để publish lại.

## Nếu giảng viên hỏi sâu

- Không dễ dùng một ACID transaction bao trùm PostgreSQL và Redis.
- Transactional Outbox đóng khoảng trống giữa commit DB và publish message.
- Publish lại có thể tạo duplicate nên consumer vẫn phải idempotent.
- Attempt là một lần chạy; Job là ý định nghiệp vụ và có thể có nhiều Attempt.

## Trạng thái và trade-off

Outbox và recovery tăng bảng trạng thái, sweeper và logic dedup, đổi lại không mất event khi process crash. Đây là eventual consistency có kiểm soát.

## Câu chốt

> Atomic trong database, at-least-once ngoài queue, idempotent ở consumer.

[Xem câu trả lời và bằng chứng chi tiết](answer.md)

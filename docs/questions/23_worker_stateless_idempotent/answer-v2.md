# 23. Để Worker scale stateless cần điều kiện gì? — Answer V2

## Trả lời vấn đáp

Worker muốn scale ngang phải **stateless**, job phải **idempotent**, và việc phân phối phải dùng Consumer Group. Stateless nghĩa là Worker không giữ trạng thái nghiệp vụ chỉ trong RAM giữa các job; job state nằm ở PostgreSQL, còn queue/progress phù hợp có thể ở Redis. Idempotent nghĩa là cùng message được xử lý lại vẫn chỉ tạo một kết quả nghiệp vụ canonical; `messageId`, unique constraint và fingerprint dùng để chống trùng. Consumer Group chia message giữa các Worker, còn pending recovery xử lý trường hợp Worker chết trước ACK.

## Ba điều kiện

```text
Stateless      → Worker chết/thay instance không mất business state
Idempotent     → Job giao lại không tạo kết quả trùng
Consumer Group → Nhiều Worker chia tải cùng queue
```

## Nếu giảng viên hỏi sâu

- Consumer Group không thay thế idempotency vì Worker có thể crash trước ACK.
- Cache cục bộ không vi phạm stateless nếu không phải source of truth và mất cache không làm sai nghiệp vụ.
- ACK chỉ nên thực hiện sau khi business effect bền vững đã được ghi.
- Tăng Worker phải theo dõi DB pool, lock contention, I/O và queue lag.

## Trạng thái và trade-off

Worker runtime, Consumer Group và `DualLayerIdempotencyGuard` đã có. Stateless/idempotent tăng thao tác storage và coordination nhưng là điều kiện để scale/recovery an toàn; nếu thiếu chúng, thêm Worker chỉ làm lỗi xảy ra nhanh hơn.

## Câu chốt

> Stateless giúp thay Worker được; idempotency giúp chạy lại được; Consumer Group giúp chia tải được.

[Xem câu trả lời và bằng chứng chi tiết](answer.md)

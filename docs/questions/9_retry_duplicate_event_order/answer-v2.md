# 9. Duplicate, retry và event order xử lý thế nào? — Answer V2

## Trả lời vấn đáp

Hệ thống chọn delivery **at-least-once**, nên không giả định message chỉ đến đúng một lần. Mỗi message có `eventId/messageId`; Worker kiểm tra idempotency trước khi xử lý và database dùng unique key hoặc fingerprint để ngăn effect nghiệp vụ trùng. Lỗi tạm thời được retry có giới hạn; Worker chết thì pending message được reclaim; lỗi vĩnh viễn đi Dead Letter Queue. Với event sai thứ tự, consumer dùng aggregate version, revision hoặc timestamp nghiệp vụ để bỏ bản cũ. Frontend cũng deduplicate theo event ID, Candle identity hoặc Leaderboard revision.

## Tình huống điển hình

```text
Xử lý xong → crash trước ACK → broker giao lại
                         ↓
       Idempotency check → đã có kết quả → ACK, không tạo effect lần hai
```

## Nếu giảng viên hỏi sâu

- Idempotency phải bảo vệ business effect trong storage, không chỉ nhớ ID trong RAM.
- Ordering toàn cục rất đắt; thường chỉ cần ordering theo aggregate/stream key.
- Retry cần phân loại lỗi, backoff, giới hạn lần thử và DLQ.
- Không được tuyên bố exactly-once end-to-end chỉ vì broker có tính năng gần giống.

## Trạng thái và trade-off

At-least-once giúp không mất việc nhưng buộc producer/consumer quản lý dedup, version và recovery. Đây là độ phức tạp có chủ đích để đổi lấy độ tin cậy thực tế.

## Câu chốt

> At-least-once + idempotent consumer + version check thực tế hơn lời hứa exactly-once.

[Xem câu trả lời và bằng chứng chi tiết](answer.md)

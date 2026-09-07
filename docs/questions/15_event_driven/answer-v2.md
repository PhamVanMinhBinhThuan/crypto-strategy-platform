# 15. Nhóm dùng Event-Driven ở đâu? — Answer V2

## Trả lời vấn đáp

Nhóm dùng Event-Driven ở các ranh giới bất đồng bộ, đặc biệt từ Outbox sang Redis Stream và từ Backtest hoàn tất sang Evaluation, Ranking hoặc Audit. Worker phát event như `BacktestCompleted` thay vì gọi trực tiếp từng consumer cụ thể. Nhờ đó có thể thêm consumer mới mà không sửa producer, tác vụ dài không block request và sự cố tạm thời có thể retry. Nhóm không dùng event cho mọi thứ; thao tác đồng bộ cần kết quả ngay và nằm trong cùng boundary vẫn dùng direct call.

## Minh họa

```text
Backtest Worker → BacktestCompleted → Evaluation
                                  ├→ Ranking
                                  └→ Audit/Observability
```

## Nếu giảng viên hỏi sâu

- Event mô tả việc đã xảy ra, nên đặt tên ở thì quá khứ và có schema/version.
- Event-Driven giảm coupling vào consumer nhưng tăng coupling vào schema/semantics.
- Consumer phải xử lý duplicate, ordering, retry, DLQ và tracing.
- Direct call phù hợp khi cần phản hồi tức thời và transaction cục bộ rõ ràng.

## Trạng thái và trade-off

Outbox/Redis Stream và event flow chính đã có. Lợi ích là decoupling và resilience; chi phí là eventual consistency và vận hành phức tạp hơn.

## Câu chốt

> Dùng event tại ranh giới cần tách thời gian và tách consumer; không dùng event cho mọi lời gọi.

[Xem câu trả lời và bằng chứng chi tiết](answer.md)

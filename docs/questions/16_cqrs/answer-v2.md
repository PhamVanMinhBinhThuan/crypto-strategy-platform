# 16. CQRS có được dùng không? — Answer V2

## Trả lời vấn đáp

Nhóm dùng **tư duy CQRS có chọn lọc** cho Leaderboard. Write side đi qua pipeline Backtest → Evaluation → Ranking và lưu dữ liệu chi tiết, trong khi read side dùng Top-K projection được tối ưu để UI đọc nhanh. Tuy nhiên nhóm không triển khai full CQRS hay Event Sourcing vì chưa có driver đủ mạnh; immutable records và frozen Manifest đã đáp ứng audit/reproducibility với chi phí thấp hơn. Đây là sự tách model đọc–ghi ở nơi có lợi ích rõ, không phải tách toàn hệ thống.

## Minh họa

```text
Write side: Backtest → Evaluation → Ranking → source records
                                              ↓ project
Read side:                              Top-K Leaderboard → UI
```

## Nếu giảng viên hỏi sâu

- Projection có thể eventual consistent so với write model.
- Revision/version giúp frontend bỏ bản Leaderboard cũ.
- Projection nên có khả năng rebuild từ source records.
- CQRS không đồng nghĩa Event Sourcing; hai pattern có thể dùng độc lập.

## Trạng thái và trade-off

Read projection tăng tốc truy vấn nhưng tạo thêm đồng bộ và recovery logic. Full CQRS/Event Sourcing sẽ tăng đáng kể độ phức tạp, trong khi nhu cầu hiện tại chưa biện minh được chi phí đó.

## Câu chốt

> Nhóm dùng CQRS-lite cho Leaderboard, không dùng full CQRS/Event Sourcing.

[Xem câu trả lời và bằng chứng chi tiết](answer.md)

# 13. ATAM scenario nào nguy hiểm nhất? — Answer V2

## Trả lời vấn đáp

Scenario nguy hiểm nhất là tải 100.000 Backtests vì nó đồng thời đánh vào scalability, performance và reliability. Nhóm giảm rủi ro bằng queue, nhiều Worker, batching, backpressure, idempotency và Top-K projection. Scenario quan trọng thứ hai là Sentiment/News bị lỗi nhưng chart vẫn phải sống; giải pháp là tách runtime, timeout và circuit breaker. Tuy nhiên nhóm chỉ có thể nói kiến trúc đã chuẩn bị tactic; khả năng chịu đúng mức tải mục tiêu vẫn cần benchmark và failure test để xác nhận.

## Cấu trúc một scenario

```text
Source → Stimulus → Environment → Artifact → Response → Response measure
```

Ví dụ: khi 100.000 Backtest được tạo trong giờ cao điểm, API vẫn trả nhanh, job được xếp hàng có giới hạn, không mất job và hệ thống đo được thời gian hoàn tất.

## Nếu giảng viên hỏi sâu

- **Sensitivity points:** batch size, số Worker, DB pool, timeout và retry count.
- **Trade-off point:** tăng Worker có thể tăng throughput nhưng làm DB nghẽn.
- **Risk:** thiếu benchmark nên chưa biết bottleneck thật ở CPU, DB hay I/O.
- ATAM dùng scenario để đánh giá quyết định, không chỉ liệt kê công nghệ.

## Trạng thái và trade-off

Các tactic chính đã có implementation/evidence, còn response measure định lượng cần benchmark. Nói rõ khoảng trống này thể hiện kiến trúc có kiểm chứng, không tô hồng.

## Câu chốt

> ATAM “đập thử” kiến trúc bằng scenario để lộ risk, sensitivity point và trade-off point.

[Xem câu trả lời và bằng chứng chi tiết](answer.md)

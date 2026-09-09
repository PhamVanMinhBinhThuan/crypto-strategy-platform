# 11. Nhóm dùng ADD như thế nào? — Answer V2

## Trả lời vấn đáp

ADD là Attribute-Driven Design: bắt đầu từ architectural drivers và quality attribute scenarios, không bắt đầu từ framework. Trong mỗi vòng lặp, nhóm chọn phần hệ thống và yêu cầu quan trọng nhất, chọn pattern/tactic phù hợp, phân bổ trách nhiệm, định nghĩa interface rồi kiểm chứng lại bằng scenario và test. Ví dụ driver scalability dẫn đến Queue–Worker; từ đó nhóm định nghĩa Backtest Job, Outbox, Consumer Group, idempotency và metric throughput. Nếu kết quả chưa đạt response measure thì quay lại điều chỉnh thiết kế.

## Chuỗi lập luận mẫu

```text
ASR: 100.000 Backtests
→ Tactic: concurrency + resource management
→ Pattern: Queue–Worker
→ Interface: BacktestJob/Outbox/Consumer
→ Verify: throughput, queue lag, failure recovery
```

## Nếu giảng viên hỏi sâu

- Input gồm business goal, yêu cầu chức năng quan trọng, quality attribute và constraint.
- Output gồm responsibilities, interface, architecture view, ADR và cách verify.
- ADD diễn ra lặp dần, không thiết kế toàn bộ hệ thống trong một lần.
- Pattern chỉ hợp lý khi truy ngược được về driver cụ thể.

## Trạng thái và trade-off

ADD tạo traceability cho quyết định nhưng cần thời gian ghi scenario, ADR và verification. Một thiết kế có pattern đẹp nhưng không có response measure vẫn chưa chứng minh đã đáp ứng driver.

## Câu chốt

> ADD tạo chuỗi bảo vệ được: driver → tactic/pattern → interface → verification.

[Xem câu trả lời và bằng chứng chi tiết](answer.md)

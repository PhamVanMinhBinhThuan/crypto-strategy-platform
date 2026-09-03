# 11. Nhóm dùng ADD như thế nào để ra được quyết định kiến trúc?

## Trả lời ngắn

ADD (Attribute-Driven Design) là phương pháp thiết kế kiến trúc bắt đầu từ driver, không bắt đầu từ công nghệ. Nhóm áp dụng ADD theo 8 bước lặp: chọn ASR (architectural significant requirement), xác định phạm vi cần thiết kế, chọn pattern/tactic phù hợp, phân bổ trách nhiệm, định nghĩa interface, rồi verify xem đã đáp ứng driver chưa — nếu chưa thì quay lại. Ví dụ cụ thể: Driver **Scalability** → chọn Queue + Worker pool → định nghĩa contract `BacktestJob` → đo throughput/failure.

## Minh họa — ADD cho driver Scalability

```mermaid
flowchart TD
    D1[\"1. Design purpose: scale backtest\"]
    D2[\"2. Chọn ASR: 100.000 candidates không block API\"]
    D3[\"3. Phạm vi: luồng từ Generate → Result\"]
    D4[\"4. Pattern/tactic: Job Queue + Consumer Group\"]
    D5[\"5. Phân bổ: Generator sinh job, Worker consume, DB lưu kết quả\"]
    D6[\"6. Interface: BacktestJob contract, Redis Stream key\"]
    D7[\"7. Verify: throughput có tuyến tính với số Worker?\"]
    D8[\"8. Chưa đạt → quay lại bước 2\"]
    D1 --> D2 --> D3 --> D4 --> D5 --> D6 --> D7 --> D8
```

## 8 bước ADD áp dụng cho dự án

| Bước | Mô tả chung | Áp dụng vào nhóm |
| --- | --- | --- |
| 1. Design purpose | Cần thiết kế cái gì? | Luồng Generate → Backtest → Evaluate |
| 2. Chọn ASRs | Yêu cầu nào quan trọng? | Scalability, Reliability, Modifiability |
| 3. Chọn phạm vi | Phần nào của hệ thống? | Worker service và Job lifecycle |
| 4. Pattern/tactic | Giải pháp khả dĩ | Redis Streams + Consumer Group |
| 5. Phân bổ trách nhiệm | Ai làm gì? | Generator, Worker, Outbox, Evaluator |
| 6. Interface | Hợp đồng giữa các phần | BacktestJob, ExperimentManifest |
| 7. Verify vs ASRs | Có đáp ứng yêu cầu chưa? | Throughput test, failure test |
| 8. Lặp lại | Chưa đạt → quay bước 2 | Điều chỉnh batch size, retry policy |

## Trạng thái và trade-off

ADD là vòng lặp, không phải đường thẳng. Nhóm đã qua nhiều vòng: ban đầu dùng synchronous call, sau đó nhận ra block API và chuyển sang async queue. Mỗi vòng thêm contract overhead nhưng giảm coupling và tăng khả năng scale độc lập.

## Bằng chứng trong project

- [ADR-0006 — Queue và Worker](../../adr/0006-queue-worker-backtesting.md)
- [ADR-0001 — Modular Monolith](../../adr/0001-modular-monolith.md)
- [Quality Attribute Scenarios](../../architecture/quality-attributes.md)
- [Architecture Evidence](../../architecture/architecture-evidence.md)

## Nguồn đề bài

Slide 32 (ADD 8 bước), slide 65–66 trong [slide kiến trúc](../../KienTrucDoAn_slide.pdf); Syllabus Topic 4 – ADD.

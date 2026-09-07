# 3. Boundary của Market / Strategy / Experiment / News? — Answer V2

## Trả lời vấn đáp

Mỗi module sở hữu một capability và ngôn ngữ nghiệp vụ riêng. Market sở hữu Candle, Dataset và provider normalization; Strategy sở hữu Strategy contract, Signal, Plugin và Registry; Experiment sở hữu Manifest, Candidate, Job và Attempt; News sở hữu NewsItem và vòng đời Sentiment. Module chỉ công bố public API, port hoặc event cần thiết. Module khác không được import package `internal`, gọi repository của nó hay truy cập trực tiếp bảng dữ liệu do nó sở hữu. `apps/api` và `apps/worker` làm composition/orchestration; persistence là adapter implement output port.

## Minh họa

```text
App/Orchestrator → Public API/Port → Module Domain
Persistence/Provider Adapter ─implement→ Output Port
Module A ─X→ internal/repository/table của Module B
```

## Nếu giảng viên hỏi sâu

- Dùng chung PostgreSQL không đồng nghĩa được đọc chéo bảng tùy ý.
- Output port do module nghiệp vụ định nghĩa; infrastructure chỉ implement.
- Boundary được thể hiện bằng Gradle module, package `api/internal` và architecture test.
- Shared database vẫn có nguy cơ coupling nên cần ownership convention và review.

## Trạng thái và trade-off

Boundary hiện được bảo vệ trong source và test. Chi phí là có thêm interface, mapping và quy tắc phụ thuộc; lợi ích là thay adapter hoặc tách module sau này ít ảnh hưởng hơn.

## Câu chốt

> Boundary tốt quy định rõ ai sở hữu dữ liệu và module khác được phép gọi qua contract nào; nó không chỉ là cách chia folder.

[Xem câu trả lời và bằng chứng chi tiết](answer.md)

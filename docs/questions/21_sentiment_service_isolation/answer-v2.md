# 21. Tại sao Sentiment là service Python riêng? — Answer V2

## Trả lời vấn đáp

Sentiment được tách thành Python/FastAPI service vì có runtime và thư viện ML khác Java, resource profile riêng và failure mode riêng như model load, inference timeout hoặc OOM. Nó cũng có thể scale độc lập theo tải phân tích tin. Khi service này down, chỉ News/Sentiment pipeline chuyển sang degraded hoặc pending; Market chart và Technical Backtest vẫn hoạt động vì không gọi Sentiment. Java Worker giao tiếp qua HTTP contract có timeout, retry giới hạn và circuit breaker.

## Phạm vi lỗi

```text
News Collector → Sentiment Service DOWN → News job pending/degraded

Market → Chart      vẫn chạy
Market → Backtest   vẫn chạy
```

## Nếu giảng viên hỏi sâu

- Tách service tăng deployment, network và contract overhead.
- Lợi ích là fault isolation và khả năng scale/runtime độc lập.
- Không retry vô hạn vì có thể gây retry storm và làm sự cố nặng hơn.
- Sentiment-based Strategy cần policy rõ khi sentiment chưa có, không được âm thầm dùng dữ liệu sai.

## Trạng thái và trade-off

Isolation boundary, HTTP client timeout và resilience test đã có. Đổi lại hệ thống phải quản lý deployment Python, contract version, network latency và observability xuyên service.

## Câu chốt

> Tách Sentiment vì có driver thật về runtime, scale và failure isolation, không phải để gắn nhãn microservices.

[Xem câu trả lời và bằng chứng chi tiết](answer.md)

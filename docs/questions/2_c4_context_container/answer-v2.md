# 2. C4 Context và Container của nhóm? — Answer V2

## Trả lời vấn đáp

Ở mức **Context**, Crypto Strategy Lab là một hệ thống được User/Trader sử dụng, lấy dữ liệu thị trường từ Binance và tin tức từ News Providers. Ở mức **Container**, bên trong có Web Next.js, API Spring Boot, Worker Spring Boot, Sentiment FastAPI, PostgreSQL và Redis. Web gọi API qua REST/WebSocket; API xử lý request ngắn và realtime; Worker chạy Backtest, Search và News bất đồng bộ; PostgreSQL là nguồn dữ liệu bền vững; Redis dùng cho queue, cache hoặc progress. API tách khỏi Worker để tác vụ dài không chặn HTTP request.

## Minh họa

```text
User → Web → API → PostgreSQL/Redis
                    ↓ queue
                  Worker → Sentiment
API ↔ Binance       Worker → News Providers
```

## Nếu giảng viên hỏi sâu

- C1 cho biết hệ thống giao tiếp với **ai và hệ thống ngoài nào**.
- C2 cho biết các **ứng dụng/runtime và data store lớn** bên trong.
- C3 mới mở Java backend thành Market, Strategy, Experiment, News...
- Sentiment tách riêng vì runtime ML/Python; Worker tách vì tải CPU dài và cần scale độc lập.

## Trạng thái và trade-off

Các container backend, Worker và Sentiment đã có source; Web foundation cũng đã có. Tách runtime tăng chi phí deploy và quan sát, nhưng giúp cô lập lỗi và scale đúng phần cần thiết. Các thông số hosting hoặc tải chưa đo phải ghi là Planned.

## Câu chốt

> Context là nhìn hệ thống từ ngoài vào; Container là mở hộp hệ thống ra.

[Xem câu trả lời và bằng chứng chi tiết](answer.md)

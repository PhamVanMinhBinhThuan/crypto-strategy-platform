# API Documentation

Thư mục này quản lý quy ước và contract giao tiếp của hệ thống qua REST và WebSocket.

## File trong thư mục

| File | Mục đích |
| --- | --- |
| `conventions.md` | Quy ước chung cho REST API |
| `openapi.yaml` | OpenAPI contract tổng của Spring API |
| `websocket-events.md` | Protocol và event catalog WebSocket |
| `internal-sentiment-contract.md` | Contract nội bộ giữa Java Worker và Python Sentiment Service |
| `error-catalog.md` | Cấu trúc lỗi và danh sách error code |
| `examples.md` | Request/response/event mẫu |

## Nguồn dữ liệu

- Khi thiết kế feature: contract nằm trong `specs/<feature>/contracts/`.
- Khi contract được duyệt: cập nhật `openapi.yaml` hoặc `websocket-events.md`.
- Không duy trì hai contract có nội dung khác nhau.

## Quy tắc

- Frontend chỉ sử dụng internal API contract, không dùng Binance payload trực tiếp.
- Contract thay đổi phải được producer và consumer review.
- Breaking change phải được version hóa.


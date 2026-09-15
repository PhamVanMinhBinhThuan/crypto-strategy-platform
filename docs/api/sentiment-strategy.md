# F016 Sentiment Strategy API Notes

OpenAPI chuẩn vẫn nằm tại [`openapi.yaml`](openapi.yaml). Trang này giải thích cách các contract hiện
có ghép thành luồng Sentiment Strategy; không tạo API riêng để Strategy gọi Python.

## Contract được dùng

- `GET /strategies`: trả `sentiment-polarity@1.0.0`, bốn parameter definitions và finite Search hints.
- `POST /experiments`: khi Strategy pool chứa Sentiment, API tạo snapshot trước khi ghi Search graph.
- `GET /backtest-results/{resultId}`: `sentimentProvenance` trả snapshot/model/fingerprint và decision
  evidence đã tạo ra BUY/SELL/HOLD.
- `POST /experiments/{experimentId}/reproductions`: tái lập bằng frozen references của run nguồn.
- `GET /news-items/sentiment-status`: trả `AVAILABLE` hoặc `DEGRADED`; outage downstream không biến
  endpoint này thành lỗi API và không ẩn kết quả News đã lưu.

## Bốn tham số

| Field | Ý nghĩa | Search range gợi ý |
|---|---|---|
| `lookbackHours` | Khoảng News trước mỗi thời điểm đánh giá | 6–72, step 6 |
| `minimumArticles` | Số bài tối thiểu trước khi BUY/SELL | 1–10, step 1 |
| `buyThreshold` | Điểm từ ngưỡng này trở lên là BUY | 0.10–0.50, step 0.05 |
| `sellThreshold` | Điểm từ ngưỡng này trở xuống là SELL | -0.50–-0.10, step 0.05 |

`sellThreshold` luôn phải nhỏ hơn `buyThreshold`. Server là nơi validate, đóng băng input và tính
kết quả; frontend chỉ dựng form từ descriptor và hiển thị evidence.

## Failure contract

Nếu Strategy yêu cầu Sentiment nhưng snapshot không hợp lệ/không tạo được, Start/Reproduction thất
bại trước khi enqueue Candidate. Lỗi công khai dùng mã ổn định và không để lộ token, SQL hoặc payload
provider. Search/Backtest kỹ thuật không thực hiện bước preflight này.

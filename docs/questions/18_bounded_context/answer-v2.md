# 18. Bounded Context của nhóm là gì? — Answer V2

## Trả lời vấn đáp

Nhóm có bốn Bounded Context chính: Market Data với Candle/Pair/Timeframe; Strategy với StrategyDefinition/Signal/Combination; Experiment với Candidate/Backtest/Evaluation/Ranking; và News Intelligence với NewsItem/Sentiment. Ranh giới ngữ nghĩa điển hình là `Signal` khác `Trade`: Signal chỉ là quyết định phân tích BUY/SELL/HOLD, còn Trade là giao dịch đã được mô phỏng với entry, exit, PnL, fee và slippage. Nếu dùng chung hai khái niệm, Strategy sẽ bị kéo vào chi tiết của Backtest và tạo semantic coupling.

## Signal và Trade

| Thuộc tính | Signal | Trade |
| --- | --- | --- |
| Context | Strategy | Experiment/Backtest |
| Ý nghĩa | BUY/SELL/HOLD | Giao dịch mô phỏng đã thực hiện |
| Dữ liệu | quyết định + thời điểm | entry, exit, PnL, fee, slippage |
| Trách nhiệm | phân tích | mô phỏng execution |

## Nếu giảng viên hỏi sâu

- Mỗi context có model, invariant và ubiquitous language riêng.
- Đổi cách tính fee/slippage không nên làm Strategy API đổi.
- Các context trao đổi qua contract rõ ràng thay vì chia sẻ object nội bộ.
- Bounded Context là ranh giới ngôn ngữ/model, không nhất thiết là microservice.

## Câu chốt

> Signal là ý định phân tích; Trade là kết quả thực thi mô phỏng.

[Xem câu trả lời và bằng chứng chi tiết](answer.md)

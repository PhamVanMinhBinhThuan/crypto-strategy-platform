# Demo Script

**Demo Version/Tag**: [Điền]  
**Thời lượng mục tiêu**: [Điền]  
**Người điều phối**: [Điền]

## 1. Giới thiệu

- Mục tiêu dự án: [Điền]
- Vấn đề kiến trúc cần giải quyết: [Điền]
- Phạm vi MVP: [Điền]

## 2. Market Dashboard

1. [Mở Dashboard]
2. [Hiển thị historical chart]
3. [Chứng minh realtime update]
4. [Mở tối đa bốn chart]
5. [Đổi timeframe độc lập]

**Điểm kiến trúc cần nói**: [Điền]

## 3. Strategy and Backtesting

1. [Chọn bốn strategies]
2. [Tạo composite]
3. [Chạy backtest]
4. [Xem metrics]
5. [Xem trades và Entry/Exit]

**Điểm kiến trúc cần nói**: [Điền]

## 4. Search and Leaderboard

1. [Khởi động Random Search]
2. [Theo dõi progress]
3. [Chứng minh stop condition]
4. [Xem Top-K]
5. [Mở strategy detail]

**Điểm kiến trúc cần nói**: [Điền]

## 5. News and Sentiment

1. [Mở News]
2. [Xem normalized news]
3. [Xem sentiment label/score]
4. [Hiển thị model version]

**Điểm kiến trúc cần nói**: [Điền]

## 6. Architecture Proof

- **Thêm Strategy**: mở diff của MACD plugin; chỉ ra không có thay đổi trong Backtester, Evaluator, Leaderboard hoặc UI; chạy contract test.
- **Thay Search**: chuyển Random sang Domain-guided qua registry/configuration và chạy cùng downstream pipeline.
- **Thay Market Provider**: chuyển Binance sang fixture adapter; giữ nguyên REST/WebSocket payload và dashboard consumer.
- **Scale Backtest**: so sánh benchmark cùng workload với 1 và 3 workers; trình bày throughput, queue lag và duplicate count.
- **Realtime recovery**: ngắt upstream, quan sát trạng thái reconnecting, backfill và reconciliation report.
- **Failure isolation**: dừng Sentiment Service; chỉ News/Sentiment degraded trong khi realtime chart tiếp tục.
- **Reproduce Experiment**: mở Top-K detail/manifest, replay và so sánh Trades, bốn metrics cùng fingerprint.
- **Evidence rule**: chỉ trình bày artifact có commit, environment và kết quả thật; planned item không được nói là đã verified.

## 7. Kết luận

- Những gì đã hoàn thành: [Điền]
- Giới hạn MVP: [Điền]
- Hướng mở rộng: [Điền]


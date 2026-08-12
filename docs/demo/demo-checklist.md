# Demo Checklist

## Trước ngày demo

- [ ] Chốt commit/tag demo
- [ ] Chốt dữ liệu và parameters
- [ ] Chạy toàn bộ test
- [ ] Tổng duyệt trên máy demo
- [ ] Chuẩn bị video dự phòng
- [ ] Kiểm tra không lộ secret

## Trước giờ demo

- [ ] Docker/services healthy
- [ ] API health check pass
- [ ] Worker đang chạy
- [ ] Frontend mở được
- [ ] Sentiment service sẵn sàng
- [ ] Binance/News Provider truy cập được
- [ ] Fixture fallback sẵn sàng
- [ ] Không có port conflict

## Chức năng phải demo

- [ ] Historical chart
- [ ] Realtime chart
- [ ] Tối đa bốn chart độc lập
- [ ] Bốn strategies
- [ ] Composite strategy
- [ ] Backtest
- [ ] Bốn metrics
- [ ] Trade visualization
- [ ] Random Search và stop condition
- [ ] Top-K Leaderboard
- [ ] News Sentiment

## Kiến trúc phải giải thích

- [ ] Modular boundaries
- [ ] Strategy Plugin/Registry
- [ ] Market Data Adapter
- [ ] Queue/Worker scalability
- [ ] Fault isolation
- [ ] Experiment reproducibility

## Architecture Proof phải thu thập

- [ ] AP-01: diff thêm MACD không sửa Backtester/Evaluator/Leaderboard/UI
- [ ] AP-02: Domain-guided generator chạy qua pipeline không đổi
- [ ] AP-04: ngắt Binance và chứng minh reconnect ≤30 giây, không thiếu/trùng closed Candle
- [ ] AP-05: benchmark 1 và 3 workers với cùng workload, throughput ≥2× và không duplicate
- [ ] AP-06: dừng Sentiment Service nhưng realtime chart vẫn chạy, degraded ≤5 giây
- [ ] AP-07: replay manifest cho cùng Trades, bốn metrics và fingerprint
- [ ] AP-08/AP-09/AP-10: lưu log/metrics cho observability, realtime latency và async Search
- [ ] Mỗi artifact ghi commit/tag, môi trường, cấu hình, ngày và người thực hiện
- [ ] Không đánh dấu `Verified` nếu chưa đạt measure trong architecture-evidence.md

## Sau demo

- [ ] Ghi câu hỏi của giảng viên
- [ ] Ghi lỗi phát sinh
- [ ] Lưu phiên bản cuối và tài liệu nộp


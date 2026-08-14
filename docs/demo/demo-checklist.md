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
- [ ] Extensibility proof: thêm MACD với downstream không đổi
- [ ] Replaceability proof: thêm/đổi Strategy Generator
- [ ] Scale proof: so sánh 1 và 3 Worker, kiểm tra duplicate
- [ ] Failure proof: tắt Sentiment và ngắt/reconnect Binance
- [ ] Provenance proof: Top-K truy về exact Manifest/version
- [ ] Evidence được gắn commit/tag và không dùng số liệu giả

Chi tiết target và trạng thái bằng chứng nằm trong [Architecture Evidence](../architecture/architecture-evidence.md).

## Sau demo

- [ ] Ghi câu hỏi của giảng viên
- [ ] Ghi lỗi phát sinh
- [ ] Lưu phiên bản cuối và tài liệu nộp


# Bản phác thảo giao diện

## Dự án Stitch

- Tên dự án: `Crypto StrategyLab`
- Liên kết: `[Thêm liên kết dự án Stitch]`
- Nền tảng: Web trên máy tính
- Giao diện: ...

## Các màn hình chính

| ID    | Màn hình                   | Người phụ trách | Trạng thái | File đã duyệt                    |
| ----- | -------------------------- | --------------- | ---------- | -------------------------------- |
| UI-01 | Bảng điều khiển thị trường | `[Phân công]`   | Bản nháp   | `screens/market-dashboard.png`   |
| UI-02 | Trình tạo chiến lược       | `[Phân công]`   | Bản nháp   | `screens/strategy-composer.png`  |
| UI-03 | Kết quả Backtest           | `[Phân công]`   | Bản nháp   | `screens/backtest-results.png`   |
| UI-04 | Tìm kiếm và bảng xếp hạng  | `[Phân công]`   | Bản nháp   | `screens/search-leaderboard.png` |
| UI-05 | Cảm xúc tin tức            | `[Phân công]`   | Bản nháp   | `screens/news-sentiment.png`     |

## Luồng màn hình

```text
Bảng điều khiển thị trường
  -> Trình tạo chiến lược
      -> Kết quả Backtest
          -> Tìm kiếm và bảng xếp hạng
      -> Tìm kiếm và bảng xếp hạng
          -> Xem chi tiết Top-K
          -> Xem biểu đồ và giao dịch

Bảng điều khiển thị trường
  -> Cảm xúc tin tức
      -> Tìm kiếm và bảng xếp hạng (minh họa SentimentStrategy, không bắt buộc)
```

## Các file liên quan

- Hướng dẫn sử dụng Stitch và các trạng thái cần vẽ: [stitch-guide.md](stitch-guide.md)
- Ảnh các màn hình đã duyệt: [`screens/`](screens/)

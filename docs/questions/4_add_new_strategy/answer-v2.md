# 4. Thêm Strategy mới sửa ở đâu? — Answer V2

## Trả lời vấn đáp

Để thêm Strategy mới như MACD, nhóm tạo `MacdStrategy` implement `Strategy`, tạo `MacdStrategyPlugin` khai báo ID, version, parameter schema và cách khởi tạo, viết test rồi đăng ký plugin vào `StrategyPlugins.trusted()`. Backtester chỉ gọi contract `evaluate(context)`, còn Search, Evaluation và Leaderboard làm việc với model chung nên không cần thêm nhánh `if MACD`. Đây là Open–Closed Principle: mở rộng bằng plugin mới và hạn chế sửa pipeline đang ổn định.

## Các bước cần làm

1. Implement `Strategy`.
2. Tạo `StrategyPlugin` và descriptor.
3. Khai báo/validate parameter schema và warm-up requirement.
4. Viết unit test rồi đăng ký vào Registry.

## Nếu giảng viên hỏi sâu

- Registry kiểm tra ID/version trùng và validate parameters trước khi tạo Strategy.
- UI có thể render cấu hình theo descriptor chung nếu contract không đổi.
- Backtester không biết Strategy cụ thể là MA, MACD hay RSI.
- Hiện project có Moving Average Crossover; MACD chỉ là ví dụ mở rộng, chưa implement.

## Trạng thái và trade-off

Plugin/Registry giúp thêm Strategy an toàn nhưng cần duy trì version và compatibility của parameter schema. Một plugin lỗi vẫn phải bị chặn bằng validation và test trước khi đưa vào danh sách trusted.

## Câu chốt

> Thêm Strategy là thêm implementation và plugin, không sửa Backtester.

[Xem câu trả lời và bằng chứng chi tiết](answer.md)

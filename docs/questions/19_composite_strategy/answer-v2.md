# 19. Composite Strategy hoạt động thế nào? — Answer V2

## Trả lời vấn đáp

Composite Strategy chạy các strategy con để lấy Signal, sau đó giao cho một `CombinationPolicy` quyết định Signal cuối. Với Majority Vote, BUY chiếm 2/3 thì kết quả là BUY. Với Weighted Vote, mỗi strategy có trọng số; quy đổi BUY = 1, HOLD = 0, SELL = -1, cộng điểm rồi so với threshold. Không strategy con nào tự quyết định kết quả tổng hợp. Việc tách policy khỏi strategy giúp thêm cách kết hợp mới và test độc lập mà không sửa thuật toán con.

## Ví dụ

```text
MA  = BUY  × 0.2 =  0.2
RSI = SELL × 0.3 = -0.3
SR  = BUY  × 0.5 =  0.5
Tổng               =  0.4 → BUY nếu threshold = 0.3
```

## Nếu giảng viên hỏi sâu

- Majority Vote dùng khi các strategy có độ tin cậy ngang nhau.
- Weighted Vote dùng khi có cơ sở gán độ tin cậy khác nhau.
- Weight và threshold phải nằm trong frozen configuration để tái lập.
- Cần quy tắc rõ cho hòa phiếu, thiếu signal hoặc strategy con lỗi.

## Trạng thái và trade-off

Tách `CombinationPolicy` tuân thủ Single Responsibility và Open–Closed. Weighted Vote linh hoạt hơn nhưng đặt ra câu hỏi weight đến từ đâu và có overfit trên dữ liệu lịch sử hay không.

## Câu chốt

> Strategy con tạo tín hiệu; CombinationPolicy giải quyết khi các tín hiệu “cãi nhau”.

[Xem câu trả lời và bằng chứng chi tiết](answer.md)

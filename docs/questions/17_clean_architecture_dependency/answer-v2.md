# 17. Strategy có được gọi trực tiếp DB hay Binance không? — Answer V2

## Trả lời vấn đáp

Không. Strategy là business policy ở lớp trong nên không import repository, Spring, database schema hay Binance client. Dữ liệu cần thiết được chuẩn bị và truyền vào qua `StrategyContext`; các port định nghĩa abstraction, còn adapter ở lớp ngoài implement việc đọc DB hoặc gọi provider. Nhờ Dependency Rule, đổi Binance sang OKX hoặc đổi persistence không làm thay đổi thuật toán Strategy, đồng thời Strategy có thể unit test bằng input thuần.

## Dependency Rule

```text
Infrastructure → Adapter → Port/Interface ← Domain Strategy
                                      data → StrategyContext
```

## Nếu giảng viên hỏi sâu

- Nếu Strategy tự query DB, kết quả phụ thuộc ngầm vào trạng thái ngoài và khó tái lập.
- Domain sở hữu interface cần dùng; infrastructure phụ thuộc và implement interface đó.
- Application Service/Orchestrator lấy dữ liệu rồi gọi Strategy.
- Strategy chỉ nên quyết định nghiệp vụ từ input, không điều phối I/O.

## Trạng thái và trade-off

Port/context tạo thêm mapping và orchestration nhưng đem lại testability, replaceability và reproducibility. Architecture test giúp ngăn dependency đảo ngược theo thời gian.

## Câu chốt

> Strategy nhận data, không tự đi tìm data.

[Xem câu trả lời và bằng chứng chi tiết](answer.md)

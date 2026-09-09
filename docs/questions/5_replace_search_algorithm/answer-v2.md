# 5. Đổi Search Algorithm sửa ở đâu? — Answer V2

## Trả lời vấn đáp

Muốn thay Random Search bằng Domain-guided hoặc Genetic Search, nhóm tạo generator mới implement `StrategyGenerator`, khai báo ID/version, bảo đảm tính deterministic, viết test và đăng ký qua factory/registry. Tất cả generator đều trả `GeneratedCandidate` theo contract chung, nên Job, Worker, Backtest, Evaluation và Leaderboard không phụ thuộc candidate được sinh bằng thuật toán nào. Experiment lưu generator ID, version, seed, Search Space và state để có thể tái lập.

## Minh họa

```text
Random / Genetic / Domain-guided
              ↓ cùng implement
       StrategyGenerator
              ↓
       GeneratedCandidate
              ↓
Job → Backtest → Evaluation → Leaderboard
```

## Nếu giảng viên hỏi sâu

- Generator chỉ đề xuất candidate, không chạy Backtest hay xếp hạng.
- Cùng input, seed và state phải sinh cùng kết quả.
- Registry chọn đúng cặp generator ID + version và từ chối đăng ký trùng.
- Hiện có Random Search; Genetic/Domain-guided là điểm mở rộng dự kiến.

## Trạng thái và trade-off

Contract, Registry, Random Search và replaceability test đã có. Thuật toán mới có thể tìm kiếm tốt hơn nhưng tăng chi phí tính toán, state phức tạp và khó bảo đảm reproducibility hơn.

## Câu chốt

> `StrategyGenerator` là điểm thay thế; output phía sau vẫn là `GeneratedCandidate` chuẩn.

[Xem câu trả lời và bằng chứng chi tiết](answer.md)

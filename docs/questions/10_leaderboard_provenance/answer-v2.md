# 10. Từ kết quả Leaderboard truy lại nguồn gốc thế nào? — Answer V2

## Trả lời vấn đáp

Mỗi Leaderboard Entry không chỉ có điểm và thứ hạng mà còn tham chiếu Evaluation Result, Backtest Result và fingerprint. Từ đó có thể lần ngược qua Successful Attempt, Candidate và Experiment Manifest để biết dataset version/checksum, Strategy ID/version, parameters, generator ID/version, seed, fee, slippage và các assumption đã dùng. Nhờ provenance graph được đóng băng, nhóm có thể giải thích vì sao một kết quả đứng Top 1 và tạo reproduction request để chạy lại đúng cấu hình.

## Chuỗi truy vết

```text
Leaderboard Entry
→ Evaluation Result
→ Backtest Result + Trades
→ Successful Attempt
→ Candidate
→ Frozen Experiment Manifest
```

## Nếu giảng viên hỏi sâu

- Không tái tạo bằng cấu hình “mới nhất” vì dataset, code hay Strategy version có thể đã đổi.
- Fingerprint giúp nhận biết input/cấu hình tương đương đã tạo kết quả canonical nào.
- Reproducibility cần cả version code, dataset và execution assumptions, không chỉ parameters.
- Leaderboard là read projection; source of truth vẫn là các record bất biến phía sau.

## Trạng thái và trade-off

Frozen graph và reproduction workflow đã có. Việc lưu nhiều metadata tăng storage và quản lý version, đổi lại có auditability, giải thích kết quả và tránh bảng xếp hạng “không rõ nguồn”.

## Câu chốt

> Từ một hạng trên Leaderboard phải lần ngược được toàn bộ dữ liệu, thuật toán, tham số và lần chạy đã tạo ra nó.

[Xem câu trả lời và bằng chứng chi tiết](answer.md)

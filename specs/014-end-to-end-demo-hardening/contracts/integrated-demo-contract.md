# Integrated Demo Contract

| Stage | Authoritative input | Observable output | Owner |
|---|---|---|---|
| Market | Provider candles/trades | Canonical candles, freshness, reconnect state | Market Data |
| Strategy | Published plugin/version/config | Validated Strategy snapshot | Strategies |
| Search | Dataset, space, seed, stop condition | Experiment, candidates, jobs, progress | Search/Experiment |
| Backtest | Candidate, frozen dataset, assumptions | Result and immutable Trades | Backtesting |
| Evaluation | Accepted Result/Trades | Return, Win Rate, Max Drawdown, Trade Count | Evaluation |
| Ranking | Evaluations, ranking version | Deterministic Top-K revision | Leaderboard |
| Detail | Leaderboard result reference | Authoritative result, trades, provenance | API/result owner |
| News | Normalized items | News plus optional sentiment/provenance | News/Sentiment |

## Invariants

- Browser chỉ gọi public application boundary, không đọc database hoặc tính nghiệp vụ.
- Người dùng có thể tạo Strategy cá nhân từ Strategy hệ thống đã phát hành; hệ thống lưu definition/version/config qua authorized application boundary và không nhận source code tùy ý.
- Retry tạo Attempt, không tạo outcome trùng; correlation ID được giữ qua async hop.
- Sentiment unavailable không chặn Market hoặc technical Backtest.
- Leaderboard entry phải resolve về đúng authoritative Result.
- Reproduction tạo run mới; `MATCHED`/`MISMATCHED` dựa trên canonical evidence.
- Fixture chỉ hợp lệ khi profile và UI cùng ghi rõ `DEMO/FIXTURE`.

F014 không thay public contract. Nếu buộc phải đổi semantics, phải cập nhật spec, compatibility plan và ADR khi phù hợp trước implementation.

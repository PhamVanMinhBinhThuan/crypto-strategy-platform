# ADR-0010: Tách Backtester, Evaluator và Ranking

**Status**: Proposed
**Date**: 2026-08-12
**Owners**: Tiến Luật

## Context

Backtester mô phỏng giao dịch, Evaluator tính metrics, còn Ranking áp score/tie-break và duy trì Top-K. Gộp ba trách nhiệm sẽ khiến thay metric hoặc ranking formula ảnh hưởng simulation, đồng thời làm Search/Strategy bị coupling vào kết quả hiển thị.

## Drivers and Quality Scenarios

- [QA-01 Add New Strategy](../architecture/quality-attributes.md#qa-01--add-new-strategy)
- [QA-02 Replace Search Algorithm](../architecture/quality-attributes.md#qa-02--replace-search-algorithm)
- [QA-07 Reproduce Experiment](../architecture/quality-attributes.md#qa-07--reproduce-experiment)

## Decision

- `Backtester` nhận immutable dataset, Strategy contract và simulation assumptions; trả `BacktestResult` cùng Trades.
- `Evaluator` nhận `BacktestResult`; trả versioned `EvaluationResult` gồm tối thiểu Return, Win Rate, Maximum Drawdown và Number of Trades.
- `RankingPolicy` nhận `EvaluationResult`; trả deterministic score/tie-break data cho Leaderboard.
- Search chỉ sinh Candidate; coordinator/worker nối pipeline qua các public contracts.
- Mỗi implementation/formula có version trong Experiment Manifest; không đọc hidden global default.

## Alternatives Considered

- **Một BacktestingService làm tất cả**: ít interface hơn nhưng tạo God Service và làm metrics/ranking khó thay độc lập.
- **Strategy tự tính performance**: trộn signal generation với simulation/evaluation và phá reproducibility.
- **Frontend tính metrics/ranking**: phân tán business logic và tạo kết quả không nhất quán.

## Consequences

### Positive

- Strategy/Search/metric/ranking có thể thay độc lập.
- Unit test từng policy bằng fixture nhỏ và replay đúng version.
- Worker có thể tối ưu execution mà không đổi domain contracts.

### Negative

- Cần DTO/mapping và version cho ba bước.
- Pipeline có nhiều artifact phải lưu và truy vết hơn.

## Affected Components

- `modules/backtesting`, `modules/evaluation`, `modules/leaderboard`
- `apps/worker`, Experiment Manifest và data model

## Validation Plan

- Thêm Strategy/Search Generator mới và xác nhận ba module không đổi.
- Thay RankingPolicy/Evaluator version và xác nhận Backtester result cũ vẫn dùng được.
- Replay manifest và so sánh Trades cùng bốn metrics theo QA-07.

## Evidence

**Status**: Planned — chưa thu thập do chưa có implementation.

- AP-01, AP-02 và AP-07 trong [Architecture Evidence](../architecture/architecture-evidence.md).

## Risks and Mitigations

- **Risk**: Contracts phình to — **Mitigation**: Chỉ truyền immutable artifact cần cho bước sau.
- **Risk**: Version mismatch — **Mitigation**: Resolve/version validate trước khi enqueue job.

## References

- [Đề bài Crypto Strategy Lab — §20, §21, §32.6 và §40](../Crypto%20Strategy%20Lab%20%E2%80%93%20%C4%90%E1%BB%93%20%C3%A1n%20cu%E1%BB%91i%20k%E1%BB%B3.pdf)
- [Slide kiến trúc — ADR Backtester/Evaluator](../KienTrucDoAn_slide.pdf)
- [Dynamic Views](../architecture/data-flows.md#3-strategy-backtest-và-evaluation)

## Supersession

- Supersedes: None
- Superseded by: None

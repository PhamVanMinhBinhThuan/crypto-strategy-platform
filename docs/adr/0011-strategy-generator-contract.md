# ADR-0011: Pluggable Strategy Generator cho Search

**Status**: Proposed
**Date**: 2026-08-12
**Owners**: Tiến Luật

## Context

MVP cần Random Search nhưng đề yêu cầu có thể thêm Domain-guided, Genetic hoặc thuật toán khác mà không sửa Backtester, Evaluator và Leaderboard. Nếu Search Coordinator hard-code Random generation, replaceability chỉ tồn tại trên tài liệu.

## Drivers and Quality Scenarios

- [QA-02 Replace Search Algorithm](../architecture/quality-attributes.md#qa-02--replace-search-algorithm)
- [QA-10 Start Large Search](../architecture/quality-attributes.md#qa-10--start-large-search)

## Decision

Module `search` định nghĩa contract khái niệm:

```java
public interface StrategyGenerator {
    GeneratorDescriptor descriptor();
    CandidateStrategy generate(SearchContext context, GenerationIndex index);
}
```

- `CandidateStrategy` chứa Strategy/Composite references và exact parameters, không chứa executable class hoặc result.
- Generator được resolve qua `SearchGeneratorRegistry` theo ID/version.
- MVP cung cấp `RandomStrategyGenerator` với seed/search space bất biến.
- Domain-guided/Genetic generators triển khai cùng contract; stop condition thuộc coordinator/policy riêng.
- Candidate Definition và generation index được persist trước khi enqueue Backtest.

## Alternatives Considered

- **Hard-code Random Search trong coordinator**: nhanh nhưng mỗi algorithm mới sửa orchestration.
- **Generator gọi Backtester**: tự đánh giá được candidate nhưng coupling generation với execution.
- **Một interface nhận/trả object tùy ý**: linh hoạt nhưng không đảm bảo reproducibility hoặc validation.

## Consequences

### Positive

- Thay thuật toán mà downstream pipeline không đổi.
- ID/version/seed giúp replay candidate generation.
- Registry/configuration tránh `if/else` theo algorithm name.

### Negative

- Cần descriptor, typed context và versioning.
- Time-based stop không tái sinh exact set nếu không lưu Candidate Definitions.

## Affected Components

- `modules/search`, `apps/worker`, `modules/contracts`
- Experiment Manifest và Candidate Definition persistence

## Validation Plan

- Thêm Domain-guided generator, chạy cùng coordinator và xác nhận Backtester/Evaluator/Leaderboard không đổi.
- Cùng seed/search space/version tạo cùng ordered Candidate Definitions.
- Start Search 1.000 candidates đáp ứng QA-10 và bounded in-flight jobs.

## Evidence

**Status**: Planned — chưa thu thập do chưa có implementation.

- AP-02 và AP-10 trong [Architecture Evidence](../architecture/architecture-evidence.md).

## Risks and Mitigations

- **Risk**: SearchContext trở thành shared dumping ground — **Mitigation**: Chỉ chứa immutable search space, registry metadata và generation state.
- **Risk**: Parallel generation làm mất determinism — **Mitigation**: Generation index/seed được cấp tập trung và persist trước execution.

## References

- [Đề bài Crypto Strategy Lab — §15–18, §32.6, §40 và §42](../Crypto%20Strategy%20Lab%20%E2%80%93%20%C4%90%E1%BB%93%20%C3%A1n%20cu%E1%BB%91i%20k%E1%BB%B3.pdf)
- [Slide kiến trúc — Replaceability Test](../KienTrucDoAn_slide.pdf)
- [ADR-0006](0006-queue-worker-backtesting.md)

## Supersession

- Supersedes: None
- Superseded by: None

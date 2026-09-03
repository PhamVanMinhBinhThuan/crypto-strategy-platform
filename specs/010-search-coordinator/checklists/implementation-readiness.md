# Implementation Readiness — F-010 Search Coordinator

**Ngày đánh giá**: 2026-09-03  
**Branch**: `010-search-coordinator`  
**Commit artifact nền**: `93eb912d551bac9d0aa5803dc839c06297568e92`

## Start gate

- [x] Atomic Start graph, exact replay, hash conflict và rollback có PostgreSQL integration evidence.
- [x] Search Request Outbox, stream mapping và consumer group `search-coordinators` có contract/unit evidence.
- [x] Finite Random Search tạo Candidate/Backtest Job, nhận authoritative Evaluation/Leaderboard và terminal.
- [x] Duplicate, stale, out-of-order, restart và queue-loss được kiểm chứng từ durable state.
- [x] Stop/allocation và completion/deadline race dùng database fence.
- [x] Public Start giữ owner identity, `202`, `Location`, idempotency conflict/replay contract.
- [x] Public error mapper chỉ trả mã/thông điệp ổn định; payload nội bộ không đi qua response.

**Quyết định**: đủ điều kiện mở riêng Start gate qua published
`StartSearchExperimentUseCase`. Evidence chi tiết nằm trong `quickstart.md`.

## Reproduce gate

- [x] Atomic reproduction graph và immutable Candidate copy có PostgreSQL rollback/replay evidence.
- [x] Durable async verification lifecycle có fence và duplicate/restart reconciliation evidence.
- [x] Exact Trade/metric/fingerprint comparator có end-to-end PostgreSQL evidence.

**Quyết định cập nhật**: đủ điều kiện mở riêng Reproduce gate qua published
`StartSearchReproductionUseCase`; việc mở gate không thay đổi source Experiment.

## Final architecture and safety review

- [x] ADR-0016 vẫn ở trạng thái Accepted và dependency direction khớp architecture tests.
- [x] Forward migration F-010 được giữ riêng, không sửa migration đã áp dụng.
- [x] Owner boundary của Start/Reproduce được kiểm tra trước durable mutation.
- [x] Public failure mapping và mismatch report không lộ secret, SQL, path, stack hay provider payload.
- [x] Start và Reproduce dùng hai release gate độc lập, chỉ mở theo evidence tương ứng.
- [x] Scope test khóa không có endpoint/dependency live trading, wallet hoặc financial advice.

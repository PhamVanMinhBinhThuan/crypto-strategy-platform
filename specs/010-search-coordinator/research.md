# Research: Search Coordinator

## R1 — Vị trí của generator và Coordinator

**Decision**: Generator contract/algorithm/state thuộc `modules/search`; runtime orchestration
thuộc `apps/worker` và chỉ gọi published ports của Search/Experiment.

**Rationale**: Dependency matrix hiện cho Search phụ thuộc Domain/Strategy, phù hợp generator
thuần. Coordinator cần Contracts, Experiment và Redis nhưng đó là composition concern của Worker.

**Alternatives considered**:

- Đặt Coordinator trong `modules/search`: loại vì buộc Search phụ thuộc Experiment/queue framework.
- Đặt generator trong Experiment: loại vì phá owner và quality scenario thay thuật toán Search.
- Coordinator trong API: loại vì request lifecycle không phù hợp durable work.

## R2 — Durable state và delivery

**Decision**: PostgreSQL giữ Search Run/generator state/decision sequence; Outbox giữ publication
intent; Redis Streams chỉ vận chuyển request/completion và có thể rebuild.

**Rationale**: Đáp ứng restart/queue-loss recovery và nguyên tắc durable truth. State transition
phải commit cùng allocation decision trước publication.

**Alternatives considered**:

- Chỉ lưu state trong Redis: mất state khi cache/queue mất.
- Rebuild generator state chỉ từ Candidate list: khả thi với Random đơn giản nhưng không đủ cho
  generator thay thế và khó chứng minh no-progress/version semantics.
- Exactly-once broker claim: không thực tế; dùng at-least-once + idempotent durable decision.

## R3 — Đơn vị atomic allocation

**Decision**: Một allocation transaction tạo Candidate, persist next generator state, tạo/reuse
logical Backtest Job và ghi Outbox. Unique constraints bảo vệ `(experiment, generation_index)`,
`(experiment, candidate_fingerprint)` và một Backtest Job mỗi Candidate.

**Rationale**: Đây là ranh giới nhỏ nhất loại partial graph và cho phép safe retry sau crash.

**Alternatives considered**:

- Tạo Candidate rồi Job ở hai transaction: có orphan Candidate và cần repair phức tạp.
- Generate cả search space upfront: tăng memory/DB, không hỗ trợ stop nhanh và bounded in-flight.

## R4 — Scheduling và concurrency

**Decision**: Fill-window scheduling với giới hạn `maxInFlightPerExperiment`; lock/fence Search Run
khi quyết định, không giữ transaction trong lúc Backtest chạy.

**Rationale**: Giữ throughput nhưng tránh unbounded queue. Short transaction và durable version
ngăn hai Coordinator cấp cùng generation index.

**Alternatives considered**:

- Mỗi lần chỉ một Candidate: đơn giản nhưng lãng phí Worker capacity.
- Sinh toàn bộ tới maximumCandidates: stop chậm và tạo queue burst.
- Redis distributed lock làm correctness boundary: loại vì Redis không phải source of truth.

## R5 — Completion consumption

**Decision**: Search Coordinator dùng consumer group riêng trên `candidate.evaluated.v1`; Ranking
Handler giữ group hiện có. Search handler ACK sau durable reconciliation, không dựa vào cache guard
làm correctness.

**Rationale**: Một Redis consumer group chia message giữa consumers, nên dùng chung ranking group
sẽ làm một capability bỏ lỡ event. Group riêng cho mỗi logical subscriber bảo toàn fan-out.

**Alternatives considered**:

- Gọi Search trực tiếp từ Ranking Handler: coupling hai owners và khó retry độc lập.
- Tạo stream completion mới: không cần thiết khi event hiện có đủ identity/version.

## R6 — Stop và terminal decision

**Decision**: `STOP_REQUESTED` chặn allocation mới ngay tại durable decision boundary. Existing
F-007 cancel/reconciliation đưa child Jobs terminal; Search reconciliation chỉ hoàn tất Search Job
và không tranh ownership transition đã có.

**Rationale**: Reuse behavior đã verified và tránh hai coordinator cùng quyết định Experiment
`STOPPED`.

**Alternatives considered**:

- Search tự cancel/update mọi Job trực tiếp: trùng F-007 và phá owner boundary.
- ACK stop transient trước DB transition: có thể tiếp tục sinh Candidate sau báo thành công.

## R7 — Reproduction mode

**Decision**: Reproduction không chạy generator để “tình cờ” sinh lại; nó copy ordered frozen
Candidate Definitions từ source trong atomic initialization, dispatch đúng sequence, rồi dùng
existing execution verification để so Trade/metrics/fingerprints.

**Rationale**: Reuse exact evidence mạnh hơn phụ thuộc vào implementation generator còn tồn tại.
Generator metadata vẫn được bảo toàn để audit.

**Alternatives considered**:

- Chạy lại generator version cũ: thất bại nếu artifact không còn hoặc implementation drift.
- Reuse Result cũ: không phải reproduction execution.

## R8 — Baseline Random Search determinism

**Decision**: Canonical sort parameter keys/options, exact discrete sampling, seeded pseudo-random
state có version và state snapshot sau mỗi accepted Candidate; duplicate draw tiếp tục hữu hạn đến
exhaustion/no-progress limit.

**Rationale**: Bảo đảm same input/state → same output, không phụ thuộc iteration order của map.

**Alternatives considered**:

- Runtime default randomness: không tái lập.
- Floating point sampling: không exact và dễ drift.
- Chỉ lưu seed: không đủ resume giữa sequence nếu thuật toán/state evolution thay đổi.

## R9 — Public readiness

**Decision**: Start/Reproduce endpoint giữ gate cho tới khi API wiring, Worker consumer,
PostgreSQL transaction/recovery và Redis contract evidence pass; sau đó dùng published command port
và giữ nguyên F-009 schemas/status codes.

**Rationale**: Tránh endpoint trông hoạt động nhưng không có runtime consumer.

**Alternatives considered**:

- Gỡ gate ngay khi domain generator xong: tạo queued work không bao giờ chạy.
- API publish thẳng Redis: bỏ qua atomic Outbox/idempotency và durable recovery.

## R10 — ADR requirement

**Decision**: Tạo ADR-0016 cho Search ownership, durable state, event groups và coordination
boundary; phải Accepted trước implementation phụ thuộc.

**Rationale**: Quyết định dài hạn, xuyên module, thêm data ownership và operational contract nên
Constitution yêu cầu ADR.

**Alternatives considered**: Chỉ ghi trong plan bị loại vì thiếu governance/lịch sử quyết định.

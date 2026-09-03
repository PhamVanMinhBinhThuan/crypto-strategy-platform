# Feature Specification: Search Coordinator

**Feature Branch**: `010-search-coordinator`

**Created**: 2026-09-02

**Status**: Draft

**Input**: User description: "Triển khai Search Coordinator để mở khóa Start Experiment và Reproduce Experiment sau F-009."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Khởi chạy tìm kiếm chiến lược (Priority: P1)

Là người dùng đã đăng nhập, tôi muốn khởi chạy một Experiment từ Dataset bất biến,
Strategy search space, generator có version và điều kiện dừng để hệ thống tự tạo, đánh giá
và xếp hạng Candidate mà không cần tôi điều phối từng Backtest.

**Why this priority**: Đây là capability còn thiếu khiến Start Experiment của F-009 chưa thể
hoạt động và là luồng giá trị chính của nền tảng.

**Independent Test**: Khởi chạy Experiment với seed cố định và giới hạn nhỏ; xác nhận request
được chấp nhận nhanh, Candidate/Job được tạo theo thứ tự xác định, tiến độ quan sát được và
Experiment kết thúc với Leaderboard authoritative.

**Acceptance Scenarios**:

1. **Given** Dataset, Strategy version và search space hợp lệ thuộc phạm vi người dùng,
   **When** người dùng khởi chạy Experiment với idempotency key mới,
   **Then** hệ thống chấp nhận đúng một Experiment, đóng băng input và bắt đầu điều phối.
2. **Given** cùng owner, key và canonical request,
   **When** command được gửi lại 100 lần,
   **Then** mọi lần trả cùng logical outcome và không tạo Experiment, Candidate hay Job trùng.
3. **Given** cùng seed, generator version, search space và prior state,
   **When** generator chạy lại,
   **Then** ordered Candidate Definitions, generation indices và fingerprints giống nhau.
4. **Given** Candidate hoàn tất đánh giá,
   **When** Coordinator nhận kết quả hợp lệ,
   **Then** tiến độ tăng đúng một lần và công việc kế tiếp được điều phối tới điều kiện dừng.

---

### User Story 2 - Dừng và phục hồi an toàn (Priority: P1)

Là người dùng, tôi muốn dừng Experiment đang chạy và vẫn giữ kết quả đã hoàn tất; là người
vận hành, tôi muốn Coordinator phục hồi sau restart hoặc duplicate delivery mà không tạo
outcome trùng hay bỏ quên công việc.

**Why this priority**: Search là tác vụ dài và tốn tài nguyên; stop/recovery sai làm người dùng
mất quyền kiểm soát và phá bằng chứng tái lập.

**Independent Test**: Dừng Experiment giữa chừng, restart Coordinator và phát lại notification;
xác nhận không sinh Candidate mới, active Jobs kết thúc phù hợp và snapshot vẫn chính xác.

**Acceptance Scenarios**:

1. **Given** Experiment đang chạy, **When** owner yêu cầu dừng, **Then** hệ thống không dispatch
   Candidate mới, kết thúc active work và giữ mọi outcome đã chấp nhận.
2. **Given** notification lặp, muộn hoặc sai thứ tự, **When** Coordinator xử lý, **Then** progress
   và business outcome không cộng lặp hoặc quay lùi.
3. **Given** process dừng giữa durable transition và publication, **When** hoạt động lại,
   **Then** Coordinator tiếp tục từ durable state và hoàn tất intent còn thiếu.
4. **Given** dependency lỗi tạm thời, **When** retry budget còn, **Then** công việc retry hữu hạn;
   khi cạn budget, Experiment nhận failure có cấu trúc thay vì treo vô hạn.

---

### User Story 3 - Tái tạo Experiment (Priority: P2)

Là người dùng, tôi muốn reproduce một Experiment đã hoàn tất để kiểm chứng cùng frozen inputs
và Candidate sequence tạo cùng kết quả có ý nghĩa, trong khi run gốc không bị sửa.

**Why this priority**: Reproduction là cam kết cốt lõi về provenance và mở khóa endpoint còn
được F-009 bảo vệ bằng readiness gate.

**Independent Test**: Reproduce một Experiment hữu hạn đã hoàn tất; xác nhận run mới liên kết
run gốc, reuse manifest/Candidates và công bố match hoặc mismatch có thể truy vết.

**Acceptance Scenarios**:

1. **Given** Experiment đã hoàn tất và đủ provenance, **When** owner yêu cầu reproduction,
   **Then** hệ thống tạo run mới liên kết nguồn và không sửa dữ liệu nguồn.
2. **Given** reproduction hoàn tất, **When** đối chiếu, **Then** hệ thống ghi match/mismatch cho
   Trade sequence, metrics và fingerprints.
3. **Given** nguồn không thuộc owner, chưa terminal hoặc thiếu evidence, **When** reproduce,
   **Then** command bị từ chối an toàn và không tạo partial graph.

---

### User Story 4 - Thay generator không đổi pipeline (Priority: P3)

Là người phát triển nền tảng, tôi muốn đăng ký generator khác theo cùng contract để thay thuật
toán tìm kiếm mà không đổi Backtest, Evaluation, Leaderboard hoặc public workflow.

**Why this priority**: Đây là quality driver về replaceability; MVP cần generator cơ sở
deterministic nhưng boundary phải chứng minh được khả năng thay thế.

**Independent Test**: Chạy cùng fixture bằng generator cơ sở và generator fixture; xác nhận
pipeline downstream và trạng thái public không đổi contract.

**Acceptance Scenarios**:

1. **Given** generator identity/version hợp lệ, **When** được chọn, **Then** Coordinator lưu đủ
   identity, version, seed và state để tái lập.
2. **Given** generator/version không tồn tại, **When** start, **Then** request bị từ chối trước
   khi tạo durable work.

### Edge Cases

- Search space rỗng, range đảo, option trùng hoặc tổ hợp vượt giới hạn an toàn.
- Điều kiện dừng bằng 0, vượt giới hạn, hoặc nhiều điều kiện đạt đồng thời.
- Generator trả Candidate trùng, index lặp, state không tiến triển hoặc output ngoài search space.
- Completion đến trước confirmation dispatch, sau stop, hoặc tham chiếu sai Experiment/Job/owner.
- Experiment đạt giới hạn trong lúc nhiều completion đến đồng thời.
- Queue/cache mất hoàn toàn nhưng durable Experiment, Candidate, Job và Outbox còn.
- Reproduction dùng generator version không còn hỗ trợ hoặc provenance đã hết retention.
- Candidate thất bại vĩnh viễn không được khiến Experiment treo.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Owner MUST có thể khởi chạy Experiment từ frozen Dataset và một Strategy version, search space,
  generator identity/version/seed, stop conditions và Top-K hợp lệ.
- **FR-001a**: MVP chỉ hỗ trợ search trên một frozen Strategy version; Composite Search nằm ngoài
  phạm vi F-010 và MUST bị từ chối bằng validation ổn định thay vì được xử lý ngầm như Strategy.
- **FR-002**: Hệ thống MUST xác thực ownership và mọi private input trước khi tạo durable work.
- **FR-003**: Start/Reproduce MUST idempotent theo owner, operation và canonical request; replay
  giống nhau trả cùng outcome, payload khác cùng key trả conflict.
- **FR-004**: Hệ thống MUST đóng băng Manifest đầy đủ, gồm provenance, versions, assumptions,
  generator seed/configuration và stop conditions, trước khi bắt đầu search.
- **FR-005**: MVP MUST có ít nhất một generator cơ sở deterministic có version.
- **FR-006**: Cùng generator version, seed, frozen search space và prior state MUST tạo cùng
  Candidate Definition, generation index, fingerprint và next state.
- **FR-007**: Hệ thống MUST từ chối Candidate trùng, index không đơn điệu, state không tiến triển
  và output ngoài search space trước dispatch.
- **FR-008**: Mỗi Candidate MUST thuộc đúng một Experiment và tối đa một logical Backtest Job;
  retry tạo attempt mới, không tạo business identity trùng.
- **FR-009**: Coordinator MUST giữ số active work trong giới hạn cấu hình và không sinh vô hạn
  chỉ để lấp đầy hàng đợi.
- **FR-010**: Progress MUST dựa trên durable outcomes; duplicate, stale và out-of-order event
  không được cộng lặp hoặc làm progress quay lùi.
- **FR-011**: Stop conditions MUST được đánh giá sau mỗi thay đổi liên quan và Experiment MUST
  vào terminal state đúng một lần. `deadlineAt` MUST được tính bằng injected UTC clock đúng một lần
  tại first start, không được kéo dài khi restart/retry; completion/deadline race MUST lock hoặc
  reload durable state để cho một deterministic terminal decision. Khi completion và deadline cùng
  hợp lệ, hệ thống MUST reconcile outcome có authoritative `completedAt <= deadlineAt` trước khi
  đánh giá deadline; tại đúng `completedAt == deadlineAt`, completion thắng. Outcome hoàn tất sau
  `deadlineAt` vẫn được giữ nhưng deadline thắng quyết định chặn allocation và đưa run sang stopping.
- **FR-012**: Sau stop, Coordinator MUST không dispatch Candidate mới và MUST chờ mọi active Job
  terminal trước khi Experiment thành `STOPPED`.
- **FR-013**: Coordinator MUST phục hồi được chỉ từ durable state khi process, queue hoặc cache mất.
- **FR-014**: Dispatch/publication MUST duplicate-safe, retry hữu hạn và ghi failure có cấu trúc.
- **FR-015**: Progress/lifecycle MUST có contract version và correlation identifiers đủ cho
  F-009 phân phối realtime mà không truy cập dữ liệu nội bộ.
- **FR-016**: Reproduction MUST tạo Experiment mới có lineage, giữ nguồn bất biến và reuse chính
  xác frozen Manifest cùng Candidate sequence.
- **FR-017**: Reproduction chỉ nhận source đúng owner, terminal và đủ evidence; lỗi trước commit
  MUST không để partial Experiment/Candidate/Job graph.
- **FR-018**: Reproduction MUST được chấp nhận bất đồng bộ, lưu durable verification lifecycle
  `PENDING` trước khi trả response, và chỉ chạy so sánh Trade sequence, metrics/fingerprints sau khi
  reproduction Experiment terminal; outcome `MATCHED`, `MISMATCHED` hoặc `FAILED` cùng differences
  an toàn MUST idempotent và phục hồi được sau restart.
- **FR-019**: Generator MUST có contract chung với identity/version; generator conforming mới
  MUST không yêu cầu đổi Backtest, Evaluation, Leaderboard hoặc public workflow.
- **FR-020**: Generator/version không tồn tại hoặc không tương thích MUST bị từ chối trước khi
  tạo durable work.
- **FR-021**: Mutation MUST đi qua application boundary của capability sở hữu; Coordinator MUST
  không ghi trực tiếp dữ liệu của capability khác.
- **FR-022**: Gate Start của F-009 chỉ được gỡ sau US1 happy path/ownership/idempotency và US2
  stop/recovery evidence pass. Gate Reproduce được gỡ độc lập, chỉ sau các điều kiện Start chung và
  US3 immutable-source/async-verification evidence pass.
- **FR-023**: Feature MUST không đặt lệnh tiền thật, quản lý ví hoặc hứa hẹn lợi nhuận.

### Key Entities

- **Search Run**: Trạng thái điều phối của Experiment, frozen generator, current state, progress
  và stop-decision evidence.
- **Generator Definition**: Identity/version và khả năng sinh Candidate deterministic từ frozen
  search space, seed và prior state.
- **Candidate Definition**: Cấu hình bất biến tại generation index, có fingerprint và Experiment.
- **Stop Conditions**: Giới hạn hữu hạn, tối thiểu gồm maximum Candidates và maximum duration.
- **Dispatch Decision**: Bằng chứng Candidate đã/chưa có logical Backtest Job để chống duplicate.
- **Reproduction Verification**: Lineage, match/mismatch, differences và fingerprints đối chiếu.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 95% Start Experiment hợp lệ trả durable identity trong dưới 2 giây, đo từ lúc API nhận
  command đến khi atomic commit hoàn tất và response chứa identity sẵn sàng; acceptance benchmark
  dùng ít nhất 100 request sau warm-up và không tính thời gian khởi tạo container hoặc migration.
- **SC-002**: Replay cùng Start/Reproduce command 100 lần tạo đúng một logical Experiment và
  không tạo Candidate/Job business trùng.
- **SC-003**: 100 lượt generator cùng frozen inputs/seed tạo cùng ordered fingerprints/indices.
- **SC-004**: Restart tại mọi dispatch/publication boundary vẫn đưa 100% fixture hữu hạn tới đúng
  terminal state mà không mất Candidate đã chấp nhận.
- **SC-005**: Duplicate/stale/out-of-order completion không làm progress giảm hoặc vượt durable count.
- **SC-006**: Sau stop hoặc frozen deadline, không Candidate mới được dispatch; active Jobs terminal
  và Experiment đạt đúng terminal state trong configured cancellation grace, kể cả qua restart và
  completion/deadline race.
- **SC-007**: Reproduction fixture có cùng Trade sequence; cùng bốn metric F-006 gồm Total Return,
  Win Rate, Maximum Drawdown và Number of Trades theo exact canonical value của frozen metric version;
  cùng fingerprints, hoặc trả mismatch report chính xác thay vì báo thành công sai.
- **SC-008**: Generator fixture thay thế chạy end-to-end mà không đổi downstream/public contract.
- **SC-009**: Mất queue/cache không làm mất durable Experiment, Candidate, Job, Result hay intent.
- **SC-010**: 100% public error/progress/lifecycle failure mappings được kiểm tra tại API boundary và
  không lộ secret, provider payload, SQL, path, stack hoặc internal exception detail.

## Assumptions

- F-003/F-004 cung cấp Dataset và Strategy version bất biến; Composite Search được hoãn khỏi MVP.
- F-005 sở hữu Experiment, Manifest, Candidate, Job, Attempt, Outbox và idempotency.
- F-006 sở hữu Backtest/Evaluation/Leaderboard; F-007 sở hữu reliable Backtest Worker, Ranking
  Handler và stop-completion reconciliation; F-009 sở hữu public transport.
- Baseline MVP là Random Search deterministic theo seed; adaptive/Bayesian search và distributed
  multi-leader Coordinator ngoài phạm vi.
- Candidate failure vĩnh viễn được ghi vào progress và search có thể tiếp tục; lỗi integrity,
  provenance hoặc invariant làm fail toàn Experiment.
- Durable storage là source of truth; queue/cache dùng cho delivery và có thể rebuild.
- Duration dùng UTC instant/elapsed duration rõ ràng; numeric Candidate parameter giữ exact semantics.

# Danh sách công việc: Database Baseline

**Đầu vào**: Các tài liệu thiết kế trong `/specs/001-database-baseline/`

**Điều kiện tiên quyết**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/database-verification.md`, `quickstart.md`

**Kiểm thử**: Specification bắt buộc có database verification. Test phải được viết trước bước rà soát migration tương ứng và phải phát hiện được constraint hoặc permission còn thiếu.

**Cách tổ chức**: Công việc được nhóm theo user story. Apply migration từ xa vẫn là bước cần người dùng phê duyệt riêng.

## Định dạng: `[ID] [P?] [Story] Mô tả`

- **[P]**: Có thể làm song song vì thay đổi file khác nhau và không phụ thuộc task chưa hoàn thành.
- **[Story]**: Ánh xạ tới user story trong `spec.md`.

## Giai đoạn 1: Chuẩn bị hạ tầng dùng chung

**Mục tiêu**: Chuẩn bị cấu trúc kiểm chứng database hosted có version mà chưa thay đổi remote.

- [ ] T001 Tạo thư mục database test và khung SQL verification tại `supabase/tests/database/001_database_baseline_test.sql`
- [ ] T002 [P] Tạo biểu mẫu ghi bằng chứng không chứa secret và trạng thái phê duyệt tại `specs/001-database-baseline/verification-evidence.md`
- [ ] T003 Chạy preview không thay đổi remote bằng `supabase db push --dry-run` và ghi kết quả vào `specs/001-database-baseline/verification-evidence.md`

---

## Giai đoạn 2: Nền tảng kiểm thử bắt buộc

**Mục tiêu**: Tạo helper catalog và fixture dùng chung cho mọi user story.

**⚠️ BẮT BUỘC**: Chưa user story nào hoàn thành nếu test chưa chạy trong transaction và chưa bảo đảm không để lại fixture.

- [ ] T004 Thêm transaction, assertion/error-capture helper, fixture ULID xác định trước và rollback cuối cùng vào `supabase/tests/database/001_database_baseline_test.sql`
- [ ] T005 Thêm catalog assertion cho 5 schema, 24 bảng, foreign key bắt buộc và index đã chốt vào `supabase/tests/database/001_database_baseline_test.sql`
- [ ] T006 Thêm catalog assertion cho kiểu ULID, decimal precision/scale chính xác, timestamp `timestamptz` và 8 timeframe canonical vào `supabase/tests/database/001_database_baseline_test.sql`

**Điểm kiểm tra**: Bộ verification có thể kiểm tra database đã migrate một cách an toàn.

---

## Giai đoạn 3: User Story 1 - Khởi tạo nền tảng dữ liệu nhất quán (P1) 🎯 MVP

**Mục tiêu**: Chứng minh baseline có version tạo đúng schema ownership, quan hệ, constraint, index và client-role permission mà không sửa schema thủ công.

**Kiểm thử độc lập**: Apply migration lên database development trống, chạy SQL verification và linked lint, rồi xác nhận migration version trong repository khớp remote history.

### Kiểm thử User Story 1

- [ ] T007 [US1] Thêm test từ chối ULID, enum, timeframe, numeric range và timestamp/range không hợp lệ vào `supabase/tests/database/001_database_baseline_test.sql`
- [ ] T008 [US1] Thêm assertion xác nhận `anon` và `authenticated` không có schema/table privilege trực tiếp vào `supabase/tests/database/001_database_baseline_test.sql`

### Triển khai User Story 1

- [ ] T009 [US1] Rà soát và sửa baseline DDL theo assertion US1 trong `supabase/migrations/20260827000100_create_database_baseline.sql`
- [ ] T010 [US1] Chạy lại dry-run không thay đổi remote và cập nhật bằng chứng review migration trong `specs/001-database-baseline/verification-evidence.md`

**Điểm kiểm tra**: Nội dung migration và bằng chứng không thay đổi remote đạt US1; feature vẫn chưa được deploy.

---

## Giai đoạn 4: User Story 2 - Tạo nền tảng ownership cho Experiment (P1)

**Mục tiêu**: Chứng minh mỗi Experiment cần Supabase Auth owner hợp lệ, dữ liệu con có đúng một đường ownership và idempotency được scope theo user.

**Kiểm thử độc lập**: Tạo hai Auth identity tạm cùng fixture; từ chối Experiment thiếu owner, truy Candidate/Result/Trade về đúng owner và chấp nhận cùng command key cho hai user.

### Kiểm thử User Story 2

- [ ] T011 [US2] Thêm assertion cho Auth user foreign key, Experiment owner và đường ownership của dữ liệu con vào `supabase/tests/database/001_database_baseline_test.sql`
- [ ] T012 [US2] Thêm assertion cho idempotency uniqueness khác user và khác scope vào `supabase/tests/database/001_database_baseline_test.sql`

### Triển khai User Story 2

- [ ] T013 [US2] Rà soát và sửa constraint ownership/idempotency theo assertion US2 trong `supabase/migrations/20260827000100_create_database_baseline.sql`

**Điểm kiểm tra**: Nền tảng ownership ở database được kiểm chứng độc lập; login và authorization của application vẫn ngoài phạm vi.

---

## Giai đoạn 5: User Story 3 - Tái lập kết quả Experiment (P1)

**Mục tiêu**: Chứng minh Result truy được Dataset, Strategy, manifest, Candidate, Attempt, Trade và Evaluation có version mà không ghi đè lần chạy gốc.

**Kiểm thử độc lập**: Chèn provenance graph đầy đủ và một reproduction Experiment có cùng manifest fingerprint nhưng result riêng tham chiếu về bản gốc.

### Kiểm thử User Story 3

- [ ] T014 [US3] Thêm assertion cho Dataset membership/checksum và identity của Strategy/Composite snapshot vào `supabase/tests/database/001_database_baseline_test.sql`
- [ ] T015 [US3] Thêm assertion truy toàn bộ provenance và reproduction dùng cùng manifest vào `supabase/tests/database/001_database_baseline_test.sql`
- [ ] T016 [US3] Thêm assertion uniqueness cho Candidate, Result, Trade, Evaluation và Leaderboard vào `supabase/tests/database/001_database_baseline_test.sql`

### Triển khai User Story 3

- [ ] T017 [US3] Rà soát và sửa quan hệ/constraint reproducibility theo assertion US3 trong `supabase/migrations/20260827000100_create_database_baseline.sql`

**Điểm kiểm tra**: Provenance của frozen input và reproduction identity được kiểm chứng độc lập ở database level.

---

## Giai đoạn 6: User Story 4 - Chống trùng và phục hồi xử lý nền (P2)

**Mục tiêu**: Chứng minh delivery trùng từ provider/queue không nhân đôi core identity và recovery record tồn tại độc lập với hạ tầng transient.

**Kiểm thử độc lập**: Thử chèn trùng Candle, membership, sentiment và processed message; sau đó truy Outbox chưa publish cùng recovery record chưa hết hạn mà không dựa vào Redis/queue.

### Kiểm thử User Story 4

- [ ] T018 [US4] Thêm assertion chống trùng Candle, Dataset membership, News source và Sentiment input vào `supabase/tests/database/001_database_baseline_test.sql`
- [ ] T019 [US4] Thêm assertion cho Outbox message, processed message và expiry/recovery index vào `supabase/tests/database/001_database_baseline_test.sql`

### Triển khai User Story 4

- [ ] T020 [US4] Rà soát và sửa constraint/index chống trùng và phục hồi theo assertion US4 trong `supabase/migrations/20260827000100_create_database_baseline.sql`

**Điểm kiểm tra**: Duplicate protection và durable recovery state ở database được kiểm chứng độc lập.

---

## Giai đoạn 7: Kiểm chứng trên Shared Development

**Mục tiêu**: Hoàn thành FR-020 sau khi mọi bước review đạt và người dùng phê duyệt rõ ràng việc thay đổi remote.

- [ ] T021 Xin phê duyệt rõ ràng ngay trước khi apply migration và ghi approval gate vào `specs/001-database-baseline/verification-evidence.md`
- [ ] T022 Apply migration bằng `supabase db push` và ghi kết quả không chứa secret vào `specs/001-database-baseline/verification-evidence.md`
- [ ] T023 Chạy `supabase migration list` và `supabase db lint --linked --fail-on error`, rồi ghi kết quả vào `specs/001-database-baseline/verification-evidence.md`
- [ ] T024 Chạy `supabase/tests/database/001_database_baseline_test.sql` trên shared development bằng `psql` và `ON_ERROR_STOP`, rồi ghi kết quả rollback-safe vào `specs/001-database-baseline/verification-evidence.md`

**Điểm kiểm tra**: Feature chỉ hoàn thành khi T021–T024 đều đạt. Nếu chưa được duyệt, dừng sau T020 và không thay đổi remote.

---

## Giai đoạn 8: Rà soát chéo và hoàn thiện

**Mục tiêu**: Giữ implementation, bằng chứng và tài liệu đồng nhất mà không âm thầm thay đổi quyết định kiến trúc.

- [ ] T025 Đối chiếu DDL và verification coverage đã triển khai với `specs/001-database-baseline/spec.md`, `specs/001-database-baseline/data-model.md` và `specs/001-database-baseline/contracts/database-verification.md`
- [ ] T026 Xác nhận không ADR hiện có nào bị sửa hoặc supersede ngầm; ghi mọi thay đổi ADR bắt buộc thành blocker trong `specs/001-database-baseline/verification-evidence.md`
- [ ] T027 Chạy mọi command phù hợp môi trường đã duyệt trong `specs/001-database-baseline/quickstart.md` và hoàn thiện `specs/001-database-baseline/verification-evidence.md`
- [ ] T028 Quét repository để xác nhận không có database credential đặc quyền trong source hoặc artifact và ghi kết quả vào `specs/001-database-baseline/verification-evidence.md`

---

## Quan hệ phụ thuộc và thứ tự thực hiện

- Giai đoạn 1 không phụ thuộc và không thay đổi remote.
- Giai đoạn 2 phụ thuộc Giai đoạn 1 và chặn toàn bộ user story.
- US1–US4 phụ thuộc Giai đoạn 2. Thực hiện tuần tự vì dùng chung một migration và một SQL test file.
- Giai đoạn 7 phụ thuộc cả bốn user story và approval tại T021.
- Giai đoạn 8 hoàn thiện việc đối chiếu; feature chỉ hoàn thành sau Giai đoạn 7.

### Cơ hội làm song song an toàn

- T001 và T002 có thể làm song song.
- Có thể review T013, T017 và T020 song song, nhưng phải tuần tự hóa mọi edit trên migration chung.
- T025 và T026 có thể review song song khi implementation đã ổn định.
- T021–T024 bắt buộc tuần tự và không được song song hóa.

## Chiến lược triển khai

1. Tạo verification harness có rollback.
2. Thêm và đáp ứng assertion US1 về schema/security.
3. Thêm và đáp ứng assertion US2 về ownership.
4. Thêm và đáp ứng assertion US3 về reproducibility.
5. Thêm và đáp ứng assertion US4 về duplicate/recovery.
6. Xin phê duyệt rõ ràng, sau đó mới apply và verify một lần trên shared development.

## Lưu ý

- Không sửa ADR đã Accepted để khớp implementation. Nếu có xung đột thật, dừng và báo rõ cần ADR Extends hay Supersedes.
- Không chạy `supabase db push` thiếu `--dry-run` trước khi T021 được phê duyệt.
- Không commit database credential hoặc đưa secret vào bằng chứng.
- Sau khi T022 thành công, không sửa migration đã apply; mọi correction phải dùng forward migration mới.

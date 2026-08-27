# Database Baseline Verification Contract

## Purpose

Verification suite là acceptance contract cho migration baseline. Suite MUST trả exit code khác 0 ngay khi một assertion thất bại và MUST không để lại fixture data.

## Preconditions

- Supabase project đã link đúng shared development project.
- Migration đã được review và chỉ được apply sau explicit approval.
- Database connection secret chỉ tồn tại trong credential store/environment, không nằm trong repository hoặc command output.
- SQL suite chạy với role đủ quyền tạo fixture và đọc PostgreSQL catalog.

## Required assertions

| ID | Assertion | Maps to |
|---|---|---|
| V-01 | Có đúng 5 business schema và đủ 24 baseline tables. | FR-001, FR-002, SC-001 |
| V-02 | ULID sai format bị từ chối; Auth user UUID FK được enforce. | FR-003, FR-013, SC-003 |
| V-03 | Candle identity và Dataset membership duplicates bị từ chối. | FR-004, FR-006, SC-002 |
| V-04 | Chỉ 8 timeframe canonical được chấp nhận. | FR-005 |
| V-05 | Strategy/composite/candidate/attempt/trade/evaluation/leaderboard duplicate identities bị từ chối. | FR-007–FR-010, SC-002 |
| V-06 | Manifest fingerprint giống nhau được phép ở hai Experiment khác nhau. | FR-008, SC-005 |
| V-07 | News source identity và sentiment model/input identity được deduplicate; confidence/polarity range được enforce. | FR-011, FR-012, SC-002 |
| V-08 | Idempotency key được scope theo user và command scope. | FR-015, SC-002 |
| V-09 | Outbox/processed-message identity và expiry metadata tồn tại, hỗ trợ durable recovery. | FR-016, FR-017, SC-006 |
| V-10 | Decimal columns có precision/scale đã chốt và critical query indexes tồn tại. | FR-018 |
| V-11 | `anon` và `authenticated` không có direct privilege trên 5 schema/business tables. | FR-014, SC-007 |
| V-12 | Migration xuất hiện đúng một lần trong remote migration history và linked lint không có error. | FR-020, SC-008 |

## Execution rules

1. Trước remote mutation, dry-run MUST thành công và output MUST được review.
2. Remote apply là bước riêng, cần explicit approval.
3. Fixture assertions MUST chạy giữa `BEGIN` và `ROLLBACK`.
4. Expected constraint failures MUST được bắt và xác nhận đúng constraint; lỗi ngoài dự kiến làm suite fail.
5. `supabase db lint --linked --fail-on error` MUST thành công sau apply.
6. Verification evidence MUST ghi project ref, migration version, git commit, thời gian và kết quả; MUST không ghi secret.

## Out of scope

- Login/UI/API authorization flow.
- Persistence-layer multi-table invariants và lifecycle implementation.
- Scheduled retention cleanup.
- Runtime worker recovery behavior; baseline chỉ chứng minh durable records tồn tại.

# Phase 0 Research: Database Baseline

## R-01 — Migration authority

**Decision**: Dùng Supabase CLI và `supabase/migrations` làm lịch sử migration duy nhất.

**Rationale**: `supabase db push` ghi migration đã chạy vào `supabase_migrations.schema_migrations` và bỏ qua chúng ở lần push sau. Cách này phù hợp DB-11 và tránh hai công cụ cùng quản lý schema.

**Alternatives considered**: Flyway/Liquibase bị loại vì tạo migration authority thứ hai; chạy SQL thủ công trong Dashboard bị loại vì khó review và tái lập.

Nguồn: [Supabase CLI reference](https://supabase.com/docs/reference/cli/supabase-start#supabase-db-push).

## R-02 — Hosted-only verification

**Decision**: Trước apply, chạy `supabase db push --dry-run`. Sau approval và apply lên shared development, chạy linked lint và SQL assertions trực tiếp trên database dev.

**Rationale**: Người dùng đã chọn không vận hành Docker Compose/local Supabase stack. CLI hỗ trợ dry-run và lint database đã link. Constraint tests chạy trên schema đã apply thật nhưng được bọc trong `BEGIN`/`ROLLBACK` để không giữ fixture.

**Alternatives considered**: `supabase start` + `supabase test db` cần Docker; chỉ review SQL không đủ bằng chứng cho FR-020 và success criteria.

Nguồn: [Supabase CLI reference](https://supabase.com/docs/reference/cli/supabase-start), [Supabase database testing](https://supabase.com/docs/guides/database/testing).

## R-03 — Test format

**Decision**: Lưu SQL verification suite có thứ tự tại `supabase/tests/database/001_database_baseline_test.sql`; chạy bằng `psql` với `ON_ERROR_STOP`, trong transaction rollback.

**Rationale**: SQL assertions kiểm tra được catalog, constraint và privilege mà không cần application code. Tách execution khỏi `supabase test db` cho phép hosted-only workflow; cấu trúc file vẫn phù hợp nếu dự án thêm pgTAP/local CI sau này.

**Alternatives considered**: pgTAP qua `supabase test db` hiện nhắm local database; test qua Java repository vượt phạm vi baseline.

## R-04 — User identity and client access

**Decision**: Dùng UUID từ `auth.users` làm identity gốc; `platform.user_profile` chỉ mở rộng profile; business schema không cấp direct privilege cho `anon` và `authenticated`.

**Rationale**: Phù hợp ADR-0011 và mô hình API/worker sở hữu transaction. Foreign key đảm bảo owner tồn tại nhưng không đồng nghĩa client được quyền truy cập bảng.

**Alternatives considered**: User table riêng trùng identity authority; cho frontend truy cập business table bằng RLS ngay làm thay đổi boundary và chưa thuộc feature.

## R-05 — Business invariants

**Decision**: Database chịu trách nhiệm cho type, nullability, check, unique và foreign key. Invariant cần đọc nhiều bảng hoặc lifecycle immutability sẽ do persistence service sau này kiểm tra trong cùng transaction; baseline không dùng trigger.

**Rationale**: Đây là câu trả lời clarify đã chốt và phù hợp FR-019/FR-021.

**Alternatives considered**: Trigger cho mọi invariant khó quan sát, test và version cùng application; không ghi nhận invariant có thể tạo result/leaderboard sai quan hệ.

## R-06 — Retention and cleanup

**Decision**: Baseline chỉ cung cấp timestamp và expiry indexes. Scheduled deletion, legal hold và provider-specific cleanup được để cho maintenance feature.

**Rationale**: Chưa có scheduler/runtime trong scope và không được xóa dữ liệu tái lập còn tham chiếu.

**Alternatives considered**: Database cron/trigger cleanup vượt phạm vi; không có expiry metadata/index làm maintenance sau này phải đổi quan hệ lõi.

## R-07 — Applied migration policy

**Decision**: Có thể sửa baseline trước lần apply đầu tiên. Sau khi migration xuất hiện trong remote history, mọi sửa đổi phải là forward migration mới.

**Rationale**: Bảo toàn lịch sử có thể tái lập và tuân thủ constitution.

**Alternatives considered**: Sửa migration đã apply tạo khác biệt giữa môi trường; rollback phá hủy mặc định không cần thiết và có rủi ro dữ liệu.

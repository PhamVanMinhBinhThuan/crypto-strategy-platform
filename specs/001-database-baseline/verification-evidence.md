# Bằng chứng kiểm chứng Database Baseline

## Phạm vi

- Feature: `001-database-baseline`
- Git branch: `feature/database-setup`
- Supabase project ref: `aeuezvsdsggrrvpaxfqf`
- Environment: shared development
- Migration: `20260827000100_create_database_baseline.sql`
- Remote apply: **ĐÃ ĐƯỢC PHÊ DUYỆT / ĐÃ ÁP DỤNG THÀNH CÔNG**
- Verification artifact commit: `b55d3031325248a24dd251e9d818bd23ff75adc2`
- Non-secret configuration: Supabase hosted PostgreSQL 17, linked project nêu trên,
  migration version nêu trên và transactional SQL verification có rollback

Tài liệu này chỉ lưu thông tin không bí mật. Không ghi database password, access token
hoặc connection string.

## Kết quả

| Bước | Thời gian (Asia/Ho_Chi_Minh) | Git commit | Trạng thái | Bằng chứng không bí mật |
|---|---|---|---|---|
| Static review | 2026-08-27 13:26:54 +07 | `b55d303` | VERIFIED | Migration và SQL test đã review: 5 schema, 24 bảng, 14 khai báo unique, không có trigger; test có `BEGIN`/`ROLLBACK` |
| `supabase db push --dry-run` | 2026-08-27 13:25 +07 | `b392e6e` | VERIFIED | Chỉ migration `20260827000100_create_database_baseline.sql` sẽ được push; `dryRun=true` |
| Approval gate | 2026-08-27 | `b55d303` | APPROVED | Người dùng phê duyệt rõ ràng ngay trước remote apply; record được commit trong verification artifact |
| `supabase db push` | 2026-08-27 | `b392e6e` | VERIFIED | Applied `20260827000100_create_database_baseline.sql`; migration content đã nằm trong commit, `dryRun=false` |
| `supabase migration list` | 2026-08-27 | `b392e6e` | VERIFIED | Local và remote cùng version `20260827000100` |
| `supabase db lint --linked --fail-on error` | 2026-08-27 | `b392e6e` | VERIFIED | Không có schema error trong business schema |
| SQL verification | 2026-08-27 | `b55d303` | VERIFIED | `database baseline verification passed`; test được commit và chạy bằng linked query trong transaction rollback |
| Repository secret scan | 2026-08-27 13:27 +07 | `b392e6e` | VERIFIED | Không tìm thấy pattern connection URL có password, Supabase access token hoặc service-role assignment có giá trị |

## Impact review sau Constitution v1.1.0

- F-001 đã implementation, apply và verification ngày 2026-08-27 trước amendment
  Constitution v1.1.0 ngày 2026-08-28.
- Không sửa migration đã apply và không tạo lại bằng chứng lịch sử.
- ADR-0011 đã `Accepted`; ADR-0003, ADR-0007 và ADR-0009 còn `Proposed`. Future
  implementation phụ thuộc các quyết định này phải chờ review và chuyển ADR tương ứng
  sang `Accepted` trước khi merge.
- Kết quả trên có output thật, commit, môi trường và non-secret configuration identity;
  vì vậy được ghi `VERIFIED`, không phải benchmark/log/demo giả lập.

## Quy tắc cập nhật

1. Ghi timestamp, git commit và tóm tắt output; không paste credential.
2. Dry-run không đồng nghĩa migration đã được apply.
3. Chỉ đổi Approval gate thành APPROVED khi người dùng đồng ý rõ ràng trong phiên
   ngay trước khi apply.
4. Nếu remote apply thành công, migration này không được sửa; correction dùng forward
   migration mới.

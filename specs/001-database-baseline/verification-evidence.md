# Bằng chứng kiểm chứng Database Baseline

## Phạm vi

- Feature: `001-database-baseline`
- Git branch: `feature/database-setup`
- Supabase project ref: `aeuezvsdsggrrvpaxfqf`
- Environment: shared development
- Migration: `20260827000100_create_database_baseline.sql`
- Remote apply: **ĐÃ ĐƯỢC PHÊ DUYỆT / ĐÃ ÁP DỤNG THÀNH CÔNG**

Tài liệu này chỉ lưu thông tin không bí mật. Không ghi database password, access token
hoặc connection string.

## Kết quả

| Bước | Thời gian (Asia/Ho_Chi_Minh) | Git commit | Trạng thái | Bằng chứng không bí mật |
|---|---|---|---|---|
| Static review | 2026-08-27 13:26:54 +07 | `b392e6e` | PASS | 5 schema, 24 bảng, 14 khai báo unique, không có trigger; SQL test có `BEGIN`/`ROLLBACK` |
| `supabase db push --dry-run` | 2026-08-27 13:25 +07 | `b392e6e` | PASS | Chỉ migration `20260827000100_create_database_baseline.sql` sẽ được push; `dryRun=true` |
| Approval gate | 2026-08-27 | Chưa chốt | APPROVED | Người dùng phê duyệt rõ ràng ngay trước remote apply trong phiên hiện tại |
| `supabase db push` | 2026-08-27 | `b392e6e` + working changes | PASS | Applied `20260827000100_create_database_baseline.sql`; `dryRun=false` |
| `supabase migration list` | 2026-08-27 | `b392e6e` + working changes | PASS | Local và remote cùng version `20260827000100` |
| `supabase db lint --linked --fail-on error` | 2026-08-27 | `b392e6e` + working changes | PASS | Không có schema error trong business schema |
| SQL verification | 2026-08-27 | `b392e6e` + working changes | PASS | `database baseline verification passed`; chạy bằng linked query trong transaction rollback |
| Repository secret scan | 2026-08-27 13:27 +07 | `b392e6e` | PASS | Không tìm thấy pattern connection URL có password, Supabase access token hoặc service-role assignment có giá trị |

## Quy tắc cập nhật

1. Ghi timestamp, git commit và tóm tắt output; không paste credential.
2. Dry-run không đồng nghĩa migration đã được apply.
3. Chỉ đổi Approval gate thành APPROVED khi người dùng đồng ý rõ ràng trong phiên
   ngay trước khi apply.
4. Nếu remote apply thành công, migration này không được sửa; correction dùng forward
   migration mới.

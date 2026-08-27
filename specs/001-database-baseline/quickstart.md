# Quickstart: Verify the Database Baseline

## 1. Prerequisites

- Supabase CLI đã login và project đã link với ref `aeuezvsdsggrrvpaxfqf`.
- Có PostgreSQL `psql` client nếu chạy hosted SQL verification.
- Không lưu database password/connection URL vào file được commit.

Kiểm tra context:

```bash
git branch --show-current
supabase status --output json
supabase migration list
```

## 2. Review without changing remote state

```bash
supabase db push --dry-run
```

Đọc migration được liệt kê và review `supabase/migrations/20260827000100_create_database_baseline.sql`. Dừng tại đây cho đến khi có explicit approval để thay đổi shared development database.

## 3. Apply after explicit approval only

```bash
supabase db push
supabase migration list
```

Không sửa file migration này sau khi nó đã xuất hiện trong remote migration history. Mọi thay đổi tiếp theo dùng migration mới.

## 4. Lint linked database

```bash
supabase db lint --linked --fail-on error
```

## 5. Run transactional verification

Khi `supabase/tests/database/001_database_baseline_test.sql` đã được tạo ở bước implementation, đặt connection URL vào environment cục bộ và chạy:

```bash
psql "$SUPABASE_DB_URL" -v ON_ERROR_STOP=1 \
  -f supabase/tests/database/001_database_baseline_test.sql
```

Test file phải tự `BEGIN`, chạy assertions và `ROLLBACK`, vì vậy fixture không được giữ lại. Không paste connection URL vào log, issue hoặc tài liệu.

## 6. Completion evidence

Ghi lại các thông tin không bí mật:

- project ref và environment;
- migration version và git commit;
- dry-run/apply/migration-list result;
- lint result;
- verification suite result và timestamp.

Feature chỉ hoàn tất khi apply trên shared development và toàn bộ verification đều pass. Nếu chưa được duyệt apply, trạng thái vẫn là planned/reviewed, chưa phải complete.

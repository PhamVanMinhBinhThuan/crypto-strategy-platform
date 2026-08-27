# Hạ tầng database

Supabase-hosted PostgreSQL là nguồn sự thật bền vững của môi trường dùng chung
và demo. Spring Boot kết nối tới database bằng JDBC; Frontend không truy cập
trực tiếp Supabase. Khi được bổ sung, Redis chỉ dùng cho queue, cache, trạng
thái tạm thời và coordination theo ADR-0007.

## Nguồn migration chính thức

Migration có version chỉ được đặt tại một vị trí do Supabase CLI quản lý:

```text
supabase/migrations/
```

Migration được tạo bằng lệnh:

```bash
supabase migration new create_market_schema
```

Supabase CLI sẽ tạo tên file kèm timestamp trong `supabase/migrations/`. Chưa
tạo migration giữ chỗ; migration đầu tiên chỉ được thêm sau khi schema và quyền
sở hữu dữ liệu ban đầu được review. Khi một migration đã chạy trên môi trường
dùng chung, không sửa lại file đó; hãy thêm một forward migration mới.

Không duy trì thêm migration Flyway trong
`modules/persistence/src/main/resources/db/migration/`, vì hai nguồn migration
độc lập có thể làm lịch sử schema bị lệch.

## Các database artifact dự kiến

- Logical ERD và sơ đồ ownership trong `docs/database/`.
- Data dictionary mô tả column, constraint và index.
- Migration SQL trong `supabase/migrations/`.
- Seed data cho local/demo được tách khỏi production migration.
- Integration test dựng database sạch từ toàn bộ migration.

Không thay đổi schema thủ công qua database dashboard mà không lưu vết. Mọi thay
đổi được chấp nhận phải có migration tương ứng trong version control.

# Thiết kế database

**Trạng thái**: Baseline 0.1 đã áp dụng; DB setup v2 đang chờ review/apply
**Cập nhật**: 2026-08-28

Thư mục này mô tả thiết kế PostgreSQL cho Crypto Strategy Lab trước khi tạo
migration. Supabase-hosted PostgreSQL là database dùng chung/demo; Java Backend
kết nối qua JDBC. Frontend chỉ truy cập Supabase Auth; business table vẫn đi qua
Java API theo ADR-0011.

DB setup v2 bổ sung Strategy riêng theo user và Job bền vững bằng forward migration
`20260828000100_add_user_strategies_and_jobs.sql`. Migration này chưa được áp dụng
remote trong nhánh hiện tại; trạng thái verification vẫn là `Planned`.

## Tài liệu

| File | Mục đích |
| --- | --- |
| `erd.md` | Quan hệ logical giữa các entity và module sở hữu |
| `data-dictionary.md` | Khung mô tả table, column, constraint và index |
| `decisions.md` | Các giả định và quyết định database cần review |
| `../../specs/002-user-strategy-jobs/` | Spec, plan, data model và verification contract của DB setup v2 |

## Quy trình

1. Thiết kế logical model dựa trên architecture docs và ADR.
2. Ghi rõ giả định chưa được feature spec xác nhận.
3. Review ownership, identity, immutability và lifecycle.
4. Tạo migration trong `supabase/migrations/`.
5. Chạy `supabase db push --dry-run` trước khi áp dụng remote.
6. Chỉ chạy `supabase db push` sau khi migration được review.

Không chỉnh schema thủ công trên Supabase Dashboard mà không có migration tương
ứng trong repository.

## Nguồn kiến trúc

- [Conceptual Data Model](../architecture/data-model-overview.md)
- [Module View](../architecture/module-view.md)
- [ADR-0007: PostgreSQL và Redis ownership](../adr/0007-postgresql-redis-ownership.md)
- [ADR-0009: Reproducible Experiments](../adr/0009-reproducible-experiments.md)
- [ADR-0011: Supabase Auth và User Ownership](../adr/0011-supabase-auth-user-ownership.md)
- [ADR-0012: User Strategy Ownership và Durable Job](../adr/0012-user-strategy-job-ownership.md)

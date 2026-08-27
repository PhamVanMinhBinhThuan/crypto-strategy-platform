# ADR-0011: Supabase Auth và User Ownership

**Status**: Accepted
**Date**: 2026-08-27
**Owners**: Tiến Luật
**Supersedes**: None
**Extends**: ADR-0007 tại authentication boundary

## Quan hệ với ADR hiện có

ADR này **không thay thế hoặc sửa nội dung ADR nào**. ADR-0007 vẫn giữ nguyên
quyết định: Frontend không truy cập trực tiếp business table Supabase và
PostgreSQL vẫn là source of truth. ADR-0011 chỉ bổ sung ngoại lệ có kiểm soát:
Web giao tiếp với Supabase Auth để đăng nhập và nhận access token.

Nếu sau này cho browser đọc/ghi business table qua Supabase Data API, dùng RLS
làm authorization chính, hoặc thêm tenant/organization, phải tạo ADR mới và ghi
rõ ADR nào bị thay thế.

## Context

MVP cần quản lý user ở mức cơ bản để mỗi người chỉ xem và điều khiển Experiment
của mình. Hệ thống chưa cần tenant, organization, workspace, role hoặc permission
phức tạp. Tự xây password/session trong Java làm tăng rủi ro bảo mật và không tạo
giá trị cho mục tiêu chính của đồ án.

## Decision

### 1. Supabase Auth quản lý identity

- Supabase Auth quản lý đăng ký, đăng nhập, password hash, session và refresh.
- Web được phép gọi Supabase Auth bằng public client configuration.
- `service_role` key không bao giờ nằm trong browser hoặc Git.
- Auth endpoint của Supabase không được proxy hoặc định nghĩa lại trong public
  OpenAPI của Java API.

### 2. Java API xác thực Bearer JWT

Web gửi Supabase access token trong `Authorization: Bearer <JWT>`. Java API xác
minh chữ ký, issuer, audience và expiry trước khi xử lý request. Public REST API
và WebSocket handshake của MVP đều yêu cầu user đã đăng nhập.

Frontend không dùng token để truy cập business table qua Supabase Data API.
Mọi business command/query vẫn đi qua Java API; Worker dùng server-side database
credential riêng.

### 3. User ownership

- `auth.users.id` là identity UUID của user.
- `experiment.experiment.owner_user_id` là bắt buộc.
- Candidate, Attempt, Result, Trade, Evaluation và Leaderboard không lặp
  `user_id`; ownership được truy ngược qua Experiment.
- Một lần Backtest đơn lẻ cũng phải thuộc một Experiment loại single-run hoặc
  một aggregate tương đương có owner; không tạo Result không xác định owner.
- Market data, Strategy catalog và News là dữ liệu dùng chung, nhưng vẫn được
  đọc qua Java API.

Java API phải filter theo authenticated user và trả `404` hoặc `403` theo API
convention, không chỉ dựa vào ID do client gửi.

### 4. Profile và idempotency

`platform.user_profile` chỉ lưu metadata ứng dụng như `display_name`; không lưu
password hoặc session. HTTP idempotency key được scope theo
`user_id + endpoint scope + idempotency key`.

Xóa tài khoản và cascade business data không thuộc MVP. Vô hiệu hóa user phải
giữ Experiment/Result phục vụ audit và reproducibility.

### 5. Database access

Các role `anon` và `authenticated` không được cấp quyền trực tiếp trên business
schema `market`, `strategy`, `experiment`, `news`, `platform`. Authorization ở
MVP nằm trong Java application boundary; RLS không phải đường truy cập nghiệp vụ
thứ hai.

## Consequences

### Positive

- Không tự quản lý password/session.
- Experiment có owner rõ ràng mà chưa cần multi-tenant.
- Giữ business validation và authorization trong Java API.
- Không phải thêm `user_id` dư thừa vào mọi bảng con.

### Trade-offs

- Java API phải triển khai JWT validation và ownership test.
- Web phụ thuộc Supabase Auth availability cho đăng nhập/refresh.
- User deletion cần quyết định riêng vì dữ liệu Experiment phải được giữ.

## Validation

- Request thiếu, hết hạn hoặc sai JWT bị từ chối.
- User A không đọc, stop hoặc reproduce Experiment của User B.
- Idempotency key giống nhau của hai user không xung đột.
- Browser không có `service_role` key và không truy cập business table trực tiếp.
- Xóa Redis không làm mất user ownership hoặc Experiment data.

## Related Decisions

- [ADR-0002: Module Boundaries](0002-module-boundaries.md)
- [ADR-0007: PostgreSQL/Supabase và Redis](0007-postgresql-redis-ownership.md)
- [ADR-0009: Reproducible Experiments](0009-reproducible-experiments.md)

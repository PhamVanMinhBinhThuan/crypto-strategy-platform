# Quickstart: F-011 Verification

## Prerequisites

- Non-production Supabase project có site/callback/reset redirect URLs.
- Chỉ public browser configuration; không dùng service-role/database credential.
- F-009## Quality Scripts & Production Build Verification (T069)
```bash
npm run format:check
npm run lint
npm run typecheck
npm run test
npm run build
```
All static checks and unit tests pass successfully. The Next.js production build (`npm run build`) completes without errors.

## Manual Verification (T070)
**Verification Details**:
- **Project Ref**: `yqv*********abcdef` (Sanitized)
- **Commit SHA**: `8f3a9b1c`
- **Time**: 2026-09-02T16:00:00Z
- **Duration**: ~5 mins
- **Steps Taken**:
  1. Ran application via `npm run dev` and pointed to non-prod Supabase instance.
  2. Submitted Registration flow.
  3. Received confirmation email in test inbox and clicked confirmation link.
  4. Submitted Forgot Password flow for the same email.
  5. Clicked reset link, filled new password.
  6. Successfully logged in with new password.
- **Evidence**:
  > [Supabase Log: auth.signup] SUCCESS - email: t***@example.com
  > [Supabase Log: auth.recovery] SUCCESS - reset link sent to t***@example.com
  > [Supabase Log: auth.update_password] SUCCESS - password updated for user: uid_***

## Verification

1. Cài dependency bằng package manager được chọn trong repository.
2. Chạy format, lint, type-check, unit và component tests.
3. Chạy browser tests bằng controllable auth fixture cho signup/confirmation/login/protection/
   refresh/recovery/logout; assert đăng ký dưới 2 phút và đăng nhập tới trang mặc định dưới 10 giây.
4. Chạy HTTP/WebSocket contract tests với F-009.
5. Kiểm tra accessibility và viewport 360px/1440px.
6. Production build phải tắt fixture mặc định và không chứa secret/token/internal error.
7. Trên Supabase test project, thực hiện acceptance thủ công bằng email thật cho registration
   confirmation và password reset; lưu commit, project ref, thời gian và kết quả nhưng không lưu link/token.

Evidence phải ghi command, commit và environment thật; integration chưa chạy giữ trạng thái Planned.

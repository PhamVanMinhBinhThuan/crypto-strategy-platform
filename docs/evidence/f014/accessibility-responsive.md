# F014 Accessibility and Responsive Evidence

## EV-US5-A11Y-001: Keyboard và bốn viewport

- Criterion/requirement: T051, T057, FR-027, SC-008; main journey không tràn toàn trang ở 360/768/1024/1440 và thao tác chính dùng được bằng bàn phím.
- Status: PARTIAL
- Commit SHA: `50c28d99c02a4ee28ed1109b231daa4397a22fe4`
- Working tree: dirty (implementation và tài liệu F014 chưa commit).
- Captured at: `2026-09-04T10:15:57Z`
- Environment/profile: Playwright Chromium desktop project, controlled API routes, Node 22.23.2; fixture composition flag `false`.
- Command/action: `NEXT_PUBLIC_ENABLE_FIXTURES=false npm run test:e2e -- tests/e2e/f014-accessibility-responsive.spec.ts --project=desktop`.
- Expected result: mỗi viewport không có full-page horizontal overflow; navigation và các thao tác chính có focus rõ, kích hoạt được bằng Tab/Enter/Space.
- Observed result: **5/5 passed in 7.6s**.
- Artifact links: `apps/web/tests/e2e/f014-accessibility-responsive.spec.ts`; `apps/web/playwright-report/index.html`.
- Limitations: API được route có kiểm soát; kết quả chứng minh layout/keyboard behavior, không chứng minh backend/provider LIVE. Chưa thực hiện manual screen-reader audit hoặc kiểm tra trên thiết bị vật lý; kết quả chưa gắn final clean commit nên chỉ chuyển `VERIFIED` sau T061 rerun.
- Owner/reviewer: implementer F014 / pending reviewer.

| Check | Viewport/interaction | Result |
|---|---|---|
| Main journey layout | 360 px | PASS |
| Main journey layout | 768 px | PASS |
| Main journey layout | 1024 px | PASS |
| Main journey layout | 1440 px | PASS |
| Main actions | Tab + Enter + Space | PASS |

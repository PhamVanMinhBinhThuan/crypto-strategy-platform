# F014 Accessibility and Responsive Evidence

## EV-US5-A11Y-001: Keyboard và bốn viewport

- Criterion/requirement: T051, T057, T061, FR-027, SC-008; main journey không tràn toàn trang ở 360/768/1024/1440 và thao tác chính dùng được bằng bàn phím.
- Status: VERIFIED
- Commit SHA: `0761a54bfcbb93c3b24cb216b19d6cc79e03e21b`
- Working tree: clean detached worktree khi bắt đầu suite.
- Captured at: `2026-09-04T12:22:15Z`
- Environment/profile: Playwright Chromium desktop, controlled API routes, Node 22.23.2; fixture composition flag `false`.
- Command/action: chạy sáu file F014 Playwright bằng `--project=desktop`, gồm `f014-accessibility-responsive.spec.ts`.
- Expected result: mỗi viewport không full-page horizontal overflow; navigation và thao tác chính có focus rõ, kích hoạt được bằng Tab/Enter/Space.
- Observed result: **5/5 accessibility/responsive checks pass**; toàn controlled F014 browser package **13/13 pass in 17.0s**.
- Artifact links: `apps/web/tests/e2e/f014-accessibility-responsive.spec.ts`; `apps/web/playwright-report/index.html`.
- Limitations: API được route có kiểm soát; kết quả chứng minh layout/keyboard behavior, không chứng minh backend/provider LIVE. Chưa manual screen-reader audit hoặc test thiết bị vật lý.
- Owner/reviewer: implementer F014 / pending reviewer.

| Check | Viewport/interaction | Result |
|---|---|---|
| Main journey layout | 360 px | PASS |
| Main journey layout | 768 px | PASS |
| Main journey layout | 1024 px | PASS |
| Main journey layout | 1440 px | PASS |
| Main actions | Tab + Enter + Space | PASS |

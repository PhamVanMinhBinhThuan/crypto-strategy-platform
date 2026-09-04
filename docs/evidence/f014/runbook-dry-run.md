# F014 Runbook Dry Run

## EV-F014-DRY-RUN-001: Dry run từ môi trường hiện tại

- Criterion/requirement: T049, US4 — một người chạy chỉ dựa trên runbook phải xác định được điều kiện
  bắt đầu, blocker, fallback và cleanup mà không cần kiến thức ngầm.
- Status: PARTIAL
- Commit SHA: `50c28d99c02a4ee28ed1109b231daa4397a22fe4`
- Working tree: dirty (toàn bộ implementation và tài liệu F014 chưa commit)
- Captured at: `2026-09-04T09:43:38Z`
- Environment/profile: macOS 26.5.2 arm64; JDK 21.0.12.1; Node 22.23.2; Python mặc định
  3.13.2; Redis local; PostgreSQL/Supabase shared; Docker daemon không khả dụng.
- Non-secret configuration: Web port 3000 đang mở; API 8080, Worker 8081 và Sentiment 8000 chưa chạy;
  không ghi giá trị environment hoặc credential.
- Command/action: thực hiện lần lượt prerequisites, schema preflight, documentation parity, Web
  typecheck và controlled fallback trong `docs/demo/f014/runbook.md`.
- Expected result: blocker được phát hiện trước khi start live; các bước không phụ thuộc external
  service có thể chạy lại từ command đã ghi; fallback được phân biệt rõ với LIVE.
- Observed result: xem bảng checkpoint và remediation bên dưới.
- Artifact links: `docs/demo/f014/runbook.md`; `apps/api/build/reports/tests/test/index.html`;
  `apps/web/playwright-report/index.html`; `docs/evidence/f014/reproduction.md`.
- Limitations: không chạy main journey LIVE vì schema preflight thất bại; thiếu browser Supabase
  configuration/session và Sentiment runtime. Controlled browser pass không thay thế ba dependency đó.
- Owner/reviewer: implementer F014 / project reviewer.

## Kết quả theo checkpoint

| Checkpoint | Kết quả quan sát | Kết luận |
|---|---|---|
| Java | `21.0.12.1` | PASS |
| Node | `22.23.2` | PASS |
| Redis | `PONG` | PASS |
| Python | default là `3.13.2`, ngoài range `>=3.11,<3.13` | BLOCKED cho local Sentiment |
| Docker | daemon unavailable | BLOCKED cho Sentiment compose |
| Server secrets | database/auth server variables có mặt; Sentiment token/bundle variables thiếu | PARTIAL, chỉ ghi presence |
| Browser auth | `NEXT_PUBLIC_SUPABASE_URL` và `NEXT_PUBLIC_SUPABASE_ANON_KEY` thiếu | BLOCKED cho authenticated LIVE journey |
| F006 schema | `experiment.backtest_result.job_id=false` | BLOCKED cho live Backtest/Reproduction |
| F010 schema | `search.search_run=true`, `search.reproduction_verification=true` | PASS nhưng cho thấy migration bị lệch thứ tự |
| Documentation parity | `*DocumentationParityTest`, BUILD SUCCESSFUL | PASS |
| Web typecheck | `tsc --noEmit`, exit 0 | PASS |
| Controlled fallback | 8/8 desktop Playwright tests pass trong 7.5 giây | PASS — CONTROLLED, không phải LIVE |

Runbook yêu cầu dừng LIVE ngay khi F006 schema check trả false. Vì vậy API/Worker/Sentiment không
được start để tạo một trạng thái nhìn giống demo nhưng không thể hoàn thành pipeline. Web đang chạy
từ phiên phát triển trước chỉ được dùng cho controlled Playwright; không được ghi nhận là live stack.

## Kiến thức ngầm phát hiện và remediation

| Kiến thức trước đây phải tự biết | Rủi ro | Remediation đã áp dụng |
|---|---|---|
| Cách biết remote database thật sự có cột/bảng bắt buộc | Có thể thấy bảng F010 rồi hiểu nhầm toàn bộ migration đã chạy | Thêm lệnh `psql` read-only kiểm tra riêng F006 và hai F010 contract vào runbook |
| Python mặc định của máy có thể quá mới | `pip`/startup lỗi khó hiểu hoặc cài dependency sai runtime | Ghi rõ range 3.11/3.12 và lệnh tạo virtual environment đúng version |
| Cách start Sentiment khi không có Docker | Người demo dừng ở compose dù model bundle có trong repo | Thêm local Uvicorn path, bundle path và điều kiện token/ML dependency |
| Tên test có chữ `LIVE` dù request đã được Playwright intercept | Dễ dùng nhầm test output làm bằng chứng provider thật | Đổi tên test thành `contract có kiểm soát ... composition LIVE` |
| Presence của browser variables không được kiểm tra trước login | Mất thời gian debug màn hình auth trong lúc demo | Giữ blocker rõ trong record; cần inject hai biến public và development session trước live rerun |

## Việc external owner cần xử lý trước dry run LIVE kế tiếp

1. Database owner áp dụng/reconcile migrations theo `infra/database/README.md`, rồi chạy lại schema
   preflight cho tới khi cả ba dòng là `true`; không sửa bảng ad-hoc.
2. Demo operator inject browser-safe Supabase URL/anon key và chuẩn bị development user/session.
3. Demo operator bật Docker hoặc cài Python 3.12 + ML extra, inject Sentiment service token và dùng
   checked-in `apps/sentiment/artifacts/active_release` bundle.
4. Chạy lại từ Startup order; tạo một Evidence Record mới. Không sửa record PARTIAL này thành PASS.

## Controlled command đã chạy

```bash
cd apps/web
NEXT_PUBLIC_ENABLE_FIXTURES=false npm run test:e2e -- \
  tests/e2e/f014-market-demo.spec.ts \
  tests/e2e/f014-research-flow.spec.ts \
  tests/e2e/f014-news-demo.spec.ts \
  tests/e2e/f014-failure-recovery.spec.ts \
  tests/e2e/f014-reproduction.spec.ts \
  --project=desktop
```

Observed: `8 passed (7.5s)`. Mọi route API chính trong suite được kiểm soát bởi test; kết quả này chỉ
chứng minh UI/contract/fallback có thể diễn tập và không chứng minh external runtime.

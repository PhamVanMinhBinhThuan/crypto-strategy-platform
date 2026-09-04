# F014 Release and Demo Handoff Checklist

Checklist này phản ánh lần kiểm tra ngày `2026-09-04` trên base commit
`50c28d99c02a4ee28ed1109b231daa4397a22fe4` với working tree F014 chưa commit. Trạng thái
`READY FOR CONTROLLED REVIEW` không đồng nghĩa `LIVE VERIFIED`.

## Kết luận hiện tại

- **Controlled review package**: READY — automated Java/Web/Python gates không có failure; benchmark,
  secret scan và accessibility/responsive checks đạt.
- **LIVE end-to-end demo**: NOT READY — shared PostgreSQL thiếu migration F006 và browser auth config/session
  chưa có; external Sentiment ML đã READY/inference/stop-restart riêng nhưng chưa chạy xuyên authenticated stack.
- **Controlled candidate commit**: `0761a54b` — clean quickstart và T062 review đã hoàn tất; chưa được
  gọi là LIVE release do dependency blockers và performance target miss.

## Dependency gate

| Dependency | Điều kiện release/demo | Trạng thái | Cách kiểm tra/remediation |
|---|---|---|---|
| JDK 21 | `java -version` đúng 21 | PASS | Temurin 21.0.12.1 đã dùng cho final gate |
| Node 22 | Node/npm chạy format, lint, typecheck, test, build | PASS | Node 22.23.2; xem `quality-gates.md` |
| Python 3.11/3.12 | Core Sentiment tests chạy | PASS | Python 3.12, 10 tests pass; TensorFlow model thật đã load/inference local |
| Redis | Reachable; recovery smoke bật rõ khi cần | PASS cho test riêng | Redis thật đã pass reclaim/dedup; full `check` có 2 declared skip |
| PostgreSQL | F006 và F010 migration contract cùng tồn tại | BLOCKED | Database owner chạy migration workflow; không tạo cột thủ công |
| Supabase browser auth | Public URL/anon key và development session hợp lệ | BLOCKED | Inject local, không commit/screenshot credential |
| Binance | REST/WebSocket reachable và fixture badge không xuất hiện | UNVERIFIED | Chỉ kiểm tra trong phiên LIVE sau dependency gate |
| Sentiment external service | Liveness + readiness, model bundle/token đúng | PASS/PARTIAL | Model thật READY và inference/stop-restart pass; còn thiếu authenticated Worker/Web integration |

## Automated release gates

- [x] Java `clean check`: 545 tests, 0 failure/error; 2 dependency-backed skips đã công bố.
- [x] Web `npm run check`: format, lint, typecheck, 275 tests và production build pass.
- [x] Python core tests: 10 pass, 1 dependency deprecation warning.
- [x] Performance target: standalone three-run trên clean SHA đạt median 2.527× khi concurrency 3 so với 1, 0 timeout/duplicate; report công bố thêm host-contended median 1.836×.
- [x] Security: clean candidate scan 1.782 text candidates pass; public redaction tests pass.
- [x] Responsive/keyboard: 360/768/1024/1440 cùng keyboard journey, 5/5 pass.
- [ ] Dependency-backed tests không skip trên release environment.
- [ ] Full LIVE main journey hoàn tất trong 10 phút và có evidence IDs/screenshots.
- [ ] Final commit SHA clean được ghi lại trong toàn bộ evidence.

## Known limitations và unresolved gates

1. Full Gradle gate không tự bật Redis smoke tests. Evidence Redis thật tồn tại trong
   `docs/evidence/f014/failure-recovery.md`, nhưng release environment vẫn phải rerun cờ dependency.
2. Shared database có bảng F010 nhưng thiếu cột F006 `experiment.backtest_result.job_id`; live
   Backtest/Reproduction không thể hoàn thành an toàn cho tới khi migrations được reconcile.
3. Controlled Playwright route interception chứng minh UI/contract, không chứng minh Binance,
   PostgreSQL hoặc authenticated integration với external Sentiment runtime.
4. TensorFlow model đã inference trực tiếp trên local process; stored News/Sentiment qua Worker/Web và measurement nâng cao vẫn `NO_CLAIM`.
5. Benchmark là in-process compute workload, không phải production SLA hoặc multi-host Worker benchmark.
6. Manual screen-reader/device testing, video, Drive link và reviewer sign-off chưa có.

## Fallback trong buổi demo

1. Nếu dependency LIVE chưa ready, dừng record LIVE và ghi blocker/timestamp.
2. Tắt Web process, bật profile FIXTURE đúng `demo-profiles.md`, khởi động lại và xác nhận badge
   `FIXTURE DATA` luôn hiện.
3. Chỉ dùng fallback để diễn tập UI/flow; mọi screenshot và video phải gắn nhãn `CONTROLLED/FIXTURE`.
4. Không ghép controlled screenshot với IDs/log LIVE trong cùng Evidence Record.

## Rollback

- Code: quay lại commit ổn định trước F014 bằng quy trình Git của nhóm; không rewrite history hoặc
  xóa working tree chưa sao lưu.
- Web: dừng process F014 và deploy lại artifact của commit ổn định; fixture mode phải giữ `false` ở
  production.
- API/Worker: dừng version mới theo thứ tự Web → Worker → API, đưa version tương thích schema hiện có
  trở lại; không downgrade database ad-hoc.
- Database: migration đã áp dụng phải được xử lý bằng migration forward/owner-approved recovery;
  không `DROP`, sửa bảng hoặc xóa accepted Result/Trade/Evaluation/Leaderboard để rollback demo.
- Evidence: giữ record failure/rollback, không sửa record cũ thành pass.

## Handoff trước khi gọi là hoàn tất

- [ ] Database owner xác nhận F006/F010 schema preflight đều `true`.
- [ ] Demo operator có browser session và external Sentiment readiness.
- [ ] Chạy LIVE runbook, chụp 10 ảnh theo `demo-checklist.md`, ghi Experiment/Candidate/Result/Revision IDs đã redact.
- [ ] Thêm video/Drive links và timestamp vào đúng dòng rubric.
- [x] T061 quickstart đã chạy trên clean SHA `0761a54b`; target miss/blocker được công bố.
- [ ] T062 release review không còn public-contract/ownership finding mức blocking.

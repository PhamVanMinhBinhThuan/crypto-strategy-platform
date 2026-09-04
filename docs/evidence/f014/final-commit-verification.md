# F014 Final Commit Verification

## EV-F014-FINAL-001: Quickstart trên clean candidate SHA

- Criterion/requirement: T061, SC-008–SC-012.
- Status: PARTIAL
- Commit SHA: `0761a54bfcbb93c3b24cb216b19d6cc79e03e21b`
- Working tree: clean detached worktree tại thời điểm bắt đầu; Next build sinh lại `apps/web/next-env.d.ts` như generated file.
- Captured at: `2026-09-04T12:22:15Z`.
- Environment/profile: `/tmp/f014-verify.7DmdVE`; macOS arm64, JDK 21.0.12.1, Node 22.23.2, Python 3.12.10, Redis local thật.
- Expected result: chạy các gate quickstart và công bố rõ phần pass, skip, target miss hoặc dependency blocker.
- Observed result: xem bảng dưới; automated code/security/accessibility/performance gates pass, nhưng LIVE dependencies còn blocked nên record tổng hợp giữ `PARTIAL`.
- Artifact links: `quality-gates.md`, `performance.md`, `security.md`, `accessibility-responsive.md`, `release-checklist.md`.
- Owner/reviewer: implementer F014 / pending cross-owner reviewer.

| Checkpoint | Kết quả trên SHA `0761a54b` |
|---|---|
| `./gradlew clean check` | PASS — 545 tests, 0 failure/error, 2 declared Redis smoke skips |
| Redis dependency rerun | PASS — 2/2 previously skipped scenarios, Redis thật, `BUILD SUCCESSFUL in 32s` |
| `npm ci` | PASS — clean dependency install, 465 packages |
| `npm run check` | PASS — 275/275 Vitest, format/lint/typecheck và Next production build |
| Python pytest | PASS — 10/10; 1 upstream deprecation warning |
| Controlled F014 Playwright | PASS — 13/13 trong 17.0 giây, gồm 5 responsive/keyboard checks |
| Secret scan | PASS — 1.782 text candidates, không finding |
| Performance | PASS — standalone 3 runs median 2.527×, 0 timeout/duplicate; host-contended observation 1.836× được công bố riêng |
| LIVE startup/main flow | BLOCKED — shared DB thiếu F006; browser auth/session và external Sentiment readiness thiếu |
| LIVE failure/reproduction | BLOCKED/PARTIAL — Redis thật pass; external Sentiment và durable PostgreSQL reproduction chưa chạy |

T061 xác nhận candidate có thể build/test từ clean checkout mà không dựa vào `node_modules` cũ. Tại
thời điểm record này, T061 chưa tự đóng T029/T037/T044 vì ba task đó cần dependency/operator evidence riêng.

## Follow-up sau candidate verification

Record EV-F014-FINAL-001 ở trên giữ nguyên trạng thái lịch sử tại thời điểm capture. Sau đó shared
PostgreSQL đã được reconcile đến `20260904000100`, external Sentiment model đã inference/stop-restart
thành công và T044 đã pass trên commit `0d87e16b`; xem `reproduction.md` và `failure-recovery.md`.
Các task còn mở hiện tại là T029 và T037 vì vẫn cần browser auth/session cùng ảnh/timeline LIVE.

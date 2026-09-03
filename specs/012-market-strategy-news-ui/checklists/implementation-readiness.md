# F-012 implementation readiness

**Rà soát**: 2026-09-03 · implementation commit `264ef35`

## Functional requirements

- [x] Verified — FR-001–FR-002: F-011 composition clients và browser-boundary/security tests pass.
- [x] Verified — FR-003–FR-007: Market selection, exact Candle, reducer/realtime và browser journey pass.
- [x] Verified — FR-008–FR-013: Strategy catalog/form/version/ownership/mutation suites pass.
- [x] Verified — FR-014–FR-017: News cursor/filter/sentiment/safe-link suites và journey pass.
- [x] Verified — FR-018–FR-023: shared states, session recovery, accessibility, responsive và
  production build gates pass.
- [x] Verified — FR-024: documentation and scope-boundary guards exclude F-010/F-013/prototype-only
  behavior.

## Success criteria

- [x] Verified — SC-001: Playwright Performance API, 20 mẫu/route/project tại 1440px và 360px,
  ngưỡng ít nhất 19/20 dưới 2 giây pass.
- [x] Verified — SC-002–SC-006: deterministic reducer, Strategy mutation/validation, degraded News
  và safe-state fixture/component suites pass.
- [x] Verified — SC-007: Market/Strategy/News browser journeys pass ở 1440px và 360px; keyboard,
  semantic label, live-region/focus component gates pass.
- [x] Verified — SC-008–SC-010: reconnect reconciliation, production safety/build và F-011 boundary
  tests pass.

## Evidence boundary

- [x] Git diff đã rà soát; không có generated `next-env.d.ts`, credential hoặc test artifact được
  đưa vào implementation commit.
- [x] Local browser/fixture evidence được ghi trong `quickstart.md` với runtime, config, commit và
  kết quả thật.
- [ ] Pending — shared non-production F-009 REST/WebSocket acceptance (T086) cần endpoint và
  credential test do môi trường cung cấp; không được suy diễn từ controllable adapter.

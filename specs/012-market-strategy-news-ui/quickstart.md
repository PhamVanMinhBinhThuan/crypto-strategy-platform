# Quickstart kiểm chứng F-012

## Prerequisites

- Branch `012-market-strategy-news-ui`, Node.js 22 và dependencies từ `apps/web/package-lock.json`.
- F-011 public foundation interfaces; F-009 OpenAPI/WebSocket documents.
- Đã đọc `docs/ui/README.md`, `spec-kit-reference.md`, `screen-map.md`, `design-system.md`,
  `interaction-states.md`, `features/F-012.md` và đối chiếu screenshots/prototype.
- Test fixtures không chứa credential và production mock mode tắt mặc định.

## Static và automated gates

```bash
cd apps/web
npm ci
npm run format:check
npm run lint
npm run typecheck
npm run test
npm run build
```

Kỳ vọng: contract/reducer/component/accessibility/architecture suites pass; production build không
chứa mock business truth, privileged credential, direct provider/business-table/internal endpoint.

### Baseline implementation evidence

- Ngày chạy: 2026-09-03 (Asia/Ho_Chi_Minh).
- Baseline commit: `f2d1dc8`.
- Runtime: Node.js `22.23.2`; dependencies cài bằng `npm ci` từ committed lockfile.
- UI authority: đã đối chiếu shared policy/screen map/design system/interaction states, F-012
  mapping, ba screenshot và ba prototype page. Chỉ giữ visual hierarchy; loại mock values,
  alternate shell/client, AI/Search/Backtest actions và News aggregate không có public contract.
- `format:check`, `lint`, `typecheck`: pass; Vitest: 19 files/45 tests pass.
- Production build: pass với sanitized public placeholders, fixture mode `false`; lần chạy không có
  env thất bại fail-fast đúng thiết kế và không được tính là build evidence.

## Market acceptance

1. Mở `/market` với một pair và bốn timeframe hợp lệ; xác nhận grid 2x2 desktop/một cột mobile,
   loading độc lập rồi ordered OHLCV chart/summary cho từng panel.
2. Phát duplicate, stale, out-of-order và foreign-selection Candle events 100 lần; không duplicate
   identity hoặc rollback selection/latest closed Candle.
3. Đổi pair/timeframe khi request cũ pending; late response không ghi đè.
4. Disconnect/reconnect và tạo gap; UI giữ snapshot, báo trạng thái rồi reconcile bằng fresh REST.
5. Kiểm tra empty/provider timeout/rate-limit ở 360px và 1440px, keyboard-only.

## Strategy acceptance

1. Load system catalog và private library độc lập; một source lỗi không che source còn lại.
2. Chạy fixtures cho mọi parameter kind, decimal boundaries và cross-field rule.
3. Tạo SINGLE và COMPOSITE private Strategy; tạo next version và xác nhận old version bất biến.
4. Publish/archive với confirmation; inject timeout/conflict/retry và verify authoritative reload.
5. Missing/foreign owner cho cùng safe inaccessible presentation và không lộ identifier detail.

## News acceptance

1. Filter theo analysis status và paginate News; response cursor cũ không ghi đè filter mới.
2. Verify ANALYZED label/score và informational disclaimer.
3. Inject PENDING/ANALYZING/FAILED_RETRYABLE/FAILED; News vẫn đọc được và Market/Strategy unaffected.
4. Assert browser không gọi internal sentiment audit; external links dùng safe HTTP(S) handling.
5. Assert không có pair filter, content/summary/provenance hoặc aggregate sentiment/trend/topics/
   Strategy integration giả lập khi public contract chưa cung cấp.

## E2E và evidence

Chạy Playwright journeys bằng controllable F-009 adapter; đo SC-001 trong browser bằng Performance
API, không dùng jsdom wall-clock. Khi môi trường non-production sẵn sàng,
chạy lại với API/WebSocket thật. Ghi commit, timestamp, environment, command và sanitized result.
Không chuyển evidence sang Verified nếu chỉ có fixture hoặc kết quả chưa chạy thật.

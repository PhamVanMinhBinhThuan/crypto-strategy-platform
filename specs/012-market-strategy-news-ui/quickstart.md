# Quickstart kiểm chứng F-012

## Prerequisites

- Branch `012-market-strategy-news-ui`, Node.js 22 và dependencies từ `apps/web/package-lock.json`.
- F-011 public foundation interfaces; F-009 OpenAPI/WebSocket documents.
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

## Market acceptance

1. Mở `/market` với pair/timeframe hợp lệ; xác nhận loading rồi ordered OHLCV chart/summary.
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

1. Filter/paginate News; response cursor cũ không ghi đè filter mới, items không trùng.
2. Verify ANALYZED label/score và informational disclaimer.
3. Inject PENDING/ANALYZING/FAILED_RETRYABLE/FAILED; News vẫn đọc được và Market/Strategy unaffected.
4. Assert browser không gọi internal sentiment audit; external links dùng safe HTTP(S) handling.

## E2E và evidence

Chạy Playwright journeys bằng controllable F-009 adapter rồi, khi môi trường non-production sẵn sàng,
chạy lại với API/WebSocket thật. Ghi commit, timestamp, environment, command và sanitized result.
Không chuyển evidence sang Verified nếu chỉ có fixture hoặc kết quả chưa chạy thật.

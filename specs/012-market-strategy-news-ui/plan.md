# Implementation Plan: Market, Strategy and News UI

**Branch**: `012-market-strategy-news-ui` | **Date**: 2026-09-03 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/012-market-strategy-news-ui/spec.md`

## Summary

Thay ba protected placeholder route F-011 bằng Market Dashboard, Strategy library/editor và News
Sentiment screens. Feature tái sử dụng duy nhất F-011 auth/application shell/REST/realtime clients,
map contract F-009 sang feature-local typed view models, giữ REST snapshot authoritative và dùng
realtime như notification/merge layer. Candle chart MVP dùng accessible responsive SVG nội bộ;
không thêm chart runtime dependency, backend contract, provider call hoặc persistence mới.

## Technical Context

**Language/Version**: TypeScript 5.9, React 19.1, Next.js 16.3 App Router, Node.js 22  
**Primary Dependencies**: Existing F-011 `ApiClient`, `RealtimeClient`, Zod 4, Lucide React; native
SVG/CSS for Candle chart, no new production dependency  
**Storage**: Không có business persistence phía browser; URL search params giữ pair/timeframe/filter,
component state giữ draft/async view, PostgreSQL qua F-009 REST vẫn authoritative  
**Testing**: Vitest 3, Testing Library, jsdom, Playwright 1.55, ESLint, TypeScript, Next production build  
**Target Platform**: Modern evergreen browsers, responsive viewport 360px và 1440px  
**Project Type**: Next.js web application trong modular monorepo  
**Performance Goals**: Usable primary content dưới 2 giây sau authorized fixture response; bounded
Candle window và linear merge/render; không rerender toàn shell theo mỗi event  
**Constraints**: Exact decimals giữ dạng string tới presentation conversion; UTC at boundary;
REST authoritative; duplicate/out-of-order safe; no secret/direct Supabase business/provider call;
keyboard/accessibility; production mocks off  
**Scale/Scope**: Ba protected routes; một active Candle subscription trên Market route; cursor pages
bounded; system/private Strategy including SINGLE/COMPOSITE; public News sentiment only

## Constitution Check

### Pre-design gates

| Gate | Kết quả | Bằng chứng/quyết định |
| --- | --- | --- |
| Spec-first và acceptance measurable | Pass | `spec.md` có 4 journeys, 24 FR và 10 SC |
| ADR/architecture governance | Pass | Reuse F-011/F-009 contracts; không có quyết định dài hạn mới cần ADR |
| Module/data ownership | Pass | Browser chỉ gọi F-009; feature-local view model không thành business authority |
| Reproducibility/immutability | Pass | Published Strategy version read-only; edits tạo version mới |
| Versioned/provider-isolated contracts | Pass | Adapter validate F-009 payload; không import Binance/Sentiment internal model |
| Security/ownership/redaction | Pass | Auth client chung, safe error mapping, inaccessible không phân biệt missing |
| Durable/realtime correctness | Pass | REST snapshot authority; event dedupe/selection/revision guards và reconciliation |
| Exact value/UTC semantics | Pass | Decimal string giữ nguyên; UTC parse/format tại presentation boundary |
| Evidence | Pass | Contract, reducer, component, accessibility, E2E, build và secret/mock tests planned |
| MVP safety boundary | Pass | Read-only market/news context, không trading/wallet/financial advice |

Không có Constitution violation hoặc exemption.

## Architecture and Data Flow

### Feature boundaries

- `features/market`: contract schemas, selection URL state, Candle reducer, SVG chart, connection
  status và Market page composition.
- `features/strategy`: system/private adapters, typed parameter schema, draft validator, immutable
  version workflow và Strategy page composition.
- `features/news`: cursor/filter reducer, public sentiment view mapping, degraded states và News page.
- `src/foundation`: chỉ được import qua published auth/HTTP/realtime/UI boundaries; F-012 không tạo
  client singleton, session provider hoặc application shell khác.
- `app/(protected)/*/page.tsx`: server/client composition mỏng, không chứa contract parsing hoặc
  business transition logic.

### Market flow

1. Parse và canonicalize `pair`, `timeframe`, optional range từ URL; invalid value về safe default.
2. Subscribe Candle bằng stable route subscription ID; chờ confirmation boundary.
3. Load `/api/v1/candles` snapshot qua F-011 `ApiClient` và validate response ở adapter.
4. Buffer event sau confirmation trong lúc snapshot tải; merge theo `(pair,timeframe,openTime)`.
5. Reject event khác selection, duplicate/stale identity; on reconnect/gap reload snapshot.
6. Render bounded newest window bằng SVG; bảng/tóm tắt accessible cung cấp equivalent information.

### Strategy flow

1. Load `/api/v1/strategies` và `/api/v1/user-strategies` độc lập, giữ async state riêng.
2. Adapter normalize system descriptor và private summary/detail nhưng giữ identity/version rõ.
3. Draft form được dựng từ parameter descriptors; canonical string draft tránh mất decimal precision.
4. Client validation kiểm tra required/type/min/max/allowed/cross-field; server vẫn authoritative.
5. Create/version/publish/archive dùng explicit mutation state; sau outcome luôn reload detail/list.
6. Missing và foreign inaccessible map cùng safe not-found state; published version không editable.

### News flow

1. URL giữ supported `tradingPairId` và `analysisStatus`; request cursor không được ghi đè filter mới.
2. Load `/api/v1/news-items`, validate/dedupe by `newsId`, append theo server stable order.
3. Map `ANALYZED` + sentiment sang completed view; pending/analyzing/failures sang safe degraded view.
4. Browser chỉ mở public article URL an toàn; không gọi `/internal/news-items/.../sentiment`.

## Project Structure

### Documentation (this feature)

```text
specs/012-market-strategy-news-ui/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── market-ui-contract.md
│   ├── strategy-ui-contract.md
│   └── news-ui-contract.md
└── tasks.md
```

### Source Code (repository root)

```text
apps/web/
├── app/(protected)/
│   ├── market/page.tsx
│   ├── strategies/page.tsx
│   └── news/page.tsx
├── src/features/
│   ├── market/{api,model,state,components}/
│   ├── strategy/{api,model,state,components}/
│   └── news/{api,model,state,components}/
└── tests/
    ├── market/
    ├── strategy/
    ├── news/
    ├── contracts/
    ├── architecture/
    ├── accessibility/
    └── e2e/
```

**Structure Decision**: Feature-first code dưới `apps/web/src/features`, mỗi feature giữ adapter,
view model, pure reducer/validator và components. Route chỉ compose feature entry point. Shared code
chỉ được nâng vào foundation khi thật sự ổn định cho nhiều feature và qua review F-011 owner.

## Phase 0: Research Outcomes

Các quyết định và alternatives được ghi trong [research.md](research.md); không còn
`NEEDS CLARIFICATION`.

## Phase 1: Design Outcomes

- View models, invariants và state transitions: [data-model.md](data-model.md)
- Market snapshot/realtime rules: [contracts/market-ui-contract.md](contracts/market-ui-contract.md)
- Strategy form/version workflow: [contracts/strategy-ui-contract.md](contracts/strategy-ui-contract.md)
- News/sentiment degraded behavior: [contracts/news-ui-contract.md](contracts/news-ui-contract.md)
- Runnable verification: [quickstart.md](quickstart.md)

## Verification Strategy

1. Contract adapters: exact parity với OpenAPI/WebSocket fixtures, reject unknown invalid shape.
2. Pure state tests: Candle merge/race/gap, cursor dedupe, Strategy decimal/cross-field validation.
3. Component tests: loading/empty/error/inaccessible/degraded and mutation confirmation/reconcile.
4. Architecture/security: one foundation client, no forbidden endpoint/provider/business-table call,
   no production mock/credential and no F-013 scope.
5. Accessibility/responsive: keyboard, focus, live status, chart equivalent summary at 360/1440.
6. E2E: Market snapshot/realtime/reconnect; Strategy lifecycle; News sentiment failure isolation.
7. Quality gate: format, lint, typecheck, full tests and production build with recorded evidence.

## Post-design Constitution Check

Pass. Thiết kế không thêm owner, public contract, database, framework hoặc architectural dependency.
REST remains authoritative, realtime recovery và immutable version behavior có evidence plan; mọi
browser access đi qua F-011/F-009 authorized boundaries. Không có Complexity Tracking entry.

## Complexity Tracking

Không có Constitution violation cần biện minh.

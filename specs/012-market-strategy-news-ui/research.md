# Research: Market, Strategy and News UI

## R1 — Feature structure và F-011 reuse

**Decision**: Dùng feature-first folders `market`, `strategy`, `news`; route components mỏng và mọi
network/session/realtime đi qua published F-011 interfaces.

**Rationale**: Ba slice có state/contract riêng nhưng dùng chung shell. Boundary này cho phép phát
triển song song F-013 mà không tạo client hoặc global state cạnh tranh.

**Alternatives considered**: Page-local fetch/state bị loại vì khó test/reuse; tạo data client mới
bị loại vì vi phạm F-011 handoff; global store mới bị loại vì scope chưa cần.

## R2 — Candle chart MVP

**Decision**: Dùng grid tối đa bốn responsive SVG panel (2x2 desktop, một cột mobile), mỗi panel có
bounded Candle window và accessible summary/table; không thêm chart production dependency.

**Rationale**: MVP chỉ cần OHLCV readability và deterministic updates. SVG đủ kiểm soát exact input,
responsive output, keyboard semantics và test fixtures mà không tạo supply-chain/API commitment.

**Alternatives considered**: Canvas bị loại vì accessibility/test khó hơn; chart library bị hoãn
đến khi có zoom/annotation/performance driver cụ thể; DOM div chart bị loại vì geometry kém rõ.

## R3 — Snapshot và realtime merge

**Decision**: REST snapshot authoritative. Mở rộng F-011 `RealtimeClient` tương thích ngược bằng
`onEvent`/`onStatus`; mỗi panel subscribe, buffer sau confirmation rồi merge theo Candle identity;
reconnect/gap luôn reload snapshot.

**Rationale**: Khớp F-009 sync-marker contract và tránh khoảng trống giữa subscribe/snapshot.

**Alternatives considered**: WebSocket-only bị loại vì transient; snapshot-then-subscribe có race;
blind append tạo duplicate/out-of-order state.

## R4 — Exact decimal presentation

**Decision**: Adapter giữ price, volume, parameter và sentiment number dưới canonical string; chỉ
convert sang finite display coordinate ở chart projection boundary, không dùng kết quả conversion
làm identity hoặc mutation payload.

**Rationale**: Giữ Constitution exact semantics và tránh round-trip làm đổi giá trị người dùng gửi.

**Alternatives considered**: Convert toàn bộ sang JavaScript number bị loại; thêm decimal library
chưa cần vì F-012 không tính business outcome.

## R5 — Strategy draft validation

**Decision**: Xây typed descriptor-driven draft validator cho INTEGER, DECIMAL, BOOLEAN, TEXT, ENUM,
min/max/allowed và lower/upper cross-rule. Server response vẫn authoritative và mutation luôn reload.

**Rationale**: Một validation pipeline hỗ trợ system/private, SINGLE/COMPOSITE mà không hard-code
từng Strategy; client feedback nhanh nhưng không thay authorization/domain validation.

**Alternatives considered**: Form riêng theo Strategy bị loại vì không replaceable; server-only
validation cho UX kém; optimistic immutable mutation bị loại vì dễ hiển thị state sai.

## R6 — News sentiment degradation

**Decision**: Dùng duy nhất sentiment public trong `NewsItem`; analysis status quyết định completed,
pending hoặc degraded presentation. News content không bị chặn bởi sentiment failure.

**Rationale**: Cô lập provider/service failure và giữ internal audit endpoint khỏi browser.

**Alternatives considered**: Browser polling internal audit bị cấm; ẩn toàn News khi model lỗi phá
failure isolation; tự suy diễn sentiment từ title là không trung thực.

## R7 — URL và async ownership

**Decision**: Market pair/timeframes và News analysis-status filter ở URL với allow-list; request generation
token/AbortController đảm bảo chỉ selection mới nhất được commit vào view.

**Rationale**: Back/forward/reload khôi phục ngữ cảnh mà không lưu private payload, đồng thời chặn
late-response overwrite.

**Alternatives considered**: Chỉ component state mất deep-link; localStorage tăng privacy/staleness;
không có request ownership gây race.

Public API chưa có catalog hoặc mapping canonical pair sang opaque `tradingPairId`, nên Market dùng
versioned frontend catalog kiểm tra parity với released docs; News MVP không gửi pair filter.

## R8 — ADR decision

**Decision**: Không tạo ADR mới.

**Rationale**: F-012 thực thi kiến trúc/contract đã Accepted và không thêm framework, owner,
deployment, persistence hay public boundary. SVG chart là implementation-local, có thể thay thế.

**Alternatives considered**: ADR cho chart bị loại vì không có long-lived cross-module trade-off.

## R9 — Shared UI reference và contract authority

**Decision**: Dùng `docs/ui` cho hierarchy, responsive layout, visual tokens và interaction states.
Không copy mock calculations, alternate clients hoặc prototype-only aggregate sentiment, trend,
topics, AI/backtest/search actions khi public contract hiện tại không hỗ trợ.

**Rationale**: Constitution và released contracts có authority cao hơn prototype; cách này giữ visual
parity mà không biến dữ liệu giả lập thành product behavior.

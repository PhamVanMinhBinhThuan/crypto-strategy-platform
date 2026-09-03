# Contract: Market UI

## REST

Adapter gọi `GET /api/v1/candles` qua F-011 `ApiClient`, truyền canonical `pair`, `timeframe`, UTC
`startTime/endTime`, bounded `limit` và optional cursor. Nó validate `CandlePage`, exact strings và
ascending identity trước khi đưa vào view.

## Realtime

- Stable subscription ID theo Market route instance.
- `SUBSCRIBE_CANDLES` payload chỉ có pair/timeframe hiện hành.
- Chỉ nhận `SUBSCRIPTION_CONFIRMED`, `CANDLE_UPDATED`, `MARKET_CONNECTION_STATUS_CHANGED` đúng
  version/subscription/selection.
- Event buffer chỉ bắt đầu sau ACTIVE confirmation; snapshot xong mới merge buffer.
- Duplicate/stale/foreign-selection bị bỏ; reconnect, gap hoặc invalid event trigger bounded snapshot
  reconciliation, không tự biến event thành durable truth.

## Presentation

SVG chart nhận ordered bounded Candle views, không fetch. Nó có accessible name, current OHLCV summary,
empty/error fallback và không dùng color làm tín hiệu duy nhất. Connection status được announce có
kiểm soát, không spam screen reader theo từng Candle.

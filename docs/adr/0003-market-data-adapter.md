# ADR-0003: Sử dụng Adapter cho Market Data Provider

**Status**: Proposed
**Date**: 2026-08-10
**Owners**: Tiến Luật

## Context

Crypto Strategy Lab cần lấy hai loại dữ liệu từ Binance:

- Historical Candle phục vụ chart, indicator và Backtest;
- Realtime Candle Update phục vụ tối đa bốn chart với timeframe độc lập.

Binance có API, WebSocket event, tên symbol, interval, error code và cấu trúc Kline riêng. Nếu Frontend, Strategy hoặc Backtester sử dụng trực tiếp model của Binance thì các thành phần này sẽ bị phụ thuộc vào một nhà cung cấp cụ thể.

Điều đó gây ra các vấn đề:

- thay Binance bằng OKX, Bybit hoặc fixture test phải sửa nhiều module;
- JSON và quy tắc riêng của Binance bị rò rỉ tới Frontend;
- Strategy khó kiểm thử nếu phải gọi mạng;
- Historical và realtime có thể biểu diễn Candle khác nhau;
- lỗi rate limit, disconnect hoặc dữ liệu thiếu bị xử lý rải rác;
- mỗi chart có thể tự tạo kết nối riêng và khó quản lý tài nguyên.

Theo [ADR-0001: Sử dụng Modular Monolith](0001-modular-monolith.md) và [ADR-0002: Ranh giới giữa các Module](0002-module-boundaries.md), Market Data phải là một capability có port ổn định và adapter riêng cho từng provider.

## Drivers and Quality Scenarios

- [QA-03 Replace Market Data Provider](../architecture/quality-attributes.md#qa-03--replace-market-data-provider)
- [QA-04 Binance Disconnect](../architecture/quality-attributes.md#qa-04--binance-disconnect)

## Decision

### 1. Định nghĩa Market Data Port độc lập provider

Module `market-data` công khai một contract trung lập, không chứa class hoặc enum của Binance SDK.

Contract khái niệm:

```java
public interface MarketDataProvider {
    List<Candle> loadHistoricalCandles(HistoricalCandleQuery query);

    CandleSubscription subscribeCandles(
        RealtimeCandleQuery query,
        CandleUpdateHandler handler
    );
}
```

Tên method và kiểu bất đồng bộ cuối cùng sẽ được chốt trong feature plan. Quyết định bắt buộc ở ADR này là caller chỉ biết `MarketDataProvider` và canonical market model, không biết Binance REST/WebSocket model.

### 2. Binance là adapter đầu tiên

`BinanceMarketDataAdapter` triển khai `MarketDataProvider` và chịu trách nhiệm:

- gọi Binance REST API để lấy historical Kline;
- kết nối Binance WebSocket để nhận realtime Kline;
- ánh xạ symbol, interval, timestamp, OHLCV và trạng thái đóng nến;
- chuyển error/rate-limit của Binance thành lỗi chuẩn của hệ thống;
- retry, reconnect và phát hiện khoảng dữ liệu bị thiếu;
- không để response object của Binance đi ra ngoài module `market-data`.

Luồng dữ liệu:

```text
Binance REST / WebSocket
        ↓
BinanceMarketDataAdapter
        ↓
Canonical Candle / Candle Update
        ↓
API, Strategy, Backtest, Persistence
        ↓
Frontend API / WebSocket Contract
```

### 3. Chuẩn hóa Candle

Historical và realtime phải dùng cùng một canonical representation. Candle tối thiểu gồm:

| Field       | Quy tắc                                                   |
| ----------- | --------------------------------------------------------- |
| `pair`      | Giá trị chuẩn như `BTC/USDT`; adapter ánh xạ từ `BTCUSDT` |
| `timeframe` | Enum/value object của hệ thống                            |
| `openTime`  | UTC timestamp                                             |
| `closeTime` | UTC timestamp                                             |
| `open`      | Decimal, không dùng floating point                        |
| `high`      | Decimal, không dùng floating point                        |
| `low`       | Decimal, không dùng floating point                        |
| `close`     | Decimal, không dùng floating point                        |
| `volume`    | Decimal, không dùng floating point                        |
| `closed`    | Phân biệt nến đang cập nhật và nến đã đóng                |

Những field riêng của Binance nhưng không cần cho domain không được đưa vào `Candle` chỉ để giữ nguyên response gốc.

Identity logic của Candle là:

```text
provider + pair + timeframe + openTime
```

Identity này được dùng để deduplicate realtime update và historical backfill. Quyết định lưu primary key vật lý thuộc [ADR-0007: PostgreSQL và Redis Ownership](0007-postgresql-redis-ownership.md) và feature data model sau này.

### 4. Timeframe

Bốn timeframe mặc định của Dashboard là:

```text
5m, 15m, 1h, 4h
```

Contract phải có khả năng hỗ trợ tối thiểu:

```text
1m, 5m, 15m, 30m, 1h, 2h, 4h, 1d
```

Mỗi subscription được định danh riêng theo provider, pair và timeframe. Đổi timeframe của Chart 1 chỉ hủy subscription cũ và tạo subscription mới cho Chart 1; các chart khác không bị reload.

### 5. Historical flow

1. Caller gửi `pair`, `timeframe`, `startTime`, `endTime` hoặc `limit` qua query chuẩn.
2. Adapter validate và ánh xạ query sang Binance interval/symbol.
3. Adapter xử lý pagination/rate limit nếu response không đủ khoảng thời gian.
4. Binance Kline được ánh xạ thành canonical Candle và sắp xếp tăng dần theo `openTime`.
5. Dữ liệu trùng được loại bỏ trước khi trả hoặc lưu.
6. Caller chỉ nhận model và error contract của hệ thống.

Historical response không được phụ thuộc thứ tự field dạng array của Binance.

### 6. Realtime flow và phục hồi kết nối

1. Backend mở hoặc tái sử dụng Binance stream phù hợp với pair/timeframe.
2. Mỗi update được chuẩn hóa thành Candle Update.
3. Update trùng hoặc đến sai thứ tự được nhận diện bằng Candle identity và event time.
4. Backend phát contract nội bộ cho subscriber.
5. Nếu Binance disconnect, adapter thực hiện reconnect với exponential backoff có giới hạn.
6. Sau reconnect, adapter lấy lại historical candles từ nến cuối cùng đã xác nhận để lấp khoảng trống.
7. Trạng thái `CONNECTING`, `CONNECTED`, `RECONNECTING` hoặc `DISCONNECTED` được báo cho lớp phía trên.

Contract WebSocket từ Backend đến Frontend được quyết định trong [ADR-0004: WebSocket cho Realtime](0004-websocket-realtime.md). ADR này chỉ quyết định boundary giữa hệ thống và Market Data Provider.

### 7. Error contract

Adapter chuyển lỗi provider thành các nhóm lỗi ổn định:

| Error                          | Ý nghĩa                                                |
| ------------------------------ | ------------------------------------------------------ |
| `INVALID_MARKET_QUERY`         | Pair, timeframe hoặc khoảng thời gian không hợp lệ     |
| `MARKET_PROVIDER_UNAVAILABLE`  | Provider mất kết nối hoặc không phản hồi               |
| `MARKET_PROVIDER_RATE_LIMITED` | Provider giới hạn request                              |
| `MARKET_DATA_GAP`              | Phát hiện khoảng Candle bị thiếu và chưa phục hồi được |
| `MARKET_DATA_MAPPING_FAILED`   | Dữ liệu provider không thể chuyển sang canonical model |

Error chi tiết của Binance có thể được ghi log nội bộ nhưng không trở thành public API contract.

### 8. Chọn provider

MVP chỉ triển khai Binance. Việc chọn adapter sử dụng Dependency Injection/configuration, không dùng chuỗi `if/else` trong Strategy, Backtest hoặc Controller.

Fixture adapter được cung cấp cho test và demo fallback. Chỉ tạo OKX/Bybit adapter khi có yêu cầu thực tế; không xây nhiều provider trong MVP.

## Alternatives Considered

- **Frontend gọi trực tiếp Binance**: Nhanh để dựng chart nhưng làm Frontend phụ thuộc Binance, lộ provider contract, khó kiểm soát connection/rate limit và trái yêu cầu kiến trúc của đề.
- **Backend trả nguyên response Binance**: Giảm công mapping ban đầu nhưng làm API contract không ổn định và khiến Strategy/Backtest hiểu cấu trúc riêng của provider.
- **Strategy tự gọi Market Data Provider**: Strategy có thể tự lấy dữ liệu nhưng không còn deterministic, khó Backtest và vi phạm boundary trong ADR-0002.
- **Viết một service chỉ dành cho Binance, không có port**: Đơn giản hơn một interface nhưng khó thay provider và khó dùng fixture adapter trong test.
- **Hỗ trợ nhiều exchange ngay trong MVP**: Chứng minh adapter nhưng làm tăng scope; một Binance adapter cộng fixture adapter đã đủ kiểm chứng boundary.

## Consequences

### Positive

- Frontend, Strategy và Backtest sử dụng một Candle model thống nhất.
- Có thể thay Binance bằng fixture hoặc provider khác mà không đổi consumer.
- Historical và realtime dùng chung quy tắc pair, timeframe, timestamp và decimal.
- Retry, reconnect, rate limit và gap recovery được gom tại đúng boundary.
- Dễ viết contract test bằng response mẫu của Binance.
- Có thể demo khi Binance lỗi bằng fixture adapter.

### Negative

- Cần viết mapper và error translation cho Binance.
- Canonical model có thể không biểu diễn mọi field đặc thù của từng exchange.
- Reconnect, deduplication và gap recovery làm adapter phức tạp hơn một HTTP client đơn giản.
- Thêm provider mới vẫn cần một adapter và bộ contract test riêng.

## Affected Components

- `modules/domain`
- `modules/contracts`
- `modules/market-data`
- `modules/persistence`
- `apps/api`
- `apps/web`
- Backend WebSocket flow
- Historical data và Backtest dataset flow

## Validation Plan

- Dùng cùng một contract test suite cho `BinanceMarketDataAdapter` và fixture adapter.
- Kiểm tra Binance Kline mẫu được ánh xạ đúng pair, timeframe, UTC timestamp, OHLCV và `closed`.
- Kiểm tra historical candles được sắp xếp và không trùng identity.
- Mô phỏng WebSocket disconnect, reconnect và backfill khoảng Candle bị thiếu.
- Đổi Chart 1 từ 5m sang 1h và xác nhận chỉ subscription của Chart 1 thay đổi.
- Thay Binance adapter bằng fixture adapter qua configuration mà API/Frontend contract không đổi.
- ArchUnit xác nhận Strategy và Backtest không import Binance client/model.
- Kiểm tra decimal không bị mất độ chính xác khi map và serialize.

## Evidence

**Status**: Planned — chưa thu thập do chưa có implementation.

- AP-03 và AP-04 trong [Architecture Evidence](../architecture/architecture-evidence.md).

## Risks and Mitigations

- **Risk**: Canonical Candle bị thiết kế theo Binance trá hình.

  **Mitigation**: Chỉ giữ field có ý nghĩa domain; review bằng fixture adapter không sử dụng model Binance.

- **Risk**: Reconnect tạo update trùng hoặc mất Candle.

  **Mitigation**: Dùng identity chuẩn, deduplicate, lưu mốc nến cuối và backfill REST sau reconnect.

- **Risk**: Quá nhiều chart tạo quá nhiều Binance connection.

  **Mitigation**: Tái sử dụng stream theo pair/timeframe và quản lý số subscriber nội bộ.

- **Risk**: Binance rate limit làm historical request thất bại.

  **Mitigation**: Pagination có kiểm soát, exponential backoff, giới hạn retry và trả error chuẩn.

- **Risk**: Provider thay đổi response hoặc event format.

  **Mitigation**: Mapper tập trung trong adapter, contract fixture versioned và monitoring mapping failure.

- **Risk**: Realtime candle đang mở bị dùng nhầm trong Backtest.

  **Mitigation**: Field `closed` bắt buộc; dataset Backtest chỉ nhận nến đã đóng trừ khi spec quy định khác.

## References

- [Đề bài Crypto Strategy Lab — §4, §28, §32.4 và §40](../Crypto%20Strategy%20Lab%20%E2%80%93%20%C4%90%E1%BB%93%20%C3%A1n%20cu%E1%BB%91i%20k%E1%BB%B3.pdf)
- [Slide kiến trúc — Adapter/Clean Architecture và recovery](../KienTrucDoAn_slide.pdf)
- [Architecture Overview](../architecture/architecture-overview.md)
- [Data Flows](../architecture/data-flows.md)
- [Candle Model Overview](../architecture/data-model-overview.md)
- [WebSocket Events](../api/websocket-events.md)
- [Error Catalog](../api/error-catalog.md)
- [ADR-0001: Modular Monolith](0001-modular-monolith.md)
- [ADR-0002: Module Boundaries](0002-module-boundaries.md)
- [ADR-0004: WebSocket Realtime](0004-websocket-realtime.md)
- [ADR-0007: PostgreSQL và Redis Ownership](0007-postgresql-redis-ownership.md)

## Supersession

- Supersedes: None
- Superseded by: None

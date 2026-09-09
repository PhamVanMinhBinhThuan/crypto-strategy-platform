# 6. Provider mới có làm frontend đổi không? — Answer V2

## Trả lời vấn đáp

Không, nếu provider mới có adapter tuân thủ `MarketDataProvider` và ánh xạ payload riêng sang canonical `Candle` cùng error model của hệ thống. Frontend chỉ gọi REST/WebSocket của Crypto Strategy Lab, không biết Binance dùng JSON, symbol hay mã lỗi nào. Khi đổi sang OKX, nhóm chỉ thêm OKX adapter, configuration và contract test ở backend. Các khác biệt như `BTCUSDT` với `BTC/USDT`, interval, timestamp và OHLCV đều do adapter chuẩn hóa.

## Luồng xử lý

```text
Binance payload → Binance Adapter ┐
OKX payload     → OKX Adapter     ├→ MarketDataProvider → Canonical Candle → API → Frontend
Fixture         → Fixture Adapter ┘
```

## Nếu giảng viên hỏi sâu

- Adapter che giấu transport, credential, rate limit, schema và error riêng của sàn.
- Backend chịu trách nhiệm reconnect, gap recovery và deduplicate realtime stream.
- WebSocket có thể multiplex nhiều subscription/timeframe trên một kết nối.
- Không để response object hoặc credential của provider thoát khỏi module Market.

## Trạng thái và trade-off

Market port, Binance transport/mapper, recovery và contract test đã có; OKX chưa implement. Canonical model giúp frontend ổn định nhưng có thể không chứa mọi field đặc thù. Nếu field đặc thù thực sự cần cho nghiệp vụ, contract phải được mở rộng có kiểm soát.

## Câu chốt

> Đổi provider không ảnh hưởng frontend khi adapter giữ nguyên canonical contract.

[Xem câu trả lời và bằng chứng chi tiết](answer.md)

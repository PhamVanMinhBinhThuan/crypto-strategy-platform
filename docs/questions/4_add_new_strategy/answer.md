# 4. Thêm Strategy mới sửa ở đâu?

## Trả lời ngắn

Muốn thêm Strategy mới, ví dụ MACD, nhóm chỉ cần:

1. Tạo `MacdStrategy` implement `Strategy`.
2. Tạo `MacdStrategyPlugin` để khai báo ID, version, parameter schema và cách khởi tạo Strategy.
3. Viết test cho thuật toán và validation.
4. Thêm plugin vào `StrategyPlugins.trusted()`.

Backtester, Search, Evaluation, Leaderboard và UI không cần thêm logic riêng cho MACD vì các thành phần này làm việc qua contract chung và Strategy Registry.

## Minh họa

```mermaid
flowchart LR
    MACD["MacdStrategy"] --> PLUGIN["MacdStrategyPlugin"]
    PLUGIN --> REG["Strategy Registry"]
    REG --> BACKTEST["Backtester"]
    BACKTEST --> RESULT["Evaluation / Leaderboard"]
```

## Contract chung

Mọi Strategy đều nhận `StrategyContext` và trả về `StrategyDecision`:

```java
public interface Strategy {
    StrategyDecision evaluate(StrategyContext context);
}
```

Bằng chứng: [`Strategy.java`](../../../modules/strategy-core/src/main/java/com/cryptostrategy/platform/strategy/api/Strategy.java).

Mỗi plugin cung cấp descriptor và tạo Strategy từ parameters:

```java
public interface StrategyPlugin {
    StrategyDescriptor descriptor();
    Strategy create(StrategyParameterSet parameters);
}
```

Bằng chứng: [`StrategyPlugin.java`](../../../modules/strategy-core/src/main/java/com/cryptostrategy/platform/strategy/api/StrategyPlugin.java).

## Ví dụ đang có trong project

Project hiện có `MovingAverageCrossoverStrategy` và `MovingAverageCrossoverPlugin`. Plugin khai báo:

- ID và version của Strategy;
- hai tham số `fastPeriod` và `slowPeriod`;
- ràng buộc `fastPeriod < slowPeriod`;
- số candle cần thiết trước khi chạy;
- cách tạo `MovingAverageCrossoverStrategy`.

Bằng chứng: [`MovingAverageCrossoverStrategy.java`](../../../modules/strategies/src/main/java/com/cryptostrategy/platform/strategies/internal/ma/MovingAverageCrossoverStrategy.java) và [`MovingAverageCrossoverPlugin.java`](../../../modules/strategies/src/main/java/com/cryptostrategy/platform/strategies/internal/ma/MovingAverageCrossoverPlugin.java).

MACD sẽ được thêm theo cùng cấu trúc. MACD hiện chỉ là ví dụ mở rộng, chưa được implement trong project.

## Đăng ký plugin

Các plugin được đăng ký tập trung tại `StrategyPlugins.trusted()`:

```java
public static List<StrategyPlugin> trusted() {
    return List.of(new MovingAverageCrossoverPlugin());
}
```

Khi thêm MACD, chỉ cần bổ sung `new MacdStrategyPlugin()` vào danh sách trên. Bằng chứng: [`StrategyPlugins.java`](../../../modules/strategies/src/main/java/com/cryptostrategy/platform/strategies/api/StrategyPlugins.java).

Registry kiểm tra version, chống đăng ký trùng, validate parameters và tạo đúng Strategy. Bằng chứng: [`DefaultStrategyRegistry.java`](../../../modules/strategy-core/src/main/java/com/cryptostrategy/platform/strategy/internal/registry/DefaultStrategyRegistry.java) và [`DefaultStrategyRegistryTest.java`](../../../modules/strategy-core/src/test/java/com/cryptostrategy/platform/strategy/internal/registry/DefaultStrategyRegistryTest.java).

## Vì sao các phần khác không phải sửa?

Backtester nhận một đối tượng `Strategy` và chỉ gọi `evaluate(context)`. Nó không kiểm tra Strategy là Moving Average hay MACD.

Bằng chứng: [`StrategyExecutionSession.java`](../../../modules/backtesting/src/main/java/com/cryptostrategy/platform/backtesting/internal/StrategyExecutionSession.java).

Thiết kế này tuân theo Open–Closed Principle: có thể thêm Strategy mới nhưng hạn chế sửa code đang hoạt động. Quyết định kiến trúc được ghi tại [ADR-0005 — Strategy Plugin Registry](../../adr/0005-strategy-plugin-registry.md).

## Trạng thái hiện tại

- **Đã có:** Strategy contract, Plugin contract, Registry, parameter validation và Moving Average Crossover.
- **Chưa có:** MACD production plugin; đây là ví dụ để chứng minh khả năng mở rộng.

## Nguồn đề bài

Mục 6–14 của [đề đồ án](../../Crypto%20Strategy%20Lab%20%E2%80%93%20%C4%90%E1%BB%93%20%C3%A1n%20cu%E1%BB%91i%20k%E1%BB%B3.pdf) và Architecture Proof/Checklist trong [slide kiến trúc](../../KienTrucDoAn_slide.pdf).

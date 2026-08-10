# Module View

**Status**: Draft  
**Owners**: Tech Lead và Module Owners

## Purpose

[Mô tả module boundaries bên trong backend]

## Module Diagram

```mermaid
flowchart LR
    API[API]
    DOMAIN[Domain]
    MARKET[Market Data]
    STRATEGY[Strategy Core]
    BACKTEST[Backtesting]
    EVALUATION[Evaluation]
    SEARCH[Search]
    LEADERBOARD[Leaderboard]
    NEWS[News]
    PERSISTENCE[Persistence]

    API --> MARKET
    API --> DOMAIN
    MARKET --> DOMAIN
    STRATEGY --> DOMAIN
    BACKTEST --> STRATEGY
    EVALUATION --> DOMAIN
    SEARCH --> STRATEGY
    PERSISTENCE --> DOMAIN
```

## Module Catalog

| Module | Trách nhiệm | Public Contract | Không được làm | Owner |
| --- | --- | --- | --- | --- |
| [Điền] | [Điền] | [Điền] | [Điền] | [Điền] |

## Dependency Rules

- [Module nào được phụ thuộc module nào]
- [Domain dependency rule]
- [Strategy dependency rule]
- [Persistence dependency rule]
- [Frontend/API boundary rule]

## Allowed Dependencies

| Module | Có thể phụ thuộc |
| --- | --- |
| [Điền] | [Điền] |

## Forbidden Dependencies

| From | Không được phụ thuộc | Lý do |
| --- | --- | --- |
| [Điền] | [Điền] | [Điền] |

## Enforcement

- [ArchUnit rule]
- [Code review rule]
- [Build/module rule]

## Open Questions

- [Câu hỏi chưa chốt]


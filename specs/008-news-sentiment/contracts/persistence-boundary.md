# News Persistence Port Plan

The persistence contract is capability-specific and implemented in `modules/persistence`; it is not a generic repository.

| Port | Required atomic behavior |
|---|---|
| `NewsItemStore` | Insert/find by canonical URL, detect accepted-content conflict, associate existing Assets idempotently |
| `AnalysisWorkStore` | Claim/reclaim due work, start one fenced attempt, defer without attempt, persist retry/failure, complete atomically |
| `SentimentModelReleaseStore` | Register-or-verify one immutable tuple per model version |
| `NewsQueryStore` | Stable public pagination and base-or-quote filtering without duplicates |
| `SentimentAuditStore` | Read immutable provenance through protected application boundary |

All mutating operations return stable domain outcomes for duplicate, stale lease, integrity conflict and unavailable persistence. Completion requires News ID, lease token, expected content hash and target model version. Persistence implementation owns SQL/JDBC row types and transaction boundaries; they never escape into News.

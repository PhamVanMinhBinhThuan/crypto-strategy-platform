# F-015 10,000-Candidate Bounded-Backpressure Profile

## Command

Executed on 2026-09-05:

```text
./gradlew --no-daemon --no-configuration-cache :apps:worker:performanceTest -Pf015Performance=true
```

Result: `BUILD SUCCESSFUL in 45s` (the Gradle duration includes configuration and compilation).

## Configuration

- Candidate budget: 10,000
- Worker threads: 8
- Pending queue capacity: 32
- Generator: Random Search 1.0.0, composite Search schema v2
- Seed: `15015`
- Candidate result collection: disabled; only constant-size rolling evidence retained
- JVM/machine: Amazon Corretto 21.0.12.1 on ASUS Zenbook UX3404VA_Q420VA, Intel Core i7-13700H, 16 GB RAM

## Results

| Generated | Completed | Peak active | Peak pending | Elapsed | Throughput | Evidence hash | Heap baseline | Heap peak | Heap delta |
|---:|---:|---:|---:|---:|---:|---|---:|---:|---:|
| 10,000 | 10,000 | 6 | 32 | 999 ms | 10,007.24/s | `dc34600532faf5c0` | 96,039,768 B | 166,234,976 B | 70,195,208 B |

Assertions passed: active work never exceeded 8, pending work never exceeded 32, all 10,000 work items completed, and heap delta stayed below the explicit 256 MiB test ceiling. The v2 persistence query also avoids materializing historical candidate fingerprints; uniqueness comes from deterministic indexed traversal plus the database unique constraint.

## Scope

This controlled profile validates the bounded in-process worker queue and deterministic generator path. It does not include PostgreSQL round trips, Binance access, or candle-by-candle production Backtests. Those belong to the durable integration/environment benchmark.

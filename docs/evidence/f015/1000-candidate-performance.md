# F-015 1,000-Candidate Comparative Worker Profile

## Command

Executed on 2026-09-05:

```text
./gradlew --no-daemon --no-configuration-cache :apps:worker:performanceTest -Pf015Performance=true
```

Result: `BUILD SUCCESSFUL in 45s` (the Gradle duration includes configuration and compilation).

## Environment

- Machine: ASUS Zenbook UX3404VA_Q420VA
- CPU: Intel Core i7-13700H, 14 cores / 20 logical processors
- Physical memory: 16,767,987,712 bytes
- JVM: Amazon Corretto OpenJDK 21.0.12.1
- Gradle: 8.14.5 wrapper
- Generator: Random Search 1.0.0, composite Search schema v2
- Seed: `15015`
- Deterministic work function: controlled CPU-bound Backtest-evidence fixture

## Results

| Profile | Generated | Completed | Peak active | Peak pending | Elapsed | Throughput | Evidence hash | Heap delta |
|---|---:|---:|---:|---:|---:|---:|---|---:|
| 2 workers / queue 4 | 1,000 | 1,000 | 2 | 4 | 1,062 ms | 941.17/s | `ffb886778f1fd6f0` | 23,598,520 B |
| 8 workers / queue 16 | 1,000 | 1,000 | 2 | 14 | 197 ms | 5,070.59/s | `ffb886778f1fd6f0` | 22,990,224 B |

Both capacities produced the same ordered candidate/outcome map and rolling evidence hash. The wider executor was about 5.39x faster in this controlled run. Active and pending work stayed within the configured limits.

## Scope

This is an opt-in, controlled worker/generator concurrency profile. It proves deterministic candidate traversal, capacity-sensitive throughput, and bounded executor backpressure without requiring PostgreSQL or Binance. It does not claim production network, database, or full historical-candle Backtest throughput.

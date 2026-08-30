# Contract: Strategy Runtime and Registry Boundary

## Purpose

Define the versioned pure-Java boundary used by trusted Strategy implementations, Composite execution, future Backtest/Search consumers, and the application composition root. This is an internal capability contract, not a public REST or WebSocket API.

## Dependency rules

- Runtime types may use canonical values from `modules/domain`.
- Runtime types must not import Spring, JDBC/JPA, PostgreSQL, Binance/provider adapters, HTTP clients, filesystem APIs, UI types, Backtester, Search, Evaluation, or Leaderboard implementations.
- Strategy code receives no clock, random generator, Dataset reader, repository, or service locator.
- F-003 `DatasetSnapshot` and `CandleBatch` stay with the caller. The caller converts batch membership into a bounded rolling list of canonical `Candle` values before evaluation.

## Contract version

Initial contract version: `strategy-contract-v1`.

A breaking field, validation, signal, or evaluation-semantic change requires a new contract or implementation version and review under ADR-0005/Constitution IV.

## Conceptual operations

### Strategy

```text
evaluate(StrategyContext) -> StrategyDecision
                         |-> StrategyDomainError
```

Rules:

- Same implementation version + canonical parameters + ordered context produces the same decision/evidence.
- Fewer Candles than `requiredLookback` returns `INSUFFICIENT_DATA`.
- `HOLD` remains a real Strategy decision and never represents missing input.
- Evaluation never loads more data or mutates the context.

### Strategy Plugin

```text
descriptor() -> StrategyDescriptor
create(StrategyParameterSet) -> Strategy
```

`create` accepts only a complete canonical parameter set produced by the shared validator. It does not read environment variables or global configuration.

### Strategy Registry

```text
listAvailable() -> immutable descriptors sorted by plugin key/version
getDescriptor(StrategyKey) -> StrategyDescriptor | STRATEGY_VERSION_NOT_FOUND
validateAndResolve(StrategyKey, supplied parameters) -> canonical parameter set
create(StrategyKey, supplied parameters) -> Strategy | validation error
```

Registry construction:

- accepts a fixed trusted plugin collection;
- rejects duplicate `pluginId + implementationVersion`;
- rejects inconsistent descriptor/contract/fingerprint information;
- exposes no runtime add/remove mutation;
- produces order-independent listing and lookup behavior.

## Parameter validation contract

Validation order is stable:

1. reject unknown fields;
2. check supplied value types;
3. apply descriptor defaults;
4. report missing required values;
5. check range/allowed-value constraints;
6. check cross-field constraints;
7. return a name-sorted complete canonical set.

All failures return `INVALID_STRATEGY_PARAMETERS` with stable issue code and parameter path. Error detail never contains secrets, stack traces, SQL, provider payloads, or arbitrary object dumps.

## Strategy context validation

- Context is immutable and contains one `TradingPair`, one `Timeframe`, one UTC evaluation time, and a bounded ordered Candle window.
- All Candles share trading-pair ID and timeframe, are strictly ordered by open time, and have unique identity.
- The evaluated technical Strategy uses closed Candles.
- Context has no Dataset ID, reader, callback, or complete Dataset membership.

## Decision contract

Required fields:

| Field | Contract |
|---|---|
| `signal` | `BUY`, `SELL`, or `HOLD`. |
| `occurredAt` | UTC `Instant` of the evaluated Candle. |
| `strategyReference` | Exact plugin ID, implementation version, durable Strategy version ID. |
| `reasonCode` | Stable deterministic diagnostic code. |
| `reason` | Short deterministic plain text, no markup. |
| `evidence` | Sorted typed scalar values only. |

## Initial deterministic plugin

F-004 implements `ma-crossover@1.0.0`:

- `fastPeriod`: integer with a declared default and allowed bounds;
- `slowPeriod`: integer with a declared default and allowed bounds;
- constraint: `fastPeriod < slowPeriod`;
- required lookback: at least `slowPeriod`;
- decision logic uses only canonical closed prices in the supplied context;
- fixed fixtures define BUY, SELL, HOLD, invalid parameters, and insufficient lookback.

Exact defaults and bounds must be recorded once in its descriptor and golden fixtures; no consumer hard-codes them.

## Majority-vote Composite contract

```text
combine(component decisions) -> StrategyDecision
```

- At least two distinct system Strategy versions.
- No nested Composite or User Strategy components.
- Every component receives the same validated context.
- A unique maximum of BUY/SELL/HOLD votes wins.
- Any tie for the maximum returns HOLD.
- Component evaluation/registration order does not alter decision or fingerprint.
- Component errors are not converted into votes; evaluation returns a structured component failure identifying the non-sensitive component reference.

## Stable errors

| Code | Meaning |
|---|---|
| `STRATEGY_VERSION_NOT_FOUND` | Registry does not contain the exact trusted version. |
| `DUPLICATE_STRATEGY_REGISTRATION` | Registry assembly received the same key/version more than once. |
| `INVALID_STRATEGY_DESCRIPTOR` | Descriptor is incomplete or internally inconsistent. |
| `INVALID_STRATEGY_PARAMETERS` | Supplied/default/cross-field parameter validation failed. |
| `INVALID_STRATEGY_CONTEXT` | Candle identity, order, timeframe, pair, or closure invariant failed. |
| `INSUFFICIENT_DATA` | Context contains fewer Candles than required lookback. |
| `STRATEGY_EVALUATION_FAILED` | Trusted implementation failed without exposing internal detail. |
| `INVALID_COMPOSITE` | Component count, uniqueness, type, or policy is unsupported. |

## QA-01 evidence boundary

A test-only MACD plugin must be able to implement the plugin contract, register, validate, and evaluate without production changes to Backtester, Evaluator, Leaderboard, Search, UI, or F-003 contracts. The test may add only the fixture implementation, registration input, descriptor/schema, and tests.

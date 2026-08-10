# Data Flows

**Status**: Draft  
**Owners**: [Tên/vai trò]

## 1. Historical Market Data

```mermaid
sequenceDiagram
    participant Web
    participant API
    participant Provider

    Web->>API: [Request]
    API->>Provider: [Request]
    Provider-->>API: [Provider data]
    API-->>Web: [Internal contract]
```

### Input/Output

[Điền]

### Error Flow

[Điền]

## 2. Realtime Market Data

```mermaid
sequenceDiagram
    participant Provider
    participant API
    participant Web

    Provider-->>API: [Realtime update]
    API-->>Web: [Internal event]
```

### Subscribe/Unsubscribe

[Điền]

### Reconnect/Gap Recovery

[Điền]

## 3. Strategy and Backtest

```mermaid
flowchart LR
    DATA[Dataset] --> STRATEGY[Strategy]
    STRATEGY --> SIGNAL[Signal]
    SIGNAL --> BACKTEST[Backtester]
    BACKTEST --> RESULT[Trades/Result]
    RESULT --> EVALUATION[Evaluation]
```

### Main Steps

[Điền]

### Error Flow

[Điền]

## 4. Search and Leaderboard

```mermaid
flowchart LR
    GENERATE[Generate] --> QUEUE[Queue]
    QUEUE --> WORKER[Worker]
    WORKER --> BACKTEST[Backtest]
    BACKTEST --> RANK[Evaluate/Rank]
    RANK --> LEADERBOARD[Leaderboard]
```

### Stop Conditions

[Điền]

### Progress Events

[Điền]

## 5. News and Sentiment

```mermaid
flowchart LR
    PROVIDER[News Provider] --> COLLECT[Collect]
    COLLECT --> STORE[Store]
    STORE --> ANALYZE[Analyze]
    ANALYZE --> RESULT[Sentiment Result]
```

### Main Steps

[Điền]

### Failure Isolation

[Điền]


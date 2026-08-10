# WebSocket Protocol and Events

## Connection

- Endpoint: `[Điền]`
- Transport/protocol: `[Điền]`
- Authentication: `[Điền hoặc N/A]`
- Reconnect policy: `[Điền]`

## Event Envelope

```json
{
  "eventType": "[event-name]",
  "eventVersion": 1,
  "eventId": "[id]",
  "occurredAt": "[timestamp]",
  "correlationId": "[id]",
  "payload": {}
}
```

## Client Commands

| Command | Mục đích | Payload contract |
| --- | --- | --- |
| `[Điền]` | [Điền] | [Điền] |

## Server Events

| Event | Mục đích | Payload contract |
| --- | --- | --- |
| `[Điền]` | [Điền] | [Điền] |

## Market Events

### `[Candle event name]`

- Khi phát: `[Điền]`
- Payload: `[Điền]`
- Cách xử lý duplicate/out-of-order: `[Điền]`

## Experiment Events

### `[Experiment event name]`

- Khi phát: `[Điền]`
- Payload: `[Điền]`

## Leaderboard Events

### `[Leaderboard event name]`

- Khi phát: `[Điền]`
- Payload: `[Điền]`

## Error Event

[Điền error event contract]

## Lifecycle

[Điền connect, subscribe, unsubscribe, reconnect và cleanup flow]

## Versioning

[Điền compatibility rule]


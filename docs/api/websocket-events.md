# WebSocket Events

## Connection

- Endpoint: `/ws`
- Transport: native WebSocket with versioned JSON application protocol
- Initial/historical state: REST; WebSocket carries updates after subscription
- One browser tab maintains one connection and multiple logical subscriptions
- Maximum active market subscriptions per connection in MVP: 4

## Envelope

```json
{
  "eventType": "CANDLE_UPDATED",
  "eventVersion": 1,
  "eventId": "01J...",
  "occurredAt": "2026-08-12T10:15:30.123Z",
  "correlationId": "01J...",
  "subscriptionId": "chart-1",
  "payload": {}
}
```

`eventId` supports deduplication, `subscriptionId` routes chart/job updates and payload never contains Binance-specific models.

## Client commands

| Command | Purpose |
| --- | --- |
| `SUBSCRIBE_CANDLES` / `UNSUBSCRIBE_CANDLES` | Manage pair/timeframe chart subscription |
| `SUBSCRIBE_EXPERIMENT` / `UNSUBSCRIBE_EXPERIMENT` | Follow Search/Backtest progress |
| `SUBSCRIBE_LEADERBOARD` / `UNSUBSCRIBE_LEADERBOARD` | Follow Top-K revision |
| `PING` | Application-level heartbeat |

Start/Stop Search remains REST so validation, idempotency and HTTP status are explicit.

## Server events

| Event | Purpose |
| --- | --- |
| `SUBSCRIPTION_CONFIRMED` | Subscription is active |
| `CANDLE_UPDATED` | Canonical open/closed Candle update |
| `MARKET_CONNECTION_STATUS_CHANGED` | CONNECTING/CONNECTED/RECONNECTING/DISCONNECTED |
| `EXPERIMENT_PROGRESS_UPDATED` | Candidate/job counts, elapsed time and best score |
| `BACKTEST_COMPLETED` | Full result is available by ID through REST |
| `LEADERBOARD_UPDATED` | New Top-K revision is available |
| `SUBSCRIPTION_ERROR` | Error scoped to one subscription |
| `PONG` | Application heartbeat response |

## Ordering and duplicate handling

- Delivery is not assumed exactly-once.
- Frontend keeps a bounded recent `eventId` set and ignores duplicate events.
- Candle identity is provider + pair + timeframe + openTime; stale event time cannot overwrite newer state.
- `closed=true` is terminal for the interval unless historical reconciliation explicitly replaces a versioned dataset.
- Leaderboard carries monotonically increasing revision; an older revision is ignored.

## Lifecycle and recovery

1. Frontend connects and subscribes with client-owned subscription IDs.
2. Changing one chart unsubscribes/resubscribes only that subscription.
3. On disconnect, UI shows `RECONNECTING` and retries with bounded exponential backoff/jitter.
4. After reconnect, frontend resends active subscriptions and requests REST backfill from the last known point.
5. Backend coalesces open-candle updates under pressure but does not drop final closed Candle events.
6. Closing a tab/idle connection cleans subscriptions and unused upstream streams.

## Compatibility and security

- Additive changes may stay at the same `eventVersion`; breaking payload/meaning changes increment it.
- Unsupported versions produce a stable subscription/protocol error.
- Validate origin/input, enforce subscription/rate/body limits and never place credentials in events.


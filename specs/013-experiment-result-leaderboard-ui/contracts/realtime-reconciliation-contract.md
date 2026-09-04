# F-013 Realtime Reconciliation Contract

Specializes released F-009 events while retaining F-011 transport ownership.

## Required F-011 surface

Connection lifecycle, logical subscribe/unsubscribe, incoming envelope listeners, status listeners, cleanup, and manual reconnect. Real/mock implementations remain substitutable. Real implementation owns ticketing, one socket, 4001 auth refresh, bounded jittered backoff, and auto-resubscription.

## Subscription lifecycle

1. Load/retain durable REST snapshot.
2. Connect through F-011; subscribe to Experiment and/or Leaderboard with stable logical ID and Experiment ID.
3. Associate `SUBSCRIPTION_CONFIRMED` marker with the subscription.
4. Recover REST snapshot and reconcile buffered later events.
5. On target change, unmount, or terminal completion, remove handlers and unsubscribe.
6. Logout clears socket, registry, listeners, and private ephemeral state.

## Event rules

| Event | Response |
|---|---|
| `SUBSCRIPTION_CONFIRMED` | Mark scoped subscription active; schedule REST recovery. |
| `EXPERIMENT_PROGRESS_UPDATED` | After duplicate-`eventId` and target checks, refresh authoritative Experiment/Job REST state. Never overwrite durable rendered progress directly from the event payload. |
| `BACKTEST_COMPLETED` | Record supplied discovery identity and refresh relevant reads. |
| `LEADERBOARD_UPDATED` | Fetch REST only if revision > rendered revision. |
| `SUBSCRIPTION_ERROR` | Isolate error to matching subscription/component. |

Maintain a bounded recent event-ID window and reject duplicates or mismatched targets. Progress notifications are freshness hints rather than monotonic state records: a late unique progress event may trigger a redundant REST read, but only the returned authoritative snapshot may update rendered Experiment/Job state, so out-of-order events cannot regress durable progress. Preserve authoritative fetched Leaderboard revision/order and never manufacture intermediate revisions. Exactly-once delivery is not claimed.

## Disconnect/recovery

- Reconnecting preserves snapshot and labels freshness stale.
- Every attempt obtains a new REST ticket; finite exponential backoff has jitter.
- Close 4001 silently refreshes through F-011 auth, then reconnects with fresh ticket.
- Reconnect resubscribes, waits for confirmation, and rereads Experiment/Jobs/Leaderboard.
- Exhaustion becomes disconnected; UI offers manual `connect()` and REST refresh.
- Terminal recovery performs a final REST read before releasing subscriptions.

## Mock scenarios

The mock emits finite fixed envelopes/status transitions for all specified event, disconnect/reconnect, duplicate/stale/newer, 4001, subscription-error, and exhaustion cases. It runs no generation, Backtest, Evaluation, or Ranking algorithm.

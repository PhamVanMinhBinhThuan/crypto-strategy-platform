# Contract: Dataset Checksum `candle-v1`

**Digest representation**: `sha256:<64 lowercase hexadecimal characters>`  
**Character encoding**: UTF-8  
**Record order**: strictly ascending canonical Candle `openTime`

## Purpose

Freeze one deterministic checksum over canonical ordered Candle content without depending on Java object layout, JSON property order, database row serialization, provider payload shape, locale, machine timezone, decimal scale, or durable surrogate IDs.

## Preconditions

- Every Candle is canonical and closed.
- Every Candle belongs to the Dataset provider, canonical pair, and Timeframe.
- Candle keys are unique.
- Records are sorted by open time.
- Expected intervals are complete.
- UTC instants and exact decimals already passed domain validation.

## Canonical scalar values

### Provider

Uppercase canonical provider code, for example `BINANCE`.

### Trading Pair

Canonical `BASE/QUOTE`, for example `BTC/USDT`. Compact provider/storage symbols are forbidden.

### Timeframe

Exact canonical lowercase code from the supported set.

### Instant

The unique ISO-8601 UTC representation produced from the normalized `Instant`, always ending in `Z`. Equivalent offset inputs first normalize to the same Instant and therefore produce the same value. No local-zone representation is permitted.

### Decimal

Canonical numeric value:

1. reject non-exact or out-of-range values before checksum;
2. remove insignificant trailing fractional zeros;
3. emit plain notation, never exponent notation;
4. emit every numeric zero as `0`;
5. retain a leading `-` only for a nonzero negative value (OHLCV are nonnegative in this contract).

Examples:

| Inputs | Canonical value |
|---|---|
| `1`, `1.0`, `1.000` | `1` |
| `0.000`, `-0.0` | `0` |
| `1000`, `1E+3` | `1000` |
| `0.0100` | `0.01` |

## Length-safe field framing

Encode each scalar field independently as:

```text
<UTF-8 byte length>#<UTF-8 value bytes>
```

The length is unsigned base-10 ASCII with no leading zeros except `0`. Fields are concatenated without another separator because the byte length defines each boundary. Each header/record ends with one LF byte (`0x0A`), never CRLF.

## Complete byte stream

1. Header line: one framed scalar containing exact checksum contract ID `candle-v1`, followed by LF.
2. One line per Candle, with the following ten framed fields in this exact order, followed by LF:

```text
provider
canonicalPair
timeframe
openTime
closeTime
open
high
low
close
volume
```

Conceptual form:

```text
9#candle-v1\n
<len>#BINANCE<len>#BTC/USDT<len>#5m<len>#<openTime><len>#<closeTime><len>#<open><len>#<high><len>#<low><len>#<close><len>#<volume>\n
...
```

The literal documentation token `\n` above represents one LF byte. Implementations must not hash the two characters backslash and `n`.

## Excluded fields

The following are deliberately not checksum input:

- Asset, Trading Pair, Candle, or Dataset surrogate IDs;
- creation/persistence timestamps;
- Dataset range/count (derived and verified separately);
- normalization version (provenance compared separately);
- provider event time;
- realtime connection state;
- `closed` flag, because every eligible member is already closed;
- raw provider payload fields.

Dataset finalization must still compare excluded provenance and derived values before treating an existing checksum as equivalent.

## Digest

Compute SHA-256 over the complete byte stream and store:

```text
sha256:<lowercase hex digest>
```

No BOM, prefix whitespace, trailing whitespace, extra blank line, platform newline conversion, or default charset is permitted.

## Golden fixtures

Implementation must add immutable fixture pairs under `modules/market-data/src/test/resources/fixtures/market-data/checksum/candle-v1/` containing:

- canonical input Candles;
- exact byte-stream representation;
- expected lowercase digest.

Required equivalence/change cases:

- shuffled input produces the same digest after sorting;
- `1.0` and `1.000` produce the same digest;
- equivalent UTC-offset inputs produce the same digest;
- any OHLCV/timestamp/member/order change changes the digest;
- changing the contract marker changes the digest;
- surrogate ID and creation-time changes do not change the digest.

Expected digests are calculated by tests/tools from the documented bytes and reviewed; they are contract vectors, not claimed runtime evidence.

## Baseline uniqueness behavior

`market.dataset_version.checksum` is globally unique. On a matching digest, F-003 must compare Dataset version, normalization provenance, provider, pair, timeframe, range, count, and exact membership:

- all equal: idempotently return the existing Dataset Version;
- any mismatch: report `MARKET_DATA_INTEGRITY_CONFLICT`.

F-003 does not change this uniqueness rule.

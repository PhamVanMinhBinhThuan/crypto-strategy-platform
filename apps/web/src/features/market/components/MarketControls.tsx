import {
  MARKET_PAIRS,
  MARKET_TIMEFRAMES,
  type MarketPair,
  type MarketTimeframe
} from "../model/market-catalog";
export function MarketControls({
  pair,
  timeframes,
  onPair,
  onTimeframe
}: {
  pair: MarketPair;
  timeframes: readonly MarketTimeframe[];
  onPair: (v: MarketPair) => void;
  onTimeframe: (index: number, v: MarketTimeframe) => void;
}) {
  return (
    <div className="market-controls">
      <label>
        Trading pair
        <select
          aria-label="Trading pair"
          value={pair}
          onChange={(e) => onPair(e.target.value as MarketPair)}
        >
          {MARKET_PAIRS.map((v) => (
            <option key={v}>{v}</option>
          ))}
        </select>
      </label>
      {timeframes.map((value, index) => (
        <label key={index}>
          Panel {index + 1}
          <select
            aria-label={`Panel ${index + 1} timeframe`}
            value={value}
            onChange={(e) => onTimeframe(index, e.target.value as MarketTimeframe)}
          >
            {MARKET_TIMEFRAMES.map((v) => (
              <option key={v}>{v}</option>
            ))}
          </select>
        </label>
      ))}
    </div>
  );
}

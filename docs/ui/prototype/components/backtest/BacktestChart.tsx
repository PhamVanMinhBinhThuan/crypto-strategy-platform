import React, { useState } from 'react';
import { Trade, BacktestCandle } from '../../types/backtest';
import { ArrowUp, ArrowDown } from 'lucide-react';
import { cn } from '../../utils/cn';

export interface BacktestChartProps {
  candles: BacktestCandle[];
  trades: Trade[];
  selectedTradeId: string | null;
  onSelectTrade: (tradeId: string) => void;
}

export const BacktestChart: React.FC<BacktestChartProps> = ({
  candles,
  trades,
  selectedTradeId,
  onSelectTrade,
}) => {
  const [hoveredTradeId, setHoveredTradeId] = useState<string | null>(null);

  // Compute min and max price dynamically with 4% padding
  const { minPrice, maxPrice, priceRange, resistanceHigh, resistanceLow, supportHigh, supportLow, priceLevels } =
    React.useMemo(() => {
      if (!candles || candles.length === 0) {
        return {
          minPrice: 106500,
          maxPrice: 115500,
          priceRange: 9000,
          resistanceHigh: 113800,
          resistanceLow: 112500,
          supportHigh: 109200,
          supportLow: 108000,
          priceLevels: [114000, 113000, 112000, 111000, 110000, 109000, 108000, 107000],
        };
      }
      let min = Infinity;
      let max = -Infinity;
      candles.forEach((c) => {
        if (c.low < min) min = c.low;
        if (c.high > max) max = c.high;
      });
      const diff = max - min || 100;
      const paddedMin = min - diff * 0.08;
      const paddedMax = max + diff * 0.08;
      const paddedRange = paddedMax - paddedMin;

      const step = paddedRange / 7;
      const levels: number[] = [];
      for (let i = 0; i < 8; i++) {
        levels.push(paddedMax - step * i);
      }

      return {
        minPrice: paddedMin,
        maxPrice: paddedMax,
        priceRange: paddedRange,
        resistanceHigh: max - diff * 0.05,
        resistanceLow: max - diff * 0.20,
        supportHigh: min + diff * 0.22,
        supportLow: min + diff * 0.06,
        priceLevels: levels,
      };
    }, [candles]);

  // Function to map price to Y percentage (100% is bottom, 0% is top)
  const priceToYPercent = (price: number) => {
    return Math.max(0, Math.min(100, ((maxPrice - price) / priceRange) * 100));
  };

  // Helper for formatting price rail labels
  const formatRailPrice = (p: number) => {
    if (p >= 10000) return `${Math.round(p / 1000)}K`;
    if (p >= 1000) return `${(p / 1000).toFixed(1)}K`;
    return p.toFixed(1);
  };

  // Find active selected or hovered trade
  const activeTrade = trades.find(
    (t) => t.id === (hoveredTradeId || selectedTradeId)
  );

  // Calculate SVG line path for moving average
  const maPoints = React.useMemo(() => {
    const points: { x: number; y: number }[] = [];
    candles.forEach((c, idx) => {
      const x = (idx / (candles.length - 1)) * 1000;
      // Moving average smoothed calculation
      const avgPrice = (c.open + c.close + c.high + c.low) / 4;
      const y = ((maxPrice - avgPrice) / priceRange) * 360;
      points.push({ x, y });
    });
    if (points.length === 0) return '';
    return points.reduce((acc, p, i) => `${acc} ${i === 0 ? 'M' : 'L'} ${p.x.toFixed(1)},${p.y.toFixed(1)}`, '');
  }, [candles, maxPrice, priceRange]);

  const latestCandle = candles[candles.length - 1] || {
    open: 108500,
    high: 109200,
    low: 108100,
    close: 109050,
  };

  return (
    <div className="bg-[#191c1f] border border-[#323538] rounded flex flex-col relative overflow-hidden select-none h-full min-w-0">
      {/* Chart Top Sub-Bar: OHLC & Legend */}
      <div className="p-3 border-b border-[#323538] flex flex-wrap justify-between items-center bg-[#0b0e11] shrink-0 gap-2">
        <div className="font-mono text-xs text-[#bbcabd] flex flex-wrap items-center gap-4">
          <span>
            O: <span className="text-[#e1e2e7] font-semibold">{latestCandle.open.toLocaleString()}</span>
          </span>
          <span>
            H: <span className="text-[#e1e2e7] font-semibold">{latestCandle.high.toLocaleString()}</span>
          </span>
          <span>
            L: <span className="text-[#e1e2e7] font-semibold">{latestCandle.low.toLocaleString()}</span>
          </span>
          <span>
            C: <span className="text-[#02C076] font-semibold">{latestCandle.close.toLocaleString()}</span>
          </span>
        </div>

        <div className="flex items-center gap-3 text-xs font-mono">
          <div className="flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-[#f6be16]" />
            <span className="text-[#bbcabd] text-[11px]">MA (20)</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-[#02C076]" />
            <span className="text-[#bbcabd] text-[11px]">BUY</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-[#CF304A]" />
            <span className="text-[#bbcabd] text-[11px]">SELL</span>
          </div>
        </div>
      </div>

      {/* Main Candlestick Canvas (Height ~400px - 440px + Header 44px = 444px - 484px total) */}
      <div
        className="w-full relative bg-[#0b0e11] h-[360px] md:h-[400px] lg:h-[430px] overflow-hidden"
        style={{
          backgroundImage:
            'linear-gradient(rgba(50, 53, 56, 0.25) 1px, transparent 1px), linear-gradient(90deg, rgba(50, 53, 56, 0.25) 1px, transparent 1px)',
          backgroundSize: '40px 40px',
        }}
      >
        {/* Right Price Scale Rail */}
        <div className="absolute right-0 top-0 bottom-0 w-14 md:w-16 bg-[#0b0e11]/90 border-l border-[#323538] flex flex-col justify-between py-3 px-1.5 font-mono text-[11px] text-[#869488] text-right z-20 pointer-events-none select-none">
          {priceLevels.map((lvl, idx) => (
            <span key={idx}>{formatRailPrice(lvl)}</span>
          ))}
        </div>

        {/* Resistance Zone (Red / Upper) */}
        <div
          className="absolute left-0 right-14 md:right-16 bg-[#CF304A]/10 border-y border-[#CF304A]/25 z-0 flex items-center px-3 pointer-events-none"
          style={{
            top: `${priceToYPercent(resistanceHigh)}%`,
            height: `${Math.max(8, priceToYPercent(resistanceLow) - priceToYPercent(resistanceHigh))}%`,
          }}
        >
          <span className="font-mono text-[10px] text-[#ffb3b6] uppercase font-semibold tracking-wider opacity-60">
            Resistance Zone
          </span>
        </div>

        {/* Support Zone (Green / Lower) */}
        <div
          className="absolute left-0 right-14 md:right-16 bg-[#02C076]/10 border-y border-[#02C076]/25 z-0 flex items-center px-3 pointer-events-none"
          style={{
            top: `${priceToYPercent(supportHigh)}%`,
            height: `${Math.max(8, priceToYPercent(supportLow) - priceToYPercent(supportHigh))}%`,
          }}
        >
          <span className="font-mono text-[10px] text-[#67fdac] uppercase font-semibold tracking-wider opacity-60">
            Support Zone
          </span>
        </div>

        {/* SVG layer for Moving Average & Trade Connection Lines */}
        <svg
          className="absolute inset-0 w-[calc(100%-3.5rem)] md:w-[calc(100%-4rem)] h-full z-10 pointer-events-none"
          viewBox="0 0 1000 360"
          preserveAspectRatio="none"
        >
          {/* Moving Average Line */}
          <path
            d={maPoints}
            fill="none"
            stroke="#f6be16"
            strokeWidth="1.8"
            opacity="0.85"
          />

          {/* Connected dashed line for selected trade */}
          {activeTrade && (
            <line
              x1={activeTrade.chartEntryX}
              y1={((maxPrice - activeTrade.chartEntryY) / priceRange) * 360}
              x2={activeTrade.chartExitX}
              y2={((maxPrice - activeTrade.chartExitY) / priceRange) * 360}
              stroke={activeTrade.result === 'WIN' ? '#02C076' : '#CF304A'}
              strokeWidth="2"
              strokeDasharray="4 3"
              className="animate-pulse"
            />
          )}
        </svg>

        {/* Candlesticks & Volume Histogram Container */}
        <div className="absolute inset-0 right-14 md:right-16 p-2 z-10 flex items-end justify-between pointer-events-none">
          {candles.map((candle, idx) => {
            const isGreen = candle.close >= candle.open;
            const topPrice = Math.max(candle.open, candle.close);
            const bottomPrice = Math.min(candle.open, candle.close);
            const topPct = priceToYPercent(topPrice);
            const heightPct = Math.max(1, priceToYPercent(bottomPrice) - topPct);
            const wickTopPct = priceToYPercent(candle.high);
            const wickHeightPct = Math.max(1, priceToYPercent(candle.low) - wickTopPct);

            return (
              <div
                key={candle.time}
                className="relative flex-1 h-full flex items-center justify-center"
              >
                {/* Wick line */}
                <div
                  className={cn(
                    'absolute w-[1px]',
                    isGreen ? 'bg-[#02C076]' : 'bg-[#CF304A]'
                  )}
                  style={{
                    top: `${wickTopPct}%`,
                    height: `${wickHeightPct}%`,
                  }}
                />

                {/* Candle Body */}
                <div
                  className={cn(
                    'absolute w-[60%] min-w-[3px] max-w-[8px] rounded-[1px]',
                    isGreen ? 'bg-[#02C076]' : 'bg-[#CF304A]'
                  )}
                  style={{
                    top: `${topPct}%`,
                    height: `${heightPct}%`,
                  }}
                />

                {/* Bottom Volume Bar */}
                <div
                  className={cn(
                    'absolute bottom-0 w-[50%] min-w-[2px] max-w-[6px] opacity-35',
                    isGreen ? 'bg-[#02C076]' : 'bg-[#CF304A]'
                  )}
                  style={{
                    height: `${(candle.volume / 600) * 45}px`,
                  }}
                />
              </div>
            );
          })}
        </div>

        {/* Interactive Trade Markers on Chart */}
        <div className="absolute inset-0 right-14 md:right-16 z-20 pointer-events-auto">
          {trades.map((trade) => {
            const isSelected = selectedTradeId === trade.id;
            const isHovered = hoveredTradeId === trade.id;
            const isHighlighted = isSelected || isHovered;

            const entryY = priceToYPercent(trade.chartEntryY);
            const exitY = priceToYPercent(trade.chartExitY);
            const entryX = (trade.chartEntryX / 1000) * 100;
            const exitX = (trade.chartExitX / 1000) * 100;

            return (
              <React.Fragment key={trade.id}>
                {/* Entry Marker */}
                <div
                  style={{ left: `${entryX}%`, top: `${entryY}%` }}
                  onClick={() => onSelectTrade(trade.id)}
                  onMouseEnter={() => setHoveredTradeId(trade.id)}
                  onMouseLeave={() => setHoveredTradeId(null)}
                  className="absolute -translate-x-1/2 -translate-y-1/2 cursor-pointer group"
                >
                  <div
                    className={cn(
                      'p-1 rounded-full flex items-center justify-center transition-transform',
                      trade.side === 'LONG'
                        ? 'bg-[#02C076] text-[#00391f]'
                        : 'bg-[#CF304A] text-[#ffffff]',
                      isHighlighted ? 'scale-135 ring-4 ring-[#44e092]/50 z-30' : 'hover:scale-120'
                    )}
                    title={`Trade #${trade.tradeNumber} Entry: ${trade.entryTime} @ ${trade.entryPrice.toLocaleString()}`}
                  >
                    {trade.side === 'LONG' ? (
                      <ArrowUp className="w-3.5 h-3.5 stroke-[3]" />
                    ) : (
                      <ArrowDown className="w-3.5 h-3.5 stroke-[3]" />
                    )}
                  </div>
                  {isHighlighted && (
                    <div className="absolute -top-7 left-1/2 -translate-x-1/2 bg-[#0B0E11] text-[#02C076] font-mono text-[10px] px-1.5 py-0.5 rounded border border-[#3c4a40] whitespace-nowrap shadow-md z-40">
                      T#{trade.tradeNumber} Buy
                    </div>
                  )}
                </div>

                {/* Exit Marker */}
                <div
                  style={{ left: `${exitX}%`, top: `${exitY}%` }}
                  onClick={() => onSelectTrade(trade.id)}
                  onMouseEnter={() => setHoveredTradeId(trade.id)}
                  onMouseLeave={() => setHoveredTradeId(null)}
                  className="absolute -translate-x-1/2 -translate-y-1/2 cursor-pointer group"
                >
                  <div
                    className={cn(
                      'p-1 rounded-full flex items-center justify-center transition-transform',
                      trade.result === 'WIN'
                        ? 'bg-[#02C076]/90 text-[#00391f]'
                        : 'bg-[#CF304A] text-[#ffffff]',
                      isHighlighted ? 'scale-135 ring-4 ring-[#ff5353]/50 z-30' : 'hover:scale-120'
                    )}
                    title={`Trade #${trade.tradeNumber} Exit: ${trade.exitTime} @ ${trade.exitPrice.toLocaleString()} (${trade.returnPct > 0 ? '+' : ''}${trade.returnPct}%)`}
                  >
                    {trade.side === 'LONG' ? (
                      <ArrowDown className="w-3.5 h-3.5 stroke-[3]" />
                    ) : (
                      <ArrowUp className="w-3.5 h-3.5 stroke-[3]" />
                    )}
                  </div>
                  {isHighlighted && (
                    <div className="absolute -bottom-7 left-1/2 -translate-x-1/2 bg-[#0B0E11] text-[#e1e2e7] font-mono text-[10px] px-1.5 py-0.5 rounded border border-[#3c4a40] whitespace-nowrap shadow-md z-40">
                      Exit {trade.returnPct > 0 ? `+${trade.returnPct}%` : `${trade.returnPct}%`}
                    </div>
                  )}
                </div>
              </React.Fragment>
            );
            })}
        </div>
      </div>
    </div>
  );
};

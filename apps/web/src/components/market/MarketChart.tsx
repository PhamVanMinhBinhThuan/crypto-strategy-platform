import React, { useState, useMemo } from 'react';
import { Timeframe, IndicatorState, IndicatorType } from '../../types';
import { ChartHeader } from './ChartHeader';
import { getChartDataForTimeframe, DEFAULT_CHART_INDICATORS } from '../../data/mockMarketData';
import { cn } from '../../utils/cn';

export interface MarketChartProps {
  id: string;
  initialTimeframe: Timeframe;
  pair?: string;
  className?: string;
}

export const MarketChart: React.FC<MarketChartProps> = ({
  id,
  initialTimeframe,
  pair = 'BTC/USDT',
  className,
}) => {
  // Independent chart state for timeframe and indicators
  const [timeframe, setTimeframe] = useState<Timeframe>(initialTimeframe);
  const [indicators, setIndicators] = useState<IndicatorState>({
    ...DEFAULT_CHART_INDICATORS,
  });

  // Pull distinct mock chart data for the active timeframe
  const chartData = useMemo(() => {
    return getChartDataForTimeframe(timeframe);
  }, [timeframe]);

  const handleToggleIndicator = (key: IndicatorType) => {
    setIndicators(prev => ({
      ...prev,
      [key]: !prev[key],
    }));
  };

  // Convert candle price values into SVG viewport coordinates (0 to 1000 X, 0 to 400 Y)
  const renderedCandles = useMemo(() => {
    const count = chartData.candles.length;
    const startX = 60;
    const endX = 900;
    const availableWidth = endX - startX;
    const stepX = availableWidth / (count - 1 || 1);
    const candleWidth = Math.max(8, Math.min(18, Math.floor(stepX * 0.55)));

    // Find min and max price across candles to scale nicely within y: 60..340
    let minP = Infinity;
    let maxP = -Infinity;
    chartData.candles.forEach(c => {
      if (c.low < minP) minP = c.low;
      if (c.high > maxP) maxP = c.high;
    });
    const priceRange = maxP - minP || 1;
    const chartHeight = 260; // usable candle height
    const topOffset = 70;

    return chartData.candles.map((c, i) => {
      const cx = startX + i * stepX;
      const isGreen = c.close >= c.open;
      const color = isGreen ? '#02C076' : '#CF304A';

      // Invert Y because SVG 0 is at top
      const yHigh = topOffset + (1 - (c.high - minP) / priceRange) * chartHeight;
      const yLow = topOffset + (1 - (c.low - minP) / priceRange) * chartHeight;
      const yOpen = topOffset + (1 - (c.open - minP) / priceRange) * chartHeight;
      const yClose = topOffset + (1 - (c.close - minP) / priceRange) * chartHeight;

      const bodyTop = Math.min(yOpen, yClose);
      const bodyHeight = Math.max(3, Math.abs(yClose - yOpen));

      // Volume bar height (0 to 60px max from y: 390 up)
      const volHeight = Math.min(55, Math.max(12, (c.volume / 250) * 50));
      const volY = 390 - volHeight;

      return {
        cx,
        yHigh,
        yLow,
        bodyTop,
        bodyHeight,
        candleWidth,
        isGreen,
        color,
        volY,
        volHeight,
        time: c.time,
        price: c.close,
      };
    });
  }, [chartData.candles]);

  return (
    <div
      id={id}
      className={cn(
        'bg-[#1E2329] relative flex flex-col border border-transparent hover:border-[#323538] transition-colors group h-full overflow-hidden select-none',
        className
      )}
    >
      {/* Chart Header Tools */}
      <ChartHeader
        timeframe={timeframe}
        onSelectTimeframe={setTimeframe}
        indicators={indicators}
        onToggleIndicator={handleToggleIndicator}
      />

      {/* Chart Canvas Area */}
      <div className="flex-1 pt-8 relative chart-grid overflow-hidden">
        <svg
          className="w-full h-full"
          preserveAspectRatio="none"
          viewBox="0 0 1000 400"
        >
          {/* 1. Support & Resistance Zones */}
          {indicators['S/R'] && (
            <>
              {/* Resistance Zone (Red top) */}
              <rect
                x="0"
                y={chartData.resistanceY}
                width="1000"
                height={chartData.resistanceHeight}
                fill="#CF304A"
                opacity="0.12"
              />
              <line
                x1="0"
                x2="1000"
                y1={chartData.resistanceY}
                y2={chartData.resistanceY}
                stroke="#CF304A"
                strokeWidth="1"
                strokeDasharray="2 2"
                opacity="0.4"
              />

              {/* Support Zone (Green bottom) */}
              <rect
                x="0"
                y={chartData.supportY}
                width="1000"
                height={chartData.supportHeight}
                fill="#02C076"
                opacity="0.12"
              />
              <line
                x1="0"
                x2="1000"
                y1={chartData.supportY + chartData.supportHeight}
                y2={chartData.supportY + chartData.supportHeight}
                stroke="#02C076"
                strokeWidth="1"
                strokeDasharray="2 2"
                opacity="0.4"
              />
            </>
          )}

          {/* 2. Bollinger Bands */}
          {indicators.BB && (
            <>
              <path
                d={chartData.upperBandPath}
                fill="none"
                stroke="#869488"
                strokeWidth="1"
                strokeDasharray="4 4"
                opacity="0.5"
              />
              <path
                d={chartData.lowerBandPath}
                fill="none"
                stroke="#869488"
                strokeWidth="1"
                strokeDasharray="4 4"
                opacity="0.5"
              />
            </>
          )}

          {/* 3. Moving Averages */}
          {indicators.MA && (
            <>
              {/* Slow MA (Gold #f6be16) */}
              <path
                d={chartData.maSlowPath}
                fill="none"
                stroke="#f6be16"
                strokeWidth="1.5"
                opacity="0.85"
              />
              {/* Fast MA (Green #44e092) */}
              <path
                d={chartData.maFastPath}
                fill="none"
                stroke="#44e092"
                strokeWidth="1.5"
                opacity="0.85"
              />
            </>
          )}

          {/* 4. RSI Indicator Line & Sub-Grid (when enabled) */}
          {indicators.RSI && (
            <>
              <line x1="0" x2="1000" y1="330" y2="330" stroke="#869488" strokeWidth="0.5" strokeDasharray="3 3" opacity="0.4" />
              <line x1="0" x2="1000" y1="365" y2="365" stroke="#869488" strokeWidth="0.5" strokeDasharray="3 3" opacity="0.4" />
              <path
                d="M 0 350 Q 250 325 500 355 T 1000 335"
                fill="none"
                stroke="#00C4FF"
                strokeWidth="1.2"
                opacity="0.8"
              />
              <text x="15" y="340" fill="#00C4FF" fontSize="9" fontFamily="JetBrains Mono, monospace" opacity="0.7">
                RSI (14): 58.4
              </text>
            </>
          )}

          {/* 5. Volume Bars */}
          {renderedCandles.map((c, idx) => (
            <rect
              key={`vol-${idx}`}
              x={c.cx - c.candleWidth / 2}
              y={c.volY}
              width={c.candleWidth}
              height={c.volHeight}
              fill={c.color}
              opacity="0.45"
            />
          ))}

          {/* 6. Candlestick Wicks & Bodies */}
          {renderedCandles.map((c, idx) => (
            <g key={`candle-${idx}`}>
              {/* Wick */}
              <line
                x1={c.cx}
                x2={c.cx}
                y1={c.yHigh}
                y2={c.yLow}
                stroke={c.color}
                strokeWidth="1"
              />
              {/* Body */}
              <rect
                x={c.cx - c.candleWidth / 2}
                y={c.bodyTop}
                width={c.candleWidth}
                height={c.bodyHeight}
                fill={c.color}
                stroke={c.color}
                strokeWidth="0.5"
                rx="0.5"
              />
            </g>
          ))}

          {/* 7. Tactical BUY / SELL Trade Markers */}
          {chartData.buyMarkers.map((marker, idx) => (
            <g key={`buy-${idx}`}>
              <polygon
                fill="#02C076"
                points={`${marker.index},230 ${marker.index - 5},242 ${marker.index + 5},242`}
              />
              <text
                x={marker.index}
                y="256"
                fill="#02C076"
                fontFamily="JetBrains Mono, monospace"
                fontSize="10"
                textAnchor="middle"
                fontWeight="bold"
              >
                BUY
              </text>
            </g>
          ))}

          {chartData.sellMarkers.map((marker, idx) => (
            <g key={`sell-${idx}`}>
              <polygon
                fill="#CF304A"
                points={`${marker.index},135 ${marker.index - 5},123 ${marker.index + 5},123`}
              />
              <text
                x={marker.index}
                y="114"
                fill="#CF304A"
                fontFamily="JetBrains Mono, monospace"
                fontSize="10"
                textAnchor="middle"
                fontWeight="bold"
              >
                SELL
              </text>
            </g>
          ))}
        </svg>

        {/* Y-Axis Labels Right Rail */}
        <div className="absolute right-0 top-8 bottom-0 w-12 bg-[#1E2329] border-l border-[#323538] flex flex-col justify-between py-4 opacity-80 pointer-events-none select-none">
          {chartData.priceLabels.map((lbl, i) => (
            <span key={i} className="font-mono text-[10px] text-[#bbcabd] text-right pr-1">
              {lbl}
            </span>
          ))}
        </div>
      </div>
    </div>
  );
};

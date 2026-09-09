"use client";

import { useEffect, useMemo, useState } from "react";
import type { ApiClient } from "@/src/foundation/http/contracts";
import { listCandles } from "@/src/features/market/api/market-api";
import type { Candle } from "@/src/features/market/model/candle";
import { isMarketPair, isMarketTimeframe } from "@/src/features/market/model/market-catalog";
import { candidateComponents } from "@/src/features/experiments/components/candidate-presentation";
import type { BacktestResultViewModel, TradeViewModel } from "../types/backtest-result";

const MAX_CANDLES_TO_LOAD = 1_000;
const DEFAULT_VISIBLE_CANDLES = 60;
const VISIBLE_CANDLE_OPTIONS = [40, 60, 100] as const;
const CHART_WIDTH = 1_000;
const PRICE_TOP = 20;
const PRICE_BOTTOM = 300;
const RSI_TOP = 338;
const RSI_BOTTOM = 418;

type StrategyComponent = Readonly<{
  strategyId: string;
  parameters: Readonly<Record<string, unknown>>;
}>;

type ChartSeries = Readonly<{
  key: string;
  label: string;
  color: string;
  values: readonly (number | null)[];
  zone?: boolean;
}>;

export type BacktestChartModel = Readonly<{
  priceSeries: readonly ChartSeries[];
  rsiSeries: readonly ChartSeries[];
  rsiThresholds: readonly Readonly<{ label: string; value: number; color: string }>[];
  unsupportedStrategies: readonly string[];
}>;

export function BacktestStrategyChart({
  api,
  result
}: {
  api: ApiClient;
  result: BacktestResultViewModel;
}) {
  const dataset = result.provenance.dataset;
  const compatibilityError = !dataset
    ? "This legacy result does not contain dataset evidence for chart reconstruction."
    : !isMarketPair(dataset.tradingPair) || !isMarketTimeframe(dataset.timeframe)
      ? "The result uses a pair or timeframe that this chart does not support yet."
      : undefined;
  const [candles, setCandles] = useState<readonly Candle[]>([]);
  const [loading, setLoading] = useState(Boolean(dataset && !compatibilityError));
  const [error, setError] = useState<string>();

  useEffect(() => {
    let cancelled = false;
    if (
      !dataset ||
      compatibilityError ||
      !isMarketPair(dataset.tradingPair) ||
      !isMarketTimeframe(dataset.timeframe)
    ) {
      return () => {
        cancelled = true;
      };
    }
    const pair = dataset.tradingPair;
    const timeframe = dataset.timeframe;

    void (async () => {
      const loaded: Candle[] = [];
      let cursor: string | undefined;
      do {
        const response = await listCandles(api, {
          pair,
          timeframe,
          startTime: dataset.rangeStart,
          endTime: dataset.rangeEnd,
          limit: 200,
          cursor
        });
        if (!response.ok) {
          if (!cancelled) {
            setError(
              response.error.retryable
                ? "Historical candles are temporarily unavailable. Please retry shortly."
                : "Unable to load historical candles for this result."
            );
            setLoading(false);
          }
          return;
        }
        loaded.push(...response.data.items);
        cursor = response.data.hasMore ? (response.data.nextCursor ?? undefined) : undefined;
      } while (cursor && loaded.length < MAX_CANDLES_TO_LOAD);

      if (!cancelled) {
        setCandles(loaded.slice(0, MAX_CANDLES_TO_LOAD));
        setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [api, compatibilityError, dataset]);

  const components = useMemo(() => strategyComponents(result), [result]);
  const model = useMemo(() => buildBacktestChartModel(candles, components), [candles, components]);

  return (
    <section className="panel backtest-strategy-chart" aria-labelledby="strategy-chart-heading">
      <div className="section-heading">
        <div>
          <p className="eyebrow">Strategy execution chart</p>
          <h2 id="strategy-chart-heading">Candles, indicators and BUY/SELL</h2>
        </div>
        {dataset && (
          <span>
            {dataset.tradingPair} · {dataset.timeframe} · dataset window
          </span>
        )}
      </div>

      {loading && <p role="status">Loading historical candles for this backtest…</p>}
      {(compatibilityError ?? error) && (
        <div className="inline-feedback" role="status">
          {compatibilityError ?? error}
        </div>
      )}
      {!loading && !compatibilityError && !error && candles.length === 0 && (
        <p className="empty-copy">No candles are available in this dataset window.</p>
      )}
      {!loading && candles.length > 0 && (
        <BacktestChart
          candles={candles}
          trades={result.trades}
          model={model}
          label={`${dataset?.tradingPair ?? "Market"} backtest execution`}
        />
      )}

      <div className="backtest-chart-legend" aria-label="Chart legend">
        <Legend color="#53df9a" label="BUY · executed entry" marker />
        <Legend color="#ff6f78" label="SELL · executed exit" marker />
        {model.priceSeries.map((item) => (
          <Legend key={item.key} color={item.color} label={item.label} />
        ))}
        {model.rsiSeries.map((item) => (
          <Legend key={item.key} color={item.color} label={item.label} />
        ))}
      </div>
      {model.unsupportedStrategies.length > 0 && (
        <p className="muted backtest-chart-note">
          No price overlay is available for: {model.unsupportedStrategies.join(", ")}. Executed
          BUY/SELL markers are still shown.
        </p>
      )}
      <p className="muted backtest-chart-note">
        Indicators are reconstructed from the candidate parameters. BUY and SELL markers come from
        the recorded trade entries and exits.
      </p>
    </section>
  );
}

function BacktestChart({
  candles,
  trades,
  model,
  label
}: {
  candles: readonly Candle[];
  trades: readonly TradeViewModel[];
  model: BacktestChartModel;
  label: string;
}) {
  const [visibleCount, setVisibleCount] = useState(DEFAULT_VISIBLE_CANDLES);
  const [hoveredOpenTime, setHoveredOpenTime] = useState<string>();
  const [windowEnd, setWindowEnd] = useState(candles.length);
  const end = Math.min(windowEnd, candles.length);
  const start = Math.max(0, end - visibleCount);
  const visible = candles.slice(start, end);

  const priceSeries = model.priceSeries.map((item) => ({
    ...item,
    values: item.values.slice(start, end)
  }));
  const rsiSeries = model.rsiSeries.map((item) => ({
    ...item,
    values: item.values.slice(start, end)
  }));
  const priceValues = [
    ...visible.flatMap((candle) => [Number(candle.low), Number(candle.high)]),
    ...priceSeries.flatMap((item) => item.values.filter(isNumber))
  ];
  const minimum = Math.min(...priceValues);
  const maximum = Math.max(...priceValues);
  const span = maximum - minimum || 1;
  const step = CHART_WIDTH / Math.max(visible.length, 1);
  const candleWidth = Math.max(1.5, Math.min(7, step * 0.52));
  const x = (index: number) => index * step + step / 2;
  const priceY = (value: number) =>
    PRICE_TOP + ((maximum - value) / span) * (PRICE_BOTTOM - PRICE_TOP);
  const rsiY = (value: number) => RSI_BOTTOM - (value / 100) * (RSI_BOTTOM - RSI_TOP);
  const markers = tradeMarkers(visible, trades);
  const hoveredIndex = visible.findIndex((candle) => candle.openTime === hoveredOpenTime);
  const selectedIndex = hoveredIndex >= 0 ? hoveredIndex : visible.length - 1;
  const selected = visible[selectedIndex];
  const selectedSeries = [...priceSeries, ...rsiSeries].flatMap((item) => {
    const value = item.values[selectedIndex];
    return isNumber(value) ? [{ key: item.key, label: item.label, color: item.color, value }] : [];
  });

  return (
    <div className="backtest-chart-shell">
      <div className="backtest-chart-toolbar">
        <span>Số nến hiển thị</span>
        <div className="backtest-chart-range" aria-label="Chọn số nến hiển thị">
          {VISIBLE_CANDLE_OPTIONS.map((option) => (
            <button
              className={visibleCount === option ? "active" : undefined}
              key={option}
              type="button"
              onClick={() => setVisibleCount(option)}
            >
              {option}
            </button>
          ))}
        </div>
        <div className="backtest-chart-navigation" aria-label="Di chuyển trên lịch sử nến">
          <button
            type="button"
            disabled={start === 0}
            onClick={() => {
              setWindowEnd(start);
              setHoveredOpenTime(undefined);
            }}
          >
            ← Cũ hơn
          </button>
          <span>
            Nến {start + 1}–{end} / {candles.length}
          </span>
          <button
            type="button"
            disabled={end === candles.length}
            onClick={() => {
              setWindowEnd(Math.min(candles.length, end + visibleCount));
              setHoveredOpenTime(undefined);
            }}
          >
            Mới hơn →
          </button>
        </div>
        <span className="backtest-chart-hover-hint">Rê chuột vào nến để xem chi tiết</span>
      </div>

      {selected && (
        <div className="backtest-chart-inspector" aria-live="polite">
          <strong>
            {new Date(selected.openTime).toLocaleString("vi-VN", { timeZone: "UTC" })} UTC
            {hoveredIndex < 0 ? " · Nến mới nhất" : ""}
          </strong>
          <span>O {formatChartNumber(selected.open)}</span>
          <span>H {formatChartNumber(selected.high)}</span>
          <span>L {formatChartNumber(selected.low)}</span>
          <span>C {formatChartNumber(selected.close)}</span>
          <span>V {formatChartNumber(selected.volume)}</span>
          {selectedSeries.map((item) => (
            <span className="backtest-chart-inspector-series" key={item.key}>
              <i style={{ backgroundColor: item.color }} aria-hidden="true" />
              {item.label} {formatChartNumber(item.value)}
            </span>
          ))}
        </div>
      )}

      <div
        className="backtest-chart-scroll"
        tabIndex={0}
        role="region"
        aria-label="Scrollable chart"
        onMouseLeave={() => setHoveredOpenTime(undefined)}
      >
        <svg viewBox={`0 0 ${CHART_WIDTH} 440`} role="img" aria-label={label}>
          <title>{label}</title>
          <g className="backtest-chart-grid" aria-hidden="true">
            {[PRICE_TOP, 90, 160, 230, PRICE_BOTTOM].map((value) => (
              <line key={value} x1="0" x2={CHART_WIDTH} y1={value} y2={value} />
            ))}
          </g>
          {visible.map((candle, index) => {
            const open = Number(candle.open);
            const close = Number(candle.close);
            const rising = close >= open;
            return (
              <g
                className={`${rising ? "backtest-candle-up" : "backtest-candle-down"}${
                  candle.openTime === hoveredOpenTime ? " backtest-candle-active" : ""
                }`}
                key={candle.openTime}
              >
                <line
                  x1={x(index)}
                  x2={x(index)}
                  y1={priceY(Number(candle.high))}
                  y2={priceY(Number(candle.low))}
                />
                <rect
                  x={x(index) - candleWidth / 2}
                  y={Math.min(priceY(open), priceY(close))}
                  width={candleWidth}
                  height={Math.max(1.5, Math.abs(priceY(open) - priceY(close)))}
                />
              </g>
            );
          })}
          {priceSeries.map((item) => (
            <g key={item.key}>
              {item.zone && (
                <path
                  d={linePath(item.values, x, priceY)}
                  fill="none"
                  stroke={item.color}
                  strokeWidth="11"
                  opacity="0.12"
                />
              )}
              <path
                d={linePath(item.values, x, priceY)}
                fill="none"
                stroke={item.color}
                strokeWidth="2"
                strokeDasharray={item.zone ? "8 5" : undefined}
              />
            </g>
          ))}
          {markers.map((marker) => (
            <g
              className={`backtest-trade-marker marker-${marker.kind.toLowerCase()}`}
              key={`${marker.tradeId}-${marker.kind}`}
              transform={`translate(${x(marker.index)} ${priceY(marker.price)})`}
            >
              <circle r="9" />
              <text textAnchor="middle" dy="3.5">
                {marker.kind === "BUY" ? "B" : "S"}
              </text>
              <title>
                {marker.kind} · {new Date(marker.time).toLocaleString("en-US", { timeZone: "UTC" })}{" "}
                UTC · {marker.price}
              </title>
            </g>
          ))}
          {rsiSeries.length > 0 && (
            <g className="backtest-rsi-panel">
              <rect x="0" y={RSI_TOP} width={CHART_WIDTH} height={RSI_BOTTOM - RSI_TOP} />
              {model.rsiThresholds.map((threshold) => (
                <g key={`${threshold.label}-${threshold.value}`}>
                  <line
                    x1="0"
                    x2={CHART_WIDTH}
                    y1={rsiY(threshold.value)}
                    y2={rsiY(threshold.value)}
                    stroke={threshold.color}
                    strokeDasharray="6 5"
                    opacity="0.65"
                  />
                  <text x="6" y={rsiY(threshold.value) - 4} fill={threshold.color}>
                    {threshold.label} {threshold.value}
                  </text>
                </g>
              ))}
              {rsiSeries.map((item) => (
                <path
                  key={item.key}
                  d={linePath(item.values, x, rsiY)}
                  fill="none"
                  stroke={item.color}
                  strokeWidth="2"
                />
              ))}
            </g>
          )}
          {hoveredIndex >= 0 && (
            <line
              className="backtest-chart-crosshair"
              x1={x(hoveredIndex)}
              x2={x(hoveredIndex)}
              y1="0"
              y2="440"
            />
          )}
          {visible.map((candle, index) => (
            <rect
              className="backtest-chart-hitbox"
              key={`hitbox-${candle.openTime}`}
              x={index * step}
              y="0"
              width={step}
              height="440"
              onMouseEnter={() => setHoveredOpenTime(candle.openTime)}
              aria-label={`Nến ${new Date(candle.openTime).toLocaleString("vi-VN", {
                timeZone: "UTC"
              })} UTC`}
            />
          ))}
        </svg>
      </div>
    </div>
  );
}

function Legend({
  color,
  label,
  marker = false
}: {
  color: string;
  label: string;
  marker?: boolean;
}) {
  return (
    <span>
      <i
        className={marker ? "backtest-legend-marker" : "backtest-legend-line"}
        style={{ backgroundColor: color }}
        aria-hidden="true"
      />
      {label}
    </span>
  );
}

export function buildBacktestChartModel(
  candles: readonly Candle[],
  components: readonly StrategyComponent[]
): BacktestChartModel {
  const closes = candles.map((candle) => Number(candle.close));
  const priceSeries: ChartSeries[] = [];
  const rsiSeries: ChartSeries[] = [];
  const rsiThresholds: Array<{ label: string; value: number; color: string }> = [];
  const unsupportedStrategies: string[] = [];

  components.forEach((component, index) => {
    const prefix = components.length > 1 ? `${strategyLabel(component.strategyId)} · ` : "";
    if (
      component.strategyId === "ma-crossover" ||
      component.strategyId === "moving-average-crossover"
    ) {
      const fast = parameterNumber(component.parameters, "fastPeriod", 5);
      const slow = parameterNumber(component.parameters, "slowPeriod", 20);
      priceSeries.push(
        series(
          `${index}-ma-fast`,
          `${prefix}MA ${fast}`,
          "#56b4ff",
          simpleMovingAverage(closes, fast)
        ),
        series(
          `${index}-ma-slow`,
          `${prefix}MA ${slow}`,
          "#ffb454",
          simpleMovingAverage(closes, slow)
        )
      );
      return;
    }
    if (component.strategyId === "bollinger-bands") {
      const period = parameterNumber(component.parameters, "period", 20);
      const multiplier = parameterNumber(component.parameters, "standardDeviation", 2);
      const bands = bollingerBands(closes, period, multiplier);
      priceSeries.push(
        series(`${index}-bb-upper`, `${prefix}Bollinger upper`, "#a98bff", bands.upper),
        series(`${index}-bb-middle`, `${prefix}Bollinger middle`, "#56b4ff", bands.middle),
        series(`${index}-bb-lower`, `${prefix}Bollinger lower`, "#a98bff", bands.lower)
      );
      return;
    }
    if (component.strategyId === "support-resistance") {
      const lookback = parameterNumber(component.parameters, "lookback", 20);
      const levels = supportResistance(closes, lookback);
      priceSeries.push(
        {
          ...series(`${index}-support`, `${prefix}Support zone`, "#53df9a", levels.support),
          zone: true
        },
        {
          ...series(
            `${index}-resistance`,
            `${prefix}Resistance zone`,
            "#ff6f78",
            levels.resistance
          ),
          zone: true
        }
      );
      return;
    }
    if (component.strategyId === "rsi-threshold" || component.strategyId === "rsi") {
      const period = parameterNumber(component.parameters, "period", 14);
      const buy = parameterNumber(component.parameters, "buyThreshold", 30);
      const sell = parameterNumber(component.parameters, "sellThreshold", 70);
      rsiSeries.push(
        series(
          `${index}-rsi`,
          `${prefix}RSI ${period}`,
          "#f4d35e",
          relativeStrengthIndex(closes, period)
        )
      );
      rsiThresholds.push(
        { label: "BUY", value: buy, color: "#53df9a" },
        { label: "SELL", value: sell, color: "#ff6f78" }
      );
      return;
    }
    unsupportedStrategies.push(strategyLabel(component.strategyId));
  });

  return { priceSeries, rsiSeries, rsiThresholds, unsupportedStrategies };
}

function strategyComponents(result: BacktestResultViewModel): readonly StrategyComponent[] {
  const definition = result.provenance.candidate?.definition;
  if (definition) {
    const resolved = candidateComponents(definition)
      .filter((component) => typeof component.strategyId === "string")
      .map((component) => ({
        strategyId: String(component.strategyId),
        parameters:
          component.parameters && typeof component.parameters === "object"
            ? (component.parameters as Record<string, unknown>)
            : {}
      }));
    if (resolved.length > 0) return resolved;
  }

  const evidence = result.provenance.strategy;
  if (!evidence) return [];
  if (evidence.singleStrategy) {
    return [
      {
        strategyId: evidence.singleStrategy.pluginId,
        parameters: definition ? { ...evidence.parameters, ...definition } : evidence.parameters
      }
    ];
  }
  return evidence.components.map((component) => ({
    strategyId: component.strategy.pluginId,
    parameters: component.parameters
  }));
}

function series(
  key: string,
  label: string,
  color: string,
  values: readonly (number | null)[]
): ChartSeries {
  return { key, label, color, values };
}

function parameterNumber(
  parameters: Readonly<Record<string, unknown>>,
  name: string,
  fallback: number
) {
  const raw = parameters[name];
  const value =
    raw && typeof raw === "object" && "value" in raw ? (raw as { value: unknown }).value : raw;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : fallback;
}

function simpleMovingAverage(values: readonly number[], period: number) {
  return values.map((_, index) => {
    if (index + 1 < period) return null;
    const window = values.slice(index + 1 - period, index + 1);
    return window.reduce((sum, value) => sum + value, 0) / period;
  });
}

function bollingerBands(values: readonly number[], period: number, multiplier: number) {
  const middle = simpleMovingAverage(values, period);
  const upper: Array<number | null> = [];
  const lower: Array<number | null> = [];
  values.forEach((_, index) => {
    const mean = middle[index];
    if (mean === null || index + 1 < period) {
      upper.push(null);
      lower.push(null);
      return;
    }
    const window = values.slice(index + 1 - period, index + 1);
    const deviation = Math.sqrt(
      window.reduce((sum, value) => sum + (value - mean) ** 2, 0) / period
    );
    upper.push(mean + deviation * multiplier);
    lower.push(mean - deviation * multiplier);
  });
  return { middle, upper, lower };
}

function supportResistance(values: readonly number[], lookback: number) {
  const support: Array<number | null> = [];
  const resistance: Array<number | null> = [];
  values.forEach((_, index) => {
    if (index < lookback) {
      support.push(null);
      resistance.push(null);
      return;
    }
    const historical = values.slice(index - lookback, index);
    support.push(Math.min(...historical));
    resistance.push(Math.max(...historical));
  });
  return { support, resistance };
}

function relativeStrengthIndex(values: readonly number[], period: number) {
  return values.map((_, index) => {
    if (index < period) return null;
    let gains = 0;
    let losses = 0;
    for (let cursor = index - period + 1; cursor <= index; cursor++) {
      const change = values[cursor] - values[cursor - 1];
      if (change > 0) gains += change;
      else if (change < 0) losses += Math.abs(change);
    }
    if (gains === 0 && losses === 0) return 50;
    if (losses === 0) return 100;
    if (gains === 0) return 0;
    return 100 - 100 / (1 + gains / losses);
  });
}

function linePath(
  values: readonly (number | null)[],
  x: (index: number) => number,
  y: (value: number) => number
) {
  let drawing = false;
  return values
    .map((value, index) => {
      if (!isNumber(value)) {
        drawing = false;
        return "";
      }
      const command = drawing ? "L" : "M";
      drawing = true;
      return `${command}${x(index).toFixed(2)},${y(value).toFixed(2)}`;
    })
    .filter(Boolean)
    .join(" ");
}

function tradeMarkers(candles: readonly Candle[], trades: readonly TradeViewModel[]) {
  const marker = (trade: TradeViewModel, kind: "BUY" | "SELL", time: string, price: string) => {
    const timestamp = Date.parse(time);
    const index = candles.findIndex(
      (candle) =>
        timestamp >= Date.parse(candle.openTime) && timestamp <= Date.parse(candle.closeTime)
    );
    if (index < 0) return undefined;
    return { tradeId: trade.tradeId, kind, time, price: Number(price), index } as const;
  };
  const markers: Array<NonNullable<ReturnType<typeof marker>>> = [];
  trades.forEach((trade) => {
    const entry = marker(trade, "BUY", trade.entryTime, trade.entryPrice);
    const exit = marker(trade, "SELL", trade.exitTime, trade.exitPrice);
    if (entry && Number.isFinite(entry.price)) markers.push(entry);
    if (exit && Number.isFinite(exit.price)) markers.push(exit);
  });
  return markers;
}

function strategyLabel(strategyId: string) {
  return strategyId
    .replaceAll("-", " ")
    .replaceAll("_", " ")
    .replace(/\b\w/g, (letter) => letter.toUpperCase())
    .replace(/^Ma\b/, "MA")
    .replace(/^Rsi\b/, "RSI");
}

function isNumber(value: number | null): value is number {
  return value !== null && Number.isFinite(value);
}

function formatChartNumber(value: string | number) {
  const parsed = Number(value);
  if (!Number.isFinite(parsed)) return String(value);
  return new Intl.NumberFormat("en-US", { maximumFractionDigits: 4 }).format(parsed);
}

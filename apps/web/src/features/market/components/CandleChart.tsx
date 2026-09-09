"use client";

import { useState } from "react";
import type { Candle } from "../model/candle";

export function CandleChart({ candles, label }: { candles: readonly Candle[]; label: string }) {
  const [hoveredOpenTime, setHoveredOpenTime] = useState<string>();

  if (!candles.length)
    return (
      <div className="market-empty" role="status">
        No candle data available.
      </div>
    );
  const visible = candles.slice(-60),
    values = visible.flatMap((c) => [Number(c.high), Number(c.low)]),
    min = Math.min(...values),
    max = Math.max(...values),
    span = max - min || 1;
  const y = (v: string) => 12 + ((max - Number(v)) / span) * 136;
  const width = 600 / visible.length;
  const latest = visible.at(-1)!;
  const hoveredIndex = visible.findIndex((candle) => candle.openTime === hoveredOpenTime);
  const hovered = hoveredIndex >= 0 ? visible[hoveredIndex] : undefined;
  const selected = hovered ?? latest;

  return (
    <figure className="candle-chart">
      <div className="candle-chart-plot" onMouseLeave={() => setHoveredOpenTime(undefined)}>
        <div className="candle-tooltip-slot" aria-live="polite">
          <div className="candle-tooltip">
            <strong>
              {new Date(selected.openTime).toLocaleString("en-US", { timeZone: "UTC" })} UTC
              {!hovered ? " · Latest candle" : ""}
            </strong>
            <span>O {selected.open}</span>
            <span>H {selected.high}</span>
            <span>L {selected.low}</span>
            <span>C {selected.close}</span>
            <span>V {selected.volume}</span>
          </div>
        </div>
        <svg viewBox="0 0 600 160" role="img" aria-label={`${label} candle chart`}>
          {hoveredIndex >= 0 ? (
            <line
              className="candle-crosshair"
              x1={hoveredIndex * width + width / 2}
              x2={hoveredIndex * width + width / 2}
              y1="0"
              y2="160"
            />
          ) : null}
          {visible.map((c, i) => {
            const x = i * width + width / 2,
              up = Number(c.close) >= Number(c.open),
              active = c.openTime === hoveredOpenTime;
            return (
              <g
                key={c.openTime}
                className={`${up ? "candle-up" : "candle-down"}${active ? " candle-active" : ""}`}
                onMouseEnter={() => setHoveredOpenTime(c.openTime)}
              >
                <line x1={x} x2={x} y1={y(c.high)} y2={y(c.low)} />
                <rect
                  x={x - Math.max(1, width * 0.25)}
                  width={Math.max(2, width * 0.5)}
                  y={Math.min(y(c.open), y(c.close))}
                  height={Math.max(2, Math.abs(y(c.open) - y(c.close)))}
                />
                <rect className="candle-hitbox" x={i * width} y="0" width={width} height="160" />
              </g>
            );
          })}
        </svg>
      </div>
    </figure>
  );
}

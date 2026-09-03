import type { Candle } from "../model/candle";
export function CandleChart({ candles, label }: { candles: readonly Candle[]; label: string }) {
  if (!candles.length)
    return (
      <div className="market-empty" role="status">
        Chưa có dữ liệu Candle.
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
  return (
    <figure className="candle-chart">
      <svg viewBox="0 0 600 160" role="img" aria-label={`Biểu đồ Candle ${label}`}>
        {visible.map((c, i) => {
          const x = i * width + width / 2,
            up = Number(c.close) >= Number(c.open);
          return (
            <g key={c.openTime} className={up ? "candle-up" : "candle-down"}>
              <line x1={x} x2={x} y1={y(c.high)} y2={y(c.low)} />
              <rect
                x={x - Math.max(1, width * 0.25)}
                width={Math.max(2, width * 0.5)}
                y={Math.min(y(c.open), y(c.close))}
                height={Math.max(2, Math.abs(y(c.open) - y(c.close)))}
              />
            </g>
          );
        })}
      </svg>
      <figcaption>
        <span>{new Date(latest.openTime).toLocaleString("vi-VN", { timeZone: "UTC" })} UTC</span>
        <span>
          O {latest.open} · H {latest.high} · L {latest.low} · C {latest.close} · V {latest.volume}
        </span>
      </figcaption>
    </figure>
  );
}

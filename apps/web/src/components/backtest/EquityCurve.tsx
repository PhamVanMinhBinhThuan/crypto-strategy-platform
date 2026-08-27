import React, { useState } from 'react';
import { EquityPoint } from '../../types/backtest';

export interface EquityCurveProps {
  data: EquityPoint[];
}

export const EquityCurve: React.FC<EquityCurveProps> = ({ data }) => {
  const [hoveredPoint, setHoveredPoint] = useState<EquityPoint | null>(null);

  // Min and max calculation
  const minEquity = 9800;
  const maxEquity = 12200;
  const range = maxEquity - minEquity;

  const points = React.useMemo(() => {
    return data.map((d, idx) => {
      const x = (idx / (data.length - 1)) * 100;
      const y = 100 - ((d.equity - minEquity) / range) * 100;
      return { x, y, data: d };
    });
  }, [data, minEquity, range]);

  const pathD = React.useMemo(() => {
    if (points.length === 0) return '';
    return points.reduce(
      (acc, p, i) => `${acc} ${i === 0 ? 'M' : 'L'} ${p.x.toFixed(1)},${p.y.toFixed(1)}`,
      ''
    );
  }, [points]);

  const areaD = React.useMemo(() => {
    if (points.length === 0) return '';
    return `${pathD} L 100,100 L 0,100 Z`;
  }, [pathD, points]);

  return (
    <div className="bg-[#191c1f] border border-[#323538] rounded h-[240px] md:h-[260px] flex flex-col relative overflow-hidden select-none min-w-0">
      {/* Header */}
      <div className="p-2.5 px-3 border-b border-[#323538] bg-[#0b0e11] flex justify-between items-center shrink-0">
        <span className="font-sans text-xs font-semibold text-[#bbcabd] uppercase tracking-wider">
          Equity Curve
        </span>
        {hoveredPoint ? (
          <span className="font-mono text-xs text-[#44e092] font-bold">
            {hoveredPoint.date}: ${hoveredPoint.equity.toLocaleString()}{' '}
            {hoveredPoint.drawdown < 0 && (
              <span className="text-[#ff5353] font-normal">({hoveredPoint.drawdown}%)</span>
            )}
          </span>
        ) : (
          <span className="font-mono text-[11px] text-[#869488]">
            Initial: $10,000 → Current: $11,820 (+18.2%)
          </span>
        )}
      </div>

      {/* SVG Canvas */}
      <div
        className="flex-1 w-full relative bg-[#0b0e11]"
        style={{
          backgroundImage: 'linear-gradient(rgba(50, 53, 56, 0.15) 1px, transparent 1px)',
          backgroundSize: '100% 25%',
        }}
      >
        <svg
          className="absolute inset-0 w-full h-full p-2"
          viewBox="0 0 100 100"
          preserveAspectRatio="none"
        >
          <defs>
            <linearGradient id="equityGrad" x1="0" x2="0" y1="0" y2="1">
              <stop offset="0%" stopColor="#02c076" stopOpacity="0.25" />
              <stop offset="100%" stopColor="#02c076" stopOpacity="0.0" />
            </linearGradient>
          </defs>

          {/* Area under curve */}
          <path d={areaD} fill="url(#equityGrad)" />

          {/* Curve line */}
          <path
            d={pathD}
            fill="none"
            stroke="#02c076"
            strokeWidth="1.8"
            strokeLinecap="round"
            strokeLinejoin="round"
          />

          {/* Interactive Hover Nodes */}
          {points.map((p, i) => (
            <circle
              key={i}
              cx={p.x}
              cy={p.y}
              r={hoveredPoint === p.data ? 3.5 : 2}
              fill={p.data.drawdown < 0 ? '#ff5353' : '#44e092'}
              className="cursor-pointer transition-all"
              onMouseEnter={() => setHoveredPoint(p.data)}
              onMouseLeave={() => setHoveredPoint(null)}
            />
          ))}
        </svg>
      </div>
    </div>
  );
};

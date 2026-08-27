import React, { useState } from 'react';
import { Maximize2, Minimize2 } from 'lucide-react';
import { SentimentTrendPoint, NewsTimeRange } from '../../types/news';
import { cn } from '../../utils/cn';

export interface SentimentTrendChartProps {
  trendPoints: SentimentTrendPoint[];
  timeRange: NewsTimeRange;
}

export const SentimentTrendChart: React.FC<SentimentTrendChartProps> = ({
  trendPoints,
  timeRange,
}) => {
  const [hoveredIndex, setHoveredIndex] = useState<number | null>(null);
  const [isExpanded, setIsExpanded] = useState<boolean>(false);

  // SVG dimensions & coordinate calculations
  const svgWidth = 800;
  const svgHeight = isExpanded ? 320 : 240;
  const paddingLeft = 55;
  const paddingRight = 30;
  const paddingTop = 25;
  const paddingBottom = 30;

  const chartWidth = svgWidth - paddingLeft - paddingRight;
  const chartHeight = svgHeight - paddingTop - paddingBottom;
  const zeroY = paddingTop + chartHeight / 2; // y at score = 0.0

  // Convert score (-1.0 to 1.0) to SVG y coordinate
  const getY = (score: number) => {
    // clamped between -1 and 1
    const clamped = Math.max(-1, Math.min(1, score));
    // score 1 -> paddingTop, score -1 -> paddingTop + chartHeight
    return paddingTop + ((1 - clamped) / 2) * chartHeight;
  };

  // Convert index to SVG x coordinate
  const getX = (index: number) => {
    if (trendPoints.length <= 1) return paddingLeft + chartWidth / 2;
    return paddingLeft + (index / (trendPoints.length - 1)) * chartWidth;
  };

  // Build SVG smooth path command using Catmull-Rom or cubic Bezier
  const coordinates = trendPoints.map((pt, idx) => ({
    x: getX(idx),
    y: getY(pt.score),
    pt,
  }));

  const buildPath = () => {
    if (coordinates.length === 0) return '';
    if (coordinates.length === 1) return `M ${coordinates[0].x} ${coordinates[0].y}`;

    let path = `M ${coordinates[0].x} ${coordinates[0].y}`;
    for (let i = 0; i < coordinates.length - 1; i++) {
      const p0 = coordinates[i === 0 ? 0 : i - 1];
      const p1 = coordinates[i];
      const p2 = coordinates[i + 1];
      const p3 = coordinates[i + 2] || p2;

      const cp1x = p1.x + (p2.x - p0.x) / 6;
      const cp1y = p1.y + (p2.y - p0.y) / 6;
      const cp2x = p2.x - (p3.x - p1.x) / 6;
      const cp2y = p2.y - (p3.y - p1.y) / 6;

      path += ` C ${cp1x} ${cp1y}, ${cp2x} ${cp2y}, ${p2.x} ${p2.y}`;
    }
    return path;
  };

  const linePath = buildPath();
  const areaPath =
    coordinates.length > 0
      ? `${linePath} L ${coordinates[coordinates.length - 1].x} ${zeroY} L ${coordinates[0].x} ${zeroY} Z`
      : '';

  const hoveredPoint = hoveredIndex !== null ? trendPoints[hoveredIndex] : null;

  return (
    <div className="p-4 rounded-lg flex flex-col gap-3 bg-[#191c1f] border border-[#2B3139] select-none">
      <div className="flex justify-between items-center">
        <h3 className="font-sans text-[11px] font-bold tracking-wider text-[#869488] uppercase">
          SENTIMENT SCORE TREND ({timeRange})
        </h3>
        <button
          type="button"
          onClick={() => setIsExpanded(!isExpanded)}
          className="text-[#869488] hover:text-[#e1e2e7] transition-colors p-1 rounded hover:bg-[#2B3139] cursor-pointer"
          title={isExpanded ? 'Collapse Chart' : 'Expand Chart'}
        >
          {isExpanded ? (
            <Minimize2 className="w-3.5 h-3.5" />
          ) : (
            <Maximize2 className="w-3.5 h-3.5" />
          )}
        </button>
      </div>

      <div
        className={cn(
          'relative w-full rounded bg-[#161a1f] border border-[#2B3139] transition-all overflow-hidden',
          isExpanded ? 'h-[320px]' : 'h-[240px]'
        )}
      >
        <svg
          className="w-full h-full"
          preserveAspectRatio="none"
          viewBox={`0 0 ${svgWidth} ${svgHeight}`}
        >
          <defs>
            <linearGradient id="sentimentTrendGrad" x1="0" x2="0" y1="0" y2="1">
              <stop offset="0%" stopColor="#02C076" stopOpacity="0.25" />
              <stop offset="100%" stopColor="#02C076" stopOpacity="0.0" />
            </linearGradient>
          </defs>

          {/* Horizontal Grid lines */}
          {/* +1.0 */}
          <line
            className="stroke-[#2B3139] stroke-1 [stroke-dasharray:4_4]"
            x1={paddingLeft}
            x2={svgWidth - paddingRight}
            y1={getY(1.0)}
            y2={getY(1.0)}
          />
          {/* +0.5 */}
          <line
            className="stroke-[#2B3139] stroke-1 [stroke-dasharray:4_4]"
            x1={paddingLeft}
            x2={svgWidth - paddingRight}
            y1={getY(0.5)}
            y2={getY(0.5)}
          />
          {/* 0.0 Zero Line (Solid) */}
          <line
            stroke="#4a5568"
            strokeWidth="1.5"
            x1={paddingLeft}
            x2={svgWidth - paddingRight}
            y1={zeroY}
            y2={zeroY}
          />
          {/* -0.5 */}
          <line
            className="stroke-[#2B3139] stroke-1 [stroke-dasharray:4_4]"
            x1={paddingLeft}
            x2={svgWidth - paddingRight}
            y1={getY(-0.5)}
            y2={getY(-0.5)}
          />
          {/* -1.0 */}
          <line
            className="stroke-[#2B3139] stroke-1 [stroke-dasharray:4_4]"
            x1={paddingLeft}
            x2={svgWidth - paddingRight}
            y1={getY(-1.0)}
            y2={getY(-1.0)}
          />

          {/* Y Axis Labels */}
          <text
            className="fill-[#848E9C] font-mono text-[10px]"
            textAnchor="end"
            x={paddingLeft - 10}
            y={getY(1.0) + 3}
          >
            +1.0
          </text>
          <text
            className="fill-[#848E9C] font-mono text-[10px]"
            textAnchor="end"
            x={paddingLeft - 10}
            y={getY(0.5) + 3}
          >
            +0.5
          </text>
          <text
            className="fill-[#848E9C] font-mono text-[10px]"
            textAnchor="end"
            x={paddingLeft - 10}
            y={zeroY + 3}
          >
            0.0
          </text>
          <text
            className="fill-[#848E9C] font-mono text-[10px]"
            textAnchor="end"
            x={paddingLeft - 10}
            y={getY(-0.5) + 3}
          >
            -0.5
          </text>
          <text
            className="fill-[#848E9C] font-mono text-[10px]"
            textAnchor="end"
            x={paddingLeft - 10}
            y={getY(-1.0) + 3}
          >
            -1.0
          </text>

          {/* Area Fill */}
          <path d={areaPath} fill="url(#sentimentTrendGrad)" />

          {/* Smooth Trend Line */}
          <path
            d={linePath}
            fill="none"
            stroke="#02C076"
            strokeWidth="2.2"
            strokeLinecap="round"
            strokeLinejoin="round"
          />

          {/* Event Markers & Data Nodes */}
          {coordinates.map((coord, idx) => {
            const hasMarker = !!coord.pt.eventMarker;
            const isMarkerPositive = coord.pt.eventMarker?.sentiment === 'POSITIVE';
            const isMarkerNegative = coord.pt.eventMarker?.sentiment === 'NEGATIVE';
            const isHovered = hoveredIndex === idx;

            return (
              <g
                key={idx}
                className="cursor-pointer transition-transform"
                onMouseEnter={() => setHoveredIndex(idx)}
                onMouseLeave={() => setHoveredIndex(null)}
              >
                {/* Click / Hover Target */}
                <circle cx={coord.x} cy={coord.y} r={12} fill="transparent" />

                {/* Event Marker Node */}
                {hasMarker ? (
                  <circle
                    cx={coord.x}
                    cy={coord.y}
                    r={isHovered ? 6 : 4.5}
                    fill="#0B0E11"
                    stroke={
                      isMarkerPositive
                        ? '#02C076'
                        : isMarkerNegative
                        ? '#f84b4b'
                        : '#848E9C'
                    }
                    strokeWidth={isHovered ? 2.5 : 2}
                  />
                ) : isHovered ? (
                  <circle
                    cx={coord.x}
                    cy={coord.y}
                    r={4}
                    fill="#02C076"
                    stroke="#0B0E11"
                    strokeWidth={1.5}
                  />
                ) : null}
              </g>
            );
          })}
        </svg>

        {/* Hover Tooltip Overlay */}
        {hoveredPoint && hoveredIndex !== null && (
          <div
            className="absolute top-2 right-3 pointer-events-none bg-[#0b0e11]/95 border border-[#2B3139] px-3 py-1.5 rounded shadow-lg backdrop-blur-sm z-20 flex items-center gap-3 animate-fade-in"
          >
            <div className="flex flex-col">
              <span className="font-mono text-[10px] text-[#869488]">
                Time: {hoveredPoint.timeLabel}
              </span>
              <span className="font-mono text-xs font-semibold text-[#e1e2e7]">
                Score:{' '}
                <span
                  className={
                    hoveredPoint.score > 0
                      ? 'text-[#02c076]'
                      : hoveredPoint.score < 0
                      ? 'text-[#f84b4b]'
                      : 'text-[#848E9C]'
                  }
                >
                  {hoveredPoint.score > 0
                    ? `+${hoveredPoint.score.toFixed(2)}`
                    : hoveredPoint.score.toFixed(2)}
                </span>
              </span>
            </div>
            {hoveredPoint.eventMarker && (
              <div className="pl-3 border-l border-[#2B3139] flex flex-col">
                <span className="font-sans text-[10px] font-bold text-[#02c076] uppercase">
                  {hoveredPoint.eventMarker.type}
                </span>
                <span className="font-sans text-xs text-[#e1e2e7] font-medium max-w-xs truncate">
                  {hoveredPoint.eventMarker.title}
                </span>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

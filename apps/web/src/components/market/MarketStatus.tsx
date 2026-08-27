import React, { useState, useEffect } from 'react';
import { AlertTriangle, Database, Radio, Clock, Gauge } from 'lucide-react';
import { cn } from '../../utils/cn';

export interface MarketStatusHeaderProps {
  symbol?: string;
  price?: string;
  change24h?: string;
  statusText?: string;
  sourceText?: string;
  connectionText?: string;
}

export const MarketHeader: React.FC<MarketStatusHeaderProps> = ({
  symbol = 'BTC/USDT',
  price = '$64,230.15',
  change24h = '+1.24% (24h)',
  statusText = 'Volatile',
  sourceText = 'Binance',
  connectionText = 'Synchronized',
}) => {
  return (
    <div className="px-4 py-2 border-b border-[#323538] bg-[#1E2329] flex items-center justify-between shrink-0 h-14 select-none">
      {/* Symbol, Price, and 24h change */}
      <div className="flex items-center gap-6">
        <div className="flex items-baseline gap-3">
          <h1 className="text-[28px] font-semibold text-[#e1e2e7] m-0 tracking-tight font-sans">
            {symbol}
          </h1>
          <span className="text-[18px] font-medium text-[#44e092] font-mono m-0">
            {price}
          </span>
          <span className="text-[14px] font-medium text-[#44e092] font-mono m-0">
            {change24h}
          </span>
        </div>
      </div>

      {/* Badges: Status, Source, Connection */}
      <div className="flex items-center gap-3 font-mono text-[12px] text-[#bbcabd]">
        {/* Status: Volatile */}
        <div className="flex items-center gap-1.5 border border-[#323538] bg-[#111417] px-2.5 py-1 rounded-[2px]">
          <AlertTriangle className="w-3.5 h-3.5 text-[#f6be16]" />
          <span>Status:</span>
          <span className="text-[#f6be16] font-medium">{statusText}</span>
        </div>

        {/* Source: Binance */}
        <div className="flex items-center gap-1.5 border border-[#323538] bg-[#111417] px-2.5 py-1 rounded-[2px]">
          <Database className="w-3.5 h-3.5 text-[#bbcabd]" />
          <span>Source:</span>
          <span className="text-[#e1e2e7] font-medium">{sourceText}</span>
        </div>

        {/* Connection: Synchronized */}
        <div className="flex items-center gap-1.5 border border-[#323538] bg-[#111417] px-2.5 py-1 rounded-[2px]">
          <span className="w-1.5 h-1.5 rounded-full bg-[#44e092] animate-pulse block shrink-0" />
          <span>Connection:</span>
          <span className="text-[#44e092] font-medium">{connectionText}</span>
        </div>
      </div>
    </div>
  );
};

export const MarketFooterStatusBar: React.FC = () => {
  const [timeStr, setTimeStr] = useState('14:02:31');

  useEffect(() => {
    const updateTime = () => {
      const now = new Date();
      setTimeStr(
        `${now.getHours().toString().padStart(2, '0')}:${now
          .getMinutes()
          .toString()
          .padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`
      );
    };
    updateTime();
    const interval = setInterval(updateTime, 1000);
    return () => clearInterval(interval);
  }, []);

  return (
    <footer className="h-6 bg-[#0b0e11] border-t border-[#323538] flex items-center px-4 justify-between font-mono text-[10px] text-[#bbcabd] shrink-0 z-20 select-none">
      {/* Left side telemetry */}
      <div className="flex items-center gap-5">
        <div className="flex items-center gap-1.5">
          <span className="w-1.5 h-1.5 rounded-full bg-[#44e092] block" />
          <span>WebSocket: Connected</span>
        </div>
        <div className="flex items-center gap-1.5">
          <Radio className="w-3 h-3 text-[#bbcabd]" />
          <span>Binance Feed: Live</span>
        </div>
      </div>

      {/* Right side telemetry */}
      <div className="flex items-center gap-5">
        <div className="flex items-center gap-1.5">
          <Clock className="w-3 h-3 text-[#bbcabd]" />
          <span>Last Update: {timeStr}</span>
        </div>
        <div className="flex items-center gap-1.5">
          <Gauge className="w-3 h-3 text-[#bbcabd]" />
          <span>
            Latency: <span className="text-[#44e092] font-semibold">12ms</span>
          </span>
        </div>
      </div>
    </footer>
  );
};

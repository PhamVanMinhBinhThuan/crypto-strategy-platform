import React, { useState, useRef, useEffect } from 'react';
import { ChevronDown, ChevronUp, Check } from 'lucide-react';
import { cn } from '../../utils/cn';

export interface MarketOption {
  symbol: string;
  name: string;
  price: string;
  change24h: string;
  isPositive: boolean;
}

const DEFAULT_MARKETS: MarketOption[] = [
  { symbol: 'BTC/USDT', name: 'Bitcoin', price: '$64,230.15', change24h: '+1.24%', isPositive: true },
  { symbol: 'ETH/USDT', name: 'Ethereum', price: '$3,485.20', change24h: '+2.85%', isPositive: true },
  { symbol: 'SOL/USDT', name: 'Solana', price: '$182.40', change24h: '-0.85%', isPositive: false },
  { symbol: 'BNB/USDT', name: 'BNB Chain', price: '$586.10', change24h: '+0.42%', isPositive: true },
  { symbol: 'AVAX/USDT', name: 'Avalanche', price: '$32.10', change24h: '+4.12%', isPositive: true },
];

export interface MarketSelectorProps {
  selectedPair: string;
  onSelectPair: (pair: string) => void;
  className?: string;
}

export const MarketSelector: React.FC<MarketSelectorProps> = ({
  selectedPair,
  onSelectPair,
  className,
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  return (
    <div className="relative inline-block" ref={dropdownRef}>
      <button
        type="button"
        onClick={() => setIsOpen(!isOpen)}
        className={cn(
          'text-[#44e092] font-bold hover:text-[#e1e2e7] transition-all duration-150 cursor-pointer flex items-center gap-1.5 focus:outline-none select-none text-sm font-mono',
          className
        )}
        title="Select Trading Pair"
      >
        <span>{selectedPair}</span>
        {isOpen ? (
          <ChevronUp className="w-4 h-4 text-[#44e092]" />
        ) : (
          <ChevronDown className="w-4 h-4 text-[#44e092]" />
        )}
      </button>

      {isOpen && (
        <div className="absolute left-0 top-full mt-2 w-64 bg-[#1E2329] border border-[#2B3139] rounded-[2px] shadow-2xl z-50 overflow-hidden font-mono">
          <div className="p-2 border-b border-[#2B3139] bg-[#191c1f] text-[11px] text-[#bbcabd] font-sans font-semibold uppercase tracking-wider">
            Select Market Pair
          </div>
          <div className="max-h-60 overflow-y-auto divide-y divide-[#2B3139]/60">
            {DEFAULT_MARKETS.map((market) => {
              const isSelected = market.symbol === selectedPair;
              return (
                <button
                  key={market.symbol}
                  type="button"
                  onClick={() => {
                    onSelectPair(market.symbol);
                    setIsOpen(false);
                  }}
                  className={cn(
                    'w-full px-3 py-2.5 flex items-center justify-between text-left transition-colors cursor-pointer text-xs',
                    isSelected
                      ? 'bg-[#272a2e] text-[#44e092] font-bold'
                      : 'hover:bg-[#272a2e] text-[#e1e2e7]'
                  )}
                >
                  <div>
                    <div className="font-bold flex items-center gap-1.5">
                      <span>{market.symbol}</span>
                      {isSelected && <Check className="w-3.5 h-3.5 text-[#44e092]" />}
                    </div>
                    <div className="text-[10px] text-[#bbcabd] font-sans">{market.name}</div>
                  </div>
                  <div className="text-right">
                    <div className="text-[#e1e2e7] font-semibold">{market.price}</div>
                    <div
                      className={cn(
                        'text-[10px] font-medium',
                        market.isPositive ? 'text-[#44e092]' : 'text-[#ff5353]'
                      )}
                    >
                      {market.change24h}
                    </div>
                  </div>
                </button>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
};

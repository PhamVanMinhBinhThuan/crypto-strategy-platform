import React from 'react';
import { StrategySignal } from '../../types/strategy';
import { cn } from '../../utils/cn';

export interface SignalBadgeProps {
  signal: StrategySignal;
  className?: string;
  size?: 'sm' | 'md';
}

export const SignalBadge: React.FC<SignalBadgeProps> = ({
  signal,
  className,
  size = 'sm',
}) => {
  const isBuy = signal === 'BUY';
  const isSell = signal === 'SELL';
  const isHold = signal === 'HOLD';

  return (
    <span
      className={cn(
        'font-mono font-bold uppercase rounded-[2px] inline-flex items-center justify-center select-none',
        size === 'sm' ? 'px-1.5 py-0.5 text-[10px]' : 'px-2.5 py-1 text-[12px]',
        isBuy && 'bg-[#02c076]/15 text-[#02C076] border border-[#02C076]/30',
        isSell && 'bg-[#CF304A]/15 text-[#CF304A] border border-[#CF304A]/30',
        isHold && 'bg-[#f6be16]/15 text-[#f6be16] border border-[#f6be16]/30',
        className
      )}
    >
      {signal}
    </span>
  );
};

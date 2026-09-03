import React from 'react';
import { cn } from '../../utils/cn';

export interface MetricCardProps {
  label: string;
  value: string | number;
  valueClass?: string;
  subtitle?: string;
}

export const MetricCard: React.FC<MetricCardProps> = ({
  label,
  value,
  valueClass = 'text-[#e1e2e7]',
  subtitle,
}) => {
  return (
    <div className="bg-[#191c1f] p-3 sm:p-3.5 md:p-4 flex flex-col justify-center select-none transition-colors min-w-0">
      <div className="font-sans text-[10px] md:text-[11px] font-bold text-[#bbcabd] mb-1 uppercase tracking-wider truncate">
        {label}
      </div>
      <div className={cn('font-mono text-sm sm:text-base md:text-lg lg:text-[19px] font-semibold truncate', valueClass)}>
        {value}
      </div>
      {subtitle && (
        <div className="font-mono text-[10px] text-[#869488] mt-0.5 truncate">
          {subtitle}
        </div>
      )}
    </div>
  );
};

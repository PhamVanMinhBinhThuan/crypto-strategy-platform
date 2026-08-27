import React from 'react';
import { cn } from '../../utils/cn';

export interface StatusBadgeProps {
  status?: 'active' | 'syncing' | 'error' | 'live' | 'idle';
  label?: string;
  className?: string;
  showDot?: boolean;
}

export const StatusBadge: React.FC<StatusBadgeProps> = ({
  status = 'active',
  label,
  className,
  showDot = true,
}) => {
  const dotColors: Record<string, string> = {
    active: 'bg-[#44e092]',
    live: 'bg-[#44e092]',
    syncing: 'bg-[#f6be16]',
    error: 'bg-[#ff5353]',
    idle: 'bg-[#869488]',
  };

  const defaultLabels: Record<string, string> = {
    active: 'Active',
    live: 'Live Status',
    syncing: 'Syncing',
    error: 'Error',
    idle: 'Idle',
  };

  const textColors: Record<string, string> = {
    active: 'text-[#e1e2e7]',
    live: 'text-[#bbcabd]',
    syncing: 'text-[#e1e2e7]',
    error: 'text-[#e1e2e7]',
    idle: 'text-[#bbcabd]',
  };

  return (
    <div
      className={cn(
        'flex items-center gap-2 text-xs font-normal select-none',
        textColors[status] || 'text-[#e1e2e7]',
        className
      )}
    >
      {showDot && (
        <span
          className={cn(
            'w-1.5 h-1.5 rounded-full block shrink-0',
            dotColors[status] || 'bg-[#44e092]'
          )}
        />
      )}
      <span>{label || defaultLabels[status]}</span>
    </div>
  );
};

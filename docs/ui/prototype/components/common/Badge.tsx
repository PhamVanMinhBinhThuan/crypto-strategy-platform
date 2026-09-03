import React from 'react';
import { cn } from '../../utils/cn';

export interface BadgeProps {
  children: React.ReactNode;
  variant?: 'default' | 'success' | 'danger' | 'warning' | 'info' | 'outline' | 'cyan';
  size?: 'sm' | 'md';
  className?: string;
}

export const Badge: React.FC<BadgeProps> = ({
  children,
  variant = 'default',
  size = 'sm',
  className,
}) => {
  const variantStyles = {
    default: 'bg-slate-800 text-slate-300 border-slate-700',
    success: 'bg-emerald-950/70 text-emerald-400 border-emerald-800/60',
    danger: 'bg-rose-950/70 text-rose-400 border-rose-800/60',
    warning: 'bg-amber-950/70 text-amber-400 border-amber-800/60',
    info: 'bg-sky-950/70 text-sky-400 border-sky-800/60',
    cyan: 'bg-cyan-950/70 text-cyan-400 border-cyan-800/60',
    outline: 'bg-transparent text-slate-400 border-slate-700',
  };

  const sizeStyles = {
    sm: 'text-[11px] px-2 py-0.5 font-mono tracking-tight',
    md: 'text-xs px-2.5 py-1 font-mono tracking-tight',
  };

  return (
    <span
      className={cn(
        'inline-flex items-center gap-1 font-medium rounded border whitespace-nowrap',
        variantStyles[variant],
        sizeStyles[size],
        className
      )}
    >
      {children}
    </span>
  );
};

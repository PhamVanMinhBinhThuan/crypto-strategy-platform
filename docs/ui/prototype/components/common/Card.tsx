import React from 'react';
import { cn } from '../../utils/cn';

export interface CardProps extends React.HTMLAttributes<HTMLDivElement> {
  header?: React.ReactNode;
  headerAction?: React.ReactNode;
  footer?: React.ReactNode;
  noPadding?: boolean;
}

export const Card: React.FC<CardProps> = ({
  children,
  header,
  headerAction,
  footer,
  noPadding = false,
  className,
  ...props
}) => {
  return (
    <div
      className={cn(
        'bg-slate-900/90 border border-slate-800 rounded-lg shadow-sm overflow-hidden flex flex-col',
        className
      )}
      {...props}
    >
      {header && (
        <div className="px-4 py-3 border-b border-slate-800 flex items-center justify-between gap-3 bg-slate-900/50">
          <div className="font-semibold text-sm text-slate-200 flex items-center gap-2">
            {header}
          </div>
          {headerAction && <div className="flex items-center gap-2">{headerAction}</div>}
        </div>
      )}
      <div className={cn('flex-1', noPadding ? '' : 'p-4')}>{children}</div>
      {footer && (
        <div className="px-4 py-2.5 border-t border-slate-800 bg-slate-950/40 text-xs text-slate-400">
          {footer}
        </div>
      )}
    </div>
  );
};

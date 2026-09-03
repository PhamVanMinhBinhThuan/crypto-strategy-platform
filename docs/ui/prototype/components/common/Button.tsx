import React from 'react';
import { cn } from '../../utils/cn';

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger' | 'success';
  size?: 'sm' | 'md' | 'lg';
  icon?: React.ReactNode;
  iconPosition?: 'left' | 'right';
  isLoading?: boolean;
}

export const Button: React.FC<ButtonProps> = ({
  children,
  variant = 'primary',
  size = 'md',
  icon,
  iconPosition = 'left',
  isLoading = false,
  className,
  disabled,
  ...props
}) => {
  const variantStyles = {
    primary: 'bg-cyan-500 hover:bg-cyan-400 text-slate-950 font-semibold shadow-sm hover:shadow-cyan-500/20 active:bg-cyan-600',
    secondary: 'bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 active:bg-slate-850',
    outline: 'bg-transparent hover:bg-slate-800/60 text-slate-300 border border-slate-700 active:bg-slate-800',
    ghost: 'bg-transparent hover:bg-slate-800 text-slate-300 hover:text-slate-100 active:bg-slate-850',
    danger: 'bg-rose-600 hover:bg-rose-500 text-white active:bg-rose-700',
    success: 'bg-emerald-600 hover:bg-emerald-500 text-white active:bg-emerald-700',
  };

  const sizeStyles = {
    sm: 'text-xs px-2.5 py-1.5 rounded gap-1.5 font-medium',
    md: 'text-sm px-3.5 py-2 rounded-md gap-2 font-medium',
    lg: 'text-base px-5 py-2.5 rounded-lg gap-2.5 font-semibold',
  };

  return (
    <button
      className={cn(
        'inline-flex items-center justify-center transition-colors cursor-pointer select-none disabled:opacity-50 disabled:cursor-not-allowed whitespace-nowrap',
        variantStyles[variant],
        sizeStyles[size],
        className
      )}
      disabled={disabled || isLoading}
      {...props}
    >
      {isLoading && (
        <svg className="animate-spin -ml-0.5 h-4 w-4 text-current" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"></path>
        </svg>
      )}
      {!isLoading && icon && iconPosition === 'left' && <span className="shrink-0">{icon}</span>}
      {children}
      {!isLoading && icon && iconPosition === 'right' && <span className="shrink-0">{icon}</span>}
    </button>
  );
};

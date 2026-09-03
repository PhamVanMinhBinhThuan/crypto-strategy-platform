import React from 'react';
import { RoutePath } from '../../types';
import { cn } from '../../utils/cn';

export interface SidebarItemProps {
  label: string;
  icon: React.ComponentType<{ className?: string }>;
  path: RoutePath;
  isActive: boolean;
  onClick: () => void;
}

export const SidebarItem: React.FC<SidebarItemProps> = ({
  label,
  icon: Icon,
  isActive,
  onClick,
}) => {
  return (
    <li>
      <button
        onClick={onClick}
        className={cn(
          'w-full flex items-center gap-3 px-4 py-3 rounded-[2px] transition-all text-left select-none cursor-pointer duration-150 text-sm',
          isActive
            ? 'bg-[#1d2023] text-[#44e092] font-bold border-r-2 border-[#44e092]'
            : 'text-[#bbcabd] hover:bg-[#1d2023] hover:text-[#e1e2e7] font-medium'
        )}
      >
        <Icon className={cn('w-5 h-5 shrink-0', isActive ? 'text-[#44e092]' : 'text-[#bbcabd]')} />
        <span className="truncate">{label}</span>
      </button>
    </li>
  );
};

import React from 'react';
import { StrategyDefinition } from '../../types/strategy';
import { Plus, Check } from 'lucide-react';
import { cn } from '../../utils/cn';

export interface StrategyLibraryItemProps {
  strategy: StrategyDefinition;
  onAdd: (strategy: StrategyDefinition) => void;
  isAlreadyAdded?: boolean;
}

export const StrategyLibraryItem: React.FC<StrategyLibraryItemProps> = ({
  strategy,
  onAdd,
  isAlreadyAdded = false,
}) => {
  const handleClick = () => {
    if (!isAlreadyAdded) {
      onAdd(strategy);
    }
  };

  return (
    <div
      onClick={handleClick}
      className={cn(
        'bg-[#0B0E11] rounded-[2px] border border-[#2B3139] p-2.5 transition-colors group select-none',
        isAlreadyAdded
          ? 'cursor-default opacity-85'
          : 'hover:border-[#869488] cursor-pointer'
      )}
    >
      <div className="flex justify-between items-center mb-1">
        <h3
          className={cn(
            'font-mono text-[13px] font-semibold text-[#e1e2e7] transition-colors',
            !isAlreadyAdded && 'group-hover:text-[#44e092]'
          )}
        >
          {strategy.name}
        </h3>

        {isAlreadyAdded ? (
          <span
            className="px-1.5 py-0.5 rounded-[2px] bg-[#2B3139] border border-[#3c4a40] text-[#44e092] font-mono text-[10px] flex items-center gap-1 font-semibold select-none"
            title="Module is already in Composite Strategy"
          >
            <Check className="w-3 h-3 text-[#44e092]" />
            <span>Added</span>
          </span>
        ) : (
          <button
            type="button"
            onClick={(e) => {
              e.stopPropagation();
              onAdd(strategy);
            }}
            className="p-1 rounded-[2px] text-[#44e092] hover:bg-[#44e092]/15 opacity-0 group-hover:opacity-100 transition-all flex items-center justify-center cursor-pointer"
            title={`Add ${strategy.name}`}
          >
            <Plus className="w-3.5 h-3.5" />
          </button>
        )}
      </div>

      <p className="font-sans text-[12px] text-[#bbcabd] leading-relaxed line-clamp-2">
        {strategy.description}
      </p>

      {strategy.tags && strategy.tags.length > 0 && (
        <div className="mt-2 flex flex-wrap gap-1">
          {strategy.tags.map((tag) => (
            <span
              key={tag}
              className="px-1.5 py-0.5 rounded-[2px] bg-[#2B3139] text-[#848E9C] font-mono text-[10px]"
            >
              {tag}
            </span>
          ))}
        </div>
      )}
    </div>
  );
};

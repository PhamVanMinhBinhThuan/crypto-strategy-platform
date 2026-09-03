import React from 'react';
import { SearchSpaceFeature } from '../../types/search';
import { cn } from '../../utils/cn';
import { Check } from 'lucide-react';

export interface SearchSpaceSelectorProps {
  features: SearchSpaceFeature[];
  onToggleFeature: (id: string) => void;
  disabled?: boolean;
}

export const SearchSpaceSelector: React.FC<SearchSpaceSelectorProps> = ({
  features,
  onToggleFeature,
  disabled = false,
}) => {
  return (
    <div className="bg-[#191c1f] p-3.5 sm:p-4 rounded border border-[#3c4a40] flex flex-col justify-between">
      <label className="block font-sans text-[11px] font-bold text-[#bbcabd] mb-2.5 uppercase tracking-wider">
        Search Space (Features)
      </label>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
        {features.map((feature) => {
          const isChecked = feature.enabled;
          return (
            <div
              key={feature.id}
              onClick={() => {
                if (!disabled) onToggleFeature(feature.id);
              }}
              className={cn(
                'flex items-center gap-2.5 p-2 px-2.5 bg-[#1d2023] rounded border transition-all select-none cursor-pointer',
                isChecked
                  ? 'border-[#3c4a40] hover:border-[#44e092]/80 bg-[#1d2023]'
                  : 'border-[#272a2e] hover:border-[#3c4a40] opacity-75',
                disabled && 'cursor-not-allowed opacity-50'
              )}
            >
              {/* Custom styled checkbox matching Stitch screenshot */}
              <div
                className={cn(
                  'w-4 h-4 rounded-[2px] flex items-center justify-center transition-colors shrink-0',
                  isChecked
                    ? 'bg-[#02c076] text-[#002110]'
                    : 'bg-[#0b0e11] border border-[#3c4a40]'
                )}
              >
                {isChecked && <Check className="w-3 h-3 stroke-[3]" />}
              </div>

              <span
                className={cn(
                  'text-xs font-sans truncate',
                  isChecked ? 'text-[#e1e2e7] font-medium' : 'text-[#869488]'
                )}
              >
                {feature.label}
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
};

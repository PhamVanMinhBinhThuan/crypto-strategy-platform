import React from 'react';
import { StopConditions } from '../../types/search';

export interface StopConditionEditorProps {
  conditions: StopConditions;
  onChange: (updated: Partial<StopConditions>) => void;
  disabled?: boolean;
}

export const StopConditionEditor: React.FC<StopConditionEditorProps> = ({
  conditions,
  onChange,
  disabled = false,
}) => {
  return (
    <div className="space-y-3.5 select-none">
      {/* Max Candidates */}
      <div>
        <label className="block font-sans text-[11px] font-bold text-[#bbcabd] mb-1 uppercase tracking-wider">
          Max Candidates
        </label>
        <input
          type="number"
          min={10}
          max={10000}
          step={50}
          disabled={disabled}
          value={conditions.maxCandidates}
          onChange={(e) =>
            onChange({ maxCandidates: Math.max(10, parseInt(e.target.value) || 100) })
          }
          className="w-full bg-[#0b0e11] border border-[#3c4a40] text-[#e1e2e7] rounded p-1.5 px-2.5 font-mono text-xs focus:border-[#44e092] focus:outline-none focus:ring-0 disabled:opacity-50"
        />
      </div>

      {/* Stop: No Improvement */}
      <div>
        <label className="block font-sans text-[11px] font-bold text-[#bbcabd] mb-1 uppercase tracking-wider">
          Stop: No Improvement
        </label>
        <div className="flex items-center bg-[#0b0e11] border border-[#3c4a40] rounded focus-within:border-[#44e092] overflow-hidden">
          <input
            type="number"
            min={5}
            max={500}
            step={5}
            disabled={disabled}
            value={conditions.noImprovementIters}
            onChange={(e) =>
              onChange({ noImprovementIters: Math.max(5, parseInt(e.target.value) || 10) })
            }
            className="w-full bg-transparent border-none text-[#e1e2e7] p-1.5 px-2.5 font-mono text-xs focus:outline-none focus:ring-0 disabled:opacity-50"
          />
          <span className="text-[#869488] font-mono text-xs px-2.5 bg-[#191c1f]/60 h-full py-1.5 border-l border-[#3c4a40]/60">
            iters
          </span>
        </div>
      </div>

      {/* Time Limit */}
      <div>
        <label className="block font-sans text-[11px] font-bold text-[#bbcabd] mb-1 uppercase tracking-wider">
          Time Limit (hrs)
        </label>
        <input
          type="number"
          min={1}
          max={72}
          disabled={disabled}
          value={conditions.timeLimitHours}
          onChange={(e) =>
            onChange({ timeLimitHours: Math.max(1, parseInt(e.target.value) || 1) })
          }
          className="w-full bg-[#0b0e11] border border-[#3c4a40] text-[#e1e2e7] rounded p-1.5 px-2.5 font-mono text-xs focus:border-[#44e092] focus:outline-none focus:ring-0 disabled:opacity-50"
        />
      </div>
    </div>
  );
};

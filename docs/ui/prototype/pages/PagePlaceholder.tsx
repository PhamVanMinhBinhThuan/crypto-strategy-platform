import React from 'react';

export interface PagePlaceholderProps {
  title: string;
  subtitle: string;
}

export const PagePlaceholder: React.FC<PagePlaceholderProps> = ({
  title,
  subtitle,
}) => {
  return (
    <div className="p-4 max-w-5xl">
      {/* Clean Route Header */}
      <div className="mb-6">
        <h1 className="text-[28px] font-bold text-[#e1e2e7] tracking-tight mb-2 font-sans">
          {title}
        </h1>
        <p className="text-[#bbcabd] text-sm font-normal font-sans">
          {subtitle}
        </p>
      </div>

      {/* Clean Route Workspace Placeholder */}
      <div className="w-full h-80 rounded-[2px] bg-[#1E2329] border border-[#2B3139] flex flex-col items-center justify-center p-8 text-center">
        <div className="w-12 h-12 rounded-full bg-[#191c1f] border border-[#2B3139] flex items-center justify-center text-[#44e092] mb-3">
          <span className="w-2.5 h-2.5 rounded-full bg-[#44e092]" />
        </div>
        <h2 className="text-base font-semibold text-[#e1e2e7] mb-1 font-sans">
          {title} Screen
        </h2>
        <p className="text-xs text-[#869488] max-w-md font-sans">
          Awaiting screen design specification. Global application shell is ready and active.
        </p>
      </div>
    </div>
  );
};

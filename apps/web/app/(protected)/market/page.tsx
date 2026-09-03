import { Suspense } from "react";
import { MarketDashboard } from "@/src/features/market/components/MarketDashboard";
export default function Page() {
  return (
    <Suspense
      fallback={
        <div className="market-empty" role="status">
          Đang tải Market…
        </div>
      }
    >
      <MarketDashboard />
    </Suspense>
  );
}

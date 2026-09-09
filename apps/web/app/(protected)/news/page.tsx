import { Suspense } from "react";
import { NewsWorkspace } from "@/src/features/news/components/NewsWorkspace";
export default function Page() {
  return (
    <Suspense
      fallback={
        <div className="news-state" role="status">
          Loading news…
        </div>
      }
    >
      <NewsWorkspace />
    </Suspense>
  );
}

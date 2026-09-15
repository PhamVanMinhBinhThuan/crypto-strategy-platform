import { DegradedState } from "@/src/components/states/DegradedState";
import type { SentimentServiceStatus } from "../model/news";

export function SentimentServiceBanner({ value }: { value: SentimentServiceStatus }) {
  return (
    <DegradedState
      state={value.status === "AVAILABLE" ? "ready" : "degraded"}
      message={value.message}
    />
  );
}

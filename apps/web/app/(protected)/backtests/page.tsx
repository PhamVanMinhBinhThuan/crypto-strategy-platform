import { BacktestResultsView } from "@/src/features/backtests/components/BacktestResultsView";
export default async function Page({
  searchParams
}: {
  searchParams: Promise<{ resultId?: string; backtestId?: string }>;
}) {
  const params = await searchParams;
  return <BacktestResultsView resultId={params.resultId} backtestId={params.backtestId} />;
}

import { StrategyWorkspace } from "@/src/features/strategy/components/StrategyWorkspace";
export default async function Page({
  searchParams
}: {
  searchParams: Promise<{ strategy?: string }>;
}) {
  const { strategy } = await searchParams;
  return <StrategyWorkspace initialStrategyId={strategy} />;
}

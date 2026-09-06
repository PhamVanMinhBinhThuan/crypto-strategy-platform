import { SearchView } from "@/src/features/experiments/components/SearchView";
export default async function Page({
  searchParams
}: {
  searchParams: Promise<{ id?: string; mode?: string; userStrategyVersionId?: string }>;
}) {
  const { id, mode, userStrategyVersionId } = await searchParams;
  return <SearchView id={id} mode={mode} initialUserStrategyVersionId={userStrategyVersionId} />;
}

import { SearchView } from "@/src/features/experiments/components/SearchView";
export default async function Page({
  searchParams
}: {
  searchParams: Promise<{ id?: string; userStrategyVersionId?: string }>;
}) {
  const { id, userStrategyVersionId } = await searchParams;
  return <SearchView id={id} initialUserStrategyVersionId={userStrategyVersionId} />;
}

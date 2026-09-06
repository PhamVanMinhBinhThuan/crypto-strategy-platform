import { SearchView } from "@/src/features/experiments/components/SearchView";
export default async function Page({ searchParams }: { searchParams: Promise<{ id?: string; mode?: string }> }) {
  const { id, mode } = await searchParams;
  return <SearchView id={id} mode={mode} />;
}

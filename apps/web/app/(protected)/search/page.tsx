import { SearchView } from "@/src/features/experiments/components/SearchView";
export default async function Page({ searchParams }: { searchParams: Promise<{ id?: string }> }) {
  const { id } = await searchParams;
  return <SearchView id={id} />;
}

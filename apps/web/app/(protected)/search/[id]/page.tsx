import { SearchView } from "@/src/features/experiments/components/SearchView";

export default async function Page({
  params,
  searchParams
}: {
  params: Promise<{ id: string }>;
  searchParams: Promise<{ candidateId?: string }>;
}) {
  const { id } = await params;
  const { candidateId } = await searchParams;
  return <SearchView id={id} candidateId={candidateId} />;
}

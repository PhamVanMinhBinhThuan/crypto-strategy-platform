import Link from "next/link";

/** Navigation only: Composer remains the authority that configures and persists the Strategy. */
export function NewsStrategyAction({ available }: { available: boolean }) {
  if (!available) return null;
  return (
    <Link className="button" href="/strategies?strategy=sentiment-polarity">
      Use as Strategy
    </Link>
  );
}

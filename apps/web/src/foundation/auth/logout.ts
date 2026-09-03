type Cleanup = () => void | Promise<void>;
const cleanups = new Set<Cleanup>();
export function registerPrivateStateCleanup(cleanup: Cleanup) {
  cleanups.add(cleanup);
  return () => cleanups.delete(cleanup);
}
export async function clearPrivateClientState() {
  await Promise.all([...cleanups].map((cleanup) => cleanup()));
}

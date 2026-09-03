import { safeRedirect } from "./safe-redirect";
export function loginRedirect(currentPath: string): string {
  return `/login?next=${encodeURIComponent(safeRedirect(currentPath))}`;
}

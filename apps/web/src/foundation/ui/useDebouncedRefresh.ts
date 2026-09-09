"use client";

import { useCallback, useEffect, useRef, useState } from "react";

/** Coalesces bursts of realtime hints into one authoritative refresh. */
export function useDebouncedRefresh(action: () => void | Promise<void>, delayMilliseconds = 200) {
  const actionRef = useRef(action);
  const timerRef = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);
  const runningRef = useRef(false);
  const queuedRef = useRef(false);
  const waitersRef = useRef<Array<() => void>>([]);
  const mountedRef = useRef(true);
  const [pending, setPending] = useState(false);

  useEffect(() => {
    actionRef.current = action;
  }, [action]);

  const run = useCallback(
    async function execute(): Promise<void> {
      timerRef.current = undefined;
      runningRef.current = true;
      queuedRef.current = false;
      try {
        await actionRef.current();
      } catch {
        // The owning data hook exposes refresh failures through its own error state.
      } finally {
        runningRef.current = false;
        if (queuedRef.current) {
          timerRef.current = setTimeout(() => void execute(), delayMilliseconds);
          return;
        }
        waitersRef.current.splice(0).forEach((resolve) => resolve());
        if (mountedRef.current) setPending(false);
      }
    },
    [delayMilliseconds]
  );

  const schedule = useCallback(() => {
    setPending(true);
    queuedRef.current = true;
    const completion = new Promise<void>((resolve) => waitersRef.current.push(resolve));
    if (runningRef.current) return completion;
    if (timerRef.current) clearTimeout(timerRef.current);
    timerRef.current = setTimeout(() => void run(), delayMilliseconds);
    return completion;
  }, [delayMilliseconds, run]);

  useEffect(() => {
    mountedRef.current = true;
    const waiters = waitersRef.current;
    return () => {
      mountedRef.current = false;
      if (timerRef.current) clearTimeout(timerRef.current);
      waiters.splice(0).forEach((resolve) => resolve());
    };
  }, []);

  return { schedule, pending };
}

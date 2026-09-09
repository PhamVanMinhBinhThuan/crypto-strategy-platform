"use client";

import { registerPrivateStateCleanup } from "@/src/foundation/auth/logout";

const KEY = "crypto-strategy:last-dataset-id";
const identifier = /^[A-Za-z0-9_-]{6,128}$/;

export function readRememberedDatasetId() {
  if (typeof window === "undefined") return null;
  try {
    const value = window.sessionStorage.getItem(KEY);
    return value && identifier.test(value) ? value : null;
  } catch {
    return null;
  }
}

export function rememberDataset(datasetId: string) {
  if (typeof window === "undefined" || !identifier.test(datasetId)) return;
  try {
    window.sessionStorage.setItem(KEY, datasetId);
  } catch {
    // The selected dataset remains valid in the form when storage is unavailable.
  }
}

export function forgetDataset() {
  if (typeof window === "undefined") return;
  try {
    window.sessionStorage.removeItem(KEY);
  } catch {
    // Logout and form reset remain safe when storage is unavailable.
  }
}

if (typeof window !== "undefined") registerPrivateStateCleanup(forgetDataset);

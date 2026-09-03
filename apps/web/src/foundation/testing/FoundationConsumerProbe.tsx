"use client";
import { useState } from "react";
import type { ApiClient } from "../http/contracts";
export function FoundationConsumerProbe({ client }: { client: ApiClient }) {
  const [text, setText] = useState("Ready");
  return (
    <button
      onClick={async () =>
        setText((await client.request<{ label: string }>("/probe")).ok ? "Loaded" : "Unavailable")
      }
    >
      {text}
    </button>
  );
}

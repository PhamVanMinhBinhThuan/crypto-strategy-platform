import type { ApiClient } from "@/src/foundation/http/contracts";
import { requestPublic } from "../../shared/feature-api";
import {
  strategyPageSchema,
  userStrategyPageSchema,
  userStrategySchema,
  userStrategyVersionSchema
} from "./schemas";
import type { StrategyDraft, StrategySourceDraft } from "../model/strategy-draft";
const json = (body?: unknown): RequestInit => ({
  method: "POST",
  headers: { "Content-Type": "application/json" },
  ...(body === undefined ? {} : { body: JSON.stringify(body) })
});
export const listSystemStrategies = (client: ApiClient) =>
  requestPublic(client, strategyPageSchema, "/api/v1/strategies");
export const listUserStrategies = (client: ApiClient) =>
  requestPublic(client, userStrategyPageSchema, "/api/v1/user-strategies");
export const getUserStrategy = (client: ApiClient, id: string) =>
  requestPublic(client, userStrategySchema, `/api/v1/user-strategies/${encodeURIComponent(id)}`);
export const createUserStrategy = (client: ApiClient, draft: StrategyDraft) =>
  requestPublic(
    client,
    userStrategySchema,
    "/api/v1/user-strategies",
    json({
      name: draft.name,
      description: draft.description,
      kind: draft.kind,
      source: draft.source
    })
  );
export const createUserStrategyVersion = (
  client: ApiClient,
  id: string,
  expectedLatestVersionNo: number,
  source: StrategySourceDraft
) =>
  requestPublic(
    client,
    userStrategyVersionSchema,
    `/api/v1/user-strategies/${encodeURIComponent(id)}/versions`,
    json({ expectedLatestVersionNo, source })
  );
export const publishUserStrategyVersion = (
  client: ApiClient,
  id: string,
  versionId: string,
  expectedVersionNo: number
) =>
  requestPublic(
    client,
    userStrategyVersionSchema,
    `/api/v1/user-strategies/${encodeURIComponent(id)}/versions/${encodeURIComponent(versionId)}/publish`,
    json({ expectedVersionNo })
  );
export const archiveUserStrategy = (client: ApiClient, id: string) =>
  requestPublic(
    client,
    userStrategySchema,
    `/api/v1/user-strategies/${encodeURIComponent(id)}/archive`,
    json()
  );

# Contract: Strategy UI

## Public operations

| Journey         | F-009 operation                              | UI outcome                          |
| --- | --- | --- |
| System catalog  | `GET /api/v1/strategies`                     | Cursor list/detail descriptor       |
| Private library | `GET /api/v1/user-strategies`                | Owner-scoped list                   |
| Private detail  | `GET /api/v1/user-strategies/{id}`           | Latest immutable version            |
| Create          | `POST /api/v1/user-strategies`               | Reload authoritative created detail |
| New version     | `POST /api/v1/user-strategies/{id}/versions` | Reload latest version               |
| Publish         | `POST .../{versionId}/publish`               | Confirm then reload detail          |
| Archive         | `POST .../{id}/archive`                      | Confirm then reload list/detail     |

Adapter uses exact OpenAPI DTO shapes; it never imports backend/provider models. Missing and foreign
owner map to the same inaccessible view. Correlation ID may be shown for support without raw detail.

## Draft validation

Descriptors drive controls for INTEGER, DECIMAL, BOOLEAN, TEXT and ENUM. Validation covers required,
type, exact lexical form, min/max, allowed values and lower/upper cross-rule. Client-valid data may
still receive server validation/conflict; field-safe errors map when supported, otherwise global.

SINGLE source contains one Strategy selection. COMPOSITE contains at least two selections plus policy
identity/version/parameters. Published source is read-only; modification creates a new version.

## Mutation rule

No optimistic business commit. Disable duplicate submission while pending; timeout does not mean
failure/success. Re-fetch authoritative resource before presenting final state.

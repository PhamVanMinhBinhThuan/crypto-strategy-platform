# Contract REST F-009

## Quy ước chung

- Base path: `/api/v1`; business operation yêu cầu Bearer identity đã xác thực.
- Request JSON từ chối field lạ; response/error có correlation ID và UTC timestamp.
- Collection dùng cursor opaque, thứ tự deterministic, limit bounded; decimal là JSON string.
- POST tạo work yêu cầu `Idempotency-Key`; replay cùng body trả outcome gốc.
- Cross-owner và missing resource dùng public inaccessible response, không trả metadata.

## Nhóm operation

| Nhóm | Operation public |
|---|---|
| Market/Dataset | đọc Candle history; tạo/đọc Dataset snapshot |
| Strategy | list system catalog; list/create/get/version/publish/archive private Strategy |
| Experiment | start/get/stop/reproduce Experiment; list/get Candidates |
| Job/Backtest | get Job; cancel khi state cho phép; start Backtest; get Result |
| Leaderboard | đọc current Top-K theo Experiment/revision |
| News | list normalized News; sentiment chỉ là summary nếu đã có |
| Realtime auth | cấp one-time WebSocket ticket |

Tên path và schema chi tiết phải được đồng bộ vào `docs/api/openapi.yaml` trong phase
implementation; bảng này là scope/ownership contract, không thay thế OpenAPI.

## Status mapping tối thiểu

`400` malformed/unknown/invalid cursor, `401` authentication required, `403` forbidden
origin/policy, `404` inaccessible/not found, `409` state/immutable/idempotency conflict,
`415` unsupported media, `422` domain validation, `429` rate limit, `502` invalid upstream,
`503` dependency unavailable, `504` timeout. Async Job failure được đọc bằng `200` với
terminal failure state.

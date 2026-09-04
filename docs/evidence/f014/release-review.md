# F014 Release Review

## Kết luận T062

- Review status: **PARTIAL — không có blocking code finding; release/LIVE gates còn mở**.
- Base commit: `50c28d99c02a4ee28ed1109b231daa4397a22fe4`; working tree F014 chưa commit.
- Reviewed at: `2026-09-04T10:15:57Z`.
- Scope: Constitution 1.2.0, ADR-0001–0016 liên quan, module/data ownership, OpenAPI/REST/realtime boundaries, F-011 browser foundation và F-012/F-013 shared UI reference.

## Review matrix

| Boundary                                                 | Kết luận          | Evidence                                                                                                                                                                 |
| -------------------------------------------------------- | ----------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Constitution I — Spec/ADR                                | PASS có điều kiện | F014 spec/plan/tasks đầy đủ; ADR-0001–0014 và 0016 liên quan đều Accepted; additive public reads được ghi lại trong plan/OpenAPI                                         |
| Constitution II — ownership/dependency                   | PASS              | 32 architecture tests; `ReproductionVerificationId` chỉ có một owner ở `experiment-execution`; API không phụ thuộc Search trực tiếp                                      |
| Constitution III — provenance/immutability               | PARTIAL           | Canonical Result/reproduction tests pass; shared DB thiếu F006 nên chưa có LIVE immutable graph/verdict                                                                  |
| Constitution IV — versioned contracts/provider isolation | PASS              | OpenAPI có Result provenance + reproduction verification; contract/API/Web tests pass; Strategy/Generator/provider không bị kéo vào browser                              |
| Constitution V — security/reliability/evidence           | PARTIAL           | Full gates không failure, redaction + secret scan pass, Redis real recovery pass riêng; final clean SHA và external LIVE dependencies còn thiếu                          |
| UI authority/reference                                   | PASS              | F-011 shell/client được tái sử dụng; F-012/F-013 routes giữ owner; browser không sinh Candidate, chạy Backtest, tính Evaluation/Ranking hay đọc business table trực tiếp |
| Financial/simulation boundary                            | PASS              | Không có trading thật, wallet action, lời khuyên mua/bán hoặc cam kết lợi nhuận; metrics hiển thị từ authoritative Result                                                |

## Finding đã remediation trong review

1. Public API từng gọi trực tiếp kiểu ID của Search, vi phạm dependency matrix. ID verification nay có
   một canonical owner tại public API của `experiment-execution`; Search model trùng đã bị loại bỏ.
2. Result evidence DTO từng dùng raw String cho business identities. Strategy/User Strategy/Combination
   identities nay dùng typed value và serializer phù hợp; canonical-boundary test pass.
3. Public 5xx logger từng đính kèm throwable làm lộ exception message/cause. Logger nay chỉ ghi safe
   code/status/type; ba redaction/public-error contract suites pass.
4. Architecture index/module view còn ghi foundation là Planned. Status, ngày và
   `experiment-execution` ownership đã được đồng bộ với implementation/ADR-0014/0016.
5. F014 plan từng nói không có public contract mới trong khi hardening bổ sung read boundary. Plan nay
   ghi rõ đây là additive owner-scoped contract remediation, không phải breaking change hoặc schema mới.

## Finding còn mở

### RR-001 — Final candidate SHA chưa tồn tại

- Mức độ: BLOCKING cho release evidence, không phải defect code.
- T061 chưa thể ghi final SHA/rerun trên clean checkout khi working tree chưa được commit.
- Remediation: chốt phạm vi commit, loại file rubric binary/changes ngoài F014 nếu không thuộc PR, commit,
  rerun quickstart và cập nhật evidence bằng SHA đã kiểm tra.

### RR-002 — LIVE dependency gate chưa đạt

- Mức độ: BLOCKING cho SC-001/SC-005/SC-006 và video LIVE.
- Shared PostgreSQL thiếu F006; browser auth config/session và external Sentiment ML readiness chưa có.
- Remediation thuộc owner/operator được ghi trong `runbook-dry-run.md` và `release-checklist.md`; không
  sửa database ad-hoc hoặc đổi fixture thành LIVE.

### RR-003 — ADR-0015 vẫn Proposed

- Mức độ: GOVERNANCE; blocking nếu PR/release tuyên bố standalone Backtest aggregate là phần đã duyệt.
- F014 main Search flow không cần dựa vào ADR này, nhưng repository có implementation standalone liên quan.
- Remediation: owners review/Accept ADR-0015 hoặc loại standalone path khỏi release claim; implementer
  F014 không tự đổi trạng thái ADR.

### RR-004 — Cross-owner sign-off và evidence media chưa có

- Mức độ: BLOCKING cho merge governance/hồ sơ cuối, không phải automated regression.
- Cần API/Search owner review additive contract, database owner review migration state, UI owner review
  screenshots và nhóm thêm video/Drive timestamp vào rubric.

## Quyết định release

Không gọi F014 là `LIVE VERIFIED` hoặc release-ready ở trạng thái hiện tại. Có thể bàn giao controlled
review package và dùng automated reports làm minh chứng kỹ thuật. Sau RR-001, T061 có thể đóng; RR-002
và RR-004 cần external owner/operator, còn RR-003 phải được giới hạn claim hoặc xử lý bằng governance.

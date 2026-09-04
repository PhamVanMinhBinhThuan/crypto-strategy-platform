# Evidence Record Contract

```yaml
criterionId: rubric-or-requirement-id
status: PLANNED | BLOCKED | PARTIAL | VERIFIED
commitSha: full-or-unambiguous-sha
capturedAt: UTC-instant
environment: local-demo-profile
nonSecretConfiguration: {}
commandOrAction: exact reproducible action
artifactPaths: []
observedResult: factual result
limitations: []
```

- `VERIFIED` cần commit, UTC time, môi trường, thao tác, kết quả và ít nhất một artifact xem lại được.
- Test skip, dependency chưa chạy hoặc screenshot mock không được tính là pass.
- Secret phải được redact; artifact chứa secret bị loại và credential liên quan phải rotate.
- Benchmark lưu workload, từng lần chạy, median, timeout/failure và machine profile không nhạy cảm.
- Evidence cũ không bị sửa để che failure; lần kiểm chứng mới tạo record/revision mới.

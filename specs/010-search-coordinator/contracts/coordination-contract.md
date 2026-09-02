# Contract: Search Coordination v1

## Published application commands

### Start Experiment

Input gồm authenticated owner UUID, idempotency key/hash, name, frozen Dataset/Strategy selection,
backtest/evaluation config, generator selection, search space, stop conditions, Top-K và correlation.

Outcome:

- accepted: typed Experiment ID + SEARCH Job ID + `QUEUED`;
- exact replay: cùng outcome;
- hash conflict: stable idempotency conflict;
- invalid/foreign input: validation hoặc ownership-safe inaccessible;
- atomic failure: không partial graph/receipt.

### Reproduce Experiment

Input gồm owner UUID, source Experiment ID, optional name, idempotency key/hash và correlation.
Source phải terminal, đúng owner và đủ evidence. Accepted outcome giống Start nhưng new Experiment
có `reproducesExperimentId` trỏ source và Candidate sequence được copy bất biến.

### Allocate Next Candidate

Trusted Worker input: Search Job/Run identity, expected durable version và correlation. Outcome:

- allocated Candidate + Backtest Job identities;
- window full;
- stop reached/exhausted;
- already applied/version stale;
- terminal failure with safe code.

Allocation là một transaction ngắn và không publish trực tiếp tới queue.

### Reconcile Completion

Trusted Worker input: evaluated Candidate/Job identity, message identity và correlation. Service
kiểm tra lineage, đọc authoritative child outcomes, update progress idempotently và trả decision:
fill slots, wait, complete, stop hoặc fail.

## Concurrency rules

- Database fencing/version quyết định correctness; Redis/cache key chỉ tối ưu.
- Hai allocators cùng expected version: tối đa một commit; bên còn lại reload/reconcile.
- Stop và allocate race: lock order bảo đảm nếu stop đã commit thì không Candidate mới được commit.
- Không giữ transaction/row lock trong lúc chạy generator chậm, chờ broker hay chạy Backtest;
  generate proposal ngoài lock, revalidate/fence khi commit.

## Recovery

Reconciler scan bounded non-terminal runs, so sánh durable counts/state và tạo lại publication intent
hoặc fill decision còn thiếu. Nó không suy luận “không có cache/message = chưa xử lý”.

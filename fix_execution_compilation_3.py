import re

# 1. Fix TrustedSearchCoordinationService (CandidateId to String, backtestJobId to String)
path = 'modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/internal/TrustedSearchCoordinationService.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# Fix trigger.experimentId().value(), trigger.candidateId(), trigger.backtestJobId()
content = re.sub(r'trigger\.candidateId\(\)', r'trigger.candidateId().value()', content)
content = re.sub(r'trigger\.backtestJobId\(\)', r'trigger.backtestJobId().value()', content)

# Fix new CoordinationOutcome(snapshot.run().searchRunId().value()
content = content.replace('snapshot.run().searchRunId().value()', 'new SearchRunId(snapshot.run().searchRunId())')

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

# 2. Fix SearchCandidateAllocationService
path2 = 'modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/internal/SearchCandidateAllocationService.java'
with open(path2, 'r', encoding='utf-8') as f:
    content = f.read()

# new ExperimentId(run.experimentRef()) where it expects CandidateId?
# Wait! Let's check the error logs for AllocationService.

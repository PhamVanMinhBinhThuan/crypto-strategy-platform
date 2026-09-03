import re

# 1. TrustedSearchCoordinationServiceTest
path = 'modules/experiment-execution/src/test/java/com/cryptostrategy/platform/execution/internal/TrustedSearchCoordinationServiceTest.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

content = re.sub(
    r'running\.experimentRef\(\),\s*"candidate",\s*"job"',
    r'new com.cryptostrategy.platform.experiment.api.ExperimentId(running.experimentRef()), new com.cryptostrategy.platform.experiment.api.CandidateId("candidate"), new com.cryptostrategy.platform.experiment.api.job.JobId("job")',
    content
)
with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

# 2. SearchCoordinator (worker)
path = 'apps/worker/src/main/java/com/cryptostrategy/platform/worker/search/coordination/SearchCoordinator.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('runs.findBySearchJobId(request.searchJobId())', 'runs.findBySearchJobId(request.searchJobId().value())')
content = content.replace('new com.cryptostrategy.platform.search.api.model.SearchRunId(durable.searchRunId())', 'durable.searchRunId()')
content = content.replace('durable.experimentRef(), Instant.now()', 'new com.cryptostrategy.platform.experiment.api.ExperimentId(durable.experimentRef()), Instant.now()')
# request.searchJobId(), where it was a parameter for JobId? Wait, the JobId was already String? 
# Wait, "MessageUlid cannot be converted to JobId" -> so it WANTS a JobId!
# So request.searchJobId() -> new com.cryptostrategy.platform.experiment.api.job.JobId(request.searchJobId().value())
content = re.sub(r'request\.searchJobId\(\)\s*,', r'new com.cryptostrategy.platform.experiment.api.job.JobId(request.searchJobId().value()),', content)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

# 3. SearchCompletionConsumer (worker)
path = 'apps/worker/src/main/java/com/cryptostrategy/platform/worker/search/consumer/SearchCompletionConsumer.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('new com.cryptostrategy.platform.experiment.api.ExperimentId(payload.experimentId())', 'new com.cryptostrategy.platform.experiment.api.ExperimentId(payload.experimentId().value())')
content = content.replace('new com.cryptostrategy.platform.experiment.api.CandidateId(payload.candidateId())', 'new com.cryptostrategy.platform.experiment.api.CandidateId(payload.candidateId().value())')
content = content.replace('new com.cryptostrategy.platform.experiment.api.job.JobId(payload.backtestJobId())', 'new com.cryptostrategy.platform.experiment.api.job.JobId(payload.backtestJobId().value())')

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

# 4. ExperimentRequestMapper (api)
path = 'apps/api/src/main/java/com/cryptostrategy/platform/api/experiment/ExperimentRequestMapper.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# Replace new GeneratorId(new GeneratorId(...)) with new GeneratorId(...)
content = re.sub(
    r'new com\.cryptostrategy\.platform\.search\.api\.model\.GeneratorId\(new com\.cryptostrategy\.platform\.search\.api\.model\.GeneratorId\(([^)]+)\)\)',
    r'new com.cryptostrategy.platform.search.api.model.GeneratorId(\1)',
    content
)
with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Fixed the remaining errors")

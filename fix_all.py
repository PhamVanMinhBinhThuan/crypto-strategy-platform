import re

# 1. TrustedSearchCoordinationServiceTest
path = 'modules/experiment-execution/src/test/java/com/cryptostrategy/platform/execution/internal/TrustedSearchCoordinationServiceTest.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# Revert gateway.loadCompletion(...) to just use strings
content = re.sub(
    r'new com\.cryptostrategy\.platform\.experiment\.api\.ExperimentId\(running\.experimentRef\(\)\),\s*new com\.cryptostrategy\.platform\.experiment\.api\.CandidateId\("candidate"\),\s*new com\.cryptostrategy\.platform\.experiment\.api\.job\.JobId\("job"\)',
    r'running.experimentRef(), "candidate", "job"',
    content
)
with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

# 2. SearchCoordinator
path = 'apps/worker/src/main/java/com/cryptostrategy/platform/worker/search/coordination/SearchCoordinator.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('request.experimentId().value(),', 'new com.cryptostrategy.platform.experiment.api.ExperimentId(request.experimentId().value()),')
with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

# 3. SearchCompletionConsumer
path = 'apps/worker/src/main/java/com/cryptostrategy/platform/worker/search/consumer/SearchCompletionConsumer.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('payload.jobId().value(),', 'new com.cryptostrategy.platform.experiment.api.job.JobId(payload.jobId().value()),')
with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

# 4. ExperimentRequestMapper
path = 'apps/api/src/main/java/com/cryptostrategy/platform/api/experiment/ExperimentRequestMapper.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('new com.cryptostrategy.platform.search.api.model.GeneratorId(new com.cryptostrategy.platform.search.api.model.GeneratorId(request.generator().generatorId()))', 'new com.cryptostrategy.platform.search.api.model.GeneratorId(request.generator().generatorId())')
with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Fixed the compilation errors again.")

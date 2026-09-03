import re

# 1. TrustedSearchCoordinationServiceTest.java
path = 'modules/experiment-execution/src/test/java/com/cryptostrategy/platform/execution/internal/TrustedSearchCoordinationServiceTest.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace(
    'new com.cryptostrategy.platform.experiment.api.ExperimentId(running.experimentRef())',
    'running.experimentRef()'
)
content = content.replace(
    'new com.cryptostrategy.platform.experiment.api.CandidateId("candidate")',
    '"candidate"'
)
content = content.replace(
    'new com.cryptostrategy.platform.experiment.api.job.JobId("job")',
    '"job"'
)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

# 2. ExperimentRequestMapper.java
path = 'apps/api/src/main/java/com/cryptostrategy/platform/api/experiment/ExperimentRequestMapper.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace(
    'request.generator() == null ? null : request.generator().generatorId()',
    'request.generator() == null ? null : new com.cryptostrategy.platform.search.api.model.GeneratorId(request.generator().generatorId().toString())'
)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

# 3. SearchCoordinatorTest.java
path = 'apps/worker/src/test/java/com/cryptostrategy/platform/worker/search/SearchCoordinatorTest.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace(
    '"01J7K8M9N0P1Q2R3S4T5A6V7W4", 7, 2, 4, 1, SearchRunStatus.RUNNING',
    'new com.cryptostrategy.platform.search.api.model.SearchRunId("01J7K8M9N0P1Q2R3S4T5A6V7W4"), 7, 2, 4, 1, SearchRunStatus.RUNNING'
)
content = content.replace(
    '"01J7K8M9N0P1Q2R3S4T5A6V7W2",',
    'new com.cryptostrategy.platform.experiment.api.job.JobId("01J7K8M9N0P1Q2R3S4T5A6V7W2"),'
)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Final fixes applied")

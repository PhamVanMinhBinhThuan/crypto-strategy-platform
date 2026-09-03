import re

# 1. SearchCoordinatorTest: Revert line 41 back to JobId
path = 'apps/worker/src/test/java/com/cryptostrategy/platform/worker/search/SearchCoordinatorTest.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace(
    'new SearchCoordinationCommand(\n                new com.cryptostrategy.platform.contracts.api.MessageUlid("01J7K8M9N0P1Q2R3S4T5A6V7W2"),',
    'new SearchCoordinationCommand(\n                new com.cryptostrategy.platform.experiment.api.job.JobId("01J7K8M9N0P1Q2R3S4T5A6V7W2"),'
)
with open(path, 'w', encoding='utf-8') as f:
    f.write(content)


# 2. ExperimentRequestMapper: use String.valueOf()
path = 'apps/api/src/main/java/com/cryptostrategy/platform/api/experiment/ExperimentRequestMapper.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace(
    'request.generator().generatorId().name()',
    'String.valueOf(request.generator().generatorId())'
)
with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Fixed the last two errors!")

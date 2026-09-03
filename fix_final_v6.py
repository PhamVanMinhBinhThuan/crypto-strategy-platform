import re
import os

# 1. SearchCoordinatorTest: MessageUlid package and values
path = 'apps/worker/src/test/java/com/cryptostrategy/platform/worker/search/SearchCoordinatorTest.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace(
    'new com.cryptostrategy.platform.worker.infra.redis.MessageUlid',
    'new com.cryptostrategy.platform.contracts.api.MessageUlid'
)
content = content.replace(
    'new com.cryptostrategy.platform.contracts.api.MessageUlid("msg")',
    'new com.cryptostrategy.platform.contracts.api.MessageUlid("01J7K8M9N0P1Q2R3S4T5A6V7W9")'
)
with open(path, 'w', encoding='utf-8') as f:
    f.write(content)


# 2. TrustedSearchCoordinationServiceTest: crockford ULID
path = 'modules/experiment-execution/src/test/java/com/cryptostrategy/platform/execution/internal/TrustedSearchCoordinationServiceTest.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('"candidate"', '"01J7K8M9N0P1Q2R3S4T5A6V7W2"')
content = content.replace('"job"', '"01J7K8M9N0P1Q2R3S4T5A6V7W3"')
with open(path, 'w', encoding='utf-8') as f:
    f.write(content)


# 3. TrustedSearchCoordinationGateway.processedMessageId -> processedMessageRef
path = 'modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/api/port/out/TrustedSearchCoordinationGateway.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('String processedMessageId', 'String processedMessageRef')
content = content.replace('processedMessageId()', 'processedMessageRef()')
with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

# Update internal usages of processedMessageId -> processedMessageRef
for root, _, files in os.walk('modules/experiment-execution'):
    for file in files:
        if file.endswith('.java'):
            filepath = os.path.join(root, file)
            with open(filepath, 'r', encoding='utf-8') as f:
                file_content = f.read()
            if 'processedMessageId' in file_content:
                file_content = file_content.replace('processedMessageId', 'processedMessageRef')
                with open(filepath, 'w', encoding='utf-8') as f:
                    f.write(file_content)


# 4. SearchStartCommandFactory.Request generatorId -> String generatorRef
path = 'modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/api/port/in/SearchStartCommandFactory.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('GeneratorId generatorId,', 'String generatorRef,')
with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

path = 'modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/internal/SearchStartCommandFactoryImpl.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('request.generatorId()', 'request.generatorRef()')
with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

path = 'modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/internal/SearchStartCommandFactoryService.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('request.generatorId().value()', 'request.generatorRef()')
content = content.replace('request.generatorId()', 'request.generatorRef()')
with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

path = 'modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/internal/SearchCandidateAllocationService.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('run.generatorId()', 'run.generatorId()') # run.generatorId() is a GeneratorId from SearchRun, so keep it!
with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

path = 'apps/api/src/main/java/com/cryptostrategy/platform/api/experiment/ExperimentRequestMapper.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('new com.cryptostrategy.platform.search.api.model.GeneratorId(request.generator().generatorId().toString())', 'request.generator().generatorId()')
with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Applied architectural ID fixes")

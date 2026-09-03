import re

# 1. TrustedSearchCoordinationServiceTest
path = 'modules/experiment-execution/src/test/java/com/cryptostrategy/platform/execution/internal/TrustedSearchCoordinationServiceTest.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace(
    'running.experimentRef(), "candidate", "job"',
    'new com.cryptostrategy.platform.experiment.api.ExperimentId(running.experimentRef()), new com.cryptostrategy.platform.experiment.api.CandidateId("candidate"), new com.cryptostrategy.platform.experiment.api.job.JobId("job")'
)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)


# 2. SearchStartCommandFactory and its usages
import os
for root, _, files in os.walk('modules/experiment-execution'):
    for file in files:
        if file.endswith('.java'):
            filepath = os.path.join(root, file)
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()
            if 'generatorId' in content:
                content = content.replace('String generatorId', 'String generatorRef')
                content = content.replace('generatorId()', 'generatorRef()')
                content = content.replace('generatorId,', 'generatorRef,')
                content = content.replace('this.generatorId =', 'this.generatorRef =')
                with open(filepath, 'w', encoding='utf-8') as f:
                    f.write(content)

# API mapper
api_mapper_path = 'apps/api/src/main/java/com/cryptostrategy/platform/api/experiment/ExperimentRequestMapper.java'
with open(api_mapper_path, 'r', encoding='utf-8') as f:
    content = f.read()
content = re.sub(
    r'new com\.cryptostrategy\.platform\.search\.api\.model\.GeneratorId\(request\.generator\(\)\.generatorId\(\)\)',
    r'request.generator().generatorId()',
    content
)
with open(api_mapper_path, 'w', encoding='utf-8') as f:
    f.write(content)


# 3. Fix NewsMigrationContractTest hash
test_path = 'modules/persistence/src/test/java/com/cryptostrategy/platform/persistence/internal/news/NewsMigrationContractTest.java'
with open(test_path, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('"6bd5fb3595e8dcf5f7cc373854a3db22b37aa41014cbec71633f8e270164935a"', '"b788494c7bcb3c7cfaae98463e3e8786819290cf65ab578d7e5656a23fc59df6"')
with open(test_path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Applied fixes")

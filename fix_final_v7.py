import re
import os

path = 'modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/api/port/in/SearchStartCommandFactory.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('GeneratorId generatorId,', 'String generatorRef,')
with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

path = 'modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/internal/SearchStartCommandFactoryService.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('request.generatorId().value()', 'request.generatorRef()')
content = content.replace('request.generatorId()', 'request.generatorRef()')
with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

path = 'apps/api/src/main/java/com/cryptostrategy/platform/api/experiment/ExperimentRequestMapper.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('new com.cryptostrategy.platform.search.api.model.GeneratorId(request.generator().generatorId().toString())', 'request.generator().generatorId()')
with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Applied architectural ID fixes")

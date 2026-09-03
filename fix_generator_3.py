import os

path_factory = 'modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/api/port/in/SearchStartCommandFactory.java'
with open(path_factory, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('String generatorId,', 'String generatorRef,')
with open(path_factory, 'w', encoding='utf-8') as f:
    f.write(content)

for root, _, files in os.walk('modules/experiment-execution'):
    for file in files:
        if file.endswith('.java') and file != 'SearchStartCommandFactory.java':
            filepath = os.path.join(root, file)
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()
            if 'request.generatorId()' in content:
                content = content.replace('request.generatorId()', 'request.generatorRef()')
                with open(filepath, 'w', encoding='utf-8') as f:
                    f.write(content)

path_mapper = 'apps/api/src/main/java/com/cryptostrategy/platform/api/experiment/ExperimentRequestMapper.java'
with open(path_mapper, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('new com.cryptostrategy.platform.search.api.model.GeneratorId(request.generator().generatorId())', 'request.generator().generatorId()')
with open(path_mapper, 'w', encoding='utf-8') as f:
    f.write(content)

print("Fixed generatorRef issues.")

import re

path = 'modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/internal/SearchStartCommandFactoryService.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace(
    '!"random-search".equals(request.generatorId())',
    '!"random-search".equals(request.generatorId().value())'
)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print("Added .value() for generatorId comparison.")

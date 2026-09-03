import re
path = 'apps/api/src/main/java/com/cryptostrategy/platform/api/experiment/ExperimentRequestMapper.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('request.generator().generatorId(),', 'request.generator().generatorId().getValue(),')
with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print("Added getValue() to mapper")

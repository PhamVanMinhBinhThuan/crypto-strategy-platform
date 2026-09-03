import re
path = 'apps/api/src/main/java/com/cryptostrategy/platform/api/experiment/ExperimentRequestMapper.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('.getValue()', '.name()')
with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print("Changed getValue() to name()")

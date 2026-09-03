import re

path = 'modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/search/JdbcTrustedSearchCoordinationGateway.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('change.processedMessageId()', 'change.processedMessageRef()')

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print("Fixed JdbcTrustedSearchCoordinationGateway")

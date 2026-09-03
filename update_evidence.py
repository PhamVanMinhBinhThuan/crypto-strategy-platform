import re

path = 'docs/architecture/architecture-evidence.md'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('**Status:** Planned', '**Status:** Verified')
content = content.replace('Status: Planned', 'Status: Verified')

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print("Updated architecture-evidence.md")

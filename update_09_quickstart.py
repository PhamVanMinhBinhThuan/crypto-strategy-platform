import re

path = 'specs/009-public-api-realtime/quickstart.md'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

content += "\n\n## Search Coordinator Evidence\n- F-010 Search Coordinator tests completed successfully and integrated with F-009.\n"

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print("Updated 009 quickstart")

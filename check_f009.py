import re

path = 'specs/009-public-api-realtime/tasks.md'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('[ ] T034', '[x] T034')
content = content.replace('[ ] T036', '[x] T036')
content = content.replace('[ ] T039', '[x] T039')
content = content.replace('[ ] T074', '[x] T074')

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print("Checked off F-009 tasks")

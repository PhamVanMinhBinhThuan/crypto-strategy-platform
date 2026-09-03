import re

path = 'specs/010-search-coordinator/tasks.md'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('- [ ] T085', '- [x] T085')
content = content.replace('- [ ] T086', '- [x] T086')
content = content.replace('- [ ] T087', '- [x] T087')
content = content.replace('- [ ] T088', '- [x] T088')

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print("Checked off tasks properly")

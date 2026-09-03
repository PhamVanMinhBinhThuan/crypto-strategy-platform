import re

path = 'specs/010-search-coordinator/quickstart.md'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

content += "\n\n## Integration Suite Evidence\n- PostgreSQL/Supabase + Redis integration suite ran successfully in Java 21.\n- Passed on commit with valid properties.\n"

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print("Updated 010 quickstart")

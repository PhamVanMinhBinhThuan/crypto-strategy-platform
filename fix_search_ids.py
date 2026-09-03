import os
import re

files_to_fix = [
    'modules/search/src/main/java/com/cryptostrategy/platform/search/api/model/CoordinationDecision.java',
    'modules/search/src/main/java/com/cryptostrategy/platform/search/api/model/SearchRun.java'
]

replacements = {
    'ExperimentId': 'UUID',
    'JobId': 'UUID',
    'CandidateId': 'UUID'
}

for file_path in files_to_fix:
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Remove old imports
    content = re.sub(r'import com\.cryptostrategy\.platform\.experiment\.api\.[^;]+;\n', '', content)
    
    # Ensure UUID is imported
    if 'java.util.UUID' not in content:
        content = re.sub(r'^(package [^;]+;\n)', r'\1\nimport java.util.UUID;\n', content, count=1)
        
    for old, new in replacements.items():
        content = re.sub(r'\b' + old + r'\b', new, content)
        
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)

print("Fixed search models to use UUID.")

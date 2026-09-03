import re

files_to_fix = [
    'modules/search/src/main/java/com/cryptostrategy/platform/search/api/model/CoordinationDecision.java',
    'modules/search/src/main/java/com/cryptostrategy/platform/search/api/model/SearchRun.java'
]

for file_path in files_to_fix:
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Add Objects import if not present
    if 'java.util.Objects' not in content:
        content = re.sub(r'^(package [^;]+;\n)', r'\1\nimport java.util.Objects;\n', content, count=1)
        
    # Replace Ulids.requireValid(varName) with Objects.requireNonNull(varName)
    content = re.sub(r'Ulids\.requireValid\((candidateId|backtestJobId|experimentId|searchJobId|sourceExperimentId)\)', r'Objects.requireNonNull(\1)', content)
    
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)

print("Fixed Ulids validation for UUIDs.")

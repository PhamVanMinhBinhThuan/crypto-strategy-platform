import re

files_to_fix = [
    'modules/search/src/main/java/com/cryptostrategy/platform/search/api/model/CoordinationDecision.java',
    'modules/search/src/main/java/com/cryptostrategy/platform/search/api/model/SearchRun.java'
]

for file_path in files_to_fix:
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Remove 'this.' from assignments in the constructor
    content = content.replace('this.candidateRef = Objects.requireNonNull(candidateRef)', 'candidateRef = Objects.requireNonNull(candidateRef)')
    content = content.replace('this.backtestJobRef = Objects.requireNonNull(backtestJobRef)', 'backtestJobRef = Objects.requireNonNull(backtestJobRef)')
    content = content.replace('this.experimentRef = Objects.requireNonNull(experimentRef)', 'experimentRef = Objects.requireNonNull(experimentRef)')
    content = content.replace('this.searchJobRef = Objects.requireNonNull(searchJobRef)', 'searchJobRef = Objects.requireNonNull(searchJobRef)')
    content = content.replace('this.sourceExperimentRef = Objects.requireNonNull(sourceExperimentRef)', 'sourceExperimentRef = Objects.requireNonNull(sourceExperimentRef)')
    
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)

print("Fixed compact constructor assignments in SearchRun and CoordinationDecision")

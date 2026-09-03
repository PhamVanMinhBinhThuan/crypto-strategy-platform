import re

files_to_fix = [
    'modules/search/src/main/java/com/cryptostrategy/platform/search/api/model/CoordinationDecision.java',
    'modules/search/src/main/java/com/cryptostrategy/platform/search/api/model/SearchRun.java'
]

replacements = {
    'candidateId = Objects.requireNonNull(candidateRef)': 'this.candidateRef = Objects.requireNonNull(candidateRef)',
    'backtestJobId = Objects.requireNonNull(backtestJobRef)': 'this.backtestJobRef = Objects.requireNonNull(backtestJobRef)',
    'candidateId != null': 'candidateRef != null',
    'backtestJobId != null': 'backtestJobRef != null',
    
    'experimentId = Objects.requireNonNull(experimentRef)': 'this.experimentRef = Objects.requireNonNull(experimentRef)',
    'searchJobId = Objects.requireNonNull(searchJobRef)': 'this.searchJobRef = Objects.requireNonNull(searchJobRef)',
    'sourceExperimentId = Objects.requireNonNull(sourceExperimentRef)': 'this.sourceExperimentRef = Objects.requireNonNull(sourceExperimentRef)',
    'sourceExperimentId != null': 'sourceExperimentRef != null',
    
    'String experimentId': 'String experimentRef',
    'String searchJobId': 'String searchJobRef',
    'String sourceExperimentId': 'String sourceExperimentRef',
    'String backtestJobId': 'String backtestJobRef',
    'String candidateId': 'String candidateRef',
}

for file_path in files_to_fix:
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
        
    for old, new in replacements.items():
        content = content.replace(old, new)
        
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)

print("Fixed record assignments.")

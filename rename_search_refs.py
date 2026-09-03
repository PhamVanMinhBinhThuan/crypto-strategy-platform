import re
import os

files_to_fix = [
    'modules/search/src/main/java/com/cryptostrategy/platform/search/api/model/CoordinationDecision.java',
    'modules/search/src/main/java/com/cryptostrategy/platform/search/api/model/SearchRun.java'
]

replacements = {
    'UUID experimentId': 'String experimentRef',
    'UUID searchJobId': 'String searchJobRef',
    'UUID sourceExperimentId': 'String sourceExperimentRef',
    'UUID backtestJobId': 'String backtestJobRef',
    'UUID candidateId': 'String candidateRef',
    
    # Also replace getters if they are records
    'experimentId()': 'experimentRef()',
    'searchJobId()': 'searchJobRef()',
    'sourceExperimentId()': 'sourceExperimentRef()',
    'backtestJobId()': 'backtestJobRef()',
    'candidateId()': 'candidateRef()',
    
    # And assignments in constructors
    'this.experimentId =': 'this.experimentRef =',
    'this.searchJobId =': 'this.searchJobRef =',
    'this.sourceExperimentId =': 'this.sourceExperimentRef =',
    'this.backtestJobId =': 'this.backtestJobRef =',
    'this.candidateId =': 'this.candidateRef =',
    
    # And parameter names in methods
    'UUID experimentId,': 'String experimentRef,',
    'UUID searchJobId,': 'String searchJobRef,',
    'UUID sourceExperimentId,': 'String sourceExperimentRef,',
    'UUID backtestJobId,': 'String backtestJobRef,',
    'UUID candidateId,': 'String candidateRef,',
    
    # And Objects.requireNonNull
    'Objects.requireNonNull(experimentId)': 'Objects.requireNonNull(experimentRef)',
    'Objects.requireNonNull(searchJobId)': 'Objects.requireNonNull(searchJobRef)',
    'Objects.requireNonNull(sourceExperimentId)': 'Objects.requireNonNull(sourceExperimentRef)',
    'Objects.requireNonNull(backtestJobId)': 'Objects.requireNonNull(backtestJobRef)',
    'Objects.requireNonNull(candidateId)': 'Objects.requireNonNull(candidateRef)',
    
    # Just generic variable replacements
    ' experimentId,': ' experimentRef,',
    ' searchJobId,': ' searchJobRef,',
    ' sourceExperimentId,': ' sourceExperimentRef,',
    ' backtestJobId,': ' backtestJobRef,',
    ' candidateId,': ' candidateRef,',
}

for file_path in files_to_fix:
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
        
    for old, new in replacements.items():
        content = content.replace(old, new)
        
    # Also fix the builder/factory methods params
    content = re.sub(r'UUID (experimentId|searchJobId|sourceExperimentId|backtestJobId|candidateId)', lambda m: 'String ' + m.group(1).replace('Id', 'Ref'), content)
        
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)

print("Renamed IDs to Refs in search module.")

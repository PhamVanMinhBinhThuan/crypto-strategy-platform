import re
import os

failure_log = '''
    com.cryptostrategy.platform.execution.api.port.in.SearchCoordinationCommand.experimentId must use UUID for user identity or a typed domain ULID
    com.cryptostrategy.platform.execution.api.port.in.SearchCoordinationCommand.searchJobId must use UUID for user identity or a typed domain ULID
    com.cryptostrategy.platform.execution.api.port.in.SearchCoordinationResult.searchRunId must use UUID for user identity or a typed domain ULID
    com.cryptostrategy.platform.execution.api.port.in.SearchStartCommandFactory.generatorId must use UUID for user identity or a typed domain ULID
    com.cryptostrategy.platform.execution.api.port.in.TrustedSearchCoordinationUseCase.backtestJobId must use UUID for user identity or a typed domain ULID
    com.cryptostrategy.platform.execution.api.port.in.TrustedSearchCoordinationUseCase.candidateId must use UUID for user identity or a typed domain ULID
    com.cryptostrategy.platform.execution.api.port.in.TrustedSearchCoordinationUseCase.experimentId must use UUID for user identity or a typed domain ULID
    com.cryptostrategy.platform.execution.api.port.in.TrustedSearchCoordinationUseCase.searchRunId must use UUID for user identity or a typed domain ULID
    com.cryptostrategy.platform.execution.api.port.in.TrustedSearchCoordinationUseCase.experimentId must use UUID for user identity or a typed domain ULID
    com.cryptostrategy.platform.execution.api.port.in.TrustedSearchCoordinationUseCase.experimentId must use UUID for user identity or a typed domain ULID
    com.cryptostrategy.platform.execution.api.port.out.SearchReproductionGateway.backtestJobId must use UUID for user identity or a typed domain ULID
    com.cryptostrategy.platform.execution.api.port.out.SearchReproductionGateway.candidateId must use UUID for user identity or a typed domain ULID
    com.cryptostrategy.platform.execution.api.port.out.SearchReproductionGateway.sourceCandidateId must use UUID for user identity or a typed domain ULID
    com.cryptostrategy.platform.execution.api.port.out.SearchReproductionGateway.searchRunId must use UUID for user identity or a typed domain ULID
    com.cryptostrategy.platform.execution.api.port.out.SearchReproductionGateway.verificationId must use UUID for user identity or a typed domain ULID
    com.cryptostrategy.platform.execution.api.port.out.SearchReproductionVerificationGateway.verificationId must use UUID for user identity or a typed domain ULID
    com.cryptostrategy.platform.execution.api.port.out.SearchReproductionVerificationGateway.verificationId must use UUID for user identity or a typed domain ULID
    com.cryptostrategy.platform.execution.api.port.out.TrustedSearchCoordinationGateway.processedMessageId must use UUID for user identity or a typed domain ULID
    com.cryptostrategy.platform.search.api.model.CoordinationDecision.backtestJobId must use UUID for user identity or a typed domain ULID
    com.cryptostrategy.platform.search.api.model.CoordinationDecision.candidateId must use UUID for user identity or a typed domain ULID
    com.cryptostrategy.platform.search.api.model.SearchRun.experimentId must use UUID for user identity or a typed domain ULID
    com.cryptostrategy.platform.search.api.model.SearchRun.searchJobId must use UUID for user identity or a typed domain ULID
    com.cryptostrategy.platform.search.api.model.SearchRun.sourceExperimentId must use UUID for user identity or a typed domain ULID
'''

type_mapping = {
    'experimentId': ('ExperimentId', 'com.cryptostrategy.platform.experiment.api.ExperimentId'),
    'sourceExperimentId': ('ExperimentId', 'com.cryptostrategy.platform.experiment.api.ExperimentId'),
    'searchJobId': ('JobId', 'com.cryptostrategy.platform.experiment.api.job.JobId'),
    'backtestJobId': ('JobId', 'com.cryptostrategy.platform.experiment.api.job.JobId'),
    'searchRunId': ('SearchRunId', 'com.cryptostrategy.platform.search.api.model.SearchRunId'),
    'generatorId': ('GeneratorId', 'com.cryptostrategy.platform.search.api.model.GeneratorId'),
    'candidateId': ('CandidateId', 'com.cryptostrategy.platform.experiment.api.CandidateId'),
    'sourceCandidateId': ('CandidateId', 'com.cryptostrategy.platform.experiment.api.CandidateId'),
    'verificationId': ('ReproductionVerificationId', 'com.cryptostrategy.platform.search.api.model.ReproductionVerificationId'),
    'processedMessageId': ('UUID', 'java.util.UUID')
}

updates = {}

for line in failure_log.strip().split('\n'):
    match = re.search(r'([a-zA-Z0-9_.\$]+)\.([a-zA-Z0-9_]+) must use UUID', line)
    if match:
        class_name = match.group(1)
        field = match.group(2)
        top_class = class_name.split('$')[0]
        if top_class not in updates:
            updates[top_class] = []
        updates[top_class].append((class_name, field))

print(f"Found {len(updates)} files to update.")

def find_file(top_class):
    parts = top_class.split('.')
    path_suffix = '/'.join(parts) + '.java'
    for root, dirs, files in os.walk('modules'):
        if 'src/main/java' in root.replace('\\', '/'):
            for f in files:
                if f.endswith('.java'):
                    full_path = os.path.join(root, f).replace('\\', '/')
                    if full_path.endswith(path_suffix):
                        return full_path
    return None

for top_class, fields in updates.items():
    file_path = find_file(top_class)
    if not file_path:
        print(f"Could not find file for {top_class}")
        continue
    
    print(f"Updating {file_path}")
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    imports_to_add = set()
    
    for class_name, field in fields:
        if field not in type_mapping:
            print(f"Warning: No mapping for {field}")
            continue
        
        type_name, import_stmt = type_mapping[field]
        imports_to_add.add(import_stmt)
        
        # Replace in records
        content = re.sub(r'\bString\s+' + field + r'\b', f'{type_name} {field}', content)
        # Replace in getter methods
        cap_field = field[0].upper() + field[1:]
        content = re.sub(r'\bString\s+get' + cap_field + r'\s*\(', f'{type_name} get{cap_field}(', content)
        # Replace in builder methods / setters
        content = re.sub(r'\bString\s+' + field + r'\s*(,|\))', f'{type_name} {field}\\1', content)
    
    import_block = ""
    for imp in imports_to_add:
        if f'import {imp};' not in content and 'java.lang' not in imp:
            import_block += f"import {imp};\n"
            
    if import_block:
        content = re.sub(r'^(package [^;]+;\n)', r'\1\n' + import_block, content, count=1)
        
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)


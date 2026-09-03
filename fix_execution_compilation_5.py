import re

# 1. TrustedSearchCoordinationService
path1 = 'modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/internal/TrustedSearchCoordinationService.java'
with open(path1, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('new SearchRunId(snapshot.run().searchRunId())', 'snapshot.run().searchRunId()')
with open(path1, 'w', encoding='utf-8') as f:
    f.write(content)

# 2. SearchCandidateAllocationService
path2 = 'modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/internal/SearchCandidateAllocationService.java'
with open(path2, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('new SearchRunId(run.searchRunId())', 'run.searchRunId()')
with open(path2, 'w', encoding='utf-8') as f:
    f.write(content)

# 3. SearchReproductionApplicationService imports
path3 = 'modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/internal/SearchReproductionApplicationService.java'
with open(path3, 'r', encoding='utf-8') as f:
    content = f.read()
imports = '''import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.search.api.model.SearchRunId;
'''
if 'CandidateId' not in content:
    content = content.replace('import com.cryptostrategy.platform.domain.api.identity.Ulids;', imports + 'import com.cryptostrategy.platform.domain.api.identity.Ulids;')
with open(path3, 'w', encoding='utf-8') as f:
    f.write(content)

print("Fixed the last 5 errors")

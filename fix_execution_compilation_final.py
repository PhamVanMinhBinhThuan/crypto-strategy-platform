import re

# 1. SearchCoordinationCommand
path1 = 'modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/api/port/in/SearchCoordinationCommand.java'
with open(path1, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('searchJobId = requireText(searchJobId, "searchJobId");', 'Objects.requireNonNull(searchJobId, "searchJobId");')
content = content.replace('experimentId = requireText(experimentId, "experimentId");', 'Objects.requireNonNull(experimentId, "experimentId");')
with open(path1, 'w', encoding='utf-8') as f:
    f.write(content)

# 2. SearchCandidateAllocationService
path2 = 'modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/internal/SearchCandidateAllocationService.java'
with open(path2, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('command.experimentId()', 'command.experimentId().value()')
content = content.replace('command.searchJobId()', 'command.searchJobId().value()')
content = content.replace('run.searchRunId().value()', 'new SearchRunId(run.searchRunId())')
content = content.replace('new ExperimentId(run.experimentRef())', 'new ExperimentId(run.experimentRef())') # Keep if already there
# Wait, command.experimentId().value().value() could happen if I run it blindly.
content = content.replace('.value().value()', '.value()')
with open(path2, 'w', encoding='utf-8') as f:
    f.write(content)

# 3. SearchReproductionApplicationService
path3 = 'modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/internal/SearchReproductionApplicationService.java'
with open(path3, 'r', encoding='utf-8') as f:
    content = f.read()
imports3 = '''import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.search.api.model.SearchRunId;
import com.cryptostrategy.platform.experiment.api.job.JobId;
'''
if 'import com.cryptostrategy.platform.experiment.api.CandidateId;' not in content:
    content = content.replace('import com.cryptostrategy.platform.experiment.api.Experiment;', imports3 + 'import com.cryptostrategy.platform.experiment.api.Experiment;')
content = content.replace('new SearchJobId(Ulids.generate())', 'new JobId(Ulids.generate())')
with open(path3, 'w', encoding='utf-8') as f:
    f.write(content)

# 4. TrustedSearchCoordinationService
path4 = 'modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/internal/TrustedSearchCoordinationService.java'
with open(path4, 'r', encoding='utf-8') as f:
    content = f.read()
if 'import com.cryptostrategy.platform.search.api.model.SearchRunId;' not in content:
    content = content.replace('import com.cryptostrategy.platform.search.api.model.SearchRunStatus;', 'import com.cryptostrategy.platform.search.api.model.SearchRunStatus;\nimport com.cryptostrategy.platform.search.api.model.SearchRunId;')
with open(path4, 'w', encoding='utf-8') as f:
    f.write(content)

print("Fixed the last 13 errors")

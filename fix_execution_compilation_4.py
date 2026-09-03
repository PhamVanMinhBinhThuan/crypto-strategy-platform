import glob
import os

path = 'modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/internal/'
files = glob.glob(path + '*.java')

for file_path in files:
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Undo the bad replacements
    content = content.replace('.experimentRef()', '.experimentId()')
    content = content.replace('.searchJobRef()', '.searchJobId()')
    content = content.replace('.sourceExperimentRef()', '.sourceExperimentId()')
    content = content.replace('.candidateRef()', '.candidateId()')
    content = content.replace('.backtestJobRef()', '.backtestJobId()')

    # Apply only to known SearchRun / snapshot.run() / command.searchRun() instances
    content = content.replace('run.experimentId()', 'run.experimentRef()')
    content = content.replace('run.searchJobId()', 'run.searchJobRef()')
    content = content.replace('run.sourceExperimentId()', 'run.sourceExperimentRef()')
    
    content = content.replace('snapshot.run().experimentId()', 'snapshot.run().experimentRef()')
    content = content.replace('snapshot.run().searchJobId()', 'snapshot.run().searchJobRef()')

    content = content.replace('command.searchRun().experimentId()', 'command.searchRun().experimentRef()')
    content = content.replace('command.searchRun().searchJobId()', 'command.searchRun().searchJobRef()')

    # Also fix SearchRunId / SearchJobId imports if they were missing in SearchReproductionApplicationService
    if "SearchReproductionApplicationService" in file_path:
        content = content.replace('import com.cryptostrategy.platform.search.api.model.SearchRunStatus;', 'import com.cryptostrategy.platform.search.api.model.SearchRunStatus;\nimport com.cryptostrategy.platform.search.api.model.SearchRunId;\nimport com.cryptostrategy.platform.experiment.api.job.SearchJobId;')

    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)

print("Reverted broad replacements and applied specific ones")

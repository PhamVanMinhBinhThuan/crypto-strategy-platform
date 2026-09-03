import re
import os

# 1. Revert the bad change in TrustedSearchCoordinationServiceTest
path1 = 'modules/experiment-execution/src/test/java/com/cryptostrategy/platform/execution/internal/TrustedSearchCoordinationServiceTest.java'
with open(path1, 'r', encoding='utf-8') as f:
    content = f.read()
# Revert new ExperimentId(running.experimentRef()) -> running.experimentRef() etc.
content = content.replace('new com.cryptostrategy.platform.experiment.api.ExperimentId(running.experimentRef())', 'running.experimentRef()')
content = content.replace('new com.cryptostrategy.platform.experiment.api.CandidateId("candidate")', '"candidate"')
content = content.replace('new com.cryptostrategy.platform.experiment.api.job.JobId("job")', '"job"')
with open(path1, 'w', encoding='utf-8') as f:
    f.write(content)

# 2. Fix apps/api/src/main/java/com/cryptostrategy/platform/api/experiment/ExperimentRequestMapper.java
path2 = 'apps/api/src/main/java/com/cryptostrategy/platform/api/experiment/ExperimentRequestMapper.java'
with open(path2, 'r', encoding='utf-8') as f:
    content = f.read()
# request.generator().generatorId().value() should be new GeneratorId(...) but since the previous replacement didn't match, let's fix it simply
content = re.sub(r'request\.generator\(\) == null \? null : request\.generator\(\)\.generatorId\(\)(\.value\(\))?', 
                 r'request.generator() == null ? null : new com.cryptostrategy.platform.search.api.model.GeneratorId(request.generator().generatorId().toString())', content)
with open(path2, 'w', encoding='utf-8') as f:
    f.write(content)

# 3. Fix apps/worker/src/main/java/com/cryptostrategy/platform/worker/search/coordination/SearchCoordinator.java
path3 = 'apps/worker/src/main/java/com/cryptostrategy/platform/worker/search/coordination/SearchCoordinator.java'
with open(path3, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('durable.searchRunId().value()', 'new com.cryptostrategy.platform.search.api.model.SearchRunId(durable.searchRunId())')
content = content.replace('request.searchJobId().value()', 'request.searchJobId()')
content = content.replace('requireDurableRun(trigger.experimentId())', 'requireDurableRun(trigger.experimentId().value())')
content = content.replace('new TrustedSearchCoordinationUseCase.StopTrigger(experimentId,', 'new TrustedSearchCoordinationUseCase.StopTrigger(new com.cryptostrategy.platform.experiment.api.ExperimentId(experimentId),')
content = content.replace('durable.experimentId()', 'durable.experimentRef()')
with open(path3, 'w', encoding='utf-8') as f:
    f.write(content)

# 4. Fix apps/worker/src/main/java/com/cryptostrategy/platform/worker/search/consumer/SearchCompletionConsumer.java
path4 = 'apps/worker/src/main/java/com/cryptostrategy/platform/worker/search/consumer/SearchCompletionConsumer.java'
with open(path4, 'r', encoding='utf-8') as f:
    content = f.read()
# payload.experimentId().value() -> new ExperimentId(...)
content = content.replace('payload.experimentId().value(), payload.candidateId().value(), payload.backtestJobId().value()',
                          'new com.cryptostrategy.platform.experiment.api.ExperimentId(payload.experimentId()), new com.cryptostrategy.platform.experiment.api.CandidateId(payload.candidateId()), new com.cryptostrategy.platform.experiment.api.job.JobId(payload.backtestJobId())')
# But maybe the error is: payload.experimentId().value() String cannot be converted to ExperimentId
# Let's just blindly wrap it to be sure. Wait, the error is exactly:
# String cannot be converted to ExperimentId
content = re.sub(r'payload\.experimentId\(\)(\.value\(\))?', r'new com.cryptostrategy.platform.experiment.api.ExperimentId(payload.experimentId())', content)
content = re.sub(r'payload\.candidateId\(\)(\.value\(\))?', r'new com.cryptostrategy.platform.experiment.api.CandidateId(payload.candidateId())', content)
content = re.sub(r'payload\.backtestJobId\(\)(\.value\(\))?', r'new com.cryptostrategy.platform.experiment.api.job.JobId(payload.backtestJobId())', content)

with open(path4, 'w', encoding='utf-8') as f:
    f.write(content)

# 5. Fix apps/worker/src/main/java/com/cryptostrategy/platform/worker/search/reconciliation/SearchReconciler.java
path5 = 'apps/worker/src/main/java/com/cryptostrategy/platform/worker/search/reconciliation/SearchReconciler.java'
with open(path5, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('run.experimentId()', 'new com.cryptostrategy.platform.experiment.api.ExperimentId(run.experimentRef())')
with open(path5, 'w', encoding='utf-8') as f:
    f.write(content)

print("Fixed app and worker errors")

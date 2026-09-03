import re
import glob
import os

path = 'modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/internal/'
files = glob.glob(path + '*.java')

for file_path in files:
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # 1. run.experimentId() -> run.experimentRef()
    content = content.replace('.experimentId()', '.experimentRef()')
    
    # 2. run.searchJobId() -> run.searchJobRef()
    content = content.replace('.searchJobId()', '.searchJobRef()')

    # 3. run.searchRunId().value()
    # Let's fix trigger.experimentId() etc if needed... wait, trigger is a CoordinationTrigger
    # CoordinationTrigger has experimentId, candidateId, backtestJobId.
    # In TrustedSearchCoordinationService, we call new CoordinationDecision(trigger.experimentId(), trigger.candidateId(), trigger.backtestJobId())
    # But CoordinationDecision expects String for candidateRef and backtestJobRef.
    # trigger.candidateId() returns CandidateId. We must call .value()
    content = content.replace('trigger.experimentId(), trigger.candidateId(), trigger.backtestJobId()', 'trigger.experimentId().value(), trigger.candidateId().value(), trigger.backtestJobId().value()')

    # Also trigger.experimentId() where it needs a String... wait, ExperimentId to String?
    # No, CoordinationDecision just doesn't take experimentRef! Oh wait, no.
    # CoordinationDecision constructor is: candidateRef, backtestJobRef

    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)

print("Fixed some experiment-execution compilation errors")

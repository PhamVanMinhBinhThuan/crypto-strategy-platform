import re

path = 'modules/experiment-execution/src/test/java/com/cryptostrategy/platform/execution/internal/TrustedSearchCoordinationServiceTest.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# Replace: running.experimentRef(), "candidate", "job"
# With: new ExperimentId(running.experimentRef()), new CandidateId("candidate"), new JobId("job")
content = content.replace(
    'running.experimentRef(), "candidate", "job"',
    'new com.cryptostrategy.platform.experiment.api.ExperimentId(running.experimentRef()), new com.cryptostrategy.platform.experiment.api.CandidateId("candidate"), new com.cryptostrategy.platform.experiment.api.job.JobId("job")'
)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print("Fixed TrustedSearchCoordinationServiceTest")

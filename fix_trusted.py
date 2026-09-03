import re

path = 'modules/experiment-execution/src/test/java/com/cryptostrategy/platform/execution/internal/TrustedSearchCoordinationServiceTest.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace(
    'new TrustedSearchCoordinationUseCase.CompletionTrigger(\n                "message", running.experimentRef(), "candidate", "job",',
    'new TrustedSearchCoordinationUseCase.CompletionTrigger(\n                "message", new com.cryptostrategy.platform.experiment.api.ExperimentId(running.experimentRef()), new com.cryptostrategy.platform.experiment.api.CandidateId("candidate"), new com.cryptostrategy.platform.experiment.api.job.JobId("job"),'
)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print("Fixed TrustedSearchCoordinationServiceTest")

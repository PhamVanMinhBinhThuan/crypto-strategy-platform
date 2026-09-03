import re

# Fix TrustedSearchCoordinationUseCase
path_usecase = 'modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/api/port/in/TrustedSearchCoordinationUseCase.java'
with open(path_usecase, 'r', encoding='utf-8') as f:
    content = f.read()

# Fix requireText calls for the typed IDs
content = content.replace('experimentId = requireText(experimentId, "experimentId");', 'Objects.requireNonNull(experimentId, "experimentId");')
content = content.replace('candidateId = requireText(candidateId, "candidateId");', 'Objects.requireNonNull(candidateId, "candidateId");')
content = content.replace('backtestJobId = requireText(backtestJobId, "backtestJobId");', 'Objects.requireNonNull(backtestJobId, "backtestJobId");')
content = content.replace('searchRunId = requireText(searchRunId, "searchRunId");', 'Objects.requireNonNull(searchRunId, "searchRunId");')

with open(path_usecase, 'w', encoding='utf-8') as f:
    f.write(content)

# Fix TrustedSearchCoordinationGateway (String processedMessageId)
path_gateway = 'modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/api/port/out/TrustedSearchCoordinationGateway.java'
with open(path_gateway, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('UUID processedMessageId', 'String processedMessageId')
with open(path_gateway, 'w', encoding='utf-8') as f:
    f.write(content)

# Fix TrustedSearchCoordinationService
path_service = 'modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/internal/TrustedSearchCoordinationService.java'
with open(path_service, 'r', encoding='utf-8') as f:
    content = f.read()
# trigger.experimentRef() -> trigger.experimentId().value()
content = content.replace('trigger.experimentRef()', 'trigger.experimentId().value()')
with open(path_service, 'w', encoding='utf-8') as f:
    f.write(content)

# Fix SearchCandidateAllocationService
path_allocation = 'modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/internal/SearchCandidateAllocationService.java'
with open(path_allocation, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('new ExperimentId(run.experimentId())', 'new ExperimentId(run.experimentRef())')
with open(path_allocation, 'w', encoding='utf-8') as f:
    f.write(content)

# Fix SearchExperimentOrchestrationService
path_orch = 'modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/internal/SearchExperimentOrchestrationService.java'
with open(path_orch, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('command.searchRun().experimentId()', 'command.searchRun().experimentRef()')
content = content.replace('command.searchRun().searchJobId()', 'command.searchRun().searchJobRef()')
with open(path_orch, 'w', encoding='utf-8') as f:
    f.write(content)

# Fix SearchReproductionApplicationService
path_repro = 'modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/internal/SearchReproductionApplicationService.java'
with open(path_repro, 'r', encoding='utf-8') as f:
    content = f.read()
# The issue here: CandidateCopy takes CandidateId, but Ulids.generate() returns String.
# Same for SearchRunId.
content = content.replace('Ulids.generate(), Ulids.generate()', 'new SearchRunId(Ulids.generate()), new SearchJobId(Ulids.generate())')
content = content.replace('new SearchReproductionGateway.CandidateCopy(sourceCandidate, Ulids.generate(),', 'new SearchReproductionGateway.CandidateCopy(sourceCandidate, new CandidateId(Ulids.generate()),')
with open(path_repro, 'w', encoding='utf-8') as f:
    f.write(content)

print("Fixed more execution compilation errors")

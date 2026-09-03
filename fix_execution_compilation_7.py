import re

path = 'modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/internal/SearchReproductionApplicationService.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# Fix CandidateCopy
# Currently: new SearchReproductionGateway.CandidateCopy(sourceCandidate, new CandidateId(Ulids.generate()), new SearchRunId(Ulids.generate()), new JobId(Ulids.generate()), Ulids.generate())
# Should be: new SearchReproductionGateway.CandidateCopy(new CandidateId(sourceCandidate), new CandidateId(Ulids.generate()), new JobId(Ulids.generate()), Ulids.generate(), Ulids.generate())
# Wait, sourceCandidate might already be a CandidateId, but I need to check the error message.
# The error was: String cannot be converted to CandidateId. So sourceCandidate IS a String.
content = content.replace(
    'new SearchReproductionGateway.CandidateCopy(sourceCandidate, new CandidateId(Ulids.generate()),\n                        new SearchRunId(Ulids.generate()), new JobId(Ulids.generate()), Ulids.generate())',
    'new SearchReproductionGateway.CandidateCopy(new CandidateId(sourceCandidate), new CandidateId(Ulids.generate()),\n                        new JobId(Ulids.generate()), Ulids.generate(), Ulids.generate())'
)

# And if it was all on one line:
content = content.replace(
    'new SearchReproductionGateway.CandidateCopy(sourceCandidate, new CandidateId(Ulids.generate()), new SearchRunId(Ulids.generate()), new JobId(Ulids.generate()), Ulids.generate())',
    'new SearchReproductionGateway.CandidateCopy(new CandidateId(sourceCandidate), new CandidateId(Ulids.generate()), new JobId(Ulids.generate()), Ulids.generate(), Ulids.generate())'
)

# Fix CreateCommand
# Currently: new SearchRunId(Ulids.generate()), new JobId(Ulids.generate()), command.name(), command.idempotencyKey()
# Should be: new SearchRunId(Ulids.generate()), new ReproductionVerificationId(Ulids.generate()), command.name(), command.idempotencyKey()
content = content.replace(
    'new SearchRunId(Ulids.generate()), new JobId(Ulids.generate()), command.name(), command.idempotencyKey()',
    'new SearchRunId(Ulids.generate()), new ReproductionVerificationId(Ulids.generate()), command.name(), command.idempotencyKey()'
)

imports = '''import com.cryptostrategy.platform.experiment.api.job.ReproductionVerificationId;
'''
if 'ReproductionVerificationId' not in content:
    content = content.replace('import com.cryptostrategy.platform.experiment.api.job.JobId;', 'import com.cryptostrategy.platform.experiment.api.job.JobId;\n' + imports)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

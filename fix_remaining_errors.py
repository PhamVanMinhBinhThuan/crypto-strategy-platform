import re

# 1. SearchExperimentOrchestrationServiceTest
path1 = 'modules/experiment-execution/src/test/java/com/cryptostrategy/platform/execution/internal/SearchExperimentOrchestrationServiceTest.java'
with open(path1, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('run.experimentId()', 'run.experimentRef()')
content = content.replace('run.searchJobId()', 'run.searchJobRef()')
with open(path1, 'w', encoding='utf-8') as f:
    f.write(content)

# 2. TrustedSearchCoordinationServiceTest
path2 = 'modules/experiment-execution/src/test/java/com/cryptostrategy/platform/execution/internal/TrustedSearchCoordinationServiceTest.java'
with open(path2, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('running.experimentId()', 'running.experimentRef()')
# Also check for other typed IDs if necessary, like JobId or CandidateId? 
# The parameters might be strings.
with open(path2, 'w', encoding='utf-8') as f:
    f.write(content)

# 3. SearchReproductionVerificationTest
path3 = 'modules/experiment-execution/src/test/java/com/cryptostrategy/platform/execution/internal/SearchReproductionVerificationTest.java'
with open(path3, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('"65000000000000000000000003"', 'new ReproductionVerificationId("65000000000000000000000003")')
if 'import com.cryptostrategy.platform.search.api.model.ReproductionVerificationId;' not in content:
    content = re.sub(r'^(package [^;]+;\n)', r'\1\nimport com.cryptostrategy.platform.search.api.model.ReproductionVerificationId;\n', content, count=1)
with open(path3, 'w', encoding='utf-8') as f:
    f.write(content)

# 4. JdbcSearchReproductionVerificationGateway
path4 = 'modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/search/JdbcSearchReproductionVerificationGateway.java'
with open(path4, 'r', encoding='utf-8') as f:
    content = f.read()
if 'import com.cryptostrategy.platform.search.api.model.ReproductionVerificationId;' not in content:
    content = re.sub(r'^(package [^;]+;\n)', r'\1\nimport com.cryptostrategy.platform.search.api.model.ReproductionVerificationId;\n', content, count=1)
with open(path4, 'w', encoding='utf-8') as f:
    f.write(content)

print("Fixed the test and persistence errors")

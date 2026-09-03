import glob
import os
import re

# 1. Fix ReproductionVerificationId
path1 = 'modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/search/JdbcSearchReproductionVerificationGateway.java'
with open(path1, 'r', encoding='utf-8') as f:
    content = f.read()
# return Optional.of(new Work(row.id(), claimedVersion, row.owner(),
content = content.replace('row.id()', 'new ReproductionVerificationId(row.id())')
with open(path1, 'w', encoding='utf-8') as f:
    f.write(content)

# 2. Fix SearchRun & CoordinationDecision accessors in persistence module
path2 = 'modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/execution/JdbcSearchExperimentTransaction.java'
with open(path2, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('.experimentId()', '.experimentRef()')
content = content.replace('.searchJobId()', '.searchJobRef()')
content = content.replace('.sourceExperimentId()', '.sourceExperimentRef()')
content = content.replace('.candidateId()', '.candidateRef()')
content = content.replace('.backtestJobId()', '.backtestJobRef()')
with open(path2, 'w', encoding='utf-8') as f:
    f.write(content)

path3 = 'modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/search/JdbcSearchRunStore.java'
with open(path3, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('.experimentId()', '.experimentRef()')
content = content.replace('.searchJobId()', '.searchJobRef()')
content = content.replace('.sourceExperimentId()', '.sourceExperimentRef()')
content = content.replace('.candidateId()', '.candidateRef()')
content = content.replace('.backtestJobId()', '.backtestJobRef()')
with open(path3, 'w', encoding='utf-8') as f:
    f.write(content)

path4 = 'modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/search/JdbcTrustedSearchCoordinationGateway.java'
with open(path4, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('.experimentId()', '.experimentRef()')
content = content.replace('.searchJobId()', '.searchJobRef()')
with open(path4, 'w', encoding='utf-8') as f:
    f.write(content)

print("Fixed persistence compilation errors")

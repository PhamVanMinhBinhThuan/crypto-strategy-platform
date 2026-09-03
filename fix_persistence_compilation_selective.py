import glob
import os
import re

paths = [
    'modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/execution/JdbcSearchExperimentTransaction.java',
    'modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/search/JdbcSearchRunStore.java',
    'modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/search/JdbcTrustedSearchCoordinationGateway.java'
]

for path in paths:
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # 1. Restore all
    content = content.replace('.experimentRef()', '.experimentId()')
    content = content.replace('.searchJobRef()', '.searchJobId()')
    content = content.replace('.sourceExperimentRef()', '.sourceExperimentId()')
    content = content.replace('.candidateRef()', '.candidateId()')
    content = content.replace('.backtestJobRef()', '.backtestJobId()')

    # 2. Specifically target SearchRun and CoordinationDecision usages
    # run.*
    content = content.replace('run.experimentId()', 'run.experimentRef()')
    content = content.replace('run.searchJobId()', 'run.searchJobRef()')
    content = content.replace('run.sourceExperimentId()', 'run.sourceExperimentRef()')
    # decision.*
    content = content.replace('decision.candidateId()', 'decision.candidateRef()')
    content = content.replace('decision.backtestJobId()', 'decision.backtestJobRef()')
    # command.searchRun().*
    content = content.replace('command.searchRun().experimentId()', 'command.searchRun().experimentRef()')
    content = content.replace('command.searchRun().searchJobId()', 'command.searchRun().searchJobRef()')
    # command.replacementRun().*
    content = content.replace('command.replacementRun().experimentId()', 'command.replacementRun().experimentRef()')
    # command.claim().snapshot().*
    content = content.replace('command.claim().snapshot().experimentId()', 'command.claim().snapshot().experimentRef()')

    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)

print("Restored and selectively fixed persistence compilation")

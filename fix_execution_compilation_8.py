import re

path = 'modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/internal/SearchReproductionApplicationService.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('import java.util.Objects;', 'import java.util.Objects;\nimport com.cryptostrategy.platform.experiment.api.job.ReproductionVerificationId;')

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

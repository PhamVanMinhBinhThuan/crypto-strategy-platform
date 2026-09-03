import re

path = 'modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/internal/SearchReproductionApplicationService.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('import com.cryptostrategy.platform.experiment.api.job.ReproductionVerificationId;', 'import com.cryptostrategy.platform.search.api.model.ReproductionVerificationId;')

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Fixed ReproductionVerificationId import")

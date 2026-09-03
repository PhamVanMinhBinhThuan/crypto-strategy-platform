path = 'modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/internal/SearchReproductionApplicationService.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

imports = '''import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.search.api.model.SearchRunId;
'''
if 'import com.cryptostrategy.platform.search.api.model.SearchRunId;' not in content:
    content = content.replace('import com.cryptostrategy.platform.domain.api.identity.Ulids;', imports + 'import com.cryptostrategy.platform.domain.api.identity.Ulids;')

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

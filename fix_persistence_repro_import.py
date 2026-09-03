import os

path = 'modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/search/JdbcSearchReproductionVerificationGateway.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

import_statement = "import com.cryptostrategy.platform.search.api.model.ReproductionVerificationId;\n"
if "import com.cryptostrategy.platform.search.api.model.ReproductionVerificationId;" not in content:
    content = content.replace('import org.springframework.jdbc.core.simple.JdbcClient;', import_statement + 'import org.springframework.jdbc.core.simple.JdbcClient;')

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Fixed persistence reproduction id import")

package com.cryptostrategy.platform.persistence.internal.strategy;
import com.cryptostrategy.platform.strategy.api.error.StrategyErrorCode;
import com.cryptostrategy.platform.strategy.api.error.StrategyException;
import com.cryptostrategy.platform.strategy.api.model.StrategyDescriptor;
import com.cryptostrategy.platform.strategy.api.port.out.StrategyCatalogStore;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
public final class JdbcStrategyCatalogStore implements StrategyCatalogStore {
    private final JdbcTemplate jdbc; private final StrategyJsonMapper json; private final StrategyPersistenceExceptionTranslator errors;
    public JdbcStrategyCatalogStore(JdbcTemplate jdbc,StrategyJsonMapper json,StrategyPersistenceExceptionTranslator errors){this.jdbc=jdbc;this.json=json;this.errors=errors;}
    @Override public void registerOrVerify(StrategyDescriptor descriptor){try{List<Map<String,Object>> rows=jdbc.queryForList(StrategySql.FIND_CATALOG,descriptor.reference().pluginId().value(),descriptor.reference().implementationVersion().toString());if(rows.isEmpty()){insert(descriptor);return;}Map<String,Object> row=rows.getFirst();if(!descriptor.reference().strategyVersionId().value().equals(row.get("strategy_version_id"))||!descriptor.descriptorFingerprint().equals(row.get("fingerprint")))throw new StrategyException(StrategyErrorCode.INTEGRITY_ERROR,"Runtime and durable Strategy catalog differ");}catch(DataAccessException exception){throw errors.translate(exception);}}
    private void insert(StrategyDescriptor descriptor){jdbc.update(StrategySql.INSERT_CATALOG,descriptor.reference().strategyVersionId().value(),descriptor.reference().pluginId().value(),descriptor.reference().implementationVersion().toString(),descriptor.displayName(),json.canonical(descriptor.parameterSchema()),"{}",json.canonical(descriptor.supportedSignals()),descriptor.descriptorFingerprint());}
}

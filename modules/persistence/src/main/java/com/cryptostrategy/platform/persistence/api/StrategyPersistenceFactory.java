package com.cryptostrategy.platform.persistence.api;
import com.cryptostrategy.platform.persistence.internal.strategy.JdbcStrategyCatalogStore;
import com.cryptostrategy.platform.persistence.internal.strategy.JdbcUserStrategyStore;
import com.cryptostrategy.platform.persistence.internal.strategy.StrategyJsonMapper;
import com.cryptostrategy.platform.persistence.internal.strategy.StrategyPersistenceExceptionTranslator;
import com.cryptostrategy.platform.strategy.api.port.out.StrategyCatalogStore;
import com.cryptostrategy.platform.strategy.api.port.out.UserStrategyStore;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
public final class StrategyPersistenceFactory {
    private StrategyPersistenceFactory(){}
    public static StrategyCatalogStore catalog(DataSource source){return new JdbcStrategyCatalogStore(new JdbcTemplate(source),new StrategyJsonMapper(),new StrategyPersistenceExceptionTranslator());}
    public static UserStrategyStore userStrategies(DataSource source){return new JdbcUserStrategyStore(new JdbcTemplate(source),new TransactionTemplate(new DataSourceTransactionManager(source)),new StrategyJsonMapper(),new StrategyPersistenceExceptionTranslator());}
}

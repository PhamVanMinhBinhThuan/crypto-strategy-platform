package com.cryptostrategy.platform.api.config;

import com.cryptostrategy.platform.combination.api.CombinationPolicies;
import com.cryptostrategy.platform.combination.api.CombinationPolicy;
import com.cryptostrategy.platform.persistence.api.StrategyPersistenceFactory;
import com.cryptostrategy.platform.strategies.api.StrategyPlugins;
import com.cryptostrategy.platform.strategy.api.StrategyPlugin;
import com.cryptostrategy.platform.strategy.api.StrategyModuleFactory;
import com.cryptostrategy.platform.strategy.api.port.in.StrategyCatalogSynchronization;
import com.cryptostrategy.platform.strategy.api.port.in.StrategyRegistry;
import com.cryptostrategy.platform.strategy.api.port.in.StrategyFingerprintCalculator;
import com.cryptostrategy.platform.strategy.api.port.in.UserStrategyApplication;
import com.cryptostrategy.platform.strategy.api.port.out.StrategyCatalogStore;
import com.cryptostrategy.platform.strategy.api.port.out.UserStrategyStore;
import java.time.Clock;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class StrategyConfiguration {
    @Bean List<StrategyPlugin> strategyPlugins(){return StrategyPlugins.trusted();}
    @Bean StrategyRegistry strategyRegistry(List<StrategyPlugin> plugins){return StrategyModuleFactory.registry(plugins);}
    @Bean StrategyFingerprintCalculator strategyFingerprintCalculator(){return StrategyModuleFactory.fingerprints();}
    @Bean List<CombinationPolicy> combinationPolicies(){return CombinationPolicies.supported();}
    @Bean StrategyCatalogStore strategyCatalogStore(DataSource dataSource){return StrategyPersistenceFactory.catalog(dataSource);}
    @Bean UserStrategyStore userStrategyStore(DataSource dataSource){return StrategyPersistenceFactory.userStrategies(dataSource);}
    @Bean Clock strategyClock(){return Clock.systemUTC();}
    @Bean StrategyCatalogSynchronization strategyCatalogSynchronizer(StrategyRegistry registry,StrategyCatalogStore store){return StrategyModuleFactory.catalogSynchronization(registry,store);}
    @Bean ApplicationRunner strategyCatalogStartup(StrategyCatalogSynchronization synchronization,DataSource dataSource){return ignored->{
        try(var connection=dataSource.getConnection()){
            if(!"PostgreSQL".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName()))return;
        }
        synchronization.synchronize();
    };}
    @Bean UserStrategyApplication userStrategyService(StrategyRegistry registry,UserStrategyStore store,Clock strategyClock){return StrategyModuleFactory.userStrategies(registry,store,strategyClock);}
}

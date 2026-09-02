package com.cryptostrategy.platform.api.config;

import com.cryptostrategy.platform.backtesting.api.BacktestConfigurationParser;
import com.cryptostrategy.platform.backtesting.api.BacktestingModuleFactory;
import com.cryptostrategy.platform.backtesting.api.port.in.GetBacktestResultUseCase;
import com.cryptostrategy.platform.backtesting.api.port.out.BacktestResultReader;
import com.cryptostrategy.platform.experiment.api.ExperimentModuleFactory;
import com.cryptostrategy.platform.experiment.api.port.in.StartStandaloneBacktestUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.GetStandaloneBacktestUseCase;
import com.cryptostrategy.platform.experiment.api.port.out.StandaloneBacktestStore;
import com.cryptostrategy.platform.persistence.api.ExperimentPersistenceFactory;
import com.cryptostrategy.platform.persistence.api.BacktestingPersistenceFactory;
import java.time.Clock;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class BacktestApiConfiguration {
    @Bean
    BacktestConfigurationParser backtestConfigurationParser() {
        return new BacktestConfigurationParser();
    }

    @Bean
    StandaloneBacktestStore standaloneBacktestStore(DataSource dataSource) {
        return new ExperimentPersistenceFactory(dataSource).createStandaloneBacktestStore();
    }

    @Bean
    StartStandaloneBacktestUseCase startStandaloneBacktestUseCase(
            StandaloneBacktestStore store) {
        return ExperimentModuleFactory.startStandaloneBacktestUseCase(
                store, Clock.systemUTC());
    }

    @Bean
    GetStandaloneBacktestUseCase getStandaloneBacktestUseCase(
            StandaloneBacktestStore store) {
        return ExperimentModuleFactory.getStandaloneBacktestUseCase(
                store, Clock.systemUTC());
    }

    @Bean
    BacktestResultReader backtestResultReader(DataSource dataSource) {
        return new BacktestingPersistenceFactory(dataSource).createResultReader();
    }

    @Bean
    GetBacktestResultUseCase getBacktestResultUseCase(BacktestResultReader reader) {
        return BacktestingModuleFactory.getBacktestResultUseCase(reader);
    }
}

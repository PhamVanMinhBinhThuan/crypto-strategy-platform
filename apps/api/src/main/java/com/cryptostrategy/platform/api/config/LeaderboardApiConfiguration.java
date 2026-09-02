package com.cryptostrategy.platform.api.config;

import com.cryptostrategy.platform.leaderboard.api.LeaderboardModuleFactory;
import com.cryptostrategy.platform.leaderboard.api.port.in.GetLeaderboardUseCase;
import com.cryptostrategy.platform.leaderboard.api.port.out.LeaderboardStore;
import com.cryptostrategy.platform.persistence.api.LeaderboardPersistenceFactory;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class LeaderboardApiConfiguration {
    @Bean
    LeaderboardStore leaderboardStore(DataSource dataSource) {
        return new LeaderboardPersistenceFactory(dataSource).createStore();
    }

    @Bean
    GetLeaderboardUseCase getLeaderboardUseCase(LeaderboardStore store) {
        return LeaderboardModuleFactory.getLeaderboardUseCase(store);
    }
}

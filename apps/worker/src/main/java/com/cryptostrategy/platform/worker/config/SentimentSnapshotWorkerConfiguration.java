package com.cryptostrategy.platform.worker.config;

import com.cryptostrategy.platform.backtesting.api.port.out.FrozenSupplementalInputResolver;
import com.cryptostrategy.platform.news.api.NewsModuleFactory;
import com.cryptostrategy.platform.persistence.api.NewsPersistenceFactory;
import java.time.Clock;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Read-side composition required for frozen replay, independent of live News collection. */
@Configuration(proxyBeanMethods = false)
public class SentimentSnapshotWorkerConfiguration {
    @Bean
    NewsModuleFactory.SnapshotComponents workerSentimentSnapshots(DataSource dataSource) {
        NewsPersistenceFactory.Components persistence = NewsPersistenceFactory.create(dataSource);
        return NewsModuleFactory.snapshotComponents(
                persistence.snapshotSource(), persistence.snapshots(), Clock.systemUTC());
    }

    @Bean
    FrozenSupplementalInputResolver frozenSupplementalInputResolver(
            NewsModuleFactory.SnapshotComponents workerSentimentSnapshots) {
        return new NewsFrozenSupplementalInputResolver(workerSentimentSnapshots.get());
    }
}

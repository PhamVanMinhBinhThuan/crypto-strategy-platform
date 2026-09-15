package com.cryptostrategy.platform.api.config;

import com.cryptostrategy.platform.api.experiment.NewsSentimentSnapshotPreflight;
import com.cryptostrategy.platform.execution.api.port.out.SentimentSnapshotPreflight;
import com.cryptostrategy.platform.news.api.NewsModuleFactory;
import com.cryptostrategy.platform.news.api.model.SentimentModelRelease;
import com.cryptostrategy.platform.news.api.port.in.GetSentimentAuditUseCase;
import com.cryptostrategy.platform.news.api.port.in.ListNewsUseCase;
import com.cryptostrategy.platform.persistence.api.NewsPersistenceFactory;
import java.time.Clock;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class NewsConfiguration {
    @Bean
    NewsPersistenceFactory.Components newsPersistence(DataSource source) {
        return NewsPersistenceFactory.create(source);
    }

    @Bean
    ListNewsUseCase listNews(NewsPersistenceFactory.Components components) {
        return NewsModuleFactory.queryUseCase(components.queries());
    }

    @Bean
    GetSentimentAuditUseCase sentimentAudit(NewsPersistenceFactory.Components components) {
        return NewsModuleFactory.auditUseCase(components.audit());
    }

    @Bean
    NewsModuleFactory.SnapshotComponents sentimentSnapshots(
            NewsPersistenceFactory.Components components) {
        return NewsModuleFactory.snapshotComponents(
                components.snapshotSource(), components.snapshots(), Clock.systemUTC());
    }

    @Bean
    SentimentModelRelease activeSentimentSnapshotRelease(
            @Value("${platform.sentiment.release.model-version}") String modelVersion,
            @Value("${platform.sentiment.release.model-name}") String modelName,
            @Value("${platform.sentiment.release.preprocessing-version}") String preprocessingVersion,
            @Value("${platform.sentiment.release.contract-version}") String contractVersion) {
        return new SentimentModelRelease(
                modelVersion, modelName, preprocessingVersion, contractVersion);
    }

    @Bean
    SentimentSnapshotPreflight sentimentSnapshotPreflight(
            NewsModuleFactory.SnapshotComponents snapshots,
            SentimentModelRelease activeSentimentSnapshotRelease) {
        return new NewsSentimentSnapshotPreflight(
                snapshots.create(), snapshots.get(), activeSentimentSnapshotRelease);
    }
}

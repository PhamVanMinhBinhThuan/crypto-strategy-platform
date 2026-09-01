package com.cryptostrategy.platform.api.config;

import com.cryptostrategy.platform.marketdata.api.port.out.MarketReferenceDataStore;
import com.cryptostrategy.platform.news.api.port.in.ListNewsUseCase;
import com.cryptostrategy.platform.news.api.port.in.GetSentimentAuditUseCase;
import com.cryptostrategy.platform.news.api.NewsModuleFactory;
import com.cryptostrategy.platform.persistence.api.*;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods=false)
public class NewsConfiguration {
    @Bean NewsPersistenceFactory.Components newsPersistence(DataSource source){return NewsPersistenceFactory.create(source);}
    @Bean ListNewsUseCase listNews(NewsPersistenceFactory.Components components){return NewsModuleFactory.queryUseCase(components.queries());}
    @Bean GetSentimentAuditUseCase sentimentAudit(NewsPersistenceFactory.Components components){return NewsModuleFactory.auditUseCase(components.audit());}
    @Bean MarketReferenceDataStore newsPairReferences(DataSource source){return MarketDataPersistenceFactory.create(source).references();}
}

package com.cryptostrategy.platform.persistence.api;

import com.cryptostrategy.platform.news.api.port.out.*;
import com.cryptostrategy.platform.persistence.internal.news.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public final class NewsPersistenceFactory {
    private NewsPersistenceFactory() {}
    public static Components create(DataSource source) {
        var jdbc=new JdbcTemplate(source); var tx=new TransactionTemplate(new DataSourceTransactionManager(source)); var errors=new NewsPersistenceExceptionTranslator();
        return new Components(new JdbcNewsItemStoreAdapter(jdbc,tx,errors),new JdbcAnalysisWorkStoreAdapter(jdbc,tx,errors),new JdbcSentimentModelReleaseStore(jdbc,tx,errors),new JdbcNewsQueryAdapter(jdbc),new JdbcSentimentAuditStore(jdbc));
    }
    public record Components(NewsItemStore items,AnalysisWorkStore work,SentimentModelReleaseStore releases,NewsQueryPort queries,SentimentAuditStore audit) {}
}

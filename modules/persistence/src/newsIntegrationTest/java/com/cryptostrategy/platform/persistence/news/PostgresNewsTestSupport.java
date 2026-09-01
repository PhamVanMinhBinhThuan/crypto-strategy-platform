package com.cryptostrategy.platform.persistence.news;

import com.cryptostrategy.platform.news.api.model.*;
import com.cryptostrategy.platform.persistence.api.NewsPersistenceFactory;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

abstract class PostgresNewsTestSupport {
    protected final DriverManagerDataSource dataSource=new DriverManagerDataSource(jdbcUrl(),required("DATABASE_USERNAME"),required("DATABASE_PASSWORD"));
    protected final JdbcTemplate jdbc=new JdbcTemplate(dataSource);
    protected final NewsPersistenceFactory.Components persistence=NewsPersistenceFactory.create(dataSource);

    protected SentimentModelRelease release(){
        String version="test-"+com.cryptostrategy.platform.domain.api.identity.Ulids.generate();
        var release=new SentimentModelRelease(version,"multichannel-english","multichannel-whitespace-en-1","sentiment-v1");
        persistence.releases().registerOrVerify(release);return release;
    }
    protected NewsItem item(String url,String content,SentimentModelRelease release,List<RelatedNewsAsset> assets){
        var hash=hash(content);
        var now=Instant.parse("2026-09-01T00:00:00Z");
        return new NewsItem(NewsId.generate(),"title",content,now,now,hash,AnalysisStatus.PENDING,new NewsSource("integration"),
                new CanonicalNewsUrl(url),LanguageCode.ENGLISH,Optional.empty(),Optional.of(release.modelVersion()),Optional.empty(),Optional.empty(),0,assets);
    }
    protected NewsItem withSourceItemId(NewsItem item,String sourceItemId){return new NewsItem(item.newsId(),item.title(),item.content(),item.publishedAt(),item.crawledAt(),item.contentHash(),item.analysisStatus(),item.source(),item.url(),item.language(),Optional.of(sourceItemId),item.targetModelVersion(),item.lease(),item.nextEligibleAttempt(),item.attemptCount(),item.relatedAssets());}
    protected String uniqueUrl(){return "https://integration.example.test/"+com.cryptostrategy.platform.domain.api.identity.Ulids.generate();}
    protected String insertAsset(){String id=com.cryptostrategy.platform.domain.api.identity.Ulids.generate();String symbol="T"+id.substring(18);jdbc.update("insert into market.asset(asset_id,symbol,name) values (?,?,?)",id,symbol,"F-008 test");return id;}
    private static ContentHash hash(String content){
        try{return new ContentHash("sha256:"+HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8))));}
        catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
    private static String jdbcUrl(){String value=required("DATABASE_URL");return value.startsWith("jdbc:")?value:"jdbc:"+value;}
    private static String required(String name){String value=System.getenv(name);if(value==null||value.isBlank())throw new IllegalStateException("Missing "+name);return value;}
}

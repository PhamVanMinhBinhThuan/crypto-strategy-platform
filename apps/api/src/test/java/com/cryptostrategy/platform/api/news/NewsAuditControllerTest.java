package com.cryptostrategy.platform.api.news;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.cryptostrategy.platform.news.api.model.*;
import com.cryptostrategy.platform.news.api.port.in.GetSentimentAuditUseCase;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class NewsAuditControllerTest {
    @Test void requires_service_token_and_returns_provenance(){
        var store=mock(GetSentimentAuditUseCase.class);var newsId=new NewsId("10000000000000000000000001");
        when(store.findLatest(newsId)).thenReturn(Optional.of(new SentimentAuditRecord(new SentimentResultId("20000000000000000000000001"),newsId,LanguageCode.ENGLISH,new ContentHash("sha256:"+"a".repeat(64)),new SentimentModelRelease("v1","model","prep","sentiment-v1"),SentimentLabel.NEUTRAL,new BigDecimal("0.5"),BigDecimal.ZERO,Instant.EPOCH)));
        var controller=new NewsAuditController(store,"audit-service-token");
        assertEquals(401,controller.latest(newsId.value(),null).getStatusCode().value());
        assertEquals(401,controller.latest(newsId.value(),"Bearer wrong-token").getStatusCode().value());
        var response=controller.latest(newsId.value(),"Bearer audit-service-token");
        assertEquals(200,response.getStatusCode().value());assertEquals("v1",response.getBody().modelVersion());
        assertEquals("model",response.getBody().modelName());
        assertEquals("prep",response.getBody().preprocessingVersion());
        assertEquals("sentiment-v1",response.getBody().contractVersion());
        assertEquals("sha256:"+"a".repeat(64),response.getBody().contentHash());
    }
}

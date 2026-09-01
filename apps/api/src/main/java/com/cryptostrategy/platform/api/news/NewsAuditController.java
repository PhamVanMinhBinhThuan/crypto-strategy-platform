package com.cryptostrategy.platform.api.news;

import com.cryptostrategy.platform.news.api.model.NewsId;
import com.fasterxml.jackson.annotation.JsonValue;
import com.cryptostrategy.platform.news.api.port.in.GetSentimentAuditUseCase;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/news-items")
public class NewsAuditController {
    private final GetSentimentAuditUseCase audit; private final String token;
    public NewsAuditController(GetSentimentAuditUseCase audit,@Value("${news.audit.service-token:}")String token){this.audit=audit;this.token=token;}
    @GetMapping("/{newsId}/sentiment") public ResponseEntity<AuditResponse> latest(@PathVariable String newsId,@RequestHeader(name="Authorization",required=false)String authorization){
        if(token.isBlank()||!constantTimeEquals("Bearer "+token,authorization))return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return audit.findLatest(new NewsId(newsId)).map(r->ResponseEntity.ok(new AuditResponse(new SentimentResultResponseId(r.resultId().value()),new NewsResponse.NewsResponseId(r.newsId().value()),r.language().value(),r.contentHash().value(),r.release().modelName(),r.release().modelVersion(),r.release().preprocessingVersion(),r.release().contractVersion(),r.label().name(),r.confidence().toPlainString(),r.polarityScore().toPlainString(),r.analyzedAt()))).orElseGet(()->ResponseEntity.notFound().build());
    }
    private static boolean constantTimeEquals(String expected,String actual){if(actual==null)return false;return java.security.MessageDigest.isEqual(expected.getBytes(java.nio.charset.StandardCharsets.UTF_8),actual.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    public record AuditResponse(SentimentResultResponseId sentimentResultId,NewsResponse.NewsResponseId newsId,String language,String contentHash,String modelName,String modelVersion,String preprocessingVersion,String contractVersion,String label,String confidence,String polarityScore,Instant analyzedAt){}
    public record SentimentResultResponseId(@JsonValue String value) {}
}

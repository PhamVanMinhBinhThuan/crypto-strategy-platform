package com.cryptostrategy.platform.api.news;

import com.cryptostrategy.platform.news.api.model.NewsId;
import com.cryptostrategy.platform.news.api.port.in.GetSentimentAuditUseCase;
import com.fasterxml.jackson.annotation.JsonValue;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/news-items")
public class NewsAuditController {
    private final GetSentimentAuditUseCase audit;
    private final String token;

    public NewsAuditController(
            GetSentimentAuditUseCase audit,
            @Value("${news.audit.service-token:}") String token) {
        this.audit = Objects.requireNonNull(audit, "audit");
        this.token = token == null ? "" : token;
    }

    @GetMapping("/{newsId}/sentiment")
    public ResponseEntity<AuditResponse> latest(
            @PathVariable String newsId,
            @RequestHeader(name = "Authorization", required = false) String authorization) {
        if (token.isBlank() || !constantTimeEquals("Bearer " + token, authorization)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return audit.findLatest(new NewsId(newsId))
                .map(result -> ResponseEntity.ok(new AuditResponse(
                        new SentimentResultResponseId(result.resultId().value()),
                        new NewsResponse.NewsResponseId(result.newsId().value()),
                        result.language().value(),
                        result.contentHash().value(),
                        result.release().modelName(),
                        result.release().modelVersion(),
                        result.release().preprocessingVersion(),
                        result.release().contractVersion(),
                        result.label().name(),
                        result.confidence().toPlainString(),
                        result.polarityScore().toPlainString(),
                        result.analyzedAt())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        return actual != null && MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }

    public record AuditResponse(
            SentimentResultResponseId sentimentResultId,
            NewsResponse.NewsResponseId newsId,
            String language,
            String contentHash,
            String modelName,
            String modelVersion,
            String preprocessingVersion,
            String contractVersion,
            String label,
            String confidence,
            String polarityScore,
            Instant analyzedAt) {}

    public record SentimentResultResponseId(@JsonValue String value) {}
}

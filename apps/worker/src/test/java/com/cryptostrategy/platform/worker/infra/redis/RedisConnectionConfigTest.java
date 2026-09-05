package com.cryptostrategy.platform.worker.infra.redis;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cryptostrategy.platform.contracts.sentiment.v1.SentimentHealthResponse;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RedisConnectionConfigTest {
    @Test
    void worker_mapper_supports_optional_fields_in_sentiment_health_contract() throws Exception {
        var mapper = new RedisConnectionConfig().workerObjectMapper();

        var response = mapper.readValue(
                """
                {
                  "status": "READY",
                  "contractVersion": "sentiment-v1",
                  "modelVersion": "multichannel-english-1.0.0"
                }
                """,
                SentimentHealthResponse.class);

        assertEquals("READY", response.status());
        assertEquals(Optional.of("sentiment-v1"), response.contractVersion());
        assertEquals(Optional.of("multichannel-english-1.0.0"), response.modelVersion());
    }
}

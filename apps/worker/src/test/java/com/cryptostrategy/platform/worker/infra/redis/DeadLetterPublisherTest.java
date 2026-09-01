package com.cryptostrategy.platform.worker.infra.redis;

import com.cryptostrategy.platform.worker.config.WorkerProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DeadLetterPublisherTest {

    private RedisStreamPublisher streamPublisher;
    private WorkerProperties workerProperties;
    private ObjectMapper objectMapper;
    private DeadLetterPublisher publisher;

    @BeforeEach
    void setUp() {
        streamPublisher = mock(RedisStreamPublisher.class);
        workerProperties = new WorkerProperties(null, null, null, null, null, null, null, null);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        publisher = new DeadLetterPublisher(streamPublisher, workerProperties, objectMapper);
    }

    @Test
    void publishesDeadLetterPayloadToDeadLetterStream() {
        publisher.publishDeadLetter(
                "01J7K8M9N0P1Q2R3S4T5A6V7W1",
                "01J7K8M9N0P1Q2R3S4T5A6V7W2",
                "01J7K8M9N0P1Q2R3S4T5A6V7W3",
                "01J7K8M9N0P1Q2R3S4T5A6V7W4",
                "PERMANENT_LOGIC_ERROR",
                "BAD_DATA",
                "NullPointerException",
                3
        );
        verify(streamPublisher).publish(eq(workerProperties.streams().getDeadLetterStream()), anyString(), anyString(), any());
    }
}

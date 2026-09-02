package com.cryptostrategy.platform.api.news;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.cryptostrategy.platform.news.api.port.in.GetSentimentAuditUseCase;
import org.junit.jupiter.api.Test;

class NewsAuditSecurityTest {
    @Test
    void browserBearerTokenCannotActAsInternalServiceCredential() {
        GetSentimentAuditUseCase audit = mock(GetSentimentAuditUseCase.class);
        var controller = new NewsAuditController(audit, "dedicated-service-secret");

        var response = controller.latest(
                "01J00000000000000000000001", "Bearer browser-user-jwt");

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody()).isNull();
        verifyNoInteractions(audit);
    }
}

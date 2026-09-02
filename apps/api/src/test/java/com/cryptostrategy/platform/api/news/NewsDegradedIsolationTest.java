package com.cryptostrategy.platform.api.news;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.cryptostrategy.platform.api.transport.PageRequestMapper;
import com.cryptostrategy.platform.marketdata.api.port.in.GetTradingPairUseCase;
import com.cryptostrategy.platform.news.api.model.AnalysisStatus;
import com.cryptostrategy.platform.news.api.model.NewsId;
import com.cryptostrategy.platform.news.api.port.in.ListNewsUseCase;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class NewsDegradedIsolationTest {
    @Test
    void sentimentFailureRemainsADataStateAndDoesNotInvokeOtherCapabilities() {
        GetTradingPairUseCase pairs = mock(GetTradingPairUseCase.class);
        ListNewsUseCase news = query -> new ListNewsUseCase.Page(List.of(
                new ListNewsUseCase.Item(
                        new NewsId("01J00000000000000000000001"),
                        "Still visible",
                        "fixture",
                        "https://example.test/degraded",
                        Instant.EPOCH,
                        AnalysisStatus.FAILED,
                        List.of(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty())), Optional.empty());
        var controller = new NewsController(
                news,
                new NewsQueryMapper(pairs, new PageRequestMapper()));

        var response = controller.list(null, Set.of(), null, 20);

        assertThat(response.items().getFirst().analysisStatus()).isEqualTo("FAILED");
        assertThat(response.items().getFirst().sentiment()).isEmpty();
        verifyNoInteractions(pairs);
    }
}

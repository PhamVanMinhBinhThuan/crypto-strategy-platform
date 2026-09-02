package com.cryptostrategy.platform.api.news;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

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

class NewsPublicApiTest {
    @Test
    void pagesNewestNormalizedNewsAndKeepsUnavailableSentimentNull() {
        ListNewsUseCase news = query -> new ListNewsUseCase.Page(List.of(
                item("01J00000000000000000000002", Instant.parse("2026-09-02T01:00:00Z")),
                item("01J00000000000000000000001", Instant.parse("2026-09-02T00:00:00Z"))),
                Optional.of("next-page"));
        var controller = new NewsController(
                news,
                new NewsQueryMapper(mock(GetTradingPairUseCase.class), new PageRequestMapper()));

        var response = controller.list(null, Set.of(AnalysisStatus.FAILED_RETRYABLE), null, 20);

        assertThat(response.items()).extracting(NewsResponse.Item::newsId)
                .extracting(NewsResponse.NewsResponseId::value)
                .containsExactly(
                        "01J00000000000000000000002",
                        "01J00000000000000000000001");
        assertThat(response.items()).allMatch(item -> item.sentiment().isEmpty());
        assertThat(response.hasMore()).isTrue();
    }

    @Test
    void rejectsUnboundedPageRequestsBeforeCallingNewsCapability() {
        var mapper = new NewsQueryMapper(
                mock(GetTradingPairUseCase.class), new PageRequestMapper());

        assertThatThrownBy(() -> mapper.map(null, Set.of(), null, 101))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static ListNewsUseCase.Item item(String id, Instant publishedAt) {
        return new ListNewsUseCase.Item(
                new NewsId(id),
                "News",
                "fixture",
                "https://example.test/" + id,
                publishedAt,
                AnalysisStatus.FAILED_RETRYABLE,
                List.of(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }
}

package com.cryptostrategy.platform.news.internal.application;

import static org.junit.jupiter.api.Assertions.*;

import com.cryptostrategy.platform.domain.api.market.AssetId;
import com.cryptostrategy.platform.news.api.model.*;
import com.cryptostrategy.platform.news.api.port.in.ListNewsUseCase;
import com.cryptostrategy.platform.news.api.port.out.NewsQueryPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class NewsQueryServiceTest {
    private static final NewsId HIGH = new NewsId("01K4A000000000000000000002");
    private static final NewsId LOW = new NewsId("01K4A000000000000000000001");
    private static final Instant TIME = Instant.parse("2026-09-01T00:00:00Z");

    @Test void validates_cursor_and_limit_before_calling_the_port() {
        var calls=new java.util.concurrent.atomic.AtomicInteger();
        var service=new NewsQueryService(query->{calls.incrementAndGet();return new ListNewsUseCase.Page(List.of(),Optional.empty());});
        assertThrows(IllegalArgumentException.class,()->service.list(new ListNewsUseCase.Query(Set.of(),Set.of(),Optional.of("bad"),20)));
        assertThrows(IllegalArgumentException.class,()->service.list(new ListNewsUseCase.Query(Set.of(),Set.of(),Optional.empty(),101)));
        assertEquals(0,calls.get());
    }

    @Test void supplies_both_asset_ids_preserves_stable_order_and_suppresses_unanalyzed_sentiment() {
        var base=AssetId.generate();var quote=AssetId.generate();
        var captured=new AtomicReference<ListNewsUseCase.Query>();
        String next=new NewsCursor(TIME,LOW).encode();
        NewsQueryPort port=query->{captured.set(query);return new ListNewsUseCase.Page(List.of(
                item(HIGH,AnalysisStatus.ANALYZED,Optional.of("POSITIVE")),
                item(LOW,AnalysisStatus.PENDING,Optional.of("POSITIVE"))),Optional.of(next));};
        var query=new ListNewsUseCase.Query(Set.of(base,quote),Set.of(),Optional.empty(),20);

        var page=new NewsQueryService(port).list(query);

        assertEquals(Set.of(base,quote),captured.get().eitherAsset());
        assertEquals(List.of(HIGH,LOW),page.items().stream().map(ListNewsUseCase.Item::newsId).toList());
        assertTrue(page.items().getFirst().label().isPresent());
        assertTrue(page.items().getLast().label().isEmpty());
        assertEquals(Optional.of(next),page.nextCursor());
    }

    @Test void rejects_unstable_port_ordering() {
        var service=new NewsQueryService(query->new ListNewsUseCase.Page(List.of(
                item(LOW,AnalysisStatus.PENDING,Optional.empty()),item(HIGH,AnalysisStatus.PENDING,Optional.empty())),Optional.empty()));
        assertThrows(IllegalStateException.class,()->service.list(new ListNewsUseCase.Query(Set.of(),Set.of(),Optional.empty(),20)));
    }

    private static ListNewsUseCase.Item item(NewsId id,AnalysisStatus status,Optional<String> label){
        return new ListNewsUseCase.Item(id,"title","source","https://example.test/"+id.value(),TIME,status,List.of(),label,
                label.map(value->new BigDecimal("0.8")),label.map(value->new BigDecimal("0.6")));
    }
}

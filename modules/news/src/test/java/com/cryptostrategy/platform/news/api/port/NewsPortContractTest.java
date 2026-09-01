package com.cryptostrategy.platform.news.api.port;

import static org.junit.jupiter.api.Assertions.*;

import com.cryptostrategy.platform.news.api.port.in.CollectNewsUseCase;
import com.cryptostrategy.platform.news.api.port.in.GetSentimentAuditUseCase;
import com.cryptostrategy.platform.news.api.port.in.ListNewsUseCase;
import com.cryptostrategy.platform.news.api.port.in.NewsAnalysisUseCase;
import com.cryptostrategy.platform.news.api.port.out.AnalysisWorkStore;
import com.cryptostrategy.platform.news.api.port.out.AssetResolver;
import com.cryptostrategy.platform.news.api.port.out.NewsItemStore;
import com.cryptostrategy.platform.news.api.port.out.NewsProvider;
import com.cryptostrategy.platform.news.api.port.out.NewsQueryPort;
import com.cryptostrategy.platform.news.api.port.out.SentimentAuditStore;
import com.cryptostrategy.platform.news.api.port.out.SentimentInferencePort;
import com.cryptostrategy.platform.news.api.port.out.SentimentModelReleaseStore;
import java.lang.reflect.Modifier;
import java.util.List;
import org.junit.jupiter.api.Test;

class NewsPortContractTest {
    @Test void publishes_narrow_framework_free_input_and_output_ports() {
        var ports = List.of(CollectNewsUseCase.class, NewsAnalysisUseCase.class, ListNewsUseCase.class,
            GetSentimentAuditUseCase.class, NewsProvider.class, NewsItemStore.class, AnalysisWorkStore.class,
            SentimentModelReleaseStore.class, NewsQueryPort.class, SentimentAuditStore.class,
            AssetResolver.class, SentimentInferencePort.class);
        ports.forEach(port -> {
            assertTrue(port.isInterface(), port.getName());
            assertTrue(Modifier.isPublic(port.getModifiers()), port.getName());
            for (var method : port.getMethods()) {
                assertFalse(method.toGenericString().contains("springframework"), method.toString());
                assertFalse(method.toGenericString().contains("java.net.http"), method.toString());
                assertFalse(method.toGenericString().contains("contracts.sentiment"), method.toString());
            }
        });
    }

    @Test void analysis_boundary_exposes_acquire_start_complete_defer_and_fail_commands() {
        var methods = java.util.Arrays.stream(NewsAnalysisUseCase.class.getDeclaredMethods())
            .map(java.lang.reflect.Method::getName).collect(java.util.stream.Collectors.toSet());
        assertTrue(methods.containsAll(List.of("acquire", "startAttempt", "complete", "defer", "fail")));
    }
}

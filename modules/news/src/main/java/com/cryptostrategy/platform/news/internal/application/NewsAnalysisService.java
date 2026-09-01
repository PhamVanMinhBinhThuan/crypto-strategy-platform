package com.cryptostrategy.platform.news.internal.application;

import com.cryptostrategy.platform.news.api.NewsModuleFactory.AnalysisPolicy;
import com.cryptostrategy.platform.news.api.model.NewsItem;
import com.cryptostrategy.platform.news.api.model.SentimentAnalysisOutcome;
import com.cryptostrategy.platform.news.api.model.SentimentAnalysisRequest;
import com.cryptostrategy.platform.news.api.port.in.NewsAnalysisUseCase;
import com.cryptostrategy.platform.news.api.port.out.AnalysisWorkStore;
import com.cryptostrategy.platform.news.api.port.out.SentimentInferencePort;
import com.cryptostrategy.platform.news.internal.validation.SentimentResponseValidator;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

/** Framework-free orchestration policy for durable analysis state transitions. */
public final class NewsAnalysisService implements NewsAnalysisUseCase {
    private final AnalysisWorkStore work;
    private final SentimentInferencePort inference;
    private final Clock clock;
    private final AnalysisPolicy policy;
    private final SentimentResponseValidator validator;

    public NewsAnalysisService(
            AnalysisWorkStore work,
            SentimentInferencePort inference,
            Clock clock,
            AnalysisPolicy policy) {
        this.work = Objects.requireNonNull(work, "work");
        this.inference = Objects.requireNonNull(inference, "inference");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.policy = Objects.requireNonNull(policy, "policy");
        this.validator = new SentimentResponseValidator();
    }

    @Override
    public List<NewsItem> acquire(Acquire command) {
        Objects.requireNonNull(command, "command");
        return work.claim(command.owner(), command.now(), command.leaseDuration(), command.limit());
    }

    @Override
    public boolean startAttempt(StartAttempt command) {
        Objects.requireNonNull(command, "command");
        return work.reserveAttempt(command.newsId(), command.leaseToken(), command.contentHash(), command.modelVersion());
    }

    @Override
    public CompletionStage<SentimentAnalysisOutcome> analyze(SentimentAnalysisRequest request) {
        Objects.requireNonNull(request, "request");
        return inference.analyze(request).thenApply(outcome -> validator.validate(request, outcome));
    }

    @Override
    public void complete(Complete command) {
        Objects.requireNonNull(command, "command");
        work.complete(command.newsId(), command.leaseToken(), command.result());
    }

    @Override
    public void defer(Defer command) {
        Objects.requireNonNull(command, "command");
        work.defer(command.newsId(), command.leaseToken(), clock.instant().plus(command.delay()));
    }

    @Override
    public void fail(Fail command) {
        Objects.requireNonNull(command, "command");
        boolean retryable = command.transientFailure() && command.consumedAttempts() < policy.maxAttempts();
        Duration delay = policy.retryDelays().get(Math.min(command.consumedAttempts() - 1, policy.retryDelays().size() - 1));
        work.fail(command.newsId(), command.leaseToken(), retryable, clock.instant().plus(delay));
    }
}

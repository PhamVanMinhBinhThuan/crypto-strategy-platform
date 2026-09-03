package com.cryptostrategy.platform.worker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "worker")
public record WorkerProperties(
        Redis redis,
        Streams streams,
        Consumer consumer,
        Concurrency concurrency,
        Retry retry,
        Execution execution,
        Reconciliation reconciliation,
        ProcessedMessage processedMessage
) {
    public WorkerProperties(
            Redis redis,
            Streams streams,
            Consumer consumer,
            Concurrency concurrency,
            Retry retry,
            Execution execution,
            Reconciliation reconciliation,
            ProcessedMessage processedMessage
    ) {
        this.redis = redis != null ? redis : Redis.defaults();
        this.streams = streams != null ? streams : Streams.defaults();
        this.consumer = consumer != null ? consumer : Consumer.defaults();
        this.concurrency = concurrency != null ? concurrency : Concurrency.defaults();
        this.retry = retry != null ? retry : Retry.defaults();
        this.execution = execution != null ? execution : Execution.defaults();
        this.reconciliation = reconciliation != null ? reconciliation : Reconciliation.defaults();
        this.processedMessage = processedMessage != null ? processedMessage : ProcessedMessage.defaults();
        validate(this.execution, this.reconciliation, this.processedMessage);
    }

    public record Redis(
            String host,
            int port,
            String password,
            String username,
            boolean ssl,
            Duration timeout
    ) {
        public static Redis defaults() {
            return new Redis("localhost", 6379, null, null, false, Duration.ofSeconds(5));
        }

        public Redis(
                String host,
                int port,
                String password,
                String username,
                boolean ssl,
                Duration timeout
        ) {
            this.host = (host != null && !host.isBlank()) ? host : "localhost";
            this.port = port > 0 ? port : 6379;
            this.password = password;
            this.username = username;
            this.ssl = ssl;
            this.timeout = timeout != null ? timeout : Duration.ofSeconds(5);
            if (this.port < 1 || this.port > 65535) throw new IllegalArgumentException("redis.port must be between 1 and 65535");
            if (this.timeout.isNegative() || this.timeout.isZero()) throw new IllegalArgumentException("redis.timeout must be positive");
        }
    }

    public record Streams(
            String prefix,
            String backtestJobs,
            String candidateEvaluated,
            String deadLetter,
            String progressEvents,
            String lifecycleEvents,
            String searchRequests
    ) {
        public static Streams defaults() {
            return new Streams("", "backtest.jobs.v1", "candidate.evaluated.v1", "jobs.dead-letter.v1", "progress.events.v1", "lifecycle.events.v1", "search.requests.v1");
        }

        public Streams(
                String prefix,
                String backtestJobs,
                String candidateEvaluated,
                String deadLetter,
                String progressEvents,
                String lifecycleEvents,
                String searchRequests
        ) {
            this.prefix = prefix != null ? prefix : "";
            this.backtestJobs = (backtestJobs != null && !backtestJobs.isBlank()) ? backtestJobs : "backtest.jobs.v1";
            this.candidateEvaluated = (candidateEvaluated != null && !candidateEvaluated.isBlank()) ? candidateEvaluated : "candidate.evaluated.v1";
            this.deadLetter = (deadLetter != null && !deadLetter.isBlank()) ? deadLetter : "jobs.dead-letter.v1";
            this.progressEvents = (progressEvents != null && !progressEvents.isBlank()) ? progressEvents : "progress.events.v1";
            this.lifecycleEvents = (lifecycleEvents != null && !lifecycleEvents.isBlank()) ? lifecycleEvents : "lifecycle.events.v1";
            this.searchRequests = (searchRequests != null && !searchRequests.isBlank()) ? searchRequests : "search.requests.v1";
        }

        public String formatStream(String baseName) {
            return prefix.isBlank() ? baseName : prefix + "." + baseName;
        }

        public String getBacktestJobsStream() {
            return formatStream(backtestJobs);
        }

        public String getCandidateEvaluatedStream() {
            return formatStream(candidateEvaluated);
        }

        public String getDeadLetterStream() {
            return formatStream(deadLetter);
        }

        public String getProgressEventsStream() {
            return formatStream(progressEvents);
        }

        public String getLifecycleEventsStream() {
            return formatStream(lifecycleEvents);
        }

        public String getSearchRequestsStream() {
            return formatStream(searchRequests);
        }
    }

    public record Consumer(
            String backtestGroup,
            String rankingGroup,
            String searchGroup,
            String consumerName,
            int readBatchSize,
            Duration pollTimeout,
            Duration pendingIdleTime,
            int pendingBatchSize
    ) {
        public static Consumer defaults() {
            return new Consumer("backtest-workers", "ranking-workers", "search-coordinators", "worker-1", 10, Duration.ofSeconds(2), Duration.ofMinutes(1), 10);
        }

        public Consumer(
                String backtestGroup,
                String rankingGroup,
                String searchGroup,
                String consumerName,
                int readBatchSize,
                Duration pollTimeout,
                Duration pendingIdleTime,
                int pendingBatchSize
        ) {
            this.backtestGroup = (backtestGroup != null && !backtestGroup.isBlank()) ? backtestGroup : "backtest-workers";
            this.rankingGroup = (rankingGroup != null && !rankingGroup.isBlank()) ? rankingGroup : "ranking-workers";
            this.searchGroup = (searchGroup != null && !searchGroup.isBlank()) ? searchGroup : "search-coordinators";
            this.consumerName = (consumerName != null && !consumerName.isBlank()) ? consumerName : "worker-1";
            this.readBatchSize = readBatchSize > 0 ? readBatchSize : 10;
            this.pollTimeout = pollTimeout != null ? pollTimeout : Duration.ofSeconds(2);
            this.pendingIdleTime = pendingIdleTime != null ? pendingIdleTime : Duration.ofMinutes(1);
            this.pendingBatchSize = pendingBatchSize > 0 ? pendingBatchSize : 10;
            if (this.pollTimeout.isNegative() || this.pollTimeout.isZero()) throw new IllegalArgumentException("consumer.pollTimeout must be positive");
            if (this.pendingIdleTime.isNegative() || this.pendingIdleTime.isZero()) throw new IllegalArgumentException("consumer.pendingIdleTime must be positive");
        }
    }

    public record Concurrency(
            int backtest,
            int ranking,
            int search,
            int maxInFlight,
            int maxInFlightPerExperiment
    ) {
        public static Concurrency defaults() {
            return new Concurrency(4, 2, 2, 20, 4);
        }

        public Concurrency(int backtest, int ranking, int search, int maxInFlight, int maxInFlightPerExperiment) {
            this.backtest = backtest > 0 ? backtest : 4;
            this.ranking = ranking > 0 ? ranking : 2;
            this.search = search > 0 ? search : 2;
            this.maxInFlight = maxInFlight > 0 ? maxInFlight : 20;
            this.maxInFlightPerExperiment = maxInFlightPerExperiment > 0 ? maxInFlightPerExperiment : 4;
            if (this.maxInFlightPerExperiment > this.maxInFlight) {
                throw new IllegalArgumentException("concurrency.maxInFlightPerExperiment must not exceed maxInFlight");
            }
        }
    }

    public record Retry(
            int maxAttempts,
            Duration baseDelay,
            double multiplier,
            Duration maxDelay,
            double jitterFactor
    ) {
        public static Retry defaults() {
            return new Retry(3, Duration.ofSeconds(2), 2.0, Duration.ofMinutes(5), 0.2);
        }

        public Retry(
                int maxAttempts,
                Duration baseDelay,
                double multiplier,
                Duration maxDelay,
                double jitterFactor
        ) {
            this.maxAttempts = maxAttempts > 0 ? maxAttempts : 3;
            this.baseDelay = baseDelay != null ? baseDelay : Duration.ofSeconds(2);
            this.multiplier = multiplier >= 1.0 ? multiplier : 2.0;
            this.maxDelay = maxDelay != null ? maxDelay : Duration.ofMinutes(5);
            this.jitterFactor = (jitterFactor >= 0.0 && jitterFactor <= 1.0) ? jitterFactor : 0.2;
            if (this.baseDelay.isNegative() || this.baseDelay.isZero()) throw new IllegalArgumentException("retry.baseDelay must be positive");
            if (this.maxDelay.isNegative() || this.maxDelay.isZero()) throw new IllegalArgumentException("retry.maxDelay must be positive");
        }
    }

    public record Execution(
            Duration timeout,
            Duration gracefulShutdownTimeout
    ) {
        public static Execution defaults() {
            return new Execution(Duration.ofMinutes(5), Duration.ofSeconds(30));
        }

        public Execution(Duration timeout, Duration gracefulShutdownTimeout) {
            this.timeout = timeout != null ? timeout : Duration.ofMinutes(5);
            this.gracefulShutdownTimeout = gracefulShutdownTimeout != null ? gracefulShutdownTimeout : Duration.ofSeconds(30);
            if (this.timeout.isNegative() || this.timeout.isZero()) throw new IllegalArgumentException("execution.timeout must be positive");
            if (this.gracefulShutdownTimeout.isNegative() || this.gracefulShutdownTimeout.isZero()) throw new IllegalArgumentException("execution.gracefulShutdownTimeout must be positive");
        }
    }

    public record Reconciliation(
            Duration outboxScanInterval,
            int outboxBatchSize,
            Duration queueInterval,
            Duration queueGracePeriod,
            Duration staleInterval,
            Duration staleGracePeriod,
            Duration leaderboardInterval,
            int leaderboardBatchSize,
            Duration stopCompletionInterval,
            Duration searchInterval,
            int searchBatchSize
    ) {
        public static Reconciliation defaults() {
            return new Reconciliation(
                    Duration.ofSeconds(1), 50,
                    Duration.ofSeconds(30), Duration.ofMinutes(2),
                    Duration.ofSeconds(30), Duration.ofMinutes(1),
                    Duration.ofSeconds(10), 20,
                    Duration.ofSeconds(5),
                    Duration.ofSeconds(5), 50
            );
        }

        public Reconciliation(
                Duration outboxScanInterval,
                int outboxBatchSize,
                Duration queueInterval,
                Duration queueGracePeriod,
                Duration staleInterval,
                Duration staleGracePeriod,
                Duration leaderboardInterval,
                int leaderboardBatchSize,
                Duration stopCompletionInterval,
                Duration searchInterval,
                int searchBatchSize
        ) {
            this.outboxScanInterval = outboxScanInterval != null ? outboxScanInterval : Duration.ofSeconds(1);
            this.outboxBatchSize = outboxBatchSize > 0 ? outboxBatchSize : 50;
            this.queueInterval = queueInterval != null ? queueInterval : Duration.ofSeconds(30);
            this.queueGracePeriod = queueGracePeriod != null ? queueGracePeriod : Duration.ofMinutes(2);
            this.staleInterval = staleInterval != null ? staleInterval : Duration.ofSeconds(30);
            this.staleGracePeriod = staleGracePeriod != null ? staleGracePeriod : Duration.ofMinutes(1);
            this.leaderboardInterval = leaderboardInterval != null ? leaderboardInterval : Duration.ofSeconds(10);
            this.leaderboardBatchSize = leaderboardBatchSize > 0 ? leaderboardBatchSize : 20;
            this.stopCompletionInterval = stopCompletionInterval != null ? stopCompletionInterval : Duration.ofSeconds(5);
            this.searchInterval = searchInterval != null ? searchInterval : Duration.ofSeconds(5);
            this.searchBatchSize = searchBatchSize > 0 ? searchBatchSize : 50;

            positive(this.outboxScanInterval, "reconciliation.outboxScanInterval");
            positive(this.queueInterval, "reconciliation.queueInterval");
            positive(this.queueGracePeriod, "reconciliation.queueGracePeriod");
            positive(this.staleInterval, "reconciliation.staleInterval");
            positive(this.staleGracePeriod, "reconciliation.staleGracePeriod");
            positive(this.leaderboardInterval, "reconciliation.leaderboardInterval");
            positive(this.stopCompletionInterval, "reconciliation.stopCompletionInterval");
            positive(this.searchInterval, "reconciliation.searchInterval");
        }
    }

    public record ProcessedMessage(
            Duration ttl
    ) {
        public static ProcessedMessage defaults() {
            return new ProcessedMessage(Duration.ofDays(7));
        }

        public ProcessedMessage(Duration ttl) {
            this.ttl = ttl != null ? ttl : Duration.ofDays(7);
            positive(this.ttl, "processedMessage.ttl");
        }
    }

    private static void validate(Execution exec, Reconciliation recon, ProcessedMessage proc) {
        Duration recoveryHorizon = exec.timeout().plus(recon.staleGracePeriod());
        if (proc.ttl().compareTo(recoveryHorizon) <= 0) {
            throw new IllegalArgumentException("processedMessage.ttl must be strictly greater than execution recovery horizon (" + recoveryHorizon + ")");
        }
    }

    private static void positive(Duration duration, String name) {
        if (duration == null || duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }
}

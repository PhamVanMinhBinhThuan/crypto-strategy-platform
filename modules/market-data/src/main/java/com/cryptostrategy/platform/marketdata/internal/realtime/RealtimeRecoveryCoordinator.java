package com.cryptostrategy.platform.marketdata.internal.realtime;

import com.cryptostrategy.platform.domain.api.market.CandleKey;
import com.cryptostrategy.platform.domain.api.market.MarketProvider;
import com.cryptostrategy.platform.marketdata.api.error.MarketDataErrorCode;
import com.cryptostrategy.platform.marketdata.api.error.MarketDataException;
import com.cryptostrategy.platform.marketdata.api.event.CandleUpdate;
import com.cryptostrategy.platform.marketdata.api.event.CandleUpdateHandler;
import com.cryptostrategy.platform.marketdata.api.event.ConnectionState;
import com.cryptostrategy.platform.marketdata.api.event.ConnectionStateHandler;
import com.cryptostrategy.platform.marketdata.api.model.CandleSubscription;
import com.cryptostrategy.platform.marketdata.api.model.HistoricalCandleBatch;
import com.cryptostrategy.platform.marketdata.api.model.HistoricalCandleQuery;
import com.cryptostrategy.platform.marketdata.api.model.RealtimeCandleQuery;
import com.cryptostrategy.platform.marketdata.api.port.out.MarketDataProvider;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.DoubleSupplier;

/** Adds bounded reconnect and historical overlap recovery to a provider subscription. */
public final class RealtimeRecoveryCoordinator implements MarketDataProvider {
    private final MarketDataProvider delegate;
    private final Clock clock;
    private final RecoveryPolicy policy;

    public RealtimeRecoveryCoordinator(MarketDataProvider delegate, Clock clock, RecoveryPolicy policy) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    @Override
    public MarketProvider providerId() {
        return delegate.providerId();
    }

    @Override
    public String normalizationVersion() {
        return delegate.normalizationVersion();
    }

    @Override
    public HistoricalCandleBatch loadHistorical(HistoricalCandleQuery query) {
        return delegate.loadHistorical(query);
    }

    @Override
    public CandleSubscription subscribe(
            RealtimeCandleQuery query,
            CandleUpdateHandler updates,
            ConnectionStateHandler states) {
        RecoverySession session = new RecoverySession(query, updates, states);
        session.start();
        return session;
    }

    public record RecoveryPolicy(
            int maxAttempts,
            Duration initialDelay,
            Duration maxDelay,
            int historicalPageSize,
            int historicalMaxPages,
            Sleeper sleeper,
            RecoveryExecutor executor,
            DoubleSupplier jitter) {
        public RecoveryPolicy {
            if (maxAttempts < 1 || historicalPageSize < 1 || historicalPageSize > 1000
                    || historicalMaxPages < 1) {
                throw new IllegalArgumentException("Invalid realtime recovery bounds");
            }
            requirePositive(initialDelay, "initialDelay");
            requirePositive(maxDelay, "maxDelay");
            Objects.requireNonNull(sleeper, "sleeper");
            Objects.requireNonNull(executor, "executor");
            Objects.requireNonNull(jitter, "jitter");
        }

        public static RecoveryPolicy defaults() {
            return runtime(5, Duration.ofMillis(500), Duration.ofSeconds(5), 1000, 1000);
        }

        public static RecoveryPolicy runtime(
                int maxAttempts,
                Duration initialDelay,
                Duration maxDelay,
                int historicalPageSize,
                int historicalMaxPages) {
            return new RecoveryPolicy(
                    maxAttempts,
                    initialDelay,
                    maxDelay,
                    historicalPageSize,
                    historicalMaxPages,
                    Thread::sleep,
                    task -> Thread.ofVirtual().name("market-data-recovery").start(task),
                    () -> ThreadLocalRandom.current().nextDouble());
        }

        private static void requirePositive(Duration value, String name) {
            if (value == null || value.isZero() || value.isNegative()) {
                throw new IllegalArgumentException(name + " must be positive");
            }
        }
    }

    @FunctionalInterface
    public interface Sleeper {
        void sleep(Duration duration) throws InterruptedException;
    }

    @FunctionalInterface
    public interface RecoveryExecutor {
        void execute(Runnable task);
    }

    private final class RecoverySession implements CandleSubscription {
        private final RealtimeCandleQuery query;
        private final CandleUpdateHandler updates;
        private final ConnectionStateHandler states;
        private final Object monitor = new Object();
        private final Map<CandleKey, CandleUpdate> latest = new HashMap<>();
        private final Map<CandleKey, CandleUpdate> buffered = new HashMap<>();
        private CandleSubscription upstream;
        private CandleUpdate lastClosed;
        private boolean recovering;
        private boolean closed;

        private RecoverySession(
                RealtimeCandleQuery query,
                CandleUpdateHandler updates,
                ConnectionStateHandler states) {
            this.query = Objects.requireNonNull(query, "query");
            this.updates = Objects.requireNonNull(updates, "updates");
            this.states = Objects.requireNonNull(states, "states");
        }

        private void start() {
            upstream = delegate.subscribe(query, this::acceptLive, this::handleInitialState);
        }

        private void handleInitialState(ConnectionState state) {
            if (state == ConnectionState.RECONNECTING) {
                beginRecovery();
                return;
            }
            synchronized (monitor) {
                if (closed || recovering) {
                    return;
                }
            }
            states.onState(state);
        }

        private void acceptLive(CandleUpdate update) {
            CandleUpdate accepted;
            synchronized (monitor) {
                if (closed) {
                    return;
                }
                if (recovering) {
                    mergeLatest(buffered, update);
                    return;
                }
                accepted = acceptLatest(update);
            }
            if (accepted != null) {
                updates.onUpdate(accepted);
            }
        }

        private void beginRecovery() {
            synchronized (monitor) {
                if (closed || recovering) {
                    return;
                }
                recovering = true;
            }
            states.onState(ConnectionState.RECONNECTING);
            policy.executor().execute(this::recover);
        }

        private void recover() {
            Duration delay = policy.initialDelay();
            for (int attempt = 1; attempt <= policy.maxAttempts(); attempt++) {
                if (isClosed()) {
                    return;
                }
                if (!pause(jittered(delay))) {
                    disconnectAfterExhaustion();
                    return;
                }
                AtomicBoolean candidateDisconnected = new AtomicBoolean();
                AtomicBoolean candidateActive = new AtomicBoolean();
                CandleSubscription candidate = null;
                try {
                    candidate = delegate.subscribe(
                            query,
                            this::bufferDuringRecovery,
                            state -> {
                                if (state == ConnectionState.RECONNECTING) {
                                    candidateDisconnected.set(true);
                                    if (candidateActive.get()) {
                                        beginRecoveryAfterCandidateLoss();
                                    }
                                }
                            });
                    if (candidateDisconnected.get()) {
                        candidate.close();
                        delay = nextDelay(delay);
                        continue;
                    }

                    List<CandleUpdate> recovered = historicalOverlap();
                    List<CandleUpdate> accepted = activateCandidate(candidate, recovered);
                    candidateActive.set(true);
                    if (candidateDisconnected.get()) {
                        beginRecoveryAfterCandidateLoss();
                        return;
                    }
                    accepted.forEach(updates::onUpdate);
                    states.onState(ConnectionState.CONNECTED);
                    return;
                } catch (RuntimeException failure) {
                    if (candidate != null) {
                        candidate.close();
                    }
                    if (attempt == policy.maxAttempts()) {
                        disconnectAfterExhaustion();
                        return;
                    }
                    delay = nextDelay(delay);
                }
            }
            disconnectAfterExhaustion();
        }

        private void beginRecoveryAfterCandidateLoss() {
            synchronized (monitor) {
                if (closed) {
                    return;
                }
                recovering = false;
            }
            beginRecovery();
        }

        private void bufferDuringRecovery(CandleUpdate update) {
            synchronized (monitor) {
                if (!closed) {
                    mergeLatest(buffered, update);
                }
            }
        }

        private List<CandleUpdate> historicalOverlap() {
            CandleUpdate confirmed;
            synchronized (monitor) {
                confirmed = lastClosed;
            }
            if (confirmed == null) {
                return List.of();
            }

            Instant cutoff = clock.instant();
            long intervalSeconds = query.timeframe().duration().toSeconds();
            Instant endExclusive = Instant.ofEpochSecond(
                    Math.floorDiv(cutoff.getEpochSecond(), intervalSeconds) * intervalSeconds);
            Instant startInclusive = confirmed.candle().key().openTime();
            if (!startInclusive.isBefore(endExclusive)) {
                return List.of();
            }

            HistoricalCandleQuery historicalQuery = new HistoricalCandleQuery(
                    query.provider(),
                    query.tradingPair(),
                    query.timeframe(),
                    startInclusive,
                    endExclusive,
                    cutoff,
                    policy.historicalPageSize(),
                    policy.historicalMaxPages());
            return delegate.loadHistorical(historicalQuery).candles().stream()
                    .map(candle -> new CandleUpdate(candle, candle.closeTime()))
                    .toList();
        }

        private List<CandleUpdate> activateCandidate(
                CandleSubscription candidate,
                List<CandleUpdate> recovered) {
            List<CandleUpdate> ordered = new ArrayList<>(recovered);
            CandleSubscription previous;
            synchronized (monitor) {
                previous = upstream;
            }
            if (previous != null && previous != candidate) {
                previous.close();
            }
            synchronized (monitor) {
                if (closed) {
                    candidate.close();
                    return List.of();
                }
                ordered.addAll(buffered.values());
                buffered.clear();
                ordered.sort(Comparator
                        .comparing((CandleUpdate update) -> update.candle().key().openTime())
                        .thenComparing(CandleUpdate::providerEventTime));
                List<CandleUpdate> accepted = new ArrayList<>();
                for (CandleUpdate update : ordered) {
                    CandleUpdate value = acceptLatest(update);
                    if (value != null) {
                        accepted.add(value);
                    }
                }
                upstream = candidate;
                recovering = false;
                ordered = accepted;
            }
            return ordered;
        }

        private CandleUpdate acceptLatest(CandleUpdate incoming) {
            CandleUpdate current = latest.get(incoming.candle().key());
            if (current != null && current.providerEventTime().equals(incoming.providerEventTime())
                    && !current.candle().canonicalContentEquals(incoming.candle())) {
                throw new MarketDataException(
                        MarketDataErrorCode.MARKET_DATA_INTEGRITY_CONFLICT,
                        "Equal-order realtime Candle conflict");
            }
            if (current != null && !shouldReplace(current, incoming)) {
                return null;
            }
            latest.put(incoming.candle().key(), incoming);
            if (incoming.candle().closed()
                    && (lastClosed == null || incoming.candle().key().openTime()
                            .isAfter(lastClosed.candle().key().openTime()))) {
                lastClosed = incoming;
            }
            return incoming;
        }

        private void mergeLatest(Map<CandleKey, CandleUpdate> target, CandleUpdate incoming) {
            CandleUpdate current = target.get(incoming.candle().key());
            if (current == null || shouldReplace(current, incoming)) {
                target.put(incoming.candle().key(), incoming);
            } else if (current.providerEventTime().equals(incoming.providerEventTime())
                    && !current.candle().canonicalContentEquals(incoming.candle())) {
                throw new MarketDataException(
                        MarketDataErrorCode.MARKET_DATA_INTEGRITY_CONFLICT,
                        "Equal-order realtime Candle conflict");
            }
        }

        private boolean isClosed() {
            synchronized (monitor) {
                return closed;
            }
        }

        private boolean pause(Duration duration) {
            try {
                policy.sleeper().sleep(duration);
                return true;
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        private Duration jittered(Duration delay) {
            double random = Math.max(0.0d, Math.min(1.0d, policy.jitter().getAsDouble()));
            return Duration.ofNanos((long) (delay.toNanos() * (0.5d + random)));
        }

        private Duration nextDelay(Duration current) {
            Duration doubled;
            try {
                doubled = current.multipliedBy(2);
            } catch (ArithmeticException overflow) {
                doubled = policy.maxDelay();
            }
            return doubled.compareTo(policy.maxDelay()) > 0 ? policy.maxDelay() : doubled;
        }

        private void disconnectAfterExhaustion() {
            synchronized (monitor) {
                if (closed) {
                    return;
                }
                recovering = false;
            }
            states.onState(ConnectionState.DISCONNECTED);
        }

        @Override
        public void close() {
            CandleSubscription current;
            synchronized (monitor) {
                if (closed) {
                    return;
                }
                closed = true;
                recovering = false;
                buffered.clear();
                current = upstream;
            }
            if (current != null) {
                current.close();
            }
            states.onState(ConnectionState.DISCONNECTED);
        }
    }

    private static boolean shouldReplace(CandleUpdate current, CandleUpdate incoming) {
        if (current.candle().closed()) {
            return false;
        }
        if (incoming.candle().closed()) {
            return true;
        }
        return incoming.providerEventTime().isAfter(current.providerEventTime());
    }
}

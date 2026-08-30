package com.cryptostrategy.platform.experiment.internal;

import com.cryptostrategy.platform.experiment.api.error.IdempotencyConflictException;
import com.cryptostrategy.platform.experiment.api.idempotency.IdempotencyClaim;
import com.cryptostrategy.platform.experiment.api.idempotency.IdempotencyOutcome;
import com.cryptostrategy.platform.experiment.api.port.out.IdempotencyStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdempotentCommandExecutorTest {

    private static class FakeIdempotencyStore implements IdempotencyStore {
        private String key;
        private String hash;
        private String outcome;
        private boolean completed = false;

        @Override
        public IdempotencyClaim claim(UUID ownerUserId, String scope, String idempotencyKey, String requestHash, Instant expiresAt) {
            if (key == null) {
                key = idempotencyKey;
                hash = requestHash;
                return IdempotencyClaim.acquired();
            }
            if (!hash.equals(requestHash)) {
                return IdempotencyClaim.conflict();
            }
            if (completed) {
                return IdempotencyClaim.completedReplay(new IdempotencyOutcome("200", outcome, Instant.now()));
            }
            return IdempotencyClaim.inProgressReplay();
        }

        @Override
        public void complete(UUID ownerUserId, String scope, String idempotencyKey, String outcomeCode, String responseBody) {
            this.completed = true;
            this.outcome = responseBody;
        }

        @Override
        public Optional<IdempotencyOutcome> getOutcome(UUID ownerUserId, String scope, String idempotencyKey) {
            return completed ? Optional.of(new IdempotencyOutcome("200", outcome, Instant.now())) : Optional.empty();
        }
    }

    private final FakeIdempotencyStore store = new FakeIdempotencyStore();
    private final IdempotentCommandExecutor executor = new IdempotentCommandExecutor(store);
    private final UUID ownerUserId = UUID.randomUUID();

    @Test
    @DisplayName("First execution acquires lock and executes command")
    void firstExecution() {
        AtomicInteger count = new AtomicInteger(0);
        String res = executor.execute(
                ownerUserId, "test-op", "key-1", "hash-A", Duration.ofMinutes(5),
                () -> {
                    count.incrementAndGet();
                    return "Success";
                },
                r -> r,
                r -> r
        );

        assertThat(res).isEqualTo("Success");
        assertThat(count.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Same key with same hash returns cached result without re-executing")
    void sameKeySameHashReplay() {
        AtomicInteger count = new AtomicInteger(0);

        executor.execute(ownerUserId, "test-op", "key-1", "hash-A", Duration.ofMinutes(5),
                () -> { count.incrementAndGet(); return "Success"; }, r -> r, r -> r);

        String second = executor.execute(ownerUserId, "test-op", "key-1", "hash-A", Duration.ofMinutes(5),
                () -> { count.incrementAndGet(); return "Re-run"; }, r -> r, r -> r);

        assertThat(second).isEqualTo("Success");
        assertThat(count.get()).isEqualTo(1); // Did not re-run
    }

    @Test
    @DisplayName("Same key with different hash throws IdempotencyConflictException")
    void sameKeyDifferentHashConflict() {
        executor.execute(ownerUserId, "test-op", "key-1", "hash-A", Duration.ofMinutes(5),
                () -> "Success", r -> r, r -> r);

        assertThatThrownBy(() -> executor.execute(ownerUserId, "test-op", "key-1", "hash-B", Duration.ofMinutes(5),
                () -> "Different", r -> r, r -> r))
                .isInstanceOf(IdempotencyConflictException.class);
    }
}

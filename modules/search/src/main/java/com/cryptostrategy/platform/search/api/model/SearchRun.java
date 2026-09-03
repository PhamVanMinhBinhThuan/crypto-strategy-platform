package com.cryptostrategy.platform.search.api.model;

import java.util.UUID;


import com.cryptostrategy.platform.domain.api.identity.Ulids;
import java.time.Instant;
import java.util.Objects;

/** Immutable durable state owned by Search for one SEARCH job. */
public record SearchRun(
        SearchRunId searchRunId,
        String experimentRef,
        String searchJobRef,
        SearchRunMode mode,
        String sourceExperimentRef,
        GeneratorId generatorId,
        GeneratorVersion generatorVersion,
        long seed,
        String searchSpaceFingerprint,
        GeneratorState generatorState,
        long nextGenerationIndex,
        SearchStopConditions stopConditions,
        int maxInFlight,
        SearchRunStatus status,
        long version,
        Instant startedAt,
        Instant deadlineAt,
        Instant finishedAt,
        String failureCode,
        String failureMessage,
        Instant createdAt,
        Instant updatedAt
) {
    public SearchRun {
        Objects.requireNonNull(searchRunId, "searchRunId");
        experimentRef = Objects.requireNonNull(experimentRef);
        searchJobRef = Objects.requireNonNull(searchJobRef);
        Objects.requireNonNull(mode, "mode");
        if (mode == SearchRunMode.REPRODUCTION) {
            sourceExperimentRef = Objects.requireNonNull(sourceExperimentRef);
        } else if (sourceExperimentRef != null) {
            throw new IllegalArgumentException("sourceExperimentId is only valid in REPRODUCTION mode");
        }
        Objects.requireNonNull(generatorId, "generatorId");
        Objects.requireNonNull(generatorVersion, "generatorVersion");
        searchSpaceFingerprint = requireText(searchSpaceFingerprint, "searchSpaceFingerprint");
        Objects.requireNonNull(generatorState, "generatorState");
        Objects.requireNonNull(stopConditions, "stopConditions");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
        if (nextGenerationIndex < 0 || version < 0) {
            throw new IllegalArgumentException("nextGenerationIndex and version must be non-negative");
        }
        if (nextGenerationIndex > stopConditions.maximumCandidates()) {
            throw new IllegalArgumentException("nextGenerationIndex cannot exceed maximumCandidates");
        }
        if (maxInFlight <= 0) {
            throw new IllegalArgumentException("maxInFlight must be positive");
        }
        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("updatedAt cannot precede createdAt");
        }
        validateLifecycle(status, startedAt, deadlineAt, finishedAt, failureCode, failureMessage);
    }

    public static SearchRun pending(
            SearchRunId searchRunId,
            String experimentRef,
            String searchJobRef,
            SearchRunMode mode,
            String sourceExperimentRef,
            GeneratorDescriptor generator,
            long seed,
            String searchSpaceFingerprint,
            GeneratorState initialState,
            SearchStopConditions stopConditions,
            int maxInFlight,
            Instant createdAt
    ) {
        Objects.requireNonNull(generator, "generator");
        return new SearchRun(searchRunId, experimentRef, searchJobRef, mode, sourceExperimentRef,
                generator.generatorId(), generator.generatorVersion(), seed, searchSpaceFingerprint,
                initialState, 0, stopConditions, maxInFlight, SearchRunStatus.PENDING, 0,
                null, null, null, null, null, createdAt, createdAt);
    }

    /** First start freezes the deadline. Replaying a start on RUNNING returns this snapshot. */
    public SearchRun start(Instant now) {
        requireMutationTime(now);
        if (status == SearchRunStatus.RUNNING) {
            return this;
        }
        requireStatus(SearchRunStatus.PENDING, "start");
        return copy(generatorState, nextGenerationIndex, SearchRunStatus.RUNNING, version + 1,
                now, now.plus(stopConditions.maximumDuration()), null, null, null, now);
    }

    /** Persists generator progress; allocation may advance the generation index by exactly one. */
    public SearchRun advance(GeneratorState nextState, long resultingGenerationIndex, Instant now) {
        requireMutationTime(now);
        requireStatus(SearchRunStatus.RUNNING, "advance");
        Objects.requireNonNull(nextState, "nextState");
        if (resultingGenerationIndex < nextGenerationIndex
                || resultingGenerationIndex > nextGenerationIndex + 1) {
            throw new IllegalArgumentException("generation index must stay unchanged or advance by one");
        }
        if (nextState.fingerprint().equals(generatorState.fingerprint())) {
            throw new IllegalArgumentException("generator state must progress");
        }
        return copy(nextState, resultingGenerationIndex, status, version + 1, startedAt,
                deadlineAt, null, null, null, now);
    }

    public SearchRun requestStop(Instant now) {
        if (status == SearchRunStatus.STOPPING || status == SearchRunStatus.STOPPED) {
            return this;
        }
        requireMutationTime(now);
        if (status != SearchRunStatus.PENDING && status != SearchRunStatus.RUNNING) {
            throw invalidTransition("request stop");
        }
        return copy(generatorState, nextGenerationIndex, SearchRunStatus.STOPPING, version + 1,
                startedAt, deadlineAt, null, null, null, now);
    }

    public SearchRun complete(Instant completedAt) {
        if (status == SearchRunStatus.COMPLETED) {
            return this;
        }
        requireMutationTime(completedAt);
        requireStatus(SearchRunStatus.RUNNING, "complete");
        return copy(generatorState, nextGenerationIndex, SearchRunStatus.COMPLETED, version + 1,
                startedAt, deadlineAt, completedAt, null, null, completedAt);
    }

    public SearchRun stop(Instant stoppedAt) {
        if (status == SearchRunStatus.STOPPED) {
            return this;
        }
        requireMutationTime(stoppedAt);
        requireStatus(SearchRunStatus.STOPPING, "stop");
        return copy(generatorState, nextGenerationIndex, SearchRunStatus.STOPPED, version + 1,
                startedAt, deadlineAt, stoppedAt, null, null, stoppedAt);
    }

    public SearchRun fail(String code, String safeMessage, Instant failedAt) {
        if (status == SearchRunStatus.FAILED) {
            return this;
        }
        requireMutationTime(failedAt);
        if (status != SearchRunStatus.PENDING && status != SearchRunStatus.RUNNING) {
            throw invalidTransition("fail");
        }
        code = requireCode(code);
        safeMessage = requireText(safeMessage, "failureMessage");
        return copy(generatorState, nextGenerationIndex, SearchRunStatus.FAILED, version + 1,
                startedAt, deadlineAt, failedAt, code, safeMessage, failedAt);
    }

    private SearchRun copy(GeneratorState nextState, long nextIndex, SearchRunStatus nextStatus,
                           long nextVersion, Instant nextStartedAt, Instant nextDeadlineAt,
                           Instant nextFinishedAt, String nextFailureCode, String nextFailureMessage,
                           Instant nextUpdatedAt) {
        return new SearchRun(searchRunId, experimentRef, searchJobRef, mode, sourceExperimentRef,
                generatorId, generatorVersion, seed, searchSpaceFingerprint, nextState, nextIndex,
                stopConditions, maxInFlight, nextStatus, nextVersion, nextStartedAt, nextDeadlineAt,
                nextFinishedAt, nextFailureCode, nextFailureMessage, createdAt, nextUpdatedAt);
    }

    private void requireMutationTime(Instant now) {
        Objects.requireNonNull(now, "now");
        if (status.isTerminal()) {
            throw invalidTransition("mutate terminal run");
        }
        if (now.isBefore(updatedAt)) {
            throw new IllegalArgumentException("transition time cannot precede updatedAt");
        }
    }

    private void requireStatus(SearchRunStatus required, String action) {
        if (status != required) {
            throw invalidTransition(action);
        }
    }

    private IllegalStateException invalidTransition(String action) {
        return new IllegalStateException("Cannot " + action + " Search Run in status " + status);
    }

    private static void validateLifecycle(SearchRunStatus status, Instant startedAt, Instant deadlineAt,
                                          Instant finishedAt, String failureCode, String failureMessage) {
        if ((startedAt == null) != (deadlineAt == null)) {
            throw new IllegalArgumentException("startedAt and deadlineAt must be set together");
        }
        if (startedAt != null && deadlineAt.isBefore(startedAt)) {
            throw new IllegalArgumentException("deadlineAt cannot precede startedAt");
        }
        if (status == SearchRunStatus.PENDING && startedAt != null) {
            throw new IllegalArgumentException("PENDING run cannot have start timestamps");
        }
        if (status.isTerminal() != (finishedAt != null)) {
            throw new IllegalArgumentException("finishedAt must be set exactly for terminal status");
        }
        if (finishedAt != null && startedAt != null && finishedAt.isBefore(startedAt)) {
            throw new IllegalArgumentException("finishedAt cannot precede startedAt");
        }
        boolean failed = status == SearchRunStatus.FAILED;
        if (failed != (failureCode != null && failureMessage != null)) {
            throw new IllegalArgumentException("failure details must be set exactly for FAILED status");
        }
        if (failed) {
            requireCode(failureCode);
            requireText(failureMessage, "failureMessage");
        }
    }

    private static String requireCode(String value) {
        value = requireText(value, "failureCode");
        if (!value.matches("^[A-Z][A-Z0-9_]*$")) {
            throw new IllegalArgumentException("failureCode must be a stable uppercase code");
        }
        return value;
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}

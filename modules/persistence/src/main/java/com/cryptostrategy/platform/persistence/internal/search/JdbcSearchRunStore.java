package com.cryptostrategy.platform.persistence.internal.search;

import com.cryptostrategy.platform.search.api.model.CoordinationDecision;
import com.cryptostrategy.platform.search.api.model.SearchRun;
import com.cryptostrategy.platform.search.api.model.SearchRunId;
import com.cryptostrategy.platform.search.api.port.out.SearchRunClaim;
import com.cryptostrategy.platform.search.api.port.out.SearchRunStore;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

public final class JdbcSearchRunStore implements SearchRunStore {
    private final JdbcTemplate jdbc;
    private final SearchRows rows;

    public JdbcSearchRunStore(JdbcTemplate jdbc) {
        this(jdbc, new SearchRows());
    }

    JdbcSearchRunStore(JdbcTemplate jdbc, SearchRows rows) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
        this.rows = Objects.requireNonNull(rows, "rows");
    }

    @Override
    public SearchRun create(SearchRun run) {
        Objects.requireNonNull(run, "run");
        int inserted = jdbc.update(SearchSql.INSERT_RUN, insertArguments(run));
        if (inserted != 1) {
            throw new IllegalStateException("Search Run insert did not affect exactly one row");
        }
        return run;
    }

    @Override
    public Optional<SearchRun> findById(SearchRunId searchRunId) {
        Objects.requireNonNull(searchRunId, "searchRunId");
        return queryOne(SearchSql.SELECT_BY_ID, searchRunId.value());
    }

    @Override
    public Optional<SearchRun> findByExperimentId(String experimentId) {
        return queryOne(SearchSql.SELECT_BY_EXPERIMENT, Objects.requireNonNull(experimentId, "experimentId"));
    }

    @Override
    public Optional<SearchRun> findBySearchJobId(String searchJobId) {
        return queryOne(SearchSql.SELECT_BY_SEARCH_JOB, Objects.requireNonNull(searchJobId, "searchJobId"));
    }

    @Override
    public Optional<SearchRunClaim> claim(SearchRunId searchRunId) {
        return findById(searchRunId).filter(run -> !run.status().isTerminal())
                .map(run -> new SearchRunClaim(run, run.version()));
    }

    @Override
    public boolean save(SearchRunClaim claim, SearchRun replacement) {
        Objects.requireNonNull(claim, "claim");
        Objects.requireNonNull(replacement, "replacement");
        if (!claim.snapshot().searchRunId().equals(replacement.searchRunId())) {
            throw new IllegalArgumentException("replacement belongs to a different Search Run");
        }
        if (replacement.version() != claim.expectedVersion() + 1) {
            throw new IllegalArgumentException("replacement must advance the fencing version by one");
        }
        return jdbc.update(
                SearchSql.UPDATE_FENCED,
                replacement.generatorState().contractVersion(),
                replacement.generatorState().canonicalState(),
                replacement.generatorState().fingerprint(),
                replacement.nextGenerationIndex(),
                replacement.status().name(),
                replacement.version(),
                timestamp(replacement.startedAt()),
                timestamp(replacement.deadlineAt()),
                timestamp(replacement.finishedAt()),
                replacement.failureCode(),
                replacement.failureMessage(),
                Timestamp.from(replacement.updatedAt()),
                replacement.searchRunId().value(),
                claim.expectedVersion()) == 1;
    }

    @Override
    public boolean appendDecision(SearchRunClaim claim, CoordinationDecision decision) {
        Objects.requireNonNull(claim, "claim");
        Objects.requireNonNull(decision, "decision");
        if (!claim.snapshot().searchRunId().equals(decision.searchRunId())) {
            throw new IllegalArgumentException("decision belongs to a different Search Run");
        }
        return jdbc.update(
                SearchSql.INSERT_DECISION_FENCED,
                decision.decisionId().value(),
                decision.searchRunId().value(),
                decision.sequence(),
                decision.type().name(),
                decision.candidateRef(),
                decision.backtestJobRef(),
                decision.candidateFingerprint(),
                decision.stateBeforeFingerprint(),
                decision.stateAfterFingerprint(),
                decision.reasonCode(),
                Timestamp.from(decision.decidedAt()),
                claim.snapshot().searchRunId().value(),
                claim.expectedVersion()) == 1;
    }

    @Override
    public List<SearchRun> findRecoverable(Instant updatedBefore, int limit) {
        Objects.requireNonNull(updatedBefore, "updatedBefore");
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
        return List.copyOf(jdbc.query(
                SearchSql.SELECT_RECOVERABLE,
                rows::mapSearchRun,
                Timestamp.from(updatedBefore),
                limit));
    }

    private Optional<SearchRun> queryOne(String sql, Object argument) {
        try {
            return Optional.ofNullable(jdbc.queryForObject(sql, rows::mapSearchRun, argument));
        } catch (EmptyResultDataAccessException ignored) {
            return Optional.empty();
        }
    }

    private static Object[] insertArguments(SearchRun run) {
        return new Object[] {
                run.searchRunId().value(), run.experimentRef(), run.searchJobRef(), run.mode().name(),
                run.sourceExperimentRef(), run.generatorId().value(), run.generatorVersion().toString(),
                run.seed(), run.searchSpaceFingerprint(), run.generatorState().contractVersion(),
                run.generatorState().canonicalState(), run.generatorState().fingerprint(),
                run.nextGenerationIndex(), run.stopConditions().maximumCandidates(),
                run.stopConditions().maximumDuration().toMillis(), run.maxInFlight(), run.status().name(),
                run.version(), timestamp(run.startedAt()), timestamp(run.deadlineAt()),
                timestamp(run.finishedAt()), run.failureCode(), run.failureMessage(),
                Timestamp.from(run.createdAt()), Timestamp.from(run.updatedAt())
        };
    }

    private static Timestamp timestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }
}

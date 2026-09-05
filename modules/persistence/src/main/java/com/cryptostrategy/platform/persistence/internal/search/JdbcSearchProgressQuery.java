package com.cryptostrategy.platform.persistence.internal.search;

import com.cryptostrategy.platform.execution.api.port.in.GetSearchProgressUseCase;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.search.api.model.SearchExperimentId;
import java.util.Objects;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;

/** Persistence-owned adapter that projects Search state without leaking Search types to hosts. */
public final class JdbcSearchProgressQuery implements GetSearchProgressUseCase {
    private final JdbcSearchRunStore runs;

    public JdbcSearchProgressQuery(JdbcTemplate jdbc) {
        this.runs = new JdbcSearchRunStore(Objects.requireNonNull(jdbc, "jdbc"));
    }

    @Override
    public Optional<SearchProgressSnapshot> findByExperimentId(ExperimentId experimentId) {
        Objects.requireNonNull(experimentId, "experimentId");
        return runs.findByExperimentId(new SearchExperimentId(experimentId.value()))
                .map(run -> new SearchProgressSnapshot(
                        run.terminalReason() == null ? null : run.terminalReason().name()));
    }
}

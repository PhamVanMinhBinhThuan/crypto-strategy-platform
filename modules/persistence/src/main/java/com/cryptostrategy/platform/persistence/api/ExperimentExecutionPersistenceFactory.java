package com.cryptostrategy.platform.persistence.api;

import com.cryptostrategy.platform.execution.api.port.out.ExecutionEvidenceReader;
import com.cryptostrategy.platform.execution.api.port.out.ReproductionVerificationStore;
import com.cryptostrategy.platform.persistence.internal.execution.JdbcExecutionEvidenceReader;
import com.cryptostrategy.platform.persistence.internal.execution.JdbcReproductionVerificationStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.util.Objects;

public final class ExperimentExecutionPersistenceFactory {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final ObjectMapper json;

    public ExperimentExecutionPersistenceFactory(DataSource source, ObjectMapper json) {
        Objects.requireNonNull(source);
        this.jdbc = new JdbcTemplate(source);
        this.tx = new TransactionTemplate(new DataSourceTransactionManager(source));
        this.json = Objects.requireNonNull(json);
    }

    public ExecutionEvidenceReader createEvidenceReader() {
        return new JdbcExecutionEvidenceReader(jdbc);
    }

    public ReproductionVerificationStore createVerificationStore() {
        return new JdbcReproductionVerificationStore(jdbc, json);
    }
}

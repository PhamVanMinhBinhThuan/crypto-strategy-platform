package com.cryptostrategy.platform.persistence.api;

import com.cryptostrategy.platform.experiment.api.port.out.ExecutionAttemptStore;
import com.cryptostrategy.platform.experiment.api.port.out.ExperimentStore;
import com.cryptostrategy.platform.experiment.api.port.out.IdempotencyStore;
import com.cryptostrategy.platform.experiment.api.port.out.JobStore;
import com.cryptostrategy.platform.experiment.api.port.out.OutboxStore;
import com.cryptostrategy.platform.persistence.internal.experiment.ExperimentJsonMapper;
import com.cryptostrategy.platform.persistence.internal.experiment.ExperimentRows;
import com.cryptostrategy.platform.persistence.internal.experiment.JdbcExecutionAttemptStore;
import com.cryptostrategy.platform.persistence.internal.experiment.JdbcExperimentStore;
import com.cryptostrategy.platform.persistence.internal.experiment.JdbcIdempotencyStore;
import com.cryptostrategy.platform.persistence.internal.experiment.JdbcJobStore;
import com.cryptostrategy.platform.persistence.internal.experiment.JdbcOutboxStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.Objects;

public class ExperimentPersistenceFactory {

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final ExperimentJsonMapper jsonMapper;
    private final ExperimentRows rows;

    public ExperimentPersistenceFactory(DataSource dataSource) {
        Objects.requireNonNull(dataSource, "dataSource cannot be null");
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.transactionTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        this.jsonMapper = new ExperimentJsonMapper();
        this.rows = new ExperimentRows(this.jsonMapper);
    }

    public ExperimentPersistenceFactory(JdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate cannot be null");
        this.transactionTemplate = Objects.requireNonNull(transactionTemplate, "transactionTemplate cannot be null");
        this.jsonMapper = new ExperimentJsonMapper();
        this.rows = new ExperimentRows(this.jsonMapper);
    }

    public ExperimentStore createExperimentStore() {
        return new JdbcExperimentStore(jdbcTemplate, transactionTemplate, rows, jsonMapper);
    }

    public JobStore createJobStore() {
        return new JdbcJobStore(jdbcTemplate, transactionTemplate, rows, jsonMapper);
    }

    public ExecutionAttemptStore createExecutionAttemptStore() {
        return new JdbcExecutionAttemptStore(jdbcTemplate, transactionTemplate, rows);
    }

    public IdempotencyStore createIdempotencyStore() {
        return new JdbcIdempotencyStore(jdbcTemplate, transactionTemplate);
    }

    public OutboxStore createOutboxStore() {
        return new JdbcOutboxStore(jdbcTemplate, jsonMapper);
    }
}

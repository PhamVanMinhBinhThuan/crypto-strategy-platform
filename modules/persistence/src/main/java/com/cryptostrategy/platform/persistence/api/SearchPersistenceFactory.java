package com.cryptostrategy.platform.persistence.api;

import com.cryptostrategy.platform.persistence.internal.search.JdbcSearchRunStore;
import com.cryptostrategy.platform.search.api.port.out.SearchRunStore;
import com.cryptostrategy.platform.execution.api.port.out.TrustedSearchCoordinationGateway;
import com.cryptostrategy.platform.persistence.internal.search.JdbcTrustedSearchCoordinationGateway;
import com.cryptostrategy.platform.execution.api.port.out.SearchAllocationContextGateway;
import com.cryptostrategy.platform.persistence.internal.search.JdbcSearchAllocationContextGateway;
import com.cryptostrategy.platform.execution.api.port.out.SearchExperimentTransactionGateway;
import com.cryptostrategy.platform.persistence.internal.execution.JdbcSearchExperimentTransaction;
import com.cryptostrategy.platform.execution.api.port.out.SearchReproductionVerificationGateway;
import com.cryptostrategy.platform.execution.api.port.out.SearchReproductionGateway;
import com.cryptostrategy.platform.persistence.internal.search.JdbcSearchReproductionVerificationGateway;
import com.cryptostrategy.platform.execution.api.port.in.GetSearchReproductionVerificationUseCase;
import com.cryptostrategy.platform.persistence.internal.search.JdbcSearchReproductionVerificationQuery;
import com.cryptostrategy.platform.execution.api.port.in.GetSearchProgressUseCase;
import com.cryptostrategy.platform.persistence.internal.search.JdbcSearchProgressQuery;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

/** Composition boundary exposing Search persistence without leaking adapter internals. */
public final class SearchPersistenceFactory {
    private final JdbcTemplate jdbc;
    private final DataSource dataSource;

    public SearchPersistenceFactory(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.jdbc = new JdbcTemplate(dataSource);
    }

    public SearchPersistenceFactory(JdbcTemplate jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
        this.dataSource = null;
    }

    public SearchRunStore createSearchRunStore() {
        return new JdbcSearchRunStore(jdbc);
    }

    public GetSearchProgressUseCase createSearchProgressQuery() {
        return new JdbcSearchProgressQuery(jdbc);
    }

    public TrustedSearchCoordinationGateway createTrustedCoordinationGateway() {
        if (dataSource == null) {
            throw new IllegalStateException("Trusted coordination requires a transactional DataSource");
        }
        return new JdbcTrustedSearchCoordinationGateway(dataSource);
    }

    public SearchAllocationContextGateway createAllocationContextGateway() {
        return new JdbcSearchAllocationContextGateway(jdbc);
    }

    public SearchExperimentTransactionGateway createExperimentTransactionGateway() {
        if (dataSource == null) {
            throw new IllegalStateException("Composite Search transaction requires a DataSource");
        }
        return new JdbcSearchExperimentTransaction(dataSource);
    }

    public ExperimentTransactions createExperimentTransactions() {
        if (dataSource == null) {
            throw new IllegalStateException("Composite Search transaction requires a DataSource");
        }
        JdbcSearchExperimentTransaction adapter = new JdbcSearchExperimentTransaction(dataSource);
        return new ExperimentTransactions(adapter, adapter);
    }

    public record ExperimentTransactions(
            SearchExperimentTransactionGateway start,
            SearchReproductionGateway reproduction) {}

    public SearchReproductionVerificationGateway createReproductionVerificationGateway() {
        if (dataSource == null) {
            throw new IllegalStateException("Reproduction verification requires a DataSource");
        }
        return new JdbcSearchReproductionVerificationGateway(dataSource);
    }

    public GetSearchReproductionVerificationUseCase createReproductionVerificationQuery(
            ObjectMapper objectMapper) {
        return new JdbcSearchReproductionVerificationQuery(jdbc, objectMapper);
    }

}

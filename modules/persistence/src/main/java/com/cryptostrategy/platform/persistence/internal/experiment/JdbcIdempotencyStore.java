package com.cryptostrategy.platform.persistence.internal.experiment;

import com.cryptostrategy.platform.experiment.api.idempotency.IdempotencyClaim;
import com.cryptostrategy.platform.experiment.api.idempotency.IdempotencyOutcome;
import com.cryptostrategy.platform.experiment.api.port.out.IdempotencyStore;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class JdbcIdempotencyStore implements IdempotencyStore {

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public JdbcIdempotencyStore(
            JdbcTemplate jdbcTemplate,
            TransactionTemplate transactionTemplate
    ) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate cannot be null");
        this.transactionTemplate = Objects.requireNonNull(transactionTemplate, "transactionTemplate cannot be null");
    }

    @Override
    public IdempotencyClaim claim(UUID ownerUserId, String scope, String idempotencyKey, String requestHash, Instant expiresAt) {
        return transactionTemplate.execute(status -> {
            Instant now = Instant.now();
            int inserted = jdbcTemplate.update(
                    ExperimentSql.INSERT_IDEMPOTENCY_CLAIM,
                    ownerUserId,
                    scope,
                    idempotencyKey,
                    requestHash,
                    Timestamp.from(now),
                    Timestamp.from(expiresAt)
            );

            if (inserted > 0) {
                return IdempotencyClaim.acquired();
            }

            // Record already exists - query it
            try {
                return jdbcTemplate.queryForObject(
                        ExperimentSql.SELECT_IDEMPOTENCY_RECORD,
                        (rs, rowNum) -> {
                            String existingHash = rs.getString("request_hash");
                            if (!existingHash.equals(requestHash)) {
                                return IdempotencyClaim.conflict();
                            }
                            String state = rs.getString("state");
                            if ("IN_PROGRESS".equals(state)) {
                                return IdempotencyClaim.inProgressReplay();
                            }
                            int responseStatus = rs.getInt("response_status");
                            String responseBody = rs.getString("response_body");
                            Instant createdAt = rs.getTimestamp("created_at").toInstant();
                            IdempotencyOutcome outcome = new IdempotencyOutcome(
                                    String.valueOf(responseStatus),
                                    responseBody,
                                    createdAt
                            );
                            return IdempotencyClaim.completedReplay(outcome);
                        },
                        ownerUserId,
                        scope,
                        idempotencyKey
                );
            } catch (EmptyResultDataAccessException e) {
                return IdempotencyClaim.acquired();
            }
        });
    }

    @Override
    public void complete(UUID ownerUserId, String scope, String idempotencyKey, String outcomeCode, String responseBody) {
        int statusCode = 200;
        try {
            statusCode = Integer.parseInt(outcomeCode);
        } catch (NumberFormatException ignored) {}

        jdbcTemplate.update(
                ExperimentSql.COMPLETE_IDEMPOTENCY_RECORD,
                statusCode,
                responseBody != null ? responseBody : "{}",
                ownerUserId,
                scope,
                idempotencyKey
        );
    }

    @Override
    public Optional<IdempotencyOutcome> getOutcome(UUID ownerUserId, String scope, String idempotencyKey) {
        try {
            return jdbcTemplate.queryForObject(
                    ExperimentSql.SELECT_IDEMPOTENCY_RECORD,
                    (rs, rowNum) -> {
                        String state = rs.getString("state");
                        if (!"COMPLETED".equals(state)) {
                            return Optional.empty();
                        }
                        int responseStatus = rs.getInt("response_status");
                        String responseBody = rs.getString("response_body");
                        Instant createdAt = rs.getTimestamp("created_at").toInstant();
                        return Optional.of(new IdempotencyOutcome(
                                String.valueOf(responseStatus),
                                responseBody,
                                createdAt
                        ));
                    },
                    ownerUserId,
                    scope,
                    idempotencyKey
            );
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
}

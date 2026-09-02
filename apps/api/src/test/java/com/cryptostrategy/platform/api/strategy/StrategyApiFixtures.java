package com.cryptostrategy.platform.api.strategy;

import static com.cryptostrategy.platform.api.support.AuthenticatedUsers.USER_A_ID;
import static com.cryptostrategy.platform.api.support.TestIdentifiers.USER_STRATEGY_ID;
import static com.cryptostrategy.platform.api.support.TestIdentifiers.opaqueId;

import com.cryptostrategy.platform.strategy.api.model.SemanticVersion;
import com.cryptostrategy.platform.strategy.api.model.StrategyDescriptor;
import com.cryptostrategy.platform.strategy.api.model.StrategyKind;
import com.cryptostrategy.platform.strategy.api.model.StrategyPluginId;
import com.cryptostrategy.platform.strategy.api.model.StrategyReference;
import com.cryptostrategy.platform.strategy.api.model.StrategySignal;
import com.cryptostrategy.platform.strategy.api.model.StrategyVersionId;
import com.cryptostrategy.platform.strategy.api.model.UserStrategyId;
import com.cryptostrategy.platform.strategy.api.model.UserStrategyStatus;
import com.cryptostrategy.platform.strategy.api.model.UserStrategyVersionId;
import com.cryptostrategy.platform.strategy.api.model.UserStrategyVersionStatus;
import com.cryptostrategy.platform.strategy.api.model.parameter.CrossParameterConstraint;
import com.cryptostrategy.platform.strategy.api.model.parameter.ParameterDefinition;
import com.cryptostrategy.platform.strategy.api.model.parameter.ParameterType;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSchema;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterValue;
import com.cryptostrategy.platform.strategy.api.model.user.SingleStrategyDraftSource;
import com.cryptostrategy.platform.strategy.api.model.user.UserStrategy;
import com.cryptostrategy.platform.strategy.api.model.user.UserStrategyDetails;
import com.cryptostrategy.platform.strategy.api.model.user.UserStrategySummary;
import com.cryptostrategy.platform.strategy.api.model.user.UserStrategyVersion;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class StrategyApiFixtures {
    static final UserStrategyId STRATEGY_ID = new UserStrategyId(USER_STRATEGY_ID);
    static final UserStrategyVersionId VERSION_ID =
            new UserStrategyVersionId(opaqueId(21));
    static final UserStrategyVersionId NEXT_VERSION_ID =
            new UserStrategyVersionId(opaqueId(22));
    static final StrategyDescriptor DESCRIPTOR = descriptor();
    static final Instant CREATED_AT = Instant.parse("2026-09-02T01:00:00Z");
    static final UserStrategyDetails DETAILS = details(
            UserStrategyStatus.ACTIVE, draft(VERSION_ID, 1, UserStrategyVersionStatus.DRAFT));
    static final UserStrategyVersion PUBLISHED = new UserStrategyVersion(
            VERSION_ID,
            STRATEGY_ID,
            1,
            StrategyKind.SINGLE,
            source(),
            UserStrategyVersionStatus.PUBLISHED,
            "strategy-v1:private-ma",
            Optional.of(CREATED_AT.plusSeconds(60)),
            CREATED_AT);
    static final UserStrategyVersion NEXT_DRAFT =
            draft(NEXT_VERSION_ID, 2, UserStrategyVersionStatus.DRAFT);

    private StrategyApiFixtures() {}

    static UserStrategyDetails archivedDetails() {
        return details(
                UserStrategyStatus.ARCHIVED,
                new UserStrategyVersion(
                        PUBLISHED.id(),
                        PUBLISHED.userStrategyId(),
                        PUBLISHED.versionNo(),
                        PUBLISHED.kind(),
                        PUBLISHED.source(),
                        PUBLISHED.status(),
                        PUBLISHED.fingerprint(),
                        PUBLISHED.publishedAt(),
                        PUBLISHED.createdAt()));
    }

    static UserStrategySummary summary() {
        return new UserStrategySummary(
                STRATEGY_ID,
                StrategyKind.SINGLE,
                "Private MA",
                "Exact private parameters",
                CREATED_AT);
    }

    private static UserStrategyDetails details(
            UserStrategyStatus status, UserStrategyVersion latest) {
        Instant archivedAt = CREATED_AT.plusSeconds(120);
        return new UserStrategyDetails(
                new UserStrategy(
                        STRATEGY_ID,
                        USER_A_ID,
                        StrategyKind.SINGLE,
                        "Private MA",
                        "Exact private parameters",
                        status,
                        status == UserStrategyStatus.ARCHIVED
                                ? Optional.of(archivedAt)
                                : Optional.empty(),
                        CREATED_AT,
                        status == UserStrategyStatus.ARCHIVED ? archivedAt : CREATED_AT),
                latest);
    }

    private static UserStrategyVersion draft(
            UserStrategyVersionId id,
            int versionNo,
            UserStrategyVersionStatus status) {
        return new UserStrategyVersion(
                id,
                STRATEGY_ID,
                versionNo,
                StrategyKind.SINGLE,
                source(),
                status,
                "strategy-v1:private-ma:" + versionNo,
                Optional.empty(),
                CREATED_AT.plusSeconds(versionNo - 1L));
    }

    private static SingleStrategyDraftSource source() {
        return new SingleStrategyDraftSource(
                DESCRIPTOR.reference(),
                StrategyParameterSet.of(Map.of(
                        "fastPeriod", new StrategyParameterValue.IntegerValue(5),
                        "slowPeriod", new StrategyParameterValue.IntegerValue(25),
                        "threshold", new StrategyParameterValue.DecimalValue(
                                new BigDecimal("0.100000000001")))));
    }

    private static StrategyDescriptor descriptor() {
        return new StrategyDescriptor(
                new StrategyReference(
                        new StrategyVersionId(opaqueId(20)),
                        new StrategyPluginId("ma-crossover"),
                        new SemanticVersion(1, 0, 0)),
                "strategy-contract-v1",
                "Moving Average Crossover",
                "Compares fast and slow moving averages",
                "TREND",
                Set.of(StrategySignal.BUY, StrategySignal.SELL, StrategySignal.HOLD),
                25,
                new StrategyParameterSchema(
                        List.of(
                                integer("fastPeriod", 5, 2, 100),
                                integer("slowPeriod", 25, 3, 500),
                                new ParameterDefinition(
                                        "threshold",
                                        ParameterType.DECIMAL,
                                        true,
                                        Optional.of(new StrategyParameterValue.DecimalValue(
                                                new BigDecimal("0.100000000001"))),
                                        Optional.of(new BigDecimal("0.000000000001")),
                                        Optional.of(new BigDecimal("1.000000000001")),
                                        Set.of(),
                                        "Exact decimal threshold")),
                        List.of(new CrossParameterConstraint(
                                "fastPeriod", "slowPeriod"))),
                "strategy-descriptor-v1:ma-crossover:1.0.0");
    }

    private static ParameterDefinition integer(
            String name, long defaultValue, long minimum, long maximum) {
        return new ParameterDefinition(
                name,
                ParameterType.INTEGER,
                true,
                Optional.of(new StrategyParameterValue.IntegerValue(defaultValue)),
                Optional.of(BigDecimal.valueOf(minimum)),
                Optional.of(BigDecimal.valueOf(maximum)),
                Set.of(),
                name);
    }
}

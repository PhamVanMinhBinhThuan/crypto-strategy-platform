package com.cryptostrategy.platform.strategy.api.model.user.query;
import java.util.Objects;
public record UsableStrategyCatalog(StrategyCatalogPage systemStrategies, UserStrategyPage privateStrategies) { public UsableStrategyCatalog { Objects.requireNonNull(systemStrategies); Objects.requireNonNull(privateStrategies); } }

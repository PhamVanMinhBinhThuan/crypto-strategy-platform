package com.cryptostrategy.platform.strategy.api.model.user.query;
import com.cryptostrategy.platform.strategy.api.model.UserStrategyVersionId;
import java.util.Objects;
public record ResolveStrategySnapshotQuery(UserStrategyVersionId versionId) { public ResolveStrategySnapshotQuery { Objects.requireNonNull(versionId); } }

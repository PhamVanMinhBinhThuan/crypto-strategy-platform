package com.cryptostrategy.platform.strategy.api.port.out;
import com.cryptostrategy.platform.strategy.api.model.UserStrategyId;
import com.cryptostrategy.platform.strategy.api.model.UserStrategyVersionId;
import com.cryptostrategy.platform.strategy.api.model.user.StrategySnapshot;
import com.cryptostrategy.platform.strategy.api.model.user.UserStrategy;
import com.cryptostrategy.platform.strategy.api.model.user.UserStrategySummary;
import com.cryptostrategy.platform.strategy.api.model.user.UserStrategyVersion;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface UserStrategyStore {
    UserStrategyVersion create(UserStrategy root, UserStrategyVersion firstDraft);
    List<UserStrategySummary> listActive(UUID ownerUserId, int limit, Optional<String> cursor);
    Optional<UserStrategy> findRoot(UUID ownerUserId, UserStrategyId id);
    Optional<UserStrategyVersion> findVersion(UUID ownerUserId, UserStrategyVersionId id);
    UserStrategyVersion createNext(UUID ownerUserId, UserStrategyVersion draft, int expectedLatestVersionNo);
    UserStrategyVersion publish(UUID ownerUserId, UserStrategyVersionId id, int expectedVersionNo, Instant publishedAt);
    UserStrategy archive(UUID ownerUserId, UserStrategyId id, Instant archivedAt);
    Optional<StrategySnapshot> resolvePublished(UUID ownerUserId, UserStrategyVersionId id);
}

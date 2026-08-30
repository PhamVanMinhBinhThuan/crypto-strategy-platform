package com.cryptostrategy.platform.strategy.api.model.user.query;
import com.cryptostrategy.platform.strategy.api.model.user.UserStrategySummary;
import java.util.List;
import java.util.Optional;
public record UserStrategyPage(List<UserStrategySummary> items, Optional<String> nextCursor) { public UserStrategyPage { items=List.copyOf(items); nextCursor=nextCursor==null?Optional.empty():nextCursor; } }

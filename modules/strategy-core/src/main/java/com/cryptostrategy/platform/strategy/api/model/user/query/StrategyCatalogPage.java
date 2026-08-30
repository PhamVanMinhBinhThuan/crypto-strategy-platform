package com.cryptostrategy.platform.strategy.api.model.user.query;
import com.cryptostrategy.platform.strategy.api.model.StrategyDescriptor;
import java.util.List;
import java.util.Optional;
public record StrategyCatalogPage(List<StrategyDescriptor> items, Optional<String> nextCursor) { public StrategyCatalogPage { items=List.copyOf(items); nextCursor=nextCursor==null?Optional.empty():nextCursor; } }

package com.cryptostrategy.platform.news.api.port.out;

import com.cryptostrategy.platform.domain.api.market.AssetId;
import java.util.Map;
import java.util.Set;

public interface AssetResolver { Map<String, AssetId> resolveSymbols(Set<String> symbols); }

package com.cryptostrategy.platform.domain.api.market;

import java.util.Objects;
import java.util.Optional;

public record Asset(AssetId assetId, AssetSymbol symbol, Optional<String> name, boolean active) {
    public Asset {
        Objects.requireNonNull(assetId, "assetId");
        Objects.requireNonNull(symbol, "symbol");
        name = name == null ? Optional.empty() : name.map(String::trim).filter(value -> !value.isEmpty());
    }
}

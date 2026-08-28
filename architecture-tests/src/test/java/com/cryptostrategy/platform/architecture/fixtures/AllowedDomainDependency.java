package com.cryptostrategy.platform.architecture.fixtures.allowed.marketdata.api;

import com.cryptostrategy.platform.architecture.fixtures.allowed.domain.api.DomainValue;

public final class AllowedDomainDependency {
    private final DomainValue value = new DomainValue();

    public DomainValue value() {
        return value;
    }
}

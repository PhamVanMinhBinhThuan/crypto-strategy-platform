package com.cryptostrategy.platform.architecture.fixtures.internal.marketdata.api;

import com.cryptostrategy.platform.architecture.fixtures.internal.domain.internal.InternalType;

public final class ForbiddenInternalDependency {
    private final InternalType internalType = new InternalType();

    public InternalType internalType() {
        return internalType;
    }
}

package com.cryptostrategy.platform.architecture.fixtures.cycle.capabilityb.api;

import com.cryptostrategy.platform.architecture.fixtures.cycle.capabilitya.api.ForbiddenCycleA;

public final class ForbiddenCycleB {
    public ForbiddenCycleA next() {
        return new ForbiddenCycleA();
    }
}

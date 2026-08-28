package com.cryptostrategy.platform.architecture.fixtures.cycle.capabilitya.api;

import com.cryptostrategy.platform.architecture.fixtures.cycle.capabilityb.api.ForbiddenCycleB;

public final class ForbiddenCycleA {
    public ForbiddenCycleB next() {
        return new ForbiddenCycleB();
    }
}

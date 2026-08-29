package com.cryptostrategy.platform.architecture.fixtures.canonical.domain.api;

import java.time.LocalDateTime;

public final class ForbiddenCanonicalBoundaryValue {
    private final String strategyId = "not-a-uuid";
    private final double price = 0.1d;
    private final LocalDateTime observedAt = LocalDateTime.now();
}

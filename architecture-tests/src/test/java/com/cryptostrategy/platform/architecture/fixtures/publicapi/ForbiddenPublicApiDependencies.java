package com.cryptostrategy.platform.architecture.fixtures.publicapi.api;

import com.cryptostrategy.platform.architecture.fixtures.publicapi.capability.internal.InternalApplicationService;
import com.cryptostrategy.platform.architecture.fixtures.publicapi.capability.provider.ProviderImplementation;
import com.cryptostrategy.platform.persistence.fixture.JdbcPersistenceImplementation;

public final class ForbiddenPublicApiDependencies {
    private InternalApplicationService internalApplicationService;
    private JdbcPersistenceImplementation persistenceImplementation;
    private ProviderImplementation providerImplementation;
}

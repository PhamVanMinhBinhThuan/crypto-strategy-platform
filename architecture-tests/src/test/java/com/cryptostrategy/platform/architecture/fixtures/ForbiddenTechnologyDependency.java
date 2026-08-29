package com.cryptostrategy.platform.architecture.fixtures.technology.domain.api;

import java.sql.Connection;

public final class ForbiddenTechnologyDependency {
    private Connection connection;

    public Connection connection() {
        return connection;
    }
}

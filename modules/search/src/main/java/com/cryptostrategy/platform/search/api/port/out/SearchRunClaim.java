package com.cryptostrategy.platform.search.api.port.out;

import com.cryptostrategy.platform.search.api.model.SearchRun;
import java.util.Objects;

/** Optimistic claim whose expected version is the database fencing token. */
public record SearchRunClaim(SearchRun snapshot, long expectedVersion) {
    public SearchRunClaim {
        Objects.requireNonNull(snapshot, "snapshot");
        if (expectedVersion < 0 || expectedVersion != snapshot.version()) {
            throw new IllegalArgumentException("expectedVersion must match the claimed snapshot");
        }
    }
}

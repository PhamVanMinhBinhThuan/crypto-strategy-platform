package com.cryptostrategy.platform.domain.api.identity;

/** Marker contract for strongly typed business identifiers backed by a canonical ULID. */
public interface UlidIdentifier {
    String value();
}

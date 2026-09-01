package com.cryptostrategy.platform.contracts.api;

import com.cryptostrategy.platform.domain.api.identity.UlidIdentifier;
import com.cryptostrategy.platform.domain.api.identity.Ulids;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

public record MessageUlid(@JsonValue String value) implements UlidIdentifier {

    public MessageUlid {
        Objects.requireNonNull(value, "value cannot be null");
        value = Ulids.requireValid(value);
    }

    @JsonCreator
    public static MessageUlid of(String value) {
        return new MessageUlid(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

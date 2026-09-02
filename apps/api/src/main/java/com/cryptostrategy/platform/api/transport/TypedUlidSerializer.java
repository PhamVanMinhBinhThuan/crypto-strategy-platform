package com.cryptostrategy.platform.api.transport;

import com.cryptostrategy.platform.domain.api.identity.UlidIdentifier;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import java.io.IOException;
import java.io.Serial;

/** Serializes typed domain ULIDs as their canonical public scalar value. */
public final class TypedUlidSerializer extends StdScalarSerializer<UlidIdentifier> {
    @Serial
    private static final long serialVersionUID = 1L;

    public TypedUlidSerializer() {
        super(UlidIdentifier.class);
    }

    @Override
    public void serialize(
            UlidIdentifier value,
            JsonGenerator generator,
            SerializerProvider provider)
            throws IOException {
        generator.writeString(value.value());
    }
}

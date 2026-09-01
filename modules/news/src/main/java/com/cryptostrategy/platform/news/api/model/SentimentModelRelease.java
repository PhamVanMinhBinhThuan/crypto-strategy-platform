package com.cryptostrategy.platform.news.api.model;

import java.util.Objects;

public record SentimentModelRelease(String modelVersion, String modelName, String preprocessingVersion, String contractVersion) {
    public SentimentModelRelease {
        modelVersion = required(modelVersion, "modelVersion");
        modelName = required(modelName, "modelName");
        preprocessingVersion = required(preprocessingVersion, "preprocessingVersion");
        contractVersion = required(contractVersion, "contractVersion");
    }
    private static String required(String value, String name) {
        value = Objects.requireNonNull(value, name).trim();
        if (value.isEmpty() || value.length() > 200) throw new IllegalArgumentException("Invalid " + name);
        return value;
    }
}

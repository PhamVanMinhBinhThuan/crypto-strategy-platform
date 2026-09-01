package com.cryptostrategy.platform.evaluation.api.model;
import java.util.Objects;
public record MetricVersion(String value){public MetricVersion{Objects.requireNonNull(value);if(value.isBlank())throw new IllegalArgumentException("metric version");}}

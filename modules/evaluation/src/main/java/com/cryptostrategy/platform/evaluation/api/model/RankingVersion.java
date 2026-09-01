package com.cryptostrategy.platform.evaluation.api.model;
import java.util.Objects;
public record RankingVersion(String value){public RankingVersion{Objects.requireNonNull(value);if(value.isBlank())throw new IllegalArgumentException("ranking version");}}

package com.cryptostrategy.platform.backtesting.api.model;
import java.util.Objects;
public record BacktestProvenance(String manifestFingerprint,String datasetFingerprint,String strategyFingerprint){public BacktestProvenance{require(manifestFingerprint);require(datasetFingerprint);require(strategyFingerprint);}private static void require(String value){Objects.requireNonNull(value);if(value.isBlank())throw new IllegalArgumentException("fingerprint");}}

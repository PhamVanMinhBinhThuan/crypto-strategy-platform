package com.cryptostrategy.platform.evaluation.api.model;
import java.math.*;import java.util.Objects;
public record NormalizedMetrics(BigDecimal returnScore,BigDecimal winRateScore,BigDecimal drawdownScore,BigDecimal overallScore){public NormalizedMetrics{returnScore=n(returnScore);winRateScore=n(winRateScore);drawdownScore=n(drawdownScore);overallScore=n(overallScore);}private static BigDecimal n(BigDecimal v){v=Objects.requireNonNull(v).setScale(10,RoundingMode.HALF_EVEN);if(v.signum()<0||v.compareTo(BigDecimal.ONE)>0)throw new IllegalArgumentException("normalized metric");return v;}}

package com.cryptostrategy.platform.marketdata.api.model;

import java.util.Optional;

public record DatasetIntegrityResult(boolean valid, Optional<String> detail) {
    public DatasetIntegrityResult { detail = detail == null ? Optional.empty() : detail; }
    public static DatasetIntegrityResult validResult() { return new DatasetIntegrityResult(true, Optional.empty()); }
    public static DatasetIntegrityResult invalid(String detail) { return new DatasetIntegrityResult(false, Optional.of(detail)); }
}

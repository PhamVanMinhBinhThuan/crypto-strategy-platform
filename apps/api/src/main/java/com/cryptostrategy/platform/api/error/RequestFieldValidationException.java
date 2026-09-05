package com.cryptostrategy.platform.api.error;

import java.util.List;
import java.util.Map;

/** Safe field-level request violation that may be returned to a client. */
public final class RequestFieldValidationException extends IllegalArgumentException {
    private final List<Map<String, String>> fieldErrors;

    public RequestFieldValidationException(String field, String reason) {
        super("Request field validation failed");
        this.fieldErrors = List.of(Map.of("field", field, "reason", reason));
    }

    List<Map<String, String>> fieldErrors() {
        return fieldErrors;
    }
}

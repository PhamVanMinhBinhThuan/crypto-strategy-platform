package com.cryptostrategy.platform.api.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.cryptostrategy.platform.api.error.DependencyUnavailableException;
import com.cryptostrategy.platform.api.error.PublicErrorMapper;
import com.cryptostrategy.platform.experiment.api.error.IdempotencyConflictException;
import com.cryptostrategy.platform.experiment.api.error.ResourceInaccessibleException;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SearchPublicFailureRedactionTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("failures")
    void everyF010FailureMappingRedactsInternalDetails(String scenario, Exception failure,
            String expectedCode) {
        var mapped = new PublicErrorMapper().map(failure);
        String publicText = mapped.code() + " " + mapped.message() + " " + mapped.details();

        assertThat(mapped.code()).isEqualTo(expectedCode);
        assertThat(publicText).doesNotContain("postgres", "redis://", "password", "secret",
                "providerPayload", "select *", "C:\\", "/srv/", "Exception", "at com.");
    }

    static Stream<Arguments> failures() {
        String internal = "postgres password=secret redis://private providerPayload select * C:\\repo /srv/app";
        return Stream.of(
                Arguments.of("validation", new IllegalArgumentException(internal), "REQUEST_VALIDATION_FAILED"),
                Arguments.of("ownership", new ResourceInaccessibleException(internal), "RESOURCE_NOT_FOUND"),
                Arguments.of("idempotency", new IdempotencyConflictException(internal), "IDEMPOTENCY_KEY_CONFLICT"),
                Arguments.of("dependency", new DependencyUnavailableException(internal), "DEPENDENCY_UNAVAILABLE"),
                Arguments.of("unexpected", new RuntimeException(internal), "INTERNAL_ERROR"));
    }
}

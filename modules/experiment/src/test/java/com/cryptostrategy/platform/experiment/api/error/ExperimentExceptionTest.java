package com.cryptostrategy.platform.experiment.api.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExperimentExceptionTest {

    @Test
    @DisplayName("ExperimentValidationException carries error code and message")
    void validationException() {
        ExperimentValidationException ex = new ExperimentValidationException("Manifest is incomplete");
        assertThat(ex).isInstanceOf(ExperimentException.class);
        assertThat(ex.getMessage()).isEqualTo("Manifest is incomplete");
    }

    @Test
    @DisplayName("ResourceInaccessibleException represents inaccessible ownership outcome")
    void resourceInaccessibleException() {
        ResourceInaccessibleException ex = new ResourceInaccessibleException("Resource not accessible");
        assertThat(ex).isInstanceOf(ExperimentException.class);
        assertThat(ex.getMessage()).isEqualTo("Resource not accessible");
    }

    @Test
    @DisplayName("IdempotencyConflictException represents active hash gate conflict")
    void idempotencyConflictException() {
        IdempotencyConflictException ex = new IdempotencyConflictException("Key reused with conflicting payload hash");
        assertThat(ex).isInstanceOf(ExperimentException.class);
        assertThat(ex.getMessage()).isEqualTo("Key reused with conflicting payload hash");
    }

    @Test
    @DisplayName("InvalidStateTransitionException represents prohibited state machine step")
    void invalidStateTransitionException() {
        InvalidStateTransitionException ex = new InvalidStateTransitionException("Cannot transition from COMPLETED to RUNNING");
        assertThat(ex).isInstanceOf(ExperimentException.class);
        assertThat(ex.getMessage()).isEqualTo("Cannot transition from COMPLETED to RUNNING");
    }
}

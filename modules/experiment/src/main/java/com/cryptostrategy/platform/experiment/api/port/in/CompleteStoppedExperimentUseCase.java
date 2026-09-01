package com.cryptostrategy.platform.experiment.api.port.in;

import com.cryptostrategy.platform.experiment.api.ExperimentId;

public interface CompleteStoppedExperimentUseCase {

    /**
     * Attempts to complete an Experiment from STOP_REQUESTED to STOPPED.
     * Transitions only if the experiment is in STOP_REQUESTED status and all child jobs are in terminal state.
     *
     * @param experimentId the experiment ID
     * @return true if successfully transitioned to STOPPED or already STOPPED, false if active child jobs remain
     */
    boolean completeIfEligible(ExperimentId experimentId);
}

package com.cryptostrategy.platform.api.experiment;

import com.cryptostrategy.platform.experiment.api.Experiment;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import java.time.Instant;
import java.util.UUID;

public class ExperimentTestFixture {
    public static Experiment createExperiment(UUID ownerId) {
        return new Experiment(
                ExperimentId.generate(),
                ownerId,
                "test-experiment",
                com.cryptostrategy.platform.experiment.api.ExperimentStatus.CREATED,
                null,
                null,
                null,
                null,
                null,
                null,
                Instant.now());
    }
}

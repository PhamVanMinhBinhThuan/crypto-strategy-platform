package com.cryptostrategy.platform.execution.api;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
public record ReproductionVerification(ExperimentId reproductionExperimentId, ExperimentId sourceExperimentId,
        ExecutionEvidence original, ExecutionEvidence reproduced, ReproductionOutcome outcome,
        Map<String,Object> differences, Map<String,String> fingerprints, Instant verifiedAt) {
    public ReproductionVerification { Objects.requireNonNull(reproductionExperimentId);Objects.requireNonNull(sourceExperimentId);Objects.requireNonNull(original);Objects.requireNonNull(reproduced);Objects.requireNonNull(outcome);differences=Map.copyOf(differences);fingerprints=Map.copyOf(fingerprints);Objects.requireNonNull(verifiedAt); }
}

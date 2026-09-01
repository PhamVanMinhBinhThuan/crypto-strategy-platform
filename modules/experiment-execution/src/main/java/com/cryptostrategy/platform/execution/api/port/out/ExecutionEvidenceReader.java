package com.cryptostrategy.platform.execution.api.port.out;
import com.cryptostrategy.platform.execution.api.ExecutionEvidence;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import java.util.UUID;
@FunctionalInterface public interface ExecutionEvidenceReader { ExecutionEvidence load(UUID ownerUserId, ExperimentId experimentId); }

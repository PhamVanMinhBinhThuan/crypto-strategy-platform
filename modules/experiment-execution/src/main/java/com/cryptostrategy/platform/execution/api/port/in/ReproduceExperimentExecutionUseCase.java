package com.cryptostrategy.platform.execution.api.port.in;
import com.cryptostrategy.platform.execution.api.ReproductionVerification;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import java.util.UUID;
public interface ReproduceExperimentExecutionUseCase { ReproductionVerification reproduce(UUID ownerUserId, ExperimentId sourceExperimentId, String newName); }

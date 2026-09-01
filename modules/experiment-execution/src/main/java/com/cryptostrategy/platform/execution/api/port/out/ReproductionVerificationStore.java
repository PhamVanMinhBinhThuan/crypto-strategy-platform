package com.cryptostrategy.platform.execution.api.port.out;
import com.cryptostrategy.platform.execution.api.ReproductionVerification;
@FunctionalInterface public interface ReproductionVerificationStore { ReproductionVerification save(ReproductionVerification verification); }

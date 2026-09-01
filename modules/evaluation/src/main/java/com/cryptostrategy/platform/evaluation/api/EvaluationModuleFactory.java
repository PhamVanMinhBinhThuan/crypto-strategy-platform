package com.cryptostrategy.platform.evaluation.api;

import com.cryptostrategy.platform.evaluation.api.port.in.EvaluateBacktestUseCase;
import com.cryptostrategy.platform.evaluation.api.port.out.EvaluationResultStore;
import com.cryptostrategy.platform.evaluation.internal.EvaluateBacktestService;

public final class EvaluationModuleFactory {
    private EvaluationModuleFactory() {}

    public static EvaluateBacktestUseCase evaluateBacktestUseCase(EvaluationResultStore store) {
        return new EvaluateBacktestService(store);
    }
}

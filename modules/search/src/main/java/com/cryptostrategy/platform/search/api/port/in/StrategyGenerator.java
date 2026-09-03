package com.cryptostrategy.platform.search.api.port.in;

import com.cryptostrategy.platform.search.api.model.GenerationOutcome;
import com.cryptostrategy.platform.search.api.model.GenerationRequest;
import com.cryptostrategy.platform.search.api.model.GeneratorDescriptor;

/** Pure deterministic generator contract; implementations must not perform I/O. */
public interface StrategyGenerator {
    GeneratorDescriptor descriptor();

    GenerationOutcome generateNext(GenerationRequest request);
}

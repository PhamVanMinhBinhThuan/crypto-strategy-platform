package com.cryptostrategy.platform.search.api.port.in;

import com.cryptostrategy.platform.search.api.model.GenerationOutcome;
import com.cryptostrategy.platform.search.api.model.GenerationRequest;
import com.cryptostrategy.platform.search.api.model.GeneratorId;
import com.cryptostrategy.platform.search.api.model.GeneratorVersion;

/** Biên thuần công khai để orchestration sử dụng generator cùng các invariant do Search sở hữu. */
public interface SearchGenerationUseCase {
    GenerationOutcome generate(
            GeneratorId generatorId,
            GeneratorVersion generatorVersion,
            GenerationRequest request);
}

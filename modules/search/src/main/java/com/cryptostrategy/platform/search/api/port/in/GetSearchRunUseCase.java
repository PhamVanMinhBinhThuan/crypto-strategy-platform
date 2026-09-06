package com.cryptostrategy.platform.search.api.port.in;

import com.cryptostrategy.platform.search.api.model.SearchExperimentId;
import com.cryptostrategy.platform.search.api.model.SearchRun;
import java.util.Optional;

@FunctionalInterface
public interface GetSearchRunUseCase {
    Optional<SearchRun> findByExperimentId(SearchExperimentId experimentId);
}

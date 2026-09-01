package com.cryptostrategy.platform.backtesting.api.port.out;

import com.cryptostrategy.platform.experiment.api.provenance.StrategyProvenanceSnapshot;

@FunctionalInterface
public interface FrozenStrategyResolver {
    ResolvedStrategy resolve(StrategyProvenanceSnapshot provenance, com.cryptostrategy.platform.experiment.api.CandidateDefinition candidate);
}

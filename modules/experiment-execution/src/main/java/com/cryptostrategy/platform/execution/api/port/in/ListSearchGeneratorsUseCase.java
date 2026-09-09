package com.cryptostrategy.platform.execution.api.port.in;

import java.util.List;

/** Authoritative catalog of generators executable by Search orchestration. */
public interface ListSearchGeneratorsUseCase {
    List<GeneratorDescriptor> listGenerators();

    record GeneratorDescriptor(
            RequestedGeneratorId generatorId,
            String version,
            String displayName,
            String stateContractVersion,
            String descriptorFingerprint) {}
}

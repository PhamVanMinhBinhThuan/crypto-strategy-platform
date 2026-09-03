package com.cryptostrategy.platform.api.experiment;

import com.cryptostrategy.platform.execution.api.port.in.SearchStartCommandFactory;
import com.cryptostrategy.platform.execution.api.port.in.StartSearchExperimentUseCase.StartCommand;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Chỉ ánh xạ HTTP DTO; validation và domain construction thuộc application boundary. */
@Component
final class ExperimentRequestMapper {
    private final SearchStartCommandFactory commands;

    ExperimentRequestMapper(SearchStartCommandFactory commands) {
        this.commands = Objects.requireNonNull(commands, "commands");
    }

    StartCommand map(UUID owner, String key, String hash, String correlationId,
            CommandDtos.StartExperimentRequest request) {
        Objects.requireNonNull(request, "request");
        Map<String, SearchStartCommandFactory.ParameterDomain> parameters = new TreeMap<>();
        if (request.searchSpace() != null && request.searchSpace().parameters() != null) {
            request.searchSpace().parameters().forEach((name, value) -> parameters.put(name,
                    new SearchStartCommandFactory.ParameterDomain(
                            value.minimum(), value.maximum(), value.options())));
        }
        return commands.create(new SearchStartCommandFactory.Request(
                owner, key, hash, correlationId, request.name(), request.datasetId(),
                request.generator() == null ? null : String.valueOf(request.generator().generatorId()),
                request.generator() == null ? null : request.generator().version(),
                request.generator() == null ? null : request.generator().seed(),
                request.searchSpace() == null ? null : request.searchSpace().strategyId(),
                request.searchSpace() == null ? null : request.searchSpace().strategyVersion(),
                Map.copyOf(parameters),
                request.stopCondition() == null ? null : request.stopCondition().maximumCandidates(),
                request.stopCondition() == null ? null : request.stopCondition().maximumDurationSeconds(),
                request.topK()));
    }
}

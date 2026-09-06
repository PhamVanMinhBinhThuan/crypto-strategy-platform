package com.cryptostrategy.platform.api.experiment;

import com.cryptostrategy.platform.execution.api.port.in.ListSearchGeneratorsUseCase;
import java.util.List;
import java.util.Objects;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read-only public catalog derived from the same registered baseline used to start Search. */
@RestController
@RequestMapping("/api/v1/search")
public final class SearchCatalogController {
    private final ListSearchGeneratorsUseCase generators;

    public SearchCatalogController(ListSearchGeneratorsUseCase generators) {
        this.generators = Objects.requireNonNull(generators, "generators");
    }

    @GetMapping("/generators")
    public GeneratorPage generators() {
        return new GeneratorPage(generators.listGenerators().stream()
                .map(descriptor -> new GeneratorItem(
                        new CommandDtos.GeneratorId(descriptor.generatorId().value()),
                        descriptor.version(), descriptor.displayName(),
                        descriptor.stateContractVersion(), descriptor.descriptorFingerprint()))
                .toList());
    }

    public record GeneratorPage(List<GeneratorItem> items) {}

    public record GeneratorItem(CommandDtos.GeneratorId generatorId, String version, String displayName,
            String stateContractVersion, String descriptorFingerprint) {}
}

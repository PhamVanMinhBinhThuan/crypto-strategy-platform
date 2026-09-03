package com.cryptostrategy.platform.execution.internal;

import com.cryptostrategy.platform.contracts.api.MessageEnvelope;
import com.cryptostrategy.platform.contracts.api.MessageTypes;
import com.cryptostrategy.platform.contracts.api.SearchRequestPayload;
import com.cryptostrategy.platform.execution.api.port.in.SearchStartCommandFactory;
import com.cryptostrategy.platform.execution.api.port.in.StartSearchExperimentUseCase.StartCommand;
import com.cryptostrategy.platform.experiment.api.Experiment;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.ExperimentManifest;
import com.cryptostrategy.platform.experiment.api.job.Job;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.experiment.api.outbox.OutboxEvent;
import com.cryptostrategy.platform.experiment.api.provenance.DatasetProvenanceSnapshot;
import com.cryptostrategy.platform.experiment.api.provenance.StrategyProvenanceSnapshot;
import com.cryptostrategy.platform.marketdata.api.port.in.GetDatasetUseCase;
import com.cryptostrategy.platform.search.api.model.*;
import com.cryptostrategy.platform.search.api.SearchModuleFactory;
import com.cryptostrategy.platform.strategy.api.model.SemanticVersion;
import com.cryptostrategy.platform.strategy.api.model.parameter.*;
import com.cryptostrategy.platform.strategy.api.port.in.StrategyFingerprintCalculator;
import com.cryptostrategy.platform.strategy.api.port.in.StrategyRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;

/** Dựng và đóng băng Start graph bên trong cross-capability application layer. */
public final class SearchStartCommandFactoryService implements SearchStartCommandFactory {
    private static final int MAX_INTEGER_DOMAIN_SIZE = 10_000;
    private final GetDatasetUseCase datasets;
    private final StrategyRegistry strategies;
    private final StrategyFingerprintCalculator fingerprints;
    private final ObjectMapper json;
    private final String softwareVersion;
    private final String gitCommit;
    private final Clock clock;

    public SearchStartCommandFactoryService(GetDatasetUseCase datasets, StrategyRegistry strategies,
            StrategyFingerprintCalculator fingerprints, ObjectMapper json, String softwareVersion,
            String gitCommit, Clock clock) {
        this.datasets = Objects.requireNonNull(datasets);
        this.strategies = Objects.requireNonNull(strategies);
        this.fingerprints = Objects.requireNonNull(fingerprints);
        this.json = Objects.requireNonNull(json);
        this.softwareVersion = Objects.requireNonNull(softwareVersion);
        this.gitCommit = Objects.requireNonNull(gitCommit);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override public StartCommand create(Request request) {
        Objects.requireNonNull(request, "request");
        if (request.datasetId() == null || request.strategyId() == null || request.parameters() == null) {
            throw new IllegalArgumentException("Dataset, generator, search space and stop condition are required");
        }
        if (request.generatorId() == null || !"random-search".equals(request.generatorId().value())
                || !"1.0.0".equals(request.generatorVersion())) {
            throw new IllegalArgumentException("Unsupported generator identity/version");
        }
        if (request.maximumCandidates() == null && request.maximumDurationSeconds() == null) {
            throw new IllegalArgumentException("At least one Search stop condition is required");
        }
        Instant now = clock.instant();
        long seed = request.seed() == null ? 0L : request.seed();
        int maximumCandidates = request.maximumCandidates() == null ? 10_000 : positive(request.maximumCandidates(), "maximumCandidates");
        int durationSeconds = request.maximumDurationSeconds() == null ? 86_400 : positive(request.maximumDurationSeconds(), "maximumDurationSeconds");
        int topK = positive(request.topK(), "topK");
        var dataset = datasets.getDataset(request.datasetId());
        var datasetSnapshot = new DatasetProvenanceSnapshot(dataset.datasetVersionId(), dataset.version(),
                dataset.checksum(), dataset.provider().value(), dataset.tradingPair().canonicalSymbol(),
                dataset.timeframe().code(), dataset.normalizationVersion(), dataset.rangeStart(),
                dataset.rangeEnd(), dataset.candleCount());
        SemanticVersion strategyVersion = SemanticVersion.parse(request.strategyVersion());
        var descriptor = strategies.descriptor(request.strategyId(), strategyVersion);
        Map<String, ParameterDefinition> definitions = new TreeMap<>();
        descriptor.parameterSchema().definitions().forEach(value -> definitions.put(value.name(), value));
        Map<String, SearchParameterDomain> domains = new TreeMap<>();
        Map<String, StrategyParameterValue> representative = new TreeMap<>();
        request.parameters().forEach((name, range) -> {
            ParameterDefinition definition = definitions.get(name);
            if (definition == null) throw new IllegalArgumentException("Unknown Strategy parameter: " + name);
            SearchParameterDomain domain = domain(definition.type(), range);
            domains.put(name, domain);
            representative.put(name, domain.options().getFirst());
        });
        SearchSpace searchSpace = new SearchSpace(domains);
        var frozenParameters = strategies.resolveParameters(request.strategyId(), strategyVersion, representative);
        var strategySnapshot = StrategyProvenanceSnapshot.single(descriptor.reference(), frozenParameters,
                Optional.empty(), fingerprints.single(descriptor.reference(), frozenParameters));
        ExperimentId experimentId = ExperimentId.generate();
        JobId searchJobId = JobId.generate();
        SearchRunId searchRunId = SearchRunId.generate();
        Experiment experiment = Experiment.create(experimentId, request.ownerUserId(), request.name(), null, null, now);
        Job searchJob = Job.createSearchJob(searchJobId, experimentId, request.correlationId(), maximumCandidates, now);
        var baseline = SearchModuleFactory.baselineDefinition(seed);
        GeneratorDescriptor generator = baseline.descriptor();
        SearchRun run = SearchRun.pending(searchRunId,
                new SearchExperimentId(experimentId.value()), new SearchJobId(searchJobId.value()),
                SearchRunMode.GENERATION, null, generator, seed, SearchModuleFactory.canonicalSearchSpaceFingerprint(searchSpace),
                baseline.initialState(),
                new SearchStopConditions(maximumCandidates, Duration.ofSeconds(durationSeconds)),
                Math.min(maximumCandidates, SearchRequestPayload.MAX_CONCURRENCY_HINT), now);
        Map<String, Object> searchConfig = new LinkedHashMap<>();
        searchConfig.put("contractVersion", "search-config-v1");
        searchConfig.put("strategyId", request.strategyId().value());
        searchConfig.put("strategyVersion", strategyVersion.toString());
        searchConfig.put("generatorId", "random-search");
        searchConfig.put("generatorVersion", "1.0.0");
        searchConfig.put("seed", seed);
        searchConfig.put("searchSpace", canonicalDomains(searchSpace));
        searchConfig.put("maximumCandidates", maximumCandidates);
        searchConfig.put("maximumDurationSeconds", durationSeconds);
        searchConfig.put("topK", topK);
        ExperimentManifest manifest = new ExperimentManifest(experimentId, "manifest-v1", datasetSnapshot,
                strategySnapshot, Map.of("assumptionsVersion", "backtest-assumptions-v1"), Map.copyOf(searchConfig),
                Map.of("metricVersion", "metric-v1", "rankingVersion", "ranking-v1"), null,
                softwareVersion, gitCommit,
                sha256(request.canonicalRequestHash() + '\n' + SearchModuleFactory.canonicalSearchSpaceFingerprint(searchSpace)), now);
        String messageId = com.cryptostrategy.platform.domain.api.identity.Ulids.generate();
        var envelope = new MessageEnvelope<>(messageId, MessageTypes.CURRENT_VERSION,
                MessageTypes.SEARCH_REQUEST, now, request.correlationId(),
                new SearchRequestPayload(searchJobId.value(), experimentId.value(), run.maxInFlight(), topK));
        OutboxEvent outbox = new OutboxEvent(com.cryptostrategy.platform.domain.api.identity.Ulids.generate(),
                messageId, "JOB", searchJobId.value(), MessageTypes.SEARCH_REQUEST, "1", writeJson(envelope),
                Map.of("correlationId", request.correlationId()), now);
        return new StartCommand(request.ownerUserId(), request.idempotencyKey(), request.canonicalRequestHash(),
                now.plus(Duration.ofHours(24)), experiment, manifest, searchJob, run, outbox);
    }

    private static SearchParameterDomain domain(ParameterType type, ParameterDomain request) {
        Objects.requireNonNull(request, "parameter range");
        if (request.options() != null && !request.options().isEmpty()) {
            if (type != ParameterType.ENUM && type != ParameterType.TEXT) throw new IllegalArgumentException("Options are only supported for ENUM/TEXT parameters");
            return new SearchParameterDomain(type, request.options().stream().map(value -> type == ParameterType.ENUM
                    ? new StrategyParameterValue.EnumValue(value) : new StrategyParameterValue.TextValue(value))
                    .map(StrategyParameterValue.class::cast).toList());
        }
        if (type != ParameterType.INTEGER || request.minimum() == null || request.maximum() == null) throw new IllegalArgumentException("MVP Search requires an integer range or discrete options");
        long size;
        try { size = Math.addExact(Math.subtractExact(request.maximum(), request.minimum()), 1L); }
        catch (ArithmeticException failure) { throw new IllegalArgumentException("Integer Search domain is too large", failure); }
        if (size < 1 || size > MAX_INTEGER_DOMAIN_SIZE) throw new IllegalArgumentException("Integer Search domain is empty or too large");
        ArrayList<StrategyParameterValue> values = new ArrayList<>((int) size);
        for (long offset = 0; offset < size; offset++) values.add(new StrategyParameterValue.IntegerValue(request.minimum() + offset));
        return new SearchParameterDomain(type, values);
    }

    private static Map<String, Object> canonicalDomains(SearchSpace searchSpace) {
        Map<String, Object> result = new TreeMap<>();
        searchSpace.parameters().forEach((name, domain) -> result.put(name,
                Map.of("type", domain.type().name(), "options", domain.options().stream().map(StrategyParameterValue::canonicalText).toList())));
        return Map.copyOf(result);
    }

    private String writeJson(Object value) {
        try { return json.writeValueAsString(value); }
        catch (JsonProcessingException failure) { throw new IllegalArgumentException("Search request cannot be serialized", failure); }
    }

    private static int positive(Integer value, String name) {
        if (value == null || value < 1) throw new IllegalArgumentException(name + " must be positive");
        return value;
    }

    private static String sha256(String value) {
        try { return "sha256:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}

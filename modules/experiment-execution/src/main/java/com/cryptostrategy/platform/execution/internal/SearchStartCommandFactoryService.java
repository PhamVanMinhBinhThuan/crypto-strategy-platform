package com.cryptostrategy.platform.execution.internal;

import com.cryptostrategy.platform.contracts.api.MessageEnvelope;
import com.cryptostrategy.platform.contracts.api.MessageTypes;
import com.cryptostrategy.platform.contracts.api.SearchRequestPayload;
import com.cryptostrategy.platform.backtesting.api.model.BacktestAssumptions;
import com.cryptostrategy.platform.execution.api.port.in.SearchStartCommandFactory;
import com.cryptostrategy.platform.execution.api.port.in.StartSearchExperimentUseCase.StartCommand;
import com.cryptostrategy.platform.experiment.api.Experiment;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.ExperimentManifest;
import com.cryptostrategy.platform.experiment.api.job.Job;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.experiment.api.outbox.OutboxEvent;
import com.cryptostrategy.platform.experiment.api.provenance.DatasetProvenanceSnapshot;
import com.cryptostrategy.platform.experiment.api.provenance.StrategyComponentSnapshot;
import com.cryptostrategy.platform.experiment.api.provenance.StrategyProvenanceSnapshot;
import com.cryptostrategy.platform.marketdata.api.port.in.GetDatasetUseCase;
import com.cryptostrategy.platform.search.api.model.*;
import com.cryptostrategy.platform.search.api.SearchModuleFactory;
import com.cryptostrategy.platform.search.api.CompositeSearchCanonicalization;
import com.cryptostrategy.platform.strategy.api.model.SemanticVersion;
import com.cryptostrategy.platform.strategy.api.model.user.*;
import com.cryptostrategy.platform.strategy.api.model.user.query.ResolveStrategySnapshotQuery;
import com.cryptostrategy.platform.strategy.api.model.parameter.*;
import com.cryptostrategy.platform.strategy.api.port.in.ResolveStrategySnapshotUseCase;
import com.cryptostrategy.platform.strategy.api.port.in.StrategyFingerprintCalculator;
import com.cryptostrategy.platform.strategy.api.port.in.StrategyRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.*;

/** Dựng và đóng băng Start graph bên trong cross-capability application layer. */
public final class SearchStartCommandFactoryService implements SearchStartCommandFactory {
    private static final int MAX_INTEGER_DOMAIN_SIZE = 10_000;
    private final GetDatasetUseCase datasets;
    private final StrategyRegistry strategies;
    private final ResolveStrategySnapshotUseCase userStrategies;
    private final StrategyFingerprintCalculator fingerprints;
    private final ObjectMapper json;
    private final String softwareVersion;
    private final String gitCommit;
    private final Clock clock;

    public SearchStartCommandFactoryService(GetDatasetUseCase datasets, StrategyRegistry strategies,
            ResolveStrategySnapshotUseCase userStrategies, StrategyFingerprintCalculator fingerprints,
            ObjectMapper json, String softwareVersion, String gitCommit, Clock clock) {
        this.datasets = Objects.requireNonNull(datasets);
        this.strategies = Objects.requireNonNull(strategies);
        this.userStrategies = Objects.requireNonNull(userStrategies);
        this.fingerprints = Objects.requireNonNull(fingerprints);
        this.json = Objects.requireNonNull(json);
        this.softwareVersion = Objects.requireNonNull(softwareVersion);
        this.gitCommit = Objects.requireNonNull(gitCommit);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override public StartCommand create(Request request) {
        Objects.requireNonNull(request, "request");
        if (request.datasetId() == null || request.parameters() == null) {
            throw new IllegalArgumentException("Dataset, generator, search space and stop condition are required");
        }
        boolean compositeV2 = request.strategyPool() != null && !request.strategyPool().isEmpty();
        boolean userStrategy = request.userStrategyVersionId() != null;
        boolean systemStrategy = request.strategyId() != null;
        if (!compositeV2 && userStrategy == systemStrategy) {
            throw new IllegalArgumentException(
                    "Choose exactly one published User Strategy or system Strategy Search Space");
        }
        if (compositeV2 && (userStrategy || systemStrategy)) {
            throw new IllegalArgumentException("Use either the v2 strategy pool or the legacy Search Space");
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
        int configuredMaximumCandidates = maximumCandidates;
        int durationSeconds = request.maximumDurationSeconds() == null ? 86_400 : positive(request.maximumDurationSeconds(), "maximumDurationSeconds");
        Integer maximumWithoutImprovement = request.maximumWithoutImprovement() == null
                ? null : positive(request.maximumWithoutImprovement(), "maximumWithoutImprovement");
        int topK = positive(request.topK(), "topK");
        var dataset = datasets.getDataset(request.datasetId());
        var datasetSnapshot = new DatasetProvenanceSnapshot(dataset.datasetVersionId(), dataset.version(),
                dataset.checksum(), dataset.provider().value(), dataset.tradingPair().canonicalSymbol(),
                dataset.timeframe().code(), dataset.normalizationVersion(), dataset.rangeStart(),
                dataset.rangeEnd(), dataset.candleCount());
        ResolvedStrategyInput strategyInput = compositeV2
                ? resolveCompositeStrategyPool(request)
                : userStrategy ? resolveUserStrategy(request) : resolveSystemStrategy(request);
        SearchSpace searchSpace = strategyInput.searchSpace();
        BigInteger finiteCardinality = strategyInput.compositeSearchSpace()
                .map(CompositeSearchSpace::combinationCount)
                .orElseGet(searchSpace::combinationCount);
        if (finiteCardinality.signum() == 0) throw new IllegalArgumentException("Search Space is empty");
        maximumCandidates = finiteCardinality.min(BigInteger.valueOf(maximumCandidates)).intValueExact();
        StrategyProvenanceSnapshot strategySnapshot = strategyInput.provenance();
        ExperimentId experimentId = ExperimentId.generate();
        JobId searchJobId = JobId.generate();
        SearchRunId searchRunId = SearchRunId.generate();
        Experiment experiment = Experiment.create(experimentId, request.ownerUserId(), request.name(), null, null, now);
        Job searchJob = Job.createSearchJob(searchJobId, experimentId, request.correlationId(), maximumCandidates, now);
        var baseline = SearchModuleFactory.baselineDefinition(seed);
        GeneratorDescriptor generator = baseline.descriptor();
        int requestedConcurrency = request.requestedConcurrency() == null
                ? Math.min(maximumCandidates, SearchRequestPayload.MAX_CONCURRENCY_HINT)
                : positive(request.requestedConcurrency(), "requestedConcurrency");
        if (requestedConcurrency > SearchRequestPayload.MAX_CONCURRENCY_HINT) {
            throw new IllegalArgumentException("requestedConcurrency exceeds the supported maximum");
        }
        String searchSpaceFingerprint = strategyInput.compositeSearchSpace()
                .map(CompositeSearchCanonicalization::searchSpaceFingerprint)
                .orElseGet(() -> SearchModuleFactory.canonicalSearchSpaceFingerprint(searchSpace));
        SearchRun run = SearchRun.pending(searchRunId,
                new SearchExperimentId(experimentId.value()), new SearchJobId(searchJobId.value()),
                SearchRunMode.GENERATION, null, generator, seed, searchSpaceFingerprint,
                baseline.initialState(),
                new SearchStopConditions(maximumCandidates, Duration.ofSeconds(durationSeconds),
                        maximumWithoutImprovement),
                Math.min(maximumCandidates, requestedConcurrency), now);
        Map<String, Object> searchConfig = new LinkedHashMap<>();
        searchConfig.put("contractVersion", compositeV2 ? "search-config-v2" : "search-config-v1");
        searchConfig.putAll(strategyInput.identity());
        searchConfig.put("generatorId", "random-search");
        searchConfig.put("generatorVersion", "1.0.0");
        searchConfig.put("seed", seed);
        searchConfig.put("searchSpace", strategyInput.compositeSearchSpace()
                .map(SearchStartCommandFactoryService::canonicalCompositeSpace)
                .orElseGet(() -> canonicalDomains(searchSpace)));
        searchConfig.put("maximumCandidates", configuredMaximumCandidates);
        searchConfig.put("effectiveMaximumCandidates", maximumCandidates);
        searchConfig.put("maximumDurationSeconds", durationSeconds);
        if (maximumWithoutImprovement != null) {
            searchConfig.put("maximumWithoutImprovement", maximumWithoutImprovement);
        }
        searchConfig.put("topK", topK);
        searchConfig.put("requestedConcurrency", run.maxInFlight());
        BacktestAssumptions assumptions = request.backtestAssumptions() == null
                ? BacktestAssumptions.mvp(new BigDecimal("10000"), new BigDecimal("0.001"), BigDecimal.ZERO)
                : BacktestAssumptions.mvp(request.backtestAssumptions().initialCapital(),
                        request.backtestAssumptions().feeRate(), request.backtestAssumptions().slippageRate());
        Map<String, Object> backtestConfig = Map.of(
                "assumptionsVersion", "backtest-assumptions-v1",
                "initialCapital", assumptions.initialCapital().value().toPlainString(),
                "feeRate", assumptions.feeRate().toPlainString(),
                "slippageRate", assumptions.slippageRate().toPlainString(),
                "executionPriceRule", "NEXT_CANDLE_OPEN",
                "positionMode", "LONG_ONLY",
                "forceCloseAtEnd", true,
                "roundingMode", "HALF_EVEN");
        ExperimentManifest manifest = new ExperimentManifest(experimentId,
                compositeV2 ? "manifest-v2" : "manifest-v1", datasetSnapshot,
                strategySnapshot, backtestConfig, Map.copyOf(searchConfig),
                Map.of("metricVersion", "metric-v1", "rankingVersion", "ranking-v1"), null,
                softwareVersion, gitCommit,
                sha256(request.canonicalRequestHash() + '\n' + searchSpaceFingerprint), now);
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

    private ResolvedStrategyInput resolveCompositeStrategyPool(Request request) {
        int minimum = positive(request.minimumComponents(), "minimumComponents");
        int maximum = positive(request.maximumComponents(), "maximumComponents");
        if (!com.cryptostrategy.platform.search.api.model.SearchCombinationPolicy.MAJORITY_VOTE
                .equals(request.combinationPolicyId())
                || !"1.0.0".equals(request.combinationPolicyVersion())) {
            throw new IllegalArgumentException("F-015 supports Majority Vote 1.0.0 only");
        }
        List<SearchStrategyPoolEntry> pool = new ArrayList<>();
        List<StrategyComponentSnapshot> snapshots = new ArrayList<>();
        for (StrategyPoolEntryRequest entry : request.strategyPool()) {
            boolean system = entry.strategyId() != null;
            boolean user = entry.userStrategyVersionId() != null;
            if (system == user) {
                throw new IllegalArgumentException("Each pool entry must identify exactly one strategy artifact");
            }
            if (system) {
                SemanticVersion version = SemanticVersion.parse(entry.strategyVersion());
                var descriptor = strategies.descriptor(entry.strategyId(), version);
                Map<String, ParameterDefinition> definitions = new TreeMap<>();
                descriptor.parameterSchema().definitions().forEach(value -> definitions.put(value.name(), value));
                Map<String, SearchParameterDomain> domains = resolveDomains(entry.parameters(), definitions);
                Map<String, StrategyParameterValue> representative = representative(domains);
                StrategyParameterSet frozen = strategies.resolveParameters(entry.strategyId(), version, representative);
                pool.add(new SearchStrategyPoolEntry(descriptor.reference(), domains,
                        descriptor.parameterSchema().constraints()));
                snapshots.add(new StrategyComponentSnapshot(descriptor.reference(), frozen));
            } else {
                if (entry.parameters() != null && !entry.parameters().isEmpty()) {
                    throw new IllegalArgumentException("Published User Strategy pool entries use frozen parameters");
                }
                StrategySnapshot snapshot = userStrategies.resolveSnapshot(request.ownerUserId(),
                        new ResolveStrategySnapshotQuery(entry.userStrategyVersionId()));
                if (!(snapshot instanceof SingleStrategySnapshot single)) {
                    throw new IllegalArgumentException("Nested composite pool entries are not executable");
                }
                Map<String, SearchParameterDomain> fixed = new TreeMap<>();
                single.source().parameters().values().forEach((name, value) -> fixed.put(name,
                        new SearchParameterDomain(value.type(), List.of(value))));
                var descriptor = strategies.descriptor(single.source().strategyReference().pluginId(),
                        single.source().strategyReference().implementationVersion());
                pool.add(new SearchStrategyPoolEntry(single.source().strategyReference(), fixed,
                        descriptor.parameterSchema().constraints()));
                snapshots.add(new StrategyComponentSnapshot(
                        single.source().strategyReference(), single.source().parameters()));
            }
        }
        CompositeSearchSpace composite = new CompositeSearchSpace(pool, minimum, maximum,
                SearchCombinationPolicy.majorityVote(), validateRequestedConstraints(request, pool));
        StrategyProvenanceSnapshot provenance;
        if (snapshots.size() == 1) {
            var only = snapshots.getFirst();
            provenance = StrategyProvenanceSnapshot.single(only.strategyReference(), only.parameters(),
                    Optional.empty(), fingerprints.single(only.strategyReference(), only.parameters()));
        } else {
            List<StrategyFingerprintCalculator.Component> components = snapshots.stream()
                    .map(value -> new StrategyFingerprintCalculator.Component(
                            value.strategyReference(), value.parameters())).toList();
            provenance = StrategyProvenanceSnapshot.composite(SearchCombinationPolicy.MAJORITY_VOTE,
                    SearchCombinationPolicy.MAJORITY_VOTE_V1, StrategyParameterSet.empty(), snapshots,
                    Optional.empty(), fingerprints.composite(SearchCombinationPolicy.MAJORITY_VOTE,
                            SearchCombinationPolicy.MAJORITY_VOTE_V1,
                            StrategyParameterSet.empty(), components));
        }
        return new ResolvedStrategyInput(new SearchSpace(Map.of()), Optional.of(composite), provenance,
                Map.of("strategyKind", "COMPOSITE_POOL"));
    }

    private static List<String> validateRequestedConstraints(Request request,
            List<SearchStrategyPoolEntry> pool) {
        if (request.constraints() == null || request.constraints().isEmpty()) return List.of();
        List<String> accepted = new ArrayList<>();
        for (ComponentConstraintRequest constraint : request.constraints()) {
            if (!"PARAMETER_LT".equals(constraint.kind())) {
                throw new IllegalArgumentException("Unsupported combination constraint: " + constraint.kind());
            }
            String[] left = constraint.left() == null ? new String[0] : constraint.left().split("\\.", 2);
            String[] right = constraint.right() == null ? new String[0] : constraint.right().split("\\.", 2);
            if (left.length != 2 || right.length != 2 || !left[0].equals(right[0])) {
                throw new IllegalArgumentException("PARAMETER_LT must compare parameters of one Strategy");
            }
            boolean published = pool.stream().anyMatch(entry ->
                    entry.strategy().pluginId().value().equals(left[0])
                            && entry.constraints().stream().anyMatch(rule ->
                                    rule.lowerParameter().equals(left[1])
                                            && rule.upperParameter().equals(right[1])));
            if (!published) {
                throw new IllegalArgumentException("Constraint is not published by the selected Strategy version");
            }
            accepted.add("PARAMETER_LT|" + constraint.left() + '|' + constraint.right());
        }
        return accepted.stream().sorted().distinct().toList();
    }

    private ResolvedStrategyInput resolveSystemStrategy(Request request) {
        SemanticVersion strategyVersion = SemanticVersion.parse(request.strategyVersion());
        var descriptor = strategies.descriptor(request.strategyId(), strategyVersion);
        Map<String, ParameterDefinition> definitions = new TreeMap<>();
        descriptor.parameterSchema().definitions().forEach(value -> definitions.put(value.name(), value));
        Map<String, SearchParameterDomain> domains = resolveDomains(request.parameters(), definitions);
        Map<String, StrategyParameterValue> representative = representative(domains);
        SearchSpace searchSpace = new SearchSpace(domains);
        var frozenParameters = strategies.resolveParameters(
                request.strategyId(), strategyVersion, representative);
        var provenance = StrategyProvenanceSnapshot.single(
                descriptor.reference(), frozenParameters, Optional.empty(),
                fingerprints.single(descriptor.reference(), frozenParameters));
        return new ResolvedStrategyInput(searchSpace, Optional.empty(), provenance, Map.of(
                "strategyKind", "SINGLE",
                "strategyId", request.strategyId().value(),
                "strategyVersion", strategyVersion.toString()));
    }

    private ResolvedStrategyInput resolveUserStrategy(Request request) {
        if (!request.parameters().isEmpty()) {
            throw new IllegalArgumentException(
                    "Published User Strategy Search uses its frozen parameters");
        }
        StrategySnapshot snapshot = userStrategies.resolveSnapshot(
                request.ownerUserId(), new ResolveStrategySnapshotQuery(request.userStrategyVersionId()));
        StrategyProvenanceSnapshot provenance;
        if (snapshot instanceof SingleStrategySnapshot single) {
            provenance = StrategyProvenanceSnapshot.single(
                    single.source().strategyReference(), single.source().parameters(),
                    Optional.of(single.userStrategyVersionId()), single.fingerprint());
        } else {
            CompositeStrategySnapshot composite = (CompositeStrategySnapshot) snapshot;
            provenance = StrategyProvenanceSnapshot.composite(
                    composite.source().policyId(), composite.source().policyVersion(),
                    composite.source().policyParameters(),
                    composite.source().components().stream()
                            .map(component -> new StrategyComponentSnapshot(
                                    component.strategyReference(), component.parameters()))
                            .toList(),
                    Optional.of(composite.userStrategyVersionId()), composite.fingerprint());
        }
        return new ResolvedStrategyInput(new SearchSpace(Map.of()), Optional.empty(), provenance, Map.of(
                "strategyKind", provenance.kind().name(),
                "userStrategyVersionId", request.userStrategyVersionId().value()));
    }

    private static Map<String, SearchParameterDomain> resolveDomains(
            Map<String, ParameterDomain> requested, Map<String, ParameterDefinition> definitions) {
        if (requested == null) throw new IllegalArgumentException("Strategy parameter domains are required");
        Map<String, SearchParameterDomain> domains = new TreeMap<>();
        requested.forEach((name, range) -> {
            ParameterDefinition definition = definitions.get(name);
            if (definition == null) throw new IllegalArgumentException("Unknown Strategy parameter: " + name);
            domains.put(name, domain(definition.type(), range));
        });
        definitions.forEach((name, definition) -> {
            if (domains.containsKey(name)) return;
            StrategyParameterValue value = definition.defaultValue().orElseThrow(() ->
                    new IllegalArgumentException("A Search domain is required for parameter: " + name));
            domains.put(name, new SearchParameterDomain(definition.type(), List.of(value)));
        });
        return Map.copyOf(domains);
    }

    private static Map<String, StrategyParameterValue> representative(
            Map<String, SearchParameterDomain> domains) {
        Map<String, StrategyParameterValue> values = new TreeMap<>();
        domains.forEach((name, domain) -> values.put(name, domain.options().getFirst()));
        return values;
    }

    private static SearchParameterDomain domain(ParameterType type, ParameterDomain request) {
        Objects.requireNonNull(request, "parameter range");
        if (request.options() != null && !request.options().isEmpty()) {
            if (request.kind() != null && !request.kind().equals("CHOICES")) {
                throw new IllegalArgumentException("Parameter domain kind does not match discrete choices");
            }
            return new SearchParameterDomain(type, request.options().stream()
                    .map(value -> parseOption(type, value)).toList());
        }
        if ((type != ParameterType.INTEGER && type != ParameterType.DECIMAL)
                || request.minimum() == null || request.maximum() == null) {
            throw new IllegalArgumentException("Search requires a numeric range or discrete choices");
        }
        String expectedKind = type == ParameterType.DECIMAL ? "DECIMAL_RANGE" : "INTEGER_RANGE";
        if (request.kind() != null && !request.kind().equals(expectedKind)) {
            throw new IllegalArgumentException("Parameter domain kind must be " + expectedKind);
        }
        BigDecimal step = request.step() == null ? BigDecimal.ONE : request.step();
        if (step.signum() <= 0) throw new IllegalArgumentException("Numeric Search domain step must be positive");
        BigDecimal distance = request.maximum().subtract(request.minimum());
        if (distance.signum() < 0) throw new IllegalArgumentException("Numeric Search domain is empty");
        BigDecimal[] division = distance.divideAndRemainder(step);
        if (division[1].signum() != 0) {
            throw new IllegalArgumentException("Numeric Search domain maximum must align with its step");
        }
        BigInteger size = division[0].toBigIntegerExact().add(BigInteger.ONE);
        if (size.signum() < 1 || size.compareTo(BigInteger.valueOf(MAX_INTEGER_DOMAIN_SIZE)) > 0) {
            throw new IllegalArgumentException("Numeric Search domain is empty or too large");
        }
        ArrayList<StrategyParameterValue> values = new ArrayList<>(size.intValueExact());
        for (int offset = 0; offset < size.intValueExact(); offset++) {
            BigDecimal value = request.minimum().add(step.multiply(BigDecimal.valueOf(offset)));
            if (type == ParameterType.INTEGER) {
                try {
                    values.add(new StrategyParameterValue.IntegerValue(value.longValueExact()));
                } catch (ArithmeticException failure) {
                    throw new IllegalArgumentException("Integer Search domain contains a non-integer value", failure);
                }
            } else {
                values.add(new StrategyParameterValue.DecimalValue(value));
            }
        }
        return new SearchParameterDomain(type, values);
    }

    private static StrategyParameterValue parseOption(ParameterType type, String value) {
        Objects.requireNonNull(value, "parameter option");
        try {
            return switch (type) {
                case INTEGER -> new StrategyParameterValue.IntegerValue(Long.parseLong(value));
                case DECIMAL -> new StrategyParameterValue.DecimalValue(new BigDecimal(value));
                case BOOLEAN -> {
                    if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                        throw new IllegalArgumentException("Boolean choice must be true or false");
                    }
                    yield new StrategyParameterValue.BooleanValue(Boolean.parseBoolean(value));
                }
                case TEXT -> new StrategyParameterValue.TextValue(value);
                case ENUM -> new StrategyParameterValue.EnumValue(value);
            };
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("Invalid " + type + " parameter choice", failure);
        }
    }

    private static Map<String, Object> canonicalDomains(SearchSpace searchSpace) {
        Map<String, Object> result = new TreeMap<>();
        searchSpace.parameters().forEach((name, domain) -> result.put(name,
                Map.of("type", domain.type().name(), "options", domain.options().stream().map(StrategyParameterValue::canonicalText).toList())));
        return Map.copyOf(result);
    }

    private static Map<String, Object> canonicalCompositeSpace(CompositeSearchSpace searchSpace) {
        return Map.of(
                "schemaVersion", CompositeSearchSpace.SCHEMA_VERSION,
                "minimumComponents", searchSpace.minimumComponents(),
                "maximumComponents", searchSpace.maximumComponents(),
                "combinationPolicy", Map.of(
                        "policyId", searchSpace.combinationPolicy().policyId().value(),
                        "version", searchSpace.combinationPolicy().version().toString(),
                        "parameters", Map.of()),
                "constraints", searchSpace.constraints(),
                "strategyPool", searchSpace.strategyPool().stream().map(entry -> Map.of(
                        "strategyVersionId", entry.strategy().strategyVersionId().value(),
                        "strategyId", entry.strategy().pluginId().value(),
                        "strategyVersion", entry.strategy().implementationVersion().toString(),
                        "parameterDomains", canonicalDomains(new SearchSpace(entry.parameterDomains())),
                        "constraints", entry.constraints().stream().map(constraint -> Map.of(
                                "kind", "PARAMETER_LT",
                                "lowerParameter", constraint.lowerParameter(),
                                "upperParameter", constraint.upperParameter())).toList()))
                        .toList(),
                "cardinality", searchSpace.combinationCount().toString(),
                "fingerprint", CompositeSearchCanonicalization.searchSpaceFingerprint(searchSpace));
    }

    private record ResolvedStrategyInput(
            SearchSpace searchSpace,
            Optional<CompositeSearchSpace> compositeSearchSpace,
            StrategyProvenanceSnapshot provenance,
            Map<String, Object> identity) {}

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

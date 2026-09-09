package com.cryptostrategy.platform.persistence.internal.search;

import com.cryptostrategy.platform.execution.api.port.out.SearchAllocationContextGateway;
import com.cryptostrategy.platform.search.api.model.SearchParameterDomain;
import com.cryptostrategy.platform.search.api.model.SearchSpace;
import com.cryptostrategy.platform.search.api.model.CompositeSearchSpace;
import com.cryptostrategy.platform.search.api.model.SearchCombinationPolicy;
import com.cryptostrategy.platform.search.api.model.SearchStrategyPoolEntry;
import com.cryptostrategy.platform.strategy.api.model.SemanticVersion;
import com.cryptostrategy.platform.strategy.api.model.StrategyPluginId;
import com.cryptostrategy.platform.strategy.api.model.StrategyReference;
import com.cryptostrategy.platform.strategy.api.model.StrategyVersionId;
import com.cryptostrategy.platform.strategy.api.model.parameter.ParameterType;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterValue;
import com.cryptostrategy.platform.strategy.api.model.parameter.CrossParameterConstraint;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/** Đọc search space đã đóng băng trong Manifest cùng fingerprint/progress durable. */
public final class JdbcSearchAllocationContextGateway implements SearchAllocationContextGateway {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json = new ObjectMapper();

    public JdbcSearchAllocationContextGateway(JdbcTemplate jdbc) {
        this.jdbc = java.util.Objects.requireNonNull(jdbc, "jdbc");
    }

    @Override
    public Optional<Context> load(String experimentId, String searchJobId) {
        List<Context> rows = jdbc.query("""
                select e.owner_user_id, m.search_config,
                    coalesce(array_agg(c.fingerprint order by c.generation_index)
                        filter (where c.candidate_id is not null and
                            coalesce(m.search_config ->> 'contractVersion', 'search-config-v1') <> 'search-config-v2'),
                        '{}') fingerprints,
                    count(c.candidate_id) allocated_work,
                    (select count(*) from experiment.job j where j.experiment_id=e.experiment_id
                        and j.job_type='BACKTEST' and j.status='SUCCEEDED') completed_work,
                    (select count(*) from experiment.job j where j.experiment_id=e.experiment_id
                        and j.job_type='BACKTEST' and j.status in ('FAILED','CANCELLED')) failed_work
                from experiment.experiment e
                join experiment.experiment_manifest m on m.experiment_id=e.experiment_id
                join search.search_run sr on sr.experiment_id=e.experiment_id and sr.search_job_id=?
                left join experiment.candidate_definition c on c.experiment_id=e.experiment_id
                where e.experiment_id=?
                group by e.owner_user_id,m.search_config,e.experiment_id
                """, (rs, row) -> new Context(
                rs.getObject("owner_user_id", UUID.class),
                parseConfig(rs.getString("search_config")).legacy(),
                parseConfig(rs.getString("search_config")).composite(),
                Set.of((String[]) rs.getArray("fingerprints").getArray()),
                rs.getInt("allocated_work"),
                rs.getInt("completed_work"), rs.getInt("failed_work")), searchJobId, experimentId);
        return rows.stream().findFirst();
    }

    private ParsedSpace parseConfig(String searchConfig) {
        try {
            JsonNode config = json.readTree(searchConfig);
            JsonNode space = config.required("searchSpace");
            if ("search-config-v2".equals(config.path("contractVersion").asText())) {
                return new ParsedSpace(new SearchSpace(Map.of()), Optional.of(parseComposite(space)));
            }
            return new ParsedSpace(parseLegacy(space), Optional.empty());
        } catch (Exception failure) {
            throw new IllegalStateException("Frozen Search configuration is invalid", failure);
        }
    }

    private SearchSpace parseLegacy(JsonNode spaces) {
        return new SearchSpace(parseDomains(spaces));
    }

    private CompositeSearchSpace parseComposite(JsonNode space) {
        List<SearchStrategyPoolEntry> pool = new ArrayList<>();
        space.required("strategyPool").forEach(entry -> {
            List<CrossParameterConstraint> constraints = new ArrayList<>();
            entry.path("constraints").forEach(value -> constraints.add(new CrossParameterConstraint(
                    value.required("lowerParameter").asText(), value.required("upperParameter").asText())));
            pool.add(new SearchStrategyPoolEntry(
                    new StrategyReference(
                            new StrategyVersionId(entry.required("strategyVersionId").asText()),
                            new StrategyPluginId(entry.required("strategyId").asText()),
                            SemanticVersion.parse(entry.required("strategyVersion").asText())),
                    parseDomains(entry.required("parameterDomains")), constraints));
        });
        JsonNode policy = space.required("combinationPolicy");
        SearchCombinationPolicy combinationPolicy = new SearchCombinationPolicy(
                new com.cryptostrategy.platform.strategy.api.model.CombinationPolicyId(
                        policy.required("policyId").asText()),
                SemanticVersion.parse(policy.required("version").asText()),
                com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet.empty());
        List<String> constraints = new ArrayList<>();
        space.path("constraints").forEach(value -> constraints.add(value.asText()));
        return new CompositeSearchSpace(pool, space.required("minimumComponents").asInt(),
                space.required("maximumComponents").asInt(), combinationPolicy, constraints);
    }

    private Map<String, SearchParameterDomain> parseDomains(JsonNode spaces) {
        Map<String, SearchParameterDomain> domains = new TreeMap<>();
        spaces.properties().forEach(entry -> {
            ParameterType type = ParameterType.valueOf(entry.getValue().required("type").asText());
            List<StrategyParameterValue> options = new ArrayList<>();
            entry.getValue().required("options").forEach(value -> options.add(parse(type, value.asText())));
            domains.put(entry.getKey(), new SearchParameterDomain(type, options));
        });
        return Map.copyOf(domains);
    }

    private static StrategyParameterValue parse(ParameterType type, String value) {
        return switch (type) {
            case INTEGER -> new StrategyParameterValue.IntegerValue(Long.parseLong(value));
            case DECIMAL -> new StrategyParameterValue.DecimalValue(new BigDecimal(value));
            case BOOLEAN -> new StrategyParameterValue.BooleanValue(Boolean.parseBoolean(value));
            case TEXT -> new StrategyParameterValue.TextValue(value);
            case ENUM -> new StrategyParameterValue.EnumValue(value);
        };
    }

    private record ParsedSpace(SearchSpace legacy, Optional<CompositeSearchSpace> composite) {}
}

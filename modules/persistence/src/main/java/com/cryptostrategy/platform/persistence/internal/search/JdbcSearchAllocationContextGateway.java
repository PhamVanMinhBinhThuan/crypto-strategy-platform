package com.cryptostrategy.platform.persistence.internal.search;

import com.cryptostrategy.platform.execution.api.port.out.SearchAllocationContextGateway;
import com.cryptostrategy.platform.search.api.model.SearchParameterDomain;
import com.cryptostrategy.platform.search.api.model.SearchSpace;
import com.cryptostrategy.platform.strategy.api.model.parameter.ParameterType;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterValue;
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
                        filter (where c.candidate_id is not null), '{}') fingerprints,
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
                parseSpace(rs.getString("search_config")),
                Set.of((String[]) rs.getArray("fingerprints").getArray()),
                rs.getInt("completed_work"), rs.getInt("failed_work")), searchJobId, experimentId);
        return rows.stream().findFirst();
    }

    private SearchSpace parseSpace(String searchConfig) {
        try {
            JsonNode spaces = json.readTree(searchConfig).required("searchSpace");
            Map<String, SearchParameterDomain> domains = new TreeMap<>();
            spaces.properties().forEach(entry -> {
                ParameterType type = ParameterType.valueOf(entry.getValue().required("type").asText());
                List<StrategyParameterValue> options = new ArrayList<>();
                entry.getValue().required("options").forEach(value -> options.add(parse(type, value.asText())));
                domains.put(entry.getKey(), new SearchParameterDomain(type, options));
            });
            return new SearchSpace(domains);
        } catch (Exception failure) {
            throw new IllegalStateException("Frozen Search configuration is invalid", failure);
        }
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
}

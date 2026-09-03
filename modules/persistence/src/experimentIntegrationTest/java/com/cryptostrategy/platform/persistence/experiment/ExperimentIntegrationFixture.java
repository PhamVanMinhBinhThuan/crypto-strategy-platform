package com.cryptostrategy.platform.persistence.experiment;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

final class ExperimentIntegrationFixture {
    private static final String BASE_ASSET = "01J7K8M9N0P1Q2R3S4T5A6V7Y1";
    private static final String QUOTE_ASSET = "01J7K8M9N0P1Q2R3S4T5A6V7Y2";
    private static final String TRADING_PAIR = "01J7K8M9N0P1Q2R3S4T5A6V7Y3";
    private static final String STRATEGY_VERSION = "01J7K8M9N0P1Q2R3S4T5A6V7W2";

    private ExperimentIntegrationFixture() {
    }

    static void seedManifestReferences(JdbcTemplate jdbc, UUID ownerUserId, String datasetVersionId) {
        jdbc.update("insert into auth.users(id) values (?) on conflict do nothing", ownerUserId);
        jdbc.update("insert into market.asset(asset_id,symbol) values (?,'LEGACYBASE') on conflict do nothing",
                BASE_ASSET);
        jdbc.update("insert into market.asset(asset_id,symbol) values (?,'LEGACYQUOTE') on conflict do nothing",
                QUOTE_ASSET);
        jdbc.update("insert into market.trading_pair(trading_pair_id,base_asset_id,quote_asset_id,symbol) "
                        + "values (?,?,?,'LEGACYPAIR') on conflict do nothing",
                TRADING_PAIR, BASE_ASSET, QUOTE_ASSET);
        Instant now = Instant.parse("2026-09-03T00:00:00Z");
        jdbc.update("insert into market.dataset_version(dataset_version_id,version,provider,trading_pair_id,timeframe,"
                        + "normalization_version,range_start,range_end,candle_count,checksum) "
                        + "values (?,?,'fixture',?,'1m','norm-v1',?,?,100,?) on conflict do nothing",
                datasetVersionId, "fixture-" + datasetVersionId, TRADING_PAIR,
                Timestamp.from(now.minusSeconds(3600)), Timestamp.from(now),
                "sha256:" + switch (datasetVersionId.substring(datasetVersionId.length() - 1)) {
                    case "1" -> "a".repeat(64);
                    case "2" -> "b".repeat(64);
                    default -> "c".repeat(64);
                });
        jdbc.update("insert into strategy.strategy_version(strategy_version_id,plugin_id,version,display_name,"
                        + "parameter_schema,default_parameters,supported_signals,fingerprint) "
                        + "values (?,'legacy-momentum','1.0.0','Legacy Momentum','{}','{}','[]',?) on conflict do nothing",
                STRATEGY_VERSION, "strategy-v1:sha256:" + "0".repeat(64));
    }
}

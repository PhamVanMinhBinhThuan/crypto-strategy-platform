package com.cryptostrategy.platform.persistence.internal.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class F015SearchMigrationContractTest {
    private static final String F010 = "20260903000100_f010_search_coordinator.sql";
    private static final String F015 = "20260905000100_f015_composite_search.sql";
    private final Path migrations = Path.of(System.getProperty("user.dir"))
            .resolve("../..").normalize().resolve("supabase/migrations");

    @Test
    void ordersForwardMigrationAfterSearchCoordinatorWithoutRewritingV1Rows() throws Exception {
        try (var files = Files.list(migrations)) {
            var names = files.map(path -> path.getFileName().toString()).sorted().toList();
            assertThat(names).contains(F010, F015);
            assertThat(names.indexOf(F015)).isGreaterThan(names.indexOf(F010));
        }
        String sql = Files.readString(migrations.resolve(F015)).toLowerCase();
        assertThat(sql)
                .contains("generated always as")
                .contains("'search-config-v1'")
                .contains("definition_schema_version")
                .contains("candidate_definition_v2_shape_check")
                .contains("maximum_without_improvement")
                .contains("terminal_reason")
                .contains("'no_improvement'")
                .contains("search_run_refill_idx")
                .doesNotContain("update experiment.candidate_definition")
                .doesNotContain("drop table")
                .doesNotContain("truncate");
    }
}

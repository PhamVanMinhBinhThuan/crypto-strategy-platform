package com.cryptostrategy.platform.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

class NewsArchitectureTest {
    @Test
    void newsModuleDependsOnlyOnItsOwnApiAndStableDomainContracts() {
        var classes = new ClassFileImporter().importPackages("com.cryptostrategy.platform.news");

        noClasses().should().dependOnClassesThat().resideInAnyPackage(
                "com.cryptostrategy.platform.contracts..",
                "org.springframework..",
                "java.sql..",
                "javax.sql..",
                "com.cryptostrategy.platform.persistence..",
                "com.cryptostrategy.platform.marketdata.internal..",
                "com.cryptostrategy.platform.strategy..",
                "com.cryptostrategy.platform.backtesting..",
                "com.cryptostrategy.platform.search..",
                "com.cryptostrategy.platform.evaluation..",
                "com.cryptostrategy.platform.leaderboard..",
                "com.cryptostrategy.platform.api..",
                "com.cryptostrategy.platform.worker..").check(classes);
    }
}

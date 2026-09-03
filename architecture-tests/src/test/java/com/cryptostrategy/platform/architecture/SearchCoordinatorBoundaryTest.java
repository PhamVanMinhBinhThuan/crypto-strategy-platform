package com.cryptostrategy.platform.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class SearchCoordinatorBoundaryTest {
    private static final String ROOT = "com.cryptostrategy.platform";

    @Test
    void searchRemainsPureAndDoesNotDependOnDownstreamCapabilities() {
        var production = productionClasses();

        noClasses()
                .that().resideInAPackage(ROOT + ".search..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..",
                        "java.sql..",
                        "javax.sql..",
                        "org.postgresql..",
                        "io.lettuce..",
                        ROOT + ".contracts..",
                        ROOT + ".experiment..",
                        ROOT + ".backtesting..",
                        ROOT + ".evaluation..",
                        ROOT + ".leaderboard..",
                        ROOT + ".execution..",
                        ROOT + ".persistence..",
                        ROOT + ".worker..")
                .allowEmptyShould(true)
                .check(production);
    }

    @Test
    void workerDoesNotExecuteSqlDirectly() {
        var production = productionClasses();

        noClasses()
                .that().resideInAPackage(ROOT + ".worker..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework.jdbc.core..",
                        "org.jooq..",
                        "jakarta.persistence..",
                        "org.hibernate..")
                .check(production);

        noClasses()
                .that().resideInAPackage(ROOT + ".worker..")
                .should().dependOnClassesThat().haveFullyQualifiedName("java.sql.Statement")
                .orShould().dependOnClassesThat().haveFullyQualifiedName("java.sql.PreparedStatement")
                .orShould().dependOnClassesThat().haveFullyQualifiedName("java.sql.CallableStatement")
                .check(production);
    }

    private static com.tngtech.archunit.core.domain.JavaClasses productionClasses() {
        return new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(ROOT);
    }
}

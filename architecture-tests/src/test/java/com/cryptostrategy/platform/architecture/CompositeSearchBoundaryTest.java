package com.cryptostrategy.platform.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

/** F-015 keeps composite-space meaning in Search and all durable SQL in Persistence. */
class CompositeSearchBoundaryTest {
    private static final String ROOT = "com.cryptostrategy.platform";

    @Test
    void compositeSearchModelDoesNotDependOnExecutionOrAdapters() {
        var production = productionClasses();
        noClasses()
                .that().resideInAPackage(ROOT + ".search..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        ROOT + ".execution..", ROOT + ".worker..", ROOT + ".persistence..",
                        "org.springframework..", "java.sql..", "javax.sql..")
                .allowEmptyShould(true)
                .check(production);
    }

    @Test
    void workerCoordinatesThroughPortsWithoutDirectSql() {
        var production = productionClasses();
        noClasses()
                .that().resideInAPackage(ROOT + ".worker..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework.jdbc.core..",
                        "org.jooq..", "jakarta.persistence..", "org.hibernate..")
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

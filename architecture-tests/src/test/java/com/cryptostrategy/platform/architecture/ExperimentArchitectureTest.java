package com.cryptostrategy.platform.architecture;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ExperimentArchitectureTest {

    @Test
    void experimentApiIsTechnologyPure() {
        var classes = new ClassFileImporter().importPackages("com.cryptostrategy.platform.experiment.api");
        noClasses().should().dependOnClassesThat().resideInAnyPackage(
                "org.springframework..",
                "java.sql..",
                "javax.sql..",
                "com.cryptostrategy.platform.persistence..",
                "com.cryptostrategy.platform.marketdata.internal..",
                "com.cryptostrategy.platform.strategy.internal.."
        ).check(classes);
    }

    @Test
    void persistenceDoesNotLeakInternalExperimentClasses() {
        var classes = new ClassFileImporter().importPackages("com.cryptostrategy.platform.persistence");
        noClasses().should().dependOnClassesThat().resideInAPackage(
                "com.cryptostrategy.platform.experiment.internal.."
        ).check(classes);
    }
}

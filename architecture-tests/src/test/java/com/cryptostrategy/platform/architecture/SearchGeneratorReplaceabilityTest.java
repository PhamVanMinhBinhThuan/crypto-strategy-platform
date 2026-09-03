package com.cryptostrategy.platform.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

/** Chứng minh thay generator không lan thay đổi vào pipeline nghiệp vụ phía sau. */
class SearchGeneratorReplaceabilityTest {
    private static final String ROOT = "com.cryptostrategy.platform";

    @Test
    void downstreamCapabilitiesDoNotKnowGeneratorOrSearchInternals() {
        var production = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(ROOT);

        noClasses()
                .that().resideInAnyPackage(
                        ROOT + ".backtesting..",
                        ROOT + ".evaluation..",
                        ROOT + ".leaderboard..",
                        ROOT + ".experiment..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        ROOT + ".search.internal..",
                        ROOT + ".search.api.port.in..")
                .allowEmptyShould(true)
                .check(production);
    }

    @Test
    void publicMessageContractsRemainIndependentFromGeneratorTypes() {
        var production = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(ROOT + ".contracts");

        noClasses()
                .that().resideInAPackage(ROOT + ".contracts..")
                .should().dependOnClassesThat().resideInAnyPackage(ROOT + ".search..")
                .check(production);
    }
}

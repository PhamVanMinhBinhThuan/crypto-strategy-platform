package com.cryptostrategy.platform.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.Test;

class PublicApiBoundaryTest {
    private static final String PLATFORM = "com.cryptostrategy.platform";
    private static final String PUBLIC_API = PLATFORM + ".api..";
    private static final String PERSISTENCE = PLATFORM + ".persistence";
    private static final String PERSISTENCE_API = PERSISTENCE + ".api";

    @Test
    void publicApiUsesOnlyPublishedModuleAndPersistenceBoundaries() {
        JavaClasses production = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(PLATFORM);

        var result = publishedBoundaryRule(PUBLIC_API).evaluate(production);

        assertFalse(result.hasViolation(), result.getFailureReport().toString());
    }

    @Test
    void internalPersistenceAndProviderImplementationFixturesAreRejected() {
        String fixtureApi = PLATFORM + ".architecture.fixtures.publicapi.api..";
        JavaClasses fixtures = new ClassFileImporter()
                .importPackages(PLATFORM + ".architecture.fixtures.publicapi.api");

        var result = publishedBoundaryRule(fixtureApi).evaluate(fixtures);
        String report = result.getFailureReport().toString();

        assertTrue(result.hasViolation());
        assertTrue(report.contains("InternalApplicationService"), report);
        assertTrue(report.contains("JdbcPersistenceImplementation"), report);
        assertTrue(report.contains("ProviderImplementation"), report);
    }

    private static ArchRule publishedBoundaryRule(String sourcePackage) {
        return classes()
                .that().resideInAPackage(sourcePackage)
                .should(new ArchCondition<>(
                        "depend only on published APIs, ports, and persistence factories") {
                    @Override
                    public void check(JavaClass source, ConditionEvents events) {
                        for (Dependency dependency : source.getDirectDependenciesFromSelf()) {
                            JavaClass target = dependency.getTargetClass();
                            if (isImplementationPackage(target.getPackageName())) {
                                events.add(SimpleConditionEvent.violated(
                                        source,
                                        source.getName() + " must not depend on implementation "
                                                + target.getName()));
                            }
                        }
                    }
                });
    }

    private static boolean isImplementationPackage(String packageName) {
        if (!packageName.startsWith(PLATFORM + ".")) {
            return false;
        }
        if (packageName.equals(PERSISTENCE)
                || packageName.startsWith(PERSISTENCE + ".")) {
            return !(packageName.equals(PERSISTENCE_API)
                    || packageName.startsWith(PERSISTENCE_API + "."));
        }
        return hasPackageSegment(packageName, "internal")
                || hasPackageSegment(packageName, "provider");
    }

    private static boolean hasPackageSegment(String packageName, String segment) {
        return packageName.equals(segment)
                || packageName.startsWith(segment + ".")
                || packageName.endsWith("." + segment)
                || packageName.contains("." + segment + ".");
    }
}

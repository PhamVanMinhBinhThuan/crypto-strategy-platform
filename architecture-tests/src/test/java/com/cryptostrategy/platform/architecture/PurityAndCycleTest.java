package com.cryptostrategy.platform.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.cryptostrategy.platform.domain.api.identity.UlidIdentifier;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PurityAndCycleTest {
    private static final String PLATFORM = "com.cryptostrategy.platform";
    private static final Set<String> BINARY_DECIMALS = Set.of("double", "float", Double.class.getName(), Float.class.getName());

    @Test
    void pureProductionModulesDoNotDependOnFrameworkProviderOrPersistenceTechnology() {
        JavaClasses production = productionClasses();

        assertFalse(forbiddenTechnologyDependencies(PLATFORM).evaluate(production).hasViolation());
    }

    @Test
    void databaseTechnologyFixtureIsRejected() {
        String fixtureRoot = PLATFORM + ".architecture.fixtures.technology";
        JavaClasses fixtures = new ClassFileImporter().importPackages(fixtureRoot);

        assertTrue(forbiddenTechnologyDependencies(fixtureRoot).evaluate(fixtures).hasViolation());
    }

    @Test
    void productionCapabilityGraphIsFreeOfCycles() {
        JavaClasses production = productionClasses();

        assertFalse(slices().matching(PLATFORM + ".(*)..").should().beFreeOfCycles().evaluate(production).hasViolation());
    }

    @Test
    void cycleFixtureIsRejected() {
        String fixtureRoot = PLATFORM + ".architecture.fixtures.cycle";
        JavaClasses fixtures = new ClassFileImporter().importPackages(fixtureRoot);

        assertTrue(slices().matching(fixtureRoot + ".(*)..").should().beFreeOfCycles().evaluate(fixtures).hasViolation());
    }

    @Test
    void productionPublicBoundariesUseCanonicalValues() {
        JavaClasses production = productionClasses();

        var result = productionCanonicalBoundaryValues().evaluate(production);
        assertFalse(result.hasViolation(), result.getFailureReport().toString());
    }

    @Test
    void nonCanonicalBoundaryFixtureIsRejected() {
        JavaClasses fixtures = new ClassFileImporter()
                .importPackages(PLATFORM + ".architecture.fixtures.canonical");

        assertTrue(canonicalBoundaryValues().evaluate(fixtures).hasViolation());
    }

    @Test
    void capabilitySpecificTypedUlidFixtureIsAccepted() {
        JavaClasses fixtures = new ClassFileImporter()
                .importPackages(PLATFORM + ".architecture.fixtures.typedid");

        assertFalse(canonicalBoundaryValues().evaluate(fixtures).hasViolation());
    }

    private static ArchRule forbiddenTechnologyDependencies(String root) {
        return noClasses()
                .that().resideInAnyPackage(
                        root + ".domain..",
                        root + ".strategy..",
                        root + ".strategies..",
                        root + ".evaluation..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..",
                        "java.sql..",
                        "javax.sql..",
                        "jakarta.persistence..",
                        "org.hibernate..",
                        "org.postgresql..",
                        "com.binance..",
                        PLATFORM + ".persistence..")
                .allowEmptyShould(true);
    }

    private static ArchRule canonicalBoundaryValues() {
        return classes()
                .that().resideInAnyPackage("..domain..", "..api..", "..port.in..", "..port.out..", "..event..")
                .should(canonicalValuesCondition());
    }

    private static ArchRule productionCanonicalBoundaryValues() {
        return classes()
                .that().resideInAnyPackage("..domain..", "..api..", "..port.in..", "..port.out..", "..event..")
                .and().resideOutsideOfPackage(PLATFORM + ".architecture..")
                .should(canonicalValuesCondition());
    }

    private static ArchCondition<JavaClass> canonicalValuesCondition() {
        return new ArchCondition<>("use UUID user identities, typed ULID business identities, exact decimals and UTC instants") {
                    @Override
                    public void check(JavaClass javaClass, ConditionEvents events) {
                        for (JavaField field : javaClass.getFields()) {
                            checkType(javaClass, field.getName(), field.getRawType(), events);
                            if (looksLikeIdentity(field.getName())
                                    && !field.getRawType().isEquivalentTo(Optional.class)
                                    && !isAllowedIdentity(field.getName(), field.getRawType())) {
                                violation(javaClass, field.getFullName()
                                        + " must use UUID for user identity or a typed domain ULID", events);
                            }
                        }
                        for (JavaMethod method : javaClass.getMethods()) {
                            checkType(javaClass, method.getFullName() + " return type", method.getRawReturnType(), events);
                        }
                        for (JavaCodeUnit codeUnit : javaClass.getCodeUnits()) {
                            for (JavaClass parameterType : codeUnit.getRawParameterTypes()) {
                                checkType(javaClass, codeUnit.getFullName() + " parameter", parameterType, events);
                            }
                        }
                    }
                };
    }

    private static void checkType(JavaClass owner, String location, JavaClass type, ConditionEvents events) {
        if (BINARY_DECIMALS.contains(type.getName())) {
            violation(owner, location + " must use an exact decimal type", events);
        }
        if (type.isEquivalentTo(LocalDateTime.class)) {
            violation(owner, location + " must use a UTC instant", events);
        }
    }

    private static boolean looksLikeIdentity(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        if (normalized.equals("correlationid")
                || normalized.equals("workerid")
                || normalized.equals("aggregateid")
                || normalized.equals("messageid")
                || normalized.equals("outboxeventid")
                || normalized.equals("serialversionuid")
                || normalized.equals("valid")) {
            return false;
        }
        return normalized.equals("id") || normalized.endsWith("id") || normalized.endsWith("identity");
    }

    private static boolean isAllowedIdentity(String name, JavaClass type) {
        String normalized = name.toLowerCase(Locale.ROOT);
        if (normalized.endsWith("userid") || normalized.equals("ownerid")) {
            return type.isEquivalentTo(UUID.class);
        }
        return type.isEquivalentTo(UUID.class)
                || type.isAssignableTo(UlidIdentifier.class)
                || isTypedSlugIdentity(type);
    }

    /**
     * Slug-based domain identities such as {@code StrategyPluginId} and
     * {@code CombinationPolicyId} are intentionally modeled as typed records
     * wrapping a validated String slug (e.g. "ma-crossover", "majority-vote").
     * They are not ULIDs or UUIDs but are still properly typed domain identities,
     * not raw Strings. Recognized when the type is a record in the platform's
     * api.model package hierarchy.
     */
    private static boolean isTypedSlugIdentity(JavaClass type) {
        return type.getName().startsWith(PLATFORM + ".")
                && (type.getName().contains(".api.model.") || type.getName().contains(".api."))
                && type.isRecord()
                && type.getName().endsWith("Id");
    }

    private static void violation(JavaClass owner, String message, ConditionEvents events) {
        events.add(SimpleConditionEvent.violated(owner, message));
    }

    private static JavaClasses productionClasses() {
        return new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(PLATFORM);
    }
}

package com.cryptostrategy.platform.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class ModuleBoundaryTest {
    private static final String PLATFORM = "com.cryptostrategy.platform";
    private static final Pattern PROJECT_DEPENDENCY = Pattern.compile("project\\(\"([^\"]+)\"\\)");
    private static final Map<String, String> PROJECT_OWNERS = projectOwners();
    private static final Map<String, Set<String>> ALLOWED = Map.ofEntries(
            Map.entry("domain", Set.of()),
            Map.entry("contracts", Set.of("domain")),
            Map.entry("marketdata", Set.of("domain")),
            Map.entry("strategy", Set.of("domain")),
            Map.entry("strategies", Set.of("domain", "strategy")),
            Map.entry("combination", Set.of("domain", "strategy")),
            Map.entry("backtesting", Set.of("domain", "marketdata", "strategy", "combination", "experiment")),
            Map.entry("evaluation", Set.of("domain", "backtesting", "experiment")),
            Map.entry("experiment", Set.of("domain", "marketdata", "strategy")),
            Map.entry("search", Set.of("domain", "strategy")),
            Map.entry("leaderboard", Set.of("domain", "evaluation", "experiment")),
            Map.entry("news", Set.of("domain")),
            Map.entry("execution", Set.of("domain", "backtesting", "evaluation", "leaderboard", "experiment", "strategy", "combination")),
            Map.entry("persistence", Set.of(
                    "domain", "marketdata", "strategy", "backtesting", "evaluation", "experiment", "execution", "search", "leaderboard", "news")),
            Map.entry("api", Set.of(
                    "domain", "contracts", "marketdata", "strategy", "strategies", "combination", "backtesting",
                    "evaluation", "experiment", "execution", "search", "leaderboard", "news", "persistence")),
            Map.entry("worker", Set.of(
                    "domain", "contracts", "marketdata", "strategy", "strategies", "combination", "backtesting",
                    "evaluation", "experiment", "execution", "search", "leaderboard", "news", "persistence")));

    @Test
    void productionPackagesRespectTheAllowedDependencyMatrix() {
        JavaClasses production = productionClasses();

        assertFalse(allowedModuleDependencies(PLATFORM).evaluate(production).hasViolation());
    }

    @Test
    void declaredGradleProjectDependenciesRespectTheAllowedDependencyMatrix() throws IOException {
        Path repositoryRoot = Path.of(System.getProperty("repository.root"));

        for (Map.Entry<String, String> project : PROJECT_OWNERS.entrySet()) {
            Path buildFile = repositoryRoot.resolve(project.getKey()).resolve("build.gradle.kts");
            Matcher dependencies = PROJECT_DEPENDENCY.matcher(Files.readString(buildFile));
            while (dependencies.find()) {
                String targetProject = dependencies.group(1).replaceFirst("^:", "").replace(':', '/');
                String targetOwner = PROJECT_OWNERS.get(targetProject);
                assertTrue(targetOwner != null && ALLOWED.get(project.getValue()).contains(targetOwner),
                        () -> project.getKey() + " must not declare dependency on " + dependencies.group(1));
            }
        }
    }

    @Test
    void allowedDomainDependencyFixturePasses() {
        String fixtureRoot = PLATFORM + ".architecture.fixtures.allowed";
        JavaClasses fixtures = new ClassFileImporter().importPackages(fixtureRoot);
        Map<String, Set<String>> fixtureMatrix = Map.of(
                "domain", Set.of(),
                "marketdata", Set.of("domain"));

        assertFalse(allowedModuleDependencies(fixtureRoot, fixtureMatrix).evaluate(fixtures).hasViolation());
    }

    @Test
    void dependencyOnAnotherModulesInternalPackageIsRejected() {
        String fixtureRoot = PLATFORM + ".architecture.fixtures.internal";
        JavaClasses fixtures = new ClassFileImporter().importPackages(fixtureRoot);

        assertTrue(noExternalInternalAccess(fixtureRoot, "domain").evaluate(fixtures).hasViolation());
    }

    @Test
    void productionCodeCannotAccessAnotherModulesInternalPackage() {
        JavaClasses production = productionClasses();

        for (String owner : ALLOWED.keySet()) {
            assertFalse(noExternalInternalAccess(PLATFORM, owner).evaluate(production).hasViolation());
        }
    }

    private static ArchRule noExternalInternalAccess(String root, String owner) {
        return noClasses()
                .that().resideOutsideOfPackage(root + "." + owner + "..")
                .should().dependOnClassesThat().resideInAPackage(root + "." + owner + ".internal..");
    }

    private static ArchRule allowedModuleDependencies(String root) {
        return allowedModuleDependencies(root, ALLOWED);
    }

    private static ArchRule allowedModuleDependencies(String root, Map<String, Set<String>> matrix) {
        return com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes()
                .that().resideInAPackage(root + "..")
                .should(new ArchCondition<>("only depend on modules allowed by the dependency matrix") {
                    @Override
                    public void check(JavaClass source, ConditionEvents events) {
                        String sourceOwner = ownerOf(source.getPackageName(), root, matrix.keySet());
                        if (sourceOwner == null) {
                            return;
                        }
                        for (Dependency dependency : source.getDirectDependenciesFromSelf()) {
                            String targetOwner = ownerOf(dependency.getTargetClass().getPackageName(), root, matrix.keySet());
                            if (targetOwner != null
                                    && !targetOwner.equals(sourceOwner)
                                    && !matrix.get(sourceOwner).contains(targetOwner)) {
                                events.add(SimpleConditionEvent.violated(source,
                                        sourceOwner + " must not depend on " + targetOwner + ": " + dependency.getDescription()));
                            }
                        }
                    }
                });
    }

    private static String ownerOf(String packageName, String root, Set<String> owners) {
        for (String owner : owners) {
            if (packageName.equals(root + "." + owner) || packageName.startsWith(root + "." + owner + ".")) {
                return owner;
            }
        }
        return null;
    }

    private static JavaClasses productionClasses() {
        return new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(PLATFORM);
    }

    private static Map<String, String> projectOwners() {
        Map<String, String> projects = new LinkedHashMap<>();
        projects.put("modules/domain", "domain");
        projects.put("modules/contracts", "contracts");
        projects.put("modules/market-data", "marketdata");
        projects.put("modules/strategy-core", "strategy");
        projects.put("modules/strategies", "strategies");
        projects.put("modules/combination", "combination");
        projects.put("modules/backtesting", "backtesting");
        projects.put("modules/evaluation", "evaluation");
        projects.put("modules/experiment", "experiment");
        projects.put("modules/search", "search");
        projects.put("modules/leaderboard", "leaderboard");
        projects.put("modules/news", "news");
        projects.put("modules/experiment-execution", "execution");
        projects.put("modules/persistence", "persistence");
        projects.put("apps/api", "api");
        projects.put("apps/worker", "worker");
        return Map.copyOf(projects);
    }
}

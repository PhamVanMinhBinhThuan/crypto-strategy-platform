package com.cryptostrategy.platform.architecture;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class BuildStructureTest {
    private static final List<String> EXPECTED_PROJECTS = List.of(
            ":apps:api",
            ":apps:worker",
            ":modules:domain",
            ":modules:contracts",
            ":modules:market-data",
            ":modules:strategy-core",
            ":modules:strategies",
            ":modules:combination",
            ":modules:backtesting",
            ":modules:evaluation",
            ":modules:experiment",
            ":modules:search",
            ":modules:leaderboard",
            ":modules:news",
            ":modules:persistence",
            ":architecture-tests");

    @Test
    void declaresEveryFoundationProjectAndBuildScript() throws IOException {
        Path repositoryRoot = Path.of(System.getProperty("repository.root"));
        String settings = Files.readString(repositoryRoot.resolve("settings.gradle.kts"));

        for (String projectPath : EXPECTED_PROJECTS) {
            assertTrue(settings.contains("\"" + projectPath + "\""), () -> "Missing project " + projectPath);
            Path buildFile = repositoryRoot.resolve(projectPath.substring(1).replace(':', '/'))
                    .resolve("build.gradle.kts");
            assertTrue(Files.isRegularFile(buildFile), () -> "Missing build file " + buildFile);
        }
    }

    @Test
    void rootCheckAggregatesSubprojectsAndBuildLogic() throws IOException {
        Path repositoryRoot = Path.of(System.getProperty("repository.root"));
        String rootBuild = Files.readString(repositoryRoot.resolve("build.gradle.kts"));

        assertTrue(rootBuild.contains("tasks.named(\"check\")"));
        assertTrue(rootBuild.contains("tasks.named(\"clean\")"));
        assertTrue(rootBuild.contains("subprojects.filter { it.buildFile.isFile }.map"));
        assertTrue(rootBuild.contains("includedBuild(\"build-logic\").task(\":check\")"));
    }
}

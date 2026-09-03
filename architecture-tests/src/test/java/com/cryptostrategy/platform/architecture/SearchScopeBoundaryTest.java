package com.cryptostrategy.platform.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Khóa phạm vi an toàn của F-010 tại source/build boundary. */
class SearchScopeBoundaryTest {
    private static final List<String> FORBIDDEN_ENDPOINTS = List.of(
            "\"/wallet", "\"/wallets", "\"/orders", "\"/trading", "\"/trade", "\"/advice");
    private static final List<String> FORBIDDEN_DEPENDENCIES = List.of(
            "alpaca", "interactivebrokers", "interactive-brokers", "coinbase-advanced", "binance-connector");

    @Test
    void f010DoesNotExposeLiveTradingWalletOrFinancialAdviceEndpoints() throws IOException {
        String controllers = readJavaSources(repositoryRoot().resolve("apps/api/src/main/java"));
        FORBIDDEN_ENDPOINTS.forEach(fragment ->
                assertFalse(controllers.contains(fragment), "Endpoint ngoài phạm vi: " + fragment));
    }

    @Test
    void f010DoesNotIntroduceBrokerWalletOrTradingSdkDependencies() throws IOException {
        String buildDefinitions;
        try (Stream<Path> files = Files.walk(repositoryRoot())) {
            buildDefinitions = files
                    .filter(path -> path.getFileName().toString().equals("build.gradle.kts"))
                    .map(SearchScopeBoundaryTest::readUnchecked)
                    .reduce("", (left, right) -> left + "\n" + right)
                    .toLowerCase(Locale.ROOT);
        }
        FORBIDDEN_DEPENDENCIES.forEach(fragment ->
                assertFalse(buildDefinitions.contains(fragment), "Dependency ngoài phạm vi: " + fragment));
    }

    private static Path repositoryRoot() {
        return Path.of(System.getProperty("repository.root")).toAbsolutePath().normalize();
    }

    private static String readJavaSources(Path sourceRoot) throws IOException {
        try (Stream<Path> files = Files.walk(sourceRoot)) {
            return files.filter(path -> path.toString().endsWith(".java"))
                    .map(SearchScopeBoundaryTest::readUnchecked)
                    .reduce("", (left, right) -> left + "\n" + right)
                    .toLowerCase(Locale.ROOT);
        }
    }

    private static String readUnchecked(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể đọc source để kiểm tra phạm vi", exception);
        }
    }
}

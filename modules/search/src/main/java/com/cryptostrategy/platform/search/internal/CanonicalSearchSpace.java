package com.cryptostrategy.platform.search.internal;

import com.cryptostrategy.platform.search.api.model.SearchParameterDomain;
import com.cryptostrategy.platform.search.api.model.SearchSpace;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterValue;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;

/** Biểu diễn chuẩn có version cho search space và candidate fingerprint. */
public final class CanonicalSearchSpace {
    private static final String SPACE_VERSION = "search-space-v1";
    private static final String CANDIDATE_VERSION = "search-candidate-v1";

    private CanonicalSearchSpace() {}

    public static String fingerprint(SearchSpace searchSpace) {
        return fingerprintOf(encode(searchSpace));
    }

    public static String candidateFingerprint(StrategyParameterSet parameters) {
        Objects.requireNonNull(parameters, "parameters");
        StringBuilder canonical = new StringBuilder(CANDIDATE_VERSION).append('\n');
        parameters.values().forEach((name, value) -> appendValue(canonical, name, value));
        return fingerprintOf(canonical.toString());
    }

    public static boolean contains(SearchSpace searchSpace, StrategyParameterSet candidate) {
        Objects.requireNonNull(searchSpace, "searchSpace");
        Objects.requireNonNull(candidate, "candidate");
        if (!searchSpace.parameters().keySet().equals(candidate.values().keySet())) {
            return false;
        }
        for (Map.Entry<String, SearchParameterDomain> entry : searchSpace.parameters().entrySet()) {
            StrategyParameterValue actual = candidate.values().get(entry.getKey());
            if (actual == null || actual.type() != entry.getValue().type()) {
                return false;
            }
            boolean allowed = entry.getValue().options().stream()
                    .anyMatch(option -> option.canonicalText().equals(actual.canonicalText()));
            if (!allowed) {
                return false;
            }
        }
        return true;
    }

    public static String encode(SearchSpace searchSpace) {
        Objects.requireNonNull(searchSpace, "searchSpace");
        StringBuilder canonical = new StringBuilder(SPACE_VERSION).append('\n');
        searchSpace.parameters().forEach((name, domain) -> {
            appendToken(canonical, name);
            appendToken(canonical, domain.type().name());
            canonical.append(domain.options().size()).append('\n');
            domain.options().forEach(option -> appendToken(canonical, option.canonicalText()));
        });
        return canonical.toString();
    }

    private static void appendValue(
            StringBuilder canonical,
            String name,
            StrategyParameterValue value) {
        appendToken(canonical, name);
        appendToken(canonical, value.type().name());
        appendToken(canonical, value.canonicalText());
    }

    private static void appendToken(StringBuilder target, String value) {
        byte[] utf8 = value.getBytes(StandardCharsets.UTF_8);
        target.append(utf8.length).append(':').append(value).append('\n');
    }

    private static String fingerprintOf(String canonical) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return "sha256:" + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is required by the JVM", impossible);
        }
    }
}

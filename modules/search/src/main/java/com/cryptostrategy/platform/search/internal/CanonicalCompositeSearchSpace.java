package com.cryptostrategy.platform.search.internal;

import com.cryptostrategy.platform.search.api.model.CompositeCandidateComponent;
import com.cryptostrategy.platform.search.api.model.CompositeGeneratedCandidate;
import com.cryptostrategy.platform.search.api.model.CompositeSearchSpace;
import com.cryptostrategy.platform.search.api.model.SearchParameterDomain;
import com.cryptostrategy.platform.search.api.model.SearchCombinationPolicy;
import com.cryptostrategy.platform.search.api.model.SearchStrategyPoolEntry;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterValue;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/** Canonical v2 encoding and deterministic bounded-memory Random traversal. */
public final class CanonicalCompositeSearchSpace {
    private static final String SPACE_VERSION = "composite-search-space-v2";
    private static final String CANDIDATE_VERSION = "composite-search-candidate-v2";

    private CanonicalCompositeSearchSpace() {}

    public static String fingerprint(CompositeSearchSpace space) {
        StringBuilder value = new StringBuilder(SPACE_VERSION).append('\n');
        value.append(space.minimumComponents()).append(':').append(space.maximumComponents()).append('\n');
        append(value, space.combinationPolicy().policyId().value());
        append(value, space.combinationPolicy().version().toString());
        space.constraints().forEach(constraint -> append(value, constraint));
        for (SearchStrategyPoolEntry entry : space.strategyPool()) {
            appendStrategy(value, entry);
        }
        return hash(value.toString());
    }

    public static Optional<CompositeGeneratedCandidate> generate(
            CompositeSearchSpace space, long seed, int generationIndex) {
        if (generationIndex < 0) throw new IllegalArgumentException("generationIndex must be non-negative");
        BigInteger count = space.combinationCount();
        if (BigInteger.valueOf(generationIndex).compareTo(count) >= 0) return Optional.empty();
        BigInteger ordinal = permutedOrdinal(seed, BigInteger.valueOf(generationIndex), count);
        List<CompositeCandidateComponent> components = candidateAt(space, ordinal);
        String fingerprint = candidateFingerprint(components, space);
        return Optional.of(new CompositeGeneratedCandidate(
                components, space.combinationPolicy(), generationIndex, fingerprint));
    }

    static List<CompositeCandidateComponent> candidateAt(CompositeSearchSpace space, BigInteger ordinal) {
        if (ordinal.signum() < 0 || ordinal.compareTo(space.combinationCount()) >= 0) {
            throw new IllegalArgumentException("Candidate ordinal is outside the Search space");
        }
        for (int size = space.minimumComponents(); size <= space.maximumComponents(); size++) {
            BigInteger sizeBlock = countWeightedSubsets(space.strategyPool(), 0, size, BigInteger.ONE);
            if (ordinal.compareTo(sizeBlock) >= 0) {
                ordinal = ordinal.subtract(sizeBlock);
                continue;
            }
            int[] subset = resolveSubset(space.strategyPool(), size, ordinal);
            BigInteger preceding = precedingSubsetWeight(space.strategyPool(), subset);
            return decode(space, subset, ordinal.subtract(preceding));
        }
        throw new IllegalStateException("Candidate ordinal could not be decoded");
    }

    public static String candidateFingerprint(
            List<CompositeCandidateComponent> components, CompositeSearchSpace space) {
        return candidateFingerprint(components, space.combinationPolicy());
    }

    public static String candidateFingerprint(
            List<CompositeCandidateComponent> components, SearchCombinationPolicy policy) {
        StringBuilder value = new StringBuilder(CANDIDATE_VERSION).append('\n');
        append(value, policy.policyId().value());
        append(value, policy.version().toString());
        policy.parameters().values().forEach((name, parameter) -> {
            append(value, name);
            append(value, parameter.type().name());
            append(value, parameter.canonicalText());
        });
        components.stream().sorted().forEach(component -> {
            append(value, component.strategy().pluginId().value());
            append(value, component.strategy().implementationVersion().toString());
            append(value, component.strategy().strategyVersionId().value());
            component.parameters().values().forEach((name, parameter) -> {
                append(value, name);
                append(value, parameter.type().name());
                append(value, parameter.canonicalText());
            });
        });
        return hash(value.toString());
    }

    private static List<CompositeCandidateComponent> decode(
            CompositeSearchSpace space, int[] subset, BigInteger ordinal) {
        ArrayList<CompositeCandidateComponent> result = new ArrayList<>();
        for (int index : subset) {
            SearchStrategyPoolEntry entry = space.strategyPool().get(index);
            BigInteger[] quotientRemainder = ordinal.divideAndRemainder(entry.combinationCount());
            ordinal = quotientRemainder[0];
            result.add(new CompositeCandidateComponent(
                    entry.strategy(), entry.parametersAt(quotientRemainder[1])));
        }
        return List.copyOf(result);
    }

    private static BigInteger permutedOrdinal(long seed, BigInteger index, BigInteger count) {
        if (BigInteger.ONE.equals(count)) return BigInteger.ZERO;
        BigInteger multiplier = hashNumber("multiplier:" + Long.toUnsignedString(seed)).mod(count);
        if (multiplier.signum() == 0) multiplier = BigInteger.ONE;
        while (!multiplier.gcd(count).equals(BigInteger.ONE)) multiplier = multiplier.add(BigInteger.ONE).mod(count);
        BigInteger offset = hashNumber("offset:" + Long.toUnsignedString(seed)).mod(count);
        return multiplier.multiply(index).add(offset).mod(count);
    }

    private static BigInteger hashNumber(String value) {
        try {
            return new BigInteger(1, MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is required by the JVM", impossible);
        }
    }

    private static int[] resolveSubset(
            List<SearchStrategyPoolEntry> pool, int size, BigInteger ordinal) {
        int[] selected = new int[size];
        int start = 0;
        BigInteger prefix = BigInteger.ONE;
        for (int depth = 0; depth < size; depth++) {
            int remaining = size - depth - 1;
            for (int index = start; index <= pool.size() - (remaining + 1); index++) {
                BigInteger nextPrefix = prefix.multiply(pool.get(index).combinationCount());
                BigInteger block = countWeightedSubsets(pool, index + 1, remaining, nextPrefix);
                if (ordinal.compareTo(block) >= 0) {
                    ordinal = ordinal.subtract(block);
                    continue;
                }
                selected[depth] = index;
                start = index + 1;
                prefix = nextPrefix;
                break;
            }
        }
        return selected;
    }

    private static BigInteger precedingSubsetWeight(
            List<SearchStrategyPoolEntry> pool, int[] selected) {
        BigInteger preceding = BigInteger.ZERO;
        BigInteger prefix = BigInteger.ONE;
        int start = 0;
        for (int depth = 0; depth < selected.length; depth++) {
            int remaining = selected.length - depth - 1;
            for (int index = start; index < selected[depth]; index++) {
                BigInteger alternative = prefix.multiply(pool.get(index).combinationCount());
                preceding = preceding.add(countWeightedSubsets(pool, index + 1, remaining, alternative));
            }
            prefix = prefix.multiply(pool.get(selected[depth]).combinationCount());
            start = selected[depth] + 1;
        }
        return preceding;
    }

    private static BigInteger countWeightedSubsets(
            List<SearchStrategyPoolEntry> pool, int start, int remaining, BigInteger product) {
        if (remaining == 0) return product;
        BigInteger total = BigInteger.ZERO;
        for (int index = start; index <= pool.size() - remaining; index++) {
            total = total.add(countWeightedSubsets(pool, index + 1, remaining - 1,
                    product.multiply(pool.get(index).combinationCount())));
        }
        return total;
    }

    private static void appendStrategy(StringBuilder value, SearchStrategyPoolEntry entry) {
        append(value, entry.strategy().pluginId().value());
        append(value, entry.strategy().implementationVersion().toString());
        append(value, entry.strategy().strategyVersionId().value());
        entry.parameterDomains().forEach((name, domain) -> {
            append(value, name);
            append(value, domain.type().name());
            domain.options().forEach(option -> append(value, option.canonicalText()));
        });
        entry.constraints().forEach(constraint -> {
            append(value, "PARAMETER_LT");
            append(value, constraint.lowerParameter());
            append(value, constraint.upperParameter());
        });
    }

    private static void append(StringBuilder target, String token) {
        byte[] bytes = token.getBytes(StandardCharsets.UTF_8);
        target.append(bytes.length).append(':').append(token).append('\n');
    }

    private static String hash(String value) {
        try {
            return "sha256:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is required by the JVM", impossible);
        }
    }
}

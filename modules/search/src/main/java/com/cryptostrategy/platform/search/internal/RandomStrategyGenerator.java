package com.cryptostrategy.platform.search.internal;

import com.cryptostrategy.platform.search.api.model.GeneratedCandidate;
import com.cryptostrategy.platform.search.api.model.GenerationOutcome;
import com.cryptostrategy.platform.search.api.model.GenerationRequest;
import com.cryptostrategy.platform.search.api.model.GeneratorDescriptor;
import com.cryptostrategy.platform.search.api.model.GeneratorId;
import com.cryptostrategy.platform.search.api.model.GeneratorState;
import com.cryptostrategy.platform.search.api.model.GeneratorVersion;
import com.cryptostrategy.platform.search.api.port.in.StrategyGenerator;
import com.cryptostrategy.platform.strategy.api.model.parameter.ParameterType;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterValue;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Random Search thuần, phục hồi chính xác từ state SplitMix64 đã version hóa. */
public final class RandomStrategyGenerator implements StrategyGenerator {
    public static final String STATE_CONTRACT_VERSION = "random-state-v1";
    private static final long GOLDEN_GAMMA = 0x9E3779B97F4A7C15L;
    private static final Pattern STATE_PATTERN = Pattern.compile(
            "\\{\"state\":\"([0-9a-f]{16})\",\"drawIndex\":([0-9]+)}");
    private static final GeneratorDescriptor DESCRIPTOR = new GeneratorDescriptor(
            new GeneratorId("random-search"),
            GeneratorVersion.parse("1.0.0"),
            STATE_CONTRACT_VERSION,
            EnumSet.allOf(ParameterType.class),
            "random-search-v1:splitmix64-canonical-space");

    @Override
    public GeneratorDescriptor descriptor() {
        return DESCRIPTOR;
    }

    public static GeneratorState initialState(long seed) {
        return encodeState(seed, 0);
    }

    @Override
    public GenerationOutcome generateNext(GenerationRequest request) {
        if (BigInteger.valueOf(request.acceptedCandidateFingerprints().size())
                .compareTo(request.searchSpace().combinationCount()) >= 0) {
            return new GenerationOutcome.Exhausted(currentState(request));
        }

        PrngState state = restore(request);
        Map<String, StrategyParameterValue> values = new TreeMap<>();
        for (var parameter : request.searchSpace().parameters().entrySet()) {
            state = state.next();
            var options = parameter.getValue().options();
            int selected = (int) Long.remainderUnsigned(state.output(), options.size());
            values.put(parameter.getKey(), options.get(selected));
        }

        StrategyParameterSet parameters = StrategyParameterSet.of(values);
        GeneratorState nextState = encodeState(state.rawState(), state.drawIndex());
        return new GenerationOutcome.Generated(
                new GeneratedCandidate(
                        parameters,
                        request.expectedGenerationIndex(),
                        CanonicalSearchSpace.candidateFingerprint(parameters)),
                nextState);
    }

    private static PrngState restore(GenerationRequest request) {
        if (request.priorState().isEmpty()) {
            return new PrngState(request.seed(), 0, mix64(request.seed()));
        }
        GeneratorState persisted = request.priorState().orElseThrow();
        if (!STATE_CONTRACT_VERSION.equals(persisted.contractVersion())) {
            throw new IllegalArgumentException("Unsupported Random generator state contract: "
                    + persisted.contractVersion());
        }
        // PostgreSQL jsonb có thể chèn khoảng trắng khi materialize lại cùng JSON canonical.
        Matcher matcher = STATE_PATTERN.matcher(persisted.canonicalState().replaceAll("\\s+", ""));
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid canonical Random generator state");
        }
        long rawState = Long.parseUnsignedLong(matcher.group(1), 16);
        long drawIndex = Long.parseLong(matcher.group(2));
        GeneratorState verified = encodeState(rawState, drawIndex);
        if (!verified.fingerprint().equals(persisted.fingerprint())) {
            throw new IllegalArgumentException("Random generator state fingerprint mismatch");
        }
        return new PrngState(rawState, drawIndex, mix64(rawState));
    }

    private static GeneratorState currentState(GenerationRequest request) {
        return request.priorState().orElseGet(() -> encodeState(request.seed(), 0));
    }

    private static GeneratorState encodeState(long rawState, long drawIndex) {
        String canonical = "{\"state\":\"%016x\",\"drawIndex\":%d}"
                .formatted(rawState, drawIndex);
        return new GeneratorState(STATE_CONTRACT_VERSION, canonical, sha256(canonical));
    }

    private static String sha256(String value) {
        try {
            return "sha256:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is required by the JVM", impossible);
        }
    }

    private static long mix64(long value) {
        long mixed = value;
        mixed = (mixed ^ (mixed >>> 30)) * 0xBF58476D1CE4E5B9L;
        mixed = (mixed ^ (mixed >>> 27)) * 0x94D049BB133111EBL;
        return mixed ^ (mixed >>> 31);
    }

    private record PrngState(long rawState, long drawIndex, long output) {
        PrngState next() {
            long next = rawState + GOLDEN_GAMMA;
            return new PrngState(next, Math.addExact(drawIndex, 1), mix64(next));
        }
    }
}

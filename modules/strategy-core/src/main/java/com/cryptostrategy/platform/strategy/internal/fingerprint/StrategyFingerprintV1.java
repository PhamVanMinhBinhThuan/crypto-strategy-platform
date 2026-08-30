package com.cryptostrategy.platform.strategy.internal.fingerprint;

import com.cryptostrategy.platform.strategy.api.model.StrategyReference;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

public final class StrategyFingerprintV1 {
    private final CanonicalStrategyEncoder encoder = new CanonicalStrategyEncoder();
    public String single(StrategyReference reference, StrategyParameterSet parameters) { return digest(encoder.encodeSingle(reference, parameters)); }
    public String composite(String policy, List<byte[]> components) { return digest(encoder.encodeComposite(policy, components)); }
    private static String digest(byte[] value) {
        try { return "strategy-v1:sha256:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value)); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256 unavailable", exception); }
    }
}

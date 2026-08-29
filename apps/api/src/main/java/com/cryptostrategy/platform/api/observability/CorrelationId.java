package com.cryptostrategy.platform.api.observability;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.Clock;

public final class CorrelationId {
    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    private static final int MAX_CLIENT_LENGTH = 128;
    private static final char[] CROCKFORD_BASE32 = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private CorrelationId() {
    }

    public static String resolve(String candidate) {
        return isValid(candidate) ? candidate : generate(Clock.systemUTC());
    }

    static String generate(Clock clock) {
        byte[] value = new byte[16];
        long timestamp = clock.millis();
        for (int index = 5; index >= 0; index--) {
            value[index] = (byte) timestamp;
            timestamp >>>= 8;
        }
        byte[] randomness = new byte[10];
        RANDOM.nextBytes(randomness);
        System.arraycopy(randomness, 0, value, 6, randomness.length);

        BigInteger number = new BigInteger(1, value);
        char[] encoded = new char[26];
        BigInteger mask = BigInteger.valueOf(31);
        for (int index = encoded.length - 1; index >= 0; index--) {
            encoded[index] = CROCKFORD_BASE32[number.and(mask).intValue()];
            number = number.shiftRight(5);
        }
        return new String(encoded);
    }

    private static boolean isValid(String candidate) {
        if (candidate == null || candidate.isBlank() || candidate.length() > MAX_CLIENT_LENGTH) {
            return false;
        }
        return candidate.chars().noneMatch(character -> Character.isISOControl((char) character));
    }
}

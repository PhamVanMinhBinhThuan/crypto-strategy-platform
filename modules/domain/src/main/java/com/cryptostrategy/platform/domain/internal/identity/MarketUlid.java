package com.cryptostrategy.platform.domain.internal.identity;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Objects;
import java.util.regex.Pattern;

public final class MarketUlid {
    private static final Pattern FORMAT = Pattern.compile("^[0-9A-HJKMNP-TV-Z]{26}$");
    private static final char[] ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private MarketUlid() {
    }

    public static String requireValid(String value) {
        Objects.requireNonNull(value, "ULID");
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("Market ID must be an uppercase Crockford ULID");
        }
        return value;
    }

    public static String generate() {
        byte[] bytes = new byte[16];
        long timestamp = Clock.systemUTC().millis();
        for (int index = 5; index >= 0; index--) {
            bytes[index] = (byte) timestamp;
            timestamp >>>= 8;
        }
        byte[] random = new byte[10];
        RANDOM.nextBytes(random);
        System.arraycopy(random, 0, bytes, 6, random.length);
        BigInteger number = new BigInteger(1, bytes);
        char[] encoded = new char[26];
        BigInteger mask = BigInteger.valueOf(31);
        for (int index = encoded.length - 1; index >= 0; index--) {
            encoded[index] = ALPHABET[number.and(mask).intValue()];
            number = number.shiftRight(5);
        }
        return new String(encoded);
    }
}
